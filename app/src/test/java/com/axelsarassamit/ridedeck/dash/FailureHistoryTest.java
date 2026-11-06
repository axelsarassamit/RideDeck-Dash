package com.axelsarassamit.ridedeck.dash;

import org.junit.Test;
import static org.junit.Assert.*;

public class FailureHistoryTest {
    @Test public void keepsOnlyLatestThirtyEventsInOrder() {
        String saved = "";
        for (int i = 0; i < 40; i++) saved = FailureHistory.append(saved, FailureHistory.Event.RECORDING_START);
        saved = FailureHistory.append(saved, FailureHistory.Event.EXPORT_SAVE);
        assertEquals(30, FailureHistory.decode(saved).size());
        assertEquals(FailureHistory.Event.EXPORT_SAVE, FailureHistory.decode(saved).get(0));
        assertEquals(FailureHistory.Event.RECORDING_START, FailureHistory.decode(saved).get(29));
    }

    @Test public void unknownStoredDataNeverReachesReportOrNewHistory() {
        String bad = "RECORDING_START\n/storage/emulated/0/private.mp4\nlocation=13.75,100.5\ntoken=secret";
        String saved = FailureHistory.append(bad, FailureHistory.Event.EXPORT_RENDER);
        assertEquals("EXPORT_RENDER\nRECORDING_START", saved);
        String report = FailureHistory.report(FailureHistory.decode(bad));
        assertTrue(report.contains("Recording could not start"));
        assertFalse(report.contains("private.mp4"));
        assertFalse(report.contains("13.75"));
        assertFalse(report.contains("secret"));
    }

    @Test public void workshopSidecarsAreExcludedFromOrdinaryExports() {
        assertTrue(RideDataExport.included("route.csv", false));
        for (String file : new String[]{"capture.csv", "frames.csv", "gyro.csv", "accel.csv", "motion.json", "hybrid-rear-report.json"}) {
            assertFalse(file, RideDataExport.included(file, false));
            assertTrue(file, RideDataExport.included(file, true));
        }
        assertFalse(RideDataExport.included("rear-0001.mp4", true));
    }
}
