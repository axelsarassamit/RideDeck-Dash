package com.axelsarassamit.ridedeck.dash;

/** Two-pole Butterworth low-cut. Keeps channels separate and never boosts the result. */
final class RumbleFilter {
    final double b0,b1,b2,a1,a2;final double[][] state;final boolean bypass;
    RumbleFilter(int rate,int channels,int cutoff){
        if(rate<=0||channels<=0||cutoff<0||cutoff>=rate/2)throw new IllegalArgumentException("Invalid audio filter format");
        state=new double[channels][4];bypass=cutoff==0;
        double k=Math.tan(Math.PI*cutoff/rate),normal=1/(1+Math.sqrt(2)*k+k*k);
        b0=normal;b1=-2*normal;b2=normal;a1=2*(k*k-1)*normal;a2=(1-Math.sqrt(2)*k+k*k)*normal;
    }
    double sample(double input,int channel){
        if(!Double.isFinite(input))input=0;if(bypass)return Math.max(-1,Math.min(1,input));
        double[] s=state[channel];double result=b0*input+b1*s[0]+b2*s[1]-a1*s[2]-a2*s[3];
        s[1]=s[0];s[0]=input;s[3]=s[2];s[2]=result;
        return Math.max(-1,Math.min(1,result));
    }
    short pcm(short value,int channel){return (short)Math.max(-32768,Math.min(32767,Math.round(sample(value/32768.0,channel)*32768)));}
}
