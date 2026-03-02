package com.android.boot.core;

import com.android.boot.entity.Enemy;
import com.android.boot.entity.Projectile;
import com.android.boot.fx.Particle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {
  public static class HudData {
    public int wave;
    public int score;
    public int gold;
    public int hp;
    public int maxHp;
    public int energy;
    public int maxEnergy;
  }

  public static class UpgradeChoice {
    public String a;
    public String b;
    public String c;
  }

  private final Random random = new Random();
  private final List<Enemy> enemies = new ArrayList<>();
  private final List<Projectile> projectiles = new ArrayList<>();
  private final List<Particle> particles = new ArrayList<>();
  private final float[] laneSpawnX = new float[3];
  private final float[] laneSpawnY = new float[3];
  private final float[] laneFreeze = new float[3];
  private final float[] pushVecX = new float[3];
  private final float[] pushVecY = new float[3];
  private final float[] oilX = new float[6];
  private final float[] oilY = new float[6];
  private final float[] oilR = new float[6];
  private final float[] oilT = new float[6];
  private final HudData hud = new HudData();
  private final UpgradeChoice upgradeChoice = new UpgradeChoice();
  private GameState state = GameState.LEVEL_SELECT;
  private boolean upgradeVisible;
  private int activeOil;
  private int wave;
  private int remainingInWave;
  private float spawnTimer;
  private float baseSpawnGap;
  private float castleX = 0.5f;
  private float castleY = 0.5f;
  private float attackDamage = 26f;
  private float attackFireRate = 1.6f;
  private float attackCooldown;
  private int attackPierce = 1;
  private float attackSplash = 0f;
  private float energyGain = 6f;
  private float oilCooldown = 0f;
  private float freezeCooldown = 0f;
  private float pushCooldown = 0f;
  private float oilCooldownMax = 8f;
  private float freezeCooldownMax = 10f;
  private float pushCooldownMax = 9f;

  public GameEngine() {
    for (int i = 0; i < 64; i++) {
      enemies.add(new Enemy());
      projectiles.add(new Projectile());
      particles.add(new Particle());
    }
    resetRun();
  }

  public void resetRun() {
    wave = 0;
    hud.score = 0;
    hud.gold = 0;
    hud.maxHp = 250;
    hud.hp = hud.maxHp;
    hud.maxEnergy = 100;
    hud.energy = 30;
    attackDamage = 26f;
    attackFireRate = 1.6f;
    attackCooldown = 0f;
    attackPierce = 1;
    attackSplash = 0f;
    energyGain = 6f;
    oilCooldownMax = 8f;
    freezeCooldownMax = 10f;
    pushCooldownMax = 9f;
    oilCooldown = 0f;
    freezeCooldown = 0f;
    pushCooldown = 0f;
    activeOil = 0;
    upgradeVisible = false;
    clearEntities();
    configureLanes();
    state = GameState.LEVEL_SELECT;
    hud.wave = 0;
  }

  public void startGame() {
    resetRun();
    state = GameState.PLAYING;
    nextWave();
  }

  public void openDeckSelect() {
    state = GameState.DECK_SELECT;
  }

  public void pause() {
    if (state == GameState.PLAYING && !upgradeVisible) {
      state = GameState.PAUSED;
    }
  }

  public void resume() {
    if (state == GameState.PAUSED) {
      state = GameState.PLAYING;
    }
  }

  public void goToMenu() {
    state = GameState.LEVEL_SELECT;
    upgradeVisible = false;
    clearEntities();
  }

  public GameState getState() {
    return state;
  }

  public HudData getHud() {
    return hud;
  }

  public List<Enemy> getEnemies() {
    return enemies;
  }

  public List<Projectile> getProjectiles() {
    return projectiles;
  }

  public List<Particle> getParticles() {
    return particles;
  }

  public boolean isUpgradeVisible() {
    return upgradeVisible;
  }

  public UpgradeChoice getUpgradeChoice() {
    return upgradeChoice;
  }

  public int getOilCount() {
    return activeOil;
  }

  public float getOilX(int i) {
    return oilX[i];
  }

  public float getOilY(int i) {
    return oilY[i];
  }

  public float getOilR(int i) {
    return oilR[i];
  }

  public float getCastleX() {
    return castleX;
  }

  public float getCastleY() {
    return castleY;
  }

  public boolean canUseOil() {
    return oilCooldown <= 0f && hud.energy >= 25;
  }

  public boolean canUseFreeze() {
    return freezeCooldown <= 0f && hud.energy >= 30;
  }

  public boolean canUsePush() {
    return pushCooldown <= 0f && hud.energy >= 35;
  }

  public float oilCooldownRatio() {
    return Math.max(0f, oilCooldown / oilCooldownMax);
  }

  public float freezeCooldownRatio() {
    return Math.max(0f, freezeCooldown / freezeCooldownMax);
  }

  public float pushCooldownRatio() {
    return Math.max(0f, pushCooldown / pushCooldownMax);
  }

  public void update(float dt) {
    if (state != GameState.PLAYING || upgradeVisible) {
      return;
    }
    if (dt > 0.05f) {
      dt = 0.05f;
    }
    attackCooldown -= dt;
    oilCooldown -= dt;
    freezeCooldown -= dt;
    pushCooldown -= dt;
    hud.energy += (int) (energyGain * dt);
    if (hud.energy > hud.maxEnergy) {
      hud.energy = hud.maxEnergy;
    }
    for (int i = 0; i < 3; i++) {
      if (laneFreeze[i] > 0f) {
        laneFreeze[i] -= dt;
      }
    }
    updateOil(dt);
    spawnTimer -= dt;
    if (remainingInWave > 0 && spawnTimer <= 0f) {
      spawnEnemy();
      remainingInWave--;
      spawnTimer = Math.max(0.22f, baseSpawnGap - wave * 0.03f);
    }
    updateProjectiles(dt);
    updateEnemies(dt);
    updateParticles(dt);
    if (hud.hp <= 0) {
      state = GameState.GAME_OVER;
    }
    if (state == GameState.PLAYING && remainingInWave <= 0 && countAliveEnemies() == 0) {
      offerUpgrade();
    }
  }

  public void fireAt(float tx, float ty) {
    if (state != GameState.PLAYING || upgradeVisible || attackCooldown > 0f) {
      return;
    }
    float dx = tx - castleX;
    float dy = ty - castleY;
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < 0.01f) {
      return;
    }
    Projectile p = allocateProjectile();
    if (p == null) {
      return;
    }
    p.x = castleX;
    p.y = castleY;
    p.vx = dx / len * 1.15f;
    p.vy = dy / len * 1.15f;
    p.radius = 0.014f;
    p.damage = attackDamage;
    p.life = 1.8f;
    p.pierce = attackPierce;
    p.alive = true;
    attackCooldown = 1f / attackFireRate;
  }

  public void useOil() {
    if (!canUseOil() || state != GameState.PLAYING || upgradeVisible) {
      return;
    }
    hud.energy -= 25;
    oilCooldown = oilCooldownMax;
    for (int lane = 0; lane < 3; lane++) {
      addOil((laneSpawnX[lane] + castleX) * 0.5f, (laneSpawnY[lane] + castleY) * 0.5f, 0.08f, 4.2f);
    }
  }

  public void useFreeze() {
    if (!canUseFreeze() || state != GameState.PLAYING || upgradeVisible) {
      return;
    }
    hud.energy -= 30;
    freezeCooldown = freezeCooldownMax;
    int lane = random.nextInt(3);
    laneFreeze[lane] = 2.7f;
  }

  public void usePush(float dx, float dy) {
    if (!canUsePush() || state != GameState.PLAYING || upgradeVisible) {
      return;
    }
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < 0.01f) {
      return;
    }
    hud.energy -= 35;
    pushCooldown = pushCooldownMax;
    float nx = dx / len;
    float ny = dy / len;
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      float ex = enemy.x - castleX;
      float ey = enemy.y - castleY;
      float dot = ex * nx + ey * ny;
      if (dot < 0f) {
        continue;
      }
      float dist = (float) Math.sqrt(ex * ex + ey * ey);
      if (dist > 0.36f) {
        continue;
      }
      enemy.x += nx * 0.11f;
      enemy.y += ny * 0.11f;
      if (enemy.x < 0.02f) {
        enemy.x = 0.02f;
      }
      if (enemy.x > 0.98f) {
        enemy.x = 0.98f;
      }
      if (enemy.y < 0.02f) {
        enemy.y = 0.02f;
      }
      if (enemy.y > 0.98f) {
        enemy.y = 0.98f;
      }
    }
  }

  public void chooseUpgrade(int index) {
    if (!upgradeVisible) {
      return;
    }
    if (index == 0) {
      attackDamage += 6f;
      attackFireRate += 0.25f;
      attackPierce += 1;
      attackSplash += 0.012f;
    } else if (index == 1) {
      hud.maxHp += 40;
      hud.hp += 55;
      if (hud.hp > hud.maxHp) {
        hud.hp = hud.maxHp;
      }
    } else {
      energyGain += 2f;
      oilCooldownMax = Math.max(4.5f, oilCooldownMax - 0.8f);
      freezeCooldownMax = Math.max(6f, freezeCooldownMax - 0.8f);
      pushCooldownMax = Math.max(5f, pushCooldownMax - 0.8f);
      hud.energy += 25;
      if (hud.energy > hud.maxEnergy) {
        hud.energy = hud.maxEnergy;
      }
    }
    upgradeVisible = false;
    nextWave();
  }

  private void updateProjectiles(float dt) {
    for (Projectile p : projectiles) {
      if (!p.alive) {
        continue;
      }
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.life -= dt;
      if (p.life <= 0f || p.x < -0.05f || p.x > 1.05f || p.y < -0.05f || p.y > 1.05f) {
        p.alive = false;
        continue;
      }
      for (Enemy enemy : enemies) {
        if (!enemy.alive) {
          continue;
        }
        float dx = enemy.x - p.x;
        float dy = enemy.y - p.y;
        float rr = enemy.radius + p.radius;
        if (dx * dx + dy * dy <= rr * rr) {
          damageEnemy(enemy, p.damage);
          p.pierce--;
          if (attackSplash > 0f) {
            applySplash(enemy.x, enemy.y, p.damage * 0.55f, attackSplash + 0.04f);
          }
          if (p.pierce < 0) {
            p.alive = false;
            break;
          }
        }
      }
    }
  }

  private void updateEnemies(float dt) {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      float freezeFactor = laneFreeze[enemy.lane] > 0f ? 0f : 1f;
      float dx = castleX - enemy.x;
      float dy = castleY - enemy.y;
      float len = (float) Math.sqrt(dx * dx + dy * dy);
      if (len > 0.0001f) {
        enemy.x += dx / len * enemy.speed * freezeFactor * dt;
        enemy.y += dy / len * enemy.speed * freezeFactor * dt;
      }
      for (int i = 0; i < activeOil; i++) {
        float ox = oilX[i] - enemy.x;
        float oy = oilY[i] - enemy.y;
        if (ox * ox + oy * oy <= oilR[i] * oilR[i]) {
          damageEnemy(enemy, 14f * dt);
          break;
        }
      }
      float cd = castleX - enemy.x;
      float ce = castleY - enemy.y;
      float rr = enemy.radius + 0.055f;
      if (cd * cd + ce * ce <= rr * rr) {
        enemy.alive = false;
        hud.hp -= 9;
      }
    }
  }

  private void updateParticles(float dt) {
    for (Particle particle : particles) {
      if (!particle.alive) {
        continue;
      }
      particle.x += particle.vx * dt;
      particle.y += particle.vy * dt;
      particle.life -= dt;
      if (particle.life <= 0f) {
        particle.alive = false;
      }
    }
  }

  private void updateOil(float dt) {
    int write = 0;
    for (int i = 0; i < activeOil; i++) {
      oilT[i] -= dt;
      if (oilT[i] > 0f) {
        oilX[write] = oilX[i];
        oilY[write] = oilY[i];
        oilR[write] = oilR[i];
        oilT[write] = oilT[i];
        write++;
      }
    }
    activeOil = write;
  }

  private void configureLanes() {
    laneSpawnX[0] = 0.5f;
    laneSpawnY[0] = 0.05f;
    laneSpawnX[1] = 0.95f;
    laneSpawnY[1] = 0.5f;
    laneSpawnX[2] = 0.05f;
    laneSpawnY[2] = 0.5f;
  }

  private void nextWave() {
    wave++;
    hud.wave = wave;
    remainingInWave = 8 + wave * 4;
    baseSpawnGap = 0.82f;
    spawnTimer = 0.6f;
  }

  private void offerUpgrade() {
    upgradeVisible = true;
    upgradeChoice.a = "Forge Weapons";
    upgradeChoice.b = "Reinforce Walls";
    upgradeChoice.c = "Arcane Training";
  }

  private void spawnEnemy() {
    Enemy enemy = allocateEnemy();
    if (enemy == null) {
      return;
    }
    int lane = random.nextInt(3);
    enemy.lane = lane;
    enemy.x = laneSpawnX[lane];
    enemy.y = laneSpawnY[lane];
    enemy.hp = 24f + wave * 3.2f;
    if (wave % 5 == 0 && random.nextFloat() < 0.4f) {
      enemy.hp *= 1.7f;
      enemy.radius = 0.024f;
      enemy.speed = 0.08f + wave * 0.003f;
    } else {
      enemy.radius = 0.018f;
      enemy.speed = 0.1f + wave * 0.003f;
    }
    enemy.alive = true;
  }

  private void damageEnemy(Enemy enemy, float damage) {
    enemy.hp -= damage;
    if (enemy.hp <= 0f && enemy.alive) {
      enemy.alive = false;
      hud.score += 12;
      hud.gold += 3;
      spawnParticles(enemy.x, enemy.y);
    }
  }

  private void applySplash(float x, float y, float damage, float radius) {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      float dx = enemy.x - x;
      float dy = enemy.y - y;
      if (dx * dx + dy * dy <= radius * radius) {
        damageEnemy(enemy, damage);
      }
    }
  }

  private void addOil(float x, float y, float radius, float duration) {
    if (activeOil >= oilX.length) {
      activeOil = oilX.length - 1;
    }
    oilX[activeOil] = x;
    oilY[activeOil] = y;
    oilR[activeOil] = radius;
    oilT[activeOil] = duration;
    activeOil++;
  }

  private Enemy allocateEnemy() {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        return enemy;
      }
    }
    return null;
  }

  private Projectile allocateProjectile() {
    for (Projectile projectile : projectiles) {
      if (!projectile.alive) {
        return projectile;
      }
    }
    return null;
  }

  private void spawnParticles(float x, float y) {
    for (int i = 0; i < 5; i++) {
      Particle particle = allocateParticle();
      if (particle == null) {
        return;
      }
      float angle = (float) (random.nextFloat() * Math.PI * 2.0);
      float speed = 0.08f + random.nextFloat() * 0.22f;
      particle.x = x;
      particle.y = y;
      particle.vx = (float) Math.cos(angle) * speed;
      particle.vy = (float) Math.sin(angle) * speed;
      particle.maxLife = 0.45f;
      particle.life = particle.maxLife;
      particle.alive = true;
    }
  }

  private Particle allocateParticle() {
    for (Particle particle : particles) {
      if (!particle.alive) {
        return particle;
      }
    }
    return null;
  }

  private int countAliveEnemies() {
    int c = 0;
    for (Enemy enemy : enemies) {
      if (enemy.alive) {
        c++;
      }
    }
    return c;
  }

  private void clearEntities() {
    for (Enemy enemy : enemies) {
      enemy.alive = false;
    }
    for (Projectile projectile : projectiles) {
      projectile.alive = false;
    }
    for (Particle particle : particles) {
      particle.alive = false;
    }
  }
}
