package com.android.boot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
  private final ArrayList<Projectile> projectiles = new ArrayList<>();
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
  private Paint paintBgGradient;
  private Paint paintLane;
  private Paint paintLaneGradient;
  private Paint paintLaneGrid;
  private Paint paintPlayer;
  private Paint paintPlayerGlow;
  private Paint paintPlayerShadow;
  private Paint paintEnemy;
  private Paint paintEnemyGlow;
  private Paint paintEnemyShadow;
  private Paint paintPlayerBase;
  private Paint paintPlayerBaseGlow;
  private Paint paintPlayerBaseShadow;
  private Paint paintPlayerBaseLight;
  private Paint paintEnemyBase;
  private Paint paintEnemyBaseGlow;
  private Paint paintEnemyBaseShadow;
  private Paint paintEnemyBaseLight;
  private Paint paintHpBarBg;
  private Paint paintHpBar;
  private Paint paintHpBarGlow;
  private Paint paintBulletPlayer;
  private Paint paintBulletEnemy;
  private Paint paintBulletTrail;
  private Paint paintMuzzleFlash;
  private Paint paintPriceBg;
  private Paint paintPriceText;
  private Paint paintBulletPlayer;
  private Paint paintBulletEnemy;
  private Paint paintBulletTrail;
  private Paint paintMuzzleFlash;
  private Paint paintPriceBg;
  private Paint paintPriceText;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    initPaints();
    resetGame();
  }

  private void initPaints() {
    paintBg = new Paint();
    paintBg.setColor(getContext().getColor(R.color.cst_bg_main));
    paintBgGradient = new Paint();
    paintLane = new Paint();
    paintLane.setColor(getContext().getColor(R.color.cst_lane));
    paintLaneGradient = new Paint();
    paintLaneGrid = new Paint();
    paintLaneGrid.setColor(getContext().getColor(R.color.cst_lane_grid));
    paintLaneGrid.setStrokeWidth(1f);
    paintPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayer.setColor(getContext().getColor(R.color.cst_unit_player));
    paintPlayer.setStyle(Paint.Style.FILL);
    paintPlayerGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerGlow.setColor(getContext().getColor(R.color.cst_unit_player_glow));
    paintPlayerGlow.setStyle(Paint.Style.STROKE);
    paintPlayerGlow.setStrokeWidth(3f);
    paintPlayerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerShadow.setColor(getContext().getColor(R.color.cst_unit_player_shadow));
    paintPlayerShadow.setStyle(Paint.Style.FILL);
    paintEnemy = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemy.setColor(getContext().getColor(R.color.cst_unit_enemy));
    paintEnemy.setStyle(Paint.Style.FILL);
    paintEnemyGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyGlow.setColor(getContext().getColor(R.color.cst_unit_enemy_glow));
    paintEnemyGlow.setStyle(Paint.Style.STROKE);
    paintEnemyGlow.setStrokeWidth(3f);
    paintEnemyShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyShadow.setColor(getContext().getColor(R.color.cst_unit_enemy_shadow));
    paintEnemyShadow.setStyle(Paint.Style.FILL);
    paintPlayerBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerBase.setColor(getContext().getColor(R.color.cst_base_player));
    paintPlayerBase.setStyle(Paint.Style.FILL);
    paintPlayerBaseGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerBaseGlow.setColor(getContext().getColor(R.color.cst_base_player_glow));
    paintPlayerBaseGlow.setStyle(Paint.Style.STROKE);
    paintPlayerBaseGlow.setStrokeWidth(4f);
    paintPlayerBaseShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerBaseShadow.setColor(getContext().getColor(R.color.cst_base_player_shadow));
    paintPlayerBaseShadow.setStyle(Paint.Style.FILL);
    paintPlayerBaseLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPlayerBaseLight.setColor(getContext().getColor(R.color.cst_base_player_light));
    paintPlayerBaseLight.setStyle(Paint.Style.FILL);
    paintEnemyBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyBase.setColor(getContext().getColor(R.color.cst_base_enemy));
    paintEnemyBase.setStyle(Paint.Style.FILL);
    paintEnemyBaseGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyBaseGlow.setColor(getContext().getColor(R.color.cst_base_enemy_glow));
    paintEnemyBaseGlow.setStyle(Paint.Style.STROKE);
    paintEnemyBaseGlow.setStrokeWidth(4f);
    paintEnemyBaseShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyBaseShadow.setColor(getContext().getColor(R.color.cst_base_enemy_shadow));
    paintEnemyBaseShadow.setStyle(Paint.Style.FILL);
    paintEnemyBaseLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintEnemyBaseLight.setColor(getContext().getColor(R.color.cst_base_enemy_light));
    paintEnemyBaseLight.setStyle(Paint.Style.FILL);
    paintHpBarBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintHpBarBg.setColor(getContext().getColor(R.color.cst_hp_bar_bg));
    paintHpBarBg.setStyle(Paint.Style.FILL);
    paintHpBar = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintHpBar.setStyle(Paint.Style.FILL);
    paintHpBarGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintHpBarGlow.setColor(getContext().getColor(R.color.cst_hp_bar_glow));
    paintHpBarGlow.setStyle(Paint.Style.STROKE);
    paintHpBarGlow.setStrokeWidth(1.5f);
    paintBulletPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBulletPlayer.setColor(getContext().getColor(R.color.cst_bullet_player));
    paintBulletPlayer.setStyle(Paint.Style.FILL);
    paintBulletEnemy = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBulletEnemy.setColor(getContext().getColor(R.color.cst_bullet_enemy));
    paintBulletEnemy.setStyle(Paint.Style.FILL);
    paintBulletTrail = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBulletTrail.setColor(getContext().getColor(R.color.cst_bullet_trail));
    paintBulletTrail.setStyle(Paint.Style.STROKE);
    paintBulletTrail.setStrokeWidth(2f);
    paintMuzzleFlash = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintMuzzleFlash.setColor(getContext().getColor(R.color.cst_muzzle_flash));
    paintMuzzleFlash.setStyle(Paint.Style.FILL);
    paintPriceBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPriceBg.setColor(getContext().getColor(R.color.cst_price_bg));
    paintPriceBg.setStyle(Paint.Style.FILL);
    paintPriceText = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintPriceText.setColor(getContext().getColor(R.color.cst_price_text));
    paintPriceText.setTextSize(18f);
    paintPriceText.setTextAlign(Paint.Align.CENTER);
    paintPriceText.setFakeBoldText(true);
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
    LinearGradient bgGradient = new LinearGradient(0f, 0f, 0f, height,
        getContext().getColor(R.color.cst_bg_gradient_start),
        getContext().getColor(R.color.cst_bg_gradient_end),
        Shader.TileMode.CLAMP);
    paintBgGradient.setShader(bgGradient);
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
    updateProjectiles(dt);
    updateUnits(playerUnits, enemyUnits, true, dt);
    updateUnits(enemyUnits, playerUnits, false, dt);
    updateProjectiles(dt);
    if (playerBaseHp <= 0f || enemyBaseHp <= 0f) {
      playerWon = enemyBaseHp <= 0f;
      state = State.GAME_OVER;
    }
  }

  private void updateProjectiles(float dt) {
    int i = projectiles.size() - 1;
    while (i >= 0) {
      Projectile proj = projectiles.get(i);
      proj.lifetime -= dt;
      if (proj.lifetime <= 0f) {
        projectiles.remove(i);
        i -= 1;
        continue;
      }
      proj.x += proj.vx * dt;
      proj.y += proj.vy * dt;
      float dx = proj.targetX - proj.x;
      float dy = proj.targetY - proj.y;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      if (dist < 10f) {
        projectiles.remove(i);
        i -= 1;
        continue;
      }
      i -= 1;
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
      if (unit.attackAnim > 0f) {
        unit.attackAnim -= dt * 3f;
        if (unit.attackAnim < 0f) {
          unit.attackAnim = 0f;
        }
      }
      Unit target = findTarget(unit, opponents, playerSide);
      boolean attacked = false;
      if (target != null) {
        if (unit.cooldown <= 0f) {
          createProjectile(unit, target, playerSide);
          unit.attackAnim = 0.3f;
          unit.cooldown = unit.cooldownTime;
        }
        attacked = true;
      }
      if (!attacked) {
        if (playerSide) {
          if (unit.x + unit.range >= enemyBaseX - baseWidth * 0.5f) {
            if (unit.cooldown <= 0f) {
              createBaseProjectile(unit, enemyBaseX, playerSide);
              unit.attackAnim = 0.3f;
              unit.cooldown = unit.cooldownTime;
            }
            attacked = true;
          }
        } else {
          if (unit.x - unit.range <= playerBaseX + baseWidth * 0.5f) {
            if (unit.cooldown <= 0f) {
              createBaseProjectile(unit, playerBaseX, playerSide);
              unit.attackAnim = 0.3f;
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
    drawBackground(canvas);
    float laneTop = laneY - laneHeight * 0.5f;
    float laneBottom = laneY + laneHeight * 0.5f;
    drawLane(canvas, laneTop, laneBottom);
    float baseTop = laneY - baseHeight * 0.5f;
    float baseBottom = laneY + baseHeight * 0.5f;
    drawBase(canvas, playerBaseX, baseTop, baseBottom, true);
    drawBase(canvas, enemyBaseX, baseTop, baseBottom, false);
    int i = 0;
    int count = playerUnits.size();
    while (i < count) {
      Unit unit = playerUnits.get(i);
      drawUnit(canvas, unit, true);
      i += 1;
    }
    i = 0;
    count = enemyUnits.size();
    while (i < count) {
      Unit unit = enemyUnits.get(i);
      drawUnit(canvas, unit, false);
      i += 1;
    }
    drawProjectiles(canvas);
    holder.unlockCanvasAndPost(canvas);
  }

  private void drawBackground(Canvas canvas) {
    canvas.drawRect(0f, 0f, width, height, paintBgGradient);
    float gridSize = 40f;
    for (float x = 0f; x < width; x += gridSize) {
      canvas.drawLine(x, 0f, x, height, paintLaneGrid);
    }
    for (float y = 0f; y < height; y += gridSize) {
      canvas.drawLine(0f, y, width, y, paintLaneGrid);
    }
  }

  private void drawLane(Canvas canvas, float top, float bottom) {
    LinearGradient laneGradient = new LinearGradient(0f, top, 0f, bottom,
        getContext().getColor(R.color.cst_lane_gradient_start),
        getContext().getColor(R.color.cst_lane_gradient_end),
        Shader.TileMode.CLAMP);
    paintLaneGradient.setShader(laneGradient);
    canvas.drawRect(0f, top, width, bottom, paintLaneGradient);
    float gridSpacing = 30f;
    for (float x = 0f; x < width; x += gridSpacing) {
      canvas.drawLine(x, top, x, bottom, paintLaneGrid);
    }
  }

  private void drawBase(Canvas canvas, float centerX, float top, float bottom, boolean playerSide) {
    float left = centerX - baseWidth * 0.5f;
    float right = centerX + baseWidth * 0.5f;
    Paint basePaint = playerSide ? paintPlayerBase : paintEnemyBase;
    Paint baseShadow = playerSide ? paintPlayerBaseShadow : paintEnemyBaseShadow;
    Paint baseGlow = playerSide ? paintPlayerBaseGlow : paintEnemyBaseGlow;
    Paint baseLight = playerSide ? paintPlayerBaseLight : paintEnemyBaseLight;
    float shadowOffset = 6f;
    canvas.drawRoundRect(left + shadowOffset, top + shadowOffset, right + shadowOffset, bottom + shadowOffset, 8f, 8f, baseShadow);
    canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, basePaint);
    float lightWidth = baseWidth * 0.15f;
    float lightHeight = (bottom - top) * 0.2f;
    canvas.drawRoundRect(left + baseWidth * 0.1f, top + baseHeight * 0.1f,
        left + baseWidth * 0.1f + lightWidth, top + baseHeight * 0.1f + lightHeight, 4f, 4f, baseLight);
    canvas.drawRoundRect(right - baseWidth * 0.1f - lightWidth, top + baseHeight * 0.1f,
        right - baseWidth * 0.1f, top + baseHeight * 0.1f + lightHeight, 4f, 4f, baseLight);
    canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, baseGlow);
    float towerTop = top - baseHeight * 0.15f;
    float towerWidth = baseWidth * 0.3f;
    float towerLeft = centerX - towerWidth * 0.5f;
    float towerRight = centerX + towerWidth * 0.5f;
    canvas.drawRoundRect(towerLeft + shadowOffset, towerTop + shadowOffset, towerRight + shadowOffset, top + shadowOffset, 6f, 6f, baseShadow);
    canvas.drawRoundRect(towerLeft, towerTop, towerRight, top, 6f, 6f, basePaint);
    canvas.drawRoundRect(towerLeft, towerTop, towerRight, top, 6f, 6f, baseGlow);
  }

  private void drawUnit(Canvas canvas, Unit unit, boolean playerSide) {
    float x = unit.x;
    float y = laneY;
    float radius = unit.radius;
    Paint unitPaint = playerSide ? paintPlayer : paintEnemy;
    Paint unitGlow = playerSide ? paintPlayerGlow : paintEnemyGlow;
    Paint unitShadow = playerSide ? paintPlayerShadow : paintEnemyShadow;
    Paint hpPaint = playerSide ? paintHpBar : paintHpBar;
    hpPaint.setColor(getContext().getColor(playerSide ? R.color.cst_hp_player : R.color.cst_hp_enemy));
    float shadowOffset = 3f;
    canvas.drawCircle(x + shadowOffset, y + shadowOffset, radius, unitShadow);
    if (unit.type == UNIT_TANK) {
      drawTankUnit(canvas, x, y, radius, unitPaint, unitGlow);
    } else if (unit.type == UNIT_RANGER) {
      drawRangerUnit(canvas, x, y, radius, unitPaint, unitGlow);
    } else {
      drawInfantryUnit(canvas, x, y, radius, unitPaint, unitGlow);
    }
    float hpRatio = unit.hp / unit.maxHp;
    float hpBarWidth = radius * 2.2f;
    float hpBarHeight = 4f;
    float hpBarX = x - hpBarWidth * 0.5f;
    float hpBarY = y - radius - 8f;
    canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth, hpBarY + hpBarHeight, 2f, 2f, paintHpBarBg);
    if (hpRatio > 0f) {
      canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth * hpRatio, hpBarY + hpBarHeight, 2f, 2f, hpPaint);
      canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth * hpRatio, hpBarY + hpBarHeight, 2f, 2f, paintHpBarGlow);
    }
  }

  private void drawInfantryUnit(Canvas canvas, float x, float y, float radius, Paint paint, Paint glow) {
    RadialGradient gradient = new RadialGradient(x, y - radius * 0.3f, radius * 1.2f,
        paint.getColor(), paint.getColor() & 0x80FFFFFF, Shader.TileMode.CLAMP);
    Paint gradientPaint = new Paint(paint);
    gradientPaint.setShader(gradient);
    canvas.drawCircle(x, y, radius, gradientPaint);
    canvas.drawCircle(x, y, radius, glow);
    float headRadius = radius * 0.4f;
    canvas.drawCircle(x, y - radius * 0.5f, headRadius, paint);
  }

  private void drawRangerUnit(Canvas canvas, float x, float y, float radius, Paint paint, Paint glow) {
    RadialGradient gradient = new RadialGradient(x, y - radius * 0.3f, radius * 1.2f,
        paint.getColor(), paint.getColor() & 0x80FFFFFF, Shader.TileMode.CLAMP);
    Paint gradientPaint = new Paint(paint);
    gradientPaint.setShader(gradient);
    canvas.drawCircle(x, y, radius, gradientPaint);
    canvas.drawCircle(x, y, radius, glow);
    float headRadius = radius * 0.35f;
    canvas.drawCircle(x, y - radius * 0.5f, headRadius, paint);
    Path bowPath = new Path();
    bowPath.moveTo(x - radius * 0.6f, y);
    bowPath.quadTo(x, y - radius * 0.8f, x + radius * 0.6f, y);
    Paint bowPaint = new Paint(glow);
    bowPaint.setStrokeWidth(2f);
    bowPaint.setStyle(Paint.Style.STROKE);
    canvas.drawPath(bowPath, bowPaint);
  }

  private void drawTankUnit(Canvas canvas, float x, float y, float radius, Paint paint, Paint glow) {
    RadialGradient gradient = new RadialGradient(x, y, radius * 1.3f,
        paint.getColor(), paint.getColor() & 0x90FFFFFF, Shader.TileMode.CLAMP);
    Paint gradientPaint = new Paint(paint);
    gradientPaint.setShader(gradient);
    float bodyWidth = radius * 1.6f;
    float bodyHeight = radius * 1.2f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.5f, radius * 0.3f, radius * 0.3f, gradientPaint);
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.5f, radius * 0.3f, radius * 0.3f, glow);
    float turretRadius = radius * 0.7f;
    canvas.drawCircle(x, y - radius * 0.2f, turretRadius, gradientPaint);
    canvas.drawCircle(x, y - radius * 0.2f, turretRadius, glow);
    float barrelLength = radius * 0.8f;
    canvas.drawRect(x, y - radius * 0.2f - 2f, x + barrelLength, y - radius * 0.2f + 2f, paint);
  }

  private void resetGame() {
    playerUnits.clear();
    enemyUnits.clear();
    projectiles.clear();
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
