package com.axelsarassamit.ridedeck.dash;
import android.content.Context;import android.graphics.*;import android.graphics.drawable.Drawable;import androidx.media3.effect.BitmapOverlay;import androidx.media3.common.util.Size;import java.util.*;import java.text.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class TelemetryOverlay extends BitmapOverlay {
 final long offsetUs;final boolean social;final RideTelemetry data;final DashSettings settings;final OverlayRoads roads;final Drawable logo;int width=1280,height=720;Bitmap cached;long bucket=-1;
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Canvas c;float u;
 TelemetryOverlay(Context context,RideTelemetry data,DashSettings settings,long offsetUs,boolean social,OverlayRoads roads){this.data=data;this.settings=settings;this.offsetUs=offsetUs;this.social=social;this.roads=roads;logo=context.getDrawable(R.drawable.ic_ridedeck_overlay);}
 @Override public void configure(Size size){super.configure(size);width=size.getWidth();height=size.getHeight();}
 boolean on(String key){return settings.number("overlay_"+key,key.equals("speed")||key.equals("time")||key.equals("route")?1:0)==1;}
 @Override public void release()throws androidx.media3.common.VideoFrameProcessingException{super.release();if(cached!=null){cached.recycle();cached=null;}}
 void text(String value,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);c.drawText(value,x,y,p);}
 void panel(float x,float y,float w,float h){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(settings.number("overlay_alpha",210),17,24,34));c.drawRoundRect(x,y,x+w,y+h,16*u,16*u,p);p.setColor(Color.argb(75,144,200,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(u);c.drawRoundRect(x,y,x+w,y+h,16*u,16*u,p);p.setStyle(Paint.Style.FILL);}
 @Override public Bitmap getBitmap(long timeUs){timeUs+=offsetUs;long tick=timeUs/(on("route")?100000:1000000);if(tick==bucket&&cached!=null)return cached;bucket=tick;
  Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);c=new Canvas(bitmap);u=Math.min(width/1280f,height/720f)*(settings.number("overlay_size",28)/28f);boolean portrait=height>width;
  float margin=24*u,left=margin,right=width-(social?width*.16f:margin),usable=right-left,top=social?height*.09f:margin,bottom=height-(social?height*.21f:margin);int white=Color.rgb(244,246,250),blue=Color.rgb(144,200,255),muted=Color.rgb(173,191,207);
  // The RideDeck mark is mandatory even in a data-free preset.
  float brandW=230*u;panel(left,top,brandW,66*u);logo.setBounds((int)(left+8*u),(int)(top+6*u),(int)(left+62*u),(int)(top+60*u));logo.draw(c);text("RideDeck",left+76*u,top+31*u,24*u,white,true);text("DASH",left+77*u,top+51*u,12*u,blue,true);
  RideTelemetry.Sample s=data.at(timeUs);boolean mph=settings.number("overlay_mph",0)==1;
  if(on("time")){String time=data.clipUtc<0?"TIME UNAVAILABLE":new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date(data.clipUtc+timeUs/1000));float w=190*u;float x=portrait?left:right-w;float y=portrait?top+76*u:top;panel(x,y,w,66*u);text(time,x+16*u,y+29*u,24*u,white,true);text(data.clipUtc<0?"NO TIMESTAMP":new SimpleDateFormat("dd MMM yyyy",Locale.ROOT).format(new Date(data.clipUtc+timeUs/1000)),x+16*u,y+50*u,13*u,muted,false);}
  List<String[]> stats=new ArrayList<>();
  if(on("distance"))stats.add(new String[]{s==null?"?":String.format(Locale.ROOT,"%.1f",s.distance/(mph?1609.344:1000)),mph?"RIDE / MI":"RIDE / KM"});
  if(on("altitude"))stats.add(new String[]{s==null||Double.isNaN(s.altitude)?"?":String.format(Locale.ROOT,"%.0f m",s.altitude),"ALTITUDE"});
  if(on("heading"))stats.add(new String[]{s==null||Double.isNaN(s.heading)?"?":String.format(Locale.ROOT,"%.0f°",s.heading),"HEADING"});
  if(on("accuracy"))stats.add(new String[]{s==null?"?":String.format(Locale.ROOT,"±%.0f m",s.accuracy),"GPS ACCURACY"});
  if(on("battery"))stats.add(new String[]{s==null||s.battery<0?"?":s.battery+"%",s!=null&&s.charging?"CHARGING":"BATTERY"});
  if(on("camera"))stats.add(new String[]{data.camera.startsWith("Front")?"FRONT":"REAR","CAMERA"});
  int columns=portrait?2:3,rows=(stats.size()+columns-1)/columns;float statsH=rows*66*u,speedH=on("speed")?106*u:0,hudH=speedH+statsH;float mapW=portrait?Math.min(usable,340*u):290*u,mapH=mapW*.82f;
  float hudW=portrait?Math.min(usable,Math.max(340*u,stats.size()>0?360*u:340*u)):Math.min(usable-(on("route")?mapW+16*u:0),Math.max(340*u,stats.size()>2?640*u:400*u));float hudY=settings.number("overlay_top",0)==1?top+(portrait?156:82)*u:bottom-hudH;
  if(hudH>0){panel(left,hudY,hudW,hudH);if(on("speed")){text(s==null||Double.isNaN(s.speed)?"?":String.format(Locale.ROOT,"%.0f",s.speed*(mph?2.236936:3.6)),left+18*u,hudY+72*u,66*u,white,true);text(mph?"mph":"km/h",left+145*u,hudY+47*u,21*u,blue,true);text(s!=null&&!Double.isNaN(s.speed)?"GPS SPEED":"GPS UNAVAILABLE",left+145*u,hudY+75*u,12*u,muted,true);}
   float cellW=hudW/columns;for(int i=0;i<stats.size();i++){float x=left+(i%columns)*cellW+16*u,y=hudY+speedH+(i/columns)*66*u;text(stats.get(i)[0],x,y+27*u,22*u,white,true);text(stats.get(i)[1],x,y+48*u,10*u,muted,true);}}
  if(on("route")){float mapX=portrait?left:right-mapW,mapY=portrait?(settings.number("overlay_top",0)==1?hudY+hudH+12*u:bottom-hudH-mapH-12*u):settings.number("overlay_top",0)==1?hudY:bottom-mapH;drawMap(timeUs,mapX,mapY,mapW,mapH,blue,white,muted);}
  cached=bitmap;return bitmap;
 }
 void drawMap(long timeUs,float x,float y,float w,float h,int blue,int white,int muted){panel(x,y,w,h);text("RIDE MAP",x+14*u,y+26*u,13*u,white,true);text("N",x+w-25*u,y+26*u,12*u,blue,true);float mx=x+10*u,my=y+38*u,mw=w-20*u,mh=h-68*u;RideTelemetry.Sample now=data.moving(timeUs);c.save();c.clipRect(mx,my,mx+mw,my+mh);p.setColor(Color.rgb(23,35,47));c.drawRect(mx,my,mx+mw,my+mh,p);
  p.setColor(Color.argb(60,92,121,145));p.setStrokeWidth(u);for(int i=1;i<5;i++){c.drawLine(mx+mw*i/5,my,mx+mw*i/5,my+mh,p);c.drawLine(mx,my+mh*i/5,mx+mw,my+mh*i/5,p);}
  if(now!=null){double lat=now.lat,lon=now.lon,span=settings.number("overlay_zoom",500)/111320.0,cos=Math.max(.01,Math.cos(Math.toRadians(lat)));boolean follow=settings.number("overlay_follow",1)==1;
   if(!follow&&!data.samples.isEmpty()){double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(RideTelemetry.Sample s:data.samples)if(s.accuracy<=30){minLat=Math.min(minLat,s.lat);maxLat=Math.max(maxLat,s.lat);minLon=Math.min(minLon,s.lon);maxLon=Math.max(maxLon,s.lon);}if(minLat<=maxLat){lat=(minLat+maxLat)/2;lon=(minLon+maxLon)/2;span=Math.max(span,Math.max(maxLat-minLat,(maxLon-minLon)*cos)*1.25);}}
   for(double[] road:roads.ways){Path path=new Path();for(int i=0;i<road.length;i+=2){float px=mx+mw/2+(float)((road[i+1]-lon)*cos/span)*mw,py=my+mh/2-(float)((road[i]-lat)/span)*mw;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(49,68,84));p.setStrokeWidth(6*u);c.drawPath(path,p);p.setColor(Color.rgb(104,129,149));p.setStrokeWidth(2*u);c.drawPath(path,p);}
   Path trace=new Path();long last=-1,target=data.clipElapsed+timeUs*1000;for(RideTelemetry.Sample s:data.samples){if(s.elapsed>target)break;if(s.accuracy>30){last=-1;continue;}float px=mx+mw/2+(float)((s.lon-lon)*cos/span)*mw,py=my+mh/2-(float)((s.lat-lat)/span)*mw;if(last<0||s.elapsed-last>6000000000L)trace.moveTo(px,py);else trace.lineTo(px,py);last=s.elapsed;}float bx=mx+mw/2+(float)((now.lon-lon)*cos/span)*mw,by=my+mh/2-(float)((now.lat-lat)/span)*mw;if(last>=0)trace.lineTo(bx,by);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(5*u);p.setColor(blue);c.drawPath(trace,p);p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
   c.save();c.translate(bx,by);c.rotate(Double.isNaN(now.heading)?0:(float)now.heading);p.setColor(Color.argb(65,144,200,255));c.drawCircle(0,0,22*u,p);p.setColor(white);c.drawRoundRect(-4*u,-15*u,4*u,-7*u,3*u,3*u,p);c.drawRoundRect(-4*u,7*u,4*u,15*u,3*u,3*u,p);p.setColor(blue);c.drawRoundRect(-7*u,-9*u,7*u,9*u,4*u,4*u,p);p.setColor(white);p.setStrokeWidth(3*u);c.drawLine(-11*u,-7*u,11*u,-7*u,p);c.restore();
  }else{text("WAITING FOR GPS",mx+12*u,my+mh/2,14*u,muted,true);}c.restore();p.setStyle(Paint.Style.FILL);text(roads.illustrative?"ILLUSTRATIVE MAP":roads.ways.isEmpty()?"GPS TRACE · OFFLINE":"© OpenStreetMap contributors",x+12*u,y+h-12*u,10*u,muted,false);
 }
}
