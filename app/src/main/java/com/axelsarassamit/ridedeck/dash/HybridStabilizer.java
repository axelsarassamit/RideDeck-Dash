package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.opengl.GLES20;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.*;
import androidx.media3.effect.*;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.*;

/** Experimental small-angle hybrid correction with a row-dependent GPU inverse warp. */
@androidx.annotation.OptIn(markerClass=UnstableApi.class)
final class HybridStabilizer extends VideoStabilizer {
    final float[][] rows;final float outputZoom;final int rotation;final HybridMotion.Model model;
    HybridStabilizer(long[] t,float[][] correction,float[][] rolling,int strength,int w,int h,int rotation,HybridMotion.Model fit)throws IOException {
        super(t,correction,strength,w,h);rows=rolling;this.rotation=rotation;model=fit;
        outputZoom=requiredZoom(correction,rolling,w,h,zoom);
        if(outputZoom>1.65f)throw new IOException("Motion exceeds the hybrid crop budget. Use a shorter, steadier selection");
    }
    static HybridStabilizer create(File file,long startUs,long endUs,long[] times,List<float[]> flow,int strength,int w,int h,int rotation,BooleanSupplier cancelled)throws Exception {
        HybridEvidence data=HybridEvidence.load(file,startUs,endUs,cancelled);
        double[] clock=new double[times.length];double[][] observed=new double[times.length][];
        for(int i=0;i<times.length;i++){clock[i]=data.center((startUs+times[i])/1e6);float[] v=flow.get(i);if(v!=null)observed[i]=new double[]{v[0]/w,v[1]/h,v[2]};}
        HybridMotion.Model fit=HybridMotion.calibrate(data.gyro,clock,observed,cancelled);
        float[][] path=new float[times.length][3],rolling=new float[times.length][15];
        for(int i=0;i<times.length;i++){
            if(i%100==0&&cancelled.getAsBoolean())throw new IOException("Hybrid analysis cancelled");
            if(i>0){double[] g=fit.delta(data.gyro,clock[i-1],clock[i]);float[] delta={(float)(g[0]*w),(float)(g[1]*h),(float)g[2]};float[] v=flow.get(i);
                if(Math.abs(delta[0])>w*.15||Math.abs(delta[1])>h*.15||Math.abs(delta[2])>.15)throw new IOException("Motion exceeds the calibrated small-angle range");
                // Image residuals refine translation/rotation when consistent; gyro bridges failed matches.
                if(v!=null&&Math.abs(v[0]-delta[0])<w*.03&&Math.abs(v[1]-delta[1])<h*.03&&Math.abs(v[2]-delta[2])<.035)
                    for(int k=0;k<3;k++)delta[k]=delta[k]*.85f+v[k]*.15f;
                path[i]=StabilizationPath.compose(path[i-1],delta);
            }
            for(int row=0;row<5;row++){double[] d=fit.delta(data.gyro,clock[i],clock[i]+(row/4.0-.5)*data.readoutSeconds);
                if(Math.abs(d[2])>.04||Math.abs(d[0])>.08||Math.abs(d[1])>.08)throw new IOException("Readout motion exceeds the hybrid correction range");
                rolling[i][row*3]=(float)(d[0]*w);rolling[i][row*3+1]=(float)(d[1]*h);rolling[i][row*3+2]=(float)d[2];}
        }
        HybridStabilizer effect=new HybridStabilizer(times,StabilizationPath.smooth(times,path,new int[times.length],strength,w,h),rolling,strength,w,h,rotation,fit);
        JSONObject report=new JSONObject().put("schema",1).put("engine","experimental-small-angle-hybrid").put("source",file.getName())
            .put("source_bytes",file.length()).put("source_modified_ms",file.lastModified()).put("from_us",startUs).put("to_us",endUs)
            .put("clock_offset_ms",fit.offset*1000).put("heldout_agreement",fit.score).put("timing_separation",fit.separation)
            .put("readout_ms",data.readoutSeconds*1000).put("zoom",effect.outputZoom).put("frames",times.length)
            .put("camera",data.cameraId).put("rotation",rotation).put("outdoor_validated",false).put("lens_model","empirical central-scene small-angle map; not a full lens distortion calibration");
        JSONArray matrix=new JSONArray();for(double[] row:fit.map){JSONArray a=new JSONArray();for(double value:row)a.put(value);matrix.put(a);}report.put("gyro_to_image",matrix);
        android.util.AtomicFile destination=new android.util.AtomicFile(new File(file.getParentFile(),"hybrid-"+file.getName().replace(".mp4","")+"-report.json"));FileOutputStream output=null;
        try{output=destination.startWrite();output.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));destination.finishWrite(output);}catch(Exception error){if(output!=null)destination.failWrite(output);throw error;}
        return effect;
    }
    static float requiredZoom(float[][] corrections,float[][] rows,int w,int h,float base){
        double z=base;
        for(int i=0;i<corrections.length;i++){
            float[] q=corrections[i];double c=Math.cos(q[2]),s=Math.sin(q[2]),tx=Math.abs(-c*q[0]-s*q[1]),ty=Math.abs(s*q[0]-c*q[1]);
            double dx=0,dy=0,a=0;for(int row=0;row<5;row++){dx=Math.max(dx,Math.abs(rows[i][row*3]));dy=Math.max(dy,Math.abs(rows[i][row*3+1]));a=Math.max(a,Math.abs(rows[i][row*3+2]));}
            double sx=tx+a*ty+dx,sy=ty+a*tx+dy,angle=Math.min(1,Math.abs(s)+a);
            if(w<=2*sx||h<=2*sy)return Float.POSITIVE_INFINITY;
            z=Math.max(z,Math.max((w+h*angle)/(w-2*sx),(h+w*angle)/(h-2*sy))+.005);
        }
        return (float)z;
    }
    int index(long time){int i=Arrays.binarySearch(times,time);return i>=0?i:Math.max(0,Math.min(times.length-1,-i-2));}
    float[] value(long time){int i=index(time),j=Math.min(i+1,times.length-1);float f=j==i?0:Math.max(0,Math.min(1,(float)(time-times[i])/(times[j]-times[i])));float[] v=new float[18];
        for(int k=0;k<3;k++)v[k]=correction[i][k]*(1-f)+correction[j][k]*f;
        for(int k=0;k<15;k++)v[k+3]=rows[i][k]*(1-f)+rows[j][k]*f;return v;}
    @Override public BaseGlShaderProgram toGlShaderProgram(Context context,boolean hdr)throws VideoFrameProcessingException{return new Shader(hdr);}
    private final class Shader extends BaseGlShaderProgram {
        final GlProgram program;
        Shader(boolean hdr)throws VideoFrameProcessingException {
            super(hdr,1);
            String vertex="attribute vec4 aPosition;varying vec2 vTex;void main(){gl_Position=aPosition;vTex=(aPosition.xy+1.0)*0.5;}";
            String fragment="precision highp float;uniform sampler2D uTexture;uniform vec2 uSize;uniform vec2 uScan;uniform float uZoom;uniform vec3 uCorrection;uniform vec3 uRow0;uniform vec3 uRow1;uniform vec3 uRow2;uniform vec3 uRow3;uniform vec3 uRow4;varying vec2 vTex;"
                +"vec3 motion(float t){t=clamp(t,0.0,1.0);if(t<0.25)return mix(uRow0,uRow1,t*4.0);if(t<0.5)return mix(uRow1,uRow2,(t-0.25)*4.0);if(t<0.75)return mix(uRow2,uRow3,(t-0.5)*4.0);return mix(uRow3,uRow4,(t-0.75)*4.0);}"
                +"vec2 rotate(vec2 p,float a){return vec2(cos(a)*p.x-sin(a)*p.y,sin(a)*p.x+cos(a)*p.y);}"
                +"void main(){vec2 p=vec2(vTex.x-0.5,0.5-vTex.y)*uSize/uZoom;vec2 base=rotate(p-uCorrection.xy,-uCorrection.z);vec2 q=base;"
                +"for(int i=0;i<2;i++){vec3 d=motion(0.5+dot(q/uSize,uScan));q=rotate(base,d.z)+d.xy;}"
                +"vec2 uv=vec2(0.5+q.x/uSize.x,0.5-q.y/uSize.y);if(uv.x<0.0||uv.x>1.0||uv.y<0.0||uv.y>1.0)gl_FragColor=vec4(0.0);else gl_FragColor=texture2D(uTexture,uv);}";
            try{program=new GlProgram(vertex,fragment);program.setBufferAttribute("aPosition",GlUtil.getNormalizedCoordinateBounds(),4);}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}
        }
        @Override public Size configure(int w,int h){program.setFloatsUniform("uSize",new float[]{analysisWidth,analysisHeight});program.setFloatUniform("uZoom",outputZoom);
            program.setFloatsUniform("uScan",rotation==90?new float[]{-1,0}:rotation==180?new float[]{0,-1}:rotation==270?new float[]{1,0}:new float[]{0,1});return new Size(w,h);}
        @Override public void drawFrame(int texture,long time)throws VideoFrameProcessingException {
            float[] v=value(time);program.setFloatsUniform("uCorrection",Arrays.copyOfRange(v,0,3));for(int i=0;i<5;i++)program.setFloatsUniform("uRow"+i,Arrays.copyOfRange(v,3+i*3,6+i*3));
            try{program.use();program.setSamplerTexIdUniform("uTexture",texture,0);program.bindAttributesAndUniforms();GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GlUtil.checkGlError();}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e,time);}
        }
        @Override public void release()throws VideoFrameProcessingException {try{program.delete();}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}finally{super.release();}}
    }
}
