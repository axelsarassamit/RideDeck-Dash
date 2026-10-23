package com.axelsarassamit.ridedeck.dash;
import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.File;
import java.util.*;
/** Opt-in preview only. Never changes or exports the selected originals. */
public class LargeEditorTest {
 @Test public void existingRideMergePreparesAndPipSeeksWithoutRescanning()throws Exception {
  Bundle args=InstrumentationRegistry.getArguments();Assume.assumeTrue("Explicit existing footage check","1".equals(args.getString("editorEvidence")));
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertFalse(RecordingService.running||RecordingService.finalizing);
  String rides=args.getString("rides","");assertTrue(rides.matches("[0-9,]+"));Set<String> chosen=new HashSet<>(Arrays.asList(rides.split(",")));
  List<String> paths=new ArrayList<>();for(File f:new ClipStore(c).clips())if(chosen.contains(f.getParentFile().getName())&&f.getName().startsWith("rear-")&&ClipStore.finalized(f))paths.add(f.getPath());Collections.sort(paths);assertTrue(paths.size()>10);
  Bundle spec=SocialProject.selection(paths);spec.putBoolean("pip",true);Bundle recipe=new Bundle();recipe.putInt("social_ratio",0);recipe.putInt("social_quality",720);recipe.putInt("social_overlay",0);recipe.putInt("studio_stabilize",0);
  long start=SystemClock.elapsedRealtime();
  try(ActivityScenario<SocialStudioActivity> scenario=ActivityScenario.launch(new Intent(c,SocialStudioActivity.class).putExtra("project",spec).putExtra("recipe",recipe))){
   ready(scenario);long opened=SystemClock.elapsedRealtime()-start;SocialComposition.Built[] built={null};long[] duration={0};
   scenario.onActivity(a->{built[0]=a.prepared;duration[0]=a.project.durationMs();assertEquals(paths.size(),a.project.clips.size());a.seekPreview(900000);});long seekStart=SystemClock.elapsedRealtime();ready(scenario);
   scenario.onActivity(a->{assertSame(built[0],a.prepared);assertEquals(900000,a.player.getCurrentPosition(),500);assertNull(a.player.getPlayerError());});
   System.out.println("Large editor: clips="+paths.size()+" durationMs="+duration[0]+" firstReadyMs="+opened+" cachedSeekMs="+(SystemClock.elapsedRealtime()-seekStart));
  }
 }
 void ready(ActivityScenario<SocialStudioActivity> scenario)throws Exception{for(int i=0;i<900;i++){boolean[] ready={false};scenario.onActivity(a->ready[0]=a.project!=null&&a.firstFrameRendered&&a.player!=null&&a.player.getPlaybackState()==androidx.media3.common.Player.STATE_READY);if(ready[0])return;Thread.sleep(100);}fail("Large editor preview did not become ready");}
}
