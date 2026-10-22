# Stabilization direction

The next engine will combine synchronized motion sensors with image tracking. Camera2 capture and the existing Media3/OpenCV editor remain in place. Recording reliability takes priority over live custom processing.

## Delivered foundation

New recordings save optional motion evidence alongside their originals:

- `gyro.csv`: calibrated Android gyroscope samples in radians/second, sensor timestamps, accuracy and callback delivery timestamps. Requested sampling is 200 Hz; `motion.json` reports the observed rate. Device axes do not change when the screen rotates.
- `frames.csv`: one row for each delivered Camera2 capture result, including exposure-start timestamp, callback arrival, frame number, exposure duration, frame duration, rolling-shutter skew, active native stabilization, lens values and crop where available.
- `motion.json`: sensor provenance, camera timestamp sources, sensor orientation, encoded dimensions, available lens calibration, data-loss counters and session completion status. Written atomically during capture and at shutdown.

No image decoding, optical flow or custom video transformation runs during recording. Camera callbacks offer scalar records to a bounded queue; a separate writer performs file I/O. Queue overflow is counted. Logging failure does not stop original video. Motion sidecars count toward recording storage allowance. A 256 MB per-ride motion limit prevents unbounded logging; reaching it ends motion logging and records a failed/incomplete motion session while video continues. Motion data can be disabled in Recording settings. Ride data export and backup include the sidecars.

**This is evidence capture, not a delivered gyro stabilizer.** The current editor still uses image tracking. Older recordings do not gain missing gyro data retroactively.

## Synchronization is a measured requirement

Camera callback arrival is not the exposure time. `SENSOR_TIMESTAMP` is the exposure-start anchor. Camera timestamps are directly comparable to Android sensor timestamps only when the camera advertises the appropriate REALTIME source. UNKNOWN sources require calibration. Even for REALTIME, lens/IMU delay and sensor-to-camera axes require validation.

MediaRecorder normalizes encoded video timestamps and rolls files independently of capture-result delivery. The `recording_file_at_callback` column records the file known at that callback, not a guaranteed encoded-frame identity. Match encoded PTS/cadence to the capture sequence and validate rollover offsets before enabling gyro correction. Frame duration is not a substitute for rolling-shutter readout time. Null values remain unknown.

## Native and calibrated capture paths

Keep the strongest verified native stabilization as the everyday default. Test a separate, explicitly selected stabilization-off capture using the existing setting for calibration and comparison. Native EIS modifies the recorded geometry. OIS moves the optical image independently of the phone body. Raw gyro correction over an already stabilized picture can therefore apply the wrong correction. Logging the OIS/EIS mode alone does not describe those movements.

Before combining them, either obtain usable native motion/intrinsic samples and model them, or validate a capture with those transformations disabled. Do not turn native stabilization off globally based on a theoretical benefit.

## Next implementation gates

1. Verify sensor rate, monotonic timestamps, camera clock source, missing rows and camera cadence on the Xiaomi, including a sustained ride and rollover.
2. Calibrate each tested lens, resolution, frame-rate and stabilization mode. Establish gyro/video time offset and axis mapping.
3. Compare a calibrated gyro reference, the original and image-only stabilization on identical intervals. Gyroflow is a reference benchmark; it is not currently embedded in Dash.
4. Build hybrid estimation: gyro for camera rotation, optical flow for residual motion and synchronization checks, confidence-aware recovery and full-clip trajectory optimization.
5. Add row-dependent GPU warping for rolling shutter, then gradual crop planning. Preserve cornering/lean unless the user selects horizon levelling.
6. Promote only after matched visual comparisons, frame/PTS/audio checks, crop measurements and sustained capture checks pass. Keep originals unchanged.

## Bounded improvements to the current editor

Image tracking can fail on moving foreground objects, darkness and blur. The current change fades correction on both sides of a failed tracking boundary instead of snapping to identity in one frame. It also removes an unnecessary fixed 30 fps frame-drop effect from offline export. Neither change can restore pictures already missing from an original or reverse exposure blur.

## Primary references

- [Gyroscope stabilization and rolling-shutter correction, Stanford](https://graphics.stanford.edu/papers/stabilization/)
- [Gyroflow synchronization and calibration](https://docs.gyroflow.xyz/app/getting-started/basic-usage)
- [Gyroflow phone capture and internal stabilization limits](https://docs.gyroflow.xyz/app/getting-started/supported-cameras/mobile-phones)
- [Android camera timestamp sources](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#SENSOR_INFO_TIMESTAMP_SOURCE)
- [Android rolling-shutter metadata](https://developer.android.com/reference/android/hardware/camera2/CaptureResult#SENSOR_ROLLING_SHUTTER_SKEW)
