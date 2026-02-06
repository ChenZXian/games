package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.Direction;
import com.android.boot.core.GameState;
import com.android.boot.core.StatsSnapshot;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameView.GameUiListener {
  private GameView gameView;
  private View overlayScrim;
  private LinearLayout menuPanel;
  private LinearLayout pausePanel;
  private LinearLayout gameOverPanel;
  private LinearLayout stageClearPanel;
  private View hudBar;
  private View controls;
  private TextView hudStage;
  private TextView hudScore;
  private TextView hudTime;
  private TextView hudBombs;
  private TextView hudSpeed;
  private TextView hudShield;
  private Button btnRemote;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    gameView = findViewById(R.id.game_view);
    overlayScrim = findViewById(R.id.overlay_scrim);
    menuPanel = findViewById(R.id.menu_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    stageClearPanel = findViewById(R.id.stage_clear_panel);
    hudBar = findViewById(R.id.hud_bar);
    controls = findViewById(R.id.controls);
    hudStage = findViewById(R.id.hud_stage_value);
    hudScore = findViewById(R.id.hud_score_value);
    hudTime = findViewById(R.id.hud_time_value);
    hudBombs = findViewById(R.id.hud_bombs_value);
    hudSpeed = findViewById(R.id.hud_speed_value);
    hudShield = findViewById(R.id.hud_shield_value);
    btnRemote = findViewById(R.id.btn_remote);

    gameView.setUiListener(this);

    Button btnStart = findViewById(R.id.btn_start);
    Button btnHowToPlay = findViewById(R.id.btn_how_to_play);
    Button btnResume = findViewById(R.id.btn_resume);
    Button btnRestart = findViewById(R.id.btn_restart);
    Button btnMenu = findViewById(R.id.btn_menu);
    Button btnRetry = findViewById(R.id.btn_retry);
    Button btnGameOverMenu = findViewById(R.id.btn_game_over_menu);
    Button btnNextStage = findViewById(R.id.btn_next_stage);
    Button btnStageMenu = findViewById(R.id.btn_stage_menu);
    ImageButton btnPause = findViewById(R.id.btn_pause);
    Button btnBomb = findViewById(R.id.btn_bomb);

    btnStart.setOnClickListener(v -> {
      gameView.startGame();
      updateUiForState(GameState.PLAYING);
    });

    btnHowToPlay.setOnClickListener(v -> {
      menuPanel.setVisibility(View.VISIBLE);
    });

    btnResume.setOnClickListener(v -> {
      gameView.resumeGame();
      updateUiForState(GameState.PLAYING);
    });

    btnRestart.setOnClickListener(v -> {
      gameView.restartStage();
      updateUiForState(GameState.PLAYING);
    });

    btnMenu.setOnClickListener(v -> {
      gameView.goToMenu();
      updateUiForState(GameState.MENU);
    });

    btnRetry.setOnClickListener(v -> {
      gameView.restartStage();
      updateUiForState(GameState.PLAYING);
    });

    btnGameOverMenu.setOnClickListener(v -> {
      gameView.goToMenu();
      updateUiForState(GameState.MENU);
    });

    btnNextStage.setOnClickListener(v -> {
      gameView.nextStage();
      updateUiForState(GameState.PLAYING);
    });

    btnStageMenu.setOnClickListener(v -> {
      gameView.goToMenu();
      updateUiForState(GameState.MENU);
    });

    btnPause.setOnClickListener(v -> {
      gameView.pauseGame();
      updateUiForState(GameState.PAUSED);
    });

    btnBomb.setOnClickListener(v -> gameView.pressBomb());
    btnRemote.setOnClickListener(v -> gameView.pressRemote());

    setupDpad();

    updateUiForState(GameState.MENU);
  }

  private void setupDpad() {
    Button btnUp = findViewById(R.id.btn_up);
    Button btnDown = findViewById(R.id.btn_down);
    Button btnLeft = findViewById(R.id.btn_left);
    Button btnRight = findViewById(R.id.btn_right);

    View.OnTouchListener listener = (v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        if (v.getId() == R.id.btn_up) {
          gameView.setInputDirection(Direction.UP);
        } else if (v.getId() == R.id.btn_down) {
          gameView.setInputDirection(Direction.DOWN);
        } else if (v.getId() == R.id.btn_left) {
          gameView.setInputDirection(Direction.LEFT);
        } else if (v.getId() == R.id.btn_right) {
          gameView.setInputDirection(Direction.RIGHT);
        }
      } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
        gameView.setInputDirection(null);
      }
      return false;
    };

    btnUp.setOnTouchListener(listener);
    btnDown.setOnTouchListener(listener);
    btnLeft.setOnTouchListener(listener);
    btnRight.setOnTouchListener(listener);
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseGame();
    updateUiForState(gameView.getEngine().getState());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (gameView.getEngine().getState() == GameState.PAUSED) {
      updateUiForState(GameState.PAUSED);
    }
  }

  @Override
  public void onStateChanged(GameState state) {
    updateUiForState(state);
  }

  @Override
  public void onHudUpdate(StatsSnapshot stats, boolean remoteAvailable) {
    hudStage.setText(String.valueOf(stats.stage));
    hudScore.setText(String.valueOf(stats.score));
    hudTime.setText(formatTime(stats.timeSeconds));
    hudBombs.setText(String.valueOf(stats.bombs));
    hudSpeed.setText(String.format(java.util.Locale.US, "%.2f", stats.speed));
    hudShield.setText(String.valueOf(stats.shield));
    btnRemote.setEnabled(remoteAvailable);
    btnRemote.setAlpha(remoteAvailable ? 1f : 0.5f);
  }

  private void updateUiForState(GameState state) {
    boolean playing = state == GameState.PLAYING;
    hudBar.setVisibility(playing ? View.VISIBLE : View.GONE);
    controls.setVisibility(playing ? View.VISIBLE : View.GONE);
    overlayScrim.setVisibility(playing ? View.GONE : View.VISIBLE);
    menuPanel.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    stageClearPanel.setVisibility(state == GameState.STAGE_CLEAR ? View.VISIBLE : View.GONE);
  }

  private String formatTime(int seconds) {
    int minutes = seconds / 60;
    int secs = seconds % 60;
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, secs);
  }
}
