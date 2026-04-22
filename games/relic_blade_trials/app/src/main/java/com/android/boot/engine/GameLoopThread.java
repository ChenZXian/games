package com.android.boot.engine;

import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
    private final GameView view;
    private boolean running;

    public GameLoopThread(GameView view) {
        this.view = view;
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
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            view.tick(dt);
            view.drawFrame();
        }
    }
}
