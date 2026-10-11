package com.axelsarassamit.ridedeck.dash;

/** Pure policy after Android Binder identity and certificate verification.
 * Package-name extras alone must never authorize a recording session. */
public final class RideSessionPolicy {
    public static final String STANDARD = "com.axelsarassamit.gx12";
    public static final String YAMAHA = "com.axelsarassamit.gx12.yamaha";
    public static final long LEASE_MS = 15_000;
    private String owner;
    private long generation;
    private long heartbeat = -1;

    public synchronized boolean renew(String verifiedPackage, long elapsedMs) {
        if (elapsedMs < 0 || (!STANDARD.equals(verifiedPackage) && !YAMAHA.equals(verifiedPackage))) return false;
        if (isActive(elapsedMs) && !verifiedPackage.equals(owner)) return false;
        if(!isActive(elapsedMs))generation++;
        owner = verifiedPackage;
        heartbeat = elapsedMs;
        return true;
    }
    public synchronized boolean isActive(long elapsedMs) {
        return owner != null && elapsedMs >= heartbeat && elapsedMs - heartbeat < LEASE_MS;
    }
    public synchronized long generation(){return generation;}
    public synchronized void stop(String verifiedPackage) {
        if (verifiedPackage != null && verifiedPackage.equals(owner)) { owner = null; heartbeat = -1; }
    }
}
