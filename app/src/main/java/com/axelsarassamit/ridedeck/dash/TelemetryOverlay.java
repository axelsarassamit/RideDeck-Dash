package com.axelsarassamit.ridedeck.dash;
import android.graphics.*;import androidx.media3.effect.BitmapOverlay;import androidx.media3.common.util.Size;import java.util.*;import java.text.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class TelemetryOverlay extends BitmapOverlay {
 final RideTelemetry data;final DashSettings settings;int width=1280,height=720;Bitmap cached;long second=-1;
 TelemetryOverlay(RideTelemetry data,DashSettings settings){this.data=data;this.settings=settings;}
 @Override public void configure(Size size){super.configure(size);width=size.getWidth();height=size.getHeight();}
 boolean on(String key){return settings.number("overlay_"+key,key.equals("speed")||key.equals("time")?1:0)==1;}
 @Override public void release()throws androidx.media3.common.VideoFrameProcessingException{super.release();if(cached!=null){cached.recycle();cached=null;}}
 @Override public Bitmap getBitmap(long timeUs){long sec=timeUs/1000000;if(sec==second&&cached!=null)return cached;second=sec;
  Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float unit=width/1280f,text=settings.number("overlay_size",28)*unit;
  RideTelemetry.Sample s=data.at(timeUs);List<String> lines=new ArrayList<>();boolean mph=settings.number("overlay_mph",0)==1;
  if(on("brand"))lines.add("RideDeck Dash");if(on("camera"))lines.add(data.camera);
  if(on("speed"))lines.add(s!=null&&!Double.isNaN(s.speed)?String.format(Locale.ROOT,"%.0f %s · GPS",s.speed*(mph?2.236936:3.6),mph?"mph":"km/h"):"GPS speed unavailable");
  if(on("time"))lines.add(data.clipUtc<0?"Time unavailable":new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date(data.clipUtc+timeUs/1000)));
  if(on("distance"))lines.add(s==null?"Distance unavailable":String.format(Locale.ROOT,"Recorded ride %.2f %s",s.distance/(mph?1609.344:1000),mph?"mi":"km"));
  if(on("altitude"))lines.add(s==null||Double.isNaN(s.altitude)?"Altitude unavailable":String.format(Locale.ROOT,"Altitude %.0f m",s.altitude));
  if(on("heading"))lines.add(s==null||Double.isNaN(s.heading)?"Heading unavailable":String.format(Locale.ROOT,"Heading %.0f°",s.heading));
  if(on("accuracy"))lines.add(s==null?"GPS unavailable":String.format(Locale.ROOT,"GPS accuracy ±%.0f m",s.accuracy));
  if(on("battery"))lines.add(s==null||s.battery<0?"Battery unavailable":s.battery+"% battery"+(s.charging?" · charging":""));
  float margin=20*unit,padding=16*unit,line=text*1.35f,panelHeight=Math.max(line,lines.size()*line)+padding*2;float y=settings.number("overlay_top",0)==1?margin:height-margin-panelHeight;
  paint.setTextSize(text);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));float panelWidth=0;for(String row:lines)panelWidth=Math.max(panelWidth,paint.measureText(row));panelWidth+=padding*2;
  if(!lines.isEmpty()){paint.setColor(Color.argb(settings.number("overlay_alpha",160),16,24,34));canvas.drawRoundRect(margin,y,margin+panelWidth,y+panelHeight,12*unit,12*unit,paint);paint.setColor(Color.WHITE);for(int i=0;i<lines.size();i++)canvas.drawText(lines.get(i),margin+padding,y+padding+text+i*line,paint);}
  if(on("route")){float side=Math.min(220*unit,height*.35f),left=width-margin-side,top=settings.number("overlay_top",0)==1?margin:height-margin-side;paint.setColor(Color.argb(settings.number("overlay_alpha",160),16,24,34));canvas.drawRoundRect(left,top,left+side,top+side,12*unit,12*unit,paint);paint.setColor(Color.WHITE);paint.setTextSize(17*unit);canvas.drawText("GPS route trace",left+12*unit,top+23*unit,paint);
   List<RideTelemetry.Sample> points=new ArrayList<>();long target=data.clipElapsed+timeUs*1000;for(RideTelemetry.Sample point:data.samples)if(point.elapsed<=target&&point.accuracy<=30)points.add(point);
   if(points.size()>1){double minLat=90,maxLat=-90,minLon=180,maxLon=-180;for(RideTelemetry.Sample point:points){minLat=Math.min(minLat,point.lat);maxLat=Math.max(maxLat,point.lat);minLon=Math.min(minLon,point.lon);maxLon=Math.max(maxLon,point.lon);}double cosine=Math.max(.01,Math.cos(Math.toRadians((minLat+maxLat)/2)));double span=Math.max(Math.max(maxLat-minLat,(maxLon-minLon)*cosine),.0001);float room=side-36*unit;Path path=new Path();float x=0,z=0;long last=-1;
    for(RideTelemetry.Sample point:points){x=left+side/2+(float)((point.lon-(minLon+maxLon)/2)*cosine/span)*room;z=top+side/2+12*unit-(float)((point.lat-(minLat+maxLat)/2)/span)*room;if(last<0||point.elapsed-last>6000000000L)path.moveTo(x,z);else path.lineTo(x,z);last=point.elapsed;}paint.setColor(Color.rgb(144,200,255));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*unit);canvas.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);if(s!=null)canvas.drawCircle(x,z,5*unit,paint);
   }else{paint.setColor(Color.WHITE);canvas.drawText("Waiting for GPS",left+12*unit,top+side/2,paint);}}
  // Distinct bitmaps let Media3 upload each changed second. Cached bitmap remains owned by the renderer.
  cached=bitmap;return bitmap;
 }
}
