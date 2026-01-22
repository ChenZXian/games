package com.android.boot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  private GameView gameView;
  private View menuPanel;
  private View pausePanel;
  private View gameOverPanel;
  private View howToPanel;
  private TextView energyText;
  private TextView gameOverText;
  private ProgressBar playerBaseBar;
  private ProgressBar enemyBaseBar;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable hudRunner = new Runnable() {
    @Override
    public void run() {
      updateHud();
      handler.postDelayed(this, 100);
    }
  };
  private GameView.State lastState = GameView.State.MENU;
  private BgmPlayer bgm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    bgm = new BgmPlayer();
    gameView = findViewById(R.id.game_view);
    menuPanel = findViewById(R.id.panel_menu);
    pausePanel = findViewById(R.id.panel_pause);
    gameOverPanel = findViewById(R.id.panel_game_over);
    howToPanel = findViewById(R.id.panel_how_to);
    energyText = findViewById(R.id.text_energy);
    gameOverText = findViewById(R.id.text_game_over);
    playerBaseBar = findViewById(R.id.bar_player_base);
    enemyBaseBar = findViewById(R.id.bar_enemy_base);

    Button startButton = findViewById(R.id.btn_start);
    Button resumeButton = findViewById(R.id.btn_resume);
    Button restartButton = findViewById(R.id.btn_restart);
    Button menuButtonPause = findViewById(R.id.btn_menu_pause);
    Button menuButtonOver = findViewById(R.id.btn_menu_over);
    Button pauseButton = findViewById(R.id.btn_pause);
    Button muteButton = findViewById(R.id.btn_mute);
    Button muteButtonPause = findViewById(R.id.btn_mute_pause);
    Button howToButton = findViewById(R.id.btn_how_to_play);
    Button howToCloseButton = findViewById(R.id.btn_menu_how_to);
    Button infantryButton = findViewById(R.id.btn_spawn_infantry);
    Button rangerButton = findViewById(R.id.btn_spawn_ranger);
    Button tankButton = findViewById(R.id.btn_spawn_tank);

    startButton.setOnClickListener(v -> {
      gameView.startNewGame();
      setState(GameView.State.PLAYING);
    });

    resumeButton.setOnClickListener(v -> setState(GameView.State.PLAYING));

    restartButton.setOnClickListener(v -> {
      gameView.startNewGame();
      setState(GameView.State.PLAYING);
    });

    menuButtonPause.setOnClickListener(v -> {
      gameView.goToMenu();
      setState(GameView.State.MENU);
    });

    menuButtonOver.setOnClickListener(v -> {
      gameView.goToMenu();
      setState(GameView.State.MENU);
    });

    pauseButton.setOnClickListener(v -> {
      if (gameView.getState() == GameView.State.PLAYING) {
        setState(GameView.State.PAUSED);
      }
    });

    View.OnClickListener muteListener = v -> {
      bgm.setMuted(!bgm.isMuted());
      float alpha = bgm.isMuted() ? 0.5f : 1f;
      muteButton.setAlpha(alpha);
      muteButtonPause.setAlpha(alpha);
    };
    muteButton.setOnClickListener(muteListener);
    muteButtonPause.setOnClickListener(muteListener);

    howToButton.setOnClickListener(v -> {
      if (gameView.getState() == GameView.State.MENU) {
        howToPanel.setVisibility(View.VISIBLE);
      }
    });

    howToCloseButton.setOnClickListener(v -> howToPanel.setVisibility(View.GONE));

    infantryButton.setOnClickListener(v -> gameView.spawnPlayerUnit(GameView.UNIT_INFANTRY));
    rangerButton.setOnClickListener(v -> gameView.spawnPlayerUnit(GameView.UNIT_RANGER));
    tankButton.setOnClickListener(v -> gameView.spawnPlayerUnit(GameView.UNIT_TANK));

    playerBaseBar.setMax((int) gameView.getPlayerBaseMaxHp());
    enemyBaseBar.setMax((int) gameView.getEnemyBaseMaxHp());

    setState(GameView.State.MENU);
  }

  private void setState(GameView.State state) {
    gameView.setState(state);
    updatePanels(state);
  }

  private void updatePanels(GameView.State state) {
    menuPanel.setVisibility(state == GameView.State.MENU ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameView.State.PAUSED ? View.VISIBLE : View.GONE);
    if (state == GameView.State.GAME_OVER) {
      gameOverText.setText(gameView.isPlayerWon() ? getString(R.string.victory_text) : getString(R.string.defeat_text));
      gameOverPanel.setVisibility(View.VISIBLE);
    } else {
      gameOverPanel.setVisibility(View.GONE);
    }
    if (state != GameView.State.MENU) {
      howToPanel.setVisibility(View.GONE);
    }
  }

  private void updateHud() {
    GameView.State state = gameView.getState();
    if (state != lastState) {
      updatePanels(state);
      lastState = state;
    }
    energyText.setText(getString(R.string.energy_value, (int) gameView.getEnergy()));
    playerBaseBar.setProgress((int) gameView.getPlayerBaseHp());
    enemyBaseBar.setProgress((int) gameView.getEnemyBaseHp());
  }

  @Override
  protected void onResume() {
    super.onResume();
    bgm.start(this);
    gameView.onResumeView();
    handler.post(hudRunner);
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.onPauseView();
    handler.removeCallbacks(hudRunner);
    bgm.stop();
  }
}
