package com.android.boot.core;

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
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1000000000f;
            last = now;
            if (dt > 0.05f) dt = 0.05f;
            gameView.step(dt);
        }
    }
}
