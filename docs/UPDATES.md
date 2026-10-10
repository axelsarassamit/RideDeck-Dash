# Signing and updates

Release application ID: com.axelsarassamit.ridedeck.dash. Debug builds use com.axelsarassamit.ridedeck.dash.debug, so a disposable debug key never blocks release updates.

Permanent key alias: ridedeck-dash. Preserve the private backup at C:\Applications\RideDeck-Dash\.private\signing. This directory is ignored and must never be committed. GitHub repository secrets DASH_KEYSTORE_BASE64 and DASH_KEYSTORE_PASSWORD feed the signing workflow. Do not regenerate this key for subsequent releases.

Numeric versions use major * 10000 + minor * 100 + patch. Minor and patch must be under 100. Every update needs a higher versionCode.

The release app's Check for updates button reads only this repository's latest stable release, downloads ridedeck-dash-release.apk and checksums.txt, verifies SHA-256, archive package ID, version and the installed signing certificate, then offers Android's installer. There is no unattended installation. Users may need to allow installs from RideDeck Dash.

Development prereleases are ignored. The 0.1.x tags publish the capability inspector; 0.2.0 introduces the first development recorder. Promote a release to stable only after its intended behavior has been verified; update source.txt status and release notes accordingly. Stable release assets must keep the exact APK and checksum names.

The earlier debug capability APK has the unsuffixed package ID. If it was installed, Android will reject a permanent-signer APK over it. Preserve any useful diagnostics first; remove that development inspector manually before the first signed release install. It contains no dashcam recordings.

Validation requires two signed versions to share the certificate and increase the versionCode. Signed 0.1.3 to 0.2.0 replacement was physically verified on the Xiaomi. Footage and protection markers also survived later signed development-build replacement. See VALIDATION.md for the exact checks.
