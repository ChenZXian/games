package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.BgmPlayer;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private ScrollView menuScreen;
    private TextView howToPlay;
    private BgmPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bgmPlayer = new BgmPlayer(this);
        gameView = findViewById(R.id.game_view);
        menuScreen = findViewById(R.id.menu_screen);
        howToPlay = findViewById(R.id.txt_how_to_play_menu);
        Button btnStart = findViewById(R.id.btn_start_game);
        Button btnHowToPlay = findViewById(R.id.btn_how_to_play_menu);

        btnStart.setOnClickListener(v -> startGame());
        btnHowToPlay.setOnClickListener(v -> {
            if (howToPlay.getVisibility() == View.VISIBLE) {
                howToPlay.setVisibility(View.GONE);
            } else {
                howToPlay.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
        if (menuScreen.getVisibility() != View.VISIBLE && bgmPlayer != null) {
            bgmPlayer.playLoop(R.raw.bgm, 0.35f);
        }
    }

    @Override
    protected void onPause() {
        gameView.onHostPause();
        if (bgmPlayer != null) bgmPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bgmPlayer != null) bgmPlayer.release();
        super.onDestroy();
    }

    private void startGame() {
        menuScreen.setVisibility(View.GONE);
        gameView.setVisibility(View.VISIBLE);
        gameView.startFromMenu();
        if (bgmPlayer != null) bgmPlayer.playLoop(R.raw.bgm, 0.35f);
    }
}
