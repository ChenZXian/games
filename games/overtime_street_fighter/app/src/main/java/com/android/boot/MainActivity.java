package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameView.Listener {
  private GameView gameView;
  private View menuPanel;
  private View pausePanel;
  private View gameOverPanel;
  private View helpPanel;
  private TextView scoreValue;
  private TextView hpValue;
  private TextView energyValue;
  private TextView comboValue;
  private GameState currentState = GameState.MENU;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_surface);
    gameView.setListener(this);

    menuPanel = findViewById(R.id.menu_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    helpPanel = findViewById(R.id.help_panel);

    scoreValue = findViewById(R.id.value_score);
    hpValue = findViewById(R.id.value_hp);
    energyValue = findViewById(R.id.value_energy);
    comboValue = findViewById(R.id.value_combo);

    Button btnStart = findViewById(R.id.btn_start);
    Button btnHow = findViewById(R.id.btn_how_to_play);
    Button btnMute = findViewById(R.id.btn_mute);
    Button btnPause = findViewById(R.id.btn_pause);
    Button btnResume = findViewById(R.id.btn_resume);
    Button btnMenu = findViewById(R.id.btn_menu);
    Button btnRestart = findViewById(R.id.btn_restart);
    Button btnMenuOver = findViewById(R.id.btn_menu_from_over);
    Button btnCloseHelp = findViewById(R.id.btn_close_help);
    Button btnMutePause = findViewById(R.id.btn_mute_pause);

    btnStart.setOnClickListener(v -> gameView.startGame());
    btnHow.setOnClickListener(v -> showHelp(true));
    btnMute.setOnClickListener(v -> gameView.toggleMute());
    btnPause.setOnClickListener(v -> gameView.pauseGame());
    btnResume.setOnClickListener(v -> gameView.resumeGame());
    btnMenu.setOnClickListener(v -> gameView.returnToMenu());
    btnRestart.setOnClickListener(v -> gameView.restartGame());
    btnMenuOver.setOnClickListener(v -> gameView.returnToMenu());
    btnCloseHelp.setOnClickListener(v -> showHelp(false));
    btnMutePause.setOnClickListener(v -> gameView.toggleMute());

    bindControl(R.id.btn_left, GameView.Control.LEFT);
    bindControl(R.id.btn_right, GameView.Control.RIGHT);
    bindControl(R.id.btn_jump, GameView.Control.JUMP);
    bindControl(R.id.btn_light, GameView.Control.LIGHT);
    bindControl(R.id.btn_heavy, GameView.Control.HEAVY);
    bindControl(R.id.btn_kick, GameView.Control.KICK);
    bindControl(R.id.btn_guard, GameView.Control.GUARD);
    bindControl(R.id.btn_dash, GameView.Control.DASH);
    bindControl(R.id.btn_special, GameView.Control.SPECIAL);

    applyState(GameState.MENU);
  }

  private void bindControl(int id, GameView.Control control) {
    View button = findViewById(id);
    button.setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        gameView.setControlState(control, true);
        return true;
      }
      if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
        gameView.setControlState(control, false);
        return true;
      }
      return false;
    });
  }

  private void showHelp(boolean show) {
    helpPanel.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onHudUpdate(int score, int hp, int energy, int combo, int stage, int wave) {
    runOnUiThread(() -> {
      scoreValue.setText(String.valueOf(score));
      hpValue.setText(String.valueOf(hp));
      energyValue.setText(String.valueOf(energy));
      comboValue.setText("x" + combo);
    });
  }

  @Override
  public void onStateChanged(GameState state) {
    runOnUiThread(() -> applyState(state));
  }

  private void applyState(GameState state) {
    currentState = state;
    menuPanel.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (currentState == GameState.PLAYING) {
      gameView.pauseGame();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (currentState == GameState.PAUSED) {
      gameView.resumeGame();
    }
  }
}
