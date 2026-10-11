package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A bounded vector road context, fetched only for an explicit export. */
final class OverlayRoads {
    final List<double[]> ways = new ArrayList<>();
    boolean illustrative;
    static OverlayRoads load(Context context, RideTelemetry data) {
        OverlayRoads roads = new OverlayRoads();
        RideTelemetry.Sample start = data.at(0);
        if(start!=null&&start.accuracy>30)start=null;
        if(start==null)for (RideTelemetry.Sample s : data.samples) if(s.elapsed >= data.clipElapsed && s.accuracy <= 30){start=s;break;}
        if(start==null||!Double.isFinite(start.lat)||!Double.isFinite(start.lon)||Math.abs(start.lat)>85||Math.abs(start.lon)>180)return roads;
        double lat=Math.rint(start.lat*100)/100,lon=Math.rint(start.lon*100)/100;
        File cache=new File(context.getCacheDir(),"overlay-roads-"+lat+"-"+lon+".json");
        try {
            String json;
            if(cache.isFile()&&System.currentTimeMillis()-cache.lastModified()<7L*86400000)
                json=new String(java.nio.file.Files.readAllBytes(cache.toPath()),StandardCharsets.UTF_8);
            else {
                String query="[out:json][timeout:12];way[highway~\"^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|motorway_link|trunk_link|primary_link|secondary_link)$\"]("+(lat-.025)+","+(lon-.025)+","+(lat+.025)+","+(lon+.025)+");out geom 1800;";
                HttpURLConnection c=(HttpURLConnection)new URL("https://overpass-api.de/api/interpreter").openConnection();
                c.setConnectTimeout(5000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setDoOutput(true);
                c.setRequestProperty("User-Agent","RideDeck-Dash/"+BuildConfig.VERSION_NAME+" (+https://github.com/axelsarassamit/RideDeck-Dash)");
                c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                try {try(OutputStream out=c.getOutputStream()){out.write(("data="+URLEncoder.encode(query,"UTF-8")).getBytes(StandardCharsets.UTF_8));}
                    if(c.getResponseCode()!=200)return roads;
                    ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Map context too large");out.write(bytes,0,n);}}
                    json=out.toString("UTF-8");new JSONObject(json);java.nio.file.Files.write(cache.toPath(),out.toByteArray());
                } finally {c.disconnect();}
            }
            JSONArray elements=new JSONObject(json).getJSONArray("elements");
            for(int i=0;i<elements.length();i++){JSONArray geometry=elements.getJSONObject(i).optJSONArray("geometry");if(geometry==null)continue;double[] points=new double[geometry.length()*2];for(int n=0;n<geometry.length();n++){points[n*2]=geometry.getJSONObject(n).getDouble("lat");points[n*2+1]=geometry.getJSONObject(n).getDouble("lon");}roads.ways.add(points);}
        } catch(Exception ignored){/* The honest offline route view remains available. */}
        return roads;
    }
}
