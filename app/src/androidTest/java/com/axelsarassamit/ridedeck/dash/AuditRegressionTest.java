package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;

/** Isolated audit cases. They must never alter production Dash preferences or recordings. */
public class AuditRegressionTest {
    final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Before public void isolated(){assertTrue("Use isolated debug app",c.getPackageName().endsWith(".debug"));c.getSharedPreferences("dash-settings",0).edit().clear().putInt("autostart",0).putString("audio","off").putString("mode","rear").putBoolean("music_safe_audio_v1",true).commit();}
    @After public void cleanup()throws Exception{c.stopService(new Intent(c,RecordingService.class));Thread.sleep(500);c.getSharedPreferences("dash-settings",0).edit().clear().putInt("autostart",0).commit();c.getSharedPreferences("dash-user-presets",0).edit().clear().commit();}
    boolean service(){for(ActivityManager.RunningServiceInfo s:c.getSystemService(ActivityManager.class).getRunningServices(100))if(s.service.getPackageName().equals(c.getPackageName())&&s.service.getClassName().equals(RecordingService.class.getName()))return true;return false;}
    @Test public void lateProtectActionDoesNotLeaveIdleRecordingService()throws Exception{
        try(ActivityScenario<DashboardActivity> s=ActivityScenario.launch(DashboardActivity.class)){
            s.onActivity(a->{a.autoView=false;a.closeAlignment(null);a.startService(new Intent(a,RecordingService.class).setAction("PROTECT"));});
            Thread.sleep(1500);assertFalse("Late Protect action left an idle capture service",service());assertFalse(RecordingService.running);
        }
    }
    @Test public void unsignedCallerCannotControlBridge(){
        Bundle extras=new Bundle();extras.putInt("protocolVersion",1);extras.putString("packageName","com.axelsarassamit.ridedeck.yamaha");
        try{c.getContentResolver().call(android.net.Uri.parse("content://"+c.getPackageName()+".bridge"),"renew",null,extras);fail("Unsigned caller accepted");}catch(SecurityException expected){}
    }
    @Test public void restoringPresetDoesNotRemigrateExplicitBluetoothChoice()throws Exception{
        c.getSharedPreferences("dash-settings",0).edit().putString("audio","headset").commit();SavedSettings slots=new SavedSettings(c);slots.save(0,"Headset");
        c.getSharedPreferences("dash-settings",0).edit().putString("audio","off").commit();slots.load(0);DashSettings.migrateMusicSafeAudio(c);
        assertEquals("Explicit preset microphone changed on restart","headset",new DashSettings(c).text("audio","off"));
    }
    @Test public void booleanSettingsAndMigrationMarkerSurviveLegacyPreset()throws Exception{
        SavedSettings slots=new SavedSettings(c);c.getSharedPreferences("dash-settings",0).edit().putBoolean("user_test_toggle",true).commit();slots.save(1,"Boolean");
        c.getSharedPreferences("dash-settings",0).edit().putBoolean("user_test_toggle",false).commit();slots.load(1);assertTrue(c.getSharedPreferences("dash-settings",0).getBoolean("user_test_toggle",false));
        slots.saved.edit().putString("data2","{\"audio\":\"headset\",\"music_safe_audio_v1\":false}").commit();slots.load(2);DashSettings.migrateMusicSafeAudio(c);assertEquals("headset",new DashSettings(c).text("audio","off"));
    }
    @Test public void recordPermissionIntentSurvivesRotation()throws Exception{
        try(ActivityScenario<DashboardActivity> s=ActivityScenario.launch(DashboardActivity.class)){
            s.onActivity(a->{a.autoView=false;a.closeAlignment(null);a.pendingRecord=true;a.sampleProfile="native_sample";a.sampleSeconds=30;a.mode="rear";
                a.permissionRequestPending=true;
            });s.recreate();s.onActivity(a->{assertTrue("Record intent was lost during permission rotation",a.pendingRecord);assertTrue(a.permissionRequestPending);assertEquals("native_sample",a.sampleProfile);assertEquals("rear",a.mode);});
        }
    }
    @Test public void previewRemainsAvailableBelowRecordingBatteryCutoff()throws Exception{
        Assume.assumeTrue("Explicit simulated battery preview check","1".equals(InstrumentationRegistry.getArguments().getString("batteryPreview")));
        Intent battery=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));assertNotNull(battery);assertEquals(0,battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1));assertTrue(RecordingGate.batteryPercent(battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1))<30);
        try(ActivityScenario<DashboardActivity> scenario=ActivityScenario.launch(DashboardActivity.class)){
            Thread.sleep(2000);scenario.onActivity(a->{assertNotNull("Preview was blocked by recording power limit",a.alignment);assertFalse(a.start.isEnabled());assertFalse(RecordingService.running);});
        }
    }
    @Test public void deniedForegroundRequestStopsWithoutProcessCrash()throws Exception{
        Assume.assumeTrue("Explicit rejected-FGS check","1".equals(InstrumentationRegistry.getArguments().getString("rejectedStart")));
        try(ActivityScenario<DashboardActivity> s=ActivityScenario.launch(DashboardActivity.class)){
            s.onActivity(a->{a.autoView=false;a.closeAlignment(null);((DashApplication)a.getApplication()).onActivityStopped(a);a.startForegroundService(new Intent(a,RecordingService.class).setAction("START"));});
            try{Thread.sleep(8000);assertFalse(service());assertFalse(RecordingService.running);}finally{s.onActivity(a->((DashApplication)a.getApplication()).onActivityStarted(a));}
        }
    }
}
