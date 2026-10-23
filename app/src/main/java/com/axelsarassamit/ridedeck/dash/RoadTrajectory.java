package com.axelsarassamit.ridedeck.dash;

/** Offline, time-aware whole-segment acceleration minimization with constant overscan. */
final class RoadTrajectory {
    static float[][] smooth(long[] times,float[][] pose,int[] segments,int strength,int width,int height){
        float[][] correction=new float[times.length][3];
        int first=0;
        while(first<times.length){
            int last=first;while(last+1<times.length&&segments[last+1]==segments[first])last++;
            int n=last-first+1;double dt=n>1?(times[last]-times[first])/1000000.0/(n-1):1.0/30;
            double seconds=strength==1?.18:strength==2?.30:.45;
            double[][] target=new double[3][];
            for(int axis=0;axis<3;axis++){
                double[] values=new double[n];for(int i=0;i<n;i++)values[i]=pose[first+i][axis];
                target[axis]=optimize(times,first,last,values,Math.pow(seconds/Math.max(.001,dt),4),dt);
            }
            double zoom=StabilizationPath.zoom(strength),maxAngle=Math.toRadians(strength+1);
            double[] angleGain=new double[n],xGain=new double[n],yGain=new double[n],fades=new double[n];
            for(int j=0;j<n;j++){
                double angle=target[2][j]-pose[first+j][2];angleGain[j]=Math.min(1,maxAngle/Math.max(1e-9,Math.abs(angle)));
                double fade=1;
                if(first>0)fade=Math.min(fade,fade((times[first+j]-times[first])/250000.0));
                if(last+1<times.length)fade=Math.min(fade,fade((times[last]-times[first+j])/250000.0));
                fades[j]=fade;
            }
            envelope(angleGain,times,first);
            for(int j=0;j<n;j++){
                int i=first+j;double angle=(target[2][j]-pose[i][2])*angleGain[j]*fades[j],c=Math.cos(angle),s=Math.sin(angle);
                double x=target[0][j]-c*pose[i][0]+s*pose[i][1],y=target[1][j]-s*pose[i][0]-c*pose[i][1];
                correction[i]=new float[]{(float)x,(float)y,(float)angle};
                double cos=Math.abs(c),sin=Math.abs(s);
                double lx=Math.max(0,(zoom-cos-(double)height/width*sin)*width/2/zoom);
                double ly=Math.max(0,(zoom-cos-(double)width/height*sin)*height/2/zoom);
                xGain[j]=Math.min(1,lx/Math.max(1e-9,Math.abs(x)));yGain[j]=Math.min(1,ly/Math.max(1e-9,Math.abs(y)));
            }
            // Each axis has its own smooth safety envelope: a long pan reaching the horizontal
            // crop bound must not turn off vertical vibration or roll correction as well.
            envelope(xGain,times,first);envelope(yGain,times,first);
            for(int j=0;j<n;j++){correction[first+j][0]*=xGain[j]*fades[j];correction[first+j][1]*=yGain[j]*fades[j];}
            first=last+1;
        }
        return correction;
    }
    private static void envelope(double[] gain,long[] times,int first){
        for(int j=1;j<gain.length;j++)gain[j]=Math.min(gain[j],gain[j-1]+(times[first+j]-times[first+j-1])/800000.0);
        for(int j=gain.length-2;j>=0;j--)gain[j]=Math.min(gain[j],gain[j+1]+(times[first+j+1]-times[first+j])/800000.0);
    }
    private static double fade(double value){double t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}

    /** Solve (I + lambda D'D)x = input using a banded LDL decomposition, O(n) memory/work.
     * D is the time-aware second difference: a constant-velocity pan or lean has zero penalty. */
    static double[] optimize(long[] times,int first,int last,double[] input,double lambda,double dt){
        int n=input.length;double[] diagonal=new double[n],lower=new double[n],lower2=new double[n];
        java.util.Arrays.fill(diagonal,1);
        for(int i=1;i<n-1;i++){
            double left=Math.max(.001,(times[first+i]-times[first+i-1])/1000000.0)/dt;
            double right=Math.max(.001,(times[first+i+1]-times[first+i])/1000000.0)/dt;
            double a=2/(left*(left+right)),b=-2/(left*right),c=2/(right*(left+right));
            diagonal[i-1]+=lambda*a*a;diagonal[i]+=lambda*b*b;diagonal[i+1]+=lambda*c*c;
            lower[i]+=lambda*a*b;lower[i+1]+=lambda*b*c;lower2[i+1]+=lambda*a*c;
        }
        double[] d=new double[n],l1=new double[n],l2=new double[n],x=input.clone();
        for(int i=0;i<n;i++){
            if(i>1)l2[i]=lower2[i]/d[i-2];
            if(i>0)l1[i]=(lower[i]-(i>1?l2[i]*d[i-2]*l1[i-1]:0))/d[i-1];
            d[i]=diagonal[i]-(i>0?l1[i]*l1[i]*d[i-1]:0)-(i>1?l2[i]*l2[i]*d[i-2]:0);
            if(i>0)x[i]-=l1[i]*x[i-1];if(i>1)x[i]-=l2[i]*x[i-2];
        }
        for(int i=0;i<n;i++)x[i]/=d[i];
        for(int i=n-1;i>=0;i--){if(i+1<n)x[i]-=l1[i+1]*x[i+1];if(i+2<n)x[i]-=l2[i+2]*x[i+2];}
        return x;
    }
}
