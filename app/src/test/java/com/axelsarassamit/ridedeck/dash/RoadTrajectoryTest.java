package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;

public class RoadTrajectoryTest {
    @Test public void horizontalCropPressureDoesNotDisableVerticalAndRollCorrection(){
        int n=240;long[] times=new long[n];float[][] pose=new float[n][3];
        for(int i=0;i<n;i++){times[i]=i*33333L;pose[i][0]=(float)Math.sin(i*.12)*200;pose[i][1]=i%2==0?2:-2;pose[i][2]=i%2==0?.01f:-.01f;}
        float[][] r=RoadTrajectory.smooth(times,pose,new int[n],3,360,640);
        for(int i=50;i<190;i++){double angle=r[i][2];assertEquals(0,pose[i][2]+angle,.0005);assertEquals(0,Math.sin(angle)*pose[i][0]+Math.cos(angle)*pose[i][1]+r[i][1],.05);}
    }
    @Test public void irregularTimingKeepsConstantPanAndLeanIncludingTrimEnds(){
        int n=180;long[] times=new long[n];float[][] pose=new float[n][3];
        for(int i=0;i<n;i++){if(i>0)times[i]=times[i-1]+(i%8==0?43333:33333);pose[i][0]=times[i]/1000000f*3;pose[i][2]=times[i]/1000000f*.02f;}
        float[][] result=RoadTrajectory.smooth(times,pose,new int[n],3,360,640);
        for(float[] row:result){assertEquals(0,row[0],.001);assertEquals(0,row[1],.001);assertEquals(0,row[2],.00001);}
    }
    @Test public void vibrationIsRemovedWithoutFlatteningSteadyCornerLean(){
        int n=240;long[] times=new long[n];float[][] pose=new float[n][3];
        for(int i=0;i<n;i++){times[i]=i*33333L;pose[i][0]=i*.1f+(i%2==0?5:-5);pose[i][1]=i%2==0?3:-3;pose[i][2]=i*.001f+(i%2==0?.02f:-.02f);}
        float[][] correction=RoadTrajectory.smooth(times,pose,new int[n],3,360,640);
        for(int i=40;i<200;i++){double a=correction[i][2],x=Math.cos(a)*pose[i][0]-Math.sin(a)*pose[i][1]+correction[i][0],y=Math.sin(a)*pose[i][0]+Math.cos(a)*pose[i][1]+correction[i][1];assertEquals(i*.1,x,.15);assertEquals(0,y,.15);assertEquals(i*.001,pose[i][2]+a,.0005);}
    }
    @Test public void cropNeverExposesEdgesAndBreaksFadeToIdentity(){
        java.util.Random random=new java.util.Random(73);int n=400;long[] t=new long[n];float[][] p=new float[n][3];int[] group=new int[n];
        for(int i=0;i<n;i++){t[i]=i*33333L;group[i]=i<200?0:1;p[i][0]=(float)random.nextGaussian()*100;p[i][1]=(float)random.nextGaussian()*100;p[i][2]=(float)random.nextGaussian()*.2f;}
        for(int[] size:new int[][]{{360,640},{640,360}})for(int strength=1;strength<=3;strength++){
            float[][] r=RoadTrajectory.smooth(t,p,group,strength,size[0],size[1]);double zoom=StabilizationPath.zoom(strength),w=size[0]/2.0,h=size[1]/2.0;
            for(float[] c:r){assertTrue(Float.isFinite(c[0])&&Float.isFinite(c[1])&&Float.isFinite(c[2]));double co=Math.abs(Math.cos(c[2])),si=Math.abs(Math.sin(c[2]));assertTrue(Math.abs(c[0])*zoom+w*co+h*si<=w*zoom+.001);assertTrue(Math.abs(c[1])*zoom+h*co+w*si<=h*zoom+.001);}
            for(int k=0;k<3;k++){assertEquals(0,r[199][k],.00001);assertEquals(0,r[200][k],.00001);}
        }
    }
    @Test public void singleAndTwoFrameSegmentsNeedNoCorrection(){
        float[][] r=RoadTrajectory.smooth(new long[]{0,33333,66666},new float[][]{{1,2,.1f},{10,8,.3f},{-20,5,.5f}},new int[]{0,1,1},3,360,640);
        for(float[] row:r)for(float v:row)assertEquals(0,v,.00001);
    }
}
