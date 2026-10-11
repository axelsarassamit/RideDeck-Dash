package com.axelsarassamit.ridedeck.dash;
import java.io.*;import java.util.*;
/** Recorded monotonic timestamps align telemetry to each individual loop segment. */
final class RideTelemetry {
 static final class Sample {long elapsed,utc;double lat,lon,accuracy,speed=Double.NaN,altitude=Double.NaN,heading=Double.NaN,distance;int battery=-1;boolean charging;}
 final List<Sample> samples=new ArrayList<>();long clipElapsed=-1,clipUtc=-1;String camera;
 RideTelemetry(File clip)throws IOException {
  camera=clip.getName().startsWith("front")?"Front camera":"Rear camera";
  File capture=new File(clip.getParentFile(),"capture.csv");
  if(capture.isFile())try(BufferedReader reader=new BufferedReader(new FileReader(capture))){String line;reader.readLine();while((line=reader.readLine())!=null){String[] c=line.split(",",-1);if(c.length>7&&c[7].equals(clip.getName())&&(c[4].equals("file_started")||c[4].equals("frame_1"))){clipUtc=Long.parseLong(c[0]);clipElapsed=Long.parseLong(c[1]);break;}}}
  File route=new File(clip.getParentFile(),"route.csv");if(!route.isFile())return;
  try(BufferedReader reader=new BufferedReader(new FileReader(route))){reader.readLine();String line;Sample previous=null;while((line=reader.readLine())!=null){String[] c=line.split(",",-1);if(c.length<6)continue;try{Sample s=new Sample();s.utc=Long.parseLong(c[0]);s.elapsed=Long.parseLong(c[1]);s.lat=Double.parseDouble(c[2]);s.lon=Double.parseDouble(c[3]);s.accuracy=Double.parseDouble(c[4]);s.speed=value(c,5);s.altitude=value(c,6);s.heading=value(c,7);if(c.length>8&&!c[8].isEmpty())s.battery=Integer.parseInt(c[8]);s.charging=c.length>9&&c[9].equals("1");s.distance=previous==null?0:previous.distance;
   if(previous!=null&&s.elapsed>previous.elapsed&&s.elapsed-previous.elapsed<=6000000000L&&s.accuracy<=30&&previous.accuracy<=30){double d=distance(previous.lat,previous.lon,s.lat,s.lon);if(d/( (s.elapsed-previous.elapsed)/1e9)<100&&d>Math.max(3,Math.min(s.accuracy,previous.accuracy)/2))s.distance+=d;}
   samples.add(s);previous=s;}catch(NumberFormatException ignored){/* Keep usable preceding records in interrupted CSVs. */}}}
 }
 static double value(String[] c,int n){return c.length>n&&!c[n].isEmpty()?Double.parseDouble(c[n]):Double.NaN;}
 Sample at(long timeUs){if(clipElapsed<0)return null;long target=clipElapsed+timeUs*1000;int lo=0,hi=samples.size()-1,found=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(samples.get(mid).elapsed<=target){found=mid;lo=mid+1;}else hi=mid-1;}if(found<0)return null;Sample s=samples.get(found);return target-s.elapsed<=6000000000L?s:null;}
 static double distance(double lat1,double lon1,double lat2,double lon2){double a=Math.toRadians(lat2-lat1),b=Math.toRadians(lon2-lon1);double h=Math.sin(a/2)*Math.sin(a/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(b/2)*Math.sin(b/2);return 6371000*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));}
}
