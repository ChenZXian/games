package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.audio.MinecartAudio;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity implements GameView.GameListener {
    private GameView gameView;
    private MinecartAudio audio;
    private LinearLayout menuPanel;
    private TextView durabilityText;
    private TextView cargoText;
    private TextView heatText;
    private TextView targetText;
    private TextView laneText;
    private TextView routeText;
    private TextView warningText;
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
        audio = new MinecartAudio(this);
        gameView = findViewById(R.id.gameView);
        menuPanel = findViewById(R.id.menuPanel);
        durabilityText = findViewById(R.id.durabilityText);
        cargoText = findViewById(R.id.cargoText);
        heatText = findViewById(R.id.heatText);
        targetText = findViewById(R.id.targetText);
        laneText = findViewById(R.id.laneText);
        routeText = findViewById(R.id.routeText);
        warningText = findViewById(R.id.warningText);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        restartButton = findViewById(R.id.restartButton);
        pauseButton = findViewById(R.id.pauseButton);
        soundButton = findViewById(R.id.soundButton);
        gameView.setGameListener(this);
        startButton.setOnClickListener(v -> startOrResume());
        restartButton.setOnClickListener(v -> startGame());
        pauseButton.setOnClickListener(v -> togglePause());
        helpButton().setOnClickListener(v -> showHelp());
        soundButton.setOnClickListener(v -> toggleSound());
        restartButton.setVisibility(View.GONE);
        gameView.showMenu();
        audio.playMenu();
    }

    private Button helpButton() {
        return findViewById(R.id.helpButton);
    }

    private void startOrResume() {
        if (gameView.isPaused()) {
            audio.playCue("ui");
            menuPanel.setVisibility(View.GONE);
            pauseButton.setText(R.string.btn_pause);
            gameView.resumeGame();
            audio.playGame();
        } else {
            startGame();
        }
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
            statusText.setText("Paused at the signal board.");
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
        statusText.setText("Left half fires left hook. Right half fires right hook. Swipe up or down to change rails. Pull barricades before impact.");
        restartButton.setVisibility(View.VISIBLE);
    }

    private void toggleSound() {
        soundOn = !soundOn;
        audio.setEnabled(soundOn);
        soundButton.setText(soundOn ? "Sound" : "Muted");
        audio.playCue("ui");
    }

    @Override
    public void onHudChanged(String durability, String cargo, String heat, String target, String lane, String route, String warning) {
        durabilityText.setText(durability);
        cargoText.setText(cargo);
        heatText.setText(heat);
        targetText.setText(target);
        laneText.setText(lane);
        routeText.setText(route);
        warningText.setText(warning);
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
