package com.android.boot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.SoundController;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;
import com.android.boot.ui.UiOverlayController;
import com.android.boot.ui.panel.MenuPanelController;

public class MainActivity extends AppCompatActivity {
    private GameSession session;
    private SoundController sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        session = new GameSession(this);
        GameView gameView = findViewById(R.id.game_view);
        TextView coins = findViewById(R.id.tv_coins);
        TextView level = findViewById(R.id.tv_level);
        TextView timer = findViewById(R.id.tv_timer);
        TextView beauty = findViewById(R.id.tv_beauty);
        TextView toolHint = findViewById(R.id.tv_tool_hint);
        FrameLayout menuOverlay = findViewById(R.id.menu_overlay);
        FrameLayout pauseOverlay = findViewById(R.id.pause_overlay);
        FrameLayout resultOverlay = findViewById(R.id.result_overlay);
        TextView resultBody = findViewById(R.id.tv_result_body);
        UiOverlayController overlay = new UiOverlayController(coins, level, timer, beauty, menuOverlay, pauseOverlay, resultOverlay, resultBody);
        gameView.bind(session, overlay);

        sound = new SoundController(this);
        MenuPanelController menu = new MenuPanelController(this);

        Button btnSeed = findViewById(R.id.btn_seed);
        Button btnWater = findViewById(R.id.btn_water);
        Button btnFertilizer = findViewById(R.id.btn_fertilizer);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        Button btnStartOverlay = findViewById(R.id.btn_start_overlay);
        Button btnHelpOverlay = findViewById(R.id.btn_help_overlay);
        Button btnResumeOverlay = findViewById(R.id.btn_resume_overlay);
        Button btnRestartOverlay = findViewById(R.id.btn_restart_overlay);
        Button btnMenuOverlay = findViewById(R.id.btn_menu_overlay);
        Button btnResultRestart = findViewById(R.id.btn_result_restart);
        Button btnResultMenu = findViewById(R.id.btn_result_menu);

        btnSeed.setOnClickListener(v -> {
            gameView.setTool(0);
            toolHint.setText(R.string.hint_tool_seed);
            sound.click();
        });
        btnWater.setOnClickListener(v -> {
            gameView.setTool(1);
            toolHint.setText(R.string.hint_tool_water);
            sound.click();
        });
        btnFertilizer.setOnClickListener(v -> {
            gameView.setTool(2);
            toolHint.setText(R.string.hint_tool_fertilizer);
            sound.click();
        });
        btnPause.setOnClickListener(v -> {
            sound.click();
            session.state = session.state == GameState.PAUSED ? GameState.PLAYING : GameState.PAUSED;
        });
        btnStartOverlay.setOnClickListener(v -> {
            sound.click();
            session.start();
        });
        btnHelpOverlay.setOnClickListener(v -> {
            sound.click();
            menu.showHowToPlay();
        });
        btnResumeOverlay.setOnClickListener(v -> {
            sound.click();
            session.state = GameState.PLAYING;
        });
        btnRestartOverlay.setOnClickListener(v -> {
            sound.click();
            session.start();
        });
        btnMenuOverlay.setOnClickListener(v -> {
            sound.click();
            session.state = GameState.MENU;
        });
        btnResultRestart.setOnClickListener(v -> {
            sound.click();
            session.start();
        });
        btnResultMenu.setOnClickListener(v -> {
            sound.click();
            session.state = GameState.MENU;
        });
        menuOverlay.setOnClickListener(v -> { });
        pauseOverlay.setOnClickListener(v -> { });
        resultOverlay.setOnClickListener(v -> { });
        toolHint.setText(R.string.hint_tool_seed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sound != null) {
            sound.resumeBgm();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sound != null) {
            sound.pauseBgm();
        }
        session.progression.save();
        session.storage.save();
    }

    @Override
    protected void onDestroy() {
        if (sound != null) {
            sound.release();
        }
        super.onDestroy();
    }
}
