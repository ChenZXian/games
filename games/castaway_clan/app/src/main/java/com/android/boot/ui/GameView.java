package com.android.boot.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.TonePlayer;
import com.android.boot.core.CastawayWorld;
import com.android.boot.core.GameRenderer;
import com.android.boot.core.GameThread;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final CastawayWorld world = new CastawayWorld();
    private final GameRenderer renderer = new GameRenderer();
    private final TonePlayer tonePlayer;
    private GameThread thread;
    private float stickX;
    private float stickY;
    private boolean actionDown;
    private int joystickPointerId = -1;
    private int actionPointerId = -1;
    private float joystickBaseX;
    private float joystickBaseY;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        tonePlayer = new TonePlayer(context);
        getHolder().addCallback(this);
    }

    public CastawayWorld.StatusSnapshot snapshot() {
        return world.snapshot();
    }

    public void cycleBuildType() {
        world.cycleBuildType();
        tonePlayer.click();
    }

    public void reassignSurvivor() {
        world.reassignSurvivor();
        tonePlayer.click();
    }

    public void craftSupplies() {
        world.craftSupplies();
        tonePlayer.collect();
    }

    public void toggleMapFocus() {
        world.toggleMapFocus();
        tonePlayer.click();
    }

    public void advanceObjectiveCard() {
        world.advanceObjectiveCard();
        tonePlayer.click();
    }

    public void startGame() {
        world.startGame();
        tonePlayer.click();
    }

    public void restartRun() {
        world.resetRun();
        world.startGame();
        tonePlayer.click();
    }

    public void openMenu() {
        world.openMenu();
        tonePlayer.click();
    }

    public void togglePause() {
        world.togglePause();
        tonePlayer.click();
    }

    public void toggleMute() {
        tonePlayer.setMuted(!tonePlayer.isMuted());
    }

    public void onHostResume() {
        if (thread == null && getHolder().getSurface().isValid()) {
            thread = new GameThread(getHolder(), world, renderer, tonePlayer);
            thread.setSize(getWidth(), getHeight());
            thread.setStick(stickX, stickY);
            thread.setActionDown(actionDown);
            thread.setRunning(true);
            thread.start();
        }
    }

    public void onHostPause() {
        stopThread();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderer.ensureAssets(getContext().getAssets());
        if (thread == null) {
            thread = new GameThread(holder, world, renderer, tonePlayer);
            thread.setRunning(true);
            thread.setSize(getWidth(), getHeight());
            thread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        joystickBaseX = width * 0.22f;
        joystickBaseY = height * 0.7f;
        if (thread != null) {
            thread.setSize(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopThread();
    }

    private void stopThread() {
        if (thread != null) {
            thread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException ignored) {
                }
            }
            thread = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (x < getWidth() * 0.58f && y > getHeight() * 0.22f && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    joystickBaseX = x;
                    joystickBaseY = y;
                    updateStick(x, y);
                } else if (x > getWidth() * 0.72f && y > getHeight() * 0.36f && actionPointerId == -1) {
                    actionPointerId = pointerId;
                    actionDown = true;
                    world.queueAction();
                } else {
                    float worldX = x + world.camX;
                    float worldY = y + world.camY;
                    world.handleWorldTap(worldX, worldY);
                }
                pushInputs();
                return true;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    if (id == joystickPointerId) {
                        updateStick(event.getX(i), event.getY(i));
                    }
                }
                pushInputs();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pointerId == joystickPointerId) {
                    joystickPointerId = -1;
                    stickX = 0f;
                    stickY = 0f;
                    joystickBaseX = getWidth() * 0.22f;
                    joystickBaseY = getHeight() * 0.7f;
                }
                if (pointerId == actionPointerId) {
                    actionPointerId = -1;
                    actionDown = false;
                }
                pushInputs();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateStick(float x, float y) {
        float dx = x - joystickBaseX;
        float dy = y - joystickBaseY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 44f) {
            dx = dx / length * 44f;
            dy = dy / length * 44f;
        }
        stickX = dx / 44f;
        stickY = dy / 44f;
    }

    private void pushInputs() {
        if (thread != null) {
            thread.setStick(stickX, stickY);
            thread.setActionDown(actionDown);
        }
    }
}
