package com.android.boot;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private LinearLayout menuPanel;
    private LinearLayout gameHud;
    private Handler checkHandler;
    private Runnable checkRunnable;
    private BgmPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuPanel = findViewById(R.id.menuPanel);
        gameHud = findViewById(R.id.gameHud);
        if (gameView == null) {
            return;
        }
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnHowToPlay = findViewById(R.id.btn_how_to_play);
        Button resumeButton = findViewById(R.id.btn_resume);
        Button restartButton = findViewById(R.id.btn_restart);
        Button menuButton = findViewById(R.id.btn_menu);
        Button muteButton = findViewById(R.id.btn_mute);
        Button helpButton = findViewById(R.id.btn_help);
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> hideMenuAndStart());
        }
        if (btnHowToPlay != null) {
            btnHowToPlay.setOnClickListener(v -> openHowTo());
        }
        if (resumeButton != null) {
            resumeButton.setOnClickListener(v -> {
                if (gameView != null) gameView.resumeGame();
            });
        }
        if (restartButton != null) {
            restartButton.setOnClickListener(v -> {
                if (gameView != null) gameView.restartLevel();
            });
        }
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> showMenu());
        }
        bgmPlayer = new BgmPlayer();
        bgmPlayer.start(this);
        if (muteButton != null) {
            muteButton.setOnClickListener(v -> {
                bgmPlayer.setMuted(!bgmPlayer.isMuted());
                updateMuteButton();
            });
            updateMuteButton();
        }
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> openHowTo());
        }
        showMenu();
        checkHandler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameView != null && gameView.checkLevelWon()) {
                    showLevelCompleteDialog();
                }
                checkHandler.postDelayed(this, 500);
            }
        };
        checkHandler.post(checkRunnable);
    }

    public void showMenu() {
        if (menuPanel != null) {
            menuPanel.setVisibility(View.VISIBLE);
        }
        if (gameHud != null) {
            gameHud.setVisibility(View.GONE);
        }
        if (gameView != null) {
            gameView.backToMenu();
        }
    }

    public void hideMenuAndStart() {
        if (menuPanel != null) {
            menuPanel.setVisibility(View.GONE);
        }
        if (gameHud != null) {
            gameHud.setVisibility(View.VISIBLE);
        }
        if (gameView != null) {
            gameView.startCampaign();
        }
    }

    private void openHowTo() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_play_title)
                .setMessage(R.string.how_to_play_text)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLevelCompleteDialog() {
        if (gameView == null) {
            return;
        }
        String message = getString(R.string.level_complete_message);
        new AlertDialog.Builder(this)
                .setTitle(R.string.level_complete_title)
                .setMessage(message)
                .setPositiveButton(R.string.next_level, (dialog, which) -> {
                    if (gameView != null) {
                        gameView.proceedToNextLevel();
                    }
                })
                .setNegativeButton(R.string.back_to_menu, (dialog, which) -> {
                    showMenu();
                })
                .setCancelable(false)
                .show();
    }

    private void updateMuteButton() {
        Button muteButton = findViewById(R.id.btn_mute);
        if (muteButton != null) {
            muteButton.setText(bgmPlayer.isMuted() ? "🔇" : "🔊");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bgmPlayer != null) {
            bgmPlayer.resume();
        }
        if (gameView != null) {
            gameView.onHostResume();
        }
    }

    @Override
    protected void onPause() {
        if (checkHandler != null && checkRunnable != null) {
            checkHandler.removeCallbacks(checkRunnable);
        }
        if (bgmPlayer != null) {
            bgmPlayer.pause();
        }
        if (gameView != null) {
            gameView.onHostPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (checkHandler != null && checkRunnable != null) {
            checkHandler.removeCallbacks(checkRunnable);
        }
        if (bgmPlayer != null) {
            bgmPlayer.stop();
        }
        super.onDestroy();
    }
}
