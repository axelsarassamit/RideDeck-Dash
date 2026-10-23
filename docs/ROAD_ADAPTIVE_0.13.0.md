# Road adaptive stabilization, 0.13.0

Offline editor update. Live capture and native EIS/OIS requests are unchanged. Original recordings remain unchanged.

The engine uses time-aware whole-segment acceleration smoothing, with a linear-time banded solve. Constant-speed pans and lean are preserved by the objective. Established reliable optical-flow matches are retained; spatially balanced upper-road features provide a conservative recovery pass for weak matches. Capture gaps and unrecoverable tracking breaks remain isolated. Fixed overscan uses independent smooth limits for translation and roll, so horizontal crop pressure does not unnecessarily weaken vertical correction. Classic image and Hybrid lab remain selectable. New edits still start with stabilization off.

## Validation

- 73 JVM tests passed, including irregular timestamps, linear pan/lean, vibration reduction, independent crop limits, portrait/landscape edge bounds and tracking breaks.
- On Xiaomi 14T Pro, the device suite completed 29 cases: 28 passed and the explicit simulated-battery opt-in case was skipped. It covers the new real GPU stabilization fixture, tracker reliability, editor playback/seeking/split/drafts/batch/PiP/geometry, and the existing hybrid fixtures.
- The synthetic GPU export reduced measured marker acceleration to less than 65% of its input, retained all 120 pictures, kept square geometry and passed uncovered-corner checks.
- Five matched 20-second ride windows were exported with both Classic and Road adaptive at Maximum strength. All 3,004 selected source pictures were retained by each engine. All five new exports contain AAC audio with nonzero sampled signal.
- Independent optical-flow measurements below use the road area of the picture, excluding letterboxing, normalized to 360 by 640 pixels. They measure frame-to-frame image motion, including deliberate panning, not a pure vibration score. Rotation and acceleration metrics were mixed. This is limited development evidence, not proof of universal improvement.

| Sample | Horizontal P95, Classic / Road (px) | Vertical P95, Classic / Road (px) |
|---|---:|---:|
| Daytime | 2.959 / 2.232 | 4.738 / 3.868 |
| Night | 0.934 / 0.972 | 1.088 / 1.138 |
| Turning section | 2.757 / 2.324 | 4.336 / 2.644 |
| Additional daytime | 3.947 / 3.322 | 3.635 / 2.878 |
| Additional night | 6.657 / 9.113 | 2.095 / 2.549 |

Daytime translation P95 was approximately 16% to 39% lower across the three daytime sections. Night results are mixed, and the additional night section has larger translation despite lower horizontal acceleration and lower rotation RMS. Classic remains useful for comparison on such clips. Existing motion smear and severe rolling-shutter distortion are not reconstructed. Gyro-only correction is not applied over native OIS recordings without reliable OIS transformation data. Private original videos, ride logs, audit files and side-by-side comparisons are not included in Git.
