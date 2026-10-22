package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class StabilizationPathTest {
    @Test public void lostTrackingFadesInsteadOfSnappingToIdentity(){
        long[] times=new long[90];float[][] path=new float[90][3];int[] segments=new int[90];
        for(int i=0;i<90;i++){times[i]=i*33333L;segments[i]=i<45?0:1;path[i][0]=(i<45?i:i-45)*2;}
        float[][] result=StabilizationPath.smooth(times,path,segments,3,360,640);
        assertEquals(0,result[44][0],0.0001);assertEquals(0,result[45][0],0.0001);
        assertTrue("Still smooth known motion away from the break",Math.abs(result[0][0])>10);
        for(int i=40;i<49;i++)assertTrue("No one-frame correction snap",Math.abs(result[i+1][0]-result[i][0])<6);
    }
    @Test public void accumulatedTranslationRotatesWithCamera() {
        float[] path=StabilizationPath.compose(new float[]{10,0,0},new float[]{0,0,(float)Math.PI/2});
        assertEquals(0,path[0],0.0001);assertEquals(10,path[1],0.0001);
        float[] restored=StabilizationPath.compose(path,new float[]{-10,0,-(float)Math.PI/2});
        assertEquals(0,restored[0],0.0001);assertEquals(0,restored[1],0.0001);
    }
    @Test public void correctionUsesInversePoseRatherThanTranslationSubtraction() {
        long[] times={0,33333,66666};
        float[][] pose={{20,0,0},{20,0,0.02f},{20,0,0}};
        float[][] correction=StabilizationPath.smooth(times,pose,new int[3],3,320,180);
        float angle=correction[1][2];
        // The corrected camera center must remain on the smoothed center (20,0).
        assertEquals(20,Math.cos(angle)*20+correction[1][0],0.0001);
        assertEquals(0,Math.sin(angle)*20+correction[1][1],0.0001);
    }
    @Test public void alternatingShakeIsReducedWithoutChangingSlowPan(){
        long[] times=new long[120];float[][] path=new float[120][3];int[] segments=new int[120];
        for(int i=0;i<120;i++){times[i]=i*33333L;path[i][0]=i*0.4f+(i%2==0?6:-6);}
        float[][] result=StabilizationPath.smooth(times,path,segments,2,320,180);
        for(int i=30;i<90;i++)assertEquals(i*0.4f,path[i][0]+result[i][0],0.6f);
    }
    @Test public void trackingBreakDoesNotBlendAcrossSceneCut(){
        long[] times={0,33333,66666,99999};float[][] path={{0,0,0},{0,0,0},{100,0,0},{100,0,0}};
        float[][] result=StabilizationPath.smooth(times,path,new int[]{0,0,1,1},3,320,180);
        for(float[] value:result)assertEquals(0,value[0],0.001f);
    }
    @Test public void cropBoundNeverExposesEdgeForLargeMotion(){
        long[] times={0,33333,66666};float[][] path={{0,0,0},{1000,-1000,2},{0,0,0}};
        float[][] result=StabilizationPath.smooth(times,path,new int[3],3,180,320);
        double zoom=StabilizationPath.zoom(3),angle=result[1][2];
        assertTrue((Math.abs(result[1][0])*zoom+90*Math.abs(Math.cos(angle))+160*Math.abs(Math.sin(angle)))<=90*zoom+0.001);
        assertTrue((Math.abs(result[1][1])*zoom+160*Math.abs(Math.cos(angle))+90*Math.abs(Math.sin(angle)))<=160*zoom+0.001);
    }
}
