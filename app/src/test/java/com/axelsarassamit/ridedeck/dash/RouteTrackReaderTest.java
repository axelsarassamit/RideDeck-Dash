package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;import java.io.*;import java.util.*;import static org.junit.Assert.*;
public class RouteTrackReaderTest {
 @Test public void longRideRetainsBothEndsAndBoundsMemory()throws Exception{File file=File.createTempFile("route", ".csv");try{try(PrintWriter out=new PrintWriter(file)){out.println("t,time,lat,lon,speed");out.println("0,0,NaN,0,0");out.println("0,0,999,0,0");for(int i=0;i<60000;i++)out.println("0,0,"+(i/1000.0)+",10,0");}List<double[]> points=RouteTrackReader.read(file);assertTrue(points.size()<=4096);assertEquals(0,points.get(0)[0],0);assertEquals(59.999,points.get(points.size()-1)[0],0.000001);}finally{file.delete();}}
}
