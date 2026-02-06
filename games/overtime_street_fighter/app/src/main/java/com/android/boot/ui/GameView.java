package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.AudioController;
import com.android.boot.core.GameInput;
import com.android.boot.core.GameState;
import com.android.boot.core.StageManager;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.Player;
import com.android.boot.entity.Projectile;
import com.android.boot.fx.ScreenShake;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  private Thread gameThread;
  private boolean running;
  private GameState state = GameState.MENU;
  private final GameInput input = new GameInput();
  private final StageManager stageManager = new StageManager();
  private final List<Enemy> enemies = new ArrayList<>();
  private final List<Projectile> projectiles = new ArrayList<>();
  private final List<Pickup> pickups = new ArrayList<>();
  private final List<HitEffect> hitEffects = new ArrayList<>();
  private final List<CodeEffect> codeEffects = new ArrayList<>();
  private final List<BugEffect> bugEffects = new ArrayList<>();
  private final RectF rectA = new RectF();
  private final RectF rectB = new RectF();
  private final Random random = new Random(7);
  private Player player;
  private float groundY;
  private int score;
  private float energyGainBoostTimer;
  private float energyGainDebuffTimer;
  private float hudTimer;
  private Listener listener;
  private final AudioController audioController = new AudioController();
  private final ScreenShake screenShake = new ScreenShake();
  private float cameraX;
  private float worldOffset;

  private final Paint paintBackground = new Paint();
  private final Paint paintFloor = new Paint();
  private final Paint paintPlayer = new Paint();
  private final Paint paintPlayerOutline = new Paint();
  private final Paint paintPlayerShadow = new Paint();
  private final Paint paintEnemy = new Paint();
  private final Paint paintEnemyOutline = new Paint();
  private final Paint paintEnemyElite = new Paint();
  private final Paint paintEnemyEliteOutline = new Paint();
  private final Paint paintProjectile = new Paint();
  private final Paint paintProjectileGlow = new Paint();
  private final Paint paintPickup = new Paint();
  private final Paint paintPickupGlow = new Paint();
  private final Paint paintAttackRange = new Paint();
  private final Paint paintHitEffect = new Paint();
  private final Paint paintHitGlow = new Paint();
  private final Paint paintCode = new Paint();
  private final Paint paintBug = new Paint();
  private final Paint paintDebug = new Paint();
  private final Paint paintRequirement = new Paint();
  private final Paint paintTest = new Paint();
  private final Paint paintHpBarBg = new Paint();
  private final Paint paintHpBarFill = new Paint();
  private final Paint paintHpBarOutline = new Paint();
  private final Paint paintCooldownOverlay = new Paint();
  private final Paint paintCooldownText = new Paint();
  private final Paint paintFloorPattern = new Paint();
  private final Paint paintBgPattern = new Paint();

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    initPaints();
  }

  private void initPaints() {
    paintBackground.setColor(ContextCompat.getColor(getContext(), R.color.cst_bg_alt));
    paintFloor.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_floor));
    
    paintPlayer.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_player));
    paintPlayer.setStyle(Paint.Style.FILL);
    paintPlayerOutline.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_player));
    paintPlayerOutline.setStyle(Paint.Style.STROKE);
    paintPlayerOutline.setStrokeWidth(3f);
    paintPlayerShadow.setColor(0x66000000);
    paintPlayerShadow.setStyle(Paint.Style.FILL);
    
    paintEnemy.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_enemy));
    paintEnemy.setStyle(Paint.Style.FILL);
    paintEnemyOutline.setColor(0xFFFF6B8A);
    paintEnemyOutline.setStyle(Paint.Style.STROKE);
    paintEnemyOutline.setStrokeWidth(2.5f);
    
    paintEnemyElite.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_enemy_elite));
    paintEnemyElite.setStyle(Paint.Style.FILL);
    paintEnemyEliteOutline.setColor(0xFFFFE066);
    paintEnemyEliteOutline.setStyle(Paint.Style.STROKE);
    paintEnemyEliteOutline.setStrokeWidth(3f);
    
    paintProjectile.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_projectile));
    paintProjectile.setStyle(Paint.Style.FILL);
    paintProjectileGlow.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_projectile));
    paintProjectileGlow.setStyle(Paint.Style.FILL);
    paintProjectileGlow.setAlpha(100);
    
    paintPickup.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_pickup));
    paintPickup.setStyle(Paint.Style.FILL);
    paintPickupGlow.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_pickup));
    paintPickupGlow.setStyle(Paint.Style.FILL);
    paintPickupGlow.setAlpha(80);
    
    paintAttackRange.setColor(0x66FFFFFF);
    paintAttackRange.setStyle(Paint.Style.FILL);
    
    paintHitEffect.setColor(0xFFFFFFFF);
    paintHitEffect.setStyle(Paint.Style.FILL);
    paintHitGlow.setColor(0xFFFFD700);
    paintHitGlow.setStyle(Paint.Style.FILL);
    
    paintCode.setColor(0xFF00FF00);
    paintCode.setStyle(Paint.Style.FILL);
    paintCode.setTextSize(24f);
    paintCode.setAntiAlias(true);
    
    paintBug.setColor(0xFFFF0000);
    paintBug.setStyle(Paint.Style.FILL);
    paintBug.setTextSize(20f);
    paintBug.setAntiAlias(true);
    
    paintDebug.setColor(0xFFFFFF00);
    paintDebug.setStyle(Paint.Style.FILL);
    paintDebug.setTextSize(22f);
    paintDebug.setAntiAlias(true);
    
    paintRequirement.setColor(0xFFFF6B9D);
    paintRequirement.setStyle(Paint.Style.FILL);
    paintRequirement.setTextSize(18f);
    paintRequirement.setAntiAlias(true);
    
    paintTest.setColor(0xFF9B7CFF);
    paintTest.setStyle(Paint.Style.FILL);
    paintTest.setTextSize(20f);
    paintTest.setAntiAlias(true);
    
    paintHpBarBg.setColor(0xFF000000);
    paintHpBarBg.setStyle(Paint.Style.FILL);
    paintHpBarFill.setColor(0xFFFF4D6D);
    paintHpBarFill.setStyle(Paint.Style.FILL);
    paintHpBarOutline.setColor(0xFFFFFFFF);
    paintHpBarOutline.setStyle(Paint.Style.STROKE);
    paintHpBarOutline.setStrokeWidth(2f);
    
    paintCooldownOverlay.setColor(0xCC000000);
    paintCooldownOverlay.setStyle(Paint.Style.FILL);
    paintCooldownText.setColor(0xFFFFFFFF);
    paintCooldownText.setTextSize(18f);
    paintCooldownText.setTextAlign(Paint.Align.CENTER);
    paintCooldownText.setAntiAlias(true);
    
    paintFloorPattern.setColor(0x33000000);
    paintFloorPattern.setStyle(Paint.Style.FILL);
    paintBgPattern.setColor(0x22000000);
    paintBgPattern.setStyle(Paint.Style.FILL);
    
    paintCooldownOverlay.setColor(0xCC000000);
    paintCooldownOverlay.setStyle(Paint.Style.FILL);
    paintCooldownText.setColor(0xFFFFFFFF);
    paintCooldownText.setTextSize(18f);
    paintCooldownText.setTextAlign(Paint.Align.CENTER);
    paintCooldownText.setAntiAlias(true);
    
    paintFloorPattern.setColor(0x33000000);
    paintFloorPattern.setStyle(Paint.Style.FILL);
    paintBgPattern.setColor(0x22000000);
    paintBgPattern.setStyle(Paint.Style.FILL);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setControlState(Control control, boolean pressed) {
    switch (control) {
      case LEFT:
        input.left = pressed;
        break;
      case RIGHT:
        input.right = pressed;
        break;
      case JUMP:
        input.jump = pressed;
        break;
      case LIGHT:
        input.light = pressed;
        break;
      case HEAVY:
        input.heavy = pressed;
        break;
      case KICK:
        input.kick = pressed;
        break;
      case GUARD:
        input.guard = pressed;
        break;
      case DASH:
        input.dash = pressed;
        break;
      case SPECIAL:
        input.special = pressed;
        break;
      case SHOOT:
        input.shoot = pressed;
        break;
    }
  }

  public void startGame() {
    resetGame();
    setState(GameState.PLAYING);
  }

  public void pauseGame() {
    if (state == GameState.PLAYING) {
      setState(GameState.PAUSED);
    }
  }

  public void resumeGame() {
    if (state == GameState.PAUSED) {
      setState(GameState.PLAYING);
    }
  }

  public void restartGame() {
    resetGame();
    setState(GameState.PLAYING);
  }

  public void returnToMenu() {
    setState(GameState.MENU);
  }

  public void toggleMute() {
    audioController.setMuted(!audioController.isMuted());
  }

  private void resetGame() {
    enemies.clear();
    projectiles.clear();
    pickups.clear();
    stageManager.reset();
    score = 0;
    energyGainBoostTimer = 0f;
    energyGainDebuffTimer = 0f;
    float size = Math.max(40f, getWidth() * 0.06f);
    player = new Player(getWidth() * 0.15f, groundY - size, size, size * 1.2f);
    spawnWave();
    updateHud(true);
  }

  private void spawnWave() {
    StageManager.StageWave wave = stageManager.getCurrentWave();
    for (int i = 0; i < wave.internCount; i++) {
      spawnEnemy(Enemy.EnemyType.INTERN, false);
    }
    for (int i = 0; i < wave.pmCount; i++) {
      spawnEnemy(Enemy.EnemyType.PM, false);
    }
    for (int i = 0; i < wave.qaCount; i++) {
      spawnEnemy(Enemy.EnemyType.QA, false);
    }
    if (wave.elite) {
      if (stageManager.isBossWave()) {
        spawnEnemy(Enemy.EnemyType.BOSS, true);
      } else {
        spawnEnemy(Enemy.EnemyType.INTERN, true);
      }
    }
    if (random.nextFloat() < 0.6f) {
      spawnPickup(Pickup.PickupType.COFFEE);
    }
    if (random.nextFloat() < 0.4f) {
      spawnPickup(Pickup.PickupType.PAPER);
    }
  }

  private void spawnEnemy(Enemy.EnemyType type, boolean elite) {
    float size = Math.max(38f, getWidth() * 0.05f);
    float x = worldOffset + getWidth() * 1.2f + random.nextFloat() * getWidth() * 0.3f;
    float y = groundY - size * 1.2f;
    float hp = type == Enemy.EnemyType.BOSS ? 260f : 70f;
    if (elite && type != Enemy.EnemyType.BOSS) {
      hp = 120f;
    }
    Enemy enemy = new Enemy(x, y, size, size * 1.2f, hp, type, elite);
    enemies.add(enemy);
  }

  private void spawnPickup(Pickup.PickupType type) {
    float size = Math.max(26f, getWidth() * 0.035f);
    float x = worldOffset + getWidth() * 0.3f + random.nextFloat() * getWidth() * 0.4f;
    float y = groundY - size;
    pickups.add(new Pickup(x, y, size, type));
  }

  private void setState(GameState next) {
    if (state == next) {
      return;
    }
    state = next;
    if (listener != null) {
      listener.onStateChanged(state);
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    groundY = getHeight() * 0.82f;
    running = true;
    gameThread = new Thread(this);
    gameThread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    groundY = height * 0.82f;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    running = false;
    boolean retry = true;
    while (retry) {
      try {
        if (gameThread != null) {
          gameThread.join();
        }
        retry = false;
      } catch (InterruptedException ignored) {
      }
    }
    audioController.release();
  }

  @Override
  public void run() {
    long lastTime = System.nanoTime();
    float accumulator = 0f;
    float fixedStep = 1f / 60f;
    while (running) {
      long now = System.nanoTime();
      float delta = (now - lastTime) / 1000000000f;
      lastTime = now;
      if (delta > 0.25f) {
        delta = 0.25f;
      }
      accumulator += delta;
      while (accumulator >= fixedStep) {
        updateGame(fixedStep);
        accumulator -= fixedStep;
      }
      render();
    }
  }

  private void updateGame(float dt) {
    if (state != GameState.PLAYING) {
      return;
    }
    if (player == null) {
      return;
    }
    if (energyGainBoostTimer > 0f) {
      energyGainBoostTimer -= dt;
    }
    if (energyGainDebuffTimer > 0f) {
      energyGainDebuffTimer -= dt;
    }
    if (energyGainBoostTimer > 0f) {
      player.energyGainMultiplier = 1.5f;
    } else if (energyGainDebuffTimer > 0f) {
      player.energyGainMultiplier = 0.6f;
    } else {
      player.energyGainMultiplier = 1f;
    }

    if (input.shoot && player.shootCooldown <= 0f) {
      float projX = player.x + player.w * 0.5f;
      float projY = player.y + player.h * 0.4f;
      float projVx = player.facing * 500f;
      projectiles.add(new Projectile(projX, projY, 12f, 6f, projVx, 15f, false));
      player.shootCooldown = 0.4f;
      audioController.playShoot();
      for (int i = 0; i < 2; i++) {
        codeEffects.add(new CodeEffect(projX, projY, random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 60f));
      }
    }
    
    boolean hasEnemiesVeryClose = false;
    for (Enemy enemy : enemies) {
      if (enemy.alive) {
        float distance = enemy.x - player.x;
        if (distance > 0f && distance < getWidth() * 0.2f) {
          hasEnemiesVeryClose = true;
          break;
        }
      }
    }
    
    float playerSpeed = 0f;
    boolean canMoveForward = !hasEnemiesVeryClose;
    
    if (input.right && canMoveForward) {
      playerSpeed = 260f;
      worldOffset += playerSpeed * dt;
    } else if (input.left) {
      playerSpeed = -260f;
      worldOffset += playerSpeed * dt;
      if (worldOffset < 0f) {
        worldOffset = 0f;
        playerSpeed = 0f;
      }
    }
    
    GameInput moveInput = new GameInput();
    moveInput.left = input.left && worldOffset > 0f;
    moveInput.right = input.right && canMoveForward;
    moveInput.jump = input.jump;
    moveInput.light = input.light;
    moveInput.heavy = input.heavy;
    moveInput.kick = input.kick;
    moveInput.guard = input.guard;
    moveInput.dash = input.dash;
    moveInput.special = input.special;
    
    player.update(moveInput, dt, groundY, getWidth());
    cameraX = player.x - getWidth() * 0.15f;
    
    updateEnemies(dt);
    updateProjectiles(dt);
    updatePickups();
    updateHitEffects(dt);
    updateCodeEffects(dt);
    updateBugEffects(dt);
    screenShake.update(dt);
    resolvePlayerAttacks();
    resolveEnemyAttacks();
    resolveProjectileHits();
    resolvePickups();

    Iterator<Enemy> enemyIterator = enemies.iterator();
    while (enemyIterator.hasNext()) {
      Enemy enemy = enemyIterator.next();
      if (enemy.x + enemy.w < cameraX - getWidth() * 0.2f) {
        enemyIterator.remove();
      }
    }
    
    if (enemies.isEmpty() && state == GameState.PLAYING) {
      if (stageManager.advanceWave()) {
        spawnWave();
      } else {
        setState(GameState.GAME_OVER);
      }
    }

    input.resetActions();
    updateHud(false);

    if (player.hp <= 0f) {
      setState(GameState.GAME_OVER);
    }
  }

  private void updateEnemies(float dt) {
    Iterator<Enemy> iterator = enemies.iterator();
    while (iterator.hasNext()) {
      Enemy enemy = iterator.next();
      if (!enemy.alive) {
        iterator.remove();
        continue;
      }
      enemy.update(dt, player, groundY);
      if (enemy.type == Enemy.EnemyType.BOSS) {
        enemy.aiTimer -= dt;
        if (enemy.aiTimer <= 0f) {
          enemy.aiTimer = 2.5f;
          enemy.vx = player.x > enemy.x ? 420f : -420f;
        }
      }
    }
  }

  private void updateProjectiles(float dt) {
    Iterator<Projectile> iterator = projectiles.iterator();
    while (iterator.hasNext()) {
      Projectile projectile = iterator.next();
      projectile.update(dt);
      if (projectile.x < cameraX - 200f || projectile.x > cameraX + getWidth() + 200f) {
        iterator.remove();
      }
    }
  }

  private void updatePickups() {
    for (Pickup pickup : pickups) {
      pickup.vy += 1200f / 60f;
      pickup.y += pickup.vy * (1f / 60f);
      if (pickup.y + pickup.h > groundY) {
        pickup.y = groundY - pickup.h;
        pickup.vy = 0f;
      }
    }
  }

  private void updateHitEffects(float dt) {
    Iterator<HitEffect> iterator = hitEffects.iterator();
    while (iterator.hasNext()) {
      HitEffect effect = iterator.next();
      effect.timer -= dt;
      if (effect.timer <= 0f) {
        iterator.remove();
      } else {
        effect.y -= 80f * dt;
        effect.scale += 120f * dt;
      }
    }
  }

  private void updateCodeEffects(float dt) {
    Iterator<CodeEffect> iterator = codeEffects.iterator();
    while (iterator.hasNext()) {
      CodeEffect effect = iterator.next();
      effect.timer -= dt;
      if (effect.timer <= 0f) {
        iterator.remove();
      } else {
        effect.y -= 60f * dt;
        effect.x += effect.vx * dt;
        effect.rotation += 90f * dt;
      }
    }
  }

  private void updateBugEffects(float dt) {
    Iterator<BugEffect> iterator = bugEffects.iterator();
    while (iterator.hasNext()) {
      BugEffect effect = iterator.next();
      effect.timer -= dt;
      if (effect.timer <= 0f) {
        iterator.remove();
      } else {
        effect.y -= 50f * dt;
        effect.scale += 100f * dt;
        effect.rotation += 180f * dt;
      }
    }
  }

  private void resolvePlayerAttacks() {
    RectF attackRect = player.getAttackRect(rectA);
    if (attackRect == null) {
      return;
    }
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      if (RectF.intersects(attackRect, enemy.getRect(rectB))) {
        if (player.isInAttackActive()) {
        enemy.damage(player.attackDamage);
        player.addEnergy(8f);
        player.combo++;
        player.comboTimer = 2f;
        score += Math.max(10, player.combo * 5);
          
          float hitX = enemy.x + enemy.w * 0.5f;
          float hitY = enemy.y + enemy.h * 0.3f;
          hitEffects.add(new HitEffect(hitX, hitY, player.attackType == Player.AttackType.SPECIAL));
          
          if (player.attackType == Player.AttackType.LIGHT) {
            for (int i = 0; i < 3; i++) {
              codeEffects.add(new CodeEffect(hitX + (random.nextFloat() - 0.5f) * 40f, hitY + (random.nextFloat() - 0.5f) * 30f, 
                  random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 100f));
            }
          } else if (player.attackType == Player.AttackType.HEAVY) {
            bugEffects.add(new BugEffect(hitX, hitY, 0f));
            for (int i = 0; i < 5; i++) {
              codeEffects.add(new CodeEffect(hitX + (random.nextFloat() - 0.5f) * 60f, hitY + (random.nextFloat() - 0.5f) * 40f, 
                  random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 150f));
            }
          } else if (player.attackType == Player.AttackType.SPECIAL) {
            for (int i = 0; i < 12; i++) {
              float angle = (float)Math.toRadians(i * 30f);
              codeEffects.add(new CodeEffect(hitX, hitY, i * 30f, (float)Math.cos(angle) * 200f));
            }
            bugEffects.add(new BugEffect(hitX, hitY, 0f));
          }
          
          float shakeIntensity = 6f;
          float shakeDuration = 0.15f;
          if (player.attackType == Player.AttackType.HEAVY) {
            shakeIntensity = 10f;
            shakeDuration = 0.2f;
          } else if (player.attackType == Player.AttackType.SPECIAL) {
            shakeIntensity = 15f;
            shakeDuration = 0.25f;
          }
          screenShake.trigger(shakeDuration, shakeIntensity);
          
          enemy.knockbackX = player.facing * (player.attackType == Player.AttackType.HEAVY ? 300f : player.attackType == Player.AttackType.SPECIAL ? 250f : 150f);
          
        if (player.attackType == Player.AttackType.KICK) {
          enemy.vy = -420f;
          enemy.applyStun(0.4f);
        }
        if (player.attackType == Player.AttackType.SPECIAL) {
          enemy.applyStun(0.6f);
          audioController.playSpecial();
        } else {
          audioController.playHit();
          }
        }
      }
    }
    Iterator<Pickup> pickupIterator = pickups.iterator();
    while (pickupIterator.hasNext()) {
      Pickup pickup = pickupIterator.next();
      if (pickup.type == Pickup.PickupType.PAPER && RectF.intersects(attackRect, pickup.getRect(rectB))) {
        for (Enemy enemy : enemies) {
          if (Math.abs(enemy.x - pickup.x) < getWidth() * 0.2f) {
            enemy.applyStun(0.8f);
          }
        }
        pickupIterator.remove();
      }
    }
  }

  private void resolveEnemyAttacks() {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      if (!enemy.canAttack()) {
        continue;
      }
      float distance = Math.abs(enemy.x - player.x);
      float attackRange = getWidth() * 0.3f;
      
      if (enemy.type == Enemy.EnemyType.INTERN) {
        if (distance < attackRange && RectF.intersects(enemy.getRect(rectA), player.getRect(rectB))) {
          float damage = 10f;
          player.takeDamage(damage);
          screenShake.trigger(0.15f, 6f);
          enemy.triggerAttackCooldown(1.2f);
        }
      } else if (enemy.type == Enemy.EnemyType.BOSS) {
        if (distance < getWidth() * 0.2f) {
          if (RectF.intersects(enemy.getRect(rectA), player.getRect(rectB))) {
            player.takeDamage(18f);
            screenShake.trigger(0.25f, 12f);
            enemy.triggerAttackCooldown(1.5f);
          } else {
            player.takeDamage(16f);
            bugEffects.add(new BugEffect(enemy.x + enemy.w * 0.5f, enemy.y + enemy.h * 0.3f, 0f));
            enemy.triggerAttackCooldown(2f);
          }
        }
      } else if (enemy.type == Enemy.EnemyType.PM) {
        if (distance < getWidth() * 0.6f) {
          float dir = player.x > enemy.x ? 1f : -1f;
          float projX = enemy.x + enemy.w * 0.5f;
          float projY = enemy.y + enemy.h * 0.4f;
          projectiles.add(new Projectile(projX, projY, 20f, 10f, dir * 420f, 12f, true));
          for (int i = 0; i < 3; i++) {
            codeEffects.add(new CodeEffect(projX, projY, random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 80f));
          }
          enemy.triggerAttackCooldown(2f);
        }
      } else if (enemy.type == Enemy.EnemyType.QA) {
        if (distance < getWidth() * 0.5f) {
          player.energyGainMultiplier = 0.6f;
          energyGainDebuffTimer = 4f;
          float effectX = enemy.x + enemy.w * 0.5f;
          float effectY = enemy.y + enemy.h * 0.3f;
          bugEffects.add(new BugEffect(effectX, effectY, 0f));
          for (int i = 0; i < 5; i++) {
            float angle = (float)Math.toRadians(i * 72f);
            codeEffects.add(new CodeEffect(effectX, effectY, i * 72f, (float)Math.cos(angle) * 100f));
          }
          enemy.triggerAttackCooldown(2.4f);
        }
      }
    }
  }

  private void resolveProjectileHits() {
    Iterator<Projectile> iterator = projectiles.iterator();
    while (iterator.hasNext()) {
      Projectile projectile = iterator.next();
      if (projectile.fromEnemy && RectF.intersects(projectile.getRect(rectA), player.getRect(rectB))) {
        player.takeDamage(projectile.damage);
        iterator.remove();
      } else if (!projectile.fromEnemy) {
        for (Enemy enemy : enemies) {
          if (!enemy.alive) {
            continue;
          }
          if (RectF.intersects(projectile.getRect(rectA), enemy.getRect(rectB))) {
            enemy.damage(projectile.damage);
            hitEffects.add(new HitEffect(enemy.x + enemy.w * 0.5f, enemy.y + enemy.h * 0.3f, false));
            iterator.remove();
            break;
          }
        }
      }
    }
  }

  private void resolvePickups() {
    Iterator<Pickup> iterator = pickups.iterator();
    while (iterator.hasNext()) {
      Pickup pickup = iterator.next();
      if (RectF.intersects(pickup.getRect(rectA), player.getRect(rectB))) {
        if (pickup.type == Pickup.PickupType.COFFEE) {
          if (player.hp < player.maxHp) {
            player.heal(15f);
          } else {
            energyGainBoostTimer = 5f;
          }
        }
        iterator.remove();
      }
    }
  }

  private void render() {
    SurfaceHolder holder = getHolder();
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    
    float shakeX = (random.nextFloat() - 0.5f) * screenShake.getIntensity();
    float shakeY = (random.nextFloat() - 0.5f) * screenShake.getIntensity();
    canvas.translate(shakeX, shakeY);
    
    float renderOffsetX = -cameraX;
    canvas.translate(renderOffsetX, 0f);
    
    renderBackground(canvas);
    renderFloor(canvas);
    
    renderPickups(canvas);
    renderProjectiles(canvas);
    renderEnemies(canvas);
    renderPlayer(canvas);
    renderAttackRange(canvas);
    renderHitEffects(canvas);
    renderCodeEffects(canvas);
    renderBugEffects(canvas);
    
    canvas.translate(-renderOffsetX, 0f);
    canvas.translate(-shakeX, -shakeY);
    holder.unlockCanvasAndPost(canvas);
  }

  private void renderBackground(Canvas canvas) {
    canvas.drawRect(cameraX, 0f, cameraX + getWidth(), getHeight(), paintBackground);
    
    float patternSize = 60f;
    for (float x = cameraX - (cameraX % patternSize); x < cameraX + getWidth(); x += patternSize) {
      for (float y = 0f; y < getHeight(); y += patternSize) {
        if ((int)(x / patternSize + y / patternSize) % 2 == 0) {
          canvas.drawRect(x, y, x + patternSize, y + patternSize, paintBgPattern);
        }
      }
    }
    
    paintBgPattern.setColor(0x11000000);
    for (int i = 0; i < 15; i++) {
      float x = cameraX + (i * 0.15f + (cameraX * 0.001f) % 1f) * getWidth();
      float y = 50f + i * 40f + (float)Math.sin(cameraX * 0.002f + i) * 20f;
      canvas.drawCircle(x, y, 3f + i % 3, paintBgPattern);
    }
    
    paintBgPattern.setColor(0x08000000);
    for (int i = 0; i < 8; i++) {
      float x = cameraX + (i * 0.25f + (cameraX * 0.0005f) % 1f) * getWidth();
      float y = 100f + i * 60f + (float)Math.cos(cameraX * 0.0015f + i) * 30f;
      float size = 8f + i % 4;
      canvas.drawCircle(x, y, size, paintBgPattern);
    }
    
    paintBgPattern.setColor(0x15000000);
    for (int i = 0; i < 12; i++) {
      float x = cameraX + (i * 0.2f + (cameraX * 0.0003f) % 1f) * getWidth();
      float y = 30f + i * 50f;
      canvas.drawRect(x - 2f, y - 1f, x + 2f, y + 1f, paintBgPattern);
    }
  }

  private void renderFloor(Canvas canvas) {
    canvas.drawRect(cameraX, groundY, cameraX + getWidth(), getHeight(), paintFloor);
    
    paintFloorPattern.setColor(0x44000000);
    paintFloorPattern.setStrokeWidth(2f);
    paintFloorPattern.setStyle(Paint.Style.STROKE);
    float lineSpacing = 40f;
    for (float x = cameraX - (cameraX % lineSpacing); x < cameraX + getWidth(); x += lineSpacing) {
      canvas.drawLine(x, groundY, x, getHeight(), paintFloorPattern);
    }
    
    paintFloorPattern.setStyle(Paint.Style.FILL);
    paintFloorPattern.setColor(0x22FFFFFF);
    for (int i = 0; i < 25; i++) {
      float x = cameraX + (i * 0.12f + (cameraX * 0.0008f) % 1f) * getWidth();
      float y = groundY + 8f + (i % 5) * 15f;
      float size = 2f + (i % 3);
      canvas.drawCircle(x, y, size, paintFloorPattern);
    }
    
    paintFloorPattern.setColor(0x33FFFFFF);
    float highlightY = groundY + 2f;
    canvas.drawRect(cameraX, highlightY, cameraX + getWidth(), highlightY + 1f, paintFloorPattern);
    
    paintFloorPattern.setColor(0x1AFFFFFF);
    for (int i = 0; i < 15; i++) {
      float x = cameraX + (i * 0.18f + (cameraX * 0.0006f) % 1f) * getWidth();
      float y = groundY + 5f + (i % 3) * 20f;
      canvas.drawRect(x - 1.5f, y - 1.5f, x + 1.5f, y + 1.5f, paintFloorPattern);
    }
    
    paintFloorPattern.setColor(0x2A000000);
    paintFloorPattern.setStrokeWidth(1f);
    paintFloorPattern.setStyle(Paint.Style.STROKE);
    for (int i = 0; i < 10; i++) {
      float x = cameraX + (i * 0.3f + (cameraX * 0.0004f) % 1f) * getWidth();
      float y = groundY + 10f + (i % 2) * 25f;
      canvas.drawRect(x - 4f, y - 2f, x + 4f, y + 2f, paintFloorPattern);
    }
  }

  private void renderPlayer(Canvas canvas) {
    if (player == null) {
      return;
    }
    RectF rect = player.getRect(rectA);
    float centerX = rect.centerX();
    float baseY = rect.bottom;
    float unit = Math.min(rect.width(), rect.height()) * 0.5f;
    
    canvas.save();
    canvas.translate(centerX, baseY);
    if (player.facing < 0) {
      canvas.scale(-1f, 1f);
    }
    
    float shadowOffset = 3f;
    canvas.drawOval(-unit * 1.1f, shadowOffset, unit * 1.1f, shadowOffset + 4f, paintPlayerShadow);
    
    float bodyY = -rect.height() * 0.5f;
    float headRadius = unit * 0.35f;
    float headY = bodyY - unit * 0.6f;
    
    float idleOffset = (float)Math.sin(player.idleBob) * 2f;
    float walkOffset = (float)Math.sin(player.walkCycle * (float)Math.PI) * 3f;
    float verticalOffset = player.vx != 0f ? walkOffset : idleOffset;
    
    boolean isAttacking = player.attackTimer > 0f;
    float attackProgress = player.getAttackProgress();
    
    if (player.guarding) {
      renderPlayerGuarding(canvas, bodyY + verticalOffset, headY + verticalOffset, headRadius, unit);
    } else if (isAttacking) {
      renderPlayerAttacking(canvas, bodyY + verticalOffset, headY + verticalOffset, headRadius, unit, attackProgress);
    } else if (player.vx != 0f) {
      renderPlayerWalking(canvas, bodyY + verticalOffset, headY + verticalOffset, headRadius, unit);
    } else {
      renderPlayerIdle(canvas, bodyY + verticalOffset, headY + verticalOffset, headRadius, unit);
    }
    
    canvas.restore();
    
    renderPlayerHpBar(canvas, centerX, rect.top - unit * 0.3f, unit);
    
    if (isAttacking && player.isInAttackActive()) {
      paintAttackRange.setAlpha((int)(150 * (1f - Math.abs(attackProgress - 0.5f) * 2f)));
      RectF attackRect = player.getAttackRect(rectB);
      if (attackRect != null) {
        canvas.drawRoundRect(attackRect, 8f, 8f, paintAttackRange);
      }
    }
  }

  private void renderPlayerHpBar(Canvas canvas, float centerX, float topY, float unit) {
    float hpRatio = player.hp / player.maxHp;
    float barWidth = unit * 1.2f;
    float barHeight = 6f;
    float barX = centerX - barWidth * 0.5f;
    float barY = topY - barHeight;
    
    RectF barRect = new RectF(barX, barY, barX + barWidth, barY + barHeight);
    canvas.drawRoundRect(barRect, 3f, 3f, paintHpBarBg);
    
    if (hpRatio > 0f) {
      paintHpBarFill.setColor(0xFF43F7A0);
      RectF fillRect = new RectF(barX, barY, barX + barWidth * hpRatio, barY + barHeight);
      canvas.drawRoundRect(fillRect, 3f, 3f, paintHpBarFill);
    }
    
    canvas.drawRoundRect(barRect, 3f, 3f, paintHpBarOutline);
  }

  private void renderPlayerIdle(Canvas canvas, float bodyY, float headY, float headRadius, float unit) {
    paintPlayer.setAlpha(255);
    paintPlayerOutline.setStrokeWidth(3f);
    
    canvas.drawCircle(0f, headY, headRadius, paintPlayer);
    canvas.drawCircle(0f, headY, headRadius, paintPlayerOutline);
    
    float bodyTop = headY + headRadius + unit * 0.15f;
    float bodyBottom = bodyY + unit * 0.3f;
    float bodyWidth = unit * 0.5f;
    RectF body = new RectF(-bodyWidth * 0.5f, bodyTop, bodyWidth * 0.5f, bodyBottom);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayer);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayerOutline);
    
    float armY = bodyTop + unit * 0.15f;
    float armLength = unit * 0.4f;
    float armWidth = unit * 0.12f;
    float armSwing = (float)Math.sin(player.idleBob) * 8f;
    
    RectF leftArm = new RectF(-bodyWidth * 0.5f - armWidth * 0.5f, armY + armSwing, -bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength + armSwing);
    RectF rightArm = new RectF(bodyWidth * 0.5f - armWidth * 0.5f, armY - armSwing, bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength - armSwing);
    canvas.drawRoundRect(leftArm, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(rightArm, 4f, 4f, paintPlayer);
    
    float legY = bodyBottom;
    float legLength = unit * 0.5f;
    float legWidth = unit * 0.14f;
    float legSpread = unit * 0.2f;
    
    RectF leftLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength);
    RectF rightLeg = new RectF(legSpread - legWidth * 0.5f, legY, legSpread + legWidth * 0.5f, legY + legLength);
    canvas.drawRoundRect(leftLeg, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(rightLeg, 4f, 4f, paintPlayer);
  }

  private void renderPlayerWalking(Canvas canvas, float bodyY, float headY, float headRadius, float unit) {
    paintPlayer.setAlpha(255);
    paintPlayerOutline.setStrokeWidth(3f);
    
    float cycle = player.walkCycle % 2f;
    float legPhase = cycle < 1f ? cycle : 2f - cycle;
    
    canvas.drawCircle(0f, headY, headRadius, paintPlayer);
    canvas.drawCircle(0f, headY, headRadius, paintPlayerOutline);
    
    float bodyTop = headY + headRadius + unit * 0.15f;
    float bodyBottom = bodyY + unit * 0.3f;
    float bodyWidth = unit * 0.5f;
    RectF body = new RectF(-bodyWidth * 0.5f, bodyTop, bodyWidth * 0.5f, bodyBottom);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayer);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayerOutline);
    
    float armY = bodyTop + unit * 0.15f;
    float armLength = unit * 0.4f;
    float armWidth = unit * 0.12f;
    float armSwing = (float)Math.sin(player.walkCycle * (float)Math.PI) * 15f;
    
    RectF leftArm = new RectF(-bodyWidth * 0.5f - armWidth * 0.5f, armY + armSwing, -bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength + armSwing);
    RectF rightArm = new RectF(bodyWidth * 0.5f - armWidth * 0.5f, armY - armSwing, bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength - armSwing);
    canvas.drawRoundRect(leftArm, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(rightArm, 4f, 4f, paintPlayer);
    
    float legY = bodyBottom;
    float legLength = unit * 0.5f;
    float legWidth = unit * 0.14f;
    float legSpread = unit * 0.2f;
    float legBend = legPhase * 25f;
    
    RectF leftLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength - legBend);
    RectF rightLeg = new RectF(legSpread - legWidth * 0.5f, legY, legSpread + legWidth * 0.5f, legY + legLength + legBend);
    canvas.drawRoundRect(leftLeg, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(rightLeg, 4f, 4f, paintPlayer);
  }

  private void renderPlayerAttacking(Canvas canvas, float bodyY, float headY, float headRadius, float unit, float progress) {
    paintPlayer.setAlpha(255);
    paintPlayerOutline.setStrokeWidth(3f);
    
    canvas.drawCircle(0f, headY, headRadius, paintPlayer);
    canvas.drawCircle(0f, headY, headRadius, paintPlayerOutline);
    
    float bodyTop = headY + headRadius + unit * 0.15f;
    float bodyBottom = bodyY + unit * 0.3f;
    float bodyWidth = unit * 0.5f;
    
    float bodyLean = 0f;
    float armAngle = 0f;
    float armExtension = 0f;
    float legStance = 0f;
    
    if (player.attackType == Player.AttackType.LIGHT) {
      if (progress < 0.3f) {
        bodyLean = progress / 0.3f * 10f;
        armAngle = -progress / 0.3f * 45f;
      } else if (progress < 0.7f) {
        float activeProgress = (progress - 0.3f) / 0.4f;
        bodyLean = 10f - activeProgress * 15f;
        armAngle = -45f + activeProgress * 90f;
        armExtension = activeProgress * unit * 0.6f;
      } else {
        float recoveryProgress = (progress - 0.7f) / 0.3f;
        bodyLean = -5f + recoveryProgress * 5f;
        armAngle = 45f - recoveryProgress * 45f;
        armExtension = unit * 0.6f - recoveryProgress * unit * 0.6f;
      }
    } else if (player.attackType == Player.AttackType.HEAVY) {
      if (progress < 0.3f) {
        bodyLean = progress / 0.3f * 20f;
        armAngle = -progress / 0.3f * 80f;
        legStance = progress / 0.3f * unit * 0.15f;
      } else if (progress < 0.7f) {
        float activeProgress = (progress - 0.3f) / 0.4f;
        bodyLean = 20f - activeProgress * 25f;
        armAngle = -80f + activeProgress * 120f;
        armExtension = activeProgress * unit * 0.8f;
        legStance = unit * 0.15f - activeProgress * unit * 0.1f;
      } else {
        float recoveryProgress = (progress - 0.7f) / 0.3f;
        bodyLean = -5f + recoveryProgress * 5f;
        armAngle = 40f - recoveryProgress * 40f;
        armExtension = unit * 0.8f - recoveryProgress * unit * 0.8f;
        legStance = unit * 0.05f - recoveryProgress * unit * 0.05f;
      }
    } else if (player.attackType == Player.AttackType.KICK) {
      if (progress < 0.3f) {
        bodyLean = progress / 0.3f * 15f;
        legStance = progress / 0.3f * unit * 0.2f;
      } else if (progress < 0.7f) {
        float activeProgress = (progress - 0.3f) / 0.4f;
        bodyLean = 15f - activeProgress * 10f;
        legStance = unit * 0.2f;
      } else {
        float recoveryProgress = (progress - 0.7f) / 0.3f;
        bodyLean = 5f - recoveryProgress * 5f;
        legStance = unit * 0.2f - recoveryProgress * unit * 0.2f;
      }
    } else if (player.attackType == Player.AttackType.SPECIAL) {
      float pulse = (float)Math.sin(progress * (float)Math.PI * 4f) * 0.1f;
      bodyLean = progress * 10f;
      armAngle = -60f + progress * 120f;
      armExtension = progress * unit * 1.2f;
      paintPlayer.setAlpha((int)(255 * (1f + pulse)));
    }
    
    canvas.save();
    canvas.rotate(bodyLean, 0f, bodyTop);
    
    RectF body = new RectF(-bodyWidth * 0.5f, bodyTop, bodyWidth * 0.5f, bodyBottom);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayer);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayerOutline);
    
    float armY = bodyTop + unit * 0.15f;
    float armLength = unit * 0.4f + armExtension;
    float armWidth = unit * 0.12f;
    
    canvas.save();
    canvas.translate(bodyWidth * 0.5f, armY);
    canvas.rotate(armAngle);
    
    RectF attackArm = new RectF(0f, -armWidth * 0.5f, armLength, armWidth * 0.5f);
    canvas.drawRoundRect(attackArm, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(attackArm, 4f, 4f, paintPlayerOutline);
    
    if (player.isInAttackActive()) {
      paintHitGlow.setAlpha((int)(180 * (1f - Math.abs(progress - 0.5f) * 2f)));
      canvas.drawCircle(armLength * 0.8f, 0f, unit * 0.25f, paintHitGlow);
    }
    
    canvas.restore();
    
    float otherArmY = armY;
    float otherArmLength = unit * 0.4f;
    RectF otherArm = new RectF(-bodyWidth * 0.5f - armWidth * 0.5f, otherArmY, -bodyWidth * 0.5f + armWidth * 0.5f, otherArmY + otherArmLength);
    canvas.drawRoundRect(otherArm, 4f, 4f, paintPlayer);
    
    canvas.restore();
    
    float legY = bodyBottom;
    float legLength = unit * 0.5f;
    float legWidth = unit * 0.14f;
    float legSpread = unit * 0.2f + legStance;
    
    if (player.attackType == Player.AttackType.KICK && progress >= 0.3f && progress < 0.7f) {
      float kickProgress = (progress - 0.3f) / 0.4f;
      float kickAngle = kickProgress * 90f;
      float kickLength = unit * 0.6f;
      
      canvas.save();
      canvas.translate(legSpread, legY);
      canvas.rotate(kickAngle);
      RectF kickLeg = new RectF(0f, -legWidth * 0.5f, kickLength, legWidth * 0.5f);
      canvas.drawRoundRect(kickLeg, 4f, 4f, paintPlayer);
      canvas.drawRoundRect(kickLeg, 4f, 4f, paintPlayerOutline);
      canvas.restore();
      
      RectF supportLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength * 0.7f);
      canvas.drawRoundRect(supportLeg, 4f, 4f, paintPlayer);
    } else {
      RectF leftLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength);
      RectF rightLeg = new RectF(legSpread - legWidth * 0.5f, legY, legSpread + legWidth * 0.5f, legY + legLength);
      canvas.drawRoundRect(leftLeg, 4f, 4f, paintPlayer);
      canvas.drawRoundRect(rightLeg, 4f, 4f, paintPlayer);
    }
  }

  private void renderPlayerGuarding(Canvas canvas, float bodyY, float headY, float headRadius, float unit) {
    paintPlayer.setAlpha(180);
    paintPlayerOutline.setStrokeWidth(4f);
    
    canvas.drawCircle(0f, headY, headRadius, paintPlayer);
    canvas.drawCircle(0f, headY, headRadius, paintPlayerOutline);
    
    float bodyTop = headY + headRadius + unit * 0.15f;
    float bodyBottom = bodyY + unit * 0.3f;
    float bodyWidth = unit * 0.5f;
    RectF body = new RectF(-bodyWidth * 0.5f, bodyTop, bodyWidth * 0.5f, bodyBottom);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayer);
    canvas.drawRoundRect(body, 6f, 6f, paintPlayerOutline);
    
    float armY = bodyTop + unit * 0.15f;
    float armLength = unit * 0.5f;
    float armWidth = unit * 0.14f;
    
    RectF guardArm = new RectF(-bodyWidth * 0.3f - armWidth * 0.5f, armY, -bodyWidth * 0.3f + armWidth * 0.5f, armY + armLength);
    RectF otherArm = new RectF(bodyWidth * 0.5f - armWidth * 0.5f, armY, bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength * 0.8f);
    canvas.drawRoundRect(guardArm, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(guardArm, 4f, 4f, paintPlayerOutline);
    canvas.drawRoundRect(otherArm, 4f, 4f, paintPlayer);
    
    float legY = bodyBottom;
    float legLength = unit * 0.5f;
    float legWidth = unit * 0.14f;
    float legSpread = unit * 0.25f;
    
    RectF leftLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength);
    RectF rightLeg = new RectF(legSpread - legWidth * 0.5f, legY, legSpread + legWidth * 0.5f, legY + legLength);
    canvas.drawRoundRect(leftLeg, 4f, 4f, paintPlayer);
    canvas.drawRoundRect(rightLeg, 4f, 4f, paintPlayer);
  }

  private void renderEnemies(Canvas canvas) {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      RectF rect = enemy.getRect(rectA);
      float centerX = rect.centerX();
      float baseY = rect.bottom;
      float unit = Math.min(rect.width(), rect.height()) * 0.5f;
      
      renderEnemyHpBar(canvas, enemy, centerX, rect.top - unit * 0.3f, unit);
      
      canvas.save();
      canvas.translate(centerX, baseY);
      int enemyFacing = player != null && player.x > enemy.x ? 1 : -1;
      if (enemyFacing < 0) {
        canvas.scale(-1f, 1f);
      }
      
      float shadowOffset = 3f;
      canvas.drawOval(-unit * 1.1f, shadowOffset, unit * 1.1f, shadowOffset + 4f, paintPlayerShadow);
      
      boolean isElite = enemy.elite || enemy.type == Enemy.EnemyType.BOSS;
      Paint fillPaint = isElite ? paintEnemyElite : paintEnemy;
      Paint outlinePaint = isElite ? paintEnemyEliteOutline : paintEnemyOutline;
      
      if (enemy.hitFlashTimer > 0f) {
        float flashAlpha = (float)Math.sin(enemy.hitFlashTimer * (float)Math.PI * 20f) * 0.5f + 0.5f;
        fillPaint.setAlpha((int)(255 * (0.5f + flashAlpha * 0.5f)));
      } else if (enemy.stunTimer > 0f) {
        fillPaint.setAlpha(150);
      } else {
        fillPaint.setAlpha(255);
      }
      
      float bodyY = -rect.height() * 0.5f;
      float headRadius = unit * 0.35f;
      float headY = bodyY - unit * 0.6f;
      
      canvas.drawCircle(0f, headY, headRadius, fillPaint);
      canvas.drawCircle(0f, headY, headRadius, outlinePaint);
      
      float bodyTop = headY + headRadius + unit * 0.15f;
      float bodyBottom = bodyY + unit * 0.3f;
      float bodyWidth = unit * 0.5f;
      RectF body = new RectF(-bodyWidth * 0.5f, bodyTop, bodyWidth * 0.5f, bodyBottom);
      canvas.drawRoundRect(body, 6f, 6f, fillPaint);
      canvas.drawRoundRect(body, 6f, 6f, outlinePaint);
      
      float armY = bodyTop + unit * 0.15f;
      float armLength = unit * 0.4f;
      float armWidth = unit * 0.12f;
      
      RectF leftArm = new RectF(-bodyWidth * 0.5f - armWidth * 0.5f, armY, -bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength);
      RectF rightArm = new RectF(bodyWidth * 0.5f - armWidth * 0.5f, armY, bodyWidth * 0.5f + armWidth * 0.5f, armY + armLength);
      canvas.drawRoundRect(leftArm, 4f, 4f, fillPaint);
      canvas.drawRoundRect(rightArm, 4f, 4f, fillPaint);
      
      float legY = bodyBottom;
      float legLength = unit * 0.5f;
      float legWidth = unit * 0.14f;
      float legSpread = unit * 0.2f;
      
      RectF leftLeg = new RectF(-legSpread - legWidth * 0.5f, legY, -legSpread + legWidth * 0.5f, legY + legLength);
      RectF rightLeg = new RectF(legSpread - legWidth * 0.5f, legY, legSpread + legWidth * 0.5f, legY + legLength);
      canvas.drawRoundRect(leftLeg, 4f, 4f, fillPaint);
      canvas.drawRoundRect(rightLeg, 4f, 4f, fillPaint);
      
      String label = "";
      Paint labelPaint = paintRequirement;
      if (enemy.type == Enemy.EnemyType.INTERN) {
        label = "      ";
        labelPaint = paintEnemy;
      } else if (enemy.type == Enemy.EnemyType.PM) {
        label = "PM";
        labelPaint = paintRequirement;
      } else if (enemy.type == Enemy.EnemyType.QA) {
        label = "QA";
        labelPaint = paintTest;
      } else if (enemy.type == Enemy.EnemyType.BOSS) {
        label = "BOSS";
        labelPaint = paintEnemyElite;
      }
      
      labelPaint.setTextSize(unit * 0.5f);
      labelPaint.setAlpha(fillPaint.getAlpha());
      canvas.drawText(label, -labelPaint.measureText(label) * 0.5f, bodyY + unit * 0.2f, labelPaint);
      
      canvas.restore();
      
      if (enemy.type == Enemy.EnemyType.BOSS) {
        float glowRadius = unit * 1.3f;
        paintEnemyEliteOutline.setAlpha(100);
        canvas.drawCircle(centerX, rect.centerY(), glowRadius, paintEnemyEliteOutline);
        paintEnemyEliteOutline.setAlpha(255);
      }
      
      if (enemy.type == Enemy.EnemyType.PM && enemy.attackCooldown > 0f && enemy.attackCooldown < 0.5f) {
        float pulse = (float)Math.sin((0.5f - enemy.attackCooldown) * (float)Math.PI * 4f) * 0.3f + 0.7f;
        paintRequirement.setAlpha((int)(180 * pulse));
        canvas.drawCircle(centerX, rect.top - unit * 0.5f, unit * 0.4f * pulse, paintRequirement);
        paintRequirement.setTextSize(unit * 0.4f);
        canvas.drawText("      ", centerX - paintRequirement.measureText("      ") * 0.5f, rect.top - unit * 0.5f + unit * 0.15f, paintRequirement);
      }
      
      if (enemy.type == Enemy.EnemyType.QA && enemy.attackCooldown > 0f && enemy.attackCooldown < 0.6f) {
        float pulse = (float)Math.sin((0.6f - enemy.attackCooldown) * (float)Math.PI * 3f) * 0.3f + 0.7f;
        paintTest.setAlpha((int)(180 * pulse));
        for (int i = 0; i < 4; i++) {
          float angle = (float)Math.toRadians(i * 90f + (0.6f - enemy.attackCooldown) * 180f);
          float dist = unit * 0.8f * pulse;
          float x = centerX + (float)Math.cos(angle) * dist;
          float y = rect.centerY() + (float)Math.sin(angle) * dist;
          canvas.drawCircle(x, y, unit * 0.2f * pulse, paintTest);
        }
      }
    }
  }

  private void renderEnemyHpBar(Canvas canvas, Enemy enemy, float centerX, float topY, float unit) {
    float hpRatio = enemy.hp / enemy.maxHp;
    float barWidth = unit * 1.2f;
    float barHeight = 6f;
    float barX = centerX - barWidth * 0.5f;
    float barY = topY - barHeight;
    
    RectF barRect = new RectF(barX, barY, barX + barWidth, barY + barHeight);
    canvas.drawRoundRect(barRect, 3f, 3f, paintHpBarBg);
    
    if (hpRatio > 0f) {
      RectF fillRect = new RectF(barX, barY, barX + barWidth * hpRatio, barY + barHeight);
      canvas.drawRoundRect(fillRect, 3f, 3f, paintHpBarFill);
    }
    
    canvas.drawRoundRect(barRect, 3f, 3f, paintHpBarOutline);
  }

  private void renderProjectiles(Canvas canvas) {
    for (Projectile projectile : projectiles) {
      RectF rect = projectile.getRect(rectA);
      float centerX = rect.centerX();
      float centerY = rect.centerY();
      float radius = Math.min(rect.width(), rect.height()) * 0.5f;
      
      canvas.drawCircle(centerX, centerY, radius * 1.4f, paintProjectileGlow);
      canvas.drawCircle(centerX, centerY, radius, paintProjectile);
    }
  }

  private void renderPickups(Canvas canvas) {
    for (Pickup pickup : pickups) {
      RectF rect = pickup.getRect(rectA);
      float centerX = rect.centerX();
      float centerY = rect.centerY();
      float radius = Math.min(rect.width(), rect.height()) * 0.5f;
      
      float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.005f) * 0.1f + 1f);
      float glowRadius = radius * (1.2f + pulse * 0.3f);
      
      paintPickupGlow.setAlpha((int)(80 * (0.7f + pulse * 0.3f)));
      canvas.drawCircle(centerX, centerY, glowRadius, paintPickupGlow);
      canvas.drawCircle(centerX, centerY, radius, paintPickup);
    }
  }

  private void renderAttackRange(Canvas canvas) {
    if (player == null || player.attackTimer <= 0f) {
      return;
    }
    RectF attackRect = player.getAttackRect(rectB);
    if (attackRect != null) {
      float alpha = (player.attackTimer / player.attackDuration) * 150f;
      paintAttackRange.setAlpha((int)alpha);
      canvas.drawRoundRect(attackRect, 12f, 12f, paintAttackRange);
    }
  }

  private void renderHitEffects(Canvas canvas) {
    for (HitEffect effect : hitEffects) {
      float progress = 1f - (effect.timer / 0.3f);
      float alpha = progress * 255f;
      paintHitEffect.setAlpha((int)alpha);
      paintHitGlow.setAlpha((int)(alpha * 0.7f));
      
      float size = 12f + effect.scale * 0.5f;
      float pulse = (float)Math.sin(progress * (float)Math.PI * 3f) * 0.3f + 1f;
      
      paintHitGlow.setAlpha((int)(alpha * 0.5f));
      canvas.drawCircle(effect.x, effect.y, size * 1.8f * pulse, paintHitGlow);
      paintHitGlow.setAlpha((int)(alpha * 0.7f));
      canvas.drawCircle(effect.x, effect.y, size * 1.2f * pulse, paintHitGlow);
      canvas.drawCircle(effect.x, effect.y, size * pulse, paintHitEffect);
      
      if (effect.isSpecial) {
        float angle = progress * 360f;
        for (int i = 0; i < 12; i++) {
          float a = (float)Math.toRadians(angle + i * 30f);
          float dist = size * (1.5f + progress * 0.5f);
          float x = effect.x + (float)Math.cos(a) * dist;
          float y = effect.y + (float)Math.sin(a) * dist;
          float particleSize = size * (0.3f + progress * 0.2f);
          paintHitGlow.setAlpha((int)(alpha * (1f - progress)));
          canvas.drawCircle(x, y, particleSize, paintHitGlow);
        }
      } else {
        for (int i = 0; i < 6; i++) {
          float a = (float)Math.toRadians(progress * 180f + i * 60f);
          float dist = size * (0.8f + progress * 0.4f);
          float x = effect.x + (float)Math.cos(a) * dist;
          float y = effect.y + (float)Math.sin(a) * dist;
          paintHitGlow.setAlpha((int)(alpha * (1f - progress * 0.7f)));
          canvas.drawCircle(x, y, size * 0.25f, paintHitGlow);
        }
      }
    }
  }

  private void updateHud(boolean force) {
    hudTimer += 1f / 60f;
    if (force || hudTimer > 0.1f) {
      hudTimer = 0f;
      if (listener != null && player != null) {
        listener.onHudUpdate(score, (int) player.hp, (int) player.energy, Math.max(1, player.combo), stageManager.getStageNumber(), stageManager.getWaveNumber());
        listener.onCooldownUpdate(
          player.lightCooldown,
          player.heavyCooldown,
          player.kickCooldown,
          player.dashCooldownRemaining,
          player.shootCooldown,
          player.specialCooldown
        );
      }
    }
  }

  public interface Listener {
    void onHudUpdate(int score, int hp, int energy, int combo, int stage, int wave);
    void onStateChanged(GameState state);
    void onCooldownUpdate(float lightCd, float heavyCd, float kickCd, float dashCd, float shootCd, float specialCd);
  }

  public enum Control {
    LEFT,
    RIGHT,
    JUMP,
    LIGHT,
    HEAVY,
    KICK,
    GUARD,
    DASH,
    SPECIAL,
    SHOOT
  }

  private static class HitEffect {
    float x;
    float y;
    float timer;
    float scale;
    boolean isSpecial;

    HitEffect(float x, float y, boolean isSpecial) {
      this.x = x;
      this.y = y;
      this.timer = 0.3f;
      this.scale = 0f;
      this.isSpecial = isSpecial;
    }
  }

  private static class CodeEffect {
    float x;
    float y;
    float rotation;
    float vx;
    float timer;
    String code;

    CodeEffect(float x, float y, float rotation, float vx) {
      this.x = x;
      this.y = y;
      this.rotation = rotation;
      this.vx = vx;
      this.timer = 1.2f;
      String[] codes = {"if", "for", "void", "class", "return", "public", "private", "static", "final", "new"};
      this.code = codes[(int)(Math.random() * codes.length)];
    }
  }

  private static class BugEffect {
    float x;
    float y;
    float rotation;
    float scale;
    float timer;

    BugEffect(float x, float y, float rotation) {
      this.x = x;
      this.y = y;
      this.rotation = rotation;
      this.scale = 0f;
      this.timer = 1.5f;
    }
  }

  private void renderCodeEffects(Canvas canvas) {
    for (CodeEffect effect : codeEffects) {
      float alpha = (effect.timer / 1.2f) * 255f;
      paintCode.setAlpha((int)alpha);
      
      canvas.save();
      canvas.translate(effect.x, effect.y);
      canvas.rotate(effect.rotation);
      canvas.drawText(effect.code, -paintCode.measureText(effect.code) * 0.5f, 0f, paintCode);
      canvas.restore();
    }
  }

  private void renderBugEffects(Canvas canvas) {
    for (BugEffect effect : bugEffects) {
      float progress = 1f - (effect.timer / 1.5f);
      float alpha = progress * 255f;
      paintBug.setAlpha((int)alpha);
      
      effect.scale = progress * 40f;
      
      canvas.save();
      canvas.translate(effect.x, effect.y);
      canvas.rotate(effect.rotation);
      
      float size = 8f + effect.scale;
      canvas.drawCircle(0f, 0f, size, paintBug);
      paintBug.setTextSize(16f + effect.scale * 0.3f);
      canvas.drawText("BUG", -paintBug.measureText("BUG") * 0.5f, size * 0.3f, paintBug);
      
      for (int i = 0; i < 6; i++) {
        float angle = (float)Math.toRadians(i * 60f + effect.rotation);
        float dist = size * 1.2f;
        float px = (float)Math.cos(angle) * dist;
        float py = (float)Math.sin(angle) * dist;
        canvas.drawCircle(px, py, size * 0.3f, paintBug);
      }
      
      canvas.restore();
    }
  }
}
