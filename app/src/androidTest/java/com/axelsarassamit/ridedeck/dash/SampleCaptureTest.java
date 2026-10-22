package com.axelsarassamit.ridedeck.dash;

import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class SampleCaptureTest {
    @Test public void oneSessionRawSampleStopsAndKeepsNormalPreferences()throws Exception {
        Assume.assumeTrue("Explicit connected-phone sample capture only","1".equals(InstrumentationRegistry.getArguments().getString("sampleCapture")));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertFalse(RecordingService.running||RecordingService.finalizing);
        android.content.SharedPreferences prefs=c.getSharedPreferences("dash-settings",Context.MODE_PRIVATE);Map<String,?> before=prefs.getAll();
        try{
            prefs.edit().putString("audio","off").putInt("autostart",0).putString("mode","dual").putInt("stabilization",1).commit();Map<String,?> normal=prefs.getAll();
            File ride;
            try(ActivityScenario<DashboardActivity> scenario=ActivityScenario.launch(new Intent(c,DashboardActivity.class).putExtra("sampleProfile","hybrid_sample").putExtra("sampleSeconds",10))){
                for(int i=0;i<150&&!RecordingService.capturing;i++)Thread.sleep(100);assertTrue("Sample must start: "+RecordingService.state,RecordingService.capturing);ride=RecordingService.rideDirectory;assertEquals("rear",RecordingService.captureMode);
                for(int i=0;i<200&&(RecordingService.running||RecordingService.finalizing);i++)Thread.sleep(100);assertFalse(RecordingService.running||RecordingService.finalizing);assertEquals("Sample saved",RecordingService.state);
                assertEquals("Session overrides must not replace normal preferences",normal,prefs.getAll());
            }
            JSONObject m=new JSONObject(new String(Files.readAllBytes(new File(ride,"motion.json").toPath()),StandardCharsets.UTF_8));assertEquals("hybrid_sample",m.getString("capture_profile"));assertEquals(0,m.getLong("dropped_rows"));assertTrue(m.getBoolean("session_closed"));assertTrue(m.getLong("accel_samples")>500);
            List<String> frames=Files.readAllLines(new File(ride,"frames.csv").toPath());assertTrue(frames.size()>250);
            for(int i=1;i<frames.size();i++){String[] v=frames.get(i).split(",",-1);assertEquals("0",v[8]);assertEquals("0",v[9]);assertTrue(v.length>=21);}
            for(File f:ride.listFiles((d,n)->n.endsWith(".mp4"))){assertTrue(ClipStore.finalized(f));assertTrue(ClipStore.protectedClip(f));assertTrue(f.getName().startsWith("rear-"));}
            MotionCaptureTest.copyEvidence(c,ride);
        }finally{
            if(RecordingService.running)c.startService(new Intent(c,RecordingService.class).setAction("STOP"));android.content.SharedPreferences.Editor e=prefs.edit().clear();
            for(Map.Entry<String,?> value:before.entrySet()){Object v=value.getValue();if(v instanceof Integer)e.putInt(value.getKey(),(Integer)v);else if(v instanceof String)e.putString(value.getKey(),(String)v);else if(v instanceof Boolean)e.putBoolean(value.getKey(),(Boolean)v);else if(v instanceof Long)e.putLong(value.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(value.getKey(),(Float)v);}e.commit();
        }
    }
}
