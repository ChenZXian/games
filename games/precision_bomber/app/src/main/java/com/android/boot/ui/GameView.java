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
  private final Paint paintEnemy;
  private final Paint paintEnemyElite;
  private final Paint paintBomb;
  private final Paint paintExplosion;
  private final Paint paintPickup;
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
    paintPlayer = new Paint();
    paintEnemy = new Paint();
    paintEnemyElite = new Paint();
    paintBomb = new Paint();
    paintExplosion = new Paint();
    paintPickup = new Paint();
    paintFloor.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_floor));
    paintSolid.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_solid));
    paintSoft.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_soft));
    paintExit.setColor(getResources().getColor(com.android.boot.R.color.cst_tile_exit));
    paintPlayer.setColor(getResources().getColor(com.android.boot.R.color.cst_player));
    paintEnemy.setColor(getResources().getColor(com.android.boot.R.color.cst_enemy));
    paintEnemyElite.setColor(getResources().getColor(com.android.boot.R.color.cst_enemy_elite));
    paintBomb.setColor(getResources().getColor(com.android.boot.R.color.cst_bomb));
    paintExplosion.setColor(getResources().getColor(com.android.boot.R.color.cst_explosion));
    paintPickup.setColor(getResources().getColor(com.android.boot.R.color.cst_pickup));
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
          canvas.drawRect(left, top, right, bottom, paintSolid);
        } else if (type == TileType.SOFT || type == TileType.EXIT_HIDDEN) {
          canvas.drawRect(left, top, right, bottom, paintSoft);
        } else if (type == TileType.EXIT_REVEALED) {
          canvas.drawRect(left, top, right, bottom, paintExit);
        } else {
          canvas.drawRect(left, top, right, bottom, paintFloor);
        }
      }
    }
    List<Pickup> pickups = engine.getPickups();
    for (Pickup pickup : pickups) {
      float px = offsetX + pickup.x * tileSize + tileSize * 0.25f;
      float py = offsetY + pickup.y * tileSize + tileSize * 0.25f;
      canvas.drawRect(px, py, px + tileSize * 0.5f, py + tileSize * 0.5f, paintPickup);
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
    float pcx = offsetX + player.posX * tileSize + tileSize * 0.5f;
    float pcy = offsetY + player.posY * tileSize + tileSize * 0.5f;
    canvas.drawCircle(pcx, pcy, tileSize * 0.35f, paintPlayer);
    List<Enemy> enemies = engine.getEnemies();
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      float ex = offsetX + enemy.posX * tileSize + tileSize * 0.15f;
      float ey = offsetY + enemy.posY * tileSize + tileSize * 0.15f;
      Paint p = enemy instanceof com.android.boot.entity.SpawnerEnemy ? paintEnemyElite : paintEnemy;
      canvas.drawRect(ex, ey, ex + tileSize * 0.7f, ey + tileSize * 0.7f, p);
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
