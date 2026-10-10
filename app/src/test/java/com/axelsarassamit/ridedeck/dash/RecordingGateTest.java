package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.axelsarassamit.ridedeck.dash.RecordingGate.Result.*;
public class RecordingGateTest {
    @Test public void standaloneAndBackgroundRules() {
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 50, true, false));
        assertEquals(COMPANION_REQUIRED, RecordingGate.evaluate(true, 50, false, false));
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 50, false, true));
    }
    @Test public void chargerOrAtLeastHalfBattery() {
        assertEquals(ALLOWED, RecordingGate.evaluate(false, 100, true, true));
        assertEquals(ALLOWED, RecordingGate.evaluate(false, 50, true, false));
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 49, true, true));
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 0, false, true));
        assertEquals(ALLOWED, RecordingGate.evaluate(true, -1, true, true));
        assertEquals(BATTERY_UNKNOWN, RecordingGate.evaluate(false, -1, true, true));
        assertEquals(BATTERY_UNKNOWN, RecordingGate.evaluate(false, 101, true, true));
    }
    @Test public void unplugDropAndBackgroundTransitionsStop() {
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 51, true, false));
        assertEquals(ALLOWED, RecordingGate.evaluate(false, 51, true, false));
        assertEquals(ALLOWED, RecordingGate.evaluate(true, 49, true, false));
        assertNotEquals(ALLOWED, RecordingGate.evaluate(false, 49, true, false));
        assertNotEquals(ALLOWED, RecordingGate.evaluate(true, 51, false, false));
    }
    @Test public void batteryConversionFailsClosed() {
        assertEquals(50, RecordingGate.batteryPercent(100, 200));
        assertEquals(49, RecordingGate.batteryPercent(99, 200));
        assertEquals(-1, RecordingGate.batteryPercent(-1, 100));
        assertEquals(-1, RecordingGate.batteryPercent(1, 0));
        assertEquals(-1, RecordingGate.batteryPercent(101, 100));
    }
}
