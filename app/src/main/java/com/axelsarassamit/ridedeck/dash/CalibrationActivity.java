package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;

/** A deliberate, one-session capture workflow; normal recording preferences are untouched. */
public final class CalibrationActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this);
        super.onCreate(state);
        LinearLayout page=DashUi.page(this);
        ScrollView scroll=new ScrollView(this);
        scroll.addView(page);
        setContentView(scroll);
        DashUi.text(this,page,"Review calibration samples",26);
        DashUi.text(this,page,"Use Yamaha Bluetooth Lab to start a new sample. Dash keeps the protected recordings and their motion evidence here.",16);
        DashUi.text(this,page,"Recent camera and motion evidence",21);
        File[] rides=new ClipStore(this).root().listFiles(f->new File(f,"motion.json").isFile());
        if(rides!=null){Arrays.sort(rides,Comparator.comparingLong((File f)->f.lastModified()).reversed());int shown=0;
            for(File ride:rides){if(shown>=6)break;File meta=new File(ride,"motion.json");try{if(meta.length()>65536)continue;JSONObject j=new JSONObject(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8));String profile=j.optString("capture_profile","normal");shown++;
                String label=(profile.equals("normal")?"Ride recording":profile.equals("hybrid_sample")?"Hybrid sample":"Native reference")+" · "+new java.text.SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date(j.optLong("created_utc_ms",ride.lastModified())));
                DashUi.text(this,page,label,18);DashUi.text(this,page,String.format(Locale.ROOT,"%.0f gyro samples/s · %d samples · %d lost rows · %s",j.optDouble("observed_hz"),j.optLong("gyro_samples"),j.optLong("dropped_rows"),j.optBoolean("session_closed")&&!j.optBoolean("write_failed")?"saved":"incomplete"),15);
                DashUi.button(this,page,"Save workshop evidence",v->RideDataExport.save(this,ride,true));
                if(profile.equals("normal"))continue;
                File[] clips=ride.listFiles(f->f.getName().startsWith("rear-")&&f.getName().endsWith(".mp4")&&ClipStore.finalized(f));if(clips!=null&&clips.length>0){Arrays.sort(clips,Comparator.comparing(File::getName));List<String> paths=new ArrayList<>();for(File clip:clips)if(paths.size()<SocialProject.MAX_CLIPS)paths.add(clip.getPath());DashUi.button(this,page,"Edit this sample",v->startActivity(new Intent(this,SocialStudioActivity.class).putExtra("project",SocialProject.selection(paths)).putExtra("labSampleReview",true)));}
            }catch(Exception ignored){/* Incomplete metadata remains intact for diagnostics. */}}
        }
        DashUi.button(this,page,"Back",v->finish());
    }
}
