package com.axelsarassamit.ridedeck.dash;

import android.hardware.camera2.*;

/** The alignment view and saved video must use the same focus policy. */
final class CameraFocus {
    static void apply(CaptureRequest.Builder request, CameraCharacteristics camera,
                      DashSettings settings, String label) {
        if(android.os.Build.VERSION.SDK_INT>=30 && camera.getAvailableCaptureRequestKeys().contains(CaptureRequest.CONTROL_ZOOM_RATIO)) {
            android.util.Range<Float> range=camera.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            if(range!=null&&range.contains(1f))request.set(CaptureRequest.CONTROL_ZOOM_RATIO,1f);
        }
        if(android.os.Build.VERSION.SDK_INT>=34 && camera.getAvailableCaptureRequestKeys().contains(CaptureRequest.CONTROL_AUTOFRAMING))
            request.set(CaptureRequest.CONTROL_AUTOFRAMING,CaptureRequest.CONTROL_AUTOFRAMING_OFF);
        int selected = settings.number(label + "Focus", "rear".equals(label) ? 1 : 0);
        Float minimum = camera.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        int[] modes = camera.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        boolean off = supports(modes, CaptureRequest.CONTROL_AF_MODE_OFF);
        boolean manual = supports(camera.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                && camera.getAvailableCaptureRequestKeys().contains(CaptureRequest.LENS_FOCUS_DISTANCE);
        if (minimum != null && minimum == 0f) {
            if (off) request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            return;
        }
        if (selected != 0 && minimum != null && minimum > 0f && off && manual) {
            request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            request.set(CaptureRequest.LENS_FOCUS_DISTANCE, Math.min(minimum, selected == 2 ? 0.2f : 0f));
            return;
        }
        if(selected!=0)throw new IllegalStateException(label+" camera cannot lock distant focus. Choose another lens or explicitly choose autofocus in Camera selection.");
        if (supports(modes, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO))
            request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        else if (supports(modes, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE))
            request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }
    private static boolean supports(int[] modes, int value) {
        if (modes != null) for (int mode : modes) if (mode == value) return true;
        return false;
    }
}
