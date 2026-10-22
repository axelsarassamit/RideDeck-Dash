# RideDeck Dash 0.11.0

New recordings can save gyroscope samples and per-frame camera timing as the foundation for calibrated hybrid stabilization. Recording settings include a Motion data switch, enabled by default. Motion logging uses separate sensor/storage workers and a bounded queue. Failures or the 256 MB per-ride motion limit stop motion logging while original video continues. The summary records the observed sensor rate, clock sources, lost rows and completion status. Sidecars count toward the recording storage allowance and are included in ride-data export and backup.

This release does not yet apply gyro correction. The existing editor remains image-based. Accurate gyro/video synchronization, lens calibration, native EIS/OIS compatibility and rolling-shutter correction still need controlled validation before the hybrid engine can be enabled. Existing recordings remain available for image-based editing but cannot gain missing historical gyro samples. See [the stabilization direction](STABILIZATION_DIRECTION.md).

Offline exports preserve source cadence instead of applying an unconditional 30 fps frame-drop effect. Tracking failures now fade correction on both sides of the break, reducing artificial one-frame correction jumps. These changes do not recreate missing source pictures, remove exposure blur or guarantee smooth motorcycle footage.

The connected Xiaomi passed a 30-second dual-camera motion-data check and a matched 20-second real-road export that retained all 601 selected source pictures. The 13 real-device editor regression checks passed, including seeking, playback, stabilization, merged video, audio and front PiP. These short checks do not replace a sustained outdoor ride and calibration test.

## Earlier features retained

The launcher now uses the approved orange camera-and-R icon, making Dash visibly different from Standard's blue R and Yamaha's separate Yamaha-branded icon. In-app and exported-video branding remains unchanged.

Editor timeline jumps and Play now refresh the visible video, including audio-enabled, muted and front-PiP previews. PiP seeks and clip/front transitions briefly rebuild the preview at the selected time instead of reusing stale compositor frames; this preview refresh does not add a pause to exports. The editing engine is updated to Media3 1.11.1, and volume/fades use its sample-position-aware gain automation.

Overlays now use the exact approved white-and-amber Canva logo and RideDeck Dash lettering already used by the app. The old blue outline watermark is removed. Deep navy panels, neutral borders and the shared blue/amber palette match the current identity. Branding is drawn once after composition, preserving its transparency and keeping it visible above PiP and titles.

One timeline-based social video editor replaces the previous single-clip and overlay-export editors. Edit one recording or selected clips in the same screen, with a large preview, thumbnail timeline, trim handles, split, duplicate, reorder, remove, undo/redo and saved drafts. Portrait and landscape use dedicated layouts.

Tools include social formats and framing, movable rounded front-camera PiP, configurable ride overlays, timed text, imported music with volume/fades, picture adjustments and integrated experimental stabilization. Preview and export share the same recipe. The RideDeck logo remains mandatory. Original recordings are preserved.

Stabilization offers Off, Gentle, Strong and Maximum and analyzes the selected main clips before preview. Stronger modes crop more. This integrates the existing correction engine; it does not guarantee elimination of road vibration, blur or captured frame gaps.

Recordings now show video thumbnails, with newest/oldest/name/file-size sorting and rear/front/protected filters. Multi-selection remains available. Thumbnail decoding pauses while recording.

Export one merged social video or separate copies. Drafts reference original files and are not backups. Music uses a local file; platform music and posting remain in the social app. The moving map uses recorded GPS trace. Keep Dash open while exporting; completed copies survive cancellation of a later batch item.
