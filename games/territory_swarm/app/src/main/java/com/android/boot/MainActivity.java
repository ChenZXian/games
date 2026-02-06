package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
  private GameView gameView;
  private View hudPanel;
  private FrameLayout menuPanel;
  private FrameLayout modePanel;
  private FrameLayout pausePanel;
  private FrameLayout gameOverPanel;
  private TextView ownedValue;
  private TextView totalValue;
  private TextView timeValue;
  private TextView goldValue;
  private TextView gameOverTitle;
  private Button btnSpeed;
  private Button btnSend;
  private Button btnForm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    gameView = findViewById(R.id.game_view);
    hudPanel = findViewById(R.id.hud_panel);
    menuPanel = findViewById(R.id.menu_panel);
    modePanel = findViewById(R.id.mode_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    ownedValue = findViewById(R.id.owned_value);
    totalValue = findViewById(R.id.total_value);
    timeValue = findViewById(R.id.time_value);
    goldValue = findViewById(R.id.gold_value);
    gameOverTitle = findViewById(R.id.game_over_title);
    btnSpeed = findViewById(R.id.btn_speed);
    btnSend = findViewById(R.id.btn_send);
    btnForm = findViewById(R.id.btn_form);

    // SurfaceView(GameView) has independent Surface layer, on some devices upper UI is visible but not clickable.
    // Force all panels to front and make panels clickable (to consume background clicks, buttons still receive events normally).
    if (hudPanel != null) {
      hudPanel.bringToFront();
    }
    if (menuPanel != null) {
      menuPanel.bringToFront();
      menuPanel.setClickable(true);
      menuPanel.setFocusable(true);
    }
    if (modePanel != null) {
      modePanel.bringToFront();
      modePanel.setClickable(true);
      modePanel.setFocusable(true);
    }
    if (pausePanel != null) {
      pausePanel.bringToFront();
      pausePanel.setClickable(true);
      pausePanel.setFocusable(true);
    }
    if (gameOverPanel != null) {
      gameOverPanel.bringToFront();
      gameOverPanel.setClickable(true);
      gameOverPanel.setFocusable(true);
    }

    // On very few devices/emulators, SurfaceView may still block upper control touches.
    // To ensure menu is always clickable, temporarily hide GameView when menu/mode panel is shown,
    // restore display after entering game.
    if (gameView != null && menuPanel != null && menuPanel.getVisibility() == View.VISIBLE) {
      gameView.setVisibility(View.GONE);
    }

    Button btnMenuPlay = findViewById(R.id.btn_menu_play);
    Button btnMenuMode = findViewById(R.id.btn_menu_mode);
    Button btnModeSkirmish = findViewById(R.id.btn_mode_skirmish);
    Button btnModeCampaign = findViewById(R.id.btn_mode_campaign);
    Button btnModeBack = findViewById(R.id.btn_mode_back);
    Button btnPause = findViewById(R.id.btn_pause);
    Button btnPauseResume = findViewById(R.id.btn_pause_resume);
    Button btnPauseMenu = findViewById(R.id.btn_pause_menu);
    Button btnGameRestart = findViewById(R.id.btn_game_restart);
    Button btnGameMenu = findViewById(R.id.btn_game_menu);

    btnMenuPlay.setOnClickListener(view -> {
      showPanel(menuPanel, false);
      if (gameView != null) gameView.setVisibility(View.VISIBLE);
      gameView.startSkirmish();
    });
    btnMenuMode.setOnClickListener(view -> {
      showPanel(menuPanel, false);
      showPanel(modePanel, true);
    });
    btnModeSkirmish.setOnClickListener(view -> {
      showPanel(modePanel, false);
      if (gameView != null) gameView.setVisibility(View.VISIBLE);
      gameView.startSkirmish();
    });
    btnModeCampaign.setOnClickListener(view -> {
      showPanel(modePanel, false);
      if (gameView != null) gameView.setVisibility(View.VISIBLE);
      gameView.startCampaign();
    });
    btnModeBack.setOnClickListener(view -> {
      showPanel(modePanel, false);
      showPanel(menuPanel, true);
    });
    btnPause.setOnClickListener(view -> {
      gameView.pauseGame();
      showPanel(pausePanel, true);
    });
    btnPauseResume.setOnClickListener(view -> {
      showPanel(pausePanel, false);
      gameView.resumeGame();
    });
    btnPauseMenu.setOnClickListener(view -> {
      showPanel(pausePanel, false);
      showPanel(menuPanel, true);
      gameView.returnToMenu();
    });
    btnGameRestart.setOnClickListener(view -> {
      showPanel(gameOverPanel, false);
      gameView.restartMode();
    });
    btnGameMenu.setOnClickListener(view -> {
      showPanel(gameOverPanel, false);
      showPanel(menuPanel, true);
      gameView.returnToMenu();
    });

    btnSpeed.setOnClickListener(view -> {
      float speed = gameView.toggleSpeed();
      String label = speed > 1.1f ? "2x" : "1x";
      btnSpeed.setText(label);
    });

    btnSend.setOnClickListener(view -> {
      int percent = gameView.toggleSendPercent();
      btnSend.setText(percent + "%");
    });

    btnForm.setOnClickListener(view -> {
      String label = gameView.toggleFormation();
      btnForm.setText(label);
    });

    gameView.setHudListener((owned, total, timeText, gold) -> {
      ownedValue.setText(String.valueOf(owned));
      totalValue.setText(String.valueOf(total));
      timeValue.setText(timeText);
      goldValue.setText(String.valueOf(gold));
    });

    gameView.setGameStateListener(state -> {
      if (state == GameView.GameState.WIN) {
        gameOverTitle.setText(R.string.label_victory);
        showPanel(gameOverPanel, true);
      } else if (state == GameView.GameState.LOSE) {
        gameOverTitle.setText(R.string.label_defeat);
        showPanel(gameOverPanel, true);
      }
    });
  }

  private void showPanel(View panel, boolean show) {
    panel.setVisibility(show ? View.VISIBLE : View.GONE);
    if (show) {
      panel.bringToFront();
      // Ensure menu/mode panel is not blocked by SurfaceView touches when shown
      if ((panel == menuPanel || panel == modePanel) && gameView != null) {
        gameView.setVisibility(View.GONE);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.resumeView();
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseView();
  }
}
