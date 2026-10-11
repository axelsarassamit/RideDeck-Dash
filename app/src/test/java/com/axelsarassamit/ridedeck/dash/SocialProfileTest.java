package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;import static org.junit.Assert.*;
public class SocialProfileTest {
 @Test public void platformsUseExpectedShapes(){for(int i:new int[]{0,1,2,4,5})assertArrayEquals(new int[]{1080,1920},SocialProfile.dimensions(SocialProfile.ratio(i),1080));for(int i:new int[]{3,6})assertArrayEquals(new int[]{1080,1350},SocialProfile.dimensions(SocialProfile.ratio(i),1080));assertArrayEquals(new int[]{1920,1080},SocialProfile.dimensions(SocialProfile.ratio(7),1080));assertArrayEquals(new int[]{720,720},SocialProfile.dimensions(2,720));}
 @Test public void trimClampsToEndAndKeepsOffset(){assertArrayEquals(new long[]{5000,12000},SocialProfile.range(12000,5,15));assertArrayEquals(new long[]{10000,25000},SocialProfile.range(60000,10,15));assertArrayEquals(new long[]{5000,60000},SocialProfile.range(60000,5,0));}
 @Test(expected=IllegalArgumentException.class) public void trimCannotStartPastClip(){SocialProfile.range(12000,12,15);}
 @Test(expected=IllegalArgumentException.class) public void invalidFormatBlocked(){SocialProfile.dimensions(99,1080);}
 @Test public void cleanAndTouringKeepPrivacyIntent(){for(String key:SocialProfile.overlayKeys())assertFalse(SocialProfile.overlay(0,key));assertTrue(SocialProfile.overlay(2,"route"));assertFalse(SocialProfile.overlay(1,"route"));assertFalse(SocialProfile.overlay(2,"battery"));}
}
