package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.audio.LunarAudio;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity implements GameView.GameListener {
    private GameView gameView;
    private LunarAudio audio;
    private LinearLayout menuPanel;
    private TextView oxygenText;
    private TextView batteryText;
    private TextView partsText;
    private TextView goalText;
    private TextView statusText;
    private Button startButton;
    private Button restartButton;
    private Button pauseButton;
    private Button soundButton;
    private boolean soundOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audio = new LunarAudio(this);
        gameView = findViewById(R.id.gameView);
        menuPanel = findViewById(R.id.menuPanel);
        oxygenText = findViewById(R.id.oxygenText);
        batteryText = findViewById(R.id.batteryText);
        partsText = findViewById(R.id.partsText);
        goalText = findViewById(R.id.goalText);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        restartButton = findViewById(R.id.restartButton);
        pauseButton = findViewById(R.id.pauseButton);
        soundButton = findViewById(R.id.soundButton);
        gameView.setGameListener(this);
        startButton.setOnClickListener(v -> startGame());
        restartButton.setOnClickListener(v -> startGame());
        pauseButton.setOnClickListener(v -> togglePause());
        findViewById(R.id.scanButton).setOnClickListener(v -> gameView.scan());
        findViewById(R.id.boostButton).setOnClickListener(v -> gameView.boost());
        findViewById(R.id.helpButton).setOnClickListener(v -> showHelp());
        soundButton.setOnClickListener(v -> toggleSound());
        bindHoldButton(findViewById(R.id.leftButton), -1);
        bindHoldButton(findViewById(R.id.rightButton), 1);
        restartButton.setVisibility(View.GONE);
        gameView.showMenu();
        audio.playMenu();
    }

    private void bindHoldButton(View button, int direction) {
        button.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                gameView.setCartMove(direction);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                gameView.setCartMove(0);
                return true;
            }
            return true;
        });
    }

    private void startGame() {
        audio.playCue("ui");
        menuPanel.setVisibility(View.GONE);
        pauseButton.setText(R.string.btn_pause);
        gameView.startGame();
        audio.playGame();
    }

    private void togglePause() {
        audio.playCue("ui");
        if (gameView.isPlaying()) {
            gameView.pauseGame();
            audio.playMenu();
            menuPanel.setVisibility(View.VISIBLE);
            startButton.setText(R.string.btn_resume);
            restartButton.setVisibility(View.VISIBLE);
            statusText.setText("Paused. Resume the salvage run or restart the region.");
            pauseButton.setText(R.string.btn_resume);
        } else if (gameView.isPaused()) {
            menuPanel.setVisibility(View.GONE);
            pauseButton.setText(R.string.btn_pause);
            gameView.resumeGame();
            audio.playGame();
        }
    }

    private void showHelp() {
        audio.playCue("ui");
        statusText.setText("Move the cart, scan debris, tap the field to fire, recover O2 and power, avoid meteors, and complete repairs.");
        restartButton.setVisibility(View.VISIBLE);
    }

    private void toggleSound() {
        soundOn = !soundOn;
        audio.setEnabled(soundOn);
        soundButton.setText(soundOn ? "Sound" : "Muted");
        audio.playCue("ui");
    }

    @Override
    public void onHudChanged(String oxygen, String battery, String parts, String goal) {
        oxygenText.setText(oxygen);
        batteryText.setText(battery);
        partsText.setText(parts);
        goalText.setText(goal);
    }

    @Override
    public void onStateChanged(String state, String message, boolean finalState) {
        if (finalState) {
            audio.playMenu();
            menuPanel.setVisibility(View.VISIBLE);
            startButton.setText(R.string.btn_start);
            restartButton.setVisibility(View.VISIBLE);
            statusText.setText(message);
            pauseButton.setText(R.string.btn_pause);
        } else if ("MENU".equals(state)) {
            audio.playMenu();
            menuPanel.setVisibility(View.VISIBLE);
            startButton.setText(R.string.btn_start);
            restartButton.setVisibility(View.GONE);
            statusText.setText(message);
        }
    }

    @Override
    public void onAudioCue(String cue) {
        audio.playCue(cue);
    }

    @Override
    protected void onPause() {
        super.onPause();
        audio.pause();
        gameView.suspendLoop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeLoop();
        if (gameView.isPlaying()) {
            audio.playGame();
        } else {
            audio.playMenu();
        }
    }

    @Override
    protected void onDestroy() {
        audio.release();
        super.onDestroy();
    }
}
