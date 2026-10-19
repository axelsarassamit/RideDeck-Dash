package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.WeakHashMap;

/** One appearance choice for every screen, independent of recording and map settings. */
public final class RideAppearance implements Application.ActivityLifecycleCallbacks {
    public static final String[] MODES = {"System", "Light", "Dark"};
    private static final WeakHashMap<Activity,String> rendered = new WeakHashMap<>();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
    private RideAppearance() { }
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ride_appearance",Context.MODE_PRIVATE); }
    public static int mode(Context c) { return prefs(c).getInt(followsHost(c) ? "host_mode" : "appearance_mode",0); }
    public static int accentIndex(Context c) { return prefs(c).getInt(followsHost(c) ? "host_accent" : "color_theme",2); }
    public static boolean isDark(Context c) {
        boolean system = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return AppearancePalette.dark(mode(c),system);
    }
    private static String key(Context c) { return mode(c)+":"+isDark(c)+":"+accentIndex(c) + ":"+followsHost(c)+":"+prefs(c).getString("host_name",""); }
    public static void prepare(Activity a) {
        update(a); int index=Math.max(0,Math.min(4,accentIndex(a)));
        int[] themes=isDark(a) ? new int[]{R.style.RideDark0,R.style.RideDark1,R.style.RideDark2,R.style.RideDark3,R.style.RideDark4} : new int[]{R.style.RideLight0,R.style.RideLight1,R.style.RideLight2,R.style.RideLight3,R.style.RideLight4};
        a.setTheme(themes[index]);
        rendered.put(a,key(a));
    }
    public static void update(Context c) { RideStyle.apply(isDark(c)); }
    public static void install(Application app) { update(app); app.registerActivityLifecycleCallbacks(new RideAppearance()); }
    public static void refresh() { MAIN.post(() -> {
        for(Activity a : new java.util.ArrayList<>(rendered.keySet())) {
            if(a != null && !a.isFinishing() && !a.isDestroyed() && !key(a).equals(rendered.get(a))) {
                // Mark before recreating so concurrent provider updates do not queue duplicate recreations.
                rendered.put(a,key(a)); a.recreate();
            }
        }
    }); }
    public static void show(Activity a) {
        new AlertDialog.Builder(a).setTitle("Appearance")
            .setSingleChoiceItems(MODES,Math.max(0,Math.min(2,mode(a))),(dialog,which) -> {
                prefs(a).edit().putInt("appearance_mode",which).apply();
                dialog.dismiss();  refresh();
            }).setNegativeButton("Cancel",null).show();
    }
    public static String description(Context c) { return MODES[Math.max(0,Math.min(2,mode(c)))]; }
    @Override public void onActivityResumed(Activity a) {
        if(!key(a).equals(rendered.get(a))) { refresh(); return; }
        update(a); 
    }
    @Override public void onActivityCreated(Activity a,Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { rendered.remove(a); }
    public static boolean followsHost(Context c) { return prefs(c).getBoolean("follow_host",true); }
    public static void receive(Context c,Bundle extras) { receive(c,extras,null); }
    public static void receive(Context c,Bundle extras,String caller) {
        int mode=extras.getInt("appearanceMode",-1), accent=extras.getInt("appearanceAccent",-1);
        if(mode<0 || mode>2 || accent<0 || accent>4) throw new IllegalArgumentException("Invalid appearance");
        SharedPreferences.Editor edit=prefs(c).edit().putInt("host_mode",mode).putInt("host_accent",accent);
        if(caller!=null)edit.putString("host_name",caller);edit.apply();
        if(followsHost(c)) refresh();
    }
    public static void showOptions(Activity a) {
        String[] labels={"Follow RideDeck", "System", "Light", "Dark"};
        new AlertDialog.Builder(a).setTitle("Appearance")
            .setSingleChoiceItems(labels,followsHost(a)?0:Math.max(0,Math.min(2,mode(a)))+1,(d,i)->{
                prefs(a).edit().putInt("color_theme",accentIndex(a)).putBoolean("follow_host",i==0).putInt("appearance_mode",Math.max(0,i-1)).apply();
                d.dismiss();refresh();
            }).setNegativeButton("Cancel",null).show();
    }
    public static void showAccent(Activity a) {
        new AlertDialog.Builder(a).setTitle("Color theme")
            .setSingleChoiceItems(new String[]{"Lime","Ice blue","Amber","White","RideDeck blue"},accentIndex(a),(d,i)->{
                prefs(a).edit().putBoolean("follow_host",false).putInt("appearance_mode",mode(a)).putInt("color_theme",i).apply();
                d.dismiss();refresh();
            }).setNegativeButton("Cancel",null).show();
    }
}
