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
  return reader(label,sensor,handler,false);
 }
 static ImageReader reader(String label,int sensor,android.os.Handler handler,boolean cameraThrottled){
  ImageReader reader=ImageReader.newInstance(640,360,ImageFormat.YUV_420_888,4);
  final long[] last={0};
  final byte[] nv21=new byte[640*360*3/2];final int[] pixels=new int[640*360];
  reader.setOnImageAvailableListener(r->{try(Image image=r.acquireLatestImage()){
   if(image==null || (!enabled && android.os.SystemClock.elapsedRealtime()>=companionPreviewUntil) || (!cameraThrottled&&android.os.SystemClock.elapsedRealtime()-last[0]<interval))return;
   last[0]=android.os.SystemClock.elapsedRealtime();
   int w=image.getWidth(),h=image.getHeight();Image.Plane[] planes=image.getPlanes();
   for(int n=0;n<3;n++){ByteBuffer b=planes[n].getBuffer();int rows=n==0?h:h/2,cols=n==0?w:w/2;
    for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){int offset=y*planes[n].getRowStride()+x*planes[n].getPixelStride();
     nv21[n==0?y*w+x:w*h+y*w+x*2+(n==1?1:0)]=b.get(offset);}}
   int degrees="front".equals(label)?(sensor+rotation*90)%360:(sensor-rotation*90+360)%360;
   PreviewPixels.convert(nv21,pixels,w,h,degrees);
   Bitmap result=Bitmap.createBitmap(pixels,degrees==90||degrees==270?h:w,degrees==90||degrees==270?w:h,Bitmap.Config.ARGB_8888);
   if("rear".equals(label)){rear=result;rearAt=android.os.SystemClock.elapsedRealtime();}else {front=result;frontAt=android.os.SystemClock.elapsedRealtime();}message="Live camera view";
  }catch(Exception e){message="Camera view unavailable";}},conversionHandler());return reader;
 }
}
