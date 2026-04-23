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
    private TextView reactorText;
    private TextView oreText;
    private TextView waveText;
    private TextView routeText;
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
        reactorText = findViewById(R.id.reactor_text);
        oreText = findViewById(R.id.ore_text);
        waveText = findViewById(R.id.wave_text);
        routeText = findViewById(R.id.route_text);
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
            public void onHudChanged(int reactor, int ore, int wave, int maxWave, int leaks, String route, String selected) {
                reactorText.setText(String.format(Locale.US, "Reactor %d", reactor));
                oreText.setText(String.format(Locale.US, "Ore %d", ore));
                waveText.setText(String.format(Locale.US, "Wave %d/%d", wave, maxWave));
                routeText.setText(String.format(Locale.US, "%s  %s  Leaks %d", route, selected, leaks));
            }

            @Override
            public void onRunEnded(boolean cleared, int wave, int leaks, int stars) {
                audio.playSfx(cleared ? "win" : "fail");
                showResult(cleared, wave, leaks, stars);
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
        findViewById(R.id.gatling_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_GATLING));
        findViewById(R.id.frost_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_FROST));
        findViewById(R.id.tesla_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_TESLA));
        findViewById(R.id.drill_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_DRILL));
        findViewById(R.id.rockfall_button).setOnClickListener(v -> gameView.useRockfall());
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

    private void showResult(boolean cleared, int wave, int leaks, int stars) {
        pausePanel.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        topHud.setVisibility(View.VISIBLE);
        buildPanel.setVisibility(View.GONE);
        resultTitleText.setText(cleared ? "Stage Clear" : "Mine Lost");
        resultStatsText.setText(String.format(Locale.US, "Wave %d  Leaks %d  Stars %d", wave, leaks, stars));
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
