package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.boot.audio.GameAudio;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
  private GameView gameView;
  private GameAudio gameAudio;
  private FrameLayout menuPanel;
  private FrameLayout chapterPanel;
  private FrameLayout upgradePanel;
  private FrameLayout helpPanel;
  private FrameLayout pausePanel;
  private FrameLayout resultPanel;
  private TextView provinceValue;
  private TextView ownedValue;
  private TextView enemyValue;
  private TextView incomeValue;
  private TextView commandValue;
  private TextView objectiveValue;
  private TextView doctrineValue;
  private TextView alertValue;
  private TextView resultTitle;
  private TextView resultSummary;
  private TextView upgradeLogisticsValue;
  private TextView upgradeCouncilValue;
  private TextView upgradeDisciplineValue;
  private Button btnRatio;
  private Button btnAbilityRally;
  private Button btnAbilityShield;
  private Button btnAbilityScout;
  private Button btnAbilityRepair;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    gameView = findViewById(R.id.game_view);
    gameAudio = new GameAudio(this);
    gameView.setAudio(gameAudio);
    menuPanel = findViewById(R.id.menu_panel);
    chapterPanel = findViewById(R.id.chapter_panel);
    upgradePanel = findViewById(R.id.upgrade_panel);
    helpPanel = findViewById(R.id.help_panel);
    pausePanel = findViewById(R.id.pause_panel);
    resultPanel = findViewById(R.id.result_panel);
    provinceValue = findViewById(R.id.province_value);
    ownedValue = findViewById(R.id.owned_value);
    enemyValue = findViewById(R.id.enemy_value);
    incomeValue = findViewById(R.id.income_value);
    commandValue = findViewById(R.id.command_value);
    objectiveValue = findViewById(R.id.objective_value);
    doctrineValue = findViewById(R.id.doctrine_value);
    alertValue = findViewById(R.id.alert_value);
    resultTitle = findViewById(R.id.result_title);
    resultSummary = findViewById(R.id.result_summary);
    upgradeLogisticsValue = findViewById(R.id.upgrade_logistics_value);
    upgradeCouncilValue = findViewById(R.id.upgrade_council_value);
    upgradeDisciplineValue = findViewById(R.id.upgrade_discipline_value);
    btnRatio = findViewById(R.id.btn_ratio);
    btnAbilityRally = findViewById(R.id.btn_ability_rally);
    btnAbilityShield = findViewById(R.id.btn_ability_shield);
    btnAbilityScout = findViewById(R.id.btn_ability_scout);
    btnAbilityRepair = findViewById(R.id.btn_ability_repair);

    gameView.setCallbacks(new GameView.Callbacks() {
      @Override
      public void onHudChanged(GameView.HudSnapshot snapshot) {
        provinceValue.setText(snapshot.provinceName);
        ownedValue.setText(snapshot.ownedText);
        enemyValue.setText(snapshot.enemyText);
        incomeValue.setText(snapshot.incomeText);
        commandValue.setText(snapshot.commandText);
        objectiveValue.setText(snapshot.objectiveText);
        doctrineValue.setText(snapshot.doctrineText);
        alertValue.setText(snapshot.alertText);
        btnAbilityRally.setText(snapshot.rallyLabel);
        btnAbilityShield.setText(snapshot.shieldLabel);
        btnAbilityScout.setText(snapshot.scoutLabel);
        btnAbilityRepair.setText(snapshot.repairLabel);
      }

      @Override
      public void onBattleEnded(GameView.ResultSnapshot snapshot) {
        resultTitle.setText(snapshot.victory ? R.string.label_victory : R.string.label_defeat);
        resultSummary.setText(snapshot.summary);
        showPanel(resultPanel, true);
        refreshUpgradeLabels();
      }

      @Override
      public void onMenuMusicNeeded(boolean menuVisible) {
        if (menuVisible) {
          gameAudio.playMenu();
        } else {
          gameAudio.playGameplay();
        }
      }
    });

    Button btnMenuCampaign = findViewById(R.id.btn_menu_campaign);
    Button btnMenuUpgrades = findViewById(R.id.btn_menu_upgrades);
    Button btnMenuHelp = findViewById(R.id.btn_menu_help);
    Button btnChapterBack = findViewById(R.id.btn_chapter_back);
    Button btnUpgradeBack = findViewById(R.id.btn_upgrade_back);
    Button btnHelpBack = findViewById(R.id.btn_help_back);
    Button btnPause = findViewById(R.id.btn_pause);
    Button btnPauseResume = findViewById(R.id.btn_pause_resume);
    Button btnPauseMenu = findViewById(R.id.btn_pause_menu);
    Button btnResultNext = findViewById(R.id.btn_result_next);
    Button btnResultMenu = findViewById(R.id.btn_result_menu);
    Button btnResultRetry = findViewById(R.id.btn_result_retry);
    Button btnUpgradeLogistics = findViewById(R.id.btn_upgrade_logistics);
    Button btnUpgradeCouncil = findViewById(R.id.btn_upgrade_council);
    Button btnUpgradeDiscipline = findViewById(R.id.btn_upgrade_discipline);

    btnMenuCampaign.setOnClickListener(v -> {
      showPanel(menuPanel, false);
      showPanel(chapterPanel, true);
    });
    btnMenuUpgrades.setOnClickListener(v -> {
      refreshUpgradeLabels();
      showPanel(menuPanel, false);
      showPanel(upgradePanel, true);
    });
    btnMenuHelp.setOnClickListener(v -> {
      showPanel(menuPanel, false);
      showPanel(helpPanel, true);
    });
    btnChapterBack.setOnClickListener(v -> {
      showPanel(chapterPanel, false);
      showPanel(menuPanel, true);
      gameAudio.playMenu();
    });
    btnUpgradeBack.setOnClickListener(v -> {
      showPanel(upgradePanel, false);
      showPanel(menuPanel, true);
    });
    btnHelpBack.setOnClickListener(v -> {
      showPanel(helpPanel, false);
      showPanel(menuPanel, true);
    });
    btnPause.setOnClickListener(v -> {
      gameView.pauseBattle();
      showPanel(pausePanel, true);
    });
    btnPauseResume.setOnClickListener(v -> {
      showPanel(pausePanel, false);
      gameView.resumeBattle();
    });
    btnPauseMenu.setOnClickListener(v -> {
      showPanel(pausePanel, false);
      gameView.returnToMenu();
      showPanel(menuPanel, true);
      gameAudio.playMenu();
    });
    btnResultNext.setOnClickListener(v -> {
      showPanel(resultPanel, false);
      if (gameView.startNextChapter()) {
        gameAudio.playGameplay();
      } else {
        showPanel(menuPanel, true);
        gameAudio.playMenu();
      }
    });
    btnResultRetry.setOnClickListener(v -> {
      showPanel(resultPanel, false);
      gameView.retryCurrentChapter();
      gameAudio.playGameplay();
    });
    btnResultMenu.setOnClickListener(v -> {
      showPanel(resultPanel, false);
      gameView.returnToMenu();
      showPanel(menuPanel, true);
      gameAudio.playMenu();
    });
    btnUpgradeLogistics.setOnClickListener(v -> {
      gameView.buyUpgrade(GameView.UpgradeBranch.LOGISTICS);
      refreshUpgradeLabels();
    });
    btnUpgradeCouncil.setOnClickListener(v -> {
      gameView.buyUpgrade(GameView.UpgradeBranch.WAR_COUNCIL);
      refreshUpgradeLabels();
    });
    btnUpgradeDiscipline.setOnClickListener(v -> {
      gameView.buyUpgrade(GameView.UpgradeBranch.IRON_DISCIPLINE);
      refreshUpgradeLabels();
    });

    btnRatio.setOnClickListener(v -> btnRatio.setText(gameView.toggleSendRatioLabel()));
    btnAbilityRally.setOnClickListener(v -> gameView.setSelectedAbility(GameView.Ability.RALLY));
    btnAbilityShield.setOnClickListener(v -> gameView.setSelectedAbility(GameView.Ability.SHIELD_WALL));
    btnAbilityScout.setOnClickListener(v -> gameView.setSelectedAbility(GameView.Ability.SCOUT_FLARE));
    btnAbilityRepair.setOnClickListener(v -> gameView.setSelectedAbility(GameView.Ability.REPAIR_BRIDGE));

    int[] chapterButtons = new int[]{
        R.id.btn_chapter_1, R.id.btn_chapter_2, R.id.btn_chapter_3, R.id.btn_chapter_4, R.id.btn_chapter_5,
        R.id.btn_chapter_6, R.id.btn_chapter_7, R.id.btn_chapter_8, R.id.btn_chapter_9, R.id.btn_chapter_10
    };
    for (int i = 0; i < chapterButtons.length; i++) {
      final int chapterIndex = i + 1;
      findViewById(chapterButtons[i]).setOnClickListener(v -> {
        showPanel(chapterPanel, false);
        gameView.startChapter(chapterIndex);
        gameAudio.playGameplay();
      });
    }

    refreshUpgradeLabels();
    showPanel(menuPanel, true);
    gameAudio.playMenu();
  }

  private void refreshUpgradeLabels() {
    upgradeLogisticsValue.setText(gameView.getUpgradeLabel(GameView.UpgradeBranch.LOGISTICS));
    upgradeCouncilValue.setText(gameView.getUpgradeLabel(GameView.UpgradeBranch.WAR_COUNCIL));
    upgradeDisciplineValue.setText(gameView.getUpgradeLabel(GameView.UpgradeBranch.IRON_DISCIPLINE));
  }

  private void showPanel(View panel, boolean visible) {
    panel.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (visible) {
      panel.bringToFront();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.resumeView();
    gameAudio.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.pauseView();
    gameAudio.onPause();
  }
}
