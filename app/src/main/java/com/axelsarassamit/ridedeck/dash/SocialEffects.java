package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import androidx.media3.common.Effect;
import androidx.media3.effect.*;
import java.io.File;
import java.util.*;

/** One framing and telemetry recipe for the editor and the saved copy. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class SocialEffects {
    static List<Effect> create(Context context, File clip, DashSettings settings, long startUs,
            VideoStabilizer stabilization, OverlayRoads roads, boolean exporting) throws Exception {
        List<Effect> effects = new ArrayList<>();
        if (stabilization != null) effects.add(stabilization);
        int[] size = SocialProfile.dimensions(settings.number("social_ratio", 0), settings.number("social_quality", 1080));
        effects.add(Presentation.createForWidthAndHeight(size[0], size[1], settings.number("social_fit", 0) == 1
                ? Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP : Presentation.LAYOUT_SCALE_TO_FIT));
        if(settings.number("studio_zoom",100)!=100||settings.number("studio_pan_x",0)!=0||settings.number("studio_pan_y",0)!=0){
            effects.add((MatrixTransformation)time->{android.graphics.Matrix m=new android.graphics.Matrix();float z=settings.number("studio_zoom",100)/100f;
                m.setScale(z,z);m.postTranslate(settings.number("studio_pan_x",0)/100f*(z-1),-settings.number("studio_pan_y",0)/100f*(z-1));return m;});
        }
        effects.add(new Brightness(settings.number("studio_brightness",0)/100f));
        effects.add(new Contrast(settings.number("studio_contrast",0)/100f));
        effects.add(new HslAdjustment.Builder().adjustSaturation(settings.number("studio_saturation",0)).build());
        Map<String,Object> values = new HashMap<>(settings.snapshot);
        values.put("studio_skip_brand",1); // The mandatory mark is drawn once, after the final composition.
        int style = settings.number("social_overlay", 0);
        if (style != 4) for (String key : SocialProfile.overlayKeys())
            values.put("overlay_" + key, SocialProfile.overlay(style, key) ? 1 : 0);
        effects.add(new OverlayEffect(Collections.singletonList(new TelemetryOverlay(context,
                new RideTelemetry(clip), new DashSettings(context, values), startUs, true, roads))));
        return effects;
    }
}
