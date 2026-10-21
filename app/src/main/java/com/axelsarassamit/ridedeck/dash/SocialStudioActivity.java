package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.effect.MultipleInputVideoGraph;
import androidx.media3.transformer.CompositionPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

/** Ordered, non-destructive batch/merge editor with the same composition as export. */
@androidx.annotation.OptIn(markerClass={androidx.media3.common.util.UnstableApi.class,androidx.media3.common.util.ExperimentalApi.class})
public final class SocialStudioActivity extends Activity {
    SocialProject project;DashSettings recipe;CompositionPlayer player;PlayerView preview;
    LinearLayout controls;TextView status;Button export;volatile int generation;
    boolean firstFrameRendered;
    final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this);super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try { project=SocialProject.load(this,state==null?getIntent().getBundleExtra("project"):state.getBundle("project"));layout(); }
        catch(Exception e){project=null;LinearLayout page=DashUi.page(this);setContentView(page);DashUi.text(this,page,"Select clips again: "+e.getMessage(),18);DashUi.button(this,page,"Back",v->finish());}
    }
    void layout() {
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root=new LinearLayout(this);root.setBackgroundColor(RideStyle.BACKGROUND);root.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        preview=new PlayerView(this);preview.setControllerShowTimeoutMs(3000);preview.setControllerAutoShow(false);
        root.addView(preview,new LinearLayout.LayoutParams(wide?0:-1,wide?-1:0,wide?3:2));
        ScrollView scroll=new ScrollView(this);controls=DashUi.page(this);controls.setOnApplyWindowInsetsListener(null);scroll.addView(controls);
        root.addView(scroll,new LinearLayout.LayoutParams(wide?0:-1,wide?-1:0,wide?2:3));setContentView(root);
    }
    @Override protected void onResume(){super.onResume();if(project!=null){recipe=new DashSettings(this,true);refresh();handler.post(recordingGuard);}}
    void refresh() {
        generation++;release();firstFrameRendered=false;controls.removeAllViews();
        DashUi.text(this,controls,"Social video studio",24);
        DashUi.text(this,controls,project.clips.size()+" clips · "+seconds(project.durationMs())+"s selected · originals kept",15);
        status=DashUi.text(this,controls,"Preparing playable preview",15);
        DashUi.button(this,controls,"Output: "+(project.merge?"Merge into one video":"Separate video for each clip"),v->new AlertDialog.Builder(this).setTitle("Export selected clips")
            .setItems(new String[]{"Merge into one video","Separate social videos"},(d,i)->{project.merge=i==0;refresh();}).show());
        DashUi.button(this,controls,"Platform, framing, sound & overlays",v->startActivity(new Intent(this,SocialSettingsActivity.class).putExtra("fromEditor",true)));
        DashUi.button(this,controls,"Apply preset trim to every clip",v->new AlertDialog.Builder(this).setTitle("Replace all trim ranges?").setMessage("Uses the preset start and length separately for each original. Order is preserved.")
            .setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,w)->{for(SocialProject.Clip item:project.clips){long[] r;try{r=SocialProfile.range(item.durationMs,recipe.number("social_start",0),recipe.number("social_length",15));}catch(Exception e){r=SocialProfile.range(item.durationMs,0,recipe.number("social_length",15));}item.fromMs=r[0];item.toMs=r[1];}refresh();}).show());
        CheckBox pip=new CheckBox(this);pip.setText("Front camera as picture-in-picture");pip.setTextColor(RideStyle.TEXT);pip.setChecked(project.pip);controls.addView(pip);
        pip.setOnCheckedChangeListener((v,on)->{project.pip=on;refresh();});
        if(project.pip) {
            DashUi.text(this,controls,"Rear camera fills the main picture. Front camera is a small rounded inset, like the live Dash screen.",14);
            DashUi.button(this,controls,"Rear framing: "+(project.fillRear?"Fill canvas":"Keep whole picture"),v->{project.fillRear=!project.fillRear;refresh();});
            String[] corners={"Top left","Top right","Bottom left","Bottom right"};
            DashUi.button(this,controls,"PiP corner: "+corners[project.corner],v->new AlertDialog.Builder(this).setTitle("Front camera position").setItems(corners,(d,i)->{project.corner=i;refresh();}).show());
            DashUi.button(this,controls,"PiP size: "+project.size+"%",v->new AlertDialog.Builder(this).setTitle("Front camera size").setItems(new String[]{"Small · 20%","Medium · 28%","Large · 36%"},(d,i)->{project.size=new int[]{20,28,36}[i];refresh();}).show());
            DashUi.button(this,controls,"Front timing offset: "+project.shiftMs+" ms",v->syncOffset());
            DashUi.text(this,controls,"Matches simultaneous front footage from the same ride using capture times, including different clip rollovers. Small startup offsets may remain; adjust timing if needed. Positive offset delays the front view. Missing front sections hide PiP. Main-clip sound is kept; front sound is muted.",14);
        }
        DashUi.text(this,controls,"CLIP ORDER & TRIM",16);
        for(int i=0;i<project.clips.size();i++) {
            final int index=i;SocialProject.Clip item=project.clips.get(i);
            DashUi.text(this,controls,(i+1)+" · "+item.file.getParentFile().getName()+" / "+item.file.getName(),15);
            DashUi.button(this,controls,"Trim: "+seconds(item.fromMs)+"s to "+seconds(item.toMs)+"s",v->trim(index));
            LinearLayout row=new LinearLayout(this);controls.addView(row);
            Button up=DashUi.button(this,row,"Move up",v->{Collections.swap(project.clips,index,index-1);refresh();});up.setEnabled(i>0);
            Button down=DashUi.button(this,row,"Move down",v->{Collections.swap(project.clips,index,index+1);refresh();});down.setEnabled(i+1<project.clips.size());
            Button remove=DashUi.button(this,row,"Remove",v->{project.clips.remove(index);refresh();});remove.setEnabled(project.clips.size()>1);
            for(Button b:new Button[]{up,down,remove})b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        }
        export=DashUi.button(this,controls,project.merge?"Create merged social video":"Create "+project.clips.size()+" separate social videos",v->{
            if(!power()||RecordingService.running||RecordingService.finalizing){status.setText("Stop recording and connect charger or charge to 50%");return;}
            startActivity(new Intent(this,SocialExportActivity.class).putExtra("project",project.bundle()).putExtra("recipe",recipe.bundle()));
        });export.setEnabled(false);
        DashUi.text(this,controls,"Preview plays the selected clips in order. Straight cuts preserve your chosen order. The RideDeck logo is always included. This multi-clip mode does not add experimental export stabilization. Up to 30 clips and 30 minutes of selected video. Keep Dash open while exporting.",14);
        DashUi.button(this,controls,"Back to recordings",v->finish());prepare();
    }
    void trim(int index) {
        SocialProject.Clip item=project.clips.get(index);LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
        EditText from=new EditText(this),to=new EditText(this);from.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);to.setInputType(from.getInputType());
        from.setHint("In point, seconds");to.setHint("Out point, seconds");from.setText(seconds(item.fromMs));to.setText(seconds(item.toMs));fields.addView(from);fields.addView(to);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Trim clip "+(index+1)+" · "+seconds(item.durationMs)+"s").setView(fields).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{try{long a=parseMs(from.getText().toString()),b=parseMs(to.getText().toString());if(a<0||b>item.durationMs||b-a<500)throw new IllegalArgumentException();item.fromMs=a;item.toMs=b;dialog.dismiss();refresh();}catch(Exception e){to.setError("Use a valid range of at least 0.5 seconds within this clip");}}));dialog.show();
    }
    void syncOffset() {
        EditText value=new EditText(this);value.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);value.setText(""+project.shiftMs);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Front delay, milliseconds (-5000 to 5000)").setView(value).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{try{int n=Integer.parseInt(value.getText().toString());if(Math.abs((long)n)>5000)throw new IllegalArgumentException();project.shiftMs=n;dialog.dismiss();refresh();}catch(Exception e){value.setError("Enter -5000 to 5000");}}));dialog.show();
    }
    static String seconds(long ms){return java.math.BigDecimal.valueOf(ms,3).stripTrailingZeros().toPlainString();}
    static long parseMs(String s){return new java.math.BigDecimal(s.trim()).movePointRight(3).longValueExact();}
    void prepare() {
        if(!power()||RecordingService.running||RecordingService.finalizing){status.setText("Stop recording and connect charger or charge to 50%");return;}
        final int request=++generation;final Bundle spec=project.bundle();final DashSettings frozen=recipe;
        new Thread(()->{try{
            SocialProject snapshot=SocialProject.load(this,spec);
            SocialComposition.Built built=SocialComposition.build(this,snapshot,frozen,false,()->generation!=request);
            runOnUiThread(()->{if(generation!=request||isFinishing()||isDestroyed())return;
                try {
                player=new CompositionPlayer.Builder(this).setVideoGraphFactory(new MultipleInputVideoGraph.Factory()).build();preview.setPlayer(player);
                player.addListener(new Player.Listener(){@Override public void onRenderedFirstFrame(){if(generation==request)firstFrameRendered=true;}@Override public void onPlayerError(PlaybackException e){if(generation==request)status.setText("Preview failed: "+e.getErrorCodeName()+". Originals are unchanged.");}});
                player.setComposition(built.composition);player.prepare();preview.hideController();export.setEnabled(true);
                status.setText(snapshot.pip?"Front PiP matched · "+(built.missingFrontMs>0?seconds(built.missingFrontMs)+"s without front footage":"full selected coverage"):"Ready · preview and export use the same framing and data");
                }catch(Exception e){release();export.setEnabled(false);status.setText("Preview unavailable: "+e.getMessage());}
            });
        }catch(Exception e){runOnUiThread(()->{if(generation==request)status.setText("Preview unavailable: "+e.getMessage());});}},"DashSocialComposition").start();
    }
    boolean power(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b!=null&&RecordingGate.evaluate(b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0,RecordingGate.batteryPercent(b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),b.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),true,false)==RecordingGate.Result.ALLOWED;}
    final Runnable recordingGuard=new Runnable(){public void run(){if(!power()||RecordingService.running||RecordingService.finalizing){generation++;release();export.setEnabled(false);status.setText("Preview stopped for recording or battery reserve. Return when ready.");return;}handler.postDelayed(this,500);}};
    void release(){if(player!=null){preview.setPlayer(null);player.release();player=null;}}
    @Override protected void onSaveInstanceState(Bundle b){if(project!=null)b.putBundle("project",project.bundle());super.onSaveInstanceState(b);}
    @Override protected void onStop(){generation++;handler.removeCallbacksAndMessages(null);release();super.onStop();}
    @Override protected void onDestroy(){generation++;release();super.onDestroy();}
}
