package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class RideSessionPolicyTest {
    @Test public void recordingRequiresCurrentLeaseOwner() {
        RideSessionPolicy p = new RideSessionPolicy();
        assertFalse(p.isOwnedBy(RideSessionPolicy.STANDARD, 0));
        p.renew(RideSessionPolicy.STANDARD, 100);
        assertTrue(p.isOwnedBy(RideSessionPolicy.STANDARD, 101));
        assertFalse(p.isOwnedBy(RideSessionPolicy.YAMAHA, 101));
        assertFalse(p.isOwnedBy(RideSessionPolicy.STANDARD, 15100));
        p.renew(RideSessionPolicy.YAMAHA, 15100);
        assertTrue(p.isOwnedBy(RideSessionPolicy.YAMAHA, 15101));
        assertFalse(p.isOwnedBy(RideSessionPolicy.STANDARD, 15101));
    }
    @Test public void onlyCompanionsAndExclusiveOwner() {
        RideSessionPolicy policy = new RideSessionPolicy();
        assertFalse(policy.renew("other.app", 0));
        assertFalse(policy.renew(RideSessionPolicy.STANDARD, -1));
        assertTrue(policy.renew(RideSessionPolicy.STANDARD, 100));
        assertFalse(policy.renew(RideSessionPolicy.YAMAHA, 101));
        policy.stop(RideSessionPolicy.YAMAHA);
        assertTrue(policy.isActive(102));
        policy.stop(RideSessionPolicy.STANDARD);
        assertFalse(policy.isActive(103));
        assertTrue(policy.renew(RideSessionPolicy.YAMAHA, 104));
    }
    @Test public void generationChangesOnlyForNewRide(){RideSessionPolicy p=new RideSessionPolicy();p.renew(RideSessionPolicy.STANDARD,0);long first=p.generation();p.renew(RideSessionPolicy.STANDARD,5000);assertEquals(first,p.generation());p.stop(RideSessionPolicy.STANDARD);p.renew(RideSessionPolicy.STANDARD,6000);assertEquals(first+1,p.generation());}
    @Test public void expiryClockAndTakeover() {
        RideSessionPolicy policy = new RideSessionPolicy();
        assertFalse(policy.isActive(0));
        assertTrue(policy.renew(RideSessionPolicy.STANDARD, 100));
        assertFalse(policy.isActive(99));
        assertTrue(policy.isActive(15099));
        assertFalse(policy.isActive(15100));
        assertTrue(policy.renew(RideSessionPolicy.YAMAHA, 15100));
    }
}
