package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

public final class DashApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static volatile int visible;
    private static volatile long rotationUntil;
    private static DashApplication instance;
    private ChargerGrace chargerGrace;
    private android.content.SharedPreferences powerPrefs;
    public static synchronized long chargerSecondsLeft() {
        return Math.max(0, (ChargerGrace.TIMEOUT_MS-instance.chargerGrace.elapsed(false,SystemClock.elapsedRealtime())+999)/1000);
    }
    public static synchronized RecordingGate.Result recordingGate(android.content.Intent battery, boolean visible, boolean activeRide) {
        boolean plugged = battery != null && battery.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int percent = battery == null ? -1 : RecordingGate.batteryPercent(battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1), battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,-1));
        long now = SystemClock.elapsedRealtime();
        instance.observePower(plugged, now);
        return RecordingGate.recording(plugged, percent, visible, activeRide, instance.chargerGrace.elapsed(plugged, now));
    }
    private synchronized void observePower(boolean plugged, long now) {
        long since = chargerGrace.observe(plugged, now);
        if (powerPrefs.getLong("since", -2) != since) powerPrefs.edit().putLong("since", since).commit();
    }
    public static synchronized boolean isVisible() {
        return visible > 0 || SystemClock.elapsedRealtime() < rotationUntil;
    }
    @Override public void onCreate() {
        super.onCreate(); RideAppearance.install(this); instance = this;
        powerPrefs = getSharedPreferences("charger-grace", MODE_PRIVATE);
        int boot = android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1);
        long saved = boot >= 0 && powerPrefs.getInt("boot", -2) == boot ? powerPrefs.getLong("since", -1) : -1;
        chargerGrace = new ChargerGrace(saved);
        powerPrefs.edit().putInt("boot", boot).apply();
        registerReceiver(new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context context, android.content.Intent battery) {
                observePower(battery.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0, SystemClock.elapsedRealtime());
            }
        }, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        DashSettings.migrateMusicSafeAudio(this); registerActivityLifecycleCallbacks(this);
    }
    @Override public synchronized void onActivityStarted(Activity activity) { visible++; rotationUntil = 0; }
    @Override public synchronized void onActivityStopped(Activity activity) {
        visible = Math.max(0, visible - 1);
        if (activity.isChangingConfigurations()) rotationUntil = SystemClock.elapsedRealtime() + 3000;
    }
    @Override public void onActivityCreated(Activity a, Bundle b) { ScreenChrome.apply(a.getWindow(), a instanceof DashboardActivity); }
    @Override public void onActivityResumed(Activity a) { ScreenChrome.apply(a.getWindow(), a instanceof DashboardActivity); }
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
