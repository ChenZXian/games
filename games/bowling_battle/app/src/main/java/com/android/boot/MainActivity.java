package com.android.boot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.SoundController;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameEngine.Listener {
    private static final String PREFS = "bowling_battle_prefs";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_STARS_PREFIX = "stars_";
    private GameView gameView;
    private TextView scoreValue;
    private TextView comboValue;
    private TextView waveValue;
    private TextView lifeValue;
    private FrameLayout menuOverlay;
    private FrameLayout pauseOverlay;
    private FrameLayout gameOverOverlay;
    private TextView gameOverTitle;
    private TextView gameOverBody;
    private TextView helpToast;
    private Button[] levelButtons;
    private SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SoundController soundController = new SoundController();
    private int selectedLevel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        gameView = findViewById(R.id.game_view);
        scoreValue = findViewById(R.id.score_value);
        comboValue = findViewById(R.id.combo_value);
        waveValue = findViewById(R.id.wave_value);
        lifeValue = findViewById(R.id.life_value);
        menuOverlay = findViewById(R.id.menu_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        gameOverOverlay = findViewById(R.id.game_over_overlay);
        gameOverTitle = findViewById(R.id.game_over_title);
        gameOverBody = findViewById(R.id.game_over_body);
        helpToast = findViewById(R.id.help_toast);
        gameView.getEngine().setListener(this);
        gameView.setInputListener(row -> {
            if (gameView.getEngine().dropBall(row)) {
                soundController.playDrop();
                pulseView(findViewById(R.id.top_hud));
            }
        });
        ImageButton pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(v -> {
            if (gameView.getEngine().getGameState() == GameState.PLAYING) {
                pulseView(v);
                gameView.getEngine().setGameState(GameState.PAUSED);
                pauseOverlay.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.resume_button).setOnClickListener(v -> {
            pulseView(v);
            pauseOverlay.setVisibility(View.GONE);
            gameView.getEngine().setGameState(GameState.PLAYING);
        });
        findViewById(R.id.pause_menu_button).setOnClickListener(v -> {
            pulseView(v);
            showMenu();
        });
        findViewById(R.id.restart_button).setOnClickListener(v -> {
            pulseView(v);
            startLevel(selectedLevel);
        });
        findViewById(R.id.game_over_menu_button).setOnClickListener(v -> {
            pulseView(v);
            showMenu();
        });
        findViewById(R.id.how_to_play_button).setOnClickListener(v -> {
            pulseView(v);
            helpToast.setVisibility(View.VISIBLE);
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(() -> helpToast.setVisibility(View.GONE), 2600);
        });
        Button muteButton = findViewById(R.id.mute_button);
        muteButton.setOnClickListener(v -> {
            pulseView(v);
            soundController.toggleMuted();
            muteButton.setText(soundController.isMuted() ? getString(R.string.btn_unmute) : getString(R.string.btn_mute));
        });
        levelButtons = new Button[] {
                findViewById(R.id.level_1_button),
                findViewById(R.id.level_2_button),
                findViewById(R.id.level_3_button),
                findViewById(R.id.level_4_button),
                findViewById(R.id.level_5_button)
        };
        for (int i = 0; i < levelButtons.length; i++) {
            final int levelIndex = i;
            levelButtons[i].setOnClickListener(v -> {
                if (levelIndex < getUnlockedLevel()) {
                    pulseView(v);
                    startLevel(levelIndex);
                }
            });
        }
        updateLevelButtons();
        showMenu();
    }

    private void startLevel(int levelIndex) {
        selectedLevel = levelIndex;
        menuOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);
        gameView.getEngine().startLevel(levelIndex);
        soundController.playClear();
    }

    private void showMenu() {
        menuOverlay.setVisibility(View.VISIBLE);
        pauseOverlay.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);
        gameView.getEngine().setGameState(GameState.MENU);
        updateLevelButtons();
    }

    private int getUnlockedLevel() {
        return Math.max(1, preferences.getInt(KEY_UNLOCKED, 1));
    }

    private void updateLevelButtons() {
        int unlocked = getUnlockedLevel();
        for (int i = 0; i < levelButtons.length; i++) {
            int stars = preferences.getInt(KEY_STARS_PREFIX + i, 0);
            String title = gameView.getEngine().getLevels()[i].name + "   " + starText(stars);
            levelButtons[i].setText((i + 1) + "  " + title + (i + 1 > unlocked ? "  Locked" : ""));
            levelButtons[i].setEnabled(i + 1 <= unlocked);
            levelButtons[i].setAlpha(i + 1 <= unlocked ? 1f : 0.45f);
        }
    }

    private String starText(int stars) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            builder.append(i < stars ? '*' : '-');
        }
        return builder.toString();
    }

    private void pulseView(View view) {
        view.animate().cancel();
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.startLoop();
        if (gameView.getEngine().getGameState() == GameState.PAUSED) {
            gameView.getEngine().setGameState(GameState.PLAYING);
            pauseOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView.getEngine().getGameState() == GameState.PLAYING) {
            gameView.getEngine().setGameState(GameState.PAUSED);
            pauseOverlay.setVisibility(View.VISIBLE);
        }
        gameView.stopLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundController.release();
    }

    @Override
    public void onHudChanged(int score, int combo, int wave, int life) {
        runOnUiThread(() -> {
            scoreValue.setText(String.valueOf(score));
            comboValue.setText(String.valueOf(combo));
            waveValue.setText(String.valueOf(wave));
            lifeValue.setText(String.valueOf(life));
        });
    }

    @Override
    public void onGameFinished(boolean cleared, int stars, String summary) {
        runOnUiThread(() -> {
            gameOverOverlay.setVisibility(View.VISIBLE);
            pauseOverlay.setVisibility(View.GONE);
            gameOverTitle.setText(cleared ? getString(R.string.label_state_clear) : getString(R.string.label_state_over));
            gameOverBody.setText(summary);
            if (cleared) {
                soundController.playClear();
                int storedStars = preferences.getInt(KEY_STARS_PREFIX + selectedLevel, 0);
                int unlocked = Math.max(getUnlockedLevel(), selectedLevel + 2);
                preferences.edit()
                        .putInt(KEY_STARS_PREFIX + selectedLevel, Math.max(storedStars, stars))
                        .putInt(KEY_UNLOCKED, Math.min(5, unlocked))
                        .apply();
                updateLevelButtons();
            }
        });
    }
}
