package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.R;
import com.android.boot.core.DeployMode;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.core.UpgradeOption;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final GameEngine engine;
    private Thread thread;
    private boolean running;
    private long lastTime;
    private float uiScale;
    private RectF shootRect;
    private RectF deployRect;
    private RectF rollRect;
    private RectF pauseRect;
    private RectF startRect;
    private RectF howRect;
    private RectF muteRect;
    private RectF resumeRect;
    private RectF restartRect;
    private RectF menuRect;
    private RectF[] upgradeRects;
    private int leftPointerId;
    private int shootPointerId;
    private int deployPointerId;
    private float joystickBaseX;
    private float joystickBaseY;
    private float joystickX;
    private float joystickY;
    private boolean joystickActive;
    private long deployPressStart;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        engine = new GameEngine(context);
        leftPointerId = -1;
        shootPointerId = -1;
        deployPointerId = -1;
        setFocusable(true);
    }

    public void onResumeView() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void onPauseView() {
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
        lastTime = System.nanoTime();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        engine.setViewport(width, height);
        uiScale = Math.min(width / 1280f, height / 720f);
        float btnSize = 96f * uiScale;
        float spacing = 18f * uiScale;
        float right = width - spacing;
        float bottom = height - spacing;
        rollRect = new RectF(right - btnSize, bottom - btnSize, right, bottom);
        deployRect = new RectF(right - btnSize, bottom - btnSize * 2f - spacing, right, bottom - btnSize - spacing);
        shootRect = new RectF(right - btnSize, bottom - btnSize * 3f - spacing * 2f, right, bottom - btnSize * 2f - spacing * 2f);
        pauseRect = new RectF(right - btnSize * 0.8f, spacing, right, spacing + btnSize * 0.6f);
        float panelWidth = width * 0.4f;
        float panelLeft = width * 0.3f;
        float panelTop = height * 0.36f;
        startRect = new RectF(panelLeft, panelTop, panelLeft + panelWidth, panelTop + btnSize);
        howRect = new RectF(panelLeft, panelTop + btnSize + spacing, panelLeft + panelWidth, panelTop + btnSize * 2f + spacing);
        muteRect = new RectF(panelLeft, panelTop + btnSize * 2f + spacing * 2f, panelLeft + panelWidth, panelTop + btnSize * 3f + spacing * 2f);
        resumeRect = new RectF(panelLeft, panelTop, panelLeft + panelWidth, panelTop + btnSize);
        restartRect = new RectF(panelLeft, panelTop + btnSize + spacing, panelLeft + panelWidth, panelTop + btnSize * 2f + spacing);
        menuRect = new RectF(panelLeft, panelTop + btnSize * 2f + spacing * 2f, panelLeft + panelWidth, panelTop + btnSize * 3f + spacing * 2f);
        upgradeRects = new RectF[3];
        for (int i = 0; i < upgradeRects.length; i++) {
            float top = height * 0.36f + i * (btnSize * 0.8f + spacing);
            upgradeRects[i] = new RectF(width * 0.25f, top, width * 0.75f, top + btnSize * 0.7f);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            if (dt > 0.05f) {
                dt = 0.05f;
            }
            engine.update(dt);
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    engine.draw(canvas, uiScale);
                    drawButtons(canvas);
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void drawButtons(Canvas canvas) {
        GameState state = engine.getState();
        if (state == GameState.PLAYING) {
            engine.drawButton(canvas, shootRect, getContext().getString(R.string.btn_shoot), uiScale);
            engine.drawButton(canvas, deployRect, getContext().getString(R.string.btn_deploy), uiScale);
            engine.drawButton(canvas, rollRect, getContext().getString(R.string.btn_roll), uiScale);
            engine.drawSmallButton(canvas, pauseRect, getContext().getString(R.string.label_pause), uiScale);
            float cooldown = engine.getRollCooldown();
            if (cooldown > 0f) {
                engine.drawSmallButton(canvas, new RectF(rollRect.left, rollRect.top - 34f * uiScale, rollRect.right, rollRect.top - 6f * uiScale), String.valueOf(Math.round(cooldown)), uiScale);
            }
        } else if (state == GameState.MENU) {
            engine.drawButton(canvas, startRect, getContext().getString(R.string.btn_start), uiScale);
            engine.drawButton(canvas, howRect, getContext().getString(R.string.btn_how_to_play), uiScale);
            String muteLabel = engine.isMuted() ? getContext().getString(R.string.btn_mute) + " On" : getContext().getString(R.string.btn_mute) + " Off";
            engine.drawButton(canvas, muteRect, muteLabel, uiScale);
        } else if (state == GameState.PAUSED) {
            engine.drawButton(canvas, resumeRect, getContext().getString(R.string.btn_resume), uiScale);
            engine.drawButton(canvas, restartRect, getContext().getString(R.string.btn_restart), uiScale);
            engine.drawButton(canvas, menuRect, getContext().getString(R.string.btn_menu), uiScale);
        } else if (state == GameState.GAME_OVER) {
            engine.drawButton(canvas, restartRect, getContext().getString(R.string.btn_restart), uiScale);
            engine.drawButton(canvas, menuRect, getContext().getString(R.string.btn_menu), uiScale);
        } else if (state == GameState.UPGRADE) {
            List<UpgradeOption> options = engine.getUpgrades();
            for (int i = 0; i < options.size(); i++) {
                engine.drawButton(canvas, upgradeRects[i], options.get(i).name, uiScale);
            }
        } else if (state == GameState.HOW_TO_PLAY) {
            engine.drawButton(canvas, menuRect, getContext().getString(R.string.btn_menu), uiScale);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        float x = event.getX(index);
        float y = event.getY(index);
        GameState state = engine.getState();
        if (state == GameState.PLAYING) {
            handlePlayingTouch(action, pointerId, x, y, event);
        } else {
            handleMenuTouch(action, x, y, state);
        }
        return true;
    }

    private void handlePlayingTouch(int action, int pointerId, float x, float y, MotionEvent event) {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (isInLeftZone(x, y) && leftPointerId == -1) {
                leftPointerId = pointerId;
                joystickActive = true;
                joystickBaseX = x;
                joystickBaseY = y;
                joystickX = 0f;
                joystickY = 0f;
                engine.setMoveInput(0f, 0f);
            } else if (shootRect.contains(x, y) && shootPointerId == -1) {
                shootPointerId = pointerId;
                engine.setShooting(true);
            } else if (deployRect.contains(x, y) && deployPointerId == -1) {
                deployPointerId = pointerId;
                deployPressStart = SystemClock.uptimeMillis();
            } else if (rollRect.contains(x, y)) {
                engine.tryRoll();
            } else if (pauseRect.contains(x, y)) {
                engine.pauseGame();
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int id = event.getPointerId(i);
                float mx = event.getX(i);
                float my = event.getY(i);
                if (id == leftPointerId) {
                    float dx = mx - joystickBaseX;
                    float dy = my - joystickBaseY;
                    float limit = 90f * uiScale;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > limit) {
                        dx = dx / len * limit;
                        dy = dy / len * limit;
                    }
                    joystickX = dx;
                    joystickY = dy;
                    engine.setMoveInput(dx / limit, dy / limit);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pointerId == leftPointerId) {
                leftPointerId = -1;
                joystickActive = false;
                engine.setMoveInput(0f, 0f);
            }
            if (pointerId == shootPointerId) {
                shootPointerId = -1;
                engine.setShooting(false);
            }
            if (pointerId == deployPointerId) {
                deployPointerId = -1;
                long duration = SystemClock.uptimeMillis() - deployPressStart;
                if (duration > 500) {
                    engine.toggleDeployMode();
                } else {
                    float tx = joystickActive ? joystickBaseX + joystickX : x;
                    float ty = joystickActive ? joystickBaseY + joystickY : y;
                    engine.tryDeploy(tx, ty);
                }
            }
        }
    }

    private void handleMenuTouch(int action, float x, float y, GameState state) {
        if (action != MotionEvent.ACTION_UP) {
            return;
        }
        if (state == GameState.MENU) {
            if (startRect.contains(x, y)) {
                engine.startGame();
            } else if (howRect.contains(x, y)) {
                engine.openHowToPlay();
            } else if (muteRect.contains(x, y)) {
                engine.toggleMute();
            }
        } else if (state == GameState.PAUSED) {
            if (resumeRect.contains(x, y)) {
                engine.resumeGame();
            } else if (restartRect.contains(x, y)) {
                engine.restartGame();
            } else if (menuRect.contains(x, y)) {
                engine.openMenu();
            }
        } else if (state == GameState.GAME_OVER) {
            if (restartRect.contains(x, y)) {
                engine.restartGame();
            } else if (menuRect.contains(x, y)) {
                engine.openMenu();
            }
        } else if (state == GameState.UPGRADE) {
            for (int i = 0; i < upgradeRects.length; i++) {
                if (upgradeRects[i].contains(x, y)) {
                    engine.applyUpgrade(i);
                    break;
                }
            }
        } else if (state == GameState.HOW_TO_PLAY) {
            if (menuRect.contains(x, y)) {
                engine.openMenu();
            }
        }
    }

    private boolean isInLeftZone(float x, float y) {
        return x < getWidth() * 0.45f && y > getHeight() * 0.45f;
    }
}
