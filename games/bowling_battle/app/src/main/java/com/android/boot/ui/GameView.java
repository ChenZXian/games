package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.input.TouchState;
import com.android.boot.engine.GameEngine;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	private Thread loopThread;
	private volatile boolean running;
	private long lastNs;

	private final TouchState touchState = new TouchState();
	private final GameEngine engine = new GameEngine();

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		setZOrderOnTop(false);
	}

	public GameEngine getEngine() {
		return engine;
	}

	public void holdLeft(boolean hold) { touchState.leftHeld = hold; }
	public void holdRight(boolean hold) { touchState.rightHeld = hold; }
	public void pressRoll() { touchState.rollPressed = true; }
	public void pressReset() { touchState.resetPressed = true; }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		engine.onResize(width, height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stop();
	}

	public void start() {
		if (running) return;
		running = true;
		lastNs = System.nanoTime();
		loopThread = new Thread(this, "GameLoop");
		loopThread.start();
	}

	public void stop() {
		running = false;
		if (loopThread != null) {
			try { loopThread.join(); } catch (InterruptedException ignored) {}
			loopThread = null;
		}
	}

	@Override
	public void run() {
		while (running) {
			long now = System.nanoTime();
			float dt = Math.min(0.05f, (now - lastNs) / 1_000_000_000f);
			lastNs = now;

			engine.update(dt, touchState);
			touchState.clearInstant();

			Canvas canvas = null;
			try {
				canvas = getHolder().lockCanvas();
				if (canvas != null) {
					engine.render(canvas);
				}
			} finally {
				if (canvas != null) {
					getHolder().unlockCanvasAndPost(canvas);
				}
			}
		}
	}
}
