package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.audio.ToneHelper;
import com.android.boot.engine.GameLoopThread;
import com.android.boot.engine.GameMode;
import com.android.boot.engine.GameState;
import com.android.boot.engine.GameWorld;
import com.android.boot.input.InputState;
import com.android.boot.model.FloatText;
import com.android.boot.model.Obstacle;
import com.android.boot.model.Particle;
import com.android.boot.model.Pickup;
import com.android.boot.model.RunSession;
import com.android.boot.model.Runner;
import com.android.boot.render.PerspectiveProjector;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    public interface OverlayCallback {
        void onState(GameState state);
    }

    private final GameWorld world;
    private final PerspectiveProjector projector;
    private final InputState input;
    private final Paint paint;
    private final ToneHelper toneHelper;
    private GameLoopThread loop;
    private OverlayCallback overlayCallback;
    private HudBinder hudBinder;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        world = new GameWorld();
        projector = new PerspectiveProjector();
        input = new InputState();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        toneHelper = new ToneHelper();
    }

    public void setOverlayCallback(OverlayCallback callback) {
        this.overlayCallback = callback;
    }

    public void setHudBinder(HudBinder binder) {
        this.hudBinder = binder;
    }

    public void startRun(GameMode mode, int stage) {
        world.start(mode, stage);
    }

    public void restartRun() {
        world.start(world.getSession().mode, world.getSession().stage);
        world.setState(GameState.PLAYING);
        if (overlayCallback != null) overlayCallback.onState(GameState.PLAYING);
    }

    public void resumeRun() {
        world.setState(GameState.PLAYING);
        if (overlayCallback != null) overlayCallback.onState(GameState.PLAYING);
    }

    public void acceptRevive() {
        world.applyRevive();
        if (overlayCallback != null) overlayCallback.onState(world.getState());
    }

    public void onControlLeft() { input.leftPressed = true; }
    public void onControlRight() { input.rightPressed = true; }
    public void onControlUp() { input.upPressed = true; }
    public void onControlDown() { input.downPressed = true; }

    public int getCurrentStage() {
        return world.getSession().stage;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        projector.setViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    public void onHostPause() {
        world.setState(GameState.PAUSED);
        if (overlayCallback != null) overlayCallback.onState(GameState.PAUSED);
        stopLoop();
    }

    public void onHostResume() {
        startLoop();
    }

    private void startLoop() {
        if (loop != null && loop.isAlive()) return;
        loop = new GameLoopThread(getHolder(), this, world, input);
        loop.setRunning(true);
        loop.start();
    }

    private void stopLoop() {
        if (loop == null) return;
        loop.setRunning(false);
        try {
            loop.join();
        } catch (InterruptedException ignored) {
        }
        loop = null;
    }

    public void render(Canvas c) {
        int w = c.getWidth();
        int h = c.getHeight();
        projector.setViewport(w, h);
        c.drawColor(Color.rgb(7, 7, 12));
        drawWorld(c, w, h);
        drawEntities(c);
        syncHud();
        GameState state = world.getState();
        if ((state == GameState.GAME_OVER || state == GameState.STAGE_CLEAR || state == GameState.REVIVE_PROMPT) && overlayCallback != null) {
            overlayCallback.onState(state);
        }
    }

    private void drawWorld(Canvas c, int w, int h) {
        float horizon = projector.getHorizon();
        paint.setColor(Color.rgb(14, 14, 23));
        c.drawRect(0, 0, w, horizon, paint);
        paint.setColor(Color.rgb(10, 10, 20));
        c.drawRect(0, horizon, w, h, paint);
        for (int i = 0; i < 24; i++) {
            float z = (i * 5 + (world.getSession().distance % 5));
            float y = projector.yFromDepth(z + 8f);
            float scale = projector.depthToScale(z + 8f);
            float laneW = w * 0.38f * scale;
            paint.setColor(i % 2 == 0 ? Color.argb(130, 0, 245, 255) : Color.argb(100, 255, 61, 255));
            c.drawRect(w * 0.5f - laneW, y, w * 0.5f + laneW, y + 4f, paint);
        }
        for (int i = 0; i < 3; i++) {
            paint.setColor(Color.argb(120, 42, 42, 61));
            float xNear = projector.laneX(i, 5f);
            float xFar = projector.laneX(i, 110f);
            c.drawLine(xNear, h, xFar, horizon, paint);
        }
    }

    private void drawEntities(Canvas c) {
        Runner r = world.getRunner();
        for (Obstacle o : world.getObstacles()) {
            if (!o.active) continue;
            float x = projector.laneX(o.lane, o.z);
            float y = projector.yFromDepth(o.z);
            float s = projector.depthToScale(o.z);
            paint.setColor(Color.rgb(255, 61, 90));
            c.drawRect(x - 20f * s, y - 28f * s, x + 20f * s, y + 24f * s, paint);
        }
        for (Pickup p : world.getPickups()) {
            if (!p.active) continue;
            float x = projector.laneX(p.lane, p.z);
            float y = projector.yFromDepth(p.z);
            float s = projector.depthToScale(p.z);
            int color = Color.rgb(255, 210, 61);
            if (p.type == Pickup.SHIELD) color = Color.rgb(32, 255, 178);
            if (p.type == Pickup.SLOW) color = Color.rgb(123, 97, 255);
            if (p.type == Pickup.MAGNET) color = Color.rgb(0, 245, 255);
            if (p.type == Pickup.REVIVE) color = Color.rgb(255, 61, 255);
            paint.setColor(color);
            c.drawCircle(x, y, 12f * s + 3f, paint);
        }
        float rx = projector.laneX(r.currentLane, 6f);
        float ry = getHeight() * 0.78f - r.y * 12f;
        paint.setColor(Color.rgb(247, 247, 255));
        float halfH = r.sliding ? 18f : 34f;
        c.drawRect(rx - 18f, ry - halfH, rx + 18f, ry + 24f, paint);
        RunSession s = world.getSession();
        float shadowSize = 80f + s.bossPressure * 180f;
        paint.setColor(Color.argb(90, 0, 0, 0));
        c.drawCircle(getWidth() * 0.5f, getHeight() * 0.9f, shadowSize, paint);
        if (s.magnetTimer > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.rgb(0, 245, 255));
            c.drawCircle(rx, ry, 30f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (s.shield) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(Color.rgb(32, 255, 178));
            c.drawCircle(rx, ry, 38f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        for (FloatText f : world.getFloatTexts()) {
            if (!f.active) continue;
            paint.setColor(Color.rgb(247, 247, 255));
            paint.setTextSize(32f);
            c.drawText(f.text, rx - 24f, ry - 50f + f.y, paint);
        }
        for (Particle p : world.getParticles()) {
            if (!p.active) continue;
            paint.setColor(Color.rgb(255, 61, 90));
            c.drawCircle(rx + p.x * 0.1f, ry + p.y * 0.1f, 4f, paint);
        }
        if (s.slowTimer > 0f) {
            paint.setColor(Color.argb(65, 123, 97, 255));
            c.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    private void syncHud() {
        if (hudBinder == null) return;
        hudBinder.bind(world.getSession(), world.getRunner());
    }
}
