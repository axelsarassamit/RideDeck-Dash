package com.axelsarassamit.ridedeck.dash;

import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import java.io.*;

/** Reversible own-app bench settings. Never edits recordings or Android system settings. */
public class VoiceBenchSettingsTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Test public void prepare()throws Exception {
        Context c=context();SharedPreferences p=c.getSharedPreferences("dash-settings",0);
        File saved=new File(c.getCacheDir(),"voice-bench-settings.json");
        if(saved.exists())throw new IllegalStateException("Restore the previous bench settings first");
        JSONObject json=new JSONObject();
        for(String key:new String[]{"audio","source","mode","height","autostart","autostop"}) json.put(key,p.contains(key)?p.getAll().get(key):JSONObject.NULL);
        try(FileOutputStream out=new FileOutputStream(saved)){out.write(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        if(!p.edit().putString("audio","auto").putInt("source",1).putString("mode","rear").putInt("height",720).putInt("autostart",0).putInt("autostop",0).commit())throw new IOException("Could not save bench settings");
    }
    @Test public void restore()throws Exception {
        Context c=context();File saved=new File(c.getCacheDir(),"voice-bench-settings.json");
        if(!saved.exists())return;
        String text;try(FileInputStream in=new FileInputStream(saved)){text=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        JSONObject json=new JSONObject(text);SharedPreferences.Editor editor=c.getSharedPreferences("dash-settings",0).edit();
        for(java.util.Iterator<String> i=json.keys();i.hasNext();){String key=i.next();Object value=json.get(key);if(value==JSONObject.NULL)editor.remove(key);else if(value instanceof Number)editor.putInt(key,((Number)value).intValue());else editor.putString(key,value.toString());}
        if(!editor.commit() || !saved.delete())throw new IOException("Restore bench settings failed");
    }
}
