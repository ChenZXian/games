package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.core.GameEngine;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  private final SurfaceHolder holder;
  private final GameEngine engine;
  private Thread thread;
  private boolean running;
  private long lastTime;
  private int movePointerId = -1;
  private int shootPointerId = -1;
  private float moveStartX;
  private float moveStartY;
  private float shootStartX;
  private float shootStartY;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    holder = getHolder();
    holder.addCallback(this);
    setFocusable(true);
    engine = new GameEngine(context);
  }

  public void setListener(GameEngine.Listener listener) {
    engine.setListener(listener);
  }

  public void startGame() {
    engine.startNewRun();
  }

  public void restartGame() {
    engine.startNewRun();
  }

  public void pauseGame() {
    engine.pause();
  }

  public void resumeGame() {
    engine.resume();
  }

  public void goToMenu() {
    engine.goToMenu();
  }

  public void chooseUpgrade(int index) {
    engine.applyUpgrade(index);
  }

  public void triggerSkill() {
    engine.triggerSkill();
  }

  public boolean isPlaying() {
    return engine.isPlaying();
  }

  public boolean toggleSound() {
    return engine.toggleSound();
  }

  public void switchWeapon(int index) {
    engine.switchWeapon(index);
  }

  public void startShooting() {
    engine.startShooting();
  }

  public void stopShooting() {
    engine.stopShooting();
  }

  public void placeItem(int type, float x, float y) {
    engine.placeItem(type, x, y);
  }

  public void placeItemAtPlayer(int type) {
    engine.placeItemAtPlayer(type);
  }

  public com.android.boot.core.Weapon getCurrentWeapon() {
    return engine.getCurrentWeapon();
  }

  public com.android.boot.core.Weapon[] getOwnedWeapons() {
    return engine.getOwnedWeapons();
  }

  public int getWallItemCount() {
    return engine.getWallItemCount();
  }

  public int getMineItemCount() {
    return engine.getMineItemCount();
  }

  public boolean isAimAssistEnabled() {
    return engine.isAimAssistEnabled();
  }

  public void setAimAssistEnabled(boolean enabled) {
    engine.setAimAssistEnabled(enabled);
  }

  public void release() {
    engine.release();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    running = true;
    thread = new Thread(this);
    thread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    engine.setSize(width, height);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  public void run() {
    lastTime = System.nanoTime();
    while (running) {
      long now = System.nanoTime();
      float delta = (now - lastTime) / 1000000000f;
      lastTime = now;
      if (delta > 0.05f) {
        delta = 0.05f;
      }
      engine.update(delta);
      Canvas canvas = holder.lockCanvas();
      if (canvas != null) {
        engine.render(canvas);
        holder.unlockCanvasAndPost(canvas);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    int index = event.getActionIndex();
    int pointerId = event.getPointerId(index);
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
      float x = event.getX(index);
      float y = event.getY(index);
      // Left area: move joystick
      if (x < getWidth() * 0.5f && movePointerId == -1) {
        movePointerId = pointerId;
        moveStartX = x;
        moveStartY = y;
        engine.setMoveInput(0f, 0f, true);
      }
      // Right area: shoot joystick
      else if (x >= getWidth() * 0.5f && shootPointerId == -1) {
        shootPointerId = pointerId;
        shootStartX = x;
        shootStartY = y;
        engine.setShootInput(0f, 0f, true);
      }
    } else if (action == MotionEvent.ACTION_MOVE) {
      for (int i = 0; i < event.getPointerCount(); i++) {
        int id = event.getPointerId(i);
        float x = event.getX(i);
        float y = event.getY(i);
        // Handle move joystick
        if (id == movePointerId) {
          float dx = x - moveStartX;
          float dy = y - moveStartY;
          float len = (float) Math.hypot(dx, dy);
          float max = 160f;
          if (len > max) {
            dx = dx / len * max;
            dy = dy / len * max;
          }
          float nx = dx / max;
          float ny = dy / max;
          engine.setMoveInput(nx, ny, true);
        }
        // Handle shoot joystick
        else if (id == shootPointerId) {
          float dx = x - shootStartX;
          float dy = y - shootStartY;
          float len = (float) Math.hypot(dx, dy);
          float max = 160f;
          if (len > max) {
            dx = dx / len * max;
            dy = dy / len * max;
          }
          float nx = dx / max;
          float ny = dy / max;
          engine.setShootInput(nx, ny, true);
        }
      }
    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
      if (pointerId == movePointerId) {
        movePointerId = -1;
        engine.setMoveInput(0f, 0f, false);
      } else if (pointerId == shootPointerId) {
        shootPointerId = -1;
        engine.setShootInput(0f, 0f, false);
      }
    }
    return true;
  }
}
