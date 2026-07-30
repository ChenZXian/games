package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
  private GameView gameView;
  private FrameLayout menuPanel;
  private FrameLayout stagePanel;
  private FrameLayout upgradePanel;
  private FrameLayout pausePanel;
  private FrameLayout resultPanel;
  private TextView regionValue;
  private TextView stageValue;
  private TextView frontValue;
  private TextView objectiveValue;
  private TextView medalValue;
  private TextView nodeNameValue;
  private TextView nodeBonusValue;
  private TextView nodeUnitsValue;
  private TextView nodeGrowthValue;
  private TextView timeValue;
  private TextView stagePanelRegion;
  private TextView stagePanelSummary;
  private TextView stagePanelRules;
  private TextView upgradeMedalsValue;
  private TextView resultTitle;
  private TextView resultSummary;
  private TextView skillSurgeValue;
  private TextView skillBoostValue;
  private TextView skillGuardValue;
  private ProgressBar frontMeter;
  private Button btnSpeed;
  private Button btnMode;
  private Button btnUpgradeLogistics;
  private Button btnUpgradeMorale;
  private Button btnUpgradeCommand;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    gameView = findViewById(R.id.game_view);
    menuPanel = findViewById(R.id.menu_panel);
    stagePanel = findViewById(R.id.stage_panel);
    upgradePanel = findViewById(R.id.upgrade_panel);
    pausePanel = findViewById(R.id.pause_panel);
    resultPanel = findViewById(R.id.result_panel);
    regionValue = findViewById(R.id.region_value);
    stageValue = findViewById(R.id.stage_value);
    frontValue = findViewById(R.id.front_value);
    objectiveValue = findViewById(R.id.objective_value);
    medalValue = findViewById(R.id.medal_value);
    nodeNameValue = findViewById(R.id.node_name_value);
    nodeBonusValue = findViewById(R.id.node_bonus_value);
    nodeUnitsValue = findViewById(R.id.node_units_value);
    nodeGrowthValue = findViewById(R.id.node_growth_value);
    timeValue = findViewById(R.id.time_value);
    stagePanelRegion = findViewById(R.id.stage_panel_region);
    stagePanelSummary = findViewById(R.id.stage_panel_summary);
    stagePanelRules = findViewById(R.id.stage_panel_rules);
    upgradeMedalsValue = findViewById(R.id.upgrade_medals_value);
    resultTitle = findViewById(R.id.result_title);
    resultSummary = findViewById(R.id.result_summary);
    skillSurgeValue = findViewById(R.id.skill_surge_value);
    skillBoostValue = findViewById(R.id.skill_boost_value);
    skillGuardValue = findViewById(R.id.skill_guard_value);
    frontMeter = findViewById(R.id.front_meter);
    btnSpeed = findViewById(R.id.btn_speed);
    btnMode = findViewById(R.id.btn_mode);
    btnUpgradeLogistics = findViewById(R.id.btn_upgrade_logistics);
    btnUpgradeMorale = findViewById(R.id.btn_upgrade_morale);
    btnUpgradeCommand = findViewById(R.id.btn_upgrade_command);

    Button btnMenuStart = findViewById(R.id.btn_menu_start);
    Button btnMenuStageMap = findViewById(R.id.btn_menu_stage_map);
    Button btnMenuUpgrades = findViewById(R.id.btn_menu_upgrades);
    Button btnStagePrev = findViewById(R.id.btn_stage_prev);
    Button btnStageNext = findViewById(R.id.btn_stage_next);
    Button btnStagePlay = findViewById(R.id.btn_stage_play);
    Button btnStageClose = findViewById(R.id.btn_stage_close);
    Button btnUpgradeClose = findViewById(R.id.btn_upgrade_close);
    ImageButton btnPause = findViewById(R.id.btn_pause);
    ImageButton btnStageMap = findViewById(R.id.btn_stage_map);
    ImageButton btnSkillSurge = findViewById(R.id.btn_skill_surge);
    ImageButton btnSkillBoost = findViewById(R.id.btn_skill_boost);
    ImageButton btnSkillGuard = findViewById(R.id.btn_skill_guard);
    Button btnPauseResume = findViewById(R.id.btn_pause_resume);
    Button btnPauseRestart = findViewById(R.id.btn_pause_restart);
    Button btnPauseMenu = findViewById(R.id.btn_pause_menu);
    Button btnResultNext = findViewById(R.id.btn_result_next);
    Button btnResultRestart = findViewById(R.id.btn_result_restart);
    Button btnResultMenu = findViewById(R.id.btn_result_menu);

    btnMenuStart.setOnClickListener(v -> {
      hideAllPanels();
      gameView.startSelectedStage();
    });
    btnMenuStageMap.setOnClickListener(v -> showPanel(stagePanel));
    btnMenuUpgrades.setOnClickListener(v -> showPanel(upgradePanel));
    btnStagePrev.setOnClickListener(v -> {
      gameView.shiftSelectedStage(-1);
      refreshStagePanel();
    });
    btnStageNext.setOnClickListener(v -> {
      gameView.shiftSelectedStage(1);
      refreshStagePanel();
    });
    btnStagePlay.setOnClickListener(v -> {
      hideAllPanels();
      gameView.startSelectedStage();
    });
    btnStageClose.setOnClickListener(v -> showPanel(menuPanel));
    btnUpgradeClose.setOnClickListener(v -> showPanel(menuPanel));
    btnPause.setOnClickListener(v -> {
      gameView.pauseGame();
      showPanel(pausePanel);
    });
    btnStageMap.setOnClickListener(v -> {
      gameView.pauseGame();
      showPanel(stagePanel);
    });
    btnPauseResume.setOnClickListener(v -> {
      hidePanel(pausePanel);
      gameView.resumeGame();
    });
    btnPauseRestart.setOnClickListener(v -> {
      hidePanel(pausePanel);
      gameView.restartCurrentStage();
    });
    btnPauseMenu.setOnClickListener(v -> {
      hideAllPanels();
      showPanel(menuPanel);
      gameView.returnToMenu();
    });
    btnResultNext.setOnClickListener(v -> {
      hideAllPanels();
      gameView.advanceAfterResult();
    });
    btnResultRestart.setOnClickListener(v -> {
      hideAllPanels();
      gameView.restartCurrentStage();
    });
    btnResultMenu.setOnClickListener(v -> {
      hideAllPanels();
      showPanel(menuPanel);
      gameView.returnToMenu();
    });
    btnSpeed.setOnClickListener(v -> btnSpeed.setText(gameView.toggleSpeedLabel()));
    btnMode.setOnClickListener(v -> btnMode.setText(gameView.toggleSendModeLabel()));
    btnUpgradeLogistics.setOnClickListener(v -> {
      gameView.purchaseUpgrade(GameView.UpgradeType.LOGISTICS);
      refreshUpgradeButtons();
    });
    btnUpgradeMorale.setOnClickListener(v -> {
      gameView.purchaseUpgrade(GameView.UpgradeType.MORALE);
      refreshUpgradeButtons();
    });
    btnUpgradeCommand.setOnClickListener(v -> {
      gameView.purchaseUpgrade(GameView.UpgradeType.COMMAND);
      refreshUpgradeButtons();
    });
    btnSkillSurge.setOnClickListener(v -> gameView.triggerSkill(0));
    btnSkillBoost.setOnClickListener(v -> gameView.triggerSkill(1));
    btnSkillGuard.setOnClickListener(v -> gameView.triggerSkill(2));

    gameView.setHudListener(hud -> runOnUiThread(() -> {
      regionValue.setText(hud.regionLabel);
      stageValue.setText(hud.stageLabel);
      frontValue.setText(hud.frontLabel);
      objectiveValue.setText(hud.objectiveLabel);
      medalValue.setText(hud.medalLabel);
      nodeNameValue.setText(hud.nodeName);
      nodeBonusValue.setText(hud.nodeBonus);
      nodeUnitsValue.setText(hud.nodeUnits);
      nodeGrowthValue.setText(hud.nodeGrowth);
      timeValue.setText(hud.timeLabel);
      frontMeter.setProgress(hud.frontMeter);
      skillSurgeValue.setText(String.valueOf(hud.surgeCharges));
      skillBoostValue.setText(String.valueOf(hud.boostCharges));
      skillGuardValue.setText(String.valueOf(hud.guardCharges));
      stagePanelRegion.setText(hud.regionLabel);
      stagePanelSummary.setText(hud.stageSummary);
      stagePanelRules.setText(hud.stageRules);
      upgradeMedalsValue.setText(hud.medalLabel);
      refreshUpgradeButtons();
    }));

    gameView.setResultListener(result -> runOnUiThread(() -> {
      resultTitle.setText(result.victory ? R.string.label_victory : R.string.label_defeat);
      resultSummary.setText(result.summary);
      showPanel(resultPanel);
    }));

    refreshUpgradeButtons();
    refreshStagePanel();
  }

  private void refreshUpgradeButtons() {
    btnUpgradeLogistics.setText(gameView.getUpgradeLabel(GameView.UpgradeType.LOGISTICS));
    btnUpgradeMorale.setText(gameView.getUpgradeLabel(GameView.UpgradeType.MORALE));
    btnUpgradeCommand.setText(gameView.getUpgradeLabel(GameView.UpgradeType.COMMAND));
    upgradeMedalsValue.setText(gameView.getMedalLabel());
    medalValue.setText(gameView.getMedalLabel());
  }

  private void refreshStagePanel() {
    GameView.HudSnapshot hud = gameView.getHudSnapshot();
    stagePanelRegion.setText(hud.regionLabel);
    stagePanelSummary.setText(hud.stageSummary);
    stagePanelRules.setText(hud.stageRules);
  }

  private void showPanel(View panel) {
    panel.setVisibility(View.VISIBLE);
    panel.bringToFront();
  }

  private void hidePanel(View panel) {
    panel.setVisibility(View.GONE);
  }

  private void hideAllPanels() {
    hidePanel(menuPanel);
    hidePanel(stagePanel);
    hidePanel(upgradePanel);
    hidePanel(pausePanel);
    hidePanel(resultPanel);
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
