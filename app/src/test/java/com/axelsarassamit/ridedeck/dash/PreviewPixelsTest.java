package com.axelsarassamit.ridedeck.dash;
import org.junit.Test;import static org.junit.Assert.*;
public class PreviewPixelsTest {
    byte[] neutral(){return new byte[]{16,50,90,(byte)130,(byte)128,(byte)128};}
    int[] image(int rotation){int[] out=new int[4];PreviewPixels.convert(neutral(),out,2,2,rotation);return out;}
    @Test public void rotatesWithoutCroppingOrMirroring(){int[] a=image(0);assertArrayEquals(new int[]{a[2],a[0],a[3],a[1]},image(90));assertArrayEquals(new int[]{a[3],a[2],a[1],a[0]},image(180));assertArrayEquals(new int[]{a[1],a[3],a[0],a[2]},image(270));}
    @Test public void neutralBlackAndWhiteAreOpaque(){int[] out=new int[4];PreviewPixels.convert(new byte[]{16,(byte)235,16,(byte)235,(byte)128,(byte)128},out,2,2,0);assertEquals(0xff000000,out[0]);assertEquals(0xffffffff,out[1]);}
    @Test public void nv21UsesVThenU(){int[] out=new int[4];PreviewPixels.convert(new byte[]{82,82,82,82,(byte)240,90},out,2,2,0);assertTrue(((out[0]>>16)&255)>=250);assertTrue(((out[0]>>8)&255)<=3);assertTrue((out[0]&255)<=3);}
    @Test public void rotatesRectangularImageToPortrait(){byte[] data={16,30,50,70,90,110,(byte)130,(byte)150,(byte)128,(byte)128,(byte)128,(byte)128};int[] a=new int[8],b=new int[8];PreviewPixels.convert(data,a,4,2,0);PreviewPixels.convert(data,b,4,2,90);assertArrayEquals(new int[]{a[4],a[0],a[5],a[1],a[6],a[2],a[7],a[3]},b);}
    @Test(expected=IllegalArgumentException.class) public void rejectsPartialImage(){PreviewPixels.convert(new byte[5],new int[4],2,2,0);}
}
