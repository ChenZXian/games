package com.android.boot.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.android.boot.R;
import com.android.boot.audio.SoundManager;
import com.android.boot.entity.Bullet;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Obstacle;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.Placeable;
import com.android.boot.entity.Player;
import com.android.boot.fx.FloatingText;
import com.android.boot.fx.Particle;
import java.util.Random;

public class GameEngine {
  public interface Listener {
    void onHudUpdate(float hp, float maxHp, float energy, float maxEnergy, int kills, int score, float time);
    void onGameOver(float time, int kills, int score);
    void onUpgradeOptions(String[] options);
    void onStateChanged(GameState state);
  }

  private static final int MAX_ENEMIES = 80;
  private static final int MAX_BULLETS = 140;
  private static final int MAX_PICKUPS = 50;
  private static final int MAX_PARTICLES = 120;
  private static final int MAX_TEXTS = 30;
  private static final int MAX_OBSTACLES = 30;
  private static final int MAX_PLACEABLES = 20;

  private static final int MOD_FAST = 1;
  private static final int MOD_TANKY = 2;
  private static final int MOD_REGEN = 3;
  private static final int MOD_EXPLODE = 4;

  private final Random random = new Random();
  private final GameTimer timer = new GameTimer();
  private final Cooldown fireCooldown = new Cooldown();
  private final Cooldown hitCooldown = new Cooldown();
  private final Cooldown skillCooldown = new Cooldown();
  private final SoundManager soundManager;
  private final Context context;
  private final Paint paintPlayer = new Paint();
  private final Paint paintEnemy = new Paint();
  private final Paint paintElite = new Paint();
  private final Paint paintBullet = new Paint();
  private final Paint paintPickupEnergy = new Paint();
  private final Paint paintPickupCoin = new Paint();
  private final Paint paintPickupMedkit = new Paint();
  private final Paint paintParticle = new Paint();
  private final Paint paintText = new Paint();
  private final Paint paintJoystick = new Paint();
  private final Paint paintAim = new Paint();

  private Listener listener;
  private GameState state = GameState.MENU;
  private Player player;
  private final Enemy[] enemies = new Enemy[MAX_ENEMIES];
  private final Bullet[] bullets = new Bullet[MAX_BULLETS];
  private final Pickup[] pickups = new Pickup[MAX_PICKUPS];
  private final Particle[] particles = new Particle[MAX_PARTICLES];
  private final FloatingText[] texts = new FloatingText[MAX_TEXTS];
  private final Obstacle[] obstacles = new Obstacle[MAX_OBSTACLES];
  private final Placeable[] placeables = new Placeable[MAX_PLACEABLES];

  private int width;
  private int height;
  private float spawnTimer;
  private float eliteTimer;
  private float nextUpgradeTime;
  private boolean moveActive;
  private boolean aimActive;
  private boolean shootActive;
  private float moveInputX;
  private float moveInputY;
  private float aimInputX;
  private float aimInputY;
  private float shootInputX;
  private float shootInputY;
  private float aimDirX = 1f;
  private float aimDirY = 0f;

  private int kills;
  private int score;
  private boolean gameOverSent;

  private Weapon currentWeapon;
  private Weapon[] availableWeapons;
  private Weapon[] ownedWeapons;
  private int selectedWeaponIndex = 0;
  private int wallItemCount = 0;
  private int mineItemCount = 0;
  private boolean isShooting = false;
  private boolean aimAssistEnabled = true; // Aim assist enabled by default
  private float damageMultiplier = 1f;
  private float fireRateMultiplier = 1f;
  private float moveSpeedMultiplier = 1f;
  private float maxHpMultiplier = 1f;
  private float regenRate = 0f;
  private int pierceBonus = 0;
  private int multishot = 1;
  private float knockbackMultiplier = 1f;
  private float magnetRange = 0f;
  private float skillDamage = 40f;
  private float skillCooldownSeconds = 8f;

  private final String[] upgradePool = new String[] {
      "Damage",
      "Fire Rate",
      "Move Speed",
      "Max HP",
      "Regen",
      "Pierce",
      "Multishot",
      "Knockback",
      "Magnet",
      "Shield",
      "Skill Cooldown",
      "Skill Damage"
  };
  private final int[] currentUpgrades = new int[3];

  public GameEngine(Context context) {
    this.context = context;
    soundManager = new SoundManager(context);
    availableWeapons = new Weapon[] {
        Weapon.createPistol(),
        Weapon.createShotgun(),
        Weapon.createSMG(),
        Weapon.createRifle(),
        Weapon.createLauncher()
    };
    ownedWeapons = new Weapon[] {
        Weapon.createPistol(),
        Weapon.createShotgun()
    };
    currentWeapon = ownedWeapons[0];
    selectedWeaponIndex = 0;
    paintPlayer.setColor(context.getColor(R.color.cst_accent));
    paintEnemy.setColor(context.getColor(R.color.cst_danger));
    paintElite.setColor(context.getColor(R.color.cst_accent_2));
    paintBullet.setColor(context.getColor(R.color.cst_success));
    paintPickupEnergy.setColor(context.getColor(R.color.cst_accent));
    paintPickupCoin.setColor(context.getColor(R.color.cst_warning));
    paintPickupMedkit.setColor(context.getColor(R.color.cst_success));
    paintParticle.setColor(context.getColor(R.color.cst_text_secondary));
    paintText.setColor(context.getColor(R.color.cst_text_primary));
    paintText.setTextSize(28f);
    paintJoystick.setColor(context.getColor(R.color.cst_panel_stroke));
    paintJoystick.setStyle(Paint.Style.STROKE);
    paintJoystick.setStrokeWidth(4f);
    paintAim.setColor(context.getColor(R.color.cst_accent));
    paintAim.setStyle(Paint.Style.STROKE);
    paintAim.setStrokeWidth(4f);
    for (int i = 0; i < MAX_ENEMIES; i++) {
      enemies[i] = new Enemy();
    }
    for (int i = 0; i < MAX_BULLETS; i++) {
      bullets[i] = new Bullet();
    }
    for (int i = 0; i < MAX_PICKUPS; i++) {
      pickups[i] = new Pickup();
    }
    for (int i = 0; i < MAX_PARTICLES; i++) {
      particles[i] = new Particle();
    }
    for (int i = 0; i < MAX_TEXTS; i++) {
      texts[i] = new FloatingText();
    }
    for (int i = 0; i < MAX_OBSTACLES; i++) {
      obstacles[i] = new Obstacle();
    }
    for (int i = 0; i < MAX_PLACEABLES; i++) {
      placeables[i] = new Placeable();
    }
  }

