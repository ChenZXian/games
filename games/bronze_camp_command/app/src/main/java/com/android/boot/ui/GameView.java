package com.android.boot.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.TonePlayer;
import com.android.boot.core.BronzeWorld;
import com.android.boot.core.GameRenderer;
import com.android.boot.core.GameThread;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final BronzeWorld world = new BronzeWorld();
    private final GameRenderer renderer = new GameRenderer();
    private final TonePlayer tonePlayer;
    private GameThread thread;
    private float downX;
    private float downY;
    private boolean dragging;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        tonePlayer = new TonePlayer(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public BronzeWorld.StatusSnapshot snapshot() {
        return world.snapshot();
    }

    public void cycleBuildPlan() {
        world.cycleBuildPlan();
        tonePlayer.click();
    }

    public void trainSelectedUnit() {
        if (world.trainSelectedUnit()) {
            tonePlayer.collect();
        } else {
            tonePlayer.warning();
        }
    }

    public void buyUpgrade() {
        if (world.buyUpgrade()) {
            tonePlayer.collect();
        } else {
            tonePlayer.warning();
        }
    }

    public void sendScout() {
        world.sendScout();
        tonePlayer.click();
    }

    public void selectArmy() {
        world.selectArmy();
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
            thread.setRunning(true);
            thread.start();
        }
    }

    public void onHostPause() {
        tonePlayer.pauseAll();
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
        if (thread != null) {
            thread.setSize(width, height);
        }
        world.setViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        tonePlayer.pauseAll();
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
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = x - downX;
                float dy = y - downY;
                if (dx * dx + dy * dy > 900f) {
                    dragging = true;
                    world.updateDragSelection(downX, downY, x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    world.finishDragSelection(downX, downY, x, y);
                } else {
                    world.handleTap(x, y);
                }
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
