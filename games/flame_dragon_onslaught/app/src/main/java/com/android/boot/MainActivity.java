package com.android.boot;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.ui.GameView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GameView.Listener {
    private GameView gameView;
    private View overlayRoot;
    private View menuPanel;
    private View pausePanel;
    private View resultPanel;
    private View helpPanel;
    private View upgradesPanel;
    private View chaptersPanel;
    private ProgressBar healthMeter;
    private ProgressBar bossMeter;
    private TextView healthValue;
    private TextView stageName;
    private TextView scoreValue;
    private TextView comboValue;
    private TextView timerValue;
    private TextView objectiveBanner;
    private TextView resultTitle;
    private TextView resultBody;
    private TextView resultStats;
    private TextView medalBank;
    private View bossPanel;
    private MaterialButton muteButton;
    private final GameView.UpgradeProfile upgradeProfile = new GameView.UpgradeProfile();
    private final StageProgress stageProgress = new StageProgress();
    private final List<UpgradeBinding> upgradeBindings = new ArrayList<>();
    private boolean muted;
    private int currentStageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        buildUpgradeRows();
        buildChapterRows();
        gameView.setListener(this);
        gameView.applyUpgradeProfile(upgradeProfile);
        refreshUpgradeTexts();
        refreshMuteLabel();
        showPanel(menuPanel);
    }

    private void bindViews() {
        gameView = findViewById(R.id.game_view);
        overlayRoot = findViewById(R.id.overlay_root);
        menuPanel = findViewById(R.id.menu_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        helpPanel = findViewById(R.id.help_panel);
        upgradesPanel = findViewById(R.id.upgrades_panel);
        chaptersPanel = findViewById(R.id.chapters_panel);
        healthMeter = findViewById(R.id.health_meter);
        bossMeter = findViewById(R.id.boss_meter);
        healthValue = findViewById(R.id.health_value);
        stageName = findViewById(R.id.stage_name);
        scoreValue = findViewById(R.id.score_value);
        comboValue = findViewById(R.id.combo_value);
        timerValue = findViewById(R.id.timer_value);
        objectiveBanner = findViewById(R.id.objective_banner);
        resultTitle = findViewById(R.id.result_title);
        resultBody = findViewById(R.id.result_body);
        resultStats = findViewById(R.id.result_stats);
        medalBank = findViewById(R.id.medal_bank);
        bossPanel = findViewById(R.id.boss_panel);
        muteButton = findViewById(R.id.menu_mute);

        findViewById(R.id.menu_start).setOnClickListener(v -> openStageSelection(true));
        findViewById(R.id.menu_chapters).setOnClickListener(v -> openStageSelection(false));
        findViewById(R.id.menu_upgrades).setOnClickListener(v -> showPanel(upgradesPanel));
        findViewById(R.id.menu_help).setOnClickListener(v -> showPanel(helpPanel));
        muteButton.setOnClickListener(v -> toggleMute());
        findViewById(R.id.help_close).setOnClickListener(v -> showPanel(menuPanel));
        findViewById(R.id.upgrades_close).setOnClickListener(v -> showPanel(menuPanel));
        findViewById(R.id.chapters_close).setOnClickListener(v -> showPanel(menuPanel));
        findViewById(R.id.pause_button).setOnClickListener(v -> {
            if (gameView.isStageActive()) {
                gameView.pauseStage();
                showPanel(pausePanel);
            }
        });
        findViewById(R.id.pause_resume).setOnClickListener(v -> {
            hideOverlay();
            gameView.resumeStage();
        });
        findViewById(R.id.pause_restart).setOnClickListener(v -> {
            hideOverlay();
            gameView.startStage(currentStageIndex);
        });
        findViewById(R.id.pause_menu).setOnClickListener(v -> {
            gameView.stopStage();
            showPanel(menuPanel);
        });
        findViewById(R.id.result_continue).setOnClickListener(v -> openStageSelection(false));
        findViewById(R.id.result_retry).setOnClickListener(v -> {
            hideOverlay();
            gameView.startStage(currentStageIndex);
        });

        setupHoldButton(findViewById(R.id.btn_left), pressed -> gameView.setLeftPressed(pressed));
        setupHoldButton(findViewById(R.id.btn_right), pressed -> gameView.setRightPressed(pressed));
        setupHoldButton(findViewById(R.id.btn_jump), pressed -> {
            if (pressed) {
                gameView.pressJump();
            }
        });
        setupHoldButton(findViewById(R.id.btn_light), pressed -> {
            if (pressed) {
                gameView.pressLightAttack();
            }
        });
        setupHoldButton(findViewById(R.id.btn_heavy), pressed -> {
            if (pressed) {
                gameView.pressHeavyAttack();
            }
        });
        setupHoldButton(findViewById(R.id.btn_skill), pressed -> {
            if (pressed) {
                gameView.pressFlameSkill();
            }
        });
        setupHoldButton(findViewById(R.id.btn_overdrive), pressed -> {
            if (pressed) {
                gameView.pressOverdrive();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupHoldButton(View view, PressHandler handler) {
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                handler.onChange(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
                handler.onChange(false);
            }
            return false;
        });
    }

    private void buildUpgradeRows() {
        LinearLayout container = findViewById(R.id.upgrade_container);
        container.removeAllViews();
        upgradeBindings.clear();
        upgradeBindings.add(createUpgradeRow(container, getString(R.string.label_upgrade_vitality), () -> upgradeProfile.buy(0)));
        upgradeBindings.add(createUpgradeRow(container, getString(R.string.label_upgrade_heat), () -> upgradeProfile.buy(1)));
        upgradeBindings.add(createUpgradeRow(container, getString(R.string.label_upgrade_skill), () -> upgradeProfile.buy(2)));
        upgradeBindings.add(createUpgradeRow(container, getString(R.string.label_upgrade_drive), () -> upgradeProfile.buy(3)));
    }

    private UpgradeBinding createUpgradeRow(LinearLayout container, String title, UpgradeAction action) {
        TextView titleView = new TextView(this);
        TextView bodyView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.cst_text_primary));
        titleView.setTextSize(18f);
        bodyView.setTextColor(getColor(R.color.cst_text_secondary));
        bodyView.setTextSize(14f);
        MaterialButton button = new MaterialButton(this, null, 0);
        button.setText("Upgrade");
        button.setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Button);
        button.setBackgroundResource(R.drawable.ui_button_primary);
        button.setTextColor(getColor(R.color.cst_text_on_primary));
        button.setOnClickListener(v -> {
            if (action.apply()) {
                gameView.applyUpgradeProfile(upgradeProfile);
                refreshUpgradeTexts();
            }
        });
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundResource(R.drawable.ui_card);
        LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        shellParams.bottomMargin = 12;
        shell.setLayoutParams(shellParams);
        shell.setPadding(20, 20, 20, 20);
        shell.addView(titleView);
        shell.addView(bodyView);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 12;
        button.setLayoutParams(btnParams);
        shell.addView(button);
        container.addView(shell);
        return new UpgradeBinding(titleView, bodyView, button, title);
    }

    private void buildChapterRows() {
        LinearLayout container = findViewById(R.id.chapters_container);
        container.removeAllViews();
        List<GameView.StageEntry> entries = GameView.getStageEntries();
        int chapter = -1;
        LinearLayout row = null;
        for (int i = 0; i < entries.size(); i++) {
            GameView.StageEntry entry = entries.get(i);
            if (entry.chapter != chapter) {
                chapter = entry.chapter;
                TextView chapterTitle = new TextView(this);
                chapterTitle.setText(String.format(Locale.US, "Chapter %d  %s", entry.chapter, entry.chapterName));
                chapterTitle.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6);
                chapterTitle.setTextColor(getColor(R.color.cst_text_primary));
                chapterTitle.setPadding(0, 12, 0, 10);
                container.addView(chapterTitle);
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                container.addView(row);
            }
            MaterialButton button = new MaterialButton(this, null, 0);
            button.setBackgroundResource(R.drawable.ui_button_secondary);
            button.setTextColor(getColor(R.color.cst_text_on_secondary));
            button.setText(String.format(Locale.US, "%d-%d", entry.chapter, entry.stageInChapter));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.rightMargin = 8;
            button.setLayoutParams(params);
            int stageIndex = i;
            button.setOnClickListener(v -> launchStage(stageIndex));
            if (row != null) {
                row.addView(button);
            }
        }
        refreshChapterButtons();
    }

    private void refreshChapterButtons() {
        LinearLayout container = findViewById(R.id.chapters_container);
        int stageIndex = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View buttonView = row.getChildAt(j);
                    if (buttonView instanceof MaterialButton) {
                        MaterialButton button = (MaterialButton) buttonView;
                        boolean unlocked = stageIndex <= stageProgress.highestUnlocked;
                        button.setEnabled(unlocked);
                        if (unlocked) {
                            int bestRank = stageProgress.bestRanks[stageIndex];
                            button.setText(String.format(Locale.US, "%s %s", GameView.getStageEntries().get(stageIndex).shortCode(), rankText(bestRank)));
                        } else {
                            button.setText(getString(R.string.label_locked));
                        }
                        stageIndex++;
                    }
                }
            }
        }
    }

    private String rankText(int rank) {
        if (rank >= 3) {
            return "S";
        }
        if (rank == 2) {
            return "A";
        }
        if (rank == 1) {
            return "B";
        }
        if (rank == 0) {
            return "C";
        }
        return "--";
    }

    private void launchStage(int stageIndex) {
        currentStageIndex = stageIndex;
        hideOverlay();
        gameView.applyUpgradeProfile(upgradeProfile);
        gameView.startStage(stageIndex);
    }

    private void openStageSelection(boolean quickStart) {
        if (quickStart) {
            launchStage(stageProgress.highestUnlocked);
        } else {
            showPanel(chaptersPanel);
        }
    }

    private void toggleMute() {
        muted = !muted;
        gameView.setMuted(muted);
        refreshMuteLabel();
    }

    private void refreshMuteLabel() {
        muteButton.setText(muted ? "Sound Off" : "Sound On");
    }

    private void refreshUpgradeTexts() {
        medalBank.setText(String.format(Locale.US, "Ember Medals: %d", upgradeProfile.medals));
        for (int i = 0; i < upgradeBindings.size(); i++) {
            UpgradeBinding binding = upgradeBindings.get(i);
            int level = upgradeProfile.levels[i];
            int cost = 20 + level * 15;
            binding.body.setText(String.format(Locale.US, "Level %d   Cost %d", level, cost));
            binding.button.setEnabled(upgradeProfile.medals >= cost && level < 8);
            if (level >= 8) {
                binding.body.setText(String.format(Locale.US, "Level %d   Maxed", level));
            }
        }
    }

    private void hideOverlay() {
        overlayRoot.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        upgradesPanel.setVisibility(View.GONE);
        chaptersPanel.setVisibility(View.GONE);
    }

    private void showPanel(View panel) {
        hideOverlay();
        overlayRoot.setVisibility(View.VISIBLE);
        panel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }

    @Override
    protected void onPause() {
        gameView.onHostPause();
        super.onPause();
    }

    @Override
    public void onHudUpdated(GameView.HudSnapshot hud) {
        runOnUiThread(() -> {
            healthMeter.setProgress(Math.max(0, Math.min(100, hud.healthPercent)));
            bossMeter.setProgress(Math.max(0, Math.min(100, hud.bossPercent)));
            healthValue.setText(hud.healthText);
            stageName.setText(hud.stageTitle);
            scoreValue.setText(hud.scoreText);
            comboValue.setText(hud.comboText);
            timerValue.setText(hud.timerText);
            objectiveBanner.setText(hud.objectiveText);
            bossPanel.setVisibility(hud.showBoss ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onStageFinished(GameView.StageResult result) {
        runOnUiThread(() -> {
            if (result.victory) {
                stageProgress.highestUnlocked = Math.min(GameView.getStageEntries().size() - 1, Math.max(stageProgress.highestUnlocked, result.stageIndex + 1));
            }
            stageProgress.bestRanks[result.stageIndex] = Math.max(stageProgress.bestRanks[result.stageIndex], result.rankScore);
            upgradeProfile.medals += result.medalsEarned;
            refreshUpgradeTexts();
            refreshChapterButtons();
            resultTitle.setText(result.victory ? getString(R.string.label_victory) : getString(R.string.label_defeat));
            resultBody.setText(result.summary);
            resultStats.setText(String.format(Locale.US, "Score %d  |  Medals %d  |  Rank %s", result.score, result.medalsEarned, rankText(result.rankScore)));
            showPanel(resultPanel);
        });
    }

    private interface PressHandler {
        void onChange(boolean pressed);
    }

    private interface UpgradeAction {
        boolean apply();
    }

    private static class UpgradeBinding {
        final TextView title;
        final TextView body;
        final MaterialButton button;
        final String label;

        UpgradeBinding(TextView title, TextView body, MaterialButton button, String label) {
            this.title = title;
            this.body = body;
            this.button = button;
            this.label = label;
        }
    }

    private static class StageProgress {
        int highestUnlocked;
        final int[] bestRanks = new int[24];
    }
}
