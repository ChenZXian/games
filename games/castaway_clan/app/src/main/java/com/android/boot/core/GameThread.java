package com.android.boot.core;

import android.view.SurfaceHolder;

import com.android.boot.audio.TonePlayer;

public class GameThread extends Thread {
    private final SurfaceHolder holder;
    private final CastawayWorld world;
    private final GameRenderer renderer;
    private final TonePlayer tonePlayer;
    private volatile boolean running;
    private long lastNs;
    private int width;
    private int height;
    private float stickX;
    private float stickY;
    private boolean actionDown;

    public GameThread(SurfaceHolder holder, CastawayWorld world, GameRenderer renderer, TonePlayer tonePlayer) {
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

    public void setStick(float x, float y) {
        stickX = x;
        stickY = y;
    }

    public void setActionDown(boolean actionDown) {
        this.actionDown = actionDown;
    }

    @Override
    public void run() {
        lastNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastNs) / 1000000000f;
            lastNs = now;
            if (dt > 0.05f) {
                dt = 0.05f;
            }
            world.setInput(stickX, stickY);
            world.update(dt);
            if (world.weather == WeatherType.STORM && world.state == CastawayState.PLAYING) {
                tonePlayer.warning();
            }
            android.graphics.Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                renderer.render(canvas, world, width, height, stickX, stickY, actionDown);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
