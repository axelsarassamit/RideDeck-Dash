package com.axelsarassamit.ridedeck.dash;

/** Evaluated before start and continuously during capture by the future recorder. */
public final class RecordingGate {
    public enum Result { ALLOWED, BATTERY_TOO_LOW, BATTERY_UNKNOWN, COMPANION_REQUIRED }
    public static Result evaluate(boolean plugged, int batteryPercent, boolean visible, boolean authorizedRideActive) {
        if (!plugged) {
            if (batteryPercent < 0 || batteryPercent > 100) return Result.BATTERY_UNKNOWN;
            if (batteryPercent < 50) return Result.BATTERY_TOO_LOW;
        }
        if (!visible && !authorizedRideActive) return Result.COMPANION_REQUIRED;
        return Result.ALLOWED;
    }
    public static int batteryPercent(int level, int scale) {
        if (scale <= 0 || level < 0 || level > scale) return -1;
        return (int) (100L * level / scale);
    }
}
