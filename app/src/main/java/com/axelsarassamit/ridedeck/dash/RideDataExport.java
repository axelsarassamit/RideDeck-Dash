package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.*;

final class RideDataExport {
    static boolean included(String name, boolean workshop) {
        if ("route.csv".equals(name)) return true;
        return workshop && ("capture.csv".equals(name) || "gyro.csv".equals(name)
            || "accel.csv".equals(name) || "frames.csv".equals(name) || "motion.json".equals(name)
            || (name.startsWith("hybrid-") && name.endsWith("-report.json")));
    }

    static void save(Activity activity, File ride, boolean workshop) {
        if (RecordingService.running || RecordingService.finalizing) {
            toast(activity, "Stop recording before saving ride data");
            return;
        }
        if (Build.VERSION.SDK_INT < 29) {
            toast(activity, "Ride data export requires Android 10 or newer");
            return;
        }
        new Thread(() -> {
            try {
                File[] files = ride.listFiles(file -> file.isFile() && included(file.getName(), workshop));
                if (files == null) throw new IOException("Ride data unavailable");
                if (files.length == 0) { toast(activity, "No saved ride data"); return; }
                for (File file : files) {
                    String name = file.getName();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Dash-" + ride.getName() + "-" + name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, name.endsWith(".json") ? "application/json" : "text/csv");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/RideDeckDash");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IOException("Downloads unavailable");
                    try {
                        try (InputStream in = new FileInputStream(file); OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                            if (out == null) throw new IOException("Downloads unavailable");
                            byte[] buffer = new byte[8192];
                            for (int n; (n = in.read(buffer)) != -1;) out.write(buffer, 0, n);
                        }
                        values.clear();
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        if (activity.getContentResolver().update(uri, values, null, null) <= 0)
                            throw new IOException("Could not publish ride data");
                    } catch (Exception error) {
                        activity.getContentResolver().delete(uri, null, null);
                        throw error;
                    }
                }
                toast(activity, "Saved in Downloads / RideDeckDash");
            } catch (Exception error) {
                FailureHistory.add(activity, FailureHistory.Event.RIDE_DATA_EXPORT);
                toast(activity, "Could not save ride data");
            }
        }, "DashDataExport").start();
    }

    private static void toast(Activity activity, String text) {
        activity.runOnUiThread(() -> {
            if (!activity.isDestroyed()) Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
        });
    }
}
