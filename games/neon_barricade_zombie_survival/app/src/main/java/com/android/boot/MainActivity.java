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
  private TextView wallCount;
  private TextView mineCount;
  private TextView weaponCount;
  private ImageButton placeWallButton;
  private ImageButton placeMineButton;
  private ImageButton weaponButton;
  private Button upgradeOption1;
  private Button upgradeOption2;
  private Button upgradeOption3;
  private Button muteButton;
  private Button aimAssistButton;
  private BgmPlayer bgm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    bgm = new BgmPlayer();

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
    weaponButton = findViewById(R.id.btn_weapon);
    placeWallButton = findViewById(R.id.btn_place_wall);
    placeMineButton = findViewById(R.id.btn_place_mine);
    wallCount = findViewById(R.id.wall_count);
    mineCount = findViewById(R.id.mine_count);
    weaponCount = findViewById(R.id.weapon_count);
    Button howToPlayButton = findViewById(R.id.btn_how_to_play);
    muteButton = findViewById(R.id.btn_mute);
    aimAssistButton = findViewById(R.id.btn_aim_assist);

    upgradeOption1 = findViewById(R.id.upgrade_option_1);
    upgradeOption2 = findViewById(R.id.upgrade_option_2);
    upgradeOption3 = findViewById(R.id.upgrade_option_3);

    hpFill.setPivotX(0f);
    energyFill.setPivotX(0f);

    startButton.setOnClickListener(v -> {
      gameView.startGame();
      showMenu(false);
      showHud(true);
      // Initialize item count display
      wallCount.setText("3");
      mineCount.setText("3");
      weaponCount.setText("2");
      placeWallButton.setEnabled(true);
      placeMineButton.setEnabled(true);
      placeWallButton.setAlpha(1.0f);
      placeMineButton.setAlpha(1.0f);
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

    placeWallButton.setOnClickListener(v -> {
      if (gameView.getWallItemCount() > 0) {
        gameView.placeItemAtPlayer(0);
      } else {
        Toast.makeText(this, "No walls available", Toast.LENGTH_SHORT).show();
      }
    });

    placeMineButton.setOnClickListener(v -> {
      if (gameView.getMineItemCount() > 0) {
        gameView.placeItemAtPlayer(1);
      } else {
        Toast.makeText(this, "No mines available", Toast.LENGTH_SHORT).show();
      }
    });

    skillButton.setOnClickListener(v -> gameView.triggerSkill());

    weaponButton.setOnClickListener(v -> {
      com.android.boot.core.Weapon[] weapons = gameView.getOwnedWeapons();
      com.android.boot.core.Weapon current = gameView.getCurrentWeapon();
      int currentIndex = 0;
      for (int i = 0; i < weapons.length; i++) {
        if (weapons[i] != null && weapons[i] == current) {
          currentIndex = i;
          break;
        }
      }
      int nextIndex = (currentIndex + 1) % weapons.length;
      while (nextIndex != currentIndex && weapons[nextIndex] == null) {
        nextIndex = (nextIndex + 1) % weapons.length;
      }
      if (weapons[nextIndex] != null) {
        gameView.switchWeapon(nextIndex);
      }
    });

    upgradeOption1.setOnClickListener(v -> selectUpgrade(0));
    upgradeOption2.setOnClickListener(v -> selectUpgrade(1));
    upgradeOption3.setOnClickListener(v -> selectUpgrade(2));

    howToPlayButton.setOnClickListener(v -> Toast.makeText(this,
        "Move with left joystick. Aim on right or auto aim. Shock uses energy.",
        Toast.LENGTH_LONG).show());

    muteButton.setOnClickListener(v -> {
      boolean enabled = gameView.toggleSound();
      bgm.setMuted(!enabled);
      muteButton.setText(enabled ? "Mute" : "Unmute");
    });

    aimAssistButton.setOnClickListener(v -> {
      boolean enabled = gameView.isAimAssistEnabled();
      gameView.setAimAssistEnabled(!enabled);
      updateAimAssistButtonText();
    });

    // Initialize aim assist button text
    updateAimAssistButtonText();

    showHud(false);
    // Start BGM
    bgm.start(this);
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
      // Update item counts
      int wallCountValue = gameView.getWallItemCount();
      int mineCountValue = gameView.getMineItemCount();
      wallCount.setText(String.valueOf(wallCountValue));
      mineCount.setText(String.valueOf(mineCountValue));
      // Disable buttons if count is 0
      placeWallButton.setEnabled(wallCountValue > 0);
      placeMineButton.setEnabled(mineCountValue > 0);
      placeWallButton.setAlpha(wallCountValue > 0 ? 1.0f : 0.5f);
      placeMineButton.setAlpha(mineCountValue > 0 ? 1.0f : 0.5f);
      // Update weapon count
      com.android.boot.core.Weapon[] weapons = gameView.getOwnedWeapons();
      int weaponCountValue = 0;
      for (com.android.boot.core.Weapon w : weapons) {
        if (w != null) weaponCountValue++;
      }
      weaponCount.setText(String.valueOf(weaponCountValue));
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
        // Ensure BGM plays in menu
        bgm.start(this);
      }
      if (state == GameState.PAUSED) {
        showPause(true);
      }
      if (state == GameState.PLAYING) {
        // Ensure BGM plays when game starts
        bgm.start(this);
      }
    });
  }

  private void updateAimAssistButtonText() {
    boolean enabled = gameView.isAimAssistEnabled();
    aimAssistButton.setText(enabled ? "Aim Assist: ON" : "Aim Assist: OFF");
  }

  private String formatTime(float time) {
    int minutes = (int) (time / 60f);
    int seconds = (int) (time % 60f);
    return String.format("%02d:%02d", minutes, seconds);
  }

  @Override
  protected void onPause() {
    super.onPause();
    bgm.stop();
    gameView.pauseGame();
    showPause(true);
  }

  @Override
  protected void onResume() {
    super.onResume();
    bgm.start(this);
    gameView.resumeGame();
    showPause(false);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    bgm.stop();
    gameView.release();
  }
}
