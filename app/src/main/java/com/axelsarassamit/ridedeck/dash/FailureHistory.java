package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** Only fixed event codes cross the diagnostic boundary. No exception or media data is accepted. */
final class FailureHistory {
    static final int LIMIT = 30;
    enum Event {
        RECORDING_START("Recording could not start"),
        CAMERA_PREVIEW("Camera preview could not start or was interrupted"),
        RECORDING_INTERRUPTED("Recording stopped unexpectedly"),
        CLIP_FINALIZATION("A recording clip could not be finalized"),
        EXPORT_PREPARATION("Video export preparation failed"),
        EXPORT_RENDER("Video rendering failed"),
        EXPORT_SAVE("Export could not be saved"),
        RIDE_DATA_EXPORT("Ride data export failed"),
        BACKUP_EXPORT("Recording backup failed");

        final String label;
        Event(String label) { this.label = label; }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("dash-failure-history", Context.MODE_PRIVATE);
    }

    static synchronized void add(Context context, Event event) {
        SharedPreferences prefs = prefs(context);
        prefs.edit().putString("events", append(prefs.getString("events", ""), event)).apply();
    }

    static synchronized List<Event> read(Context context) {
        return decode(prefs(context).getString("events", ""));
    }

    static synchronized void clear(Context context) {
        prefs(context).edit().remove("events").apply();
    }

    static List<Event> decode(String stored) {
        List<Event> events = new ArrayList<>();
        for (String code : stored.split("\n")) {
            if (events.size() == LIMIT) break;
            try { events.add(Event.valueOf(code)); }
            catch (IllegalArgumentException ignored) { /* Ignore unknown or malformed entries. */ }
        }
        return events;
    }

    static String append(String stored, Event event) {
        StringBuilder result = new StringBuilder(event.name());
        List<Event> previous = decode(stored);
        for (int i = 0; i < Math.min(previous.size(), LIMIT - 1); i++) {
            result.append('\n').append(previous.get(i).name());
        }
        return result.toString();
    }

    static String report(List<Event> events) {
        StringBuilder result = new StringBuilder("RideDeck Dash ").append(BuildConfig.VERSION_NAME)
            .append("\nLocal failure history (newest first)");
        if (events.isEmpty()) result.append("\nNo failures recorded yet.");
        for (Event event : events) result.append("\n").append(event.name()).append(": ").append(event.label);
        return result.toString();
    }
}
