package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

import com.android.boot.core.GameSession;

public class AchievementPanelController {
    private final Context context;
    public AchievementPanelController(Context context) { this.context = context; }
    public void show(GameSession s) { Toast.makeText(context, "Achievements " + s.achievements.names.size(), Toast.LENGTH_SHORT).show(); }
}
