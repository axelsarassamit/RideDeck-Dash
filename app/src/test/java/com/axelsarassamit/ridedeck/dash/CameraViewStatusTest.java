package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public final class CameraViewStatusTest {
    @Test public void errorsRemainVisibleEvenWhenAnOldFrameExists() {
        assertEquals("! Camera disconnected",CameraViewStatus.label("Camera disconnected",true));
        assertEquals("! Camera view unavailable",CameraViewStatus.label("Camera view unavailable",true));
        assertEquals("! Camera view error 4",CameraViewStatus.label("Camera view error 4",true));
    }
    @Test public void openingNeverClaimsReadyFromAnOldFrame() {
        assertEquals("◌ Connecting · Opening cameras",CameraViewStatus.label("Opening cameras",true));
    }
    @Test public void readyNeedsAFrameAndTheLiveState() {
        assertEquals("○ Ready · Live camera",CameraViewStatus.label("Live camera view",true));
        assertEquals("◌ Connecting · Camera view",CameraViewStatus.label("Live camera view",false));
    }
    @Test public void unknownStatesKeepTheirExplanation() {
        assertEquals("! Camera view unavailable",CameraViewStatus.label(null,true));
        assertEquals("Tap Camera view to align your cameras",CameraViewStatus.label("Tap Camera view to align your cameras",false));
    }
}
