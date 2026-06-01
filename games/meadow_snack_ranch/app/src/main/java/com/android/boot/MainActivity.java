package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private RanchGameView gameView;
    private LinearLayout menuOverlay;
    private LinearLayout pauseOverlay;
    private Button pauseButton;
    private RanchAudio audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.menu_overlay);
        pauseOverlay = findViewById(R.id.pause_overlay);
        pauseButton = findViewById(R.id.pause_button);
        audio = new RanchAudio(this);
        Button startButton = findViewById(R.id.start_button);
        Button helpButton = findViewById(R.id.help_button);
        Button resumeButton = findViewById(R.id.resume_button);
        Button restartButton = findViewById(R.id.restart_button);
        gameView.setListener(new RanchGameView.GameListener() {
            @Override
            public void onGameOver() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pauseOverlay.setVisibility(View.VISIBLE);
                        pauseButton.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onSound(String role) {
                audio.playSound(role);
            }

            @Override
            public void onDayClear() {
                audio.playSound("day_clear");
            }
        });
        audio.playMenu();
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.playSound("ui_click");
                menuOverlay.setVisibility(View.GONE);
                pauseOverlay.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                gameView.startGame();
                audio.playGame();
            }
        });
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.playSound("ui_click");
                gameView.showHelpToast();
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.playSound("ui_click");
                pauseOverlay.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                gameView.pauseGame();
                audio.pause();
            }
        });
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.playSound("ui_click");
                pauseOverlay.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                gameView.resumeGame();
                audio.resume();
            }
        });
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.playSound("ui_click");
                pauseOverlay.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                gameView.startGame();
                audio.playGame();
            }
        });
        hideSystemUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resumeLoop();
        }
        if (audio != null) {
            audio.resume();
        }
        hideSystemUi();
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.pauseLoop();
        }
        if (audio != null) {
            audio.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (audio != null) {
            audio.release();
        }
        super.onDestroy();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
