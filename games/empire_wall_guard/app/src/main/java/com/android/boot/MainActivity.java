package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.boot.audio.GameAudio;
import com.android.boot.ui.GameView;

import java.util.Locale;

public class MainActivity extends Activity {
    private GameView gameView;
    private TextView gateText;
    private TextView supplyText;
    private TextView waveText;
    private TextView statusText;
    private TextView resultTitleText;
    private TextView resultStatsText;
    private View menuPanel;
    private View helpPanel;
    private View pausePanel;
    private View resultPanel;
    private View topHud;
    private View buildPanel;
    private Button soundButton;
    private GameAudio audio;
    private boolean soundOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        bindViews();
        audio = new GameAudio(this);
        bindGame();
        bindButtons();
        showMenu();
    }

    private void bindViews() {
        gameView = findViewById(R.id.game_view);
        gateText = findViewById(R.id.gate_text);
        supplyText = findViewById(R.id.supply_text);
        waveText = findViewById(R.id.wave_text);
        statusText = findViewById(R.id.status_text);
        resultTitleText = findViewById(R.id.result_title_text);
        resultStatsText = findViewById(R.id.result_stats_text);
        menuPanel = findViewById(R.id.menu_panel);
        helpPanel = findViewById(R.id.help_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        topHud = findViewById(R.id.top_hud);
        buildPanel = findViewById(R.id.build_panel);
        soundButton = findViewById(R.id.sound_button);
    }

    private void bindGame() {
        gameView.setListener(new GameView.GameListener() {
            @Override
            public void onHudChanged(int gate, int supplies, int wave, int maxWave, int breaches, String status) {
                gateText.setText(String.format(Locale.US, "Gate %d", gate));
                supplyText.setText(String.format(Locale.US, "Supplies %d", supplies));
                waveText.setText(String.format(Locale.US, "Wave %d/%d", wave, maxWave));
                statusText.setText(String.format(Locale.US, "%s  Breaches %d", status, breaches));
            }

            @Override
            public void onRunEnded(boolean cleared, int wave, int breaches, int stars, int gate) {
                audio.playSfx(cleared ? "win" : "fail");
                showResult(cleared, wave, breaches, stars, gate);
            }

            @Override
            public void onAudioEvent(String key) {
                audio.playSfx(key);
            }
        });
    }

    private void bindButtons() {
        findViewById(R.id.start_button).setOnClickListener(v -> startRun());
        findViewById(R.id.help_button).setOnClickListener(v -> showHelp());
        findViewById(R.id.help_back_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.pause_button).setOnClickListener(v -> pauseRun());
        findViewById(R.id.resume_button).setOnClickListener(v -> resumeRun());
        findViewById(R.id.restart_button).setOnClickListener(v -> startRun());
        findViewById(R.id.menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.result_restart_button).setOnClickListener(v -> startRun());
        findViewById(R.id.result_menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.archer_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_ARCHER));
        findViewById(R.id.barracks_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_BARRACKS));
        findViewById(R.id.stone_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_STONE));
        findViewById(R.id.oil_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_OIL));
        findViewById(R.id.beacon_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_BEACON));
        findViewById(R.id.fire_oil_button).setOnClickListener(v -> gameView.useFireOil());
        findViewById(R.id.repair_button).setOnClickListener(v -> gameView.useRepair());
        findViewById(R.id.next_wave_button).setOnClickListener(v -> gameView.startNextWave());
        soundButton.setOnClickListener(v -> toggleSound());
    }

    private void startRun() {
        audio.playSfx("ui_click");
        audio.playGameplay();
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        topHud.setVisibility(View.VISIBLE);
        buildPanel.setVisibility(View.VISIBLE);
        gameView.startGame();
    }

    private void pauseRun() {
        audio.playSfx("ui_click");
        gameView.pauseGame();
        pausePanel.setVisibility(View.VISIBLE);
    }

    private void resumeRun() {
        audio.playSfx("ui_click");
        pausePanel.setVisibility(View.GONE);
        gameView.resumeGame();
    }

    private void showMenu() {
        if (audio != null) {
            audio.playMenu();
        }
        gameView.resetForMenu();
        menuPanel.setVisibility(View.VISIBLE);
        helpPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        topHud.setVisibility(View.GONE);
        buildPanel.setVisibility(View.GONE);
    }

    private void showHelp() {
        audio.playSfx("ui_click");
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.VISIBLE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
    }

    private void showResult(boolean cleared, int wave, int breaches, int stars, int gate) {
        pausePanel.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        topHud.setVisibility(View.VISIBLE);
        buildPanel.setVisibility(View.GONE);
        resultTitleText.setText(cleared ? "Frontier Held" : "Gate Broken");
        resultStatsText.setText(String.format(Locale.US, "Wave %d  Gate %d  Breaches %d  Stars %d", wave, gate, breaches, stars));
    }

    private void toggleSound() {
        soundOn = !soundOn;
        soundButton.setText(soundOn ? "Sound On" : "Sound Off");
        audio.setEnabled(soundOn);
        gameView.setSoundEnabled(soundOn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.startLoop();
        }
        if (audio != null) {
            audio.setEnabled(soundOn);
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.pauseFromLifecycle();
        }
        if (audio != null) {
            audio.setEnabled(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (audio != null) {
            audio.release();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (helpPanel.getVisibility() == View.VISIBLE) {
            showMenu();
        } else if (pausePanel.getVisibility() == View.VISIBLE) {
            resumeRun();
        } else if (menuPanel.getVisibility() != View.VISIBLE) {
            pauseRun();
        } else {
            super.onBackPressed();
        }
    }
}
