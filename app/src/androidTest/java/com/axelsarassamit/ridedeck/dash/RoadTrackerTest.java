package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class RoadTrackerTest {
    @Test public void distributedRoadTracksRotationAndIgnoresFixedCockpit(){
        assertTrue(OpenCVLoader.initLocal());Mat scene=new Mat(640,360,CvType.CV_8UC1,new Scalar(35)),current=new Mat();Mat warp=null;
        try{
            java.util.Random random=new java.util.Random(71);
            for(int i=0;i<1200;i++)Imgproc.circle(scene,new Point(random.nextInt(360),random.nextInt(640)),2+random.nextInt(3),new Scalar(70+random.nextInt(185)),-1);
            double angle=.015;warp=Imgproc.getRotationMatrix2D(new Point(180,320),-Math.toDegrees(angle),1);warp.put(0,2,warp.get(0,2)[0]+4);warp.put(1,2,warp.get(1,2)[0]-2);Imgproc.warpAffine(scene,current,warp,scene.size());
            Mat cockpit=scene.submat(430,640,0,360),target=current.submat(430,640,0,360);try{cockpit.copyTo(target);}finally{cockpit.release();target.release();}
            float[] motion=RoadTracker.motion(scene,current);assertNotNull(motion);assertEquals(4,motion[0],.3);assertEquals(-2,motion[1],.3);assertEquals(angle,motion[2],.001);
        }finally{scene.release();current.release();if(warp!=null)warp.release();}
    }
    @Test public void oneTexturedObjectCannotDefineWholeRoadMotion(){
        assertTrue(OpenCVLoader.initLocal());Mat scene=new Mat(640,360,CvType.CV_8UC1,new Scalar(35));
        try{java.util.Random random=new java.util.Random(91);for(int i=0;i<120;i++)Imgproc.circle(scene,new Point(105+random.nextInt(50),180+random.nextInt(90)),2,new Scalar(70+random.nextInt(180)),-1);assertNull(RoadTracker.motion(scene,scene));}finally{scene.release();}
    }
    @Test public void texturelessNightFrameIsRejectedRatherThanInventingMotion(){
        assertTrue(OpenCVLoader.initLocal());Mat blank=new Mat(640,360,CvType.CV_8UC1,new Scalar(15));try{assertNull(RoadTracker.motion(blank,blank));}finally{blank.release();}
    }
}
