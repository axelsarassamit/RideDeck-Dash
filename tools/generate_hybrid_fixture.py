"""Generate synthetic public gyro/rolling-shutter fixtures. Requires numpy, OpenCV and ffmpeg."""
import argparse,csv,json,subprocess
from pathlib import Path
import cv2,numpy as np
p=argparse.ArgumentParser();p.add_argument('--ffmpeg',required=True);p.add_argument('--out',type=Path,default=Path('app/src/androidTest/assets/hybrid'));args=p.parse_args();args.out.mkdir(parents=True,exist_ok=True)
w,h,n,fps=640,360,300,30;readout=.009216;exposure=.004;offset=.042;anchor=1000000000000
rng=np.random.default_rng(426);canvas=rng.integers(0,256,(720,1000,3),dtype=np.uint8);canvas=cv2.GaussianBlur(canvas,(7,7),0)
for i in range(90):
 x,y=rng.integers(0,900),rng.integers(0,650);cv2.rectangle(canvas,(x,y),(x+int(rng.integers(10,80)),y+int(rng.integers(10,60))),tuple(int(x) for x in rng.integers(0,256,3)),2)
cv2.putText(canvas,'SYNTHETIC ROAD MOTION',(200,350),cv2.FONT_HERSHEY_SIMPLEX,1,(255,255,255),2)
def rate(t):return np.array([.3*np.sin(5*t)+.1*np.cos(13*t),.2*np.sin(9*t)+.13*np.cos(17*t),.15*np.cos(7*t)+.1*np.sin(21*t)])
def integral(t):return np.array([-.3/5*np.cos(5*t)+.1/13*np.sin(13*t),-.2/9*np.cos(9*t)+.13/17*np.sin(17*t),.15/7*np.sin(7*t)-.1/21*np.cos(21*t)])
mat=np.array([[0,.8,.1],[.7,0,.03],[.01,0,1.]])
def delta(a,b):return (integral(b)-integral(a))@mat.T
encoder=subprocess.Popen([args.ffmpeg,'-hide_banner','-loglevel','error','-y','-f','rawvideo','-pix_fmt','bgr24','-s',f'{w}x{h}','-r',str(fps),'-i','-','-f','lavfi','-i','sine=frequency=440:sample_rate=48000','-t',str(n/fps),'-c:v','libx264','-preset','fast','-crf','14','-pix_fmt','yuv420p','-c:a','aac','-movflags','+faststart',str(args.out/'rear.mp4')],stdin=subprocess.PIPE)
xx,yy=np.meshgrid(np.arange(w)-w/2,np.arange(h)-h/2);pose=np.zeros(3);last=exposure/2+readout/2+offset
for i in range(n):
 center=i/fps+exposure/2+readout/2+offset
 if i:
  d=delta(last,center)*[w,h,1];a=d[2];pose=np.array([np.cos(a)*pose[0]-np.sin(a)*pose[1]+d[0],np.sin(a)*pose[0]+np.cos(a)*pose[1]+d[1],pose[2]+a])
 last=center;rowtime=center+(np.arange(h)/(h-1)-.5)*readout;d=np.array([delta(center,t)*[w,h,1] for t in rowtime]);a=d[:,2,None];px=xx-d[:,0,None];py=yy-d[:,1,None];qx=np.cos(a)*px+np.sin(a)*py; qy=-np.sin(a)*px+np.cos(a)*py
 a=pose[2];qx-=pose[0];qy-=pose[1];mx=(np.cos(a)*qx+np.sin(a)*qy+500).astype(np.float32);my=(-np.sin(a)*qx+np.cos(a)*qy+360).astype(np.float32)
 frame=cv2.remap(canvas,mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_REFLECT);encoder.stdin.write(frame.tobytes())
encoder.stdin.close();assert encoder.wait()==0
with (args.out/'gyro.csv').open('w',newline='') as f:
 writer=csv.writer(f);writer.writerow(['elapsed_ns','gx_rad_s','gy_rad_s','gz_rad_s','accuracy','delivery_elapsed_ns'])
 for t in np.arange(-1,11.5,.0025):writer.writerow([anchor+round(t*1e9),*rate(t),3,anchor+round(t*1e9)+1000000])
with (args.out/'frames.csv').open('w',newline='') as f:
 writer=csv.writer(f);writer.writerow('sensor_ns,callback_elapsed_ns,camera,recording_file_at_callback,frame_number,exposure_ns,frame_duration_ns,rolling_shutter_skew_ns,eis,ois,focus_diopters,focal_mm,crop_ltrb,physical_camera_id'.split(','))
 for i in range(n):writer.writerow([anchor+round(i/fps*1e9),anchor+round(i/fps*1e9)+50000000,'rear','rear-0001.mp4',i,int(exposure*1e9),round(1e9/fps),int(readout*1e9),0,0,0,4,'0:0:640:360','0'])
(args.out/'motion.json').write_text(json.dumps({'schema':1,'session_closed':True,'write_failed':False,'dropped_rows':0,'gyro_registered':True,'camera_modes':[{'camera':'rear','id':'synthetic','timestamp_source':1}],'capture_profile':'hybrid_sample','fixture_only':True},indent=2))
(args.out/'README.md').write_text('Synthetic generated geometry, texture, gyro and row-readout data. No user footage. Recreate using tools/generate_hybrid_fixture.py. 640x360,30fps,10s,4ms exposure,9.216ms readout,42ms gyro delay. Trigonometric gyro motion with three independent axes; known gyro-to-image map. Includes a440Hz tone. This verifies implementation, not motorcycle performance.\n')
print('Generated synthetic fixture:',args.out)
