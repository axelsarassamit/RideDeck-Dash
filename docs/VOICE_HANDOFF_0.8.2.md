# Voice handoff test update 0.8.2

Signed Yamaha 0.12.37 can request that Dash save the current clip and release its microphone before dictation or the Assistant opens. Only an authenticated RideDeck caller is accepted. Another host's active ride cannot be paused by that caller. Yamaha may resume recording after voice returns only while the same explicit ride remains active and its own screen is visible. A short recording gap is expected.

Audio capture is explicitly non-private on Android 11 and later, so compatible system voice features may take priority. Temporary Android silencing of the recording microphone is displayed without stopping the video. Source-routing failures still stop recording rather than silently claiming the selected microphone is working.

This is an integration test artifact, not a published stable release. It does not change video analysis, stabilization, focus settings, pairing or bike protocol behavior. A connected-headset test is required when that hardware is available. External voice triggers outside the RideDeck buttons may still require pausing Dash manually.
