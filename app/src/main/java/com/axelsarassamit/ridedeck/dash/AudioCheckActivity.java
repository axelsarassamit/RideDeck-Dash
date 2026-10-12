package com.axelsarassamit.ridedeck.dash;
import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.media.*;import android.provider.MediaStore;import android.widget.*;import android.view.WindowManager;
import java.io.*;import java.util.*;import org.json.*;

/** Local test samples only. Not a claim of supported live video mixing. */
public final class AudioCheckActivity extends Activity {
    LinearLayout page,results;TextView status;final List<Button> starts=new ArrayList<>();volatile boolean cancelled,running;int pendingMode;
    AudioCheckRecorder.Result last;MediaPlayer player;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DashUi.text(this,page,"Sound check",28);
        DashUi.text(this,page,"Eight-second local samples. Speak a short sentence for the voice test. During the combined test, speak into the headset and gently tap the phone case away from the headset to distinguish inputs. No samples are shared automatically.",16);
        DashUi.text(this,page,"This checks actual microphone routing, signal and Android silencing. Different phone capsules are not always exposed separately. A successful check does not enable live video mixing. Outdoor wind and engine sound still need a riding comparison.",15);
        status=DashUi.text(this,page,"Ready. Stop video recording before testing.",18);
        starts.add(DashUi.button(this,page,"Test phone sound",v->begin(0)));
        starts.add(DashUi.button(this,page,"Test Bluetooth voice",v->begin(1)));
        starts.add(DashUi.button(this,page,"Test phone + Bluetooth together",v->begin(2)));
        DashUi.button(this,page,"Stop sound check",v->{cancelled=true;status.setText("Stopping microphones");});
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);page.addView(results);
        DashUi.button(this,page,"Back",v->finish());
    }
    void begin(int mode){
        if(running)return;if(RecordingService.running||RecordingService.finalizing){status.setText("Stop video recording first");return;}
        if(!power()){status.setText("Connect charger or charge to 50%");return;}
        List<String> permissions=new ArrayList<>();if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)permissions.add(android.Manifest.permission.RECORD_AUDIO);
        if(mode!=0&&Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        if(!permissions.isEmpty()){pendingMode=mode;requestPermissions(permissions.toArray(new String[0]),73);return;}
        stopPlayback();cancelled=false;running=true;results.removeAllViews();for(Button b:starts)b.setEnabled(false);status.setText(mode==0?"Preparing phone microphone":"Connecting headset voice microphone");
        new Thread(()->{try{
            AudioCheckRecorder.Result recorded=AudioCheckRecorder.run(this,mode,()->cancelled||!power()||RecordingService.running||RecordingService.finalizing,message->runOnUiThread(()->{if(!cancelled)status.setText(message);}));
            last=recorded;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&!cancelled)show(recorded);});
        }catch(Exception error){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())status.setText("Sound check stopped: "+error.getMessage());});}
        finally{running=false;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())for(Button b:starts)b.setEnabled(true);});}},"DashSoundCheck").start();
    }
    void show(AudioCheckRecorder.Result result){try{
        status.setText(result.report.getString("meaning"));JSONArray tracks=result.report.getJSONArray("tracks");
        for(int i=0;i<tracks.length();i++){JSONObject track=tracks.getJSONObject(i);DashUi.text(this,results,track.getString("track")+": requested "+track.getString("preferred")+"\nActual: "+track.getString("actual")+"\n"+(track.getBoolean("silenced")?"Android silenced this track":track.getBoolean("signalPresent")?"Audio signal present":"No usable signal confirmed")+" · "+track.getInt("rate")+" Hz · "+track.getInt("channels")+" channel(s)"+(track.getLong("clippedSamples")>0?"\nSome samples clipped; filtering cannot restore them":""),16);}
        File[] files=result.folder.listFiles((folder,name)->name.endsWith(".wav"));if(files!=null){Arrays.sort(files,Comparator.comparing(File::getName));for(File file:files)DashUi.button(this,results,"Listen: "+file.getName().replace(".wav","").replace('-',' '),v->play(file));}
        DashUi.text(this,results,"Natural: 50 Hz low-cut. Wind: 120 Hz low-cut, with more engine bass removed. Voice uses Android communication processing and an available voice noise suppressor. Originals are retained.",15);
        DashUi.button(this,results,"Stop playback",v->stopPlayback());DashUi.button(this,results,"Save samples and report to Downloads",v->save(result));
    }catch(JSONException error){status.setText("Sound check report unavailable");}}
    void play(File file){stopPlayback();try{player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());player.setDataSource(file.getPath());player.prepare();player.start();player.setOnCompletionListener(p->stopPlayback());}catch(Exception error){stopPlayback();status.setText("Sample playback unavailable");}}
    void stopPlayback(){if(player!=null){player.release();player=null;}}
    void save(AudioCheckRecorder.Result result){if(Build.VERSION.SDK_INT<29){status.setText("Downloads export needs Android 10 or newer");return;}
        new Thread(()->{try{File[] files=result.folder.listFiles();if(files==null)throw new IOException("Samples unavailable");for(File file:files){android.net.Uri uri=null;try{ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,file.getName());values.put(MediaStore.MediaColumns.MIME_TYPE,file.getName().endsWith(".wav")?"audio/wav":"application/json");values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/RideDeckDash/AudioChecks/"+result.folder.getName());values.put(MediaStore.MediaColumns.IS_PENDING,1);uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Downloads unavailable");try(InputStream input=new FileInputStream(file);OutputStream output=getContentResolver().openOutputStream(uri)){if(output==null)throw new IOException("Downloads unavailable");byte[] bytes=new byte[65536];int n;while((n=input.read(bytes))!=-1)output.write(bytes,0,n);}values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(uri,values,null,null);}catch(Exception error){if(uri!=null)getContentResolver().delete(uri,null,null);throw error;}}
        runOnUiThread(()->{if(!isDestroyed())status.setText("Saved in Downloads / RideDeckDash / AudioChecks / "+result.folder.getName());});}catch(Exception error){runOnUiThread(()->{if(!isDestroyed())status.setText("Sample export failed: "+error.getMessage());});}},"DashSoundSamples").start();
    }
    boolean power(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b!=null&&RecordingGate.evaluate(b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0,RecordingGate.batteryPercent(b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),b.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),true,false)==RecordingGate.Result.ALLOWED;}
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] granted){super.onRequestPermissionsResult(request,names,granted);if(request==73){for(int result:granted)if(result!=PackageManager.PERMISSION_GRANTED){status.setText("Microphone and headset permissions are required for this test");return;}begin(pendingMode);}}
    @Override protected void onStop(){cancelled=true;stopPlayback();super.onStop();}
}
