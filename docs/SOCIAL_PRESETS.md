# Social video export

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
