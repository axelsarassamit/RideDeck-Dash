package com.axelsarassamit.ridedeck.dash;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.camera2.*;
import android.media.ImageReader;
import android.os.*;
import android.view.Surface;
import java.util.*;
final class AlignmentPreview {
 final Context context;final HandlerThread thread=new HandlerThread("DashAlignment");Handler handler;
 final List<CameraDevice> cameras=new ArrayList<>();final List<ImageReader> readers=new ArrayList<>();boolean closed;
 AlignmentPreview(Context c){context=c;thread.start();handler=new Handler(thread.getLooper());}
 void start(String mode){handler.post(()->{try{
  if(context.checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
  CameraManager manager=context.getSystemService(CameraManager.class);List<String> ids=new ArrayList<>();
  for(String id:manager.getCameraIdList()){Integer facing=manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
   boolean rear=Objects.equals(facing,CameraCharacteristics.LENS_FACING_BACK),front=Objects.equals(facing,CameraCharacteristics.LENS_FACING_FRONT);
   if((rear&&!"front".equals(mode)||front&&!"rear".equals(mode))&&!ids.stream().anyMatch(old->{try{return Objects.equals(manager.getCameraCharacteristics(old).get(CameraCharacteristics.LENS_FACING),facing);}catch(Exception e){return false;}}))ids.add(id);}
  if(ids.size()>1 && (Build.VERSION.SDK_INT<30 || manager.getConcurrentCameraIds().stream().noneMatch(s->s.containsAll(ids))))throw new Exception("Dual view unavailable; choose one camera");
  for(String id:ids){CameraCharacteristics c=manager.getCameraCharacteristics(id);String label=Objects.equals(c.get(CameraCharacteristics.LENS_FACING),CameraCharacteristics.LENS_FACING_BACK)?"rear":"front";
   ImageReader reader=PreviewFrames.reader(label,c.get(CameraCharacteristics.SENSOR_ORIENTATION),handler);readers.add(reader);
   manager.openCamera(id,new CameraDevice.StateCallback(){
    public void onOpened(CameraDevice d){if(closed){d.close();return;}cameras.add(d);if(cameras.size()==ids.size())for(int i=0;i<cameras.size();i++)configure(cameras.get(i),readers.get(ids.indexOf(cameras.get(i).getId())).getSurface());}
    public void onDisconnected(CameraDevice d){d.close();PreviewFrames.message="Camera disconnected";}
    public void onError(CameraDevice d,int e){d.close();PreviewFrames.message="Camera view error "+e;}
   },handler);}
 }catch(Exception e){PreviewFrames.message=e.getMessage();}});}
 void configure(CameraDevice camera,Surface surface){try{camera.createCaptureSession(Collections.singletonList(surface),new CameraCaptureSession.StateCallback(){
  public void onConfigured(CameraCaptureSession session){if(closed){session.close();return;}try{CaptureRequest.Builder r=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);r.addTarget(surface);CameraCharacteristics c=context.getSystemService(CameraManager.class).getCameraCharacteristics(camera.getId());String label=Objects.equals(c.get(CameraCharacteristics.LENS_FACING),CameraCharacteristics.LENS_FACING_BACK)?"rear":"front";CameraFocus.apply(r,c,new DashSettings(context),label);session.setRepeatingRequest(r.build(),null,handler);}catch(Exception e){PreviewFrames.message="Camera view unavailable";}}
  public void onConfigureFailed(CameraCaptureSession s){s.close();PreviewFrames.message="Camera view configuration unavailable";}
 },handler);}catch(Exception e){PreviewFrames.message="Camera view unavailable";}}
 void close(Runnable after){closed=true;handler.post(()->{for(CameraDevice c:cameras)c.close();for(ImageReader r:readers)r.close();thread.quitSafely();if(after!=null)new Handler(Looper.getMainLooper()).postDelayed(after,300);});}
}
