package com.axelsarassamit.ridedeck.dash;
import android.app.Activity;import android.os.*;import android.widget.*;import android.graphics.*;import java.util.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class OverlayPreviewActivity extends Activity {
 final Handler ui=new Handler(Looper.getMainLooper());TelemetryOverlay overlay;ImageView preview;long started;
 public void onCreate(Bundle state){super.onCreate(state);LinearLayout page=DashUi.page(this);setContentView(page);DashUi.text(this,page,"RideDeck overlay preview",24);DashUi.text(this,page,"Illustrative data and roads. Your exports use the recorded ride, never this example.",15);preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setBackgroundColor(Color.BLACK);page.addView(preview,new LinearLayout.LayoutParams(-1,0,1));DashUi.button(this,page,"Back to overlay settings",v->finish());
  RideTelemetry data=new RideTelemetry();data.clipElapsed=1000000000;data.clipUtc=System.currentTimeMillis();for(int i=0;i<=60;i++){RideTelemetry.Sample s=new RideTelemetry.Sample();s.elapsed=data.clipElapsed+i*1000000000L;s.lat=13.75+i*.00003;s.lon=100.5+Math.sin(i*.08)*.0007;s.accuracy=4;s.speed=17.2;s.altitude=18;s.heading=30;s.battery=86;s.distance=i*17.2;s.charging=true;data.samples.add(s);}OverlayRoads roads=new OverlayRoads();roads.illustrative=true;for(int i=-3;i<4;i++){roads.ways.add(new double[]{13.748,100.5+i*.0005,13.754,100.5+i*.0005});roads.ways.add(new double[]{13.75+i*.0005,100.497,13.75+i*.0005,100.503});}
  DashSettings saved=new DashSettings(this,true);Map<String,Object> values=new HashMap<>(saved.snapshot);overlay=new TelemetryOverlay(this,data,new DashSettings(this,values),0,false,roads);boolean portrait=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT;overlay.configure(new androidx.media3.common.util.Size(portrait?720:1280,portrait?1280:720));started=SystemClock.elapsedRealtime();}
 final Runnable tick=new Runnable(){public void run(){if(isFinishing())return;try{preview.setImageBitmap(overlay.getBitmap(((SystemClock.elapsedRealtime()-started)%60000)*1000));}catch(Exception ignored){}ui.postDelayed(this,100);}};
 protected void onResume(){super.onResume();ui.post(tick);}
 protected void onPause(){ui.removeCallbacks(tick);super.onPause();}
 protected void onDestroy(){preview.setImageDrawable(null);try{overlay.release();}catch(Exception ignored){}super.onDestroy();}
}