  private void initMap() {
    if (width <= 0 || height <= 0) {
      return;
    }
    for (Obstacle obs : obstacles) {
      obs.deactivate();
    }
    int count = 0;
    // Generate wilderness style map: houses, trees, rivers, etc.
    // Generate 1-2 dilapidated wooden houses
    for (int i = 0; i < 2 && count < MAX_OBSTACLES; i++) {
      float x = width * (0.15f + random.nextFloat() * 0.7f);
      float y = height * (0.15f + random.nextFloat() * 0.7f);
      if (Math.abs(x - width * 0.5f) > 120f || Math.abs(y - height * 0.5f) > 120f) {
        obstacles[count].spawn(x, y, 80f + random.nextFloat() * 40f, 60f + random.nextFloat() * 30f,
            Obstacle.TYPE_HOUSE, false, 200f);
        count++;
      }
    }
    // Generate trees (5-8 trees)
    for (int i = 0; i < 8 && count < MAX_OBSTACLES; i++) {
      float x = width * (0.1f + random.nextFloat() * 0.8f);
      float y = height * (0.1f + random.nextFloat() * 0.8f);
      if (Math.abs(x - width * 0.5f) > 80f || Math.abs(y - height * 0.5f) > 80f) {
        float treeSize = 25f + random.nextFloat() * 20f;
        obstacles[count].spawn(x, y, treeSize, treeSize * 1.2f,
            Obstacle.TYPE_TREE, false, 0f);
        count++;
      }
    }
    // Generate river (horizontal or vertical)
    if (count < MAX_OBSTACLES) {
      boolean horizontal = random.nextBoolean();
      if (horizontal) {
        float riverY = height * (0.3f + random.nextFloat() * 0.4f);
        for (int i = 0; i < 6 && count < MAX_OBSTACLES; i++) {
          float riverX = width * (0.1f + i * 0.15f);
          obstacles[count].spawn(riverX, riverY, 60f, 25f, Obstacle.TYPE_RIVER, false, 0f);
          count++;
        }
      } else {
        float riverX = width * (0.3f + random.nextFloat() * 0.4f);
        for (int i = 0; i < 5 && count < MAX_OBSTACLES; i++) {
          float riverY = height * (0.1f + i * 0.18f);
          obstacles[count].spawn(riverX, riverY, 25f, 60f, Obstacle.TYPE_RIVER, false, 0f);
          count++;
        }
      }
    }
    // Generate some rocks
    for (int i = 0; i < 4 && count < MAX_OBSTACLES; i++) {
      float x = width * (0.2f + random.nextFloat() * 0.6f);
      float y = height * (0.2f + random.nextFloat() * 0.6f);
      if (Math.abs(x - width * 0.5f) > 100f || Math.abs(y - height * 0.5f) > 100f) {
        float rockSize = 20f + random.nextFloat() * 15f;
        obstacles[count].spawn(x, y, rockSize, rockSize, Obstacle.TYPE_ROCK, false, 0f);
        count++;
      }
    }
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    if (player == null) {
      player = new Player(width * 0.5f, height * 0.5f);
    } else {
      player.x = width * 0.5f;
      player.y = height * 0.5f;
    }
    if (width > 0 && height > 0) {
      initMap();
    }
  }

  public void startNewRun() {
    resetStats();
    if (player == null) {
      player = new Player(width * 0.5f, height * 0.5f);
    }
    player.reset(width * 0.5f, height * 0.5f);
    state = GameState.PLAYING;
    notifyState();
  }

  public void goToMenu() {
    state = GameState.MENU;
    notifyState();
  }

  public void pause() {
    if (state == GameState.PLAYING) {
      state = GameState.PAUSED;
      notifyState();
    }
  }

  public void resume() {
    if (state == GameState.PAUSED) {
      state = GameState.PLAYING;
      notifyState();
    }
  }

  public boolean isPlaying() {
    return state == GameState.PLAYING;
  }

  public void setMoveInput(float x, float y, boolean active) {
    moveInputX = x;
    moveInputY = y;
    moveActive = active;
  }

  public void setAimInput(float x, float y, boolean active) {
    aimInputX = x;
    aimInputY = y;
    aimActive = active;
  }

  public void setShootInput(float x, float y, boolean active) {
    shootInputX = x;
    shootInputY = y;
    shootActive = active;
    isShooting = active;
  }

  public void update(float delta) {
    if (state == GameState.MENU || state == GameState.GAME_OVER) {
      return;
    }
    if (state == GameState.PAUSED || state == GameState.UPGRADE) {
      return;
    }
    timer.update(delta);
    hitCooldown.update(delta);
    fireCooldown.update(delta);
    skillCooldown.update(delta);

    updatePlayer(delta);
    updateAutoAim();
    updateSpawning(delta);
    updateEnemies(delta);
    updateBullets(delta);
    updatePickups(delta);
    updatePlaceables(delta);
    updateParticles(delta);
    updateTexts(delta);

    if (listener != null) {
      listener.onHudUpdate(player.hp, player.maxHp, player.energy, player.maxEnergy, kills, score, timer.getElapsed());
    }

    if (timer.getElapsed() >= nextUpgradeTime) {
      triggerUpgrade();
    }

    if (player.hp <= 0f && !gameOverSent) {
      state = GameState.GAME_OVER;
      gameOverSent = true;
      if (listener != null) {
        listener.onGameOver(timer.getElapsed(), kills, score);
      }
    }
  }

  public void render(Canvas canvas) {
    if (canvas == null || width <= 0 || height <= 0) {
      return;
    }
    renderBackground(canvas);
    renderObstacles(canvas);
    renderPlaceables(canvas);
    renderPickups(canvas);
    renderBullets(canvas);
    renderEnemies(canvas);
    renderParticles(canvas);
    renderPlayer(canvas);
    renderTexts(canvas);
    renderControls(canvas);
  }

  private void renderBackground(Canvas canvas) {
    if (width <= 0 || height <= 0) {
      return;
    }
    // 1. Overall grass background (gradient from dark to light), creating wilderness atmosphere
    Paint bgPaint = new Paint();
    android.graphics.LinearGradient grassGradient = new android.graphics.LinearGradient(
        0, 0, 0, height,
        0xFF102418, // Darker at top
        0xFF264A2C, // Lighter at bottom
        android.graphics.Shader.TileMode.CLAMP);
    bgPaint.setShader(grassGradient);
    canvas.drawRect(0, 0, width, height, bgPaint);

    // 2. A slightly brighter "trodden path" in the middle area, making player activity area clearer
    Paint pathPaint = new Paint();
    pathPaint.setColor(0x332F3F26);
    float pathWidth = width * 0.35f;
    float pathCenterX = width * 0.5f;
    android.graphics.RectF pathRect = new android.graphics.RectF(
        pathCenterX - pathWidth * 0.5f, 0,
        pathCenterX + pathWidth * 0.5f, height);
    canvas.drawRect(pathRect, pathPaint);

    // 3. Grass texture: layered grass dots for stronger depth perception
    Paint grassPaintFar = new Paint();
    grassPaintFar.setColor(0x33228B22);
    Paint grassPaintNear = new Paint();
    grassPaintNear.setColor(0x5532A852);

    int countFar = 220;
    int countNear = 140;
    for (int i = 0; i < countFar; i++) {
      float gx = random.nextFloat() * width;
      float gy = random.nextFloat() * height;
      canvas.drawCircle(gx, gy, 1.5f + random.nextFloat() * 2.5f, grassPaintFar);
    }
    for (int i = 0; i < countNear; i++) {
      float gx = random.nextFloat() * width;
      float gy = random.nextFloat() * height;
      canvas.drawCircle(gx, gy, 2.5f + random.nextFloat() * 3.5f, grassPaintNear);
    }

    // 4. Small amount of debris and dry grass patches, enhancing wilderness decay feel (not obstacles, just background texture)
    Paint patchPaint = new Paint();
    patchPaint.setColor(0x332E2E2E);
    for (int i = 0; i < 35; i++) {
      float px = random.nextFloat() * width;
      float py = random.nextFloat() * height;
      float rw = 14f + random.nextFloat() * 24f;
      float rh = 6f + random.nextFloat() * 14f;
      canvas.save();
      canvas.rotate((random.nextFloat() - 0.5f) * 40f, px, py);
      canvas.drawRoundRect(px - rw * 0.5f, py - rh * 0.5f,
          px + rw * 0.5f, py + rh * 0.5f, 4f, 4f, patchPaint);
      canvas.restore();
    }
  }

