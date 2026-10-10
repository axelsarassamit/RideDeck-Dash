# Xiaomi 14T Pro development inspection

Device reported model 2407FPN8EG, Android 16 (API 36), HyperOS OS3.0. USB power connected, battery 89% when first inspected.

The GitHub-signed v0.1.2 development APK was downloaded, its checksum matched, and its signing certificate matched the permanent Dash signer. Initial debug installation was rejected by Xiaomi. After the user unlocked the phone, signed release installation succeeded and the capability activity launched successfully.

Visible capability report: rear camera 0 and front camera 1; concurrent camera group {0,1}. Both report electronic video stabilization modes [0,1,2]. Rear optical modes [0,1]; front optical mode [0]. These are advertised capabilities, not a completed recording test.

Physical screenshot inspection found content overlapping the status bar under Android's edge-to-edge layout. The next source change adds system-bar and display-cutout insets for portrait and landscape. Both orientations, sustained capture, power-loss finalization and actual update installation still need testing. No camera capture or recording has occurred.

The v0.1.2 release source commit is ae23f9eb7b975308df5f4edf78410a02147f7712. Its Android and signed-release workflows passed. The documentation-only head 37970d86ec0319bd08e944e8778bc0ed11bbd5aa also passed Android CI.
