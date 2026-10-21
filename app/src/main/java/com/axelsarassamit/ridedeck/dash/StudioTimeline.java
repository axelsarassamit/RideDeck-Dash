package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.view.*;
import java.util.*;
import java.util.concurrent.*;

/** Bounded thumbnail strip with source trim handles and a composition playhead. */
final class StudioTimeline extends View {
    interface Listener {void select(int index,long outputMs);void trim(int index,long from,long to);}
    SocialProject project;boolean compact;int selected;long playhead;Listener listener;final Paint paint=new Paint(3);
    final Map<String,Bitmap[]> frames=new ConcurrentHashMap<>();final Set<String> loading=ConcurrentHashMap.newKeySet();
    final ExecutorService worker=Executors.newSingleThreadExecutor();volatile boolean closed;
    int drag=-1,index=-1;long from,to;float downX;boolean moved;
    StudioTimeline(Context c){super(c);setContentDescription("Video timeline. Tap a clip to seek. Drag amber handles to trim the selected clip.");setFocusable(true);}
    float dp(float n){return n*getResources().getDisplayMetrics().density;}
    float width(SocialProject.Clip clip){return dp(Math.max(140,Math.min(520,clip.durationMs/1000f*18)));}
    void bind(SocialProject p,int active){project=p;selected=Math.max(0,Math.min(active,p.clips.size()-1));requestLayout();invalidate();for(SocialProject.Clip clip:p.clips)load(clip);}
    @Override protected void onMeasure(int w,int h){float total=dp(16);if(project!=null)for(SocialProject.Clip clip:project.clips)total+=width(clip)+dp(8);setMeasuredDimension((int)total,(int)dp(compact?76:104));}
    void load(SocialProject.Clip clip){String path=clip.file.getPath();if(frames.containsKey(path)||!loading.add(path))return;
        worker.execute(()->{Bitmap[] images=new Bitmap[4];try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(path);for(int i=0;i<4&&!closed&&!RecordingService.running&&!RecordingService.finalizing;i++){long t=clip.durationMs*1000*(2*i+1)/8;
                Bitmap b=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(t,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,160,90):r.getFrameAtTime(t,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(b!=null){images[i]=Bitmap.createScaledBitmap(b,160,90,true);if(images[i]!=b)b.recycle();}
            }
        }catch(Exception ignored){}if(!closed){frames.put(path,images);postInvalidate();}});
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(project==null)return;float x=dp(8);long offset=0;
        for(int i=0;i<project.clips.size();i++){SocialProject.Clip clip=project.clips.get(i);float w=width(clip),top=dp(compact?20:24),bottom=dp(compact?62:86);
            paint.setColor(RideStyle.RAISED);c.drawRoundRect(x,top,x+w,bottom,dp(8),dp(8),paint);Bitmap[] images=frames.get(clip.file.getPath());
            c.save();c.clipRect(x,top,x+w,bottom);if(images!=null)for(int n=0;n<4;n++)if(images[n]!=null)c.drawBitmap(images[n],null,new RectF(x+w*n/4,top,x+w*(n+1)/4,bottom),paint);c.restore();
            long a=drag>=0&&index==i?from:clip.fromMs,b=drag>=0&&index==i?to:clip.toMs;float left=x+w*a/clip.durationMs,right=x+w*b/clip.durationMs;
            paint.setColor(0xbb05080d);c.drawRect(x,top,left,bottom,paint);c.drawRect(right,top,x+w,bottom,paint);
            paint.setColor(i==selected?0xffffbf69:RideStyle.MUTED);paint.setTextSize(dp(11));c.drawText((i+1)+"  "+SocialStudioActivity.seconds(clip.lengthMs())+"s",x,dp(16),paint);
            if(i==selected){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawRect(left,top,right,bottom,paint);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(left-dp(4),top,left+dp(4),bottom,dp(3),dp(3),paint);c.drawRoundRect(right-dp(4),top,right+dp(4),bottom,dp(3),dp(3),paint);}
            if(playhead>=offset&&playhead<=offset+clip.lengthMs()){float px=x+w*(clip.fromMs+playhead-offset)/clip.durationMs;paint.setColor(Color.WHITE);paint.setStrokeWidth(dp(2));c.drawLine(px,dp(20),px,dp(compact?70:94),paint);}
            offset+=clip.lengthMs();x+=w+dp(8);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(project==null)return false;float x=dp(8);long offset=0;
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();moved=false;index=-1;drag=-1;
            for(int i=0;i<project.clips.size();i++){SocialProject.Clip clip=project.clips.get(i);float w=width(clip);if(e.getX()>=x-dp(10)&&e.getX()<=x+w+dp(10)){index=i;from=clip.fromMs;to=clip.toMs;if(i==selected){if(Math.abs(e.getX()-(x+w*from/clip.durationMs))<dp(18))drag=0;else if(Math.abs(e.getX()-(x+w*to/clip.durationMs))<dp(18))drag=1;}if(drag>=0)getParent().requestDisallowInterceptTouchEvent(true);return true;}x+=w+dp(8);}return true;
        }
        if(index<0)return true;for(int i=0;i<index;i++){x+=width(project.clips.get(i))+dp(8);offset+=project.clips.get(i).lengthMs();}
        SocialProject.Clip clip=project.clips.get(index);long position=(long)(Math.max(0,Math.min(1,(e.getX()-x)/width(clip)))*clip.durationMs);
        if(e.getAction()==MotionEvent.ACTION_MOVE){moved|=Math.abs(e.getX()-downX)>dp(8);if(drag==0)from=Math.max(0,Math.min(to-500,position));if(drag==1)to=Math.min(clip.durationMs,Math.max(from+500,position));invalidate();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){performClick();if(drag>=0&&listener!=null)listener.trim(index,from,to);else if(!moved&&listener!=null)listener.select(index,offset+Math.max(0,Math.min(clip.lengthMs(),position-clip.fromMs)));drag=-1;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;}
        if(e.getAction()==MotionEvent.ACTION_CANCEL){drag=-1;invalidate();return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    void close(){closed=true;worker.shutdownNow();frames.clear();}
}
