# RideDeck Dash 0.13.0

The editor offers Road adaptive stabilization, Classic image and the existing Hybrid lab. When image stabilization is enabled, Road adaptive is the default engine; stabilization itself remains off for a new edit until selected. Road adaptive optimizes the complete selected trajectory with time-aware acceleration smoothing, preserving steady pans and lean. A conservative recovery pass balances features across the upper road scene when the established tracker has no reliable match. The crop stays fixed at the selected strength, with smoothly limited correction near its bounds and fades around tracking breaks. Preview and export share the same correction.

Original video frames and audio are retained; live recording stays lightweight with the existing native stabilization. Classic remains available for comparison, particularly for low-light footage. Neither engine recovers missing exposures, vibration blur or severe rolling-shutter distortion. See [the validation notes](ROAD_ADAPTIVE_0.13.0.md).

## Earlier 0.12.3 features

Saved edits now offer Delete drafts from both the library and the editor. Select individual drafts, several drafts or Select all, then confirm deletion. Only saved edit recipes are removed; source recordings and exported videos are preserved. Deleting the currently open edit closes it and prevents automatic saving from recreating it.

## Earlier 0.12.2 features

The editor follows the inspected Xiaomi Gallery workflow: a centered proportion-correct preview, fixed playhead over a scrolling filmstrip, amber clip selection, flat icon tools, contextual Editor actions, and a working Add recordings control. It retains Dash overlays, front PiP and stabilization. Only implemented tools are shown.

The editor splits the clip under the playhead into two editable halves without changing total duration and selects the new right-hand half. Undo/redo retain selection and playhead. The timeline shows only the edited portions, samples thumbnails from each portion, preserves thumbnail proportions and no longer trims on a simple tap of a handle. Preview uses the chosen output-canvas proportions in both phone orientations.

Editing preview is capped at 720p while exports keep their selected quality. Rotating the export screen no longer interrupts an active batch. Project opening runs off the UI thread. Only visible timeline clips request thumbnails. Duration metadata is cached by file identity/size/modification time, and PiP seeks and boundary refreshes reuse the prepared composition instead of rescanning and rebuilding the whole project. Stabilization caching now accommodates multi-clip edits with a bounded frame budget. Projects support up to 30 clips and two hours in total; per-clip stabilization analysis still supports at most 30 minutes. Long encodes remain device-dependent and are not instantaneous.

Rear priority now disables front-camera EIS/OIS while keeping the front recording at 720p and at most 6 Mbps. Rear stabilization stays selected as before. After two consecutive five-second windows with rear cadence below 27 fps or at least five long gaps, the session slows live previews to at most two updates per second. Both recordings still request 30 fps. Relief remains active until Stop to avoid repeated camera-request changes. Same-quality dual mode retains its prior stabilization behavior.

Preview conversion is also rate-limited on the image consumer, even when camera-side delivery was already throttled.

Image stabilization checks optical flow in both directions and rejects inconsistent tracks. It starts a fresh motion segment after source-picture gaps of 100 ms or longer and fades corrections on either side. Existing native-OIS recordings remain supported by image smoothing. The original timing and all available source pictures are retained. These changes reduce competing work and unsafe correction; they do not recover missing pictures or guarantee gap-free outdoor recording.

## Earlier 0.12.1 features

Phone microphone selection no longer requires Bluetooth permission. Nearby-device permission is requested only when selecting a Bluetooth microphone. A visible MIC OFF indicator makes silent recording explicit, and new motion summaries include the configured audio input and source for diagnosis. Explicit audio-off choices remain respected.

## Earlier 0.12.0 features

Video editing, preview, stabilization analysis and social export now work on battery without a charger or a 50% reserve. Recording retains its existing charger grace and 30% minimum; backup and sound-check rules are unchanged.

Setup > Recording > Calibration and sample recordings offers a protected 30-second parked calibration, native reference sessions and separate native-stabilization-off hybrid samples. Choose 30 seconds, two minutes, five minutes or until Stop. Session-specific stabilization and focus overrides do not replace normal settings. Both orientations remain supported.

New recordings save accelerometer data, ISO, focus/auto-exposure state, zoom, per-frame intrinsics and available optical-stabilization samples alongside gyro and camera timing. Unsupported measurements stay absent. Motion data has a configurable per-ride limit, defaults to 512 MB, counts toward the storage allowance and travels with ride-data export/backup. Logging remains separate from image processing.

