package com.axelsarassamit.ridedeck.dash;

import android.graphics.Matrix;
import android.media.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.calib3d.Calib3d;
import androidx.media3.effect.MatrixTransformation;

/** Offline only: sequential low-resolution luma tracking, followed by GPU export of the original. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
class VideoStabilizer implements MatrixTransformation {
    final long[] times; final float[][] correction; final float zoom; final int analysisWidth,analysisHeight; boolean loggedTime;
    VideoStabilizer(long[] timestamps,float[][] values,int strength,int width,int height) {
        times=timestamps;correction=values;zoom=StabilizationPath.zoom(strength);analysisWidth=width;analysisHeight=height;
    }
    @Override public androidx.media3.common.util.Size configure(int width,int height) {
        android.util.Log.i("DashStabilizer","effect="+width+"x"+height+" analysis="+analysisWidth+"x"+analysisHeight+" firstUs="+times[0]);
        return new androidx.media3.common.util.Size(width,height);
    }
    @Override public Matrix getMatrix(long presentationTimeUs) {
        if(!loggedTime){loggedTime=true;android.util.Log.i("DashStabilizer","firstEffectUs="+presentationTimeUs+" path="+times[0]+".."+times[times.length-1]);}
        int i=Arrays.binarySearch(times,presentationTimeUs); if(i<0)i=Math.max(0,-i-2);
        int next=Math.min(i+1,times.length-1);float fraction=next==i?0:StabilizationPath.clamp((float)(presentationTimeUs-times[i])/(times[next]-times[i]),1);
        fraction=Math.max(0,fraction);
        float dx=correction[i][0]*(1-fraction)+correction[next][0]*fraction;
        float dy=correction[i][1]*(1-fraction)+correction[next][1]*fraction;
        float angle=correction[i][2]*(1-fraction)+correction[next][2]*fraction;
        float cos=(float)Math.cos(angle)*zoom,sin=(float)Math.sin(angle)*zoom;
        Matrix matrix=new Matrix();matrix.setValues(new float[]{cos,sin*analysisHeight/analysisWidth,2*dx/analysisWidth*zoom,
            -sin*analysisWidth/analysisHeight,cos,-2*dy/analysisHeight*zoom,0,0,1});return matrix;
    }
    static VideoStabilizer analyze(File file,long startUs,long endUs,int strength,BooleanSupplier cancelled,IntConsumer progress) throws Exception {
        return analyze(file,startUs,endUs,strength,false,cancelled,progress);
    }
    static VideoStabilizer analyze(File file,long startUs,long endUs,int strength,boolean hybrid,BooleanSupplier cancelled,IntConsumer progress) throws Exception {
        if(!OpenCVLoader.initLocal())throw new IOException("Stabilization engine unavailable");
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;Mat previous=null;
        List<Long> timestamps=new ArrayList<>();List<float[]> paths=new ArrayList<>(),deltas=new ArrayList<>();List<Integer> groups=new ArrayList<>();
        float x=0,y=0,angle=0;int segment=0,accepted=0,attempted=0,aw=0,ah=0;
        try {
            extractor.setDataSource(file.getPath());MediaFormat format=null;
            for(int i=0;i<extractor.getTrackCount();i++)if(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith("video/")){extractor.selectTrack(i);format=extractor.getTrackFormat(i);break;}
            if(format==null)throw new IOException("Video track unavailable");
            int rotation=format.containsKey(MediaFormat.KEY_ROTATION)?format.getInteger(MediaFormat.KEY_ROTATION):0;
            long duration=format.getLong(MediaFormat.KEY_DURATION);endUs=Math.min(endUs,duration);
            if(endUs-startUs>30L*60*1000000)throw new IOException("Stabilization supports clips up to 30 minutes; select a shorter social range");
            extractor.seekTo(startUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_ROTATION,0);
            decoder=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));decoder.configure(format,null,null,0);decoder.start();
            boolean inputDone=false,outputDone=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();int idle=0;
            while(!outputDone){
                if(cancelled.getAsBoolean())throw new IOException("Stabilization cancelled");
                if(!inputDone){int index=decoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer buffer=decoder.getInputBuffer(index);int size=extractor.readSampleData(buffer,0);long pts=extractor.getSampleTime();
                    if(size<0||pts>endUs+1000000){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true;}
                    else{decoder.queueInputBuffer(index,0,size,pts,0);extractor.advance();}}}
                int index=decoder.dequeueOutputBuffer(info,10000);
                if(index>=0){idle=0;outputDone=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    try{if(info.size>0&&info.presentationTimeUs>=startUs&&info.presentationTimeUs<endUs&&(timestamps.isEmpty()||info.presentationTimeUs-startUs>timestamps.get(timestamps.size()-1))){
                        Mat gray;try(Image image=decoder.getOutputImage(index)){if(image==null)throw new IOException("Phone decoder cannot provide stabilization frames");gray=luma(image,rotation);}
                        aw=gray.cols();ah=gray.rows();float[] delta=previous==null?null:motion(previous,gray);
                        if(previous!=null){attempted++;if(delta!=null){accepted++;float[] composed=StabilizationPath.compose(new float[]{x,y,angle},delta);x=composed[0];y=composed[1];angle=composed[2];}else{segment++;x=y=angle=0;}previous.release();}
                        previous=gray;timestamps.add(info.presentationTimeUs-startUs);paths.add(new float[]{x,y,angle});deltas.add(delta);groups.add(segment);
                        if(timestamps.size()>60000)throw new IOException("Too many frames; select a shorter social range");
                        if(timestamps.size()%30==0)progress.accept((int)(100*(info.presentationTimeUs-startUs)/Math.max(1,endUs-startUs)));
                    }}finally{decoder.releaseOutputBuffer(index,false);}
                }else if(index==MediaCodec.INFO_TRY_AGAIN_LATER&&++idle>500)throw new IOException("Video analysis timed out");
            }
            if(timestamps.size()<10||accepted<attempted*0.25)throw new IOException("Too few reliable road features for stabilization; export without smoothing");
            long[] times=new long[timestamps.size()];int[] segments=new int[times.length];for(int i=0;i<times.length;i++){times[i]=timestamps.get(i);segments[i]=groups.get(i);}
            if(hybrid){progress.accept(100);return HybridStabilizer.create(file,startUs,endUs,times,deltas,strength,aw,ah,rotation,cancelled);}
            float[][] correction=StabilizationPath.smooth(times,paths.toArray(new float[0][]),segments,strength,aw,ah);
            // Keep a local motion audit with the ride for evidence-based export diagnostics.
            // It contains no decoded pictures, audio or location; backups include this CSV.
            File report=null;
            try{
                report=File.createTempFile("stabilization-"+file.getName().replace(".mp4","")+"-",".csv",file.getParentFile());
                try(PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(report),java.nio.charset.StandardCharsets.UTF_8))){
                    writer.println("time_us,segment,path_x,path_y,path_angle,correction_x,correction_y,correction_angle,width,height,zoom");
                    for(int i=0;i<times.length;i++){float[] path=paths.get(i);writer.printf(Locale.ROOT,"%d,%d,%.6f,%.6f,%.8f,%.6f,%.6f,%.8f,%d,%d,%.3f%n",times[i],segments[i],path[0],path[1],path[2],correction[i][0],correction[i][1],correction[i][2],aw,ah,StabilizationPath.zoom(strength));}
                    if(writer.checkError())throw new IOException("Motion report unavailable");
                }
            }catch(Exception error){if(report!=null)report.delete();android.util.Log.w("DashStabilizer","Motion report unavailable");}
            progress.accept(100);return new VideoStabilizer(times,correction,strength,aw,ah);
        }finally{if(previous!=null)previous.release();if(decoder!=null){try{decoder.stop();}finally{decoder.release();}}extractor.release();}
    }
    private static Mat luma(Image image,int rotation){
        android.graphics.Rect crop=image.getCropRect();int width=640,height=Math.max(1,crop.height()*width/crop.width());
        Image.Plane plane=image.getPlanes()[0];ByteBuffer data=plane.getBuffer().duplicate();int base=data.position();
        Mat source;
        if(plane.getPixelStride()==1){
            data.position(base+crop.top*plane.getRowStride()+crop.left);
            source=new Mat(crop.height(),crop.width(),CvType.CV_8UC1,data.slice(),plane.getRowStride());
        }else{
            byte[] pixels=new byte[crop.width()*crop.height()];
            for(int y=0;y<crop.height();y++)for(int x=0;x<crop.width();x++)pixels[y*crop.width()+x]=data.get(base+(crop.top+y)*plane.getRowStride()+(crop.left+x)*plane.getPixelStride());
            source=new Mat(crop.height(),crop.width(),CvType.CV_8UC1);source.put(0,0,pixels);
        }
        Mat result=new Mat();try{Imgproc.resize(source,result,new Size(width,height),0,0,Imgproc.INTER_AREA);}finally{source.release();}
        if(rotation==90||rotation==180||rotation==270){Mat upright=new Mat();Core.rotate(result,upright,rotation==90?Core.ROTATE_90_CLOCKWISE:rotation==180?Core.ROTATE_180:Core.ROTATE_90_COUNTERCLOCKWISE);result.release();return upright;}
        return result;
    }
    private static float[] motion(Mat previous,Mat current){
        Mat mask=Mat.zeros(previous.rows(),previous.cols(),CvType.CV_8UC1);MatOfPoint corners=new MatOfPoint();MatOfPoint2f points=new MatOfPoint2f(),next=new MatOfPoint2f();MatOfByte status=new MatOfByte();MatOfFloat errors=new MatOfFloat();Mat affine=null,inliers=new Mat();
        try{
            // Exclude fixed cockpit, logo and common overlay panels. Track the distant road scene.
            Imgproc.rectangle(mask,new Point(previous.cols()*0.08,previous.rows()*0.06),new Point(previous.cols()*0.92,previous.rows()*0.65),new Scalar(255),-1);
            Imgproc.goodFeaturesToTrack(previous,corners,300,0.015,12,mask,3,false,0.04);
            if(corners.rows()<15)return null;points.fromArray(corners.toArray());Video.calcOpticalFlowPyrLK(previous,current,points,next,status,errors);
            Point[] from=points.toArray(),to=next.toArray();byte[] valid=status.toArray();float[] error=errors.toArray();List<Point> a=new ArrayList<>(),b=new ArrayList<>();
            for(int i=0;i<valid.length;i++)if(valid[i]!=0&&error[i]<25){a.add(from[i]);b.add(to[i]);}
            if(a.size()<12)return null;points.fromList(a);next.fromList(b);affine=Calib3d.estimateAffinePartial2D(points,next,inliers,Calib3d.RANSAC,3,1000,0.99,10);
            if(affine.empty()||Core.countNonZero(inliers)<12||Core.countNonZero(inliers)<a.size()*0.45)return null;
            double cos=affine.get(0,0)[0],sin=affine.get(1,0)[0],dx=affine.get(0,2)[0],dy=affine.get(1,2)[0],scale=Math.hypot(cos,sin),angle=Math.atan2(sin,cos);
            if(!Double.isFinite(scale)||!Double.isFinite(angle)||!Double.isFinite(dx)||!Double.isFinite(dy)||scale<0.93||scale>1.07||Math.abs(angle)>0.15||Math.hypot(dx,dy)>previous.cols()*0.15)return null;
            // Convert translation about the top-left corner to motion about the picture center.
            dx+=(cos-1)*previous.cols()/2-sin*previous.rows()/2;dy+=sin*previous.cols()/2+(cos-1)*previous.rows()/2;
            return new float[]{(float)dx,(float)dy,(float)angle};
        }finally{mask.release();corners.release();points.release();next.release();status.release();errors.release();inliers.release();if(affine!=null)affine.release();}
    }
}
