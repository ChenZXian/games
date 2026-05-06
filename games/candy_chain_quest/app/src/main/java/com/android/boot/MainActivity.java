package com.android.boot;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.BgmPlayer;
import com.android.boot.audio.SoundController;
import com.android.boot.core.GameEventListener;
import com.android.boot.core.GameState;
import com.android.boot.core.GameStateMachine;
import com.android.boot.core.GameSession;
import com.android.boot.databinding.ActivityMainBinding;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameEventListener {
    private ActivityMainBinding binding;
    private GameView gameView;
    private GameSession gameSession;
    private SoundController soundController;
    private BgmPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        soundController = new SoundController();
        bgmPlayer = new BgmPlayer(this);
        gameSession = new GameSession(this, soundController);
        gameSession.setEventListener(this);
        gameView = new GameView(this, gameSession);
        binding.gameContainer.addView(gameView);
        wireUi();
        refreshUi();
    }

    private void wireUi() {
        binding.startButton.setOnClickListener(v -> {
            gameSession.startNewRun();
            refreshUi();
        });
        binding.helpButton.setOnClickListener(v -> showHelp());
        binding.pauseButton.setOnClickListener(v -> {
            if (gameSession.getStateMachine().isPlaying()) {
                gameSession.pauseGame();
                refreshUi();
            }
        });
        binding.resumeButton.setOnClickListener(v -> {
            gameSession.resumeGame();
            refreshUi();
        });
        binding.restartButton.setOnClickListener(v -> {
            gameSession.restartLevel();
            refreshUi();
        });
        binding.menuButton.setOnClickListener(v -> {
            gameSession.backToMenu();
            refreshUi();
        });
        binding.muteButton.setOnClickListener(v -> {
            soundController.toggleMute();
            bgmPlayer.setMuted(soundController.isMuted());
            if (!soundController.isMuted()) {
                bgmPlayer.playLoop(R.raw.bgm, 0.4f);
            }
            binding.muteButton.setImageResource(soundController.isMuted() ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
        });
        binding.gameOverRestartButton.setOnClickListener(v -> {
            gameSession.restartLevel();
            refreshUi();
        });
        binding.gameOverMenuButton.setOnClickListener(v -> {
            gameSession.backToMenu();
            refreshUi();
        });
        binding.nextLevelButton.setOnClickListener(v -> {
            gameSession.advanceAfterResult();
            refreshUi();
        });
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.btn_how_to_play))
                .setMessage(getString(R.string.help_body))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!soundController.isMuted()) {
            bgmPlayer.playLoop(R.raw.bgm, 0.4f);
        }
        if (gameView != null) {
            gameView.onResume();
        }
        if (gameSession != null && gameSession.getStateMachine().getState() == GameState.PAUSED && !binding.pauseOverlay.isShown()) {
            gameSession.resumeGame();
        }
    }

    @Override
    protected void onPause() {
        bgmPlayer.pause();
        if (gameView != null) {
            gameView.onPause();
        }
        if (gameSession != null && gameSession.getStateMachine().isPlaying()) {
            gameSession.pauseGame();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
        }
        super.onDestroy();
    }

    @Override
    public void onUiChanged() {
        runOnUiThread(this::refreshUi);
    }

    private void refreshUi() {
        binding.scoreValue.setText(String.valueOf(gameSession.getScoreManager().getScore()));
        binding.turnValue.setText(gameSession.getLevelManager().getHudTurnText());
        binding.levelTitle.setText(gameSession.getLevelManager().getLevelTitle());
        binding.goalsValue.setText(gameSession.getLevelManager().buildGoalSummary());
        GameStateMachine machine = gameSession.getStateMachine();
        setOverlayVisible(binding.menuOverlay, machine.getState() == GameState.MENU);
        setOverlayVisible(binding.pauseOverlay, machine.getState() == GameState.PAUSED);
        setOverlayVisible(binding.gameOverOverlay, machine.getState() == GameState.GAME_OVER);
        if (machine.getState() == GameState.GAME_OVER) {
            binding.resultTitle.setText(gameSession.getLastResultWin() ? getString(R.string.label_win) : getString(R.string.label_lose));
            binding.resultBody.setText(gameSession.getResultSummary());
            binding.nextLevelButton.setVisibility(gameSession.getLastResultWin() ? View.VISIBLE : View.GONE);
        }
        binding.pauseButton.setVisibility(machine.isPlaying() ? View.VISIBLE : View.INVISIBLE);
        binding.topHud.setAlpha(machine.getState() == GameState.MENU ? 0.2f : 1f);
        binding.bottomPanel.setAlpha(machine.getState() == GameState.MENU ? 0.2f : 1f);
    }

    private void setOverlayVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
