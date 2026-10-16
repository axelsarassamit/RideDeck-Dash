package com.axelsarassamit.ridedeck.dash;
import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
public final class AppearanceActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        RideAppearance.prepare(this); super.onCreate(state);
        LinearLayout page=DashUi.page(this);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(page);setContentView(scroll);
        DashUi.text(this,page,"Appearance",28);
        DashUi.text(this,page,"Follow RideDeck uses the appearance and colour theme from the last opened RideDeck app. System follows your phone. Light and Dark override it.",17);
        DashUi.button(this,page,"Appearance: "+(RideAppearance.followsHost(this)?"Follow RideDeck":RideAppearance.description(this)),v->RideAppearance.showOptions(this));
        String[] names={"Lime","Ice blue","Amber","White","RideDeck blue"};
        DashUi.button(this,page,"Color theme: "+names[Math.max(0,Math.min(4,RideAppearance.accentIndex(this)))],v->RideAppearance.showAccent(this));
        DashUi.button(this,page,"Back",v->finish());
    }
}
