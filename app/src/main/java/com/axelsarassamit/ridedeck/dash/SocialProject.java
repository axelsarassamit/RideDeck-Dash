package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import java.io.*;
import java.util.*;

/** Serializable edit recipe. Sources are always revalidated before preview/export. */
final class SocialProject {
    static final int MAX_CLIPS=30;
    static final long MAX_DURATION_MS=30*60*1000L;
    static final class Clip {
        final File file; final long durationMs;
        long fromMs,toMs;
        Clip(File file,long durationMs,long fromMs,long toMs) {
            this.file=file;this.durationMs=durationMs;this.fromMs=fromMs;this.toMs=toMs;
        }
        long lengthMs(){return toMs-fromMs;}
    }
    final List<Clip> clips=new ArrayList<>();
    boolean pip,merge=true,fillRear=true; int corner=1,size=28,shiftMs;
    int pipX=-1,pipY=-1;
    static SocialProject load(Context c,Bundle b)throws Exception {
        if(b==null)throw new IOException("Select recordings first");
        ArrayList<String> paths=b.getStringArrayList("paths");
        if(paths==null||paths.isEmpty()||paths.size()>MAX_CLIPS)throw new IOException("Choose 1 to "+MAX_CLIPS+" clips");
        long[] starts=b.getLongArray("starts"),ends=b.getLongArray("ends");
        if((starts!=null&&starts.length!=paths.size())||(ends!=null&&ends.length!=paths.size()))throw new IOException("Invalid trim list");
        SocialProject p=new SocialProject();p.pip=b.getBoolean("pip");p.merge=b.getBoolean("merge",true);
        p.fillRear=b.getBoolean("fillRear",true);
        p.pipX=b.getInt("pipX",-1);p.pipY=b.getInt("pipY",-1);
        if(p.pipX < -1||p.pipX>85||p.pipY < -1||p.pipY>85)throw new IOException("Invalid PiP position");
        p.corner=b.getInt("corner",1);p.size=b.getInt("size",28);p.shiftMs=b.getInt("shift",0);
        if(p.corner<0||p.corner>3||p.size<15||p.size>40||Math.abs((long)p.shiftMs)>5000)throw new IOException("Invalid picture-in-picture settings");
        for(int i=0;i<paths.size();i++) {
            File f=validate(c,new File(paths.get(i)));
            long duration=duration(f),from=starts==null?0:starts[i],to=ends==null?duration:ends[i];
            if(from<0||to>duration||to-from<500)throw new IOException("Each trim needs at least half a second");
            p.clips.add(new Clip(f,duration,from,to));
        }
        return p;
    }
    static File validate(Context c,File f)throws IOException {
        File actual=f.getCanonicalFile(),root=new ClipStore(c).root().getCanonicalFile();
        if(!actual.getPath().startsWith(root.getPath()+File.separator)||!actual.isFile()||!ClipStore.finalized(actual))throw new IOException("A selected original is missing or not finalized");
        return actual;
    }
    static long duration(File f)throws Exception {
        try(MediaMetadataRetriever r=new MediaMetadataRetriever()) { r.setDataSource(f.getPath());return Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)); }
    }
    long durationMs(){long n=0;for(Clip c:clips)n+=c.lengthMs();return n;}
    Bundle bundle() {
        Bundle b=new Bundle();ArrayList<String> paths=new ArrayList<>();long[] starts=new long[clips.size()],ends=new long[clips.size()];
        for(int i=0;i<clips.size();i++){Clip c=clips.get(i);paths.add(c.file.getPath());starts[i]=c.fromMs;ends[i]=c.toMs;}
        b.putStringArrayList("paths",paths);b.putLongArray("starts",starts);b.putLongArray("ends",ends);b.putBoolean("pip",pip);b.putBoolean("merge",merge);b.putBoolean("fillRear",fillRear);b.putInt("corner",corner);b.putInt("size",size);b.putInt("shift",shiftMs);b.putInt("pipX",pipX);b.putInt("pipY",pipY);return b;
    }
    SocialProject single(int index) {
        SocialProject p=new SocialProject();p.clips.add(clips.get(index));p.pip=pip;p.fillRear=fillRear;p.corner=corner;p.size=size;p.shiftMs=shiftMs;p.pipX=pipX;p.pipY=pipY;return p;
    }
    static Bundle selection(Collection<String> paths) { Bundle b=new Bundle();b.putStringArrayList("paths",new ArrayList<>(paths));return b; }
    /** No wall-clock guesses: only same-ride monotonic start records are used for PiP. */
    static Map<String,Long> anchors(File ride)throws IOException {
        Map<String,Long> map=new HashMap<>();File log=new File(ride,"capture.csv");if(!log.isFile())return map;
        try(BufferedReader r=new BufferedReader(new FileReader(log))){r.readLine();String line;while((line=r.readLine())!=null){String[] v=line.split(",",-1);if(v.length>7&&v[4].equals("file_started"))try{map.putIfAbsent(v[7],Long.parseLong(v[1])/1000);}catch(NumberFormatException ignored){}}}
        return map;
    }
}
