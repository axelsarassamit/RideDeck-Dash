package com.axelsarassamit.ridedeck.dash;

/** Automatic camera audio must not move music onto a Bluetooth voice connection. */
final class RecordingAudioPolicy {
    static boolean bluetooth(String key) {
        // Android input types: Bluetooth SCO = 7, BLE headset = 26.
        return "headset".equals(key) || key.startsWith("7:") || key.startsWith("26:");
    }
    static String migrateLegacyInput(String key) {
        return bluetooth(key) ? "auto" : key;
    }
    static boolean automaticPhone(String key) { return "auto".equals(key); }
}
