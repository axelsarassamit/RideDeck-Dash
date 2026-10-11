# RideDeck Dash validation

## 0.3.0 and 0.3.1 changes and bench checks, 2026-10-11

- Real rear/front alignment views rendered before capture and during capture on the Xiaomi. Portrait/landscape rotation retained recording and changed the displayed orientation.
- Four presets and individual clip size, recording quota, free-space reserve, protection window, resolution, bitrate, stabilization and preview refresh settings are implemented. Changes apply to the next session.
- Xiaomi exposed two built-in microphone inputs. Each was selected and the active routed input reported its corresponding device ID. No headset mixing is claimed.
- A selected built-in microphone produced a 720p rear MP4 with H.264 and AAC. All 999 video frames decoded.
- Clear detail preset with custom 64 MB clips recorded simultaneous cameras. Exported rear file was 1920 x 1080 H.264 with AAC; all 1,082 video frames and 1,716 audio frames decoded. Microphone audibility and riding wind suppression were not assessed.
- Finalization retains the busy state until cameras/encoders finish. Audio routing is checked periodically; disconnected selections do not silently substitute another input.
- Bluetooth/USB microphone selection is implemented, but no connected headset route or multi-source mixing has been physically validated. 60 fps, gyro post-processing, voice control and automatic incident detection are not implemented.

- Published 0.3.0 CI APK installed preserving existing clips. A roughly 90-second dual 1080p session with the second built-in microphone crossed two file rollovers per camera. Three segments per camera finalized, with expected protected markers and native playback. Both host session and original saved clips survived updates.
- Standard RideDeck 0.12.11 signed artifact was installed after explicit CLI download authorization. Dash controls disappeared while Dash was temporarily disabled; ordinary Resume/Pause worked. Restoring Dash's original default enabled state and resuming the host returned Dash controls and authenticated its provider lease. Yamaha 0.12.27 independently passed the same visibility/restoration and ordinary ride tests in its chat. Neither app was uninstalled and no app data was cleared.
- 0.3.1 candidate automatically opened both live alignment views after existing Camera permission, while visible and powered. Starting dual 1080p recording released the idle preview first and succeeded with the correct microphone. Idle dashboard displayed selected resolution/input instead of stale capture status.

The following evidence records the previous 0.2.0 bench checks and remains relevant to unchanged gating, map and storage marker behavior.

## Build checks

2026-10-10: 10 unit tests passed. Debug and release lint passed. The signed release APK assembled successfully with the permanent Dash signing certificate:
cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b.

Tests cover charger OR 50 percent boundaries, unknown battery, foreground/background authorization, lease ownership/expiry, numeric release versions, and preservation of protected or interrupted files during loop recycling.

A stale idle Gradle process held an old classes.dex open on Windows. Its lock ownership and completed Dash build were verified before stopping that process. A clean build then passed. No signing material or footage is tracked in Git.

## Xiaomi 14T Pro physical checks

Device: 2407FPN8EG, Android 16 / API 36, HyperOS 3. USB authorized. Signed replacement installation succeeded over Dash 0.1.3, preserving the application ID and existing app data. Later 0.2.0 development builds also installed in place.

- Both camera IDs 0 and 1 opened together and recorded separate H.264 1280 x 720 files.
- First short exports decoded fully: rear 1,422 frames / 47.335 seconds; front 1,438 frames / 47.895 seconds, approximately 30 fps. Maximum adjacent frame interval in that short sample: rear 0.043 seconds, front 0.033 seconds.
- Front capture results reported EIS mode 2. Rear EIS modes 2 and 1 were ignored in the tested dual configuration, both reporting 0 after warm-up. The fallback to rear OIS mode 1 was confirmed by capture results at frame 240. The dashboard now labels applied stabilization rather than requested settings.
- Portrait to landscape and back retained capture. Landscape controls were moved into a second column after the first UI test, then physically checked with Start, Protect and Stop visible.
- Protect clip markers survived stopping and signed app replacement. Protected current/next files stayed marked; later files beyond the protection window were unprotected.
- Both cameras rolled into segment 2 and then segment 3 during a session of roughly 131 seconds. MediaRecorder used next-output-file handover, without restarting the cameras for normal rollover. Segment events and monotonic timestamps are exported in capture.csv.
- The exported second segments each decoded fully with 1,698 frames at approximately 30 fps, with maximum adjacent frame interval 0.033 seconds in both tracks.
- GPS speed was shown as GPS, with timestamped coordinates, accuracy and speed saved in route.csv. A 26-sample short export contained valid UTC/monotonic timestamps and 26 speed readings. Stationary indoor GPS can drift; this is not a speed-accuracy road test.
- Saved historical trace and OpenStreetMap tiles rendered on the phone with attribution. The public Valhalla endpoint returned a valid motorcycle route for an independent Bangkok test request. Actual turn-by-turn riding was not tested or implemented.
- Gallery video export and Downloads CSV export succeeded. Footage and actual coordinates are kept only in private test files, never committed or published.
- Standalone recording stopped after screen off without a companion lease and did not restart on returning to Dash.
- Standard RideDeck 0.12.10 was installed using its verified original signing identity. Resume authenticated a live background lease. Dual capture continued with the phone screen off for approximately 55 seconds, exceeding the 15-second lease duration and crossing a video rollover. After host Pause and keeping Dash backgrounded, recording stopped and files finalized.
- The shell caller was rejected by the exported bridge with SecurityException. A caller-supplied package name cannot authorize recording.
- Physical recording succeeded while USB-powered at 36 percent battery, confirming the charger alternative below 50 percent. The unplugged low-battery and unknown-battery boundaries are unit-tested; no phone battery readings were spoofed.

