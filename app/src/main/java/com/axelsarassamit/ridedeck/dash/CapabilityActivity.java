package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.widget.TextView;
import android.widget.ScrollView;
import java.util.Arrays;
import android.widget.Button;
import android.widget.LinearLayout;

/** Setup-only capability inspector. Does not open cameras or begin recording. */
public final class CapabilityActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this);
        text.setTextSize(20);
        text.setPadding(28, 32, 28, 32);
        StringBuilder report = new StringBuilder("RideDeck Dash\nDevelopment foundation\n\n");
        report.append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
            .append("\nAndroid ").append(Build.VERSION.RELEASE).append("\n\n");
        try {
            CameraManager manager = getSystemService(CameraManager.class);
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                String label = facing == null ? "Unknown" : facing == CameraCharacteristics.LENS_FACING_BACK
                    ? "Rear" : facing == CameraCharacteristics.LENS_FACING_FRONT ? "Front" : "External";
                report.append(label).append(" camera ").append(id).append("\n");
                report.append("Video stabilization modes: ").append(Arrays.toString(c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES))).append("\n");
                report.append("Optical stabilization modes: ").append(Arrays.toString(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION))).append("\n\n");
            }
            report.append("Concurrent camera groups: ");
            report.append(Build.VERSION.SDK_INT >= 30 ? manager.getConcurrentCameraIds().toString() : "Capability API unavailable");
        } catch (Exception error) {
            report.append("Camera capability check unavailable: ").append(error.getClass().getSimpleName());
        }
        report.append("\n\nRecording is not implemented in this foundation.\nThe next step is a recording-session test on the Xiaomi 14T Pro. Advertised camera modes do not establish stable dual recording.");
        text.setText(report);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(text);
        Button update = new Button(this);
        update.setText("Check for updates");
        update.setEnabled(!BuildConfig.DEBUG);
        if (BuildConfig.DEBUG) update.setText("Updates available in the signed release app");
        update.setOnClickListener(v -> new ReleaseUpdater(this).check(update));
        page.addView(update);
        scroll.addView(page);
        setContentView(scroll);
    }
}
