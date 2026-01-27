package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface GameListener {
        void onStateChanged(GameState state, GameEngine.GameStats stats);
        void onHudUpdated(GameEngine.GameStats stats);
        void onUpgradeOptions(String[] options);
    }

    private final GameEngine engine = new GameEngine();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Thread thread;
    private boolean running;
    private float moveX;
    private float moveY;
    private float aimX = 1f;
    private float aimY;
    private boolean aiming;
    private int movePointerId = -1;
    private int aimPointerId = -1;
    private float joystickRadius;
    private float joystickCenterX;
    private float joystickCenterY;
    private float aimCenterX;
    private float aimCenterY;
    private long lastTime;
    private GameListener listener;
    private GameState lastState = GameState.MENU;
    private float hudTimer;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        joystickRadius = getResources().getDimension(R.dimen.cst_pad_xl) * 2.2f;
    }

    public GameEngine getEngine() {
        return engine;
    }

    public void setGameListener(GameListener gameListener) {
        listener = gameListener;
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        engine.setScreenSize(getWidth(), getHeight());
        joystickCenterX = joystickRadius + getResources().getDimension(R.dimen.cst_pad_l);
        joystickCenterY = getHeight() - joystickRadius - getResources().getDimension(R.dimen.cst_pad_l);
        aimCenterX = getWidth() - joystickCenterX;
        aimCenterY = joystickCenterY;
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        engine.setScreenSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    @Override
    public void run() {
        lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1000000000f;
            lastTime = now;
            if (dt > 0.05f) {
                dt = 0.05f;
            }
            engine.update(dt, moveX, moveY, aimX, aimY, aiming);
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    engine.draw(canvas, paint);
                    drawControls(canvas);
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
            GameState state = engine.getState();
            if (state != lastState && listener != null) {
                lastState = state;
                listener.onStateChanged(state, engine.getStats());
                if (state == GameState.UPGRADE) {
                    listener.onUpgradeOptions(engine.getUpgradeOptions());
                }
            }
            hudTimer += dt;
            if (hudTimer >= 0.15f && listener != null) {
                hudTimer = 0f;
                listener.onHudUpdated(engine.getStats());
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index);
            float y = event.getY(index);
            int id = event.getPointerId(index);
            if (x < getWidth() * 0.5f && y > getHeight() * 0.45f && movePointerId == -1) {
                movePointerId = id;
                updateMove(x, y);
            } else if (aimPointerId == -1) {
                aimPointerId = id;
                updateAim(x, y);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int id = event.getPointerId(i);
                float x = event.getX(i);
                float y = event.getY(i);
                if (id == movePointerId) {
                    updateMove(x, y);
                } else if (id == aimPointerId) {
                    updateAim(x, y);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            int id = event.getPointerId(index);
            if (id == movePointerId) {
                movePointerId = -1;
                moveX = 0f;
                moveY = 0f;
            } else if (id == aimPointerId) {
                aimPointerId = -1;
                aiming = false;
            }
        }
        return true;
    }

    private void updateMove(float x, float y) {
        float dx = x - joystickCenterX;
        float dy = y - joystickCenterY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > joystickRadius) {
            dx = dx / len * joystickRadius;
            dy = dy / len * joystickRadius;
        }
        moveX = dx / joystickRadius;
        moveY = dy / joystickRadius;
    }

    private void updateAim(float x, float y) {
        float dx = x - aimCenterX;
        float dy = y - aimCenterY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0f) {
            aimX = dx / len;
            aimY = dy / len;
            aiming = true;
        }
    }

    private void drawControls(Canvas canvas) {
        int baseColor = ContextCompat.getColor(getContext(), R.color.cst_panel_alt);
        int accent = ContextCompat.getColor(getContext(), R.color.cst_accent);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(baseColor);
        canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint);
        canvas.drawCircle(aimCenterX, aimCenterY, joystickRadius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawCircle(joystickCenterX + moveX * joystickRadius, joystickCenterY + moveY * joystickRadius, joystickRadius * 0.35f, paint);
        if (aiming) {
            canvas.drawCircle(aimCenterX + aimX * joystickRadius, aimCenterY + aimY * joystickRadius, joystickRadius * 0.3f, paint);
        }
    }
}
