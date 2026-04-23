package com.android.boot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.AudioController;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;
import com.android.boot.ui.UiOverlayController;

public class MainActivity extends AppCompatActivity {
    private GameSession session;
    private AudioController audio;
    private GameView gameView;
    private UiOverlayController overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        session = new GameSession(this);
        audio = new AudioController();

        gameView = findViewById(R.id.game_view);
        TextView commandValue = findViewById(R.id.tv_coins);
        TextView statusValue = findViewById(R.id.tv_level);
        TextView timerValue = findViewById(R.id.tv_timer);
        TextView toolHint = findViewById(R.id.tv_tool_hint);
        TextView menuBody = findViewById(R.id.tv_menu_body);
        TextView resultTitle = findViewById(R.id.tv_result_title);
        TextView resultBody = findViewById(R.id.tv_result_body);
        FrameLayout menuOverlay = findViewById(R.id.menu_overlay);
        FrameLayout pauseOverlay = findViewById(R.id.pause_overlay);
        FrameLayout resultOverlay = findViewById(R.id.result_overlay);
        Button btnRoute = findViewById(R.id.btn_route);
        Button btnSurge = findViewById(R.id.btn_surge);
        Button btnUpgrade = findViewById(R.id.btn_upgrade);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        Button btnStartOverlay = findViewById(R.id.btn_start_overlay);
        Button btnHelpOverlay = findViewById(R.id.btn_help_overlay);
        Button btnResumeOverlay = findViewById(R.id.btn_resume_overlay);
        Button btnRestartOverlay = findViewById(R.id.btn_restart_overlay);
        Button btnMenuOverlay = findViewById(R.id.btn_menu_overlay);
        Button btnResultRestart = findViewById(R.id.btn_result_restart);
        Button btnResultMenu = findViewById(R.id.btn_result_menu);

        overlay = new UiOverlayController(
                commandValue,
                statusValue,
                timerValue,
                menuBody,
                resultTitle,
                resultBody,
                menuOverlay,
                pauseOverlay,
                resultOverlay,
                btnStartOverlay,
                btnResultRestart
        );

        gameView.bind(session, overlay);

        btnRoute.setOnClickListener(v -> {
            audio.click();
            session.setTool(GameSession.TOOL_ROUTE);
            toolHint.setText(R.string.hint_tool_route);
            overlay.sync(session);
        });

        btnSurge.setOnClickListener(v -> {
            audio.click();
            session.setTool(GameSession.TOOL_SURGE);
            toolHint.setText(R.string.hint_tool_surge);
            overlay.sync(session);
        });

        btnUpgrade.setOnClickListener(v -> {
            audio.click();
            session.setTool(GameSession.TOOL_UPGRADE);
            toolHint.setText(R.string.hint_tool_upgrade);
            overlay.sync(session);
        });

        btnPause.setOnClickListener(v -> {
            audio.click();
            if (session.state == GameState.PAUSED) {
                session.resume();
            } else if (session.state == GameState.PLAYING) {
                session.pause();
            }
            overlay.sync(session);
        });

        btnStartOverlay.setOnClickListener(v -> {
            audio.confirm();
            session.startCampaign();
            overlay.sync(session);
        });

        btnHelpOverlay.setOnClickListener(v -> {
            audio.click();
            Toast.makeText(this, R.string.body_help, Toast.LENGTH_LONG).show();
        });

        btnResumeOverlay.setOnClickListener(v -> {
            audio.confirm();
            session.resume();
            overlay.sync(session);
        });

        btnRestartOverlay.setOnClickListener(v -> {
            audio.confirm();
            session.restartStage();
            overlay.sync(session);
        });

        btnMenuOverlay.setOnClickListener(v -> {
            audio.click();
            session.returnToMenu();
            overlay.sync(session);
        });

        btnResultRestart.setOnClickListener(v -> {
            audio.confirm();
            if (session.lastStageWon && session.canAdvanceStage()) {
                session.startNextStage();
            } else {
                session.restartStage();
            }
            overlay.sync(session);
        });

        btnResultMenu.setOnClickListener(v -> {
            audio.click();
            session.returnToMenu();
            overlay.sync(session);
        });

        menuOverlay.setOnClickListener(v -> {
        });
        pauseOverlay.setOnClickListener(v -> {
        });
        resultOverlay.setOnClickListener(v -> {
        });

        session.setTool(GameSession.TOOL_ROUTE);
        toolHint.setText(R.string.hint_tool_route);
        overlay.sync(session);
    }

    @Override
    protected void onResume() {
        super.onResume();
        audio.startBgm(this);
        gameView.onHostResume();
        overlay.sync(session);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onHostPause();
        session.saveProgress();
        audio.stopBgm();
    }
}
