package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.SoundManager;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GameView.GameListener {
    private GameView gameView;
    private View menuOverlay;
    private View pauseOverlay;
    private View upgradeOverlay;
    private View gameOverOverlay;
    private View hpFill;
    private View energyFill;
    private TextView timeText;
    private TextView killsText;
    private TextView gameOverStats;
    private Button startButton;
    private Button resumeButton;
    private Button restartButton;
    private Button upgradeButton1;
    private Button upgradeButton2;
    private Button upgradeButton3;
    private ImageButton pauseButton;
    private Button skillButton;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        upgradeOverlay = findViewById(R.id.upgrade_overlay);
        gameOverOverlay = findViewById(R.id.game_over_overlay);
        hpFill = findViewById(R.id.hp_fill);
        energyFill = findViewById(R.id.energy_fill);
        timeText = findViewById(R.id.time_text);
        killsText = findViewById(R.id.kills_text);
        gameOverStats = findViewById(R.id.game_over_stats);
        startButton = findViewById(R.id.start_button);
        resumeButton = findViewById(R.id.resume_button);
        restartButton = findViewById(R.id.restart_button);
        upgradeButton1 = findViewById(R.id.upgrade_button_1);
        upgradeButton2 = findViewById(R.id.upgrade_button_2);
        upgradeButton3 = findViewById(R.id.upgrade_button_3);
        pauseButton = findViewById(R.id.pause_button);
        skillButton = findViewById(R.id.skill_button);
        soundManager = new SoundManager(this);
        gameView.getEngine().setSoundManager(soundManager);
        gameView.setGameListener(this);

        startButton.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
            gameView.getEngine().startGame();
        });
        resumeButton.setOnClickListener(v -> {
            pauseOverlay.setVisibility(View.GONE);
            gameView.getEngine().resumeGame();
        });
        restartButton.setOnClickListener(v -> {
            gameOverOverlay.setVisibility(View.GONE);
            gameView.getEngine().restartGame();
        });
        pauseButton.setOnClickListener(v -> {
            if (gameView.getEngine().getState() == GameState.PLAYING) {
                gameView.getEngine().pauseGame();
            }
        });
        skillButton.setOnClickListener(v -> gameView.getEngine().triggerSkill());

        upgradeButton1.setOnClickListener(v -> onUpgradeChosen(0));
        upgradeButton2.setOnClickListener(v -> onUpgradeChosen(1));
        upgradeButton3.setOnClickListener(v -> onUpgradeChosen(2));
    }

    private void onUpgradeChosen(int index) {
        upgradeOverlay.setVisibility(View.GONE);
        gameView.getEngine().applyUpgrade(index);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView.getEngine().getState() == GameState.PLAYING) {
            gameView.getEngine().pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView.getEngine().getState() == GameState.PAUSED) {
            pauseOverlay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStateChanged(GameState state, GameEngine.GameStats stats) {
        runOnUiThread(() -> {
            if (state == GameState.MENU) {
                menuOverlay.setVisibility(View.VISIBLE);
            } else if (state == GameState.PAUSED) {
                pauseOverlay.setVisibility(View.VISIBLE);
            } else if (state == GameState.UPGRADE) {
                upgradeOverlay.setVisibility(View.VISIBLE);
            } else if (state == GameState.GAME_OVER) {
                gameOverOverlay.setVisibility(View.VISIBLE);
                String text = formatGameOver(stats);
                gameOverStats.setText(text);
            } else {
                menuOverlay.setVisibility(View.GONE);
                pauseOverlay.setVisibility(View.GONE);
                upgradeOverlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onHudUpdated(GameEngine.GameStats stats) {
        runOnUiThread(() -> {
            float hpRatio = stats.maxHp > 0f ? stats.hp / stats.maxHp : 0f;
            float energyRatio = stats.maxEnergy > 0f ? stats.energy / stats.maxEnergy : 0f;
            hpFill.setScaleX(hpRatio);
            energyFill.setScaleX(energyRatio);
            String time = formatTime(stats.timeSurvived);
            timeText.setText(time);
            killsText.setText(String.format(Locale.US, "%d", stats.kills));
        });
    }

    @Override
    public void onUpgradeOptions(String[] options) {
        runOnUiThread(() -> {
            upgradeButton1.setText(options[0]);
            upgradeButton2.setText(options[1]);
            upgradeButton3.setText(options[2]);
        });
    }

    private String formatTime(float seconds) {
        int total = (int) seconds;
        int minutes = total / 60;
        int secs = total % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, secs);
    }

    private String formatGameOver(GameEngine.GameStats stats) {
        String time = formatTime(stats.timeSurvived);
        return "Time " + time + "\nKills " + stats.kills + "\nScore " + stats.score;
    }
}
