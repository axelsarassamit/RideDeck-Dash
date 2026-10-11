package com.axelsarassamit.ridedeck.dash;
/** Export geometry and trim rules, independent of Android. */
final class SocialProfile {
 static final String[] PLATFORMS={"TikTok video","Instagram Reel","Instagram Story","Instagram feed","Facebook Reel","Facebook Story","Facebook feed","Facebook landscape"};
 static final String[] STYLES={"Quick highlight","Ride story","Route tour","Clean full clip"};
 static int ratio(int platform){return platform==3||platform==6?1:platform==7?3:0;}
 static int[] dimensions(int ratio,int quality){if(quality!=720&&quality!=1080)throw new IllegalArgumentException("Choose 720 or 1080 quality");switch(ratio){case 0:return new int[]{quality,quality*16/9};case 1:return new int[]{quality,quality*5/4};case 2:return new int[]{quality,quality};case 3:return new int[]{quality*16/9,quality};default:throw new IllegalArgumentException("Choose a supported format");}}
 static long[] range(long durationMs,int startSeconds,int lengthSeconds){if(durationMs<=0||startSeconds<0||lengthSeconds<0)throw new IllegalArgumentException("Invalid clip duration");long start=startSeconds*1000L;if(start>=durationMs)throw new IllegalArgumentException("Start must be before the end of this clip");return new long[]{start,lengthSeconds==0?durationMs:Math.min(durationMs,start+lengthSeconds*1000L)};}
 static int seconds(int style){return new int[]{15,60,60,0}[style];}
 static String[] overlayKeys(){return new String[]{"speed","time","distance","route","altitude","heading","accuracy","battery","brand","camera"};}
 static boolean overlay(int mode,String key){return mode==3||mode==1&&(key.equals("speed")||key.equals("brand"))||mode==2&&(key.equals("speed")||key.equals("time")||key.equals("distance")||key.equals("route")||key.equals("brand"));}
}
