package com.axelsarassamit.ridedeck.dash;
import java.io.*;import java.util.*;
/** Bounded display snapshot. Originals retain every recorded coordinate. */
final class RouteTrackReader {
 static List<double[]> read(File file)throws IOException {
  ArrayList<double[]> points=new ArrayList<>();long count=0,stride=1;double[] last=null;
  try(BufferedReader reader=new BufferedReader(new FileReader(file))){String line;reader.readLine();while((line=reader.readLine())!=null){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
   String[] fields=line.split(",");if(fields.length<5)continue;double lat,lon;
   try{lat=Double.parseDouble(fields[2]);lon=Double.parseDouble(fields[3]);}catch(NumberFormatException invalid){continue;}
   if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;
   last=new double[]{lat,lon};if(count++%stride==0)points.add(last);
   if(points.size()>=4096){for(int i=0,j=0;i<points.size();i+=2)points.set(j++,points.get(i));points.subList(2048,points.size()).clear();stride*=2;}
  }}
  if(last!=null&&(points.isEmpty()||points.get(points.size()-1)!=last))points.add(last);return points;
 }
}
