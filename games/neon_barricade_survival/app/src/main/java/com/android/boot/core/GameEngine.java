package com.android.boot.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.android.boot.audio.SoundManager;
import com.android.boot.entity.Bullet;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.Player;
import com.android.boot.fx.Particle;

import java.util.ArrayList;
import java.util.Random;

public class GameEngine {
    public static class GameStats {
        public float hp;
        public float maxHp;
        public float energy;
        public float maxEnergy;
        public int kills;
        public int score;
        public float timeSurvived;
    }

    private final Player player = new Player();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Pickup> pickups = new ArrayList<>();
    private final Bullet[] bullets = new Bullet[120];
    private final Particle[] particles = new Particle[120];
    private int bulletIndex;
    private int particleIndex;
    private float fireTimer;
    private float spawnTimer;
    private float eliteTimer;
    private float upgradeTimer;
    private float skillTimer;
    private float spawnInterval;
    private float difficultyTimer;
    private int width;
    private int height;
    private final Random random = new Random();
    private final GameStats stats = new GameStats();
    private GameState state = GameState.MENU;
    private final RectF bounds = new RectF();
    private String[] upgradeOptions = new String[3];
    private UpgradeType[] upgradeTypes = new UpgradeType[3];
    private SoundManager soundManager;

    public enum UpgradeType {
        DAMAGE,
        FIRE_RATE,
        MOVE_SPEED,
        MAX_HP,
        REGEN,
        PIERCE,
        MULTISHOT,
        KNOCKBACK,
        MAGNET,
        SHIELD,
        SKILL_COOLDOWN,
        SKILL_DAMAGE
    }

    public void setSoundManager(SoundManager manager) {
        soundManager = manager;
    }

    public GameStats getStats() {
        return stats;
    }

    public GameState getState() {
        return state;
    }

    public String[] getUpgradeOptions() {
        return upgradeOptions;
    }

    public void setScreenSize(int w, int h) {
        width = w;
        height = h;
        bounds.set(0f, 0f, width, height);
        if (state == GameState.MENU) {
            player.reset(width * 0.5f, height * 0.5f);
        }
    }

