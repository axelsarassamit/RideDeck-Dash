package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.content.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import org.json.*;

/** Explicit local backup of original clips overlapping a chosen time window. */
public final class RecordingBackupActivity extends Activity {
    EditText day,from,to;CheckBox rear;TextView status;Button save;volatile boolean cancelled;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        DashUi.text(this,page,"Back up recordings",26);
        DashUi.text(this,page,"Copy complete originals that overlap this local time window, plus their ride logs. Originals stay in Dash. Copies may include footage before and after the selected times.",16);
        day=input(page,"Date · yyyy-MM-dd",new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date()));
        from=input(page,"From · HH:mm","00:00");to=input(page,"Until · HH:mm","23:59");
        rear=new CheckBox(this);rear.setText("Rear camera only");rear.setTextColor(DashUi.WHITE);rear.setChecked(true);page.addView(rear);
        status=DashUi.text(this,page,"Choose the window, then save a local copy.",16);
        save=DashUi.button(this,page,"Save originals and logs",v->backup());
        DashUi.button(this,page,"Back / cancel",v->finish());
    }
    EditText input(LinearLayout page,String label,String value){DashUi.text(this,page,label,15);EditText field=new EditText(this);field.setSingleLine(true);field.setTextColor(DashUi.WHITE);field.setText(value);page.addView(field);return field;}
    void backup(){
        if(Build.VERSION.SDK_INT<29){status.setText("Local backup requires Android 10 or newer");return;}
        if(RecordingService.running||RecordingService.finalizing){status.setText("Stop recording before backing up originals");return;}
        final long start,end;final boolean rearOnly=rear.isChecked();
        try{SimpleDateFormat parser=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT);parser.setLenient(false);start=parse(parser,day.getText()+" "+from.getText());end=parse(parser,day.getText()+" "+to.getText());if(end<=start)throw new IllegalArgumentException();}
        catch(Exception error){status.setText("Enter a valid date and a later end time, for example 2026-10-13, 13:57, 16:51");return;}
        save.setEnabled(false);String folder="Download/RideDeckDash/Backup-"+System.currentTimeMillis();status.setText("Finding original clips…");
        new Thread(()->{try{
            List<File> chosen=new ArrayList<>();Set<File> rides=new LinkedHashSet<>();long bytes=0;JSONArray manifest=new JSONArray();
            for(File clip:new ClipStore(this).clips()){
                if(cancelled)throw new IOException("Backup cancelled");
                if(!ClipStore.finalized(clip)||rearOnly&&!clip.getName().startsWith("rear-"))continue;
                long finish=clip.lastModified();if(finish<start)continue;
                long duration=0;MediaMetadataRetriever reader=new MediaMetadataRetriever();
                try{reader.setDataSource(clip.getPath());duration=Long.parseLong(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));}finally{reader.release();}
                if(finish-duration>=end)continue;
                chosen.add(clip);rides.add(clip.getParentFile());bytes+=clip.length();
                manifest.put(new JSONObject().put("ride",clip.getParentFile().getName()).put("file",clip.getName()).put("bytes",clip.length()).put("modified_ms",finish).put("duration_ms",duration));
            }
            if(chosen.isEmpty())throw new IOException("No finalized clips overlap that window");
            if(getFilesDir().getUsableSpace()<bytes+256L*1024*1024)throw new IOException("Not enough phone space for complete copies");
            int done=0;for(File clip:chosen){copy(clip,folder,"Dash-"+clip.getParentFile().getName()+"-"+clip.getName(),"video/mp4");final int count=++done,total=chosen.size();runOnUiThread(()->status.setText("Copied "+count+" / "+total+" originals"));}
            for(File ride:rides){File[] logs=ride.listFiles((dir,name)->name.endsWith(".csv"));if(logs!=null)for(File file:logs)copy(file,folder,"Dash-"+ride.getName()+"-"+file.getName(),"text/csv");}
            JSONObject report=new JSONObject().put("start_ms",start).put("end_ms",end).put("timezone",TimeZone.getDefault().getID()).put("clips",manifest);
            write(folder,"manifest.json","application/json",new ByteArrayInputStream(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            runOnUiThread(()->{if(!isDestroyed())status.setText("Backup saved: "+folder+"\n"+chosen.size()+" complete originals and ride logs. Originals kept.");});
        }catch(Exception error){runOnUiThread(()->{if(!isDestroyed())status.setText("Backup stopped: "+error.getMessage()+". Completed copies are kept; no originals were removed.");});}
        finally{runOnUiThread(()->{if(!isDestroyed())save.setEnabled(true);});}},"DashOriginalBackup").start();
    }
    static long parse(SimpleDateFormat parser,String text)throws IOException{ParsePosition position=new ParsePosition(0);Date value=parser.parse(text,position);if(value==null||position.getIndex()!=text.length())throw new IOException("Invalid time");return value.getTime();}
    void copy(File file,String folder,String name,String type)throws IOException{try(InputStream in=new FileInputStream(file)){write(folder,name,type,in);}}
    void write(String folder,String name,String type,InputStream in)throws IOException{
        if(Build.VERSION.SDK_INT<29)throw new IOException("Local backup requires Android 10 or newer");
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,type);values.put(MediaStore.MediaColumns.RELATIVE_PATH,folder);values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Downloads unavailable");
        try{try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("Downloads unavailable");byte[] buffer=new byte[65536];int count;while((count=in.read(buffer))!=-1){if(cancelled||RecordingService.running||RecordingService.finalizing)throw new IOException("Backup interrupted");out.write(buffer,0,count);}}
            values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(uri,values,null,null);
        }catch(Exception error){getContentResolver().delete(uri,null,null);throw new IOException(error.getMessage(),error);}
    }
    @Override protected void onDestroy(){cancelled=true;super.onDestroy();}
}
