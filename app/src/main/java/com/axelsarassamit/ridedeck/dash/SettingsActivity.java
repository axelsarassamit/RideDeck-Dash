package com.axelsarassamit.ridedeck.dash;
import android.app.*;import android.os.*;import android.content.*;import android.media.*;import android.widget.*;import java.util.*;
public final class SettingsActivity extends Activity {
 DashSettings settings;LinearLayout page;
 public void onCreate(Bundle b){super.onCreate(b);settings=new DashSettings(this);page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
  String section=getIntent().getStringExtra("section");if(section==null)section="video";
  DashUi.text(this,page,section.equals("audio")?"Microphones and audio":section.equals("automation")?"Drive automation":"Video and storage",28);
  DashUi.button(this,page,"Setup menu",v->new AlertDialog.Builder(this).setTitle("Ride setup").setItems(new String[]{"Video and storage","Microphones and audio","Drive automation","Video overlays"},(d,i)->{if(i==3){startActivity(new Intent(this,OverlaySettingsActivity.class));}else{getIntent().putExtra("section",new String[]{"video","audio","automation"}[i]);recreate();}}).show());
  DashUi.text(this,page,"Changes apply to the next recording. Unsupported camera combinations stop with a clear message. Protected clips count toward your storage limit.",16);
  if(section.equals("automation")){
  choice("Automatic ride recording","autostart",new int[]{1,0},new String[]{"On","Off"},1);
  choice("Stop when unplugged and stationary","autostop",new int[]{60,120,30,0},new String[]{"60 seconds","2 minutes","30 seconds","Off"},60);
  DashUi.text(this,page,"Starts once for a new RideDeck session or after GPS confirms at least 8 km/h for 3 seconds while Dash is open. Manual Stop suppresses restart until a new session or a stationary-to-moving transition. Unplugged recording below 50% still stops immediately. Missing GPS does not count as stationary.",15);
  }
  if(section.equals("video")){
  DashUi.button(this,page,"Video overlays",v->startActivity(new Intent(this,OverlaySettingsActivity.class)));
  DashUi.text(this,page,"Video and storage",24);
  DashUi.button(this,page,"Presets",v->new AlertDialog.Builder(this).setTitle("Choose a preset").setItems(new String[]{"Balanced: 720p / 6 Mbps / 128 MB / 8 GB","Clear detail: 1080p / 12 Mbps / 256 MB / 16 GB","Long ride: 720p / 4 Mbps / 64 MB / 4 GB","High quality: 1080p / 16 Mbps / 512 MB / 32 GB"},(d,i)->{settings.preset(i);recreate();}).show());
  choice("Resolution","height",new int[]{720,1080},new String[]{"720p","1080p"},720);
  choice("Video bitrate per camera","bitrate",new int[]{4,6,8,12,16,24},new String[]{"4 Mbps","6 Mbps","8 Mbps","12 Mbps","16 Mbps","24 Mbps"},6);
  choice("Clip size per camera","clip",new int[]{64,128,256,512,1024,2048},new String[]{"64 MB","128 MB","256 MB","512 MB","1 GB","2 GB"},128);
  choice("Recording storage limit","quota",new int[]{2,4,8,16,32,64,128},new String[]{"2 GB","4 GB","8 GB","16 GB","32 GB","64 GB","128 GB"},8);
  choice("Keep phone space free","reserve",new int[]{256,512,1024,2048,4096},new String[]{"256 MB","512 MB","1 GB","2 GB","4 GB"},512);
  choice("Protection before and after button press","protect",new int[]{15,30,60,120,300},new String[]{"15 seconds","30 seconds","1 minute","2 minutes","5 minutes"},60);
  choice("Stabilization","stabilization",new int[]{1,0},new String[]{"Best verified available","Off"},1);
  choice("Camera view refresh","preview",new int[]{5,10,2},new String[]{"5 fps (balanced)","10 fps (smooth)","2 fps (low power)"},5);
  }
  if(section.equals("audio")){
  DashUi.button(this,page,"Microphone: "+AudioInputs.savedLabel(this,settings),v->microphones());
  choice("Microphone processing","source",new int[]{1,5,6,9},new String[]{"Standard microphone","Camera recording","Voice recognition","Unprocessed if supported"},1);
  DashUi.text(this,page,"Audio is embedded in the rear video, or front video in front-only mode. Android chooses the physical capsules within a phone input. A phone + headset mix is not available in this version. Headsets must be connected and exposed as an input. Bluetooth requires Android permission and communication routing; quality may be lower. The dashboard reports the actual routed input.",16);
  }
  DashUi.text(this,page,"Video targets 30 fps. Dual 1080p and stabilization depend on the phone. Preview is a reduced-resolution alignment view; saved video uses your selected resolution and bitrate. Gyro post-processing and automatic incident detection are not enabled.",16);
  DashUi.button(this,page,"Back",v->finish());
 }
 void choice(String title,String key,int[] values,String[] labels,int fallback){int current=settings.number(key,fallback),index=0;for(int i=0;i<values.length;i++)if(values[i]==current)index=i;
  DashUi.button(this,page,title+": "+labels[index],v->new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,i)->{settings.put(key,values[i]);settings.put("preset","Custom");recreate();}).show());}
 void microphones(){
  if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},41);return;}
  AudioDeviceInfo[] devices=getSystemService(AudioManager.class).getDevices(AudioManager.GET_DEVICES_INPUTS);AudioInputs.discover(devices);List<String> labels=new ArrayList<>(),keys=new ArrayList<>();labels.add("Off");keys.add("off");labels.add("Automatic input");keys.add("auto");
  for(AudioDeviceInfo device:devices){if(!AudioInputs.selectable(device))continue;labels.add(AudioInputs.label(device));keys.add(AudioInputs.key(device));}
  new AlertDialog.Builder(this).setTitle("Choose recording input").setItems(labels.toArray(new String[0]),(d,i)->{settings.put("audio",keys.get(i));settings.put("audioLabel",labels.get(i));recreate();}).show();
 }
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41)microphonesWithoutPrompt();}
 void microphonesWithoutPrompt(){if(Build.VERSION.SDK_INT<31||checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED)microphones();else Toast.makeText(this,"Bluetooth permission denied. Phone and wired inputs remain available on reopening settings.",Toast.LENGTH_LONG).show();}
}
