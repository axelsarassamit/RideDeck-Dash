package com.axelsarassamit.ridedeck.dash;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.*;
import android.location.*;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Size;
import android.view.Surface;
import java.io.*;
import java.util.*;

/** Camera ownership and file rollover live here, never in the rotating dashboard. */
public final class RecordingService extends Service {
    public static volatile boolean running, finalizing;
    static volatile boolean capturing;
    static long companionStartUntil;
    static String companionStartToken, companionStartOwner;
    public static volatile String state = "Ready to record";
    public static volatile String cameraStatus = "Rear + front, 720p / 30 fps";
    public static volatile Location position;
    public static volatile File rideDirectory;
    private HandlerThread thread;
    private Handler worker;
    private ClipStore store;
    private DashSettings settings;
    public static volatile String audioStatus="Audio off";
    public static volatile long startedAt;
    private android.media.AudioManager audioManager;
    private boolean communicationRoute;
    private final List<Track> tracks = new ArrayList<>();
    private LocationManager locations;
    private BufferedWriter route;
    private PowerManager.WakeLock wake;
    private boolean stopping;
    private final DriveAutomation driveAutomation=new DriveAutomation();
    private long protectUntil;
    private int rotation;
    private volatile int startedTracks;
    private BufferedWriter events;
    private final BroadcastReceiver battery = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (worker != null) worker.post(() -> enforceGate(i)); }
    };
    private final LocationListener gps = new LocationListener() {
        @Override public void onLocationChanged(Location p) {
            if (!running || route == null) return;
            position = new Location(p);
            try {
                route.write(String.format(Locale.ROOT, "%d,%d,%.8f,%.8f,%.1f,%s,%s,%s,%d,%d\n", p.getTime(), p.getElapsedRealtimeNanos(),
                    p.getLatitude(), p.getLongitude(), p.getAccuracy(), p.hasSpeed() ? Float.toString(p.getSpeed()) : "",p.hasAltitude()?Double.toString(p.getAltitude()):"",p.hasBearing()?Float.toString(p.getBearing()):"",batteryPercent(),charging()?1:0));
                route.flush();
            } catch (IOException error) { fail("Route could not be saved"); }
        }
        @Override public void onProviderDisabled(String provider) { position = null; }
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onStatusChanged(String p, int s, Bundle b) {}
    };
    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("DashCapture"); thread.start(); worker = new Handler(thread.getLooper());
        settings = new DashSettings(this,true); store = new ClipStore(this,settings);
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("recording", "Dash recording", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        String action = intent == null ? "STOP" : intent.getAction();
        if ("STOP".equals(action)) { DashboardActivity.motionConsumed=true;DashboardActivity.consumedSession=DashBridgeProvider.SESSIONS.generation(); worker.post(() -> finish("Recording stopped")); return START_NOT_STICKY; }
        if ("PROTECT".equals(action)) { worker.post(this::protect); return START_NOT_STICKY; }
        boolean companionStart = companionStartUntil > SystemClock.elapsedRealtime()
            && companionStartToken != null && intent != null
            && companionStartToken.equals(intent.getStringExtra("startToken"))
            && DashBridgeProvider.SESSIONS.isOwnedBy(companionStartOwner, SystemClock.elapsedRealtime());
        companionStartUntil = 0;
        companionStartToken = null; companionStartOwner = null;
        if (running) return START_NOT_STICKY;
        if (!DashApplication.isVisible() && !companionStart) { stopSelf(); return START_NOT_STICKY; }
        rotation = intent == null ? 0 : intent.getIntExtra("rotation", 0);
        PreviewFrames.rotation = rotation;
        PreviewFrames.rear = null; PreviewFrames.front = null;
        PreviewFrames.rearAt = 0; PreviewFrames.frontAt = 0;
        int types = Build.VERSION.SDK_INT >= 30 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA : 0;
        if (!"off".equals(settings.text("audio","off")) && Build.VERSION.SDK_INT>=30) types |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
        if (Build.VERSION.SDK_INT >= 29 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            types |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
        try {
            if (Build.VERSION.SDK_INT >= 29 && types != 0) startForeground(1, notification("Starting cameras"), types);
            else startForeground(1, notification("Starting cameras"));
        } catch (RuntimeException error) {
            state = "Android blocked recording. Keep RideDeck visible and try Start again."; stopping = true;
            android.util.Log.w("RideDeckDash", "Foreground recording start denied", error); stopSelf(); return START_NOT_STICKY;
        }
        running = true; stopping = false; state = "Starting cameras";
        capturing = false;
        registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        worker.post(() -> begin(intent == null ? "dual" : intent.getStringExtra("mode")));
        return START_NOT_STICKY;
    }
    private int batteryPercent(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b==null?-1:RecordingGate.batteryPercent(b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),b.getIntExtra(BatteryManager.EXTRA_SCALE,-1));}
    private boolean charging(){Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return b!=null&&b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;}
    private Notification notification(String text) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, DashboardActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, RecordingService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent protect = PendingIntent.getService(this, 2, new Intent(this, RecordingService.class).setAction("PROTECT"), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "recording").setSmallIcon(com.axelsarassamit.ridedeck.dash.R.drawable.ic_dash)
            .setContentTitle("RideDeck Dash").setContentText(text).setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "Check camera view", open).build())
            .addAction(new Notification.Action.Builder(null, "Protect clip", protect).build())
            .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).build();
    }
    private void begin(String mode) {
        try {
            Intent power = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (!enforceGate(power)) return;
            store.reserve(2*settings.clipBytes());
            startedAt=SystemClock.elapsedRealtime(); audioStatus="Audio off";
            rideDirectory = new File(store.root(), Long.toString(System.currentTimeMillis()));
            if (!rideDirectory.mkdirs()) throw new IOException("Cannot create ride folder");
            route = new BufferedWriter(new FileWriter(new File(rideDirectory, "route.csv")));
            route.write("utc_ms,elapsed_ns,latitude,longitude,accuracy_m,speed_m_s,altitude_m,bearing_deg,battery_percent,charging\n"); route.flush();
            events = new BufferedWriter(new FileWriter(new File(rideDirectory, "capture.csv")));
            events.write("utc_ms,elapsed_ns,sensor_ns,camera,event,eis,ois,file\n"); events.flush();
            position = null; startedTracks = 0; protectUntil = 0;
            CameraManager manager = getSystemService(CameraManager.class);
            String rear = null, front = null;
            for (String camera : manager.getCameraIdList()) {
                Integer facing = manager.getCameraCharacteristics(camera).get(CameraCharacteristics.LENS_FACING);
                if (Objects.equals(facing, CameraCharacteristics.LENS_FACING_BACK) && rear == null) rear = camera;
                if (Objects.equals(facing, CameraCharacteristics.LENS_FACING_FRONT) && front == null) front = camera;
            }
            if ("dual".equals(mode)) {
                rear = RearLenses.selected(manager,settings);
                if (rear == null || front == null || Build.VERSION.SDK_INT < 30) throw new IOException("Dual camera recording unavailable. Select one camera.");
                boolean supported = false;
                for (Set<String> group : manager.getConcurrentCameraIds()) if (group.contains(rear) && group.contains(front)) supported = true;
                if (!supported) throw new IOException("Phone does not advertise simultaneous front and rear cameras.");
            }
            if(!"dual".equals(mode)&&!"front".equals(mode))rear=RearLenses.selected(manager,settings);
            if (!"front".equals(mode)) tracks.add(new Track(manager, rear, "rear"));
            if (!"rear".equals(mode)) tracks.add(new Track(manager, front, "front"));
            for (Track track : tracks) track.prepare();
            // Concurrent camera devices must be opened before any capture session is configured.
            for (Track track : tracks) track.open();
            locations = getSystemService(LocationManager.class);
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gps, thread.getLooper());
            wake = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RideDeckDash:Capture");
            wake.acquire(12 * 60 * 60 * 1000L);
            worker.postDelayed(gateTick, 500);
            worker.postDelayed(() -> { if (running && startedTracks != tracks.size()) fail("Camera start timed out"); }, 15000);
        } catch (Exception error) { fail(error.getMessage()); }
    }
    private final Runnable gateTick = new Runnable() {
        @Override public void run() {
            if (!running || stopping) return;
            if (enforceGate(registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)))) worker.postDelayed(this, 500);
        }
    };
    private boolean enforceGate(Intent power) {
        if (!running || stopping) return false;
        boolean plugged = power != null && power.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int percent = power == null ? -1 : RecordingGate.batteryPercent(power.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), power.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
        RecordingGate.Result gate = RecordingGate.evaluate(plugged, percent, DashApplication.isVisible(),
            DashBridgeProvider.SESSIONS.isActive(SystemClock.elapsedRealtime()));
        if (gate != RecordingGate.Result.ALLOWED) {
            finish(gate == RecordingGate.Result.COMPANION_REQUIRED ? "Stopped: no active RideDeck ride session" : "Stopped: connect charger or charge to 50%");
            return false;
        }
        Location fix=position;long now=SystemClock.elapsedRealtime();boolean fresh=fix!=null&&fix.hasSpeed()&&SystemClock.elapsedRealtimeNanos()-fix.getElapsedRealtimeNanos()<6000000000L;
        int stopSeconds=settings.number("autostop",60);
        if(stopSeconds>0 && driveAutomation.stop(now,plugged,fresh,fix==null?0:fix.getSpeed(),fix==null?Float.MAX_VALUE:fix.getAccuracy(),stopSeconds*1000L)){
            finish("Stopped: charger unplugged and bike stationary");return false;
        }
        return true;
    }
    private void protect() {
        if (!running || stopping) return;
        protectUntil = SystemClock.elapsedRealtime() + settings.number("protect",60)*1000L;
        try {
            for (Track track : tracks) {
                if (track.current != null) ClipStore.mark(track.current, ".protected");
                if (track.next != null) ClipStore.mark(track.next, ".protected");
            }
            for (File f : store.clips()) if (f.lastModified() >= System.currentTimeMillis() - settings.number("protect",60)*1000L) ClipStore.mark(f, ".protected");
            state = "Recording · clip protected";
            getSystemService(NotificationManager.class).notify(1, notification(state));
        } catch (IOException error) { fail("Cannot protect clip"); }
    }
    private void fail(String message) { finish("Stopped: " + (message == null ? "camera error" : message)); }
    private void finish(String reason) {
        if (stopping) return;
        stopping = true; finalizing=true; state = "Saving clips";
        worker.removeCallbacksAndMessages(null);
        for (Track track : tracks) track.close(); tracks.clear();
        if (locations != null) try { locations.removeUpdates(gps); } catch (SecurityException ignored) {}
        if (route != null) try { route.close(); } catch (IOException ignored) {}
        route = null; position = null;
        if (events != null) try { events.close(); } catch (IOException ignored) {}
        events = null;
        try { unregisterReceiver(battery); } catch (IllegalArgumentException ignored) {}
        if (wake != null && wake.isHeld()) wake.release();
        if(communicationRoute && Build.VERSION.SDK_INT>=31) audioManager.clearCommunicationDevice(); communicationRoute=false;
        capturing=false; running=false; state=reason; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }
    @Override public void onDestroy() {
        if (worker != null) worker.post(() -> { finish("Recording stopped"); thread.quitSafely(); finalizing=false; });
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }

    private final class Track {
        final CameraManager manager; final String id, label;
        CameraDevice camera; CameraCaptureSession session; MediaRecorder recorder;
        File current, next; boolean recording; int count; int eis, ois; Size size;
        int frames;
        android.media.ImageReader view;
        Integer appliedEis, appliedOis;
        boolean basicEis, opticalAvailable;
        CaptureRequest.Builder request;
        String modeKey;
        Track(CameraManager m, String cameraId, String name) throws IOException {
            if (cameraId == null) throw new IOException(name + " camera unavailable");
            manager = m; id = cameraId; label = name;
        }
        File file() throws IOException {
            store.reserve(settings.clipBytes());
            File f = new File(rideDirectory, label + "-" + String.format(Locale.ROOT, "%04d", ++count) + ".mp4");
            if (SystemClock.elapsedRealtime() < protectUntil) ClipStore.mark(f, ".protected");
            return f;
        }
        void prepare() throws Exception {
            CameraCharacteristics c = manager.getCameraCharacteristics(id);
            Size[] choices = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(MediaRecorder.class);
            size = null;
            for (Size choice : choices) if (choice.getWidth() == settings.width() && choice.getHeight() == settings.height()) size = choice;
            if (size == null) throw new IOException(label + " camera has no selected recording resolution");
            int[] modes = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            // Preview stabilization mode also stabilizes video streams on supporting devices.
            for (int value : modes == null ? new int[0] : modes) if (value > eis) eis = value;
            if (modes != null) for (int value : modes) if (value == 1) basicEis = true;
            int[] optical = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            if (optical != null) for (int value : optical) if (value == 1) opticalAvailable = true;
            if (eis == 0 && optical != null) for (int value : optical) ois = Math.max(ois, value);
            modeKey = "session-v2:" + Build.FINGERPRINT + ":" + id + ":"+settings.height()+"p30:" + tracks.size();
            android.content.SharedPreferences learned = getSharedPreferences("camera-modes", MODE_PRIVATE);
            int knownEis = learned.getInt(modeKey + ":eis", -1), knownOis = learned.getInt(modeKey + ":ois", -1);
            boolean supportedKnown = false;
            if (modes != null) for (int value : modes) if (value == knownEis && knownEis > 0) supportedKnown = true;
            if (supportedKnown) { eis = knownEis; ois = 0; }
            else if (knownEis == 0 && knownOis == 1 && opticalAvailable && settings.number("stabilization",1)!=0) { eis = 0; ois = 1; }
            if(settings.number("stabilization",1)==0){eis=0;ois=0;}
            view=PreviewFrames.reader(label,c.get(CameraCharacteristics.SENSOR_ORIENTATION),worker);
            recorder = new MediaRecorder();
            boolean withAudio=!"off".equals(settings.text("audio","off")) && ("rear".equals(label)||tracks.size()==1);
            android.media.AudioDeviceInfo chosen=null;
            if(withAudio){
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new IOException("Microphone permission required");
                audioManager=getSystemService(android.media.AudioManager.class);
                String desired=settings.text("audio","off");
                if(!"auto".equals(desired)){for(android.media.AudioDeviceInfo device:audioManager.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS))if(AudioInputs.key(device).equals(desired))chosen=device;
                    if(chosen==null)throw new IOException("Selected microphone disconnected; choose another input");}
                boolean bluetooth=chosen!=null&&(chosen.getType()==android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT>=31 && chosen.getType()==android.media.AudioDeviceInfo.TYPE_BLE_HEADSET));
                if(bluetooth){if(Build.VERSION.SDK_INT<31)throw new IOException("Bluetooth routing requires Android 12 or newer");
                    android.media.AudioDeviceInfo routeDevice=null;for(android.media.AudioDeviceInfo device:audioManager.getAvailableCommunicationDevices())if(device.getType()==chosen.getType()&&device.getAddress().equals(chosen.getAddress()))routeDevice=device;
                    if(routeDevice==null||!audioManager.setCommunicationDevice(routeDevice))throw new IOException("Headset communication input unavailable");communicationRoute=true;}
                recorder.setAudioSource(bluetooth?MediaRecorder.AudioSource.VOICE_COMMUNICATION:settings.number("source",1));
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            if(withAudio){recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioEncodingBitRate(128000);recorder.setAudioSamplingRate(48000);if(chosen!=null&&!recorder.setPreferredDevice(chosen))throw new IOException("Selected microphone routing rejected");}
            recorder.setVideoSize(size.getWidth(), size.getHeight()); recorder.setVideoFrameRate(30);
            recorder.setVideoEncodingBitRate(settings.bitrate());
            int sensor = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int degrees = rotation * 90;
            recorder.setOrientationHint("front".equals(label) ? (sensor + degrees) % 360 : (sensor - degrees + 360) % 360);
            current = file(); recorder.setOutputFile(current.getPath());
            recorder.setMaxFileSize(settings.clipBytes());
            recorder.setOnInfoListener((r, what, extra) -> worker.post(() -> {
                if (!running || stopping) return;
                try {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING) {
                        next = file(); recorder.setNextOutputFile(next);
                    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED) {
                        ClipStore.mark(current, ".ready"); event("file_finalized", current, null); current = next; next = null;
                        event("file_started", current, null);
                    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) fail("Video rollover failed");
                } catch (Exception error) { fail(error.getMessage()); }
            }));
            recorder.setOnErrorListener((r, what, extra) -> worker.post(() -> fail(label + " recorder error " + what)));
            recorder.prepare();
        }
        void open() throws CameraAccessException {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("Camera permission was revoked");
            manager.openCamera(id, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice device) {
                    if (!running || stopping) { device.close(); return; }
                    camera = device;
                    if (tracks.stream().allMatch(t -> t.camera != null)) for (Track t : tracks) t.configure();
                }
                @Override public void onDisconnected(CameraDevice d) { d.close(); fail(label + " camera disconnected"); }
                @Override public void onError(CameraDevice d, int error) { d.close(); fail(label + " camera error " + error); }
            }, worker);
        }
        void configure() {
            try {
                Surface target = recorder.getSurface();
                request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                request.addTarget(target); request.addTarget(view.getSurface());
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                CameraFocus.apply(request, characteristics, settings, label);
                android.util.Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                android.util.Range<Integer> fps = null;
                if (ranges != null) for (android.util.Range<Integer> range : ranges)
                    if (range.getUpper() == 30 && (fps == null || range.getLower() > fps.getLower())) fps = range;
                if (fps != null) request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, eis);
                request.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, ois);
                CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
                    @Override public void onConfigured(CameraCaptureSession s) {
                        if (!running || stopping) { s.close(); return; }
                        session = s;
                        try {
                            session.setRepeatingRequest(request.build(), captureCallback, worker);
                            recorder.start(); recording = true; event("file_started",current,null);
                            if(!"off".equals(settings.text("audio","off")) && ("rear".equals(label)||tracks.size()==1)){
                                if(Build.VERSION.SDK_INT<28) throw new IOException("Verified audio input routing requires Android 9 or newer");
                                audioStatus="Checking microphone input";
                                worker.postDelayed(thisTrackAudioCheck(),1000);
                            }
                            startedTracks++;
                            if (startedTracks == tracks.size()) {
                                capturing = true;
                                state = "Recording";
                                updateCameraStatus();
                                getSystemService(NotificationManager.class).notify(1, notification("Recording " + tracks.size() + " camera(s)"));
                            }
                        } catch (Exception error) { fail(label + ": " + error.getMessage()); }
                    }
                    @Override public void onConfigureFailed(CameraCaptureSession s) { s.close(); fail(label + " recording configuration unavailable"); }
                };
                if (Build.VERSION.SDK_INT >= 28) {
                    android.hardware.camera2.params.SessionConfiguration configuration = new android.hardware.camera2.params.SessionConfiguration(
                        android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                        Arrays.asList(new android.hardware.camera2.params.OutputConfiguration(target), new android.hardware.camera2.params.OutputConfiguration(view.getSurface())),
                        command -> worker.post(command), callback);
                    configuration.setSessionParameters(request.build());
                    camera.createCaptureSession(configuration);
                } else camera.createCaptureSession(Arrays.asList(target,view.getSurface()), callback, worker);
            } catch (Exception error) { fail(error.getMessage()); }
        }
        Runnable thisTrackAudioCheck(){return ()->{if(!running||stopping||recorder==null)return;
            if(Build.VERSION.SDK_INT>=28){android.media.AudioDeviceInfo actual=recorder.getRoutedDevice();audioStatus=actual==null?"Audio route unavailable":AudioInputs.label(actual);
                if(actual==null || (!"auto".equals(settings.text("audio","off"))&&!AudioInputs.key(actual).equals(settings.text("audio","off"))))fail("Selected microphone is not the active input");
                else worker.postDelayed(thisTrackAudioCheck(),2000);
            }
        };}
        private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                                @Override public void onCaptureCompleted(CameraCaptureSession s, CaptureRequest r, TotalCaptureResult result) {
                                    if (events == null || stopping) return;
                                    frames++;
                                    if (frames != 1 && frames != 30 && frames != 90 && frames != 180 && frames != 240) return;
                                    appliedEis = result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE);
                                    appliedOis = result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE);
                                    android.util.Log.i("DashFocus", label + " frame=" + frames + " af=" + result.get(CaptureResult.CONTROL_AF_MODE) + " distance=" + result.get(CaptureResult.LENS_FOCUS_DISTANCE));
                                    updateCameraStatus();
                                    try {
                                        event("frame_"+frames, current, result.get(CaptureResult.SENSOR_TIMESTAMP));
                                        if (frames == 240 && appliedEis != null && appliedOis != null && (appliedEis > 0 || appliedOis > 0))
                                            getSharedPreferences("camera-modes", MODE_PRIVATE).edit().putInt(modeKey+":eis",appliedEis).putInt(modeKey+":ois",appliedOis).apply();
                                        if (frames == 90 && Objects.equals(appliedEis, 0) && eis == 2 && basicEis) {
                                            eis = 1; request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, eis);
                                            session.setRepeatingRequest(request.build(), this, worker);
                                        } else if (frames == 180 && Objects.equals(appliedEis, 0) && opticalAvailable && settings.number("stabilization",1)!=0) {
                                            eis = 0; ois = 1;
                                            request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, eis);
                                            request.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, ois);
                                            session.setRepeatingRequest(request.build(), this, worker);
                                        }
                                    } catch (Exception error) { fail("Cannot verify camera stabilization: " + error.getMessage()); }
                                }
        };
        void event(String kind, File file, Long sensor) throws IOException {
            if (events == null) return;
            events.write(System.currentTimeMillis()+","+SystemClock.elapsedRealtimeNanos()+","+(sensor==null?"":sensor)+","+label+","+kind+","+appliedEis+","+appliedOis+","+(file==null?"":file.getName())+"\n");
            events.flush();
        }
        void close() {
            // Keep camera frames flowing until the recorder drains and finalizes its MP4.
            if (recorder != null) {
                if (recording) try { recorder.stop(); ClipStore.mark(current, ".ready"); event("file_finalized", current, null); } catch (Exception ignored) { /* Retain unverified output, never label it complete. */ }
                recorder.release();
            }
            if (session != null) { try { session.stopRepeating(); session.abortCaptures(); } catch (Exception ignored) {} session.close(); }
            if (camera != null) camera.close();
            if(view!=null)view.close();
        }
    }
    private void updateCameraStatus() {
        cameraStatus = tracks.stream().map(t -> t.label + " "+settings.height()+"p · " +
            (t.appliedEis == null ? "checking stabilization" : t.appliedEis > 0 ? "electronic stabilization" :
            t.appliedOis != null && t.appliedOis > 0 ? "optical stabilization" : "stabilization off"))
            .collect(java.util.stream.Collectors.joining("\n"));
    }
}
