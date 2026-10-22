package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.hardware.*;
import android.hardware.camera2.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Motion evidence for future calibrated stabilization. It never processes camera pictures. */
final class MotionTelemetry implements SensorEventListener,AutoCloseable {
    interface Row {void write(BufferedWriter gyro,BufferedWriter frames)throws IOException;}
    private final ArrayBlockingQueue<Row> pending=new ArrayBlockingQueue<>(4096);
    private final Object queueLock=new Object();
    private final AtomicLong dropped=new AtomicLong();
    private final SensorManager sensors;
    private final Sensor gyro;
    private final File directory;
    private final HandlerThread callbacks=new HandlerThread("DashMotionSensors");
    private final Thread writer=new Thread(this::write,"DashMotionWriter");
    private final JSONObject metadata=new JSONObject();
    private volatile boolean accepting=true,failed;
    private long samples,firstNs,lastNs;
    private boolean registered;
    private long bytes;

    MotionTelemetry(Context context,File ride)throws Exception {
        directory=ride;sensors=context.getSystemService(SensorManager.class);
        gyro=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        metadata.put("schema",1).put("purpose","motion evidence; not yet used by the editor")
            .put("video_sync","camera timestamps require matching to encoded clip PTS and calibration; callback arrival is not an exposure timestamp")
            .put("gyro_units","rad/s; Android device axes, unaffected by display rotation")
            .put("gyro_clock","elapsedRealtimeNanos").put("requested_period_us",5000)
            .put("created_elapsed_ns",SystemClock.elapsedRealtimeNanos()).put("created_utc_ms",System.currentTimeMillis())
            .put("camera_modes",new JSONArray());
        if(gyro!=null)metadata.put("sensor",new JSONObject().put("name",gyro.getName()).put("vendor",gyro.getVendor())
            .put("version",gyro.getVersion()).put("min_delay_us",gyro.getMinDelay()).put("resolution",gyro.getResolution()));
        callbacks.start();
        if(gyro!=null)try{registered=sensors.registerListener(this,gyro,5000,new Handler(callbacks.getLooper()));}catch(RuntimeException error){registered=false;}
        metadata.put("gyro_registered",registered);
        writer.start();
    }
    synchronized void camera(String label,String id,CameraCharacteristics c,int width,int height,int rotation)throws JSONException {
        JSONObject entry=new JSONObject().put("camera",label).put("id",id).put("encoded_width",width).put("encoded_height",height)
            .put("display_rotation",rotation).put("sensor_orientation",c.get(CameraCharacteristics.SENSOR_ORIENTATION))
            .put("timestamp_source",c.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE));
        float[] intrinsic=c.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION),distortion=Build.VERSION.SDK_INT>=28?c.get(CameraCharacteristics.LENS_DISTORTION):null;
        if(intrinsic!=null)entry.put("intrinsics",array(intrinsic));if(distortion!=null)entry.put("distortion",array(distortion));
        android.graphics.Rect active=c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if(active!=null)entry.put("active_array",new JSONArray(new int[]{active.left,active.top,active.right,active.bottom}));
        metadata.getJSONArray("camera_modes").put(entry);
    }
    private static JSONArray array(float[] values)throws JSONException {JSONArray a=new JSONArray();for(float v:values)a.put(v);return a;}
    @Override public void onAccuracyChanged(Sensor sensor,int value){}
    @Override public void onSensorChanged(SensorEvent event){
        if(!accepting||failed||event.values.length<3)return;
        long time=event.timestamp,delivery=SystemClock.elapsedRealtimeNanos();float x=event.values[0],y=event.values[1],z=event.values[2];int quality=event.accuracy;
        offer((g,f)->{g.write(time+","+Float.toString(x)+","+y+","+z+","+quality+","+delivery+"\n");samples++;if(firstNs==0)firstNs=time;lastNs=time;});
    }
    void frame(String camera,File file,TotalCaptureResult result){
        if(!accepting||failed)return;
        Long sensor=result.get(CaptureResult.SENSOR_TIMESTAMP);if(sensor==null)return;
        long arrival=SystemClock.elapsedRealtimeNanos(),number=result.getFrameNumber();String name=file==null?"":file.getName();
        Long exposure=result.get(CaptureResult.SENSOR_EXPOSURE_TIME),duration=result.get(CaptureResult.SENSOR_FRAME_DURATION),skew=result.get(CaptureResult.SENSOR_ROLLING_SHUTTER_SKEW);
        Integer eis=result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE),ois=result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE);
        Float focus=result.get(CaptureResult.LENS_FOCUS_DISTANCE),focal=result.get(CaptureResult.LENS_FOCAL_LENGTH);
        android.graphics.Rect crop=result.get(CaptureResult.SCALER_CROP_REGION);String region=crop==null?"":crop.left+":"+crop.top+":"+crop.right+":"+crop.bottom;
        String physical=Build.VERSION.SDK_INT>=29?result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID):null;
        // Retain no camera result or image; callbacks never wait for storage.
        offer((g,f)->f.write(sensor+","+arrival+","+camera+","+name+","+number+","+value(exposure)+","+value(duration)+","+value(skew)+","+value(eis)+","+value(ois)+","+value(focus)+","+value(focal)+","+region+","+value(physical)+"\n"));
    }
    private static String value(Object value){return value==null?"":value.toString();}
    private void offer(Row row){synchronized(queueLock){if(accepting&&!failed&&!pending.offer(row))dropped.incrementAndGet();}}
    private void write(){
        try(BufferedWriter g=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory,"gyro.csv")),StandardCharsets.UTF_8),65536);
            BufferedWriter f=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory,"frames.csv")),StandardCharsets.UTF_8),65536)){
            g.write("elapsed_ns,gx_rad_s,gy_rad_s,gz_rad_s,accuracy,delivery_elapsed_ns\n");
            f.write("sensor_ns,callback_elapsed_ns,camera,recording_file_at_callback,frame_number,exposure_ns,frame_duration_ns,rolling_shutter_skew_ns,eis,ois,focus_diopters,focal_mm,crop_ltrb,physical_camera_id\n");
            summary(false);
            long flushed=SystemClock.elapsedRealtime(),summarized=flushed;
            while(accepting||!pending.isEmpty()){
                Row row=pending.poll(100,TimeUnit.MILLISECONDS);if(row!=null)row.write(g,f);
                if(SystemClock.elapsedRealtime()-flushed>=1000){g.flush();f.flush();flushed=SystemClock.elapsedRealtime();
                    bytes=new File(directory,"gyro.csv").length()+new File(directory,"frames.csv").length();
                    if(bytes>=256L*1024*1024)throw new IOException("Motion data reached the 256 MB per-ride limit");
                }
                if(SystemClock.elapsedRealtime()-summarized>=5000){summary(false);summarized=SystemClock.elapsedRealtime();}
            }
        }catch(Exception error){failed=true;pending.clear();if(sensors!=null)sensors.unregisterListener(this);callbacks.quitSafely();android.util.Log.w("DashMotion","Motion logging unavailable; original video continues",error);}
        finally{
            try{summary(!accepting&&!failed);}catch(Exception error){android.util.Log.w("DashMotion","Motion summary unavailable",error);}
        }
    }
    private synchronized void summary(boolean complete)throws Exception {
            metadata.put("session_closed",complete).put("data_limit_bytes",256L*1024*1024).put("dropped_rows",dropped.get()).put("gyro_samples",samples).put("first_gyro_ns",firstNs).put("last_gyro_ns",lastNs)
                .put("observed_hz",samples>1&&lastNs>firstNs?(samples-1)*1e9/(lastNs-firstNs):0).put("write_failed",failed);
            android.util.AtomicFile file=new android.util.AtomicFile(new File(directory,"motion.json"));FileOutputStream out=null;
            try{out=file.startWrite();out.write(metadata.toString(2).getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
            catch(Exception error){if(out!=null)file.failWrite(out);throw error;}
    }
    @Override public void close(){
        synchronized(queueLock){accepting=false;}
        if(sensors!=null)sensors.unregisterListener(this);callbacks.quitSafely();
        try{writer.join(2000);}catch(InterruptedException error){Thread.currentThread().interrupt();}
    }
}
