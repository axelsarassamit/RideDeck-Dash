package com.axelsarassamit.ridedeck.dash;
import android.hardware.camera2.*;
import java.util.*;
final class RearLenses {
    static String selected(CameraManager manager,DashSettings settings)throws CameraAccessException {
        String wanted=settings.text("rearLens","auto"),first=null;
        for(String id:manager.getCameraIdList())if(Objects.equals(manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING),CameraCharacteristics.LENS_FACING_BACK)){
            if(first==null)first=id;if(id.equals(wanted))return id;
        }
        if(!"auto".equals(wanted))throw new IllegalStateException("Selected rear lens unavailable. Choose Automatic in Video and storage.");
        return first;
    }
    static String label(CameraCharacteristics camera){
        float[] focal=camera.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        android.util.SizeF sensor=camera.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        float equivalent=focal!=null&&focal.length>0&&sensor!=null?focal[0]*36/sensor.getWidth():0;
        String name=equivalent==0?"Rear lens":equivalent<20?"Ultrawide":equivalent>40?"Telephoto":"Main wide";
        boolean optical=has(camera.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION),1);
        boolean electronic=has(camera.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES),1)||has(camera.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES),2);
        return name+(equivalent==0?"":String.format(Locale.ROOT," (%.0f mm)",equivalent))+" / "+(optical?"OIS":"No OIS")+" / "+(electronic?"EIS advertised":"No EIS advertised");
    }
    static boolean has(int[] values,int wanted){if(values!=null)for(int value:values)if(value==wanted)return true;return false;}
}
