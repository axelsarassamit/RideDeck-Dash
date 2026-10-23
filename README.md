# RideDeck Dash

A separate Android dashcam for RideDeck and RideDeck for Yamaha. Package: com.axelsarassamit.ridedeck.dash.

## Current editor and stabilization: 0.13.0

The Gallery-style editor has a proportion-correct preview, center-playhead timeline, true splitting, merge/batch export, front-camera PiP, overlays, saved drafts and multi-select draft deletion. Projects support up to 30 clips and two hours in total. Editing and export work on battery; recording retains its five-minute charger grace and 30% unplugged minimum.

Road adaptive stabilization smooths the whole selected trajectory with a fixed crop and conservative recovery of weak road matches. Classic image and Hybrid lab remain selectable. Live capture stays lightweight. Night blur and severe rolling-shutter distortion remain limitations. See [release notes](docs/RELEASE_NOTES.md) and [validation](docs/ROAD_ADAPTIVE_0.13.0.md).

## Earlier social timeline editor: 0.10.1

One editor replaces the old single-clip and overlay-export screens. Current Canva artwork appears in overlays as well as the app. Playback and timeline seeking use the updated media engine, with a brief fresh-compositor refresh for PiP jumps and cuts. It provides a large preview, thumbnail timeline, draggable trims, split/duplicate/reorder/remove, crop and social formats, movable front-camera PiP, configurable ride overlays, timed title text, music/volume/fades, colour adjustments and integrated experimental stabilization. Undo/redo and app-private saved drafts preserve non-destructive edits. Export selected recordings separately or merge up to 30 sections and 30 minutes. Preview and export share the recipe. Recording thumbnails, persistent sorting and camera/protection filters make the library easier to browse.

Originals stay untouched. Drafts reference source media and are not backups. Editing and export work on battery while retaining capture priority. The existing recording policy is unchanged. A date/time-filtered local backup still copies complete originals and ride CSVs with a manifest.

Rear distant focus no longer silently falls back to continuous autofocus when manual locking is unavailable. Capture requests fixed 1x zoom and disables autoframing where supported. Capture logs now sample applied focus, focal length, zoom, exposure/frame timing and thermal status alongside cadence diagnostics. Rear-priority dual capture and native preview throttling remain in place.

Export stabilization has a corrected rigid-pose calculation and filtered luma analysis, but remains experimental. Road vibration, blur and dropped-frame performance are not established by local UI tests. New editor sessions default to stabilization off. Longer ride footage will be reviewed separately; gyro correction, deblurring and rolling-shutter reconstruction are not implemented.

See [the long ride checklist](docs/LONG_RIDE_CHECK.md) and [editor choices](docs/SOCIAL_PRESETS.md).

## Road footage stabilization: 0.7.0

Overlay and social exports now offer Off, Gentle, Strong and Maximum image stabilization. Gentle uses 12% zoom, Strong 22%, and Maximum 35%: smoothing trades picture edges for steadier footage. Analysis runs locally on clean originals, tracks road features with OpenCV, rejects unreliable intervals, and bounds translation and rotation inside the crop. Logo and telemetry overlays are added afterward. Originals and audio remain preserved. Gyro correction and rolling-shutter reconstruction are not implemented, and already blurred details cannot be recovered.

Analysis and export require Dash to stay visible, recording stopped, and charger or at least 50% battery. Cancel, leaving or rotation abort unfinished export. Analysis is bounded to 30 minutes / 60,000 frames; use a shorter social range for longer clips. Unsupported decoder output or insufficient tracking features produce an explicit error rather than a falsely stabilized copy. OpenCV is packaged in the app, increasing download size; no video is uploaded for analysis.

Capture requests the most stable supported range ending at 30 fps and supplies stabilization parameters at session creation. Older learned mode choices are rechecked under the new session configuration. Preview conversion runs on a separate thread. Rear-lens selection shows only cameras exposed by Android, with advertised optical/electronic stabilization; preview and recording share the selected lens. Hidden physical camera IDs are diagnostics only. Dual mode still requires an advertised concurrent pair, and a lens unsupported in dual mode is rejected. A better lens must be confirmed on the phone, not inferred from advertising alone.

## Ride automation and customizable overlay exports: 0.4.1

