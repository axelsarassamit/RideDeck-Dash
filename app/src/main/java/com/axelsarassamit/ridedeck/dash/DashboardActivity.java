package com.axelsarassamit.ridedeck.dash;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.location.*;import android.os.*;import android.widget.*;import android.view.*;import java.util.*;
public final class DashboardActivity extends Activity {
 String sampleProfile;int sampleSeconds;boolean sampleStarted;
 @Override protected void onPostResume(){super.onPostResume();String profile=getIntent().getStringExtra("sampleProfile");if(profile==null)return;getIntent().removeExtra("sampleProfile");
  if(!profile.equals("hybrid_sample")&&!profile.equals("native_sample"))return;if(RecordingService.running||RecordingService.finalizing){Toast.makeText(this,"Stop recording first",Toast.LENGTH_LONG).show();return;}
  sampleProfile=profile;sampleSeconds=getIntent().getIntExtra("sampleSeconds",120);mode="rear";autoView=false;request(true);
 }
 TextView status,speed,details,viewLabel,gpsCard,stabilizationCard;Button start,protect,stopAction;ImageView rear,front;DashSettings settings;AlignmentPreview alignment;
 LocationManager idleLocations;Location idlePosition;static boolean motionConsumed;static long consumedSession;final DriveAutomation drive=new DriveAutomation();
 final LocationListener idleGps=new LocationListener(){public void onLocationChanged(Location p){idlePosition=new Location(p);}public void onProviderDisabled(String p){idlePosition=null;}public void onProviderEnabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};
 String mode="dual";boolean visible,pendingRecord,autoView=true,starting,closingView;long storageChecked;String storageSummary="";final Handler ui=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){render();ui.postDelayed(this,200);}};
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle b){RideAppearance.prepare(this); super.onCreate(b);settings=new DashSettings(this);mode=settings.text("mode","dual");if(b!=null){motionConsumed=b.getBoolean("motionConsumed");consumedSession=b.getLong("consumedSession");sampleProfile=b.getString("samplePending");sampleSeconds=b.getInt("sampleSeconds");sampleStarted=b.getBoolean("sampleStarted");}
  boolean wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  LinearLayout shell=DashUi.page(this);shell.setPadding(dp(12),dp(8),dp(12),dp(8));setContentView(shell);
  LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);shell.addView(titleRow,new LinearLayout.LayoutParams(-1,dp(56)));
  VelocityBrandView brand=new VelocityBrandView(this);brand.setContentDescription("RideDeck Dash");titleRow.addView(brand,new LinearLayout.LayoutParams(dp(88),dp(48)));
  View spacer=new View(this);titleRow.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  Button menu=DashUi.button(this,titleRow,"Setup",v->menu());menu.setBackgroundColor(android.graphics.Color.TRANSPARENT);menu.setTextSize(13);menu.setTextColor(AppearancePalette.accent(4,RideAppearance.isDark(this)));menu.setLayoutParams(new LinearLayout.LayoutParams(dp(72),dp(48)));
  LinearLayout body=new LinearLayout(this);body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);shell.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  FrameLayout stage=new FrameLayout(this);stage.setTag("roadline_camera_frame");DashUi.card(stage,android.graphics.Color.BLACK,12);stage.setClipToOutline(true);
  body.addView(stage,wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
  rear=new ImageView(this);rear.setContentDescription("Rear camera live view");rear.setScaleType(ImageView.ScaleType.FIT_CENTER);stage.addView(rear,new FrameLayout.LayoutParams(-1,-1));
  front=new ImageView(this);front.setContentDescription("Front camera live view");front.setScaleType(ImageView.ScaleType.CENTER_CROP);DashUi.card(front,android.graphics.Color.BLACK,8);front.setClipToOutline(true);
  FrameLayout.LayoutParams inset=new FrameLayout.LayoutParams(dp(wide?120:88),dp(wide?68:132),Gravity.TOP|Gravity.END);inset.setMargins(0,dp(12),dp(12),0);stage.addView(front,inset);
  LinearLayout readouts=new LinearLayout(this);readouts.setOrientation(LinearLayout.VERTICAL);readouts.setPadding(dp(8),dp(4),dp(8),dp(8));
  // Only a subtle shadow is applied to telemetry; the camera content stays visible.
  stage.addView(readouts,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  LinearLayout stateRow=new LinearLayout(this);readouts.addView(stateRow);
  status=new TextView(this);status.setTextColor(android.graphics.Color.WHITE);status.setShadowLayer(dp(2),0,1,android.graphics.Color.BLACK);status.setTextSize(12);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);stateRow.addView(status,new LinearLayout.LayoutParams(0,-2,1));
  viewLabel=new TextView(this);viewLabel.setTextSize(11);viewLabel.setTextColor(android.graphics.Color.WHITE);stateRow.addView(viewLabel);
  LinearLayout telemetry=new LinearLayout(this);readouts.addView(telemetry);
  speed=readout(telemetry,"?\nkm/h",22);gpsCard=readout(telemetry,"GPS\nWaiting",12);stabilizationCard=readout(telemetry,"MAX\nStabilization",12);
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setGravity(Gravity.CENTER);
  body.addView(controls,wide?new LinearLayout.LayoutParams(dp(270),-1):new LinearLayout.LayoutParams(-1,-2));
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);controls.addView(actions,new LinearLayout.LayoutParams(-1,dp(64)));
  start=DashUi.button(this,actions,"Record",v->{if(RecordingService.running){motionConsumed=true;consumedSession=DashBridgeProvider.SESSIONS.generation();command("STOP");}else request(true);});
  Button stop=DashUi.button(this,actions,"Stop",v->{motionConsumed=true;consumedSession=DashBridgeProvider.SESSIONS.generation();command("STOP");});
  protect=DashUi.button(this,actions,"Protect",v->command("PROTECT"));
  for(Button button:new Button[]{start,stop,protect}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(3),dp(4),dp(3),0);button.setLayoutParams(lp);button.setTextSize(13);button.setPadding(dp(3),0,dp(3),0);button.setTextColor(android.graphics.Color.WHITE);button.setBackground(RideStyle.surface(this,0xff0665db,8,false));}
  stopAction=stop;stop.setContentDescription("Stop recording");
  LinearLayout footer=new LinearLayout(this);controls.addView(footer,new LinearLayout.LayoutParams(-1,dp(56)));
  Button recordings=DashUi.button(this,footer,"Recordings",v->startActivity(new Intent(this,ClipsActivity.class)));
  Button settingsButton=DashUi.button(this,footer,"Setup",v->menu());
  for(Button button:new Button[]{recordings,settingsButton}){button.setLayoutParams(new LinearLayout.LayoutParams(0,dp(56),1));button.setTextSize(13);button.setBackgroundColor(android.graphics.Color.TRANSPARENT);}
  StartupReveal.show(this,b,"Dash",RideStyle.BACKGROUND,AppearancePalette.accent(RideAppearance.accentIndex(this),RideAppearance.isDark(this)),RideStyle.TEXT,()->RecordingService.running || getSystemService(android.media.AudioManager.class).getMode()!=android.media.AudioManager.MODE_NORMAL);

  details=new TextView(this);
  if(b!=null){setupState=b.getBundle("setup_state");if(b.getBoolean("setup_open"))menu();}
  rear.setOnClickListener(v->fullView("front".equals(mode)?"front":"rear"));front.setOnClickListener(v->fullView("front"));
 }
 TextView readout(LinearLayout row,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(android.graphics.Color.WHITE);t.setShadowLayer(dp(3),0,dp(1),android.graphics.Color.BLACK);t.setTextSize(size);t.setTypeface(null,android.graphics.Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(dp(4),dp(4),dp(4),dp(8));t.setBackgroundColor(android.graphics.Color.TRANSPARENT);LinearLayout.LayoutParams tile=new LinearLayout.LayoutParams(0,-2,1);tile.setMargins(dp(4),0,dp(4),0);row.addView(t,tile);return t;}
 void metric(TextView view,String primary,String label,int size){android.text.SpannableString text=new android.text.SpannableString(primary+"\n"+label);text.setSpan(new android.text.style.AbsoluteSizeSpan(size,true),0,primary.length(),0);text.setSpan(new android.text.style.AbsoluteSizeSpan(11,true),primary.length()+1,text.length(),0);text.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE),primary.length()+1,text.length(),0);view.setText(text);}
 void openSettings(){closeAlignment(null);startActivity(new Intent(this,SettingsActivity.class));}
 private Dialog setupDialog; private SetupMenu setupMenu; private Bundle setupState;
 void menu(){
  if(setupDialog!=null&&setupDialog.isShowing())return;
  final Dialog dialog=new Dialog(this){@Override public void onBackPressed(){if(setupMenu==null||!setupMenu.back())super.onBackPressed();}};setupDialog=dialog;dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  setupMenu=new SetupMenu(this,"RideDeck Dash",AppearancePalette.accent(RideAppearance.accentIndex(this),RideAppearance.isDark(this)),false,setupState,dialog::dismiss);
  LinearLayout screen=setupMenu.add("screen","General","Screen & layout","Light or dark, colours and shared appearance");
  menuItem(screen,"Appearance and colour theme",dialog,()->startActivity(new Intent(this,AppearanceActivity.class)));
  LinearLayout audio=setupMenu.add("audio","General","Headset & sound","Microphone, audio input and sound check");
  menuItem(audio,"Microphones and audio",dialog,()->settingsSection("audio"));menuItem(audio,"Sound check",dialog,()->startActivity(new Intent(this,AudioCheckActivity.class)));
  LinearLayout power=setupMenu.add("power","General","Background & power","Battery limits and ride automation");
  menuItem(power,"Drive automation and battery limits",dialog,()->settingsSection("automation"));
  LinearLayout help=setupMenu.add("help","General","Help & app","Permissions, camera capabilities and updates");
  menuItem(help,"Updates and camera capabilities",dialog,()->startActivity(new Intent(this,CapabilityActivity.class)));menuItem(help,"Recording information",dialog,this::information);
  LinearLayout video=setupMenu.add("recording","Ride","Recording","Camera selection, quality, presets and storage");
  menuItem(video,"Video and storage",dialog,()->settingsSection("video"));
  menuItem(video,"Calibration and sample recordings",dialog,()->{dialog.dismiss();closeAlignment(null);startActivity(new Intent(this,CalibrationActivity.class));});
  menuItem(video,"Camera selection",dialog,()->{dialog.dismiss();if(RecordingService.running){Toast.makeText(this,"Stop recording to change cameras",Toast.LENGTH_LONG).show();return;}new AlertDialog.Builder(this).setTitle("Cameras").setSingleChoiceItems(new String[]{"Rear + front","Rear only","Front only"},"dual".equals(mode)?0:"rear".equals(mode)?1:2,(d,n)->{mode=new String[]{"dual","rear","front"}[n];settings.put("mode",mode);closeAlignment(null);d.dismiss();}).setNegativeButton("Cancel",null).show();});
  menuItem(video,alignment==null?"Open camera view":"Close camera view",dialog,()->{dialog.dismiss();if(RecordingService.running){Toast.makeText(this,"Live view stays on while recording",Toast.LENGTH_SHORT).show();return;}if(alignment!=null){autoView=false;closeAlignment(null);}else{autoView=true;request(false);}});
  LinearLayout overlays=setupMenu.add("overlays","Ride","Speed & overlays","GPS speed, data overlays and stabilisation");
  menuItem(overlays,"Video overlays",dialog,()->startActivity(new Intent(this,OverlaySettingsActivity.class)));
  menuItem(overlays,"Stabilisation and camera settings",dialog,()->settingsSection("video"));
  LinearLayout companion=setupMenu.add("companion","Ride","RideDeck connection","Companion session and shared appearance");
  DashUi.text(this,companion,"RideDeck and RideDeck Yamaha connect automatically when they start a ride. Background recording requires an authenticated companion session.",16);
  menuItem(companion,"Shared appearance",dialog,()->startActivity(new Intent(this,AppearanceActivity.class)));
  LinearLayout library=setupMenu.add("library","Ride","Recordings & sharing","Saved clips, social presets and saved settings");
  menuItem(library,"Recordings",dialog,()->startActivity(new Intent(this,ClipsActivity.class)));menuItem(library,"Map and route",dialog,()->startActivity(new Intent(this,MapActivity.class)));
  menuItem(library,"Social video presets",dialog,()->startActivity(new Intent(this,SocialSettingsActivity.class)));menuItem(library,"My saved settings",dialog,()->startActivity(new Intent(this,SocialSettingsActivity.class).putExtra("section","saved")));
  dialog.setContentView(setupMenu.view());
  dialog.setOnDismissListener(d->{setupState=new Bundle();setupMenu.save(setupState);setupDialog=null;});
  dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(-1,-1);}setupMenu.show();
 }
 void information(){Dialog dialog=new Dialog(this);LinearLayout page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);dialog.setContentView(scroll);TextView title=DashUi.text(this,page,"Recording information",24);title.setTypeface(null,android.graphics.Typeface.BOLD);
  String[] values=details.getText().toString().split("\\n");String[] labels={"CAMERAS","PRESET & LOOP STORAGE","SPACE AVAILABLE","MICROPHONE","POWER & RIDE SESSION","POWER REQUIREMENT"};
  menuSection(page,"RECORDING STATUS");DashUi.text(this,page,RecordingService.state,17);
  int cameraLines=Math.max(1,values.length-(values[values.length-1].startsWith("Battery ")?4:5));
  menuSection(page,labels[0]);DashUi.text(this,page,String.join("\n",Arrays.copyOfRange(values,0,cameraLines)),17);
  for(int n=cameraLines;n<values.length;n++){menuSection(page,labels[Math.min(n-cameraLines+1,labels.length-1)]);DashUi.text(this,page,values[n],17);}
  DashUi.button(this,page,"Close",v->dialog.dismiss());dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels,dp(440)),-2);}}
 void menuSection(LinearLayout page,String title){TextView t=DashUi.text(this,page,title,12);t.setTextColor(AppearancePalette.accent(RideAppearance.accentIndex(this),RideAppearance.isDark(this)));t.setPadding(dp(10),dp(24),dp(10),dp(8));}
 void menuItem(LinearLayout page,String label,Dialog dialog,Runnable action){DashUi.button(this,page,label,v->action.run());}
 void settingsSection(String section){closeAlignment(null);startActivity(new Intent(this,SettingsActivity.class).putExtra("section",section));}
 void startIdleGps(){if(idleLocations!=null||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;idleLocations=getSystemService(LocationManager.class);try{idleLocations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,idleGps);}catch(Exception ignored){idleLocations=null;}}
 void fullView(String camera){ImageView big=new ImageView(this);big.setScaleType(ImageView.ScaleType.FIT_CENTER);big.setBackgroundColor(android.graphics.Color.BLACK);big.setMinimumHeight(dp(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?200:500));AlertDialog dialog=new AlertDialog.Builder(this).setTitle(camera+" live view").setView(big).setPositiveButton("Close",null).create();
  Runnable update=new Runnable(){public void run(){if(!dialog.isShowing())return;big.setImageBitmap("rear".equals(camera)?PreviewFrames.rear:PreviewFrames.front);ui.postDelayed(this,200);}};dialog.show();ui.post(update);}
 void render(){
  if(RecordingService.running)mode=RecordingService.captureMode;else if(sampleStarted&&!starting&&!RecordingService.finalizing){sampleStarted=false;mode=settings.text("mode","dual");autoView=true;}
  PreviewFrames.rotation=getWindowManager().getDefaultDisplay().getRotation();PreviewFrames.interval=1000/settings.number("preview",5);
  Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int percent=battery==null?-1:RecordingGate.batteryPercent(battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1));boolean plugged=battery!=null&&battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
  boolean power=DashApplication.recordingGate(battery,true,false)==RecordingGate.Result.ALLOWED;if(!power&&alignment!=null)closeAlignment(null);
  if(power&&visible&&autoView&&!starting&&!closingView&&alignment==null&&!RecordingService.running&&!RecordingService.finalizing&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){pendingRecord=false;ready();}
  if(RecordingService.running||alignment!=null)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  long seconds=Math.max(0,(SystemClock.elapsedRealtime()-RecordingService.startedAt)/1000);status.setText(RecordingService.running&&!RecordingService.finalizing?String.format(Locale.ROOT,"● Recording %02d:%02d",seconds/60,seconds%60):!power?"Connect charger; 30% minimum and 5-minute battery limit":RecordingService.state.startsWith("Stopped:")?RecordingService.state:alignment!=null?CameraViewStatus.label(PreviewFrames.message,PreviewFrames.rear!=null||PreviewFrames.front!=null):RecordingService.state);
  status.setTextColor(RecordingService.running||status.getText().toString().startsWith("! ")?RideStyle.DANGER:RideStyle.TEXT);
  boolean live=RecordingService.running||alignment!=null;rear.setImageBitmap(live?("front".equals(mode)?PreviewFrames.front:PreviewFrames.rear):null);rear.setContentDescription("front".equals(mode)?"Front camera live view":"Rear camera live view");front.setVisibility("dual".equals(mode)?View.VISIBLE:View.GONE);front.setImageBitmap(live?PreviewFrames.front:null);viewLabel.setText(!power?"Power required":live?settings.height()+"p · "+("dual".equals(mode)?"Dual":"front".equals(mode)?"Front":"Rear"):"View off");
  if(power)startIdleGps();else if(idleLocations!=null){idleLocations.removeUpdates(idleGps);idleLocations=null;idlePosition=null;}Location p=RecordingService.running?RecordingService.position:idlePosition;boolean fresh=p!=null&&SystemClock.elapsedRealtimeNanos()-p.getElapsedRealtimeNanos()<6000000000L;metric(speed,fresh&&p.hasSpeed()?String.format(Locale.ROOT,"%.0f",p.getSpeed()*3.6):"?","km/h",getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?28:34);metric(gpsCard,"GPS",fresh?"Active":"Waiting",17);metric(stabilizationCard,settings.number("stabilization",1)==0?"OFF":"MAX","Stabilization",17);
  if(fresh&&p.hasSpeed()&&p.getAccuracy()<=30&&p.getSpeed()<=2f/3.6f)motionConsumed=false;
  boolean session=DashBridgeProvider.SESSIONS.isActive(SystemClock.elapsedRealtime());long generation=DashBridgeProvider.SESSIONS.generation();
  boolean moving=drive.start(SystemClock.elapsedRealtime(),fresh&&p.hasSpeed(),p==null?0:p.getSpeed(),p==null?Float.MAX_VALUE:p.getAccuracy());
  if(settings.number("autostart",1)==1&&power&&visible&&!starting&&!closingView&&!RecordingService.running&&!RecordingService.finalizing&&((session&&generation!=consumedSession)||(!motionConsumed&&moving))){consumedSession=generation;motionConsumed=true;request(true);}

  if(SystemClock.elapsedRealtime()-storageChecked>3000){long used=new ClipStore(this).usedBytes();storageSummary=String.format(Locale.ROOT,"%.1f GB used · %.1f GB phone free",used/1073741824.0,getFilesDir().getUsableSpace()/1073741824.0);storageChecked=SystemClock.elapsedRealtime();}
  details.setText((RecordingService.running?RecordingService.cameraStatus:("dual".equals(mode)?"Rear + front":"front".equals(mode)?"Front only":"Rear only")+" · "+settings.height()+"p / 30 fps")+"\n"+settings.text("preset","Balanced")+" · "+settings.number("clip",128)+" MB clips · "+settings.number("quota",8)+" GB limit\n"+storageSummary+"\n"+(RecordingService.running?RecordingService.audioStatus:"Microphone: "+AudioInputs.savedLabel(this,settings))+"\nBattery "+percent+"%"+(plugged?" · charging":" · charger grace "+DashApplication.chargerSecondsLeft()+"s remaining")+" · "+(DashBridgeProvider.SESSIONS.isActive(SystemClock.elapsedRealtime())?"RideDeck session active":"Standalone")+(power?"":"\nConnect charger; 30% minimum and 5-minute battery limit"));
  stopAction.setEnabled(RecordingService.running&&!RecordingService.finalizing);stopAction.setAlpha(stopAction.isEnabled()?1f:0.45f);status.setTextColor(RecordingService.running?0xffff8340:android.graphics.Color.WHITE);
  String recordLabel=RecordingService.running?"Stop recording":"Record";if(!start.getText().toString().equals(recordLabel)){start.setText(recordLabel);}start.setEnabled((power||RecordingService.running)&&!starting&&!closingView&&!RecordingService.finalizing);protect.setEnabled(RecordingService.running&&!RecordingService.finalizing);protect.setAlpha(protect.isEnabled()?1f:0.45f);
 }
 void request(boolean recording){pendingRecord=recording;ArrayList<String> permissions=new ArrayList<>();if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.CAMERA);
  if(recording||settings.number("autostart",1)==1){if(!"off".equals(settings.text("audio","off"))&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.RECORD_AUDIO);
   if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.POST_NOTIFICATIONS);}
  if(Build.VERSION.SDK_INT>=31&&RecordingAudioPolicy.bluetooth(settings.text("audio","off"))&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
  if(!permissions.isEmpty()){requestPermissions(permissions.toArray(new String[0]),17);return;}ready();}
 protected void onSaveInstanceState(Bundle b){if(setupMenu!=null){Bundle saved=new Bundle();setupMenu.save(saved);b.putBundle("setup_state",saved);}b.putBoolean("setup_open",setupDialog!=null&&setupDialog.isShowing());b.putBoolean("motionConsumed",motionConsumed);b.putLong("consumedSession",consumedSession);b.putString("samplePending",sampleProfile);b.putInt("sampleSeconds",sampleSeconds);b.putBoolean("sampleStarted",sampleStarted);super.onSaveInstanceState(b);}
 public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==17&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){if(pendingRecord&&!"off".equals(settings.text("audio","off"))&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Choose Audio off or allow microphone recording",Toast.LENGTH_LONG).show();return;}ready();}}
 void ready(){if(!visible)return;if(!pendingRecord&&alignment!=null)return;if(pendingRecord){starting=true;closeAlignment(this::begin);}else{PreviewFrames.rear=null;PreviewFrames.front=null;PreviewFrames.message="Opening cameras";alignment=new AlignmentPreview(this);alignment.start(mode);}}
 void closeAlignment(Runnable after){AlignmentPreview old=alignment;alignment=null;if(old!=null){closingView=true;old.close(()->{closingView=false;if(after!=null)after.run();});}else if(after!=null)after.run();}
 void begin(){if(!visible||RecordingService.finalizing){starting=false;return;}PreviewFrames.rear=null;PreviewFrames.front=null;Intent i=new Intent(this,RecordingService.class).setAction("START").putExtra("mode",mode).putExtra("rotation",getWindowManager().getDefaultDisplay().getRotation());try{if(sampleProfile!=null){i.putExtra("captureProfile",sampleProfile).putExtra("sampleSeconds",sampleSeconds);sampleProfile=null;sampleStarted=true;}startForegroundService(i);ui.postDelayed(()->starting=false,1000);}catch(Exception e){starting=false;Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
 void command(String action){if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(action));}
 protected void onStart(){super.onStart();if(!RecordingService.running)mode=settings.text("mode","dual");visible=true;PreviewFrames.enabled=true;ui.post(refresh);}
 protected void onDestroy(){if(setupDialog!=null)setupDialog.dismiss();super.onDestroy();}
 protected void onStop(){if(idleLocations!=null){idleLocations.removeUpdates(idleGps);idleLocations=null;}idlePosition=null;visible=false;PreviewFrames.enabled=false;closeAlignment(null);ui.removeCallbacks(refresh);super.onStop();}
}
