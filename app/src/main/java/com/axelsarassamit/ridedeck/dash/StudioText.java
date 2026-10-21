package com.axelsarassamit.ridedeck.dash;

import android.graphics.*;
import androidx.media3.effect.BitmapOverlay;
import androidx.media3.common.util.Size;

/** Project title, timed on the final output rather than restarting on every cut. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class StudioText extends BitmapOverlay {
    final DashSettings settings;int width,height;Bitmap visible,empty;
    StudioText(DashSettings s){settings=s;}
    @Override public void configure(Size size){super.configure(size);width=size.getWidth();height=size.getHeight();}
    @Override public Bitmap getBitmap(long us){
        if(empty==null)empty=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        String value=settings.text("studio_text","");
        if(value.isEmpty()||us<settings.number("studio_text_start",0)*1000L||us>=settings.number("studio_text_end",1800000)*1000L)return empty;
        if(visible!=null)return visible;visible=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(visible);Paint p=new Paint(3);
        p.setTypeface(Typeface.create(settings.number("studio_text_font",0)==1?"sans-serif-condensed":"sans-serif",Typeface.BOLD));
        p.setTextSize(Math.min(width,height)*settings.number("studio_text_scale",6)/100f);
        p.setColor(settings.number("studio_color",0xff6284ff));p.setShadowLayer(p.getTextSize()*.08f,0,2,Color.BLACK);
        float x=width*settings.number("studio_text_x",8)/100f,y=height*settings.number("studio_text_y",35)/100f;
        for(String line:value.split("\n")){float available=width-x-width*.03f;float size=p.getTextSize();if(p.measureText(line)>available)p.setTextSize(size*available/p.measureText(line));
            if(settings.number("studio_text_bg",1)==1){Paint bg=new Paint(3);bg.setColor(0x990c111b);c.drawRoundRect(x-8,y-p.getTextSize(),Math.min(width-4,x+p.measureText(line)+8),y+p.getTextSize()*.3f,8,8,bg);}c.drawText(line,x,y,p);p.setTextSize(size);y+=size*1.3f;
        }return visible;
    }
    @Override public void release()throws androidx.media3.common.VideoFrameProcessingException{super.release();if(visible!=null){visible.recycle();visible=null;}if(empty!=null){empty.recycle();empty=null;}}
}
