package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.opengl.GLES20;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.*;
import androidx.media3.effect.*;

/** Rounded, transparent front-camera corners, matching the live dashboard inset. */
@androidx.annotation.OptIn(markerClass=UnstableApi.class)
final class PipCorners implements GlEffect {
    @Override public GlShaderProgram toGlShaderProgram(Context context,boolean hdr)throws VideoFrameProcessingException {return new Shader(hdr);}
    private static final class Shader extends BaseGlShaderProgram {
        final GlProgram program;
        Shader(boolean hdr)throws VideoFrameProcessingException {
            super(hdr,1);
            String vertex="attribute vec4 aPosition; varying vec2 vTex; void main(){gl_Position=aPosition;vTex=(aPosition.xy+1.0)*0.5;}";
            String fragment="precision mediump float; uniform sampler2D uTexture; uniform vec2 uSize; uniform float uRadius; varying vec2 vTex;"
                +"void main(){vec2 q=abs((vTex-0.5)*uSize)-(uSize*0.5-vec2(uRadius));"
                +"float d=length(max(q,vec2(0.0)))+min(max(q.x,q.y),0.0)-uRadius;"
                +"float a=1.0-smoothstep(-0.8,0.8,d);vec4 c=texture2D(uTexture,vTex);"
                +"float edge=smoothstep(-3.0,-1.0,d);gl_FragColor=vec4(mix(c.rgb,vec3(0.06,0.09,0.12),edge),c.a*a);}";
            try {program=new GlProgram(vertex,fragment);program.setBufferAttribute("aPosition",GlUtil.getNormalizedCoordinateBounds(),4);}
            catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}
        }
        @Override public Size configure(int width,int height){program.setFloatsUniform("uSize",new float[]{width,height});program.setFloatUniform("uRadius",Math.min(width,height)*.06f);return new Size(width,height);}
        @Override public void drawFrame(int texture,long timeUs)throws VideoFrameProcessingException {
            try{program.use();program.setSamplerTexIdUniform("uTexture",texture,0);program.bindAttributesAndUniforms();GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GlUtil.checkGlError();}
            catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e,timeUs);}
        }
        @Override public void release()throws VideoFrameProcessingException {try{program.delete();}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}finally{super.release();}}
    }
}
