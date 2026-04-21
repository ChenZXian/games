package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        gameView.bindActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }

    @Override
    protected void onPause() {
        gameView.onHostPause();
        super.onPause();
    }
}
