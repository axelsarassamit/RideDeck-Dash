package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

/** Foreground handoff from the workshop app to Dash-owned camera and audio capture. */
public final class DashLabActivity extends Activity {
    private static final String LAB_PACKAGE = "app.yamahalab";

    @Override protected void onCreate(Bundle state) {
        RideAppearance.prepare(this);
        super.onCreate(state);
        if (!LAB_PACKAGE.equals(getCallingPackage())) {
            finish();
            return;
        }
        launched = state != null && state.getBoolean("launched");
        if (launched) return;
        switch (getIntent().getAction() == null ? "" : getIntent().getAction()) {
            case "app.yamahalab.action.DASH_REVIEW":
                launch(new Intent(this, CalibrationActivity.class));
                break;
            case "app.yamahalab.action.DASH_AUDIO":
                launch(new Intent(this, AudioCheckActivity.class));
                break;
            case "app.yamahalab.action.DASH_SAMPLE":
                confirmSample();
                break;
            default:
                finish();
        }
    }

    private boolean launched;

    private void confirmSample() {
        String profile = getIntent().getStringExtra("profile");
        int seconds = getIntent().getIntExtra("seconds", 0);
        boolean hybrid = "hybrid_sample".equals(profile);
        if ((!hybrid && !"native_sample".equals(profile)) ||
                (seconds != 30 && seconds != 120 && seconds != 300)) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(hybrid ? "Record hybrid sample" : "Record native reference")
                .setMessage("Start while parked. Dash will stop this rear-camera sample after " + seconds +
                        " seconds. " + (hybrid ? "Native stabilization is disabled for this sample." :
                        "This sample uses your configured native stabilization."))
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .setPositiveButton("Start sample", (dialog, which) -> launch(new Intent(this, DashboardActivity.class)
                        .putExtra("sampleProfile", profile).putExtra("sampleSeconds", seconds)))
                .show();
    }

    private void launch(Intent intent) {
        launched = true;
        startActivityForResult(intent, 1);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("launched", launched);
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 1) finish();
    }
}
