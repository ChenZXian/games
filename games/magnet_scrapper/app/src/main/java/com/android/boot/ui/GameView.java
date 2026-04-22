package com.android.boot.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.core.content.ContextCompat;
import com.android.boot.R;
import com.android.boot.audio.TonePlayer;
import com.android.boot.core.GameLoopThread;
import com.android.boot.core.GameState;
import com.android.boot.core.InputState;
import com.android.boot.core.SpawnDirector;
import com.android.boot.entity.EnemyDrone;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.PlayerDrone;
import com.android.boot.entity.ProjectilePiece;
import com.android.boot.entity.ScrapPiece;
import com.android.boot.fx.ParticleBurst;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  public interface GameEvents {
    void onHudChanged(int score, int health, int ammo, int wave, float specialRatio, boolean specialReady, boolean pullMode);
    void onStateChanged(GameState state);
    void onGameOver(int score, int bestScore, int wavesSurvived);
  }

  private static final int MAX_ENEMIES = 48;
  private static final int MAX_SCRAP = 56;
  private static final int MAX_PROJECTILES = 40;
  private static final int MAX_PICKUPS = 32;
  private static final int MAX_FLOATS = 20;
  private final SurfaceHolder surfaceHolder;
  private GameLoopThread loopThread;
  private final InputState inputState = new InputState();
  private final SpawnDirector spawnDirector = new SpawnDirector();
  private final PlayerDrone player = new PlayerDrone();
  private final EnemyDrone[] enemies = new EnemyDrone[MAX_ENEMIES];
  private final ScrapPiece[] scrapPieces = new ScrapPiece[MAX_SCRAP];
  private final ProjectilePiece[] projectiles = new ProjectilePiece[MAX_PROJECTILES];
  private final Pickup[] pickups = new Pickup[MAX_PICKUPS];
  private final ParticleBurst particleBurst = new ParticleBurst();
  private final Random random = new Random();
  private final TonePlayer tonePlayer = new TonePlayer();
  private final Paint arenaPaint = new Paint();
  private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private final float[] floatX = new float[MAX_FLOATS];
  private final float[] floatY = new float[MAX_FLOATS];
  private final float[] floatLife = new float[MAX_FLOATS];
  private final int[] floatValue = new int[MAX_FLOATS];
  private final DisplayMetrics displayMetrics;
  private GameEvents gameEvents;
  private GameState state = GameState.MENU;
  private float arenaWidth;
  private float arenaHeight;
  private float playerRadiusPx;
  private float scrapRadiusPx;
  private float projectileRadiusPx;
  private float gridStepPx;
  private float particleSizePx;
  private int score;
  private int kills;
  private float survivalTime;
  private int bestScore;
  private float pushCooldown;
  private float scoreBank;
  private float modePulse;
  private float shakeTime;
  private float shakePower;
  private float specialCenterPull;
  private boolean resumePlaying;
  private final SharedPreferences preferences;
  private final int colorBg;
  private final int colorAlt;
  private final int colorAccent;
  private final int colorAccent2;
  private final int colorDanger;
  private final int colorSuccess;
  private final int colorWarning;
  private final int colorText;
  private final int colorMuted;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    surfaceHolder = getHolder();
    surfaceHolder.addCallback(this);
    setFocusable(true);
    displayMetrics = context.getResources().getDisplayMetrics();
    preferences = context.getSharedPreferences("magnet_scrapper", Context.MODE_PRIVATE);
    bestScore = preferences.getInt("best_score", 0);
    colorBg = ContextCompat.getColor(context, R.color.cst_bg_main);
    colorAlt = ContextCompat.getColor(context, R.color.cst_bg_alt);
    colorAccent = ContextCompat.getColor(context, R.color.cst_accent);
    colorAccent2 = ContextCompat.getColor(context, R.color.cst_accent_2);
    colorDanger = ContextCompat.getColor(context, R.color.cst_danger);
    colorSuccess = ContextCompat.getColor(context, R.color.cst_success);
    colorWarning = ContextCompat.getColor(context, R.color.cst_warning);
    colorText = ContextCompat.getColor(context, R.color.cst_text_primary);
    colorMuted = ContextCompat.getColor(context, R.color.cst_text_muted);
    initSizes(context.getResources());
    initEntities();
    configurePaints();
  }

  private void initSizes(Resources resources) {
    playerRadiusPx = resources.getDimension(R.dimen.cst_arena_player_radius);
    scrapRadiusPx = resources.getDimension(R.dimen.cst_arena_scrap_radius);
    projectileRadiusPx = resources.getDimension(R.dimen.cst_arena_projectile_radius);
    gridStepPx = resources.getDimension(R.dimen.cst_arena_grid_step);
    particleSizePx = resources.getDimension(R.dimen.cst_particle_size);
  }

  private void initEntities() {
    for (int i = 0; i < MAX_ENEMIES; i++) {
      enemies[i] = new EnemyDrone();
    }
    for (int i = 0; i < MAX_SCRAP; i++) {
      scrapPieces[i] = new ScrapPiece();
    }
    for (int i = 0; i < MAX_PROJECTILES; i++) {
      projectiles[i] = new ProjectilePiece();
    }
    for (int i = 0; i < MAX_PICKUPS; i++) {
      pickups[i] = new Pickup();
    }
  }

  private void configurePaints() {
    gridPaint.setStyle(Paint.Style.STROKE);
    gridPaint.setStrokeWidth(getResources().getDimension(R.dimen.cst_stroke_hair));
    gridPaint.setColor(colorMuted);
    gridPaint.setAlpha(55);
    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setStrokeWidth(getResources().getDimension(R.dimen.cst_arena_ring_stroke));
    playerPaint.setStyle(Paint.Style.FILL);
    enemyPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    enemyPaint.setStrokeWidth(getResources().getDimension(R.dimen.cst_stroke_s));
    accentPaint.setStyle(Paint.Style.FILL);
    textPaint.setColor(colorText);
    textPaint.setTextSize(getResources().getDimension(R.dimen.cst_text_s));
    particlePaint.setStyle(Paint.Style.FILL);
  }

  public void setGameEvents(GameEvents events) {
    gameEvents = events;
  }

  public GameState getCurrentState() {
    return state;
  }

  public void startRun() {
    resetRun();
    setState(GameState.PLAYING);
  }

  public void restartRun() {
    resetRun();
    setState(GameState.PLAYING);
  }

  public void pauseRun() {
    if (state == GameState.PLAYING) {
      setState(GameState.PAUSED);
    }
  }

  public void resumeRun() {
    if (state == GameState.PAUSED) {
      setState(GameState.PLAYING);
    }
  }

  public void returnToMenu() {
    setState(GameState.MENU);
  }

  public void toggleMode() {
    if (state != GameState.PLAYING) {
      return;
    }
    player.pullMode = !player.pullMode;
    modePulse = 0.35f;
    tonePlayer.pulse();
    dispatchHud();
  }

  public void triggerSpecial() {
    if (state != GameState.PLAYING || player.special < 100f) {
      return;
    }
    player.special = 0f;
    specialCenterPull = 1.3f;
    shakeTime = 0.22f;
    shakePower = 18f;
    for (EnemyDrone enemy : enemies) {
      if (enemy.active) {
        float dx = enemy.x - player.x;
        float dy = enemy.y - player.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.001f) {
          len = 1f;
        }
        enemy.knockback = 0.45f;
        enemy.knockX = dx / len * 260f;
        enemy.knockY = dy / len * 260f;
        enemy.hitFlash = 0.18f;
      }
    }
    particleBurst.emit(player.x, player.y, colorAccent2, 180f, particleSizePx * 1.7f, 18);
    tonePlayer.blast();
    dispatchHud();
  }

  public boolean toggleSound() {
    return tonePlayer.toggle();
  }

  public void handleHostPause() {
    resumePlaying = state == GameState.PLAYING;
    if (resumePlaying) {
      pauseRun();
    }
  }

  public void handleHostResume() {
    if (resumePlaying && state == GameState.PAUSED) {
      resumePlaying = false;
      resumeRun();
    }
  }

  public void shutdown() {
    tonePlayer.release();
    stopLoop();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    startLoop();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    arenaWidth = width;
    arenaHeight = height;
    if (player.x == 0f && player.y == 0f) {
      player.reset(width * 0.5f, height * 0.5f, playerRadiusPx);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopLoop();
  }

  private void startLoop() {
    stopLoop();
    loopThread = new GameLoopThread(surfaceHolder, this);
    loopThread.start();
  }

  private void stopLoop() {
    if (loopThread != null) {
      loopThread.requestStop();
      try {
        loopThread.join();
      } catch (InterruptedException ignored) {
      }
      loopThread = null;
    }
  }

  private void resetRun() {
    score = 0;
    kills = 0;
    survivalTime = 0f;
    pushCooldown = 0f;
    scoreBank = 0f;
    modePulse = 0f;
    shakeTime = 0f;
    shakePower = 0f;
    specialCenterPull = 0f;
    for (EnemyDrone enemy : enemies) {
      enemy.active = false;
    }
    for (ScrapPiece scrap : scrapPieces) {
      scrap.active = false;
    }
    for (ProjectilePiece projectile : projectiles) {
      projectile.active = false;
    }
    for (Pickup pickup : pickups) {
      pickup.active = false;
    }
    for (int i = 0; i < MAX_FLOATS; i++) {
      floatLife[i] = 0f;
    }
    if (arenaWidth <= 0f) {
      arenaWidth = getWidth();
      arenaHeight = getHeight();
    }
    player.reset(arenaWidth * 0.5f, arenaHeight * 0.5f, playerRadiusPx);
    spawnDirector.reset();
    for (int i = 0; i < 18; i++) {
      spawnLooseScrap(random.nextFloat() * Math.max(1f, arenaWidth), random.nextFloat() * Math.max(1f, arenaHeight));
    }
    dispatchHud();
  }

  public void tick(float delta) {
    if (arenaWidth <= 0f || arenaHeight <= 0f) {
      arenaWidth = getWidth();
      arenaHeight = getHeight();
    }
    particleBurst.update(delta);
    updateFloating(delta);
    if (modePulse > 0f) {
      modePulse -= delta;
    }
    if (shakeTime > 0f) {
      shakeTime -= delta;
    }
    if (state != GameState.PLAYING) {
      return;
    }
    survivalTime += delta;
    pushCooldown -= delta;
    if (player.invuln > 0f) {
      player.invuln -= delta;
    }
    if (specialCenterPull > 0f) {
      specialCenterPull -= delta;
    }
    spawnDirector.update(delta);
    updatePlayer(delta);
    updateScrap(delta);
    updatePickups(delta);
    updateProjectiles(delta);
    updateEnemies(delta);
    if (spawnDirector.shouldSpawn(activeEnemyCount())) {
      spawnEnemy();
    }
    scoreBank += delta * 12f * spawnDirector.getWave();
    while (scoreBank >= 1f) {
      score++;
      scoreBank -= 1f;
    }
    if (score > bestScore) {
      bestScore = score;
      preferences.edit().putInt("best_score", bestScore).apply();
    }
    dispatchHud();
    if (player.health <= 0) {
      setState(GameState.GAME_OVER);
      if (gameEvents != null) {
        gameEvents.onGameOver(score, bestScore, spawnDirector.getWave());
      }
    }
  }

  private void updatePlayer(float delta) {
    if (inputState.dragging) {
      float dx = inputState.targetX - player.x;
      float dy = inputState.targetY - player.y;
      float len = (float) Math.hypot(dx, dy);
      if (len > 0.001f) {
        float desired = Math.min(280f, len * 3f);
        player.vx += (dx / len * desired - player.vx) * Math.min(1f, delta * 8f);
        player.vy += (dy / len * desired - player.vy) * Math.min(1f, delta * 8f);
      }
    } else {
      player.vx *= 0.9f;
      player.vy *= 0.9f;
    }
    player.x += player.vx * delta;
    player.y += player.vy * delta;
    float margin = player.radius * 1.2f;
    if (player.x < margin) {
      player.x = margin;
      player.vx = 0f;
    } else if (player.x > arenaWidth - margin) {
      player.x = arenaWidth - margin;
      player.vx = 0f;
    }
    if (player.y < margin) {
      player.y = margin;
      player.vy = 0f;
    } else if (player.y > arenaHeight - margin) {
      player.y = arenaHeight - margin;
      player.vy = 0f;
    }
    if (!player.pullMode && player.ammo > 0 && pushCooldown <= 0f) {
      EnemyDrone target = findNearestEnemy(player.x, player.y, 160f);
      if (target != null) {
        float dx = target.x - player.x;
        float dy = target.y - player.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.001f) {
          len = 1f;
        }
        spawnProjectile(player.x, player.y, dx / len * 360f, dy / len * 360f);
        player.ammo--;
        pushCooldown = 0.22f;
        modePulse = 0.14f;
        tonePlayer.pulse();
      }
    }
  }

  private void updateScrap(float delta) {
    for (ScrapPiece scrap : scrapPieces) {
      if (!scrap.active) {
        continue;
      }
      scrap.life -= delta;
      if (scrap.life <= 0f) {
        scrap.active = false;
        continue;
      }
      float dxCenter = arenaWidth * 0.5f - scrap.x;
      float dyCenter = arenaHeight * 0.5f - scrap.y;
      if (specialCenterPull > 0f) {
        scrap.vx += dxCenter * delta * 3.4f;
        scrap.vy += dyCenter * delta * 3.4f;
      }
      if (player.pullMode) {
        float dx = player.x - scrap.x;
        float dy = player.y - scrap.y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < 180f) {
          float force = (180f - dist) * 3.2f;
          if (dist > 0.001f) {
            scrap.vx += dx / dist * force * delta;
            scrap.vy += dy / dist * force * delta;
          }
        }
      }
      scrap.x += scrap.vx * delta;
      scrap.y += scrap.vy * delta;
      scrap.vx *= 0.98f;
      scrap.vy *= 0.98f;
      if (distanceSq(scrap.x, scrap.y, player.x, player.y) < square(player.radius + scrap.radius + 4f)) {
        scrap.active = false;
        player.ammo = Math.min(18, player.ammo + 1);
        score += 6;
        addFloat(scrap.x, scrap.y, 6);
        particleBurst.emit(scrap.x, scrap.y, colorAccent, 80f, particleSizePx, 4);
      }
    }
  }

  private void updatePickups(float delta) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        continue;
      }
      if (specialCenterPull > 0f) {
        pickup.vx += (arenaWidth * 0.5f - pickup.x) * delta * 2.1f;
        pickup.vy += (arenaHeight * 0.5f - pickup.y) * delta * 2.1f;
      }
      if (player.pullMode) {
        float dx = player.x - pickup.x;
        float dy = player.y - pickup.y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < 220f) {
          float force = (220f - dist) * 2.2f;
          if (dist > 0.001f) {
            pickup.vx += dx / dist * force * delta;
            pickup.vy += dy / dist * force * delta;
          }
        }
      }
      pickup.x += pickup.vx * delta;
      pickup.y += pickup.vy * delta;
      pickup.vx *= 0.985f;
      pickup.vy *= 0.985f;
      if (distanceSq(pickup.x, pickup.y, player.x, player.y) < square(player.radius + pickup.radius + 6f)) {
        pickup.active = false;
        if (pickup.type == Pickup.TYPE_ENERGY) {
          player.special = Math.min(100f, player.special + 34f);
          score += 12;
          addFloat(pickup.x, pickup.y, 12);
          particleBurst.emit(pickup.x, pickup.y, colorSuccess, 90f, particleSizePx * 1.1f, 5);
        } else {
          player.ammo = Math.min(18, player.ammo + 2);
          score += 10;
          addFloat(pickup.x, pickup.y, 10);
          particleBurst.emit(pickup.x, pickup.y, colorAccent, 85f, particleSizePx, 4);
        }
      }
    }
  }

  private void updateProjectiles(float delta) {
    for (ProjectilePiece projectile : projectiles) {
      if (!projectile.active) {
        continue;
      }
      projectile.life -= delta;
      if (projectile.life <= 0f) {
        projectile.active = false;
        continue;
      }
      projectile.x += projectile.vx * delta;
      projectile.y += projectile.vy * delta;
      for (EnemyDrone enemy : enemies) {
        if (enemy.active && distanceSq(projectile.x, projectile.y, enemy.x, enemy.y) < square(projectile.radius + enemy.radius)) {
          projectile.active = false;
          enemy.hp -= enemy.type == EnemyDrone.TYPE_TANK ? 1.3f : 1.8f;
          enemy.hitFlash = 0.12f;
          tonePlayer.hit();
          particleBurst.emit(projectile.x, projectile.y, colorWarning, 100f, particleSizePx, 4);
          if (enemy.hp <= 0f) {
            destroyEnemy(enemy, projectile.x, projectile.y);
          }
          break;
        }
      }
    }
  }

  private void updateEnemies(float delta) {
    for (EnemyDrone enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      if (enemy.hitFlash > 0f) {
        enemy.hitFlash -= delta;
      }
      float dx = player.x - enemy.x;
      float dy = player.y - enemy.y;
      float dist = (float) Math.hypot(dx, dy);
      if (dist < 0.001f) {
        dist = 1f;
      }
      float speed = enemy.speed;
      enemy.vx = dx / dist * speed;
      enemy.vy = dy / dist * speed;
      enemy.x += enemy.vx * delta;
      enemy.y += enemy.vy * delta;
      if (enemy.knockback > 0f) {
        enemy.knockback -= delta;
        enemy.x += enemy.knockX * delta;
        enemy.y += enemy.knockY * delta;
        enemy.knockX *= 0.92f;
        enemy.knockY *= 0.92f;
      }
      if (distanceSq(enemy.x, enemy.y, player.x, player.y) < square(enemy.radius + player.radius)) {
        if (player.invuln <= 0f) {
          player.health--;
          player.invuln = 1f;
          shakeTime = 0.18f;
          shakePower = enemy.type == EnemyDrone.TYPE_TANK ? 20f : 12f;
          tonePlayer.hit();
          if (enemy.type == EnemyDrone.TYPE_LEECH && player.ammo > 0) {
            player.ammo--;
          }
        }
        float repel = enemy.type == EnemyDrone.TYPE_TANK ? 120f : 180f;
        enemy.x -= dx / dist * repel * delta;
        enemy.y -= dy / dist * repel * delta;
      }
    }
  }

  private void destroyEnemy(EnemyDrone enemy, float fx, float fy) {
    enemy.active = false;
    kills++;
    int reward = enemy.type == EnemyDrone.TYPE_TANK ? 35 : enemy.type == EnemyDrone.TYPE_LEECH ? 24 : 18;
    score += reward;
    addFloat(fx, fy, reward);
    player.special = Math.min(100f, player.special + (enemy.type == EnemyDrone.TYPE_TANK ? 22f : 12f));
    particleBurst.emit(fx, fy, enemy.type == EnemyDrone.TYPE_TANK ? colorDanger : colorAccent2, 120f, particleSizePx * 1.3f, 10);
    shakeTime = 0.1f;
    shakePower = enemy.type == EnemyDrone.TYPE_TANK ? 16f : 10f;
    if (random.nextBoolean()) {
      spawnPickup(random.nextBoolean() ? Pickup.TYPE_SCRAP : Pickup.TYPE_ENERGY, fx, fy);
    } else {
      spawnLooseScrap(fx, fy);
      if (enemy.type == EnemyDrone.TYPE_TANK) {
        spawnLooseScrap(fx + 8f, fy - 10f);
      }
    }
  }

  private void spawnEnemy() {
    for (EnemyDrone enemy : enemies) {
      if (!enemy.active) {
        spawnDirector.populateEnemy(enemy, arenaWidth, arenaHeight, displayMetrics);
        return;
      }
    }
  }

  private void spawnLooseScrap(float x, float y) {
    for (ScrapPiece scrap : scrapPieces) {
      if (!scrap.active) {
        float angle = random.nextFloat() * 6.28318f;
        float speed = 18f + random.nextFloat() * 24f;
        scrap.activate(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, scrapRadiusPx);
        return;
      }
    }
  }

  private void spawnPickup(int type, float x, float y) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        float angle = random.nextFloat() * 6.28318f;
        float speed = 40f + random.nextFloat() * 30f;
        pickup.activate(type, x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, scrapRadiusPx + 2f);
        return;
      }
    }
  }

  private void spawnProjectile(float x, float y, float vx, float vy) {
    for (ProjectilePiece projectile : projectiles) {
      if (!projectile.active) {
        projectile.activate(x, y, vx, vy, projectileRadiusPx);
        particleBurst.emit(x, y, colorWarning, 70f, particleSizePx, 3);
        return;
      }
    }
  }

  private EnemyDrone findNearestEnemy(float x, float y, float range) {
    EnemyDrone nearest = null;
    float best = range * range;
    for (EnemyDrone enemy : enemies) {
      if (enemy.active) {
        float dist = distanceSq(x, y, enemy.x, enemy.y);
        if (dist < best) {
          best = dist;
          nearest = enemy;
        }
      }
    }
    return nearest;
  }

  private int activeEnemyCount() {
    int count = 0;
    for (EnemyDrone enemy : enemies) {
      if (enemy.active) {
        count++;
      }
    }
    return count;
  }

  private void addFloat(float x, float y, int value) {
    for (int i = 0; i < MAX_FLOATS; i++) {
      if (floatLife[i] <= 0f) {
        floatX[i] = x;
        floatY[i] = y;
        floatLife[i] = 0.8f;
        floatValue[i] = value;
        return;
      }
    }
  }

  private void updateFloating(float delta) {
    for (int i = 0; i < MAX_FLOATS; i++) {
      if (floatLife[i] > 0f) {
        floatLife[i] -= delta;
        floatY[i] -= 34f * delta;
      }
    }
  }

  private void dispatchHud() {
    if (gameEvents != null) {
      gameEvents.onHudChanged(score, player.health, player.ammo, spawnDirector.getWave(), player.special / 100f, player.special >= 100f, player.pullMode);
    }
  }

  private void setState(GameState nextState) {
    state = nextState;
    if (gameEvents != null) {
      gameEvents.onStateChanged(nextState);
    }
  }

  public void drawFrame(Canvas canvas) {
    if (canvas == null) {
      return;
    }
    float shakeX = 0f;
    float shakeY = 0f;
    if (shakeTime > 0f) {
      shakeX = (random.nextFloat() - 0.5f) * shakePower;
      shakeY = (random.nextFloat() - 0.5f) * shakePower;
    }
    canvas.save();
    canvas.translate(shakeX, shakeY);
    drawArena(canvas);
    drawScrap(canvas);
    drawPickups(canvas);
    drawProjectiles(canvas);
    drawEnemies(canvas);
    drawPlayer(canvas);
    drawParticles(canvas);
    drawFloatingScores(canvas);
    canvas.restore();
  }

  private void drawArena(Canvas canvas) {
    arenaPaint.setShader(new LinearGradient(0f, 0f, 0f, arenaHeight, colorBg, colorAlt, Shader.TileMode.CLAMP));
    canvas.drawRect(0f, 0f, arenaWidth, arenaHeight, arenaPaint);
    for (float x = 0f; x <= arenaWidth; x += gridStepPx) {
      canvas.drawLine(x, 0f, x, arenaHeight, gridPaint);
    }
    for (float y = 0f; y <= arenaHeight; y += gridStepPx) {
      canvas.drawLine(0f, y, arenaWidth, y, gridPaint);
    }
    ringPaint.setColor(colorAccent);
    ringPaint.setAlpha(60);
    canvas.drawCircle(arenaWidth * 0.5f, arenaHeight * 0.5f, Math.min(arenaWidth, arenaHeight) * 0.28f, ringPaint);
    ringPaint.setColor(colorAccent2);
    ringPaint.setAlpha(70);
    canvas.drawCircle(arenaWidth * 0.5f, arenaHeight * 0.5f, Math.min(arenaWidth, arenaHeight) * 0.43f, ringPaint);
    accentPaint.setShader(new RadialGradient(arenaWidth * 0.15f, arenaHeight * 0.18f, arenaWidth * 0.35f, colorAccent, Color.TRANSPARENT, Shader.TileMode.CLAMP));
    accentPaint.setAlpha(45);
    canvas.drawRect(0f, 0f, arenaWidth, arenaHeight, accentPaint);
    accentPaint.setShader(new RadialGradient(arenaWidth * 0.82f, arenaHeight * 0.76f, arenaWidth * 0.26f, colorAccent2, Color.TRANSPARENT, Shader.TileMode.CLAMP));
    accentPaint.setAlpha(40);
    canvas.drawRect(0f, 0f, arenaWidth, arenaHeight, accentPaint);
    accentPaint.setShader(null);
  }

  private void drawPlayer(Canvas canvas) {
    int alpha = player.invuln > 0f ? 130 : 255;
    playerPaint.setColor(player.pullMode ? colorAccent : colorWarning);
    playerPaint.setAlpha(alpha);
    canvas.drawCircle(player.x, player.y, player.radius, playerPaint);
    ringPaint.setColor(player.pullMode ? colorAccent2 : colorDanger);
    ringPaint.setAlpha(alpha);
    canvas.drawCircle(player.x, player.y, player.radius + 8f + modePulse * 30f, ringPaint);
    playerPaint.setColor(colorText);
    playerPaint.setAlpha(alpha);
    rect.set(player.x - player.radius * 0.45f, player.y - player.radius * 0.22f, player.x + player.radius * 0.45f, player.y + player.radius * 0.22f);
    canvas.drawRoundRect(rect, player.radius * 0.2f, player.radius * 0.2f, playerPaint);
  }

  private void drawEnemies(Canvas canvas) {
    for (EnemyDrone enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      if (enemy.type == EnemyDrone.TYPE_SCOUT) {
        enemyPaint.setColor(enemy.hitFlash > 0f ? colorWarning : colorAccent2);
        float r = enemy.radius;
        float[] pts = {enemy.x, enemy.y - r, enemy.x + r * 0.9f, enemy.y, enemy.x, enemy.y + r, enemy.x - r * 0.9f, enemy.y};
        canvas.drawLines(new float[]{pts[0], pts[1], pts[2], pts[3], pts[2], pts[3], pts[4], pts[5], pts[4], pts[5], pts[6], pts[7], pts[6], pts[7], pts[0], pts[1]}, enemyPaint);
        canvas.drawCircle(enemy.x, enemy.y, r * 0.5f, enemyPaint);
      } else if (enemy.type == EnemyDrone.TYPE_TANK) {
        enemyPaint.setColor(enemy.hitFlash > 0f ? colorWarning : colorDanger);
        rect.set(enemy.x - enemy.radius, enemy.y - enemy.radius * 0.78f, enemy.x + enemy.radius, enemy.y + enemy.radius * 0.78f);
        canvas.drawRoundRect(rect, enemy.radius * 0.4f, enemy.radius * 0.4f, enemyPaint);
        playerPaint.setColor(colorText);
        canvas.drawCircle(enemy.x, enemy.y, enemy.radius * 0.28f, playerPaint);
      } else {
        enemyPaint.setColor(enemy.hitFlash > 0f ? colorWarning : colorSuccess);
        canvas.drawCircle(enemy.x, enemy.y, enemy.radius, enemyPaint);
        ringPaint.setColor(colorSuccess);
        ringPaint.setAlpha(140);
        canvas.drawCircle(enemy.x, enemy.y, enemy.radius + 6f, ringPaint);
      }
    }
  }

  private void drawScrap(Canvas canvas) {
    playerPaint.setColor(colorAccent);
    for (ScrapPiece scrap : scrapPieces) {
      if (scrap.active) {
        canvas.drawCircle(scrap.x, scrap.y, scrap.radius, playerPaint);
      }
    }
  }

  private void drawPickups(Canvas canvas) {
    for (Pickup pickup : pickups) {
      if (pickup.active) {
        playerPaint.setColor(pickup.type == Pickup.TYPE_ENERGY ? colorSuccess : colorWarning);
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, playerPaint);
      }
    }
  }

  private void drawProjectiles(Canvas canvas) {
    playerPaint.setColor(colorWarning);
    for (ProjectilePiece projectile : projectiles) {
      if (projectile.active) {
        canvas.drawCircle(projectile.x, projectile.y, projectile.radius, playerPaint);
      }
    }
  }

  private void drawParticles(Canvas canvas) {
    for (int i = 0; i < ParticleBurst.MAX_PARTICLES; i++) {
      if (particleBurst.active[i]) {
        particlePaint.setColor(particleBurst.color[i]);
        particlePaint.setAlpha((int) (Math.min(1f, particleBurst.life[i]) * 255f));
        canvas.drawCircle(particleBurst.x[i], particleBurst.y[i], particleBurst.size[i], particlePaint);
      }
    }
  }

  private void drawFloatingScores(Canvas canvas) {
    for (int i = 0; i < MAX_FLOATS; i++) {
      if (floatLife[i] > 0f) {
        textPaint.setAlpha((int) (Math.min(1f, floatLife[i]) * 255f));
        canvas.drawText("+" + floatValue[i], floatX[i], floatY[i], textPaint);
      }
    }
    textPaint.setAlpha(255);
  }

  private float distanceSq(float x1, float y1, float x2, float y2) {
    float dx = x2 - x1;
    float dy = y2 - y1;
    return dx * dx + dy * dy;
  }

  private float square(float value) {
    return value * value;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) {
      float x = event.getX(event.getActionIndex());
      float y = event.getY(event.getActionIndex());
      inputState.set(x, y, true);
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
      if (event.getPointerCount() <= 1) {
        inputState.set(player.x, player.y, false);
      }
      return true;
    }
    return super.onTouchEvent(event);
  }
}
