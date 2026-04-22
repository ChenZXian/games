package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.engine.GameLoopThread;
import com.android.boot.engine.GameSession;
import com.android.boot.engine.GameState;
import com.android.boot.input.InputController;
import com.android.boot.render.GameRenderer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final GameSession session;
    private final InputController input;
    private final GameRenderer renderer;
    private GameLoopThread thread;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        session = new GameSession();
        input = new InputController();
        renderer = new GameRenderer();
    }

    public void tick(float dt) {
        session.update(dt, input);
    }

    public void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            renderer.draw(canvas, session);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (session.state == GameState.MENU && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            session.state = GameState.STAGE_SELECT;
            return true;
        }
        if (session.state == GameState.STAGE_SELECT && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            session.state = GameState.PLAYING;
            session.player.hp = session.player.maxHp;
            return true;
        }
        if (session.state == GameState.STAGE_RESULT && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (event.getX() < getWidth() / 2f) {
                session.state = GameState.PLAYING;
                session.player.hp = session.player.maxHp;
            } else {
                session.stageIndex = Math.min(6, session.stageIndex + 1);
                session.state = GameState.STAGE_SELECT;
            }
            return true;
        }
        input.onTouch(event, getWidth(), getHeight());
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new GameLoopThread(this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    public void onPauseView() {
        stopLoop();
    }

    public void onResumeView() {
        if (thread == null || !thread.isAlive()) {
            thread = new GameLoopThread(this);
            thread.setRunning(true);
            thread.start();
        }
    }

    private void stopLoop() {
        if (thread != null) {
            thread.setRunning(false);
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
