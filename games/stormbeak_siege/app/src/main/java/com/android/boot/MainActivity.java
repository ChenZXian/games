package com.android.boot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.AudioController;
import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameDefs;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            handler.postDelayed(this, 120L);
        }
    };

    private GameView gameView;
    private AudioController audioController;
    private TextView titleView;
    private TextView subtitleView;
    private TextView statusBirdsView;
    private TextView statusStarsView;
    private TextView objectiveView;
    private TextView menuInfoView;
    private TextView resultTitleView;
    private TextView resultBodyView;
    private TextView pauseStageView;
    private TextView pauseBodyView;
    private TextView helpBodyView;
    private Button nextButton;
    private Button muteButton;
    private LinearLayout menuOverlay;
    private LinearLayout resultOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout helpOverlay;
    private int lastState = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioController = new AudioController(this);
        bindViews();
        bindActions();
        gameView.setMuted(false);
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        audioController.stopMusic();
        gameView.onHostPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        audioController.release();
        gameView.onHostDestroy();
        super.onDestroy();
    }

    private void bindViews() {
        gameView = findViewById(R.id.gameView);
        titleView = findViewById(R.id.titleView);
        subtitleView = findViewById(R.id.subtitleView);
        statusBirdsView = findViewById(R.id.statusBirdsView);
        statusStarsView = findViewById(R.id.statusStarsView);
        objectiveView = findViewById(R.id.objectiveView);
        menuInfoView = findViewById(R.id.menuInfoView);
        resultTitleView = findViewById(R.id.resultTitleView);
        resultBodyView = findViewById(R.id.resultBodyView);
        pauseStageView = findViewById(R.id.pauseStageView);
        pauseBodyView = findViewById(R.id.pauseBodyView);
        helpBodyView = findViewById(R.id.helpBodyView);
        nextButton = findViewById(R.id.nextButton);
        muteButton = findViewById(R.id.muteButton);
        menuOverlay = findViewById(R.id.menuOverlay);
        resultOverlay = findViewById(R.id.resultOverlay);
        pauseOverlay = findViewById(R.id.pauseOverlay);
        helpOverlay = findViewById(R.id.helpOverlay);
    }

    private void bindActions() {
        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.startCampaign();
                refreshUi();
            }
        });
        findViewById(R.id.helpButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                helpOverlay.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.helpCloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                helpOverlay.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.restartButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.restartLevel();
                refreshUi();
            }
        });
        findViewById(R.id.menuButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.backToMenu();
                refreshUi();
            }
        });
        findViewById(R.id.pauseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.pauseGame();
                refreshUi();
            }
        });
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newMuted = !gameView.isMuted();
                gameView.setMuted(newMuted);
                audioController.setMuted(newMuted);
                if (!newMuted) {
                    syncMusic(gameView.getSnapshot().state);
                }
                refreshUi();
            }
        });
        findViewById(R.id.resumeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.resumeGame();
                refreshUi();
            }
        });
        findViewById(R.id.pauseRestartButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.restartLevel();
                refreshUi();
            }
        });
        findViewById(R.id.pauseMenuButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.backToMenu();
                refreshUi();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.advanceAfterResult();
                refreshUi();
            }
        });
        findViewById(R.id.resultMenuButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.playSfx("click", "audio/sfx_click.wav");
                gameView.backToMenu();
                refreshUi();
            }
        });
    }

    private void refreshUi() {
        GameEngine.HudSnapshot snapshot = gameView.getSnapshot();
        titleView.setText(snapshot.chapterName);
        subtitleView.setText(snapshot.stageName + "  " + snapshot.weatherName);
        statusBirdsView.setText("Birds " + snapshot.birdsLeft);
        statusStarsView.setText("Score " + snapshot.score);
        objectiveView.setText(snapshot.objective + "  |  Current " + snapshot.currentBird + "  |  Next " + snapshot.nextBird);
        menuInfoView.setText("Landscape storm siege  Tap help for bird skills and weather rules");
        pauseStageView.setText(snapshot.stageName);
        pauseBodyView.setText(snapshot.objective + "  |  " + snapshot.weatherName);
        helpBodyView.setText("Drag back from the sling to aim.\nRelease to launch.\nTap once in flight to trigger the active bird skill.\nGust boosts speed, Ember bursts, Bolt chains into metal, Frost cushions impact, and Ram pierces heavy walls.");
        resultTitleView.setText(snapshot.resultTitle);
        resultBodyView.setText(snapshot.resultBody);
        nextButton.setText(snapshot.resultWin ? (snapshot.levelIndex < snapshot.levelCount - 1 ? R.string.btn_next : R.string.btn_menu) : R.string.btn_restart);
        muteButton.setText(gameView.isMuted() ? getString(R.string.btn_unmute) : getString(R.string.btn_mute));
        menuOverlay.setVisibility(snapshot.state == GameDefs.MENU ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(snapshot.state == GameDefs.PAUSED ? View.VISIBLE : View.GONE);
        resultOverlay.setVisibility(snapshot.state == GameDefs.GAME_OVER ? View.VISIBLE : View.GONE);
        syncMusic(snapshot.state);
        lastState = snapshot.state;
    }

    private void syncMusic(int state) {
        if (gameView.isMuted()) {
            audioController.stopMusic();
            return;
        }
        if (state == lastState) {
            return;
        }
        if (state == GameDefs.MENU) {
            audioController.playMusic("audio/bgm_menu.wav", true);
        } else if (state == GameDefs.PLAYING || state == GameDefs.PAUSED) {
            audioController.playMusic("audio/bgm_play.wav", true);
        } else if (state == GameDefs.GAME_OVER) {
            audioController.playMusic("audio/bgm_climax.wav", false);
        }
    }
}
