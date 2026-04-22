package com.android.boot.ui;

import android.widget.TextView;

import com.android.boot.core.GameSession;

public class UiOverlayController {
    private final TextView coins;
    private final TextView level;
    private final TextView timer;

    public UiOverlayController(TextView coins, TextView level, TextView timer) {
        this.coins = coins;
        this.level = level;
        this.timer = timer;
    }

    public void sync(GameSession s) {
        coins.setText("Coins " + s.progression.coins);
        level.setText("Lv " + s.progression.level + " Combo " + s.combo);
        int t = (int) s.sessionLeft;
        timer.setText(String.format("%02d:%02d", t / 60, t % 60));
    }
}
