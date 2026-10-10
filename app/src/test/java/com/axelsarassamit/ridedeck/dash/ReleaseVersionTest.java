package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseVersionTest {
    @Test public void versionIncreasesAcrossBoundaries() {
        assertEquals(100, ReleaseUpdater.versionCode("v0.1.0"));
        assertTrue(ReleaseUpdater.versionCode("v0.2.0") > ReleaseUpdater.versionCode("v0.1.99"));
        assertTrue(ReleaseUpdater.versionCode("v1.0.0") > ReleaseUpdater.versionCode("v0.99.99"));
    }
    @Test public void rejectsNonReleaseTags() {
        for (String value : new String[]{"v0.1.0-beta", "v0.100.0", "0.1.0", "v-1.0.0"}) {
            try { ReleaseUpdater.versionCode(value); fail(value); } catch (IllegalArgumentException expected) { }
        }
    }
}
