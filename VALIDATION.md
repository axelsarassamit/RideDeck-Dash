# RideDeck Dash validation

## 0.3.0 changes and bench checks, 2026-10-11

- Real rear/front alignment views rendered before capture and during capture on the Xiaomi. Portrait/landscape rotation retained recording and changed the displayed orientation.
- Four presets and individual clip size, recording quota, free-space reserve, protection window, resolution, bitrate, stabilization and preview refresh settings are implemented. Changes apply to the next session.
- Xiaomi exposed two built-in microphone inputs. Each was selected and the active routed input reported its corresponding device ID. No headset mixing is claimed.
- A selected built-in microphone produced a 720p rear MP4 with H.264 and AAC. All 999 video frames decoded.
- Clear detail preset with custom 64 MB clips recorded simultaneous cameras. Exported rear file was 1920 x 1080 H.264 with AAC; all 1,082 video frames and 1,716 audio frames decoded. Microphone audibility and riding wind suppression were not assessed.
- Finalization retains the busy state until cameras/encoders finish. Audio routing is checked periodically; disconnected selections do not silently substitute another input.
- Bluetooth/USB microphone selection is implemented, but no connected headset route or multi-source mixing has been physically validated. 60 fps, gyro post-processing, voice control and automatic incident detection are not implemented.

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

This is a development dashcam release. No motorbike ride, sustained heat/storage-pressure test, perceptual shake-reduction comparison, crash recovery of damaged MP4s or real Y-Connect speed feed has been validated. There is no burned-in speed overlay or spoken turn-by-turn navigation yet. Public map/routing service availability is not guaranteed. Interrupted MP4s are retained but may be unplayable. Android devices other than the tested Xiaomi are not physically validated.

The stable updater intentionally excludes development prereleases. The permanent app ID, increasing numeric version code and preserved signing key support in-place updates. GitHub build and signed-release run IDs for the final commit are verified after publication.
