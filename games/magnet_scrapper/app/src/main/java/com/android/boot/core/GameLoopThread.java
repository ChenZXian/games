package com.android.boot.core;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import com.android.boot.ui.GameView;

public class GameLoopThread extends Thread {
  private final SurfaceHolder holder;
  private final GameView view;
  private boolean running;
  private long lastNanos;

  public GameLoopThread(SurfaceHolder holder, GameView view) {
    this.holder = holder;
    this.view = view;
  }

  public void requestStop() {
    running = false;
    interrupt();
  }

  @Override
  public void run() {
    running = true;
    lastNanos = System.nanoTime();
    while (running) {
      long now = System.nanoTime();
      float delta = (now - lastNanos) / 1000000000f;
      lastNanos = now;
      if (delta > 0.033f) {
        delta = 0.033f;
      }
      view.tick(delta);
      Canvas canvas = holder.lockCanvas();
      if (canvas != null) {
        view.drawFrame(canvas);
        holder.unlockCanvasAndPost(canvas);
      }
    }
  }
}
