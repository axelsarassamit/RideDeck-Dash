package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;
import static org.junit.Assert.*;
public class HybridMotionTest {
    static HybridMotion.Gyro fixture(){int n=5201;double[] t=new double[n];double[][] w=new double[n][3];
        for(int i=0;i<n;i++){t[i]=-1+i*.0025;double a=t[i];w[i][0]=.3*Math.sin(5*a)+.1*Math.cos(13*a);w[i][1]=.2*Math.sin(9*a)+.13*Math.cos(17*a);w[i][2]=.15*Math.cos(7*a)+.1*Math.sin(21*a);}
        return new HybridMotion.Gyro(t,w);
    }
    @Test public void recoversClockDelayAndRotatedCameraMapping(){
        HybridMotion.Gyro g=fixture();double[] t=new double[301];double[][] observed=new double[t.length][];double[][] map={{0,.8,.1},{.7,0,.03},{.01,0,1}};
        HybridMotion.Model truth=new HybridMotion.Model(.042,1,1,map);
        for(int i=0;i<t.length;i++){t[i]=i/30.0;if(i>0)observed[i]=truth.delta(g,t[i-1],t[i]);}
        HybridMotion.Model m=HybridMotion.calibrate(g,t,observed,()->false);assertEquals(.042,m.offset,.0021);assertTrue(m.score>.99);
        for(int i=0;i<3;i++)for(int j=0;j<3;j++)assertEquals(map[i][j],m.map[i][j],.01);
    }
    @Test public void constantOrStationaryMovementCannotProduceCalibration(){
        HybridMotion.Gyro g=fixture();double[] t=new double[301];double[][] v=new double[301][3];for(int i=0;i<t.length;i++)t[i]=i/30.0;
        try{HybridMotion.calibrate(g,t,v,()->false);fail("Stationary clip must be rejected");}catch(IllegalArgumentException expected){}
    }
    @Test public void rejectsGyroGapsInsteadOfInventingMovement(){
        double[] t=new double[100];double[][] w=new double[100][3];for(int i=0;i<t.length;i++)t[i]=i*.005+(i>=50?.1:0);
        try{new HybridMotion.Gyro(t,w);fail("Missing motion cannot be interpolated across a large gap");}catch(IllegalArgumentException expected){}
    }
    @Test public void trapezoidIntegrationInterpolatesRateWithinSample(){
        double[] t=new double[100];double[][] w=new double[100][3];for(int i=0;i<t.length;i++){t[i]=i*.01;w[i][0]=2*t[i];}
        HybridMotion.Gyro g=new HybridMotion.Gyro(t,w);assertEquals(.125*.125,g.at(.125)[0],1e-10);
        assertEquals(.25*.25-.125*.125,g.between(.125,.25)[0],1e-10);
    }
    @Test public void honorsCancellation(){double[] t=new double[301];double[][] o=new double[301][3];for(int i=0;i<t.length;i++)t[i]=i/30.0;
        try{HybridMotion.calibrate(fixture(),t,o,()->true);fail();}catch(IllegalArgumentException e){assertTrue(e.getMessage().contains("cancelled"));}}
}
