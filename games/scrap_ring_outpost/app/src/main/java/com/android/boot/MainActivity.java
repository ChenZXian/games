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
    private GameAudio audio;
    private TextView scrapText;
    private TextView fuelText;
    private TextView partsText;
    private TextView nightText;
    private TextView objectiveText;
    private TextView integrityText;
    private TextView focusText;
    private TextView resultTitleText;
    private TextView resultStatsText;
    private View menuPanel;
    private View loadoutPanel;
    private View helpPanel;
    private View pausePanel;
    private View resultPanel;
    private View topHud;
    private View sectorPanel;
    private View commandPanel;
    private Button soundButton;
    private Button sectorNorthButton;
    private Button sectorEastButton;
    private Button sectorSouthButton;
    private Button sectorWestButton;
    private boolean soundOn = true;
    private int selectedTrait = GameView.TRAIT_SALVAGE;

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
        scrapText = findViewById(R.id.scrap_text);
        fuelText = findViewById(R.id.fuel_text);
        partsText = findViewById(R.id.parts_text);
        nightText = findViewById(R.id.night_text);
        objectiveText = findViewById(R.id.objective_text);
        integrityText = findViewById(R.id.integrity_text);
        focusText = findViewById(R.id.focus_text);
        resultTitleText = findViewById(R.id.result_title_text);
        resultStatsText = findViewById(R.id.result_stats_text);
        menuPanel = findViewById(R.id.menu_panel);
        loadoutPanel = findViewById(R.id.loadout_panel);
        helpPanel = findViewById(R.id.help_panel);
        pausePanel = findViewById(R.id.pause_panel);
        resultPanel = findViewById(R.id.result_panel);
        topHud = findViewById(R.id.top_hud);
        sectorPanel = findViewById(R.id.sector_panel);
        commandPanel = findViewById(R.id.command_panel);
        soundButton = findViewById(R.id.sound_button);
        sectorNorthButton = findViewById(R.id.sector_north_button);
        sectorEastButton = findViewById(R.id.sector_east_button);
        sectorSouthButton = findViewById(R.id.sector_south_button);
        sectorWestButton = findViewById(R.id.sector_west_button);
    }

    private void bindGame() {
        gameView.setListener(new GameView.GameListener() {
            @Override
            public void onHudChanged(GameView.HudSnapshot hud) {
                scrapText.setText(String.format(Locale.US, "Scrap %d", hud.scrap));
                fuelText.setText(String.format(Locale.US, "Fuel %d", hud.fuel));
                partsText.setText(String.format(Locale.US, "Parts %d", hud.parts));
                nightText.setText(String.format(Locale.US, "Night %d/%d", hud.night, hud.maxNight));
                objectiveText.setText(hud.objective);
                integrityText.setText(String.format(Locale.US, "%s %d", hud.sectorName, hud.integrity));
                focusText.setText(String.format(Locale.US, "Focus %s  Core %d", hud.focusReady ? "Ready" : "Cooling", hud.commandCore));
                updateSectorButtons(hud.selectedSector);
            }

            @Override
            public void onRunEnded(boolean cleared, int nights, int breaches, int stars) {
                audio.playSfx(cleared ? "win" : "fail");
                showResult(cleared, nights, breaches, stars);
            }

            @Override
            public void onAudioEvent(String key) {
                audio.playSfx(key);
            }
        });
    }

    private void bindButtons() {
        findViewById(R.id.start_button).setOnClickListener(v -> startRun());
        findViewById(R.id.loadout_button).setOnClickListener(v -> showLoadout());
        findViewById(R.id.help_button).setOnClickListener(v -> showHelp());
        findViewById(R.id.sound_button).setOnClickListener(v -> toggleSound());
        findViewById(R.id.help_back_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.loadout_back_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.pause_button).setOnClickListener(v -> pauseRun());
        findViewById(R.id.resume_button).setOnClickListener(v -> resumeRun());
        findViewById(R.id.restart_button).setOnClickListener(v -> startRun());
        findViewById(R.id.menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.result_restart_button).setOnClickListener(v -> startRun());
        findViewById(R.id.result_menu_button).setOnClickListener(v -> showMenu());
        findViewById(R.id.barricade_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_BARRICADE));
        findViewById(R.id.gun_nest_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_GUN_NEST));
        findViewById(R.id.spike_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_SPIKE_TRAP));
        findViewById(R.id.flame_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_FLAME_PIPE));
        findViewById(R.id.arc_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_ARC_LAMP));
        findViewById(R.id.repair_station_button).setOnClickListener(v -> gameView.setBuildMode(GameView.BUILD_REPAIR_STATION));
        findViewById(R.id.focus_burst_button).setOnClickListener(v -> gameView.useFocusBurst());
        findViewById(R.id.field_patch_button).setOnClickListener(v -> gameView.useFieldPatch());
        findViewById(R.id.begin_night_button).setOnClickListener(v -> gameView.beginNight());
        sectorNorthButton.setOnClickListener(v -> gameView.selectSector(0));
        sectorEastButton.setOnClickListener(v -> gameView.selectSector(1));
        sectorSouthButton.setOnClickListener(v -> gameView.selectSector(2));
        sectorWestButton.setOnClickListener(v -> gameView.selectSector(3));
        findViewById(R.id.trait_salvage_button).setOnClickListener(v -> selectTrait(GameView.TRAIT_SALVAGE));
        findViewById(R.id.trait_flare_button).setOnClickListener(v -> selectTrait(GameView.TRAIT_FLARE));
        findViewById(R.id.trait_welder_button).setOnClickListener(v -> selectTrait(GameView.TRAIT_WELDER));
    }

    private void selectTrait(int trait) {
        selectedTrait = trait;
        gameView.setTrait(trait);
        audio.playSfx("ui_click");
    }

    private void startRun() {
        audio.playSfx("ui_click");
        audio.playGameplay();
        menuPanel.setVisibility(View.GONE);
        loadoutPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        topHud.setVisibility(View.VISIBLE);
        sectorPanel.setVisibility(View.VISIBLE);
        commandPanel.setVisibility(View.VISIBLE);
        gameView.setTrait(selectedTrait);
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
        audio.playGameplay();
    }

    private void showMenu() {
        audio.playMenu();
        gameView.resetForMenu();
        menuPanel.setVisibility(View.VISIBLE);
        loadoutPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        pausePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        topHud.setVisibility(View.GONE);
        sectorPanel.setVisibility(View.GONE);
        commandPanel.setVisibility(View.GONE);
    }

    private void showLoadout() {
        audio.playSfx("ui_click");
        menuPanel.setVisibility(View.GONE);
        loadoutPanel.setVisibility(View.VISIBLE);
        helpPanel.setVisibility(View.GONE);
    }

    private void showHelp() {
        audio.playSfx("ui_click");
        menuPanel.setVisibility(View.GONE);
        loadoutPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.VISIBLE);
    }

    private void showResult(boolean cleared, int nights, int breaches, int stars) {
        pausePanel.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
        loadoutPanel.setVisibility(View.GONE);
        helpPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        topHud.setVisibility(View.VISIBLE);
        sectorPanel.setVisibility(View.VISIBLE);
        commandPanel.setVisibility(View.GONE);
        resultTitleText.setText(cleared ? R.string.title_result_victory : R.string.title_result_fail);
        resultStatsText.setText(String.format(Locale.US, "Nights %d  Breaches %d  Stars %d", nights, breaches, stars));
        if (cleared) {
            audio.playClimax();
        }
    }

    private void updateSectorButtons(int selectedSector) {
        sectorNorthButton.setSelected(selectedSector == 0);
        sectorEastButton.setSelected(selectedSector == 1);
        sectorSouthButton.setSelected(selectedSector == 2);
        sectorWestButton.setSelected(selectedSector == 3);
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
        if (helpPanel.getVisibility() == View.VISIBLE || loadoutPanel.getVisibility() == View.VISIBLE) {
            showMenu();
        } else if (pausePanel.getVisibility() == View.VISIBLE) {
            resumeRun();
        } else if (menuPanel.getVisibility() != View.VISIBLE && resultPanel.getVisibility() != View.VISIBLE) {
            pauseRun();
        } else {
            super.onBackPressed();
        }
    }
}
