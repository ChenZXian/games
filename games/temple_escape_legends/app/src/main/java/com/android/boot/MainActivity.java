package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.engine.GameMode;
import com.android.boot.engine.GameState;
import com.android.boot.ui.GameView;
import com.android.boot.ui.HudBinder;
import com.android.boot.ui.OverlayController;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private OverlayController overlays;
    private HudBinder hudBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.gameView);
        hudBinder = new HudBinder(findViewById(R.id.hudRoot));
        overlays = new OverlayController(findViewById(R.id.overlayRoot), gameView, this::startRun);
        gameView.setHudBinder(hudBinder);
        gameView.setOverlayCallback(overlays::showState);
        bindButtons();
        overlays.showState(GameState.MENU);
    }

    private void bindButtons() {
        Button left = findViewById(R.id.btnLeft);
        Button right = findViewById(R.id.btnRight);
        Button up = findViewById(R.id.btnUp);
        Button down = findViewById(R.id.btnDown);
        left.setOnClickListener(v -> gameView.onControlLeft());
        right.setOnClickListener(v -> gameView.onControlRight());
        up.setOnClickListener(v -> gameView.onControlUp());
        down.setOnClickListener(v -> gameView.onControlDown());

        findViewById(R.id.btnStart).setOnClickListener(v -> overlays.showState(GameState.MODE_SELECT));
        findViewById(R.id.btnEndless).setOnClickListener(v -> startRun(GameMode.ENDLESS, 0));
        findViewById(R.id.btnStageMode).setOnClickListener(v -> overlays.showState(GameState.STAGE_SELECT));
        findViewById(R.id.btnUpgrades).setOnClickListener(v -> overlays.showUpgrade());
        findViewById(R.id.btnResume).setOnClickListener(v -> gameView.resumeRun());
        findViewById(R.id.btnRestart).setOnClickListener(v -> gameView.restartRun());
        findViewById(R.id.btnMenu).setOnClickListener(v -> overlays.showState(GameState.MENU));
        findViewById(R.id.btnMenu2).setOnClickListener(v -> overlays.showState(GameState.MENU));
        findViewById(R.id.btnMenu3).setOnClickListener(v -> overlays.showState(GameState.MENU));
        findViewById(R.id.btnPause).setOnClickListener(v -> overlays.showState(GameState.PAUSED));
        findViewById(R.id.btnNextStage).setOnClickListener(v -> startRun(GameMode.STAGE, Math.min(6, gameView.getCurrentStage() + 1)));
        findViewById(R.id.btnRevive).setOnClickListener(v -> gameView.acceptRevive());

        LinearLayout stageList = findViewById(R.id.stageList);
        for (int i = 1; i <= 6; i++) {
            final int idx = i;
            Button b = new Button(this, null, 0, R.style.Widget_Game_Button_Secondary);
            b.setText(getString(R.string.label_stage) + " " + i);
            b.setOnClickListener(v -> startRun(GameMode.STAGE, idx));
            stageList.addView(b);
        }

        TextView how = findViewById(R.id.howText);
        how.setText("Use Left Right Up Down to move jump and slide. Avoid hazards, collect coins, control boss pressure, clear stages.");
        findViewById(R.id.btnHowToPlay).setOnClickListener(v -> overlays.showHowToPlay());
        findViewById(R.id.btnCloseHow).setOnClickListener(v -> overlays.closeHowToPlay());
    }

    private void startRun(GameMode mode, int stage) {
        gameView.startRun(mode, stage);
        overlays.showState(GameState.PLAYING);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onHostPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }
}
