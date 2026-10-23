package com.axelsarassamit.ridedeck.dash;

import android.graphics.*;
import android.media.MediaMetadataRetriever;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/** Known moving synthetic marker: real decoder, road analysis, GPU correction and encoder. */
public class RoadExportTest {
    @Test public void roadExportReducesJitterKeepsGeometryAndEveryPicture()throws Exception {
        SocialExportTest h=new SocialExportTest();h.setup();
        try{
            File source=new File(h.ride,"rear-0001.mp4");
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("social/road-shake.mp4");OutputStream out=new FileOutputStream(source)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            SocialProject p=SocialProject.load(h.c,SocialProject.selection(Collections.singletonList(source.getPath())));
            Map<String,Object> options=new HashMap<>(h.recipe.snapshot);options.put("social_ratio",0);options.put("studio_stabilize",3);options.put("studio_road",1);
            File output=h.render(p,new DashSettings(h.c,options));
            assertEquals(120,RearStabilizationTest.frames(source,0,Long.MAX_VALUE));assertEquals(120,RearStabilizationTest.frames(output,0,Long.MAX_VALUE));
            double[][] before=markers(source,false),after=markers(output,true);double a=jitter(before),b=jitter(after);
            System.out.println("Synthetic marker acceleration before="+a+" after="+b);assertTrue("Reduce vibration, not just keep export playable",b<a*.65);
        }finally{h.cleanup();}
    }
    static double[][] markers(File file,boolean checkEdges)throws Exception {
        List<double[]> centers=new ArrayList<>();
        try(MediaMetadataRetriever r=new MediaMetadataRetriever()){
            r.setDataSource(file.getPath());
            for(int i=5;i<55;i++){
                Bitmap image=r.getScaledFrameAtTime(i*66667L,MediaMetadataRetriever.OPTION_CLOSEST,180,320);assertNotNull(image);
                int w=image.getWidth(),h=image.getHeight();int[] pixels=new int[w*h];image.getPixels(pixels,0,w,0,0,w,h);byte[] mask=new byte[pixels.length];
                for(int k=0;k<pixels.length;k++)if(Color.red(pixels[k])>180&&Color.blue(pixels[k])>180&&Color.green(pixels[k])<70)mask[k]=(byte)255;
                Mat input=new Mat(h,w,CvType.CV_8UC1),labels=new Mat(),stats=new Mat(),centroids=new Mat();
                try{input.put(0,0,mask);int count=Imgproc.connectedComponentsWithStats(input,labels,stats,centroids);int best=0,area=0;
                    for(int k=1;k<count;k++){int size=(int)stats.get(k,Imgproc.CC_STAT_AREA)[0];if(size>area){area=size;best=k;}}
                    assertTrue("Marker survives",area>500);centers.add(new double[]{centroids.get(best,0)[0],centroids.get(best,1)[0]});
                    assertEquals("Square must not stretch",1,stats.get(best,Imgproc.CC_STAT_WIDTH)[0]/stats.get(best,Imgproc.CC_STAT_HEIGHT)[0],.06);
                }finally{input.release();labels.release();stats.release();centroids.release();}
                if(checkEdges)for(int at:new int[]{w+1,2*w-2,(h-2)*w+1,(h-1)*w-2})assertTrue("No uncovered crop corners",Color.red(pixels[at])+Color.green(pixels[at])+Color.blue(pixels[at])>40);
                image.recycle();
            }
        }
        return centers.toArray(new double[0][]);
    }
    static double jitter(double[][] p){double sum=0;for(int i=1;i<p.length-1;i++)for(int k=0;k<2;k++)sum+=Math.pow(p[i-1][k]-2*p[i][k]+p[i+1][k],2);return Math.sqrt(sum/(p.length-2));}
}
