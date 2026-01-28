package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
  private GameView gameView;
  private View menuPanel;
  private View pausePanel;
  private View gameOverPanel;
  private TextView textScore;
  private TextView textCombo;
  private TextView textTime;
  private TextView textParcel;
  private TextView textFinalScore;
  private Button brakeButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_view);
    menuPanel = findViewById(R.id.menu_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    textScore = findViewById(R.id.text_score);
    textCombo = findViewById(R.id.text_combo);
    textTime = findViewById(R.id.text_time);
    textParcel = findViewById(R.id.text_parcel);
    textFinalScore = findViewById(R.id.text_final_score);
    brakeButton = findViewById(R.id.btn_brake);

    Button startButton = findViewById(R.id.btn_start);
    Button resumeButton = findViewById(R.id.btn_resume);
    Button menuButton = findViewById(R.id.btn_menu);
    Button pauseButton = findViewById(R.id.btn_pause);
    Button restartButton = findViewById(R.id.btn_restart);
    Button restartMenuButton = findViewById(R.id.btn_restart_menu);

    gameView.setListener(new GameView.GameListener() {
      @Override
      public void onHudUpdate(int score, int combo, int timeLeft, String parcel, boolean brakeReady, GameState state) {
        textScore.setText(String.valueOf(score));
        textCombo.setText("x" + combo);
        textTime.setText(String.valueOf(timeLeft));
        textParcel.setText(parcel);
        brakeButton.setEnabled(brakeReady);
        if (state == GameState.PLAYING && menuPanel.getVisibility() == View.VISIBLE) {
          menuPanel.setVisibility(View.GONE);
        }
      }

      @Override
      public void onStateChanged(GameState state, int score) {
        if (state == GameState.MENU) {
          menuPanel.setVisibility(View.VISIBLE);
          pausePanel.setVisibility(View.GONE);
          gameOverPanel.setVisibility(View.GONE);
        } else if (state == GameState.PAUSED) {
          pausePanel.setVisibility(View.VISIBLE);
        } else if (state == GameState.GAME_OVER) {
          pausePanel.setVisibility(View.GONE);
          gameOverPanel.setVisibility(View.VISIBLE);
          textFinalScore.setText("Score " + score);
        } else {
          pausePanel.setVisibility(View.GONE);
          gameOverPanel.setVisibility(View.GONE);
        }
      }
    });

    startButton.setOnClickListener(v -> gameView.startGame());
    resumeButton.setOnClickListener(v -> gameView.resumeGame());
    menuButton.setOnClickListener(v -> gameView.goToMenu());
    pauseButton.setOnClickListener(v -> gameView.pauseGame());
    restartButton.setOnClickListener(v -> gameView.restartGame());
    restartMenuButton.setOnClickListener(v -> gameView.goToMenu());
    brakeButton.setOnClickListener(v -> gameView.triggerBrake());
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseGame();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }
}
