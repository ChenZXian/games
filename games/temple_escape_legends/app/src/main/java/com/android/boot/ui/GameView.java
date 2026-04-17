package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
    private long lastHudSyncMs;
    private long lastRenderNs;
    private float renderTimeSec;
    private float hitFlashTimer;
    private float lastInvulnTimer;
    private float touchStartX;
    private float touchStartY;
    private boolean touchHandled;

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
        dispatchOverlayState(GameState.PLAYING);
    }

    public void resumeRun() {
        world.setState(GameState.PLAYING);
        dispatchOverlayState(GameState.PLAYING);
    }

    public void acceptRevive() {
        world.applyRevive();
        dispatchOverlayState(world.getState());
    }

    public void onControlLeft() { input.leftPressed = true; }
    public void onControlRight() { input.rightPressed = true; }
    public void onControlUp() { input.upPressed = true; }
    public void onControlDown() { input.downPressed = true; }

    public void pauseRun() {
        if (world.getState() != GameState.PLAYING) return;
        world.setState(GameState.PAUSED);
        dispatchOverlayState(GameState.PAUSED);
    }

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
        dispatchOverlayState(GameState.PAUSED);
        stopLoop();
    }

    public void onHostResume() {
        startLoop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (world.getState() != GameState.PLAYING) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchHandled = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (touchHandled) return true;
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                float threshold = Math.max(36f, Math.min(getWidth(), getHeight()) * 0.06f);
                if (Math.abs(dx) >= threshold || Math.abs(dy) >= threshold) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        if (dx > 0f) onControlRight();
                        else onControlLeft();
                    } else {
                        if (dy < 0f) onControlUp();
                        else onControlDown();
                    }
                    touchHandled = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchHandled) {
                    onControlUp();
                }
                touchHandled = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
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
        float frameDt = updateRenderClock();
        int w = c.getWidth();
        int h = c.getHeight();
        projector.setViewport(w, h);
        c.drawColor(Color.rgb(7, 7, 12));
        drawWorld(c, w, h);
        drawEntities(c);
        updateHitFlash(frameDt);
        drawHitFlash(c);
        syncHud();
        GameState state = world.getState();
        if (state == GameState.GAME_OVER || state == GameState.STAGE_CLEAR || state == GameState.REVIVE_PROMPT) {
            dispatchOverlayState(state);
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
            drawObstacle(c, o, x, y, s);
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
        drawRunner(c, r, rx, ry);
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

    private void drawObstacle(Canvas c, Obstacle o, float x, float y, float s) {
        float pulse = 1f + 0.04f * (float) Math.sin(renderTimeSec * 5.5f + o.lane * 0.8f + o.z * 0.09f);
        float hw = 22f * s * pulse;
        float hh = 28f * s * pulse;
        int topColor = Color.rgb(255, 92, 120);
        int bottomColor = Color.rgb(154, 20, 64);
        if (o.type == Obstacle.ROLLING) {
            topColor = Color.rgb(255, 174, 61);
            bottomColor = Color.rgb(186, 104, 0);
        } else if (o.type == Obstacle.OVERHEAD || o.type == Obstacle.SLIDE_CHAIN) {
            topColor = Color.rgb(114, 109, 255);
            bottomColor = Color.rgb(69, 61, 176);
        } else if (o.type == Obstacle.GAP || o.type == Obstacle.NARROW) {
            topColor = Color.rgb(255, 73, 150);
            bottomColor = Color.rgb(174, 21, 95);
        } else if (o.type == Obstacle.FINISH) {
            topColor = Color.rgb(0, 245, 255);
            bottomColor = Color.rgb(18, 118, 176);
            hw = 26f * s;
            hh = 40f * s;
        }

        paint.setShader(new LinearGradient(x, y - hh, x, y + hh, topColor, bottomColor, Shader.TileMode.CLAMP));
        c.drawRoundRect(x - hw, y - hh, x + hw, y + hh, 8f * s, 8f * s, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, 2f * s));
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(x - hw, y - hh, x + hw, y + hh, 8f * s, 8f * s, paint);
        paint.setStyle(Paint.Style.FILL);

        if (o.type == Obstacle.ROLLING) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, 2f * s));
            paint.setColor(Color.argb(210, 255, 226, 163));
            c.drawCircle(x, y, 12f * s, paint);
            c.drawCircle(x, y, 6f * s, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawRunner(Canvas c, Runner r, float rx, float ry) {
        float halfH = r.sliding ? 18f : 34f;
        float halfW = r.sliding ? 24f : 18f;
        int bodyTop = Color.rgb(244, 248, 255);
        int bodyBottom = Color.rgb(106, 132, 210);
        if (r.invulnTimer > 0f) {
            bodyTop = Color.rgb(255, 255, 190);
            bodyBottom = Color.rgb(255, 174, 61);
        }
        paint.setShader(new LinearGradient(rx, ry - halfH, rx, ry + 24f, bodyTop, bodyBottom, Shader.TileMode.CLAMP));
        c.drawRoundRect(rx - halfW, ry - halfH, rx + halfW, ry + 24f, 10f, 10f, paint);
        paint.setShader(null);

        paint.setColor(Color.argb(215, 0, 245, 255));
        c.drawRoundRect(rx - 10f, ry - halfH - 10f, rx + 10f, ry - halfH + 2f, 6f, 6f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(rx - halfW, ry - halfH, rx + halfW, ry + 24f, 10f, 10f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void syncHud() {
        if (hudBinder == null) return;
        long now = System.currentTimeMillis();
        if (now - lastHudSyncMs < 80L) return;
        lastHudSyncMs = now;
        RunSession session = world.getSession();
        Runner runner = world.getRunner();
        post(() -> hudBinder.bind(session, runner));
    }

    private float updateRenderClock() {
        long now = System.nanoTime();
        if (lastRenderNs == 0L) {
            lastRenderNs = now;
            return 0.016f;
        }
        float dt = (now - lastRenderNs) / 1000000000f;
        lastRenderNs = now;
        if (dt < 0.001f) dt = 0.001f;
        if (dt > 0.05f) dt = 0.05f;
        renderTimeSec += dt;
        return dt;
    }

    private void updateHitFlash(float dt) {
        Runner runner = world.getRunner();
        if (runner.invulnTimer > lastInvulnTimer + 0.25f) {
            hitFlashTimer = 0.14f;
        }
        lastInvulnTimer = runner.invulnTimer;
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
            if (hitFlashTimer < 0f) hitFlashTimer = 0f;
        }
    }

    private void drawHitFlash(Canvas c) {
        if (hitFlashTimer <= 0f) return;
        float t = hitFlashTimer / 0.14f;
        int alpha = Math.min(145, Math.max(0, (int) (145f * t)));
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        c.drawRect(0f, 0f, getWidth(), getHeight(), paint);
    }

    private void dispatchOverlayState(GameState state) {
        if (overlayCallback == null) return;
        post(() -> {
            if (overlayCallback != null) {
                overlayCallback.onState(state);
            }
        });
    }
}
