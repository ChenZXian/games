package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.core.MarketAudio;
import com.android.boot.ui.MarketGameView;

public class MainActivity extends Activity implements MarketGameView.GameListener {
    private MarketGameView gameView;
    private MarketAudio audio;
    private LinearLayout menuPanel;
    private LinearLayout pausePanel;
    private LinearLayout resultPanel;
    private LinearLayout upgradePanel;
    private LinearLayout seedBar;
    private TextView dayTimeText;
    private TextView revenueText;
    private TextView reputationText;
    private TextView resultTitle;
    private TextView resultBody;
    private TextView upgradeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        gameView.setGameListener(this);
        audio = new MarketAudio(this);
        menuPanel = findViewById(R.id.menu_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        upgradePanel = findViewById(R.id.upgrade_panel);
        seedBar = findViewById(R.id.seed_bar);
        dayTimeText = findViewById(R.id.day_time_text);
        revenueText = findViewById(R.id.revenue_text);
        reputationText = findViewById(R.id.reputation_text);
        resultTitle = findViewById(R.id.result_title);
        resultBody = findViewById(R.id.result_body);
        upgradeStatus = findViewById(R.id.upgrade_status);
        bindButtons();
        showMenu();
    }

    private void bindButtons() {
        findViewById(R.id.start_button).setOnClickListener(v -> startDay());
        findViewById(R.id.how_button).setOnClickListener(v -> {
            resultTitle.setText("How To Play");
            resultBody.setText("Plant crops, harvest them into the crate, tap shelves to stock goods, tap stocked shelves to change price, and keep customers happy.");
            menuPanel.setVisibility(View.GONE);
            resultPanel.setVisibility(View.VISIBLE);
            findViewById(R.id.upgrade_button).setVisibility(View.GONE);
            ((Button) findViewById(R.id.next_day_button)).setText(R.string.btn_menu);
        });
        findViewById(R.id.pause_button).setOnClickListener(v -> {
            if (gameView.isPlaying()) {
                gameView.pauseGame();
                showPause();
            }
        });
        findViewById(R.id.resume_button).setOnClickListener(v -> {
            hidePanels();
            gameView.resumeGame();
            audio.resumeBgm();
        });
        findViewById(R.id.restart_button).setOnClickListener(v -> startDay());
        findViewById(R.id.menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.next_day_button).setOnClickListener(v -> {
            if (gameView.isResultVisible()) {
                startDay();
            } else {
                showMenu();
            }
        });
        findViewById(R.id.upgrade_button).setOnClickListener(v -> showUpgrades());
        findViewById(R.id.close_upgrade).setOnClickListener(v -> startDay());
        findViewById(R.id.upgrade_plot).setOnClickListener(v -> buyUpgrade(MarketGameView.UPGRADE_PLOT));
        findViewById(R.id.upgrade_crate).setOnClickListener(v -> buyUpgrade(MarketGameView.UPGRADE_CRATE));
        findViewById(R.id.upgrade_shelf).setOnClickListener(v -> buyUpgrade(MarketGameView.UPGRADE_SHELF));
        findViewById(R.id.upgrade_water).setOnClickListener(v -> buyUpgrade(MarketGameView.UPGRADE_WATER));
        bindSeed(R.id.seed_carrot, 0);
        bindSeed(R.id.seed_tomato, 1);
        bindSeed(R.id.seed_corn, 2);
        bindSeed(R.id.seed_berry, 3);
        bindSeed(R.id.seed_pumpkin, 4);
        bindSeed(R.id.seed_herb, 5);
    }

    private void bindSeed(int id, int crop) {
        findViewById(id).setOnClickListener(v -> gameView.selectSeed(crop));
    }

    private void buyUpgrade(int type) {
        gameView.buyUpgrade(type);
        updateUpgradeStatus();
    }

    private void startDay() {
        hidePanels();
        seedBar.setVisibility(View.VISIBLE);
        findViewById(R.id.upgrade_button).setVisibility(View.VISIBLE);
        ((Button) findViewById(R.id.next_day_button)).setText(R.string.btn_next_day);
        audio.playGameBgm();
        gameView.startDay();
    }

    private void showMenu() {
        gameView.stopToMenu();
        audio.playMenuBgm();
        seedBar.setVisibility(View.GONE);
        menuPanel.setVisibility(View.VISIBLE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        upgradePanel.setVisibility(View.GONE);
    }

    private void showPause() {
        audio.pauseBgm();
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.VISIBLE);
        resultPanel.setVisibility(View.GONE);
        upgradePanel.setVisibility(View.GONE);
    }

    private void showUpgrades() {
        gameView.pauseGame();
        audio.pauseBgm();
        hidePanels();
        upgradePanel.setVisibility(View.VISIBLE);
        updateUpgradeStatus();
    }

    private void hidePanels() {
        menuPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        upgradePanel.setVisibility(View.GONE);
    }

    private void updateUpgradeStatus() {
        upgradeStatus.setText("Coins " + gameView.getBankCoins() + "  Plot " + gameView.getUpgradeLevel(MarketGameView.UPGRADE_PLOT) + "  Crate " + gameView.getUpgradeLevel(MarketGameView.UPGRADE_CRATE) + "  Shelf " + gameView.getUpgradeLevel(MarketGameView.UPGRADE_SHELF) + "  Water " + gameView.getUpgradeLevel(MarketGameView.UPGRADE_WATER));
    }

    @Override
    public void onHudChanged(String dayTime, String revenue, String reputation) {
        dayTimeText.setText(dayTime);
        revenueText.setText(revenue);
        reputationText.setText(reputation);
    }

    @Override
    public void onDayFinished(boolean won, String summary) {
        if (won) {
            audio.playWinBgm();
        } else {
            audio.playFailBgm();
        }
        resultTitle.setText(won ? "Market Cleared" : "Market Closed");
        resultBody.setText(summary);
        hidePanels();
        resultPanel.setVisibility(View.VISIBLE);
        seedBar.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
        audio.pauseBgm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pausePanel != null && pausePanel.getVisibility() != View.VISIBLE && menuPanel != null && menuPanel.getVisibility() != View.VISIBLE && resultPanel != null && resultPanel.getVisibility() != View.VISIBLE && upgradePanel != null && upgradePanel.getVisibility() != View.VISIBLE) {
            gameView.resumeGame();
            audio.resumeBgm();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameView.shutdown();
        audio.release();
    }
}
