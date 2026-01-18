package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements GameView.Listener {
  private GameView gameView;
  private TextView textDistance;
  private TextView textScore;
  private TextView textEnergy;
  private TextView textCombo;
  private TextView textFinal;
  private TextView textBest;
  private FrameLayout panelMenu;
  private FrameLayout panelPause;
  private FrameLayout panelGameOver;
  private Button btnPause;
  private Button btnJump;
  private Button btnSkill;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_view);
    textDistance = findViewById(R.id.text_distance);
    textScore = findViewById(R.id.text_score);
    textEnergy = findViewById(R.id.text_energy);
    textCombo = findViewById(R.id.text_combo);
    textFinal = findViewById(R.id.text_final);
    textBest = findViewById(R.id.text_best);
    panelMenu = findViewById(R.id.panel_menu);
    panelPause = findViewById(R.id.panel_pause);
    panelGameOver = findViewById(R.id.panel_game_over);
    btnPause = findViewById(R.id.btn_pause);
    btnJump = findViewById(R.id.btn_jump);
    btnSkill = findViewById(R.id.btn_skill);
    Button btnStart = findViewById(R.id.btn_start);
    Button btnResume = findViewById(R.id.btn_resume);
    Button btnRestart = findViewById(R.id.btn_restart);
    Button btnRestartPause = findViewById(R.id.btn_restart_pause);

    gameView.setListener(this);

    btnStart.setOnClickListener(v -> gameView.startGame());
    btnResume.setOnClickListener(v -> gameView.resumeGame());
    btnRestart.setOnClickListener(v -> gameView.restartGame());
    btnRestartPause.setOnClickListener(v -> gameView.restartGame());
    btnPause.setOnClickListener(v -> gameView.pauseGame());

    btnJump.setOnTouchListener((v, event) -> {
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        gameView.setJumpHeld(true);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
        gameView.setJumpHeld(false);
      }
      return true;
    });

    btnSkill.setOnTouchListener((v, event) -> {
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        gameView.triggerDash();
      }
      return true;
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.onActivityResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.onActivityPause();
  }

  @Override
  public void onHudUpdate(final String distance, final String score, final String energy, final String combo) {
    runOnUiThread(() -> {
      textDistance.setText(distance);
      textScore.setText(score);
      textEnergy.setText(energy);
      textCombo.setText(combo);
    });
  }

  @Override
  public void onStateChange(final GameView.GameState state, final String finalText, final String bestText, final boolean controlsEnabled) {
    runOnUiThread(() -> {
      panelMenu.setVisibility(state == GameView.GameState.MENU ? View.VISIBLE : View.GONE);
      panelPause.setVisibility(state == GameView.GameState.PAUSED ? View.VISIBLE : View.GONE);
      panelGameOver.setVisibility(state == GameView.GameState.GAME_OVER ? View.VISIBLE : View.GONE);
      textFinal.setText(finalText);
      textBest.setText(bestText);
      btnPause.setEnabled(controlsEnabled);
      btnJump.setEnabled(controlsEnabled);
      btnSkill.setEnabled(controlsEnabled);
      float alpha = controlsEnabled ? 1f : 0.4f;
      btnPause.setAlpha(alpha);
      btnJump.setAlpha(alpha);
      btnSkill.setAlpha(alpha);
    });
  }
}
