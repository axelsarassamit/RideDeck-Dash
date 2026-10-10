# RideDeck Dash instructions

- This is a separate app and repository. Preserve application ID com.axelsarassamit.ridedeck.dash and the permanent Dash release signer. Debug builds use the .debug suffix.
- Commit and push completed changes; do not leave completed source changes only locally. Do not force push.
- Run the Dash tests and build before pushing. Check workflows for the exact pushed head and report failures accurately.
- After a push, also inspect the latest Android workflow heads in RideDeck and RideDeck-for-Yamaha. Do not modify those apps without integration scope.
- Only the two authenticated companion apps can own recording sessions. Never trust an intent package-name extra.
- Keep release keystores, passwords, telemetry credentials and provider keys out of Git. Use repository secrets and the preserved private signing backup.
- Releases require numeric vMAJOR.MINOR.PATCH tags, increasing versionCode, the same signing key, ridedeck-dash-release.apk, checksums.txt and source.txt.
- Never claim dual recording, stabilization, speed telemetry or screen-off reliability without physical verification. The current app is a development capability inspector.
- Synced project sources are read-only. Canonical development checkout is C:\Applications\RideDeck-Dash.
