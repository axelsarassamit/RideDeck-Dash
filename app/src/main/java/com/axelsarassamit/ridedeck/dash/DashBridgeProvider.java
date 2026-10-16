package com.axelsarassamit.ridedeck.dash;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import java.security.MessageDigest;

/** Authenticated heartbeat only. This provider never opens a camera. */
public final class DashBridgeProvider extends ContentProvider {
    public static final RideSessionPolicy SESSIONS = new RideSessionPolicy();
    private static final String HOST_CERT = "79f76edba53e22dcb9e4610dc5fa151e5c52d0895c68b2802444f1ced3a1c06f";
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        String caller = verifiedCaller(getContext(), Binder.getCallingUid());
        if (extras == null || extras.getInt("protocolVersion", 0) != 1)
            throw new IllegalArgumentException("Dash bridge requires protocolVersion 1");
        Bundle result = new Bundle();
        result.putInt("protocolVersion", 1);
        if("appearance".equals(method)) {
            RideAppearance.receive(getContext(),extras); result.putBoolean("accepted",true); return result;
        }
        if("pauseForVoice".equals(method)) {
            long now=SystemClock.elapsedRealtime();
            if(SESSIONS.isActive(now) && !SESSIONS.isOwnedBy(caller,now))
                throw new SecurityException("The other RideDeck app owns this recording session");
            boolean resumeAllowed=SESSIONS.isOwnedBy(caller,now);
            try {
                boolean paused=RecordingService.pauseForVoice();
                result.putBoolean("accepted",true);result.putBoolean("paused",paused);
                result.putBoolean("resumeAllowed",paused && resumeAllowed);
                return result;
            } catch(InterruptedException error) {
                Thread.currentThread().interrupt();throw new IllegalStateException("Voice preparation interrupted");
            }
        }
        if ("renew".equals(method)) result.putBoolean("accepted", SESSIONS.renew(caller, SystemClock.elapsedRealtime()));
        else if ("stop".equals(method)) { SESSIONS.stop(caller); result.putBoolean("accepted", true); }
        else if (!"status".equals(method)) throw new IllegalArgumentException("Unknown Dash bridge method");
        result.putBoolean("rideActive", SESSIONS.isActive(SystemClock.elapsedRealtime()));
        if ("status".equals(method)) {
            result.putInt("appearanceMode", RideAppearance.mode(getContext()));
            result.putInt("appearanceAccent", RideAppearance.accentIndex(getContext()));
            result.putBoolean("appearanceFollowsHost", RideAppearance.followsHost(getContext()));
            result.putInt("appearanceHostMode", RideAppearance.prefs(getContext()).getInt("host_mode",0));
            result.putInt("appearanceHostAccent", RideAppearance.prefs(getContext()).getInt("host_accent",4));
        }
        return result;
    }
    static String verifiedCaller(android.content.Context context, int uid) {
        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null) for (String name : packages) {
            if (!RideSessionPolicy.STANDARD.equals(name) && !RideSessionPolicy.YAMAHA.equals(name)) continue;
            try {
                PackageInfo info = pm.getPackageInfo(name, Build.VERSION.SDK_INT >= 28
                    ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
                Signature[] signers = Build.VERSION.SDK_INT >= 28
                    ? info.signingInfo.getApkContentsSigners() : info.signatures;
                if (signers == null || signers.length != 1) continue;
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
                StringBuilder hex = new StringBuilder();
                for (byte b : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
                if (HOST_CERT.equals(hex.toString())) return name;
            } catch (Exception ignored) { /* Fail closed on unknown package/signing state. */ }
        }
        throw new SecurityException("Only signed RideDeck companion apps may renew a Dash ride session");
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
