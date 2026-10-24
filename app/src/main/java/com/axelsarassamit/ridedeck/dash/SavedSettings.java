package com.axelsarassamit.ridedeck.dash;
import android.content.*;import org.json.*;import java.util.*;
final class SavedSettings {
 final SharedPreferences saved;final DashSettings settings;
 SavedSettings(Context c){saved=c.getSharedPreferences("dash-user-presets",Context.MODE_PRIVATE);settings=new DashSettings(c);}
 String name(int slot){return saved.getString("name"+slot,"Slot "+slot+" · empty");}
 boolean exists(int slot){return saved.contains("data"+slot);}
 void save(int slot,String name)throws JSONException{JSONObject data=new JSONObject();for(Map.Entry<String,?> entry:settings.prefs.getAll().entrySet())if(!"music_safe_audio_v1".equals(entry.getKey())&&(entry.getValue() instanceof String||entry.getValue() instanceof Integer||entry.getValue() instanceof Boolean))data.put(entry.getKey(),entry.getValue());saved.edit().putString("data"+slot,data.toString()).putString("name"+slot,name).apply();}
 void load(int slot)throws JSONException{JSONObject data=new JSONObject(saved.getString("data"+slot,"{}"));Map<String,Object> checked=new HashMap<>();Iterator<String> keys=data.keys();while(keys.hasNext()){String key=keys.next();Object value=data.get(key);if("music_safe_audio_v1".equals(key))continue;if(!(value instanceof String)&&!(value instanceof Integer)&&!(value instanceof Boolean))throw new JSONException("Unsupported preset value");checked.put(key,value);}boolean migrated=settings.prefs.getBoolean("music_safe_audio_v1",false);SharedPreferences.Editor edit=settings.prefs.edit().clear().putBoolean("music_safe_audio_v1",migrated);for(Map.Entry<String,Object> entry:checked.entrySet()){if(entry.getValue() instanceof String)edit.putString(entry.getKey(),(String)entry.getValue());else if(entry.getValue() instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)entry.getValue());else edit.putInt(entry.getKey(),(Integer)entry.getValue());}edit.apply();}
 void remove(int slot){saved.edit().remove("data"+slot).remove("name"+slot).apply();}
}
