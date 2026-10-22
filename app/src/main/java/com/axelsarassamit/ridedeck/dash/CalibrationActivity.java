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
    int duration=120;TextView durationLabel;
    @Override public void onCreate(Bundle b){RideAppearance.prepare(this);super.onCreate(b);if(b!=null)duration=b.getInt("duration",120);show();}
    void show(){LinearLayout page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        DashUi.text(this,page,"Calibration & samples",26);
        DashUi.text(this,page,"Normal recording keeps your usual native stabilization. These separate rear-camera sessions save motion and camera evidence and protect their clips from loop deletion. They use your selected rear lens and resolution. Recording power and RideDeck background rules still apply.",16);
        DashUi.text(this,page,"1 · Parked calibration",21);
        DashUi.text(this,page,"While safely parked, film a bright, detailed distant scene. Gently vary pitch, yaw and roll with irregular movement on all three axes for 20–30 seconds. Avoid moving vehicles filling the view. Use locked road focus. Never adjust the phone while riding.",16);
        DashUi.button(this,page,"Record a 30-second calibration sample",v->start("hybrid_sample",30));
        DashUi.text(this,page,"2 · Matched ride samples",21);
        DashUi.text(this,page,"Compare the same route and mount with a native reference and a separate hybrid sample. The hybrid sample deliberately turns native EIS/OIS off for this session, so its original may look shakier. It is for experimental offline correction. Stop before changing sample type.",16);
        durationLabel=DashUi.text(this,page,"Sample length: "+durationText(),17);
        DashUi.button(this,page,"Choose sample length",v->new AlertDialog.Builder(this).setTitle("Sample length").setItems(new String[]{"30 seconds","2 minutes","5 minutes","Until I press Stop"},(d,n)->{duration=new int[]{30,120,300,0}[n];durationLabel.setText("Sample length: "+durationText());}).show());
        DashUi.button(this,page,"Record native reference",v->start("native_sample",duration));
        DashUi.button(this,page,"Record hybrid sample · native stabilization off",v->new AlertDialog.Builder(this).setTitle("Hybrid sample").setMessage("This rear-only sample uses motion logging, locked distant focus and native stabilization off. Your normal recording settings stay unchanged. Protected samples count toward the storage limit.").setNegativeButton("Cancel",null).setPositiveButton("Start sample",(d,w)->start("hybrid_sample",duration)).show());
        DashUi.text(this,page,"3 · Review and export",21);
        DashUi.text(this,page,"Open a hybrid sample in the editor and choose Stabilize > Hybrid lab. The selection is checked for timing agreement and varied movement before correction is allowed. Older clips and native-stabilized recordings use Image smoothing. Hybrid lab is a small-angle prototype, not yet validated on an outdoor ride or a complete lens-distortion calibration.",16);
        DashUi.button(this,page,"Open recordings",v->startActivity(new Intent(this,ClipsActivity.class)));
        DashUi.text(this,page,"Recent sample evidence",21);
        File[] rides=new ClipStore(this).root().listFiles(f->new File(f,"motion.json").isFile());
        if(rides!=null){Arrays.sort(rides,Comparator.comparingLong((File f)->f.lastModified()).reversed());int shown=0;
            for(File ride:rides){if(shown>=6)break;File meta=new File(ride,"motion.json");try{if(meta.length()>65536)continue;JSONObject j=new JSONObject(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8));String profile=j.optString("capture_profile","normal");if(profile.equals("normal"))continue;shown++;
                String label=(profile.equals("hybrid_sample")?"Hybrid sample":"Native reference")+" · "+new java.text.SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date(j.optLong("created_utc_ms",ride.lastModified())));
                DashUi.text(this,page,label,18);DashUi.text(this,page,String.format(Locale.ROOT,"%.0f gyro samples/s · %d samples · %d lost rows · %s",j.optDouble("observed_hz"),j.optLong("gyro_samples"),j.optLong("dropped_rows"),j.optBoolean("session_closed")&&!j.optBoolean("write_failed")?"saved":"incomplete"),15);
                File[] clips=ride.listFiles(f->f.getName().startsWith("rear-")&&f.getName().endsWith(".mp4")&&ClipStore.finalized(f));if(clips!=null&&clips.length>0){Arrays.sort(clips,Comparator.comparing(File::getName));List<String> paths=new ArrayList<>();for(File clip:clips)if(paths.size()<SocialProject.MAX_CLIPS)paths.add(clip.getPath());DashUi.button(this,page,"Edit this sample",v->startActivity(new Intent(this,SocialStudioActivity.class).putExtra("project",SocialProject.selection(paths))));}
            }catch(Exception ignored){/* Incomplete metadata remains intact for diagnostics. */}}
        }
        DashUi.button(this,page,"Back",v->finish());
    }
    String durationText(){return duration==0?"until Stop":duration<60?duration+" seconds":duration/60+" minutes";}
    void start(String profile,int seconds){if(RecordingService.running||RecordingService.finalizing){Toast.makeText(this,"Stop the current recording first",Toast.LENGTH_LONG).show();return;}
        startActivity(new Intent(this,DashboardActivity.class).putExtra("sampleProfile",profile).putExtra("sampleSeconds",seconds));finish();}
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("duration",duration);super.onSaveInstanceState(b);}
}
