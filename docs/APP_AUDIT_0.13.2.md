# Dash app audit, 24 October 2026

Audit baseline: 0.13.1, source `8afb623d43f90f8fa3c2fc87c19f1a4345a9a8fe`. Work started after the Yamaha audit handed back the phone. Scope includes capture lifecycle, permissions, background authorization, power policy, audio routing and saved settings, storage/deletion, editor and drafts, preview/export, maps, updater and build configuration. This is a source review with targeted automated device checks, not a guarantee that every device or road condition is error-free.

## Corrected findings

| Finding | Evidence and correction |
| --- | --- |
| Rejected foreground recording request could crash the process | Xiaomi Android 16 regression reproduced `ForegroundServiceDidNotStartInTimeException` after the app rejected START. Rejection now briefly acknowledges the foreground request and immediately removes its notification and stops. Android 14+ uses only the declared shortService type for this shutdown, without opening cameras or bypassing background authorization. |
| Stale Protect action kept an idle capture service alive | Isolated device regression reproduced an idle service and capture worker. Idle Protect/Stop now shut down; active Protect still runs on the capture worker. |
| Preset restore changed an explicitly selected Bluetooth microphone | Device regression reproduced headset becoming auto after migration. Presets now preserve the current migration marker, ignore legacy snapshots of that internal marker, and retain Boolean user preferences. |
| Rotation during camera/microphone permission flow lost Record intent | Device recreation regression reproduced lost pendingRecord. Pending intent, permission-request state, mode and preview choice now survive recreation. Auto-preview cannot consume a pending permission/record request. |
| Invalid GPS values could pass movement thresholds | Movement policy now rejects non-finite and negative speed/accuracy. Unit regression covers NaN, infinities and negative values and verifies the dwell timer resets. |
| Long ride map repeatedly parsed route data on the main thread and stopped at 50,000 rows | Route parsing/JSON construction now run on one worker, cache unchanged files, and create a bounded display snapshot. Downsampling retains the first and last valid points, including beyond the old cap. Unit check uses 60,000 valid points plus invalid rows. Original route files are unchanged. |
| Live alignment preview inherited the recording battery gate | Preview now stays available independently of charger/grace/battery. Record remains subject to the five-minute unplugged allowance and 30% floor. |
| Dashboard action overrides defeated shared sizing/icons | Existing orientation regression exposed Setup height below its expected minimum. Header/footer and capture actions now have adequate fixed height and their established action glyphs; Protect is explicitly labelled Protect clip. |

The Android build workflow still tests, builds and lints every push. Debug APK artifact upload is now opt-in on manual dispatch, avoiding automatic artifact uploads during the owner's upload deferral. Signed release workflow and stable updater rules are unchanged. No release tag is created by this audit.

## Verification

Final results and installation provenance are recorded below after device validation. Baseline unit suite had 73 tests; the updated suite has 75, covering invalid GPS and bounded long-ride map snapshots. Baseline lint had no errors and 107 warnings, mainly hardcoded/composed UI strings. Exported bridge components and packaged-map JavaScript warnings were reviewed in context rather than disabled blindly.

Device regressions use the separate .debug app and its private preferences/files. Synthetic editor fixtures have local sound and offset front/rear timing. Production source recordings are neither deleted nor used as disposable fixtures. Synthetic fixtures and test-generated gallery copies are cleaned up by the existing test harness.

## Reviewed boundaries and remaining work

- Companion provider/service verify the caller UID, exact host package and pinned host certificate, plus session ownership and token expiry. A caller-supplied package string grants no access. Ongoing capture still checks background authorization. Positive real host/headset/call/road behavior is not inferred from a negative authorization test.
- Recording rotation keeps the service-owned recorder. Originals and finalized/protected flags remain separate. The audit does not change native EIS/OIS, camera frame scheduling, encoder settings or the Road adaptive trajectory engine.
- Editor checks cover actual seeking and playback, split preserving both halves, trim/order, merged duration/audio, PiP synchronization across rollovers, proportion-correct preview, drafts and deletion, thumbnails/sorting, timed text/music/overlays, cancellation and encoded output. These are synthetic decoder/GPU/encoder checks, not outdoor stabilization quality measurements.
- Advanced stabilization still cannot reconstruct missing exposures, vibration blur or severe rolling-shutter distortion. Native-stabilized video cannot be treated as raw gyro footage. The earlier Road adaptive road comparison has mixed night results; no universal smoothness claim is made here.
- Generic Bluetooth routing exists; simultaneously mixing phone ambience with a Bluetooth voice mic is not a released feature. Not every headset or communication-profile behavior has been physically checked. Explicit Bluetooth input can reduce music quality; automatic phone-mic mode remains the music-safe default.
- **Requirement gap:** Dash speed telemetry is currently GPS based. The companion protocol does not ingest Y-Connect speed. Adding that needs a coordinated host telemetry protocol and physical validation; this audit does not claim that link exists.
- Diagnostic motion/capture sidecars are intentionally retained when loop video is removed. They count against storage and can accumulate on long use. A future explicit, previewable cleanup tool should offer safe removal after originals/evidence are no longer needed. No private logs are automatically purged in this audit.
- Map route planning still requires explicit location-sharing confirmation and a working public routing service. Live network/motorcycle route suitability was not tested with private coordinates here.
- Updates retain the package ID, numeric version progression, permanent signer, checksum checks and stable-release filtering. Upload/promotion remains deferred. Local signed installation does not mean the in-app updater can download this version yet.
- A sustained outdoor ride, thermal/storage pressure, physical cable disconnect/reconnect, headset/call handoff and subjective audio quality remain separate checks. Automated tests do not establish zero frame drops or best-possible stabilization.

## Final validation ledger

- Updated unit suite: 75 passed. Debug tests/build/lint and signed release build/lint completed successfully. Lint has no errors; remaining warnings are documented above.
- Final connected-device suite: 37 cases completed, 35 passed and two opt-in battery cases skipped. Those two were then explicitly run and both passed with simulated unplugged 12% battery. Android battery reporting was reset afterward and verified back on real USB power.
- Audit regressions passed: rejected foreground start, idle Protect teardown, saved microphone choice, Boolean/legacy preset handling, pending permission intent/mode through recreation, and unsigned bridge caller rejection. Bridge rejection was tested against the isolated debug provider; production host authorization was source-reviewed.
- Dashboard actions passed the existing portrait/landscape sizing/icon test. Social editor, image/gyro export and tracker checks passed using synthetic media. The synthetic output checks preserve frame/audio content and do not establish outdoor quality.
- One additional short rear-camera recording passed the live microphone/camera check in the isolated app. An independent local decode found H.264 video (69 pictures) and AAC audio (109 decoded audio frames, nonzero sample peak). The device remained in normal audio mode. This is a short indoor smoke check, not a sustained road/thermal assessment.
- Signed 0.13.2 installed with replacement over production 0.13.1. Package/version verified as com.axelsarassamit.ridedeck.dash, versionCode 1302. Existing production preferences and recordings were preserved.
- Permanent signer SHA-256: cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b.
- Installed APK SHA-256: ef88c1016746d2c677417afec7a31d9311d94f6f9f69c49dc3e2b86066c88bdb.
- Debug audit app/helper are removed after checks. Private audit logs and the short test recording remain outside Git under .private/audit-2026-10-24.
- Exact pushed Android build is checked separately against the final commit. No tag, APK artifact upload or release promotion is requested.
