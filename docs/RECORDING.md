# Recording and stored data

Start recording from the visible dashboard after granting Camera. Precise Location adds live GPS speed and route points; camera recording still works if location is declined, with GPS shown unavailable. Notification permission makes the persistent Stop and Protect actions visible. Microphone permission is requested only when an audio input is selected. Audio off remains available. Bluetooth selection requires Nearby devices permission and Android 12 or newer communication routing. Verified audio input reporting requires Android 9 or newer.

The dashboard remains awake while it is visible and recording. This is a window flag, not a change to the phone's screen-timeout settings. Manual screen off is supported only while a signed RideDeck host keeps its explicit ride session active. The recorder service is non-sticky and never restarts capture after process death, boot, regained power or a restored Activity.

## Stabilization

Probe the camera's advertised modes, request the strongest electronic mode, and inspect camera capture results after warm-up. If that mode is ignored, try basic electronic stabilization at about 90 frames, then optical at about 180 frames where supported. Electronic and optical modes are not blindly combined. The stabilization setting can also explicitly disable both modes. Positive applied modes after 240 frames are cached privately for the same Android build fingerprint, camera ID, resolution/frame-rate configuration and camera count. An OS build change or switching between dual and single capture uses a separate key. Actual status is shown on the dashboard; capture.csv contains the observed modes. This proves selected camera controls, not perceptual road shake reduction.

## Files and protection

Internal directory: files/recordings/<session UTC milliseconds>/.
Video names: rear-0001.mp4 and front-0001.mp4, with increasing segment numbers. Files are independent camera streams, not a composited picture-in-picture video. Video orientation is fixed at recording start; rotating the dashboard does not restart cameras or change an existing segment's orientation.

A .ready marker means MediaRecorder completed the file. A .protected marker excludes it from automatic recycling. Marker files persist across app replacement and process death. Missing .ready means active or interrupted, so automatic recycling and normal playback/export do not treat it as finalized. Interrupted video is retained; no MP4 repair is promised.

Protect marks current, already-queued next files, recent finalized footage and newly allocated files for the selected protection window (15, 30, 60, 120 or 300 seconds). It does not protect the whole future ride. Recording storage is selectable from 2 to 128 GB, with a 256 MB to 4 GB phone free-space reserve. Active or interrupted files reserve at least one selected clip size each when assessing allocation capacity. Protected clips count toward the budget. The limit applies to recordings, not the APK size or exported copies. Only finalized unprotected videos may be recycled. Protected or unverified files can fill the budget and stop recording. Export needed footage, then explicitly permit recycling or delete a clip with confirmation. Exports are copies, not moves.

## Timestamp formats

route.csv columns: utc_ms, elapsed_ns, latitude, longitude, accuracy_m, speed_m_s, altitude_m, bearing_deg, battery_percent, charging. Each row comes from a real Android GPS fix. Missing speed is empty, never synthesized. Dashboard readings older than six seconds are unavailable. Coordinates remain inside the app until an explicit export or route request.

capture.csv columns: utc_ms, elapsed_ns, sensor_ns, camera, event, eis, ois, file. UTC and elapsed clocks refer to callback/event reception. sensor_ns is the hardware camera timestamp where available, kept separately because its time source is device-dependent. Sampled frame results record applied stabilization; file_started/file_finalized events index rollovers. Camera files start and finish independently; this is not frame-perfect synchronized stereo capture.

Saved clips offer gallery video copy, Downloads CSV copy, sharing and historical map review. Android 10 or newer supports the direct gallery/Downloads export. Older supported Android versions use the video share flow. Clearing app data or uninstalling removes internal footage; copies exported to shared storage remain.

## Online map

Leaflet 1.9.4 is bundled locally with its BSD license. Only the local map page and OpenStreetMap tile requests are loaded by the map WebView; it cannot navigate to arbitrary remote script pages or read local files. Attribution stays visible.

Blue shows real GPS samples. A destination tap can request a motorcycle route from current GPS using public Valhalla after confirmation. Orange shows that plan. Historical views do not silently reuse old coordinates as a live start position. Online provider outages are reported. This release does not provide spoken navigation, rerouting ; optional overlay export is available for saved clips.

## Presets, live view and audio

Balanced: 720p, 6 Mbps, 128 MB clips, 8 GB recording storage. Clear detail: 1080p, 12 Mbps, 256 MB clips, 16 GB. Long ride: 720p, 4 Mbps, 64 MB clips, 4 GB. High quality: 1080p, 16 Mbps, 512 MB clips, 32 GB. All target 30 fps. Individual changes mark the configuration Custom and apply to the next capture. Higher dual-camera modes are not guaranteed on every phone.

The alignment view opens cameras only while Dash is visible and power is permitted, then releases them before recording begins. Recording uses an additional 640 x 360 YUV stream per camera. Display conversion is throttled to the selected preview refresh rate and skipped while Dash is hidden. A fitted image is rotated for current screen orientation without cropping or mirroring, so both edges of the camera field remain visible. Preview is not a full-resolution video quality inspection. Recording surfaces and camera ownership survive dashboard rotation.

Audio is captured by one recorder to avoid competing microphone recordings: rear when present, front in front-only mode. AAC is embedded in that video's MP4. Input choices come from actual Android input devices; internal virtual/telephony inputs are filtered out. A selected input must still exist and accept preferred routing. Actual input is checked after start and repeatedly while recording. A route mismatch stops recording instead of silently substituting a microphone. Automatic input permits Android's selected source. Physical microphone capsules may be grouped by Android; the app cannot promise separate access where the OS does not expose it. No simultaneous phone/headset mixer is implemented. Bluetooth and USB routes require connected hardware and separate physical validation.

Finalization keeps Start disabled until existing recorders and service shutdown have completed. Saved clips preserve unverified output if recorder finalization fails.

## Drive automation and overlay copies

See README for the configurable start/stop thresholds and original-preserving overlay export. A clip now records its initial file_started event as well as rollover starts, allowing each segment to align GPS by elapsed clock rather than filename or last-modified time. Altitude, heading and battery fields are optional; older route CSVs remain readable. Route distance is an estimate from accurate continuous fixes with gap, jitter and implausible-speed filtering, not an odometer.
