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

This bridge does not open cameras or start recording. Start recording in
the visible Dash application with permission granted. A renewed ride lease
permits that recording to continue after Dash leaves the foreground.
Hosts must not renew for an idle app without an active ride session.
Missing provider, incompatible protocol, rejected lease and security errors
must leave normal host functionality available. Debug host APKs are rejected.

No Yamaha speed feed is defined yet. A heartbeat is not vehicle telemetry.
