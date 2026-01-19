package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.R;
import com.android.boot.audio.SoundManager;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  private final GameEngine engine;
  private final SurfaceHolder holder;
  private Thread thread;
  private boolean running;
  private long lastTime;

  private final Paint hudTextPaint;
  private final Paint hudLabelPaint;
  private final Paint panelPaint;
  private final Paint panelStrokePaint;
  private final Paint buttonPaint;
  private final Paint buttonPressedPaint;
  private final Paint ringPaint;

  private final RectF jumpRect = new RectF();
  private final RectF dashRect = new RectF();
  private final RectF pauseRect = new RectF();
  private final RectF startRect = new RectF();
  private final RectF resumeRect = new RectF();
  private final RectF restartRect = new RectF();
  private final RectF menuRect = new RectF();
  private final RectF muteRect = new RectF();

  private int jumpPointerId = -1;
  private int dashPointerId = -1;
  private boolean jumpPressed;
  private boolean dashPressed;

  private String labelStart;
  private String labelResume;
  private String labelRestart;
  private String labelMenu;
  private String labelMute;
  private String labelPaused;
  private String labelGameOver;
  private String labelDistance;
  private String labelScore;
  private String labelEnergy;
  private String labelApp;
  private String labelJump;
  private String labelDash;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    holder = getHolder();
    holder.addCallback(this);
    setFocusable(true);
    engine = new GameEngine(context, new SoundManager());

    hudTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hudTextPaint.setColor(Color.parseColor("#F7F2E8"));
    hudTextPaint.setTextSize(30f);

    hudLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hudLabelPaint.setColor(Color.parseColor("#C7D0EA"));
    hudLabelPaint.setTextSize(22f);

    panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    panelPaint.setColor(Color.parseColor("#B20A0E1F"));

    panelStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    panelStrokePaint.setStyle(Paint.Style.STROKE);
    panelStrokePaint.setStrokeWidth(3f);
    panelStrokePaint.setColor(Color.parseColor("#3C57A8"));

    buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    buttonPaint.setColor(Color.parseColor("#202C56"));

    buttonPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    buttonPressedPaint.setColor(Color.parseColor("#162047"));

    ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setStrokeWidth(6f);
    ringPaint.setColor(Color.parseColor("#58D6FF"));

    initLabels();
  }

  private void initLabels() {
    labelStart = getResources().getString(R.string.btn_start);
    labelResume = getResources().getString(R.string.btn_resume);
    labelRestart = getResources().getString(R.string.btn_restart);
    labelMenu = getResources().getString(R.string.btn_menu);
    labelMute = getResources().getString(R.string.btn_mute);
    labelPaused = getResources().getString(R.string.title_paused);
    labelGameOver = getResources().getString(R.string.title_game_over);
    labelDistance = getResources().getString(R.string.hud_distance);
    labelScore = getResources().getString(R.string.hud_score);
    labelEnergy = getResources().getString(R.string.hud_energy);
    labelApp = getResources().getString(R.string.app_name);
    labelJump = getResources().getString(R.string.btn_jump);
    labelDash = getResources().getString(R.string.btn_dash);
  }

  public void onHostResume() {
    resumeLoop();
  }

  public void onHostPause() {
    pauseLoop();
  }

  private void resumeLoop() {
    running = true;
    lastTime = System.nanoTime();
    thread = new Thread(this);
    thread.start();
  }

  private void pauseLoop() {
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
    resumeLoop();
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    engine.setViewport(width, height);
    updateControlRects(width, height);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    pauseLoop();
  }

  @Override
  public void run() {
    while (running) {
      if (!holder.getSurface().isValid()) {
        continue;
      }
      long now = System.nanoTime();
      float dt = (now - lastTime) / 1000000000f;
      if (dt > 0.033f) {
        dt = 0.033f;
      }
      lastTime = now;
      engine.update(dt);
      Canvas canvas = holder.lockCanvas();
      if (canvas != null) {
        drawFrame(canvas);
        holder.unlockCanvasAndPost(canvas);
      }
    }
  }

  private void drawFrame(Canvas canvas) {
    engine.drawWorld(canvas);
    drawHud(canvas);
    drawButtons(canvas);
    drawOverlays(canvas);
  }

  private void drawHud(Canvas canvas) {
    float left = 20f;
    float top = 16f;
    float width = 300f;
    float height = 120f;
    RectF panel = new RectF(left, top, left + width, top + height);
    canvas.drawRoundRect(panel, 20f, 20f, panelPaint);
    canvas.drawRoundRect(panel, 20f, 20f, panelStrokePaint);
    float distance = engine.getDistance();
    int score = engine.getScore();
    float energy = engine.getEnergy();
    canvas.drawText(labelDistance + " " + (int) distance + "m", left + 16f, top + 36f, hudTextPaint);
    canvas.drawText(labelScore + " " + score, left + 16f, top + 70f, hudTextPaint);
    canvas.drawText(labelEnergy + " " + (int) energy, left + 16f, top + 104f, hudTextPaint);
  }

  private void drawButtons(Canvas canvas) {
    if (engine.getState() == GameState.MENU) {
      return;
    }
    Paint jumpPaint = jumpPressed ? buttonPressedPaint : buttonPaint;
    Paint dashPaint = dashPressed ? buttonPressedPaint : buttonPaint;
    canvas.drawRoundRect(jumpRect, 26f, 26f, jumpPaint);
    canvas.drawRoundRect(dashRect, 26f, 26f, dashPaint);
    canvas.drawRoundRect(pauseRect, 22f, 22f, buttonPaint);
    drawCenteredText(canvas, labelJump, jumpRect, hudTextPaint);
    drawCenteredText(canvas, labelDash, dashRect, hudTextPaint);
    drawCenteredText(canvas, "II", pauseRect, hudTextPaint);
    float cooldown = engine.getDashCooldown();
    if (cooldown > 0f) {
      float ratio = Math.min(1f, cooldown / 1.2f);
      drawCooldownRing(canvas, dashRect, ratio);
    }
    if (engine.getRuneTimer() > 0f) {
      canvas.drawText("Rune x2", dashRect.left - 20f, dashRect.top - 16f, hudLabelPaint);
    }
  }

  private void drawOverlays(Canvas canvas) {
    if (engine.getState() == GameState.MENU) {
      drawMenuOverlay(canvas);
    } else if (engine.getState() == GameState.PAUSED) {
      drawPauseOverlay(canvas);
    } else if (engine.getState() == GameState.GAME_OVER) {
      drawGameOverOverlay(canvas);
    }
  }

  private void drawMenuOverlay(Canvas canvas) {
    canvas.drawRect(0, 0, getWidth(), getHeight(), panelPaint);
    RectF panel = new RectF(getWidth() * 0.2f, getHeight() * 0.2f, getWidth() * 0.8f, getHeight() * 0.8f);
    canvas.drawRoundRect(panel, 28f, 28f, buttonPaint);
    canvas.drawRoundRect(panel, 28f, 28f, panelStrokePaint);
    RectF titleRect = new RectF(panel.left, panel.top + 20f, panel.right, panel.top + 120f);
    drawCenteredText(canvas, labelApp, titleRect, hudTextPaint);
    canvas.drawRoundRect(startRect, 26f, 26f, buttonPaint);
    drawCenteredText(canvas, labelStart, startRect, hudTextPaint);
    canvas.drawRoundRect(muteRect, 26f, 26f, buttonPaint);
    drawCenteredText(canvas, engine.isMuted() ? labelMute + " Off" : labelMute + " On", muteRect, hudLabelPaint);
  }

  private void drawPauseOverlay(Canvas canvas) {
    canvas.drawRect(0, 0, getWidth(), getHeight(), panelPaint);
    RectF panel = new RectF(getWidth() * 0.25f, getHeight() * 0.22f, getWidth() * 0.75f, getHeight() * 0.78f);
    canvas.drawRoundRect(panel, 28f, 28f, buttonPaint);
    canvas.drawRoundRect(panel, 28f, 28f, panelStrokePaint);
    RectF titleRect = new RectF(panel.left, panel.top + 10f, panel.right, panel.top + 120f);
    drawCenteredText(canvas, labelPaused, titleRect, hudTextPaint);
    canvas.drawRoundRect(resumeRect, 26f, 26f, buttonPaint);
    canvas.drawRoundRect(restartRect, 26f, 26f, buttonPaint);
    canvas.drawRoundRect(menuRect, 26f, 26f, buttonPaint);
    drawCenteredText(canvas, labelResume, resumeRect, hudTextPaint);
    drawCenteredText(canvas, labelRestart, restartRect, hudTextPaint);
    drawCenteredText(canvas, labelMenu, menuRect, hudTextPaint);
  }

  private void drawGameOverOverlay(Canvas canvas) {
    canvas.drawRect(0, 0, getWidth(), getHeight(), panelPaint);
    RectF panel = new RectF(getWidth() * 0.2f, getHeight() * 0.22f, getWidth() * 0.8f, getHeight() * 0.8f);
    canvas.drawRoundRect(panel, 28f, 28f, buttonPaint);
    canvas.drawRoundRect(panel, 28f, 28f, panelStrokePaint);
    RectF titleRect = new RectF(panel.left, panel.top + 12f, panel.right, panel.top + 120f);
    drawCenteredText(canvas, labelGameOver, titleRect, hudTextPaint);
    String result = labelScore + " " + engine.getScore() + "  " + labelDistance + " " + (int) engine.getDistance() + "m";
    canvas.drawText(result, panel.left + 40f, panel.top + 160f, hudLabelPaint);
    String best = "Best " + engine.getBestScore() + "  " + (int) engine.getBestDistance() + "m";
    canvas.drawText(best, panel.left + 40f, panel.top + 200f, hudLabelPaint);
    canvas.drawRoundRect(restartRect, 26f, 26f, buttonPaint);
    canvas.drawRoundRect(menuRect, 26f, 26f, buttonPaint);
    drawCenteredText(canvas, labelRestart, restartRect, hudTextPaint);
    drawCenteredText(canvas, labelMenu, menuRect, hudTextPaint);
  }

  private void drawCenteredText(Canvas canvas, String text, RectF rect, Paint paint) {
    float textWidth = paint.measureText(text);
    float x = rect.centerX() - textWidth * 0.5f;
    float y = rect.centerY() - (paint.ascent() + paint.descent()) * 0.5f;
    canvas.drawText(text, x, y, paint);
  }

  private void drawCooldownRing(Canvas canvas, RectF rect, float ratio) {
    float inset = 8f;
    RectF ring = new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
    canvas.drawArc(ring, -90f, 360f * ratio, false, ringPaint);
  }

  private void updateControlRects(int width, int height) {
    float buttonWidth = 180f;
    float buttonHeight = 86f;
    jumpRect.set(24f, height - buttonHeight - 20f, 24f + buttonWidth, height - 20f);
    dashRect.set(width - buttonWidth - 24f, height - buttonHeight - 20f, width - 24f, height - 20f);
    pauseRect.set(width - 80f, 20f, width - 20f, 80f);

    startRect.set(width * 0.5f - 140f, height * 0.5f - 20f, width * 0.5f + 140f, height * 0.5f + 60f);
    resumeRect.set(width * 0.5f - 160f, height * 0.5f - 40f, width * 0.5f + 160f, height * 0.5f + 30f);
    restartRect.set(width * 0.5f - 160f, height * 0.5f + 50f, width * 0.5f + 160f, height * 0.5f + 120f);
    menuRect.set(width * 0.5f - 160f, height * 0.5f + 140f, width * 0.5f + 160f, height * 0.5f + 210f);
    muteRect.set(width * 0.5f - 140f, height * 0.5f + 80f, width * 0.5f + 140f, height * 0.5f + 150f);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    int index = event.getActionIndex();
    float x = event.getX(index);
    float y = event.getY(index);
    GameState state = engine.getState();

    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
      if (state == GameState.MENU) {
        if (startRect.contains(x, y)) {
          engine.startGame();
          return true;
        }
        if (muteRect.contains(x, y)) {
          engine.setMuted(!engine.isMuted());
          return true;
        }
      } else if (state == GameState.PLAYING) {
        if (pauseRect.contains(x, y)) {
          engine.pauseGame();
          return true;
        }
        if (jumpRect.contains(x, y) && jumpPointerId == -1) {
          jumpPointerId = event.getPointerId(index);
          jumpPressed = true;
          engine.setJumpHeld(true);
          return true;
        }
        if (dashRect.contains(x, y) && dashPointerId == -1) {
          dashPointerId = event.getPointerId(index);
          dashPressed = true;
          engine.requestDash();
          return true;
        }
      } else if (state == GameState.PAUSED) {
        if (resumeRect.contains(x, y)) {
          engine.resumeGame();
          return true;
        }
        if (restartRect.contains(x, y)) {
          engine.restartGame();
          return true;
        }
        if (menuRect.contains(x, y)) {
          engine.backToMenu();
          return true;
        }
      } else if (state == GameState.GAME_OVER) {
        if (restartRect.contains(x, y)) {
          engine.restartGame();
          return true;
        }
        if (menuRect.contains(x, y)) {
          engine.backToMenu();
          return true;
        }
      }
    }

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
      int pointerId = event.getPointerId(index);
      if (pointerId == jumpPointerId) {
        jumpPointerId = -1;
        jumpPressed = false;
        engine.setJumpHeld(false);
      }
      if (pointerId == dashPointerId) {
        dashPointerId = -1;
        dashPressed = false;
      }
    }

    return true;
  }
}
