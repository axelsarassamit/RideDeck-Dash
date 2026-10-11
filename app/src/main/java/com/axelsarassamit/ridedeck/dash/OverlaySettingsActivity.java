package com.axelsarassamit.ridedeck.dash;
import android.app.*;import android.os.*;import android.widget.*;
public final class OverlaySettingsActivity extends Activity {
 public void onCreate(Bundle state){super.onCreate(state);DashSettings settings=new DashSettings(this);LinearLayout page=DashUi.page(this);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
  DashUi.text(this,page,"Video overlays",28);
  DashUi.text(this,page,"Choose the data burned into an exported copy from Recordings. Your original video stays clean. GPS gaps display unavailable data. The route panel is an offline GPS trace, with no downloaded road map.",16);
  DashUi.button(this,page,"Overlay presets",v->new AlertDialog.Builder(this).setTitle("Overlay preset").setItems(new String[]{"Minimal: speed and time","Touring: speed, time, distance and route","All available data","Clean: no data"},(d,i)->{String[] keys={"speed","time","distance","route","altitude","heading","accuracy","battery","brand","camera"};for(String key:keys)settings.put("overlay_"+key,i==2?1:i==3?0:(key.equals("speed")||key.equals("time")||i==1&&(key.equals("distance")||key.equals("route")))?1:0);recreate();}).show());
  String[] keys={"speed","time","distance","route","altitude","heading","accuracy","battery","brand","camera"},labels={"GPS speed","Date and time","Recorded ride distance","Route trace map","GPS altitude","GPS heading","GPS accuracy","Battery and charging","RideDeck Dash branding","Camera name"};
  for(int i=0;i<keys.length;i++){String key=keys[i];Switch toggle=new Switch(this);toggle.setText(labels[i]);toggle.setTextColor(DashUi.WHITE);toggle.setTextSize(17);toggle.setPadding(12,24,12,24);toggle.setChecked(settings.number("overlay_"+key,key.equals("speed")||key.equals("time")?1:0)==1);toggle.setOnCheckedChangeListener((b,on)->settings.put("overlay_"+key,on?1:0));page.addView(toggle);}
  choice(page,settings,"Position","overlay_top",new int[]{0,1},new String[]{"Bottom","Top"},0);
  choice(page,settings,"Panel size","overlay_size",new int[]{20,28,36},new String[]{"Compact","Medium","Large"},28);
  choice(page,settings,"Panel opacity","overlay_alpha",new int[]{100,160,220},new String[]{"Light","Balanced","Solid"},160);
  choice(page,settings,"Speed units","overlay_mph",new int[]{0,1},new String[]{"km/h","mph"},0);
  DashUi.text(this,page,"Export uses additional temporary storage and processing time. Coordinates are included visually only in the route trace. Share ride data only when you intend to share location history.",15);
  DashUi.button(this,page,"Back",v->finish());
 }
 void choice(LinearLayout page,DashSettings settings,String title,String key,int[] values,String[] labels,int fallback){int current=settings.number(key,fallback),index=0;for(int i=0;i<values.length;i++)if(values[i]==current)index=i;DashUi.button(this,page,title+": "+labels[index],v->new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,i)->{settings.put(key,values[i]);recreate();}).show());}
}
