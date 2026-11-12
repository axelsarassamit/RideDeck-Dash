# Original-key Dash migration delivery preparation

Prepared locally for the coordinator,12November2026. This document authorizes no signing, dispatch, uninstall or phone migration.

## Signing material status

The expected preserved local paths .private/signing/dash-release.p12, .private/signing/password.dpapi and .private/multisocial/build_signed.ps1 are absent. The GitHub repository still exposes metadata for both original DASH signing secret names. Values were not read, printed or exported. Their presence alone does not prove their contents are correct; the future package certificate check is mandatory.

## Prepared manual workflow

.github/workflows/original-key-delivery.yml is workflow_dispatch only. It cannot run on push or a version tag. The operator supplies source_sha (exact40hex characters) and ci_run_id (positive numeric), after manually checking that exact Android build.

The prerequisite independently reads that GitHub run and requires matching40SHA/runID, completed/success status, the actual .github/workflows/android.yml path and Android build name, trusted owning repository/head repository, main branch and push/manual-dispatch event. Fork/PR/pending/failure/unknown identity is rejected before the signing job. The signing checkout verifies its actual HEAD, main ancestry and unchanged workflow tree relative to current main. The last check avoids GitHub's restriction on creating drafts targeting changed workflow files with GITHUB_TOKEN. Prefer the exact current atomic main commit after successful source CI.

No credentials are persisted by checkout. Existing original DASH_KEYSTORE_BASE64 and DASH_KEYSTORE_PASSWORD secrets are supplied only to the build step. The key is decoded into RUNNER_TEMP with restrictive permissions and removed by both a shell trap and an always cleanup step. No debug key fallback exists.

Build targets: testReleaseUnitTest, lintRelease, assembleRelease and assembleReleaseAndroidTest with -PphoneTestVariant=release and2workers. app/build.gradle.kts already provides the required testBuildType property. No new app or test variant configuration was needed. Version stays0.13.7/code101307.

Before delivery, main APK verification requires exact production package,0.13.7/code101307, no application-debuggable flag and original certificate cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b. The separate instrumentation APK must have production .test package, AndroidJUnitRunner targeting production Dash and the same original certificate. Instrumentation APKs may themselves be debuggable; that is not a relaxation for the main app.

Only the three explicitly named, verified APKs, checksums.txt and source.txt are delivered. Source provenance includes exact commit, source CI run, version/code and migration-candidate status. No TAR, recordings, provider configuration, app-private data, keystore or password is in the delivery directory or upload list.

## Delivery privacy and publication boundaries

Workflow artifacts are not private enough for this public repository: signed-in users with read access can download them. Therefore this workflow has no upload-artifact action. It creates an unpublished draft with a unique non-version delivery-original-key-SHA-RUN-ATTEMPT identifier, draft=true, prerelease=true and make_latest=false. It checks unpublished/draft state before and after explicit asset upload. There is no git tag/push command, no numeric v* release/tag operation, no publish/promotion command and no installer action. The ordinary stable updater ignores drafts.

Draft releases are restricted to users with push access. This is private to authorized repository writers, not encryption against those writers. GitHub may associate the draft with its non-version identifier; it must never be published as a production release. The workflow does not reuse or modify existing numeric tags, including the occupiedv1.0.0 tag.

Official policy references: https://docs.github.com/en/rest/releases/releases (draft visibility and workflow-permission target restriction), https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts (artifact read access).

## Local validation completed

- Seven Python tests pass across release identity and delivery prerequisite/instrumentation identity checks.
- YAML parses as manual-only with exactly two required inputs and a dependency on prerequisite verification.
- All six shell blocks pass bash -n. No artifact upload action is present.
- Read-only validation against actual successful Android CI42967443180 for575d4bcc66a048cbadb27a0d1727f9182a4fa31c passed the exact-run checker. This proves API field handling, not authorization to deliver that old commit.
- Git diff whitespace check passed.
- No new Gradle build, signing, workflow dispatch, draft creation, upload, tag, commit, push, phone action or live private-delivery test occurred.

The original-key APK cannot replace a currently installed debug-signed production-ID APK in place. The coordinator's verified private backup and separate emulator restoration rehearsal remain prerequisites for any later user-approved uninstall/migration. Merely preparing or dispatching this workflow does not authorize that migration.


## Standalone migration helper adaptation, held for coordinator completion

After the main app and separate AndroidJUnitRunner QA APK are built and validated, the workflow hashes the frozen delivery/ridedeck-dash-release.apk. It then invokes the root wrapper with -p tools/migration-helper assembleRelease -PtargetPackage=com.axelsarassamit.ridedeck.dash -PexpectedTargetApkSha256=<computedhash>. This standalone application has no target-source dependency, preventing a target/helper circular hash.

Confirmed unsigned output: tools/migration-helper/build/outputs/apk/release/RideDeckMigrationHelper-release-unsigned.apk. The workflow externally signs it with the decoded original Dash key/alias and environment-supplied passwords into delivery/migration-helper.apk. No password is put in a command-line argument value. It verifies the frozen target hash again afterward and calls the coordinator-owned scripts/verify_migration_delivery.py with --helper-apk, --target-apk and --build-tools.

Required verifier contract: exact application ID com.axelsarassamit.migrationhelper.dash_release, runner com.axelsarassamit.migrationhelper.MigrationInstrumentation, target com.axelsarassamit.ridedeck.dash, original Dash certificate and android:testOnly=true. The separate dash-original-key-tests.apk remains the ordinary app QA runner, not the data-import runner. Provenance records the pinned target hash and migration helper identity. Uploads and checksums explicitly enumerate all three APK names.

This workflow remains HELD: tools/migration-helper has not yet been vendored into Dash, and the coordinator is fixing binary-manifest XML-tree inspection in the QA/helper verifiers. aapt dump badging does not emit instrumentation rows; the old QA verifier regex cannot validate actual APK instrumentation. This chat did not modify that verifier or the helper sources, as the coordinator owns them. The first helper emulator validation crash is under investigation. Neither source inspection nor shell syntax checks establish safe data restoration. Root must finish helper review/rehearsal and supply the verifier before approving the combined source commit or any dispatch.

Validation of this adaptation: final YAML parses, all shell blocks pass bash -n, app-hash calculation precedes the standalone build, original-key external signing follows it, target identity/hash verification precedes the explicit three-APK upload, and fixed version0.13.7/code101307 is retained. No helper build, signing, dispatch, commit, push, installer or phone action was performed for this adaptation.


## Verifier prerequisite update

The Yamaha owner supplied actual XML-tree manifest inspection in scripts/apk_manifest.py, the repaired QA verifier and the standalone migration verifier, with recorded real-tool fixtures. The delivery prerequisite now runs python3 -m unittest discover -s scripts/tests -p 'test_*.py' before the CI gate or signing job. Failure blocks delivery. This does not execute migration instrumentation or authorize signing/phone actions. The coordinator still owns final helper validation and combined rollout approval.

## Release-variant task correction

The first private signing run failed because AGP9.4.1 exposes testReleaseUnitTest, rather than testDebugUnitTest, when phoneTestVariant=release. The signing workflow now selects the release unit-test task alongside release assembly and release instrumentation. The failed run is retained; source CI remains a separate exact-commit prerequisite.
