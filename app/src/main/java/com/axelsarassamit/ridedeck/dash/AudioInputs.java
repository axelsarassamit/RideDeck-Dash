package com.axelsarassamit.ridedeck.dash;
import android.media.*;
final class AudioInputs {
 static boolean selectable(AudioDeviceInfo d){int t=d.getType();return t==AudioDeviceInfo.TYPE_BUILTIN_MIC || t==AudioDeviceInfo.TYPE_WIRED_HEADSET || t==AudioDeviceInfo.TYPE_USB_DEVICE || t==AudioDeviceInfo.TYPE_USB_HEADSET || t==AudioDeviceInfo.TYPE_USB_ACCESSORY || t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (android.os.Build.VERSION.SDK_INT>=31 && t==AudioDeviceInfo.TYPE_BLE_HEADSET);}
 static String key(AudioDeviceInfo d){return d.getType()+":"+(android.os.Build.VERSION.SDK_INT>=28?d.getAddress():Integer.toString(d.getId()));}
 static String label(AudioDeviceInfo d){String type;
  switch(d.getType()){case AudioDeviceInfo.TYPE_BUILTIN_MIC:type="Phone microphone";break;case AudioDeviceInfo.TYPE_WIRED_HEADSET:type="Wired headset";break;case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:type="Bluetooth headset";break;case AudioDeviceInfo.TYPE_USB_DEVICE:case AudioDeviceInfo.TYPE_USB_HEADSET:type="USB microphone";break;default:type="Input "+d.getType();}
  return type+" · "+d.getProductName()+" ("+d.getId()+")";
 }
}
