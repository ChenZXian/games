package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

import com.android.boot.core.GameSession;

public class LoginRewardPanelController {
    private final Context context;
    public LoginRewardPanelController(Context context) { this.context = context; }
    public void show(GameSession s) {
        int day = s.login.claimableDay();
        Toast.makeText(context, day > 0 ? "Login reward day " + day : "Reward already claimed", Toast.LENGTH_SHORT).show();
    }
}