## Coordinated host builds

Standard integration head: c4999cb758acf9aeb7e95798a92cb37a72d37460. Android run 38052170721 passed.
Yamaha integration head: 58478d978e8856d4e16e7e685d8dc1c915761dcd. Android run 38068085546 passed. Signed Yamaha 0.12.26 installed. Its explicit ride session authenticated Dash, and dual recording continued with the screen off for over a minute. End ride cleared the session. A subsequent Dash restart reused rear optical and front electronic stabilization. Final Yamaha menu styling is being verified in its own chat.

## Limits

This is a development dashcam release. No motorbike ride, sustained heat/storage-pressure test, perceptual shake-reduction comparison, crash recovery of damaged MP4s or real Y-Connect speed feed has been validated. Optional overlay exports are now available; spoken turn-by-turn navigation is not implemented. Public map/routing service availability is not guaranteed. Interrupted MP4s are retained but may be unplayable. Android devices other than the tested Xiaomi are not physically validated.

The stable updater intentionally excludes development prereleases. The permanent app ID, increasing numeric version code and preserved signing key support in-place updates. GitHub build and signed-release run IDs for the final commit are verified after publication.


## Ride automation and overlay update, 2026-10-11

Dash 0.4.1 code head: 7cecb601268debe782725f7099c8c0d975e80f9e. Exact Android run 38109622868 and signed-release run 38109624518 passed. All 16 unit tests and debug/release lint passed locally. Published APK SHA-256: 980fbf59ec162930c6d41b83d5eec9677978d1cc983f1e9689ca6240398f695a. Permanent signer matched; installed by replacement and previous protected footage remained visible.

Phone testing began only after implementation and local signed checks finished. An initial finished-build check found landscape telemetry below the viewport; 0.4.1 corrected this and the final phone check showed the full fitted camera view, inset and all three telemetry cards together. Recording continued through portrait/landscape switches. Microphone choices displayed Phone microphone 1/2 and Bluetooth headset with its product name, without Xiaomi model codes or Android device IDs.

Original-signed standard RideDeck 0.12.12 (edd1d3aaab5f4637e0ae55d28e7cefcaeed5953b, Android 38109188321, signing 38109201286) and Yamaha 0.12.28 (eabb54c3bb97b0ea7a72e1a954841f8dba5ff0be, Android 38109194271, signing 38109205271) were installed by replacement. Standard Resume and Yamaha Start ride each authenticated the lease, opened Dashboard and started dual 1080p recording automatically. Manual Stop remained stopped in the current standard session. Standard Pause and Yamaha End released their sessions. The MA390 Wireless actual input route was verified during Yamaha recording and the final published-APK standalone check.

Temporarily disabling Dash hid Dash actions in both hosts and kept their ordinary ride controls available. Dash was restored to its default enabled state. No app was uninstalled or cleared.

All-data overlay exports succeeded. A longer initial copy decoded all 1,625 H.264 video frames and 2,540 AAC audio frames. Frame inspection showed unavailable fields before GPS arrival, then real speed, timestamp, accuracy, altitude, heading, battery and the offline GPS trace. The final published 0.4.1 headset recording exported to a 1920x1080 H.264/AAC copy and decoded all 432 video frames and 677 audio frames. Display rotation metadata was retained. Originals stayed in the clip library. Private footage and location data were not committed or published.

The charger-disconnected plus sixty-seconds-stationary policy passed unit checks, including stale GPS, poor accuracy, movement and reconnection resets. The user explicitly skipped the physical unplug check. Standalone GPS movement auto-start has not been road-tested. No claims are made about wind audio quality, long ride heat, shake-reduction effectiveness or Y-Connect telemetry. Final recording stopped, both test rides inactive, headset selection retained and idle camera view closed.

## Road focus, 0.5.5

Rear defaults to locked infinity focus where manual lens control is advertised. Front defaults to supported continuous autofocus. Each camera can instead select approximately 5 metres (0.2 dioptres) or autofocus in Video and storage. Alignment preview and recording share the policy; fixed-focus lenses retain their focus and unsupported manual control falls back to supported autofocus. Settings are included in saved user slots. Actual focus mode and lens distance are sampled in DashFocus device logs. A real road clarity comparison remains unverified.

The first road-focus smoke test confirmed rear AF off with 0.0 dioptres on Xiaomi 14T Pro; front reported fixed focus. It also exposed MP4 finalization hanging after capture requests were aborted before MediaRecorder.stop. Version 0.5.6 keeps frames flowing until recorder finalization, then closes the camera session. The interrupted test files are retained without marking them verified.

## Overlay redesign, 0.6.0

Exported copies always carry the existing RideDeck road mark, including logo-only social presets. Original MP4s remain untouched. Layout uses blue outlined dark cards, separate speed/source, optional telemetry grid and a moving bike map. Portrait and landscape adapt independently, with social safe margins. Marker motion interpolates recorded positions and shortest-angle heading only across accurate, plausible GPS intervals up to six seconds. Missing or inaccurate GPS never produces an invented bike position.

Optional road context uses one bounded OpenStreetMap vector query near the clip start through Overpass, with a seven-day local cache, response limit and timeout. No OSM raster tiles are downloaded for export. It is a limited context map, not turn-by-turn navigation or a full offline road database; longer clips can leave its coverage. Public Overpass is a development dependency and a configured/self-hosted service will be needed for larger deployment. Offline or failed road loading retains a labelled GPS trace. Requests send the approximate ride area, explained in overlay settings.

Animated style preview uses explicitly illustrative telemetry and roads, with no demo data exported into recordings. Real outdoor route motion remains pending outdoor footage at the user request.
