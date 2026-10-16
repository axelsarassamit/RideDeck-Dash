package com.axelsarassamit.ridedeck.dash;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.content.Context;
/** Small native line icons matching the RideDeck cockpit controls. */
final class RideGlyph extends Drawable {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);private final String action;private final Context context;
    RideGlyph(Context c,String action){context=c;this.action=action;int size=Math.round(24*c.getResources().getDisplayMetrics().density);setBounds(0,0,size,size);}
    private void line(Canvas c,float... xy){Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,ink);}
    @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);boolean enabled=false;for(int state:getState())if(state==android.R.attr.state_enabled)enabled=true;
        ink.setColor(!enabled?RideStyle.MUTED:action.equals("Stop recording")?RideStyle.DANGER:AppearancePalette.accent(RideAppearance.accentIndex(context),RideAppearance.isDark(context)));ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
        switch(action){
            case "Setup":c.drawCircle(12,12,6,ink);c.drawCircle(12,12,2,ink);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10);}break;
            case "Recordings":c.drawRoundRect(4,6,21,21,2,2,ink);line(c,2,17,2,3,17,3);line(c,10,10,16,14,10,18,10,10);break;
            case "Protect clip":line(c,12,2,21,6,20,15,12,22,4,15,3,6,12,2);line(c,8,12,11,15,16,9);break;
            case "Stop recording":ink.setStyle(Paint.Style.FILL);c.drawRoundRect(5,5,19,19,2,2,ink);break;
            default:c.drawCircle(12,12,9,ink);ink.setStyle(Paint.Style.FILL);c.drawCircle(12,12,5,ink);break;
        }c.restore();
    }
    @Override public boolean isStateful(){return true;}
    @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    @Override public void setAlpha(int alpha){ink.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){ink.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
