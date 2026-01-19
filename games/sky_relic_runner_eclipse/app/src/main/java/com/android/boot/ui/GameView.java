package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.audio.SoundManager;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread thread;
    private boolean running;
    private final GameEngine engine;
    private int jumpPointerId = -1;
    private int dashPointerId = -1;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        SoundManager soundManager = new SoundManager(context);
        engine = new GameEngine(context, soundManager);
    }

    public void onResumeView() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void onPauseView() {
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                if (thread != null) {
                    thread.join();
                }
                retry = false;
            } catch (InterruptedException ignored) {
                retry = true;
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        float density = getResources().getDisplayMetrics().density;
        engine.setSize(getWidth(), getHeight(), density);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        float density = getResources().getDisplayMetrics().density;
        engine.setSize(width, height, density);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        onPauseView();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1000000000f;
            if (dt > 0.05f) {
                dt = 0.05f;
            }
            lastTime = now;
            engine.update(dt);
            drawFrame();
        }
    }

    private void drawFrame() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                engine.render(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(event.getActionIndex());

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            handlePointerDown(x, y, pointerId);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            handlePointerUp(pointerId);
        }
        return true;
    }

    private void handlePointerDown(float x, float y, int pointerId) {
        if (engine.getState() != GameState.PLAYING) {
            engine.handleUiTap(x, y);
            return;
        }
        if (engine.getPauseButton().contains(x, y)) {
            engine.handleUiTap(x, y);
            return;
        }
        if (engine.getJumpButton().contains(x, y) && jumpPointerId == -1) {
            jumpPointerId = pointerId;
            engine.onJumpPressed();
            engine.setDetectedTouch(true);
        } else if (engine.getDashButton().contains(x, y) && dashPointerId == -1) {
            dashPointerId = pointerId;
            engine.onDashPressed();
        }
    }

    private void handlePointerUp(int pointerId) {
        if (pointerId == jumpPointerId) {
            jumpPointerId = -1;
            engine.onJumpReleased();
            engine.setDetectedTouch(false);
        }
        if (pointerId == dashPointerId) {
            dashPointerId = -1;
        }
    }
}
