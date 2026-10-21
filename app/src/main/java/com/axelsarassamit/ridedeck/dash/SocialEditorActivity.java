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
    long[] appliedRange; volatile int generation; int editorStrength, restoredStart=-1,restoredEnd=-1;
    Button export; final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this); super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(state!=null){editorStrength=state.getInt("strength",0);restoredStart=state.getInt("draftStart",-1);restoredEnd=state.getInt("draftEnd",-1);}
        try {
            if(RecordingService.running||RecordingService.finalizing)throw new IOException("Stop recording before editing a clip");
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
        LinearLayout root = new LinearLayout(this); root.setBackgroundColor(RideStyle.BACKGROUND);
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
        DashSettings base=new DashSettings(this,true);Map<String,Object> values=new HashMap<>(base.snapshot);
        values.put("export_stabilization",editorStrength);
        try{SocialProfile.range(duration,base.number("social_start",0),base.number("social_length",15));}
        catch(IllegalArgumentException error){values.put("social_start",0);values.put("social_length",15);}
        settings = new DashSettings(this,values); stabilization = null; generation++; buildControls(); prepare();
        handler.post(recordingGuard);
    }
    void buildControls() {
        controls.removeAllViews(); DashUi.text(this,controls,"RideDeck · Video editor",24);
        DashUi.text(this,controls,clip.getName()+" · original kept",14);
        status = DashUi.text(this,controls,"Playable preview · framing and overlays",15);
        long[] selected;
        try { selected=SocialProfile.range(duration,settings.number("social_start",0),settings.number("social_length",15)); }
        catch (IllegalArgumentException error) { selected=new long[]{0,Math.min(duration,15000)}; }
        rangeLabel=DashUi.text(this,controls,"",16);
        start=slider("In point",restoredStart>=0?restoredStart:(int)(selected[0]/1000)); end=slider("Out point",restoredEnd>=0?restoredEnd:(int)Math.ceil(selected[1]/1000.0));restoredStart=restoredEnd=-1; updateRange();
        DashUi.button(this,controls,"Set in point at playhead",v->{if(player!=null&&appliedRange!=null)start.setProgress((int)((appliedRange[0]+player.getCurrentPosition())/1000));});
        DashUi.button(this,controls,"Set out point at playhead",v->{if(player!=null&&appliedRange!=null)end.setProgress((int)Math.ceil((appliedRange[0]+player.getCurrentPosition())/1000.0));});
        DashUi.button(this,controls,"Apply trim & replay",v->{
            if (analyzing) return;
            if (end.getProgress()<=start.getProgress()) { status.setText("Out point must be after the in point"); return; }
            DashSettings editable=new DashSettings(this); editable.put("social_start",start.getProgress());
            editable.put("social_length",end.getProgress()-start.getProgress());
            Map<String,Object> recipe=new HashMap<>(new DashSettings(this,true).snapshot);recipe.put("export_stabilization",editorStrength);settings=new DashSettings(this,recipe); stabilization=null; generation++; prepare();
        });
        DashUi.button(this,controls,"Format, framing, sound & overlays",v->{if(!analyzing)startActivity(new Intent(this,SocialSettingsActivity.class).putExtra("clip",clip.getPath()).putExtra("fromEditor",true));});
        DashUi.button(this,controls,"Open multi-clip / front PiP studio",v->{Bundle b=SocialProject.selection(Collections.singletonList(clip.getPath()));if(appliedRange!=null){b.putLongArray("starts",new long[]{appliedRange[0]});b.putLongArray("ends",new long[]{appliedRange[1]});}startActivity(new Intent(this,SocialStudioActivity.class).putExtra("project",b));});
        DashUi.button(this,controls,"Preview stabilization · experimental",v->{if(analyzing||appliedRange==null)return;new AlertDialog.Builder(this).setTitle("Experimental stabilization preview").setItems(new String[]{"Off · full view","Gentle · 12% zoom","Strong · 22% zoom","Maximum · 35% zoom"},(d,strength)->analyze(strength)).show();});
        export=DashUi.button(this,controls,"Create edited copy",v->{
            if(analyzing)return;
            if(appliedRange==null||RecordingService.running||RecordingService.finalizing){status.setText("Stop recording and apply a valid trim before exporting");return;}
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
            if(RecordingService.running||RecordingService.finalizing)throw new IOException("Stop recording before previewing video");
            appliedRange=SocialProfile.range(duration,settings.number("social_start",0),settings.number("social_length",15));
            releasePlayer();
            player=new ExoPlayer.Builder(this).build(); preview.setPlayer(player);
            player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){status.setText("Preview failed: "+error.getErrorCodeName());}});
            player.setVideoEffects(SocialEffects.create(this,clip,settings,appliedRange[0]*1000,stabilization,new OverlayRoads(),false));
            player.setVolume(settings.number("social_mute",0)==1?0:1);
            player.setMediaItem(new MediaItem.Builder().setUri(Uri.fromFile(clip)).setClippingConfiguration(
                new MediaItem.ClippingConfiguration.Builder().setStartPositionMs(appliedRange[0]).setEndPositionMs(appliedRange[1]).build()).build());
            player.setRepeatMode(Player.REPEAT_MODE_ONE);player.prepare();
            preview.hideController();
            export.setEnabled(true);
            status.setText(stabilization==null?(editorStrength==0?"Framing + data preview · stabilization off":"Framing + data preview · export stabilization not previewed"):"Experimental stabilized preview · same correction as export");
        } catch(Exception error){appliedRange=null;export.setEnabled(false);releasePlayer();status.setText("Preview unavailable: "+error.getMessage());}
    }
    void analyze(int strength) {
        if(RecordingService.running||RecordingService.finalizing){status.setText("Stop recording before analyzing video");return;}
        if(!power()){status.setText("Connect charger or charge to 50% before analyzing video");return;}
        new DashSettings(this).put("export_stabilization",strength);
        editorStrength=strength;
        Map<String,Object> values=new HashMap<>(settings.snapshot);values.put("export_stabilization",strength);settings=new DashSettings(this,values);
        if(strength==0){stabilization=null;prepare();return;}
        analyzing=true; if(player!=null)player.pause(); final int request=++generation;
        final long from=appliedRange[0]*1000,to=appliedRange[1]*1000;
        status.setText("Analyzing selected section…");
        new Thread(()->{try{
            VideoStabilizer result=VideoStabilizer.analyze(clip,from,to,strength,()->cancelled||request!=generation||!power()||RecordingService.running||RecordingService.finalizing,
                percent->runOnUiThread(()->{if(request==generation&&!cancelled)status.setText("Analyzing movement · "+percent+"%");}));
            runOnUiThread(()->{if(cancelled||request!=generation)return;analyzing=false;stabilization=result;prepare();});
        }catch(Exception error){runOnUiThread(()->{if(cancelled||request!=generation)return;analyzing=false;editorStrength=0;Map<String,Object> safe=new HashMap<>(settings.snapshot);safe.put("export_stabilization",0);settings=new DashSettings(this,safe);stabilization=null;status.setText("Analysis stopped: "+error.getMessage()+". Export stabilization is off.");});}},"DashEditorAnalysis").start();
    }
    boolean power(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b!=null&&RecordingGate.evaluate(b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0,RecordingGate.batteryPercent(b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),b.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),true,false)==RecordingGate.Result.ALLOWED;}
    void releasePlayer(){if(player!=null){preview.setPlayer(null);player.release();player=null;}}
    final Runnable recordingGuard=new Runnable(){public void run(){if(RecordingService.running||RecordingService.finalizing){generation++;analyzing=false;releasePlayer();appliedRange=null;export.setEnabled(false);status.setText("Preview stopped to prioritize recording. Return after the ride to edit.");return;}handler.postDelayed(this,500);}};
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("strength",editorStrength);if(start!=null&&end!=null){state.putInt("draftStart",start.getProgress());state.putInt("draftEnd",end.getProgress());}super.onSaveInstanceState(state);}
    @Override protected void onStop(){generation++;if(analyzing)editorStrength=0;analyzing=false;handler.removeCallbacksAndMessages(null);releasePlayer();super.onStop();}
    @Override protected void onDestroy(){cancelled=true;generation++;releasePlayer();super.onDestroy();}
}
