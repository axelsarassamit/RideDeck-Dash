package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Synthetic known camera/gyro/readout motion through the real decoder, tracker, GPU and encoder. */
public class HybridExportTest {
    SocialExportTest harness;File clip;
    @Before public void setup()throws Exception {
        harness=new SocialExportTest();harness.setup();clip=new File(harness.ride,"rear-0001.mp4");
        for(String name:new String[]{"rear.mp4","gyro.csv","frames.csv","motion.json"}){
            File dest=name.equals("rear.mp4")?clip:new File(harness.ride,name);
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("hybrid/"+name);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        }
        ClipStore.mark(clip,".ready");
    }
    @After public void cleanup(){if(harness!=null)harness.cleanup();}
    @Test public void calibratedHybridExportsEveryFrameAndPreservesAudio()throws Exception {
        verifyCalibratedExport();
    }
    @Test public void portraitReadoutUsesRotatedSensorScanDirection()throws Exception {
        File rotated=new File(harness.ride,"rotated.mp4");android.media.MediaExtractor e=new android.media.MediaExtractor();android.media.MediaMuxer m=new android.media.MediaMuxer(rotated.getPath(),android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        try{e.setDataSource(clip.getPath());int[] tracks=new int[e.getTrackCount()];for(int i=0;i<tracks.length;i++){tracks[i]=m.addTrack(e.getTrackFormat(i));e.selectTrack(i);}m.setOrientationHint(90);m.start();
            java.nio.ByteBuffer buffer=java.nio.ByteBuffer.allocate(1024*1024);android.media.MediaCodec.BufferInfo info=new android.media.MediaCodec.BufferInfo();while(e.getSampleTrackIndex()>=0){buffer.clear();int size=e.readSampleData(buffer,0);if(size<0)break;info.set(0,size,e.getSampleTime(),e.getSampleFlags());m.writeSampleData(tracks[e.getSampleTrackIndex()],buffer,info);e.advance();}m.stop();
        }finally{e.release();m.release();}assertTrue(clip.delete());assertTrue(rotated.renameTo(clip));verifyCalibratedExport();
    }
    void verifyCalibratedExport()throws Exception {
        SocialProject p=SocialProject.load(harness.c,SocialProject.selection(Collections.singletonList(clip.getPath())));
        p.clips.get(0).fromMs=1000;p.clips.get(0).toMs=9000;
        Map<String,Object> settings=new HashMap<>(harness.recipe.snapshot);settings.put("studio_stabilize",2);settings.put("studio_hybrid",1);
        File output=harness.render(p,new DashSettings(harness.c,settings));
        assertEquals(RearStabilizationTest.frames(clip,1000000,9000000),RearStabilizationTest.frames(output,0,Long.MAX_VALUE));
        try(MediaMetadataRetriever r=new MediaMetadataRetriever()){r.setDataSource(output.getPath());assertEquals("yes",r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));assertEquals(8000,Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),100);assertNotNull(r.getFrameAtTime(3000000));}
        JSONObject report=new JSONObject(new String(Files.readAllBytes(new File(harness.ride,"hybrid-rear-0001-report.json").toPath()),StandardCharsets.UTF_8));
        assertTrue(report.getDouble("heldout_agreement")>.80);assertEquals(42,report.getDouble("clock_offset_ms"),12);assertTrue(report.getDouble("zoom")<=1.65);assertFalse(report.getBoolean("outdoor_validated"));
        File folder=harness.c.getExternalFilesDir("tests");assertNotNull(folder);if(!folder.isDirectory())assertTrue(folder.mkdirs());
        try(InputStream in=new FileInputStream(output);OutputStream out=new FileOutputStream(new File(folder,"synthetic-hybrid.mp4"))){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    }
    @Test public void nativeStabilizedEvidenceIsRejected()throws Exception {
        File frames=new File(harness.ride,"frames.csv");String s=new String(Files.readAllBytes(frames.toPath()),StandardCharsets.UTF_8);s=s.replace(",0,0,0,4,",",0,1,0,4,");Files.write(frames.toPath(),s.getBytes(StandardCharsets.UTF_8));
        try{HybridEvidence.load(clip,0,9000000,()->false);fail("Do not apply raw gyro correction over OIS");}catch(IOException expected){assertTrue(expected.getMessage().contains("native stabilization"));}
    }
    @Test public void incompleteMotionIsRejected()throws Exception {
        File meta=new File(harness.ride,"motion.json");JSONObject j=new JSONObject(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8));j.put("dropped_rows",1);Files.write(meta.toPath(),j.toString().getBytes(StandardCharsets.UTF_8));
        try{HybridEvidence.load(clip,0,9000000,()->false);fail("Incomplete evidence cannot enable hybrid correction");}catch(IOException expected){assertTrue(expected.getMessage().contains("incomplete"));}
    }
}
