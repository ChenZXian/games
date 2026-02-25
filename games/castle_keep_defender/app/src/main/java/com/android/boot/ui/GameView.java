package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.R;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Projectile;
import com.android.boot.fx.Particle;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  public interface Listener {
    void onHud(GameEngine.HudData hud, float oilCd, float freezeCd, float pushCd);
    void onState(GameState state, boolean upgradeVisible, GameEngine.UpgradeChoice choice);
  }

  private final GameEngine engine;
  private final Paint paintLane = new Paint();
  private final Paint paintCastle = new Paint();
  private final Paint paintEnemy = new Paint();
  private final Paint paintEnemyElite = new Paint();
  private final Paint paintProjectile = new Paint();
  private final Paint paintOil = new Paint();
  private final Paint paintFreeze = new Paint();
  private final Paint paintPush = new Paint();
  private final Paint paintParticle = new Paint();
  private LoopThread thread;
  private Listener listener;
  private boolean dragging;
  private float dragX;
  private float dragY;
  private float hudTimer;
  private float lastAimX = 1f;
  private float lastAimY = 0f;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    setFocusable(true);
    engine = new GameEngine();
    paintLane.setColor(getResources().getColor(R.color.cst_lane));
    paintCastle.setColor(getResources().getColor(R.color.cst_castle));
    paintEnemy.setColor(getResources().getColor(R.color.cst_enemy));
    paintEnemyElite.setColor(getResources().getColor(R.color.cst_enemy_elite));
    paintProjectile.setColor(getResources().getColor(R.color.cst_projectile));
    paintOil.setColor(getResources().getColor(R.color.cst_oil));
    paintFreeze.setColor(getResources().getColor(R.color.cst_freeze));
    paintPush.setColor(getResources().getColor(R.color.cst_push));
    paintParticle.setColor(getResources().getColor(R.color.cst_accent));
    paintOil.setAlpha(170);
    paintFreeze.setAlpha(120);
    paintPush.setAlpha(90);
  }

  public GameEngine getEngine() {
    return engine;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public float getLastAimX() {
    return lastAimX;
  }

  public float getLastAimY() {
    return lastAimY;
  }

  public void pauseLoop() {
    engine.pause();
  }

  public void resumeLoop() {
    engine.resume();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (engine.getState() != GameState.PLAYING || engine.isUpgradeVisible()) {
      return false;
    }
    float nx = event.getX() / Math.max(1f, getWidth());
    float ny = event.getY() / Math.max(1f, getHeight());
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      dragging = true;
      dragX = nx;
      dragY = ny;
      return true;
    }
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      dragX = nx;
      dragY = ny;
      return true;
    }
    if (event.getAction() == MotionEvent.ACTION_UP && dragging) {
      dragging = false;
      float ax = nx - engine.getCastleX();
      float ay = ny - engine.getCastleY();
      float len = (float) Math.sqrt(ax * ax + ay * ay);
      if (len > 0.01f) {
        lastAimX = ax / len;
        lastAimY = ay / len;
      }
      engine.fireAt(nx, ny);
      return true;
    }
    return false;
  }

  private void frame(float dt) {
    engine.update(dt);
    hudTimer += dt;
    if (hudTimer > 0.12f && listener != null) {
      hudTimer = 0f;
      listener.onHud(engine.getHud(), engine.oilCooldownRatio(), engine.freezeCooldownRatio(), engine.pushCooldownRatio());
      listener.onState(engine.getState(), engine.isUpgradeVisible(), engine.getUpgradeChoice());
    }
  }

  private void drawFrame(Canvas c) {
    c.drawColor(getResources().getColor(R.color.cst_bg_alt));
    float w = c.getWidth();
    float h = c.getHeight();
    float cx = w * engine.getCastleX();
    float cy = h * engine.getCastleY();
    c.drawRect(cx - 8, 0, cx + 8, cy, paintLane);
    c.drawRect(cx, cy - 8, w, cy + 8, paintLane);
    c.drawRect(0, cy - 8, cx, cy + 8, paintLane);
    for (int i = 0; i < engine.getOilCount(); i++) {
      c.drawCircle(engine.getOilX(i) * w, engine.getOilY(i) * h, engine.getOilR(i) * w, paintOil);
    }
    if (engine.getState() == GameState.PLAYING && engine.isUpgradeVisible()) {
      c.drawCircle(cx, cy, 120, paintFreeze);
    }
    c.drawCircle(cx, cy, 46, paintCastle);
    c.drawRect(cx - 18, cy - 26, cx + 18, cy + 26, paintLane);
    for (Enemy enemy : engine.getEnemies()) {
      if (!enemy.alive) {
        continue;
      }
      Paint p = enemy.radius > 0.02f ? paintEnemyElite : paintEnemy;
      c.drawCircle(enemy.x * w, enemy.y * h, enemy.radius * w, p);
    }
    for (Projectile projectile : engine.getProjectiles()) {
      if (projectile.alive) {
        c.drawCircle(projectile.x * w, projectile.y * h, projectile.radius * w, paintProjectile);
      }
    }
    for (Particle particle : engine.getParticles()) {
      if (particle.alive) {
        float alpha = particle.life / particle.maxLife;
        paintParticle.setAlpha((int) (255 * alpha));
        c.drawCircle(particle.x * w, particle.y * h, 4f, paintParticle);
      }
    }
    paintParticle.setAlpha(255);
    if (dragging) {
      c.drawLine(cx, cy, dragX * w, dragY * h, paintPush);
      c.drawCircle(dragX * w, dragY * h, 14f, paintPush);
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    thread = new LoopThread(holder);
    thread.running = true;
    thread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (thread != null) {
      thread.running = false;
      boolean wait = true;
      while (wait) {
        try {
          thread.join();
          wait = false;
        } catch (InterruptedException e) {
          wait = true;
        }
      }
    }
  }

  private class LoopThread extends Thread {
    private final SurfaceHolder holder;
    public boolean running;

    LoopThread(SurfaceHolder holder) {
      this.holder = holder;
    }

    @Override
    public void run() {
      long last = System.nanoTime();
      float accumulator = 0f;
      float fixedStep = 1f / 60f;
      while (running) {
        long now = System.nanoTime();
        float delta = (now - last) / 1000000000f;
        last = now;
        if (delta > 0.25f) {
          delta = 0.25f;
        }
        accumulator += delta;
        while (accumulator >= fixedStep) {
          frame(fixedStep);
          accumulator -= fixedStep;
        }
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
          drawFrame(canvas);
          holder.unlockCanvasAndPost(canvas);
        }
      }
    }
  }
}
