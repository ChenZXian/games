package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPauseView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResumeView();
    }
}
