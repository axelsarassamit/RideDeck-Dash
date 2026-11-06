package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class BugReportTest {
    @Test public void failedExportAppearsInReviewedTextAndGitHubDraft() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".debug"));
        FailureHistory.clear(context);
        try {
            try (ActivityScenario<SocialExportActivity> ignored = ActivityScenario.launch(SocialExportActivity.class)) {
                assertEquals(FailureHistory.Event.EXPORT_PREPARATION, FailureHistory.read(context).get(0));
            }
            try (ActivityScenario<BugReportActivity> screen = ActivityScenario.launch(BugReportActivity.class)) {
                screen.onActivity(activity -> {
                    String expected = FailureHistory.report(FailureHistory.read(context));
                    assertTrue(hasText(activity.getWindow().getDecorView(), expected));
                    assertTrue(hasText(activity.getWindow().getDecorView(), "Report on GitHub"));
                    Uri draft = BugReportActivity.issueDraft(expected);
                    assertEquals("https", draft.getScheme());
                    assertEquals("github.com", draft.getHost());
                    assertEquals("/axelsarassamit/RideDeck-Dash/issues/new", draft.getPath());
                    assertEquals(expected, draft.getQueryParameter("body"));
                });
            }
        } finally { FailureHistory.clear(context); }
    }

    @Test public void ordinaryDashboardLaunchRejectsWorkshopSampleExtras() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".debug"));
        Intent intent = new Intent(context, DashboardActivity.class)
            .putExtra("sampleProfile", "native_sample").putExtra("sampleSeconds", 30);
        try (ActivityScenario<DashboardActivity> screen = ActivityScenario.launch(intent)) {
            screen.onActivity(activity -> {
                assertNull(activity.sampleProfile);
                assertFalse(activity.sampleStarted);
            });
        }
    }

    private boolean hasText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasText(group.getChildAt(i), expected)) return true;
        }
        return false;
    }
}
