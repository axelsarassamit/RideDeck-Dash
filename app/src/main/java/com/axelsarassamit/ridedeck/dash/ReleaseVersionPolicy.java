package com.axelsarassamit.ridedeck.dash;

/** New epoch for future builds, preserving immutable identities of the mistaken legacy releases. */
final class ReleaseVersionPolicy {
    static int encoded(String tag) {
        if (tag == null || !tag.matches("v[0-9]{1,3}\\.[0-9]{1,2}\\.[0-9]{1,2}"))
            throw new IllegalArgumentException("Invalid release version");
        String[] parts = tag.substring(1).split("\\.");
        return 100000 + Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100 + Integer.parseInt(parts[2]);
    }

    static int offeredCode(String tag, long releaseId) {
        if (releaseId <= 0) throw new IllegalArgumentException("Missing GitHub release identity");
        int code = encoded(tag);
        // GitHub release IDs recorded read-only on 12 November 2026. A later explicitly
        // recreated production release has a NEW ID and uses the new epoch normally.
        long[] ids = {441221047L,441248249L,441260211L,441272125L,441282256L};
        String[] tags = {"v1.0.0","v1.3.4","v1.3.5","v1.3.6","v1.3.8"};
        for (int i = 0; i < ids.length; i++) if (ids[i] == releaseId) {
            if (!tags[i].equals(tag)) throw new IllegalArgumentException("Legacy release identity does not match tag");
            return code - 100000;
        }
        return code;
    }
}
