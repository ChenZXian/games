package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameState;
import com.android.boot.core.StateMachine;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameView.Listener {
    private StateMachine stateMachine;
    private GameView gameView;
    private View hud;
    private View menu;
    private View paused;
    private View gameOver;
    private View levelSelect;
    private TextView txtLives;
    private TextView txtGold;
    private TextView txtWave;
    private TextView txtGameOver;
    private ProgressBar manaBar;
    private int selectedLevel;
    private BgmPlayer bgmPlayer;
    private ImageButton btnMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        stateMachine = new StateMachine();
        gameView = findViewById(R.id.game_view);
        gameView.setListener(this);
        bgmPlayer = new BgmPlayer();
        bindViews();
        bindActions();
        renderState(GameState.MENU);
        bgmPlayer.start(this);
        updateMuteButton();
    }

    private void bindViews() {
        hud = findViewById(R.id.hud_overlay);
        menu = findViewById(R.id.menu_overlay);
        paused = findViewById(R.id.paused_overlay);
        gameOver = findViewById(R.id.game_over_overlay);
        levelSelect = findViewById(R.id.level_select_overlay);
        txtLives = findViewById(R.id.txt_lives);
        txtGold = findViewById(R.id.txt_gold);
        txtWave = findViewById(R.id.txt_wave);
        txtGameOver = findViewById(R.id.txt_game_over);
        manaBar = findViewById(R.id.mana_bar);
        btnMute = findViewById(R.id.btn_mute);
    }

    private void bindActions() {
        findViewById(R.id.btn_start).setOnClickListener(v -> startGame(selectedLevel));
        findViewById(R.id.btn_levels).setOnClickListener(v -> renderState(GameState.LEVEL_SELECT));
        findViewById(R.id.btn_help).setOnClickListener(v -> ((Button) v).setText("Tap field to move hero. Skills consume mana."));
        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            if (stateMachine.getState() == GameState.PLAYING) {
                gameView.setPaused(true);
                renderState(GameState.PAUSED);
            }
        });
        findViewById(R.id.btn_resume).setOnClickListener(v -> {
            gameView.setPaused(false);
            renderState(GameState.PLAYING);
        });
        findViewById(R.id.btn_menu).setOnClickListener(v -> renderState(GameState.MENU));
        findViewById(R.id.btn_game_over_menu).setOnClickListener(v -> renderState(GameState.MENU));
        findViewById(R.id.btn_restart).setOnClickListener(v -> startGame(selectedLevel));
        findViewById(R.id.btn_meteor).setOnClickListener(v -> gameView.castMeteor());
        findViewById(R.id.btn_freeze).setOnClickListener(v -> gameView.castFreeze());
        findViewById(R.id.btn_reinforce).setOnClickListener(v -> gameView.castReinforce());
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                bgmPlayer.setMuted(!bgmPlayer.isMuted());
                updateMuteButton();
            });
        }
        findViewById(R.id.btn_level_1).setOnClickListener(v -> selectLevel(0));
        findViewById(R.id.btn_level_2).setOnClickListener(v -> selectLevel(1));
        findViewById(R.id.btn_level_3).setOnClickListener(v -> selectLevel(2));
        findViewById(R.id.btn_level_4).setOnClickListener(v -> selectLevel(3));
        findViewById(R.id.btn_level_5).setOnClickListener(v -> selectLevel(4));
    }

    private void selectLevel(int level) {
        selectedLevel = level;
        renderState(GameState.MENU);
    }

    private void startGame(int level) {
        gameView.startLevel(level);
        gameView.setPaused(false);
        renderState(GameState.PLAYING);
    }

    private void renderState(GameState state) {
        stateMachine.setState(state);
        hud.setVisibility(state == GameState.PLAYING ? View.VISIBLE : View.GONE);
        menu.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
        paused.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        gameOver.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
        levelSelect.setVisibility(state == GameState.LEVEL_SELECT ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onHud(int lives, int gold, int wave, int waveTotal, int mana) {
        runOnUiThread(() -> {
            txtLives.setText("Lives " + lives);
            txtGold.setText("Gold " + gold);
            txtWave.setText("Wave " + wave + "/" + waveTotal);
            manaBar.setProgress(mana);
        });
    }

    @Override
    public void onGameOver(boolean victory) {
        runOnUiThread(() -> {
            txtGameOver.setText(victory ? getString(R.string.label_victory) : getString(R.string.label_game_over));
            renderState(GameState.GAME_OVER);
        });
    }

    private void updateMuteButton() {
        if (btnMute != null) {
            btnMute.setImageResource(bgmPlayer.isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bgmPlayer.pause();
        gameView.setPaused(true);
        if (stateMachine.getState() == GameState.PLAYING) {
            renderState(GameState.PAUSED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bgmPlayer.isMuted()) {
            bgmPlayer.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgmPlayer.stop();
    }
}
