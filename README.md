# RideDeck Dash

A separate Android dashcam for RideDeck and RideDeck for Yamaha. Package: com.axelsarassamit.ridedeck.dash.

## Live-view and configurable recording: 0.3.0

- Front and rear Camera2 recording, with an explicit single-camera choice when a phone cannot run both.
- Separate 720p or 1080p H.264 MP4 files at a requested 30 fps, with adjustable 4 to 24 Mbps video bitrate. Unsupported configurations fail explicitly.
- Live rear/front alignment views before and during recording. Tap a view to enlarge it. Preview is 640 x 360 at a selectable 2, 5 or 10 fps; saved video retains the selected recording resolution. Views fit without cropping.
- Balanced, Clear detail, Long ride and High quality presets, plus custom settings.
- Optional AAC microphone audio in rear video, or front video for front-only capture. Choose an actual Android phone, wired, USB or headset input; selected routing is checked. Phone plus headset mixing is not implemented.
- Highest advertised electronic stabilization requested first. Camera results are checked after warm-up; if the strongest electronic mode is ignored, Dash tries basic electronic stabilization and then optical stabilization. Applied settings and segment events are saved in capture.csv. Advertisement alone is not proof of shake reduction.
- Clip-size choices of 64, 128, 256 and 512 MB, or 1 and 2 GB per camera, using MediaRecorder file-size rollover. Duration depends on actual bitrate. No recorder reset is needed for normal rollover.
- Protect clip keeps current/queued clips and footage within a selectable 15-second to 5-minute window before and after the press. A selectable 2 to 128 GB recording budget recycles only finalized unprotected files. Interrupted files are retained as unverified. Recording stops if protected footage or disk space prevents another allocation.
- Saved clips can be played, shared and, on Android 10 or newer, copied to the gallery. Ride CSV files can be exported to Downloads. Clip protection can be changed; permanent deletion requires an explicit confirmation. Internal footage survives app updates, but clearing app data or uninstalling deletes it.
- Timestamped GPS speed, accuracy and coordinates are saved separately in route.csv. The dashboard labels stale/missing speed unavailable. Speed is not burned into the video in this version.
- Online OpenStreetMap map, recorded ride trace and tapped-destination motorcycle route planning through public Valhalla. Coordinates are sent for routing only after confirmation. This is route planning, not turn-by-turn spoken navigation; public services have no guaranteed availability. Leaflet 1.9.4 is bundled with its license.
- Portrait and landscape dashboard. The foreground service retains camera ownership through dashboard rotation.

## Power and background rules

Recording requires a connected charger OR battery at least 50%. Unknown battery state blocks recording unless plugged in. The rule is checked before start and continuously. No automatic restart occurs when power recovers.

Standalone recording is permitted while a Dash screen is visible. Background recording requires a live authenticated ride heartbeat from one of the two production RideDeck apps. The 15-second lease expires if renewals stop. Camera capture starts only through the visible Dash action and Android permission flow. No boot or silent background recording.

The provider authenticates Binder UID, exact host package and its verified production signer. It never opens cameras. See docs/COMPANION_PROTOCOL.md. Both hosts implement an explicit ride session and hide Dash-specific controls unless the genuine signed Dash app is installed and enabled. An older host without that integration cannot authorize background recording.

## Honest limits

Yamaha Y-Connect speed is not connected. Existing Yamaha integration launches the official app; no usable live speed API has been established. No simulated bike speed is shown.

Live views now let you align both cameras. No burned-in video speed overlay, phone/headset audio mixing, voice controls, gyro post-processing or automatic incident protection is included yet. Long-duration heat, road shake reduction, vehicle telemetry and screen-off capture need separate physical verification. See VALIDATION.md for completed checks and limits.

## Updates and build

The stable application ID and permanent signing key are preserved. Numeric version tags produce signed development prereleases with APK, checksums and source provenance. Stable in-app updates verify package, version, signer and SHA-256. Prereleases are intentionally excluded from the stable updater until promoted. See docs/UPDATES.md.

Use JDK 17 and Android SDK 36. Run gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug on Windows. Release signing uses DASH_KEYSTORE_PATH and DASH_KEYSTORE_PASSWORD. Never commit signing files. Debug builds use the .debug package suffix.
