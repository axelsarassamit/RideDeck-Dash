package com.axelsarassamit.ridedeck.dash;
import androidx.media3.common.C;
import androidx.media3.common.audio.*;
import java.nio.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class WindAudioProcessor extends BaseAudioProcessor {
    final int cutoff;RumbleFilter filter;int channel;
    WindAudioProcessor(int value){cutoff=value;}
    @Override protected AudioFormat onConfigure(AudioFormat input)throws UnhandledAudioFormatException{
        if(input.encoding!=C.ENCODING_PCM_16BIT&&input.encoding!=C.ENCODING_PCM_FLOAT)throw new UnhandledAudioFormatException(input);
        if(cutoff==0)return AudioFormat.NOT_SET;
        if(input.sampleRate<=cutoff*2)throw new UnhandledAudioFormatException(input);
        return input;
    }
    @Override protected void onFlush(StreamMetadata metadata){filter=new RumbleFilter(inputAudioFormat.sampleRate,inputAudioFormat.channelCount,cutoff);channel=0;}
    @Override public void queueInput(ByteBuffer input){
        if(hasPendingOutput())return;
        int bytes=inputAudioFormat.encoding==C.ENCODING_PCM_FLOAT?4:2;
        int count=input.remaining()/bytes;ByteBuffer output=replaceOutputBuffer(count*bytes);input.order(ByteOrder.nativeOrder());
        for(int i=0;i<count;i++){
            if(bytes==4)output.putFloat((float)filter.sample(input.getFloat(),channel));else output.putShort(filter.pcm(input.getShort(),channel));
            channel=(channel+1)%inputAudioFormat.channelCount;
        }
        output.flip();
    }
    @Override protected void onReset(){filter=null;channel=0;}
}
