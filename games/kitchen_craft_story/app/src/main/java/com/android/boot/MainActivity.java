package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import com.android.boot.ui.GameView;

public class MainActivity extends Activity {
    private GameView gameView;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        FrameLayout root = findViewById(R.id.root);
        gameView = new GameView(this);
        root.addView(gameView);
    }

    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
    }

    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
    }

    protected void onDestroy() {
        if (gameView != null) {
            gameView.releaseAudio();
        }
        super.onDestroy();
    }
}
