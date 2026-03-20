package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.TonePlayer;
import com.android.boot.core.BattleManager;
import com.android.boot.core.GameLoopThread;
import com.android.boot.core.GameState;
import com.android.boot.core.RuneFavor;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameLoopThread loopThread;
    private BattleManager battleManager;
    private GameHudController hudController;
    private OverlayController overlayController;
    private TonePlayer tonePlayer;
    private boolean surfaceReady;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        battleManager = new BattleManager(context);
    }

    public void bindControllers(GameHudController hudController, OverlayController overlayController, TonePlayer tonePlayer) {
        this.hudController = hudController;
        this.overlayController = overlayController;
        this.tonePlayer = tonePlayer;
        battleManager.bindTonePlayer(tonePlayer);
    }

    public void startNewMatch() {
        battleManager.startNewMatch();
        ensureLoop();
    }

    public void pauseBattle() {
        battleManager.pauseBattle();
    }

    public void resumeBattle() {
        battleManager.resumeBattle();
        ensureLoop();
    }

    public void returnToMenu() {
        battleManager.returnToMenu();
    }

    public boolean performAction(int action) {
        return battleManager.performAction(action);
    }

    public void selectFavor(RuneFavor favor) {
        battleManager.selectFavor(favor);
    }

    public void onHostPause() {
        if (battleManager.getState() == GameState.PLAYING) {
            battleManager.pauseBattle();
        }
        stopLoop();
    }

    public void onHostResume() {
        if (battleManager.getState() == GameState.PLAYING) {
            ensureLoop();
        }
    }

    private void ensureLoop() {
        if (!surfaceReady) {
            return;
        }
        if (loopThread == null || !loopThread.isAlive()) {
            loopThread = new GameLoopThread(this);
            loopThread.setRunning(true);
            loopThread.start();
        }
    }

    private void stopLoop() {
        if (loopThread != null) {
            loopThread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    loopThread.join();
                    retry = false;
                } catch (InterruptedException ignored) {
                }
            }
            loopThread = null;
        }
    }

    public void step(float deltaSeconds) {
        battleManager.update(deltaSeconds);
        if (hudController != null) {
            hudController.renderSnapshot(battleManager.createSnapshot());
        }
    }

    public void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            battleManager.render(canvas, getWidth(), getHeight());
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        ensureLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }
}
