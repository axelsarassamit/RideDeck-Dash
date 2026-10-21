package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.graphics.*;

/** One original approved Canva asset for the cockpit and exported watermark. */
final class VelocityArtwork {
    private static Bitmap shared;
    static synchronized Bitmap get(Context context){if(shared==null)shared=BitmapFactory.decodeResource(context.getResources(),R.drawable.velocity_logo);return shared;}
    static Rect source(Bitmap b,int l,int t,int r,int bottom){return new Rect(Math.round(b.getWidth()*l/200f),Math.round(b.getHeight()*t/200f),Math.round(b.getWidth()*r/200f),Math.round(b.getHeight()*bottom/200f));}
    static void compact(Canvas canvas,Bitmap bitmap,float x,float y,float unit){
        Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        // Keep the actual R/road and the actual RideDeck Dash lettering, with native proportions.
        canvas.drawBitmap(bitmap,source(bitmap,43,40,163,120),new RectF(x+10*unit,y+13*unit,x+70*unit,y+53*unit),ink);
        canvas.drawBitmap(bitmap,source(bitmap,19,129,181,167),new RectF(x+80*unit,y+16*unit,x+222*unit,y+(16+142*38f/162)*unit),ink);
    }
}
