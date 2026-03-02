package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.core.StatsSnapshot;
import com.android.boot.entity.Bomb;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.Player;
import com.android.boot.fx.Explosion;
import com.android.boot.grid.Grid;
import com.android.boot.grid.TileType;

import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  public interface GameUiListener {
    void onStateChanged(GameState state);
    void onHudUpdate(StatsSnapshot stats, boolean remoteAvailable);
  }

  private final GameEngine engine;
  private final Paint paintFloor;
  private final Paint paintSolid;
  private final Paint paintSoft;
  private final Paint paintExit;
  private final Paint paintPlayer;
  private final Paint paintPlayerOutline;
  private final Paint paintPlayerFace;
  private final Paint paintPlayerEye;
  private final Paint paintPlayerShadow;
  private final Paint paintEnemy;
  private final Paint paintEnemyOutline;
  private final Paint paintEnemyElite;
  private final Paint paintBomb;
  private final Paint paintExplosion;
  private final Paint paintGridLines;
  private final Paint paintBlockedOverlay;
  private final Paint paintPickupBomb;
  private final Paint paintPickupSpeed;
  private final Paint paintPickupFuse;
  private final Paint paintPickupShield;
  private final Paint paintPickupRemote;
  private GameLoopThread loopThread;
  private GameUiListener uiListener;
  private GameState lastState;
  private float hudTimer;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    engine = new GameEngine(13, 11);
    paintFloor = new Paint();
    paintSolid = new Paint();
    paintSoft = new Paint();
    paintExit = new Paint();
    paintPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerFace = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerEye = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemy = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyElite = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBomb = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintExplosion = new Paint();
    paintGridLines = new Paint();
    paintBlockedOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPickupBomb = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPickupSpeed = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPickupFuse = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPickupShield = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPickupRemote = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintFloor.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_floor));
    paintSolid.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_solid));
    paintSoft.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_soft));
    paintExit.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_exit));
    paintPlayer.setColor(getResources().getColor(com.android.boot.R.color.cst_player));
    paintPlayerOutline.setColor(getResources().getColor(com.android.boot.R.color.cst_text_on_secondary));
    paintPlayerOutline.setStyle(Paint.Style.STROKE);
    paintPlayerOutline.setStrokeWidth(3f);
    paintPlayerFace.setColor(0xCCFFFFFF);
    paintPlayerEye.setColor(0xFF111111);
    paintPlayerShadow.setColor(0x55000000);
    paintEnemy.setColor(getResources().getColor(com.android.boot.R.color.cst_enemy));
    paintEnemyOutline.setColor(getResources().getColor(com.android.boot.R.color.cst_text_on_secondary));
    paintEnemyOutline.setStyle(Paint.Style.STROKE);
    paintEnemyOutline.setStrokeWidth(3f);
    paintEnemyElite.setColor(getResources().getColor(com.android.boot.R.color.cst_enemy_elite));
    paintBomb.setColor(getResources().getColor(com.android.boot.R.color.cst_bomb));
    paintExplosion.setColor(getResources().getColor(com.android.boot.R.color.cst_explosion));
    paintGridLines.setColor(0x33FFFFFF);
    paintGridLines.setStyle(Paint.Style.STROKE);
    paintGridLines.setStrokeWidth(1f);
    paintBlockedOverlay.setColor(0x22FF4D6D);
    paintBlockedOverlay.setStyle(Paint.Style.STROKE);
    paintBlockedOverlay.setStrokeWidth(1.5f);
    paintPickupBomb.setColor(getResources().getColor(com.android.boot.R.color.cst_pickup));
    paintPickupSpeed.setColor(getResources().getColor(com.android.boot.R.color.cst_accent));
    paintPickupFuse.setColor(getResources().getColor(com.android.boot.R.color.cst_accent_2));
    paintPickupShield.setColor(getResources().getColor(com.android.boot.R.color.cst_meter_fill));
    paintPickupRemote.setColor(getResources().getColor(com.android.boot.R.color.cst_text_on_secondary));
    lastState = engine.getState();
  }

  public void setUiListener(GameUiListener listener) {
    this.uiListener = listener;
  }

  public GameEngine getEngine() {
    return engine;
  }

  public void startGame() {
    engine.startGame();
  }

  public void restartStage() {
    engine.restartStage();
  }

  public void nextStage() {
    engine.nextStage();
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

  public void setInputDirection(com.android.boot.core.Direction direction) {
    engine.setInputDirection(direction);
  }

  public void pressBomb() {
    engine.pressBomb();
  }

  public void pressRemote() {
    engine.pressRemote();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    loopThread = new GameLoopThread(holder);
    loopThread.running = true;
    loopThread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (loopThread != null) {
      loopThread.running = false;
      boolean retry = true;
      while (retry) {
        try {
          loopThread.join();
          retry = false;
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  private void update(float dt) {
    engine.update(dt);
    GameState current = engine.getState();
    if (current != lastState) {
      lastState = current;
      if (uiListener != null) {
        post(() -> uiListener.onStateChanged(current));
      }
    }
    hudTimer += dt;
    if (hudTimer >= 0.2f) {
      hudTimer = 0f;
      if (uiListener != null) {
        StatsSnapshot stats = engine.getStatsSnapshot();
        boolean remoteAvailable = engine.getPlayer().remoteDetonator;
        post(() -> uiListener.onHudUpdate(stats, remoteAvailable));
      }
    }
  }

  private void render(Canvas canvas) {
    if (canvas == null) {
      return;
    }
    canvas.drawColor(getResources().getColor(com.android.boot.R.color.cst_bg_main));
    Grid grid = engine.getGrid();
    int width = canvas.getWidth();
    int height = canvas.getHeight();
    float tileSize = Math.min(width / (float) grid.getWidth(), height / (float) grid.getHeight());
    float offsetX = (width - tileSize * grid.getWidth()) * 0.5f;
    float offsetY = (height - tileSize * grid.getHeight()) * 0.5f;
    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        float left = offsetX + x * tileSize;
        float top = offsetY + y * tileSize;
        float right = left + tileSize;
        float bottom = top + tileSize;
        TileType type = grid.get(x, y);
        if (type == TileType.SOLID) {
          // Solid wall: full dark block
          canvas.drawRect(left, top, right, bottom, paintSolid);
        } else if (type == TileType.SOFT || type == TileType.EXIT_HIDDEN) {
          // Destructible block: floor background + smaller card on top
          canvas.drawRect(left, top, right, bottom, paintFloor);
          float inset = tileSize * 0.14f;
          canvas.drawRoundRect(left + inset, top + inset, right - inset, bottom - inset,
            tileSize * 0.12f, tileSize * 0.12f, paintSoft);
        } else if (type == TileType.EXIT_REVEALED) {
          canvas.drawRect(left, top, right, bottom, paintExit);
        } else {
          // FLOOR: pure walkable floor
          canvas.drawRect(left, top, right, bottom, paintFloor);
        }
      }
    }
    // Thin grid lines: help player see each tile clearly
    for (int x = 0; x <= grid.getWidth(); x++) {
      float gx = offsetX + x * tileSize;
      canvas.drawLine(gx, offsetY, gx, offsetY + grid.getHeight() * tileSize, paintGridLines);
    }
    for (int y = 0; y <= grid.getHeight(); y++) {
      float gy = offsetY + y * tileSize;
      canvas.drawLine(offsetX, gy, offsetX + grid.getWidth() * tileSize, gy, paintGridLines);
    }

    // Draw a subtle red X on every non-walkable tile
    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        if (!grid.isWalkable(x, y)) {
          float left = offsetX + x * tileSize;
          float top = offsetY + y * tileSize;
          float right = left + tileSize;
          float bottom = top + tileSize;
          float inset = tileSize * 0.18f;
          float ix1 = left + inset;
          float iy1 = top + inset;
          float ix2 = right - inset;
          float iy2 = bottom - inset;
          canvas.drawLine(ix1, iy1, ix2, iy2, paintBlockedOverlay);
          canvas.drawLine(ix1, iy2, ix2, iy1, paintBlockedOverlay);
        }
      }
    }
    List<Pickup> pickups = engine.getPickups();
    for (Pickup pickup : pickups) {
      float cx = offsetX + (pickup.x + 0.5f) * tileSize;
      float cy = offsetY + (pickup.y + 0.5f) * tileSize;
      float r = tileSize * 0.24f;
      switch (pickup.type) {
        case BOMB_PLUS: {
          // Extra bomb capacity: plus-shaped icon
          Paint p = paintPickupBomb;
          float bar = tileSize * 0.10f;
          canvas.drawRoundRect(cx - bar, cy - r, cx + bar, cy + r, bar, bar, p);
          canvas.drawRoundRect(cx - r, cy - bar, cx + r, cy + bar, bar, bar, p);
          break;
        }
        case SPEED_PLUS: {
          // Speed up: diamond arrow
          Paint p = paintPickupSpeed;
          android.graphics.Path path = new android.graphics.Path();
          path.moveTo(cx + r, cy);
          path.lineTo(cx, cy - r);
          path.lineTo(cx - r * 0.5f, cy);
          path.lineTo(cx, cy + r);
          path.close();
          canvas.drawPath(path, p);
          break;
        }
        case FUSE_MINUS: {
          // Shorter fuse: hourglass
          Paint p = paintPickupFuse;
          float topY = cy - r;
          float botY = cy + r;
          float leftX = cx - r * 0.7f;
          float rightX = cx + r * 0.7f;
          canvas.drawLine(leftX, topY, rightX, botY, p);
          canvas.drawLine(rightX, topY, leftX, botY, p);
          canvas.drawLine(leftX, topY, rightX, topY, p);
          canvas.drawLine(leftX, botY, rightX, botY, p);
          break;
        }
        case SHIELD: {
          // Shield: double ring
          Paint p = paintPickupShield;
          Paint stroke = new Paint(p);
          stroke.setStyle(Paint.Style.STROKE);
          stroke.setStrokeWidth(tileSize * 0.06f);
          canvas.drawCircle(cx, cy, r * 0.9f, stroke);
          canvas.drawCircle(cx, cy, r * 0.45f, p);
          break;
        }
        case REMOTE:
        default: {
          // Remote trigger: bullseye
          Paint p = paintPickupRemote;
          Paint stroke = new Paint(p);
          stroke.setStyle(Paint.Style.STROKE);
          stroke.setStrokeWidth(tileSize * 0.05f);
          canvas.drawCircle(cx, cy, r, stroke);
          canvas.drawCircle(cx, cy, r * 0.45f, p);
          break;
        }
      }
    }
    List<Bomb> bombs = engine.getBombs();
    for (Bomb bomb : bombs) {
      float cx = offsetX + bomb.x * tileSize + tileSize * 0.5f;
      float cy = offsetY + bomb.y * tileSize + tileSize * 0.5f;
      canvas.drawCircle(cx, cy, tileSize * 0.3f, paintBomb);
    }
    List<Explosion> explosions = engine.getExplosions();
    for (Explosion explosion : explosions) {
      float left = offsetX + explosion.x * tileSize;
      float top = offsetY + explosion.y * tileSize;
      canvas.drawRect(left, top, left + tileSize, top + tileSize, paintExplosion);
    }
    Player player = engine.getPlayer();
    float pcx = offsetX + (player.posX + 0.5f) * tileSize;
    float pcy = offsetY + (player.posY + 0.5f) * tileSize;
    float bodyHalfW = tileSize * 0.28f;
    float bodyHalfH = tileSize * 0.34f;

    // Elliptical shadow under the player for depth
    float shadowH = tileSize * 0.10f;
    canvas.drawOval(pcx - bodyHalfW, pcy + bodyHalfH * 0.7f,
      pcx + bodyHalfW, pcy + bodyHalfH * 0.7f + shadowH, paintPlayerShadow);

    // Body: vertical capsule
    canvas.drawRoundRect(pcx - bodyHalfW, pcy - bodyHalfH,
      pcx + bodyHalfW, pcy + bodyHalfH,
      tileSize * 0.25f, tileSize * 0.25f, paintPlayer);
    canvas.drawRoundRect(pcx - bodyHalfW, pcy - bodyHalfH,
      pcx + bodyHalfW, pcy + bodyHalfH,
      tileSize * 0.25f, tileSize * 0.25f, paintPlayerOutline);

    // Helmet / head
    float headR = tileSize * 0.26f;
    float headCx = pcx;
    float headCy = pcy - bodyHalfH * 0.45f;
    canvas.drawCircle(headCx, headCy, headR, paintPlayerFace);
    canvas.drawCircle(headCx, headCy, headR, paintPlayerOutline);

    // Eyes shift slightly based on facing
    float eyeBaseCx = headCx;
    float eyeBaseCy = headCy;
    switch (player.facing) {
      case LEFT:
        eyeBaseCx -= tileSize * 0.04f;
        break;
      case RIGHT:
        eyeBaseCx += tileSize * 0.04f;
        break;
      case UP:
        eyeBaseCy -= tileSize * 0.03f;
        break;
      case DOWN:
      default:
        eyeBaseCy += tileSize * 0.01f;
        break;
    }
    float eyeOffsetX = tileSize * 0.06f;
    float eyeOffsetY = tileSize * 0.02f;
    float eyeR = tileSize * 0.05f;
    canvas.drawCircle(eyeBaseCx - eyeOffsetX, eyeBaseCy - eyeOffsetY, eyeR, paintPlayerEye);
    canvas.drawCircle(eyeBaseCx + eyeOffsetX, eyeBaseCy - eyeOffsetY, eyeR, paintPlayerEye);

    // Direction arrow: triangle above the head
    float arrowY = headCy - headR - tileSize * 0.02f;
    float arrowSize = tileSize * 0.10f;
    float ax1, ay1, ax2, ay2, ax3, ay3;
    switch (player.facing) {
      case LEFT:
        ax1 = headCx - headR - arrowSize * 0.6f;
        ay1 = headCy;
        ax2 = ax1 + arrowSize;
        ay2 = headCy - arrowSize * 0.6f;
        ax3 = ax1 + arrowSize;
        ay3 = headCy + arrowSize * 0.6f;
        break;
      case RIGHT:
        ax1 = headCx + headR + arrowSize * 0.6f;
        ay1 = headCy;
        ax2 = ax1 - arrowSize;
        ay2 = headCy - arrowSize * 0.6f;
        ax3 = ax1 - arrowSize;
        ay3 = headCy + arrowSize * 0.6f;
        break;
      case UP:
        ax1 = headCx;
        ay1 = arrowY - arrowSize * 0.4f;
        ax2 = headCx - arrowSize * 0.6f;
        ay2 = arrowY + arrowSize * 0.6f;
        ax3 = headCx + arrowSize * 0.6f;
        ay3 = arrowY + arrowSize * 0.6f;
        break;
      case DOWN:
      default:
        ax1 = headCx;
        ay1 = headCy + headR + arrowSize * 0.6f;
        ax2 = headCx - arrowSize * 0.6f;
        ay2 = headCy + headR - arrowSize * 0.4f;
        ax3 = headCx + arrowSize * 0.6f;
        ay3 = headCy + headR - arrowSize * 0.4f;
        break;
    }
    android.graphics.Path arrowPath = new android.graphics.Path();
    arrowPath.moveTo(ax1, ay1);
    arrowPath.lineTo(ax2, ay2);
    arrowPath.lineTo(ax3, ay3);
    arrowPath.close();
    canvas.drawPath(arrowPath, paintPlayer);
    List<Enemy> enemies = engine.getEnemies();
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      float ex = offsetX + (enemy.posX + 0.5f) * tileSize;
      float ey = offsetY + (enemy.posY + 0.5f) * tileSize;
      Paint bodyPaint = enemy instanceof com.android.boot.entity.SpawnerEnemy ? paintEnemyElite : paintEnemy;
      float half = tileSize * 0.32f;
      // Enemy body: rounded rectangle + outline
      canvas.drawRoundRect(ex - half, ey - half, ex + half, ey + half,
        tileSize * 0.16f, tileSize * 0.16f, bodyPaint);
      canvas.drawRoundRect(ex - half, ey - half, ex + half, ey + half,
        tileSize * 0.16f, tileSize * 0.16f, paintEnemyOutline);
      // Two eyes to make enemies more cartoony
      float er = tileSize * 0.06f;
      canvas.drawCircle(ex - half * 0.4f, ey - half * 0.1f, er, paintPlayerEye);
      canvas.drawCircle(ex + half * 0.4f, ey - half * 0.1f, er, paintPlayerEye);
    }
  }

  private class GameLoopThread extends Thread {
    private final SurfaceHolder holder;
    public boolean running;

    GameLoopThread(SurfaceHolder holder) {
      this.holder = holder;
    }

    @Override
    public void run() {
      long lastTime = System.nanoTime();
      float accumulator = 0f;
      float step = 1f / 60f;
      while (running) {
        long now = System.nanoTime();
        float delta = (now - lastTime) / 1000000000f;
        lastTime = now;
        if (delta > 0.25f) {
          delta = 0.25f;
        }
        accumulator += delta;
        while (accumulator >= step) {
          update(step);
          accumulator -= step;
        }
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
          render(canvas);
          holder.unlockCanvasAndPost(canvas);
        }
      }
    }
  }
}
