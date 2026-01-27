package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameEngine.Listener {
  private GameView gameView;
  private View menuOverlay;
  private View pauseOverlay;
  private View upgradeOverlay;
  private View gameOverOverlay;
  private View hudContainer;
  private View hpFill;
  private View energyFill;
  private TextView hudTime;
  private TextView hudKills;
  private TextView gameOverStats;
  private Button upgradeOption1;
  private Button upgradeOption2;
  private Button upgradeOption3;
  private Button muteButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_view);
    gameView.setListener(this);

    menuOverlay = findViewById(R.id.menu_overlay);
    pauseOverlay = findViewById(R.id.pause_overlay);
    upgradeOverlay = findViewById(R.id.upgrade_overlay);
    gameOverOverlay = findViewById(R.id.game_over_overlay);
    hudContainer = findViewById(R.id.hud_container);

    hpFill = findViewById(R.id.hud_hp_fill);
    energyFill = findViewById(R.id.hud_energy_fill);
    hudTime = findViewById(R.id.hud_time);
    hudKills = findViewById(R.id.hud_kills);
    gameOverStats = findViewById(R.id.game_over_stats);

    Button startButton = findViewById(R.id.btn_start);
    Button resumeButton = findViewById(R.id.btn_resume);
    Button menuButton = findViewById(R.id.btn_menu);
    Button restartButton = findViewById(R.id.btn_restart);
    ImageButton pauseButton = findViewById(R.id.btn_pause);
    Button skillButton = findViewById(R.id.btn_skill);
    Button howToPlayButton = findViewById(R.id.btn_how_to_play);
    muteButton = findViewById(R.id.btn_mute);

    upgradeOption1 = findViewById(R.id.upgrade_option_1);
    upgradeOption2 = findViewById(R.id.upgrade_option_2);
    upgradeOption3 = findViewById(R.id.upgrade_option_3);

    hpFill.setPivotX(0f);
    energyFill.setPivotX(0f);

    startButton.setOnClickListener(v -> {
      gameView.startGame();
      showMenu(false);
      showHud(true);
    });

    pauseButton.setOnClickListener(v -> {
      if (gameView.isPlaying()) {
        gameView.pauseGame();
        showPause(true);
      }
    });

    resumeButton.setOnClickListener(v -> {
      gameView.resumeGame();
      showPause(false);
    });

    menuButton.setOnClickListener(v -> {
      gameView.goToMenu();
      showPause(false);
      showGameOver(false);
      showUpgrade(false);
      showMenu(true);
      showHud(false);
    });

    restartButton.setOnClickListener(v -> {
      gameView.restartGame();
      showGameOver(false);
      showUpgrade(false);
      showHud(true);
    });

    skillButton.setOnClickListener(v -> gameView.triggerSkill());

    upgradeOption1.setOnClickListener(v -> selectUpgrade(0));
    upgradeOption2.setOnClickListener(v -> selectUpgrade(1));
    upgradeOption3.setOnClickListener(v -> selectUpgrade(2));

    howToPlayButton.setOnClickListener(v -> Toast.makeText(this,
        "Move with left joystick. Aim on right or auto aim. Shock uses energy.",
        Toast.LENGTH_LONG).show());

    muteButton.setOnClickListener(v -> {
      boolean enabled = gameView.toggleSound();
      muteButton.setText(enabled ? "Mute" : "Unmute");
    });

    showHud(false);
  }

  private void selectUpgrade(int index) {
    gameView.chooseUpgrade(index);
    showUpgrade(false);
  }

  private void showMenu(boolean show) {
    menuOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  private void showPause(boolean show) {
    pauseOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  private void showUpgrade(boolean show) {
    upgradeOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  private void showGameOver(boolean show) {
    gameOverOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  private void showHud(boolean show) {
    hudContainer.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onHudUpdate(float hp, float maxHp, float energy, float maxEnergy, int kills, int score, float time) {
    runOnUiThread(() -> {
      float hpScale = maxHp <= 0f ? 0f : Math.max(0f, Math.min(1f, hp / maxHp));
      float energyScale = maxEnergy <= 0f ? 0f : Math.max(0f, Math.min(1f, energy / maxEnergy));
      hpFill.setScaleX(hpScale);
      energyFill.setScaleX(energyScale);
      hudKills.setText("Kills " + kills + " | Score " + score);
      int minutes = (int) (time / 60f);
      int seconds = (int) (time % 60f);
      hudTime.setText(String.format("%02d:%02d", minutes, seconds));
    });
  }

  @Override
  public void onGameOver(float time, int kills, int score) {
    runOnUiThread(() -> {
      showGameOver(true);
      showHud(false);
      String stats = "Time " + formatTime(time) + "\nKills " + kills + "\nScore " + score;
      gameOverStats.setText(stats);
    });
  }

  @Override
  public void onUpgradeOptions(String[] options) {
    runOnUiThread(() -> {
      if (options.length >= 3) {
        upgradeOption1.setText(options[0]);
        upgradeOption2.setText(options[1]);
        upgradeOption3.setText(options[2]);
      }
      showUpgrade(true);
    });
  }

  @Override
  public void onStateChanged(GameState state) {
    runOnUiThread(() -> {
      if (state == GameState.MENU) {
        showMenu(true);
        showHud(false);
        showPause(false);
        showUpgrade(false);
        showGameOver(false);
      }
      if (state == GameState.PAUSED) {
        showPause(true);
      }
    });
  }

  private String formatTime(float time) {
    int minutes = (int) (time / 60f);
    int seconds = (int) (time % 60f);
    return String.format("%02d:%02d", minutes, seconds);
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseGame();
    showPause(true);
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.resumeGame();
    showPause(false);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    gameView.release();
  }
}
