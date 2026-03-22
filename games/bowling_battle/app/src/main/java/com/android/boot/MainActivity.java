package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameSnapshot;
import com.android.boot.ui.GameView;

public final class MainActivity extends AppCompatActivity implements GameView.UiCallbacks {
    private GameView gameView;
    private LinearLayout menuOverlay;
    private LinearLayout levelOverlay;
    private LinearLayout helpOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout resultOverlay;
    private TextView txtLevel;
    private TextView txtWave;
    private TextView txtResultTitle;
    private TextView txtResultScore;
    private TextView txtResultStars;
    private ImageButton btnMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        txtLevel = findViewById(R.id.txt_level);
        txtWave = findViewById(R.id.txt_wave);
        txtResultTitle = findViewById(R.id.txt_result_title);
        txtResultScore = findViewById(R.id.txt_result_score);
        txtResultStars = findViewById(R.id.txt_result_stars);
        btnMute = findViewById(R.id.btn_mute);
        menuOverlay = findViewById(R.id.menu_overlay);
        levelOverlay = findViewById(R.id.level_overlay);
        helpOverlay = findViewById(R.id.help_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        bindButtons();
        populateLevelButtons();
        gameView.setUiCallbacks(this);
    }

    private void bindButtons() {
        bindScale(findViewById(R.id.btn_start), v -> {
            levelOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setVisibility(View.GONE);
        });
        bindScale(findViewById(R.id.btn_how_to_play), v -> {
            helpOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setVisibility(View.GONE);
        });
        bindScale(findViewById(R.id.btn_help_close), v -> {
            helpOverlay.setVisibility(View.GONE);
            menuOverlay.setVisibility(View.VISIBLE);
        });
        bindScale(findViewById(R.id.btn_pause), v -> {
            gameView.getEngine().pause();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_resume), v -> {
            gameView.getEngine().resume();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_restart), v -> {
            gameView.getEngine().restartLevel();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_menu), v -> {
            gameView.getEngine().goToMenu();
            populateLevelButtons();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(findViewById(R.id.btn_next), v -> {
            gameView.getEngine().startNextLevelOrMenu();
            populateLevelButtons();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindScale(btnMute, v -> {
            gameView.getEngine().toggleMuted();
            syncMute();
        });
    }

    private void bindScale(View view, View.OnClickListener listener) {
        view.setOnTouchListener((v, event) -> false);
        view.setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
            listener.onClick(v);
        });
    }

    private void populateLevelButtons() {
        GridLayout grid = findViewById(R.id.level_grid);
        int margin = getResources().getDimensionPixelSize(R.dimen.cst_pad_8);
        grid.removeAllViews();
        GameEngine engine = gameView.getEngine();
        int count = engine.getLevelCount();
        for (int i = 0; i < count; i++) {
            Button button = new Button(this, null, 0, R.style.Widget_Game_Button_Secondary);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            boolean unlocked = engine.isLevelUnlocked(i);
            int stars = engine.getLevelStars(i);
            button.setText((i + 1) + "\n" + engine.getLevelName(i) + "\nStars " + stars + (unlocked ? "" : "\nLocked"));
            button.setEnabled(unlocked);
            final int levelIndex = i;
            bindScale(button, v -> {
                gameView.getEngine().startLevel(levelIndex);
                onSnapshot(gameView.getEngine().getSnapshot());
            });
            grid.addView(button);
        }
    }

    private void syncMute() {
        btnMute.setImageResource(gameView.getEngine().isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
    }

    @Override
    public void onSnapshot(GameSnapshot snapshot) {
        runOnUiThread(() -> {
            txtLevel.setText("Level " + snapshot.levelIndex + "  " + snapshot.levelName);
            txtWave.setText("Wave " + snapshot.waveIndex);
            txtResultTitle.setText(snapshot.resultTitle);
            txtResultScore.setText(snapshot.resultScore);
            txtResultStars.setText("Stars " + snapshot.resultStars);
            syncMute();
            menuOverlay.setVisibility(GameEngine.STATE_MENU.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            helpOverlay.setVisibility(helpOverlay.getVisibility() == View.VISIBLE && GameEngine.STATE_MENU.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            levelOverlay.setVisibility(levelOverlay.getVisibility() == View.VISIBLE && GameEngine.STATE_MENU.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            pauseOverlay.setVisibility(GameEngine.STATE_PAUSED.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            resultOverlay.setVisibility(GameEngine.STATE_LEVEL_CLEAR.equals(snapshot.state) || GameEngine.STATE_GAME_OVER.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            if (!GameEngine.STATE_MENU.equals(snapshot.state)) {
                helpOverlay.setVisibility(View.GONE);
                levelOverlay.setVisibility(View.GONE);
                menuOverlay.setVisibility(View.GONE);
            }
            if (GameEngine.STATE_GAME_OVER.equals(snapshot.state)) {
                txtResultStars.setText("Stars 0");
            }
            if (GameEngine.STATE_LEVEL_CLEAR.equals(snapshot.state) || GameEngine.STATE_GAME_OVER.equals(snapshot.state)) {
                populateLevelButtons();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResumeView();
    }

    @Override
    protected void onPause() {
        gameView.onPauseView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameView.release();
        super.onDestroy();
    }
}
