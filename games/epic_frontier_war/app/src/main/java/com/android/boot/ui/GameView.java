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
    private final BattleManager battleManager;
    private final TonePlayer tonePlayer;
    private GameLoopThread gameLoopThread;
    private GameHudController hudController;
    private OverlayController overlayController;
    private boolean surfaceReady;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        tonePlayer = new TonePlayer();
        battleManager = new BattleManager(context, tonePlayer);
    }

    public void setHudController(GameHudController hudController) {
        this.hudController = hudController;
        battleManager.setHudCallback(snapshot -> {
            if (this.hudController != null) {
                this.hudController.update(snapshot);
            }
        });
    }

    public void setOverlayController(OverlayController overlayController) {
        this.overlayController = overlayController;
        battleManager.setStateCallback((state, stats, victory) -> {
            if (this.overlayController != null) {
                this.overlayController.showState(state, stats, victory);
            }
        });
    }

    public void startNewMatch() {
        battleManager.startNewMatch(getWidth(), getHeight());
    }

    public void resumeMatch() {
        battleManager.resume();
    }

    public void pauseMatch() {
        battleManager.pause();
    }

    public void returnToMenu() {
        battleManager.returnToMenu();
    }

    public void queueSummon(int type) {
        battleManager.queueSummon(type);
    }

    public void selectFavor(RuneFavor favor) {
        battleManager.selectFavor(favor);
    }

    public void castSpell(int spell) {
        battleManager.castSpell(spell);
    }

    public void triggerHeroAbility() {
        battleManager.triggerHeroAbility();
    }

    public void toggleMute() {
        tonePlayer.setMuted(!tonePlayer.isMuted());
    }

    public boolean isMuted() {
        return tonePlayer.isMuted();
    }

    public void setPaused(boolean paused) {
        if (paused) {
            battleManager.pause();
        } else if (battleManager.getGameState() == GameState.PAUSED) {
            battleManager.resume();
        }
    }

    public GameState getCurrentState() {
        return battleManager.getGameState();
    }

    public void onActivityResume() {
        if (gameLoopThread != null) {
            gameLoopThread.setPaused(false);
        }
    }

    public void onActivityPause() {
        battleManager.onExternalPause();
        if (gameLoopThread != null) {
            gameLoopThread.setPaused(true);
        }
    }

    public void release() {
        if (gameLoopThread != null) {
            gameLoopThread.requestStop();
            try {
                gameLoopThread.join(400L);
            } catch (InterruptedException ignored) {
            }
            gameLoopThread = null;
        }
        tonePlayer.release();
    }

    public void tick(float dt) {
        battleManager.update(dt, getWidth(), getHeight());
    }

    public void render() {
        if (!surfaceReady) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                battleManager.render(canvas, getWidth(), getHeight());
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (gameLoopThread == null) {
            gameLoopThread = new GameLoopThread(this);
            gameLoopThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        battleManager.ensureWorld(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
    }
}
