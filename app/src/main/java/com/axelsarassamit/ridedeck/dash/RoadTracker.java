package com.axelsarassamit.ridedeck.dash;

import java.util.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.calib3d.Calib3d;

/** Distributed background features; forward perspective scale is measured but never rendered as zoom. */
final class RoadTracker {
    static float[] motion(Mat previous,Mat current){
        MatOfPoint corners=new MatOfPoint();MatOfPoint2f points=new MatOfPoint2f(),next=new MatOfPoint2f(),back=new MatOfPoint2f();
        MatOfByte status=new MatOfByte(),backStatus=new MatOfByte();MatOfFloat errors=new MatOfFloat(),backErrors=new MatOfFloat();Mat affine=null,inliers=new Mat();
        int width=previous.cols(),height=previous.rows();
        try{
            List<Point> distributed=new ArrayList<>();
            // Equal quotas prevent a textured windscreen edge, dashboard or one vehicle from
            // consuming the full feature budget. The lower cockpit and outer mount edges are excluded.
            for(int row=0;row<3;row++)for(int col=0;col<4;col++){
                int x0=(int)(width*(.08+.84*col/4)),x1=(int)(width*(.08+.84*(col+1)/4));
                int y0=(int)(height*(.06+.60*row/3)),y1=(int)(height*(.06+.60*(row+1)/3));
                Mat tile=previous.submat(y0,y1,x0,x1);
                try{Imgproc.goodFeaturesToTrack(tile,corners,20,.015,10);for(Point p:corners.toArray())distributed.add(new Point(p.x+x0,p.y+y0));}finally{tile.release();}
            }
            if(distributed.size()<12)return null;
            points.fromList(distributed);Video.calcOpticalFlowPyrLK(previous,current,points,next,status,errors);Video.calcOpticalFlowPyrLK(current,previous,next,back,backStatus,backErrors);
            Point[] from=points.toArray(),to=next.toArray(),returned=back.toArray();byte[] valid=status.toArray(),reverse=backStatus.toArray();float[] error=errors.toArray();
            List<Point> a=new ArrayList<>(),b=new ArrayList<>();
            for(int i=0;i<valid.length;i++)if(valid[i]!=0&&reverse[i]!=0&&error[i]<25&&Math.hypot(from[i].x-returned[i].x,from[i].y-returned[i].y)<=1.5){a.add(from[i]);b.add(to[i]);}
            if(a.size()<12)return null;points.fromList(a);next.fromList(b);
            affine=Calib3d.estimateAffinePartial2D(points,next,inliers,Calib3d.RANSAC,2.5,1500,.995,10);
            if(affine.empty())return null;byte[] flags=new byte[(int)inliers.total()];inliers.get(0,0,flags);
            List<Point> goodA=new ArrayList<>(),goodB=new ArrayList<>();
            for(int i=0;i<flags.length;i++)if(flags[i]!=0){goodA.add(a.get(i));goodB.add(b.get(i));}
            if(goodA.size()<12||goodA.size()<a.size()*.40)return null;
            Set<Integer> cells=new HashSet<>();for(Point p:goodA)cells.add(Math.min(2,(int)((p.y/height-.06)/.20))*4+Math.min(3,(int)((p.x/width-.08)/.21)));
            if(cells.size()<3)return null;
            // A moving road expands in perspective. Keep that scale in the tracking fit so it
            // does not contaminate rotation; only rigid center motion is corrected on export.
            double cos=affine.get(0,0)[0],sin=affine.get(1,0)[0],scale=Math.hypot(cos,sin),angle=Math.atan2(sin,cos);
            double dx=affine.get(0,2)[0]+(cos-1)*width/2-sin*height/2,dy=affine.get(1,2)[0]+sin*width/2+(cos-1)*height/2;
            if(!Double.isFinite(dx)||!Double.isFinite(dy)||scale<.93||scale>1.07||Math.abs(angle)>.15||Math.hypot(dx,dy)>width*.15)return null;
            return new float[]{(float)dx,(float)dy,(float)angle};
        }finally{corners.release();points.release();next.release();back.release();status.release();backStatus.release();errors.release();backErrors.release();inliers.release();if(affine!=null)affine.release();}
    }
}
