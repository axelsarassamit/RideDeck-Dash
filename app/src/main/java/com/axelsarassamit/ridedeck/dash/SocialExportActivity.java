package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.transformer.*;
import java.io.*;
import java.util.*;

/** Foreground sequential export. Completed batch copies survive cancellation of a later item. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class SocialExportActivity extends Activity {
    SocialProject project;DashSettings recipe;Transformer transformer;File output;
    TextView progress;Button share;final ArrayList<Uri> saved=new ArrayList<>();
    final Handler handler=new Handler(Looper.getMainLooper());volatile boolean cancelled;boolean completed;int job;
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this);super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        DashUi.text(this,page,"Create social videos",26);progress=DashUi.text(this,page,"Preparing selection",18);
        DashUi.text(this,page,"Keep Dash open until finished. Leaving cancels the unfinished export. You can rotate the screen while it runs. Completed copies remain saved; original recordings are never replaced.",15);
        share=DashUi.button(this,page,"Share completed videos",v->share());share.setVisibility(View.GONE);
        DashUi.button(this,page,"Close / cancel",v->finish());
        if(state!=null){ArrayList<String> previous=state.getStringArrayList("saved");if(previous!=null)for(String uri:previous)saved.add(Uri.parse(uri));progress.setText("Export interrupted by recreation. "+saved.size()+" completed copies kept. Return to the studio to start again.");showShare();return;}
        try {
            if(Build.VERSION.SDK_INT<29)throw new IOException("Gallery export requires Android 10 or newer");
            recipe=DashSettings.fromBundle(this,getIntent().getBundleExtra("recipe"));project=SocialProject.load(this,getIntent().getBundleExtra("project"));
            check();handler.post(tick);next();
        }catch(Exception e){stop("Cannot export: "+e.getMessage());}
    }
    void next() {
        if(cancelled)return;
        if(job>=(project.merge?1:project.clips.size())){completed=true;handler.removeCallbacks(tick);progress.setText("Saved "+saved.size()+" video(s) in Movies / RideDeckDash / Social");showShare();return;}
        final SocialProject selected=project.merge?project:project.single(job);final int current=job;
        progress.setText("Preparing video "+(job+1)+" of "+(project.merge?1:project.clips.size()));
        new Thread(()->{try{
            check();SocialComposition.Built built=SocialComposition.build(this,selected,recipe,true,()->cancelled);
            long estimate=selected.durationMs()*recipe.number("social_bitrate",8)*1000L/8;
            if(getCacheDir().getUsableSpace()<estimate*2+512L*1024*1024)throw new IOException("Not enough free space for a complete copy");
            runOnUiThread(()->{if(cancelled||isFinishing()||isDestroyed()||current!=job)return;
                try{check();output=new File(getCacheDir(),"social-project-"+UUID.randomUUID()+".mp4");
                    transformer=new Transformer.Builder(this).setPortraitEncodingEnabled(true).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(new DefaultEncoderFactory.Builder(this).setEnableFallback(false).setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder().setBitrate(recipe.number("social_bitrate",8)*1000000).build()).build())
                        .addListener(new Transformer.Listener(){
                            @Override public void onCompleted(Composition composition,ExportResult result){transformer=null;if(!cancelled)save(output,current);}
                            @Override public void onError(Composition composition,ExportResult result,ExportException e){stop("Video "+(current+1)+" failed: "+e.getMessage());}
                        }).build();
                    transformer.start(built.composition,output.getPath());
                }catch(Exception e){stop("Export stopped: "+e.getMessage());}
            });
        }catch(Exception e){runOnUiThread(()->{if(!cancelled)stop("Export stopped: "+e.getMessage());});}},"DashSocialExportPrepare").start();
    }
    void save(File rendered,int current) {
        progress.setText("Saving video "+(current+1));
        new Thread(()->{Uri target=null;try{
            check();ContentValues values=new ContentValues();String type=project.merge?"merged":"clip-"+(current+1);
            values.put(MediaStore.Video.Media.DISPLAY_NAME,"RideDeck-"+type+"-"+System.currentTimeMillis()+".mp4");values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/RideDeckDash/Social");values.put(MediaStore.Video.Media.IS_PENDING,1);
            target=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);if(target==null)throw new IOException("Gallery unavailable");
            try(InputStream in=new FileInputStream(rendered);OutputStream out=getContentResolver().openOutputStream(target)) {
                if(out==null)throw new IOException("Gallery unavailable");byte[] buffer=new byte[65536];int n;long checkAt=0;
                while((n=in.read(buffer))!=-1){if(cancelled)throw new IOException("Export cancelled");long now=SystemClock.elapsedRealtime();if(now>=checkAt){check();checkAt=now+500;}out.write(buffer,0,n);}
            }
            final Uri uri=target;
            // Publication and lifecycle cancellation are serialized on the activity's main thread.
            runOnUiThread(()->{try{
                check();if(isFinishing()||isDestroyed()||current!=job)throw new IOException("Export cancelled");
                ContentValues done=new ContentValues();done.put(MediaStore.Video.Media.IS_PENDING,0);
                if(getContentResolver().update(uri,done,null,null)<=0)throw new IOException("Could not finish gallery copy");
                saved.add(uri);showShare();job++;next();
            }catch(Exception e){getContentResolver().delete(uri,null,null);if(!cancelled)stop("Saving stopped: "+e.getMessage());}});
        }catch(Exception e){if(target!=null)getContentResolver().delete(target,null,null);runOnUiThread(()->{if(!cancelled)stop("Saving stopped: "+e.getMessage());});}
        finally{rendered.delete();}},"DashSocialGallery").start();
    }
    void showShare(){share.setVisibility(saved.isEmpty()?View.GONE:View.VISIBLE);}
    void share() {
        if(saved.isEmpty())return;
        Intent intent=new Intent(saved.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE).setType("video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(saved.size()==1)intent.putExtra(Intent.EXTRA_STREAM,saved.get(0));else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,new ArrayList<>(saved));
        ClipData data=ClipData.newUri(getContentResolver(),"RideDeck social video",saved.get(0));for(int i=1;i<saved.size();i++)data.addItem(new ClipData.Item(saved.get(i)));intent.setClipData(data);
        startActivity(Intent.createChooser(intent,"Share social videos"));
    }
    void check()throws IOException{if(cancelled)throw new IOException("Export cancelled");if(RecordingService.running||RecordingService.finalizing)throw new IOException("Stop recording before exporting");}
    void stop(String message){cancelled=true;handler.removeCallbacksAndMessages(null);if(transformer!=null){transformer.cancel();transformer=null;}if(output!=null)output.delete();progress.setText(message+"\n"+saved.size()+" completed copies kept.");showShare();}
    final Runnable tick=new Runnable(){public void run(){if(cancelled||completed)return;try{check();if(transformer!=null){ProgressHolder holder=new ProgressHolder();if(transformer.getProgress(holder)==Transformer.PROGRESS_STATE_AVAILABLE)progress.setText("Creating video "+(job+1)+" of "+(project.merge?1:project.clips.size())+" · "+holder.progress+"%");}handler.postDelayed(this,500);}catch(Exception e){stop("Export stopped: "+e.getMessage());}}};
    @Override protected void onSaveInstanceState(Bundle b){ArrayList<String> uris=new ArrayList<>();for(Uri uri:saved)uris.add(uri.toString());b.putStringArrayList("saved",uris);super.onSaveInstanceState(b);}
    @Override protected void onStop(){if(!completed){android.util.Log.i("DashSocialExport","Export stopped; changingConfiguration="+isChangingConfigurations()+" finishing="+isFinishing());stop("Export cancelled");}super.onStop();}
    @Override protected void onDestroy(){if(!completed){cancelled=true;if(transformer!=null)transformer.cancel();if(output!=null)output.delete();}handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
