package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.core.GameView;

public class MainActivity extends AppCompatActivity implements GameView.GameListener {
    private GameView gameView;
    private LinearLayout menuLayout;
    private LinearLayout hudLayout;
    private LinearLayout pauseLayout;
    private LinearLayout gameOverLayout;
    private TextView textScore;
    private TextView textLives;
    private TextView textEnergy;
    private TextView textFinalScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.game_view);
        menuLayout = findViewById(R.id.menu_layout);
        hudLayout = findViewById(R.id.hud_layout);
        pauseLayout = findViewById(R.id.pause_layout);
        gameOverLayout = findViewById(R.id.game_over_layout);
        textScore = findViewById(R.id.text_score);
        textLives = findViewById(R.id.text_lives);
        textEnergy = findViewById(R.id.text_energy);
        textFinalScore = findViewById(R.id.text_final_score);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnPause = findViewById(R.id.btn_pause);
        Button btnResume = findViewById(R.id.btn_resume);
        Button btnRestart = findViewById(R.id.btn_restart);

        gameView.setGameListener(this);

        btnStart.setOnClickListener(v -> {
            menuLayout.setVisibility(View.GONE);
            gameOverLayout.setVisibility(View.GONE);
            pauseLayout.setVisibility(View.GONE);
            hudLayout.setVisibility(View.VISIBLE);
            gameView.startGame();
        });

        btnPause.setOnClickListener(v -> {
            if (gameView.isPlaying()) {
                gameView.pauseGame();
                pauseLayout.setVisibility(View.VISIBLE);
                hudLayout.setVisibility(View.GONE);
            }
        });

        btnResume.setOnClickListener(v -> {
            gameView.resumeGame();
            pauseLayout.setVisibility(View.GONE);
            hudLayout.setVisibility(View.VISIBLE);
        });

        btnRestart.setOnClickListener(v -> {
            gameOverLayout.setVisibility(View.GONE);
            pauseLayout.setVisibility(View.GONE);
            hudLayout.setVisibility(View.VISIBLE);
            gameView.startGame();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView.isPlaying()) {
            gameView.pauseGame();
            pauseLayout.setVisibility(View.VISIBLE);
            hudLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScoreChanged(int score) {
        runOnUiThread(() -> textScore.setText("Score: " + score));
    }

    @Override
    public void onLivesChanged(int lives) {
        runOnUiThread(() -> textLives.setText("Lives: " + lives));
    }

    @Override
    public void onEnergyChanged(int energy) {
        runOnUiThread(() -> textEnergy.setText("Energy: " + energy));
    }

    @Override
    public void onGameOver(int finalScore) {
        runOnUiThread(() -> {
            textFinalScore.setText("Score: " + finalScore);
            gameOverLayout.setVisibility(View.VISIBLE);
            hudLayout.setVisibility(View.GONE);
            pauseLayout.setVisibility(View.GONE);
        });
    }

    @Override
    public void onMenuState() {
        runOnUiThread(() -> {
            menuLayout.setVisibility(View.VISIBLE);
            hudLayout.setVisibility(View.GONE);
            pauseLayout.setVisibility(View.GONE);
            gameOverLayout.setVisibility(View.GONE);
        });
    }
}
