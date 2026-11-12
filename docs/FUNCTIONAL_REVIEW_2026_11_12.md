# Dash functional repair handoff, 12 November 2026

Local repair scope requested by the RideDeck Design chat. No commits, pushes, tags, signing, uploads or physical-phone actions are authorized for this work. Design owns shared styling and will combine the changes atomically. Existing owner changes to root build.gradle.kts, wrapper and daemon JVM criteria are preserved.

## Critical published artifact mismatch

Read-only download of the current GitHub v1.3.8 asset ridedeck-dash-release.apk:

- Actual package: com.axelsarassamit.ridedeck.dash.
- Actual versionName: 1.3.5-dev; versionCode: 10305.
- Actual signer: Android Debug, SHA-256 ce1eaec35ba8ba0a17505953ddb880c7ca87ce4ad63c88d8ded1c263ccb0d2df.
- Required permanent signer: cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b.

The public v1.3.8 release tag therefore does not describe the APK it distributes. The existing updater correctly rejects an APK whose version or signer differs; do not weaken those checks. If the Android-debug-signed production-ID APK has been manually installed, Android cannot replace it in place with the permanent-signer build. Inspect installed provenance during the later authorized phone window. Do not uninstall it or discard recordings to bypass that boundary.

Git history includes 0.13.x, 1.0.0 and 1.3.4/5/6/8. The owner reserved1.x for a future PlayStore release and wants development numbering kept at0.x. Intentional history already includes0.13.4,0.13.5 and0.13.6, so the next local version is0.13.7. Its Android versionCode is101307 using epoch100000 + major*10000 + minor*100 + patch, above actual APK10305 and intended source/tag10308. Future1.0.0 maps to110000, above every0.x version. Build, updater and release validator use the same epoch. DASH_VERSION_CODE can explicitly mirror the derived code; incompatible overrides are rejected rather than silently producing an APK the updater cannot validate.

Legacy updaters use the old code formula and may not offer a lower-displayed0.x version after an installed1.x mistake. A one-time authorized in-place install is therefore required to enter this epoch, and only if the installed signer matches. The existing publicv1.0.0 tag is already occupied by the mistaken Nov11 release; do not overwrite it. Resolving that tag before the true production release is a separate owner decision.

### Stale latest-feed migration

The updater uses immutable GitHub release IDs to distinguish the five already-published mistaken1.x releases from future epoch builds:441221047/v1.0.0,441248249/v1.3.4,441260211/v1.3.5,441272125/v1.3.6,441282256/v1.3.8. These keep legacy codes10000/10304/10305/10306/10308 when evaluating the latest feed, so an installed0.13.7/code101307 will not offer them or download an APK merely to fail identity validation. Their files may have additional manifest mismatches, already noted above; the code numbers here are conservative legacy-tag ceilings, not assertions that all those APKs match their labels.

A future explicitly recreated1.0.0 release with a new release ID uses110000 normally. Missing or mismatched release identity fails closed. No old tag/release was changed. Regression tests cover all five stale feed identities and a new future production identity.

## Prepared repairs

1. Restore debug applicationIdSuffix .debug and versionNameSuffix -dev. Release keeps the original package and signing configuration.
2. Remove the blanket DEBUG companion-certificate bypass. Production host IDs always require the existing pinned host signer. Only a debug Dash can accept exact .preview host IDs, and only when their certificate matches that debug Dash certificate. Canonicalize trusted debug host IDs to the existing lease identities. Dash itself remains .debug. Preview package queries live in the debug manifest only.
3. Preserve the recent four-second preview expiry and 150k JPEG ceiling. These changes still need real-host/phone validation; no image/staleness claim is made from compilation.
4. Hybrid gyro correction again rejects OIS/EIS footage, even when OIS samples were logged. The pipeline records those samples but has no implemented OIS compensation model. Log presence cannot establish gyro-correction eligibility.
5. Retain the larger 31px optical-flow windows/four pyramid levels, but stop discarding all stationary/subpixel features. That discard rejects reliable static scenes and can bias the estimator toward moving objects. Android tests now cover stationary and subpixel distributed road scenes.
6. Correct the studio strength label's default from Gentle to Off, matching actual preview/export behavior. There is one strength control, not a duplicated control. Other owner's stabilization/camera settings text remains.
7. Add scripts/verify_release_apk.py plus four Python regressions and call it from the tag release workflow before upload. It checks exact package, tag-derived versionName/code, non-debuggable status and permanent certificate. No release workflow has been triggered here. The signing-request file is deliberately untouched.

## Build compatibility and validation

Owner AGP9.4.1 + Gradle9.6.0 is a documented compatible pair; Android documentation for AGP9.4 lists Gradle9.6.0 and JDK17 minimum. The generated daemon criteria request JetBrains21 and the existing Foojay resolver supplies it. The baseline configuration build succeeded with this upgrade. Legacy DSL/Kotlin flags produce migration warnings; they were not silently removed.

