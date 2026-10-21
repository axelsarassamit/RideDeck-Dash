package com.axelsarassamit.ridedeck.dash;

import androidx.media3.common.C;
import androidx.media3.common.audio.*;

/** Sample-position-aware gain automation, including seeks in the preview audio graph. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class StudioAudio {
    static AudioProcessor create(int percent,long durationMs,int fadeMs){
        final float gain=Math.max(0,Math.min(100,percent))/100f;
        return new GainProcessor(new GainProcessor.GainProvider(){
            public float getGainFactorAtSamplePosition(long frame,int rate){double ms=frame*1000.0/rate;return gain*(fadeMs==0?1:(float)Math.max(0,Math.min(1,Math.min(ms/fadeMs,(durationMs-ms)/fadeMs))));}
            public long isUnityUntil(long frame,int rate){if(gain!=1)return C.TIME_UNSET;if(fadeMs==0)return C.TIME_END_OF_SOURCE;long a=fadeMs*rate/1000L,b=(durationMs-fadeMs)*rate/1000L;return frame>=a&&frame<b?b:C.TIME_UNSET;}
        });
    }
}
