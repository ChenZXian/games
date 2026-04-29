package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

public class MenuPanelController {
    private final Context context;
    public MenuPanelController(Context context) { this.context = context; }
    public void showHowToPlay() {
        Toast.makeText(context, "Plant seeds, water in time, fertilize smart, harvest mature crops, chain combos, build beauty, fulfill orders, expand land, upgrade storage, discover codex, complete tasks, claim login rewards", Toast.LENGTH_LONG).show();
    }
}
