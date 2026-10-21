package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class SocialTimelineTest {
    @Test public void differentCameraRolloversAreJoinedByTime() {
        List<SocialTimeline.Slice> r=SocialTimeline.intersect(1500,6000,Arrays.asList(
            new SocialTimeline.Segment("front-0008",0,3000),new SocialTimeline.Segment("front-0009",3000,9000)),0);
        assertEquals(2,r.size());assertEquals(1500,r.get(0).sourceStartUs);assertEquals(1500,r.get(0).durationUs);
        assertEquals(0,r.get(1).sourceStartUs);assertEquals(1500,r.get(1).outputStartUs);assertEquals(4500,r.get(1).durationUs);
    }
    @Test public void missingFootageIsNotFrozenOrStretched() {
        List<SocialTimeline.Slice> r=SocialTimeline.intersect(0,10000,Arrays.asList(new SocialTimeline.Segment("a",1000,2000),new SocialTimeline.Segment("b",7000,1000)),0);
        assertFalse(SocialTimeline.visible(r,999));assertTrue(SocialTimeline.visible(r,1000));assertFalse(SocialTimeline.visible(r,3000));
        assertFalse(SocialTimeline.visible(r,6000));assertTrue(SocialTimeline.visible(r,7999));assertFalse(SocialTimeline.visible(r,8000));
    }
    @Test public void manualDelayMovesOnlyFrontCoverage() {
        List<SocialTimeline.Slice> r=SocialTimeline.intersect(0,5000,Collections.singletonList(new SocialTimeline.Segment("a",0,5000)),1000);
        assertEquals(1000,r.get(0).outputStartUs);assertEquals(0,r.get(0).sourceStartUs);assertEquals(4000,r.get(0).durationUs);
        r=SocialTimeline.intersect(0,5000,Collections.singletonList(new SocialTimeline.Segment("a",0,5000)),-1000);
        assertEquals(0,r.get(0).outputStartUs);assertEquals(1000,r.get(0).sourceStartUs);assertEquals(4000,r.get(0).durationUs);
    }
    @Test public void overlapCannotDuplicateOutputTime() {
        List<SocialTimeline.Slice> r=SocialTimeline.intersect(0,10000,Arrays.asList(new SocialTimeline.Segment("b",3000,7000),new SocialTimeline.Segment("a",0,5000)),0);
        assertEquals("a",r.get(0).path);assertEquals(5000,r.get(1).outputStartUs);assertEquals(2000,r.get(1).sourceStartUs);
    }
    @Test public void unrelatedAndAdjacentSessionsDoNotMatch() {
        assertTrue(SocialTimeline.intersect(5000,5000,Arrays.asList(new SocialTimeline.Segment("a",0,5000),new SocialTimeline.Segment("b",10000,2000)),0).isEmpty());
    }
}
