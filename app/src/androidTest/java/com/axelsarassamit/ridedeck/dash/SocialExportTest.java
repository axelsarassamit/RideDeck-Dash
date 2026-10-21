package com.axelsarassamit.ridedeck.dash;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.common.MimeTypes;
import androidx.media3.transformer.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Real decoder/GPU/encoder checks using synthetic colours, sound and deliberately offset cameras. */
@androidx.annotation.OptIn(markerClass={androidx.media3.common.util.UnstableApi.class,androidx.media3.common.util.ExperimentalApi.class})
public class SocialExportTest {
    Context c;File ride;SocialProject project;DashSettings recipe;final List<File> outputs=new ArrayList<>();final List<Uri> gallery=new ArrayList<>();
    @Before public void setup()throws Exception {
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertFalse("Stop the ride before tests",RecordingService.running);
        ride=new File(new ClipStore(c).root(),"social-test-"+UUID.randomUUID());assertTrue(ride.mkdir());
        String[] assets={"rear-a","rear-b","front-a","front-b"},names={"rear-0001.mp4","rear-0002.mp4","front-0001.mp4","front-0002.mp4"};long[] starts={0,6000,1000,6000};
        try(PrintWriter log=new PrintWriter(new File(ride,"capture.csv"))) {
            log.println("utc_ms,elapsed_ns,sensor_ns,camera,event,eis,ois,file");
            for(int i=0;i<assets.length;i++){
                File f=new File(ride,names[i]);try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("social/"+assets[i]+".mp4");OutputStream out=new FileOutputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                ClipStore.mark(f,".ready");log.println((1700000000000L+starts[i])+","+(starts[i]*1000000L)+",,"+(i<2?"rear":"front")+",file_started,null,null,"+names[i]);
            }
        }
        project=SocialProject.load(c,SocialProject.selection(Arrays.asList(new File(ride,names[0]).getPath(),new File(ride,names[1]).getPath())));
        project.clips.get(0).toMs=3000;project.clips.get(1).fromMs=1000;project.clips.get(1).toMs=4000;
        Map<String,Object> options=new HashMap<>();options.put("social_ratio",3);options.put("social_quality",720);options.put("social_bitrate",4);options.put("social_overlay",0);options.put("social_mute",0);options.put("overlay_roads",0);recipe=new DashSettings(c,options);
    }
    @After public void cleanup() {
        for(Uri uri:gallery)c.getContentResolver().delete(uri,null,null);
        for(File f:outputs)f.delete();
        if(ride!=null&&ride.getName().startsWith("social-test-")){File[] files=ride.listFiles();if(files!=null)for(File f:files)f.delete();ride.delete();}
    }
    File render(SocialProject spec)throws Exception {
        SocialComposition.Built built=SocialComposition.build(c,spec,recipe,true,()->false);
        File output=File.createTempFile("social-verify-",".mp4",c.getCacheDir());output.delete();outputs.add(output);
        CountDownLatch done=new CountDownLatch(1);Throwable[] error={null};Transformer[] exporter={null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{exporter[0]=new Transformer.Builder(c).setPortraitEncodingEnabled(true).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(new Transformer.Listener(){@Override public void onCompleted(Composition composition,ExportResult result){done.countDown();}@Override public void onError(Composition composition,ExportResult result,ExportException e){error[0]=e;done.countDown();}}).build();exporter[0].start(built.composition,output.getPath());});
        if(!done.await(90,TimeUnit.SECONDS)){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->exporter[0].cancel());fail("Composition export timed out");}
        if(error[0]!=null)throw new AssertionError("Real export failed",error[0]);return output;
    }
    int colour(MediaMetadataRetriever r,long timeUs,float x,float y){Bitmap b=r.getFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST);assertNotNull(b);int value=b.getPixel((int)(b.getWidth()*x),(int)(b.getHeight()*y));b.recycle();return value;}
    void red(int v){assertTrue("Expected red, got "+Integer.toHexString(v),Color.red(v)>160&&Color.blue(v)<80&&Color.green(v)<80);}
    void blue(int v){assertTrue("Expected blue, got "+Integer.toHexString(v),Color.blue(v)>160&&Color.red(v)<80);}
    @Test public void mergedOrderDurationAndMainAudioArePreserved()throws Exception {
        File file=render(project);try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(file.getPath());assertEquals(6000,Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),250);
            assertEquals("yes",r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));red(colour(r,1000000,.5f,.5f));blue(colour(r,4500000,.5f,.5f));
        }
    }
    @Test public void pipHidesLeadingGapAndUsesFrontAcrossDifferentRollovers()throws Exception {
        project.pip=true;SocialComposition.Built built=SocialComposition.build(c,project,recipe,true,()->false);assertEquals(1000,built.missingFrontMs);
        File file=render(project);try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(file.getPath());red(colour(r,500000,.93f,.15f));
            int green=colour(r,1500000,.93f,.15f);assertTrue("Front must be green",Color.green(green)>160&&Color.red(green)<80);
            red(colour(r,1500000,.80f,.15f)); // Portrait front retains its shape; it is not stretched across the corner.
            red(colour(r,1500000,.969f,.031f)); // Rounded corner reveals the rear image, not a black rectangle.
            int top=colour(r,1500000,.93f,.07f),bottom=colour(r,1500000,.93f,.27f);
            assertTrue("Front image must stay upright (cyan top)",Color.green(top)>160&&Color.blue(top)>160&&Color.red(top)<80);
            assertTrue("Front image must stay upright (magenta bottom)",Color.red(bottom)>160&&Color.blue(bottom)>160&&Color.green(bottom)<80);
            int yellow=colour(r,4500000,.93f,.15f);assertTrue("Second front must be yellow",Color.red(yellow)>160&&Color.green(yellow)>160&&Color.blue(yellow)<80);
            blue(colour(r,4500000,.5f,.5f));
        }
    }
    @Test public void realBatchSavesTwoIndependentTrimmedCopies()throws Exception {
        project.merge=false;Intent intent=new Intent(c,SocialExportActivity.class).putExtra("project",project.bundle()).putExtra("recipe",recipe.bundle());
        try(ActivityScenario<SocialExportActivity> scenario=ActivityScenario.launch(intent)) {
            boolean complete=false;for(int i=0;i<600;i++) {final boolean[] state={false};final String[] message={""};scenario.onActivity(a->{state[0]=a.completed;message[0]=a.progress.getText().toString();if(a.cancelled&&!a.completed)throw new AssertionError(message[0]);});if(state[0]){complete=true;break;}Thread.sleep(100);}
            assertTrue("Batch export timed out",complete);scenario.onActivity(a->{gallery.addAll(a.saved);assertEquals(2,a.saved.size());});
            for(int i=0;i<gallery.size();i++)try(MediaMetadataRetriever r=new MediaMetadataRetriever()){r.setDataSource(c,gallery.get(i));assertEquals(3000,Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),250);if(i==0)red(colour(r,1000000,.5f,.5f));else blue(colour(r,1000000,.5f,.5f));}
        }
    }
    @Test public void studioDraftSurvivesRecreationAndShowsBothLayouts()throws Exception {
        Intent intent=new Intent(c,SocialStudioActivity.class).putExtra("project",project.bundle());
        try(ActivityScenario<SocialStudioActivity> scenario=ActivityScenario.launch(intent)) {
            scenario.onActivity(a->{a.project.pip=true;a.project.corner=3;a.project.size=36;a.project.shiftMs=250;a.project.merge=false;Collections.swap(a.project.clips,0,1);a.refresh();});
            scenario.recreate();scenario.onActivity(a->{assertTrue(a.project.pip);assertEquals(3,a.project.corner);assertEquals(36,a.project.size);assertEquals(250,a.project.shiftMs);assertFalse(a.project.merge);assertEquals("rear-0002.mp4",a.project.clips.get(0).file.getName());assertEquals(1000,a.project.clips.get(0).fromMs);});
            for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}) {
                scenario.onActivity(a->a.setRequestedOrientation(orientation));boolean ready=false;
                int expected=orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?Configuration.ORIENTATION_PORTRAIT:Configuration.ORIENTATION_LANDSCAPE;
                for(int i=0;i<100;i++){boolean[] done={false};scenario.onActivity(a->done[0]=a.getResources().getConfiguration().orientation==expected&&a.player!=null&&a.firstFrameRendered&&a.player.getPlaybackState()==androidx.media3.common.Player.STATE_READY);if(done[0]){ready=true;break;}Thread.sleep(100);}
                assertTrue("Studio preview did not become ready after rotation",ready);
                Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(shot);
                File directory=new File(c.getExternalFilesDir("tests"),"social-studio");directory.mkdirs();
                try(OutputStream out=new FileOutputStream(new File(directory,expected==1?"portrait.png":"landscape.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
            }
        }
    }
}