- Baseline run completed: 78 unit tests, assembleDebug and lintDebug; no lint errors. It took9m55s under competing project builds. Configuration preceded the functional edits, so its APK still identifies as production-ID1.3.8/code10308 and is suitable only as a design baseline in the coordinator's reserved emulator.
- Baseline APK: app/build/outputs/apk/debug/app-debug.apk (will be replaced by final debug build). Baseline metadata also saved outside the repo at C:/Applications/dash-20261111-baseline-apk.txt.
- New Python release-identity tests:4 passed, including the observed mislabeled v1.3.8 metadata and wrong-signer/debuggable/wrong-package cases.
- Final local command, capped at2workers: testDebugUnitTest, assembleDebug, assembleDebugAndroidTest, lintDebug, lintRelease. Result recorded below.
- New Android/OpenCV regressions are compiled for tomorrow's device checks; no Android instrumentation or physical testing was run here.
- Shared design primitives, owner IDE files, unrelated untracked logs and existing media are preserved. The report does not establish outdoor stabilization, frame pacing, live IPC or headset behavior.

## Final local result

Final combined source (including the coordinator's appearance contracts) passed:

- 91 JVM tests, zero failures/errors.
- Four Python release-identity tests, including stale mislabeled APK metadata, wrong signer/debuggable/wrong package, and future1.0.0/code110000.
- assembleDebug and assembleDebugAndroidTest.
- lintDebug: zero errors,105warnings. lintRelease: zero errors,106warnings. Existing composed strings, drawing allocations and dependency/compatibility warnings remain; they were not hidden.
- Final cap2workers run: BUILD SUCCESSFUL in2m20s,101tasks. Log C:/Applications/dash-20261112-migration-final.log.
- aapt verified final debug APK as com.axelsarassamit.ridedeck.dash.debug,0.13.7-dev,code101307,debuggable. File: C:/Applications/RideDeck-Dash/app/build/outputs/apk/debug/app-debug.apk. Test APK: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk.
- Package/version migration tests include all five immutable legacy-release feed IDs, canonical .preview lease ownership, wrong/unknown signer rejection, and future1.0.0 ordering.
- Existing Android/OpenCV regressions plus new stationary/subpixel and OIS-sample-rejection cases compiled only. They require a later authorized device/emulator run. No phone, emulator, live cameras, Bluetooth, export GPU, real-road footage or APK installer was used by this repair chat.
- No release APK signed or produced, no signing request changed, no artifact uploaded or release/tag modified, no commit/push made. HEAD remains575d4bc. This is a local handoff for a combined coordinator review/commit, not a published or phone-verified update.

Functional files prepared: app/build.gradle.kts; debug manifest; CompanionCallerPolicy; DashBridgeProvider; NativeMotionEligibility; HybridEvidence; RoadTracker; VideoStabilizer; SocialStudioActivity default strength; ReleaseVersionPolicy; ReleaseUpdater; associated JVM/Android tests; scripts/verify_release_apk.py and tests; tag-release validation step; Python cache exclusions in .gitignore; this report. Shared styling changes belong to the coordinator and were not edited by this chat. Owner root build.gradle.kts/wrapper/daemon configuration is unchanged from audit entry.


## Coordinator emulator-only follow-up

The RideDeck Design coordinator reported that the completed app/test APKs were pinned with ZIP CRC and SHA-256 snapshots before installation. Pinned app SHA-256:1c956227927104c2a5b229e4fd8d32f75169dd20e40a63f255a278b4a63cd008.

On emulator5580 only, all four RoadTrackerTest synthetic Android/OpenCV tests passed in0.665seconds, including the stationary/subpixel regression. Installation used non-streaming mode after an earlier transient parse failure from a live build-output path. This is coordinator-reported emulator verification, separate from this chat's local build checks. It does not establish physical-phone, road, capture pacing or outdoor stabilization reliability. Other Android instrumentation cases remain unverified unless separately recorded. No extra build or physical-phone action was performed for this documentation update.


## Coordinator physical-device synthetic follow-up

The RideDeck Design coordinator reported installing the pinned Dash.debug app and helper on the authorized physical Xiaomi (YLO7FM8LSS49SO4P). All four RoadTrackerTest synthetic Android/OpenCV tests passed in0.143seconds. The helper was removed afterward.

Evidence supplied by the coordinator: chat-workspace work/audit-2026-11-11/dash-physical-road-tests.txt. This is coordinator-reported physical-device execution of synthetic tracker fixtures. It did not open live cameras, record video, export a video or test outdoor road footage. It does not establish live capture pacing, IPC/headset reliability or real-road stabilization quality.

Functional source is frozen after this documentation-only update. The original-key delivery workflow remains held until the migration helper is ready for the coordinator's atomic source commit. This chat performed no additional build, test, signing, dispatch, commit, push or phone action for this update.


## Coordinator appearance validation follow-up

The coordinator reported that both AppearanceTest methods passed on the physical Xiaomi's isolated Dash.debug app in38.273seconds. Coverage included local appearance overrides/follow-host behavior and20 native screen combinations:5 accents, Light/Dark, portrait/landscape. The test restored its isolated debug preferences and the helper was removed afterward. Evidence: coordinator chat-workspace work/audit-2026-11-11/dash-physical-appearance-tests.txt.

Separate emulator visual attempts failed window-focus assertions while system/systemUI ANR dialogs were foreground, including after the dedicated emulator was rebooted. Those failures were preserved and are not marked passed or attributed to a Dash defect. No app source was changed to conceal them. The physical appearance result does not establish live camera, recording, export, headset or road reliability.

The coordinator also confirmed that no production Dash or Yamaha uninstall has occurred. Original-key Dash delivery remains held for the final standalone migration-helper signing contract. This entry is documentation only; functional source remains frozen.
