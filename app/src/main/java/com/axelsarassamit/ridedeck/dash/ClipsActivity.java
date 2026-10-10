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
    @Override public void onCreate(Bundle b) { super.onCreate(b); showList(); }
    private void showList() {
        if(RecordingService.finalizing){LinearLayout waiting=DashUi.page(this);setContentView(waiting);DashUi.text(this,waiting,"Saving clips, please wait",22);waiting.postDelayed(()->{if(!isFinishing()&&!isDestroyed())showList();},500);return;}
        LinearLayout page=DashUi.page(this); ScrollView scroll=new ScrollView(this); scroll.addView(page); setContentView(scroll);
        DashUi.text(this,page,"Saved clips",28);
        DashUi.text(this,page,"Protected clips stay in loop storage. Share a copy before clearing app data. Interrupted files are kept but may not play.",16);
        java.util.List<File> files=new ClipStore(this).clips();
        if(files.isEmpty()) DashUi.text(this,page,"Your first recording will appear here.",20);
        for(File file:files) {
            String label=(ClipStore.protectedClip(file)?"Protected · ":"")+file.getName()+"\n"+new SimpleDateFormat("dd MMM HH:mm:ss",Locale.getDefault()).format(new Date(file.lastModified()))
                +" · "+(file.length()/1024/1024)+" MB"+(ClipStore.finalized(file)?"":" · recording / unverified");
            DashUi.button(this,page,label,v -> new AlertDialog.Builder(this).setTitle(file.getName()).setItems(new String[]{"Play","Share video","Save to gallery","Save ride data","Show ride map",ClipStore.protectedClip(file)?"Allow loop recycling":"Keep clip","Delete clip"},(dialog,item) -> {
                if(item==0) play(file);
                else if(item==1) {
                    if(!ClipStore.finalized(file)) { Toast.makeText(this,"Stop recording before sharing this clip",Toast.LENGTH_LONG).show(); return; }
                    Intent share=new Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID+".updates",file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share,"Share clip"));
                } else if(item==2) export(file);
                else if(item==3) exportData(file.getParentFile());
                else if(item==4) startActivity(new Intent(this,MapActivity.class).putExtra("ride",file.getParentFile().getName()));
                else if(item==5) new AlertDialog.Builder(this).setMessage(ClipStore.protectedClip(file)?"Allow this clip to be recycled when loop storage fills? Export a copy first if you need it.":"Keep this clip so loop recording cannot recycle it?")
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
        }
        DashUi.button(this,page,"Back to dashboard",v -> finish());
    }
    private void exportData(File ride) {
        if(RecordingService.running) { Toast.makeText(this,"Stop recording before exporting ride data",Toast.LENGTH_LONG).show(); return; }
        if(Build.VERSION.SDK_INT<29) { Toast.makeText(this,"Ride data export requires Android 10 or newer",Toast.LENGTH_LONG).show(); return; }
        new Thread(() -> {
            try {
                for(String name:new String[]{"route.csv","capture.csv"}) {
                    File file=new File(ride,name);if(!file.isFile())continue;
                    ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,"Dash-"+ride.getName()+"-"+name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE,"text/csv");values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/RideDeckDash");values.put(MediaStore.MediaColumns.IS_PENDING,1);
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
