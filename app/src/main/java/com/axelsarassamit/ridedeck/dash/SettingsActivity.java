package com.axelsarassamit.ridedeck.dash;
import android.app.*;import android.os.*;import android.content.*;import android.media.*;import android.widget.*;import java.util.*;
public final class SettingsActivity extends Activity {
 DashSettings settings;LinearLayout page;
 public void onCreate(Bundle b){RideAppearance.prepare(this); super.onCreate(b);settings=new DashSettings(this);page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
  String section=getIntent().getStringExtra("section");if(section==null)section="video";
  DashUi.text(this,page,section.equals("audio")?"Microphones and audio":section.equals("automation")?"Drive automation":"Video and storage",28);
  DashUi.text(this,page,"Changes apply to the next recording. Unsupported camera combinations stop with a clear message. Protected clips count toward your storage limit.",16);
  if(section.equals("automation")){
  choice("Automatic ride recording","autostart",new int[]{1,0},new String[]{"On","Off"},1);
  DashUi.text(this,page,"Charger grace: 5 minutes",22);
  DashUi.text(this,page,"Recording can continue on battery for up to five minutes after unplugging, even while moving. Reconnecting resets the grace period. Below 30% battery, unplugged recording stops immediately. The timer is shared across recording restarts; GPS is not needed. If Dash first opens without a charger, its five-minute allowance begins then.",15);
  DashUi.text(this,page,"Starts once for a new RideDeck session or after GPS confirms at least 8 km/h for 3 seconds while Dash is open. Manual Stop suppresses restart until a new session or a stationary-to-moving transition. Reconnecting power alone does not start recording. Editing, export and backup still require charging or at least 50% battery.",15);
  }
  if(section.equals("video")){
  DashUi.button(this,page,"Rear lens: "+settings.text("rearLensLabel","Automatic main camera"),v->rearLenses());
  DashUi.button(this,page,"Video overlays",v->startActivity(new Intent(this,OverlaySettingsActivity.class)));
  DashUi.text(this,page,"Video and storage",24);
  DashUi.button(this,page,"Presets",v->new AlertDialog.Builder(this).setTitle("Choose a preset").setItems(new String[]{"Balanced: 720p / 6 Mbps / 128 MB / 8 GB","Clear detail: 1080p / 12 Mbps / 256 MB / 16 GB","Long ride: 720p / 4 Mbps / 64 MB / 4 GB","High quality: 1080p / 16 Mbps / 512 MB / 32 GB"},(d,i)->{settings.preset(i);recreate();}).show());
  choice("Resolution","height",new int[]{720,1080},new String[]{"720p","1080p"},720);
  choice("Dual camera workload","rearPriority",new int[]{1,0},new String[]{"Rear priority: front 720p, at most 6 Mbps","Same quality on both cameras"},1);
  DashUi.text(this,page,"Rear priority reduces front-camera encoding load when recording both cameras at 1080p. Rear quality stays at your chosen setting. Hidden previews stop generating camera frames. Actual camera cadence is shown while recording and saved with ride data. Outdoor timing and heat still need validation.",15);
  choice("Video bitrate per camera","bitrate",new int[]{4,6,8,12,16,24},new String[]{"4 Mbps","6 Mbps","8 Mbps","12 Mbps","16 Mbps","24 Mbps"},6);
  choice("Clip size per camera","clip",new int[]{64,128,256,512,1024,2048},new String[]{"64 MB","128 MB","256 MB","512 MB","1 GB","2 GB"},128);
  choice("Recording storage limit","quota",new int[]{2,4,8,16,32,64,128},new String[]{"2 GB","4 GB","8 GB","16 GB","32 GB","64 GB","128 GB"},8);
  choice("Keep phone space free","reserve",new int[]{256,512,1024,2048,4096},new String[]{"256 MB","512 MB","1 GB","2 GB","4 GB"},512);
  choice("Protection before and after button press","protect",new int[]{15,30,60,120,300},new String[]{"15 seconds","30 seconds","1 minute","2 minutes","5 minutes"},60);
  choice("Stabilization","stabilization",new int[]{1,0},new String[]{"Best verified available","Off"},1);
  choice("Motion data","motion_data",new int[]{1,0},new String[]{"Save for future stabilization","Off"},1);
  choice("Rear camera focus","rearFocus",new int[]{1,2,0},new String[]{"Road: distant focus locked","Road: approximately 5 metres locked","Continuous autofocus"},1);
  choice("Front camera focus","frontFocus",new int[]{0,1,2},new String[]{"Continuous autofocus","Road: distant focus locked","Road: approximately 5 metres locked"},0);
  DashUi.text(this,page,"Road focus keeps nearby mirrors and screens from pulling focus. Rear camera defaults to distant focus; front camera keeps autofocus for rider footage. Preview and recording use the same setting. Fixed-focus cameras keep their built-in focus. If a camera cannot lock focus, Dash asks you to select another lens or explicitly choose autofocus. Distance accuracy depends on the lens; check the road in the preview before riding.",16);
  choice("Camera view refresh","preview",new int[]{5,10,2},new String[]{"5 fps (balanced)","10 fps (smooth)","2 fps (low power)"},5);
  }
  if(section.equals("audio")){
  DashUi.text(this,page,"Automatic recording uses the phone microphone so Spotify and other music can keep their normal Bluetooth sound. Wired and USB microphones remain selectable. The chosen input is checked during recording.",15);
  DashUi.button(this,page,"Sound check: phone and Bluetooth",v->startActivity(new Intent(this,AudioCheckActivity.class)));
  choice("Export sound preset","audio_cutoff",new int[]{0,50,120,180},new String[]{"Original audio","Natural ride (50 Hz)","Wind rumble reduction (120 Hz)","Voice focus (180 Hz)"},0);
  choice("Custom low-cut frequency","audio_cutoff",new int[]{0,30,50,80,120,180,250},new String[]{"Off","30 Hz","50 Hz","80 Hz","120 Hz","180 Hz","250 Hz"},0);
  DashUi.text(this,page,"Low-cut filtering applies to overlay and social exported copies only. Originals keep their sound. Higher settings remove more wind rumble and engine bass. It does not remove all wind or restore clipped sound. For natural capture, Unprocessed (if supported) or Voice recognition can avoid some automatic processing. Video capture still uses one input; a phone + Bluetooth mix is not enabled by the sound check.",16);
  DashUi.button(this,page,"Microphone: "+AudioInputs.savedLabel(this,settings),v->microphones());
  choice("Microphone processing","source",new int[]{1,5,6,9},new String[]{"Standard microphone","Camera recording","Voice recognition","Unprocessed if supported"},1);
  DashUi.text(this,page,"Audio is embedded in the rear video, or front video in front-only mode. Bluetooth microphone recording can make Spotify quiet and muffled for the entire recording. Choose the phone microphone for clear music. Yamaha voice buttons can use the headset briefly while Dash pauses. A phone + headset mix is not available. The dashboard reports the actual recording input.",16);
  }
  DashUi.text(this,page,"Video requests the steadiest supported 30 fps camera range. Dual 1080p and hardware stabilization depend on the phone. Preview conversion runs separately from recording. From Recordings, overlay and social exports offer Gentle, Strong and Maximum motion smoothing with extra crop. Originals stay unchanged. Image smoothing cannot remove existing vibration blur or all rolling-shutter distortion. New recordings can save gyroscope and camera timing data for future calibrated stabilization. The editor does not use gyro correction yet. Native stabilization remains unchanged. Automatic incident detection is not enabled.",16);
  DashUi.button(this,page,"Back",v->finish());
 }
 void choice(String title,String key,int[] values,String[] labels,int fallback){int current=settings.number(key,fallback),index=0;for(int i=0;i<values.length;i++)if(values[i]==current)index=i;
  DashUi.button(this,page,title+": "+labels[index],v->new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,i)->{settings.put(key,values[i]);settings.put("preset","Custom");recreate();}).show());}
 void microphones(){
  if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},41);return;}
  AudioDeviceInfo[] devices=getSystemService(AudioManager.class).getDevices(AudioManager.GET_DEVICES_INPUTS);AudioInputs.discover(devices);List<String> labels=new ArrayList<>(),keys=new ArrayList<>();labels.add("Off");keys.add("off");labels.add("Automatic phone microphone");keys.add("auto");
  labels.add("Bluetooth microphone (reduces music quality)");keys.add("headset");
  for(AudioDeviceInfo device:devices){if(!AudioInputs.selectable(device))continue;labels.add(AudioInputs.label(device));keys.add(AudioInputs.key(device));}
  if(Build.VERSION.SDK_INT>=31)for(AudioDeviceInfo device:getSystemService(AudioManager.class).getAvailableCommunicationDevices()){if(!HeadsetRoute.bluetooth(device)||keys.contains(AudioInputs.key(device)))continue;labels.add(AudioInputs.label(device));keys.add(AudioInputs.key(device));}
  new AlertDialog.Builder(this).setTitle("Choose recording input").setItems(labels.toArray(new String[0]),(d,i)->{
   Runnable save=()->{settings.put("audio",keys.get(i));settings.put("audioLabel",labels.get(i));recreate();};
   if(RecordingAudioPolicy.bluetooth(keys.get(i)))new AlertDialog.Builder(this).setTitle("Bluetooth microphone reduces music quality").setMessage("Recording through the headset microphone can make Spotify quiet and muffled. Use the phone microphone to keep clear music.").setPositiveButton("Use Bluetooth microphone",(warning,which)->save.run()).setNegativeButton("Keep current input",null).show();else save.run();
  }).show();
 }
 void rearLenses(){try{
  android.hardware.camera2.CameraManager manager=getSystemService(android.hardware.camera2.CameraManager.class);
  List<String> names=new ArrayList<>(),ids=new ArrayList<>();names.add("Automatic main camera");ids.add("auto");
  for(String id:manager.getCameraIdList()){
   android.hardware.camera2.CameraCharacteristics c=manager.getCameraCharacteristics(id);
   if(!Objects.equals(c.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING),android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK))continue;
   names.add(RearLenses.label(c));ids.add(id);
  }
  new AlertDialog.Builder(this).setTitle("Rear lenses exposed by Android").setItems(names.toArray(new String[0]),(dialog,index)->{settings.put("rearLens",ids.get(index));settings.put("rearLensLabel",names.get(index));settings.put("preset","Custom");recreate();}).show();
  Toast.makeText(this,"Rear + front availability depends on the phone. Unsupported pairs stop before recording.",Toast.LENGTH_LONG).show();
 }catch(Exception error){Toast.makeText(this,"Cannot inspect rear lenses",Toast.LENGTH_LONG).show();}}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41)microphonesWithoutPrompt();}
 void microphonesWithoutPrompt(){if(Build.VERSION.SDK_INT<31||checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED)microphones();else Toast.makeText(this,"Bluetooth permission denied. Phone and wired inputs remain available on reopening settings.",Toast.LENGTH_LONG).show();}
}
