# RideDeck Dash companion protocol v1

Authority: `content://com.axelsarassamit.ridedeck.dash.bridge`.
Call with `ContentResolver.call(uri, method, null, extras)`.
Every call requires `extras.putInt("protocolVersion", 1)`.

Methods: `renew`, `stop`, `status`. Renew returns `accepted`; all return
`protocolVersion` and `rideActive`. Renew every 5 seconds during an actual
active ride session. Lease expiry is 15 seconds using elapsed realtime.
The current owner is exclusive while its lease is active. End the session
with `stop`; expiry is the fallback for a killed/disconnected host process.

Only package `com.axelsarassamit.gx12` or `com.axelsarassamit.gx12.yamaha`
with installed signer SHA-256
`79f76edba53e22dcb9e4610dc5fa151e5c52d0895c68b2802444f1ced3a1c06f`
may call the provider. Identity comes from Binder UID, never a supplied name.
The certificate was verified from both installed host APKs on 2026-10-10.

The provider does not open cameras. A visible host can bind the explicit
`com.axelsarassamit.ridedeck.dash.CompanionRecordingService` with
`BIND_AUTO_CREATE | BIND_INCLUDE_CAPABILITIES` (capabilities flag on API 30+).
Keep this binding for the active ride. Messenger requests require
`protocolVersion=1`, a `replyTo`, verified sending UID/signature, and a lease
owned by that caller. `what=1` prepares recording without opening Dash;
reply `accepted` means a start ticket was issued, not that capture has begun.
The reply includes immutable one-shot `PendingIntent startIntent`. Verify
its creator is the genuine signed Dash UID/package and send it only while
the initiating host is visible and the explicit Start/Resume gesture remains
current. The ticket expires after 15 seconds and requires the same active
lease owner. Do not replay it after UI leave, permission prompts or timeout.
This visible-host PendingIntent is the Android while-in-use exemption;
binding alone did not permit a separate internal camera service on the
tested Xiaomi Android 16. No automatic Dash activity launch is involved.
Missing camera/audio permission requires explicit one-time Dash setup.
Never automatically launch Dash from a recording request.

`what=2` obtains an angle-check preview. Poll at 1 Hz only while the host
preview is visible, with one request outstanding. Reply includes
`protocolVersion`, `accepted`, `recording` (all requested tracks capturing),
`message`, and optional upright `rearJpeg` / `frontJpeg` arrays, each at most
100 KB. Frames older than one second are omitted; clear missing images.
Use FIT_CENTER to show the entire recorded frame. Preview decoding expires
1.5 seconds after the last request. Background recording still requires a
renewed ride lease and the existing power gate.

`what=3` returns the same status fields, plus `starting` (camera startup in
progress), without JPEGs or enabling preview
decoding. Use this for a visible main-screen REC indicator; show REC only
when `recording=true` is freshly confirmed. Do not interpret an active lease
or an accepted start request as actual recording. Clear stale status after
three seconds without a reply and stop polling when the host UI is hidden.
Hosts must not renew for an idle app without an active ride session.
Missing provider, incompatible protocol, rejected lease and security errors
must leave normal host functionality available. Debug host APKs are rejected.

No Yamaha speed feed is defined yet. A heartbeat is not vehicle telemetry.
