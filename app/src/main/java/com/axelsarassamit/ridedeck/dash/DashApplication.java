package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

public final class DashApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static volatile int visible;
    private static volatile long rotationUntil;
    public static synchronized boolean isVisible() {
        return visible > 0 || SystemClock.elapsedRealtime() < rotationUntil;
    }
    @Override public void onCreate() { super.onCreate(); registerActivityLifecycleCallbacks(this); }
    @Override public synchronized void onActivityStarted(Activity activity) { visible++; rotationUntil = 0; }
    @Override public synchronized void onActivityStopped(Activity activity) {
        visible = Math.max(0, visible - 1);
        if (activity.isChangingConfigurations()) rotationUntil = SystemClock.elapsedRealtime() + 3000;
    }
    @Override public void onActivityCreated(Activity a, Bundle b) {}
    @Override public void onActivityResumed(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
