package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.BgmPlayer;
import com.android.boot.R;
import com.android.boot.core.GameEngine;
import com.android.boot.entity.Prefecture;
import com.android.boot.fx.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    void onHudUpdate(int owned, int total, String timeText, int gold);
  }

  public interface GameStateListener {
    void onStateChanged(GameState state);
  }

  private Thread thread;
  private volatile boolean running;
  private boolean surfaceReady;
  private final SurfaceHolder holder;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint linkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint dragPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battlePanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleBarTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleBarFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleFxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint battleArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path battlePath = new Path();
  private final GameEngine engine;
  private final BgmPlayer bgmPlayer;
  private LinearGradient backgroundGradient;
  private HudListener hudListener;
  private GameStateListener stateListener;
  private long lastHudUpdate;
  private float downX;
  private float downY;
  private boolean dragging;
  private Prefecture dragSource;
  private float dragX;
  private float dragY;

  private boolean battleOverlayActive;
  private float battleOverlayAlpha;
  private float battleOverlayDir;
  private int battleOverlayTargetId = -1;
  private int battleOverlayBattleNonce = -1;
  private boolean battleOverlayRunning;
  private boolean battleOverlayFinished;
  private int battleOverlayOutcome; // 0 none, 1 attacker win, -1 defender win, 2 both wiped
  private long battleOverlayLastTick;
  private BattleSim battleSim;
  private final Random battleRandom = new Random();
  
  // UI
  private boolean shopPanelActive;
  private Prefecture shopPanelTarget;
  private long longPressStartTime;
  private static final long LONG_PRESS_DURATION = 500; // 500ms
  
  
  private boolean skillPlacementMode;
  private int selectedSkillToPlace = Prefecture.SKILL_NONE;
  
  private Prefecture tapCandidatePref;
  
  
  private float cameraX = 0f;
  private float cameraY = 0f;
  private float cameraZoom = 1f;
  private float minZoom = 0.5f;
  private float maxZoom = 2.5f;
  private boolean isPanning = false;
  private float panStartX, panStartY;
  private float panStartCameraX, panStartCameraY;
  private Matrix cameraMatrix = new Matrix();
  private Matrix inverseCameraMatrix = new Matrix();

  
  private final ScaleGestureDetector scaleDetector;
  private boolean isScaling;

  // UI
  private final RectF zoomInRect = new RectF();
  private final RectF zoomOutRect = new RectF();
  private final RectF zoomFitRect = new RectF();
  private final RectF endTurnButtonRect = new RectF();
  private final RectF mapBounds = new RectF();

  // sync fence / fd
  private static final long FRAME_TIME_MS = 16L; // ~60fps

  // Shader/Path
  private LinearGradient cachedOceanGradient;
  private float cachedOceanTop;
  private float cachedOceanBottom;
  private final Path cachedGrassPath = new Path();
  private final Path cachedTempPath = new Path();
  private final Path chinaPath = new Path();
  private boolean chinaPathInitialized = false;
  
  // RectF
  private final RectF cachedRectF1 = new RectF();
  private final RectF cachedRectF2 = new RectF();
  private final RectF cachedRectF3 = new RectF();

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    holder = getHolder();
    holder.addCallback(this);
    engine = new GameEngine(context);
    bgmPlayer = new BgmPlayer();
    scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      @Override
      public boolean onScaleBegin(ScaleGestureDetector detector) {
        isScaling = true;
        // /
        dragging = false;
        dragSource = null;
        isPanning = false;
        longPressStartTime = 0;
        return true;
      }

      @Override
      public boolean onScale(ScaleGestureDetector detector) {
        float factor = detector.getScaleFactor();
        if (Float.isNaN(factor) || Float.isInfinite(factor)) {
          return false;
        }
        applyZoomAt(detector.getFocusX(), detector.getFocusY(), factor);
        return true;
      }

      @Override
      public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
      }
    });
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    linkPaint.setStyle(Paint.Style.STROKE);
    linkPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 1.2f);
    linkPaint.setColor(getResources().getColor(R.color.cst_text_muted));
    linkPaint.setAlpha(90);
    dragPaint.setStyle(Paint.Style.STROKE);
    dragPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 3f);
    dragPaint.setColor(getResources().getColor(R.color.cst_game_select));
    dragPaint.setAlpha(180);
    battlePanelPaint.setStyle(Paint.Style.FILL);
    battlePanelPaint.setColor(getResources().getColor(R.color.cst_panel_bg));
    battlePanelPaint.setAlpha(220);
    battleStrokePaint.setStyle(Paint.Style.STROKE);
    battleStrokePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    battleStrokePaint.setColor(getResources().getColor(R.color.cst_panel_stroke));
    battleStrokePaint.setAlpha(230);
    battleBarTrackPaint.setStyle(Paint.Style.FILL);
    battleBarTrackPaint.setColor(getResources().getColor(R.color.cst_meter_track));
    battleBarTrackPaint.setAlpha(220);
    battleBarFillPaint.setStyle(Paint.Style.FILL);
    battleFxPaint.setStyle(Paint.Style.STROKE);
    battleFxPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    battleFxPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    battleFxPaint.setAlpha(160);
    battleDotPaint.setStyle(Paint.Style.FILL);
    battleArrowPaint.setStyle(Paint.Style.STROKE);
    battleArrowPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    battleArrowPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    battleArrowPaint.setAlpha(150);
    battleOverlayAlpha = 0f;
    battleOverlayDir = 0f;
    battleOverlayLastTick = SystemClock.elapsedRealtime();
    battleOverlayRunning = false;
    battleOverlayFinished = false;
    battleOverlayOutcome = 0;
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
    centerCameraOnMap();
    updateState(GameState.PLAYING);
  }

  public void startCampaign() {
    engine.startCampaign(0);
    centerCameraOnMap();
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
    engine.ensurePreviewMap();
    centerCameraOnMap();
    updateState(GameState.MENU);
  }

  public float toggleSpeed() {
    return engine.toggleSpeed();
  }

  public int toggleSendPercent() {
    return engine.toggleSendPercent();
  }

  public String toggleFormation() {
    return engine.toggleFormation();
  }

  public synchronized void resumeView() {
    
    if (running) {
      return;
    }
    // Owned/Total=0
    engine.ensurePreviewMap();
    running = true;
    thread = new Thread(this);
    thread.setName("GameViewLoop");
    thread.start();
    bgmPlayer.start(getContext());
    

    centerCameraOnMap();
  }

  private void centerCameraOnMap() {
    List<Prefecture> prefs = engine.getPrefectures();
    if (prefs == null || prefs.isEmpty()) {
      return;
    }
    int screenWidth = getWidth();
    int screenHeight = getHeight();
    if (screenWidth <= 0 || screenHeight <= 0) {
      return;
    }

    computeMapBounds(prefs, mapBounds);
    float bw = Math.max(1f, mapBounds.width());
    float bh = Math.max(1f, mapBounds.height());
    float margin = 0.75f;
    float fitZoom = Math.min(screenWidth * margin / bw, screenHeight * margin / bh);
    if (fitZoom < 0.1f) {
      fitZoom = 0.1f;
    }
    cameraZoom = fitZoom;
    
    minZoom = cameraZoom * 0.5f;
    maxZoom = cameraZoom * 3.0f;

    float centerX = mapBounds.centerX();
    float centerY = mapBounds.centerY();
    cameraX = screenWidth * 0.5f - centerX * cameraZoom;
    cameraY = screenHeight * 0.5f - centerY * cameraZoom;
  }

  private void computeMapBounds(List<Prefecture> prefs, RectF out) {
 
      // Fit
    float ww = engine.getWorldMapWidth();
    float wh = engine.getWorldMapHeight();
    if (ww > 1f && wh > 1f) {
      out.set(0f, 0f, ww, wh);
      return;
    }

    out.set(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    for (int i = 0; i < prefs.size(); i++) {
      Prefecture p = prefs.get(i);
      if (p.polygonPoints != null && p.polygonPoints.length >= 6) {
        for (int k = 0; k < p.polygonVertexCount; k++) {
          float x = p.polygonPoints[k * 2];
          float y = p.polygonPoints[k * 2 + 1];
          if (x < out.left) out.left = x;
          if (y < out.top) out.top = y;
          if (x > out.right) out.right = x;
          if (y > out.bottom) out.bottom = y;
        }
      } else {
        float r = p.radius * 1.4f;
        if (p.x - r < out.left) out.left = p.x - r;
        if (p.y - r < out.top) out.top = p.y - r;
        if (p.x + r > out.right) out.right = p.x + r;
        if (p.y + r > out.bottom) out.bottom = p.y + r;
      }
    }
    if (out.left == Float.MAX_VALUE) {
      out.set(0, 0, 1, 1);
    }
  }

  private void applyZoomAt(float screenX, float screenY, float scaleFactor) {

    float[] worldBefore = screenToWorld(screenX, screenY);
    float newZoom = cameraZoom * scaleFactor;
    newZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));
    if (Math.abs(newZoom - cameraZoom) < 0.0001f) {
      return;
    }
    cameraZoom = newZoom;
    // worldBefore screenX/screenY screen = world*zoom + cameraTranslate
    cameraX = screenX - worldBefore[0] * cameraZoom;
    cameraY = screenY - worldBefore[1] * cameraZoom;
  }

  public synchronized void pauseView() {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
      }
    }
    thread = null;
    bgmPlayer.stop();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    surfaceReady = true;
    updateBackground();
    // surface Activity.onResume -> resumeView()
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    surfaceReady = true;
    updateBackground();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    surfaceReady = false;
  }

  private void updateBackground() {
    float h = Math.max(1f, getHeight());
    backgroundGradient = new LinearGradient(0f, 0f, 0f, h,
      getResources().getColor(R.color.cst_game_bg_start),
      getResources().getColor(R.color.cst_game_bg_end),
      Shader.TileMode.CLAMP);
    Log.d("JPWAR", "updateBackground height=" + h);
  }

  @Override
  public void run() {
    Log.d("JPWAR", "GameView thread started");
    long lastTime = SystemClock.elapsedRealtime();
    float accumulator = 0f;
    float step = 1f / 60f;
    while (running) {
      long frameStart = SystemClock.elapsedRealtime();
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
        updateBattleOverlay(step);
        accumulator -= step;
      }
      drawFrame();
      updateHud(now);

      // "Too many open files"
      long frameCost = SystemClock.elapsedRealtime() - frameStart;
      long sleepMs = FRAME_TIME_MS - frameCost;
      if (sleepMs > 0) {
        SystemClock.sleep(sleepMs);
      }
    }
  }

  private void updateBattleOverlay(float dt) {
    Prefecture active = findAnyActiveBattle();
    if (active != null) {
      long now = SystemClock.elapsedRealtime();
      boolean needInit = false;
      if (!battleOverlayActive) {
        battleOverlayActive = true;
        battleOverlayDir = 1f;
        engine.setBattlePaused(true);
        needInit = true;
      } else if (battleOverlayDir < 0f) {
        battleOverlayDir = 1f;
      }

      if (battleOverlayTargetId != active.id) {
        battleOverlayTargetId = active.id;
        battleOverlayBattleNonce = -1;
        needInit = true;
      }
      if (battleOverlayBattleNonce != active.battleNonce) {
        battleOverlayBattleNonce = active.battleNonce;
        needInit = true;
      }

      if (needInit) {
        battleOverlayLastTick = now;
        battleOverlayRunning = false;  // ""
        battleOverlayFinished = false;
        battleOverlayOutcome = 0;
        if (battleSim == null) {
          battleSim = new BattleSim();
        }
        battleSim.initFrom(active, getResources().getDisplayMetrics().density, battleRandom);
      }
      
      // battleTime < 0.5
      // BattleSim
      if (battleSim != null && active.battleTime < 0.5f && !battleOverlayRunning) {
        int currentAtkS = Math.max(0, active.battleAtkShield);
        int currentAtkW = Math.max(0, active.battleAtkSword);
        int currentAtkA = Math.max(0, active.battleAtkArcher);
        // battleTick
        int totalCurrent = currentAtkS + currentAtkW + currentAtkA;
        int totalBattleSim = battleSim.atkS + battleSim.atkW + battleSim.atkA;
        // BattleSim
        if (totalCurrent > totalBattleSim + 2) {
          android.util.Log.d("GameView", String.format("updateBattleOverlay: units increased, reinitializing BattleSim. current=%d/%d/%d (total=%d), battleSim=%d/%d/%d (total=%d)", 
            currentAtkS, currentAtkW, currentAtkA, totalCurrent, battleSim.atkS, battleSim.atkW, battleSim.atkA, totalBattleSim));
          battleSim.initFrom(active, getResources().getDisplayMetrics().density, battleRandom);
        }
      }
      // ""
    } else if (battleOverlayActive) {
      battleOverlayDir = -1f;
    }

    if (!battleOverlayActive && battleOverlayAlpha <= 0f) {
      battleOverlayDir = 0f;
      battleOverlayTargetId = -1;
      return;
    }

    float speed = 5.2f;
    battleOverlayAlpha = clamp01(battleOverlayAlpha + battleOverlayDir * dt * speed);
    if (battleOverlayDir < 0f && battleOverlayAlpha <= 0f) {
      battleOverlayActive = false;
      battleOverlayDir = 0f;
      battleOverlayTargetId = -1;
      battleOverlayBattleNonce = -1;
      battleOverlayRunning = false;
      battleOverlayFinished = false;
      battleOverlayOutcome = 0;
      engine.setBattlePaused(false);
      battleSim = null;

      // owner
      if (shopPanelTarget != null && battleOverlayTargetId == shopPanelTarget.id) {
        shopPanelActive = false;
        shopPanelTarget = null;
      }
    }
  }

  private float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    if (v > 1f) {
      return 1f;
    }
    return v;
  }

  private Prefecture findAnyActiveBattle() {
    List<Prefecture> prefs = engine.getPrefectures();
    if (prefs == null) {
      return null;
    }

    if (battleOverlayTargetId >= 0 && battleOverlayTargetId < prefs.size()) {
      Prefecture pref = prefs.get(battleOverlayTargetId);
      if (pref != null && pref.battleActive) {
        
        if (pref.battleAttackerOwner == 1 || pref.owner == 1) {
          return pref;
        }
      }
    }
    for (int i = 0; i < prefs.size(); i++) {
      Prefecture p = prefs.get(i);
      if (p != null && p.battleActive) {
        
        if (p.battleAttackerOwner == 1 || p.owner == 1) {
          return p;
        }
      }
    }
    return null;
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
    int gold = engine.getPlayerGold();
    post(() -> hudListener.onHudUpdate(owned, total, timeText, gold));
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
    if (!surfaceReady || !holder.getSurface().isValid()) {
      return;
    }
    Canvas canvas = null;
    try {
      canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
      int w = canvas.getWidth();
      int h = canvas.getHeight();

      // ""
      
      paint.setStyle(Paint.Style.FILL);
      paint.setShader(null);
      paint.setColor(Color.rgb(30, 90, 210)); 
      paint.setAlpha(255); 
      canvas.drawColor(Color.rgb(30, 90, 210)); // drawColor

      // + /
      canvas.save();
      updateCameraMatrix(w, h);
      canvas.concat(cameraMatrix);


      drawMapBackground(canvas);

      // drawTerritoryConnections(canvas);
      drawDrag(canvas);
    drawNodes(canvas);
    drawParticles(canvas);

      canvas.restore();

      // UI
      if (!battleOverlayActive || battleOverlayAlpha < 0.02f) {
        drawBattleOverlays(canvas);
      } else {
        drawBattleOverlayFullScreen(canvas);
      }

      if (!battleOverlayActive || battleOverlayAlpha < 0.02f) {
        drawZoomControls(canvas);

        if (engine.isPlaying() && engine.isPlayerTurn() && !hasActiveBattles()) {
          drawEndTurnButton(canvas);
        }
      }
      if (shopPanelActive && shopPanelTarget != null) {
        drawShopPanel(canvas);
      }
      if (skillPlacementMode) {
        drawSkillPlacementUI(canvas);
      }
    } catch (Throwable ignored) {
      // unlock Surface
    } finally {
      if (canvas != null) {
        try {
    holder.unlockCanvasAndPost(canvas);
        } catch (Throwable ignored) {
        }
      }
    }
  }

  private void drawZoomControls(Canvas canvas) {
    float dens = getResources().getDisplayMetrics().density;
    float w = canvas.getWidth();
    float h = canvas.getHeight();

    float pad = 10f * dens;
    float r = 18f * dens;
    float gap = 10f * dens;
    float x = w - pad - r * 2f;
    float y = h - pad - r * 2f;

    zoomInRect.set(x, y - (r * 2f + gap) * 2f, x + r * 2f, y - (r * 2f + gap));
    zoomOutRect.set(x, y - (r * 2f + gap), x + r * 2f, y);
    zoomFitRect.set(x, y, x + r * 2f, y + r * 2f);

    
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.argb(140, 0, 0, 0));
    canvas.drawRoundRect(zoomInRect, r, r, paint);
    canvas.drawRoundRect(zoomOutRect, r, r, paint);
    canvas.drawRoundRect(zoomFitRect, r, r, paint);

    textPaint.setColor(Color.WHITE);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(16f * dens);
    canvas.drawText("+", zoomInRect.centerX(), zoomInRect.centerY() + 6f * dens, textPaint);
    canvas.drawText("-", zoomOutRect.centerX(), zoomOutRect.centerY() + 6f * dens, textPaint);
    textPaint.setTextSize(12f * dens);
    canvas.drawText("Fit", zoomFitRect.centerX(), zoomFitRect.centerY() + 4f * dens, textPaint);

    
    textPaint.setTextSize(11f * dens);
    textPaint.setColor(Color.argb(210, 255, 255, 255));
    canvas.drawText((int) (cameraZoom * 100f) + "%", zoomFitRect.centerX(),
      zoomFitRect.bottom + 14f * dens, textPaint);
  }
  
  private void drawEndTurnButton(Canvas canvas) {
    float dens = getResources().getDisplayMetrics().density;
    float w = canvas.getWidth();
    float h = canvas.getHeight();
    
    float btnW = 120f * dens;
    float btnH = 50f * dens;
    float btnX = w - btnW - 20f * dens;
    float btnY = 20f * dens;
    
    endTurnButtonRect.set(btnX, btnY, btnX + btnW, btnY + btnH);
    
    
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(getResources().getColor(R.color.cst_accent_2));
    paint.setAlpha(240);
    canvas.drawRoundRect(endTurnButtonRect, 12f * dens, 12f * dens, paint);
    
    
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(2f * dens);
    strokePaint.setColor(Color.argb(200, 255, 255, 255));
    canvas.drawRoundRect(endTurnButtonRect, 12f * dens, 12f * dens, strokePaint);
    
    
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(14f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    textPaint.setFakeBoldText(true);
    canvas.drawText("End Turn", endTurnButtonRect.centerX(), endTurnButtonRect.centerY() + 5f * dens, textPaint);
    
    // Display turn number
    textPaint.setTextSize(11f * dens);
    textPaint.setFakeBoldText(false);
    textPaint.setColor(Color.argb(220, 255, 255, 255));
    canvas.drawText("Turn " + engine.getCurrentTurn(), endTurnButtonRect.centerX(), 
      endTurnButtonRect.bottom + 16f * dens, textPaint);
  }
  
  private boolean hasActiveBattles() {
    for (Prefecture pref : engine.getPrefectures()) {
      if (pref.battleActive) {
        return true;
      }
    }
    return false;
  }
  
  private void updateCameraMatrix(float screenWidth, float screenHeight) {
    cameraMatrix.reset();
    
    cameraMatrix.postScale(cameraZoom, cameraZoom);

    cameraMatrix.postTranslate(cameraX, cameraY);
    
    try {
      cameraMatrix.invert(inverseCameraMatrix);
    } catch (Exception e) {
      inverseCameraMatrix.reset();
    }
  }
  
  
  private float[] screenToWorld(float screenX, float screenY) {
    float[] point = {screenX, screenY};
    inverseCameraMatrix.mapPoints(point);
    return point;
  }

  private void drawBattleOverlayFullScreen(Canvas canvas) {
    Prefecture target = findAnyActiveBattle();
    if (target == null) {
      return;
    }

    float a = battleOverlayAlpha;
    int dim = Color.argb((int) (170f * a), 0, 0, 0);
    paint.setShader(null);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(dim);
    canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), paint);

    float dens = getResources().getDisplayMetrics().density;
    float pad = 14f * dens;
    float w = canvas.getWidth() - pad * 2f;
    float h = canvas.getHeight() - pad * 2f;
    RectF rect = new RectF(pad, pad, pad + w, pad + h);
    float r = 16f * dens;
    battlePanelPaint.setAlpha((int) (235f * a));
    battleStrokePaint.setAlpha((int) (245f * a));
    canvas.drawRoundRect(rect, r, r, battlePanelPaint);
    canvas.drawRoundRect(rect, r, r, battleStrokePaint);

    int atkColor = engine.getOwnerColor(target.battleAttackerOwner);
    int defColor = engine.getOwnerColor(target.owner);
    int atkS = Math.max(0, target.battleAtkShield);
    int atkW = Math.max(0, target.battleAtkSword);
    int atkA = Math.max(0, target.battleAtkArcher);
    int defS = Math.max(0, target.shield);
    int defW = Math.max(0, target.sword);
    int defA = Math.max(0, target.archer);

    float topBarH = 44f * dens;
    RectF top = new RectF(rect.left, rect.top, rect.right, rect.top + topBarH);
    paint.setColor(getResources().getColor(R.color.cst_panel_header_bg));
    paint.setAlpha((int) (210f * a));
    canvas.drawRoundRect(top, r, r, paint);

    textPaint.setAlpha((int) (255f * a));
    textPaint.setTextSize(16f * dens);
    textPaint.setTextAlign(Paint.Align.LEFT);
    textPaint.setColor(atkColor);
    canvas.drawText("ATK", rect.left + pad, rect.top + 28f * dens, textPaint);
    textPaint.setTextAlign(Paint.Align.RIGHT);
    textPaint.setColor(defColor);
    canvas.drawText("DEF", rect.right - pad, rect.top + 28f * dens, textPaint);

    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(14f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    String name = target.name != null ? target.name : "";
    canvas.drawText(name, rect.centerX(), rect.top + 28f * dens, textPaint);

    float battleTop = rect.top + topBarH + pad;
    float skillBarH = battleOverlayRunning ? 50f * dens : 0f;
    float battleBottom = rect.bottom - pad - 40f * dens - skillBarH;
    RectF field = new RectF(rect.left + pad, battleTop, rect.right - pad, battleBottom);
    drawBattleField(canvas, field, target, atkColor, defColor, atkS, atkW, atkA, defS, defW, defA, a);
    

    if (battleOverlayRunning && battleSim != null) {
      float skillY = battleBottom + 8f * dens;
      float skillH = 42f * dens;
      RectF skillArea = new RectF(rect.left + pad, skillY, rect.right - pad, skillY + skillH);
      drawSkillButtons(canvas, skillArea, target, a);
    }

    // bottom control bar (start / status)
    float barY = battleOverlayRunning ? (battleBottom + skillBarH + 8f * dens) : (battleBottom + 8f * dens);
    RectF bar = new RectF(rect.left + pad, barY, rect.right - pad, rect.bottom - pad);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.argb((int) (110f * a), 0, 0, 0));
    canvas.drawRoundRect(bar, 12f * dens, 12f * dens, paint);

    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(13f * dens);
    if (!battleOverlayRunning && !battleOverlayFinished) {
      textPaint.setColor(getResources().getColor(R.color.cst_accent));
      canvas.drawText("Tap to Start Battle", bar.centerX(), bar.centerY() + 4f * dens, textPaint);
    } else if (battleOverlayFinished) {
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      String msg;
      if (battleOverlayOutcome == 1) {
        msg = "Battle Over: Attacker Wins";
      } else if (battleOverlayOutcome == -1) {
        msg = "Battle Over: Defender Wins";
      } else {
        msg = "Battle Over: Both Wiped";
      }
      canvas.drawText(msg + " (Tap to Close)", bar.centerX(), bar.centerY() + 4f * dens, textPaint);
    } else {
      textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
      canvas.drawText("Battle in Progress...", bar.centerX(), bar.centerY() + 4f * dens, textPaint);
    }
  }

  private void drawSkillButtons(Canvas canvas, RectF area, Prefecture target, float alpha) {
    if (battleSim == null) return;
    float dens = getResources().getDisplayMetrics().density;
    float btnW = (area.width() - 20f * dens) / 4f;
    float btnH = area.height();
    float btnSpacing = 4f * dens;
    
    String[] skillNames = {"Charge", "Arrow Rain", "Defense", "Retreat"};
    int[] skills = {
      BattleSim.SKILL_CHARGE,
      BattleSim.SKILL_ARROW_RAIN,
      BattleSim.SKILL_DEFENSE_FORM,
      BattleSim.SKILL_RETREAT
    };
    

    int playerSide = target.battleAttackerOwner == 1 ? BattleSim.SIDE_ATK : BattleSim.SIDE_DEF;
    float cd = battleSim.getSkillCooldown(playerSide);
    int activeSkill = battleSim.getActiveSkill(playerSide);
    
    for (int i = 0; i < 4; i++) {
      float btnX = area.left + i * (btnW + btnSpacing);
      RectF btnRect = new RectF(btnX, area.top, btnX + btnW, area.top + btnH);
      boolean isActive = activeSkill == skills[i];
      boolean isReady = cd <= 0f && !isActive;
      
      paint.setStyle(Paint.Style.FILL);
      if (isActive) {
        paint.setColor(getResources().getColor(R.color.cst_accent));
        paint.setAlpha((int) (200f * alpha));
      } else if (isReady) {
        paint.setColor(getResources().getColor(R.color.cst_accent_2));
        paint.setAlpha((int) (180f * alpha));
      } else {
        paint.setColor(Color.argb((int) (100f * alpha), 100, 100, 100));
      }
      canvas.drawRoundRect(btnRect, 8f * dens, 8f * dens, paint);
      
      textPaint.setTextSize(11f * dens);
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      textPaint.setAlpha((int) (255f * alpha));
      canvas.drawText(skillNames[i], btnRect.centerX(), btnRect.centerY() - 4f * dens, textPaint);
      
      if (!isReady && !isActive) {
        textPaint.setTextSize(9f * dens);
        canvas.drawText(String.format("%.1f", cd), btnRect.centerX(), btnRect.centerY() + 8f * dens, textPaint);
      }
    }
  }

  private void drawBattleField(Canvas canvas, RectF field, Prefecture target, int atkColor, int defColor,
                               int atkS, int atkW, int atkA, int defS, int defW, int defA, float alpha) {
    float dens = getResources().getDisplayMetrics().density;
    paint.setColor(Color.argb((int) (90f * alpha), 255, 255, 255));
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2f * dens);
    canvas.drawRoundRect(field, 14f * dens, 14f * dens, paint);

    float midX = field.centerX();
    float midY = field.centerY();
    paint.setAlpha((int) (80f * alpha));
    canvas.drawLine(midX, field.top + 10f * dens, midX, field.bottom - 10f * dens, paint);

    int atkT = Math.max(0, atkS + atkW + atkA);
    int defT = Math.max(0, defS + defW + defA);
    int maxT = Math.max(1, Math.max(atkT, defT));

    float yTop = field.top + 22f * dens;
    textPaint.setAlpha((int) (255f * alpha));
    textPaint.setTextSize(13f * dens);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    canvas.drawText(atkT + " vs " + defT, midX, yTop, textPaint);

    float unitAreaTop = field.top + 40f * dens;
    RectF unitArea = new RectF(field.left + 10f * dens, unitAreaTop, field.right - 10f * dens, field.bottom - 18f * dens);
    long now = SystemClock.elapsedRealtime();
    float dt = (now - battleOverlayLastTick) / 1000f;
    if (dt > 0.05f) {
      dt = 0.05f;
    }
    battleOverlayLastTick = now;

    if (battleSim != null) {
      if (battleOverlayRunning && !battleOverlayFinished) {
        battleSim.update(dt, unitArea, battleRandom);
        battleSim.writeBackTo(target);
      }
      battleSim.draw(canvas, unitArea, this, atkColor, defColor, alpha);
    } else {
      drawArmy(canvas, unitArea, true, atkColor, atkS, atkW, atkA, maxT, alpha);
      drawArmy(canvas, unitArea, false, defColor, defS, defW, defA, maxT, alpha);
    }

    float fx = (SystemClock.elapsedRealtime() % 500L) / 500f;
    float pr = 12f * dens + 18f * dens * fx;
    battleFxPaint.setAlpha((int) (80f * alpha * (1f - fx)));
    battleFxPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawCircle(midX, midY, pr, battleFxPaint);
  }

  private void drawArmy(Canvas canvas, RectF area, boolean left, int sideColor, int s, int w, int a, int maxT, float alpha) {
    float dens = getResources().getDisplayMetrics().density;
    float halfW = area.width() * 0.5f;
    float x0 = left ? area.left : area.left + halfW;
    float x1 = left ? area.left + halfW : area.right;
    RectF side = new RectF(x0, area.top, x1, area.bottom);

    float rows = 3f;
    float rowH = side.height() / rows;
    float r = 4.2f * dens;
    float step = r * 2.6f;
    int maxDots = Math.max(10, Math.min(18, (int) (side.width() / step)));
    int scale = Math.max(4, (int) Math.ceil(maxT / (float) maxDots));
    int ds = Math.min(maxDots, (int) Math.ceil(s / (float) scale));
    int dw = Math.min(maxDots, (int) Math.ceil(w / (float) scale));
    int da = Math.min(maxDots, (int) Math.ceil(a / (float) scale));

    float t = (SystemClock.elapsedRealtime() % 700L) / 700f;
    float push = (float) Math.sin(t * Math.PI * 2f) * (side.width() * 0.02f);
    float dir = left ? 1f : -1f;

    float base = left ? (side.left + side.width() * 0.26f) : (side.right - side.width() * 0.26f);
    float front = left ? (side.right - side.width() * 0.08f) : (side.left + side.width() * 0.08f);
    float mid = left ? (side.right - side.width() * 0.20f) : (side.left + side.width() * 0.20f);
    float back = base;
    float xShield = front + dir * push * 1.1f;
    float xSword = mid + dir * push * 1.0f;
    float xArcher = back + dir * push * 0.8f;

    float yShield = side.top + rowH * 0.5f;
    float ySword = side.top + rowH * 1.5f;
    float yArcher = side.top + rowH * 2.5f;

    int sCol = getResources().getColor(R.color.cst_accent);
    int wCol = getResources().getColor(R.color.cst_warning);
    int aCol = getResources().getColor(R.color.cst_accent_2);

    drawUnitRow(canvas, left, xShield, yShield, ds, r, sideColor, sCol, 0, alpha);
    drawUnitRow(canvas, left, xSword, ySword, dw, r, sideColor, wCol, 1, alpha);
    drawUnitRow(canvas, left, xArcher, yArcher, da, r, sideColor, aCol, 2, alpha);
  }

  private void drawUnitRow(Canvas canvas, boolean left, float cx, float cy, int count, float r, int bodyColor, int accentColor, int type, float alpha) {
    if (count <= 0) {
      return;
    }
    float dens = getResources().getDisplayMetrics().density;
    float step = r * 2.8f;
    float startX = cx - (count - 1) * step * 0.5f;

    for (int i = 0; i < count; i++) {
      float x = startX + i * step;
      float y = cy;
      drawUnit(canvas, x, y, r, bodyColor, accentColor, type, left, alpha);
    }

    if (type == 2 && count > 0) {
      float phase = (SystemClock.elapsedRealtime() % 520L) / 520f;
      float sx = left ? (startX + count * step * 0.2f) : (startX + (count - 1) * step * 0.8f);
      float ex = left ? (sx + 180f * dens) : (sx - 180f * dens);
      float mx = (sx + ex) * 0.5f;
      float my = cy - 46f * dens;
      float ax = sx + (ex - sx) * phase;
      float ay = quadAt(cy, my, cy, phase);
      battleArrowPaint.setColor(accentColor);
      battleArrowPaint.setAlpha((int) (140f * alpha));
      battlePath.reset();
      battlePath.moveTo(sx, cy);
      battlePath.quadTo(mx, my, ex, cy);
      canvas.drawPath(battlePath, battleArrowPaint);
      battleDotPaint.setColor(accentColor);
      battleDotPaint.setAlpha((int) (220f * alpha));
      canvas.drawCircle(ax, ay, 2.4f * dens, battleDotPaint);
    }
  }

  private void drawUnit(Canvas canvas, float x, float y, float r, int bodyColor, int accentColor, int type, boolean left, float alpha) {
    float dens = getResources().getDisplayMetrics().density;
    float headR = r * 0.75f;
    float bodyW = r * 1.2f;
    float bodyH = r * 1.6f;
    float dir = left ? 1f : -1f;

    battleDotPaint.setColor(bodyColor);
    battleDotPaint.setAlpha((int) (230f * alpha));
    canvas.drawCircle(x, y - bodyH * 0.55f, headR, battleDotPaint);
    canvas.drawRoundRect(new RectF(x - bodyW, y - bodyH * 0.2f, x + bodyW, y + bodyH), r, r, battleDotPaint);

    battleDotPaint.setColor(accentColor);
    battleDotPaint.setAlpha((int) (240f * alpha));
    if (type == 0) {
      float sw = r * 1.0f;
      float sh = r * 1.6f;
      float sx = x + dir * (bodyW + sw * 0.6f);
      RectF shield = new RectF(sx - sw, y - sh * 0.2f, sx + sw, y + sh);
      canvas.drawRoundRect(shield, r, r, battleDotPaint);
    } else if (type == 1) {
      float sx = x + dir * (bodyW + r * 0.4f);
      float sy = y - r * 0.3f;
      canvas.drawLine(sx, sy, sx + dir * (r * 1.6f), sy + r * 1.6f, battleArrowPaint);
    } else {
      float bx = x + dir * (bodyW + r * 0.2f);
      float by = y + r * 0.1f;
      battleFxPaint.setAlpha((int) (170f * alpha));
      battleFxPaint.setColor(accentColor);
      canvas.drawCircle(bx, by, r * 0.9f, battleFxPaint);
      battleFxPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      battleFxPaint.setAlpha(160);
    }
  }

  private class BattleSim {
    private static final int SIDE_ATK = 0;
    private static final int SIDE_DEF = 1;
    private static final int TYPE_SHIELD = 0;
    private static final int TYPE_SWORD = 1;
    private static final int TYPE_ARCHER = 2;

    private static class Unit {
      float x;
      float y;
      float vx;
      float vy;
      float cooldown;
      float dash;
      float dashCd;
      int side;
      int type;
      int hp;
      int id;
      int slot;
      boolean positioned;
      boolean alive;
      boolean isGiant;  
    }

    private static class Fx {
      float x;
      float y;
      float t;
      float life;
      int color;
    }

    private static class Projectile {
      float sx;
      float sy;
      float ex;
      float ey;
      float t;
      float life;
      int side;
      int type;
      int targetId;
      int color;
    }

    private final ArrayList<Unit> units = new ArrayList<>();
    private final ArrayList<Fx> fxs = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private int atkS;
    private int atkW;
    private int atkA;
    private int defS;
    private int defW;
    private int defA;
    private float dens;
    private float time;
    private int nextId;
    
    
    public static final int SKILL_NONE = 0;
    public static final int SKILL_CHARGE = 1;
    public static final int SKILL_ARROW_RAIN = 2;
    public static final int SKILL_DEFENSE_FORM = 3;
    public static final int SKILL_RETREAT = 4;     // HP
    
    private float atkSkillCooldown = 0f;
    private float defSkillCooldown = 0f;
    private int atkActiveSkill = SKILL_NONE;
    private int defActiveSkill = SKILL_NONE;
    private float atkSkillDuration = 0f;
    private float defSkillDuration = 0f;
    
    
    public static final int COMMAND_NONE = 0;
    public static final int COMMAND_ADVANCE = 1;   
    public static final int COMMAND_RETREAT = 2;   
    public static final int COMMAND_FORMATION = 3; 
    
    private int atkCommand = COMMAND_NONE;
    private int defCommand = COMMAND_NONE;
    
    
    private float cannonCooldown = 0f;
    private Prefecture battleTarget;

    void initFrom(Prefecture target, float dens, Random rnd) {
      this.dens = dens;
      time = 0f;
      nextId = 1;
      units.clear();
      fxs.clear();
      projectiles.clear();
      battleTarget = target;
      cannonCooldown = 0f;
      
      
      atkSkillCooldown = 0f;
      defSkillCooldown = 0f;
      atkActiveSkill = SKILL_NONE;
      defActiveSkill = SKILL_NONE;
      atkSkillDuration = 0f;
      defSkillDuration = 0f;
      atkCommand = COMMAND_NONE;
      defCommand = COMMAND_NONE;

      // battleAtk*
      atkS = Math.max(0, target.battleAtkShield);
      atkW = Math.max(0, target.battleAtkSword);
      atkA = Math.max(0, target.battleAtkArcher);
      
      
      // engine.update() updateBattleOverlay() battleTick initFrom
      // target.shield/sword/archer battleTick
      // battleDef*Start
      // 0
      defS = target.battleDefShieldStart > 0 ? target.battleDefShieldStart : Math.max(0, target.shield);
      defW = target.battleDefSwordStart > 0 ? target.battleDefSwordStart : Math.max(0, target.sword);
      defA = target.battleDefArcherStart > 0 ? target.battleDefArcherStart : Math.max(0, target.archer);
      

      android.util.Log.d("BattleSim", String.format("initFrom: atk=%d/%d/%d, def=%d/%d/%d (saved=%d/%d/%d, current=%d/%d/%d), battleTime=%.3f, battleNonce=%d", 
        atkS, atkW, atkA, defS, defW, defA,
        target.battleDefShieldStart, target.battleDefSwordStart, target.battleDefArcherStart,
        target.shield, target.sword, target.archer,
        target.battleTime, target.battleNonce));
      
      
      // target.giantShield/Sword/Archer

      int atkGS = Math.max(0, target.giantShield);
      int atkGW = Math.max(0, target.giantSword);
      int atkGA = Math.max(0, target.giantArcher);
      // 0
      int defGS = 0, defGW = 0, defGA = 0;


      spawnSide(SIDE_ATK, atkS, atkW, atkA, atkGS, atkGW, atkGA, rnd);
      spawnSide(SIDE_DEF, defS, defW, defA, defGS, defGW, defGA, rnd);
    }
    
    boolean useSkill(int side, int skill) {
      if (side == SIDE_ATK) {
        if (atkSkillCooldown > 0f || atkActiveSkill != SKILL_NONE) {
          return false;
        }
        atkActiveSkill = skill;
        atkSkillCooldown = 8f;  // 8
        atkSkillDuration = 3f;  // 3
        
        triggerSkillEffect(side, skill);
        return true;
      } else {
        if (defSkillCooldown > 0f || defActiveSkill != SKILL_NONE) {
          return false;
        }
        defActiveSkill = skill;
        defSkillCooldown = 8f;
        defSkillDuration = 3f;
        
        triggerSkillEffect(side, skill);
        return true;
      }
    }
    
    private void triggerSkillEffect(int side, int skill) {
      if (skill == SKILL_ARROW_RAIN) {

        for (int i = 0; i < units.size(); i++) {
          Unit u = units.get(i);
          if (!u.alive || u.side != side || u.type != TYPE_ARCHER) {
            continue;
          }
          Unit enemy = findNearestEnemy(u);
          if (enemy != null) {
            float dx = enemy.x - u.x;
            float dy = enemy.y - u.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= 150f * dens) {
              fireArrow(u, enemy, dist, 140f * dens, 1.65f);
              u.cooldown = 0.5f;  
            }
          }
        }
      } else if (skill == SKILL_RETREAT) {
        // HP
        for (int i = 0; i < units.size(); i++) {
          Unit u = units.get(i);
          if (!u.alive || u.side != side) {
            continue;
          }
          int maxHp = u.type == TYPE_SHIELD ? 4 : (u.type == TYPE_SWORD ? 2 : 1);
          u.hp = Math.min(maxHp, u.hp + 1);  // 1HP
        }
        setCommand(side, COMMAND_RETREAT);
      } else if (skill == SKILL_DEFENSE_FORM) {

        // applyHit
      }
    }
    
    void setCommand(int side, int command) {
      if (side == SIDE_ATK) {
        atkCommand = command;
      } else {
        defCommand = command;
      }
    }
    
    float getSkillCooldown(int side) {
      return side == SIDE_ATK ? atkSkillCooldown : defSkillCooldown;
    }
    
    int getActiveSkill(int side) {
      return side == SIDE_ATK ? atkActiveSkill : defActiveSkill;
    }

    void writeBackTo(Prefecture target) {
      int aS = 0, aW = 0, aA = 0, dS = 0, dW = 0, dA = 0;
      int aGS = 0, aGW = 0, aGA = 0, dGS = 0, dGW = 0, dGA = 0;
      int prevOwner = target.owner;
      int attackerOwner = target.battleAttackerOwner;
      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive) {
          continue;
        }
        if (u.side == SIDE_ATK) {
          if (u.isGiant) {
            if (u.type == TYPE_SHIELD) aGS++;
            else if (u.type == TYPE_SWORD) aGW++;
            else aGA++;
          } else {
            if (u.type == TYPE_SHIELD) aS++;
            else if (u.type == TYPE_SWORD) aW++;
            else aA++;
          }
        } else {
          if (u.isGiant) {
            if (u.type == TYPE_SHIELD) dGS++;
            else if (u.type == TYPE_SWORD) dGW++;
            else dGA++;
          } else {
            if (u.type == TYPE_SHIELD) dS++;
            else if (u.type == TYPE_SWORD) dW++;
            else dA++;
          }
        }
      }
      
      int atkT = aS + aW + aA + aGS + aGW + aGA;
      int defT = dS + dW + dA + dGS + dGW + dGA;
      
      if (atkT > 0 && defT > 0) {
        target.battleAtkShield = aS;
        target.battleAtkSword = aW;
        target.battleAtkArcher = aA;
        target.giantShield = aGS;
        target.giantSword = aGW;
        target.giantArcher = aGA;
        target.shield = dS;
        target.sword = dW;
        target.archer = dA;

        return;
      }

      if (atkT <= 0 && defT > 0) {
        target.battleAtkShield = 0;
        target.battleAtkSword = 0;
        target.battleAtkArcher = 0;
        target.giantShield = 0;
        target.giantSword = 0;
        target.giantArcher = 0;
        target.shield = dS;
        target.sword = dW;
        target.archer = dA;
        target.battleAttackerOwner = 0;
        target.battleActive = false;
        engine.onBattleResolved(attackerOwner, prevOwner);
        battleOverlayFinished = true;
        battleOverlayOutcome = -1;
        return;
      }

      if (defT <= 0 && atkT > 0) {
        // Attacker captures: survivors become new garrison, battle pool cleared.
        target.owner = target.battleAttackerOwner;
        target.shield = aS;
        target.sword = aW;
        target.archer = aA;
        target.giantShield = aGS;
        target.giantSword = aGW;
        target.giantArcher = aGA;
        target.battleAtkShield = 0;
        target.battleAtkSword = 0;
        target.battleAtkArcher = 0;
        target.battleAttackerOwner = 0;
        target.battleActive = false;
        engine.onBattleResolved(attackerOwner, prevOwner);
        battleOverlayFinished = true;
        battleOverlayOutcome = 1;
        return;
      }

      // Both sides wiped.
      target.battleAtkShield = 0;
      target.battleAtkSword = 0;
      target.battleAtkArcher = 0;
      target.shield = 0;
      target.sword = 0;
      target.archer = 0;
      target.battleAttackerOwner = 0;
      target.battleActive = false;
      engine.onBattleResolved(attackerOwner, prevOwner);
      battleOverlayFinished = true;
      battleOverlayOutcome = 2;
    }

    void update(float dt, RectF area, Random rnd) {
      time += dt;
      float midX = area.centerX();
      float midY = area.centerY();
      
      
      atkSkillCooldown = Math.max(0f, atkSkillCooldown - dt);
      defSkillCooldown = Math.max(0f, defSkillCooldown - dt);
      if (atkActiveSkill != SKILL_NONE) {
        atkSkillDuration -= dt;
        if (atkSkillDuration <= 0f) {
          atkActiveSkill = SKILL_NONE;
        }
      }
      if (defActiveSkill != SKILL_NONE) {
        defSkillDuration -= dt;
        if (defSkillDuration <= 0f) {
          defActiveSkill = SKILL_NONE;
        }
      }

      float speedShield = 26f * dens;
      float speedSword = 38f * dens;
      float speedArcher = 24f * dens;

      float rangeShield = 16f * dens;
      float rangeSword = 22f * dens;
      float rangeArcher = 150f * dens;

      float windup = 1.60f;
      float minSep = 16.5f * dens;
      float arrowSpeed = 140f * dens;
      float maxArrowLife = 1.65f;
      

      if (battleTarget != null && battleTarget.buildingType == Prefecture.BUILDING_CANNON && 
          battleTarget.owner == battleTarget.battleAttackerOwner) {
        
        cannonCooldown -= dt;
        if (cannonCooldown <= 0f && time >= windup) {
          
          Unit target = null;
          float bestDist = Float.MAX_VALUE;
          for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.alive || u.side != SIDE_ATK) {
              continue;
            }
            float dx = u.x - midX;
            float dy = u.y - midY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) {
              bestDist = dist;
              target = u;
            }
          }
          if (target != null && bestDist < 200f * dens) {
            
            Projectile cannon = new Projectile();
            cannon.sx = midX;
            cannon.sy = midY;
            cannon.ex = target.x;
            cannon.ey = target.y;
            cannon.t = 0f;
            cannon.life = Math.max(0.3f, Math.min(1.2f, bestDist / (arrowSpeed * 1.2f)));
            cannon.side = SIDE_DEF;
            cannon.type = TYPE_ARCHER;
            cannon.color = getResources().getColor(R.color.cst_warning);
            projectiles.add(cannon);
            cannonCooldown = 3.0f + rnd.nextFloat() * 1.0f;
            addHitFx(target.x, target.y, TYPE_ARCHER, rnd);
          }
        }
      }

      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive) {
          continue;
        }
        if (!u.positioned) {
          placeUnit(u, area, rnd);
        }
      }

      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive) {
          continue;
        }
        u.cooldown = Math.max(0f, u.cooldown - dt);
        u.dashCd = Math.max(0f, u.dashCd - dt);
        if (u.dash > 0f) {
          u.dash = Math.max(0f, u.dash - dt);
        }

        Unit enemy = findNearestEnemy(u);
        if (enemy == null) {
          continue;
        }

        float dx = enemy.x - u.x;
        float dy = enemy.y - u.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.0001f) {
          dist = 0.0001f;
        }
        float nx = dx / dist;
        float ny = dy / dist;

        float spd = u.type == TYPE_SWORD ? speedSword : (u.type == TYPE_ARCHER ? speedArcher : speedShield);
        if (u.type == TYPE_SWORD && u.dash > 0f) {
          spd *= 2.35f;
        }
        
        if ((u.side == SIDE_ATK && atkActiveSkill == SKILL_CHARGE) ||
            (u.side == SIDE_DEF && defActiveSkill == SKILL_CHARGE)) {
          spd *= 1.5f;
        }

        if ((u.side == SIDE_ATK && atkCommand == COMMAND_RETREAT) ||
            (u.side == SIDE_DEF && defCommand == COMMAND_RETREAT)) {
          nx = -nx;
          ny = -ny;
        }
        float desiredRange = u.type == TYPE_ARCHER ? rangeArcher : (u.type == TYPE_SWORD ? rangeSword : rangeShield);

        boolean canAttack = time >= windup;
        canAttack = canAttack && dist <= desiredRange;

        if (canAttack && u.cooldown <= 0f) {
          if (u.type == TYPE_ARCHER) {
            fireArrow(u, enemy, dist, arrowSpeed, maxArrowLife);
            u.cooldown = 2.00f + rnd.nextFloat() * 0.50f;
          } else {
            boolean blocked = applyHit(u, enemy, rnd);
            if (blocked) {
              addBlockFx(enemy.x, enemy.y, rnd);
            } else {
              addHitFx(enemy.x, enemy.y, u.type, rnd);
            }
            u.cooldown = u.type == TYPE_SWORD ? (1.60f + rnd.nextFloat() * 0.40f) : (2.10f + rnd.nextFloat() * 0.45f);
          }
          continue;
        }

        float targetDist = u.type == TYPE_ARCHER ? (desiredRange * 0.85f) : (desiredRange * 0.55f);
        float move = dist - targetDist;
        if (move > 0f) {
          float step = Math.min(move, spd * dt);
          u.x += nx * step;
          u.y += ny * step;
        } else if (u.type == TYPE_ARCHER) {

          float backNeed = (desiredRange * 0.9f) - dist;
          if (backNeed > 0f) {
            float step = Math.min(backNeed, spd * 0.6f * dt);
            u.x -= nx * step;
            u.y -= ny * step;
          }
        }

        u.x = clamp(u.x, area.left + 8f * dens, area.right - 8f * dens);
        u.y = clamp(u.y, area.top + 8f * dens, area.bottom - 8f * dens);

        float bias = (u.side == SIDE_ATK ? 1f : -1f) * 0.55f;

        float forwardScale = 1f;
        if (Math.abs(move) < desiredRange * 0.25f) {
          forwardScale = 0.25f;
        }
        if (u.type == TYPE_ARCHER && dist < rangeArcher * 0.7f) {
          forwardScale = 0f;
        }
        u.x += bias * forwardScale * dt * 5f * dens;
        u.y += (float) Math.sin((time + i) * 2.0f) * dt * 3f * dens;

        if (u.type == TYPE_SWORD && time >= windup && u.dashCd <= 0f && dist < 95f * dens) {
          u.dash = 0.12f;
          u.dashCd = 1.10f;
        }

      }

      for (int i = 0; i < units.size(); i++) {
        Unit a = units.get(i);
        if (!a.alive) {
          continue;
        }
        for (int j = i + 1; j < units.size(); j++) {
          Unit b = units.get(j);
          if (!b.alive) {
            continue;
          }
          float dx = b.x - a.x;
          float dy = b.y - a.y;
          float d2 = dx * dx + dy * dy;
          if (d2 <= 0.0001f) {
            continue;
          }
          if (d2 < minSep * minSep) {
            float d = (float) Math.sqrt(d2);
            float push = (minSep - d) * 0.5f;
            float nx = dx / d;
            float ny = dy / d;
            a.x -= nx * push;
            a.y -= ny * push;
            b.x += nx * push;
            b.y += ny * push;
          }
        }
      }

      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive) {
          continue;
        }
        u.x = clamp(u.x, area.left + 8f * dens, area.right - 8f * dens);
        u.y = clamp(u.y, area.top + 8f * dens, area.bottom - 8f * dens);
      }

      // HP
      for (int i = units.size() - 1; i >= 0; i--) {
        if (!units.get(i).alive) {
          units.remove(i);
        }
      }

      for (int i = projectiles.size() - 1; i >= 0; i--) {
        Projectile p = projectiles.get(i);
        p.t += dt;
        if (p.t >= p.life) {
          Unit target = findAliveById(p.targetId);
          if (target != null) {
            boolean blocked = applyProjectileHit(p, target, rnd);
            if (blocked) {
              addBlockFx(target.x, target.y, rnd);
            } else {
              addHitFx(target.x, target.y, TYPE_ARCHER, rnd);
            }
          }
          projectiles.remove(i);
        }
      }

      for (int i = fxs.size() - 1; i >= 0; i--) {
        Fx fx = fxs.get(i);
        fx.t += dt;
        if (fx.t >= fx.life) {
          fxs.remove(i);
        }
      }

      if (units.isEmpty()) {
        float jitter = 6f * dens;
        addHitFx(midX + (rnd.nextFloat() - 0.5f) * jitter, midY + (rnd.nextFloat() - 0.5f) * jitter, TYPE_SWORD, rnd);
      }
    }

    void draw(Canvas canvas, RectF area, GameView view, int atkColor, int defColor, float alpha) {
      float r = 5.2f * dens;
      for (int i = 0; i < projectiles.size(); i++) {
        Projectile p = projectiles.get(i);
        float t = p.t / Math.max(0.001f, p.life);
        float x = p.sx + (p.ex - p.sx) * t;
        float y = quadAtLocal(p.sy, (p.sy + p.ey) * 0.5f - 50f * dens, p.ey, t);
        view.battleDotPaint.setColor(p.color);
        view.battleDotPaint.setAlpha((int) (220f * alpha));
        canvas.drawCircle(x, y, 2.2f * dens, view.battleDotPaint);
      }
      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive) {
          continue;
        }
        int body = (u.side == SIDE_ATK) ? atkColor : defColor;
        int accent = u.type == TYPE_SHIELD ? view.getResources().getColor(R.color.cst_accent)
            : (u.type == TYPE_SWORD ? view.getResources().getColor(R.color.cst_warning) : view.getResources().getColor(R.color.cst_accent_2));
        float unitSize = u.isGiant ? r * 1.5f : r;
        view.drawUnit(canvas, u.x, u.y, unitSize, body, accent, u.type, u.side == SIDE_ATK, alpha);
        // HP bar
        int maxHp = u.isGiant ? (u.type == TYPE_SHIELD ? 8 : (u.type == TYPE_SWORD ? 4 : 2))
            : (u.type == TYPE_SHIELD ? 4 : (u.type == TYPE_SWORD ? 2 : 1));
        float frac = Math.max(0f, Math.min(1f, u.hp / (float) maxHp));
        if (frac > 0f && frac < 1.01f) {
          float barW = r * 2.4f;
          float barH = r * 0.35f;
          float bx = u.x - barW * 0.5f;
          float by = u.y - r * 2.4f;
          RectF back = new RectF(bx, by, bx + barW, by + barH);
          RectF fill = new RectF(bx, by, bx + barW * frac, by + barH);
          view.battleBarTrackPaint.setAlpha((int) (180f * alpha));
          canvas.drawRoundRect(back, barH, barH, view.battleBarTrackPaint);
          int hpCol = u.type == TYPE_SHIELD ? view.getResources().getColor(R.color.cst_accent)
              : (u.type == TYPE_SWORD ? view.getResources().getColor(R.color.cst_warning) : view.getResources().getColor(R.color.cst_accent_2));
          view.battleBarFillPaint.setColor(hpCol);
          view.battleBarFillPaint.setAlpha((int) (220f * alpha));
          canvas.drawRoundRect(fill, barH, barH, view.battleBarFillPaint);
        }
      }

      for (int i = 0; i < fxs.size(); i++) {
        Fx fx = fxs.get(i);
        float p = fx.t / Math.max(0.001f, fx.life);
        int a = (int) (160f * alpha * (1f - p));
        view.battleFxPaint.setColor(fx.color);
        view.battleFxPaint.setAlpha(a);
        canvas.drawCircle(fx.x, fx.y, (10f * dens + 26f * dens * p), view.battleFxPaint);
        view.battleFxPaint.setColor(view.getResources().getColor(R.color.cst_text_primary));
        view.battleFxPaint.setAlpha(160);
      }
    }

    private void spawnSide(int side, int s, int w, int a, int gs, int gw, int ga, Random rnd) {
      // maxPerType

      int totalSpawned = 0;
      for (int i = 0; i < s; i++) {
        units.add(makeUnit(side, TYPE_SHIELD, false, rnd, totalSpawned));
        totalSpawned++;
      }
      for (int i = 0; i < w; i++) {
        units.add(makeUnit(side, TYPE_SWORD, false, rnd, totalSpawned));
        totalSpawned++;
      }
      for (int i = 0; i < a; i++) {
        units.add(makeUnit(side, TYPE_ARCHER, false, rnd, totalSpawned));
        totalSpawned++;
      }
      
      for (int i = 0; i < gs; i++) {
        units.add(makeUnit(side, TYPE_SHIELD, true, rnd, totalSpawned));
        totalSpawned++;
      }
      for (int i = 0; i < gw; i++) {
        units.add(makeUnit(side, TYPE_SWORD, true, rnd, totalSpawned));
        totalSpawned++;
      }
      for (int i = 0; i < ga; i++) {
        units.add(makeUnit(side, TYPE_ARCHER, true, rnd, totalSpawned));
        totalSpawned++;
      }
      
      String sideName = side == SIDE_ATK ? "ATK" : "DEF";
      android.util.Log.d("BattleSim", String.format("spawnSide %s: shield=%d, sword=%d, archer=%d, giant=%d/%d/%d, totalUnits=%d", 
        sideName, s, w, a, gs, gw, ga, totalSpawned));
    }

    private Unit makeUnit(int side, int type, boolean isGiant, Random rnd, int slot) {
      Unit u = new Unit();
      u.side = side;
      u.type = type;
      u.isGiant = isGiant;
      u.alive = true;
      u.positioned = false;
      u.id = nextId++;
      u.slot = slot;
      // HP
      if (isGiant) {
        u.hp = type == TYPE_SHIELD ? 8 : (type == TYPE_SWORD ? 4 : 2);
      } else {
        u.hp = type == TYPE_SHIELD ? 4 : (type == TYPE_SWORD ? 2 : 1);
      }
      u.cooldown = 0.80f + rnd.nextFloat() * 1.00f;
      u.dash = 0f;
      u.dashCd = 0.70f + rnd.nextFloat() * 0.9f;
      return u;
    }

    private Unit findNearestEnemy(Unit me) {
      Unit best = null;
      float bestD = Float.MAX_VALUE;
      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (!u.alive || u.side == me.side) {
          continue;
        }
        float dx = u.x - me.x;
        float dy = u.y - me.y;
        float d = dx * dx + dy * dy;
        if (d < bestD) {
          bestD = d;
          best = u;
        }
      }
      return best;
    }

    private void addHitFx(float x, float y, int type, Random rnd) {
      Fx fx = new Fx();
      fx.x = x;
      fx.y = y;
      fx.t = 0f;
      fx.life = 0.22f + rnd.nextFloat() * 0.12f;
      fx.color = type == TYPE_ARCHER ? Color.WHITE : (type == TYPE_SWORD ? Color.WHITE : Color.WHITE);
      fxs.add(fx);
    }

    private void addBlockFx(float x, float y, Random rnd) {
      Fx fx = new Fx();
      fx.x = x;
      fx.y = y;
      fx.t = 0f;
      fx.life = 0.16f + rnd.nextFloat() * 0.08f;
      fx.color = Color.WHITE;
      fxs.add(fx);
    }

    private void fireArrow(Unit archer, Unit target, float dist, float arrowSpeed, float maxLife) {
      Projectile p = new Projectile();
      p.sx = archer.x;
      p.sy = archer.y;
      p.ex = target.x;
      p.ey = target.y;
      p.t = 0f;
      p.life = Math.max(0.18f, Math.min(maxLife, dist / Math.max(1f, arrowSpeed)));
      p.side = archer.side;
      p.type = TYPE_ARCHER;
      p.targetId = target.id;
      p.color = Color.WHITE;
      projectiles.add(p);
    }

    private Unit findAliveById(int id) {
      for (int i = 0; i < units.size(); i++) {
        Unit u = units.get(i);
        if (u.alive && u.id == id) {
          return u;
        }
      }
      return null;
    }

    private boolean applyHit(Unit attacker, Unit target, Random rnd) {
      if (!target.alive) {
        return false;
      }
      if (target.type == TYPE_SHIELD) {
        float block = 0.55f;
        if (attacker.type == TYPE_SWORD) {
          block = 0.35f;
        }
        if (rnd.nextFloat() < block) {
          return true;
        }
      }
      int damage = 1;
      
      if (attacker.isGiant) {
        damage = 2;
      }
      
      if ((target.side == SIDE_ATK && defActiveSkill == SKILL_DEFENSE_FORM) ||
          (target.side == SIDE_DEF && atkActiveSkill == SKILL_DEFENSE_FORM)) {
        damage = Math.max(0, damage - 1);
      }
      // HP
      // if (target.isGiant && damage > 0) { damage = Math.max(1, damage - 1); }
      target.hp -= damage;
      if (target.hp <= 0) {
        target.alive = false;
      }
      return false;
    }

    private boolean applyProjectileHit(Projectile p, Unit target, Random rnd) {
      if (!target.alive) {
        return false;
      }
      if (target.type == TYPE_SHIELD) {
        if (rnd.nextFloat() < 0.65f) {
          return true;
        }
      }
      int damage = 1;
      
      if ((target.side == SIDE_ATK && defActiveSkill == SKILL_DEFENSE_FORM) ||
          (target.side == SIDE_DEF && atkActiveSkill == SKILL_DEFENSE_FORM)) {
        damage = Math.max(0, damage - 1);
      }
      // HP
      // if (target.isGiant && damage > 0) { damage = Math.max(1, damage - 1); }
      target.hp -= damage;
      if (target.hp <= 0) {
        target.alive = false;
      }
      return false;
    }

    private void placeUnit(Unit u, RectF area, Random rnd) {
      float half = area.width() * 0.5f;
      float sideLeft = u.side == SIDE_ATK ? area.left : (area.left + half);
      float sideRight = u.side == SIDE_ATK ? (area.left + half) : area.right;
      float depthFront = u.side == SIDE_ATK ? (sideRight - 24f * dens) : (sideLeft + 24f * dens);
      float depthMid = u.side == SIDE_ATK ? (sideRight - 70f * dens) : (sideLeft + 70f * dens);
      float depthBack = u.side == SIDE_ATK ? (sideRight - 120f * dens) : (sideLeft + 120f * dens);

      float x = u.type == TYPE_SHIELD ? depthFront : (u.type == TYPE_SWORD ? depthMid : depthBack);
      float lane = u.type == TYPE_SHIELD ? 0.30f : (u.type == TYPE_SWORD ? 0.55f : 0.78f);
      float y = area.top + area.height() * lane;

      float spacingY = 14f * dens;
      int col = u.slot % 5;
      int row = u.slot / 5;
      float jyBase = (col - 2) * spacingY;
      float jxBase = (row - 1) * 10f * dens;
      float jx = jxBase + (rnd.nextFloat() - 0.5f) * 10f * dens;
      float jy = jyBase + (rnd.nextFloat() - 0.5f) * 8f * dens;
      u.x = clamp(x + jx, sideLeft + 10f * dens, sideRight - 10f * dens);
      u.y = clamp(y + jy, area.top + 10f * dens, area.bottom - 10f * dens);
      u.positioned = true;
    }

    private static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
    }

    private static float quadAtLocal(float a, float b, float c, float t) {
      float u = 1f - t;
      return u * u * a + 2f * u * t * b + t * t * c;
    }
  }

  private void drawMapBackground(Canvas canvas) {

    float w = engine.getWorldMapWidth();
    float h = engine.getWorldMapHeight();
    if (w <= 1f || h <= 1f) {

      return;
    }


    if (!chinaPathInitialized) {
      chinaPathInitialized = true;
      chinaPath.reset();
      float left = w * 0.10f;
      float top = h * 0.05f;
      float right = w * 0.95f;
      float bottom = h * 0.90f;
      float midX = (left + right) * 0.5f;
      float midY = (top + bottom) * 0.5f;


      chinaPath.moveTo(left, midY * 0.6f);           
      chinaPath.lineTo(midX * 0.9f, top);           
      chinaPath.lineTo(right, midY * 0.55f);        
      chinaPath.lineTo(right * 0.95f, midY);        
      chinaPath.lineTo(right * 0.92f, bottom * 0.9f); 
      chinaPath.lineTo(midX * 0.9f, bottom);        
      chinaPath.lineTo(left * 0.9f, bottom * 0.9f); 
      chinaPath.lineTo(left * 0.8f, midY);          // /
      chinaPath.close();
    }

    paint.setColor(Color.rgb(210, 230, 240)); 
    canvas.drawPath(chinaPath, paint);
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setColor(Color.rgb(160, 185, 200));
    strokePaint.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
    canvas.drawPath(chinaPath, strokePaint);
  }
  
  private void drawTerritoryConnections(Canvas canvas) {
    List<Prefecture> prefs = engine.getPrefectures();
    Prefecture selected = engine.getSelectedPrefecturePublic();
    float dens = getResources().getDisplayMetrics().density;
    
    // /
    for (int i = 0; i < prefs.size(); i++) {
      Prefecture a = prefs.get(i);
      if (a.neighbors == null || a.polygonPoints == null) {
        continue;
      }
      for (int k = 0; k < a.neighbors.length; k++) {
        int j = a.neighbors[k];
        if (j <= a.id || j >= prefs.size()) {
          continue;
        }
        Prefecture b = prefs.get(j);
        if (b.polygonPoints == null) {
          continue;
        }
        

        drawRoadBetween(canvas, a, b, selected, dens);
      }
    }
  }
  
  private void drawRoadBetween(Canvas canvas, Prefecture a, Prefecture b, Prefecture selected, float dens) {
    
    float minDist = Float.MAX_VALUE;
    int aIdx = -1, bIdx = -1;
    
    for (int i = 0; i < a.polygonVertexCount; i++) {
      float ax = a.polygonPoints[i * 2];
      float ay = a.polygonPoints[i * 2 + 1];
      for (int j = 0; j < b.polygonVertexCount; j++) {
        float bx = b.polygonPoints[j * 2];
        float by = b.polygonPoints[j * 2 + 1];
        float dx = bx - ax;
        float dy = by - ay;
        float dist = dx * dx + dy * dy;
        if (dist < minDist) {
          minDist = dist;
          aIdx = i;
          bIdx = j;
        }
      }
    }
    
    if (aIdx >= 0 && bIdx >= 0 && minDist < (a.radius + b.radius) * 2f) {
      float ax = a.polygonPoints[aIdx * 2];
      float ay = a.polygonPoints[aIdx * 2 + 1];
      float bx = b.polygonPoints[bIdx * 2];
      float by = b.polygonPoints[bIdx * 2 + 1];
      

      Path roadPath = new Path();
      roadPath.moveTo(ax, ay);
      float midX = (ax + bx) * 0.5f;
      float midY = (ay + by) * 0.5f;

      Random rnd = new Random((a.id * 1000 + b.id));
      float offsetX = (rnd.nextFloat() * 2f - 1f) * 8f * dens;
      float offsetY = (rnd.nextFloat() * 2f - 1f) * 8f * dens;
      roadPath.quadTo(midX + offsetX, midY + offsetY, bx, by);
      
      if (selected != null && (selected.id == a.id || selected.id == b.id)) {
        linkPaint.setAlpha(220);
        linkPaint.setColor(getResources().getColor(R.color.cst_game_select));
        linkPaint.setStrokeWidth(5f * dens);
      } else {
        linkPaint.setAlpha(120);
        linkPaint.setColor(Color.argb(180, 180, 160, 120)); 
        linkPaint.setStrokeWidth(4f * dens);
      }
      linkPaint.setStyle(Paint.Style.STROKE);
      canvas.drawPath(roadPath, linkPaint);
    }
  }

  private void drawDrag(Canvas canvas) {
    if (!dragging || dragSource == null) {
      return;
    }
    
    float[] worldDrag = screenToWorld(dragX, dragY);
    float dragX_world = worldDrag[0];
    float dragY_world = worldDrag[1];
    

    dragPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 5f);
    dragPaint.setAlpha(220);
    canvas.drawLine(dragSource.x, dragSource.y, dragX_world, dragY_world, dragPaint);
    

    Prefecture target = engine.hitTest(dragX_world, dragY_world);
    if (target != null && target != dragSource) {
      // Path
      cachedTempPath.reset();
      if (target.polygonPoints != null && target.polygonPoints.length >= 6) {
        cachedTempPath.moveTo(target.polygonPoints[0], target.polygonPoints[1]);
        for (int j = 1; j < target.polygonVertexCount; j++) {
          cachedTempPath.lineTo(target.polygonPoints[j * 2], target.polygonPoints[j * 2 + 1]);
        }
        cachedTempPath.close();
      } else {
        cachedTempPath.addCircle(target.x, target.y, target.radius, Path.Direction.CW);
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.argb(100, 255, 200, 0));
      canvas.drawPath(cachedTempPath, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(Color.argb(255, 255, 200, 0));
      paint.setStrokeWidth(getResources().getDisplayMetrics().density * 4f);
      canvas.drawPath(cachedTempPath, paint);
    }
    
    // Path
    float dx = dragX_world - dragSource.x;
    float dy = dragY_world - dragSource.y;
    float dist = (float) Math.sqrt(dx * dx + dy * dy);
    if (dist > 10f) {
      float nx = dx / dist;
      float ny = dy / dist;
      float arrowSize = 15f * getResources().getDisplayMetrics().density;
      float arrowX = dragX_world - nx * arrowSize;
      float arrowY = dragY_world - ny * arrowSize;
      cachedTempPath.reset();
      cachedTempPath.moveTo(dragX_world, dragY_world);
      cachedTempPath.lineTo(arrowX - ny * arrowSize * 0.5f, arrowY + nx * arrowSize * 0.5f);
      cachedTempPath.lineTo(arrowX + ny * arrowSize * 0.5f, arrowY - nx * arrowSize * 0.5f);
      cachedTempPath.close();
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(getResources().getColor(R.color.cst_game_select));
      paint.setAlpha(220);
      canvas.drawPath(cachedTempPath, paint);
    }
  }

  private void drawNodes(Canvas canvas) {
    List<Prefecture> prefectures = engine.getPrefectures();
    float dens = getResources().getDisplayMetrics().density;
    // Path
    Path polygonPath = cachedGrassPath;
    
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      int color = engine.getOwnerColor(pref.owner);
      int totalUnits = pref.total();
      boolean lowDetail = cameraZoom < 1.1f;
      
      // Path
      polygonPath.reset();
      if (pref.polygonPoints != null && pref.polygonPoints.length >= 6) {
        polygonPath.moveTo(pref.polygonPoints[0], pref.polygonPoints[1]);
        for (int j = 1; j < pref.polygonVertexCount; j++) {
          polygonPath.lineTo(pref.polygonPoints[j * 2], pref.polygonPoints[j * 2 + 1]);
        }
        polygonPath.close();
      } else {

        polygonPath.addCircle(pref.x, pref.y, pref.radius, Path.Direction.CW);
      }
      
      // /+
      
      paint.setShader(null);
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(color);
      paint.setAlpha(255);

      canvas.drawPath(polygonPath, paint);
     

      // strokePaint
      strokePaint.setColor(getResources().getColor(R.color.cst_game_node_stroke));
      strokePaint.setStrokeWidth(2.5f * dens);
      strokePaint.setStyle(Paint.Style.STROKE);
      strokePaint.setAlpha(220);
      canvas.drawPath(polygonPath, strokePaint);


      if (pref.hasAttackedThisTurn && pref.owner == 1 && !lowDetail) {
        strokePaint.setColor(Color.argb(180, 150, 150, 150));
        strokePaint.setStrokeWidth(3f * dens);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{8f * dens, 4f * dens}, 0f));
        canvas.drawPath(polygonPath, strokePaint);
        strokePaint.setPathEffect(null);  
      }


      if (pref.battleActive && !lowDetail) {
        
        float battlePulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
        paint.setColor(Color.argb((int)(100 * battlePulse), 255, 0, 0));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(polygonPath, paint);
        

        strokePaint.setColor(Color.argb((int)(200 * battlePulse), 255, 50, 50));
        strokePaint.setStrokeWidth(4f * dens);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAlpha((int)(200 * battlePulse));
        canvas.drawPath(polygonPath, strokePaint);
        

        textPaint.setTextSize(pref.radius * 0.6f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.argb((int)(255 * battlePulse), 255, 255, 255));
        textPaint.setFakeBoldText(true);
        canvas.drawText("!", pref.x, pref.y + pref.radius * 0.2f, textPaint);
        textPaint.setFakeBoldText(false);
      }


      if (pref.selected && !lowDetail) {
        
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 300.0) * 0.2 + 0.8);
        strokePaint.setColor(getResources().getColor(R.color.cst_game_select));
        strokePaint.setStrokeWidth(6f * dens * pulse);
        strokePaint.setAlpha(255);
        canvas.drawPath(polygonPath, strokePaint);
        
        strokePaint.setStrokeWidth(3f * dens);
        strokePaint.setAlpha(180);
        canvas.drawPath(polygonPath, strokePaint);
      }

      // +
      if (lowDetail) {

        if (pref.name != null && pref.name.length() > 0) {
          textPaint.setTextSize(10f * dens);
          textPaint.setTextAlign(Paint.Align.CENTER);
          textPaint.setColor(Color.BLACK);
          textPaint.setFakeBoldText(true);
          canvas.drawText(pref.name, pref.x, pref.y - 6f * dens, textPaint);
          textPaint.setFakeBoldText(false);
        }

        textPaint.setTextSize(12f * dens);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(totalUnits), pref.x, pref.y + 8f * dens, textPaint);
        continue;
      }


      
      drawUnitsInsidePrefecture(canvas, pref, dens);

      
      if (pref.name != null && pref.name.length() > 0) {
        textPaint.setTextSize(pref.radius * 0.4f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        textPaint.setFakeBoldText(true);
        canvas.drawText(pref.name, pref.x, pref.y - pref.radius * 0.6f, textPaint);
        textPaint.setFakeBoldText(false);
      }

      
      textPaint.setTextSize(pref.radius * 0.5f);
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      canvas.drawText(String.valueOf(totalUnits), pref.x, pref.y + pref.radius * 0.15f, textPaint);

      // S/W/A
      textPaint.setTextSize(pref.radius * 0.25f);
      String comp = "S" + pref.shield + " W" + pref.sword + " A" + pref.archer;
      if (pref.totalGiant() > 0) {
        comp += " G" + pref.totalGiant();
      }
      textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
      canvas.drawText(comp, pref.x, pref.y + pref.radius * 0.75f, textPaint);


      if (pref.buildingType != Prefecture.BUILDING_NONE) {
        float iconSize = pref.radius * 0.35f;
        float iconX = pref.x + pref.radius * 0.65f;
        float iconY = pref.y - pref.radius * 0.65f;
        paint.setColor(getResources().getColor(R.color.cst_accent));
        paint.setAlpha(240);
        canvas.drawCircle(iconX, iconY, iconSize, paint);
        textPaint.setTextSize(iconSize * 0.8f);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        String iconText = "";
        switch (pref.buildingType) {
          case Prefecture.BUILDING_CANNON: iconText = "C"; break;
          case Prefecture.BUILDING_BARRACKS: iconText = "B"; break;
          case Prefecture.BUILDING_FORTRESS: iconText = "F"; break;
          case Prefecture.BUILDING_TRAINING: iconText = "T"; break;
        }
        canvas.drawText(iconText, iconX, iconY + iconSize * 0.3f, textPaint);
      }


      if (pref.placedSkill != Prefecture.SKILL_NONE) {
        float skillSize = pref.radius * 0.3f;
        float skillX = pref.x - pref.radius * 0.65f;
        float skillY = pref.y - pref.radius * 0.65f;
        paint.setColor(getResources().getColor(R.color.cst_warning));
        paint.setAlpha(200);
        canvas.drawCircle(skillX, skillY, skillSize, paint);
        textPaint.setTextSize(skillSize * 0.9f);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        String skillText = "";
        switch (pref.placedSkill) {
          case Prefecture.SKILL_CHARGE: skillText = "C"; break;
          case Prefecture.SKILL_ARROW_RAIN: skillText = "A"; break;
          case Prefecture.SKILL_DEFENSE: skillText = "D"; break;
          case Prefecture.SKILL_RETREAT: skillText = "R"; break;
        }
        canvas.drawText(skillText, skillX, skillY + skillSize * 0.3f, textPaint);
      }


      if (pref.owner == 1 && pref.goldIncome > 0) {
        textPaint.setTextSize(pref.radius * 0.2f);
        textPaint.setColor(getResources().getColor(R.color.cst_accent));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("$" + String.format("%.1f", pref.goldIncome),
          pref.x - pref.radius * 0.5f, pref.y + pref.radius * 1.05f, textPaint);
      }
    }
  }
  
  private void drawUnitsInsidePrefecture(Canvas canvas, Prefecture pref, float dens) {
    if (pref.totalNormal() == 0 && pref.totalGiant() == 0) {
      return;
    }
    
    // 3
    float unitSize = Math.max(3f * dens, pref.radius * 0.12f);
    float spacing = unitSize * 1.8f;
    float startY = pref.y - pref.radius * 0.3f;
    float centerX = pref.x;
    

    if (pref.shield > 0) {
      int count = Math.min(pref.shield, 8); // 8
      float startX = centerX - (count - 1) * spacing * 0.5f;
      for (int i = 0; i < count; i++) {
        float x = startX + i * spacing;
        paint.setColor(getResources().getColor(R.color.cst_accent));
        paint.setAlpha(220);
        canvas.drawCircle(x, startY, unitSize, paint);
        // RectF
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * dens);
        paint.setColor(getResources().getColor(R.color.cst_text_primary));
        cachedRectF1.set(x - unitSize * 0.6f, startY - unitSize * 0.8f,
          x + unitSize * 0.6f, startY + unitSize * 0.8f);
        canvas.drawRoundRect(cachedRectF1, unitSize * 0.3f, unitSize * 0.3f, paint);
      paint.setStyle(Paint.Style.FILL);
      }
      if (pref.shield > 8) {
        textPaint.setTextSize(unitSize * 0.8f);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        canvas.drawText("+" + (pref.shield - 8), startX + 8 * spacing, startY + unitSize * 0.3f, textPaint);
      }
    }
    

    if (pref.sword > 0) {
      int count = Math.min(pref.sword, 8);
      float startX = centerX - (count - 1) * spacing * 0.5f;
      float y = startY + spacing;
      for (int i = 0; i < count; i++) {
        float x = startX + i * spacing;
        paint.setColor(getResources().getColor(R.color.cst_warning));
        paint.setAlpha(220);
        canvas.drawCircle(x, y, unitSize, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * dens);
        paint.setColor(getResources().getColor(R.color.cst_text_primary));
        canvas.drawLine(x, y - unitSize * 0.6f, x, y + unitSize * 0.6f, paint);
        paint.setStyle(Paint.Style.FILL);
      }
      if (pref.sword > 8) {
        textPaint.setTextSize(unitSize * 0.8f);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        canvas.drawText("+" + (pref.sword - 8), startX + 8 * spacing, y + unitSize * 0.3f, textPaint);
      }
    }
    

    if (pref.archer > 0) {
      int count = Math.min(pref.archer, 8);
      float startX = centerX - (count - 1) * spacing * 0.5f;
      float y = startY + spacing * 2;
      for (int i = 0; i < count; i++) {
        float x = startX + i * spacing;
        paint.setColor(getResources().getColor(R.color.cst_accent_2));
        paint.setAlpha(220);
        canvas.drawCircle(x, y, unitSize, paint);
        // RectF
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * dens);
        paint.setColor(getResources().getColor(R.color.cst_text_primary));
        cachedRectF2.set(x - unitSize * 0.5f, y - unitSize * 0.5f,
          x + unitSize * 0.5f, y + unitSize * 0.5f);
        canvas.drawArc(cachedRectF2, 30f, 120f, false, paint);
        paint.setStyle(Paint.Style.FILL);
      }
      if (pref.archer > 8) {
        textPaint.setTextSize(unitSize * 0.8f);
        textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
        canvas.drawText("+" + (pref.archer - 8), startX + 8 * spacing, y + unitSize * 0.3f, textPaint);
      }
    }
    

    if (pref.totalGiant() > 0) {
      float giantX = pref.x + pref.radius * 0.5f;
      float giantY = pref.y + pref.radius * 0.4f;
      float giantSize = unitSize * 1.3f;
      paint.setColor(getResources().getColor(R.color.cst_accent_2));
      paint.setAlpha(240);
      canvas.drawCircle(giantX, giantY, giantSize, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2f * dens);
      paint.setColor(getResources().getColor(R.color.cst_warning));
      canvas.drawCircle(giantX, giantY, giantSize + 1f * dens, paint);
      paint.setStyle(Paint.Style.FILL);
      textPaint.setTextSize(giantSize * 0.7f);
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      textPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText("*", giantX, giantY + giantSize * 0.3f, textPaint);
    }
  }

  private void drawParticles(Canvas canvas) {

    // List<Particle> particles = engine.getParticles();
    // for (int i = 0; i < particles.size(); i++) {
    // Particle particle = particles.get(i);
    // paint.setShader(null);
    // paint.setStyle(Paint.Style.FILL);
    // paint.setColor(engine.getOwnerParticleColor(particle.owner));
    // canvas.drawCircle(particle.x, particle.y, particle.radius, paint);
    // }
  }

  private void drawBattleOverlays(Canvas canvas) {
    List<Prefecture> prefs = engine.getPrefectures();
    long now = SystemClock.elapsedRealtime();
    for (int i = 0; i < prefs.size(); i++) {
      Prefecture pref = prefs.get(i);
      if (!pref.battleActive) {
        continue;
      }
      drawBattlePanel(canvas, pref, now);
    }
  }

  private void drawBattlePanel(Canvas canvas, Prefecture target, long now) {
    float dens = getResources().getDisplayMetrics().density;
    float pad = 6f * dens;
    float w = Math.max(160f * dens, target.radius * 5.4f);
    float h = Math.max(72f * dens, target.radius * 2.6f);
    float x0 = target.x - w * 0.5f;
    float y0 = target.y - target.radius - h - 10f * dens;
    if (y0 < 6f * dens) {
      y0 = target.y + target.radius + 10f * dens;
    }
    if (x0 < 6f * dens) {
      x0 = 6f * dens;
    }
    if (x0 + w > canvas.getWidth() - 6f * dens) {
      x0 = canvas.getWidth() - 6f * dens - w;
    }
    RectF rect = new RectF(x0, y0, x0 + w, y0 + h);
    float r = 10f * dens;
    canvas.drawRoundRect(rect, r, r, battlePanelPaint);
    canvas.drawRoundRect(rect, r, r, battleStrokePaint);

    int defS = Math.max(0, target.shield);
    int defW = Math.max(0, target.sword);
    int defA = Math.max(0, target.archer);
    int atkS = Math.max(0, target.battleAtkShield);
    int atkW = Math.max(0, target.battleAtkSword);
    int atkA = Math.max(0, target.battleAtkArcher);
    int defT = Math.max(0, defS + defW + defA);
    int atkT = Math.max(0, atkS + atkW + atkA);
    int maxT = Math.max(1, Math.max(defT, atkT));

    float titleY = rect.top + pad + 10f * dens;
    textPaint.setTextSize(12f * dens);
    textPaint.setTextAlign(Paint.Align.LEFT);
    int atkColor = engine.getOwnerColor(target.battleAttackerOwner);
    int defColor = engine.getOwnerColor(target.owner);
    textPaint.setColor(atkColor);
    canvas.drawText("ATK", rect.left + pad, titleY, textPaint);
    textPaint.setColor(defColor);
    canvas.drawText("DEF", rect.left + w * 0.5f + pad * 0.2f, titleY, textPaint);

    float barLeftAtk = rect.left + pad;
    float barLeftDef = rect.left + w * 0.5f + pad * 0.2f;
    float barW = w * 0.5f - pad * 1.2f;
    float barH = 6f * dens;
    float gap = 6f * dens;
    float baseY = titleY + 10f * dens;

    drawTypeBar(canvas, barLeftAtk, baseY + 0f * (barH + gap), barW, barH, atkS, maxT, getResources().getColor(R.color.cst_accent));
    drawTypeBar(canvas, barLeftAtk, baseY + 1f * (barH + gap), barW, barH, atkW, maxT, getResources().getColor(R.color.cst_warning));
    drawTypeBar(canvas, barLeftAtk, baseY + 2f * (barH + gap), barW, barH, atkA, maxT, getResources().getColor(R.color.cst_accent_2));

    drawTypeBar(canvas, barLeftDef, baseY + 0f * (barH + gap), barW, barH, defS, maxT, getResources().getColor(R.color.cst_accent));
    drawTypeBar(canvas, barLeftDef, baseY + 1f * (barH + gap), barW, barH, defW, maxT, getResources().getColor(R.color.cst_warning));
    drawTypeBar(canvas, barLeftDef, baseY + 2f * (barH + gap), barW, barH, defA, maxT, getResources().getColor(R.color.cst_accent_2));

    float cx = rect.centerX();
    float cy = rect.top + h * 0.56f;
    float phase = (now % 600L) / 600f;
    float len = 10f * dens + 6f * dens * (float) Math.sin(phase * Math.PI * 2f);
    battleFxPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawLine(cx - len, cy - len, cx + len, cy + len, battleFxPaint);
    canvas.drawLine(cx - len, cy + len, cx + len, cy - len, battleFxPaint);

    float fieldH = 18f * dens;
    float fieldTop = rect.bottom - pad - 18f * dens - 14f * dens;
    RectF field = new RectF(rect.left + pad, fieldTop, rect.right - pad, fieldTop + fieldH);
    drawBattleDots(canvas, field, target, now, atkColor, defColor, atkS, atkW, atkA, defS, defW, defA, maxT);

    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(11f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    canvas.drawText(atkT + " vs " + defT, rect.centerX(), rect.bottom - pad, textPaint);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
  }

  private void drawBattleDots(Canvas canvas, RectF field, Prefecture target, long now, int atkColor, int defColor,
                              int atkS, int atkW, int atkA, int defS, int defW, int defA, int maxT) {
    float dens = getResources().getDisplayMetrics().density;
    float rowGap = field.height() / 3f;
    float r = 1.9f * dens;
    float step = r * 2.7f;
    int maxDots = Math.max(8, Math.min(16, (int) (field.width() / step)));
    int scale = Math.max(4, (int) Math.ceil(maxT / (float) maxDots));
    int aS = Math.min(maxDots, (int) Math.ceil(atkS / (float) scale));
    int aW = Math.min(maxDots, (int) Math.ceil(atkW / (float) scale));
    int aA = Math.min(maxDots, (int) Math.ceil(atkA / (float) scale));
    int dS = Math.min(maxDots, (int) Math.ceil(defS / (float) scale));
    int dW = Math.min(maxDots, (int) Math.ceil(defW / (float) scale));
    int dA = Math.min(maxDots, (int) Math.ceil(defA / (float) scale));

    float t = (now % 900L) / 900f;
    float wave = (float) Math.sin(t * Math.PI * 2f);
    float hit = (float) Math.sin((now % 180L) / 180f * Math.PI * 2f);
    float push = (field.width() * 0.08f) * wave + (field.width() * 0.01f) * hit;

    float midX = field.centerX();
    float leftBase = field.left + field.width() * 0.18f + push;
    float rightBase = field.right - field.width() * 0.18f - push;

    battleDotPaint.setAlpha(220);

    int sCol = getResources().getColor(R.color.cst_accent);
    int wCol = getResources().getColor(R.color.cst_warning);
    int aCol = getResources().getColor(R.color.cst_accent_2);

    float yS = field.top + rowGap * 0.5f;
    float yW = field.top + rowGap * 1.5f;
    float yA = field.top + rowGap * 2.5f;

    drawDotRow(canvas, leftBase, yS, aS, r, sCol);
    drawDotRow(canvas, leftBase, yW, aW, r, wCol);
    drawDotRow(canvas, leftBase, yA, aA, r, aCol);

    drawDotRow(canvas, rightBase, yS, dS, r, sCol);
    drawDotRow(canvas, rightBase, yW, dW, r, wCol);
    drawDotRow(canvas, rightBase, yA, dA, r, aCol);

    float clash = (field.width() * 0.02f) * (0.5f + 0.5f * (float) Math.sin((now % 260L) / 260f * Math.PI * 2f));
    battleFxPaint.setAlpha(140);
    canvas.drawCircle(midX, field.centerY(), clash + 2f * dens, battleFxPaint);
    battleFxPaint.setAlpha(90);
    canvas.drawCircle(midX, field.centerY(), clash + 6f * dens, battleFxPaint);

    float shieldPulse = (now % 520L) / 520f;
    if (aS + dS > 0) {
      float pr = (field.width() * 0.05f) + (field.width() * 0.06f) * shieldPulse;
      battleFxPaint.setAlpha((int) (70 * (1f - shieldPulse)));
      battleFxPaint.setColor(sCol);
      canvas.drawCircle(midX, yS, pr, battleFxPaint);
      battleFxPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      battleFxPaint.setAlpha(160);
    }

    float dashPhase = (now % 380L) / 380f;
    if (aW > 0) {
      float sx = leftBase + field.width() * 0.08f;
      float ex = midX - field.width() * 0.05f;
      float px = sx + (ex - sx) * dashPhase;
      battleArrowPaint.setColor(wCol);
      battleArrowPaint.setAlpha(160);
      canvas.drawLine(px - 6f * dens, yW - 3f * dens, px + 2f * dens, yW + 3f * dens, battleArrowPaint);
    }
    if (dW > 0) {
      float sx = rightBase - field.width() * 0.08f;
      float ex = midX + field.width() * 0.05f;
      float px = sx + (ex - sx) * dashPhase;
      battleArrowPaint.setColor(wCol);
      battleArrowPaint.setAlpha(160);
      canvas.drawLine(px + 6f * dens, yW - 3f * dens, px - 2f * dens, yW + 3f * dens, battleArrowPaint);
    }

    float arrowPhase = (now % 420L) / 420f;
    if (aA > 0) {
      float y = yA;
      float sx = leftBase + field.width() * 0.08f;
      float ex = midX + field.width() * 0.12f;
      float mx = (sx + ex) * 0.5f;
      float my = y - field.height() * 0.55f;
      float ax = sx + (ex - sx) * arrowPhase;
      float ay = quadAt(y, my, y, arrowPhase);
      battleArrowPaint.setColor(aCol);
      battleArrowPaint.setAlpha(140);
      battlePath.reset();
      battlePath.moveTo(sx, y);
      battlePath.quadTo(mx, my, ex, y);
      canvas.drawPath(battlePath, battleArrowPaint);
      canvas.drawCircle(ax, ay, 1.6f * dens, battleDotPaint);
    }
    if (dA > 0) {
      float y = yA;
      float sx = rightBase - field.width() * 0.08f;
      float ex = midX - field.width() * 0.12f;
      float mx = (sx + ex) * 0.5f;
      float my = y - field.height() * 0.55f;
      float ax = sx + (ex - sx) * arrowPhase;
      float ay = quadAt(y, my, y, arrowPhase);
      battleArrowPaint.setColor(aCol);
      battleArrowPaint.setAlpha(140);
      battlePath.reset();
      battlePath.moveTo(sx, y);
      battlePath.quadTo(mx, my, ex, y);
      canvas.drawPath(battlePath, battleArrowPaint);
      canvas.drawCircle(ax, ay, 1.6f * dens, battleDotPaint);
    }
  }

  private float quadAt(float a, float b, float c, float t) {
    float u = 1f - t;
    return u * u * a + 2f * u * t * b + t * t * c;
  }

  private void drawDotRow(Canvas canvas, float centerX, float centerY, int count, float r, int color) {
    if (count <= 0) {
      return;
    }
    battleDotPaint.setColor(color);
    float step = r * 2.7f;
    float startX = centerX - (count - 1) * step * 0.5f;
    for (int i = 0; i < count; i++) {
      canvas.drawCircle(startX + i * step, centerY, r, battleDotPaint);
    }
  }

  private void drawTypeBar(Canvas canvas, float x, float y, float w, float h, int value, int max, int color) {
    RectF track = new RectF(x, y, x + w, y + h);
    canvas.drawRoundRect(track, h, h, battleBarTrackPaint);
    float frac = Math.max(0f, Math.min(1f, value / (float) max));
    float fw = w * frac;
    if (fw < 1f) {
      return;
    }
    battleBarFillPaint.setColor(color);
    battleBarFillPaint.setAlpha(220);
    RectF fill = new RectF(x, y, x + fw, y + h);
    canvas.drawRoundRect(fill, h, h, battleBarFillPaint);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    
    if (shopPanelActive) {
      float x = event.getX();
      float y = event.getY();
      float dens = getResources().getDisplayMetrics().density;
      float w = getWidth();
      float panelW = Math.min(360f * dens, w * 0.38f);
      float panelH = getHeight() - 80f * dens;
      float panelX = w - panelW - 12f * dens;
      float panelY = 60f * dens;
      RectF panel = new RectF(panelX, panelY, panelX + panelW, panelY + panelH);
      
      int action = event.getAction();

      if (panel.contains(x, y)) {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
          handleShopPanelTouch(event);
      return true;
    }
        return true; 
      }

      // /
      if (action == MotionEvent.ACTION_UP) {
        float[] worldPos = screenToWorld(x, y);
        Prefecture hit = engine.hitTest(worldPos[0], worldPos[1]);
        // /
        if (hit != null && hit.owner != 1) {
          // /
          return false; 
        }
        
        shopPanelActive = false;
        shopPanelTarget = null;
        return true;
      }
      // DOWN
      return false;
    }
    if (battleOverlayActive && battleOverlayAlpha > 0.02f) {
      handleBattleOverlayTouch(event);
      return true;
    }

    // UI
    int action = event.getAction();
    if (action == MotionEvent.ACTION_DOWN) {
      float x = event.getX();
      float y = event.getY();
      if (zoomInRect.contains(x, y) || zoomOutRect.contains(x, y) || zoomFitRect.contains(x, y)) {
        return true;
      }
      if (engine.isPlaying() && engine.isPlayerTurn() && !hasActiveBattles() && endTurnButtonRect.contains(x, y)) {
        return true;
      }
    }
    if (action == MotionEvent.ACTION_UP) {
      float x = event.getX();
      float y = event.getY();
      if (zoomInRect.contains(x, y)) {
        applyZoomAt(x, y, 1.15f);
        return true;
      }
      if (zoomOutRect.contains(x, y)) {
        applyZoomAt(x, y, 1f / 1.15f);
        return true;
      }
      if (zoomFitRect.contains(x, y)) {
        centerCameraOnMap();
        return true;
      }
      
      if (engine.isPlaying() && engine.isPlayerTurn() && !hasActiveBattles() && endTurnButtonRect.contains(x, y)) {
        engine.endPlayerTurn();
        return true;
      }
    }
    
    
    if (skillPlacementMode) {
      if (action == MotionEvent.ACTION_UP) {
        float x = event.getX();
        float y = event.getY();
        float[] world = screenToWorld(x, y);
        Prefecture hit = engine.hitTest(world[0], world[1]);
        if (hit != null && hit.owner == 1 && selectedSkillToPlace != Prefecture.SKILL_NONE) {
          hit.placedSkill = selectedSkillToPlace;
          skillPlacementMode = false;
          selectedSkillToPlace = Prefecture.SKILL_NONE;
        } else {
          
          skillPlacementMode = false;
          selectedSkillToPlace = Prefecture.SKILL_NONE;
        }
        return true;
      }
      return true;
    }

    // ScaleGestureDetector
    if (scaleDetector != null) {
      scaleDetector.onTouchEvent(event);
      if (isScaling) {
        return true;
      }
    }
    
    // / / /
    if (action == MotionEvent.ACTION_DOWN) {
      downX = event.getX();
      downY = event.getY();
      float[] worldPos = screenToWorld(downX, downY);
      Prefecture hit = engine.hitTest(worldPos[0], worldPos[1]);
      tapCandidatePref = hit;
      dragging = false;

      if (hit == null) {
        
        isPanning = true;
        panStartX = downX;
        panStartY = downY;
        panStartCameraX = cameraX;
        panStartCameraY = cameraY;
        engine.clearSelectionPublic();
        shopPanelActive = false;
        shopPanelTarget = null;
        dragging = false;
        dragSource = null;
        longPressStartTime = 0;
        return true;
      }

      // /
      if (!engine.isPlaying()) {
        isPanning = true;
        panStartX = downX;
        panStartY = downY;
        panStartCameraX = cameraX;
        panStartCameraY = cameraY;
        dragging = false;
        dragSource = null;
        longPressStartTime = 0;
    return true;
      }

      if (hit.owner == 1) {

        engine.selectPrefecturePublic(hit);
        dragSource = hit;
        dragX = downX;
        dragY = downY;
        longPressStartTime = SystemClock.elapsedRealtime();
        return true;
      } else {
        // / ACTION_UP
        // ""
        // ACTION_DOWN ACTION_UP
        engine.selectPrefecturePublic(hit);
        dragging = false;
        dragSource = null;
        longPressStartTime = 0;
        return true;
      }
    }
    if (action == MotionEvent.ACTION_MOVE) {
      if (isPanning) {
        
        float dx = event.getX() - panStartX;
        float dy = event.getY() - panStartY;
        cameraX = panStartCameraX + dx;
        cameraY = panStartCameraY + dy;
        return true;
      }
      if (dragSource != null) {
        float dx = event.getX() - downX;
        float dy = event.getY() - downY;
        float dist2 = dx * dx + dy * dy;
        // 8040
        if (!dragging && dist2 > 40f * 40f) {
          dragging = true;
          shopPanelActive = false;
          shopPanelTarget = null;
        }
        if (dragging) {
          dragX = event.getX();
          dragY = event.getY();
        }
      }
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      isPanning = false;
      if (dragging && dragSource != null) {
        float[] worldPos = screenToWorld(event.getX(), event.getY());
        Prefecture target = engine.hitTest(worldPos[0], worldPos[1]);
        if (target != null) {
          engine.tryPlayerSend(dragSource, target);
        }
      } else {
        // ""
        if (tapCandidatePref != null) {
          Prefecture selected = engine.getSelectedPrefecturePublic();
          if (tapCandidatePref.owner == 1) {
            
            if (selected != null && selected.id == tapCandidatePref.id) {

              shopPanelActive = true;
              shopPanelTarget = tapCandidatePref;
            } else {

              engine.selectPrefecturePublic(tapCandidatePref);
              shopPanelActive = false;
              shopPanelTarget = null;
            }
          } else {
            // /

            engine.selectPrefecturePublic(tapCandidatePref);
            shopPanelActive = true;
            shopPanelTarget = tapCandidatePref;
          }
        } else {

          engine.clearSelectionPublic();
          shopPanelActive = false;
          shopPanelTarget = null;
        }
      }
      dragging = false;
      dragSource = null;
      longPressStartTime = 0;
      tapCandidatePref = null;
      return true;
    }
    return true;
  }
  
  private void handleShopPanelTouch(MotionEvent event) {
    int action = event.getAction();
    if (action != MotionEvent.ACTION_UP) {
      return;
    }
    float x = event.getX();
    float y = event.getY();
    float dens = getResources().getDisplayMetrics().density;
    float w = getWidth();
    float h = getHeight();

    // drawShopPanel
    float panelW = Math.min(360f * dens, w * 0.38f);
    float panelH = h - 80f * dens;
    float panelX = w - panelW - 12f * dens;
    float panelY = 60f * dens;
    RectF panel = new RectF(panelX, panelY, panelX + panelW, panelY + panelH);
    
    
    float closeBtnSize = 40f * dens;
    RectF closeBtn = new RectF(panel.right - closeBtnSize - 10f * dens, panel.top + 10f * dens,
      panel.right - 10f * dens, panel.top + 10f * dens + closeBtnSize);
    if (closeBtn.contains(x, y)) {
      shopPanelActive = false;
      shopPanelTarget = null;
      return;
    }
    
    
    if (!panel.contains(x, y)) {
      return;
    }
    
    // /
    if (shopPanelTarget != null && shopPanelTarget.owner != 1) {
      Prefecture attackSource = findNeighboringPlayerPrefecture(shopPanelTarget);
      if (attackSource != null) {

        float unitIconSize = 16f * dens;
        float troopY = panelY + 80f * dens;
        float troopHeight = 0f;
        if (shopPanelTarget.shield > 0) troopHeight += unitIconSize + 6f * dens;
        if (shopPanelTarget.sword > 0) troopHeight += unitIconSize + 6f * dens;
        if (shopPanelTarget.archer > 0) troopHeight += unitIconSize + 6f * dens;
        if (shopPanelTarget.totalGiant() > 0) troopHeight += unitIconSize * 1.3f + 6f * dens;
        

        float goldY = troopY + troopHeight + 10f * dens;
        float btnY = goldY + 30f * dens;
        float attackBtnH = 55f * dens; 
        float attackBtnW = panelW - 40f * dens;
        RectF attackBtn = new RectF(panelX + 20f * dens, btnY, 
          panelX + 20f * dens + attackBtnW, btnY + attackBtnH);
        
        if (attackBtn.contains(x, y)) {
          
          boolean success = engine.tryPlayerSend(attackSource, shopPanelTarget);
          if (success) {
            
            shopPanelActive = false;
            shopPanelTarget = null;
          }
          return;
        }
      }
      // /
      return;
    }
    
    
    if (shopPanelTarget == null || shopPanelTarget.owner != 1) {
      return;
    }

    

    float unitIconSize = 16f * dens;
    float troopY = panelY + 80f * dens;
    float troopHeight = 0f;
    if (shopPanelTarget.shield > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.sword > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.archer > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.totalGiant() > 0) troopHeight += unitIconSize * 1.3f + 6f * dens;
    

    float goldY = troopY + troopHeight + 10f * dens;
    

    float btnY = goldY + 30f * dens;
    float btnH = 50f * dens;
    float btnSpacing = 8f * dens;
    float btnW = (panelW - 40f * dens) / 2f;
    

    // ""
    // btnY - 5f * densY btnY
    for (int i = 0; i < 3; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = btnY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      if (btnRect.contains(x, y)) {
        boolean success = engine.buyUnits(shopPanelTarget, i, false, 1);
        if (success) {

          
          postInvalidate();
        }
        return;
      }
    }
    

    float giantY = btnY + 3 * (btnH + btnSpacing) + 20f * dens;
    for (int i = 0; i < 3; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = giantY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      if (btnRect.contains(x, y)) {
        boolean success = engine.buyUnits(shopPanelTarget, i, true, 1);

        return;
      }
    }
    

    float buildY = giantY + 3 * (btnH + btnSpacing) + 20f * dens;
    int[] buildings = {
      Prefecture.BUILDING_CANNON,
      Prefecture.BUILDING_BARRACKS,
      Prefecture.BUILDING_FORTRESS,
      Prefecture.BUILDING_TRAINING
    };
    for (int i = 0; i < 4; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = buildY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      if (btnRect.contains(x, y)) {
        engine.buildStructure(shopPanelTarget, buildings[i]);
        return;
      }
    }
    

    float skillY = buildY + 3 * (btnH + btnSpacing) + 24f * dens + 18f * dens;
    float skillSize = 30f * dens;
    float skillGap = 12f * dens;
    float sx = panelX + 20f * dens;
    int[] skills = {
      Prefecture.SKILL_CHARGE,
      Prefecture.SKILL_ARROW_RAIN,
      Prefecture.SKILL_DEFENSE,
      Prefecture.SKILL_RETREAT
    };
    for (int i = 0; i < skills.length; i++) {
      RectF r = new RectF(sx + i * (skillSize + skillGap), skillY,
        sx + i * (skillSize + skillGap) + skillSize, skillY + skillSize);
      if (r.contains(x, y)) {
        
        if (shopPanelTarget.placedSkill == skills[i]) {
          shopPanelTarget.placedSkill = Prefecture.SKILL_NONE;
        } else {
          shopPanelTarget.placedSkill = skills[i];
        }
        return;
      }
    }
  }

  private void handleBattleOverlayTouch(MotionEvent event) {
    int action = event.getAction();
    if (action != MotionEvent.ACTION_UP) {
      return;
    }
    float x = event.getX();
    float y = event.getY();
    float dens = getResources().getDisplayMetrics().density;
    float pad = 14f * dens;
    float w = getWidth() - pad * 2f;
    float h = getHeight() - pad * 2f;
    RectF rect = new RectF(pad, pad, pad + w, pad + h);
    float topBarH = 44f * dens;
    float battleTop = rect.top + topBarH + pad;
    float skillBarH = battleOverlayRunning ? 50f * dens : 0f;
    float battleBottom = rect.bottom - pad - 40f * dens - skillBarH;
    
    
    if (battleOverlayRunning && battleSim != null && skillBarH > 0f) {
      float skillY = battleBottom + 8f * dens;
      float skillH = 42f * dens;
      RectF skillArea = new RectF(rect.left + pad, skillY, rect.right - pad, skillY + skillH);
      if (skillArea.contains(x, y)) {
        Prefecture target = findAnyActiveBattle();
        if (target != null) {
          int playerSide = target.battleAttackerOwner == 1 ? BattleSim.SIDE_ATK : BattleSim.SIDE_DEF;
          float btnW = (skillArea.width() - 20f * dens) / 4f;
          float btnSpacing = 4f * dens;
          int btnIndex = (int) ((x - skillArea.left) / (btnW + btnSpacing));
          if (btnIndex >= 0 && btnIndex < 4) {
            int[] skills = {
              BattleSim.SKILL_CHARGE,
              BattleSim.SKILL_ARROW_RAIN,
              BattleSim.SKILL_DEFENSE_FORM,
              BattleSim.SKILL_RETREAT
            };
            battleSim.useSkill(playerSide, skills[btnIndex]);
          }
        }
        return;
      }
    }
    
    float barY = battleOverlayRunning ? (battleBottom + skillBarH + 8f * dens) : (battleBottom + 8f * dens);
    RectF bar = new RectF(rect.left + pad, barY, rect.right - pad, rect.bottom - pad);

    if (!bar.contains(x, y)) {
      return;
    }

    if (!battleOverlayRunning && !battleOverlayFinished) {
      battleOverlayRunning = true;
      return;
    }

    if (battleOverlayFinished) {
      battleOverlayDir = -1f;
    }
  }
  
  private void drawShopPanel(Canvas canvas) {
    if (shopPanelTarget == null) {
      return;
    }
    // shopPanelTarget
    // shopPanelTarget
    if (shopPanelTarget.battleActive && battleOverlayActive && battleOverlayTargetId == shopPanelTarget.id) {

      return;
    }
    float dens = getResources().getDisplayMetrics().density;
    float w = getWidth();
    float h = getHeight();
    // 1/3
    float panelW = Math.min(360f * dens, w * 0.38f);
    float panelH = h - 80f * dens;
    float panelX = w - panelW - 12f * dens;
    float panelY = 60f * dens;
    RectF panel = new RectF(panelX, panelY, panelX + panelW, panelY + panelH);
    

    paint.setStyle(Paint.Style.FILL);
    
    paint.setColor(Color.argb(100, 0, 0, 0));
    canvas.drawRoundRect(panel.left + 4f * dens, panel.top + 4f * dens, 
      panel.right + 4f * dens, panel.bottom + 4f * dens, 16f * dens, 16f * dens, paint);
    
    paint.setColor(Color.argb(250, 25, 25, 32));
    canvas.drawRoundRect(panel, 16f * dens, 16f * dens, paint);

    strokePaint.setColor(Color.argb(200, 100, 120, 150));
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(2.5f * dens);
    canvas.drawRoundRect(panel, 16f * dens, 16f * dens, strokePaint);

    strokePaint.setColor(Color.argb(80, 150, 170, 200));
    strokePaint.setStrokeWidth(1f * dens);
    RectF innerPanel = new RectF(panel.left + 2f * dens, panel.top + 2f * dens,
      panel.right - 2f * dens, panel.bottom - 2f * dens);
    canvas.drawRoundRect(innerPanel, 14f * dens, 14f * dens, strokePaint);
    
    
    textPaint.setTextSize(18f * dens);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    String title = shopPanelTarget.name != null ? shopPanelTarget.name : "City Details";
    canvas.drawText(title, panel.centerX(), panelY + 30f * dens, textPaint);
    
    
    float closeBtnSize = 40f * dens;
    RectF closeBtn = new RectF(panel.right - closeBtnSize - 10f * dens, panel.top + 10f * dens,
      panel.right - 10f * dens, panel.top + 10f * dens + closeBtnSize);
    paint.setColor(getResources().getColor(R.color.cst_warning));
    canvas.drawRoundRect(closeBtn, 8f * dens, 8f * dens, paint);
    textPaint.setTextSize(16f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawText("X", closeBtn.centerX(), closeBtn.centerY() + 5f * dens, textPaint);
    
    // /
    textPaint.setTextSize(13f * dens);
    textPaint.setTextAlign(Paint.Align.LEFT);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    String ownerText;
    if (shopPanelTarget.owner == 1) {
      ownerText = "Faction: Player";
    } else if (shopPanelTarget.owner == 0) {
      ownerText = "Faction: Neutral";
    } else {
      ownerText = "Faction: Enemy " + shopPanelTarget.owner;
    }
    canvas.drawText(ownerText, panelX + 20f * dens, panelY + 60f * dens, textPaint);
    

    float troopY = panelY + 80f * dens;
    float unitIconSize = 16f * dens;
    float unitIconGap = 4f * dens;
    float unitStartX = panelX + 20f * dens;
    
    
    if (shopPanelTarget.shield > 0) {
      for (int i = 0; i < Math.min(shopPanelTarget.shield, 10); i++) {
        float x = unitStartX + i * (unitIconSize + unitIconGap);
        drawShieldUnitIcon(canvas, x, troopY, unitIconSize, dens);
      }
      if (shopPanelTarget.shield > 10) {
        textPaint.setTextSize(11f * dens);
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
        canvas.drawText("+" + (shopPanelTarget.shield - 10), 
          unitStartX + 10 * (unitIconSize + unitIconGap), troopY + unitIconSize * 0.3f, textPaint);
      }
    }
    
    
    float swordY = troopY + unitIconSize + 6f * dens;
    if (shopPanelTarget.sword > 0) {
      for (int i = 0; i < Math.min(shopPanelTarget.sword, 10); i++) {
        float x = unitStartX + i * (unitIconSize + unitIconGap);
        drawSwordUnitIcon(canvas, x, swordY, unitIconSize, dens);
      }
      if (shopPanelTarget.sword > 10) {
        textPaint.setTextSize(11f * dens);
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
        canvas.drawText("+" + (shopPanelTarget.sword - 10), 
          unitStartX + 10 * (unitIconSize + unitIconGap), swordY + unitIconSize * 0.3f, textPaint);
      }
    }
    
    
    float archerY = swordY + unitIconSize + 6f * dens;
    if (shopPanelTarget.archer > 0) {
      for (int i = 0; i < Math.min(shopPanelTarget.archer, 10); i++) {
        float x = unitStartX + i * (unitIconSize + unitIconGap);
        drawArcherUnitIcon(canvas, x, archerY, unitIconSize, dens);
      }
      if (shopPanelTarget.archer > 10) {
        textPaint.setTextSize(11f * dens);
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
        canvas.drawText("+" + (shopPanelTarget.archer - 10), 
          unitStartX + 10 * (unitIconSize + unitIconGap), archerY + unitIconSize * 0.3f, textPaint);
      }
    }
    
    
    if (shopPanelTarget.totalGiant() > 0) {
      float giantY = archerY + unitIconSize + 6f * dens;
      for (int i = 0; i < Math.min(shopPanelTarget.totalGiant(), 5); i++) {
        float x = unitStartX + i * (unitIconSize * 1.3f + unitIconGap);
        drawGiantUnitIcon(canvas, x, giantY, unitIconSize * 1.3f, dens);
      }
      if (shopPanelTarget.totalGiant() > 5) {
        textPaint.setTextSize(11f * dens);
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
        canvas.drawText("+" + (shopPanelTarget.totalGiant() - 5), 
          unitStartX + 5 * (unitIconSize * 1.3f + unitIconGap), giantY + unitIconSize * 0.3f, textPaint);
      }
    }

    
    float troopHeight = 0f;
    if (shopPanelTarget.shield > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.sword > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.archer > 0) troopHeight += unitIconSize + 6f * dens;
    if (shopPanelTarget.totalGiant() > 0) troopHeight += unitIconSize * 1.3f + 6f * dens;
    

    float goldY = troopY + troopHeight + 10f * dens;
    if (shopPanelTarget.owner == 1) {
      textPaint.setTextSize(13f * dens);
      textPaint.setTextAlign(Paint.Align.LEFT);
      textPaint.setColor(getResources().getColor(R.color.cst_accent));
      canvas.drawText("Gold: " + engine.getPlayerGold(), panelX + 20f * dens, goldY, textPaint);
    }
    
    // /
    Prefecture attackSource = findNeighboringPlayerPrefecture(shopPanelTarget);
    float btnY = goldY + 30f * dens;
    if (shopPanelTarget.owner != 1 && attackSource != null) {
      float attackBtnH = 55f * dens;
      float attackBtnW = panelW - 40f * dens;
      RectF attackBtn = new RectF(panelX + 20f * dens, btnY, panelX + 20f * dens + attackBtnW, btnY + attackBtnH);
      
      
      paint.setColor(getResources().getColor(R.color.cst_warning));
      canvas.drawRoundRect(attackBtn, 10f * dens, 10f * dens, paint);
      
      
      textPaint.setTextSize(16f * dens);
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      textPaint.setFakeBoldText(true);
      canvas.drawText("Attack", attackBtn.centerX(), attackBtn.centerY() - 5f * dens, textPaint);
      
      // Display source territory name
      if (attackSource.name != null && attackSource.name.length() > 0) {
        textPaint.setTextSize(11f * dens);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(Color.argb(220, 255, 255, 255));
        canvas.drawText("From: " + attackSource.name, attackBtn.centerX(), attackBtn.centerY() + 12f * dens, textPaint);
      }
      
      textPaint.setFakeBoldText(false);
      btnY += attackBtnH + 20f * dens;
    }
    
    
    if (shopPanelTarget.owner != 1) {
      return; // /
    }
    float btnH = 50f * dens;
    float btnSpacing = 8f * dens;
    float btnW = (panelW - 40f * dens) / 2f;
    

    textPaint.setTextSize(12f * dens);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    canvas.drawText("Normal Units", panelX + panelW * 0.5f, btnY - 5f * dens, textPaint);
    
    String[] unitNames = {"Shield", "Sword", "Archer"};
    int[] unitCosts = {50, 40, 45};
    for (int i = 0; i < 3; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = btnY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      boolean canAfford = shopPanelTarget.owner == 1 && engine.getPlayerGold() >= unitCosts[i];
      

      if (canAfford) {

        paint.setColor(getResources().getColor(R.color.cst_accent_2));
        paint.setAlpha(240);
      } else {

        paint.setColor(Color.argb(200, 60, 60, 70));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, paint);
      
      
      strokePaint.setStyle(Paint.Style.STROKE);
      strokePaint.setStrokeWidth(1.5f * dens);
      if (canAfford) {
        strokePaint.setColor(Color.argb(180, 255, 255, 255));
      } else {
        strokePaint.setColor(Color.argb(150, 100, 100, 100));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, strokePaint);
      
      
      textPaint.setTextSize(11f * dens);
      textPaint.setColor(canAfford ? getResources().getColor(R.color.cst_text_primary) : Color.argb(180, 150, 150, 150));
      textPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(unitNames[i], btnRect.centerX(), btnRect.centerY() - 8f * dens, textPaint);
      

      textPaint.setTextSize(10f * dens);
      if (canAfford) {
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
      } else {
        textPaint.setColor(Color.argb(220, 255, 100, 100)); 
      }
      canvas.drawText("$" + unitCosts[i], btnRect.centerX(), btnRect.centerY() + 10f * dens, textPaint);
    }
    
    
    float giantY = btnY + 3 * (btnH + btnSpacing) + 20f * dens;
    textPaint.setTextSize(12f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    canvas.drawText("Giant Units", panelX + panelW * 0.5f, giantY - 5f * dens, textPaint);
    
    int[] giantCosts = {200, 180, 190};
    for (int i = 0; i < 3; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = giantY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      boolean canAfford = shopPanelTarget.owner == 1 && engine.getPlayerGold() >= giantCosts[i];
      

      if (canAfford) {
        paint.setColor(getResources().getColor(R.color.cst_accent));
        paint.setAlpha(240);
      } else {
        paint.setColor(Color.argb(200, 60, 60, 70));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, paint);
      
      
      strokePaint.setStyle(Paint.Style.STROKE);
      strokePaint.setStrokeWidth(1.5f * dens);
      if (canAfford) {
        strokePaint.setColor(Color.argb(180, 255, 255, 255));
      } else {
        strokePaint.setColor(Color.argb(150, 100, 100, 100));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, strokePaint);
      
      
      textPaint.setTextSize(11f * dens);
      textPaint.setColor(canAfford ? getResources().getColor(R.color.cst_text_primary) : Color.argb(180, 150, 150, 150));
      textPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText("Giant " + unitNames[i], btnRect.centerX(), btnRect.centerY() - 8f * dens, textPaint);
      

      textPaint.setTextSize(10f * dens);
      if (canAfford) {
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
      } else {
        textPaint.setColor(Color.argb(220, 255, 100, 100)); 
      }
      canvas.drawText("$" + giantCosts[i], btnRect.centerX(), btnRect.centerY() + 10f * dens, textPaint);
    }
    
    
    float buildY = giantY + 3 * (btnH + btnSpacing) + 20f * dens;
    textPaint.setTextSize(12f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    canvas.drawText("Buildings", panelX + panelW * 0.5f, buildY - 5f * dens, textPaint);
    
    String[] buildingNames = {"Cannon", "Barracks", "Fortress", "Training"};
    int buildCost = 300;
    boolean canAffordBuild = shopPanelTarget.owner == 1 && engine.getPlayerGold() >= buildCost;
    for (int i = 0; i < 4; i++) {
      float btnX = panelX + 20f * dens + (i % 2) * (btnW + btnSpacing);
      float btnYPos = buildY + (i / 2) * (btnH + btnSpacing);
      RectF btnRect = new RectF(btnX, btnYPos, btnX + btnW, btnYPos + btnH);
      

      if (canAffordBuild) {
        paint.setColor(getResources().getColor(R.color.cst_warning));
        paint.setAlpha(240);
      } else {
        paint.setColor(Color.argb(200, 60, 60, 70));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, paint);
      
      
      strokePaint.setStyle(Paint.Style.STROKE);
      strokePaint.setStrokeWidth(1.5f * dens);
      if (canAffordBuild) {
        strokePaint.setColor(Color.argb(180, 255, 255, 255));
      } else {
        strokePaint.setColor(Color.argb(150, 100, 100, 100));
      }
      canvas.drawRoundRect(btnRect, 10f * dens, 10f * dens, strokePaint);
      
      
      textPaint.setTextSize(11f * dens);
      textPaint.setColor(canAffordBuild ? getResources().getColor(R.color.cst_text_primary) : Color.argb(180, 150, 150, 150));
      textPaint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(buildingNames[i], btnRect.centerX(), btnRect.centerY() - 8f * dens, textPaint);
      

      textPaint.setTextSize(10f * dens);
      if (canAffordBuild) {
        textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
      } else {
        textPaint.setColor(Color.argb(220, 255, 100, 100)); 
      }
      canvas.drawText("$" + buildCost, btnRect.centerX(), btnRect.centerY() + 10f * dens, textPaint);
    }

    // placedSkill
    float skillY = buildY + 3 * (btnH + btnSpacing) + 24f * dens;
    textPaint.setTextSize(12f * dens);
    textPaint.setColor(getResources().getColor(R.color.cst_text_secondary));
    textPaint.setTextAlign(Paint.Align.LEFT);
    canvas.drawText("Install Skill", panelX + 20f * dens, skillY, textPaint);
    skillY += 18f * dens;

    float skillSize = 30f * dens;
    float skillGap = 12f * dens;
    float sx = panelX + 20f * dens;
    int[] skills = {
      Prefecture.SKILL_CHARGE,
      Prefecture.SKILL_ARROW_RAIN,
      Prefecture.SKILL_DEFENSE,
      Prefecture.SKILL_RETREAT
    };
    String[] skillLabels = {"C", "A", "D", "R"};
    for (int i = 0; i < skills.length; i++) {
      RectF r = new RectF(sx + i * (skillSize + skillGap), skillY,
        sx + i * (skillSize + skillGap) + skillSize, skillY + skillSize);
      boolean active = shopPanelTarget.placedSkill == skills[i];
      paint.setColor(active ? getResources().getColor(R.color.cst_warning) :
        Color.argb(200, 80, 80, 90));
      canvas.drawRoundRect(r, 8f * dens, 8f * dens, paint);
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setTextSize(14f * dens);
      textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
      canvas.drawText(skillLabels[i], r.centerX(), r.centerY() + 5f * dens, textPaint);
    }
  }
  
  public void startSkillPlacement(int skillType) {
    skillPlacementMode = true;
    selectedSkillToPlace = skillType;
  }
  
  private void drawSkillPlacementUI(Canvas canvas) {
    float dens = getResources().getDisplayMetrics().density;
    float w = getWidth();
    float h = getHeight();
    
    
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.argb(100, 0, 0, 0));
    canvas.drawRect(0, 0, w, h, paint);
    
    
    textPaint.setTextSize(20f * dens);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    String skillName = "";
    switch (selectedSkillToPlace) {
      case Prefecture.SKILL_CHARGE: skillName = "Charge"; break;
      case Prefecture.SKILL_ARROW_RAIN: skillName = "Arrow Rain"; break;
      case Prefecture.SKILL_DEFENSE: skillName = "Defense"; break;
      case Prefecture.SKILL_RETREAT: skillName = "Retreat"; break;
    }
    canvas.drawText("Tap player territory to place skill: " + skillName, w * 0.5f, h * 0.1f, textPaint);
    canvas.drawText("Tap empty area to cancel", w * 0.5f, h * 0.15f, textPaint);
    
    
    List<Prefecture> prefectures = engine.getPrefectures();
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (pref.owner == 1) {
        strokePaint.setColor(getResources().getColor(R.color.cst_accent));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f * dens);
        strokePaint.setAlpha(200);
        canvas.drawCircle(pref.x, pref.y, pref.radius + 8f * dens, strokePaint);
      }
    }
  }
  

  private Prefecture findNeighboringPlayerPrefecture(Prefecture target) {
    if (target == null) {
      return null;
    }
    List<Prefecture> prefectures = engine.getPrefectures();
    if (prefectures == null) {
      return null;
    }
    
    // neighbors
    if (target.neighbors != null) {
      for (int i = 0; i < target.neighbors.length; i++) {
        int neighborId = target.neighbors[i];
        if (neighborId >= 0 && neighborId < prefectures.size()) {
          Prefecture neighbor = prefectures.get(neighborId);
          if (neighbor != null && neighbor.owner == 1 && neighbor.total() > 0) {
            return neighbor;
          }
        }
      }
    }
    
    // neighbors
    Prefecture best = null;
    float bestDist = Float.MAX_VALUE;
    for (int i = 0; i < prefectures.size(); i++) {
      Prefecture pref = prefectures.get(i);
      if (pref == null || pref.owner != 1 || pref.total() <= 0) {
        continue;
      }
      float dx = pref.x - target.x;
      float dy = pref.y - target.y;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      float maxDist = (pref.radius + target.radius) * 1.5f; 
      if (dist < maxDist && dist < bestDist) {
        bestDist = dist;
        best = pref;
      }
    }
    return best;
  }
  
  
  private void drawShieldUnitIcon(Canvas canvas, float x, float y, float size, float dens) {

    paint.setColor(getResources().getColor(R.color.cst_accent));
    paint.setAlpha(220);
    canvas.drawCircle(x, y, size * 0.4f, paint);

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.argb(200, 200, 200, 255));
    cachedRectF1.set(x - size * 0.35f, y - size * 0.5f, x + size * 0.35f, y + size * 0.5f);
    canvas.drawRoundRect(cachedRectF1, size * 0.2f, size * 0.2f, paint);
    
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.5f * dens);
    paint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawRoundRect(cachedRectF1, size * 0.2f, size * 0.2f, paint);
    paint.setStyle(Paint.Style.FILL);
  }
  
  
  private void drawSwordUnitIcon(Canvas canvas, float x, float y, float size, float dens) {

    paint.setColor(getResources().getColor(R.color.cst_warning));
    paint.setAlpha(220);
    canvas.drawCircle(x, y, size * 0.4f, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2.5f * dens);
    paint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawLine(x, y - size * 0.5f, x, y + size * 0.5f, paint);

    canvas.drawLine(x - size * 0.15f, y + size * 0.4f, x + size * 0.15f, y + size * 0.4f, paint);
    paint.setStyle(Paint.Style.FILL);
  }
  
  
  private void drawArcherUnitIcon(Canvas canvas, float x, float y, float size, float dens) {

    paint.setColor(getResources().getColor(R.color.cst_accent_2));
    paint.setAlpha(220);
    canvas.drawCircle(x, y, size * 0.4f, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2.5f * dens);
    paint.setColor(getResources().getColor(R.color.cst_text_primary));
    cachedRectF2.set(x - size * 0.4f, y - size * 0.4f, x + size * 0.4f, y + size * 0.4f);
    canvas.drawArc(cachedRectF2, 30f, 120f, false, paint);

    float stringY1 = y - size * 0.25f;
    float stringY2 = y + size * 0.25f;
    canvas.drawLine(x - size * 0.2f, stringY1, x - size * 0.2f, stringY2, paint);
    paint.setStyle(Paint.Style.FILL);
  }
  
  
  private void drawGiantUnitIcon(Canvas canvas, float x, float y, float size, float dens) {

    paint.setColor(getResources().getColor(R.color.cst_accent_2));
    paint.setAlpha(240);
    canvas.drawCircle(x, y, size * 0.5f, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(3f * dens);
    paint.setColor(getResources().getColor(R.color.cst_warning));
    canvas.drawCircle(x, y, size * 0.6f, paint);

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(getResources().getColor(R.color.cst_warning));
    textPaint.setTextSize(size * 0.6f);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setColor(getResources().getColor(R.color.cst_text_primary));
    canvas.drawText("*", x, y + size * 0.2f, textPaint);
    paint.setStyle(Paint.Style.FILL);
  }
}