  public void applyUpgrade(int index) {
    if (state != GameState.UPGRADE) {
      return;
    }
    int upgrade = currentUpgrades[index];
    if (upgrade == 0) {
      damageMultiplier += 0.2f;
    } else if (upgrade == 1) {
      fireRateMultiplier += 0.15f;
    } else if (upgrade == 2) {
      moveSpeedMultiplier += 0.1f;
    } else if (upgrade == 3) {
      maxHpMultiplier += 0.15f;
      player.maxHp = 120f * maxHpMultiplier;
      player.hp = Math.min(player.hp + 20f, player.maxHp);
    } else if (upgrade == 4) {
      regenRate += 0.4f;
    } else if (upgrade == 5) {
      pierceBonus += 1;
    } else if (upgrade == 6) {
      multishot = Math.min(4, multishot + 1);
    } else if (upgrade == 7) {
      knockbackMultiplier += 0.2f;
    } else if (upgrade == 8) {
      magnetRange += 60f;
    } else if (upgrade == 9) {
      player.shield += 12f;
    } else if (upgrade == 10) {
      skillCooldownSeconds = Math.max(2.5f, skillCooldownSeconds - 0.8f);
    } else if (upgrade == 11) {
      skillDamage += 12f;
    }
    state = GameState.PLAYING;
  }

