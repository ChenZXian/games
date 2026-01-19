package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity implements GameView.GameEventListener {
    private GameView gameView;
    private View menuOverlay;
    private View pauseOverlay;
    private View gameOverOverlay;
    private TextView scoreText;
    private TextView bestText;
    private TextView distanceText;
    private TextView energyText;
    private TextView speedText;
    private TextView shieldText;
    private TextView finalScoreText;
    private TextView finalBestText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.game_view);
        menuOverlay = findViewById(R.id.overlay_menu);
        pauseOverlay = findViewById(R.id.overlay_pause);
        gameOverOverlay = findViewById(R.id.overlay_game_over);
        scoreText = findViewById(R.id.text_score);
        bestText = findViewById(R.id.text_best);
        distanceText = findViewById(R.id.text_distance);
        energyText = findViewById(R.id.text_energy);
        speedText = findViewById(R.id.text_speed);
        shieldText = findViewById(R.id.text_shield);
        finalScoreText = findViewById(R.id.text_final_score);
        finalBestText = findViewById(R.id.text_final_best);

        Button startButton = findViewById(R.id.btn_start);
        Button resumeButton = findViewById(R.id.btn_resume);
        Button restartButtonPause = findViewById(R.id.btn_restart_pause);
        Button restartButtonOver = findViewById(R.id.btn_restart_over);
        Button backToMenuButton = findViewById(R.id.btn_back_menu);
        View pauseButton = findViewById(R.id.btn_pause);
        View jumpButton = findViewById(R.id.btn_jump);
        View dashButton = findViewById(R.id.btn_dash);

        gameView.setGameEventListener(this);

        startButton.setOnClickListener(v -> gameView.startGame());
        resumeButton.setOnClickListener(v -> gameView.resumeGame());
        restartButtonPause.setOnClickListener(v -> gameView.restartGame());
        restartButtonOver.setOnClickListener(v -> gameView.restartGame());
        backToMenuButton.setOnClickListener(v -> gameView.backToMenu());
        pauseButton.setOnClickListener(v -> gameView.pauseGame());

        jumpButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.onJumpPress();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                gameView.onJumpRelease();
                return true;
            }
            return false;
        });

        dashButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.onDash();
                return true;
            }
            return false;
        });

        View root = findViewById(R.id.root_container);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.onJumpPress();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                gameView.onJumpRelease();
                return true;
            }
            return false;
        });

        updateOverlay(GameState.MENU);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeIfVisible();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameView.release();
    }

    @Override
    public void onStateChanged(GameState state) {
        runOnUiThread(() -> updateOverlay(state));
    }

    @Override
    public void onHudUpdate(String score, String best, String distance, String energy, String speed, String shield) {
        runOnUiThread(() -> {
            scoreText.setText(score);
            bestText.setText(best);
            distanceText.setText(distance);
            energyText.setText(energy);
            speedText.setText(speed);
            shieldText.setText(shield);
        });
    }

    @Override
    public void onGameOver(String finalScore, String finalBest) {
        runOnUiThread(() -> {
            finalScoreText.setText(finalScore);
            finalBestText.setText(finalBest);
        });
    }

    private void updateOverlay(GameState state) {
        menuOverlay.setVisibility(state == GameState.MENU ? View.VISIBLE : View.GONE);
        pauseOverlay.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        gameOverOverlay.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
    }
}
