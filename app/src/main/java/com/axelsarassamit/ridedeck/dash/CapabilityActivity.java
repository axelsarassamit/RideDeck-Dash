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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.WindowInsets;
import android.graphics.Insets;

/** Setup-only capability inspector. Does not open cameras or begin recording. */
public final class CapabilityActivity extends Activity {
    private TextView powerStatus;
    private Button update;
    private boolean powerAllowed;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent battery) { applyBattery(battery); }
    };
    private void applyBattery(Intent battery) {
        boolean plugged = battery != null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int percent = battery == null ? -1 : RecordingGate.batteryPercent(
            battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
        RecordingGate.Result result = RecordingGate.evaluate(plugged, percent, true, false);
        powerAllowed = result == RecordingGate.Result.ALLOWED;
        if (powerStatus != null) powerStatus.setText(powerAllowed
            ? "Power ready: " + (plugged ? "charger connected" : "battery " + percent + "%")
            : "Dash paused: " + (result == RecordingGate.Result.BATTERY_TOO_LOW ? "connect a charger or charge to 50%" : "battery unknown; connect a charger"));
        if (update != null) update.setEnabled(powerAllowed && !BuildConfig.DEBUG);
    }
    @Override protected void onStart() {
        super.onStart();
        applyBattery(registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
    }
    @Override protected void onStop() {
        unregisterReceiver(batteryReceiver);
        super.onStop();
    }
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this); super.onCreate(state);
        TextView text = new TextView(this);
        text.setTextSize(20);
        text.setPadding(28, 32, 28, 32);
        StringBuilder report = new StringBuilder("RideDeck Dash\nCamera diagnostics\n\n");
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
                if(label.equals("Rear"))report.append(RearLenses.label(c)).append("\n");
                if(Build.VERSION.SDK_INT>=28)report.append("Physical camera IDs: ").append(c.getPhysicalCameraIds()).append("\n");
                if(Build.VERSION.SDK_INT>=28)for(String physical:c.getPhysicalCameraIds()){
                    try{report.append("  Physical ").append(physical).append(": ").append(RearLenses.label(manager.getCameraCharacteristics(physical))).append("\n");}
                    catch(Exception ignored){report.append("  Physical ").append(physical).append(": metadata unavailable\n");}
                }
                report.append("Video stabilization modes: ").append(Arrays.toString(c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES))).append("\n");
                report.append("Optical stabilization modes: ").append(Arrays.toString(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION))).append("\n\n");
            }
            report.append("Concurrent camera groups: ");
            report.append(Build.VERSION.SDK_INT >= 30 ? manager.getConcurrentCameraIds().toString() : "Capability API unavailable");
        } catch (Exception error) {
            report.append("Camera capability check unavailable: ").append(error.getClass().getSimpleName());
        }
        report.append("\n\nThese are advertised camera modes. Actual recording support depends on the selected capture configuration. Return to the dashboard to record.");
        text.setText(report);
        ScrollView scroll = new ScrollView(this);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(text);
        powerStatus = new TextView(this);
        powerStatus.setTextSize(20);
        powerStatus.setPadding(28, 16, 28, 16);
        page.addView(powerStatus, 0);
        update = new Button(this);
        RideStyle.action(update,true);
        update.setText("Check for updates");
        update.setEnabled(false);
        if (BuildConfig.DEBUG) update.setText("Updates available in the signed release app");
        update.setOnClickListener(v -> { if (powerAllowed) new ReleaseUpdater(this).check(update); });
        page.addView(update);
        scroll.addView(page);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }
}
