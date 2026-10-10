package com.axelsarassamit.ridedeck.dash;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.*;
import android.widget.*;
import java.util.*;

public final class DashboardActivity extends Activity {
    private TextView status, speed, details;
    private Button start, protect, stop;
    private Spinner mode;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() { @Override public void run() { render(); ui.postDelayed(this, 500); } };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout page = DashUi.page(this), controls=page;
        if(landscape) {
            LinearLayout root=page;root.setOrientation(LinearLayout.HORIZONTAL);
            page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
            controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);
            ScrollView left=new ScrollView(this),right=new ScrollView(this);left.addView(page);right.addView(controls);
            root.addView(left,new LinearLayout.LayoutParams(0,-1,1));root.addView(right,new LinearLayout.LayoutParams(0,-1,1));setContentView(root);
        } else {
            ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(page); setContentView(scroll);
            ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ic_dash); page.addView(logo, new LinearLayout.LayoutParams(96,96));
        }
        DashUi.text(this,page,"RideDeck Dash",30);
        status = DashUi.text(this,page,"Ready to record",23);
        speed = DashUi.text(this,page,"GPS speed unavailable",32);
        details = DashUi.text(this,page,"",17);
        mode = new Spinner(this);
        mode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Rear + front", "Rear only", "Front only"}));
        controls.addView(mode);
        start = DashUi.button(this,controls,"Start recording",v -> requestStart());
        protect = DashUi.button(this,controls,"Protect clip",v -> command("PROTECT"));
        stop = DashUi.button(this,controls,"Stop recording",v -> command("STOP"));
        DashUi.button(this,controls,"Saved clips",v -> startActivity(new Intent(this,ClipsActivity.class)));
        DashUi.button(this,controls,"Map and route",v -> startActivity(new Intent(this,MapActivity.class)));
        DashUi.button(this,controls,"Cameras and updates",v -> {
            if (RecordingService.running) new AlertDialog.Builder(this).setMessage("Stop recording before checking updates.").setPositiveButton("OK",null).show();
            else startActivity(new Intent(this,CapabilityActivity.class));
        });
        if(!landscape) DashUi.text(this,page,"2 GB loop storage · marked clips are kept\nBackground capture requires an active RideDeck ride.\nVideo records without audio. GPS is saved alongside each ride.\nYamaha speed link: not connected.",16);
    }
    private void render() {
        if(RecordingService.running) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent battery = registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int percent = battery == null ? -1 : RecordingGate.batteryPercent(battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1));
        boolean plugged = battery != null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
        boolean power = RecordingGate.evaluate(plugged,percent,true,false)==RecordingGate.Result.ALLOWED;
        status.setText(RecordingService.state);
        Location p = RecordingService.position;
        boolean fresh = p != null && SystemClock.elapsedRealtimeNanos()-p.getElapsedRealtimeNanos() < 6_000_000_000L;
        speed.setText(fresh && p.hasSpeed() ? String.format(Locale.ROOT,"%.0f km/h · GPS",p.getSpeed()*3.6) : RecordingService.running ? "Waiting for GPS speed" : "GPS speed unavailable");
        details.setText(RecordingService.cameraStatus+"\nBattery "+(percent<0?"unknown":percent+"%")+(plugged?" · charger connected":"")
            +"\n"+(DashBridgeProvider.SESSIONS.isActive(SystemClock.elapsedRealtime())?"RideDeck background session active":"Standalone: keep Dash visible")
            +(power?"":"\nConnect charger or charge to 50%"));
        start.setEnabled(power && !RecordingService.running); mode.setEnabled(!RecordingService.running);
        protect.setEnabled(RecordingService.running); stop.setEnabled(RecordingService.running);
    }
    private void requestStart() {
        ArrayList<String> permissions = new ArrayList<>();
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.CAMERA);
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION); permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!permissions.isEmpty()) { requestPermissions(permissions.toArray(new String[0]),17); return; }
        begin();
    }
    @Override public void onRequestPermissionsResult(int id,String[] p,int[] grants) {
        super.onRequestPermissionsResult(id,p,grants);
        if(id==17 && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) begin();
        else if(id==17) Toast.makeText(this,"Camera permission is needed to record",Toast.LENGTH_LONG).show();
    }
    private void begin() {
        String[] choices={"dual","rear","front"};
        Intent i=new Intent(this,RecordingService.class).setAction("START").putExtra("mode",choices[mode.getSelectedItemPosition()])
            .putExtra("rotation",getWindowManager().getDefaultDisplay().getRotation());
        try { startForegroundService(i); } catch (Exception error) { Toast.makeText(this,"Cannot start: "+error.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private void command(String action) { if(RecordingService.running) startService(new Intent(this,RecordingService.class).setAction(action)); }
    @Override protected void onStart() { super.onStart(); ui.post(refresh); }
    @Override protected void onStop() { ui.removeCallbacks(refresh); super.onStop(); }
}
