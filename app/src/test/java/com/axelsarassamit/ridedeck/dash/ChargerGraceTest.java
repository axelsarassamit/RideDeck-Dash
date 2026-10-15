package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.axelsarassamit.ridedeck.dash.RecordingGate.Result.*;
public class ChargerGraceTest {
    @Test public void recordingThresholdAndExactDeadline() {
        assertEquals(ALLOWED, RecordingGate.recording(false,30,true,false,299999));
        assertEquals(BATTERY_TOO_LOW, RecordingGate.recording(false,29,true,false,0));
        assertEquals(CHARGER_TIMEOUT, RecordingGate.recording(false,100,true,false,300000));
        assertEquals(BATTERY_UNKNOWN, RecordingGate.recording(false,-1,true,false,0));
        assertEquals(ALLOWED, RecordingGate.recording(true,0,true,false,999999));
        assertEquals(COMPANION_REQUIRED, RecordingGate.recording(false,80,false,false,0));
        assertEquals(ALLOWED, RecordingGate.recording(false,80,false,true,0));
    }
    @Test public void reconnectResetsButRestartDoesNot() {
        ChargerGrace grace = new ChargerGrace(-1);
        assertEquals(0,grace.elapsed(false,100));
        assertEquals(300000,grace.elapsed(false,300100));
        ChargerGrace restarted = new ChargerGrace(grace.observe(false,300100));
        assertEquals(300100,restarted.elapsed(false,300200));
        assertEquals(0,restarted.elapsed(true,300300));
        assertEquals(0,restarted.elapsed(false,300400));
        assertEquals(100,restarted.elapsed(false,300500));
    }
    @Test public void rollbackFailsToNewBudgetRatherThanNegativeTime() {
        assertEquals(0,new ChargerGrace(900000).elapsed(false,100));
    }
    @Test public void heavyWorkKeepsItsReserve() {
        assertEquals(BATTERY_TOO_LOW, RecordingGate.evaluate(false,30,true,false));
        assertEquals(ALLOWED, RecordingGate.evaluate(false,50,true,false));
    }
}
