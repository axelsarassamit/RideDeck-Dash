package com.axelsarassamit.ridedeck.dash;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;

/** Bound by a visible signed host with BIND_INCLUDE_CAPABILITIES. */
public final class CompanionRecordingService extends Service {
    private final Messenger messenger = new Messenger(new Handler(Looper.getMainLooper(), request -> {
        Bundle result = new Bundle();
        result.putInt("protocolVersion", 1);
        try {
            String caller = DashBridgeProvider.verifiedCaller(this, request.sendingUid);
            if ((request.what < 1 || request.what > 3) || request.getData().getInt("protocolVersion") != 1)
                throw new IllegalArgumentException("Unsupported recording request");
            if (!DashBridgeProvider.SESSIONS.isOwnedBy(caller, SystemClock.elapsedRealtime()))
                throw new IllegalStateException("Start a RideDeck ride session first");
            if (request.what == 2 || request.what == 3) {
                long now = SystemClock.elapsedRealtime();
                if (request.what == 2) PreviewFrames.companionPreviewUntil = now + 1500;
                result.putBoolean("accepted", true);
                result.putBoolean("recording", RecordingService.capturing);
                result.putBoolean("starting", RecordingService.running && !RecordingService.capturing && !RecordingService.finalizing);
                result.putString("message", RecordingService.state);
                if (request.what == 2) {
                    addFrame(result, "rear", PreviewFrames.rear, PreviewFrames.rearAt, now);
                    addFrame(result, "front", PreviewFrames.front, PreviewFrames.frontAt, now);
                }
                respond(request, result);
                return true;
            }
            DashSettings settings = new DashSettings(this, true);
            Intent battery = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            boolean plugged = battery != null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            int percent = battery == null ? -1 : RecordingGate.batteryPercent(
                battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
            if (RecordingGate.evaluate(plugged, percent, false, true) != RecordingGate.Result.ALLOWED)
                throw new IllegalStateException("Connect a charger or charge to at least 50%");
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || (!"off".equals(settings.text("audio", "off"))
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED))
                throw new IllegalStateException("Open Dash once to grant camera and microphone permissions");
            String token = java.util.UUID.randomUUID().toString();
            RecordingService.companionStartToken = token;
            RecordingService.companionStartOwner = caller;
            RecordingService.companionStartUntil = SystemClock.elapsedRealtime() + 15000;
            Intent start = new Intent(this, RecordingService.class).setAction("START")
                .setData(android.net.Uri.parse("ridedeck-dash://start/" + token))
                .putExtra("startToken", token).putExtra("mode", settings.text("mode", "dual"))
                .putExtra("rotation", getSystemService(android.view.WindowManager.class).getDefaultDisplay().getRotation());
            result.putParcelable("startIntent", android.app.PendingIntent.getForegroundService(this, 0, start,
                android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE));
            result.putBoolean("accepted", true);
            result.putString("message", "Ready to start. Check the camera angle in the preview.");
        } catch (RuntimeException error) {
            result.putBoolean("accepted", false);
            result.putString("message", error.getMessage());
        }
        respond(request, result);
        return true;
    }));
    private static void addFrame(Bundle result, String name, android.graphics.Bitmap bitmap, long at, long now) {
        if (bitmap == null || now - at > 1000 || at == 0) return;
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 65, bytes);
        if (bytes.size() <= 100000) result.putByteArray(name + "Jpeg", bytes.toByteArray());
    }
    private static void respond(Message request, Bundle result) {
        if (request.replyTo != null) {
            Message reply = Message.obtain(null, request.what); reply.setData(result);
            try { request.replyTo.send(reply); } catch (RemoteException ignored) { }
        }
    }
    @Override public IBinder onBind(Intent intent) { return messenger.getBinder(); }
}
