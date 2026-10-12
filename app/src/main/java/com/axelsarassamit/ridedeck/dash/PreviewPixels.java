package com.axelsarassamit.ridedeck.dash;

/** Direct NV21 display conversion. Avoids lossy JPEG encode/decode and a rotated intermediate bitmap. */
final class PreviewPixels {
    static void convert(byte[] nv21,int[] pixels,int width,int height,int rotation){
        if(width<=0||height<=0||(width&1)!=0||(height&1)!=0||nv21.length<width*height*3/2||pixels.length<width*height)throw new IllegalArgumentException("Invalid preview image");
        if(rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270)throw new IllegalArgumentException("Invalid preview rotation");
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int yy=Math.max(0,(nv21[y*width+x]&255)-16);
            int chroma=width*height+(y/2)*width+(x&~1);
            int v=(nv21[chroma]&255)-128,u=(nv21[chroma+1]&255)-128;
            int r=clamp((298*yy+409*v+128)>>8),g=clamp((298*yy-100*u-208*v+128)>>8),b=clamp((298*yy+516*u+128)>>8);
            int index=rotation==90?x*height+height-1-y:rotation==180?(height-1-y)*width+width-1-x:rotation==270?(width-1-x)*height+y:y*width+x;
            pixels[index]=0xff000000|(r<<16)|(g<<8)|b;
        }
    }
    static int clamp(int value){return Math.min(255,Math.max(0,value));}
}
