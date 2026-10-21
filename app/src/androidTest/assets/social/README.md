Synthetic test fixtures only. No ride footage or personal data.

Four H.264/AAC MP4s at 30 fps, 48 kHz mono were generated with FFmpeg's `color` and `sine` sources. Rear A is red/440 Hz for six seconds; rear B blue/660 Hz for six seconds, both 640x360. Front A is lime/880 Hz for four seconds; front B yellow/990 Hz for six seconds, both 360x640. Tests give the front recordings different capture starts and portrait geometry inside a landscape output to exercise leading gaps, rollovers and native PiP aspect ratio.

Video options: libx264, yuv420p, veryfast, CRF 28. Audio: AAC 48 kbit/s. MP4 faststart.

Front fixtures also have a cyan strip over the top fifth and a magenta strip over the bottom fifth, verifying that the GPU mask preserves orientation. These are generated with the `drawbox` filter.
