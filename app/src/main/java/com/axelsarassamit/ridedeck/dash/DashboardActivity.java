package com.axelsarassamit.ridedeck.dash;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.location.Location;import android.os.*;import android.widget.*;import android.view.*;import java.util.*;
public final class DashboardActivity extends Activity {
 TextView status,speed,details,viewLabel;Button start,protect,stop,viewButton;ImageView rear,front;DashSettings settings;AlignmentPreview alignment;
 String mode="dual";boolean visible,pendingRecord;long storageChecked;String storageSummary="";final Handler ui=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){render();ui.postDelayed(this,200);}};
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle b){super.onCreate(b);settings=new DashSettings(this);mode=settings.text("mode","dual");
  boolean wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  LinearLayout root=DashUi.page(this),main=new LinearLayout(this),controls=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);controls.setOrientation(LinearLayout.VERTICAL);
  if(wide){root.setOrientation(LinearLayout.HORIZONTAL);ScrollView left=new ScrollView(this),right=new ScrollView(this);left.addView(main);right.addView(controls);root.addView(left,new LinearLayout.LayoutParams(0,-1,2.3f));root.addView(right,new LinearLayout.LayoutParams(0,-1,1));setContentView(root);}
  else{ScrollView scroll=new ScrollView(this);root.addView(main);root.addView(controls);scroll.addView(root);setContentView(scroll);}
  DashUi.text(this,main,"RideDeck Dash",26);status=DashUi.text(this,main,"Ready",20);
  LinearLayout cameras=new LinearLayout(this);cameras.setOrientation(LinearLayout.HORIZONTAL);main.addView(cameras,new LinearLayout.LayoutParams(-1,dp(wide?190:285)));
  rear=image(cameras,"Rear camera live view",2);front=image(cameras,"Front camera live view",1);
  viewLabel=DashUi.text(this,main,"Camera view is off",14);viewLabel.setTextColor(DashUi.BLUE);
  speed=DashUi.text(this,main,"GPS",27);details=DashUi.text(this,main,"",15);
  LinearLayout actions=controls; if(!wide){actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);controls.addView(actions);}
  start=DashUi.button(this,actions,"Start recording",v->request(true));protect=DashUi.button(this,actions,"Protect clip",v->command("PROTECT"));stop=DashUi.button(this,actions,"Stop recording",v->command("STOP"));
  if(!wide)for(Button button:new Button[]{start,protect,stop}){button.setTextSize(14);button.setLayoutParams(new LinearLayout.LayoutParams(0,dp(64),1));}
  viewButton=DashUi.button(this,controls,"Camera view",v->{if(alignment!=null)closeAlignment(null);else request(false);});
  DashUi.button(this,controls,"Choose cameras",v->{if(RecordingService.running)return;new AlertDialog.Builder(this).setTitle("Cameras").setItems(new String[]{"Rear + front","Rear only","Front only"},(d,i)->{mode=new String[]{"dual","rear","front"}[i];settings.put("mode",mode);closeAlignment(null);}).show();});
  rear.setOnClickListener(v->fullView("rear"));front.setOnClickListener(v->fullView("front"));
  DashUi.button(this,controls,"Presets and options",v->{if(RecordingService.running){Toast.makeText(this,"Stop recording to change recording options",Toast.LENGTH_LONG).show();return;}closeAlignment(null);startActivity(new Intent(this,SettingsActivity.class));});
  DashUi.button(this,controls,"Saved clips",v->startActivity(new Intent(this,ClipsActivity.class)));DashUi.button(this,controls,"Map and route",v->startActivity(new Intent(this,MapActivity.class)));
  DashUi.button(this,controls,"Cameras and updates",v->{if(!RecordingService.running){closeAlignment(null);startActivity(new Intent(this,CapabilityActivity.class));}});
 }
 ImageView image(LinearLayout parent,String description,float weight){ImageView image=new ImageView(this);image.setBackgroundColor(android.graphics.Color.BLACK);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(description);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,weight);lp.setMargins(3,3,3,3);parent.addView(image,lp);return image;}
 void fullView(String camera){ImageView big=new ImageView(this);big.setScaleType(ImageView.ScaleType.FIT_CENTER);big.setBackgroundColor(android.graphics.Color.BLACK);big.setMinimumHeight(dp(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?200:500));AlertDialog dialog=new AlertDialog.Builder(this).setTitle(camera+" live view").setView(big).setPositiveButton("Close",null).create();
  Runnable update=new Runnable(){public void run(){if(!dialog.isShowing())return;big.setImageBitmap("rear".equals(camera)?PreviewFrames.rear:PreviewFrames.front);ui.postDelayed(this,200);}};dialog.show();ui.post(update);}
 void render(){
  PreviewFrames.rotation=getWindowManager().getDefaultDisplay().getRotation();PreviewFrames.interval=1000/settings.number("preview",5);
  Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int percent=battery==null?-1:RecordingGate.batteryPercent(battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1));boolean plugged=battery!=null&&battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
  boolean power=RecordingGate.evaluate(plugged,percent,true,false)==RecordingGate.Result.ALLOWED;if(!power&&alignment!=null)closeAlignment(null);
  if(RecordingService.running||alignment!=null)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  long seconds=Math.max(0,(SystemClock.elapsedRealtime()-RecordingService.startedAt)/1000);status.setText(RecordingService.running&&!RecordingService.finalizing?String.format(Locale.ROOT,"● REC %02d:%02d · %dp / 30 fps",seconds/60,seconds%60,settings.height()):RecordingService.state);
  status.setTextColor(RecordingService.running?android.graphics.Color.rgb(255,100,100):DashUi.WHITE);
  boolean live=RecordingService.running||alignment!=null;rear.setImageBitmap(live?PreviewFrames.rear:null);front.setImageBitmap(live?PreviewFrames.front:null);viewLabel.setText(live?PreviewFrames.message+" · rear / front · tap to enlarge":"Camera view off · tap Camera view to align before recording");
  Location p=RecordingService.position;boolean fresh=p!=null&&SystemClock.elapsedRealtimeNanos()-p.getElapsedRealtimeNanos()<6000000000L;speed.setText(fresh&&p.hasSpeed()?String.format(Locale.ROOT,"%.0f km/h · GPS",p.getSpeed()*3.6):"GPS speed unavailable");
  if(SystemClock.elapsedRealtime()-storageChecked>3000){long used=0;for(java.io.File file:new ClipStore(this).clips())used+=file.length();storageSummary=String.format(Locale.ROOT,"%.1f GB used · %.1f GB phone free",used/1073741824.0,getFilesDir().getUsableSpace()/1073741824.0);storageChecked=SystemClock.elapsedRealtime();}
  details.setText(RecordingService.cameraStatus+"\n"+settings.text("preset","Balanced")+" · "+settings.number("clip",128)+" MB clips · "+settings.number("quota",8)+" GB limit\n"+storageSummary+"\n"+RecordingService.audioStatus+"\nBattery "+percent+"%"+(plugged?" · charging":"")+" · "+(DashBridgeProvider.SESSIONS.isActive(SystemClock.elapsedRealtime())?"RideDeck session active":"Standalone")+(power?"":"\nConnect charger or charge to 50%"));
  start.setEnabled(power&&!RecordingService.running&&!RecordingService.finalizing);protect.setEnabled(RecordingService.running&&!RecordingService.finalizing);stop.setEnabled(RecordingService.running&&!RecordingService.finalizing);viewButton.setEnabled(power&&!RecordingService.running&&!RecordingService.finalizing);viewButton.setText(alignment==null?"Camera view":"Close camera view");
 }
 void request(boolean recording){pendingRecord=recording;ArrayList<String> permissions=new ArrayList<>();if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.CAMERA);
  if(recording){if(!"off".equals(settings.text("audio","off"))&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.RECORD_AUDIO);
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.POST_NOTIFICATIONS);}
  if(!permissions.isEmpty()){requestPermissions(permissions.toArray(new String[0]),17);return;}ready();}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==17&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){if(pendingRecord&&!"off".equals(settings.text("audio","off"))&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Choose Audio off or allow microphone recording",Toast.LENGTH_LONG).show();return;}ready();}}
 void ready(){if(!visible)return;if(pendingRecord)closeAlignment(this::begin);else{PreviewFrames.rear=null;PreviewFrames.front=null;PreviewFrames.message="Opening cameras";alignment=new AlignmentPreview(this);alignment.start(mode);}}
 void closeAlignment(Runnable after){AlignmentPreview old=alignment;alignment=null;if(old!=null)old.close(after);else if(after!=null)after.run();}
 void begin(){if(!visible||RecordingService.finalizing)return;PreviewFrames.rear=null;PreviewFrames.front=null;Intent i=new Intent(this,RecordingService.class).setAction("START").putExtra("mode",mode).putExtra("rotation",getWindowManager().getDefaultDisplay().getRotation());try{startForegroundService(i);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
 void command(String action){if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(action));}
 protected void onStart(){super.onStart();visible=true;PreviewFrames.enabled=true;ui.post(refresh);}
 protected void onStop(){visible=false;PreviewFrames.enabled=false;closeAlignment(null);ui.removeCallbacks(refresh);super.onStop();}
}
