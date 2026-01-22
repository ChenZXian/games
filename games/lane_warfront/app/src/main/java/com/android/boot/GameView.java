package com.android.boot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  public enum State {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
  }

  public static final int UNIT_INFANTRY = 0;
  public static final int UNIT_RANGER = 1;
  public static final int UNIT_TANK = 2;

  private final ArrayList<Unit> playerUnits = new ArrayList<>();
  private final ArrayList<Unit> enemyUnits = new ArrayList<>();
  private Thread thread;
  private boolean running = false;
  private long lastTime = 0L;
  private State state = State.MENU;

  private float width;
  private float height;
  private float laneY;
  private float laneHeight;
  private float playerBaseX;
  private float enemyBaseX;
  private float baseWidth;
  private float baseHeight;

  private float playerBaseHp;
  private float enemyBaseHp;
  private float playerBaseMaxHp = 1000f;
  private float enemyBaseMaxHp = 1000f;
  private float energy = 0f;
  private float energyRate = 10f;
  private float energyMax = 100f;

  private float enemySpawnTimer = 0f;
  private float elapsedTime = 0f;
  private int enemySpawnIndex = 0;
  private boolean playerWon = false;

  private Paint paintBg;
  private Paint paintLane;
  private Paint paintPlayer;
  private Paint paintEnemy;
  private Paint paintPlayerBase;
  private Paint paintEnemyBase;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    initPaints();
    resetGame();
  }

  private void initPaints() {
    paintBg = new Paint();
    paintBg.setColor(getContext().getColor(R.color.cst_bg_main));
    paintLane = new Paint();
    paintLane.setColor(getContext().getColor(R.color.cst_lane));
    paintPlayer = new Paint();
    paintPlayer.setColor(getContext().getColor(R.color.cst_unit_player));
    paintEnemy = new Paint();
    paintEnemy.setColor(getContext().getColor(R.color.cst_unit_enemy));
    paintPlayerBase = new Paint();
    paintPlayerBase.setColor(getContext().getColor(R.color.cst_base_player));
    paintEnemyBase = new Paint();
    paintEnemyBase.setColor(getContext().getColor(R.color.cst_base_enemy));
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    startLoop();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    this.width = width;
    this.height = height;
    laneY = height * 0.6f;
    laneHeight = height * 0.18f;
    baseWidth = width * 0.12f;
    baseHeight = height * 0.32f;
    float margin = width * 0.06f;
    playerBaseX = margin + baseWidth * 0.5f;
    enemyBaseX = width - margin - baseWidth * 0.5f;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopLoop();
  }

  @Override
  public void run() {
    while (running) {
      long now = System.nanoTime();
      if (lastTime == 0L) {
        lastTime = now;
      }
      float dt = (now - lastTime) / 1000000000f;
      if (dt > 0.05f) {
        dt = 0.05f;
      }
      lastTime = now;
      update(dt);
      drawFrame();
    }
  }

  private void update(float dt) {
    if (state != State.PLAYING) {
      return;
    }
    elapsedTime += dt;
    energy += energyRate * dt;
    if (energy > energyMax) {
      energy = energyMax;
    }
    enemySpawnTimer -= dt;
    if (enemySpawnTimer <= 0f) {
      spawnEnemyUnit(enemySpawnIndex % 3);
      enemySpawnIndex += 1;
      float interval = 3.5f - elapsedTime * 0.02f;
      if (interval < 1.2f) {
        interval = 1.2f;
      }
      enemySpawnTimer = interval;
    }
    updateUnits(playerUnits, enemyUnits, true, dt);
    updateUnits(enemyUnits, playerUnits, false, dt);
    if (playerBaseHp <= 0f || enemyBaseHp <= 0f) {
      playerWon = enemyBaseHp <= 0f;
      state = State.GAME_OVER;
    }
  }

  private void updateUnits(ArrayList<Unit> owners, ArrayList<Unit> opponents, boolean playerSide, float dt) {
    int i = owners.size() - 1;
    while (i >= 0) {
      Unit unit = owners.get(i);
      if (unit.hp <= 0f) {
        owners.remove(i);
        i -= 1;
        continue;
      }
      unit.cooldown -= dt;
      Unit target = findTarget(unit, opponents, playerSide);
      boolean attacked = false;
      if (target != null) {
        if (unit.cooldown <= 0f) {
          target.hp -= unit.damage;
          unit.cooldown = unit.cooldownTime;
        }
        attacked = true;
      }
      if (!attacked) {
        if (playerSide) {
          if (unit.x + unit.range >= enemyBaseX - baseWidth * 0.5f) {
            if (unit.cooldown <= 0f) {
              enemyBaseHp -= unit.damage;
              unit.cooldown = unit.cooldownTime;
            }
            attacked = true;
          }
        } else {
          if (unit.x - unit.range <= playerBaseX + baseWidth * 0.5f) {
            if (unit.cooldown <= 0f) {
              playerBaseHp -= unit.damage;
              unit.cooldown = unit.cooldownTime;
            }
            attacked = true;
          }
        }
      }
      if (!attacked) {
        float dir = playerSide ? 1f : -1f;
        unit.x += unit.speed * dir * dt;
      }
      i -= 1;
    }
  }

  private Unit findTarget(Unit unit, ArrayList<Unit> opponents, boolean playerSide) {
    Unit nearest = null;
    float nearestDist = 0f;
    int count = opponents.size();
    for (int i = 0; i < count; i += 1) {
      Unit other = opponents.get(i);
      float dist = Math.abs(other.x - unit.x);
      if (dist <= unit.range) {
        if (nearest == null || dist < nearestDist) {
          nearest = other;
          nearestDist = dist;
        }
      }
    }
    return nearest;
  }

  private void drawFrame() {
    SurfaceHolder holder = getHolder();
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    canvas.drawRect(0f, 0f, width, height, paintBg);
    float laneTop = laneY - laneHeight * 0.5f;
    float laneBottom = laneY + laneHeight * 0.5f;
    canvas.drawRect(0f, laneTop, width, laneBottom, paintLane);
    float baseTop = laneY - baseHeight * 0.5f;
    float baseBottom = laneY + baseHeight * 0.5f;
    canvas.drawRect(playerBaseX - baseWidth * 0.5f, baseTop, playerBaseX + baseWidth * 0.5f, baseBottom, paintPlayerBase);
    canvas.drawRect(enemyBaseX - baseWidth * 0.5f, baseTop, enemyBaseX + baseWidth * 0.5f, baseBottom, paintEnemyBase);
    int i = 0;
    int count = playerUnits.size();
    while (i < count) {
      Unit unit = playerUnits.get(i);
      canvas.drawCircle(unit.x, laneY, unit.radius, paintPlayer);
      i += 1;
    }
    i = 0;
    count = enemyUnits.size();
    while (i < count) {
      Unit unit = enemyUnits.get(i);
      canvas.drawCircle(unit.x, laneY, unit.radius, paintEnemy);
      i += 1;
    }
    holder.unlockCanvasAndPost(canvas);
  }

  private void resetGame() {
    playerUnits.clear();
    enemyUnits.clear();
    playerBaseHp = playerBaseMaxHp;
    enemyBaseHp = enemyBaseMaxHp;
    energy = 40f;
    elapsedTime = 0f;
    enemySpawnTimer = 1.5f;
    enemySpawnIndex = 0;
    playerWon = false;
  }

  public void startNewGame() {
    resetGame();
    state = State.PLAYING;
  }

  public void goToMenu() {
    resetGame();
    state = State.MENU;
  }

  public void setState(State newState) {
    if (newState == State.PLAYING && state == State.MENU) {
      state = State.MENU;
    }
    if (newState == State.PLAYING && state == State.GAME_OVER) {
      state = State.GAME_OVER;
    }
    if (newState == State.PLAYING && state == State.PAUSED) {
      state = State.PLAYING;
      return;
    }
    if (newState == State.PAUSED && state == State.PLAYING) {
      state = State.PAUSED;
      return;
    }
    if (newState == State.MENU) {
      state = State.MENU;
    }
    if (newState == State.GAME_OVER) {
      state = State.GAME_OVER;
    }
  }

  public State getState() {
    return state;
  }

  public boolean isPlayerWon() {
    return playerWon;
  }

  public float getEnergy() {
    return energy;
  }

  public float getPlayerBaseHp() {
    return playerBaseHp;
  }

  public float getEnemyBaseHp() {
    return enemyBaseHp;
  }

  public float getPlayerBaseMaxHp() {
    return playerBaseMaxHp;
  }

  public float getEnemyBaseMaxHp() {
    return enemyBaseMaxHp;
  }

  public void spawnPlayerUnit(int type) {
    if (state != State.PLAYING) {
      return;
    }
    float cost = getCost(type, true);
    if (energy < cost) {
      return;
    }
    energy -= cost;
    Unit unit = buildUnit(type, true);
    playerUnits.add(unit);
  }

  private void spawnEnemyUnit(int type) {
    if (state != State.PLAYING) {
      return;
    }
    Unit unit = buildUnit(type, false);
    enemyUnits.add(unit);
  }

  private Unit buildUnit(int type, boolean playerSide) {
    Unit unit = new Unit();
    unit.player = playerSide;
    unit.type = type;
    unit.radius = type == UNIT_TANK ? 28f : type == UNIT_RANGER ? 20f : 18f;
    unit.speed = getSpeed(type, playerSide);
    unit.range = getRange(type, playerSide);
    unit.damage = getDamage(type, playerSide);
    unit.cooldownTime = getCooldown(type, playerSide);
    unit.cooldown = 0f;
    unit.maxHp = getHp(type, playerSide);
    unit.hp = unit.maxHp;
    if (playerSide) {
      unit.x = playerBaseX + baseWidth * 0.5f + unit.radius;
    } else {
      unit.x = enemyBaseX - baseWidth * 0.5f - unit.radius;
    }
    return unit;
  }

  private float getCost(int type, boolean playerSide) {
    if (!playerSide) {
      return 0f;
    }
    if (type == UNIT_INFANTRY) {
      return 15f;
    }
    if (type == UNIT_RANGER) {
      return 25f;
    }
    return 45f;
  }

  private float getHp(int type, boolean playerSide) {
    float base;
    if (type == UNIT_INFANTRY) {
      base = 60f;
    } else if (type == UNIT_RANGER) {
      base = 90f;
    } else {
      base = 200f;
    }
    if (!playerSide) {
      base *= 1.1f;
    }
    return base;
  }

  private float getSpeed(int type, boolean playerSide) {
    float base;
    if (type == UNIT_INFANTRY) {
      base = 140f;
    } else if (type == UNIT_RANGER) {
      base = 120f;
    } else {
      base = 80f;
    }
    if (!playerSide) {
      base *= 0.95f;
    }
    return base;
  }

  private float getRange(int type, boolean playerSide) {
    float base;
    if (type == UNIT_INFANTRY) {
      base = 36f;
    } else if (type == UNIT_RANGER) {
      base = 140f;
    } else {
      base = 48f;
    }
    if (!playerSide) {
      base *= 1.05f;
    }
    return base;
  }

  private float getDamage(int type, boolean playerSide) {
    float base;
    if (type == UNIT_INFANTRY) {
      base = 12f;
    } else if (type == UNIT_RANGER) {
      base = 16f;
    } else {
      base = 28f;
    }
    if (!playerSide) {
      base *= 1.05f;
    }
    return base;
  }

  private float getCooldown(int type, boolean playerSide) {
    float base;
    if (type == UNIT_INFANTRY) {
      base = 0.7f;
    } else if (type == UNIT_RANGER) {
      base = 1.0f;
    } else {
      base = 1.4f;
    }
    if (!playerSide) {
      base *= 0.95f;
    }
    return base;
  }

  public void onResumeView() {
    startLoop();
  }

  public void onPauseView() {
    if (state == State.PLAYING) {
      state = State.PAUSED;
    }
    stopLoop();
  }

  private void startLoop() {
    if (running) {
      return;
    }
    running = true;
    thread = new Thread(this);
    thread.start();
  }

  private void stopLoop() {
    running = false;
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static class Unit {
    int type;
    boolean player;
    float x;
    float hp;
    float maxHp;
    float speed;
    float range;
    float damage;
    float cooldown;
    float cooldownTime;
    float radius;
  }
}
