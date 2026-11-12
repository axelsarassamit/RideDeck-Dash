package com.axelsarassamit.migrationhelper;

import java.util.Map;

/** Pure decisions shared by migration operations and host regression tests. */
final class RestorePolicy {
    private RestorePolicy(){}
    static boolean fileTimesPath(String path){
        if(path==null||path.length()>1024||!path.startsWith("files/recordings/")||path.indexOf('\\')>=0||path.indexOf(':')>=0)return false;
        for(String part:path.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals("..")||part.matches(".*[\\x00-\\x1f\\x7f].*"))return false;
        return true;
    }
    static boolean fileTimeMillis(long value){return value>=0&&value<=253402300799000L;}
    static boolean validPreviewCopyPause(boolean preview,String value,long maxBytes){
        return preview&&value!=null&&value.matches("[1-9][0-9]{0,9}")&&Long.parseLong(value)<=maxBytes;
    }
    static boolean sameOwnedFile(long dev,long ino,long actualDev,long actualIno,long bytes,long expectedBytes){
        return dev>=0&&ino>0&&dev==actualDev&&ino==actualIno&&bytes>=0&&bytes<=expectedBytes;
    }
    static boolean matchingPrefixChunk(byte[] current,byte[] source,int count){
        if(count<0||count>current.length||count>source.length)return false;
        for(int i=0;i<count;i++)if(current[i]!=source[i])return false;
        return true;
    }
    static boolean needsPreferenceWrite(boolean fileExists,Map<String,?> current,Map<String,?> expected){
        return !fileExists||!current.equals(expected);
    }
    static boolean safePreferenceRollback(Map<String,?> cache,Map<String,?> disk,Map<String,?> expected){
        return cache.equals(disk)&&(cache.isEmpty()||cache.equals(expected));
    }
}
