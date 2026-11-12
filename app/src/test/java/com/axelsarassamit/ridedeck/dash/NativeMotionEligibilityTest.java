package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class NativeMotionEligibilityTest {
 @Test public void recordedOisIsNotACompensationModel(){assertFalse(NativeMotionEligibility.rawGyroSupported("0","1"));assertFalse(NativeMotionEligibility.rawGyroSupported("1","0"));assertFalse(NativeMotionEligibility.rawGyroSupported("2","0"));}
 @Test public void unknownModesAreRejected(){for(String mode:new String[]{null,"","unknown","-1"}){assertFalse(NativeMotionEligibility.rawGyroSupported(mode,"0"));assertFalse(NativeMotionEligibility.rawGyroSupported("0",mode));}assertTrue(NativeMotionEligibility.rawGyroSupported("0","0"));}
}
