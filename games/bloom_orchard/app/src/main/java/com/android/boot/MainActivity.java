package com.android.boot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.BgmPlayer;
import com.android.boot.audio.SoundController;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;
import com.android.boot.ui.UiOverlayController;
import com.android.boot.ui.panel.AchievementPanelController;
import com.android.boot.ui.panel.CodexPanelController;
import com.android.boot.ui.panel.DailyTaskPanelController;
import com.android.boot.ui.panel.LoginRewardPanelController;
import com.android.boot.ui.panel.MenuPanelController;
import com.android.boot.ui.panel.OrderPanelController;
import com.android.boot.ui.panel.StoragePanelController;

public class MainActivity extends AppCompatActivity {
    private GameSession session;
    private GameView gameView;
    private ScrollView menuScreen;
    private View topHud;
    private View bottomBar;
    private TextView howToPlay;
    private View gameOverCard;
    private TextView gameOverScore;
    private BgmPlayer bgmPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statePoller = new Runnable() {
        @Override
        public void run() {
            if (session.state == GameState.GAME_OVER) {
                showGameOver();
                return;
            }
            if (session.state == GameState.PLAYING || session.state == GameState.PAUSED) {
                handler.postDelayed(this, 250);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        session = new GameSession(this);
        bgmPlayer = new BgmPlayer(this);
        gameView = findViewById(R.id.game_view);
        menuScreen = findViewById(R.id.menu_screen);
        topHud = findViewById(R.id.top_hud);
        bottomBar = findViewById(R.id.bottom_bar);
        howToPlay = findViewById(R.id.txt_how_to_play);
        gameOverCard = findViewById(R.id.game_over_card);
        gameOverScore = findViewById(R.id.txt_game_over_score);
        TextView coins = findViewById(R.id.tv_coins);
        TextView level = findViewById(R.id.tv_level);
        TextView timer = findViewById(R.id.tv_timer);
        UiOverlayController overlay = new UiOverlayController(coins, level, timer);
        gameView.bind(session, overlay);

        SoundController sound = new SoundController();
        MenuPanelController menu = new MenuPanelController(this);
        AchievementPanelController ach = new AchievementPanelController(this);
        StoragePanelController storage = new StoragePanelController(this);
        OrderPanelController orders = new OrderPanelController(this);
        CodexPanelController codex = new CodexPanelController(this);
        DailyTaskPanelController tasks = new DailyTaskPanelController(this);
        LoginRewardPanelController login = new LoginRewardPanelController(this);
        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnHowToPlay = findViewById(R.id.btn_how_to_play);
        Button btnRestartGameOver = findViewById(R.id.btn_restart_game_over);
        Button btnMenuGameOver = findViewById(R.id.btn_menu_game_over);

        Button btnSeed = findViewById(R.id.btn_seed);
        Button btnWater = findViewById(R.id.btn_water);
        Button btnFertilizer = findViewById(R.id.btn_fertilizer);
        ImageButton btnPause = findViewById(R.id.btn_pause);

        btnStartGame.setOnClickListener(v -> startGameFromMenu());
        btnHowToPlay.setOnClickListener(v -> {
            if (howToPlay.getVisibility() == View.VISIBLE) {
                howToPlay.setVisibility(View.GONE);
            } else {
                howToPlay.setVisibility(View.VISIBLE);
            }
        });
        btnRestartGameOver.setOnClickListener(v -> startGameFromMenu());
        btnMenuGameOver.setOnClickListener(v -> showMenu());

        btnSeed.setOnClickListener(v -> {
            gameView.setTool(0);
            sound.click();
            menu.showHowToPlay();
        });
        btnWater.setOnClickListener(v -> {
            gameView.setTool(1);
            sound.click();
            tasks.show(session);
        });
        btnFertilizer.setOnClickListener(v -> {
            gameView.setTool(2);
            sound.click();
            orders.show(session);
            storage.show(session);
            ach.show(session);
            codex.show();
            login.show(session);
        });
        btnPause.setOnClickListener(v -> {
            sound.click();
            session.state = session.state == GameState.PAUSED ? GameState.PLAYING : GameState.PAUSED;
        });

        showMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statePoller);
        if (bgmPlayer != null) bgmPlayer.pause();
        session.progression.save();
        session.storage.save();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session.state == GameState.PLAYING || session.state == GameState.PAUSED) {
            if (bgmPlayer != null) bgmPlayer.playLoop(R.raw.bgm, 0.35f);
        }
    }

    @Override
    protected void onDestroy() {
        if (bgmPlayer != null) bgmPlayer.release();
        super.onDestroy();
    }

    private void showMenu() {
        handler.removeCallbacks(statePoller);
        session.state = GameState.MENU;
        menuScreen.setVisibility(View.VISIBLE);
        gameView.setVisibility(View.GONE);
        topHud.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
        howToPlay.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        if (bgmPlayer != null) bgmPlayer.pause();
    }

    private void startGameFromMenu() {
        session.start();
        menuScreen.setVisibility(View.GONE);
        gameView.setVisibility(View.VISIBLE);
        topHud.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        howToPlay.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        handler.removeCallbacks(statePoller);
        handler.post(statePoller);
        if (bgmPlayer != null) bgmPlayer.playLoop(R.raw.bgm, 0.35f);
    }

    private void showGameOver() {
        int coins = session.progression.coins;
        int level = session.progression.level;
        int combo = session.combo;
        int bestBeauty = session.bestBeauty;
        gameOverScore.setText("Coins " + coins + "  Level " + level + "  Best Beauty " + bestBeauty + "  Combo " + combo);
        gameOverCard.setVisibility(View.VISIBLE);
        topHud.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
        session.state = GameState.PAUSED;
    }
}
