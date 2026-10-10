package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Button;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** User-initiated stable updates. Android retains final installation approval. */
final class ReleaseUpdater {
    private static final String REPO = "axelsarassamit/RideDeck-Dash";
    private static final String APK = "ridedeck-dash-release.apk";
    private final Activity activity;
    ReleaseUpdater(Activity activity) { this.activity = activity; }
    void check(Button button) {
        button.setEnabled(false);
        button.setText("Checking for updates...");
        new Thread(() -> {
            try {
                JSONObject release = new JSONObject(read("https://api.github.com/repos/" + REPO + "/releases/latest"));
                if (release.getBoolean("draft") || release.getBoolean("prerelease")) throw new IOException("No stable release available");
                int code = versionCode(release.getString("tag_name"));
                if (code <= BuildConfig.VERSION_CODE) { message("You have the latest stable version."); return; }
                String apkUrl = null, checksumUrl = null;
                JSONArray assets = release.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if (APK.equals(asset.getString("name"))) apkUrl = asset.getString("browser_download_url");
                    if ("checksums.txt".equals(asset.getString("name"))) checksumUrl = asset.getString("browser_download_url");
                }
                if (apkUrl == null || checksumUrl == null) throw new IOException("Release APK or checksum missing");
                validateAssetUrl(apkUrl); validateAssetUrl(checksumUrl);
                String hash = null;
                for (String line : read(checksumUrl).split("\\r?\\n")) {
                    String[] fields = line.trim().split("\\s+");
                    if (fields.length == 2 && fields[1].replaceFirst("^\\*", "").equals(APK) && fields[0].matches("[a-fA-F0-9]{64}")) hash = fields[0];
                }
                if (hash == null) throw new IOException("Valid APK checksum missing");
                File folder = new File(activity.getCacheDir(), "updates");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Cannot prepare update storage");
                File apk = new File(folder, APK);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                HttpURLConnection c = connection(apkUrl);
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[8192]; int n; long size = 0;
                    while ((n = in.read(buffer)) != -1) {
                        size += n;
                        if (size > 250L * 1024 * 1024) throw new IOException("Update exceeds size limit");
                        out.write(buffer, 0, n); digest.update(buffer, 0, n);
                    }
                } finally { c.disconnect(); }
                StringBuilder actual = new StringBuilder();
                for (byte b : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", b & 255));
                if (!hash.equalsIgnoreCase(actual.toString())) { apk.delete(); throw new SecurityException("APK checksum mismatch"); }
                validateIdentity(apk, code);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    new AlertDialog.Builder(activity).setTitle("RideDeck Dash update")
                        .setMessage("Version " + release.optString("tag_name") + " is verified. Open Android's installer?")
                        .setNegativeButton("Later", null).setPositiveButton("Install", (dialog, which) -> install(apk)).show();
                });
            } catch (FileNotFoundException e) { message("No stable release is published yet."); }
            catch (Exception e) { message("Update unavailable: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())); }
            finally { activity.runOnUiThread(() -> { button.setEnabled(true); button.setText("Check for updates"); }); }
        }, "dash-update").start();
    }
    static int versionCode(String tag) {
        if (!tag.matches("v[0-9]{1,3}\\.[0-9]{1,2}\\.[0-9]{1,2}")) throw new IllegalArgumentException("Invalid release version");
        String[] p = tag.substring(1).split("\\.");
        return Integer.parseInt(p[0]) * 10000 + Integer.parseInt(p[1]) * 100 + Integer.parseInt(p[2]);
    }
    private void validateIdentity(File apk, int expectedCode) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flags);
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName) || candidate.versionCode != expectedCode
            || candidate.versionCode <= installed.versionCode) throw new SecurityException("Update package or version mismatch");
        if (!signers(installed).equals(signers(candidate))) throw new SecurityException("Update signing certificate mismatch");
    }
    private Set<String> signers(PackageInfo info) {
        Signature[] signatures = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
            ? info.signingInfo.getApkContentsSigners() : info.signatures;
        if (signatures == null || signatures.length == 0) throw new SecurityException("Missing signing identity");
        Set<String> result = new HashSet<>();
        for (Signature signature : signatures) result.add(signature.toCharsString());
        return result;
    }
    private void install(File apk) {
        try {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(activity).setMessage("Allow RideDeck Dash to install updates, then tap Check for updates again.")
                    .setPositiveButton("Open settings", (d, w) -> activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()))))
                    .setNegativeButton("Cancel", null).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", apk);
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { message("Could not open the installer."); }
    }
    private void message(String text) {
        activity.runOnUiThread(() -> { if (!activity.isFinishing() && !activity.isDestroyed()) new AlertDialog.Builder(activity)
            .setTitle("RideDeck Dash").setMessage(text).setPositiveButton("OK", null).show(); });
    }
    private static void validateAssetUrl(String address) {
        if (!address.startsWith("https://github.com/" + REPO + "/releases/download/")) throw new SecurityException("Unexpected release source");
    }
    private static HttpURLConnection connection(String address) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "RideDeck-Dash");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        return c;
    }
    private static String read(String address) throws IOException {
        HttpURLConnection c = connection(address);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) { if (out.size() + n > 2 * 1024 * 1024) throw new IOException("Response too large"); out.write(buffer, 0, n); }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally { c.disconnect(); }
    }
}
