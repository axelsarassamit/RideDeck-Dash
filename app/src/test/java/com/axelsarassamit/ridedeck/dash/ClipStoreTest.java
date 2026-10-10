package com.axelsarassamit.ridedeck.dash;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class ClipStoreTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();
    @Test public void incompleteAndProtectedFilesSurviveRecycling() throws Exception {
        File clip = files.newFile("rear-0001.mp4");
        assertFalse(ClipStore.canRecycle(clip));
        ClipStore.mark(clip, ".ready");
        assertTrue(ClipStore.canRecycle(clip));
        ClipStore.mark(clip, ".protected");
        File reopenedAfterProcessDeath = new File(clip.getPath());
        assertTrue(ClipStore.finalized(reopenedAfterProcessDeath));
        assertTrue(ClipStore.protectedClip(reopenedAfterProcessDeath));
        assertFalse(ClipStore.canRecycle(reopenedAfterProcessDeath));
    }
    @Test public void protectionDoesNotMarkInterruptedFootageAsComplete() throws Exception {
        File clip = files.newFile("front-0001.mp4");
        ClipStore.mark(clip, ".protected");
        ClipStore.mark(clip, ".protected");
        assertFalse(ClipStore.finalized(clip));
        assertFalse(ClipStore.canRecycle(clip));
    }
}
