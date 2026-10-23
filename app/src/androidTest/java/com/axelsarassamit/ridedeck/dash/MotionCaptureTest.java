package com.axelsarassamit.ridedeck.dash;

import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Opt-in real dual-camera capture. Test output remains available for independent timing checks. */
public class MotionCaptureTest {
    @Test public void dualCaptureSavesMonotonicMotionAndCameraRows()throws Exception {
        Assume.assumeTrue("Explicit connected-phone capture only","1".equals(InstrumentationRegistry.getArguments().getString("motionCapture")));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertFalse(RecordingService.running||RecordingService.finalizing);
        SharedPreferences prefs=c.getSharedPreferences("dash-settings",Context.MODE_PRIVATE);Map<String,?> before=prefs.getAll();
        File ride=null;
        try{
            prefs.edit().putInt("motion_data",1).putInt("autostart",0).putString("audio","off").putString("mode","dual").putInt("rearPriority",1).commit();
            try(ActivityScenario<DashboardActivity> scenario=ActivityScenario.launch(DashboardActivity.class)){
                scenario.onActivity(a->{a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);a.mode="dual";a.request(true);});
                for(int i=0;i<150&&!RecordingService.capturing;i++)Thread.sleep(100);
                assertTrue("Both cameras must start: "+RecordingService.state,RecordingService.capturing);ride=RecordingService.rideDirectory;
                Thread.sleep(Math.max(30,Math.min(180,Integer.parseInt(InstrumentationRegistry.getArguments().getString("captureSeconds","30"))))*1000L);assertTrue("Capture must continue with logging: "+RecordingService.state,RecordingService.capturing);
                scenario.onActivity(a->a.command("STOP"));for(int i=0;i<150&&(RecordingService.running||RecordingService.finalizing);i++)Thread.sleep(100);
                assertFalse(RecordingService.running||RecordingService.finalizing);
            }
            JSONObject summary=new JSONObject(new String(Files.readAllBytes(new File(ride,"motion.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertTrue(summary.getBoolean("gyro_registered"));assertTrue(summary.getBoolean("session_closed"));assertFalse(summary.getBoolean("write_failed"));assertEquals(0,summary.getLong("dropped_rows"));assertTrue(summary.getLong("gyro_samples")>2000);assertTrue(summary.getDouble("observed_hz")>80);assertEquals(2,summary.getJSONArray("camera_modes").length());
            List<String> gyro=Files.readAllLines(new File(ride,"gyro.csv").toPath());long last=0;
            for(int i=1;i<gyro.size();i++){long time=Long.parseLong(gyro.get(i).split(",")[0]);assertTrue(time>last);last=time;}
            Map<String,Long> lastFrame=new HashMap<>();Map<String,Integer> counts=new HashMap<>();
            List<String> frameRows=Files.readAllLines(new File(ride,"frames.csv").toPath());
            for(String row:frameRows.subList(1,frameRows.size())){
                String[] v=row.split(",",-1);assertTrue(v.length>=14);long time=Long.parseLong(v[0]);assertTrue(time>lastFrame.getOrDefault(v[2],0L));lastFrame.put(v[2],time);counts.put(v[2],counts.getOrDefault(v[2],0)+1);
            }
            for(String row:frameRows.subList(1,frameRows.size())){String[] v=row.split(",",-1);if("front".equals(v[2])){assertEquals("Rear priority removes front EIS workload","0",v[8]);assertEquals("Rear priority removes front OIS workload","0",v[9]);}}
            assertTrue(counts.getOrDefault("rear",0)>750);assertTrue(counts.getOrDefault("front",0)>750);
            copyEvidence(c,ride);
            System.out.println("Motion capture ride: "+ride.getName());
        }finally{
            if(RecordingService.running)c.startService(new Intent(c,RecordingService.class).setAction("STOP"));
            SharedPreferences.Editor restore=prefs.edit().clear();for(Map.Entry<String,?> e:before.entrySet()){Object v=e.getValue();if(v instanceof Integer)restore.putInt(e.getKey(),(Integer)v);else if(v instanceof String)restore.putString(e.getKey(),(String)v);else if(v instanceof Boolean)restore.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Long)restore.putLong(e.getKey(),(Long)v);else if(v instanceof Float)restore.putFloat(e.getKey(),(Float)v);}restore.commit();
        }
    }
    @Test public void copyLatestFinishedMotionEvidence()throws Exception {
        Assume.assumeTrue("Explicit local diagnostic copy only","1".equals(InstrumentationRegistry.getArguments().getString("motionEvidence")));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertFalse(RecordingService.running||RecordingService.finalizing);
        File[] rides=new ClipStore(c).root().listFiles(f->new File(f,"motion.json").isFile());assertNotNull(rides);assertTrue(rides.length>0);
        Arrays.sort(rides,Comparator.comparingLong((File f)->new File(f,"motion.json").lastModified()).reversed());copyEvidence(c,rides[0]);
    }
    static void copyEvidence(Context c,File ride)throws Exception {
        File out=c.getExternalFilesDir("rear-audit");assertNotNull(out);assertTrue(out.isDirectory()||out.mkdirs());
        for(String name:new String[]{"gyro.csv","accel.csv","frames.csv","motion.json","capture.csv"}){
            if(!new File(ride,name).isFile())continue;
            File dest=new File(out,"motion-readable-"+name);
            try(InputStream in=new FileInputStream(new File(ride,name));OutputStream writer=new FileOutputStream(dest)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)writer.write(b,0,n);}
        }
    }
}
