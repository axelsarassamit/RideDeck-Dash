package com.axelsarassamit.ridedeck.dash;

/** Pure monotonic-time movement policy. Missing or inaccurate GPS never means stopped. */
final class DriveAutomation {
    private static boolean valid(float speed,float accuracy){return Float.isFinite(speed)&&Float.isFinite(accuracy)&&speed>=0&&accuracy>=0;}
    private long movingSince=-1, stoppedSince=-1;
    boolean start(long now, boolean fresh, float speed, float accuracy) {
        if (!fresh || !valid(speed,accuracy) || accuracy>30 || speed<8f/3.6f) { movingSince=-1; return false; }
        if(movingSince<0)movingSince=now;
        return now-movingSince>=3000;
    }
    boolean stop(long now, boolean plugged, boolean fresh, float speed, float accuracy, long delay) {
        if(plugged || !fresh || !valid(speed,accuracy) || accuracy>30 || speed>2f/3.6f) { stoppedSince=-1; return false; }
        if(stoppedSince<0)stoppedSince=now;
        return now-stoppedSince>=delay;
    }
}
