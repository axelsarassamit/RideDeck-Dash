package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class CapturePacingTest {
    @Test public void sustainedPressureRelievesPreviewOnlyOnce(){
        CapturePacing pacing=new CapturePacing();assertFalse(pacing.observe(25,9));
        assertEquals(200,pacing.previewInterval(200));assertTrue(pacing.observe(26,6));
        assertEquals(500,pacing.previewInterval(200));assertEquals(1000,pacing.previewInterval(1000));
        assertFalse(pacing.observe(30,0));assertEquals(500,pacing.previewInterval(200));
    }
    @Test public void oneIsolatedPauseDoesNotAlterPreview(){
        CapturePacing pacing=new CapturePacing();assertFalse(pacing.observe(24,8));
        assertFalse(pacing.observe(30,0));assertFalse(pacing.observe(24,8));assertEquals(200,pacing.previewInterval(200));
    }
    @Test public void captureGapsBreakTrackingButNormalCadenceDoesNot(){
        assertFalse(StabilizationPath.captureGap(0,33333));assertFalse(StabilizationPath.captureGap(0,66666));
        assertTrue(StabilizationPath.captureGap(500000,600000));assertTrue(StabilizationPath.captureGap(0,433155));
    }
}