- Front and rear Camera2 recording, with an explicit single-camera choice when a phone cannot run both.
- Separate 720p or 1080p H.264 MP4 files at a requested 30 fps, with adjustable 4 to 24 Mbps video bitrate. Unsupported configurations fail explicitly.
- Live rear/front alignment views before and during recording. After Camera permission has been granted, the view opens automatically while Dash is visible and power is allowed; it can be closed explicitly. Tap a view to enlarge it. Preview is 640 x 360 at a selectable 2, 5 or 10 fps; saved video retains the selected recording resolution. Views fit without cropping.
- Balanced, Clear detail, Long ride and High quality presets, plus custom settings.
- Optional AAC microphone audio in rear video, or front video for front-only capture. Choose an actual Android phone, wired, USB or headset input; selected routing is checked. Phone plus headset mixing is not implemented.
- Highest advertised electronic stabilization requested first. Camera results are checked after warm-up; if the strongest electronic mode is ignored, Dash tries basic electronic stabilization and then optical stabilization. Applied settings and segment events are saved in capture.csv. Advertisement alone is not proof of shake reduction.
- Clip-size choices of 64, 128, 256 and 512 MB, or 1 and 2 GB per camera, using MediaRecorder file-size rollover. Duration depends on actual bitrate. No recorder reset is needed for normal rollover.
- Protect clip keeps current/queued clips and footage within a selectable 15-second to 5-minute window before and after the press. A selectable 2 to 128 GB recording budget recycles only finalized unprotected files. Interrupted files are retained as unverified. Recording stops if protected footage or disk space prevents another allocation.
- Saved clips can be played, shared and, on Android 10 or newer, copied to the gallery. Ride CSV files can be exported to Downloads. Clip protection can be changed; permanent deletion requires an explicit confirmation. Internal footage survives app updates, but clearing app data or uninstalling deletes it.
- Timestamped GPS speed, accuracy and coordinates are saved separately in route.csv. The dashboard labels stale/missing speed unavailable. Optional speed and other recorded data can be burned into an exported copy; the original remains clean.
- Online OpenStreetMap map, recorded ride trace and tapped-destination motorcycle route planning through public Valhalla. Coordinates are sent for routing only after confirmation. This is route planning, not turn-by-turn spoken navigation; public services have no guaranteed availability. Leaflet 1.9.4 is bundled with its license.
- Portrait and landscape dashboard. The foreground service retains camera ownership through dashboard rotation.

## Clip selection and deletion: 0.4.2

In Recordings, tap Select clips or long-press a clip. Choose individual videos, Select all or Clear selection. The toolbar shows selected count, combined size and protected count. Delete selected requires confirmation that names the count/size and explicitly includes protected clips. Capture must be stopped. Successful removals clear their ready/protected markers; failed clips stay selected for retry. Ride CSVs and exported gallery copies are kept. Selection survives screen rotation.

## New ride controls and overlays

- Concept-inspired large fitted rear view, front-camera inset, telemetry cards and a focused menu. Front-only mode fills the main view.
- Microphone names use Phone microphone 1/2 and the connected headset name, without phone model codes or Android device IDs. Android input endpoints do not necessarily identify individual physical capsules.
- Automatic recording is on by default: one start per authenticated RideDeck session, or GPS at least 8 km/h with accuracy 30 m or better for 3 seconds while Dash is visible. Host auto-opening is configurable and occurs only on explicit visible ride Start/Resume with genuine Dash installed.
- Recording stops five minutes after the charger disconnects, regardless of movement or GPS availability. Reconnecting resets the shared allowance. Below 30% or unknown battery stops unplugged capture immediately. This replaces the old unplugged-and-stationary stop preference.
- Manual Stop suppresses automatic restart until a new host session or a fresh stationary-to-moving transition. No boot restart or heartbeat-triggered background camera start.
- Recordings offers Export with overlays: presets plus individual speed, timestamp, recorded distance, offline GPS route trace, altitude, heading, accuracy, battery/charging, branding and camera-name switches. Position, panel size/opacity and km/h/mph are adjustable. GPS samples align to the segment's recorded monotonic start; missing or stale samples show unavailable. Old files without timing cannot invent telemetry.
- Overlay export re-encodes a copy with Android Media3, preserves audio and the clean original, and saves it to Movies/RideDeckDash. Keep the export screen open; leaving or rotating cancels processing. Free-space checks, cancel/error cleanup and pending-gallery publication preserve originals. The route overlay is a GPS trace, not a downloaded street map.

## Power and background rules

Recording allows up to five minutes without a charger while battery is at least 30%. Unknown battery blocks unplugged recording. The shared monotonic timer survives recording and process restarts within the same boot; first opening without a charger starts the allowance, including after a reboot. A disconnect that happens while Dash is not running is measured from its next observation. The rule is checked before start and every 500 milliseconds during capture. Power recovery alone does not trigger recording. Editing, export, backup and sound checks retain their charger-or-50% reserve.

Standalone recording is permitted while a Dash screen is visible. Background recording requires a live authenticated ride heartbeat from one of the two production RideDeck apps. The 15-second lease expires if renewals stop. Camera capture starts from visible Dash, either manually or through its enabled automatic ride/movement option, after the Android permission flow. No boot or silent background recording.

The provider authenticates Binder UID, exact host package and its verified production signer. It never opens cameras. See docs/COMPANION_PROTOCOL.md. Both hosts implement an explicit ride session and hide Dash-specific controls unless the genuine signed Dash app is installed and enabled. An older host without that integration cannot authorize background recording.

