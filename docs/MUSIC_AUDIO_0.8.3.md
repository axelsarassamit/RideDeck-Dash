# Music audio integration test 0.8.3

Automatic camera recording pins audio to a built-in phone microphone instead of opening a Bluetooth call connection. This keeps Dash from moving Spotify onto a quiet, narrowband headset voice route. No music volume or global Bluetooth setting is changed.

On the first launch after this update, existing Bluetooth recording selections move to automatic phone microphone. Disabled audio and explicit wired, USB and phone inputs are preserved. The migration runs once. Users can subsequently choose Bluetooth recording deliberately after a warning about reduced music quality.

The Yamaha 0.12.37 pause/resume voice handoff remains supported. Yamaha finalizes the current clip before requesting voice input and resumes only the same active ride when its screen returns. This creates a brief recording gap during that voice action.

Acceptance requires the migration/routing unit tests, build/lint checks and a connected-phone recording test. Actual Spotify quality and volume through the motorcycle headset need a listening test with that headset. This is a signed integration build, not a claim of completed bike-data authentication or telemetry support.
