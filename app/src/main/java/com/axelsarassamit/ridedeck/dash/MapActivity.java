package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Local map UI. Coordinates leave the phone only for an explicit route request. */
public final class MapActivity extends Activity {
    private WebView map;
    private TextView info;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private boolean loaded;
    private File ride;
    private final Runnable tick=new Runnable(){@Override public void run(){updateMap();ui.postDelayed(this,2000);}};
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout page=DashUi.page(this);setContentView(page);
        DashUi.text(this,page,"Map and route",25);
        info=DashUi.text(this,page,"Blue: recorded ride. Tap a destination to plan a motorcycle route. Online map uses OpenStreetMap.",16);
        String name=getIntent().getStringExtra("ride");
        if(name!=null && name.matches("[0-9]+")) ride=new File(new ClipStore(this).root(),name);
        map=new WebView(this); page.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        map.getSettings().setJavaScriptEnabled(true);
        map.getSettings().setAllowFileAccess(false); map.getSettings().setAllowContentAccess(false);
        map.addJavascriptInterface(new Destination(),"Dash");
        map.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){
                UriParts parts=new UriParts(request.getUrl().toString());
                if(parts.url.startsWith("https://dash.local/map/")) {
                    String path=parts.url.substring("https://dash.local/map/".length());
                    if(!path.matches("[a-zA-Z0-9.-]+")) return blocked();
                    try{return new WebResourceResponse(path.endsWith(".js")?"application/javascript":path.endsWith(".css")?"text/css":"text/html","UTF-8",getAssets().open("map/"+path));}catch(IOException e){return blocked();}
                }
                if("tile.openstreetmap.org".equals(request.getUrl().getHost()) && "https".equals(request.getUrl().getScheme())) return null;
                return blocked();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                if("www.openstreetmap.org".equals(r.getUrl().getHost())) startActivity(new Intent(Intent.ACTION_VIEW,r.getUrl()));
                return true;
            }
            @Override public void onPageFinished(WebView v,String url){loaded=true;updateMap();}
        });
        map.loadUrl("https://dash.local/map/index.html");
        DashUi.button(this,page,"Back to dashboard",v -> finish());
    }
    private static final class UriParts { final String url; UriParts(String v){url=v;} }
    private static WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    private void updateMap(){
        if(RecordingService.running) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!loaded) return;
        File dir=ride!=null?ride:RecordingService.rideDirectory;
        JSONArray points=new JSONArray();
        if(dir!=null) try(BufferedReader reader=new BufferedReader(new FileReader(new File(dir,"route.csv")))){
            String line;reader.readLine();int count=0;
            while((line=reader.readLine())!=null && count++<50000){String[] fields=line.split(",");if(fields.length>=5)points.put(new JSONArray().put(Double.parseDouble(fields[2])).put(Double.parseDouble(fields[3])));}
        }catch(Exception ignored){}
        Location p=ride==null?RecordingService.position:null;
        String current=p!=null?String.format(java.util.Locale.ROOT,"[%.8f,%.8f]",p.getLatitude(),p.getLongitude()):"null";
        map.evaluateJavascript("update("+points+","+current+")",null);
    }
    private final class Destination {
        @JavascriptInterface public void destination(double lat,double lon){
            if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return;
            ui.post(()->{
                Location p=RecordingService.position;
                if(ride!=null || p==null || SystemClock.elapsedRealtimeNanos()-p.getElapsedRealtimeNanos()>6_000_000_000L){info.setText("A current GPS fix is needed. Start a recording and wait for GPS, then tap your destination.");return;}
                new AlertDialog.Builder(MapActivity.this).setTitle("Plan motorcycle route?")
                    .setMessage("Send your current location and this destination to the public Valhalla routing service? Online routes depend on map data and service availability.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Plan route",(d,w)->route(p,lat,lon)).show();
            });
        }
    }
    private void route(Location from,double lat,double lon){
        info.setText("Planning route…");
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                JSONObject query=new JSONObject().put("locations",new JSONArray().put(new JSONObject().put("lat",from.getLatitude()).put("lon",from.getLongitude())).put(new JSONObject().put("lat",lat).put("lon",lon)))
                    .put("costing","motorcycle").put("units","kilometers");
                URL url=new URL("https://valhalla1.openstreetmap.de/route?json="+URLEncoder.encode(query.toString(),"UTF-8"));
                connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setRequestProperty("User-Agent","RideDeckDash/"+BuildConfig.VERSION_NAME);connection.setInstanceFollowRedirects(false);
                if(connection.getResponseCode()!=200)throw new IOException("Routing service unavailable");
                ByteArrayOutputStream data=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){data.write(buf,0,n);if(data.size()>2_000_000)throw new IOException("Route too large");}}
                JSONObject trip=new JSONObject(data.toString(StandardCharsets.UTF_8.name())).getJSONObject("trip");
                JSONArray legs=trip.getJSONArray("legs"),points=new JSONArray();
                for(int i=0;i<legs.length();i++)decode(legs.getJSONObject(i).getString("shape"),points);
                JSONObject summary=trip.getJSONObject("summary");String text=String.format(java.util.Locale.ROOT,"Motorcycle route: %.1f km · %.0f min\nOrange: planned route · blue: your recorded ride",summary.getDouble("length"),summary.getDouble("time")/60);
                ui.post(()->{if(!isDestroyed()){info.setText(text);map.evaluateJavascript("plan("+points+")",null);}});
            }catch(Exception e){ui.post(()->{if(!isDestroyed())info.setText("Route unavailable. Check connection and try again.");});}
            finally{if(connection!=null)connection.disconnect();}
        },"DashRoute").start();
    }
    private static void decode(String encoded,JSONArray result)throws JSONException{
        int index=0,lat=0,lon=0;
        while(index<encoded.length()){
            int[] values=new int[2];for(int axis=0;axis<2;axis++){int value=0,shift=0,b;do{b=encoded.charAt(index++)-63;value|=(b&31)<<shift;shift+=5;if(shift>30)throw new JSONException("Invalid polyline");}while(b>=32);values[axis]=(value&1)!=0?~(value>>1):value>>1;}
            lat+=values[0];lon+=values[1];result.put(new JSONArray().put(lat/1_000_000.0).put(lon/1_000_000.0));
        }
    }
    @Override protected void onStart(){super.onStart();ui.post(tick);}
    @Override protected void onStop(){ui.removeCallbacks(tick);super.onStop();}
    @Override protected void onDestroy(){map.removeJavascriptInterface("Dash");map.destroy();super.onDestroy();}
}
