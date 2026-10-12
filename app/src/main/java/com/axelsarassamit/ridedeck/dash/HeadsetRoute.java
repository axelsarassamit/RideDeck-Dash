package com.axelsarassamit.ridedeck.dash;
import android.media.*;import android.os.*;import java.io.*;import java.util.*;

/** Establish the voice transport before configuring the video recorder. No brand checks. */
final class HeadsetRoute implements AutoCloseable {
    final AudioManager manager; boolean owned;
    HeadsetRoute(AudioManager value){manager=value;}
    static boolean bluetooth(AudioDeviceInfo d){return d!=null&&(d.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||(Build.VERSION.SDK_INT>=31&&d.getType()==AudioDeviceInfo.TYPE_BLE_HEADSET));}
    AudioDeviceInfo connect(String key)throws IOException,InterruptedException{
        if(Build.VERSION.SDK_INT<31)throw new IOException("Bluetooth microphone routing requires Android 12 or newer");
        if(manager.getMode()!=AudioManager.MODE_NORMAL)throw new IOException("Finish the call or voice app before recording headset audio");
        List<AudioDeviceInfo> candidates=new ArrayList<>();for(AudioDeviceInfo d:manager.getAvailableCommunicationDevices())if(bluetooth(d))candidates.add(d);
        AudioDeviceInfo wanted=null;
        for(AudioDeviceInfo d:candidates)if(AudioInputs.key(d).equals(key))wanted=d;
        if("headset".equals(key)){
            AudioDeviceInfo active=manager.getCommunicationDevice();for(AudioDeviceInfo d:candidates)if(active!=null&&d.getId()==active.getId())wanted=d;
            if(wanted==null&&candidates.size()==1)wanted=candidates.get(0);
            if(wanted==null&&candidates.size()>1)throw new IOException("Multiple headsets connected; choose a microphone in settings");
        }
        if(wanted==null)throw new IOException("No connected Bluetooth voice microphone. Playback-only devices cannot record sound");
        owned=true;manager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if(!manager.setCommunicationDevice(wanted))throw new IOException("Android rejected the headset microphone connection");
        long deadline=SystemClock.elapsedRealtime()+5000;
        while(SystemClock.elapsedRealtime()<deadline){
            AudioDeviceInfo actual=manager.getCommunicationDevice();
            if(actual!=null&&AudioInputs.key(actual).equals(AudioInputs.key(wanted))){
                for(AudioDeviceInfo input:manager.getDevices(AudioManager.GET_DEVICES_INPUTS))if(bluetooth(input)&&AudioInputs.key(input).equals(AudioInputs.key(wanted)))return input;
            }
            Thread.sleep(100);
        }
        throw new IOException("Headset microphone connection timed out");
    }
    @Override public void close(){if(owned&&Build.VERSION.SDK_INT>=31){try{manager.clearCommunicationDevice();}finally{manager.setMode(AudioManager.MODE_NORMAL);owned=false;}}}
}