The editor adds opt-in Hybrid lab: a per-clip small-angle gyro/image fit, bounded clock-offset search with held-out agreement and ambiguity checks, confidence-gated image residuals, motion smoothing and a five-knot row-dependent GPU inverse warp. Constant crop avoids zoom pumping and is capped at 1.65x. It supports rear native-off samples with complete shared-clock evidence, fixed geometry/focus and enough independent movement. Native-stabilized clips, missing motion, weak fits, ambiguous timing and excessive correction are rejected rather than silently processed.

Hybrid lab is experimental. It is not a full physical lens-distortion calibration and has not been validated on a mounted outdoor ride. Synthetic landscape and portrait tests exercise the real decoder, tracker, shader and encoder with known gyro/readout motion, checking timing recovery, source frame count and audio. Real sample-capture tests check native-off operation, protected clips, timed stopping and preservation of normal preferences. Outdoor sample footage is the next validation step.

## Earlier features retained

New recordings can save gyroscope samples and per-frame camera timing as the foundation for calibrated hybrid stabilization. Recording settings include a Motion data switch, enabled by default. Motion logging uses separate sensor/storage workers and a bounded queue. Failures or the configured per-ride motion limit stop motion logging while original video continues. The summary records the observed sensor rate, clock sources, lost rows and completion status. Sidecars count toward the recording storage allowance and are included in ride-data export and backup.

Older recordings use image-based correction. The new experimental hybrid option requires complete native-off sample evidence; physical lens calibration and mounted-road validation remain future work. Existing recordings remain available for image-based editing but cannot gain missing historical gyro samples. See [the stabilization direction](STABILIZATION_DIRECTION.md).

Offline exports preserve source cadence instead of applying an unconditional 30 fps frame-drop effect. Tracking failures now fade correction on both sides of the break, reducing artificial one-frame correction jumps. These changes do not recreate missing source pictures, remove exposure blur or guarantee smooth motorcycle footage.

The connected Xiaomi passed a 30-second dual-camera motion-data check and a matched 20-second real-road export that retained all 601 selected source pictures. The 13 real-device editor regression checks passed, including seeking, playback, stabilization, merged video, audio and front PiP. These short checks do not replace a sustained outdoor ride and calibration test.

### Previous editor and recording features

The launcher now uses the approved orange camera-and-R icon, making Dash visibly different from Standard's blue R and Yamaha's separate Yamaha-branded icon. In-app and exported-video branding remains unchanged.

Editor timeline jumps and Play now refresh the visible video, including audio-enabled, muted and front-PiP previews. PiP seeks and clip/front transitions briefly rebuild the preview at the selected time instead of reusing stale compositor frames; this preview refresh does not add a pause to exports. The editing engine is updated to Media3 1.11.1, and volume/fades use its sample-position-aware gain automation.

Overlays now use the exact approved white-and-amber Canva logo and RideDeck Dash lettering already used by the app. The old blue outline watermark is removed. Deep navy panels, neutral borders and the shared blue/amber palette match the current identity. Branding is drawn once after composition, preserving its transparency and keeping it visible above PiP and titles.

One timeline-based social video editor replaces the previous single-clip and overlay-export editors. Edit one recording or selected clips in the same screen, with a large preview, thumbnail timeline, trim handles, split, duplicate, reorder, remove, undo/redo and saved drafts. Portrait and landscape use dedicated layouts.

Tools include social formats and framing, movable rounded front-camera PiP, configurable ride overlays, timed text, imported music with volume/fades, picture adjustments and integrated experimental stabilization. Preview and export share the same recipe. The RideDeck logo remains mandatory. Original recordings are preserved.

Stabilization offers Off, Gentle, Strong and Maximum and analyzes the selected main clips before preview. Stronger modes crop more. This integrates the existing correction engine; it does not guarantee elimination of road vibration, blur or captured frame gaps.

Recordings now show video thumbnails, with newest/oldest/name/file-size sorting and rear/front/protected filters. Multi-selection remains available. Thumbnail decoding pauses while recording.

Export one merged social video or separate copies. Drafts reference original files and are not backups. Music uses a local file; platform music and posting remain in the social app. The moving map uses recorded GPS trace. Keep Dash open while exporting; rotation is supported and completed copies survive cancellation of a later batch item.
