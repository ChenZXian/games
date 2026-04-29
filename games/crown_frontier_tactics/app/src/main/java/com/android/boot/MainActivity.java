package com.android.boot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.boot.audio.GameAudio;
import com.android.boot.ui.GameView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GameView.Callbacks {
    private enum OverlayState {
        MENU,
        ATLAS,
        BRIEFING,
        PLAYING,
        PAUSED,
        RESULT,
        HELP
    }

    private static final String PREFS = "crown_frontier_tactics_prefs";
    private static final String KEY_UNLOCKED = "unlocked_chapter";
    private static final String KEY_DIFFICULTY = "difficulty_index";
    private static final String KEY_MUTED = "muted";
    private static final String KEY_DOCTRINE_POOL = "doctrine_pool";
    private static final String KEY_DOCTRINE_ECONOMY = "doctrine_economy";
    private static final String KEY_DOCTRINE_TACTICS = "doctrine_tactics";
    private static final String KEY_DOCTRINE_COMMAND = "doctrine_command";

    private GameView gameView;
    private GameAudio gameAudio;
    private SharedPreferences preferences;
    private OverlayState overlayState = OverlayState.MENU;
    private OverlayState helpReturnState = OverlayState.MENU;

    private int unlockedChapter = 1;
    private int selectedChapter = 1;
    private int difficultyIndex = 1;
    private int doctrinePool = 0;
    private int doctrineEconomy = 0;
    private int doctrineTactics = 0;
    private int doctrineCommand = 0;
    private boolean muted;

    private View hudLeft;
    private View objectiveCard;
    private View unitDrawer;
    private View turnRibbon;
    private View menuPanel;
    private View atlasPanel;
    private View briefingPanel;
    private View helpPanel;
    private View pausePanel;
    private View resultPanel;

    private TextView txtRegion;
    private TextView txtTurn;
    private TextView txtGold;
    private TextView txtIncome;
    private TextView txtDifficulty;
    private TextView txtHint;
    private TextView txtObjective;
    private TextView txtSelectedName;
    private TextView txtSelectedStats;
    private TextView txtSelectedTerrain;
    private TextView txtAtlasMeta;
    private TextView txtDoctrinePoints;
    private TextView txtBriefTitle;
    private TextView txtBriefBody;
    private TextView txtResultTitle;
    private TextView txtResultBody;

    private Button btnContinue;
    private Button btnNewCampaign;
    private Button btnDifficulty;
    private Button btnMenuMute;
    private Button btnEndTurn;
    private Button btnContext;
    private Button btnOrder;
    private Button btnMilitia;
    private Button btnArcher;
    private Button btnKnight;
    private Button btnHealer;
    private Button btnMage;
    private Button btnPause;
    private Button btnDoctrineEconomy;
    private Button btnDoctrineTactics;
    private Button btnDoctrineCommand;

    private LinearLayout chapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadPrefs();
        gameAudio = new GameAudio(this);
        gameAudio.setMuted(muted);
        bindViews();
        bindActions();
        gameView.setCallbacks(this);
        gameView.setAudio(gameAudio);
        refreshChapterList();
        refreshDoctrineLabels();
        updateDifficultyTexts();
        updateMuteText();
        showState(OverlayState.MENU);
        gameAudio.playMenuLoop();
    }

    private void bindViews() {
        gameView = findViewById(R.id.game_view);
        hudLeft = findViewById(R.id.hud_left);
        objectiveCard = findViewById(R.id.objective_card);
        unitDrawer = findViewById(R.id.unit_drawer);
        turnRibbon = findViewById(R.id.turn_ribbon);
        menuPanel = findViewById(R.id.menu_panel);
        atlasPanel = findViewById(R.id.atlas_panel);
        briefingPanel = findViewById(R.id.briefing_panel);
        helpPanel = findViewById(R.id.help_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);

        txtRegion = findViewById(R.id.txt_region);
        txtTurn = findViewById(R.id.txt_turn);
        txtGold = findViewById(R.id.txt_gold);
        txtIncome = findViewById(R.id.txt_income);
        txtDifficulty = findViewById(R.id.txt_difficulty);
        txtHint = findViewById(R.id.txt_hint);
        txtObjective = findViewById(R.id.txt_objective);
        txtSelectedName = findViewById(R.id.txt_selected_name);
        txtSelectedStats = findViewById(R.id.txt_selected_stats);
        txtSelectedTerrain = findViewById(R.id.txt_selected_terrain);
        txtAtlasMeta = findViewById(R.id.txt_atlas_meta);
        txtDoctrinePoints = findViewById(R.id.txt_doctrine_points);
        txtBriefTitle = findViewById(R.id.txt_brief_title);
        txtBriefBody = findViewById(R.id.txt_brief_body);
        txtResultTitle = findViewById(R.id.txt_result_title);
        txtResultBody = findViewById(R.id.txt_result_body);

        btnContinue = findViewById(R.id.btn_continue);
        btnNewCampaign = findViewById(R.id.btn_start);
        btnDifficulty = findViewById(R.id.btn_difficulty);
        btnMenuMute = findViewById(R.id.btn_menu_mute);
        btnEndTurn = findViewById(R.id.btn_end_turn);
        btnContext = findViewById(R.id.btn_context);
        btnOrder = findViewById(R.id.btn_order);
        btnMilitia = findViewById(R.id.btn_recruit_militia);
        btnArcher = findViewById(R.id.btn_archer);
        btnKnight = findViewById(R.id.btn_recruit_knight);
        btnHealer = findViewById(R.id.btn_recruit_healer);
        btnMage = findViewById(R.id.btn_recruit_mage);
        btnPause = findViewById(R.id.btn_pause);
        btnDoctrineEconomy = findViewById(R.id.btn_doctrine_economy);
        btnDoctrineTactics = findViewById(R.id.btn_doctrine_tactics);
        btnDoctrineCommand = findViewById(R.id.btn_doctrine_command);
        chapterList = findViewById(R.id.chapter_list);
    }

    private void bindActions() {
        btnContinue.setOnClickListener(v -> openAtlas());
        btnNewCampaign.setOnClickListener(v -> startFreshCampaign());
        btnDifficulty.setOnClickListener(v -> cycleDifficulty());
        btnMenuMute.setOnClickListener(v -> toggleMute());
        findViewById(R.id.btn_menu_help).setOnClickListener(v -> openHelp());
        findViewById(R.id.btn_help_back).setOnClickListener(v -> closeHelp());
        findViewById(R.id.btn_atlas_back).setOnClickListener(v -> showState(OverlayState.MENU));
        findViewById(R.id.btn_atlas_help).setOnClickListener(v -> openHelp());
        findViewById(R.id.btn_brief_back).setOnClickListener(v -> openAtlas());
        findViewById(R.id.btn_brief_start).setOnClickListener(v -> startChapter(selectedChapter));
        findViewById(R.id.btn_resume).setOnClickListener(v -> resumeBattle());
        findViewById(R.id.btn_restart).setOnClickListener(v -> restartBattle());
        findViewById(R.id.btn_pause_atlas).setOnClickListener(v -> openAtlas());
        findViewById(R.id.btn_pause_menu).setOnClickListener(v -> showState(OverlayState.MENU));
        findViewById(R.id.btn_result_restart).setOnClickListener(v -> startChapter(selectedChapter));
        findViewById(R.id.btn_result_atlas).setOnClickListener(v -> openAtlas());
        findViewById(R.id.btn_result_menu).setOnClickListener(v -> showState(OverlayState.MENU));

        btnEndTurn.setOnClickListener(v -> {
            gameAudio.playUiClick();
            gameView.endTurn();
        });
        btnContext.setOnClickListener(v -> {
            gameAudio.playUiClick();
            gameView.performContextAction();
        });
        btnOrder.setOnClickListener(v -> {
            gameAudio.playUiClick();
            gameView.useCommanderOrder();
        });
        btnMilitia.setOnClickListener(v -> recruit(GameView.UnitKind.MILITIA));
        btnArcher.setOnClickListener(v -> recruit(GameView.UnitKind.ARCHER));
        btnKnight.setOnClickListener(v -> recruit(GameView.UnitKind.KNIGHT));
        btnHealer.setOnClickListener(v -> recruit(GameView.UnitKind.HEALER));
        btnMage.setOnClickListener(v -> recruit(GameView.UnitKind.BATTLE_MAGE));
        btnPause.setOnClickListener(v -> pauseBattle());

        btnDoctrineEconomy.setOnClickListener(v -> spendDoctrine(0));
        btnDoctrineTactics.setOnClickListener(v -> spendDoctrine(1));
        btnDoctrineCommand.setOnClickListener(v -> spendDoctrine(2));
    }

    private void recruit(GameView.UnitKind kind) {
        gameAudio.playUiClick();
        gameView.recruit(kind);
    }

    private void startFreshCampaign() {
        unlockedChapter = 1;
        selectedChapter = 1;
        doctrinePool = 0;
        doctrineEconomy = 0;
        doctrineTactics = 0;
        doctrineCommand = 0;
        savePrefs();
        refreshDoctrineLabels();
        refreshChapterList();
        openAtlas();
    }

    private void cycleDifficulty() {
        difficultyIndex = (difficultyIndex + 1) % 3;
        updateDifficultyTexts();
        savePrefs();
        gameAudio.playUiClick();
    }

    private void toggleMute() {
        muted = !muted;
        gameAudio.setMuted(muted);
        updateMuteText();
        savePrefs();
    }

    private void updateDifficultyTexts() {
        String text = "Difficulty: " + difficultyName();
        btnDifficulty.setText(text);
        txtDifficulty.setText(difficultyName());
        txtAtlasMeta.setText(buildAtlasMetaText());
    }

    private void updateMuteText() {
        btnMenuMute.setText(muted ? "Unmute" : getString(R.string.btn_mute));
    }

    private void openAtlas() {
        gameView.pauseGame();
        refreshChapterList();
        refreshDoctrineLabels();
        txtAtlasMeta.setText(buildAtlasMetaText());
        showState(OverlayState.ATLAS);
        gameAudio.playMenuLoop();
    }

    private String buildAtlasMetaText() {
        return "Unlocked Chapters: " + unlockedChapter + " / " + gameView.getChapters().size() + "\n"
                + "Current Difficulty: " + difficultyName() + "\n"
                + "Doctrine: Economy " + doctrineEconomy + "  Tactics " + doctrineTactics + "  Command " + doctrineCommand;
    }

    private void openHelp() {
        helpReturnState = overlayState;
        showState(OverlayState.HELP);
    }

    private void closeHelp() {
        showState(helpReturnState);
    }

    private void pauseBattle() {
        if (overlayState == OverlayState.PLAYING) {
            gameView.pauseGame();
            showState(OverlayState.PAUSED);
        }
    }

    private void resumeBattle() {
        gameView.resumeGame();
        showState(OverlayState.PLAYING);
    }

    private void restartBattle() {
        startChapter(selectedChapter);
    }

    private void startChapter(int chapterIndex) {
        selectedChapter = chapterIndex;
        gameView.startChapter(chapterIndex, difficultyIndex, doctrineEconomy, doctrineTactics, doctrineCommand);
        showState(OverlayState.PLAYING);
        if (chapterIndex >= 26) {
            gameAudio.playClimaxLoop();
        } else {
            gameAudio.playGameplayLoop();
        }
    }

    private void spendDoctrine(int branch) {
        if (doctrinePool <= 0) {
            return;
        }
        doctrinePool--;
        if (branch == 0) {
            doctrineEconomy++;
        } else if (branch == 1) {
            doctrineTactics++;
        } else {
            doctrineCommand++;
        }
        savePrefs();
        refreshDoctrineLabels();
        gameAudio.playUiClick();
    }

    private void refreshDoctrineLabels() {
        txtDoctrinePoints.setText("Doctrine Points: " + doctrinePool);
        btnDoctrineEconomy.setText("Economy " + doctrineEconomy);
        btnDoctrineTactics.setText("Tactics " + doctrineTactics);
        btnDoctrineCommand.setText("Command " + doctrineCommand);
    }

    private void refreshChapterList() {
        chapterList.removeAllViews();
        List<GameView.ChapterDefinition> chapters = gameView.getChapters();
        for (GameView.ChapterDefinition chapter : chapters) {
            Button button = new Button(this);
            boolean unlocked = chapter.index <= unlockedChapter;
            button.setText(chapter.index + ". " + chapter.title + "\n" + chapter.region + "  " + chapter.objectiveLabel);
            button.setAllCaps(false);
            button.setTextColor(ContextCompat.getColor(this, unlocked ? R.color.cst_text_on_primary : R.color.cst_text_on_secondary));
            button.setBackgroundResource(unlocked ? R.drawable.ui_button_primary : R.drawable.ui_button_secondary);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(6);
            button.setLayoutParams(params);
            button.setEnabled(unlocked);
            if (unlocked) {
                button.setOnClickListener(v -> openBriefing(chapter.index));
            }
            chapterList.addView(button);
        }
    }

    private void openBriefing(int chapterIndex) {
        selectedChapter = chapterIndex;
        GameView.ChapterDefinition chapter = gameView.getChapter(chapterIndex);
        txtBriefTitle.setText(chapter.title);
        txtBriefBody.setText(chapter.briefing + "\n\nObjective: " + chapter.objectiveLabel + "\nMap: " + chapter.width + " x " + chapter.height + "\nRegion: " + chapter.region + "\nDifficulty: " + difficultyName());
        showState(OverlayState.BRIEFING);
        gameAudio.playUiClick();
    }

    private String difficultyName() {
        if (difficultyIndex == 0) {
            return "Easy";
        }
        if (difficultyIndex == 2) {
            return "Hard";
        }
        return "Normal";
    }

    private void showState(OverlayState state) {
        overlayState = state;
        boolean gameplayVisible = state == OverlayState.PLAYING || state == OverlayState.PAUSED || state == OverlayState.RESULT;
        hudLeft.setVisibility(gameplayVisible ? View.VISIBLE : View.GONE);
        objectiveCard.setVisibility(gameplayVisible ? View.VISIBLE : View.GONE);
        unitDrawer.setVisibility(gameplayVisible ? View.VISIBLE : View.GONE);
        turnRibbon.setVisibility(state == OverlayState.PLAYING ? View.VISIBLE : View.GONE);
        menuPanel.setVisibility(state == OverlayState.MENU ? View.VISIBLE : View.GONE);
        atlasPanel.setVisibility(state == OverlayState.ATLAS ? View.VISIBLE : View.GONE);
        briefingPanel.setVisibility(state == OverlayState.BRIEFING ? View.VISIBLE : View.GONE);
        helpPanel.setVisibility(state == OverlayState.HELP ? View.VISIBLE : View.GONE);
        pausePanel.setVisibility(state == OverlayState.PAUSED ? View.VISIBLE : View.GONE);
        resultPanel.setVisibility(state == OverlayState.RESULT ? View.VISIBLE : View.GONE);
        if (state == OverlayState.MENU || state == OverlayState.ATLAS || state == OverlayState.BRIEFING || state == OverlayState.HELP || state == OverlayState.RESULT) {
            gameView.pauseGame();
        }
    }

    @Override
    public void onHudChanged(GameView.HudSnapshot snapshot) {
        runOnUiThread(() -> {
            txtRegion.setText(snapshot.regionText);
            txtTurn.setText(snapshot.turnText);
            txtGold.setText(snapshot.goldText);
            txtIncome.setText(snapshot.incomeText);
            txtDifficulty.setText(snapshot.difficultyText);
            txtObjective.setText(snapshot.objectiveText);
            txtHint.setText(snapshot.hintText);
            txtSelectedName.setText(snapshot.selectedName);
            txtSelectedStats.setText(snapshot.selectedStats);
            txtSelectedTerrain.setText(snapshot.selectedTerrain);
            btnContext.setText(snapshot.contextLabel);
            btnContext.setEnabled(snapshot.contextEnabled);
            btnOrder.setEnabled(snapshot.orderEnabled);
            btnMilitia.setEnabled(snapshot.recruitEnabled);
            btnArcher.setEnabled(snapshot.recruitEnabled);
            btnKnight.setEnabled(snapshot.recruitEnabled);
            btnHealer.setEnabled(snapshot.recruitEnabled);
            btnMage.setEnabled(snapshot.recruitEnabled);
        });
    }

    @Override
    public void onBattleEnded(boolean victory, GameView.BattleReport report) {
        runOnUiThread(() -> {
            if (victory) {
                unlockedChapter = Math.min(gameView.getChapters().size(), Math.max(unlockedChapter, selectedChapter + 1));
                doctrinePool++;
                refreshDoctrineLabels();
                refreshChapterList();
                txtResultTitle.setText("Victory");
                gameAudio.playVictory();
            } else {
                txtResultTitle.setText("Defeat");
                gameAudio.playDefeat();
            }
            txtResultBody.setText(report.summaryText);
            savePrefs();
            showState(OverlayState.RESULT);
            gameAudio.playMenuLoop();
        });
    }

    private void loadPrefs() {
        unlockedChapter = preferences.getInt(KEY_UNLOCKED, 1);
        difficultyIndex = preferences.getInt(KEY_DIFFICULTY, 1);
        muted = preferences.getBoolean(KEY_MUTED, false);
        doctrinePool = preferences.getInt(KEY_DOCTRINE_POOL, 0);
        doctrineEconomy = preferences.getInt(KEY_DOCTRINE_ECONOMY, 0);
        doctrineTactics = preferences.getInt(KEY_DOCTRINE_TACTICS, 0);
        doctrineCommand = preferences.getInt(KEY_DOCTRINE_COMMAND, 0);
    }

    private void savePrefs() {
        preferences.edit()
                .putInt(KEY_UNLOCKED, unlockedChapter)
                .putInt(KEY_DIFFICULTY, difficultyIndex)
                .putBoolean(KEY_MUTED, muted)
                .putInt(KEY_DOCTRINE_POOL, doctrinePool)
                .putInt(KEY_DOCTRINE_ECONOMY, doctrineEconomy)
                .putInt(KEY_DOCTRINE_TACTICS, doctrineTactics)
                .putInt(KEY_DOCTRINE_COMMAND, doctrineCommand)
                .apply();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPauseView();
        savePrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResumeView();
        if (overlayState == OverlayState.PLAYING) {
            gameView.resumeGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savePrefs();
        gameAudio.release();
    }
}
