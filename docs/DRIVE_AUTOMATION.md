# Automatic ride recording

Default: enabled. Dash starts once when a signed companion's new active session reaches the visible Dashboard. Host apps may open Dashboard only from their explicit visible Start/Resume action with genuine enabled Dash installed. Heartbeats never open cameras or activities.

Standalone automatic movement start needs visible Dashboard, precise GPS permission, fresh fixes (under six seconds), reported speed at least 8 km/h and accuracy at most 30 m for three seconds. Camera/microphone permission and charger OR 50% battery gating remain mandatory. Auto start can be disabled independently of host auto-opening.

Stop defaults to charger disconnected plus fresh, accurate speed at most 2 km/h continuously for sixty seconds. Missing speed, GPS loss, inaccurate fixes, movement and reconnection reset the timer. Users can select 30 or 120 seconds or disable this convenience rule. Unauthorized backgrounding or unplugged battery below 50% still stops immediately. No battery spoofing or fake GPS is used for physical acceptance.

Manual Stop from dashboard or notification consumes the current session and movement trigger. A new companion session or genuine stationary-to-moving transition can arm a new start. Activity rotation does not count as a new ride. Process restart never autonomously opens a camera.

Version 0.4.0 adds unit coverage for sustained motion, stale fixes, stop duration/reset behavior, session generations and segment-aligned telemetry including old/missing GPS data. Physical ride and unplugged-stationary verification must be reported separately from these policy tests.
