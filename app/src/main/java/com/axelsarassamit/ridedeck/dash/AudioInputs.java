package com.axelsarassamit.ridedeck.dash;
import android.media.*;
final class AudioInputs {
 static boolean selectable(AudioDeviceInfo d){int t=d.getType();return t==AudioDeviceInfo.TYPE_BUILTIN_MIC || t==AudioDeviceInfo.TYPE_WIRED_HEADSET || t==AudioDeviceInfo.TYPE_USB_DEVICE || t==AudioDeviceInfo.TYPE_USB_HEADSET || t==AudioDeviceInfo.TYPE_USB_ACCESSORY || t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (android.os.Build.VERSION.SDK_INT>=31 && t==AudioDeviceInfo.TYPE_BLE_HEADSET);}
 static String key(AudioDeviceInfo d){return d.getType()+":"+(android.os.Build.VERSION.SDK_INT>=28?d.getAddress():Integer.toString(d.getId()));}
 static final java.util.Map<String,Integer> phoneNames=new java.util.concurrent.ConcurrentHashMap<>();
 static void discover(AudioDeviceInfo[] devices){int n=0;for(AudioDeviceInfo d:devices)if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC)phoneNames.put(key(d),++n);}
 static int phoneNumber(AudioDeviceInfo d){return phoneNames.computeIfAbsent(key(d),k->phoneNames.size()+1);}
 static String savedLabel(android.content.Context c,DashSettings settings){String key=settings.text("audio","off");if("off".equals(key))return "Off";if("auto".equals(key))return "Automatic phone microphone";if("headset".equals(key))return "Bluetooth microphone (reduces music quality)";
  try{AudioDeviceInfo[] devices=c.getSystemService(AudioManager.class).getDevices(AudioManager.GET_DEVICES_INPUTS);discover(devices);for(AudioDeviceInfo d:devices)if(key(d).equals(key))return label(d);}catch(SecurityException ignored){}
  String old=settings.text("audioLabel","Selected microphone");return old.startsWith("Phone microphone")?"Phone microphone (disconnected)":old.replaceAll(" [(][0-9]+[)]$", "")+" (disconnected)";
 }
 static String label(AudioDeviceInfo d){String type;
  switch(d.getType()){case AudioDeviceInfo.TYPE_BUILTIN_MIC:type="Phone microphone";break;case AudioDeviceInfo.TYPE_WIRED_HEADSET:type="Wired headset";break;case AudioDeviceInfo.TYPE_BLE_HEADSET:case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:type="Bluetooth headset";break;case AudioDeviceInfo.TYPE_USB_DEVICE:case AudioDeviceInfo.TYPE_USB_HEADSET:type="USB microphone";break;default:type="Input "+d.getType();}
  if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC)return "Phone microphone " + phoneNumber(d);
  String name=d.getProductName().toString();return type+(name.trim().isEmpty()?"":" · "+name);
 }
}
