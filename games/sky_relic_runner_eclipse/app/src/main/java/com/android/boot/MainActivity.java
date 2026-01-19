package com.android.boot;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onResumeView();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onPauseView();
        }
        super.onPause();
    }
}
