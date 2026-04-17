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
    private BgmPlayer bgmPlayer;
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
        bgmPlayer = new BgmPlayer();
        bgmPlayer.start(this);
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
        Button btnRight = findViewById(R.id.btn_right);
        Button btnJump = findViewById(R.id.btn_jump);
        Button btnSpray = findViewById(R.id.btn_spray);
        Button btnKick = findViewById(R.id.btn_kick);
        ImageButton btnPause = findViewById(R.id.btn_pause);
        ImageButton btnMute = findViewById(R.id.btn_mute);

        btnStart.setOnClickListener(v -> {
            tapAnim(v);
            setUiState(GameDefs.STATE_LEVEL_SELECT);
        });
        btnHow.setOnClickListener(v -> {
            tapAnim(v);
            showHowToPlay();
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
            boolean muted = !bgmPlayer.isMuted();
            bgmPlayer.setMuted(muted);
            btnMute.setImageResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
        });

        btnLeft.setOnTouchListener((v, e) -> {
            tapAnim(v);
            gameView.holdLeft(e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL);
            return false;
        });
        btnRight.setOnTouchListener((v, e) -> {
            tapAnim(v);
            gameView.holdRight(e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL);
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
        btnKick.setOnClickListener(v -> {
            tapAnim(v);
            gameView.pressKick();
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

    private void showHowToPlay() {
        String message = "【游戏玩法】\n\n" +
                "控制雪人消灭所有敌人！\n\n" +
                "【基本操作】\n" +
                "• LEFT/RIGHT: 左右移动\n" +
                "• JUMP: 跳跃（可在平台上停留）\n" +
                "• SPRAY: 喷雪攻击敌人\n" +
                "• KICK: 踢雪球或直接攻击\n\n" +
                "【战斗技巧】\n" +
                "1. 按住SPRAY向敌人喷雪，累积雪层\n" +
                "2. 敌人被完全覆盖后变成雪球\n" +
                "3. 点击KICK踢雪球，击中其他敌人可形成连击\n" +
                "4. 也可以直接KICK攻击附近敌人\n\n" +
                "【游戏目标】\n" +
                "消灭所有敌人即可通关！\n" +
                "连击数越多，通关评价越高！\n\n" +
                "【特殊元素】\n" +
                "• 平台：可以跳跃到平台上\n" +
                "• 冰面：移动更滑\n" +
                "• 弹簧垫：可以跳得更高\n" +
                "• 风扇：会被吹动\n" +
                "• 冰锥陷阱：可以消灭敌人";
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("游戏说明")
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
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
        if (bgmPlayer != null) {
            bgmPlayer.resume();
        }
    }

    @Override
    protected void onPause() {
        gameView.getEngine().setState(GameDefs.STATE_PAUSED);
        gameView.pauseGameLoop();
        if (bgmPlayer != null) {
            bgmPlayer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
        }
        super.onDestroy();
    }
}
