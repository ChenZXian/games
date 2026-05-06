package com.android.boot.ui;

import android.os.SystemClock;
import android.view.SurfaceHolder;

public class GameLoopThread extends Thread {
    private static final float MAX_DELTA = 0.033f;
    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean running = true;

    public GameLoopThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void requestStopLoop() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        long lastTime = SystemClock.elapsedRealtime();
        while (running) {
            if (!surfaceHolder.getSurface().isValid()) {
                SystemClock.sleep(10);
                continue;
            }
            long now = SystemClock.elapsedRealtime();
            float delta = Math.min((now - lastTime) / 1000f, MAX_DELTA);
            lastTime = now;
            gameView.step(delta);
            android.graphics.Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    gameView.render(canvas);
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
