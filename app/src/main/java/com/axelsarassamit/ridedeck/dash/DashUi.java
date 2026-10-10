package com.axelsarassamit.ridedeck.dash;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.widget.*;
import android.view.View;
import android.view.WindowInsets;

final class DashUi {
    static final int INK = Color.rgb(16,28,36), WHITE = Color.rgb(244,246,250), BLUE = Color.rgb(144,200,255);
    static LinearLayout page(Activity activity) {
        LinearLayout page = new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(INK); page.setPadding(20,20,20,20);
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(b.left+20,b.top+20,b.right+20,b.bottom+20);
            } else v.setPadding(20+insets.getSystemWindowInsetLeft(),20+insets.getSystemWindowInsetTop(),20+insets.getSystemWindowInsetRight(),20+insets.getSystemWindowInsetBottom());
            return insets;
        });
        return page;
    }
    static TextView text(Activity a, LinearLayout page, String value, int size) {
        TextView t = new TextView(a); t.setText(value); t.setTextColor(WHITE); t.setTextSize(size); t.setPadding(8,10,8,10); page.addView(t); return t;
    }
    static Button button(Activity a, LinearLayout p, String label, View.OnClickListener action) {
        Button b = new Button(a); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        float density=a.getResources().getDisplayMetrics().density;
        b.setMinHeight((int)(56*density));b.setPadding((int)(10*density),8,(int)(10*density),8);
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
        background.setColor(Color.rgb(31,44,56));background.setCornerRadius(12*density);background.setStroke((int)density,Color.rgb(55,72,86));b.setBackground(background);
        b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(100,117,130),WHITE}));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(4,(int)(4*density),4,(int)(6*density));p.addView(b,lp);b.setOnClickListener(action);return b;
    }
}
