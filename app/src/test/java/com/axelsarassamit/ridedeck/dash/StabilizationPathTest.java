package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class StabilizationPathTest {
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
