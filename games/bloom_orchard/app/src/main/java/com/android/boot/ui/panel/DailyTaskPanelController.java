package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

import com.android.boot.core.GameSession;

public class DailyTaskPanelController {
    private final Context context;
    public DailyTaskPanelController(Context context) { this.context = context; }
    public void show(GameSession s) { Toast.makeText(context, "Daily tasks " + s.tasks.tasks.size(), Toast.LENGTH_SHORT).show(); }
}
