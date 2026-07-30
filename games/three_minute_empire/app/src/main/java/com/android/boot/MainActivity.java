package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.boot.audio.TonePlayer;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity implements GameView.HudListener {
    private GameView gameView;
    private TonePlayer tonePlayer;
    private FrameLayout menuOverlay;
    private FrameLayout pauseOverlay;
    private FrameLayout resultOverlay;
    private TextView timerText;
    private TextView scoreText;
    private TextView supplyText;
    private TextView selectedText;
    private TextView hintText;
    private TextView resultTitle;
    private TextView resultBody;
    private ImageButton muteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tonePlayer = new TonePlayer();
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        timerText = findViewById(R.id.tv_timer);
        scoreText = findViewById(R.id.tv_score);
        supplyText = findViewById(R.id.tv_supply);
        selectedText = findViewById(R.id.tv_selected);
        hintText = findViewById(R.id.tv_hint);
        resultTitle = findViewById(R.id.tv_result_title);
        resultBody = findViewById(R.id.tv_result_body);
        muteButton = findViewById(R.id.btn_mute);
        Button start = findViewById(R.id.btn_start_overlay);
        Button help = findViewById(R.id.btn_help_overlay);
        Button resume = findViewById(R.id.btn_resume_overlay);
        Button restart = findViewById(R.id.btn_restart_overlay);
        Button resultRestart = findViewById(R.id.btn_result_restart);
        Button resultMenu = findViewById(R.id.btn_result_menu);
        Button upgrade = findViewById(R.id.btn_upgrade);
        Button surge = findViewById(R.id.btn_surge);
        Button repair = findViewById(R.id.btn_repair);
        ImageButton pause = findViewById(R.id.btn_pause);
        gameView.setHudListener(this);
        gameView.setTonePlayer(tonePlayer);
        start.setOnClickListener(v -> startGame());
        help.setOnClickListener(v -> showHelp());
        pause.setOnClickListener(v -> pauseGame());
        resume.setOnClickListener(v -> resumeGame());
        restart.setOnClickListener(v -> restartGame());
        resultRestart.setOnClickListener(v -> restartGame());
        resultMenu.setOnClickListener(v -> showMenu());
        upgrade.setOnClickListener(v -> gameView.upgradeSelected());
        surge.setOnClickListener(v -> gameView.activateSurge());
        repair.setOnClickListener(v -> gameView.activateRepair());
        muteButton.setOnClickListener(v -> toggleMute());
    }

    private void startGame() {
        menuOverlay.setVisibility(View.GONE);
        resultOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        gameView.startMatch();
    }

    private void showHelp() {
        selectedText.setText("How To Play");
        hintText.setText("Drag from teal nodes to connected gray or red nodes. Captured gray points become your color and raise supply income.");
    }

    private void pauseGame() {
        gameView.pauseMatch();
        pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void resumeGame() {
        pauseOverlay.setVisibility(View.GONE);
        gameView.resumeMatch();
    }

    private void restartGame() {
        resultOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        menuOverlay.setVisibility(View.GONE);
        gameView.startMatch();
    }

    private void showMenu() {
        resultOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        menuOverlay.setVisibility(View.VISIBLE);
        gameView.showMenuState();
    }

    private void toggleMute() {
        tonePlayer.setMuted(!tonePlayer.isMuted());
        muteButton.setImageResource(tonePlayer.isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
    }

    @Override
    public void onHudChanged(String timer, String score, String supply, String selected, String hint) {
        runOnUiThread(() -> {
            timerText.setText(timer);
            scoreText.setText(score);
            supplyText.setText(supply);
            selectedText.setText(selected);
            hintText.setText(hint);
        });
    }

    @Override
    public void onMatchEnded(boolean win, String summary) {
        runOnUiThread(() -> {
            resultTitle.setText(win ? "Victory" : "Defeat");
            resultBody.setText(summary);
            resultOverlay.setVisibility(View.VISIBLE);
        });
        tonePlayer.result(win);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseMatch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (menuOverlay.getVisibility() != View.VISIBLE && resultOverlay.getVisibility() != View.VISIBLE && pauseOverlay.getVisibility() != View.VISIBLE) {
            gameView.resumeMatch();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tonePlayer.release();
    }
}
