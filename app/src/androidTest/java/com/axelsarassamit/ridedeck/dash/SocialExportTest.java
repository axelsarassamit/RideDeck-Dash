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
        for(File draft:StudioDrafts.list(c))try{Bundle b=StudioDrafts.read(c,draft.getName().replace(".draft",""));ArrayList<String> paths=b.getBundle("project").getStringArrayList("paths");if(paths!=null&&!paths.isEmpty()&&paths.stream().allMatch(path->path.startsWith(ride.getPath()+File.separator)))draft.delete();}catch(Exception ignored){}
        for(Uri uri:gallery)c.getContentResolver().delete(uri,null,null);
        for(File f:outputs)f.delete();
        if(ride!=null&&ride.getName().startsWith("social-test-")){File[] files=ride.listFiles();if(files!=null)for(File f:files)f.delete();ride.delete();}
    }
    File render(SocialProject spec)throws Exception {
        return render(spec,recipe);
    }
    File render(SocialProject spec,DashSettings settings)throws Exception {
        SocialComposition.Built built=SocialComposition.build(c,spec,settings,true,()->false);
        File output=File.createTempFile("social-verify-",".mp4",c.getCacheDir());output.delete();outputs.add(output);
        CountDownLatch done=new CountDownLatch(1);Throwable[] error={null};Transformer[] exporter={null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{exporter[0]=new Transformer.Builder(c).setPortraitEncodingEnabled(true).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(new Transformer.Listener(){@Override public void onCompleted(Composition composition,ExportResult result){done.countDown();}@Override public void onError(Composition composition,ExportResult result,ExportException e){error[0]=e;done.countDown();}}).build();exporter[0].start(built.composition,output.getPath());});
        if(!done.await(90,TimeUnit.SECONDS)){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->exporter[0].cancel());fail("Composition export timed out");}
        if(error[0]!=null)throw new AssertionError("Real export failed",error[0]);return output;
    }
    int colour(MediaMetadataRetriever r,long timeUs,float x,float y){Bitmap b=r.getFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST);assertNotNull(b);int value=b.getPixel((int)(b.getWidth()*x),(int)(b.getHeight()*y));b.recycle();return value;}
    double[] sound(File file)throws Exception {
        android.media.MediaExtractor extractor=new android.media.MediaExtractor();android.media.MediaCodec decoder=null;
        try{extractor.setDataSource(file.getPath());android.media.MediaFormat format=null;for(int i=0;i<extractor.getTrackCount();i++)if(extractor.getTrackFormat(i).getString("mime").startsWith("audio/")){extractor.selectTrack(i);format=extractor.getTrackFormat(i);break;}assertNotNull(format);
            decoder=android.media.MediaCodec.createDecoderByType(format.getString("mime"));decoder.configure(format,null,null,0);decoder.start();boolean inputDone=false,outputDone=false;android.media.MediaCodec.BufferInfo info=new android.media.MediaCodec.BufferInfo();
            double early=0,middle=0;int earlyCount=0,middleCount=0,crossings=0,last=0,channels=format.getInteger("channel-count"),rate=format.getInteger("sample-rate");
            for(int n=0;n<3000&&!outputDone;n++){
                if(!inputDone){int index=decoder.dequeueInputBuffer(1000);if(index>=0){java.nio.ByteBuffer buffer=decoder.getInputBuffer(index);int size=extractor.readSampleData(buffer,0);if(size<0){decoder.queueInputBuffer(index,0,0,0,android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true;}else{decoder.queueInputBuffer(index,0,size,extractor.getSampleTime(),0);extractor.advance();}}}
                int index=decoder.dequeueOutputBuffer(info,1000);if(index==android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){android.media.MediaFormat f=decoder.getOutputFormat();channels=f.getInteger("channel-count");rate=f.getInteger("sample-rate");}
                if(index>=0){java.nio.ByteBuffer b=decoder.getOutputBuffer(index).order(java.nio.ByteOrder.nativeOrder());b.position(info.offset);b.limit(info.offset+info.size);int frame=0;while(b.remaining()>=channels*2){int sample=b.getShort();for(int k=1;k<channels;k++)b.getShort();long time=info.presentationTimeUs+frame++*1000000L/rate;
                    if(time>=0&&time<100000){early+=(double)sample*sample;earlyCount++;}if(time>=1000000&&time<2000000){middle+=(double)sample*sample;middleCount++;if(last<=0&&sample>0)crossings++;last=sample;}
                }outputDone=(info.flags&android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0||info.presentationTimeUs>2100000;decoder.releaseOutputBuffer(index,false);}
            }assertTrue(middleCount>rate/2);return new double[]{Math.sqrt(early/Math.max(1,earlyCount)),Math.sqrt(middle/middleCount),crossings*rate/(double)middleCount};
        }finally{if(decoder!=null){decoder.stop();decoder.release();}extractor.release();}
    }
    void red(int v){assertTrue("Expected red, got "+Integer.toHexString(v),Color.red(v)>160&&Color.blue(v)<80&&Color.green(v)<80);}
    void blue(int v){assertTrue("Expected blue, got "+Integer.toHexString(v),Color.blue(v)>160&&Color.red(v)<80);}
    android.view.View text(android.view.View view,String value){if(view instanceof android.widget.TextView&&value.contentEquals(((android.widget.TextView)view).getText()))return view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.view.View found=text(group.getChildAt(i),value);if(found!=null)return found;}}return null;}
    void thumbnails(android.view.View view,List<android.widget.ImageView> images){if(view instanceof android.widget.ImageView&&view.getTag() instanceof String&&((String)view.getTag()).contains(".mp4:"))images.add((android.widget.ImageView)view);if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)thumbnails(group.getChildAt(i),images);}}
    Bitmap previewFrame(ActivityScenario<SocialStudioActivity> scenario)throws Exception {
        CountDownLatch done=new CountDownLatch(1);int[] result={-1};Bitmap[] frame={null};scenario.onActivity(a->{android.view.SurfaceView view=(android.view.SurfaceView)a.preview.getVideoSurfaceView();frame[0]=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);android.view.PixelCopy.request(view,frame[0],code->{result[0]=code;done.countDown();},new Handler(Looper.getMainLooper()));});assertTrue(done.await(5,TimeUnit.SECONDS));assertEquals(android.view.PixelCopy.SUCCESS,result[0]);return frame[0];
    }
    @Test public void editorSeekAndPlayMustActuallyUpdateVisibleVideo()throws Exception {
        verifyTransport();
    }
    @Test public void editorSeekAndPlayWithFrontPipMustUpdateVisibleVideo()throws Exception {
        project.pip=true;verifyTransport();
    }
    @Test public void mutedEditorSeekAndPlayMustUpdateVisibleVideo()throws Exception {
        Map<String,Object> options=new HashMap<>(recipe.snapshot);options.put("social_mute",1);recipe=new DashSettings(c,options);verifyTransport();
    }
    @Test public void editorMusicAndFilteredAudioMustStillSeekAndPlay()throws Exception {
        Map<String,Object> options=new HashMap<>(recipe.snapshot);options.put("studio_volume",50);options.put("studio_fade",500);options.put("audio_cutoff",120);options.put("studio_music",androidx.core.content.FileProvider.getUriForFile(c,BuildConfig.APPLICATION_ID+".updates",project.clips.get(1).file).toString());recipe=new DashSettings(c,options);verifyTransport();
    }
    void verifyTransport()throws Exception {
        try(ActivityScenario<SocialStudioActivity> scenario=ActivityScenario.launch(new Intent(c,SocialStudioActivity.class).putExtra("project",project.bundle()).putExtra("recipe",recipe.bundle()))){
            boolean ready=false;for(int n=0;n<120;n++){boolean[] state={false};scenario.onActivity(a->state[0]=a.firstFrameRendered&&a.player!=null&&a.player.getPlaybackState()==androidx.media3.common.Player.STATE_READY);if(state[0]){ready=true;break;}Thread.sleep(100);}assertTrue("Preview must prepare",ready);
            scenario.onActivity(a->a.seekPreview(4500));Thread.sleep(1000);scenario.onActivity(a->android.util.Log.i("DashTransportTest","seek position="+a.player.getCurrentPosition()+" duration="+a.player.getDuration()+" state="+a.player.getPlaybackState()+" buffering="+a.player.getBufferedPosition()));Bitmap frame=previewFrame(scenario);try{blue(frame.getPixel(frame.getWidth()/2,frame.getHeight()/2));}finally{frame.recycle();}
            scenario.onActivity(a->{a.seekPreview(2000);a.play.performClick();});for(int n=0;n<60;n++){boolean[] advanced={false};scenario.onActivity(a->advanced[0]=a.player!=null&&a.firstFrameRendered&&a.player.getCurrentPosition()>3300&&(!a.project.pip||a.segmentAt(a.player.getCurrentPosition())==a.previewSegment));if(advanced[0])break;Thread.sleep(100);}Thread.sleep(100);scenario.onActivity(a->{assertTrue("Play must advance: position="+a.player.getCurrentPosition()+" state="+a.player.getPlaybackState()+" ready="+a.player.getPlayWhenReady()+" suppression="+a.player.getPlaybackSuppressionReason(),a.player.getCurrentPosition()>3200);assertNull(a.player.getPlayerError());});frame=previewFrame(scenario);try{blue(frame.getPixel(frame.getWidth()/2,frame.getHeight()/2));}finally{frame.recycle();}
            scenario.onActivity(a->{a.play.performClick();a.timeline.listener.select(0,500);});for(int n=0;n<60;n++){boolean[] readyAgain={false};scenario.onActivity(a->readyAgain[0]=a.firstFrameRendered&&a.player!=null&&a.player.getPlaybackState()==androidx.media3.common.Player.STATE_READY);if(readyAgain[0])break;Thread.sleep(100);}Thread.sleep(400);frame=previewFrame(scenario);try{red(frame.getPixel(frame.getWidth()/2,frame.getHeight()/2));}finally{frame.recycle();}
        }
    }
    @Test public void mergedOrderDurationAndMainAudioArePreserved()throws Exception {
        File file=render(project);try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(file.getPath());assertEquals(6000,Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),250);
            assertEquals("yes",r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));red(colour(r,1000000,.5f,.5f));blue(colour(r,4500000,.5f,.5f));
        }
    }
    int amberPixels(Bitmap bitmap){int count=0;for(int y=0;y<bitmap.getHeight()/4;y++)for(int x=0;x<bitmap.getWidth()/3;x++){int v=bitmap.getPixel(x,y);if(Color.alpha(v)>100&&Color.red(v)>180&&Color.green(v)>60&&Color.green(v)<185&&Color.blue(v)<110)count++;}return count;}
    @Test public void approvedCanvaBrandAppearsInPreviewAndEncodedExport()throws Exception {
        Map<String,Object> options=new HashMap<>(recipe.snapshot);options.put("overlay_alpha",0);DashSettings branded=new DashSettings(c,options);
        TelemetryOverlay overlay=new TelemetryOverlay(c,new RideTelemetry(),branded,0,true,new OverlayRoads());overlay.configure(new androidx.media3.common.util.Size(1280,720));assertTrue("Preview must use white/amber Canva artwork",amberPixels(overlay.getBitmap(0))>30);overlay.release();
        File output=render(project,branded);try(MediaMetadataRetriever r=new MediaMetadataRetriever()){r.setDataSource(output.getPath());Bitmap frame=r.getFrameAtTime(1000000,MediaMetadataRetriever.OPTION_CLOSEST);assertNotNull(frame);try{assertTrue("Encoded watermark must use current Canva artwork",amberPixels(frame)>30);}finally{frame.recycle();}}
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
    @Test public void timelineSplitUndoAndPersistentDraftKeepIndependentRanges()throws Exception {
        Intent intent=new Intent(c,SocialStudioActivity.class).putExtra("project",project.bundle()).putExtra("recipe",recipe.bundle());final String[] id={null};
        try(ActivityScenario<SocialStudioActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(a->{a.selected=0;a.seekMs=1500;a.release();a.split();assertEquals(3,a.project.clips.size());assertEquals(1500,a.project.clips.get(0).toMs);assertEquals(1500,a.project.clips.get(1).fromMs);a.history(false);assertEquals(2,a.project.clips.size());a.history(true);assertEquals(3,a.project.clips.size());a.setting("studio_text","Ride title");a.setting("studio_text_start",500);a.project.pipX=60;a.project.pipY=10;a.save(false);id[0]=a.draftId;});
            Thread.sleep(500);scenario.onActivity(a->{android.view.View split=text(a.controls,"Split"),trim=text(a.controls,"Trim"),duplicate=text(a.controls,"Duplicate");assertNotNull(split);assertNotNull(trim);assertNotNull(duplicate);assertTrue("Clip actions must share the row",trim.getLeft()>split.getLeft()&&duplicate.getLeft()>trim.getLeft());assertTrue("Duplicate must fit inside editor",duplicate.getRight()<=a.controls.getWidth());});
        }
        Bundle draft=StudioDrafts.read(c,id[0]);SocialProject restored=SocialProject.load(c,draft.getBundle("project"));assertEquals(3,restored.clips.size());assertEquals(6000,restored.durationMs());assertEquals(60,restored.pipX);assertEquals("Ride title",draft.getBundle("recipe").getString("studio_text"));
    }
    @Test public void recordingsHaveDecodedThumbnailsAndRememberSortingAndCameraFilter()throws Exception {
        android.content.SharedPreferences prefs=c.getSharedPreferences("clip-library",Context.MODE_PRIVATE);boolean hadSort=prefs.contains("sort"),hadFilter=prefs.contains("filter");int oldSort=prefs.getInt("sort",0),oldFilter=prefs.getInt("filter",0);
        prefs.edit().putInt("sort",0).putInt("filter",1).commit();
        try(ActivityScenario<ClipsActivity> scenario=ActivityScenario.launch(new Intent(c,ClipsActivity.class))){
            boolean ready=false;for(int n=0;n<100;n++){boolean[] decoded={false};scenario.onActivity(a->{List<android.widget.ImageView> images=new ArrayList<>();thumbnails(a.getWindow().getDecorView(),images);for(android.widget.ImageView image:images)if(((String)image.getTag()).startsWith(ride.getPath())&&image.getDrawable()!=null)decoded[0]=true;});if(decoded[0]){ready=true;break;}Thread.sleep(100);}assertTrue("Recording thumbnail must decode",ready);
            scenario.recreate();scenario.onActivity(a->{assertNotNull(text(a.getWindow().getDecorView(),"Newest first"));assertNotNull(text(a.getWindow().getDecorView(),"Rear camera"));List<android.widget.ImageView> images=new ArrayList<>();thumbnails(a.getWindow().getDecorView(),images);long previous=Long.MAX_VALUE;for(android.widget.ImageView image:images){String tag=(String)image.getTag(),path=tag.substring(0,tag.lastIndexOf(':'));File file=new File(path);assertTrue(file.getName().startsWith("rear-"));assertTrue(file.lastModified()<=previous);previous=file.lastModified();}});
        }finally{android.content.SharedPreferences.Editor editor=prefs.edit();if(hadSort)editor.putInt("sort",oldSort);else editor.remove("sort");if(hadFilter)editor.putInt("filter",oldFilter);else editor.remove("filter");editor.commit();}
    }
    @Test public void adjustedVideoWithTimedTextAndMusicUsesRealComposition()throws Exception {
        Map<String,Object> values=new HashMap<>(recipe.snapshot);values.put("studio_saturation",-100);values.put("studio_text","RIDE");values.put("studio_text_x",20);values.put("studio_text_y",40);values.put("studio_text_scale",12);values.put("studio_color",Color.WHITE);values.put("studio_text_start",1000);values.put("studio_text_end",2500);
        values.put("studio_music",androidx.core.content.FileProvider.getUriForFile(c,BuildConfig.APPLICATION_ID+".updates",project.clips.get(1).file).toString());values.put("studio_music_volume",25);values.put("studio_volume",0);values.put("studio_fade",500);
        File output=render(project,new DashSettings(c,values));try(MediaMetadataRetriever r=new MediaMetadataRetriever()){r.setDataSource(output.getPath());int pixel=colour(r,500000,.5f,.5f);assertTrue(Math.abs(Color.red(pixel)-Color.blue(pixel))<8);assertEquals("yes",r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
            Bitmap early=r.getFrameAtTime(500000,MediaMetadataRetriever.OPTION_CLOSEST),title=r.getFrameAtTime(1500000,MediaMetadataRetriever.OPTION_CLOSEST);int brightEarly=0,brightTitle=0;for(int y=early.getHeight()/4;y<early.getHeight()/2;y++)for(int x=early.getWidth()/5;x<early.getWidth()/2;x++){if(Color.red(early.getPixel(x,y))>220)brightEarly++;if(Color.red(title.getPixel(x,y))>220)brightTitle++;}early.recycle();title.recycle();assertTrue("Timed title should appear in output",brightTitle>brightEarly+100);
        }
        double[] audio=sound(output);assertTrue("Imported music must be audible",audio[1]>100);assertEquals("Music tone should replace the zero-volume original",660,audio[2],30);assertTrue("Fade-in should soften the opening",audio[0]<audio[1]*.4);
    }
    @Test public void stabilizationIsSharedByPreviewAndExportAndHonorsCancellation()throws Exception {
        File file=new File(ride,"rear-shake.mp4");try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("social/shake.mp4");OutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}ClipStore.mark(file,".ready");
        SocialProject p=SocialProject.load(c,SocialProject.selection(Collections.singletonList(file.getPath())));Map<String,Object> options=new HashMap<>(recipe.snapshot);options.put("studio_stabilize",1);DashSettings stable=new DashSettings(c,options);
        SocialComposition.Built preview=SocialComposition.build(c,p,stable,false,()->false);assertEquals(4000,preview.durationMs);File output=render(p,stable);assertEquals("Stabilization must preserve all source pictures",RearStabilizationTest.frames(file,0,4000000),RearStabilizationTest.frames(output,0,Long.MAX_VALUE));try(MediaMetadataRetriever r=new MediaMetadataRetriever()){r.setDataSource(output.getPath());assertEquals(4000,Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),250);assertNotNull(r.getFrameAtTime(2000000));}
        try{SocialComposition.build(c,p,stable,true,()->true);fail("Cancelled project must not export");}catch(IOException expected){}
    }
}