    public void startGame() {
        resetGame();
        state = GameState.PLAYING;
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public void restartGame() {
        resetGame();
        state = GameState.PLAYING;
    }

    public void update(float dt, float moveX, float moveY, float aimX, float aimY, boolean aiming) {
        if (state == GameState.PAUSED || state == GameState.MENU || state == GameState.GAME_OVER) {
            return;
        }
        if (state == GameState.UPGRADE) {
            return;
        }
        stats.timeSurvived += dt;
        player.energy = Math.min(player.maxEnergy, player.energy + dt * 8f);
        player.hp = Math.min(player.maxHp, player.hp + player.regen * dt);
        float moveLen = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        float dirX = moveLen > 0f ? moveX / moveLen : 0f;
        float dirY = moveLen > 0f ? moveY / moveLen : 0f;
        player.vx = dirX * player.speed;
        player.vy = dirY * player.speed;
        player.x = clamp(player.x + player.vx * dt, player.radius, width - player.radius);
        player.y = clamp(player.y + player.vy * dt, player.radius, height - player.radius);

        difficultyTimer += dt;
        spawnInterval = Math.max(0.45f, 1.5f - difficultyTimer * 0.01f);

        spawnTimer += dt;
        if (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval;
            spawnEnemy();
        }

        eliteTimer += dt;
        if (eliteTimer > 18f) {
            eliteTimer = 0f;
            spawnElite();
        }

        upgradeTimer += dt;
        if (upgradeTimer >= 45f) {
            upgradeTimer = 0f;
            state = GameState.UPGRADE;
            rollUpgrades();
            return;
        }

        skillTimer = Math.max(0f, skillTimer - dt);

        if (!aiming) {
            float[] autoDir = findAutoAim();
            aimX = autoDir[0];
            aimY = autoDir[1];
        }

        fireTimer += dt;
        float fireInterval = 1f / Math.max(1f, player.fireRate);
        if (fireTimer >= fireInterval) {
            fireTimer -= fireInterval;
            fireBullets(aimX, aimY);
        }

        updateBullets(dt);
        updateEnemies(dt);
        updatePickups(dt);
        updateParticles(dt);

        stats.hp = player.hp;
        stats.maxHp = player.maxHp;
        stats.energy = player.energy;
        stats.maxEnergy = player.maxEnergy;
    }

    public void triggerSkill() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (player.energy < 35f || skillTimer > 0f) {
            return;
        }
        player.energy -= 35f;
        skillTimer = player.skillCooldown;
        if (soundManager != null) {
            soundManager.playSkill();
        }
        float radius = 200f;
        for (Enemy enemy : enemies) {
            if (enemy == null) {
                continue;
            }
            float dx = enemy.x - player.x;
            float dy = enemy.y - player.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < radius) {
                enemy.hp -= player.skillDamage;
                float force = player.knockback + 200f;
                if (dist > 0f) {
                    enemy.x += dx / dist * force;
                    enemy.y += dy / dist * force;
                }
                spawnParticles(enemy.x, enemy.y, 6, 3f, 0.5f);
            }
        }
    }

    public void applyUpgrade(int index) {
        if (state != GameState.UPGRADE) {
            return;
        }
        UpgradeType type = upgradeTypes[index];
        if (type == null) {
            state = GameState.PLAYING;
            return;
        }
        if (type == UpgradeType.DAMAGE) {
            player.damage += 4f;
        } else if (type == UpgradeType.FIRE_RATE) {
            player.fireRate += 1.2f;
        } else if (type == UpgradeType.MOVE_SPEED) {
            player.speed += 20f;
        } else if (type == UpgradeType.MAX_HP) {
            player.maxHp += 18f;
            player.hp += 18f;
        } else if (type == UpgradeType.REGEN) {
            player.regen += 0.5f;
        } else if (type == UpgradeType.PIERCE) {
            player.pierce += 1;
        } else if (type == UpgradeType.MULTISHOT) {
            player.multishot = Math.min(2, player.multishot + 1);
        } else if (type == UpgradeType.KNOCKBACK) {
            player.knockback += 25f;
        } else if (type == UpgradeType.MAGNET) {
            player.magnetRange += 40f;
        } else if (type == UpgradeType.SHIELD) {
            player.shield = Math.min(0.5f, player.shield + 0.1f);
        } else if (type == UpgradeType.SKILL_COOLDOWN) {
            player.skillCooldown = Math.max(3f, player.skillCooldown - 1f);
        } else if (type == UpgradeType.SKILL_DAMAGE) {
            player.skillDamage += 12f;
        }
        state = GameState.PLAYING;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (canvas == null) {
            return;
        }
        canvas.drawColor(0xFF0B0B16);
        paint.setStyle(Paint.Style.FILL);
        drawArena(canvas, paint);
        drawPlayer(canvas, paint);
        drawBullets(canvas, paint);
        drawEnemies(canvas, paint);
        drawPickups(canvas, paint);
        drawParticles(canvas, paint);
    }

    public void setState(GameState nextState) {
        state = nextState;
    }

    private void resetGame() {
        enemies.clear();
        pickups.clear();
        bulletIndex = 0;
        particleIndex = 0;
        fireTimer = 0f;
        spawnTimer = 0f;
        eliteTimer = 0f;
        upgradeTimer = 0f;
        skillTimer = 0f;
        difficultyTimer = 0f;
        stats.kills = 0;
        stats.score = 0;
        stats.timeSurvived = 0f;
        for (int i = 0; i < bullets.length; i++) {
            bullets[i] = new Bullet();
        }
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
        player.reset(width * 0.5f, height * 0.5f);
        stats.hp = player.hp;
        stats.maxHp = player.maxHp;
        stats.energy = player.energy;
        stats.maxEnergy = player.maxEnergy;
    }

    private void spawnEnemy() {
        float baseHp = 24f + difficultyTimer * 0.8f;
        float baseSpeed = 70f + difficultyTimer * 0.5f;
        Enemy enemy = new Enemy();
        float[] spawn = randomEdge();
        enemy.init(spawn[0], spawn[1], baseSpeed, baseHp, false, 0);
        enemies.add(enemy);
    }

    private void spawnElite() {
        float baseHp = 50f + difficultyTimer * 1.2f;
        float baseSpeed = 90f + difficultyTimer * 0.6f;
        Enemy enemy = new Enemy();
        float[] spawn = randomEdge();
        enemy.init(spawn[0], spawn[1], baseSpeed, baseHp, true, random.nextInt(4));
        enemies.add(enemy);
    }

    private float[] randomEdge() {
        float[] spawn = new float[2];
        int edge = random.nextInt(4);
        if (edge == 0) {
            spawn[0] = -20f;
            spawn[1] = random.nextFloat() * height;
        } else if (edge == 1) {
            spawn[0] = width + 20f;
            spawn[1] = random.nextFloat() * height;
        } else if (edge == 2) {
            spawn[0] = random.nextFloat() * width;
            spawn[1] = -20f;
        } else {
            spawn[0] = random.nextFloat() * width;
            spawn[1] = height + 20f;
        }
        return spawn;
    }

    private void fireBullets(float aimX, float aimY) {
        float len = (float) Math.sqrt(aimX * aimX + aimY * aimY);
        float dirX = len > 0f ? aimX / len : 1f;
        float dirY = len > 0f ? aimY / len : 0f;
        int count = 1 + player.multishot;
        float spread = 0.18f;
        for (int i = 0; i < count; i++) {
            float offset = (i - (count - 1) * 0.5f) * spread;
            float cos = (float) Math.cos(offset);
            float sin = (float) Math.sin(offset);
            float sx = dirX * cos - dirY * sin;
            float sy = dirX * sin + dirY * cos;
            Bullet bullet = nextBullet();
            if (bullet != null) {
                bullet.fire(player.x, player.y, sx, sy, 520f, player.damage, player.pierce);
            }
        }
        if (soundManager != null) {
            soundManager.playShoot();
        }
    }

    private Bullet nextBullet() {
        for (int i = 0; i < bullets.length; i++) {
            Bullet bullet = bullets[bulletIndex];
            bulletIndex = (bulletIndex + 1) % bullets.length;
            if (!bullet.active) {
                return bullet;
            }
        }
        return null;
    }

    private void updateBullets(float dt) {
        for (Bullet bullet : bullets) {
            if (!bullet.active) {
                continue;
            }
            bullet.x += bullet.vx * bullet.speed * dt;
            bullet.y += bullet.vy * bullet.speed * dt;
            if (!bounds.contains(bullet.x, bullet.y)) {
                bullet.active = false;
                continue;
            }
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                float dx = enemy.x - bullet.x;
                float dy = enemy.y - bullet.y;
                float dist = dx * dx + dy * dy;
                float hitDist = (enemy.radius + bullet.radius);
                if (dist < hitDist * hitDist) {
                    enemy.hp -= bullet.damage;
                    float knock = player.knockback;
                    float len = (float) Math.sqrt(dist);
                    if (len > 0f) {
                        enemy.x += dx / len * knock;
                        enemy.y += dy / len * knock;
                    }
                    spawnParticles(enemy.x, enemy.y, 3, 2f, 0.3f);
                    if (soundManager != null) {
                        soundManager.playHit();
                    }
                    if (bullet.pierce > 0) {
                        bullet.pierce -= 1;
                    } else {
                        bullet.active = false;
                        break;
                    }
                }
            }
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            float dx = player.x - enemy.x;
            float dy = player.y - enemy.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float dirX = len > 0f ? dx / len : 0f;
            float dirY = len > 0f ? dy / len : 0f;
            enemy.x += dirX * enemy.speed * dt;
            enemy.y += dirY * enemy.speed * dt;
            enemy.hitCooldown = Math.max(0f, enemy.hitCooldown - dt);
            if (enemy.regen) {
                enemy.hp = Math.min(enemy.maxHp, enemy.hp + dt * 2f);
            }
            float hitRange = enemy.radius + player.radius;
            if (len < hitRange && enemy.hitCooldown <= 0f) {
                float damage = 12f * (1f - player.shield);
                player.hp -= damage;
                enemy.hitCooldown = 0.8f;
                if (player.hp <= 0f) {
                    state = GameState.GAME_OVER;
                }
            }
            if (enemy.hp <= 0f) {
                enemies.remove(i);
                stats.kills += 1;
                stats.score += enemy.elite ? 40 : 15;
                dropPickup(enemy.x, enemy.y);
                if (enemy.explode) {
                    explode(enemy.x, enemy.y);
                }
            }
        }
    }

    private void updatePickups(float dt) {
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pickup = pickups.get(i);
            if (!pickup.active) {
                pickups.remove(i);
                continue;
            }
            float dx = player.x - pickup.x;
            float dy = player.y - pickup.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < player.magnetRange && dist > 0f) {
                pickup.x += dx / dist * dt * 160f;
                pickup.y += dy / dist * dt * 160f;
            }
            if (dist < pickup.radius + player.radius) {
                if (pickup.type == Pickup.ENERGY) {
                    player.energy = Math.min(player.maxEnergy, player.energy + 25f);
                } else if (pickup.type == Pickup.COIN) {
                    stats.score += 10;
                } else if (pickup.type == Pickup.MEDKIT) {
                    player.hp = Math.min(player.maxHp, player.hp + 25f);
                }
                pickup.active = false;
                if (soundManager != null) {
                    soundManager.playPickup();
                }
            }
        }
    }

    private void updateParticles(float dt) {
        for (Particle particle : particles) {
            if (!particle.active) {
                continue;
            }
            particle.life -= dt;
            if (particle.life <= 0f) {
                particle.active = false;
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
        }
    }

    private void drawArena(Canvas canvas, Paint paint) {
        paint.setColor(0xFF101028);
        canvas.drawRect(bounds, paint);
    }

    private void drawPlayer(Canvas canvas, Paint paint) {
        paint.setColor(0xFF25F5FF);
        canvas.drawCircle(player.x, player.y, player.radius, paint);
        paint.setColor(0xFFB260FF);
        canvas.drawCircle(player.x, player.y, player.radius * 0.45f, paint);
    }

    private void drawEnemies(Canvas canvas, Paint paint) {
        for (Enemy enemy : enemies) {
            paint.setColor(enemy.elite ? 0xFFFF4F7A : 0xFFB260FF);
            canvas.drawCircle(enemy.x, enemy.y, enemy.radius, paint);
        }
    }

    private void drawBullets(Canvas canvas, Paint paint) {
        paint.setColor(0xFF25FFB2);
        for (Bullet bullet : bullets) {
            if (bullet.active) {
                canvas.drawCircle(bullet.x, bullet.y, bullet.radius, paint);
            }
        }
    }

    private void drawPickups(Canvas canvas, Paint paint) {
        for (Pickup pickup : pickups) {
            if (!pickup.active) {
                continue;
            }
            if (pickup.type == Pickup.ENERGY) {
                paint.setColor(0xFF25FFB2);
            } else if (pickup.type == Pickup.COIN) {
                paint.setColor(0xFFFFC857);
            } else {
                paint.setColor(0xFFFF4F7A);
            }
            canvas.drawCircle(pickup.x, pickup.y, pickup.radius, paint);
        }
    }

    private void drawParticles(Canvas canvas, Paint paint) {
        for (Particle particle : particles) {
            if (!particle.active) {
                continue;
            }
            float alpha = particle.life / particle.maxLife;
            int a = (int) (alpha * 255);
            int color = (particle.color & 0x00FFFFFF) | (a << 24);
            paint.setColor(color);
            canvas.drawCircle(particle.x, particle.y, particle.size, paint);
        }
    }

    private void spawnParticles(float x, float y, int count, float speed, float life) {
        for (int i = 0; i < count; i++) {
            Particle particle = nextParticle();
            if (particle != null) {
                float angle = (float) (random.nextFloat() * Math.PI * 2);
                float vx = (float) Math.cos(angle) * speed * 60f;
                float vy = (float) Math.sin(angle) * speed * 60f;
                particle.spawn(x, y, vx, vy, life, 4f, 0xFF25F5FF);
            }
        }
    }

    private Particle nextParticle() {
        for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[particleIndex];
            particleIndex = (particleIndex + 1) % particles.length;
            if (!particle.active) {
                return particle;
            }
        }
        return null;
    }

    private void dropPickup(float x, float y) {
        if (random.nextFloat() > 0.45f) {
            return;
        }
        Pickup pickup = new Pickup();
        int roll = random.nextInt(3);
        pickup.init(x, y, roll);
        pickups.add(pickup);
    }

    private void explode(float x, float y) {
        float radius = 90f;
        float dx = player.x - x;
        float dy = player.y - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < radius) {
            player.hp -= 25f * (1f - player.shield);
            if (player.hp <= 0f) {
                state = GameState.GAME_OVER;
            }
        }
        spawnParticles(x, y, 12, 4f, 0.5f);
    }

    private float[] findAutoAim() {
        float bestDist = Float.MAX_VALUE;
        float dirX = 1f;
        float dirY = 0f;
        for (Enemy enemy : enemies) {
            float dx = enemy.x - player.x;
            float dy = enemy.y - player.y;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                float len = (float) Math.sqrt(dist);
                if (len > 0f) {
                    dirX = dx / len;
                    dirY = dy / len;
                }
            }
        }
        return new float[]{dirX, dirY};
    }

    private void rollUpgrades() {
        UpgradeType[] all = UpgradeType.values();
        for (int i = 0; i < 3; i++) {
            UpgradeType pick = all[random.nextInt(all.length)];
            boolean unique = false;
            while (!unique) {
                unique = true;
                for (int j = 0; j < i; j++) {
                    if (upgradeTypes[j] == pick) {
                        pick = all[random.nextInt(all.length)];
                        unique = false;
                    }
                }
            }
            upgradeTypes[i] = pick;
            upgradeOptions[i] = labelForUpgrade(pick);
        }
    }

    private String labelForUpgrade(UpgradeType type) {
        if (type == UpgradeType.DAMAGE) {
            return "Damage +";
        } else if (type == UpgradeType.FIRE_RATE) {
            return "Fire Rate +";
        } else if (type == UpgradeType.MOVE_SPEED) {
            return "Move Speed +";
        } else if (type == UpgradeType.MAX_HP) {
            return "Max HP +";
        } else if (type == UpgradeType.REGEN) {
            return "Regen +";
        } else if (type == UpgradeType.PIERCE) {
            return "Pierce +";
        } else if (type == UpgradeType.MULTISHOT) {
            return "Multishot +";
        } else if (type == UpgradeType.KNOCKBACK) {
            return "Knockback +";
        } else if (type == UpgradeType.MAGNET) {
            return "Magnet +";
        } else if (type == UpgradeType.SHIELD) {
            return "Shield +";
        } else if (type == UpgradeType.SKILL_COOLDOWN) {
            return "Skill Cooldown -";
        } else {
            return "Skill Damage +";
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
