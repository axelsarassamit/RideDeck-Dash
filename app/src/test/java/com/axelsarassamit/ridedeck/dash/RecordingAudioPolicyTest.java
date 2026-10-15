package com.axelsarassamit.ridedeck.dash;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecordingAudioPolicyTest {
    @Test public void legacyBluetoothRecordingMovesToPhoneForClearMusic() {
        for(String key:new String[]{"headset","7:headset-address","26:le-headset-address"})
            assertEquals("auto",RecordingAudioPolicy.migrateLegacyInput(key));
    }
    @Test public void disabledAndExplicitWiredUsbPhoneInputsArePreserved() {
        for(String key:new String[]{"off","auto","3:wired","11:usb","22:usb-headset","15:phone"})
            assertEquals(key,RecordingAudioPolicy.migrateLegacyInput(key));
    }
    @Test public void automaticPhoneNeverRequestsBluetoothVoiceMode() {
        assertTrue(RecordingAudioPolicy.automaticPhone("auto"));
        assertFalse(RecordingAudioPolicy.bluetooth("auto"));
        assertFalse(RecordingAudioPolicy.automaticPhone("headset"));
        assertTrue(RecordingAudioPolicy.bluetooth("headset"));
    }
}
