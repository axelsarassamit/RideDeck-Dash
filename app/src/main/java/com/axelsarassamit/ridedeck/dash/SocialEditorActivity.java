package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.util.*;

/** Non-destructive, single-clip editor. Effects are shared with the export pipeline. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class SocialEditorActivity extends Activity {
    File clip; long duration; DashSettings settings; ExoPlayer player; PlayerView preview;
    LinearLayout controls; TextView status, rangeLabel; SeekBar start, end;
    VideoStabilizer stabilization; volatile boolean cancelled; boolean analyzing;
    long[] appliedRange; int generation;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            clip = new File(getIntent().getStringExtra("clip"));
            if (!clip.getCanonicalPath().startsWith(new ClipStore(this).root().getCanonicalPath()+File.separator)
                    || !ClipStore.finalized(clip)) throw new IOException("Select a finalized recording");
            MediaMetadataRetriever reader = new MediaMetadataRetriever();
            try {
                reader.setDataSource(clip.getPath());
                duration = Long.parseLong(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            } finally { reader.release(); }
            if (duration < 1000) throw new IOException("Clip is too short to edit");
            layout();
        } catch (Exception error) {
            LinearLayout page = DashUi.page(this); setContentView(page);
            DashUi.text(this,page,"Editor unavailable: "+error.getMessage(),18);
            DashUi.button(this,page,"Back",v->finish());
        }
    }
    void layout() {
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this); root.setBackgroundColor(DashUi.INK);
        root.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(b.left,b.top,b.right,b.bottom);}
            else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        preview = new PlayerView(this); preview.setUseController(true); preview.setKeepContentOnPlayerReset(false);
        preview.setShowPreviousButton(false);preview.setShowNextButton(false);preview.setShowRewindButton(false);preview.setShowFastForwardButton(false);
        preview.setControllerAutoShow(false);preview.setControllerShowTimeoutMs(3000);
        root.addView(preview,new LinearLayout.LayoutParams(landscape?0:-1,landscape?-1:0,landscape?3:2));
        ScrollView scroll = new ScrollView(this); controls = DashUi.page(this); scroll.addView(controls);
        controls.setOnApplyWindowInsetsListener(null);controls.setPadding(20,20,20,20);
        root.addView(scroll,new LinearLayout.LayoutParams(landscape?0:-1,landscape?-1:0,landscape?2:3));
        setContentView(root);
    }
    @Override protected void onResume() {
        super.onResume(); if (controls == null) return;
        settings = new DashSettings(this,true); stabilization = null; generation++; buildControls(); prepare();
    }
    void buildControls() {
        controls.removeAllViews(); DashUi.text(this,controls,"RideDeck · Video editor",24);
        DashUi.text(this,controls,clip.getName()+" · original kept",14);
        status = DashUi.text(this,controls,"Playable preview · framing and overlays",15);
        long[] selected;
        try { selected=SocialProfile.range(duration,settings.number("social_start",0),settings.number("social_length",15)); }
        catch (IllegalArgumentException error) { selected=new long[]{0,Math.min(duration,15000)}; }
        rangeLabel=DashUi.text(this,controls,"",16);
        start=slider("In point",(int)(selected[0]/1000)); end=slider("Out point",(int)Math.ceil(selected[1]/1000.0)); updateRange();
        DashUi.button(this,controls,"Apply trim & replay",v->{
            if (analyzing) return;
            if (end.getProgress()<=start.getProgress()) { status.setText("Out point must be after the in point"); return; }
            DashSettings editable=new DashSettings(this); editable.put("social_start",start.getProgress());
            editable.put("social_length",end.getProgress()-start.getProgress());
            settings=new DashSettings(this,true); stabilization=null; generation++; prepare();
        });
        DashUi.button(this,controls,"Format, framing, sound & overlays",v->{if(!analyzing)startActivity(new Intent(this,SocialSettingsActivity.class).putExtra("clip",clip.getPath()).putExtra("fromEditor",true));});
        DashUi.button(this,controls,"Preview stabilization",v->{if(analyzing)return;new AlertDialog.Builder(this).setTitle("Preview stabilization").setItems(new String[]{"Off · full view","Gentle · 12% zoom","Strong · 22% zoom","Maximum · 35% zoom"},(d,strength)->analyze(strength)).show();});
        DashUi.button(this,controls,"Create edited copy",v->{
            if(analyzing)return;
            if(start.getProgress()*1000L!=appliedRange[0] || Math.min(duration,end.getProgress()*1000L)!=appliedRange[1]) {
                status.setText("Apply your trim before exporting"); return;
            }
            startActivity(new Intent(this,OverlayExportActivity.class).putExtra("clip",clip.getPath()).putExtra("social",true).putExtra("recipe",settings.bundle()));
        });
        DashUi.text(this,controls,"Use the player controls to inspect your selected section. The RideDeck logo stays on every copy. Preview stabilization before exporting; blur and missing captured frames cannot be recovered.",14);
        DashUi.button(this,controls,"Back to clips",v->finish());
    }
    SeekBar slider(String label,int value) {
        DashUi.text(this,controls,label,14); SeekBar bar=new SeekBar(this);
        bar.setContentDescription(label); bar.setMax((int)Math.ceil(duration/1000.0)); bar.setProgress(value);
        controls.addView(bar,new LinearLayout.LayoutParams(-1,64));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){if(start!=null&&end!=null)updateRange();}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        }); return bar;
    }
    void updateRange(){rangeLabel.setText("Trim · "+start.getProgress()+"s to "+Math.min(duration/1000.0,end.getProgress())+"s");}
    void prepare() {
        try {
            appliedRange=SocialProfile.range(duration,settings.number("social_start",0),settings.number("social_length",15));
            if(player!=null){preview.setPlayer(null);player.release();}
            player=new ExoPlayer.Builder(this).build(); preview.setPlayer(player);
            player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){status.setText("Preview failed: "+error.getErrorCodeName());}});
            player.setVideoEffects(SocialEffects.create(this,clip,settings,appliedRange[0]*1000,stabilization,new OverlayRoads(),false));
            player.setVolume(settings.number("social_mute",0)==1?0:1);
            player.setMediaItem(new MediaItem.Builder().setUri(Uri.fromFile(clip)).setClippingConfiguration(
                new MediaItem.ClippingConfiguration.Builder().setStartPositionMs(appliedRange[0]).setEndPositionMs(appliedRange[1]).build()).build());
            player.setRepeatMode(Player.REPEAT_MODE_ONE);player.prepare();
            preview.hideController();
            status.setText(stabilization==null?"Framing + data preview · stabilization not applied":"Stabilized preview · same correction as export");
        } catch(Exception error){status.setText("Preview unavailable: "+error.getMessage());}
    }
    void analyze(int strength) {
        if(RecordingService.running||RecordingService.finalizing){status.setText("Stop recording before analyzing video");return;}
        if(!power()){status.setText("Connect charger or charge to 50% before analyzing video");return;}
        new DashSettings(this).put("export_stabilization",strength);
        Map<String,Object> values=new HashMap<>(settings.snapshot);values.put("export_stabilization",strength);settings=new DashSettings(this,values);
        if(strength==0){stabilization=null;prepare();return;}
        analyzing=true; if(player!=null)player.pause(); final int request=++generation;
        final long from=appliedRange[0]*1000,to=appliedRange[1]*1000;
        status.setText("Analyzing selected section…");
        new Thread(()->{try{
            VideoStabilizer result=VideoStabilizer.analyze(clip,from,to,strength,()->cancelled||request!=generation||!power()||RecordingService.running||RecordingService.finalizing,
                percent->runOnUiThread(()->{if(request==generation&&!cancelled)status.setText("Analyzing movement · "+percent+"%");}));
            runOnUiThread(()->{if(cancelled||request!=generation)return;analyzing=false;stabilization=result;prepare();});
        }catch(Exception error){runOnUiThread(()->{if(cancelled||request!=generation)return;analyzing=false;status.setText("Analysis stopped: "+error.getMessage());});}},"DashEditorAnalysis").start();
    }
    boolean power(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b!=null&&RecordingGate.evaluate(b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0,RecordingGate.batteryPercent(b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),b.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),true,false)==RecordingGate.Result.ALLOWED;}
    @Override protected void onStop(){generation++;analyzing=false;if(player!=null)player.pause();super.onStop();}
    @Override protected void onDestroy(){cancelled=true;generation++;if(player!=null){preview.setPlayer(null);player.release();}super.onDestroy();}
}
