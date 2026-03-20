package com.android.boot.core;

import android.os.SystemClock;

import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
    private static final float MAX_DELTA = 1f / 24f;
    private final GameView gameView;
    private boolean running;

    public GameLoopThread(GameView gameView) {
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        long last = SystemClock.elapsedRealtime();
        while (running) {
            long now = SystemClock.elapsedRealtime();
            float delta = (now - last) / 1000f;
            last = now;
            if (delta > MAX_DELTA) {
                delta = MAX_DELTA;
            }
            gameView.step(delta);
            gameView.drawFrame();
            SystemClock.sleep(16L);
        }
    }
}
