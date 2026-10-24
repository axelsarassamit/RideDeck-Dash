package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;import static org.junit.Assert.*;
public class DriveAutomationTest {
 @Test public void invalidFixCannotStartOrStop(){for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1}){DriveAutomation d=new DriveAutomation();assertFalse(d.start(0,true,3,10));assertFalse(d.start(3000,true,bad,10));assertFalse(d.start(4000,true,3,10));assertFalse(d.start(7000,true,3,bad));assertFalse(d.stop(0,false,true,0,10,100));assertFalse(d.stop(100,false,true,bad,10,100));assertFalse(d.stop(200,false,true,0,10,100));assertFalse(d.stop(300,false,true,0,bad,100));}}

 @Test public void sustainedMovementResetsOnStaleFix(){DriveAutomation d=new DriveAutomation();assertFalse(d.start(0,true,3,10));assertFalse(d.start(2999,true,3,10));assertFalse(d.start(3000,false,3,10));assertFalse(d.start(4000,true,3,10));assertTrue(d.start(7000,true,3,10));}
 @Test public void unplugAndFreshStationaryMustLastFullMinute(){DriveAutomation d=new DriveAutomation();assertFalse(d.stop(0,true,true,0,10,60000));assertFalse(d.stop(10,false,true,0,10,60000));assertFalse(d.stop(60009,false,true,0,10,60000));assertTrue(d.stop(60010,false,true,0,10,60000));}
 @Test public void MovementGpsLossPoorAccuracyAndReplugResetStop(){DriveAutomation d=new DriveAutomation();assertFalse(d.stop(0,false,true,0,10,60000));assertFalse(d.stop(60000,false,false,0,10,60000));assertFalse(d.stop(70000,false,true,0,10,60000));assertFalse(d.stop(130000,false,true,0,100,60000));assertFalse(d.stop(140000,false,true,0,10,60000));assertFalse(d.stop(200000,false,true,1,10,60000));assertFalse(d.stop(210000,false,true,0,10,60000));assertFalse(d.stop(270000,true,true,0,10,60000));assertFalse(d.stop(280000,false,true,0,10,60000));}
}
