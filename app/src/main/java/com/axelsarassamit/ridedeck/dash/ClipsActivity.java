package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public final class ClipsActivity extends Activity {
    private final Set<String> selected=new LinkedHashSet<>();
    private boolean selecting;
    private ClipThumbnails thumbnails;
    private static volatile boolean deleting;
    private static volatile ClipDeletion.Result pendingDeletion;
    private TextView selectionSummary;
    private Button deleteSelected;
    private Button socialSelected;
    @Override public void onCreate(Bundle b) { RideAppearance.prepare(this); super.onCreate(b);thumbnails=new ClipThumbnails(this);if(b!=null){selecting=b.getBoolean("selecting");ArrayList<String> saved=b.getStringArrayList("selected");if(saved!=null)selected.addAll(saved);}showList(); }
    @Override protected void onSaveInstanceState(Bundle b){b.putBoolean("selecting",selecting);b.putStringArrayList("selected",new ArrayList<>(selected));super.onSaveInstanceState(b);}

    private void showList() {
        thumbnails.begin();
        if(deleting){LinearLayout waiting=DashUi.page(this);setContentView(waiting);DashUi.text(this,waiting,"Deleting selected clips",22);waiting.postDelayed(()->{if(!isFinishing()&&!isDestroyed())showList();},500);return;}
        if(RecordingService.finalizing){LinearLayout waiting=DashUi.page(this);setContentView(waiting);DashUi.text(this,waiting,"Saving clips, please wait",22);waiting.postDelayed(()->{if(!isFinishing()&&!isDestroyed())showList();},500);return;}
        LinearLayout page=DashUi.page(this); ScrollView scroll=new ScrollView(this); scroll.addView(page); setContentView(scroll);
        DashUi.text(this,page,"Saved clips",28);
        DashUi.text(this,page,"Protected clips stay in loop storage. Share a copy before clearing app data. Interrupted files are kept but may not play.",16);
        final java.util.List<File> allFiles=new ClipStore(this).clips();
        java.util.List<File> files=new ArrayList<>(allFiles);
        android.content.SharedPreferences library=getSharedPreferences("clip-library",MODE_PRIVATE);
        int sort=library.getInt("sort",0),filter=library.getInt("filter",0);
        String[] sorts={"Newest first","Oldest first","Name A to Z","Largest first","Smallest first"};
        String[] filters={"All cameras","Rear camera","Front camera","Protected clips"};
        Comparator<File> comparison=sort==1?Comparator.comparingLong(File::lastModified):sort==2?Comparator.comparing(File::getName):sort==3?Comparator.comparingLong(File::length).reversed():sort==4?Comparator.comparingLong(File::length):Comparator.comparingLong(File::lastModified).reversed();
        files.sort(comparison.thenComparing(File::getPath));
        files.removeIf(f->filter==1&&!f.getName().startsWith("rear-")||filter==2&&!f.getName().startsWith("front-")||filter==3&&!ClipStore.protectedClip(f));
        LinearLayout sorting=new LinearLayout(this);page.addView(sorting);
        Button sortButton=DashUi.button(this,sorting,sorts[Math.max(0,Math.min(4,sort))],v->new AlertDialog.Builder(this).setTitle("Sort recordings").setItems(sorts,(d,i)->{library.edit().putInt("sort",i).apply();showList();}).show());
        Button filterButton=DashUi.button(this,sorting,filters[Math.max(0,Math.min(3,filter))],v->new AlertDialog.Builder(this).setTitle("Show recordings").setItems(filters,(d,i)->{library.edit().putInt("filter",i).apply();showList();}).show());
        for(Button button:new Button[]{sortButton,filterButton}){button.setTextSize(13);button.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}

        ClipDeletion.Result completed=pendingDeletion;if(completed!=null){pendingDeletion=null;selected.removeAll(completed.removed);if(selected.isEmpty())selecting=false;page.post(()->{if(!isDestroyed()&&!isFinishing())new AlertDialog.Builder(this).setTitle("Deletion complete").setMessage(completed.removed.size()+" clips removed"+(completed.failed>0?"\n"+completed.failed+" clips could not be removed. They remain selected.":"")).setPositiveButton("OK",null).show();});}
        Set<String> existing=new HashSet<>();for(File f:allFiles)existing.add(f.getPath());selected.retainAll(existing);
        DashUi.button(this,page,selecting?"Done selecting":"Select clips",v->{selecting=!selecting;if(!selecting)selected.clear();showList();});
        if(!selecting)DashUi.button(this,page,"Saved video edits",v->showDrafts());
        if(!selecting)DashUi.button(this,page,"Back up originals and ride logs",v->startActivity(new Intent(this,RecordingBackupActivity.class)));
        if(selecting){
            selectionSummary=DashUi.text(this,page,"",18);
            LinearLayout actions=new LinearLayout(this);page.addView(actions);
            Button all=DashUi.button(this,actions,"Select visible",v->{for(File f:files)selected.add(f.getPath());showList();});
            Button clear=DashUi.button(this,actions,"Clear selection",v->{selected.clear();showList();});
            for(Button button:new Button[]{all,clear})button.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            socialSelected=DashUi.button(this,page,"Social export / merge selected",v->openStudio(new ArrayList<>(selected)));
            deleteSelected=DashUi.button(this,page,"Delete selected",v->confirmDelete(allFiles));updateSelection(allFiles);
            DashUi.text(this,page,"Select clips for social export, merging or removal. You can reorder and trim in the studio. For front PiP, select rear clips; simultaneous front footage is matched automatically. Stop recording before editing or deleting.",15);
        }
        if(files.isEmpty()) DashUi.text(this,page,allFiles.isEmpty()?"Your first recording will appear here.":"No recordings match this filter.",20);
        for(File file:files) {
            String label=(ClipStore.protectedClip(file)?"Protected · ":"")+file.getName()+"\n"+new SimpleDateFormat("dd MMM HH:mm:ss",Locale.getDefault()).format(new Date(file.lastModified()))
                +" · "+(file.length()/1024/1024)+" MB"+(ClipStore.finalized(file)?"":" · recording / unverified");
            LinearLayout mediaRow=new LinearLayout(this);mediaRow.setGravity(android.view.Gravity.CENTER_VERTICAL);mediaRow.setPadding(0,4,0,4);page.addView(mediaRow);
            ImageView thumbnail=new ImageView(this);thumbnail.setContentDescription("Thumbnail "+file.getName());thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);DashUi.card(thumbnail,RideStyle.RAISED,8);thumbnail.setClipToOutline(true);mediaRow.addView(thumbnail,new LinearLayout.LayoutParams(DashUi.dp(this,96),DashUi.dp(this,72)));thumbnails.bind(thumbnail,file);
            if(selecting){CheckBox box=new CheckBox(this);box.setText(label);box.setTextColor(RideStyle.TEXT);box.setTextSize(16);box.setPadding(12,20,12,20);box.setMinHeight((int)(72*getResources().getDisplayMetrics().density));box.setChecked(selected.contains(file.getPath()));box.setOnCheckedChangeListener((button,on)->{if(on)selected.add(file.getPath());else selected.remove(file.getPath());updateSelection(allFiles);});mediaRow.addView(box,new LinearLayout.LayoutParams(0,-2,1));thumbnail.setOnClickListener(v->box.toggle());continue;}
            if(getIntent().getBooleanExtra("socialPick",false)){Button pick=DashUi.button(this,mediaRow,label,v->openStudio(Collections.singletonList(file.getPath())));pick.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));continue;}
            Button clipButton=DashUi.button(this,mediaRow,label,v -> new AlertDialog.Builder(this).setTitle(file.getName()).setItems(new String[]{"Play","Share video","Save to gallery","Edit video","Save ride data","Show ride map",ClipStore.protectedClip(file)?"Allow loop recycling":"Keep clip","Delete clip"},(dialog,item) -> {
                if(item==0) play(file);
                else if(item==1) {
                    if(!ClipStore.finalized(file)) { Toast.makeText(this,"Stop recording before sharing this clip",Toast.LENGTH_LONG).show(); return; }
                    Intent share=new Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID+".updates",file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share,"Share clip"));
                } else if(item==2) export(file);
                else if(item==3) {if(!ClipStore.finalized(file)||RecordingService.running){Toast.makeText(this,"Stop recording before editing",Toast.LENGTH_LONG).show();return;}openStudio(Collections.singletonList(file.getPath()));}
                else if(item==4) exportData(file.getParentFile());
                else if(item==5) startActivity(new Intent(this,MapActivity.class).putExtra("ride",file.getParentFile().getName()));
                else if(item==6) new AlertDialog.Builder(this).setMessage(ClipStore.protectedClip(file)?"Allow this clip to be recycled when loop storage fills? Export a copy first if you need it.":"Keep this clip so loop recording cannot recycle it?")
                    .setNegativeButton("Cancel",null).setPositiveButton("Confirm",(d,w)->{
                        try { if(ClipStore.protectedClip(file)) { if(!new File(file.getPath()+".protected").delete())throw new java.io.IOException(); }
                            else ClipStore.mark(file,".protected");showList(); }
                        catch(java.io.IOException error){Toast.makeText(this,"Could not change clip protection",Toast.LENGTH_LONG).show();}
                    }).show();
                else new AlertDialog.Builder(this).setTitle("Delete this clip?").setMessage("This removes the internal video permanently, including a protected clip. Export a copy first if you need it.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete clip",(d,w)->{
                        if(RecordingService.running && !ClipStore.finalized(file)) {Toast.makeText(this,"Stop recording before deleting this clip",Toast.LENGTH_LONG).show();return;}
                        if(file.delete()) {new File(file.getPath()+".ready").delete();new File(file.getPath()+".protected").delete();showList();}
                        else Toast.makeText(this,"Could not delete clip",Toast.LENGTH_LONG).show();
                    }).show();
            }).show());
            clipButton.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            thumbnail.setOnClickListener(v->clipButton.performClick());
            clipButton.setOnLongClickListener(v->{selecting=true;selected.add(file.getPath());showList();return true;});
        }
        DashUi.button(this,page,"Back to dashboard",v -> finish());
    }
    @Override protected void onDestroy(){if(thumbnails!=null)thumbnails.close();super.onDestroy();}
    private void showDrafts(){
        List<File> drafts=StudioDrafts.list(this);if(drafts.isEmpty()){Toast.makeText(this,"No saved edits yet. Open Edit video on a recording to begin.",Toast.LENGTH_LONG).show();return;}
        String[] labels=new String[drafts.size()];for(int i=0;i<labels.length;i++)labels[i]=new SimpleDateFormat("dd MMM HH:mm:ss",Locale.getDefault()).format(new Date(drafts.get(i).lastModified()));
        new AlertDialog.Builder(this).setTitle("Saved video edits").setItems(labels,(d,i)->startActivity(new Intent(this,SocialStudioActivity.class).putExtra("draft",drafts.get(i).getName().replace(".draft","")))).setNegativeButton("Close",null).show();
    }
    private void updateSelection(List<File> files){long bytes=0;int protectedCount=0,count=0;for(File file:files)if(selected.contains(file.getPath())){count++;bytes+=file.length();if(ClipStore.protectedClip(file))protectedCount++;}selectionSummary.setText(count+" selected · "+String.format(Locale.ROOT,"%.1f MB",bytes/1048576.0)+(protectedCount>0?" · "+protectedCount+" protected":""));deleteSelected.setEnabled(count>0&&!RecordingService.running&&!RecordingService.finalizing);socialSelected.setEnabled(count>0&&count<=SocialProject.MAX_CLIPS&&!RecordingService.running&&!RecordingService.finalizing);}
    private void openStudio(Collection<String> paths){
        if(RecordingService.running||RecordingService.finalizing){Toast.makeText(this,"Stop recording before editing",Toast.LENGTH_LONG).show();return;}
        if(paths.isEmpty()||paths.size()>SocialProject.MAX_CLIPS){Toast.makeText(this,"Select 1 to "+SocialProject.MAX_CLIPS+" clips",Toast.LENGTH_LONG).show();return;}
        startActivity(new Intent(this,SocialStudioActivity.class).putExtra("project",SocialProject.selection(paths)));
    }
    private void confirmDelete(List<File> files){
        if(RecordingService.running||RecordingService.finalizing){Toast.makeText(this,"Stop recording before deleting clips",Toast.LENGTH_LONG).show();return;}
        List<File> chosen=new ArrayList<>();long bytes=0;int protectedCount=0;for(File file:files)if(selected.contains(file.getPath())){chosen.add(file);bytes+=file.length();if(ClipStore.protectedClip(file))protectedCount++;}
        if(chosen.isEmpty())return;
        String message="Permanently remove "+chosen.size()+" selected internal videos ("+String.format(Locale.ROOT,"%.1f MB",bytes/1048576.0)+")?"+(protectedCount>0?"\nIncludes "+protectedCount+" protected clips.":"")+"\nExported gallery copies and ride data are kept.";
        new AlertDialog.Builder(this).setTitle("Delete selected clips?").setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Delete "+chosen.size()+" clips",(dialog,which)->{
            if(RecordingService.running||RecordingService.finalizing){Toast.makeText(this,"Stop recording before deleting clips",Toast.LENGTH_LONG).show();return;}
            deleting=true;showList();File root=new ClipStore(this).root();
            new Thread(()->{ClipDeletion.Result result=ClipDeletion.delete(root,chosen);pendingDeletion=result;deleting=false;runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())showList();});},"DashClipDelete").start();
        }).show();
    }
    private void exportData(File ride) {
        if(RecordingService.running) { Toast.makeText(this,"Stop recording before exporting ride data",Toast.LENGTH_LONG).show(); return; }
        if(Build.VERSION.SDK_INT<29) { Toast.makeText(this,"Ride data export requires Android 10 or newer",Toast.LENGTH_LONG).show(); return; }
        new Thread(() -> {
            try {
                File[] evidence=ride.listFiles((dir,name)->name.endsWith(".csv")||name.equals("motion.json")||(name.startsWith("hybrid-")&&name.endsWith("-report.json")));if(evidence==null)throw new java.io.IOException("Ride data unavailable");
                for(File file:evidence) {
                    String name=file.getName();if(!file.isFile())continue;
                    ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,"Dash-"+ride.getName()+"-"+name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE,name.endsWith(".json")?"application/json":"text/csv");values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/RideDeckDash");values.put(MediaStore.MediaColumns.IS_PENDING,1);
                    Uri target=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
                    if(target==null)throw new java.io.IOException("Downloads unavailable");
                    try {
                        try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(target)) {
                            if(out==null)throw new java.io.IOException("Downloads unavailable");byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1)out.write(data,0,n);
                        }
                        values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(target,values,null,null);
                    } catch(Exception error) {getContentResolver().delete(target,null,null);throw error;}
                }
                runOnUiThread(()->Toast.makeText(this,"Ride data saved in Downloads / RideDeckDash",Toast.LENGTH_LONG).show());
            }catch(Exception error){runOnUiThread(()->Toast.makeText(this,"Ride data export failed",Toast.LENGTH_LONG).show());}
        },"DashDataExport").start();
    }
    private void export(File file) {
        if(!ClipStore.finalized(file)) { Toast.makeText(this,"Stop recording before exporting this clip",Toast.LENGTH_LONG).show(); return; }
        if(Build.VERSION.SDK_INT<29) { Toast.makeText(this,"Use Share video to save this clip on this Android version",Toast.LENGTH_LONG).show(); return; }
        new Thread(() -> {
            Uri target=null;
            try {
                ContentValues values=new ContentValues(); values.put(MediaStore.Video.Media.DISPLAY_NAME,"Dash-"+file.getParentFile().getName()+"-"+file.getName());
                values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4"); values.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/RideDeckDash"); values.put(MediaStore.Video.Media.IS_PENDING,1);
                target=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);
                if(target==null)throw new java.io.IOException("Gallery unavailable");
                try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(target)) {
                    if(out==null)throw new java.io.IOException("Gallery unavailable"); byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                }
                values.clear();values.put(MediaStore.Video.Media.IS_PENDING,0);getContentResolver().update(target,values,null,null);
                runOnUiThread(()->Toast.makeText(this,"Saved in Movies / RideDeckDash",Toast.LENGTH_LONG).show());
            } catch(Exception error) {
                if(target!=null)getContentResolver().delete(target,null,null);
                runOnUiThread(()->Toast.makeText(this,"Could not export clip",Toast.LENGTH_LONG).show());
            }
        },"DashExport").start();
    }
    private void play(File file) {
        if(!ClipStore.finalized(file)) { Toast.makeText(this,"Stop recording before playing this clip",Toast.LENGTH_LONG).show(); return; }
        LinearLayout page=DashUi.page(this); setContentView(page);
        DashUi.text(this,page,file.getName(),20);
        VideoView video=new VideoView(this); page.addView(video,new LinearLayout.LayoutParams(-1,0,1));
        video.setVideoPath(file.getPath()); video.setMediaController(new MediaController(this)); video.setOnPreparedListener(m -> video.start());
        video.setOnErrorListener((m,w,e)->{Toast.makeText(this,"This clip could not be played",Toast.LENGTH_LONG).show();return true;});
        DashUi.button(this,page,"Back to clips",v -> {video.stopPlayback();showList();});
    }
}
