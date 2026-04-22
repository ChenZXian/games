package com.android.boot.core;

import android.os.SystemClock;

import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
    private final GameView gameView;
    private boolean running = true;
    private boolean paused;
    private long lastFrameTime;

    public GameLoopThread(GameView gameView) {
        this.gameView = gameView;
    }

    @Override
    public void run() {
        lastFrameTime = SystemClock.elapsedRealtime();
        while (running) {
            long now = SystemClock.elapsedRealtime();
            float dt = (now - lastFrameTime) / 1000f;
            lastFrameTime = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            if (!paused) {
                gameView.tick(dt);
            }
            gameView.render();
            SystemClock.sleep(16L);
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void requestStop() {
        running = false;
        interrupt();
    }
}