## Honest limits

Yamaha Y-Connect speed is not connected. Existing Yamaha integration launches the official app; no usable live speed API has been established. No simulated bike speed is shown.

Live views now let you align both cameras. Phone/headset audio mixing, voice controls, gyro post-processing and automatic incident protection are not included. Long-duration heat, road shake reduction, vehicle telemetry and screen-off capture need separate physical verification. See VALIDATION.md for completed checks and limits.

## Updates and build

The stable application ID and permanent signing key are preserved. Numeric version tags produce signed development prereleases with APK, checksums and source provenance. Stable in-app updates verify package, version, signer and SHA-256. Prereleases are intentionally excluded from the stable updater until promoted. See docs/UPDATES.md.

Use JDK 17 and Android SDK 36. Run gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug on Windows. Release signing uses DASH_KEYSTORE_PATH and DASH_KEYSTORE_PASSWORD. Never commit signing files. Debug builds use the .debug package suffix.

## Camera-first dashboard

The dashboard uses one fixed screen in portrait and landscape, with the entire camera frame visible and a front-camera inset. Recording status, speed, GPS and stabilization appear as compact live-view overlays. Record/Stop and Protect clip are the only main actions. The grouped Menu contains recordings, map and route, recording information, presets, cameras, microphones, automation, overlays and updates. The information window groups camera, storage, audio and power details.

## Social video presets and saved settings

Menu > Social video presets prepares TikTok, Instagram and Facebook exports. Purpose presets include Quick highlight, Ride story, Route tour and Clean full clip. Custom settings cover format, quality, bitrate, trim start, duration, full-picture fit or center crop, audio or mute and data overlays. Selected clips include a framing preview. Exports are separate gallery copies; Share opens Android's chooser.

Menu > My saved settings provides three named slots for the complete recording, audio, camera, storage, automation, overlay and social configuration. Loading is confirmed and blocked while recording/finalizing. Capture changes apply to the next recording. Slots are stored separately.

See [social preset choices and limits](docs/SOCIAL_PRESETS.md).
## Sound routing checks and export low-cut, 0.7.1

Menu > Sound check records eight-second local phone, Bluetooth voice or concurrent samples. It checks actual routes, Android silencing and signal, preserves the original WAVs and offers 50 Hz and 120 Hz phone comparisons. Saving copies to Downloads is explicit. These are foreground diagnostics, not production live video mixing.

Microphones and audio now provides export presets Original, Natural ride (50 Hz), Wind rumble reduction (120 Hz) and Voice focus (180 Hz), plus custom low-cut choices. The filter runs on exported video copies only. Stronger settings remove more engine bass. Filtering cannot recover clipped audio or remove every kind of wind noise. Original is the default. Phone/headset live mixing remains pending synchronization, capture-service integration and outdoor validation.

## Bluetooth video routing and rear capture cadence, 0.7.2

Video recording now establishes MODE_IN_COMMUNICATION and waits for the selected Bluetooth voice transport and matching microphone input before preparing MediaRecorder. Automatic input prefers an available Bluetooth voice headset; explicit Connected Bluetooth headset works with any exposed classic SCO or BLE headset and requires a specific choice when multiple voice devices are connected. Selection uses Android device capabilities and identity, never brand names. Wired and USB inputs remain selectable. Route checks, Android silencing checks and signal peaks continue during recording; microphone diagnostics are saved with ride data. Playback-only Bluetooth devices cannot supply microphone audio. Calls or other voice-mode owners block headset startup rather than taking their route.

Dual camera workload defaults to Rear priority: the selected rear resolution and bitrate remain, while the front uses 720p and at most 6 Mbps. Same quality on both cameras remains available. Hidden live previews are removed from the repeating camera request until visible/requested again, without restarting the recorder. Actual sensor cadence is shown in recording information and summarized every five seconds in capture.csv. Capture timing remains device and load dependent; this does not reconstruct frames missing from existing recordings.

## Capture priority and direct live preview, 0.7.4

Recording camera requests now include the recorder surface on every frame and the preview surface only at the selected 2, 5 or 10 fps display cadence. Hidden previews use recorder-only requests. This avoids producing and discarding 30 preview images per second while video still targets 30 fps. Display conversion uses reusable NV21 and pixel arrays, one directly rotated bitmap, and four preview buffers. It no longer compresses and decodes JPEGs for local display. The full frame remains visible; companion preview sharing remains supported.

Recording information groups both camera lines under Cameras and includes the current recording status. Recorder faults remain visible over the idle preview, and stop reasons are written to capture.csv and the local diagnostic log. Rear priority applies the front bitrate cap at either recording resolution. No synthesized or duplicated frames are inserted to conceal missing capture.
