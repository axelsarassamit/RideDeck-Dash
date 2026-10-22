package com.axelsarassamit.ridedeck.dash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.*;

/** Strict eligibility checks. Callback arrival is never treated as an exposure timestamp. */
final class HybridEvidence {
    final HybridMotion.Gyro gyro;final double readoutSeconds;final String cameraId;final double[] frameStarts,midpointOffsets;
    HybridEvidence(HybridMotion.Gyro g,double readout,String id,double[] starts,double[] offsets){gyro=g;readoutSeconds=readout;cameraId=id;frameStarts=starts;midpointOffsets=offsets;}
    double center(double pts){int i=Arrays.binarySearch(frameStarts,pts);if(i>=0)return pts+midpointOffsets[i];i=Math.max(0,Math.min(frameStarts.length-1,-i-2));return pts+midpointOffsets[i];}
    static HybridEvidence load(File video,long startUs,long endUs,BooleanSupplier cancelled)throws Exception {
        if(!video.getName().startsWith("rear-"))throw new IOException("Hybrid calibration currently supports the rear camera only");
        File ride=video.getParentFile(),meta=new File(ride,"motion.json");
        if(!meta.isFile()||meta.length()>65536)throw new IOException("This clip has no usable motion data. Record a new hybrid sample");
        JSONObject json=new JSONObject(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8));
        if((json.optInt("schema")!=1&&json.optInt("schema")!=2)||!json.optBoolean("session_closed")||json.optBoolean("write_failed",true)||json.optLong("dropped_rows",1)!=0||!json.optBoolean("gyro_registered"))throw new IOException("Motion data is incomplete; hybrid correction is unavailable for this ride");
        JSONObject camera=null;JSONArray modes=json.getJSONArray("camera_modes");for(int i=0;i<modes.length();i++)if("rear".equals(modes.getJSONObject(i).optString("camera")))camera=modes.getJSONObject(i);
        if(camera==null||camera.optInt("timestamp_source",-1)!=1)throw new IOException("This camera does not provide a verified shared motion clock");
        long anchor=0;double skew=0;int count=0;String crop=null,physical=null;Double focus=null;List<Double> starts=new ArrayList<>(),offsets=new ArrayList<>();
        try(BufferedReader in=Files.newBufferedReader(new File(ride,"frames.csv").toPath(),StandardCharsets.UTF_8)){
            String header=in.readLine();if(header==null||!header.startsWith("sensor_ns,callback_elapsed_ns,camera,recording_file_at_callback,"))throw new IOException("Camera timing format is not supported");
            String line;int rows=0;while((line=in.readLine())!=null){if(++rows%1024==0&&cancelled.getAsBoolean())throw new IOException("Preparation cancelled");String[] v=line.split(",",-1);if(v.length<14||!v[2].equals("rear")||!v[3].equals(video.getName()))continue;
                if(!v[8].equals("0")||!v[9].equals("0"))throw new IOException("This clip uses native stabilization. Record a separate hybrid sample from Setup > Calibration & samples");
                long sensor=Long.parseLong(v[0]);long exposure=Long.parseLong(v[5]),readout=Long.parseLong(v[7]);
                if(readout<=0||readout>40000000||exposure<=0)throw new IOException("Camera readout or exposure timing is unavailable");
                if(anchor==0)anchor=sensor;
                double time=(sensor-anchor)/1e3;if(time<startUs-500000||time>endUs+500000)continue;
                if(count>0&&(Math.abs(readout/1e9-skew/count)>.0005||!Objects.equals(crop,v[12])||!Objects.equals(physical,v[13])))throw new IOException("Camera geometry changed within the sample; use one fixed rear lens");
                if(v[10].isEmpty())throw new IOException("Focus metadata is unavailable");double f=Double.parseDouble(v[10]);if(focus!=null&&Math.abs(f-focus)>.02)throw new IOException("Focus changed during the sample. Select locked road focus");
                if(!starts.isEmpty()&&time/1e6<=starts.get(starts.size()-1))throw new IOException("Camera timestamps are out of order");
                starts.add(time/1e6);offsets.add((exposure/2.0+readout/2.0)/1e9);
                focus=f;crop=v[12];physical=v[13];skew+=readout/1e9;count++;
            }
        }
        if(count<120||anchor==0)throw new IOException("Not enough camera timing rows for this selection");
        List<Double> times=new ArrayList<>();List<double[]> rates=new ArrayList<>();
        try(BufferedReader in=Files.newBufferedReader(new File(ride,"gyro.csv").toPath(),StandardCharsets.UTF_8)){
            String header=in.readLine();if(header==null||!header.startsWith("elapsed_ns,gx_rad_s,gy_rad_s,gz_rad_s,"))throw new IOException("Gyroscope format is not supported");
            String line;int rows=0;while((line=in.readLine())!=null){if(++rows%1024==0&&cancelled.getAsBoolean())throw new IOException("Preparation cancelled");String[] v=line.split(",",-1);if(v.length<6)throw new IOException("Incomplete gyroscope row");double t=(Long.parseLong(v[0])-anchor)/1e9;
                if(t<startUs/1e6-.6)continue;if(t>endUs/1e6+.6)break;
                if(Integer.parseInt(v[4])<1)throw new IOException("The gyroscope reported unreliable accuracy during this sample");
                times.add(t);rates.add(new double[]{Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3])});
            }
        }
        double[] t=new double[times.size()];for(int i=0;i<t.length;i++)t[i]=times.get(i);
        double[] fs=new double[starts.size()],mo=new double[fs.length];for(int i=0;i<fs.length;i++){fs[i]=starts.get(i);mo[i]=offsets.get(i);}
        return new HybridEvidence(new HybridMotion.Gyro(t,rates.toArray(new double[0][])),skew/count,camera.optString("id"),fs,mo);
    }
}
