package com.axelsarassamit.ridedeck.dash;
import android.content.*;import android.media.*;import android.media.audiofx.NoiseSuppressor;import android.os.*;
import java.io.*;import java.util.*;import java.util.function.*;import org.json.*;

/** Short foreground-only capability probe. Never changes production recording preferences. */
final class AudioCheckRecorder {
    static boolean bluetooth(AudioDeviceInfo device){return device!=null&&(device.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||(Build.VERSION.SDK_INT>=31&&device.getType()==AudioDeviceInfo.TYPE_BLE_HEADSET));}
    static final class Result {final File folder;final JSONObject report;Result(File f,JSONObject r){folder=f;report=r;}}
    static Result run(Context context,int mode,BooleanSupplier cancelled,Consumer<String> progress)throws Exception{
        AudioManager manager=context.getSystemService(AudioManager.class);
        if(manager.getMode()!=AudioManager.MODE_NORMAL)throw new IOException("Finish the call or voice app before sound checking");
        File folder=new File(context.getFilesDir(),"audio-checks/"+System.currentTimeMillis());if(!folder.mkdirs())throw new IOException("Cannot save sound check");
        List<Capture> captures=new ArrayList<>();boolean communication=false;JSONObject report=new JSONObject();
        try{
            if(mode!=0){
                if(Build.VERSION.SDK_INT<31)throw new IOException("Headset routing check requires Android 12 or newer");
                AudioDeviceInfo choice=null;for(AudioDeviceInfo d:manager.getAvailableCommunicationDevices())if(bluetooth(d)){choice=d;break;}
                if(choice==null)throw new IOException("Connect a Bluetooth headset with a microphone");
                manager.setMode(AudioManager.MODE_IN_COMMUNICATION);communication=true;
                if(!manager.setCommunicationDevice(choice))throw new IOException("Android rejected the headset voice route");
                long until=SystemClock.elapsedRealtime()+4000;
                while(SystemClock.elapsedRealtime()<until&&!cancelled.getAsBoolean()){
                    AudioDeviceInfo actual=manager.getCommunicationDevice();if(bluetooth(actual))break;Thread.sleep(100);
                }
                if(!bluetooth(manager.getCommunicationDevice()))throw new IOException("Headset voice connection timed out");
            }
            AudioDeviceInfo phone=null,headset=null;
            AudioDeviceInfo[] devices=manager.getDevices(AudioManager.GET_DEVICES_INPUTS);AudioInputs.discover(devices);
            for(AudioDeviceInfo d:devices){if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC&&phone==null)phone=d;if(bluetooth(d)&&headset==null)headset=d;}
            if(mode!=1){if(phone==null)throw new IOException("Phone microphone unavailable");captures.add(new Capture(phone,"phone",48000,true,manager,folder));}
            if(mode!=0){if(headset==null)throw new IOException("Headset connected for playback but no microphone input is exposed");captures.add(new Capture(headset,"voice",16000,false,manager,folder));}
            for(Capture c:captures)c.prepare(context);
            for(Capture c:captures)c.record.startRecording();
            long end=SystemClock.elapsedRealtime()+8000;List<Thread> readers=new ArrayList<>();
            for(Capture c:captures){Thread reader=new Thread(()->c.read(end,cancelled),"DashSoundCheck-"+c.name);readers.add(reader);reader.start();}
            while(SystemClock.elapsedRealtime()<end&&!cancelled.getAsBoolean()){
                progress.accept("Recording "+(mode==0?"phone sound":mode==1?"headset voice":"phone and headset separately")+": "+Math.max(1,(end-SystemClock.elapsedRealtime()+999)/1000)+" seconds remaining");Thread.sleep(200);
            }
            for(Thread reader:readers){reader.join(2000);if(reader.isAlive())throw new IOException("Sound check reader timed out");}
            if(cancelled.getAsBoolean())throw new IOException("Sound check cancelled");
            JSONArray tracks=new JSONArray();boolean verified=true;Set<Integer> routes=new HashSet<>();
            for(Capture c:captures){JSONObject track=c.report();tracks.put(track);verified&=track.getBoolean("routeVerified")&&track.getBoolean("signalPresent");routes.add(c.actualId);}
            if(mode==2)verified&=routes.size()==2;
            report.put("test",mode==0?"phone":mode==1?"headset":"phone-plus-headset");report.put("verified",verified);report.put("tracks",tracks);
            report.put("meaning",mode==2?(verified?"Two distinct active routes with sound verified in this short test. Live video mixing is not implemented.":"Separate phone ambience and headset voice were NOT verified. Do not use a combined mix."):(verified?"Requested route and non-silent samples verified":"Requested route or usable signal was not verified"));
            for(Capture c:captures)c.close();captures.clear();
            File original=new File(folder,"phone-original.wav");if(original.exists()){filtered(original,new File(folder,"phone-natural-50Hz.wav"),50);filtered(original,new File(folder,"phone-wind-120Hz.wav"),120);}
            try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(folder,"report.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(report.toString(2));}
            return new Result(folder,report);
        }catch(Exception error){
            JSONArray tracks=new JSONArray();for(Capture c:captures)tracks.put(c.report());
            report.put("verified",false).put("tracks",tracks).put("error",String.valueOf(error.getMessage())).put("meaning","Not verified: "+error.getMessage());
            try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(folder,"report.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(report.toString(2));}
            return new Result(folder,report);
        }finally{
            for(Capture c:captures)c.close();
            if(communication){try{manager.clearCommunicationDevice();}finally{manager.setMode(AudioManager.MODE_NORMAL);}}
        }
    }
    private static final class Capture {
        final AudioDeviceInfo wanted;final String name;final int rate,channels,source;final File folder;AudioRecord record;NoiseSuppressor noise;PcmWave wave;
        long samples,clipped;double squares;int peak,actualId=-1,actualType=-1,routeChecks,routeFailures;boolean silenced,noiseEnabled;String error="",actualName="Unknown";JSONArray routing=new JSONArray();long firstReadNs;AudioTimestamp timestamp=new AudioTimestamp();boolean timestampValid;
        Capture(AudioDeviceInfo device,String label,int sampleRate,boolean ambience,AudioManager manager,File output){
            wanted=device;name=label;folder=output;
            int chosenRate=sampleRate;int[] rates=device.getSampleRates();if(bluetooth(device)&&rates.length>0){chosenRate=0;for(int r:rates)if(r<=48000)chosenRate=Math.max(chosenRate,r);if(chosenRate==0)chosenRate=sampleRate;}rate=chosenRate;
            boolean stereo=false;if(ambience)for(int n:device.getChannelCounts())if(n==2)stereo=true;channels=stereo?2:1;
            source=ambience?("true".equals(manager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED))?MediaRecorder.AudioSource.UNPROCESSED:MediaRecorder.AudioSource.VOICE_RECOGNITION):MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        void prepare(Context context)throws Exception{
            if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission denied");
            int channelMask=channels==2?AudioFormat.CHANNEL_IN_STEREO:AudioFormat.CHANNEL_IN_MONO;
            int size=AudioRecord.getMinBufferSize(rate,channelMask,AudioFormat.ENCODING_PCM_16BIT);if(size<=0)throw new IOException(name+" audio format unsupported");
            AudioRecord.Builder builder=new AudioRecord.Builder().setAudioSource(source).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(channelMask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(size*4,rate*channels/5));
            if(Build.VERSION.SDK_INT>=30)builder.setPrivacySensitive(false);
            record=builder.build();if(record.getState()!=AudioRecord.STATE_INITIALIZED||!record.setPreferredDevice(wanted))throw new IOException(name+" preferred input rejected");
            if(name.equals("voice")&&NoiseSuppressor.isAvailable()){try{noise=NoiseSuppressor.create(record.getAudioSessionId());if(noise!=null){noise.setEnabled(true);noiseEnabled=noise.getEnabled();}}catch(RuntimeException ignored){noiseEnabled=false;}}
            wave=new PcmWave(new File(folder,name+"-original.wav"),rate,channels);
        }
        void read(long end,BooleanSupplier cancelled){
            short[] data=new short[Math.max(320,rate/100)*channels];long last=0,start=SystemClock.elapsedRealtime();
            try{
                while(SystemClock.elapsedRealtime()<end&&!cancelled.getAsBoolean()){
                    int count=record.read(data,0,data.length,AudioRecord.READ_NON_BLOCKING);
                    if(count<0)throw new IOException("Audio read failed: "+count);
                    if(count>0){if(firstReadNs==0)firstReadNs=SystemClock.elapsedRealtimeNanos();wave.append(data,count);for(int i=0;i<count;i++){int value=Math.abs((int)data[i]);peak=Math.max(peak,value);if(value>=32760)clipped++;squares+=(double)data[i]*data[i];}samples+=count;}
                    long now=SystemClock.elapsedRealtime();if(now-last>=200&&now-start>=1000){
                        AudioDeviceInfo actual=record.getRoutedDevice();routeChecks++;boolean correct=actual!=null&&actual.getId()==wanted.getId();
                        if(actual!=null){actualId=actual.getId();actualType=actual.getType();actualName=AudioInputs.label(actual);}
                        if(Build.VERSION.SDK_INT>=29){AudioRecordingConfiguration configuration=record.getActiveRecordingConfiguration();if(configuration==null||configuration.isClientSilenced()){silenced=true;correct=false;}}
                        if(!correct)routeFailures++;
                        JSONObject route=new JSONObject().put("elapsed_ms",now-start).put("actualId",actualId).put("actualType",actualType).put("matches",correct);routing.put(route);last=now;
                        timestampValid=record.getTimestamp(timestamp,AudioTimestamp.TIMEBASE_MONOTONIC)==AudioRecord.SUCCESS;
                    }
                    if(count==0)Thread.sleep(5);
                }
            }catch(Exception failure){error=failure.getMessage();}
        }
        JSONObject report()throws JSONException{
            double rms=samples==0?0:Math.sqrt(squares/samples)/32768;
            return new JSONObject().put("track",name).put("preferred",AudioInputs.label(wanted)).put("preferredId",wanted.getId()).put("actual",actualName).put("actualId",actualId).put("actualType",actualType)
                .put("rate",rate).put("channels",channels).put("source",source).put("samples",samples).put("rms",rms).put("peak",peak).put("clippedSamples",clipped).put("silenced",silenced).put("voiceNoiseSuppression",noiseEnabled)
                .put("routeVerified",error.isEmpty()&&!silenced&&routeChecks>0&&routeFailures==0).put("signalPresent",samples>rate*channels*4L&&rms>0.00003).put("error",error).put("routes",routing)
                .put("firstReadElapsedNs",firstReadNs).put("timestampValid",timestampValid).put("timestampFrames",timestamp.framePosition).put("timestampNs",timestamp.nanoTime);
        }
        void close(){if(noise!=null){try{noise.release();}catch(Exception ignored){}noise=null;}if(record!=null){try{if(record.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)record.stop();}catch(Exception ignored){}try{record.release();}catch(Exception ignored){}record=null;}if(wave!=null){try{wave.close();}catch(Exception ignored){}wave=null;}}
    }
    static void filtered(File source,File destination,int cutoff)throws IOException{
        try(RandomAccessFile in=new RandomAccessFile(source,"r")){
            byte[] header=new byte[44];in.readFully(header);java.nio.ByteBuffer info=java.nio.ByteBuffer.wrap(header).order(java.nio.ByteOrder.LITTLE_ENDIAN);int channels=info.getShort(22),rate=info.getInt(24);RumbleFilter filter=new RumbleFilter(rate,channels,cutoff);int channel=0;
            try(PcmWave out=new PcmWave(destination,rate,channels)){byte[] bytes=new byte[8192];short[] values=new short[4096];int n;while((n=in.read(bytes))>0){java.nio.ByteBuffer data=java.nio.ByteBuffer.wrap(bytes,0,n).order(java.nio.ByteOrder.LITTLE_ENDIAN);int count=n/2;for(int i=0;i<count;i++){values[i]=filter.pcm(data.getShort(),channel);channel=(channel+1)%channels;}out.append(values,count);}}
        }
    }
}
