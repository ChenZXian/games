package com.android.boot.core;

import android.os.SystemClock;

import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
    private final GameView gameView;
    private volatile boolean running;

    public GameLoopThread(GameView gameView) {
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        long last = SystemClock.elapsedRealtimeNanos();
        while (running) {
            long now = SystemClock.elapsedRealtimeNanos();
            float dt = (now - last) / 1000000000f;
            last = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            gameView.step(dt);
        }
    }
}
