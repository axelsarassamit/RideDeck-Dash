# RideDeck Dash

A separate Android dashcam for RideDeck and RideDeck for Yamaha. Package: com.axelsarassamit.ridedeck.dash.

## First recording version: 0.2.0

- Front and rear Camera2 recording, with an explicit single-camera choice when a phone cannot run both.
- Separate 720p H.264 MP4 files at a requested 30 fps, without microphone audio.
- Highest advertised electronic stabilization requested first. Camera results are checked after warm-up; if the strongest electronic mode is ignored, Dash tries basic electronic stabilization and then optical stabilization. Applied settings and segment events are saved in capture.csv. Advertisement alone is not proof of shake reduction.
- Approximately one-minute segments using MediaRecorder file-size rollover. Duration depends on actual encoder bitrate. No recorder reset is needed for a normal rollover.
- Protect clip keeps the current, previous minute and next minute of footage from loop recycling. A 2 GB loop budget recycles only finalized unprotected files. Interrupted files are retained as unverified. Recording stops if protected footage or disk space prevents another allocation.
- Saved clips can be played, shared and, on Android 10 or newer, copied to the gallery. Ride CSV files can be exported to Downloads. Clip protection can be changed; permanent deletion requires an explicit confirmation. Internal footage survives app updates, but clearing app data or uninstalling deletes it.
- Timestamped GPS speed, accuracy and coordinates are saved separately in route.csv. The dashboard labels stale/missing speed unavailable. Speed is not burned into the video in this version.
- Online OpenStreetMap map, recorded ride trace and tapped-destination motorcycle route planning through public Valhalla. Coordinates are sent for routing only after confirmation. This is route planning, not turn-by-turn spoken navigation; public services have no guaranteed availability. Leaflet 1.9.4 is bundled with its license.
- Portrait and landscape dashboard. The foreground service retains camera ownership through dashboard rotation.

## Power and background rules

Recording requires a connected charger OR battery at least 50%. Unknown battery state blocks recording unless plugged in. The rule is checked before start and continuously. No automatic restart occurs when power recovers.

Standalone recording is permitted while a Dash screen is visible. Background recording requires a live authenticated ride heartbeat from one of the two production RideDeck apps. The 15-second lease expires if renewals stop. Camera capture starts only through the visible Dash action and Android permission flow. No boot or silent background recording.

The provider authenticates Binder UID, exact host package and its verified production signer. It never opens cameras. See docs/COMPANION_PROTOCOL.md. Both host chats are coordinating their integration independently; an installed host without that integration cannot authorize background recording.

## Honest limits

Yamaha Y-Connect speed is not connected. Existing Yamaha integration launches the official app; no usable live speed API has been established. No simulated bike speed is shown.

No live camera preview or video speed overlay is included yet. Review a short saved clip to check positioning before a ride. Long-duration heat, road shake reduction, vehicle telemetry and screen-off capture need separate physical verification. See VALIDATION.md for completed checks and limits.

## Updates and build

The stable application ID and permanent signing key are preserved. Numeric version tags produce signed development prereleases with APK, checksums and source provenance. Stable in-app updates verify package, version, signer and SHA-256. Prereleases are intentionally excluded from the stable updater until promoted. See docs/UPDATES.md.

Use JDK 17 and Android SDK 36. Run gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug on Windows. Release signing uses DASH_KEYSTORE_PATH and DASH_KEYSTORE_PASSWORD. Never commit signing files. Debug builds use the .debug package suffix.
