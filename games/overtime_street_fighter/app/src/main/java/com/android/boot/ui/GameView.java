package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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

  private final Paint paintBackground = new Paint();
  private final Paint paintFloor = new Paint();
  private final Paint paintPlayer = new Paint();
  private final Paint paintEnemy = new Paint();
  private final Paint paintEnemyElite = new Paint();
  private final Paint paintProjectile = new Paint();
  private final Paint paintPickup = new Paint();

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    initPaints();
  }

  private void initPaints() {
    paintBackground.setColor(ContextCompat.getColor(getContext(), R.color.cst_bg_alt));
    paintFloor.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_floor));
    paintPlayer.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_player));
    paintEnemy.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_enemy));
    paintEnemyElite.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_enemy_elite));
    paintProjectile.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_projectile));
    paintPickup.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_pickup));
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
    float x = getWidth() * 0.7f + random.nextFloat() * getWidth() * 0.2f;
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
    float x = getWidth() * 0.3f + random.nextFloat() * getWidth() * 0.4f;
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

    player.update(input, dt, groundY, getWidth());
    updateEnemies(dt);
    updateProjectiles(dt);
    updatePickups();
    resolvePlayerAttacks();
    resolveEnemyAttacks();
    resolveProjectileHits();
    resolvePickups();

    input.resetActions();
    updateHud(false);

    if (player.hp <= 0f) {
      setState(GameState.GAME_OVER);
    }
    if (enemies.isEmpty() && state == GameState.PLAYING) {
      if (stageManager.advanceWave()) {
        spawnWave();
      } else {
        setState(GameState.GAME_OVER);
      }
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
      if (projectile.x < -100f || projectile.x > getWidth() + 100f) {
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
        enemy.damage(player.attackDamage);
        player.addEnergy(8f);
        player.combo++;
        player.comboTimer = 2f;
        score += Math.max(10, player.combo * 5);
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
      if (RectF.intersects(enemy.getRect(rectA), player.getRect(rectB))) {
        float damage = enemy.type == Enemy.EnemyType.BOSS ? 18f : 10f;
        player.takeDamage(damage);
        enemy.triggerAttackCooldown(1.2f);
      } else if (enemy.type == Enemy.EnemyType.BOSS) {
        if (Math.abs(enemy.x - player.x) < getWidth() * 0.15f) {
          player.takeDamage(16f);
          enemy.triggerAttackCooldown(2f);
        }
      } else if (enemy.type == Enemy.EnemyType.PM) {
        if (Math.abs(enemy.x - player.x) < getWidth() * 0.5f) {
          float dir = player.x > enemy.x ? 1f : -1f;
          projectiles.add(new Projectile(enemy.x + enemy.w * 0.5f, enemy.y + enemy.h * 0.4f, 16f, 8f, dir * 420f, 12f, true));
          enemy.triggerAttackCooldown(2f);
        }
      } else if (enemy.type == Enemy.EnemyType.QA) {
        if (Math.abs(enemy.x - player.x) < getWidth() * 0.4f) {
          player.energyGainMultiplier = 0.6f;
          energyGainDebuffTimer = 4f;
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
    canvas.drawRect(0f, 0f, getWidth(), getHeight(), paintBackground);
    canvas.drawRect(0f, groundY, getWidth(), getHeight(), paintFloor);
    if (player != null) {
      canvas.drawRect(player.getRect(rectA), paintPlayer);
    }
    for (Enemy enemy : enemies) {
      Paint paint = enemy.elite || enemy.type == Enemy.EnemyType.BOSS ? paintEnemyElite : paintEnemy;
      canvas.drawRect(enemy.getRect(rectA), paint);
    }
    for (Projectile projectile : projectiles) {
      canvas.drawRect(projectile.getRect(rectA), paintProjectile);
    }
    for (Pickup pickup : pickups) {
      canvas.drawRect(pickup.getRect(rectA), paintPickup);
    }
    holder.unlockCanvasAndPost(canvas);
  }

  private void updateHud(boolean force) {
    hudTimer += 1f / 60f;
    if (force || hudTimer > 0.2f) {
      hudTimer = 0f;
      if (listener != null && player != null) {
        listener.onHudUpdate(score, (int) player.hp, (int) player.energy, Math.max(1, player.combo), stageManager.getStageNumber(), stageManager.getWaveNumber());
      }
    }
  }

  public interface Listener {
    void onHudUpdate(int score, int hp, int energy, int combo, int stage, int wave);
    void onStateChanged(GameState state);
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
    SPECIAL
  }
}