  public void triggerSkill() {
    if (state != GameState.PLAYING) {
      return;
    }
    if (!skillCooldown.isReady()) {
      return;
    }
    float cost = 35f;
    if (player.energy < cost) {
      return;
    }
    player.energy -= cost;
    skillCooldown.start(skillCooldownSeconds);
    soundManager.playShock();
    float radius = 220f;
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      float dx = enemy.x - player.x;
      float dy = enemy.y - player.y;
      float dist = (float) Math.hypot(dx, dy);
      if (dist < radius) {
        enemy.hp -= skillDamage;
        float nx = dist == 0f ? 0f : dx / dist;
        float ny = dist == 0f ? 0f : dy / dist;
        enemy.x += nx * 40f;
        enemy.y += ny * 40f;
        spawnParticles(enemy.x, enemy.y, 4);
      }
    }
  }

  public boolean toggleSound() {
    return soundManager.toggle();
  }

  public void release() {
    soundManager.release();
  }

  private void updatePlayer(float delta) {
    float speed = player.speed * moveSpeedMultiplier;
    float newX = player.x + moveInputX * speed * delta;
    float newY = player.y + moveInputY * speed * delta;
    if (newX >= player.radius && newX <= width - player.radius && !checkObstacleCollision(newX, player.y, player.radius)) {
      player.x = newX;
    }
    if (newY >= player.radius && newY <= height - player.radius && !checkObstacleCollision(player.x, newY, player.radius)) {
      player.y = newY;
    }
    if (regenRate > 0f) {
      player.hp = Math.min(player.maxHp, player.hp + regenRate * delta);
    }
    if (player.energy < player.maxEnergy) {
      player.energy = Math.min(player.maxEnergy, player.energy + 10f * delta);
    }
    float weaponFireRate = currentWeapon.fireRate * fireRateMultiplier;
    if (isShooting && fireCooldown.isReady()) {
      fireCooldown.start(1f / weaponFireRate);
      fireBurst();
    }
  }

  private boolean checkObstacleCollision(float x, float y, float radius) {
    for (Obstacle obs : obstacles) {
      if (!obs.active) continue;
      if (obs.contains(x, y)) return true;
      float dx = Math.max(obs.x - obs.width * 0.5f, Math.min(x, obs.x + obs.width * 0.5f)) - x;
      float dy = Math.max(obs.y - obs.height * 0.5f, Math.min(y, obs.y + obs.height * 0.5f)) - y;
      if (dx * dx + dy * dy < radius * radius) return true;
    }
    for (Placeable place : placeables) {
      if (!place.active) continue;
      if (place.contains(x, y)) return true;
      float dx = Math.max(place.x - place.width * 0.5f, Math.min(x, place.x + place.width * 0.5f)) - x;
      float dy = Math.max(place.y - place.height * 0.5f, Math.min(y, place.y + place.height * 0.5f)) - y;
      if (dx * dx + dy * dy < radius * radius) return true;
    }
    return false;
  }

  private void updateSpawning(float delta) {
    spawnTimer -= delta;
    eliteTimer -= delta;
    float elapsed = timer.getElapsed();
    if (spawnTimer <= 0f) {
      spawnTimer = Math.max(0.35f, 1.4f - elapsed * 0.01f);
      spawnEnemy(false);
    }
    if (eliteTimer <= 0f) {
      eliteTimer = 18f;
      spawnEnemy(true);
    }
  }

  private void spawnEnemy(boolean elite) {
    Enemy enemy = getInactiveEnemy();
    if (enemy == null) {
      return;
    }
    float x;
    float y;
    int edge = random.nextInt(4);
    if (edge == 0) {
      x = -40f;
      y = random.nextFloat() * height;
    } else if (edge == 1) {
      x = width + 40f;
      y = random.nextFloat() * height;
    } else if (edge == 2) {
      x = random.nextFloat() * width;
      y = -40f;
    } else {
      x = random.nextFloat() * width;
      y = height + 40f;
    }
    float elapsed = timer.getElapsed();
    float baseSpeed = 80f + elapsed * 1.1f;
    float baseHp = 25f + elapsed * 0.8f;
    float radius = 22f;
    int modifier = 0;
    if (elite) {
      int roll = 1 + random.nextInt(4);
      modifier = roll;
      if (modifier == MOD_FAST) {
        baseSpeed *= 1.6f;
        baseHp *= 0.9f;
      } else if (modifier == MOD_TANKY) {
        baseSpeed *= 0.7f;
        baseHp *= 2.4f;
        radius = 30f;
      } else if (modifier == MOD_REGEN) {
        baseHp *= 1.4f;
      } else if (modifier == MOD_EXPLODE) {
        baseSpeed *= 1.1f;
      }
    }
    enemy.spawn(x, y, radius, baseSpeed, baseHp, elite, modifier);
    if (elite && modifier == MOD_REGEN) {
      enemy.regen = baseHp * 0.03f;
    }
    if (elite && modifier == MOD_EXPLODE) {
      enemy.explodeDamage = 18f + elapsed * 0.3f;
    }
  }

  private void updateEnemies(float delta) {
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      float dx = player.x - enemy.x;
      float dy = player.y - enemy.y;
      float dist = (float) Math.hypot(dx, dy);
      float nx = dist == 0f ? 0f : dx / dist;
      float ny = dist == 0f ? 0f : dy / dist;
      float newX = enemy.x + nx * enemy.speed * delta;
      float newY = enemy.y + ny * enemy.speed * delta;
      if (!checkObstacleCollision(newX, enemy.y, enemy.radius)) {
        enemy.x = newX;
      }
      if (!checkObstacleCollision(enemy.x, newY, enemy.radius)) {
        enemy.y = newY;
      }
      if (enemy.regen > 0f) {
        enemy.hp = Math.min(enemy.maxHp, enemy.hp + enemy.regen * delta);
      }
      float hitDist = enemy.radius + player.radius;
      if (dist < hitDist && hitCooldown.isReady()) {
        float damage = 12f + timer.getElapsed() * 0.05f;
        if (player.shield > 0f) {
          float blocked = Math.min(player.shield, damage);
          player.shield -= blocked;
          damage -= blocked;
        }
        if (damage > 0f) {
          player.hp -= damage;
        }
        hitCooldown.start(0.6f);
        spawnParticles(player.x, player.y, 6);
        soundManager.playHit();
      }
    }
  }

  private void updateBullets(float delta) {
    for (Bullet bullet : bullets) {
      if (!bullet.active) {
        continue;
      }
      bullet.x += bullet.vx * delta;
      bullet.y += bullet.vy * delta;
      if (bullet.x < -20f || bullet.x > width + 20f || bullet.y < -20f || bullet.y > height + 20f) {
        bullet.deactivate();
        continue;
      }
      for (Obstacle obs : obstacles) {
        if (!obs.active || !obs.destructible) continue;
        if (obs.contains(bullet.x, bullet.y)) {
          obs.hp -= bullet.damage;
          bullet.deactivate();
          if (obs.hp <= 0f) {
            obs.deactivate();
            spawnParticles(obs.x, obs.y, 8);
          }
          continue;
        }
      }
      for (Enemy enemy : enemies) {
        if (!enemy.active) {
          continue;
        }
        float dx = enemy.x - bullet.x;
        float dy = enemy.y - bullet.y;
        float dist2 = dx * dx + dy * dy;
        float hit = enemy.radius + bullet.radius;
        if (dist2 < hit * hit) {
          enemy.hp -= bullet.damage;
          float dist = (float) Math.sqrt(dist2);
          float nx = dist == 0f ? 0f : dx / dist;
          float ny = dist == 0f ? 0f : dy / dist;
          float kb = currentWeapon.knockback * knockbackMultiplier;
          enemy.x += nx * kb * 0.1f;
          enemy.y += ny * kb * 0.1f;
          spawnParticles(bullet.x, bullet.y, 2);
          if (bullet.pierce <= 0) {
            bullet.deactivate();
          } else {
            bullet.pierce -= 1;
          }
          if (enemy.hp <= 0f) {
            handleEnemyDeath(enemy);
          }
          break;
        }
      }
    }
  }

  private void updatePickups(float delta) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        continue;
      }
      float dx = player.x - pickup.x;
      float dy = player.y - pickup.y;
      float dist = (float) Math.hypot(dx, dy);
      if (magnetRange > 0f && dist < magnetRange) {
        float nx = dist == 0f ? 0f : dx / dist;
        float ny = dist == 0f ? 0f : dy / dist;
        pickup.x += nx * 280f * delta;
        pickup.y += ny * 280f * delta;
      }
      if (dist < pickup.radius + player.radius) {
        applyPickup(pickup);
      }
    }
  }

  private void updatePlaceables(float delta) {
    for (Placeable place : placeables) {
      if (!place.active) continue;
      place.cooldownTimer -= delta;
      if (place.type == Placeable.TYPE_MINE) {
        for (Enemy enemy : enemies) {
          if (!enemy.active) continue;
          float dx = enemy.x - place.x;
          float dy = enemy.y - place.y;
          float dist = (float) Math.hypot(dx, dy);
          if (dist < place.triggerRadius) {
            enemy.hp -= place.damage;
            spawnParticles(place.x, place.y, 12);
            if (enemy.hp <= 0f) {
              handleEnemyDeath(enemy);
            }
            place.deactivate();
            break;
          }
        }
      } else if (place.type == Placeable.TYPE_TURRET && place.cooldownTimer <= 0f) {
        Enemy nearest = null;
        float nearestDist = place.triggerRadius;
        for (Enemy enemy : enemies) {
          if (!enemy.active) continue;
          float dx = enemy.x - place.x;
          float dy = enemy.y - place.y;
          float dist = (float) Math.hypot(dx, dy);
          if (dist < nearestDist) {
            nearest = enemy;
            nearestDist = dist;
          }
        }
        if (nearest != null) {
          float dx = nearest.x - place.x;
          float dy = nearest.y - place.y;
          float dist = (float) Math.hypot(dx, dy);
          float dirX = dx / dist;
          float dirY = dy / dist;
          Bullet bullet = getInactiveBullet();
          if (bullet != null) {
            bullet.spawn(place.x, place.y, dirX * 600f, dirY * 600f, place.damage, 0, 0xFFFFD700, 5f);
          }
          place.cooldownTimer = place.cooldown;
        }
      }
    }
  }

  private void updateParticles(float delta) {
    for (Particle particle : particles) {
      if (!particle.active) {
        continue;
      }
      particle.x += particle.vx * delta;
      particle.y += particle.vy * delta;
      particle.life -= delta;
      if (particle.life <= 0f) {
        particle.deactivate();
      }
    }
  }

  private void updateTexts(float delta) {
    for (FloatingText text : texts) {
      if (!text.active) {
        continue;
      }
      text.y += text.vy * delta;
      text.life -= delta;
      if (text.life <= 0f) {
        text.deactivate();
      }
    }
  }

  private void updateAutoAim() {
    // Priority: use shoot joystick input
    if (shootActive) {
      float len = (float) Math.hypot(shootInputX, shootInputY);
      if (len > 0.1f) {
        aimDirX = shootInputX / len;
        aimDirY = shootInputY / len;
      }
      return;
    }
    // Second: use aim input (if exists)
    if (aimActive) {
      float len = (float) Math.hypot(aimInputX, aimInputY);
      if (len > 0.1f) {
        aimDirX = aimInputX / len;
        aimDirY = aimInputY / len;
      }
      return;
    }
    // Third: use move joystick input to update direction
    if (moveActive) {
      float len = (float) Math.hypot(moveInputX, moveInputY);
      if (len > 0.1f) {
        aimDirX = moveInputX / len;
        aimDirY = moveInputY / len;
      }
      return;
    }
    // If no manual input and aim assist enabled, auto-aim at nearest enemy
    if (aimAssistEnabled) {
      Enemy nearest = null;
      float nearestDist = Float.MAX_VALUE;
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      float dx = enemy.x - player.x;
      float dy = enemy.y - player.y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < nearestDist && dist < 600f) { // Max aim assist distance 600 pixels
          nearest = enemy;
          nearestDist = dist;
        }
      }
      if (nearest != null) {
        float dx = nearest.x - player.x;
        float dy = nearest.y - player.y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > 0.1f) {
          aimDirX = dx / dist;
          aimDirY = dy / dist;
        }
      }
    }
    // Keep current direction when no input
  }

  public void switchWeapon(int index) {
    if (index >= 0 && index < ownedWeapons.length) {
      selectedWeaponIndex = index;
      currentWeapon = ownedWeapons[index];
    }
  }

  public void addWeapon(Weapon weapon) {
    for (int i = 0; i < ownedWeapons.length; i++) {
      if (ownedWeapons[i] == null) {
        ownedWeapons[i] = weapon;
        return;
      }
    }
    Weapon[] newOwned = new Weapon[ownedWeapons.length + 1];
    System.arraycopy(ownedWeapons, 0, newOwned, 0, ownedWeapons.length);
    newOwned[ownedWeapons.length] = weapon;
    ownedWeapons = newOwned;
  }

  public void startShooting() {
    isShooting = true;
  }

  public void stopShooting() {
    isShooting = false;
  }

  public void placeItem(int type, float x, float y) {
    if (type == Placeable.TYPE_WALL && wallItemCount <= 0) {
      return;
    }
    if (type == Placeable.TYPE_MINE && mineItemCount <= 0) {
      return;
    }
    for (Placeable placeable : placeables) {
      if (!placeable.active) {
        placeable.spawn(x, y, type);
        if (type == Placeable.TYPE_WALL) {
          wallItemCount--;
        } else if (type == Placeable.TYPE_MINE) {
          mineItemCount--;
        }
        break;
      }
    }
  }

  public int getWallItemCount() {
    return wallItemCount;
  }

  public int getMineItemCount() {
    return mineItemCount;
  }

  public boolean isAimAssistEnabled() {
    return aimAssistEnabled;
  }

  public void setAimAssistEnabled(boolean enabled) {
    aimAssistEnabled = enabled;
  }

  public Weapon[] getOwnedWeapons() {
    return ownedWeapons;
  }

  public Weapon getCurrentWeapon() {
    return currentWeapon;
  }

  public void placeItemAtPlayer(int type) {
    float placeDistance = 60f;
    float placeX = player.x + aimDirX * placeDistance;
    float placeY = player.y + aimDirY * placeDistance;
    if (placeX < 20f || placeX > width - 20f || placeY < 20f || placeY > height - 20f) {
      return;
    }
    if (checkObstacleCollision(placeX, placeY, 15f)) {
      return;
    }
    placeItem(type, placeX, placeY);
  }

  public Weapon[] getAvailableWeapons() {
    return availableWeapons;
  }

  private void fireBurst() {
    float baseDamage = currentWeapon.damage * damageMultiplier;
    soundManager.playShoot();
    int shotsPerBurst = currentWeapon.bulletCount * multishot;
    float spread = currentWeapon.spread;
    for (int i = 0; i < shotsPerBurst; i++) {
      float angle = 0f;
      if (shotsPerBurst > 1 && spread > 0f) {
        float t = shotsPerBurst == 1 ? 0f : (float) i / (shotsPerBurst - 1);
        angle = (t - 0.5f) * spread;
        if (shotsPerBurst > 1) {
          angle += (random.nextFloat() - 0.5f) * spread * 0.3f;
        }
      }
      float cos = (float) Math.cos(angle);
      float sin = (float) Math.sin(angle);
      float dirX = aimDirX * cos - aimDirY * sin;
      float dirY = aimDirX * sin + aimDirY * cos;
      spawnBullet(dirX, dirY, baseDamage);
    }
  }

  private void spawnBullet(float dirX, float dirY, float damage) {
    Bullet bullet = getInactiveBullet();
    if (bullet == null) {
      return;
    }
    float speed = currentWeapon.bulletSpeed;
    int pierce = currentWeapon.pierce + pierceBonus;
    float radius = currentWeapon.type == Weapon.TYPE_LAUNCHER ? 10f : currentWeapon.type == Weapon.TYPE_SHOTGUN ? 5f : 6f;
    bullet.spawn(player.x + dirX * player.radius, player.y + dirY * player.radius,
        dirX * speed, dirY * speed, damage, pierce, currentWeapon.color, radius);
  }

  private void handleEnemyDeath(Enemy enemy) {
    enemy.deactivate();
    kills += 1;
    score += enemy.elite ? 60 : 15;
    spawnText(enemy.x, enemy.y, enemy.elite ? "+60" : "+15");
    soundManager.playHit();
    if (enemy.elite && enemy.modifier == MOD_EXPLODE) {
      float dx = player.x - enemy.x;
      float dy = player.y - enemy.y;
      float dist = (float) Math.hypot(dx, dy);
      if (dist < 140f) {
        player.hp -= enemy.explodeDamage;
      }
    }
    float roll = random.nextFloat();
    if (enemy.elite) {
      if (roll < 0.25f) {
        int weaponType = random.nextInt(availableWeapons.length);
        spawnWeaponPickup(enemy.x, enemy.y, weaponType);
      } else if (roll < 0.45f) {
        spawnPlaceablePickup(enemy.x, enemy.y, Placeable.TYPE_WALL);
      } else if (roll < 0.6f) {
        spawnPlaceablePickup(enemy.x, enemy.y, Placeable.TYPE_MINE);
      } else if (roll < 0.75f) {
        spawnPickup(enemy.x, enemy.y, Pickup.TYPE_ENERGY, 30f);
      } else if (roll < 0.9f) {
        spawnPickup(enemy.x, enemy.y, Pickup.TYPE_MEDKIT, 25f);
      } else {
        spawnPickup(enemy.x, enemy.y, Pickup.TYPE_COIN, 20f);
      }
    } else {
    if (roll < 0.35f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_ENERGY, 20f);
    } else if (roll < 0.6f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_COIN, 10f);
    } else if (roll < 0.7f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_MEDKIT, 18f);
      }
    }
  }

  private void spawnWeaponPickup(float x, float y, int weaponType) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        pickup.spawnWeapon(x, y, weaponType);
        break;
      }
    }
  }

  private void spawnPlaceablePickup(float x, float y, int placeableType) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        pickup.spawnPlaceable(x, y, placeableType);
        break;
      }
    }
  }

  private void applyPickup(Pickup pickup) {
    if (pickup.type == Pickup.TYPE_ENERGY) {
      player.energy = Math.min(player.maxEnergy, player.energy + pickup.value);
    } else if (pickup.type == Pickup.TYPE_COIN) {
      score += (int) pickup.value;
    } else if (pickup.type == Pickup.TYPE_MEDKIT) {
      player.hp = Math.min(player.maxHp, player.hp + pickup.value);
    } else if (pickup.type == Pickup.TYPE_WEAPON) {
      addWeapon(availableWeapons[pickup.weaponType]);
      spawnText(pickup.x, pickup.y, "Weapon!");
    } else if (pickup.type == Pickup.TYPE_WALL_ITEM) {
      wallItemCount++;
      spawnText(pickup.x, pickup.y, "Wall +1");
    } else if (pickup.type == Pickup.TYPE_MINE_ITEM) {
      mineItemCount++;
      spawnText(pickup.x, pickup.y, "Mine +1");
    }
    pickup.deactivate();
    soundManager.playPickup();
  }

  private void spawnPickup(float x, float y, int type, float value) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        pickup.spawn(x, y, type, value);
        break;
      }
    }
  }

  private void spawnParticles(float x, float y, int count) {
    for (int i = 0; i < count; i++) {
      Particle particle = getInactiveParticle();
      if (particle == null) {
        return;
      }
      float angle = random.nextFloat() * 6.2831855f;
      float speed = 120f + random.nextFloat() * 120f;
      float vx = (float) Math.cos(angle) * speed;
      float vy = (float) Math.sin(angle) * speed;
      particle.spawn(x, y, vx, vy, 0.5f, 6f);
    }
  }

  private void spawnText(float x, float y, String text) {
    for (FloatingText t : texts) {
      if (!t.active) {
        t.spawn(x, y, -30f, 0.8f, text);
        return;
      }
    }
  }

  private Enemy getInactiveEnemy() {
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        return enemy;
      }
    }
    return null;
  }

  private Bullet getInactiveBullet() {
    for (Bullet bullet : bullets) {
      if (!bullet.active) {
        return bullet;
      }
    }
    return null;
  }

  private Particle getInactiveParticle() {
    for (Particle particle : particles) {
      if (!particle.active) {
        return particle;
      }
    }
    return null;
  }

  private void triggerUpgrade() {
    state = GameState.UPGRADE;
    nextUpgradeTime += 45f;
    int a = random.nextInt(upgradePool.length);
    int b = random.nextInt(upgradePool.length);
    int c = random.nextInt(upgradePool.length);
    while (b == a) {
      b = random.nextInt(upgradePool.length);
    }
    while (c == a || c == b) {
      c = random.nextInt(upgradePool.length);
    }
    currentUpgrades[0] = a;
    currentUpgrades[1] = b;
    currentUpgrades[2] = c;
    if (listener != null) {
      listener.onUpgradeOptions(new String[] {upgradePool[a], upgradePool[b], upgradePool[c]});
    }
  }

  private void resetStats() {
    timer.reset();
    kills = 0;
    score = 0;
    spawnTimer = 1f;
    eliteTimer = 10f;
    nextUpgradeTime = 45f;
    ownedWeapons = new Weapon[] {
        Weapon.createPistol(),
        Weapon.createShotgun()
    };
    currentWeapon = ownedWeapons[0];
    selectedWeaponIndex = 0;
    wallItemCount = 3;
    mineItemCount = 3;
    damageMultiplier = 1f;
    fireRateMultiplier = 1f;
    moveSpeedMultiplier = 1f;
    maxHpMultiplier = 1f;
    regenRate = 0f;
    pierceBonus = 0;
    multishot = 1;
    knockbackMultiplier = 1f;
    magnetRange = 0f;
    skillDamage = 40f;
    skillCooldownSeconds = 8f;
    fireCooldown.start(0.2f);
    hitCooldown.start(0f);
    skillCooldown.start(0f);
    gameOverSent = false;
    clearPools();
  }

  private void clearPools() {
    for (Enemy enemy : enemies) {
      enemy.deactivate();
    }
    for (Bullet bullet : bullets) {
      bullet.deactivate();
    }
    for (Pickup pickup : pickups) {
      pickup.deactivate();
    }
    for (Particle particle : particles) {
      particle.deactivate();
    }
    for (FloatingText text : texts) {
      text.deactivate();
    }
    for (Placeable place : placeables) {
      place.deactivate();
    }
    initMap();
  }

  private void renderPlayer(Canvas canvas) {
    drawHumanoid(canvas, player.x, player.y, player.radius, aimDirX, aimDirY, paintPlayer, true);
    if (player.shield > 0f) {
      Paint shieldPaint = new Paint(paintJoystick);
      shieldPaint.setStyle(Paint.Style.STROKE);
      shieldPaint.setStrokeWidth(3f);
      shieldPaint.setAlpha(180);
      canvas.drawCircle(player.x, player.y, player.radius + 8f, shieldPaint);
    }
  }

  private void renderEnemies(Canvas canvas) {
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      float dx = player.x - enemy.x;
      float dy = player.y - enemy.y;
      float dist = (float) Math.hypot(dx, dy);
      float dirX = dist == 0f ? 1f : dx / dist;
      float dirY = dist == 0f ? 0f : dy / dist;
      Paint paint = enemy.elite ? paintElite : paintEnemy;
      drawZombie(canvas, enemy.x, enemy.y, enemy.radius, dirX, dirY, paint, enemy.elite);
    }
  }

  private void renderBullets(Canvas canvas) {
    for (Bullet bullet : bullets) {
      if (!bullet.active) {
        continue;
      }
      Paint bulletPaint = new Paint();
      bulletPaint.setColor(bullet.color);
      bulletPaint.setAntiAlias(true);
      canvas.drawCircle(bullet.x, bullet.y, bullet.radius, bulletPaint);
      if (bullet.radius > 7f) {
        Paint glowPaint = new Paint(bulletPaint);
        glowPaint.setAlpha(100);
        canvas.drawCircle(bullet.x, bullet.y, bullet.radius * 1.5f, glowPaint);
      }
    }
  }

  private void renderObstacles(Canvas canvas) {
    if (width <= 0 || height <= 0) {
      return;
    }
    Paint obsPaint = new Paint();
    obsPaint.setAntiAlias(true);
    android.graphics.RectF rect = new android.graphics.RectF();
    for (Obstacle obs : obstacles) {
      if (!obs.active) {
        continue;
      }

      // Render different wilderness elements based on type
      switch (obs.type) {
        case Obstacle.TYPE_HOUSE: {
          // Dilapidated wooden house: wooden walls + roof + doors/windows
          // Walls
          obsPaint.setStyle(Paint.Style.FILL);
          obsPaint.setColor(0xFF8B5A2B); // Dark wood color
          rect.set(obs.x - obs.width * 0.5f, obs.y - obs.height * 0.5f,
              obs.x + obs.width * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, 6f, 6f, obsPaint);

          // Roof
          Paint roofPaint = new Paint(obsPaint);
          roofPaint.setColor(0xFF5A3A1A);
          float left = obs.x - obs.width * 0.55f;
          float right = obs.x + obs.width * 0.55f;
          float top = obs.y - obs.height * 0.6f;
          float midY = obs.y - obs.height * 0.1f;
          android.graphics.Path roofPath = new android.graphics.Path();
          roofPath.moveTo(left, midY);
          roofPath.lineTo(obs.x, top);
          roofPath.lineTo(right, midY);
          roofPath.close();
          canvas.drawPath(roofPath, roofPaint);

          // Door
          Paint doorPaint = new Paint(obsPaint);
          doorPaint.setColor(0xFF3C2A1A);
          float doorW = obs.width * 0.18f;
          float doorH = obs.height * 0.35f;
          rect.set(obs.x - doorW * 0.5f, obs.y + obs.height * 0.5f - doorH,
              obs.x + doorW * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, 3f, 3f, doorPaint);

          // Windows
          Paint windowPaint = new Paint(obsPaint);
          windowPaint.setColor(0xFFB3E6FF);
          float winSize = obs.width * 0.18f;
          rect.set(obs.x - obs.width * 0.3f - winSize * 0.5f, obs.y - winSize * 0.5f,
              obs.x - obs.width * 0.3f + winSize * 0.5f, obs.y + winSize * 0.5f);
          canvas.drawRoundRect(rect, 3f, 3f, windowPaint);
          rect.set(obs.x + obs.width * 0.3f - winSize * 0.5f, obs.y - winSize * 0.5f,
              obs.x + obs.width * 0.3f + winSize * 0.5f, obs.y + winSize * 0.5f);
          canvas.drawRoundRect(rect, 3f, 3f, windowPaint);
          break;
        }

        case Obstacle.TYPE_TREE: {
          // Tree: trunk + crown
          // Trunk
          Paint trunkPaint = new Paint(obsPaint);
          trunkPaint.setColor(0xFF5A3A1A);
          float trunkW = obs.width * 0.18f;
          float trunkH = obs.height * 0.5f;
          rect.set(obs.x - trunkW * 0.5f, obs.y + obs.height * 0.5f - trunkH,
              obs.x + trunkW * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, 3f, 3f, trunkPaint);

          // Crown
          Paint leafPaint = new Paint(obsPaint);
          leafPaint.setColor(0xFF2E8B57);
          float crownR = Math.max(obs.width, obs.height) * 0.35f;
          canvas.drawCircle(obs.x, obs.y - obs.height * 0.1f, crownR, leafPaint);
          leafPaint.setColor(0xFF3CB371);
          canvas.drawCircle(obs.x - crownR * 0.4f, obs.y - obs.height * 0.05f, crownR * 0.7f, leafPaint);
          canvas.drawCircle(obs.x + crownR * 0.4f, obs.y - obs.height * 0.05f, crownR * 0.6f, leafPaint);
          break;
        }

        case Obstacle.TYPE_ROCK: {
          // Rock: irregular gray stone
          Paint rockPaint = new Paint(obsPaint);
          rockPaint.setColor(0xFF7F8C8D);
          rect.set(obs.x - obs.width * 0.5f, obs.y - obs.height * 0.5f,
              obs.x + obs.width * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, obs.width * 0.4f, obs.height * 0.4f, rockPaint);
          rockPaint.setColor(0xFFBDC3C7);
          canvas.drawCircle(obs.x - obs.width * 0.15f, obs.y - obs.height * 0.15f,
              Math.min(obs.width, obs.height) * 0.18f, rockPaint);
          break;
        }

        case Obstacle.TYPE_RIVER: {
          // River: blue wide rectangle with gradient, as terrain blocking + decoration
          Paint riverPaint = new Paint(obsPaint);
          riverPaint.setStyle(Paint.Style.FILL);
          riverPaint.setColor(0xFF1E90FF);
          rect.set(obs.x - obs.width * 0.5f, obs.y - obs.height * 0.5f,
              obs.x + obs.width * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, obs.height * 0.6f, obs.height * 0.6f, riverPaint);

          // Water surface highlight
          Paint highlight = new Paint(obsPaint);
          highlight.setColor(0x66FFFFFF);
          float hx1 = rect.left + obs.width * 0.1f;
          float hx2 = rect.right - obs.width * 0.1f;
          float hy = rect.centerY() - obs.height * 0.15f;
          canvas.drawLine(hx1, hy, hx2, hy, highlight);
          hy = rect.centerY() + obs.height * 0.05f;
          canvas.drawLine(hx1, hy, hx2, hy, highlight);
          break;
        }

        default: {
          // Old crates/walls etc: unified more refined block rendering
          obsPaint.setStyle(Paint.Style.FILL);
          obsPaint.setColor(obs.destructible ? 0xFF4A5568 : 0xFF2A3542);
          rect.set(obs.x - obs.width * 0.5f, obs.y - obs.height * 0.5f,
              obs.x + obs.width * 0.5f, obs.y + obs.height * 0.5f);
          canvas.drawRoundRect(rect, 4f, 4f, obsPaint);
          break;
        }
      }

      // Draw health bar for destructible objects (houses, crates, rocks, etc.)
      if (obs.destructible && obs.hp < obs.maxHp && obs.maxHp > 0f) {
        Paint hpPaint = new Paint();
        hpPaint.setColor(0xFFFF3D5A);
        float hpRatio = Math.max(0f, Math.min(1f, obs.hp / obs.maxHp));
        float barLeft = obs.x - obs.width * 0.5f;
        float barRight = barLeft + obs.width * hpRatio;
        float barTop = obs.y - obs.height * 0.5f - 4f;
        float barBottom = barTop + 2f;
        canvas.drawRect(barLeft, barTop, barRight, barBottom, hpPaint);
      }
    }
  }

  private void renderPlaceables(Canvas canvas) {
    if (width <= 0 || height <= 0) {
      return;
    }
    Paint placePaint = new Paint();
    placePaint.setAntiAlias(true);
    android.graphics.RectF rect = new android.graphics.RectF();
    for (Placeable place : placeables) {
      if (!place.active) continue;
      if (place.type == Placeable.TYPE_WALL) {
        placePaint.setColor(0xFF3A4A5A);
        rect.set(place.x - place.width * 0.5f, place.y - place.height * 0.5f,
            place.x + place.width * 0.5f, place.y + place.height * 0.5f);
        canvas.drawRoundRect(rect, 3f, 3f, placePaint);
      } else if (place.type == Placeable.TYPE_MINE) {
        placePaint.setColor(0xFFFF6B35);
        canvas.drawCircle(place.x, place.y, place.width * 0.5f, placePaint);
        placePaint.setColor(0xFFFFD23D);
        canvas.drawCircle(place.x, place.y, place.width * 0.3f, placePaint);
      } else if (place.type == Placeable.TYPE_TURRET) {
        placePaint.setColor(0xFF4A5568);
        canvas.drawCircle(place.x, place.y, place.width * 0.5f, placePaint);
        placePaint.setColor(0xFF00F5FF);
        canvas.drawCircle(place.x, place.y, place.width * 0.3f, placePaint);
      }
    }
  }

  private void renderPickups(Canvas canvas) {
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        continue;
      }
      if (pickup.type == Pickup.TYPE_ENERGY) {
        paint.setColor(paintPickupEnergy.getColor());
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
      } else if (pickup.type == Pickup.TYPE_COIN) {
        paint.setColor(paintPickupCoin.getColor());
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
      } else if (pickup.type == Pickup.TYPE_MEDKIT) {
        paint.setColor(paintPickupMedkit.getColor());
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
      } else if (pickup.type == Pickup.TYPE_WEAPON) {
        paint.setColor(availableWeapons[pickup.weaponType].color);
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f);
        canvas.drawCircle(pickup.x, pickup.y, pickup.radius, strokePaint);
      } else if (pickup.type == Pickup.TYPE_WALL_ITEM) {
        paint.setColor(0xFF3A4A5A);
        canvas.drawRect(pickup.x - pickup.radius * 0.7f, pickup.y - pickup.radius * 0.7f,
            pickup.x + pickup.radius * 0.7f, pickup.y + pickup.radius * 0.7f, paint);
      } else if (pickup.type == Pickup.TYPE_MINE_ITEM) {
        paint.setColor(0xFFFF6B35);
      canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
      }
    }
  }

  private void renderParticles(Canvas canvas) {
    for (Particle particle : particles) {
      if (!particle.active) {
        continue;
      }
      canvas.drawCircle(particle.x, particle.y, particle.size, paintParticle);
    }
  }

  private void renderTexts(Canvas canvas) {
    for (FloatingText text : texts) {
      if (!text.active) {
        continue;
      }
      canvas.drawText(text.text, text.x, text.y, paintText);
    }
  }

  private void drawHumanoid(Canvas canvas, float x, float y, float radius, float dirX, float dirY, Paint paint, boolean isPlayer) {
    Paint bodyPaint = new Paint(paint);
    bodyPaint.setAntiAlias(true);
    Paint headPaint = new Paint(bodyPaint);
    headPaint.setColor(isPlayer ? 0xFF00F5FF : 0xFFC7C7FF);
    Paint armPaint = new Paint(bodyPaint);
    float bodyWidth = radius * 0.7f;
    float bodyHeight = radius * 1.2f;
    float headRadius = radius * 0.4f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.3f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.5f, 4f, 4f, bodyPaint);
    canvas.drawCircle(x, y - radius * 0.6f, headRadius, headPaint);
    float armLength = radius * 0.6f;
    float armWidth = radius * 0.15f;
    float armY = y - bodyHeight * 0.1f;
    float armOffsetX = dirX * armLength * 0.3f;
    float armOffsetY = dirY * armLength * 0.3f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f - armOffsetX - armWidth * 0.5f, armY - armWidth * 0.5f,
        x - bodyWidth * 0.5f - armOffsetX + armWidth * 0.5f, armY + armLength + armWidth * 0.5f, 2f, 2f, armPaint);
    canvas.drawRoundRect(x + bodyWidth * 0.5f + armOffsetX - armWidth * 0.5f, armY - armWidth * 0.5f,
        x + bodyWidth * 0.5f + armOffsetX + armWidth * 0.5f, armY + armLength + armWidth * 0.5f, 2f, 2f, armPaint);
    float legWidth = radius * 0.12f;
    float legHeight = radius * 0.5f;
    canvas.drawRect(x - bodyWidth * 0.25f, y + bodyHeight * 0.3f,
        x - bodyWidth * 0.25f + legWidth, y + bodyHeight * 0.3f + legHeight, bodyPaint);
    canvas.drawRect(x + bodyWidth * 0.25f - legWidth, y + bodyHeight * 0.3f,
        x + bodyWidth * 0.25f, y + bodyHeight * 0.3f + legHeight, bodyPaint);
    if (isPlayer) {
      Paint weaponPaint = new Paint();
      weaponPaint.setColor(currentWeapon.color);
      weaponPaint.setAntiAlias(true);
      weaponPaint.setStrokeWidth(4f);
      weaponPaint.setStyle(Paint.Style.STROKE);
      float weaponLength = radius * 0.8f;
      float weaponX = x + dirX * weaponLength;
      float weaponY = y + dirY * weaponLength;
      canvas.drawLine(x + dirX * radius * 0.3f, y + dirY * radius * 0.3f, weaponX, weaponY, weaponPaint);
    }
  }

  private void drawZombie(Canvas canvas, float x, float y, float radius, float dirX, float dirY, Paint paint, boolean elite) {
    Paint bodyPaint = new Paint(paint);
    bodyPaint.setAntiAlias(true);
    Paint headPaint = new Paint(bodyPaint);
    headPaint.setColor(elite ? 0xFFFF3DFF : 0xFF8A0000);
    Paint armPaint = new Paint(bodyPaint);
    armPaint.setColor(elite ? 0xFFFF6BFF : 0xFF6A0000);
    float bodyWidth = radius * 0.75f;
    float bodyHeight = radius * 1.3f;
    float headRadius = radius * 0.42f;
    canvas.drawRoundRect(x - bodyWidth * 0.5f, y - bodyHeight * 0.35f,
        x + bodyWidth * 0.5f, y + bodyHeight * 0.55f, 5f, 5f, bodyPaint);
    canvas.drawCircle(x, y - radius * 0.65f, headRadius, headPaint);
    float armLength = radius * 0.7f;
    float armWidth = radius * 0.16f;
    float armY = y - bodyHeight * 0.15f;
    float armStretch = 0.4f;
    float armOffsetX = dirX * armLength * armStretch;
    float armOffsetY = dirY * armLength * armStretch;
    canvas.drawRoundRect(x - bodyWidth * 0.6f - armOffsetX - armWidth * 0.5f, armY - armWidth * 0.5f,
        x - bodyWidth * 0.6f - armOffsetX + armWidth * 0.5f, armY + armLength + armWidth * 0.5f, 3f, 3f, armPaint);
    canvas.drawRoundRect(x + bodyWidth * 0.6f + armOffsetX - armWidth * 0.5f, armY - armWidth * 0.5f,
        x + bodyWidth * 0.6f + armOffsetX + armWidth * 0.5f, armY + armLength + armWidth * 0.5f, 3f, 3f, armPaint);
    float legWidth = radius * 0.13f;
    float legHeight = radius * 0.55f;
    canvas.drawRect(x - bodyWidth * 0.3f, y + bodyHeight * 0.35f,
        x - bodyWidth * 0.3f + legWidth, y + bodyHeight * 0.35f + legHeight, bodyPaint);
    canvas.drawRect(x + bodyWidth * 0.3f - legWidth, y + bodyHeight * 0.35f,
        x + bodyWidth * 0.3f, y + bodyHeight * 0.35f + legHeight, bodyPaint);
    if (elite) {
      Paint glowPaint = new Paint(paint);
      glowPaint.setStyle(Paint.Style.STROKE);
      glowPaint.setStrokeWidth(3f);
      glowPaint.setAlpha(150);
      canvas.drawCircle(x, y, radius + 4f, glowPaint);
    }
  }

  private void renderControls(Canvas canvas) {
    float leftX = 140f;
    float leftY = height - 140f;
    float rightX = width - 160f;
    float rightY = height - 140f;
    // Draw left move joystick
    Paint bgPaint = new Paint(paintJoystick);
    bgPaint.setAlpha(120);
    canvas.drawCircle(leftX, leftY, 90f, bgPaint);
    Paint strokePaint = new Paint(paintJoystick);
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(4f);
    strokePaint.setAlpha(200);
    canvas.drawCircle(leftX, leftY, 90f, strokePaint);
    // Move joystick knob (show center point even when inactive)
    float knobX = moveActive ? (leftX + moveInputX * 60f) : leftX;
    float knobY = moveActive ? (leftY + moveInputY * 60f) : leftY;
    Paint knobPaint = new Paint(paintAim);
    knobPaint.setStyle(Paint.Style.FILL);
    knobPaint.setAlpha(moveActive ? 255 : 180);
    canvas.drawCircle(knobX, knobY, 32f, knobPaint);
    // Draw right shoot joystick (same style as move joystick)
    canvas.drawCircle(rightX, rightY, 90f, bgPaint);
    canvas.drawCircle(rightX, rightY, 90f, strokePaint);
    // Shoot joystick knob
    float shootKnobX = shootActive ? (rightX + shootInputX * 60f) : rightX;
    float shootKnobY = shootActive ? (rightY + shootInputY * 60f) : rightY;
    Paint shootKnobPaint = new Paint(paintAim);
    shootKnobPaint.setStyle(Paint.Style.FILL);
    shootKnobPaint.setAlpha(shootActive ? 255 : 180);
    canvas.drawCircle(shootKnobX, shootKnobY, 32f, shootKnobPaint);
    Paint weaponTextPaint = new Paint(paintText);
    weaponTextPaint.setTextSize(16f);
    weaponTextPaint.setColor(currentWeapon.color);
    canvas.drawText(currentWeapon.name, rightX, rightY - 130f, weaponTextPaint);
    if (wallItemCount > 0 || mineItemCount > 0) {
      Paint itemTextPaint = new Paint(paintText);
      itemTextPaint.setTextSize(14f);
      itemTextPaint.setColor(0xFFC7C7FF);
      float itemY = rightY - 150f;
      if (wallItemCount > 0) {
        canvas.drawText("Wall: " + wallItemCount, rightX, itemY, itemTextPaint);
        itemY -= 20f;
      }
      if (mineItemCount > 0) {
        canvas.drawText("Mine: " + mineItemCount, rightX, itemY, itemTextPaint);
      }
    }
  }

  private void notifyState() {
    if (listener != null) {
      listener.onStateChanged(state);
    }
  }
}
