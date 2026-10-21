package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.File;
import java.util.concurrent.*;

/** Small, bounded decoded thumbnails, never a video decode on the UI thread. */
final class ClipThumbnails {
    final Activity activity;final LruCache<String,Bitmap> cache=new LruCache<>(32);
    final ExecutorService worker=Executors.newSingleThreadExecutor();volatile boolean closed;volatile int generation;
    ClipThumbnails(Activity a){activity=a;}
    void bind(ImageView view,File file){String key=file.getPath()+":"+file.lastModified();view.setTag(key);Bitmap cached=cache.get(key);if(cached!=null){view.setImageBitmap(cached);return;}if(!ClipStore.finalized(file)||RecordingService.running||RecordingService.finalizing)return;
        int request=generation;worker.execute(()->{if(closed||request!=generation||RecordingService.running||RecordingService.finalizing)return;Bitmap image=null;try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(file.getPath());Bitmap b=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,160,90):r.getFrameAtTime(0);
            if(b!=null){float scale=Math.min(160f/b.getWidth(),90f/b.getHeight());image=Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*scale)),Math.max(1,Math.round(b.getHeight()*scale)),true);if(image!=b)b.recycle();}
        }catch(Exception ignored){}final Bitmap ready=image;if(ready!=null&&!closed)activity.runOnUiThread(()->{if(closed||request!=generation)return;cache.put(key,ready);if(key.equals(view.getTag())&&view.isAttachedToWindow())view.setImageBitmap(ready);});});
    }
    void begin(){generation++;}
    void close(){closed=true;worker.shutdownNow();cache.evictAll();}
}
