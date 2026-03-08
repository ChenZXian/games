package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.ToneFx;
import com.android.boot.engine.GameEngine;
import com.android.boot.input.TouchState;
import com.android.boot.render.GameRenderer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final TouchState touchState = new TouchState();
    private final ToneFx toneFx = new ToneFx();
    private final GameEngine engine = new GameEngine(toneFx);
    private final GameRenderer renderer = new GameRenderer();
    private Thread gameThread;
    private volatile boolean running;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public GameEngine getEngine() {
        return engine;
    }

    public ToneFx getToneFx() {
        return toneFx;
    }

    public void holdLeft(boolean hold) {
        touchState.leftHeld = hold;
    }

    public void pressJump() {
        touchState.jumpPressed = true;
    }

    public void holdSpray(boolean hold) {
        touchState.sprayHeld = hold;
        touchState.kickPressed = hold;
    }

    public void resumeGameLoop() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this::loop, "snow-loop");
        gameThread.start();
    }

    public void pauseGameLoop() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            gameThread = null;
        }
    }

    private void loop() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1000000000f;
            last = now;
            engine.update(dt, touchState);
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                renderer.render(canvas, engine);
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resumeGameLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGameLoop();
    }
}
