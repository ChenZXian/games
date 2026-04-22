package com.android.boot.ui;

import android.view.View;
import android.widget.TextView;

import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;

import java.util.Locale;

public class UiOverlayController {
    private final TextView coins;
    private final TextView level;
    private final TextView timer;
    private final View menuOverlay;
    private final View pauseOverlay;
    private final View resultOverlay;
    private final TextView resultBody;

    public UiOverlayController(TextView coins, TextView level, TextView timer, View menuOverlay, View pauseOverlay, View resultOverlay, TextView resultBody) {
        this.coins = coins;
        this.level = level;
        this.timer = timer;
        this.menuOverlay = menuOverlay;
        this.pauseOverlay = pauseOverlay;
        this.resultOverlay = resultOverlay;
        this.resultBody = resultBody;
    }

    public void sync(GameSession s) {
        coins.setText(String.valueOf(s.progression.coins));
        level.setText("Lv " + s.progression.level + " Combo " + s.combo);
        int t = (int) s.sessionLeft;
        timer.setText(String.format(Locale.US, "%02d:%02d", t / 60, t % 60));
        menuOverlay.setVisibility(s.state == GameState.MENU ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(s.state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        resultOverlay.setVisibility(s.state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
        resultBody.setText(String.format(Locale.US, "Coins %d\nCombo %d\nBest beauty %d", s.progression.coins, s.combo, s.bestBeauty));
    }
}
