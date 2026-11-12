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
import java.util.Locale;

/** Authenticated heartbeat only. This provider never opens a camera. */
public final class DashBridgeProvider extends ContentProvider {
    public static final RideSessionPolicy SESSIONS = new RideSessionPolicy();
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        String caller = verifiedCaller(getContext(), Binder.getCallingUid());
        if (extras == null || extras.getInt("protocolVersion", 0) != 1)
            throw new IllegalArgumentException("Dash bridge requires protocolVersion 1");
        Bundle result = new Bundle();
        result.putInt("protocolVersion", 1);
        if("appearance".equals(method)) {
            RideAppearance.receive(getContext(),extras,caller); result.putBoolean("accepted",true); return result;
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
        String ownCertificate = null;
        if (BuildConfig.DEBUG) try { ownCertificate = certificate(pm, context.getPackageName()); }
        catch (Exception ignored) { /* Unknown debug signing state grants no debug access. */ }
        if (packages != null) for (String name : packages) {
            if (!RideSessionPolicy.STANDARD.equals(name) && !RideSessionPolicy.YAMAHA.equals(name)
                    && !(BuildConfig.DEBUG && ((RideSessionPolicy.STANDARD + ".preview").equals(name)
                    || (RideSessionPolicy.YAMAHA + ".preview").equals(name)))) continue;
            try {
                String host = CompanionCallerPolicy.canonicalHost(name, certificate(pm, name), BuildConfig.DEBUG, ownCertificate);
                if (host != null) return host;
            } catch (Exception ignored) { /* Fail closed on unknown package/signing state. */ }
        }
        throw new SecurityException("Only signed RideDeck companion apps may renew a Dash ride session");
    }
    private static String certificate(PackageManager pm, String name) throws Exception {
        PackageInfo info = pm.getPackageInfo(name, Build.VERSION.SDK_INT >= 28
            ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
        Signature[] signers = Build.VERSION.SDK_INT >= 28
            ? (info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners()) : info.signatures;
        if (signers == null || signers.length != 1) return null;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
        return hex.toString();
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
