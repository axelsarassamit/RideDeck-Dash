# RideDeck Dash validation

## 0.9.0 social studio and front inset, 2026-10-21

- Fifty-nine unit tests, debug and signed release builds, and both lint variants passed. The original-signer replacement installed on the authorized Xiaomi 14T Pro, Android 16, preserving user recordings and settings.
- Four on-phone tests passed using generated footage: merged order/duration and audio presence, front-camera gaps and rollover matching, native portrait front shape with upright colored edges and rounded transparency, two independently saved batch copies, and studio draft recreation plus rendered portrait/landscape previews. Generated test recordings and exported copies were removed by the tests.
- Live front preview uses center crop to fill its existing rounded inset without letterboxing. Exported PiP retains native front aspect ratio within a small corner box. Rear fill is optional. The composition preview and export share framing and timing logic.
- Tests establish synthetic short-video behavior, not outdoor stability, long-export heat behavior, audio waveform identity or frame-accurate dual-camera synchronization. No user ride footage was analyzed for this update. The existing experimental single-clip stabilization remains separate.

## 0.8.4 charger grace, 2026-10-15

- Recording uses a shared persisted monotonic five-minute unplugged allowance with a 30% minimum, independent of GPS. Reconnection resets the allowance. Standalone, signed companion starts and the continuous recording gate use the same policy. The old stationary stop control is replaced with an explanation of the fixed grace rule.
- Forty-seven unit tests and debug build/lint passed. New coverage checks the exact expiry, 29%/30% boundary, reconnect/reset, restored timer and background authorization. Heavy-work reserve remains charging or 50% battery.
- Real five-minute cable removal and low-battery cutoff are not claimed physically verified. Battery and GPS were not spoofed for acceptance. Source diagnostics and the app's short phone smoke check do not establish ride performance.

## 0.8.1 finishing checks, 2026-10-14

- Hardened editor trim/export settings, draft rotation state, decoder release and capture-priority interruption. A new editor session defaults to stabilization off. Its optional stabilization preview/export remains experimental; no ride footage was analyzed in this finishing task.
- Hardened time-window backup cancellation and continuous charger-or-50-percent checks, and retained the selected date/time/camera fields across rotation. Updated distant-focus settings text to match explicit rejection of unsupported manual locking.
- Forty unit checks passed, as did debug build/lint and original-signer release build/lint. Signed replacement installation of 0.8.1 preserved the app and its existing recordings on Xiaomi 14T Pro (Android 16).
- A new approximately 45-second stationary dual-camera recording finalized. Rear reported AF mode 0 and the user's saved 0.2-dioptre focus; front reported fixed focus. Nine optics samples per camera reported zoom 1.0 and thermal status 0. These are short bench samples, not outdoor focus/heat/frame-drop guarantees.
- The playable editor rendered its fitted preview and controls in portrait and landscape. Draft 5–10-second boundaries survived rotation, then applied and exported without a second settings prompt. The separate copy has H.264 1080x1920 video, AAC 48 kHz mono audio and approximately 5.005 seconds duration. Only container metadata was read; no frame-motion, shake or blur analysis was run.
- A backup of the new rear recording and its ride logs completed with a manifest; the copied original byte size matched it. Date/time/camera selection survived rotation. One attempted cancellation completed before Home reached the app, so interruption during an active copy is not claimed as physically verified. Original clips remain in the library. Test trim settings were returned to their prior 0–40-second values.
- Long ride timing, vibration, focus breathing under road conditions and stabilization quality remain deferred to the user's next ride. Gyro correction, deblurring and rolling-shutter reconstruction remain unimplemented.

## 0.8.0 editor and backup candidate, 2026-10-14

- Added a non-destructive single-clip editor with playable preview, trim controls, framing/audio/overlay settings, optional stabilization preview and separate-copy export. Added explicit complete-original and ride-log backup to Downloads.
- The local 0.8.0 signed APK passed signature verification with the permanent Dash certificate. `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleRelease` and `:app:lintRelease` passed; all 38 unit tests passed.
- Ride footage was not opened or analyzed for this build. Stabilization quality is not claimed. Landscape editor, applied nonzero trim, interruption/rotation and backup behavior still require phone verification; the phone was left available for the user's ride.

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

The first phone export decoded all 761 video frames and retained the original display rotation. Inspection caught a locale-specific month label and road lookup missing a valid GPS sample just before clip start; 0.6.1 corrects both. Original clips remained in place.

## Outdoor footage review and stabilization, 0.7.0

The user-provided rear overlay recording and an additional exported phone clip were inspected locally. The first-minute sample was approximately 30 fps with no near-identical consecutive frames in the sample; a second clip also had steady timestamps. The outdoor ride's capture log confirmed rear electronic mode 0 / optical mode 1, while front reported electronic mode 2. Hardware mode advertisement alone was therefore not evidence of effective rear electronic stabilization.

An original-signer development build completed a full-length Strong stabilized export from the original outdoor rear clip on Xiaomi 14T Pro. The saved H.264 copy retained portrait framing, audio and newly drawn fixed logo/telemetry overlays. A local feature-tracking comparison of a 40-second section measured roughly 56% lower 95th-percentile horizontal displacement, 71% lower vertical displacement and 52% lower angular displacement. This is a sample-specific image-motion comparison, not a general road-safety or future-ride guarantee. Strong uses 22% zoom and loses edges; existing vibration blur remains.

Twenty-eight unit tests passed, including alternating shake with slow pan, separation at tracking breaks, and crop limits with large motion in portrait. Debug and original-signer release builds and lint passed. The new session-parameter/FPS path recorded and finalized both cameras on the connected phone. These indoor capture checks do not establish improved outdoor native stabilization, thermal endurance or rolling-shutter correction. Follow-up outdoor footage is required for those claims. Private recordings, GPS logs, camera images and comparison scripts are excluded from Git.

