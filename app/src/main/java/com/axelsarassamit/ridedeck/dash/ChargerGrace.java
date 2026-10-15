package com.axelsarassamit.ridedeck.dash;

/** Monotonic unplugged budget shared by all recording entry points. */
final class ChargerGrace {
    static final long TIMEOUT_MS = 5 * 60 * 1000L;
    private long disconnectedSince;
    ChargerGrace(long savedSince) { disconnectedSince = savedSince; }
    synchronized long observe(boolean plugged, long now) {
        if (plugged) disconnectedSince = -1;
        else if (disconnectedSince < 0 || disconnectedSince > now) disconnectedSince = now;
        return disconnectedSince;
    }
    synchronized long elapsed(boolean plugged, long now) {
        long since = observe(plugged, now);
        return plugged ? 0 : now - since;
    }
}
