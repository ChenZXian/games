package com.android.boot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameHudController;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameView.GameEvents {
  private GameView gameView;
  private GameHudController hudController;
  private ImageButton muteButton;
  private ImageButton helpButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_view);
    hudController = new GameHudController(findViewById(android.R.id.content));
    gameView.setGameEvents(this);

    Button startButton = findViewById(R.id.btn_start);
    Button resumeButton = findViewById(R.id.btn_resume);
    Button pauseMenuButton = findViewById(R.id.btn_pause_menu);
    Button restartButton = findViewById(R.id.btn_restart);
    Button gameOverMenuButton = findViewById(R.id.btn_game_over_menu);
    Button modeButton = findViewById(R.id.btn_mode);
    Button specialButton = findViewById(R.id.btn_special);
    ImageButton pauseButton = findViewById(R.id.btn_pause);
    muteButton = findViewById(R.id.btn_mute);
    helpButton = findViewById(R.id.btn_help);

    startButton.setOnClickListener(v -> {
      gameView.startRun();
      syncState(GameState.PLAYING);
    });
    resumeButton.setOnClickListener(v -> {
      gameView.resumeRun();
      syncState(GameState.PLAYING);
    });
    pauseMenuButton.setOnClickListener(v -> {
      gameView.returnToMenu();
      syncState(GameState.MENU);
    });
    restartButton.setOnClickListener(v -> {
      gameView.restartRun();
      syncState(GameState.PLAYING);
    });
    gameOverMenuButton.setOnClickListener(v -> {
      gameView.returnToMenu();
      syncState(GameState.MENU);
    });
    pauseButton.setOnClickListener(v -> {
      gameView.pauseRun();
      syncState(GameState.PAUSED);
    });
    modeButton.setOnClickListener(v -> gameView.toggleMode());
    specialButton.setOnClickListener(v -> gameView.triggerSpecial());
    muteButton.setOnClickListener(v -> {
      boolean soundOn = gameView.toggleSound();
      hudController.setMuteState(soundOn);
    });
    helpButton.setOnClickListener(v -> hudController.toggleHowToPanel());

    hudController.setMuteState(true);
    syncState(GameState.MENU);
  }

  private void syncState(GameState state) {
    hudController.showState(state);
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.handleHostPause();
    if (gameView.getCurrentState() == GameState.PLAYING) {
      syncState(GameState.PAUSED);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.handleHostResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    gameView.shutdown();
  }

  @Override
  public void onHudChanged(int score, int health, int ammo, int wave, float specialRatio, boolean specialReady, boolean pullMode) {
    runOnUiThread(() -> hudController.updateHud(score, health, ammo, wave, specialRatio, specialReady, pullMode));
  }

  @Override
  public void onStateChanged(GameState state) {
    runOnUiThread(() -> syncState(state));
  }

  @Override
  public void onGameOver(int score, int bestScore, int wavesSurvived) {
    runOnUiThread(() -> hudController.showGameOver(score, bestScore, wavesSurvived));
  }
}
