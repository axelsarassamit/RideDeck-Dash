package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.common.MimeTypes;
import androidx.media3.transformer.*;
import org.junit.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Explicit opt-in comparison using an existing finalized original, without changing it. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class RearStabilizationTest {
    @Test public void exportRoadEvidence()throws Exception {
        Bundle args=InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Explicit road comparison only","1".equals(args.getString("roadEvidence")));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertFalse(RecordingService.running||RecordingService.finalizing);
        String ride=args.getString("ride","1792302539718"),name=args.getString("clip","rear-0002.mp4"),label=args.getString("label","comparison");
        assertTrue(ride.matches("[0-9]+")&&name.matches("rear-[0-9]+\\.mp4")&&label.matches("[a-zA-Z0-9-]+"));
        File source=new File(new File(new ClipStore(c).root(),ride),name);assertTrue(ClipStore.finalized(source));
        SocialProject p=SocialProject.load(c,SocialProject.selection(Collections.singletonList(source.getPath())));
        p.clips.get(0).fromMs=Long.parseLong(args.getString("startMs","40000"));
        p.clips.get(0).toMs=p.clips.get(0).fromMs+Long.parseLong(args.getString("durationMs","20000"));
        Map<String,Object> settings=new HashMap<>();settings.put("social_ratio",Integer.parseInt(args.getString("ratio","3")));settings.put("social_quality",720);settings.put("social_bitrate",8);settings.put("social_overlay",0);settings.put("overlay_roads",0);settings.put("studio_road",Integer.parseInt(args.getString("roadMode","1")));settings.put("studio_stabilize",Integer.parseInt(args.getString("strength","3")));
        SocialComposition.Built built=SocialComposition.build(c,p,new DashSettings(c,settings),true,()->false);
        File folder=c.getExternalFilesDir("rear-audit");assertNotNull(folder);assertTrue(folder.isDirectory()||folder.mkdirs());File output=new File(folder,label+".mp4");assertFalse("Use a fresh label",output.exists());
        // Copy the exact most recent local audit for this source alongside opt-in comparisons.
        File[] reports=source.getParentFile().listFiles((d,n)->n.startsWith("stabilization-"+name.replace(".mp4","")+"-")&&n.endsWith(".csv"));
        if(reports!=null&&reports.length>0){Arrays.sort(reports,Comparator.comparingLong(File::lastModified).reversed());try(InputStream in=new FileInputStream(reports[0])){java.nio.file.Files.copy(in,new File(folder,label+".csv").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}
        if("1".equals(args.getString("auditOnly")))return;
        CountDownLatch done=new CountDownLatch(1);Throwable[] error={null};Transformer[] transformer={null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{transformer[0]=new Transformer.Builder(c).setPortraitEncodingEnabled(true).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC).addListener(new Transformer.Listener(){
            @Override public void onCompleted(Composition composition,ExportResult result){android.util.Log.i("DashRoadAudit","frames="+result.videoFrameCount+" durationMs="+result.durationMs);done.countDown();}
            @Override public void onError(Composition composition,ExportResult result,ExportException e){error[0]=e;done.countDown();}
        }).build();transformer[0].start(built.composition,output.getPath());});
        if(!done.await(240,TimeUnit.SECONDS)){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->transformer[0].cancel());fail("Road export timed out");}
        if(error[0]!=null)throw new AssertionError(error[0]);assertTrue(output.length()>1000);
        if("1".equals(args.getString("verifyCadence")))assertEquals("Keep every original picture in the selected range",frames(source,p.clips.get(0).fromMs*1000,p.clips.get(0).toMs*1000),frames(output,0,Long.MAX_VALUE));
        System.out.println("Road comparison: "+output.getPath());
    }
    static int frames(File file,long startUs,long endUs)throws Exception {
        android.media.MediaExtractor extractor=new android.media.MediaExtractor();
        try{extractor.setDataSource(file.getPath());for(int i=0;i<extractor.getTrackCount();i++)if(extractor.getTrackFormat(i).getString("mime").startsWith("video/")){extractor.selectTrack(i);break;}
            int count=0;while(extractor.getSampleTime()>=0){long time=extractor.getSampleTime();if(time>=startUs&&time<endUs)count++;extractor.advance();}return count;
        }finally{extractor.release();}
    }
}
