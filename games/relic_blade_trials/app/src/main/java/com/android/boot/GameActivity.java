package com.android.boot;

import android.app.Activity;
import android.os.Bundle;
import com.android.boot.audio.BgmPlayer;
import com.android.boot.ui.GameView;

public class GameActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int stageIndex = getIntent().getIntExtra(LevelSelectActivity.EXTRA_STAGE_INDEX, 1);
        gameView = new GameView(this, stageIndex);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BgmPlayer.playLoop(this, R.raw.bgm, 0.42f);
        gameView.onResumeView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BgmPlayer.pause();
        gameView.onPauseView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            BgmPlayer.release();
        }
    }
}
