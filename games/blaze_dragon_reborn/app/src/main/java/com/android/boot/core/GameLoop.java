package com.android.boot.core;

public class GameLoop extends Thread {
    public interface ErrorHandler {
        void onLoopFailure(Throwable throwable);
    }

    private static final long FRAME_NS = 16666667L;
    public interface Callback {
        void step(float dt);
    }

    private final Callback callback;
    private volatile boolean running = true;

    public GameLoop(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            float dt = (frameStart - last) / 1000000000f;
            last = frameStart;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            try {
                callback.step(dt);
            } catch (Throwable throwable) {
                running = false;
                if (callback instanceof ErrorHandler) {
                    ((ErrorHandler) callback).onLoopFailure(throwable);
                }
            }
            long remaining = FRAME_NS - (System.nanoTime() - frameStart);
            if (remaining > 0L && running) {
                try {
                    Thread.sleep(remaining / 1000000L, (int) (remaining % 1000000L));
                } catch (InterruptedException interruptedException) {
                    interrupt();
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
