package com.axelsarassamit.ridedeck.dash;

import android.media.*;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual short capture, with no music playback, pairing or volume changes. */
public class MusicRouteTest {
    @Test public void automaticRecordingUsesPhoneMicWithoutCallMode()throws Exception {
        assertFalse("Stop existing recording before running the bench test",RecordingService.running);
        try(ActivityScenario<DashboardActivity> scenario=ActivityScenario.launch(DashboardActivity.class)){
            try {
                scenario.onActivity(a->a.request(true));
                long end=SystemClock.elapsedRealtime()+30000;
                while(!RecordingService.capturing && SystemClock.elapsedRealtime()<end)Thread.sleep(150);
                assertTrue("Camera did not start: "+RecordingService.state,RecordingService.capturing);
                AudioManager manager=InstrumentationRegistry.getInstrumentation().getTargetContext().getSystemService(AudioManager.class);
                end=SystemClock.elapsedRealtime()+8000;boolean phone=false;
                while(!phone && RecordingService.running && SystemClock.elapsedRealtime()<end){
                    for(AudioRecordingConfiguration config:manager.getActiveRecordingConfigurations()){
                        AudioDeviceInfo device=config.getAudioDevice();
                        if(device!=null && device.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC && !config.isClientSilenced())phone=true;
                    }
                    if(!phone)Thread.sleep(150);
                }
                assertTrue("Live unsilenced phone microphone not confirmed: "+RecordingService.audioStatus,phone);
                assertEquals("Dash must leave music out of call mode",AudioManager.MODE_NORMAL,manager.getMode());
                Thread.sleep(2500);
                assertTrue("Recording stopped unexpectedly: "+RecordingService.state,RecordingService.capturing);
            } finally {
                scenario.onActivity(a->a.command("STOP"));
                long end=SystemClock.elapsedRealtime()+15000;
                while((RecordingService.running||RecordingService.finalizing)&&SystemClock.elapsedRealtime()<end)Thread.sleep(150);
                assertFalse("Test recording did not stop",RecordingService.running);
            }
        }
    }
}
