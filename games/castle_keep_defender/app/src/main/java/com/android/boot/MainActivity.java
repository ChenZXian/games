package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
  private GameView gameView;
  private LinearLayout menuPanel;
  private LinearLayout pausePanel;
  private LinearLayout gameOverPanel;
  private LinearLayout upgradePanel;
  private LinearLayout skillRow;
  private ImageButton btnPause;
  private ImageButton btnMute;
  private TextView hudWave;
  private TextView hudScore;
  private TextView hudGold;
  private TextView hudHp;
  private Button btnOil;
  private Button btnFreeze;
  private Button btnPush;
  private Button btnUpgradeA;
  private Button btnUpgradeB;
  private Button btnUpgradeC;
  private BgmPlayer bgmPlayer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    bgmPlayer = new BgmPlayer();
    bindViews();
    bindButtons();
    gameView.setListener(new GameView.Listener() {
      @Override
      public void onHud(GameEngine.HudData hud, float oilCd, float freezeCd, float pushCd) {
        runOnUiThread(() -> {
          hudWave.setText(getString(R.string.label_wave) + " " + hud.wave);
          hudScore.setText(getString(R.string.label_score) + " " + hud.score);
          hudGold.setText(getString(R.string.label_gold) + " " + hud.gold + "  " + getString(R.string.label_energy) + " " + hud.energy);
          hudHp.setText(getString(R.string.label_hp) + " " + hud.hp + "/" + hud.maxHp);
          updateSkillLabel(btnOil, getString(R.string.btn_oil), oilCd);
          updateSkillLabel(btnFreeze, getString(R.string.btn_freeze), freezeCd);
          updateSkillLabel(btnPush, getString(R.string.btn_push), pushCd);
        });
      }

      @Override
      public void onState(GameState state, boolean upgradeVisible, GameEngine.UpgradeChoice choice) {
        runOnUiThread(() -> {
          if (upgradeVisible) {
            upgradePanel.setVisibility(View.VISIBLE);
            btnUpgradeA.setText(choice.a);
            btnUpgradeB.setText(choice.b);
            btnUpgradeC.setText(choice.c);
          } else {
            upgradePanel.setVisibility(View.GONE);
          }
          renderState(state);
        });
      }
    });
    renderState(GameState.MENU);
    bgmPlayer.start(this);
  }

  private void bindViews() {
    gameView = findViewById(R.id.game_view);
    menuPanel = findViewById(R.id.menu_panel);
    pausePanel = findViewById(R.id.pause_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    upgradePanel = findViewById(R.id.upgrade_panel);
    skillRow = findViewById(R.id.skill_row);
    btnPause = findViewById(R.id.btn_pause);
    btnMute = findViewById(R.id.btn_mute);
    hudWave = findViewById(R.id.hud_wave);
    hudScore = findViewById(R.id.hud_score);
    hudGold = findViewById(R.id.hud_gold);
    hudHp = findViewById(R.id.hud_hp);
    btnOil = findViewById(R.id.btn_skill_oil);
    btnFreeze = findViewById(R.id.btn_skill_freeze);
    btnPush = findViewById(R.id.btn_skill_push);
    btnUpgradeA = findViewById(R.id.btn_upgrade_a);
    btnUpgradeB = findViewById(R.id.btn_upgrade_b);
    btnUpgradeC = findViewById(R.id.btn_upgrade_c);
    updateMuteButton();
  }

  private void bindButtons() {
    findViewById(R.id.btn_start).setOnClickListener(v -> gameView.getEngine().startGame());
    findViewById(R.id.btn_how).setOnClickListener(v -> {
    });
    findViewById(R.id.btn_resume).setOnClickListener(v -> gameView.getEngine().resume());
    findViewById(R.id.btn_menu).setOnClickListener(v -> gameView.getEngine().goToMenu());
    findViewById(R.id.btn_restart).setOnClickListener(v -> gameView.getEngine().startGame());
    findViewById(R.id.btn_game_over_menu).setOnClickListener(v -> gameView.getEngine().goToMenu());
    btnPause.setOnClickListener(v -> gameView.getEngine().pause());
    btnOil.setOnClickListener(v -> gameView.getEngine().useOil());
    btnFreeze.setOnClickListener(v -> gameView.getEngine().useFreeze());
    btnPush.setOnClickListener(v -> gameView.getEngine().usePush(gameView.getLastAimX(), gameView.getLastAimY()));
    btnUpgradeA.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(0));
    btnUpgradeB.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(1));
    btnUpgradeC.setOnClickListener(v -> gameView.getEngine().chooseUpgrade(2));
    if (btnMute != null) {
      btnMute.setOnClickListener(v -> {
        bgmPlayer.setMuted(!bgmPlayer.isMuted());
        updateMuteButton();
      });
    }
  }

  private void renderState(GameState state) {
    boolean playing = state == GameState.PLAYING;
    menuPanel.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    skillRow.setVisibility(playing ? View.VISIBLE : View.GONE);
    btnPause.setVisibility(playing ? View.VISIBLE : View.GONE);
  }

  private void updateSkillLabel(Button button, String base, float ratio) {
    if (ratio <= 0f) {
      button.setText(base);
    } else {
      int pct = (int) (ratio * 100f);
      button.setText(base + " " + pct + "%");
    }
  }

  private void updateMuteButton() {
    if (btnMute != null) {
      btnMute.setImageResource(bgmPlayer.isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseLoop();
    if (bgmPlayer != null) {
      bgmPlayer.pause();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.resumeLoop();
    if (bgmPlayer != null && !bgmPlayer.isMuted()) {
      bgmPlayer.resume();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (bgmPlayer != null) {
      bgmPlayer.stop();
    }
  }
}
