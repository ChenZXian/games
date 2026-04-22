package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.TonePlayer;
import com.android.boot.core.BattleManager;
import com.android.boot.core.CampaignProgress;
import com.android.boot.core.MatchStats;
import com.android.boot.core.MetaState;
import com.android.boot.core.RoyalOath;
import com.android.boot.ui.CampaignMapController;
import com.android.boot.ui.GameHudController;
import com.android.boot.ui.GameView;
import com.android.boot.ui.OverlayController;

public class MainActivity extends AppCompatActivity implements BattleManager.BattleEvents {
    private GameView gameView;
    private BattleManager battleManager;
    private CampaignProgress campaignProgress;
    private TonePlayer tonePlayer;
    private OverlayController overlayController;
    private CampaignMapController campaignMapController;
    private GameHudController gameHudController;
    private MetaState metaState = MetaState.MENU_HOME;
    private int selectedChapter = 1;
    private TextView prepTitle;
    private TextView prepEnemy;
    private TextView prepFeel;
    private TextView resultTitle;
    private TextView resultSummary;
    private Button muteMenu;
    private Button mutePause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        campaignProgress = new CampaignProgress(this);
        selectedChapter = campaignProgress.getSelectedChapter();
        tonePlayer = new TonePlayer();
        tonePlayer.setMuted(campaignProgress.isMuted());
        bindUi();
        updateMuteButtons();
        overlayController.showState(metaState);
        overlayController.showHelp(false);
    }

    private void bindUi() {
        gameView = findViewById(R.id.game_view);
        battleManager = gameView.getBattleManager();
        battleManager.setBattleEvents(this);
        View menuOverlay = findViewById(R.id.menu_overlay);
        View campaignOverlay = findViewById(R.id.campaign_overlay);
        View prepOverlay = findViewById(R.id.prep_overlay);
        View pauseOverlay = findViewById(R.id.pause_overlay);
        View resultOverlay = findViewById(R.id.result_overlay);
        View helpOverlay = findViewById(R.id.help_overlay);
        View hudRoot = findViewById(R.id.hud_root);
        View actionRoot = findViewById(R.id.action_root);
        overlayController = new OverlayController(menuOverlay, campaignOverlay, prepOverlay, pauseOverlay, resultOverlay, helpOverlay, hudRoot, actionRoot);

        prepTitle = findViewById(R.id.txt_prep_title);
        prepEnemy = findViewById(R.id.txt_prep_enemy);
        prepFeel = findViewById(R.id.txt_prep_feel);
        resultTitle = findViewById(R.id.txt_result_title);
        resultSummary = findViewById(R.id.txt_result_summary);
        muteMenu = findViewById(R.id.btn_menu_mute);
        mutePause = findViewById(R.id.btn_pause_mute);

        LinearLayout chapterList = findViewById(R.id.chapter_list);
        TextView campaignProgressView = findViewById(R.id.txt_campaign_progress);
        campaignMapController = new CampaignMapController(this, chapterList, campaignProgressView, campaignProgress, battleManager.getChapters(), this::openChapterPrep);

        gameHudController = new GameHudController(
                battleManager,
                findViewById(R.id.txt_player_hp),
                findViewById(R.id.txt_enemy_hp),
                findViewById(R.id.txt_phase),
                findViewById(R.id.txt_energy),
                findViewById(R.id.txt_oath_power),
                findViewById(R.id.txt_active_oath),
                findViewById(R.id.txt_status),
                (ProgressBar) findViewById(R.id.meter_energy),
                (ProgressBar) findViewById(R.id.meter_oath),
                findViewById(R.id.btn_meteor),
                findViewById(R.id.btn_chain),
                findViewById(R.id.btn_holy),
                findViewById(R.id.btn_hero_skill)
        );

        findViewById(R.id.btn_campaign).setOnClickListener(v -> showCampaignMap());
        findViewById(R.id.btn_campaign_back).setOnClickListener(v -> showMenu());
        findViewById(R.id.btn_menu_help).setOnClickListener(v -> overlayController.showHelp(true));
        findViewById(R.id.btn_help_close).setOnClickListener(v -> overlayController.showHelp(false));
        muteMenu.setOnClickListener(v -> toggleMute());
        mutePause.setOnClickListener(v -> toggleMute());
        findViewById(R.id.btn_pause).setOnClickListener(v -> pauseBattle());
        findViewById(R.id.btn_resume).setOnClickListener(v -> resumeBattle());
        findViewById(R.id.btn_pause_restart).setOnClickListener(v -> startBattle(selectedChapter));
        findViewById(R.id.btn_pause_map).setOnClickListener(v -> showCampaignMap());
        findViewById(R.id.btn_pause_menu).setOnClickListener(v -> showMenu());
        findViewById(R.id.btn_result_restart).setOnClickListener(v -> startBattle(selectedChapter));
        findViewById(R.id.btn_result_map).setOnClickListener(v -> showCampaignMap());
        findViewById(R.id.btn_result_menu).setOnClickListener(v -> showMenu());
        findViewById(R.id.btn_prep_start).setOnClickListener(v -> startBattle(selectedChapter));
        findViewById(R.id.btn_prep_back).setOnClickListener(v -> showCampaignMap());

        findViewById(R.id.btn_footman).setOnClickListener(v -> perform(battleManager.summonFootman()));
        findViewById(R.id.btn_archer).setOnClickListener(v -> perform(battleManager.summonArcher()));
        findViewById(R.id.btn_knight).setOnClickListener(v -> perform(battleManager.summonKnight()));
        findViewById(R.id.btn_cleric).setOnClickListener(v -> perform(battleManager.summonCleric()));
        findViewById(R.id.btn_war_mage).setOnClickListener(v -> perform(battleManager.summonWarMage()));
        findViewById(R.id.btn_titan).setOnClickListener(v -> perform(battleManager.summonTitan()));
        findViewById(R.id.btn_ember).setOnClickListener(v -> perform(battleManager.switchOath(RoyalOath.EMBER)));
        findViewById(R.id.btn_storm).setOnClickListener(v -> perform(battleManager.switchOath(RoyalOath.STORM)));
        findViewById(R.id.btn_sanctum).setOnClickListener(v -> perform(battleManager.switchOath(RoyalOath.SANCTUM)));
        findViewById(R.id.btn_meteor).setOnClickListener(v -> perform(battleManager.castMeteor()));
        findViewById(R.id.btn_chain).setOnClickListener(v -> perform(battleManager.castChainStorm()));
        findViewById(R.id.btn_holy).setOnClickListener(v -> perform(battleManager.castHolySurge()));
        findViewById(R.id.btn_hero_skill).setOnClickListener(v -> perform(battleManager.triggerHeroSkill()));
    }

    private void perform(boolean success) {
        if (success) {
            tonePlayer.playTap();
        }
        gameHudController.refresh();
    }

    private void showMenu() {
        metaState = MetaState.MENU_HOME;
        battleManager.pause();
        overlayController.showState(metaState);
        overlayController.showHelp(false);
    }

    private void showCampaignMap() {
        metaState = MetaState.CAMPAIGN_MAP;
        overlayController.showState(metaState);
        overlayController.showHelp(false);
        campaignMapController.rebuild();
    }

    private void openChapterPrep(int chapterIndex) {
        selectedChapter = chapterIndex;
        campaignProgress.setSelectedChapter(chapterIndex);
        prepTitle.setText(battleManager.getChapters().get(chapterIndex - 1).chapterName);
        prepEnemy.setText(battleManager.getChapters().get(chapterIndex - 1).enemyDescription);
        prepFeel.setText(battleManager.getChapters().get(chapterIndex - 1).recommendedFeel);
        metaState = MetaState.CHAPTER_PREP;
        overlayController.showState(metaState);
    }

    private void startBattle(int chapterIndex) {
        selectedChapter = chapterIndex;
        battleManager.startChapter(chapterIndex);
        metaState = MetaState.PLAYING;
        overlayController.showState(metaState);
        overlayController.showHelp(false);
        gameHudController.refresh();
        tonePlayer.playHeavy();
    }

    private void pauseBattle() {
        battleManager.pause();
        metaState = MetaState.PAUSED;
        overlayController.showState(metaState);
    }

    private void resumeBattle() {
        battleManager.resume();
        metaState = MetaState.PLAYING;
        overlayController.showState(metaState);
    }

    private void toggleMute() {
        boolean muted = !campaignProgress.isMuted();
        campaignProgress.setMuted(muted);
        tonePlayer.setMuted(muted);
        updateMuteButtons();
    }

    private void updateMuteButtons() {
        String text = campaignProgress.isMuted() ? "Unmute" : getString(R.string.btn_mute);
        if (muteMenu != null) {
            muteMenu.setText(text);
        }
        if (mutePause != null) {
            mutePause.setText(text);
        }
    }

    @Override
    public void onHudChanged() {
        runOnUiThread(() -> gameHudController.refresh());
    }

    @Override
    public void onBattleEnded(boolean victory, MatchStats stats) {
        runOnUiThread(() -> {
            metaState = MetaState.GAME_OVER;
            if (victory) {
                campaignProgress.unlockNextChapter(selectedChapter, battleManager.getChapters().size());
                resultTitle.setText("Victory");
            } else {
                resultTitle.setText("Defeat");
            }
            resultSummary.setText(
                    "Units summoned: " + stats.unitsSummoned + "\n" +
                    "Enemies defeated: " + stats.enemiesDefeated + "\n" +
                    "Spells cast: " + stats.spellsCast + "\n" +
                    "Stronghold damage: " + stats.strongholdDamage
            );
            overlayController.showState(metaState);
            campaignMapController.rebuild();
            tonePlayer.playHeavy();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPauseView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResumeView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tonePlayer.release();
    }
}
