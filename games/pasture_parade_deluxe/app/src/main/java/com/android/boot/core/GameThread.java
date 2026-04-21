package com.android.boot.core;

import android.view.SurfaceHolder;
import com.android.boot.audio.TonePlayer;

public class GameThread extends Thread {
    private final SurfaceHolder holder;
    private final RanchWorld world;
    private final GameRenderer renderer;
    private final TonePlayer tonePlayer;
    private volatile boolean running;
    private long lastNs;
    private int width;
    private int height;

    public GameThread(SurfaceHolder holder, RanchWorld world, GameRenderer renderer, TonePlayer tonePlayer) {
        this.holder = holder;
        this.world = world;
        this.renderer = renderer;
        this.tonePlayer = tonePlayer;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void run() {
        lastNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastNs) / 1000000000f;
            lastNs = now;
            if (dt > 0.05f) dt = 0.05f;
            world.update(dt);
            if (world.neglect > 0.95f) tonePlayer.warning();
            android.graphics.Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                renderer.render(canvas, world, width, height);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
