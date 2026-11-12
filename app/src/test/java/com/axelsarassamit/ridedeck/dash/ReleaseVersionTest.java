package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseVersionTest {
    @Test public void versionIncreasesAcrossBoundaries() {
        assertEquals(100100, ReleaseUpdater.versionCode("v0.1.0"));
        assertTrue(ReleaseUpdater.versionCode("v0.2.0") > ReleaseUpdater.versionCode("v0.1.99"));
        assertTrue(ReleaseUpdater.versionCode("v1.0.0") > ReleaseUpdater.versionCode("v0.99.99"));
    }
    @Test public void developmentEpochExceedsAllKnownLegacyApkCodes(){
        int development=ReleaseUpdater.versionCode("v0.13.7");assertEquals(101307,development);
        for(int previous:new int[]{1302,1303,1304,1305,1306,10000,10304,10305,10306,10308})assertTrue(development>previous);
    }
    @Test public void futureProductionOnePointZeroUpdatesDevelopment(){
        assertEquals(110000,ReleaseUpdater.versionCode("v1.0.0"));
        assertTrue(ReleaseUpdater.versionCode("v1.0.0")>ReleaseUpdater.versionCode("v0.13.7"));
        assertTrue(ReleaseUpdater.versionCode("v1.0.0")>ReleaseUpdater.versionCode("v0.99.99"));
    }
    @Test public void staleLatestLegacyReleaseNeverOffersDowngradeAfterEpochMigration(){
        int installed=101307;
        long[] ids={441221047L,441248249L,441260211L,441272125L,441282256L};
        String[] tags={"v1.0.0","v1.3.4","v1.3.5","v1.3.6","v1.3.8"};
        for(int i=0;i<ids.length;i++)assertTrue(ReleaseVersionPolicy.offeredCode(tags[i],ids[i])<installed);
        assertEquals(10308,ReleaseVersionPolicy.offeredCode("v1.3.8",441282256L));
        assertEquals(101307,ReleaseVersionPolicy.offeredCode("v0.13.7",500000001L));
    }
    @Test public void explicitFutureProductionReleaseHasNewIdentityAndHigherCode(){
        assertEquals(10000,ReleaseVersionPolicy.offeredCode("v1.0.0",441221047L));
        assertEquals(110000,ReleaseVersionPolicy.offeredCode("v1.0.0",500000000L));
        assertTrue(ReleaseVersionPolicy.offeredCode("v1.0.0",500000000L)>101307);
    }
    @Test public void missingOrMismatchedReleaseIdentityFailsClosed(){
        for(long id:new long[]{-1,0})try{ReleaseVersionPolicy.offeredCode("v1.3.8",id);fail();}catch(IllegalArgumentException expected){}
        try{ReleaseVersionPolicy.offeredCode("v1.0.1",441282256L);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void currentBuildPreservesVariantIdentityAndUpdateEpoch(){
        assertTrue(BuildConfig.VERSION_CODE > 10308);
        assertEquals("com.axelsarassamit.ridedeck.dash"+(BuildConfig.DEBUG?".debug":""),BuildConfig.APPLICATION_ID);
        assertEquals(BuildConfig.DEBUG,BuildConfig.VERSION_NAME.endsWith("-dev"));
        String name=BuildConfig.VERSION_NAME.replace("-dev","");
        assertEquals(ReleaseUpdater.versionCode("v"+name),BuildConfig.VERSION_CODE);
    }
    @Test public void rejectsNonReleaseTags() {
        for (String value : new String[]{"v0.1.0-beta", "v0.100.0", "0.1.0", "v-1.0.0"}) {
            try { ReleaseUpdater.versionCode(value); fail(value); } catch (IllegalArgumentException expected) { }
        }
    }
}
