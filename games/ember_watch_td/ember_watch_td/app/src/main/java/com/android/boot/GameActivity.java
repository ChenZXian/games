package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import com.android.boot.audio.BgmPlayer;
import com.android.boot.ui.GameView;

public class GameActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int levelIndex = getIntent().getIntExtra(LevelSelectActivity.EXTRA_LEVEL_INDEX, 1);
        FrameLayout root = new FrameLayout(this);
        gameView = new GameView(this, levelIndex);
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BgmPlayer.pause();
        if (gameView != null) {
            gameView.onHostPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BgmPlayer.playLoop(this, R.raw.bgm, 0.42f);
        if (gameView != null) {
            gameView.onHostResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            BgmPlayer.release();
        }
    }
}
