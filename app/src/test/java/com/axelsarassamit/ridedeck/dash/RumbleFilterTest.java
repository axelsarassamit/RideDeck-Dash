package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;import static org.junit.Assert.*;
public class RumbleFilterTest {
    double level(int frequency,int cutoff){RumbleFilter filter=new RumbleFilter(48000,1,cutoff);double sum=0;for(int i=0;i<96000;i++){double out=filter.sample(0.5*Math.sin(2*Math.PI*frequency*i/48000),0);if(i>=48000)sum+=out*out;}return Math.sqrt(sum/48000);}
    @Test public void gentlePreservesSpeechBand(){assertEquals(level(1000,0),level(1000,50),0.0001);}
    @Test public void strongerRemovesMoreLowRumble(){assertTrue(level(20,120)<level(20,50)*0.2);assertTrue(level(20,50)<level(20,0)*0.2);}
    @Test public void channelsDoNotBleed(){RumbleFilter filter=new RumbleFilter(48000,2,120);for(int i=0;i<48000;i++){filter.sample(Math.sin(i),0);assertEquals(0,filter.sample(0,1),0);}}
    @Test public void offPreservesPcmExactly(){RumbleFilter filter=new RumbleFilter(48000,1,0);for(int n=-32768;n<=32767;n++)assertEquals((short)n,filter.pcm((short)n,0));}
    @Test public void fullScaleStaysFiniteAndBounded(){RumbleFilter filter=new RumbleFilter(16000,1,180);for(int i=0;i<32000;i++){double value=filter.sample(i%1000<500?1:-1,0);assertTrue(Double.isFinite(value)&&Math.abs(value)<=1);}}
}
