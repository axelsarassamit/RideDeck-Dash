# RideDeck Dash

Initial Android source foundation, not a working dashcam. Dedicated repository: https://github.com/axelsarassamit/RideDeck-Dash. Canonical local checkout: C:\Applications\RideDeck-Dash.

First target: Xiaomi 14T Pro. Recording continues for the active RideDeck ride session, including screen off and navigation use. Android/HyperOS version still needs device verification.

## Agreed product behavior

- Separate companion for RideDeck and RideDeck for Yamaha.
- Standalone recording while Dash is visible; background recording only with an active, authenticated RideDeck or RideDeck for Yamaha ride session.
- Portrait and landscape supported. Rotation must not interrupt future capture.
- Operations require a connected charger OR at least 50% battery. Unknown battery state blocks operation unless plugged in. Stop capture and finalize its current segment only when unplugged and below 50%, or when backgrounded without an authorized host session. No automatic restart.
- Record rear and front cameras simultaneously where device sessions support it. Explicitly show any single-camera fallback.
- Prefer the strongest stable, supported stabilization for each tested camera/resolution/frame-rate combination. Do not blindly combine optical and electronic modes.
- Dashcam loop recording with bounded storage, protected clips, timestamps and recovery after interruption. Protected clips must never be automatically overwritten.
- Record GPS speed, position and actual traveled route. Display route guidance in RideDeck and synchronize the planned route to footage review in Dash.
- Yamaha speed integration only through a verified, supported telemetry source. Keep GPS and bike speed separately labeled, preserve timestamps and mark stale readings unavailable.
- Large recording-status and protect-clip controls in both host apps. Persistent Android recording notification with Stop.

## Foundation implemented

An Android capability inspection screen reports camera directions, advertised stabilization modes and concurrent camera groups. It does not request camera access or record. RideSessionPolicy models an exclusive companion lease with a 15-second expiry. This policy must sit behind real Android caller authentication; it is not a security boundary by itself.

The signed release app includes a user-initiated update checker with SHA-256, package, version and signer verification. Debug builds have a separate package ID and do not offer release updates. Android builds/tests run on pushes and pull requests. Numeric version tags trigger signed development releases with checksums and source provenance. See docs/UPDATES.md for the permanent signing and stable release policy.

## Integration design to implement

1. The rider starts recording from visible Dash or through a RideDeck launch into Dash. Obtain camera/location permissions and check the power gate there before starting the camera/location foreground service. Standalone recording ends when Dash is backgrounded without an authenticated companion session. No silent background camera startup or boot recording.
2. Authenticate Binder callers using calling UID, installed package identity and approved signing certificates. Allow only the two package IDs in RideSessionPolicy. Verify actual production signing identities before enabling the integration. Reject caller-supplied package names and arbitrary intents.
3. Host ride service sends a heartbeat every 5 seconds using monotonic time. Dash stops and finalizes footage on ride end, Binder death or lease expiry, and checks expiry independently of incoming messages. Activity pause and screen off do not end the ride.
4. Probe and test dual encoder sessions on the Xiaomi. Validate stabilization, orientation, dropped frames, encoder limits, screen-off behavior, storage pressure and heat in sustained operation.
5. Add rolling segments and an indexed metadata timeline, clip protection, storage quotas, export and interrupted-file handling. Compare independent camera files against composited footage on-device before choosing the recording pipeline.
6. Add timestamped GPS telemetry and route synchronization. Current local Yamaha code launches Y-Connect; a usable speed-data interface has not been established. Do not fabricate a connection or display demo bike speed as live.
7. Integrate controls into both existing apps and verify both builds when changes are pushed. Signed release and physical recording tests are required before any supported-device claim.

## References

- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/reference/android/hardware/camera2/CameraManager#getConcurrentCameraIds()
- https://developer.android.com/reference/androidx/camera/video/VideoCapabilities

## Validation status

No phone test yet. Android build validation is recorded separately in VALIDATION.md. No integrations or existing RideDeck app files were changed.

## Build

Use JDK 17 and Android SDK 36. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` (Windows: `gradlew.bat`). Release signing uses DASH_KEYSTORE_PATH and DASH_KEYSTORE_PASSWORD. Set DASH_VERSION_NAME to a numeric version when building an update. Never commit signing files.
