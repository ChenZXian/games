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
  private Paint paintInfantryBody;
  private Paint paintInfantryArmor;
  private Paint paintRangerBody;
  private Paint paintRangerArmor;
  private Paint paintTankBody;
  private Paint paintTankTurret;
  private Paint paintTankTrack;

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
    paintInfantryBody = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintInfantryBody.setColor(getContext().getColor(R.color.cst_infantry_body));
    paintInfantryBody.setStyle(Paint.Style.FILL);
    paintInfantryArmor = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintInfantryArmor.setColor(getContext().getColor(R.color.cst_infantry_armor));
    paintInfantryArmor.setStyle(Paint.Style.FILL);
    paintRangerBody = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintRangerBody.setColor(getContext().getColor(R.color.cst_ranger_body));
    paintRangerBody.setStyle(Paint.Style.FILL);
    paintRangerArmor = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintRangerArmor.setColor(getContext().getColor(R.color.cst_ranger_armor));
    paintRangerArmor.setStyle(Paint.Style.FILL);
    paintTankBody = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintTankBody.setColor(getContext().getColor(R.color.cst_tank_body));
    paintTankBody.setStyle(Paint.Style.FILL);
    paintTankTurret = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintTankTurret.setColor(getContext().getColor(R.color.cst_tank_turret));
    paintTankTurret.setStyle(Paint.Style.FILL);
    paintTankTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintTankTrack.setColor(getContext().getColor(R.color.cst_tank_track));
    paintTankTrack.setStyle(Paint.Style.FILL);
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
      if (proj.targetUnit != null && proj.targetUnit.hp > 0f) {
        proj.targetX = proj.targetUnit.x;
        proj.targetY = proj.targetUnit.y;
      }
      float dx = proj.targetX - proj.x;
      float dy = proj.targetY - proj.y;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      if (dist < 12f) {
        if (proj.targetUnit != null && proj.targetUnit.hp > 0f) {
          proj.targetUnit.hp -= proj.damage;
        } else if (proj.targetBase) {
          if (proj.playerSide) {
            enemyBaseHp -= proj.damage;
          } else {
            playerBaseHp -= proj.damage;
          }
        }
        projectiles.remove(i);
        i -= 1;
        continue;
      }
      i -= 1;
    }
  }

  public int getUnitCost(int type) {
    return (int) getCost(type, true);
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

  private void createProjectile(Unit shooter, Unit target, boolean playerSide) {
    Projectile proj = new Projectile();
    proj.x = shooter.x;
    proj.y = laneY;
    float dx = target.x - shooter.x;
    float dy = target.y - shooter.y;
    float dist = (float) Math.sqrt(dx * dx + dy * dy);
    proj.vx = (dx / dist) * 600f;
    proj.vy = (dy / dist) * 600f;
    proj.damage = shooter.damage;
    proj.playerSide = playerSide;
    proj.targetUnit = target;
    proj.targetX = target.x;
    proj.targetY = target.y;
    proj.life = 2f;
    proj.lifetime = 2f;
    shooter.targetUnit = target;
    projectiles.add(proj);
  }

  private void createBaseProjectile(Unit shooter, float baseX, boolean playerSide) {
    Projectile proj = new Projectile();
    proj.x = shooter.x;
    proj.y = laneY;
    float dx = baseX - shooter.x;
    float dy = 0f;
    float dist = Math.abs(dx);
    if (dist < 1f) {
      dist = 1f;
    }
    proj.vx = (dx / dist) * 600f;
    proj.vy = 0f;
    proj.damage = shooter.damage;
    proj.playerSide = playerSide;
    proj.targetBase = true;
    proj.targetBaseX = baseX;
    proj.targetX = baseX;
    proj.targetY = laneY;
    proj.life = 2f;
    proj.lifetime = 2f;
    projectiles.add(proj);
  }

  private void drawProjectiles(Canvas canvas) {
    int count = projectiles.size();
    for (int i = 0; i < count; i += 1) {
      Projectile proj = projectiles.get(i);
      Paint bulletPaint = proj.playerSide ? paintBulletPlayer : paintBulletEnemy;
      float bulletSize = 6f;
      RadialGradient bulletGradient = new RadialGradient(proj.x, proj.y, bulletSize * 2f,
          bulletPaint.getColor(), bulletPaint.getColor() & 0x00FFFFFF, Shader.TileMode.CLAMP);
      Paint gradientBullet = new Paint(bulletPaint);
      gradientBullet.setShader(bulletGradient);
      canvas.drawCircle(proj.x, proj.y, bulletSize * 2f, gradientBullet);
      canvas.drawCircle(proj.x, proj.y, bulletSize, bulletPaint);
      if (proj.lifetime > 0f && proj.lifetime < 0.1f) {
        float trailAlpha = (proj.lifetime / 0.1f) * 0.5f;
        Paint trailPaint = new Paint(paintBulletTrail);
        trailPaint.setAlpha((int) (trailAlpha * 255));
        canvas.drawCircle(proj.x, proj.y, bulletSize * 2.5f, trailPaint);
      }
    }
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
    // sky gradient
    canvas.drawRect(0f, 0f, width, height, paintBgGradient);
    // distant mountains
    Paint mountainPaintFar = new Paint(Paint.ANTI_ALIAS_FLAG);
    mountainPaintFar.setColor(getContext().getColor(R.color.cst_bg_gradient_end));
    mountainPaintFar.setAlpha(180);
    Paint mountainPaintNear = new Paint(Paint.ANTI_ALIAS_FLAG);
    mountainPaintNear.setColor(getContext().getColor(R.color.cst_lane));
    mountainPaintNear.setAlpha(200);
    float horizonY = height * 0.45f;
    Path farRange = new Path();
    farRange.moveTo(0f, horizonY);
    farRange.lineTo(width * 0.15f, horizonY - height * 0.12f);
    farRange.lineTo(width * 0.35f, horizonY - height * 0.06f);
    farRange.lineTo(width * 0.55f, horizonY - height * 0.13f);
    farRange.lineTo(width * 0.8f, horizonY - height * 0.08f);
    farRange.lineTo(width, horizonY - height * 0.14f);
    farRange.lineTo(width, horizonY + height * 0.1f);
    farRange.lineTo(0f, horizonY + height * 0.1f);
    farRange.close();
    canvas.drawPath(farRange, mountainPaintFar);
    Path nearRange = new Path();
    nearRange.moveTo(0f, horizonY + height * 0.02f);
    nearRange.lineTo(width * 0.2f, horizonY - height * 0.06f);
    nearRange.lineTo(width * 0.45f, horizonY);
    nearRange.lineTo(width * 0.7f, horizonY - height * 0.05f);
    nearRange.lineTo(width, horizonY + height * 0.02f);
    nearRange.lineTo(width, horizonY + height * 0.18f);
    nearRange.lineTo(0f, horizonY + height * 0.18f);
    nearRange.close();
    canvas.drawPath(nearRange, mountainPaintNear);
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
    // main keep
    canvas.drawRoundRect(left + shadowOffset, bottom - baseHeight * 0.8f + shadowOffset,
        right + shadowOffset, bottom + shadowOffset, 10f, 10f, baseShadow);
    canvas.drawRoundRect(left, bottom - baseHeight * 0.8f, right, bottom, 10f, 10f, basePaint);
    // battlements
    float merlonWidth = baseWidth / 6f;
    float merlonHeight = baseHeight * 0.18f;
    for (int i = 0; i < 6; i += 1) {
      float mx = left + merlonWidth * i;
      canvas.drawRect(mx + 2f, bottom - baseHeight * 0.8f - merlonHeight + 2f,
          mx + merlonWidth - 2f, bottom - baseHeight * 0.8f + 2f, basePaint);
    }
    // inner glow frame
    canvas.drawRoundRect(left, bottom - baseHeight * 0.8f, right, bottom, 10f, 10f, baseGlow);
    // banner tower
    float towerHeight = baseHeight * 0.9f;
    float towerWidth = baseWidth * 0.24f;
    float towerLeft = centerX - towerWidth * 0.5f;
    float towerRight = centerX + towerWidth * 0.5f;
    float towerTop = bottom - towerHeight;
    canvas.drawRoundRect(towerLeft + shadowOffset, towerTop + shadowOffset, towerRight + shadowOffset,
        bottom - baseHeight * 0.25f + shadowOffset, 8f, 8f, baseShadow);
    canvas.drawRoundRect(towerLeft, towerTop, towerRight, bottom - baseHeight * 0.25f, 8f, 8f, basePaint);
    canvas.drawRoundRect(towerLeft, towerTop, towerRight, bottom - baseHeight * 0.25f, 8f, 8f, baseGlow);
    // energy core window
    float coreWidth = towerWidth * 0.6f;
    float coreHeight = towerHeight * 0.28f;
    float coreLeft = centerX - coreWidth * 0.5f;
    float coreTop = bottom - baseHeight * 0.55f;
    canvas.drawRoundRect(coreLeft, coreTop, coreLeft + coreWidth, coreTop + coreHeight,
        6f, 6f, baseLight);
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

      drawTankUnit(canvas, x, y, radius, unit, unitPaint, unitGlow, playerSide);
    } else if (unit.type == UNIT_RANGER) {
      drawRangerUnit(canvas, x, y, radius, unit, unitPaint, unitGlow, playerSide);
    } else {
      drawInfantryUnit(canvas, x, y, radius, unit, unitPaint, unitGlow, playerSide);
    }
    if (unit.attackAnim > 0f && unit.targetUnit != null) {
      drawMuzzleFlash(canvas, x, y, unit.targetUnit.x, unit.targetUnit.y, unit.attackAnim, radius, playerSide);
    }
    float hpRatio = unit.hp / unit.maxHp;
    float hpBarWidth = radius * 2.2f;
    float hpBarHeight = 4f;
    float hpBarX = x - hpBarWidth * 0.5f;
    float hpBarY = y - radius - 10f;
    canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth, hpBarY + hpBarHeight, 2f, 2f, paintHpBarBg);
    if (hpRatio > 0f) {
      canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth * hpRatio, hpBarY + hpBarHeight, 2f, 2f, hpPaint);
      canvas.drawRoundRect(hpBarX, hpBarY, hpBarX + hpBarWidth * hpRatio, hpBarY + hpBarHeight, 2f, 2f, paintHpBarGlow);
    }
    if (playerSide) {
      float spawnTime = elapsedTime - unit.spawnTime;
      if (spawnTime < 1.5f && spawnTime >= 0f) {
        float cost = getCost(unit.type, true);
        float alpha = 1f - (spawnTime / 1.5f);
        Paint pricePaint = new Paint(paintPriceText);
        pricePaint.setAlpha((int) (alpha * 255));
        Paint bgPaint = new Paint(paintPriceBg);
        bgPaint.setAlpha((int) (alpha * 200));
        drawPriceTag(canvas, x, y - radius - 20f, (int) cost, pricePaint, bgPaint);
      }
    }
  }

  private void drawMuzzleFlash(Canvas canvas, float x, float y, float targetX, float targetY, float anim, float radius, boolean playerSide) {
    float flashSize = 8f + anim * 12f;
    float angle = (float) Math.atan2(targetY - y, targetX - x);
    float flashX = x + (float) Math.cos(angle) * (radius + 5f);
    float flashY = y + (float) Math.sin(angle) * (radius + 5f);
    Paint flashPaint = new Paint(paintMuzzleFlash);
    flashPaint.setAlpha((int) (anim * 255));
    canvas.drawCircle(flashX, flashY, flashSize, flashPaint);
    canvas.drawCircle(flashX, flashY, flashSize * 0.6f, paintMuzzleFlash);
  }

  private void drawPriceTag(Canvas canvas, float x, float y, int cost, Paint textPaint, Paint bgPaint) {
    String priceText = "$" + cost;
    float textWidth = textPaint.measureText(priceText);
    float padding = 6f;
    float tagWidth = textWidth + padding * 2f;
    float tagHeight = 18f;
    canvas.drawRoundRect(x - tagWidth * 0.5f, y - tagHeight * 0.5f,
        x + tagWidth * 0.5f, y + tagHeight * 0.5f, 4f, 4f, bgPaint);
    canvas.drawText(priceText, x, y + 6f, textPaint);
  }

  private void drawInfantryUnit(Canvas canvas, float x, float y, float radius, Unit unit, Paint paint, Paint glow, boolean playerSide) {
    float bodyY = y + radius * 0.1f;
    float bodyHeight = radius * 1.4f;
    float bodyWidth = radius * 0.8f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, bodyY - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f, 4f, 4f, paintInfantryBody);
    canvas.drawRoundRect(x - bodyWidth * 0.5f + 2f, bodyY - bodyHeight * 0.5f + 2f,
        x + bodyWidth * 0.5f - 2f, bodyY - bodyHeight * 0.3f, 2f, 2f, paintInfantryArmor);
    float headRadius = radius * 0.35f;
    canvas.drawCircle(x, y - radius * 0.6f, headRadius, paintInfantryArmor);
    float legWidth = radius * 0.15f;
    canvas.drawRect(x - bodyWidth * 0.3f, bodyY + bodyHeight * 0.3f,
        x - bodyWidth * 0.3f + legWidth, bodyY + bodyHeight * 0.6f, paintInfantryBody);
    canvas.drawRect(x + bodyWidth * 0.3f - legWidth, bodyY + bodyHeight * 0.3f,
        x + bodyWidth * 0.3f, bodyY + bodyHeight * 0.6f, paintInfantryBody);
    float armOffset = unit.attackAnim > 0f ? unit.attackAnim * radius * 0.3f : 0f;
    float armY = bodyY - bodyHeight * 0.1f;
    canvas.drawRect(x - bodyWidth * 0.5f - armOffset, armY - legWidth * 0.5f,
        x - bodyWidth * 0.3f - armOffset, armY + legWidth * 0.5f, paintInfantryBody);
    canvas.drawRect(x + bodyWidth * 0.3f + armOffset, armY - legWidth * 0.5f,
        x + bodyWidth * 0.5f + armOffset, armY + legWidth * 0.5f, paintInfantryBody);
    canvas.drawRoundRect(x - bodyWidth * 0.5f, bodyY - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f, 4f, 4f, glow);
  }

  private void drawRangerUnit(Canvas canvas, float x, float y, float radius, Unit unit, Paint paint, Paint glow, boolean playerSide) {
    float bodyY = y + radius * 0.1f;
    float bodyHeight = radius * 1.3f;
    float bodyWidth = radius * 0.75f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, bodyY - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f, 4f, 4f, paintRangerBody);
    canvas.drawRoundRect(x - bodyWidth * 0.5f + 2f, bodyY - bodyHeight * 0.5f + 2f,
        x + bodyWidth * 0.5f - 2f, bodyY - bodyHeight * 0.3f, 2f, 2f, paintRangerArmor);
    float headRadius = radius * 0.32f;
    canvas.drawCircle(x, y - radius * 0.6f, headRadius, paintRangerArmor);
    float legWidth = radius * 0.14f;
    canvas.drawRect(x - bodyWidth * 0.3f, bodyY + bodyHeight * 0.3f,
        x - bodyWidth * 0.3f + legWidth, bodyY + bodyHeight * 0.6f, paintRangerBody);
    canvas.drawRect(x + bodyWidth * 0.3f - legWidth, bodyY + bodyHeight * 0.3f,
        x + bodyWidth * 0.3f, bodyY + bodyHeight * 0.6f, paintRangerBody);
    float bowAngle = unit.attackAnim > 0f ? unit.attackAnim * 0.5f : 0f;
    Path bowPath = new Path();
    float bowX1 = x - radius * 0.5f;
    float bowY1 = y;
    float bowX2 = x + radius * 0.5f;
    float bowY2 = y;
    float bowCenterY = y - radius * 0.7f - bowAngle * radius * 0.3f;
    bowPath.moveTo(bowX1, bowY1);
    bowPath.quadTo(x, bowCenterY, bowX2, bowY2);
    Paint bowPaint = new Paint(glow);
    bowPaint.setStrokeWidth(3f);
    bowPaint.setStyle(Paint.Style.STROKE);
    canvas.drawPath(bowPath, bowPaint);
    canvas.drawRoundRect(x - bodyWidth * 0.5f, bodyY - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f, 4f, 4f, glow);
  }

  private void drawTankUnit(Canvas canvas, float x, float y, float radius, Unit unit, Paint paint, Paint glow, boolean playerSide) {
    float bodyWidth = radius * 1.8f;
    float bodyHeight = radius * 1.1f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.5f, radius * 0.25f, radius * 0.25f, paintTankBody);
    float trackHeight = radius * 0.25f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y + bodyHeight * 0.3f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.3f + trackHeight, radius * 0.15f, radius * 0.15f, paintTankTrack);
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.3f - trackHeight,
        x + bodyWidth * 0.5f, y - bodyHeight * 0.3f, radius * 0.15f, radius * 0.15f, paintTankTrack);
    float turretRadius = radius * 0.65f;
    float turretY = y - radius * 0.15f;
    canvas.drawCircle(x, turretY, turretRadius, paintTankTurret);
    float barrelAngle = unit.attackAnim > 0f ? unit.attackAnim * 0.2f : 0f;
    float barrelLength = radius * 1.0f;
    float barrelX = x + (float) Math.cos(barrelAngle) * barrelLength;
    float barrelY = turretY + (float) Math.sin(barrelAngle) * barrelLength;
    Paint barrelPaint = new Paint(paintTankTrack);
    barrelPaint.setStrokeWidth(5f);
    barrelPaint.setStyle(Paint.Style.STROKE);
    canvas.drawLine(x, turretY, barrelX, barrelY, barrelPaint);
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.5f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.5f, radius * 0.25f, radius * 0.25f, glow);
    canvas.drawCircle(x, turretY, turretRadius, glow);
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
    unit.attackAnim = 0f;
    unit.spawnTime = elapsedTime;
    unit.y = laneY;
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
    float y;
    float hp;
    float maxHp;
    float speed;
    float range;
    float damage;
    float cooldown;
    float cooldownTime;
    float radius;
    float attackAnim;
    float spawnTime;
    Unit targetUnit;
  }

  private static class Projectile {
    float x;
    float y;
    float vx;
    float vy;
    float damage;
    boolean playerSide;
    Unit targetUnit;
    boolean targetBase;
    float targetBaseX;
    float targetX;
    float targetY;
    float lifetime;
    float life;
  }
}
