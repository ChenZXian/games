package com.android.boot.ui;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.boot.R;
import com.android.boot.core.GameState;

public class GameHudController {
  private final View hudContainer;
  private final View controlContainer;
  private final View menuOverlay;
  private final View pauseOverlay;
  private final View gameOverOverlay;
  private final View howToPanel;
  private final TextView scoreText;
  private final TextView healthText;
  private final TextView chargeText;
  private final TextView waveText;
  private final TextView modeText;
  private final View specialFill;
  private final View specialTrack;
  private final TextView gameOverStats;
  private final Button modeButton;
  private final Button specialButton;
  private final ImageButton muteButton;

  public GameHudController(View root) {
    hudContainer = root.findViewById(R.id.hud_container);
    controlContainer = root.findViewById(R.id.control_container);
    menuOverlay = root.findViewById(R.id.menu_overlay);
    pauseOverlay = root.findViewById(R.id.pause_overlay);
    gameOverOverlay = root.findViewById(R.id.game_over_overlay);
    howToPanel = root.findViewById(R.id.how_to_panel);
    scoreText = root.findViewById(R.id.txt_score);
    healthText = root.findViewById(R.id.txt_health);
    chargeText = root.findViewById(R.id.txt_charge);
    waveText = root.findViewById(R.id.txt_wave);
    modeText = root.findViewById(R.id.txt_mode);
    specialFill = root.findViewById(R.id.special_meter_fill);
    specialTrack = root.findViewById(R.id.special_meter_track);
    gameOverStats = root.findViewById(R.id.txt_game_over_stats);
    modeButton = root.findViewById(R.id.btn_mode);
    specialButton = root.findViewById(R.id.btn_special);
    muteButton = root.findViewById(R.id.btn_mute);
    specialFill.setPivotX(0f);
  }

  public void updateHud(int score, int health, int ammo, int wave, float specialRatio, boolean specialReady, boolean pullMode) {
    scoreText.setText(String.valueOf(score));
    healthText.setText(String.valueOf(health));
    chargeText.setText(String.valueOf(ammo));
    waveText.setText(String.valueOf(wave));
    String mode = pullMode ? "PULL" : "PUSH";
    modeText.setText(mode);
    modeButton.setText(mode);
    specialFill.setScaleX(Math.max(0f, Math.min(1f, specialRatio)));
    float glow = specialReady ? 1f : 0f;
    specialTrack.setAlpha(0.85f + glow * 0.15f);
    specialButton.setEnabled(specialReady);
    specialButton.setAlpha(specialReady ? 1f : 0.55f);
  }

  public void setMuteState(boolean soundOn) {
    muteButton.setImageResource(soundOn ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
  }

  public void toggleHowToPanel() {
    howToPanel.setVisibility(howToPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
  }

  public void showState(GameState state) {
    menuOverlay.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
    pauseOverlay.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    gameOverOverlay.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    boolean playingUi = state == GameState.PLAYING || state == GameState.PAUSED;
    hudContainer.setVisibility(playingUi ? View.VISIBLE : View.GONE);
    controlContainer.setVisibility(playingUi ? View.VISIBLE : View.GONE);
    if (state != GameState.MENU) {
      howToPanel.setVisibility(View.GONE);
    }
  }

  public void showGameOver(int score, int bestScore, int wavesSurvived) {
    gameOverStats.setText("Score " + score + "\nBest " + bestScore + "\nWaves " + wavesSurvived);
    showState(GameState.GAME_OVER);
  }
}