The phone exposes one logical rear camera and one front camera, with concurrent group {0,1}. Rear metadata advertises OIS and both electronic stabilization modes, with underlying physical IDs that are not independently enumerated as openable cameras. Lens choices are limited to Android's exposed camera list; diagnostic physical IDs do not imply usable standalone or concurrent recording support.

## Local sound checks, 0.7.1

On 12 October 2026, the original-signer development update was installed on the Xiaomi 14T Pro without clearing data. Individual phone and MA390 Wireless Bluetooth checks passed. The eight-second concurrent probe retained distinct active inputs: phone device 18 at 48 kHz stereo and headset device 11868 at 16 kHz mono. All monitored route checks matched, neither track was marked silenced, both had non-silent signal and neither clipped. Bluetooth voice noise suppression was enabled. These IDs are session-specific, not hard-coded.

The combined report and WAVs were explicitly saved locally to Downloads. This establishes only short diagnostic capture support, not long-duration reliability, perceived outdoor sound improvement or synchronized live video mixing. A 15-second 1080 x 1920 social copy completed using the 120 Hz export filter and decoded completely with audio. Original recordings were preserved. The export sound setting was restored to Original, communication mode returned to normal and the temporary screen-awake setting was restored.

Thirty-three debug unit checks passed, including five low-cut checks for speech-band preservation, stronger rumble attenuation, channel isolation, bypass and bounded full-scale behavior. Debug/release builds and lint passed. Wind and engine comparisons outdoors remain required before choosing a default filter or claiming superior natural sound. No microphone mixing option is exposed in production capture yet.

## Rear skipping and Bluetooth video verification, 0.7.2

The user identified outdoor rear-0002 and rear-0003 from the same 12 October ride. Local packet timing confirmed 57 gaps over 50 ms in rear-0002 (29.31 fps average, longest 133 ms), and 102 in original rear-0003 (28.50 fps, longest 267 ms). The exported rear-0003 retained all 4,939 source frames and the capture gaps. Its initial displayed frame also spans the source's initial video offset. No missing source frames were repaired or originals overwritten. The existing capture log only sampled startup and rollover, so it cannot retrospectively distinguish late camera delivery from encoder loss during the ride.

The original-signer 0.7.2 update was installed preserving data. With rear 1080p and front 720p, a first 62-second indoor phone-microphone recording had two roughly 290 ms startup gaps during stabilization mode discovery, then steady cadence. After stabilization was learned, a 52-second MA390 Wireless video and 58-second GEARELEC GX12 video both had approximately 30.02 fps and zero saved video intervals over 50 ms. Both cameras finalized; logs showed rear OIS and front EIS. Each headset was independently connected, selected by the automatic capability-based route, confirmed as the active recording input throughout its test, and produced non-silent 16 kHz AAC in the saved rear MP4. Local decoding confirmed audio signal in both files. This is not a claim that every headset has been physically tested, that speech quality was subjectively validated, or that outdoor/thermal skipping is solved.

Thirty-three unit checks, debug/release builds and lint passed. The phone's audio mode returned to MODE_NORMAL and temporary stay-awake was restored to 0. No recordings were deleted, unrelated settings changed, or private footage/telemetry committed. Further outdoor footage with this workload split is required to establish sustained rear capture improvement. Live phone-plus-headset mixing remains unimplemented.

## Sustained capture checks and preview scheduling, 0.7.4

The 0.7.2 rear-priority build completed approximately 6.6 minutes of simultaneous rear 1080p/front 720p recording with GEARELEC audio. All five saved camera clips decoded, but the first rear segment retained three 67 ms intervals and the first front segment one 100 ms interval. A direct-conversion 0.7.3 development build reduced the remaining rear issue to one 67 ms interval in roughly 7.8 minutes. Neither intermediate test was treated as proof that frame skipping was solved.

The final original-signer 0.7.4 build additionally controls preview cadence at the camera using repeating requests that always target the recorder. In an approximately 7 minute 45 second test, both cameras recorded through two rear rollovers and one front rollover with GEARELEC as the active audio input. All 184 five-second sensor timing summaries reported zero gaps. Three saved rear segments contained 5,066, 5,062 and 3,816 frames, each approximately 30.020 fps. Every successive presentation interval inside each rear file was at most 33.311 ms, with zero intervals over 50 ms. All 13,944 rear frames and their audio decoded successfully. The manual stop reason was saved. Clip presentation times restart at rollover; these per-file checks are not an independent synchronized measurement across file boundaries.

Thirty-eight unit checks passed, including NV21 colour order, opaque black/white, all orthogonal rotations, rectangular portrait mapping and partial-input rejection. Debug and original-signer release builds and lint passed. Portrait live preview and the corrected information grouping were inspected on the phone. Android reported no thermal throttling during the checks. Temporary stay-awake was restored to 0, audio mode to normal, and captures were stopped. Existing user recordings were preserved; private media and diagnostics remain excluded from Git.

One earlier short development recording ended without a retained stop reason; its cause was not established. Stop-reason logging and visible recorder faults now make a recurrence diagnosable. No guarantee is made for every headset, future ride, temperature, resolution or system load. Both MA390 and GEARELEC were physically verified in actual video recording; BLE, wired and USB paths remain capability-based but were not all physically tested. Outdoor footage with 0.7.4 remains required to verify sustained riding performance, exposure, vibration and thermal conditions. Phone-plus-headset live mixing remains unimplemented.

