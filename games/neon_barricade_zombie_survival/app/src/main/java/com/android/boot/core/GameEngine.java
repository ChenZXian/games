package com.android.boot.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.android.boot.R;
import com.android.boot.audio.SoundManager;
import com.android.boot.entity.Bullet;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Pickup;
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

  private int width;
  private int height;
  private float spawnTimer;
  private float eliteTimer;
  private float nextUpgradeTime;
  private boolean moveActive;
  private boolean aimActive;
  private float moveInputX;
  private float moveInputY;
  private float aimInputX;
  private float aimInputY;
  private float aimDirX = 1f;
  private float aimDirY = 0f;

  private int kills;
  private int score;
  private boolean gameOverSent;

  private float damageMultiplier = 1f;
  private float fireRate = 4f;
  private float moveSpeedMultiplier = 1f;
  private float maxHpMultiplier = 1f;
  private float regenRate = 0f;
  private int pierce = 0;
  private int multishot = 1;
  private float knockback = 60f;
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
    soundManager = new SoundManager(context);
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
    updateSpawning(delta);
    updateEnemies(delta);
    updateBullets(delta);
    updatePickups(delta);
    updateParticles(delta);
    updateTexts(delta);
    updateAutoAim();

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
    canvas.drawColor(0xFF07070C);
    renderPickups(canvas);
    renderBullets(canvas);
    renderEnemies(canvas);
    renderParticles(canvas);
    renderPlayer(canvas);
    renderTexts(canvas);
    renderControls(canvas);
  }

  public void applyUpgrade(int index) {
    if (state != GameState.UPGRADE) {
      return;
    }
    int upgrade = currentUpgrades[index];
    if (upgrade == 0) {
      damageMultiplier += 0.2f;
    } else if (upgrade == 1) {
      fireRate += 0.5f;
    } else if (upgrade == 2) {
      moveSpeedMultiplier += 0.1f;
    } else if (upgrade == 3) {
      maxHpMultiplier += 0.15f;
      player.maxHp = 120f * maxHpMultiplier;
      player.hp = Math.min(player.hp + 20f, player.maxHp);
    } else if (upgrade == 4) {
      regenRate += 0.4f;
    } else if (upgrade == 5) {
      pierce += 1;
    } else if (upgrade == 6) {
      multishot = Math.min(4, multishot + 1);
    } else if (upgrade == 7) {
      knockback += 20f;
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
    player.x += moveInputX * speed * delta;
    player.y += moveInputY * speed * delta;
    if (player.x < player.radius) {
      player.x = player.radius;
    }
    if (player.x > width - player.radius) {
      player.x = width - player.radius;
    }
    if (player.y < player.radius) {
      player.y = player.radius;
    }
    if (player.y > height - player.radius) {
      player.y = height - player.radius;
    }
    if (regenRate > 0f) {
      player.hp = Math.min(player.maxHp, player.hp + regenRate * delta);
    }
    if (player.energy < player.maxEnergy) {
      player.energy = Math.min(player.maxEnergy, player.energy + 10f * delta);
    }
    if (fireCooldown.isReady()) {
      fireCooldown.start(1f / fireRate);
      fireBurst();
    }
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
      enemy.x += nx * enemy.speed * delta;
      enemy.y += ny * enemy.speed * delta;
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
          enemy.x += nx * knockback * 0.1f;
          enemy.y += ny * knockback * 0.1f;
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
    if (aimActive) {
      float len = (float) Math.hypot(aimInputX, aimInputY);
      if (len > 0.1f) {
        aimDirX = aimInputX / len;
        aimDirY = aimInputY / len;
      }
      return;
    }
    float closest = Float.MAX_VALUE;
    Enemy target = null;
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      float dx = enemy.x - player.x;
      float dy = enemy.y - player.y;
      float dist2 = dx * dx + dy * dy;
      if (dist2 < closest) {
        closest = dist2;
        target = enemy;
      }
    }
    if (target != null) {
      float dx = target.x - player.x;
      float dy = target.y - player.y;
      float len = (float) Math.hypot(dx, dy);
      if (len > 0f) {
        aimDirX = dx / len;
        aimDirY = dy / len;
      }
    }
  }

  private void fireBurst() {
    float baseDamage = 12f * damageMultiplier;
    soundManager.playShoot();
    if (multishot <= 1) {
      spawnBullet(aimDirX, aimDirY, baseDamage);
      return;
    }
    float spread = 0.2f;
    int count = multishot;
    for (int i = 0; i < count; i++) {
      float t = count == 1 ? 0f : (float) i / (count - 1);
      float angle = (t - 0.5f) * spread;
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
    float speed = 720f;
    bullet.spawn(player.x + dirX * player.radius, player.y + dirY * player.radius, dirX * speed, dirY * speed, damage, pierce);
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
    if (roll < 0.35f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_ENERGY, 20f);
    } else if (roll < 0.6f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_COIN, 10f);
    } else if (roll < 0.7f) {
      spawnPickup(enemy.x, enemy.y, Pickup.TYPE_MEDKIT, 18f);
    }
  }

  private void applyPickup(Pickup pickup) {
    if (pickup.type == Pickup.TYPE_ENERGY) {
      player.energy = Math.min(player.maxEnergy, player.energy + pickup.value);
    } else if (pickup.type == Pickup.TYPE_COIN) {
      score += (int) pickup.value;
    } else if (pickup.type == Pickup.TYPE_MEDKIT) {
      player.hp = Math.min(player.maxHp, player.hp + pickup.value);
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
    damageMultiplier = 1f;
    fireRate = 4f;
    moveSpeedMultiplier = 1f;
    maxHpMultiplier = 1f;
    regenRate = 0f;
    pierce = 0;
    multishot = 1;
    knockback = 60f;
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
  }

  private void renderPlayer(Canvas canvas) {
    canvas.drawCircle(player.x, player.y, player.radius, paintPlayer);
    if (player.shield > 0f) {
      paintJoystick.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(player.x, player.y, player.radius + 8f, paintJoystick);
    }
  }

  private void renderEnemies(Canvas canvas) {
    for (Enemy enemy : enemies) {
      if (!enemy.active) {
        continue;
      }
      canvas.drawCircle(enemy.x, enemy.y, enemy.radius, enemy.elite ? paintElite : paintEnemy);
    }
  }

  private void renderBullets(Canvas canvas) {
    for (Bullet bullet : bullets) {
      if (!bullet.active) {
        continue;
      }
      canvas.drawCircle(bullet.x, bullet.y, bullet.radius, paintBullet);
    }
  }

  private void renderPickups(Canvas canvas) {
    for (Pickup pickup : pickups) {
      if (!pickup.active) {
        continue;
      }
      Paint paint = pickup.type == Pickup.TYPE_ENERGY ? paintPickupEnergy
          : pickup.type == Pickup.TYPE_COIN ? paintPickupCoin : paintPickupMedkit;
      canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
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

  private void renderControls(Canvas canvas) {
    float leftX = 140f;
    float leftY = height - 140f;
    float rightX = width - 160f;
    float rightY = height - 180f;
    canvas.drawCircle(leftX, leftY, 90f, paintJoystick);
    if (moveActive) {
      float knobX = leftX + moveInputX * 60f;
      float knobY = leftY + moveInputY * 60f;
      canvas.drawCircle(knobX, knobY, 32f, paintAim);
    }
    canvas.drawCircle(rightX, rightY, 110f, paintJoystick);
    float aimX = rightX + aimDirX * 60f;
    float aimY = rightY + aimDirY * 60f;
    canvas.drawCircle(aimX, aimY, 26f, paintAim);
  }

  private void notifyState() {
    if (listener != null) {
      listener.onStateChanged(state);
    }
  }
}
