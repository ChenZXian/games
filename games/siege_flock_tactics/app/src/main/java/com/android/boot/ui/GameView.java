package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.audio.ToneFx;
import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameDefs;
import com.android.boot.render.GameRenderer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final GameEngine engine;
    private final GameRenderer renderer;
    private final ToneFx toneFx;
    private Thread loopThread;
    private boolean running;
    private long lastTime;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        engine = new GameEngine();
        renderer = new GameRenderer(getResources());
        toneFx = new ToneFx();
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    private void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastTime = System.nanoTime();
        loopThread = new Thread(this, "siege-loop");
        loopThread.start();
    }

    private void stopLoop() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            loopThread = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1000000000f;
            lastTime = now;
            engine.update(dt, getWidth(), getHeight());
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    renderer.draw(canvas, engine, getWidth(), getHeight());
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            engine.touchState.touching = true;
            engine.touchState.downX = event.getX();
            engine.touchState.downY = event.getY();
            engine.touchState.nowX = event.getX();
            engine.touchState.nowY = event.getY();
            if (engine.bird.active) {
                engine.touchState.tapped = true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            engine.touchState.nowX = event.getX();
            engine.touchState.nowY = event.getY();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            engine.touchState.nowX = event.getX();
            engine.touchState.nowY = event.getY();
            engine.touchState.touching = false;
            engine.touchState.released = true;
            toneFx.playLaunch(engine.muted);
        }
        return true;
    }

    public void onHostResume() {
        startLoop();
    }

    public void onHostPause() {
        engine.pause();
        stopLoop();
    }

    public void startCampaign() {
        engine.startCampaign();
    }

    public void resumeGame() {
        engine.resume();
    }

    public void restartLevel() {
        if (engine.state == GameDefs.GAME_OVER && engine.stars > 0) {
            engine.nextLevel();
        } else {
            engine.restart();
        }
    }

    public void backToMenu() {
        engine.menu();
    }

    public void toggleMute() {
        engine.muted = !engine.muted;
    }

    public void toggleHelp() {
        engine.showHelp = !engine.showHelp;
        engine.toast = engine.showHelp ? "Drag to aim, release to fire, tap to skill" : "";
        engine.toastTime = engine.showHelp ? 2.5f : 0f;
    }
}
