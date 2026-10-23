package com.axelsarassamit.ridedeck.dash;

/** Session-only, one-way preview relief; never changes encoded cadence or camera ownership. */
final class CapturePacing {
    private int poorWindows;
    private boolean relieved;
    boolean observe(float framesPerSecond,int gaps) {
        if(relieved)return false;
        boolean poor=Float.isFinite(framesPerSecond)&&(framesPerSecond<27 || gaps>=5);
        poorWindows=poor?poorWindows+1:0;
        if(poorWindows<2)return false;
        relieved=true;return true;
    }
    int previewInterval(int requestedMs){return relieved?Math.max(500,requestedMs):requestedMs;}
}
