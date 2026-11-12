package com.axelsarassamit.ridedeck.dash;

/** Recorded OIS samples are evidence, not an implemented OIS compensation model. */
final class NativeMotionEligibility {
    static boolean rawGyroSupported(String eis, String ois) {
        return "0".equals(eis) && "0".equals(ois);
    }
}
