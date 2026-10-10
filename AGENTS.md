# RideDeck Dash instructions

- This is a separate app and repository. Preserve application ID com.axelsarassamit.ridedeck.dash and the permanent Dash release signer. Debug builds use the .debug suffix.
- Commit and push completed changes; do not leave completed source changes only locally. Do not force push.
- Run the Dash tests and build before pushing. ALWAYS check axelsarassamit/RideDeck-Dash Android build for the exact pushed head, wait until it finishes, inspect failed logs and repair failures. A pending or older successful run never verifies a new commit.
- After a push, also inspect the latest Android workflow heads in RideDeck and RideDeck-for-Yamaha. Do not modify those apps without integration scope.
- Standalone recording is allowed while Dash is visible. Background recording requires an active session from one of the two authenticated companion apps. Never trust an intent package-name extra.
- Support portrait and landscape. Never lock orientation. Preserve an active recording across rotation once the recorder exists.
- Dash operations require a connected charger OR at least 50% battery. Unknown battery state is blocked unless a charger is connected. Apply the gate at start and continuously; stop and finalize capture if unplugged AND below 50%, or on unauthorized backgrounding. Unplugging alone is allowed at 50% or higher. No automatic recording restart when power returns.
- Keep release keystores, passwords, telemetry credentials and provider keys out of Git. Use repository secrets and the preserved private signing backup.
- Releases require numeric vMAJOR.MINOR.PATCH tags, increasing versionCode, the same signing key, ridedeck-dash-release.apk, checksums.txt and source.txt.
- Never claim dual recording, stabilization, speed telemetry or screen-off reliability without physical verification. The current app is a development capability inspector.
- Whenever the user's phone is connected and authorized, install and smoke-test changes as part of validation. The user explicitly authorizes this. Verify device identity and install with replacement to preserve data. Never uninstall an existing app or modify unrelated phone settings merely to bypass an installation/test failure. Report device prompts or blocked installation accurately. Test only implemented behavior; do not claim that unimplemented recording works.
- Synced project sources are read-only. Canonical development checkout is C:\Applications\RideDeck-Dash.
