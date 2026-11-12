package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompanionCallerPolicyTest {
 @Test public void releaseHostsAlwaysNeedPinnedCertificate(){for(boolean debug:new boolean[]{false,true})for(String host:new String[]{RideSessionPolicy.STANDARD,RideSessionPolicy.YAMAHA}){
  assertEquals(host,CompanionCallerPolicy.canonicalHost(host,CompanionCallerPolicy.HOST_CERT,debug,"local-debug"));
  assertNull(CompanionCallerPolicy.canonicalHost(host,"local-debug",debug,"local-debug"));
  assertNull(CompanionCallerPolicy.canonicalHost(host,null,debug,null));
 }}
 @Test public void debugHostsNeedDebugDashAndMatchingSigner(){for(String host:new String[]{RideSessionPolicy.STANDARD,RideSessionPolicy.YAMAHA}){
  String testHost=host+".preview";
  assertEquals(host,CompanionCallerPolicy.canonicalHost(testHost,"local-debug",true,"local-debug"));
  assertNull(CompanionCallerPolicy.canonicalHost(testHost,"other-debug",true,"local-debug"));
  assertNull(CompanionCallerPolicy.canonicalHost(testHost,"local-debug",false,"local-debug"));
  assertNull(CompanionCallerPolicy.canonicalHost(testHost,CompanionCallerPolicy.HOST_CERT,false,CompanionCallerPolicy.HOST_CERT));
  assertNull(CompanionCallerPolicy.canonicalHost(testHost,null,true,null));
  assertNull(CompanionCallerPolicy.canonicalHost(testHost,"",true,""));
 }}
 @Test public void unrelatedOrLookalikePackagesCannotBorrowTrustedSigner(){for(String name:new String[]{"other.app",RideSessionPolicy.STANDARD+".preview.extra",RideSessionPolicy.YAMAHA+"2",RideSessionPolicy.STANDARD+".debug"}){
  assertNull(CompanionCallerPolicy.canonicalHost(name,CompanionCallerPolicy.HOST_CERT,true,CompanionCallerPolicy.HOST_CERT));
 }}
 @Test public void canonicalDebugIdentityCanOwnLease(){String host=CompanionCallerPolicy.canonicalHost(RideSessionPolicy.YAMAHA+".preview","key",true,"key");RideSessionPolicy sessions=new RideSessionPolicy();assertTrue(sessions.renew(host,10));assertTrue(sessions.isOwnedBy(host,11));}
}
