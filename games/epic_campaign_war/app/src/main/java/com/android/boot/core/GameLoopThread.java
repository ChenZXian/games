package com.android.boot.core;

import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
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
        long previous = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - previous) / 1000000000f;
            previous = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            gameView.step(dt);
        }
    }
}
