package com.android.boot.ui;

import android.view.View;
import android.widget.TextView;
import com.android.boot.R;
import com.android.boot.engine.GameMode;
import com.android.boot.engine.GameState;

public class OverlayController {
    public interface StartRunAction {
        void start(GameMode mode, int stage);
    }

    private final View root;
    private final GameView gameView;
    private final StartRunAction action;

    public OverlayController(View root, GameView gameView, StartRunAction action) {
        this.root = root;
        this.gameView = gameView;
        this.action = action;
    }

    public void showState(GameState state) {
        hideAll();
        if (state == GameState.MENU) root.findViewById(R.id.menuOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.MODE_SELECT) root.findViewById(R.id.modeOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.STAGE_SELECT) root.findViewById(R.id.stageOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.PAUSED) root.findViewById(R.id.pauseOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.GAME_OVER) root.findViewById(R.id.gameOverOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.STAGE_CLEAR) root.findViewById(R.id.stageClearOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.REVIVE_PROMPT) root.findViewById(R.id.reviveOverlay).setVisibility(View.VISIBLE);
        if (state == GameState.PLAYING) hideAll();
    }

    public void showUpgrade() {
        hideAll();
        root.findViewById(R.id.upgradeOverlay).setVisibility(View.VISIBLE);
    }

    public void showHowToPlay() {
        root.findViewById(R.id.howOverlay).setVisibility(View.VISIBLE);
    }

    public void closeHowToPlay() {
        root.findViewById(R.id.howOverlay).setVisibility(View.GONE);
    }

    private void hideAll() {
        int[] ids = new int[] {R.id.menuOverlay, R.id.modeOverlay, R.id.stageOverlay, R.id.upgradeOverlay, R.id.pauseOverlay, R.id.gameOverOverlay, R.id.stageClearOverlay, R.id.reviveOverlay};
        for (int id : ids) root.findViewById(id).setVisibility(View.GONE);
    }
}
