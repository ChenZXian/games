package com.android.boot.engine;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import com.android.boot.input.InputState;
import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
    private final SurfaceHolder holder;
    private final GameView view;
    private final GameWorld world;
    private final InputState input;
    private boolean running;
    private long lastNs;

    public GameLoopThread(SurfaceHolder holder, GameView view, GameWorld world, InputState input) {
        this.holder = holder;
        this.view = view;
        this.world = world;
        this.input = input;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        lastNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastNs) / 1000000000f;
            lastNs = now;
            if (dt > 0.033f) dt = 0.033f;
            if (dt < 0.001f) dt = 0.001f;
            world.update(dt, input);
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                view.render(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
