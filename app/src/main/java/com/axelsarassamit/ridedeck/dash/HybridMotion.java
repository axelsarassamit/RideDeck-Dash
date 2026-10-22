package com.axelsarassamit.ridedeck.dash;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Small-angle, per-clip empirical gyro/image calibration. No Android or camera I/O. */
final class HybridMotion {
    static final class Gyro {
        final double[] time;final double[][] rate,integral;
        Gyro(double[] seconds,double[][] radiansPerSecond){
            if(seconds.length<20||seconds.length!=radiansPerSecond.length)throw new IllegalArgumentException("More motion samples are needed");
            time=seconds;rate=radiansPerSecond;integral=new double[time.length][3];
            for(int i=0;i<time.length;i++){
                if(!Double.isFinite(time[i])||rate[i].length!=3)throw new IllegalArgumentException("Invalid motion sample");
                for(double v:rate[i])if(!Double.isFinite(v))throw new IllegalArgumentException("Invalid motion sample");
                if(i>0){double dt=time[i]-time[i-1];if(dt<=0||dt>.02)throw new IllegalArgumentException("Motion data has a gap or out-of-order timestamps");
                    for(int k=0;k<3;k++)integral[i][k]=integral[i-1][k]+(rate[i-1][k]+rate[i][k])*.5*dt;}
            }
        }
        double[] at(double t){
            if(t<time[0]||t>time[time.length-1])throw new IllegalArgumentException("Motion samples do not cover this video range");
            int i=Arrays.binarySearch(time,t);if(i>=0)return integral[i].clone();i=-i-2;
            double dt=t-time[i],fraction=dt/(time[i+1]-time[i]);double[] value=integral[i].clone();
            for(int k=0;k<3;k++)value[k]+=rate[i][k]*dt+(rate[i+1][k]-rate[i][k])*fraction*dt*.5;
            return value;
        }
        double[] between(double from,double to){double[] a=at(from),b=at(to);for(int k=0;k<3;k++)b[k]-=a[k];return b;}
    }
    static final class Model {
        final double offset,score,separation;final double[][] map;
        Model(double delay,double validation,double margin,double[][] coefficients){offset=delay;score=validation;separation=margin;map=coefficients;}
        double[] predict(double[] radians){double[] v=new double[3];for(int i=0;i<3;i++)for(int k=0;k<3;k++)v[i]+=map[i][k]*radians[k];return v;}
        double[] delta(Gyro gyro,double a,double b){return predict(gyro.between(a+offset,b+offset));}
    }
    private static final class Fit {double[][] map;double score;Fit(double[][] m,double s){map=m;score=s;}}
    static Model calibrate(Gyro gyro,double[] times,double[][] observed,BooleanSupplier cancelled){
        int count=0;for(int i=1;i<times.length;i++)if(observed[i]!=null)count++;
        if(count<120||times[times.length-1]-times[0]<5)throw new IllegalArgumentException("Use at least 5 seconds with clear, varied camera movement");
        // Bounded deterministic sampling keeps a long clip's calibration cost predictable.
        int stride=Math.max(1,count/900);List<Integer> pairs=new ArrayList<>();int n=0;
        for(int i=1;i<times.length;i++)if(observed[i]!=null&&n++%stride==0&&times[i]-times[i-1]<.06&&times[i]>times[0]+.4&&times[i]<times[times.length-1]-.4)pairs.add(i);
        double best=-Double.MAX_VALUE,offset=0;Fit winner=null;double[] scores=new double[251];Arrays.fill(scores,-Double.MAX_VALUE);
        for(int step=0;step<scores.length;step++){
            if(cancelled.getAsBoolean())throw new IllegalArgumentException("Calibration cancelled");
            double delay=(step-125)*.002;
            try{Fit fit=fit(gyro,times,observed,pairs,delay);scores[step]=fit.score;if(fit.score>best){best=fit.score;offset=delay;winner=fit;}}
            catch(IllegalArgumentException ignored){/* Missing coverage or insufficient excitation cannot calibrate. */}
        }
        if(winner==null||best<.80||Math.abs(offset)>.23)throw new IllegalArgumentException("Calibration is not reliable. Record a clear sample with gentle, varied movement on all three axes");
        double alternative=-Double.MAX_VALUE;for(int i=0;i<scores.length;i++)if(Math.abs((i-125)*.002-offset)>=.024)alternative=Math.max(alternative,scores[i]);
        double separation=best-alternative;
        if(!Double.isFinite(alternative)||separation<.015)throw new IllegalArgumentException("Gyro timing is ambiguous. Use non-repeating movement and record another sample");
        return new Model(offset,best,separation,winner.map);
    }
    private static Fit fit(Gyro gyro,double[] times,double[][] observed,List<Integer> pairs,double delay){
        int size=pairs.size();if(size<120)throw new IllegalArgumentException("Too few road features");
        double[][] x=new double[size][],y=new double[size][];double[] mx=new double[3],my=new double[3];int train=0;
        for(int j=0;j<size;j++){int i=pairs.get(j);x[j]=gyro.between(times[i-1]+delay,times[i]+delay);y[j]=observed[i];
            if(j%5!=0){train++;for(int k=0;k<3;k++){mx[k]+=x[j][k];my[k]+=y[j][k];}}}
        for(int k=0;k<3;k++){mx[k]/=train;my[k]/=train;}
        double[][] covariance=new double[3][3],cross=new double[3][3];
        for(int j=0;j<size;j++)if(j%5!=0)for(int a=0;a<3;a++)for(int b=0;b<3;b++){
            covariance[a][b]+=(x[j][a]-mx[a])*(x[j][b]-mx[b]);cross[a][b]+=(y[j][a]-my[a])*(x[j][b]-mx[b]);}
        double[][] inverse=inverse(covariance);double condition=norm(covariance)*norm(inverse);
        if(condition>150||!Double.isFinite(condition))throw new IllegalArgumentException("Camera movement needs three independent axes");
        double[][] map=new double[3][3];for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)map[i][j]+=cross[i][k]*inverse[k][j];
        double determinant=Math.abs(det(map));if(determinant<.01||determinant>30||norm(map)>12)throw new IllegalArgumentException("Implausible camera motion model");
        double error=0,variation=0;int[] valid=new int[3];double[] componentError=new double[3],componentVariation=new double[3];
        for(int j=0;j<size;j++)if(j%5==0)for(int a=0;a<3;a++){
            double predicted=my[a];for(int k=0;k<3;k++)predicted+=map[a][k]*(x[j][k]-mx[k]);
            double d=y[j][a]-predicted,v=y[j][a]-my[a];error+=d*d;variation+=v*v;componentError[a]+=d*d;componentVariation[a]+=v*v;valid[a]++;
        }
        for(int k=0;k<3;k++)if(componentVariation[k]<1e-8||componentError[k]/componentVariation[k]>.35)throw new IllegalArgumentException("An image axis does not match the gyro");
        return new Fit(map,1-error/Math.max(variation,1e-12));
    }
    private static double norm(double[][] m){double s=0;for(double[] row:m)for(double v:row)s+=v*v;return Math.sqrt(s);}
    private static double det(double[][] m){return m[0][0]*(m[1][1]*m[2][2]-m[1][2]*m[2][1])-m[0][1]*(m[1][0]*m[2][2]-m[1][2]*m[2][0])+m[0][2]*(m[1][0]*m[2][1]-m[1][1]*m[2][0]);}
    private static double[][] inverse(double[][] m){double d=det(m);if(Math.abs(d)<1e-24)throw new IllegalArgumentException("Insufficient independent movement");double[][] r=new double[3][3];
        for(int i=0;i<3;i++)for(int j=0;j<3;j++)r[j][i]=(m[(i+1)%3][(j+1)%3]*m[(i+2)%3][(j+2)%3]-m[(i+1)%3][(j+2)%3]*m[(i+2)%3][(j+1)%3])/d;
        return r;
    }
}
