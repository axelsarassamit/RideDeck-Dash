# Social video export

## Multi-clip studio (0.9.0)

In Recordings, choose **Select clips**, mark up to 30 recordings, then **Social export / merge selected**. A single clip's **Social studio / front PiP** action opens the same studio. Reorder with Move up/down, remove an item from the project without deleting its original, and set a separate in/out range for each clip. The initial selection uses the full clips; **Apply preset trim to every clip** applies the chosen preset start/length individually. Selected output can total up to 30 minutes. Long source selections can be trimmed down before preview/export becomes available.

Choose **Merge into one video** for straight cuts in the displayed order, or **Separate social videos** for one gallery copy per selected clip. Platform, output dimensions, bitrate, framing, sound and telemetry use the existing social presets. Existing saved-settings slots remain accessible there. The whole sequence has a playable composition preview in portrait or landscape. Main-clip audio is retained unless muted; front PiP audio is always muted, and missing main audio produces silence.

For **Front camera as picture-in-picture**, select rear clips as the main view. Rear footage fills the output canvas by default, with **Keep whole picture** available when crop would hide important detail. The front is a small rounded corner inset, matching the live Dash layout. It keeps its native aspect ratio, fits within the chosen fraction of output width/height, and adds no letterbox strips. Matching front clips must be finalized originals from the same ride with recorded monotonic start timestamps. Matching follows time intersections across different camera rollovers, not matching file numbers. Missing front sections hide PiP rather than stretching or freezing unrelated footage. If nothing matches, export is blocked with a clear message. Four corner positions, 20/28/36% sizes, and a front timing offset of up to ±5000 milliseconds are available. Positive offset delays the front view. Capture-start timestamps can have small startup offsets; inspect the preview and adjust when needed. This is not a claim of frame-accurate hardware synchronization.

Branding is drawn after composition, so PiP cannot hide the RideDeck mark. The studio uses local telemetry and route trace consistently in preview/export; downloaded road-network context and experimental motion stabilization remain in the separate single-clip export workflow. No automatic social upload is performed.

Exports are processed sequentially in the foreground. Leaving, rotating, starting recording or losing the charging-or-50% battery reserve cancels the unfinished item. Earlier completed batch videos remain saved and shareable. A partial pending gallery item is removed. Project order, trims and PiP options survive studio recreation/rotation; they are not a permanently saved project library.

## Video editor (0.8.1)

Choose a clip's Single-clip stabilization editor action to open the original playable editor. Portrait uses a preview above the controls; landscape places them side by side. Set the in/out points, apply the trim, then replay that section. Format, framing, sound and overlay choices remain available through the presets screen, including the three saved app settings slots.

Stabilization starts off in a new editor session. Its experimental preview analyzes only the selected section when explicitly requested; Off, Gentle, Strong and Maximum remain selectable. The editor and export share framing and telemetry effects. The preview shows recorded audio or mute; export-only wind filtering and downloaded road context can differ. Export freezes the chosen settings and writes a separate gallery copy, without a second settings prompt. No source file is modified and nothing is posted automatically.

Leaving or rotating during analysis cancels that analysis. Applied trim/settings and draft sliders survive rotation; playback restarts paused. Set either boundary from the current playhead or the sliders, then apply the trim before export. Playback releases its decoder when the editor leaves the screen or capture starts, so it does not keep competing with recording. This is a single-clip editor; multi-clip assembly, captions, music and transitions are not implemented.

These are editable starting settings, not platform upload guarantees. Platforms can change limits and recompress videos. The app prepares a video and opens the Android share chooser; it never posts automatically.

| Target | Default format | Detailed output |
| --- | --- | --- |
| TikTok, Instagram Reel/Story, Facebook Reel/Story | 9:16 | 1080 × 1920 |
| Instagram/Facebook feed | 4:5 | 1080 × 1350 |
| Custom square | 1:1 | 1080 × 1080 |
| Facebook landscape / custom landscape | 16:9 | 1920 × 1080 |

Platform defaults use 8 Mbps H.264 MP4, preserve recorded audio with AAC and keep the entire picture with bars where required. Center crop fills the format; the framing preview shows removed edges. 720 quality and 4/6/8/12/16 Mbps are available. Export drops frames above 30 fps; it does not invent missing source frames. The recorder targets 30 fps.

Quick highlight selects the first 15 seconds with no overlays. Ride story selects up to 60 seconds with speed and branding. Route tour selects up to 60 seconds with speed, time, distance, route trace and branding. Clean full clip uses the remaining recording without telemetry. Start time and length are editable; length 0 means the remaining clip. Short clips stop at their actual end. Invalid starts are rejected. Route overlays reveal location history and audio may include conversations.

Exports go to Movies/RideDeckDash/Social. Internal videos, protection markers and ride CSVs are retained. Stop recording first and stay on the export screen. Leaving or rotating cancels an unfinished export. Battery must be at least 50% or charging, checked continuously. Unsupported encoder output fails instead of silently changing dimensions.

Prepare detailed recording sets the next recording to 1080p, 12 Mbps per camera, 256 MB clips and a 16 GB loop limit. Camera, microphone and automation choices remain selected. Sustained dual-camera performance and road stabilization still require separate validation.

## Three saved settings slots

Save, load after confirmation, replace or remove a named slot. The complete configuration includes capture, camera, microphone, storage, automation, overlays and social settings. Loading one slot retains the others. Footage and companion authentication state are not part of a preset. Loading is blocked during recording/finalization. A saved microphone must still be connected and available.

## Format guidance checked 11 October 2026

TikTok recommends vertical 9:16 and supports MP4 in its official [in-feed creative specifications](https://ads.tiktok.com/resources/help/article/tiktok-auction-in-feed-ads?lang=en-GB). Meta discusses vertical 9:16 for [Facebook and Instagram Reels](https://www.facebook.com/business/ads/facebook-instagram-reels-ads). These advertising references inform the vertical starting format; they do not certify every organic post type, duration or account-specific publishing flow. Pixel dimensions, bitrate and purpose durations above are RideDeck Dash choices. No maximum platform duration is enforced as a universal rule.
