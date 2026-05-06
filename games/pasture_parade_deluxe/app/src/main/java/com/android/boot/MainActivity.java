package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.boot.R;
import com.android.boot.audio.BgmPlayer;
import com.android.boot.core.RanchState;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private GameView gameView;
    private ScrollView menuScreen;
    private View hudTop;
    private View panelActions;
    private TextView howToPlay;
    private View gameOverCard;
    private TextView gameOverScore;
    private Button btnStart;
    private boolean gameStarted;
    private BgmPlayer bgmPlayer;
    private boolean muted;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statePoller = new Runnable() {
        @Override
        public void run() {
            if (gameStarted) {
                if (gameView.getState() == RanchState.GAME_OVER) {
                    int coins = gameView.getSessionCoins();
                    int deliveries = gameView.getDeliveriesDone();
                    int combo = gameView.getHighestCombo();
                    int level = gameView.getRanchLevel();
                    int score = coins + deliveries * 50 + combo * 10 + level * 25;

                    gameView.onHostPause();
                    gameView.setState(RanchState.MENU);
                    showGameOver(score, coins, deliveries, combo, level);
                    return;
                }
                handler.postDelayed(this, 250);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bgmPlayer = new BgmPlayer(this);
        gameView = findViewById(R.id.game_view);
        menuScreen = findViewById(R.id.menu_screen);
        hudTop = findViewById(R.id.hud_top);
        panelActions = findViewById(R.id.panel_actions);
        howToPlay = findViewById(R.id.txt_how_to_play);
        gameOverCard = findViewById(R.id.game_over_card);
        gameOverScore = findViewById(R.id.txt_game_over_score);

        btnStart = findViewById(R.id.btn_start_game);
        Button btnHow = findViewById(R.id.btn_how_to_play);

        btnHow.setOnClickListener(v -> {
            howToPlay.setVisibility(howToPlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        btnStart.setOnClickListener(v -> startGameFromMenu());

        showMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameStarted) {
            gameView.onHostResume();
            handler.post(statePoller);
            if (bgmPlayer != null) bgmPlayer.playLoop(R.raw.bgm, 0.35f);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(statePoller);
        if (gameStarted) gameView.onHostPause();
        if (bgmPlayer != null) bgmPlayer.pause();
        super.onPause();
    }

    private void showMenu() {
        gameStarted = false;
        handler.removeCallbacks(statePoller);
        menuScreen.setVisibility(View.VISIBLE);
        gameView.setVisibility(View.GONE);
        hudTop.setVisibility(View.GONE);
        panelActions.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        btnStart.setText(getString(R.string.btn_start));
        if (bgmPlayer != null) bgmPlayer.pause();
    }

    private void showGameOver(int score, int coins, int deliveries, int combo, int level) {
        showMenu();
        gameOverCard.setVisibility(View.VISIBLE);
        gameOverScore.setText("Score " + score + "  Coins " + coins + "  Deliveries " + deliveries + "  Combo " + combo + "  Lv " + level);
        btnStart.setText(getString(R.string.btn_restart));
    }

    private void startGameFromMenu() {
        if (!gameStarted) {
            gameStarted = true;
            gameView.bindActivity(this);
        }
        menuScreen.setVisibility(View.GONE);
        gameView.setVisibility(View.VISIBLE);
        hudTop.setVisibility(View.VISIBLE);
        panelActions.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.GONE);
        gameView.startGame();
        gameView.onHostResume();
        handler.removeCallbacks(statePoller);
        handler.post(statePoller);
        if (bgmPlayer != null) {
            bgmPlayer.setMuted(muted);
            bgmPlayer.playLoop(R.raw.bgm, 0.35f);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (bgmPlayer != null) bgmPlayer.setMuted(muted);
    }
}
