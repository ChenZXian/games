package com.android.boot.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.core.GameRenderer;
import com.android.boot.core.GameThread;
import com.android.boot.core.RanchState;
import com.android.boot.core.RanchWorld;
import com.android.boot.audio.TonePlayer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final RanchWorld world = new RanchWorld();
    private final GameRenderer renderer = new GameRenderer();
    private final TonePlayer tonePlayer = new TonePlayer();
    private GameThread thread;
    private float lastX;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void bindActivity(Activity activity) {
        activity.findViewById(com.android.boot.R.id.btn_feed).setOnClickListener(v -> { world.feedSelected(); tonePlayer.tap(); });
        activity.findViewById(com.android.boot.R.id.btn_clean).setOnClickListener(v -> { world.cleanSelected(); tonePlayer.tap(); });
        activity.findViewById(com.android.boot.R.id.btn_collect).setOnClickListener(v -> { world.collectSelected(); tonePlayer.collect(); });
        activity.findViewById(com.android.boot.R.id.btn_upgrade).setOnClickListener(v -> { world.upgradeSelected(); tonePlayer.tap(); });
        activity.findViewById(com.android.boot.R.id.btn_pause).setOnClickListener(v -> {
            if (world.state == RanchState.PLAYING) world.state = RanchState.PAUSED;
            else if (world.state == RanchState.PAUSED) world.state = RanchState.PLAYING;
        });
        activity.findViewById(com.android.boot.R.id.btn_mute).setOnClickListener(v -> tonePlayer.setMuted(!tonePlayer.isMuted()));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread == null) {
            thread = new GameThread(holder, world, renderer, tonePlayer);
            thread.setRunning(true);
            thread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (thread != null) thread.setSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopThread();
    }

    public void onHostResume() {
        if (world.state == RanchState.MENU) world.startGame();
        if (thread == null && getHolder().getSurface().isValid()) {
            thread = new GameThread(getHolder(), world, renderer, tonePlayer);
            thread.setSize(getWidth(), getHeight());
            thread.setRunning(true);
            thread.start();
        }
    }

    public void onHostPause() {
        if (world.state == RanchState.PLAYING) world.state = RanchState.PAUSED;
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastX = event.getX();
            if (world.state == RanchState.MENU) {
                world.startGame();
            } else if (world.state == RanchState.GAME_OVER) {
                world.state = RanchState.MENU;
            } else {
                world.selectPen(event.getX(), event.getY());
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - lastX;
            world.scrollX -= dx;
            if (world.scrollX < 0f) world.scrollX = 0f;
            if (world.scrollX > 1300f) world.scrollX = 1300f;
            lastX = event.getX();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
