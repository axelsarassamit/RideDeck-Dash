# RideDeck Dash

A separate Android dashcam for RideDeck and RideDeck for Yamaha. Package: com.axelsarassamit.ridedeck.dash.

## Ride automation and customizable overlay exports: 0.4.0

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

## New ride controls and overlays

- Concept-inspired large fitted rear view, front-camera inset, telemetry cards and a focused menu. Front-only mode fills the main view.
- Microphone names use Phone microphone 1/2 and the connected headset name, without phone model codes or Android device IDs. Android input endpoints do not necessarily identify individual physical capsules.
- Automatic recording is on by default: one start per authenticated RideDeck session, or GPS at least 8 km/h with accuracy 30 m or better for 3 seconds while Dash is visible. Host auto-opening is configurable and occurs only on explicit visible ride Start/Resume with genuine Dash installed.
- Automatic stopping defaults to charger disconnected AND fresh accurate GPS stationary at 2 km/h or less for 60 seconds. Options: 30, 60, 120 seconds or off. GPS gaps reset the timer. The battery and background authorization gates still apply immediately.
- Manual Stop suppresses automatic restart until a new host session or a fresh stationary-to-moving transition. No boot restart or heartbeat-triggered background camera start.
- Recordings offers Export with overlays: presets plus individual speed, timestamp, recorded distance, offline GPS route trace, altitude, heading, accuracy, battery/charging, branding and camera-name switches. Position, panel size/opacity and km/h/mph are adjustable. GPS samples align to the segment's recorded monotonic start; missing or stale samples show unavailable. Old files without timing cannot invent telemetry.
- Overlay export re-encodes a copy with Android Media3, preserves audio and the clean original, and saves it to Movies/RideDeckDash. Keep the export screen open; leaving or rotating cancels processing. Free-space checks, cancel/error cleanup and pending-gallery publication preserve originals. The route overlay is a GPS trace, not a downloaded street map.

## Power and background rules

Recording requires a connected charger OR battery at least 50%. Unknown battery state blocks recording unless plugged in. The rule is checked before start and continuously. Power recovery alone does not trigger recording.

Standalone recording is permitted while a Dash screen is visible. Background recording requires a live authenticated ride heartbeat from one of the two production RideDeck apps. The 15-second lease expires if renewals stop. Camera capture starts from visible Dash, either manually or through its enabled automatic ride/movement option, after the Android permission flow. No boot or silent background recording.

The provider authenticates Binder UID, exact host package and its verified production signer. It never opens cameras. See docs/COMPANION_PROTOCOL.md. Both hosts implement an explicit ride session and hide Dash-specific controls unless the genuine signed Dash app is installed and enabled. An older host without that integration cannot authorize background recording.

## Honest limits

Yamaha Y-Connect speed is not connected. Existing Yamaha integration launches the official app; no usable live speed API has been established. No simulated bike speed is shown.

Live views now let you align both cameras. Phone/headset audio mixing, voice controls, gyro post-processing and automatic incident protection are not included. Long-duration heat, road shake reduction, vehicle telemetry and screen-off capture need separate physical verification. See VALIDATION.md for completed checks and limits.

## Updates and build

The stable application ID and permanent signing key are preserved. Numeric version tags produce signed development prereleases with APK, checksums and source provenance. Stable in-app updates verify package, version, signer and SHA-256. Prereleases are intentionally excluded from the stable updater until promoted. See docs/UPDATES.md.

Use JDK 17 and Android SDK 36. Run gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug on Windows. Release signing uses DASH_KEYSTORE_PATH and DASH_KEYSTORE_PASSWORD. Never commit signing files. Debug builds use the .debug package suffix.
