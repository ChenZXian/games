package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import com.android.boot.entity.AcidPool;
import com.android.boot.entity.Barricade;
import com.android.boot.entity.Bullet;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.EnemyType;
import com.android.boot.entity.Fence;
import com.android.boot.entity.Player;
import com.android.boot.entity.WireNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {
    private final Context context;
    private final Random random;
    private final Paint paint;
    private final Paint textPaint;
    private final Paint uiPaint;
    private final Paint accentPaint;
    private final Paint warningPaint;
    private final Paint energyPaint;
    private final RectF tempRect;
    private final ArrayList<Enemy> enemies;
    private final ArrayList<Bullet> bullets;
    private final ArrayList<Barricade> barricades;
    private final ArrayList<WireNode> nodes;
    private final ArrayList<Fence> fences;
    private final ArrayList<AcidPool> acidPools;
    private final ArrayList<UpgradeOption> upgrades;
    private final String labelWave;
    private final String labelTime;
    private final String labelScore;
    private final String labelScrap;
    private final String labelEnergy;
    private final String labelHp;
    private final String labelBest;
    private final String labelSurvivalTime;
    private final String labelFinalScore;
    private final String labelKills;
    private final String labelUpgrade;
    private final String labelModeBarricade;
    private final String labelModeWire;
    private final String labelPause;
    private GameState state;
    private DeployMode deployMode;
    private Player player;
    private float width;
    private float height;
    private float moveX;
    private float moveY;
    private boolean shooting;
    private float shootTimer;
    private float spawnTimer;
    private float waveTimer;
    private float timeSurvived;
    private int wave;
    private int scrap;
    private int energy;
    private float score;
    private int killRunner;
    private int killBrute;
    private int killSpitter;
    private int killShocker;
    private float fireInterval;
    private float bulletSpeed;
    private float bulletDamage;
    private float critChance;
    private float rollCooldownMax;
    private float rollDuration;
    private float barricadeMaxHp;
    private float fenceDamage;
    private float fenceSlow;
    private float fenceRange;
    private int fenceLimit;
    private float empTimer;
    private float shieldOnHitDuration;
    private boolean mute;
    private float spawnInterval;
    private float killStreakTimer;
    private int killStreakCount;
    private float killStreakMultiplier;
    private int bestScore;
    private SharedPreferences preferences;

    public GameEngine(Context context) {
        this.context = context;
        random = new Random();
        paint = new Paint();
        textPaint = new Paint();
        uiPaint = new Paint();
        accentPaint = new Paint();
        warningPaint = new Paint();
        energyPaint = new Paint();
        tempRect = new RectF();
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        barricades = new ArrayList<>();
        nodes = new ArrayList<>();
        fences = new ArrayList<>();
        acidPools = new ArrayList<>();
        upgrades = new ArrayList<>();
        labelWave = context.getString(com.android.boot.R.string.label_wave);
        labelTime = context.getString(com.android.boot.R.string.label_time);
        labelScore = context.getString(com.android.boot.R.string.label_score);
        labelScrap = context.getString(com.android.boot.R.string.label_scrap);
        labelEnergy = context.getString(com.android.boot.R.string.label_energy);
        labelHp = context.getString(com.android.boot.R.string.label_hp);
        labelBest = context.getString(com.android.boot.R.string.label_best);
        labelSurvivalTime = context.getString(com.android.boot.R.string.label_survival_time);
        labelFinalScore = context.getString(com.android.boot.R.string.label_final_score);
        labelKills = context.getString(com.android.boot.R.string.label_kills);
        labelUpgrade = context.getString(com.android.boot.R.string.label_upgrade);
        labelModeBarricade = context.getString(com.android.boot.R.string.label_mode_barricade);
        labelModeWire = context.getString(com.android.boot.R.string.label_mode_wire);
        labelPause = context.getString(com.android.boot.R.string.label_pause);
        textPaint.setAntiAlias(true);
        uiPaint.setAntiAlias(true);
        accentPaint.setAntiAlias(true);
        warningPaint.setAntiAlias(true);
        energyPaint.setAntiAlias(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        bestScore = preferences.getInt("best_score", 0);
        resetSession();
    }

    public void setViewport(float width, float height) {
        this.width = width;
        this.height = height;
        if (player == null) {
            player = new Player(width * 0.5f, height * 0.5f);
        }
    }

    private void resetSession() {
        state = GameState.MENU;
        deployMode = DeployMode.BARRICADE;
        player = new Player(0f, 0f);
        enemies.clear();
        bullets.clear();
        barricades.clear();
        nodes.clear();
        fences.clear();
        acidPools.clear();
        upgrades.clear();
        moveX = 0f;
        moveY = 0f;
        shooting = false;
        shootTimer = 0f;
        spawnTimer = 0f;
        waveTimer = 0f;
        timeSurvived = 0f;
        wave = 1;
        scrap = 8;
        energy = 0;
        score = 0f;
        killRunner = 0;
        killBrute = 0;
        killSpitter = 0;
        killShocker = 0;
        fireInterval = 0.22f;
        bulletSpeed = 520f;
        bulletDamage = 20f;
        critChance = 0.08f;
        rollCooldownMax = 4f;
        rollDuration = 0.25f;
        barricadeMaxHp = 140f;
        fenceDamage = 22f;
        fenceSlow = 0.45f;
        fenceRange = 18f;
        fenceLimit = 3;
        empTimer = 0f;
        shieldOnHitDuration = 0f;
        mute = false;
        spawnInterval = 1.2f;
        killStreakTimer = 0f;
        killStreakCount = 0;
        killStreakMultiplier = 1f;
    }

    public void startGame() {
        resetSession();
        player.x = width * 0.5f;
        player.y = height * 0.5f;
        state = GameState.PLAYING;
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void openMenu() {
        state = GameState.MENU;
    }

    public void openHowToPlay() {
        state = GameState.HOW_TO_PLAY;
    }

    public void restartGame() {
        startGame();
    }

    public void toggleMute() {
        mute = !mute;
    }

    public boolean isMuted() {
        return mute;
    }

    public GameState getState() {
        return state;
    }

    public DeployMode getDeployMode() {
        return deployMode;
    }

    public List<UpgradeOption> getUpgrades() {
        return upgrades;
    }

    public void setMoveInput(float x, float y) {
        moveX = x;
        moveY = y;
    }

    public void setShooting(boolean shooting) {
        this.shooting = shooting;
    }

    public void toggleDeployMode() {
        if (deployMode == DeployMode.BARRICADE) {
            deployMode = DeployMode.WIRE;
        } else {
            deployMode = DeployMode.BARRICADE;
        }
    }

    public void tryDeploy(float x, float y) {
        if (state != GameState.PLAYING) {
            return;
        }
        if (deployMode == DeployMode.BARRICADE) {
            int cost = 3;
            if (scrap >= cost) {
                scrap -= cost;
                barricades.add(new Barricade(x, y, barricadeMaxHp));
            }
        } else {
            int cost = 2;
            if (scrap >= cost) {
                scrap -= cost;
                WireNode node = new WireNode(x, y);
                nodes.add(node);
                if (nodes.size() >= 2) {
                    WireNode a = nodes.get(nodes.size() - 2);
                    WireNode b = nodes.get(nodes.size() - 1);
                    Fence fence = new Fence(a, b, fenceDamage, fenceSlow);
                    fences.add(fence);
                    if (fences.size() > fenceLimit) {
                        fences.remove(0);
                    }
                }
            }
        }
    }

    public void tryRoll() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (player.rollCooldown <= 0f) {
            player.rollTime = rollDuration;
            player.invulnerableTime = rollDuration;
            player.rollCooldown = rollCooldownMax;
        }
    }

    public void applyUpgrade(int index) {
        if (state != GameState.UPGRADE || index < 0 || index >= upgrades.size()) {
            return;
        }
        UpgradeOption option = upgrades.get(index);
        if (option.type == UpgradeType.FIRE_RATE) {
            fireInterval = Math.max(0.1f, fireInterval - 0.03f);
        } else if (option.type == UpgradeType.BULLET_SPEED) {
            bulletSpeed += 60f;
        } else if (option.type == UpgradeType.CRIT_CHANCE) {
            critChance = Math.min(0.4f, critChance + 0.05f);
        } else if (option.type == UpgradeType.MAX_HP) {
            player.maxHp += 20;
            player.hp += 20;
        } else if (option.type == UpgradeType.SHIELD_ON_HIT) {
            shieldOnHitDuration = Math.min(2f, shieldOnHitDuration + 0.4f);
        } else if (option.type == UpgradeType.ROLL_COOLDOWN) {
            rollCooldownMax = Math.max(1.8f, rollCooldownMax - 0.4f);
        } else if (option.type == UpgradeType.BARRICADE_HP) {
            barricadeMaxHp += 40f;
        } else if (option.type == UpgradeType.FENCE_DAMAGE) {
            fenceDamage += 6f;
        } else if (option.type == UpgradeType.FENCE_SLOW) {
            fenceSlow = Math.min(0.75f, fenceSlow + 0.1f);
        } else if (option.type == UpgradeType.FENCE_RANGE) {
            fenceRange += 4f;
            fenceLimit += 1;
        }
        upgrades.clear();
        waveTimer = 0f;
        state = GameState.PLAYING;
    }

    private void generateUpgrades() {
        upgrades.clear();
        ArrayList<UpgradeOption> pool = new ArrayList<>();
        pool.add(new UpgradeOption("Weapon Fire Rate", UpgradeType.FIRE_RATE));
        pool.add(new UpgradeOption("Weapon Bullet Speed", UpgradeType.BULLET_SPEED));
        pool.add(new UpgradeOption("Weapon Crit Chance", UpgradeType.CRIT_CHANCE));
        pool.add(new UpgradeOption("Defense Max HP", UpgradeType.MAX_HP));
        pool.add(new UpgradeOption("Defense Shield On Hit", UpgradeType.SHIELD_ON_HIT));
        pool.add(new UpgradeOption("Defense Roll Cooldown", UpgradeType.ROLL_COOLDOWN));
        pool.add(new UpgradeOption("Build Barricade Durability", UpgradeType.BARRICADE_HP));
        pool.add(new UpgradeOption("Build Fence Damage", UpgradeType.FENCE_DAMAGE));
        pool.add(new UpgradeOption("Build Fence Slow", UpgradeType.FENCE_SLOW));
        pool.add(new UpgradeOption("Build Fence Range", UpgradeType.FENCE_RANGE));
        for (int i = 0; i < 3; i++) {
            int idx = random.nextInt(pool.size());
            upgrades.add(pool.remove(idx));
        }
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) {
            return;
        }
        timeSurvived += dt;
        waveTimer += dt;
        spawnTimer += dt;
        if (killStreakTimer > 0f) {
            killStreakTimer -= dt;
            if (killStreakTimer <= 0f) {
                killStreakCount = 0;
                killStreakMultiplier = 1f;
            }
        }
        if (player.invulnerableTime > 0f) {
            player.invulnerableTime -= dt;
        }
        if (player.rollCooldown > 0f) {
            player.rollCooldown -= dt;
        }
        if (player.rollTime > 0f) {
            player.rollTime -= dt;
        }
        if (empTimer > 0f) {
            empTimer -= dt;
        }
        if (waveTimer >= 35f) {
            wave += 1;
            waveTimer = 0f;
            generateUpgrades();
            state = GameState.UPGRADE;
            return;
        }
        updatePlayer(dt);
        updateSpawns(dt);
        updateBullets(dt);
        updateFences(dt);
        updateAcidPools(dt);
        updateEnemies(dt);
        if (player.hp <= 0) {
            state = GameState.GAME_OVER;
            int finalScore = Math.round(score);
            if (finalScore > bestScore) {
                bestScore = finalScore;
                preferences.edit().putInt("best_score", bestScore).apply();
            }
        }
    }

    private void updatePlayer(float dt) {
        float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        float dx = moveX;
        float dy = moveY;
        if (len > 1f) {
            dx /= len;
            dy /= len;
        }
        float speed = player.speed;
        if (player.rollTime > 0f) {
            speed *= 2.2f;
        }
        player.x += dx * speed * dt;
        player.y += dy * speed * dt;
        player.x = MathUtil.clamp(player.x, player.radius, width - player.radius);
        player.y = MathUtil.clamp(player.y, player.radius, height - player.radius);
        if (shooting) {
            shootTimer -= dt;
            if (shootTimer <= 0f) {
                shootTimer = fireInterval;
                fireBullet();
            }
        }
    }

    private void fireBullet() {
        Enemy target = findNearestEnemy();
        float dirX = 1f;
        float dirY = 0f;
        if (target != null) {
            float vx = target.x - player.x;
            float vy = target.y - player.y;
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            if (len > 0f) {
                dirX = vx / len;
                dirY = vy / len;
            }
        } else {
            float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            if (len > 0.1f) {
                dirX = moveX / len;
                dirY = moveY / len;
            }
        }
        float angle = (random.nextFloat() - 0.5f) * 0.3f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float nx = dirX * cos - dirY * sin;
        float ny = dirX * sin + dirY * cos;
        bullets.add(new Bullet(player.x, player.y, nx * bulletSpeed, ny * bulletSpeed, bulletDamage));
    }

    private Enemy findNearestEnemy() {
        Enemy nearest = null;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            float d = MathUtil.dist(player.x, player.y, enemy.x, enemy.y);
            if (d < best) {
                best = d;
                nearest = enemy;
            }
        }
        return nearest;
    }

    private void updateSpawns(float dt) {
        float intensity = 1f + wave * 0.12f + timeSurvived * 0.004f;
        spawnInterval = Math.max(0.3f, 1.2f / intensity);
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnEnemy();
        }
    }

    private void spawnEnemy() {
        float edge = random.nextFloat();
        float x;
        float y;
        if (edge < 0.25f) {
            x = -20f;
            y = random.nextFloat() * height;
        } else if (edge < 0.5f) {
            x = width + 20f;
            y = random.nextFloat() * height;
        } else if (edge < 0.75f) {
            x = random.nextFloat() * width;
            y = -20f;
        } else {
            x = random.nextFloat() * width;
            y = height + 20f;
        }
        EnemyType type;
        float roll = random.nextFloat();
        float mix = Math.min(0.6f, 0.15f + timeSurvived * 0.01f);
        if (roll < 0.45f) {
            type = EnemyType.RUNNER;
        } else if (roll < 0.65f) {
            type = EnemyType.BRUTE;
        } else if (roll < 0.85f) {
            type = EnemyType.SPITTER;
        } else {
            type = EnemyType.SHOCKER;
        }
        if (random.nextFloat() < mix * 0.4f) {
            type = EnemyType.BRUTE;
        }
        enemies.add(new Enemy(x, y, type));
    }

    private void updateBullets(float dt) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.life -= dt;
            bullet.x += bullet.vx * dt;
            bullet.y += bullet.vy * dt;
            if (bullet.life <= 0f || bullet.x < -40f || bullet.x > width + 40f || bullet.y < -40f || bullet.y > height + 40f) {
                bullets.remove(i);
            }
        }
    }

    private void updateFences(float dt) {
        if (empTimer > 0f) {
            return;
        }
        for (int i = 0; i < fences.size(); i++) {
            Fence fence = fences.get(i);
            float x1 = fence.a.x;
            float y1 = fence.a.y;
            float x2 = fence.b.x;
            float y2 = fence.b.y;
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                float dist = MathUtil.distanceToSegment(enemy.x, enemy.y, x1, y1, x2, y2);
                if (dist <= fenceRange) {
                    enemy.hp -= fence.damagePerSecond * dt;
                    enemy.slowTimer = Math.max(enemy.slowTimer, 0.4f);
                }
            }
        }
    }

    private void updateAcidPools(float dt) {
        for (int i = acidPools.size() - 1; i >= 0; i--) {
            AcidPool pool = acidPools.get(i);
            pool.life -= dt;
            if (pool.life <= 0f) {
                acidPools.remove(i);
            } else {
                float d = MathUtil.dist(player.x, player.y, pool.x, pool.y);
                if (d < pool.radius && player.invulnerableTime <= 0f) {
                    damagePlayer(pool.damagePerSecond * dt);
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
            float dirX = 0f;
            float dirY = 0f;
            if (len > 0f) {
                dirX = dx / len;
                dirY = dy / len;
            }
            float speed = enemy.speed;
            if (enemy.slowTimer > 0f) {
                enemy.slowTimer -= dt;
                speed *= 1f - fenceSlow;
            }
            for (int b = 0; b < barricades.size(); b++) {
                Barricade barricade = barricades.get(b);
                float bd = MathUtil.dist(enemy.x, enemy.y, barricade.x, barricade.y);
                if (bd < barricade.radius + enemy.radius + 6f) {
                    float tx = -dirY;
                    float ty = dirX;
                    dirX = tx;
                    dirY = ty;
                    if (enemy.type == EnemyType.BRUTE) {
                        barricade.hp -= 20f * dt;
                    } else {
                        barricade.hp -= 8f * dt;
                    }
                }
            }
            enemy.x += dirX * speed * dt;
            enemy.y += dirY * speed * dt;
            if (enemy.type == EnemyType.SPITTER) {
                enemy.attackTimer -= dt;
                if (enemy.attackTimer <= 0f) {
                    enemy.attackTimer = 2.6f;
                    float ax = player.x + (random.nextFloat() - 0.5f) * 60f;
                    float ay = player.y + (random.nextFloat() - 0.5f) * 60f;
                    acidPools.add(new AcidPool(MathUtil.clamp(ax, 20f, width - 20f), MathUtil.clamp(ay, 20f, height - 20f)));
                }
            }
            if (MathUtil.dist(player.x, player.y, enemy.x, enemy.y) < player.radius + enemy.radius) {
                if (player.invulnerableTime <= 0f) {
                    float damage = enemy.type == EnemyType.BRUTE ? 18f : 10f;
                    damagePlayer(damage);
                }
            }
            if (enemy.hp <= 0f) {
                onEnemyKilled(enemy);
                enemies.remove(i);
            }
        }
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            boolean hit = false;
            for (int e = enemies.size() - 1; e >= 0; e--) {
                Enemy enemy = enemies.get(e);
                if (MathUtil.dist(bullet.x, bullet.y, enemy.x, enemy.y) <= enemy.radius + bullet.radius) {
                    float damage = bullet.damage;
                    if (random.nextFloat() < critChance) {
                        damage *= 1.8f;
                    }
                    enemy.hp -= damage;
                    hit = true;
                    break;
                }
            }
            if (hit) {
                bullets.remove(i);
            }
        }
        for (int b = barricades.size() - 1; b >= 0; b--) {
            Barricade barricade = barricades.get(b);
            if (barricade.hp <= 0f) {
                barricades.remove(b);
            }
        }
    }

    private void damagePlayer(float amount) {
        player.hp -= Math.round(amount);
        if (shieldOnHitDuration > 0f) {
            player.invulnerableTime = Math.max(player.invulnerableTime, shieldOnHitDuration);
        }
    }

    private void onEnemyKilled(Enemy enemy) {
        float baseScore;
        if (enemy.type == EnemyType.RUNNER) {
            killRunner++;
            baseScore = 30f;
        } else if (enemy.type == EnemyType.BRUTE) {
            killBrute++;
            baseScore = 60f;
        } else if (enemy.type == EnemyType.SPITTER) {
            killSpitter++;
            baseScore = 55f;
        } else {
            killShocker++;
            baseScore = 70f;
            empTimer = 2.5f;
        }
        scrap += 1 + random.nextInt(2);
        if (random.nextFloat() < 0.25f) {
            energy += 1;
        }
        killStreakCount++;
        killStreakTimer = 3f;
        killStreakMultiplier = 1f + Math.min(2f, killStreakCount * 0.1f);
        score += baseScore * killStreakMultiplier;
    }

    public void draw(Canvas canvas, float uiScale) {
        canvas.drawColor(context.getColor(com.android.boot.R.color.cst_bg_main));
        drawArena(canvas);
        drawFences(canvas);
        drawAcid(canvas);
        drawBarricades(canvas);
        drawBullets(canvas);
        drawEnemies(canvas);
        drawPlayer(canvas);
        drawHud(canvas, uiScale);
        if (state == GameState.MENU) {
            drawMenu(canvas, uiScale);
        } else if (state == GameState.PAUSED) {
            drawPause(canvas, uiScale);
        } else if (state == GameState.GAME_OVER) {
            drawGameOver(canvas, uiScale);
        } else if (state == GameState.UPGRADE) {
            drawUpgrade(canvas, uiScale);
        } else if (state == GameState.HOW_TO_PLAY) {
            drawHowToPlay(canvas, uiScale);
        }
    }

    private void drawArena(Canvas canvas) {
        paint.setColor(context.getColor(com.android.boot.R.color.cst_ui_panel));
        float inset = 12f;
        canvas.drawRect(inset, inset, width - inset, height - inset, paint);
    }

    private void drawPlayer(Canvas canvas) {
        paint.setColor(context.getColor(com.android.boot.R.color.cst_accent));
        canvas.drawCircle(player.x, player.y, player.radius, paint);
        if (player.invulnerableTime > 0f) {
            warningPaint.setStyle(Paint.Style.STROKE);
            warningPaint.setStrokeWidth(3f);
            warningPaint.setColor(context.getColor(com.android.boot.R.color.cst_warning));
            canvas.drawCircle(player.x, player.y, player.radius + 6f, warningPaint);
            warningPaint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.type == EnemyType.RUNNER) {
                paint.setColor(context.getColor(com.android.boot.R.color.cst_warning));
            } else if (enemy.type == EnemyType.BRUTE) {
                paint.setColor(context.getColor(com.android.boot.R.color.cst_energy));
            } else if (enemy.type == EnemyType.SPITTER) {
                paint.setColor(context.getColor(com.android.boot.R.color.cst_accent));
            } else {
                paint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
            }
            canvas.drawCircle(enemy.x, enemy.y, enemy.radius, paint);
        }
    }

    private void drawBullets(Canvas canvas) {
        accentPaint.setColor(context.getColor(com.android.boot.R.color.cst_accent));
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            canvas.drawCircle(bullet.x, bullet.y, bullet.radius, accentPaint);
        }
    }

    private void drawBarricades(Canvas canvas) {
        paint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        for (int i = 0; i < barricades.size(); i++) {
            Barricade barricade = barricades.get(i);
            canvas.drawRect(barricade.x - barricade.radius, barricade.y - barricade.radius, barricade.x + barricade.radius, barricade.y + barricade.radius, paint);
            warningPaint.setStyle(Paint.Style.STROKE);
            warningPaint.setStrokeWidth(2f);
            warningPaint.setColor(context.getColor(com.android.boot.R.color.cst_warning));
            float ratio = MathUtil.clamp(barricade.hp / barricade.maxHp, 0f, 1f);
            canvas.drawLine(barricade.x - barricade.radius, barricade.y - barricade.radius - 6f, barricade.x - barricade.radius + ratio * 2f * barricade.radius, barricade.y - barricade.radius - 6f, warningPaint);
            warningPaint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawFences(Canvas canvas) {
        if (empTimer > 0f) {
            warningPaint.setColor(context.getColor(com.android.boot.R.color.cst_warning));
        } else {
            energyPaint.setColor(context.getColor(com.android.boot.R.color.cst_energy));
        }
        for (int i = 0; i < fences.size(); i++) {
            Fence fence = fences.get(i);
            if (empTimer > 0f) {
                canvas.drawLine(fence.a.x, fence.a.y, fence.b.x, fence.b.y, warningPaint);
            } else {
                canvas.drawLine(fence.a.x, fence.a.y, fence.b.x, fence.b.y, energyPaint);
            }
        }
        paint.setColor(context.getColor(com.android.boot.R.color.cst_energy));
        for (int i = 0; i < nodes.size(); i++) {
            WireNode node = nodes.get(i);
            canvas.drawCircle(node.x, node.y, node.radius, paint);
        }
    }

    private void drawAcid(Canvas canvas) {
        warningPaint.setColor(context.getColor(com.android.boot.R.color.cst_warning));
        for (int i = 0; i < acidPools.size(); i++) {
            AcidPool pool = acidPools.get(i);
            canvas.drawCircle(pool.x, pool.y, pool.radius, warningPaint);
        }
    }

    private void drawHud(Canvas canvas, float uiScale) {
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        textPaint.setTextSize(18f * uiScale);
        String hud = labelHp + " " + player.hp + "  " + labelScrap + " " + scrap + "  " + labelEnergy + " " + energy;
        canvas.drawText(hud, 16f, 28f * uiScale, textPaint);
        String line2 = labelWave + " " + wave + "  " + labelTime + " " + Math.round(timeSurvived) + "  " + labelScore + " " + Math.round(score);
        canvas.drawText(line2, 16f, 52f * uiScale, textPaint);
        String mode = deployMode == DeployMode.BARRICADE ? labelModeBarricade : labelModeWire;
        canvas.drawText(mode, 16f, height - 18f * uiScale, textPaint);
    }

    private void drawMenu(Canvas canvas, float uiScale) {
        drawCenterPanel(canvas, uiScale);
        textPaint.setTextSize(34f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        canvas.drawText(context.getString(com.android.boot.R.string.app_name), width * 0.5f - 180f * uiScale, height * 0.32f, textPaint);
    }

    private void drawPause(Canvas canvas, float uiScale) {
        drawCenterPanel(canvas, uiScale);
        textPaint.setTextSize(28f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        canvas.drawText(labelPause, width * 0.5f - 60f * uiScale, height * 0.35f, textPaint);
    }

    private void drawGameOver(Canvas canvas, float uiScale) {
        drawCenterPanel(canvas, uiScale);
        textPaint.setTextSize(26f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        float y = height * 0.34f;
        canvas.drawText(labelSurvivalTime + " " + Math.round(timeSurvived), width * 0.5f - 140f * uiScale, y, textPaint);
        y += 36f * uiScale;
        canvas.drawText(labelFinalScore + " " + Math.round(score), width * 0.5f - 140f * uiScale, y, textPaint);
        y += 36f * uiScale;
        canvas.drawText(labelBest + " " + bestScore, width * 0.5f - 140f * uiScale, y, textPaint);
        y += 36f * uiScale;
        String kills = labelKills + " R" + killRunner + " B" + killBrute + " S" + killSpitter + " E" + killShocker;
        canvas.drawText(kills, width * 0.5f - 140f * uiScale, y, textPaint);
    }

    private void drawUpgrade(Canvas canvas, float uiScale) {
        drawCenterPanel(canvas, uiScale);
        textPaint.setTextSize(26f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        canvas.drawText(labelUpgrade, width * 0.5f - 120f * uiScale, height * 0.28f, textPaint);
        textPaint.setTextSize(20f * uiScale);
        for (int i = 0; i < upgrades.size(); i++) {
            UpgradeOption option = upgrades.get(i);
            float y = height * 0.38f + i * 48f * uiScale;
            canvas.drawText(option.name, width * 0.5f - 160f * uiScale, y, textPaint);
        }
    }

    private void drawHowToPlay(Canvas canvas, float uiScale) {
        drawCenterPanel(canvas, uiScale);
        textPaint.setTextSize(20f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        float x = width * 0.2f;
        float y = height * 0.32f;
        String text = context.getString(com.android.boot.R.string.how_to_play_text);
        String[] parts = text.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i].trim();
            if (!line.isEmpty()) {
                canvas.drawText(line + ".", x, y, textPaint);
                y += 32f * uiScale;
            }
        }
    }

    private void drawCenterPanel(Canvas canvas, float uiScale) {
        uiPaint.setColor(context.getColor(com.android.boot.R.color.cst_ui_panel));
        float panelWidth = width * 0.6f;
        float panelHeight = height * 0.6f;
        float left = width * 0.2f;
        float top = height * 0.2f;
        tempRect.set(left, top, left + panelWidth, top + panelHeight);
        canvas.drawRect(tempRect, uiPaint);
    }

    public void drawButton(Canvas canvas, RectF rect, String label, float uiScale) {
        uiPaint.setColor(context.getColor(com.android.boot.R.color.cst_ui_panel));
        canvas.drawRoundRect(rect, 18f * uiScale, 18f * uiScale, uiPaint);
        textPaint.setTextSize(18f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        float textWidth = textPaint.measureText(label);
        canvas.drawText(label, rect.centerX() - textWidth / 2f, rect.centerY() + 6f * uiScale, textPaint);
    }

    public void drawSmallButton(Canvas canvas, RectF rect, String label, float uiScale) {
        uiPaint.setColor(context.getColor(com.android.boot.R.color.cst_ui_panel));
        canvas.drawRoundRect(rect, 12f * uiScale, 12f * uiScale, uiPaint);
        textPaint.setTextSize(16f * uiScale);
        textPaint.setColor(context.getColor(com.android.boot.R.color.cst_text_primary));
        float textWidth = textPaint.measureText(label);
        canvas.drawText(label, rect.centerX() - textWidth / 2f, rect.centerY() + 5f * uiScale, textPaint);
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getScrap() {
        return scrap;
    }

    public int getEnergy() {
        return energy;
    }

    public float getRollCooldown() {
        return player.rollCooldown;
    }

    public float getRollCooldownMax() {
        return rollCooldownMax;
    }

    public float getTimeSurvived() {
        return timeSurvived;
    }

    public float getScore() {
        return score;
    }
}
