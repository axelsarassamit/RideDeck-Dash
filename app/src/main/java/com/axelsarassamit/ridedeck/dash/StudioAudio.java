package com.axelsarassamit.ridedeck.dash;

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.*;

/** Linear gain and optional short fades, on decoded PCM only. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class StudioAudio extends BaseAudioProcessor {
    final float gain;final long durationMs;final int fadeMs;long samples;
    StudioAudio(int percent,long durationMs,int fadeMs){gain=Math.max(0,Math.min(200,percent))/100f;this.durationMs=durationMs;this.fadeMs=fadeMs;}
    @Override protected AudioFormat onConfigure(AudioFormat f)throws UnhandledAudioFormatException {
        if(f.encoding!=C.ENCODING_PCM_16BIT&&f.encoding!=C.ENCODING_PCM_FLOAT)throw new UnhandledAudioFormatException(f);return f;
    }
    @Override protected void onFlush(StreamMetadata metadata){samples=0;}
    @Override public void queueInput(ByteBuffer input){
        if(hasPendingOutput())return;int bytes=inputAudioFormat.encoding==C.ENCODING_PCM_FLOAT?4:2;
        ByteBuffer out=replaceOutputBuffer(input.remaining());input.order(ByteOrder.nativeOrder());
        while(input.remaining()>=bytes){double ms=1000.0*(samples/inputAudioFormat.channelCount)/inputAudioFormat.sampleRate;
            float envelope=fadeMs==0?1:(float)Math.max(0,Math.min(1,Math.min(ms/fadeMs,(durationMs-ms)/fadeMs)));
            float v=(bytes==4?input.getFloat():input.getShort()/32768f)*gain*envelope;v=Math.max(-1,Math.min(1,v));
            if(bytes==4)out.putFloat(v);else out.putShort((short)Math.max(-32768,Math.min(32767,Math.round(v*32768))));samples++;
        }out.flip();
    }
}
