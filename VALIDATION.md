# Foundation validation

Date: 2026-10-10

- Debug assembly and four JUnit tests: PASS.
- Debug and release Android lint: PASS with warnings for retained target SDK 35, pinned AndroidX version and English UI strings. No lint errors.
- Permanent-signer release builds v0.1.0 (versionCode 100) and v0.1.1 (versionCode 101): PASS. Both APK signatures verify, with certificate SHA-256 cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b.
- Repository signing secrets configured. The key backup and DPAPI-protected password remain outside Git in the ignored, access-restricted .private/signing directory.
- Update checker verifies hash, package, increasing version and installed signer before offering Android's installer. End-to-end phone installation and download tests have not been performed.
- The app is a capability inspector and update foundation. Caller authentication, recording, loop storage, maps, GPS acquisition and Y-Connect telemetry are not implemented yet.
- No phone test yet. Dual encoding, stabilization, recording continuity and heat remain unverified.
- Existing RideDeck apps are unchanged. Their latest Android workflows were successful when inspected: standard bfa631f374f990969684c1816a8ba2e2223d0ebc and Yamaha 2d192c7341b9733e5f63c50149fce73ae7cd073f.
- GitHub workflow and release results must be checked against the actual pushed commit; local build success alone does not establish CI success.
