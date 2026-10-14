package com.axelsarassamit.ridedeck.dash;
import android.content.*;
final class DashSettings {
 final android.content.SharedPreferences prefs;
 final java.util.Map<String,?> snapshot;
 DashSettings(Context c){this(c,false);}
 DashSettings(Context c,boolean frozen){prefs=c.getSharedPreferences("dash-settings",Context.MODE_PRIVATE);snapshot=frozen?prefs.getAll():null;}
 DashSettings(Context c,java.util.Map<String,?> values){prefs=c.getSharedPreferences("dash-settings",Context.MODE_PRIVATE);snapshot=new java.util.HashMap<>(values);}
 int number(String key,int fallback){return snapshot==null?prefs.getInt(key,fallback):snapshot.get(key) instanceof Integer?(Integer)snapshot.get(key):fallback;}
 String text(String key,String fallback){return snapshot==null?prefs.getString(key,fallback):snapshot.get(key) instanceof String?(String)snapshot.get(key):fallback;}
 void put(String key,int value){prefs.edit().putInt(key,value).apply();}
 void put(String key,String value){prefs.edit().putString(key,value).apply();}
 android.os.Bundle bundle(){android.os.Bundle result=new android.os.Bundle();java.util.Map<String,?> values=snapshot==null?prefs.getAll():snapshot;for(java.util.Map.Entry<String,?> entry:values.entrySet()){if(entry.getValue() instanceof Integer)result.putInt(entry.getKey(),(Integer)entry.getValue());else if(entry.getValue() instanceof String)result.putString(entry.getKey(),(String)entry.getValue());}return result;}
 static DashSettings fromBundle(Context context,android.os.Bundle bundle){if(bundle==null)return new DashSettings(context,true);java.util.Map<String,Object> values=new java.util.HashMap<>();for(String key:bundle.keySet()){Object value=bundle.get(key);if(value instanceof Integer||value instanceof String)values.put(key,value);}return new DashSettings(context,values);}
 long clipBytes(){return number("clip",128)*1024L*1024;}
 long quotaBytes(){return number("quota",8)*1024L*1024*1024;}
 int width(){return number("height",720)==1080?1920:1280;}
 int height(){return number("height",720);}
 int bitrate(){return number("bitrate",6)*1000000;}
 void preset(int i){int[] heights={720,1080,720,1080};int[] rates={6,12,4,16};int[] clips={128,256,64,512};int[] quotas={8,16,4,32};
  prefs.edit().putInt("height",heights[i]).putInt("bitrate",rates[i]).putInt("clip",clips[i]).putInt("quota",quotas[i]).putString("preset",new String[]{"Balanced","Clear detail","Long ride","High quality"}[i]).apply();}
}
