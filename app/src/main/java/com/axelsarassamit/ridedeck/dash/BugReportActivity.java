package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public final class BugReportActivity extends Activity {
    private TextView history;
    private Button share, github, copy, clear;
    private String visibleReport;

    @Override protected void onCreate(android.os.Bundle state) {
        RideAppearance.prepare(this);
        super.onCreate(state);
        LinearLayout page = DashUi.page(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        setContentView(scroll);
        DashUi.text(this, page, "Bug report", 26);
        DashUi.text(this, page, "Dash keeps up to 30 recent failures on this device. Review the exact report below before sharing it. The report does not include recordings, locations, file paths or technical logs.", 16);
        history = DashUi.text(this, page, "", 15);
        history.setTextIsSelectable(true);
        share = DashUi.button(this, page, "Share report", v -> {
            if (visibleReport == null) return;
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, visibleReport);
            startActivity(Intent.createChooser(send, "Share Dash bug report"));
        });
        github = DashUi.button(this, page, "Report on GitHub", v -> reviewGitHub());
        copy = DashUi.button(this, page, "Copy report", v -> { if (visibleReport != null) copy(visibleReport); });
        clear = DashUi.button(this, page, "Clear failure history", v -> new AlertDialog.Builder(this)
            .setTitle("Clear failure history?")
            .setMessage("This clears only the bug report history.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (d, w) -> { FailureHistory.clear(this); refresh(); }).show());
        DashUi.button(this, page, "Back", v -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<FailureHistory.Event> events = FailureHistory.read(this);
        visibleReport = FailureHistory.report(events);
        history.setText(visibleReport);
        clear.setEnabled(!events.isEmpty());
    }

    static Uri issueDraft(String body) {
        return Uri.parse("https://github.com/axelsarassamit/RideDeck-Dash/issues/new").buildUpon()
            .appendQueryParameter("title", "RideDeck Dash bug report")
            .appendQueryParameter("body", body).build();
    }

    private void reviewGitHub() {
        String report = visibleReport;
        if (report == null) return;
        Uri prefilled = issueDraft(report);
        boolean longReport = prefilled.toString().length() > 1800;
        new AlertDialog.Builder(this).setTitle("Report on GitHub")
            .setMessage("This repository is public. GitHub opens a draft; you sign in, review and submit it there."
                + (longReport ? " This report is long. Copy it and paste it into the draft." : ""))
            .setNegativeButton("Cancel", null)
            .setPositiveButton(longReport ? "Copy and open GitHub" : "Open GitHub draft", (d, w) -> {
                if (longReport) copy(report);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, longReport
                        ? issueDraft("Paste the reviewed Dash report here.") : prefilled));
                } catch (ActivityNotFoundException unavailable) {
                    Toast.makeText(this, "No app can open GitHub. You can copy the report instead.", Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    private void copy(String report) {
        getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Dash bug report", report));
        Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show();
    }
}
