package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Explicit opt-in, localhost-only original evidence transfer. Never runs in the normal suite. */
public class RearEvidenceTest {
    @Test public void serveFinalizedRearOriginalsAndLogs()throws Exception {
        Bundle arguments=InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Explicit local evidence export only","1".equals(arguments.getString("rearEvidence")));
        String token=arguments.getString("evidenceToken","");if(!token.matches("[a-zA-Z0-9-]{20,80}"))throw new IOException("A private transfer token is required");
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        if(RecordingService.running||RecordingService.finalizing)throw new IOException("Stop recording first");
        List<File> files=new ArrayList<>();Set<File> rides=new LinkedHashSet<>();
        for(File file:new ClipStore(c).clips())if(file.getName().startsWith("rear-")&&ClipStore.finalized(file)&&!file.getParentFile().getName().startsWith("social-test-")){files.add(file);rides.add(file.getParentFile());}
        for(File ride:rides){File[] logs=ride.listFiles((dir,name)->name.endsWith(".csv")||name.equals("motion.json"));if(logs!=null)files.addAll(Arrays.asList(logs));}
        JSONArray entries=new JSONArray();byte[] buffer=new byte[1024*1024];long total=0;
        for(int i=0;i<files.size();i++){
            File file=files.get(i);MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(buffer))!=-1)hash.update(buffer,0,n);}
            StringBuilder hex=new StringBuilder();for(byte value:hash.digest())hex.append(String.format(Locale.ROOT,"%02x",value&255));
            entries.put(new JSONObject().put("id",i).put("ride",file.getParentFile().getName()).put("name",file.getName()).put("bytes",file.length()).put("modified_ms",file.lastModified()).put("sha256",hex.toString()));total+=file.length();
        }
        byte[] index=new JSONObject().put("schema",1).put("created_ms",System.currentTimeMillis()).put("scope","all finalized internal rear originals and same-ride CSV logs").put("bytes",total).put("files",entries).toString(2).getBytes(StandardCharsets.UTF_8);
        try(ServerSocket server=new ServerSocket(19867,8,InetAddress.getByName("127.0.0.1"))){
            server.setSoTimeout(10000);long deadline=android.os.SystemClock.elapsedRealtime()+30*60*1000;boolean done=false;
            Bundle ready=new Bundle();ready.putString("stream","Evidence ready: "+files.size()+" files, "+total+" bytes\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,ready);
            while(!done&&android.os.SystemClock.elapsedRealtime()<deadline){
                try(Socket socket=server.accept()){
                    socket.setSoTimeout(30000);BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));String line=reader.readLine();if(line==null||line.length()>1024)continue;
                    String[] request=line.split(" ");String path=request.length>1?request[1]:"";int headers=0;String h;while((h=reader.readLine())!=null&&!h.isEmpty()){headers+=h.length();if(headers>16384)throw new IOException("Headers too large");}
                    OutputStream out=socket.getOutputStream();String prefix="/"+token+"/";
                    if(!line.startsWith("GET ")||!path.startsWith(prefix)){header(out,404,0);continue;}
                    String name=path.substring(prefix.length());
                    if(name.equals("index")){header(out,200,index.length);out.write(index);}
                    else if(name.equals("finish")){header(out,200,0);done=true;}
                    else if(name.matches("file/[0-9]+")){
                        int id=Integer.parseInt(name.substring(5));if(id>=files.size()||RecordingService.running||RecordingService.finalizing){header(out,409,0);continue;}
                        File file=files.get(id);header(out,200,file.length());try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
                    }else header(out,404,0);
                    out.flush();
                }catch(SocketTimeoutException ignored){}
            }
            if(!done)throw new IOException("Transfer session expired; originals remain intact");
        }
    }
    private static void header(OutputStream out,int status,long length)throws IOException {out.write(("HTTP/1.1 "+status+" Result\r\nContent-Length: "+length+"\r\nContent-Type: application/octet-stream\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));}
}
