package com.axelsarassamit.ridedeck.dash;
import android.content.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
/** Local microphone picker must work even when nearby-device access is denied. */
public class MicrophonePickerTest {
 @Test public void phoneChoiceDoesNotRequireBluetoothPermission() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  SharedPreferences p=c.getSharedPreferences("dash-settings",Context.MODE_PRIVATE);
  boolean existed=p.contains("audio");String old=p.getString("audio","off");
  boolean named=p.contains("audioLabel");String label=p.getString("audioLabel","");
  try(ActivityScenario<SettingsActivity> scene=ActivityScenario.launch(new Intent(c,SettingsActivity.class).putExtra("section","audio"))){
   scene.onActivity(a->a.microphones());InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scene.onActivity(a->{assertTrue("Phone input picker must open without nearby-device access",SharedSetupTest.has(a,"Automatic phone microphone"));android.widget.ListView list=null;for(android.view.View root:android.view.inspector.WindowInspector.getGlobalWindowViews())for(android.view.View v:SharedSetupTest.views(root))if(v instanceof android.widget.ListView && v.isShown())list=(android.widget.ListView)v;assertNotNull(list);assertTrue(list.performItemClick(list.getChildAt(1),1,list.getAdapter().getItemId(1)));});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals("auto",p.getString("audio","off"));
  } finally {SharedPreferences.Editor e=p.edit();if(existed)e.putString("audio",old);else e.remove("audio");if(named)e.putString("audioLabel",label);else e.remove("audioLabel");e.commit();}
 }
}
