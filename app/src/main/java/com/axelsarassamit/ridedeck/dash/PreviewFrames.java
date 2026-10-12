package com.axelsarassamit.ridedeck.dash;
import android.graphics.*;
import android.media.*;
import java.io.*;
import java.nio.*;
final class PreviewFrames {
 static volatile Bitmap rear,front;
 static volatile int rotation;
 static volatile int interval=200;
 static volatile boolean enabled;
 static volatile long companionPreviewUntil, rearAt, frontAt;
 static volatile String message="Tap Camera view to align your cameras";
 private static android.os.Handler conversion;
 private static synchronized android.os.Handler conversionHandler(){
  if(conversion==null){android.os.HandlerThread thread=new android.os.HandlerThread("DashPreviewConversion");thread.start();conversion=new android.os.Handler(thread.getLooper());}
  return conversion;
 }
 static ImageReader reader(String label,int sensor,android.os.Handler handler){
  ImageReader reader=ImageReader.newInstance(640,360,ImageFormat.YUV_420_888,2);
  final long[] last={0};
  reader.setOnImageAvailableListener(r->{try(Image image=r.acquireLatestImage()){
   if(image==null || (!enabled && android.os.SystemClock.elapsedRealtime()>=companionPreviewUntil) || android.os.SystemClock.elapsedRealtime()-last[0]<interval)return;
   last[0]=android.os.SystemClock.elapsedRealtime();
   int w=image.getWidth(),h=image.getHeight();byte[] nv21=new byte[w*h*3/2];Image.Plane[] planes=image.getPlanes();
   for(int n=0;n<3;n++){ByteBuffer b=planes[n].getBuffer();int rows=n==0?h:h/2,cols=n==0?w:w/2;
    for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){int offset=y*planes[n].getRowStride()+x*planes[n].getPixelStride();
     nv21[n==0?y*w+x:w*h+y*w+x*2+(n==1?1:0)]=b.get(offset);}}
   ByteArrayOutputStream out=new ByteArrayOutputStream();new YuvImage(nv21,ImageFormat.NV21,w,h,null).compressToJpeg(new Rect(0,0,w,h),82,out);
   byte[] data=out.toByteArray();Bitmap raw=BitmapFactory.decodeByteArray(data,0,data.length);Matrix m=new Matrix();
   m.postRotate("front".equals(label)?(sensor+rotation*90)%360:(sensor-rotation*90+360)%360);
   Bitmap result=Bitmap.createBitmap(raw,0,0,w,h,m,true);if(result!=raw)raw.recycle();
   if("rear".equals(label)){rear=result;rearAt=android.os.SystemClock.elapsedRealtime();}else {front=result;frontAt=android.os.SystemClock.elapsedRealtime();}message="Live camera view";
  }catch(Exception e){message="Camera view unavailable";}},conversionHandler());return reader;
 }
}
