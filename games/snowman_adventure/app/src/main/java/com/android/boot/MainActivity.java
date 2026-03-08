package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.model.GameDefs;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private LinearLayout menuOverlay;
    private LinearLayout levelOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout resultOverlay;
    private LinearLayout controlsPanel;
    private LinearLayout hudTop;
    private TextView txtStage;
    private TextView txtCombo;
    private TextView txtStars;
    private final Runnable hudTask = new Runnable() {
        @Override
        public void run() {
            txtStage.setText(gameView.getEngine().getLevelTitle());
            txtCombo.setText("Combo " + gameView.getEngine().getCombo());
            if (gameView.getEngine().getState() == GameDefs.STATE_GAME_OVER) {
                showResult();
            }
            txtStage.postDelayed(this, 120L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        levelOverlay = findViewById(R.id.level_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        resultOverlay = findViewById(R.id.result_overlay);
        controlsPanel = findViewById(R.id.controls_panel);
        hudTop = findViewById(R.id.hud_top);
        txtStage = findViewById(R.id.txt_stage);
        txtCombo = findViewById(R.id.txt_combo);
        txtStars = findViewById(R.id.txt_stars);
        setupButtons();
        buildLevelButtons();
        setUiState(GameDefs.STATE_MENU);
        txtStage.post(hudTask);
    }

    private void setupButtons() {
        Button btnStart = findViewById(R.id.btn_start);
        Button btnHow = findViewById(R.id.btn_how_to_play);
        Button btnResume = findViewById(R.id.btn_resume);
        Button btnRestart = findViewById(R.id.btn_restart);
        Button btnMenu = findViewById(R.id.btn_menu);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnLeft = findViewById(R.id.btn_left);
        Button btnJump = findViewById(R.id.btn_jump);
        Button btnSpray = findViewById(R.id.btn_spray);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        ImageButton btnMute = findViewById(R.id.btn_mute);

        btnStart.setOnClickListener(v -> {
            tapAnim(v);
            setUiState(GameDefs.STATE_LEVEL_SELECT);
        });
        btnHow.setOnClickListener(v -> {
            tapAnim(v);
            txtCombo.setText("Spray to build snow then kick to chain");
        });
        btnResume.setOnClickListener(v -> {
            tapAnim(v);
            gameView.getEngine().setState(GameDefs.STATE_PLAYING);
            setUiState(GameDefs.STATE_PLAYING);
        });
        btnRestart.setOnClickListener(v -> {
            tapAnim(v);
            gameView.getEngine().startLevel(gameView.getEngine().getLevelIndex());
            setUiState(GameDefs.STATE_PLAYING);
        });
        btnMenu.setOnClickListener(v -> {
            tapAnim(v);
            gameView.getEngine().setState(GameDefs.STATE_MENU);
            setUiState(GameDefs.STATE_MENU);
        });
        btnNext.setOnClickListener(v -> {
            tapAnim(v);
            int next = Math.min(gameView.getEngine().getLevelIndex() + 1, 9);
            gameView.getEngine().startLevel(next);
            setUiState(GameDefs.STATE_PLAYING);
        });
        btnPause.setOnClickListener(v -> {
            tapAnim(v);
            gameView.getEngine().setState(GameDefs.STATE_PAUSED);
            setUiState(GameDefs.STATE_PAUSED);
        });
        btnMute.setOnClickListener(v -> {
            tapAnim(v);
            boolean muted = !gameView.getToneFx().isMuted();
            gameView.getToneFx().setMuted(muted);
            btnMute.setImageResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
        });

        btnLeft.setOnTouchListener((v, e) -> {
            tapAnim(v);
            gameView.holdLeft(e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL);
            return false;
        });
        btnJump.setOnClickListener(v -> {
            tapAnim(v);
            gameView.pressJump();
        });
        btnSpray.setOnTouchListener((v, e) -> {
            tapAnim(v);
            gameView.holdSpray(e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL);
            return false;
        });
    }

    private void buildLevelButtons() {
        GridLayout grid = findViewById(R.id.level_grid);
        for (int i = 0; i < 10; i++) {
            Button levelBtn = new Button(this);
            levelBtn.setText(String.valueOf(i + 1));
            levelBtn.setBackgroundResource(R.drawable.ui_button_secondary);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            levelBtn.setLayoutParams(params);
            final int level = i;
            levelBtn.setOnClickListener(v -> {
                tapAnim(v);
                if (level < gameView.getEngine().getUnlockedLevel()) {
                    gameView.getEngine().startLevel(level);
                    setUiState(GameDefs.STATE_PLAYING);
                }
            });
            grid.addView(levelBtn);
        }
    }

    private void tapAnim(View view) {
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(55).withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(55).start()).start();
    }

    private void showResult() {
        txtStars.setText("Stars " + gameView.getEngine().getStars());
        txtStars.setScaleX(0.7f);
        txtStars.setScaleY(0.7f);
        txtStars.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        setUiState(GameDefs.STATE_GAME_OVER);
    }

    private void setUiState(int state) {
        menuOverlay.setVisibility(state == GameDefs.STATE_MENU ? View.VISIBLE : View.GONE);
        levelOverlay.setVisibility(state == GameDefs.STATE_LEVEL_SELECT ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(state == GameDefs.STATE_PAUSED ? View.VISIBLE : View.GONE);
        resultOverlay.setVisibility(state == GameDefs.STATE_GAME_OVER ? View.VISIBLE : View.GONE);
        boolean playing = state == GameDefs.STATE_PLAYING;
        controlsPanel.setVisibility(playing ? View.VISIBLE : View.GONE);
        hudTop.setVisibility(playing ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGameLoop();
    }

    @Override
    protected void onPause() {
        gameView.getEngine().setState(GameDefs.STATE_PAUSED);
        gameView.pauseGameLoop();
        super.onPause();
    }
}
