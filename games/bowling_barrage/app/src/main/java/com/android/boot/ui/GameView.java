package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.ToneFx;
import com.android.boot.engine.GameEngine;
import com.android.boot.input.TouchState;
import com.android.boot.model.GameSnapshot;
import com.android.boot.render.GameRenderer;

public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface UiCallbacks {
        void onSnapshot(GameSnapshot snapshot);
    }

    private final SurfaceHolder surfaceHolder;
    private final TouchState touchState = new TouchState();
    private final ToneFx toneFx;
    private final GameEngine engine;
    private final GameRenderer renderer;
    private Thread loopThread;
    private boolean running;
    private boolean surfaceReady;
    private long lastFrameNs;
    private long lastUiPushNs;
    private UiCallbacks callbacks;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        toneFx = new ToneFx();
        engine = new GameEngine(context);
        engine.setAudio(toneFx);
        renderer = new GameRenderer(context);
        setFocusable(true);
    }

    public void setUiCallbacks(UiCallbacks callbacks) {
        this.callbacks = callbacks;
        pushSnapshot();
    }

    public GameEngine getEngine() {
        return engine;
    }

    public void onResumeView() {
        startLoop();
    }

    public void onPauseView() {
        stopLoop();
        engine.pause();
        pushSnapshot();
    }

    public void release() {
        stopLoop();
        toneFx.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchState.setTap(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }

    @Override
    public void run() {
        lastFrameNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastFrameNs) / 1000000000f;
            lastFrameNs = now;
            consumeInput();
            engine.update(dt);
            drawFrame();
            if (now - lastUiPushNs >= 80000000L) {
                lastUiPushNs = now;
                post(this::pushSnapshot);
            }
        }
    }

    private void consumeInput() {
        if (touchState.consumeTap()) {
            engine.handleTap(touchState.getX(), touchState.getY(), getWidth(), getHeight());
        }
    }

    private void drawFrame() {
        if (!surfaceReady) {
            return;
        }
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            renderer.render(canvas, engine, getWidth(), getHeight());
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void startLoop() {
        if (running || !surfaceReady) {
            return;
        }
        if (GameEngine.STATE_PAUSED.equals(engine.getSnapshot().state)) {
            engine.resume();
        }
        running = true;
        loopThread = new Thread(this, "BowlingBarrageLoop");
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

    private void pushSnapshot() {
        if (callbacks != null) {
            callbacks.onSnapshot(engine.getSnapshot());
        }
    }
}
