package com.axelsarassamit.ridedeck.dash;

/** A cached preview image must never turn a camera error into a Ready state. */
final class CameraViewStatus {
    private CameraViewStatus() { }
    static String label(String message, boolean hasFrame) {
        if (message == null || message.trim().isEmpty()) return "! Camera view unavailable";
        if ("Live camera view".equals(message)) return hasFrame ? "○ Ready · Live camera" : "◌ Connecting · Camera view";
        if ("Opening cameras".equals(message)) return "◌ Connecting · Opening cameras";
        String state = message.toLowerCase(java.util.Locale.ROOT);
        if (state.contains("unavailable") || state.contains("disconnected") || state.contains("error") || state.contains("failed")) return "! " + message;
        return message;
    }
}
