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
    private TextView bunkerText;
    private TextView moraleText;
    private TextView supplyText;
    private TextView waveText;
    private TextView statusText;
    private TextView resultTitleText;
    private TextView resultStatsText;
    private View menuPanel;
    private View helpPanel;
    private View pausePanel;
    private View resultPanel;
    private View leftRail;
    private View topRouteStrip;
    private View rightCommandTray;
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
        bunkerText = findViewById(R.id.bunker_text);
        moraleText = findViewById(R.id.morale_text);
        supplyText = findViewById(R.id.supply_text);
        waveText = findViewById(R.id.wave_text);
        statusText = findViewById(R.id.status_text);
        resultTitleText = findViewById(R.id.result_title_text);
        resultStatsText = findViewById(R.id.result_stats_text);
        menuPanel = findViewById(R.id.menu_panel);
        helpPanel = findViewById(R.id.help_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        leftRail = findViewById(R.id.left_rail);
        topRouteStrip = findViewById(R.id.top_route_strip);
        rightCommandTray = findViewById(R.id.right_command_tray);
        soundButton = findViewById(R.id.sound_button);
    }

    private void bindGame() {
        gameView.setListener(new GameView.GameListener() {
            @Override
            public void onHudChanged(int bunker, int morale, int supplies, int wave, int maxWave, int breaches, String status) {
                bunkerText.setText(String.format(Locale.US, "Bunker %d", bunker));
                moraleText.setText(String.format(Locale.US, "Morale %d", morale));
                supplyText.setText(String.format(Locale.US, "Supply %d", supplies));
                waveText.setText(String.format(Locale.US, "Wave %d/%d", wave, maxWave));
                statusText.setText(String.format(Locale.US, "%s  Breaches %d", status, breaches));
            }

            @Override
            public void onRunEnded(boolean cleared, int wave, int breaches, int stars, int bunker, int morale) {
                audio.playSfx(cleared ? "win" : "fail");
                showResult(cleared, wave, breaches, stars, bunker, morale);
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
        findViewById(R.id.rifle_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_RIFLE));
        findViewById(R.id.gun_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_GUN));
        findViewById(R.id.sniper_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_SNIPER));
        findViewById(R.id.engineer_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_ENGINEER));
        findViewById(R.id.mortar_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_MORTAR));
        findViewById(R.id.signal_button).setOnClickListener(v -> gameView.setBuildMode(GameView.DEPLOY_SIGNAL));
        findViewById(R.id.flare_button).setOnClickListener(v -> gameView.useFlareScan());
        findViewById(R.id.demo_button).setOnClickListener(v -> gameView.useDemolition());
        findViewById(R.id.retreat_button).setOnClickListener(v -> gameView.useRetreatOrder());
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
        leftRail.setVisibility(View.VISIBLE);
        topRouteStrip.setVisibility(View.VISIBLE);
        rightCommandTray.setVisibility(View.VISIBLE);
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
        leftRail.setVisibility(View.GONE);
        topRouteStrip.setVisibility(View.GONE);
        rightCommandTray.setVisibility(View.GONE);
    }

    private void showHelp() {
        audio.playSfx("ui_click");
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.VISIBLE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
    }

    private void showResult(boolean cleared, int wave, int breaches, int stars, int bunker, int morale) {
        pausePanel.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        leftRail.setVisibility(View.VISIBLE);
        topRouteStrip.setVisibility(View.VISIBLE);
        rightCommandTray.setVisibility(View.GONE);
        resultTitleText.setText(cleared ? "Pass Secured" : "Bunker Lost");
        resultStatsText.setText(String.format(Locale.US, "Wave %d  Bunker %d  Morale %d  Breaches %d  Stars %d", wave, bunker, morale, breaches, stars));
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
