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
  private View boardPanel;
  private View safehousePanel;
  private View pausePanel;
  private View resultPanel;
  private View gameOverPanel;
  private View helpPanel;
  private View blockHeader;
  private View dossierRail;
  private View partnerRail;
  private View combatSlab;
  private View controlsLeft;
  private View controlsRight;
  private TextView txtBlockTitle;
  private TextView txtObjective;
  private TextView txtControl;
  private TextView txtHealth;
  private TextView txtStamina;
  private TextView txtArmor;
  private TextView txtHeat;
  private TextView txtCash;
  private TextView txtWeapon;
  private TextView txtAmmo;
  private TextView txtCombo;
  private TextView txtFinisher;
  private TextView txtPartner1;
  private TextView txtPartner2;
  private TextView txtMenuSummary;
  private TextView txtBoardSelection;
  private TextView txtBoardSummary;
  private TextView txtSafehouseSummary;
  private TextView txtPauseSummary;
  private TextView txtResultSummary;
  private TextView txtGameOverSummary;
  private TextView txtHelpSummary;
  private Button btnPartner1;
  private Button btnPartner2;
  private Button btnSwap;
  private Button btnReload;
  private Button btnInteract;
  private final BgmPlayer bgmPlayer = new BgmPlayer();
  private GameState currentState = GameState.MENU;
  private GameView.UiSnapshot latestSnapshot = new GameView.UiSnapshot();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    gameView = findViewById(R.id.game_surface);

    menuPanel = findViewById(R.id.menu_panel);
    boardPanel = findViewById(R.id.board_panel);
    safehousePanel = findViewById(R.id.safehouse_panel);
    pausePanel = findViewById(R.id.pause_panel);
    resultPanel = findViewById(R.id.result_panel);
    gameOverPanel = findViewById(R.id.game_over_panel);
    helpPanel = findViewById(R.id.help_panel);
    blockHeader = findViewById(R.id.block_header);
    dossierRail = findViewById(R.id.dossier_rail);
    partnerRail = findViewById(R.id.partner_rail);
    combatSlab = findViewById(R.id.combat_slab);
    controlsLeft = findViewById(R.id.controls_left);
    controlsRight = findViewById(R.id.controls_right);

    txtBlockTitle = findViewById(R.id.txt_block_title);
    txtObjective = findViewById(R.id.txt_objective);
    txtControl = findViewById(R.id.txt_control);
    txtHealth = findViewById(R.id.txt_health);
    txtStamina = findViewById(R.id.txt_stamina);
    txtArmor = findViewById(R.id.txt_armor);
    txtHeat = findViewById(R.id.txt_heat);
    txtCash = findViewById(R.id.txt_cash);
    txtWeapon = findViewById(R.id.txt_weapon);
    txtAmmo = findViewById(R.id.txt_ammo);
    txtCombo = findViewById(R.id.txt_combo);
    txtFinisher = findViewById(R.id.txt_finisher);
    txtPartner1 = findViewById(R.id.txt_partner_1);
    txtPartner2 = findViewById(R.id.txt_partner_2);
    txtMenuSummary = findViewById(R.id.txt_menu_summary);
    txtBoardSelection = findViewById(R.id.txt_board_selection);
    txtBoardSummary = findViewById(R.id.txt_board_summary);
    txtSafehouseSummary = findViewById(R.id.txt_safehouse_summary);
    txtPauseSummary = findViewById(R.id.txt_pause_summary);
    txtResultSummary = findViewById(R.id.txt_result_summary);
    txtGameOverSummary = findViewById(R.id.txt_game_over_summary);
    txtHelpSummary = findViewById(R.id.txt_help_summary);

    btnPartner1 = findViewById(R.id.btn_partner_1);
    btnPartner2 = findViewById(R.id.btn_partner_2);
    btnSwap = findViewById(R.id.btn_swap);
    btnReload = findViewById(R.id.btn_reload);
    btnInteract = findViewById(R.id.btn_interact);

    bindHoldControl(R.id.btn_left, GameView.Control.LEFT);
    bindHoldControl(R.id.btn_right, GameView.Control.RIGHT);
    bindHoldControl(R.id.btn_jump, GameView.Control.JUMP);
    bindHoldControl(R.id.btn_light, GameView.Control.LIGHT);
    bindHoldControl(R.id.btn_heavy, GameView.Control.HEAVY);
    bindHoldControl(R.id.btn_dash, GameView.Control.DASH);
    bindHoldControl(R.id.btn_shoot, GameView.Control.SHOOT);
    bindHoldControl(R.id.btn_special, GameView.Control.SPECIAL);
    bindHoldControl(R.id.btn_interact, GameView.Control.INTERACT);

    findViewById(R.id.btn_new_campaign).setOnClickListener(v -> gameView.beginNewCampaign());
    findViewById(R.id.btn_open_board).setOnClickListener(v -> gameView.openDistrictBoard());
    findViewById(R.id.btn_open_safehouse).setOnClickListener(v -> gameView.openSafehouse());
    findViewById(R.id.btn_help).setOnClickListener(v -> showHelp(true));
    findViewById(R.id.btn_close_help).setOnClickListener(v -> showHelp(false));
    findViewById(R.id.btn_mute).setOnClickListener(v -> toggleMute());

    findViewById(R.id.btn_prev_district).setOnClickListener(v -> gameView.cycleDistrict(-1));
    findViewById(R.id.btn_next_district).setOnClickListener(v -> gameView.cycleDistrict(1));
    findViewById(R.id.btn_deploy).setOnClickListener(v -> gameView.deploySelectedDistrict());
    findViewById(R.id.btn_board_safehouse).setOnClickListener(v -> gameView.openSafehouse());
    findViewById(R.id.btn_board_menu).setOnClickListener(v -> gameView.returnToMenu());

    findViewById(R.id.btn_heal).setOnClickListener(v -> gameView.buyFieldMeds());
    findViewById(R.id.btn_ammo).setOnClickListener(v -> gameView.buyAmmoCache());
    findViewById(R.id.btn_bench).setOnClickListener(v -> gameView.upgradeWeaponBench());
    findViewById(R.id.btn_recruit).setOnClickListener(v -> gameView.recruitPartner());
    findViewById(R.id.btn_safehouse_board).setOnClickListener(v -> gameView.openDistrictBoard());

    findViewById(R.id.btn_pause).setOnClickListener(v -> gameView.pauseGame());
    findViewById(R.id.btn_resume).setOnClickListener(v -> gameView.resumeGame());
    findViewById(R.id.btn_retreat).setOnClickListener(v -> gameView.openDistrictBoard());
    findViewById(R.id.btn_pause_menu).setOnClickListener(v -> gameView.returnToMenu());

    findViewById(R.id.btn_next_block).setOnClickListener(v -> gameView.continueAfterResult());
    findViewById(R.id.btn_result_safehouse).setOnClickListener(v -> gameView.openSafehouse());
    findViewById(R.id.btn_result_board).setOnClickListener(v -> gameView.openDistrictBoard());

    findViewById(R.id.btn_retry).setOnClickListener(v -> gameView.retrySector());
    findViewById(R.id.btn_game_over_board).setOnClickListener(v -> gameView.openDistrictBoard());
    findViewById(R.id.btn_game_over_menu).setOnClickListener(v -> gameView.returnToMenu());

    btnPartner1.setOnClickListener(v -> gameView.triggerPartner(0));
    btnPartner2.setOnClickListener(v -> gameView.triggerPartner(1));
    btnSwap.setOnClickListener(v -> gameView.cycleWeapon());
    btnReload.setOnClickListener(v -> gameView.reloadWeapon());

    bgmPlayer.start(this);
    applyState(GameState.MENU);
    applySnapshot(latestSnapshot);
    gameView.setListener(this);
  }

  private void bindHoldControl(int id, GameView.Control control) {
    View view = findViewById(id);
    view.setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        v.setAlpha(0.68f);
        v.setScaleX(0.92f);
        v.setScaleY(0.92f);
        gameView.setControlState(control, true);
        return true;
      }
      if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
        v.setAlpha(1f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        gameView.setControlState(control, false);
        return true;
      }
      return false;
    });
  }

  private void toggleMute() {
    gameView.toggleMute();
    bgmPlayer.setMuted(!bgmPlayer.isMuted());
  }

  private void showHelp(boolean show) {
    helpPanel.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onStateChanged(GameState state) {
    runOnUiThread(() -> applyState(state));
  }

  @Override
  public void onSnapshot(GameView.UiSnapshot snapshot) {
    runOnUiThread(() -> applySnapshot(snapshot));
  }

  private void applySnapshot(GameView.UiSnapshot snapshot) {
    latestSnapshot = snapshot;
    if (txtBlockTitle == null) {
      return;
    }
    txtBlockTitle.setText(snapshot.blockTitle);
    txtObjective.setText(snapshot.objective);
    txtControl.setText(snapshot.control);
    txtHealth.setText(snapshot.health);
    txtStamina.setText(snapshot.stamina);
    txtArmor.setText(snapshot.armor);
    txtHeat.setText(snapshot.heat);
    txtCash.setText(snapshot.cash);
    txtWeapon.setText(snapshot.weapon);
    txtAmmo.setText(snapshot.ammo);
    txtCombo.setText(snapshot.combo);
    txtFinisher.setText(snapshot.finisher);
    txtPartner1.setText(snapshot.partnerOne);
    txtPartner2.setText(snapshot.partnerTwo);
    txtMenuSummary.setText(snapshot.menuSummary);
    txtBoardSelection.setText(snapshot.boardSelection);
    txtBoardSummary.setText(snapshot.boardSummary);
    txtSafehouseSummary.setText(snapshot.safehouseSummary);
    txtPauseSummary.setText(snapshot.pauseSummary);
    txtResultSummary.setText(snapshot.resultSummary);
    txtGameOverSummary.setText(snapshot.gameOverSummary);
    txtHelpSummary.setText(snapshot.helpSummary);
    btnPartner1.setText(snapshot.partnerOneAction);
    btnPartner2.setText(snapshot.partnerTwoAction);
    btnSwap.setText(snapshot.swapAction);
    btnReload.setText(snapshot.reloadAction);
    btnInteract.setText(snapshot.interactAction);
  }

  private void applyState(GameState state) {
    currentState = state;
    if (menuPanel == null) {
      return;
    }
    menuPanel.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
    boardPanel.setVisibility(state == GameState.DISTRICT_BOARD ? View.VISIBLE : View.GONE);
    safehousePanel.setVisibility(state == GameState.SAFEHOUSE ? View.VISIBLE : View.GONE);
    pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
    resultPanel.setVisibility(state == GameState.RESULT ? View.VISIBLE : View.GONE);
    gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);

    int hudVisibility = state == GameState.MENU ? View.GONE : View.VISIBLE;
    blockHeader.setVisibility(hudVisibility);
    dossierRail.setVisibility(hudVisibility);
    partnerRail.setVisibility(hudVisibility);
    combatSlab.setVisibility(hudVisibility);

    int controlsVisibility = state == GameState.PLAYING ? View.VISIBLE : View.GONE;
    controlsLeft.setVisibility(controlsVisibility);
    controlsRight.setVisibility(controlsVisibility);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (currentState == GameState.PLAYING) {
      gameView.pauseGame();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    bgmPlayer.stop();
  }
}
