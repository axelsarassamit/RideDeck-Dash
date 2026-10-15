package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.widget.*;
import android.view.View;
import android.view.WindowInsets;

final class DashUi {
    static final int INK = RideStyle.BACKGROUND, WHITE = RideStyle.TEXT, BLUE = 0xff6c94ff;
    static LinearLayout page(Activity activity) {
        LinearLayout page = new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(INK); int spacing = dp(activity,16); page.setPadding(spacing,spacing,spacing,spacing);
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
        TextView t = new TextView(a); t.setText(value); t.setTextColor(WHITE); t.setTextSize(size); t.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)); t.setPadding(dp(a,8),dp(a,10),dp(a,8),dp(a,10)); page.addView(t); return t;
    }
    static Button button(Activity a, LinearLayout p, String label, View.OnClickListener action) {
        Button b = new Button(a); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        float density=a.getResources().getDisplayMetrics().density;
        b.setMinHeight((int)(56*density));b.setPadding(dp(a,16),dp(a,8),dp(a,16),dp(a,8));
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
        background.setColor(RideStyle.RAISED);background.setCornerRadius(8*density);background.setStroke((int)density,RideStyle.LINE);b.setBackgroundTintList(null); b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x286c94ff),background,null));
        b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(100,117,130),WHITE}));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(4,(int)(4*density),4,(int)(6*density));p.addView(b,lp);b.setOnClickListener(action);return b;
    }
}
