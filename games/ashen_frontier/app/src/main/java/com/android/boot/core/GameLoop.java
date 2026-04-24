package com.android.boot.core;

public class GameLoop extends Thread {
    public interface Callback {
        void step(float dt);
    }

    private final Callback callback;
    private volatile boolean running;

    public GameLoop(Callback callback) {
        this.callback = callback;
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        running = true;
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1000000000f;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            if (dt < 0.001f) {
                dt = 0.001f;
            }
            last = now;
            callback.step(dt);
            try {
                sleep(8);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
