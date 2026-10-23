package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/** The family appearance page: real colours, one preview, explicit saved choices. */
public final class AppearanceActivity extends Activity {
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);return p;}
    private TextView text(LinearLayout p,String value,int size,int colour){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(colour);t.setPadding(0,dp(4),0,dp(8));p.addView(t);return t;}
    private void action(LinearLayout p,String label,Runnable run){Button b=new Button(this);b.setText(label);RideStyle.action(b,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);p.addView(b,lp);b.setOnClickListener(v->run.run());}
    private String accentName(){String[] names=new String[]{"Lime","Ice blue","Amber","White","RideDeck blue"};return names[Math.max(0,Math.min(4,RideAppearance.accentIndex(this)))];}
    @Override public void onCreate(Bundle saved){
        RideAppearance.prepare(this);super.onCreate(saved);
        LinearLayout page=column();page.setPadding(dp(16),dp(12),dp(16),dp(12));page.setBackgroundColor(RideStyle.BACKGROUND);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(page,(v,insets)->{androidx.core.graphics.Insets b=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());v.setPadding(b.left+dp(16),b.top+dp(12),b.right+dp(16),b.bottom+dp(12));return insets;});
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        LinearLayout header=column();header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);page.addView(header);
        TextView heading=text(header,"Appearance",28,RideStyle.TEXT);heading.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        action(header,"Done",()->finish());Button done=(Button)header.getChildAt(1);done.setGravity(Gravity.CENTER);done.setLayoutParams(new LinearLayout.LayoutParams(dp(80),dp(56)));
        text(page,RideAppearance.followsHost(this)?"Following "+(RideAppearance.prefs(this).getString("host_name","").endsWith(".yamaha")?"RideDeck Yamaha":"RideDeck")+" · "+RideAppearance.description(this)+" · "+accentName():"Your saved appearance and colour theme",15,RideStyle.MUTED);
        boolean wide=getResources().getConfiguration().screenWidthDp>=600;
        LinearLayout body=column();body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);page.addView(body);
        LinearLayout preview=column();preview.setPadding(dp(16),dp(12),dp(16),dp(12));preview.setBackground(RideStyle.surface(this,RideStyle.PANEL,8,true));body.addView(preview,wide?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2));
        text(preview,"LIVE PREVIEW",11,RideStyle.MUTED).setLetterSpacing(.1f);
        text(preview,"RideDeck",23,AppearancePalette.accent(RideAppearance.accentIndex(this),RideAppearance.isDark(this)));
        text(preview,"Music, messages and your ride",16,RideStyle.TEXT);
        text(preview,"Ready · "+(RideAppearance.isDark(this)?"Dark":"Light")+" · "+accentName(),14,RideStyle.MUTED);
        LinearLayout choices=column();LinearLayout.LayoutParams choicesParams=wide?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2);if(wide)choicesParams.leftMargin=dp(12);body.addView(choices,choicesParams);
        action(choices,"Appearance: "+(RideAppearance.followsHost(this)?"Follow RideDeck":RideAppearance.description(this)),()->RideAppearance.showOptions(this));
        action(choices,"Color theme: "+accentName(),()->RideAppearance.showAccent(this));
        text(choices,"System follows your phone. Light and Dark stay fixed. All colour themes support both appearances.",14,RideStyle.MUTED);
    }

}
