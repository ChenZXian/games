package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.R;
import com.android.boot.core.GameEngine;
import com.android.boot.entity.Node;
import com.android.boot.fx.Particle;

import java.util.List;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
  public enum GameState {
    MENU,
    MODE_SELECT,
    PLAYING,
    PAUSED,
    WIN,
    LOSE,
    GAME_OVER
  }

  public interface HudListener {
    void onHudUpdate(int owned, int total, String timeText);
  }

  public interface GameStateListener {
    void onStateChanged(GameState state);
  }

  private Thread thread;
  private boolean running;
  private final SurfaceHolder holder;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final GameEngine engine;
  private LinearGradient backgroundGradient;
  private HudListener hudListener;
  private GameStateListener stateListener;
  private long lastHudUpdate;
  private float downX;
  private float downY;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    holder = getHolder();
    holder.addCallback(this);
    engine = new GameEngine(context);
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(getResources().getDimension(R.dimen.cst_text_s));
  }

  public void setHudListener(HudListener listener) {
    hudListener = listener;
  }

  public void setGameStateListener(GameStateListener listener) {
    stateListener = listener;
  }

  public void startSkirmish() {
    engine.startSkirmish();
    updateState(GameState.PLAYING);
  }

  public void startCampaign() {
    engine.startCampaign(0);
    updateState(GameState.PLAYING);
  }

  public void restartMode() {
    if (engine.isCampaign()) {
      engine.startCampaign(engine.getCampaignIndex());
    } else {
      engine.startSkirmish();
    }
    updateState(GameState.PLAYING);
  }

  public void pauseGame() {
    engine.pause();
    updateState(GameState.PAUSED);
  }

  public void resumeGame() {
    engine.resume();
    updateState(GameState.PLAYING);
  }

  public void returnToMenu() {
    engine.reset();
    updateState(GameState.MENU);
  }

  public float toggleSpeed() {
    return engine.toggleSpeed();
  }

  public int toggleSendPercent() {
    return engine.toggleSendPercent();
  }

  public void resumeView() {
    running = true;
    thread = new Thread(this);
    thread.start();
  }

  public void pauseView() {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    updateBackground();
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    updateBackground();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
  }

  private void updateBackground() {
    float h = getHeight();
    backgroundGradient = new LinearGradient(0f, 0f, 0f, h,
      getResources().getColor(R.color.cst_game_bg_start),
      getResources().getColor(R.color.cst_game_bg_end),
      Shader.TileMode.CLAMP);
  }

  @Override
  public void run() {
    long lastTime = SystemClock.elapsedRealtime();
    float accumulator = 0f;
    float step = 1f / 60f;
    while (running) {
      long now = SystemClock.elapsedRealtime();
      float delta = (now - lastTime) / 1000f;
      if (delta > 0.05f) {
        delta = 0.05f;
      }
      lastTime = now;
      accumulator += delta;
      while (accumulator >= step) {
        if (engine.isPlaying()) {
          engine.update(step);
        }
        accumulator -= step;
      }
      drawFrame();
      updateHud(now);
    }
  }

  private void updateHud(long now) {
    if (hudListener == null) {
      return;
    }
    if (now - lastHudUpdate < 200) {
      return;
    }
    lastHudUpdate = now;
    int owned = engine.getOwnedCount();
    int total = engine.getTotalCount();
    String timeText = engine.getElapsedTimeText();
    post(() -> hudListener.onHudUpdate(owned, total, timeText));
    if (engine.getResultState() == GameState.WIN || engine.getResultState() == GameState.LOSE) {
      GameState state = engine.getResultState();
      engine.clearResultState();
      post(() -> updateState(state));
    }
  }

  private void updateState(GameState state) {
    if (stateListener != null) {
      stateListener.onStateChanged(state);
    }
  }

  private void drawFrame() {
    if (!holder.getSurface().isValid()) {
      return;
    }
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    paint.setShader(backgroundGradient);
    canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), paint);
    drawNodes(canvas);
    drawParticles(canvas);
    holder.unlockCanvasAndPost(canvas);
  }

  private void drawNodes(Canvas canvas) {
    List<Node> nodes = engine.getNodes();
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      int color = engine.getOwnerColor(node.owner);
      paint.setShader(null);
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(color);
      canvas.drawCircle(node.x, node.y, node.radius, paint);
      strokePaint.setColor(getResources().getColor(R.color.cst_game_node_stroke));
      canvas.drawCircle(node.x, node.y, node.radius, strokePaint);
      if (node.selected) {
        strokePaint.setColor(getResources().getColor(R.color.cst_game_select));
        canvas.drawCircle(node.x, node.y, node.radius + 6f, strokePaint);
      }
      textPaint.setTextSize(node.radius * 0.6f);
      canvas.drawText(String.valueOf(node.units), node.x, node.y + (node.radius * 0.2f), textPaint);
    }
  }

  private void drawParticles(Canvas canvas) {
    List<Particle> particles = engine.getParticles();
    for (int i = 0; i < particles.size(); i++) {
      Particle particle = particles.get(i);
      paint.setShader(null);
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(engine.getOwnerParticleColor(particle.owner));
      canvas.drawCircle(particle.x, particle.y, particle.radius, paint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!engine.isPlaying()) {
      return true;
    }
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      downX = event.getX();
      downY = event.getY();
      engine.handleTap(downX, downY);
    }
    return true;
  }
}
