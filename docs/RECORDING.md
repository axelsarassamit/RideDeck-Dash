# Recording and stored data

Start recording from the visible dashboard after granting Camera. Precise Location adds live GPS speed and route points; camera recording still works if location is declined, with GPS shown unavailable. Notification permission makes the persistent Stop and Protect actions visible. Microphone audio is not requested.

The dashboard remains awake while it is visible and recording. This is a window flag, not a change to the phone's screen-timeout settings. Manual screen off is supported only while a signed RideDeck host keeps its explicit ride session active. The recorder service is non-sticky and never restarts capture after process death, boot, regained power or a restored Activity.

## Stabilization

Probe the camera's advertised modes, request the strongest electronic mode, and inspect camera capture results after warm-up. If that mode is ignored, try basic electronic stabilization at about 90 frames, then optical at about 180 frames where supported. Electronic and optical modes are not blindly combined. Positive applied modes after 240 frames are cached privately for the same Android build fingerprint, camera ID, resolution/frame-rate configuration and camera count. An OS build change or switching between dual and single capture uses a separate key. Actual status is shown on the dashboard; capture.csv contains the observed modes. This proves selected camera controls, not perceptual road shake reduction.

## Files and protection

Internal directory: files/recordings/<session UTC milliseconds>/.
Video names: rear-0001.mp4 and front-0001.mp4, with increasing segment numbers. Files are independent camera streams, not a composited picture-in-picture video. Video orientation is fixed at recording start; rotating the dashboard does not restart cameras or change an existing segment's orientation.

A .ready marker means MediaRecorder completed the file. A .protected marker excludes it from automatic recycling. Marker files persist across app replacement and process death. Missing .ready means active or interrupted, so automatic recycling and normal playback/export do not treat it as finalized. Interrupted video is retained; no MP4 repair is promised.

Protect marks current, already-queued next files, recent finalized footage and newly allocated files for the following minute. It does not protect the whole future ride. Loop storage has a 2 GB budget and at least 256 MB of free-space reserve before allocations; currently open/queued files mean the budget is approximate. Only finalized unprotected videos may be recycled. Protected or unverified files can fill the budget and stop recording. Export needed footage, then explicitly permit recycling or delete a clip with confirmation. Exports are copies, not moves.

## Timestamp formats

route.csv columns: utc_ms, elapsed_ns, latitude, longitude, accuracy_m, speed_m_s. Each row comes from a real Android GPS fix. Missing speed is empty, never synthesized. Dashboard readings older than six seconds are unavailable. Coordinates remain inside the app until an explicit export or route request.

capture.csv columns: utc_ms, elapsed_ns, sensor_ns, camera, event, eis, ois, file. UTC and elapsed clocks refer to callback/event reception. sensor_ns is the hardware camera timestamp where available, kept separately because its time source is device-dependent. Sampled frame results record applied stabilization; file_started/file_finalized events index rollovers. Camera files start and finish independently; this is not frame-perfect synchronized stereo capture.

Saved clips offer gallery video copy, Downloads CSV copy, sharing and historical map review. Android 10 or newer supports the direct gallery/Downloads export. Older supported Android versions use the video share flow. Clearing app data or uninstalling removes internal footage; copies exported to shared storage remain.

## Online map

Leaflet 1.9.4 is bundled locally with its BSD license. Only the local map page and OpenStreetMap tile requests are loaded by the map WebView; it cannot navigate to arbitrary remote script pages or read local files. Attribution stays visible.

Blue shows real GPS samples. A destination tap can request a motorcycle route from current GPS using public Valhalla after confirmation. Orange shows that plan. Historical views do not silently reuse old coordinates as a live start position. Online provider outages are reported. This release does not provide spoken navigation, rerouting or a burned-in video speed overlay.
