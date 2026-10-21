package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.*;
import androidx.media3.common.util.Size;
import androidx.media3.effect.*;
import androidx.media3.transformer.*;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared GPU composition for playable multi-clip preview and export. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class SocialComposition {
    static final class Built {
        final Composition composition;final long durationMs;final int matched;final long missingFrontMs;
        Built(Composition c,long d,int m,long missing){composition=c;durationMs=d;matched=m;missingFrontMs=missing;}
    }
    static Built build(Context c,SocialProject p,DashSettings recipe,boolean exporting,BooleanSupplier cancelled)throws Exception {
        if(p.durationMs()>SocialProject.MAX_DURATION_MS)throw new IOException("Trim the selection to 30 minutes or less");
        if(p.pip){Map<String,Object> rearSettings=new HashMap<>(recipe.snapshot);rearSettings.put("social_fit",p.fillRear?1:0);recipe=new DashSettings(c,rearSettings);}
        boolean mute=recipe.number("social_mute",0)==1;
        Set<Integer> rearTypes=new HashSet<>();rearTypes.add(C.TRACK_TYPE_VIDEO);if(!mute)rearTypes.add(C.TRACK_TYPE_AUDIO);
        EditedMediaItemSequence.Builder rear=new EditedMediaItemSequence.Builder(rearTypes);
        EditedMediaItemSequence.Builder front=new EditedMediaItemSequence.Builder(Collections.singleton(C.TRACK_TYPE_VIDEO));
        int[] dimensions=SocialProfile.dimensions(recipe.number("social_ratio",0),recipe.number("social_quality",1080));
        List<SocialTimeline.Slice> visible=new ArrayList<>();long outputUs=0,coveredUs=0;int matched=0;
        Map<String,List<SocialTimeline.Segment>> frontByRide=new HashMap<>();Map<String,Map<String,Long>> anchorsByRide=new HashMap<>();
        for(SocialProject.Clip item:p.clips) {
            if(cancelled.getAsBoolean())throw new IOException("Preparation cancelled");
            File file=SocialProject.validate(c,item.file);
            OverlayRoads roads=new OverlayRoads();
            // Preview/export use identical local overlays; road-network downloads remain in single-clip export.
            List<Effect> effects=SocialEffects.create(c,file,recipe,item.fromMs*1000,null,roads,false);
            List<androidx.media3.common.audio.AudioProcessor> audio=new ArrayList<>();
            if(recipe.number("audio_cutoff",0)>0)audio.add(new WindAudioProcessor(recipe.number("audio_cutoff",0)));
            rear.addItem(edit(file,item.durationMs,item.fromMs*1000,item.toMs*1000,mute,new Effects(audio,effects)));
            long lengthUs=item.lengthMs()*1000;
            if(p.pip) {
                if(!file.getName().startsWith("rear-"))throw new IOException("Use rear clips as the main view when adding front-camera PiP");
                String ride=file.getParent();Map<String,Long> anchors=anchorsByRide.get(ride);
                if(anchors==null){anchors=SocialProject.anchors(file.getParentFile());anchorsByRide.put(ride,anchors);}
                Long start=anchors.get(file.getName());if(start==null)throw new IOException("Recording timestamps are missing for "+file.getName());
                List<SocialTimeline.Segment> candidates=frontByRide.get(ride);
                if(candidates==null) {
                    candidates=new ArrayList<>();File[] files=file.getParentFile().listFiles((dir,name)->name.startsWith("front-")&&name.endsWith(".mp4"));
                    if(files!=null)for(File f:files)if(ClipStore.finalized(f)&&anchors.containsKey(f.getName()))candidates.add(new SocialTimeline.Segment(f.getCanonicalPath(),anchors.get(f.getName()),SocialProject.duration(f)*1000));
                    frontByRide.put(ride,candidates);
                }
                List<SocialTimeline.Slice> slices=SocialTimeline.intersect(start+item.fromMs*1000,lengthUs,candidates,p.shiftMs*1000L);
                long local=0;
                for(SocialTimeline.Slice s:slices) {
                    if(s.outputStartUs>local)front.addGap(s.outputStartUs-local);
                    File f=SocialProject.validate(c,new File(s.path));
                    List<Effect> fit=Arrays.asList(Presentation.createForHeight(480),new PipCorners());
                    front.addItem(edit(f,SocialProject.duration(f),s.sourceStartUs,s.sourceStartUs+s.durationUs,true,new Effects(Collections.emptyList(),fit)));
                    visible.add(new SocialTimeline.Slice(s.path,s.sourceStartUs,s.durationUs,outputUs+s.outputStartUs));
                    local=s.outputStartUs+s.durationUs;coveredUs+=s.durationUs;matched++;
                }
                if(local<lengthUs)front.addGap(lengthUs-local);
            }
            outputUs+=lengthUs;
        }
        if(p.pip&&matched==0)throw new IOException("No simultaneous front recording found. Choose another rear clip or turn PiP off");
        Composition.Builder builder=p.pip?new Composition.Builder(front.build(),rear.build()):new Composition.Builder(rear.build());
        if(p.pip)builder.setVideoCompositorSettings(new VideoCompositorSettings(){
            private Size frontSize=new Size(dimensions[0],dimensions[1]);
            public Size getOutputSize(List<Size> inputs){frontSize=inputs.get(0);return new Size(dimensions[0],dimensions[1]);}
            public OverlaySettings getOverlaySettings(int id,long timeUs) {
                if(id!=0)return new StaticOverlaySettings.Builder().build();
                float x=(p.corner%2==0)?-.94f:.94f,y=p.corner<2?.94f:-.94f;
                float scale=Math.min(dimensions[0]*p.size/100f/frontSize.getWidth(),dimensions[1]*p.size/100f/frontSize.getHeight());
                return new StaticOverlaySettings.Builder().setScale(scale,scale)
                    .setBackgroundFrameAnchor(x,y).setOverlayFrameAnchor(x<0?-1:1,y>0?1:-1)
                    .setAlphaScale(SocialTimeline.visible(visible,timeUs)?1:0).build();
            }
        });
        // Branding is drawn after composition so even a top-left PiP cannot obscure it.
        Map<String,Object> brand=new HashMap<>(recipe.snapshot);for(String key:SocialProfile.overlayKeys())brand.put("overlay_"+key,0);
        List<Effect> finishing=new ArrayList<>();
        if(exporting)finishing.add(FrameDropEffect.createDefaultFrameDropEffect(30));
        finishing.add(new OverlayEffect(Collections.singletonList(new TelemetryOverlay(c,new RideTelemetry(),new DashSettings(c,brand),0,true,new OverlayRoads()))));
        builder.setEffects(new Effects(Collections.emptyList(),finishing));
        return new Built(builder.build(),p.durationMs(),matched,(outputUs-coveredUs)/1000);
    }
    private static EditedMediaItem edit(File file,long durationMs,long startUs,long endUs,boolean mute,Effects effects) {
        MediaItem media=new MediaItem.Builder().setUri(Uri.fromFile(file)).setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder()
            .setStartPositionUs(startUs).setEndPositionUs(endUs).build()).build();
        return new EditedMediaItem.Builder(media).setDurationUs(durationMs*1000).setRemoveAudio(mute).setEffects(effects).build();
    }
}
