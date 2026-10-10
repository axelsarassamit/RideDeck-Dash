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
        Button b = new Button(a); b.setText(label); b.setTextSize(19); b.setMinHeight(64); b.setOnClickListener(action); p.addView(b); return b;
    }
}
