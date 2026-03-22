package com.android.boot;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.GameState;
import com.android.boot.ui.GameHudController;
import com.android.boot.ui.GameView;
import com.android.boot.ui.OverlayController;

public class MainActivity extends AppCompatActivity implements OverlayController.Listener {
    private GameView gameView;
    private GameHudController hudController;
    private OverlayController overlayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        hudController = new GameHudController(this, findViewById(android.R.id.content), gameView);
        overlayController = new OverlayController(this, findViewById(android.R.id.content), this);
        gameView.setHudController(hudController);
        gameView.setOverlayController(overlayController);
        hudController.bind();
        overlayController.bind();
        overlayController.showState(GameState.MENU, null, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onActivityPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.release();
        }
        super.onDestroy();
    }

    @Override
    public void onStartBattle() {
        gameView.startNewMatch();
    }

    @Override
    public void onResumeBattle() {
        gameView.resumeMatch();
    }

    @Override
    public void onRestartBattle() {
        gameView.startNewMatch();
    }

    @Override
    public void onReturnToMenu() {
        gameView.returnToMenu();
    }

    @Override
    public void onToggleMute() {
        gameView.toggleMute();
        overlayController.refreshMute(gameView.isMuted());
    }

    @Override
    public void onShowHelp(boolean show) {
        overlayController.showHelp(show);
        gameView.setPaused(show || gameView.getCurrentState() == GameState.PAUSED);
    }
}
