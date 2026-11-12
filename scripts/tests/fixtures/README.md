# Android manifest tool fixtures

Captured with Android SDK build-tools 36.0.0 aapt on 12 November 2026.

- dash-release-tests files come from the actual local Dash release Android-test APK.
- migration-helper files come from root's actual built Yamaha preview migration helper, including its target APK hash metadata.

The fixtures retain real badging and xmltree formatting. Badging contains no instrumentation declaration. Positive Dash migration policy tests substitute only the helper and target package identities in the captured Yamaha manifest. Certificate checks use the public expected certificate digest as a separate unit fixture. These tests do not claim an original-key helper was signed or executed.
