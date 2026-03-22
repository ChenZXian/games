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
    private LinearLayout helpOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout resultOverlay;
    private TextView txtLevel;
    private TextView txtWave;
    private TextView txtIntegrity;
    private TextView txtQueue;
    private TextView txtScore;
    private TextView txtResultTitle;
    private TextView txtResultBody;
    private ImageButton btnMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        helpOverlay = findViewById(R.id.help_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        txtLevel = findViewById(R.id.txt_level);
        txtWave = findViewById(R.id.txt_wave);
        txtIntegrity = findViewById(R.id.txt_integrity);
        txtQueue = findViewById(R.id.txt_queue);
        txtScore = findViewById(R.id.txt_score);
        txtResultTitle = findViewById(R.id.txt_result_title);
        txtResultBody = findViewById(R.id.txt_result_body);
        btnMute = findViewById(R.id.btn_mute);
        gameView.setUiCallbacks(this);
        bindButtons();
        populateLevelButtons();
    }

    private void bindButtons() {
        bindClick(findViewById(R.id.btn_start), v -> {
            helpOverlay.setVisibility(View.GONE);
            gameView.getEngine().startSelectedLevel();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_how_to_play), v -> {
            helpOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setVisibility(View.GONE);
        });
        bindClick(findViewById(R.id.btn_close_help), v -> {
            helpOverlay.setVisibility(View.GONE);
            menuOverlay.setVisibility(View.VISIBLE);
        });
        bindClick(findViewById(R.id.btn_pause), v -> {
            gameView.getEngine().pause();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_resume), v -> {
            gameView.getEngine().resume();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_restart), v -> {
            gameView.getEngine().restartLevel();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_menu), v -> {
            gameView.getEngine().goToMenu();
            populateLevelButtons();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_next), v -> {
            gameView.getEngine().startNextLevelOrMenu();
            populateLevelButtons();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_result_restart), v -> {
            gameView.getEngine().restartLevel();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(findViewById(R.id.btn_result_menu), v -> {
            gameView.getEngine().goToMenu();
            populateLevelButtons();
            onSnapshot(gameView.getEngine().getSnapshot());
        });
        bindClick(btnMute, v -> {
            gameView.getEngine().toggleMuted();
            syncMute();
        });
    }

    private void bindClick(View view, View.OnClickListener listener) {
        view.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(50).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
            listener.onClick(v);
        });
    }

    private void populateLevelButtons() {
        GridLayout grid = findViewById(R.id.level_grid);
        grid.removeAllViews();
        int margin = getResources().getDimensionPixelSize(R.dimen.cst_pad_4);
        GameEngine engine = gameView.getEngine();
        for (int i = 0; i < engine.getLevelCount(); i++) {
            Button button = new Button(this, null, 0, R.style.Widget_Game_Button_Secondary);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            boolean unlocked = engine.isLevelUnlocked(i);
            int stars = engine.getLevelStars(i);
            String prefix = i == engine.getMenuSelectedLevel() ? "> " : "";
            button.setText(prefix + (i + 1) + "\n" + engine.getLevelName(i) + "\nStars " + stars + (unlocked ? "" : "\nLocked"));
            button.setEnabled(unlocked);
            final int levelIndex = i;
            bindClick(button, v -> {
                engine.startMenuLevel(levelIndex);
                populateLevelButtons();
                onSnapshot(engine.getSnapshot());
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
            txtLevel.setText(getString(R.string.hud_level) + " " + snapshot.levelIndex + "  " + snapshot.levelName);
            txtWave.setText(getString(R.string.hud_wave) + " " + snapshot.waveIndex);
            txtIntegrity.setText(getString(R.string.hud_integrity) + " " + snapshot.integrity);
            txtQueue.setText(getString(R.string.hud_queue) + " " + snapshot.queueText);
            txtScore.setText(getString(R.string.hud_score) + " " + snapshot.score);
            txtResultTitle.setText(snapshot.resultTitle);
            txtResultBody.setText(snapshot.resultBody);
            syncMute();
            menuOverlay.setVisibility(GameEngine.STATE_MENU.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            pauseOverlay.setVisibility(GameEngine.STATE_PAUSED.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            resultOverlay.setVisibility(GameEngine.STATE_LEVEL_CLEAR.equals(snapshot.state) || GameEngine.STATE_GAME_OVER.equals(snapshot.state) ? View.VISIBLE : View.GONE);
            if (!GameEngine.STATE_MENU.equals(snapshot.state)) {
                helpOverlay.setVisibility(View.GONE);
            }
            if (GameEngine.STATE_MENU.equals(snapshot.state)) {
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
