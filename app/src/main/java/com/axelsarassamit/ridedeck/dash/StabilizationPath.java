package com.axelsarassamit.ridedeck.dash;

/** Bounded, centered motion smoothing. Rejected tracking intervals are isolated segments. */
final class StabilizationPath {
    // Do not infer one camera move across a missing tenth of a second.
    static boolean captureGap(long previousUs,long currentUs){return currentUs-previousUs>=100000;}
    static float[][] smooth(long[] times, float[][] motion, int[] segments, int strength, int width, int height) {
        float zoom = zoom(strength);
        float[][] result = new float[times.length][3];
        double sigma = strength == 1 ? 180000 : strength == 2 ? 350000 : 600000;
        for (int i = 0; i < times.length; i++) {
            double[] sum = new double[3]; double weights = 0;
            for (int j = i; j >= 0 && times[i] - times[j] < sigma * 3 && segments[j] == segments[i]; j--) {
                double weight = Math.exp(-Math.pow((times[j]-times[i])/sigma,2)/2);
                weights += weight; for (int k=0;k<3;k++) sum[k] += motion[j][k]*weight;
            }
            for (int j = i+1; j < times.length && times[j] - times[i] < sigma * 3 && segments[j] == segments[i]; j++) {
                double weight = Math.exp(-Math.pow((times[j]-times[i])/sigma,2)/2);
                weights += weight; for (int k=0;k<3;k++) sum[k] += motion[j][k]*weight;
            }
            float angle = clamp((float)(sum[2]/weights-motion[i][2]), (float)Math.toRadians(strength+1));
            double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
            float xLimit = (float)Math.max(0, (zoom-cos-(double)height/width*sin)*width/2/zoom);
            float yLimit = (float)Math.max(0, (zoom-cos-(double)width/height*sin)*height/2/zoom);
            // S * inverse(P): rotations change the coordinate system of accumulated translation.
            // Subtracting translations directly is only valid when the correction has no rotation.
            double c=Math.cos(angle),s=Math.sin(angle);
            result[i][0] = clamp((float)(sum[0]/weights-c*motion[i][0]+s*motion[i][1]), xLimit);
            result[i][1] = clamp((float)(sum[1]/weights-s*motion[i][0]-c*motion[i][1]), yLimit);
            result[i][2] = angle;
        }
        // A failed match starts a fresh coordinate system. Fade on both sides of that
        // boundary instead of jumping from a nonzero correction to identity in one frame.
        // Do not fade the ends of a normal trim: its motion is still known there.
        int first=0;
        while(first<times.length){
            int last=first;while(last+1<times.length&&segments[last+1]==segments[first])last++;
            for(int i=first;i<=last;i++){
                float fade=1;
                if(first>0)fade=Math.min(fade,fade((times[i]-times[first])/200000f));
                if(last+1<times.length)fade=Math.min(fade,fade((times[last]-times[i])/200000f));
                for(int k=0;k<3;k++)result[i][k]*=fade;
            }
            first=last+1;
        }
        return result;
    }
    private static float fade(float value){float t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
    static float zoom(int strength) { return strength == 1 ? 1.12f : strength == 2 ? 1.22f : 1.35f; }
    static float[] compose(float[] path,float[] delta) {
        double c=Math.cos(delta[2]),s=Math.sin(delta[2]);
        return new float[]{(float)(c*path[0]-s*path[1]+delta[0]),
            (float)(s*path[0]+c*path[1]+delta[1]),path[2]+delta[2]};
    }
    static float clamp(float value,float limit) { return Math.max(-limit,Math.min(limit,value)); }
}
