package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.core.BattleManager;
import com.android.boot.core.GameLoopThread;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final BattleManager battleManager;
    private GameLoopThread loopThread;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        battleManager = new BattleManager(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public BattleManager getBattleManager() {
        return battleManager;
    }

    public void step(float dt) {
        battleManager.update(dt);
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                battleManager.render(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    public void onPauseView() {
        stopLoop();
        battleManager.pause();
    }

    public void onResumeView() {
        if (battleManager.getGameState() == com.android.boot.core.GameState.PAUSED) {
            battleManager.resume();
        }
        startLoop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        battleManager.setViewport(getWidth(), getHeight());
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        battleManager.setViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    private void startLoop() {
        if (loopThread != null && loopThread.isAlive()) {
            return;
        }
        loopThread = new GameLoopThread(this);
        loopThread.setRunning(true);
        loopThread.start();
    }

    private void stopLoop() {
        if (loopThread == null) {
            return;
        }
        loopThread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                loopThread.join();
                retry = false;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                retry = false;
            }
        }
        loopThread = null;
    }
}
