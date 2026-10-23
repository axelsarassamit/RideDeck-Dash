package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.widget.*;
import android.view.View;
import android.view.WindowInsets;

final class DashUi {
    static LinearLayout page(Activity activity) {
        LinearLayout page = new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(RideStyle.BACKGROUND); int spacing = dp(activity,16); page.setPadding(spacing,spacing,spacing,spacing);
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(b.left+spacing,b.top+spacing,b.right+spacing,b.bottom+spacing);
            } else v.setPadding(spacing+insets.getSystemWindowInsetLeft(),spacing+insets.getSystemWindowInsetTop(),spacing+insets.getSystemWindowInsetRight(),spacing+insets.getSystemWindowInsetBottom());
            return insets;
        });
        return page;
    }
    static int dp(Activity a,int n) { return Math.round(n*a.getResources().getDisplayMetrics().density); }
    static void card(android.view.View v,int color,float radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(radius*v.getResources().getDisplayMetrics().density);v.setBackground(d);}
    static TextView text(Activity a, LinearLayout page, String value, int size) {
        TextView t = new TextView(a); t.setText(value); t.setTextColor(RideStyle.TEXT); t.setTextSize(size); t.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)); t.setPadding(dp(a,8),dp(a,10),dp(a,8),dp(a,10)); page.addView(t); return t;
    }
    static void actionIcon(Button b,String label) {
        b.setCompoundDrawables(null,new RideGlyph(b.getContext(),label),null,null);b.setCompoundDrawablePadding(Math.round(2*b.getResources().getDisplayMetrics().density));
        int pad=Math.round(4*b.getResources().getDisplayMetrics().density);
        b.setGravity(android.view.Gravity.CENTER);b.setPadding(pad,pad,pad,pad);b.setTextSize(12);b.setMaxLines(2);
    }
    static Button button(Activity a, LinearLayout p, String label, View.OnClickListener action) {
        Button b = new Button(a); b.setText(label); RideStyle.action(b,true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(a,8));p.addView(b,lp);b.setOnClickListener(action);return b;
    }
}
