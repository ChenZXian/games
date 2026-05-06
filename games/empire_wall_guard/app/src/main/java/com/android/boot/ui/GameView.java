package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.boot.R;
import com.android.boot.core.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    public static final int BUILD_ARCHER = 0;
    public static final int BUILD_BARRACKS = 1;
    public static final int BUILD_STONE = 2;
    public static final int BUILD_OIL = 3;
    public static final int BUILD_BEACON = 4;

    private static final int ENEMY_RAIDER = 0;
    private static final int ENEMY_SCOUT = 1;
    private static final int ENEMY_BRUTE = 2;
    private static final int ENEMY_SIEGE = 3;
    private static final int ENEMY_WARLORD = 4;

    private static final int STATE_IDLE = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_ATTACK = 2;
    private static final int STATE_HIT = 3;
    private static final int STATE_DYING = 4;

    public interface GameListener {
        void onHudChanged(int gate, int supplies, int wave, int maxWave, int breaches, String status);
        void onRunEnded(boolean cleared, int wave, int breaches, int stars, int gate);
        void onAudioEvent(String key);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF rect = new RectF();
    private final RectF dstRect = new RectF();
    private final Random random = new Random(31);
    private final ArrayList<Defense> defenses = new ArrayList<>();
    private final ArrayList<Soldier> soldiers = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final HashMap<String, Bitmap> sprites = new HashMap<>();
    private final Slot[] slots = new Slot[]{
            new Slot(0.32f, 0.22f, 0),
            new Slot(0.46f, 0.22f, 0),
            new Slot(0.30f, 0.50f, 1),
            new Slot(0.48f, 0.50f, 1),
            new Slot(0.32f, 0.78f, 2),
            new Slot(0.46f, 0.78f, 2)
    };
    private final float[] laneY = new float[]{0.30f, 0.50f, 0.70f};
    private GameListener listener;
    private GameState state = GameState.MENU;
    private Bitmap unitSheet;
    private boolean running;
    private boolean soundEnabled = true;
    private boolean spritesLoaded;
    private long lastTick;
    private int gate = 100;
    private int supplies = 155;
    private int wave;
    private int maxWave = 9;
    private int breaches;
    private int buildMode = BUILD_ARCHER;
    private int spawnRemaining;
    private int selectedRallyLane = 1;
    private float spawnTimer;
    private float hudTimer;
    private boolean waveActive;
    private String statusText = "Plan Defense";

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.nanoTime();
            float dt = (now - lastTick) / 1000000000f;
            lastTick = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            if (state == GameState.PLAYING) {
                update(dt);
            }
            invalidate();
            handler.postDelayed(this, 16);
        }
    };

    public GameView(Context context) {
        super(context);
        setup();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        setFocusable(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        loadSprites();
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
        notifyHud();
    }

    public void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastTick = System.nanoTime();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    public void stopLoop() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    public void pauseFromLifecycle() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
        stopLoop();
    }

    public void resetForMenu() {
        state = GameState.MENU;
        defenses.clear();
        soldiers.clear();
        enemies.clear();
        projectiles.clear();
        effects.clear();
        waveActive = false;
        spawnRemaining = 0;
        buildMode = BUILD_ARCHER;
        statusText = "Plan Defense";
        notifyHud();
        invalidate();
    }

    public void startGame() {
        gate = 100;
        supplies = 155;
        wave = 0;
        breaches = 0;
        spawnRemaining = 0;
        spawnTimer = 0f;
        selectedRallyLane = 1;
        buildMode = BUILD_ARCHER;
        waveActive = false;
        statusText = "Plan Defense";
        defenses.clear();
        soldiers.clear();
        enemies.clear();
        projectiles.clear();
        effects.clear();
        state = GameState.PLAYING;
        startLoop();
        notifyHud();
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            notifyHud();
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastTick = System.nanoTime();
            startLoop();
            notifyHud();
        }
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void setBuildMode(int mode) {
        buildMode = mode;
        statusText = "Build " + defenseName(mode);
        notifyHud();
    }

    public void startNextWave() {
        if (state != GameState.PLAYING || waveActive || wave >= maxWave) {
            return;
        }
        wave++;
        spawnRemaining = 7 + wave * 3;
        spawnTimer = 0f;
        waveActive = true;
        supplies += 12 + wave * 2;
        statusText = "Wave Incoming";
        audioEvent("wave_start");
        if (wave == maxWave || wave % 3 == 0) {
            audioEvent("siege_warning");
        }
        notifyHud();
    }

    public void useFireOil() {
        if (state != GameState.PLAYING || supplies < 40) {
            statusText = "Need Supplies 40";
            notifyHud();
            return;
        }
        boolean hit = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.x < 0.44f && enemy.state != STATE_DYING) {
                damageEnemy(enemy, 42f + wave * 3f);
                enemy.burn = 2.4f;
                hit = true;
            }
        }
        if (!hit) {
            statusText = "No Oil Target";
            notifyHud();
            return;
        }
        supplies -= 40;
        effects.add(new Effect(0.23f, 0.50f, 0.55f, 0.45f, color(R.color.cst_game_fire), 2));
        statusText = "Fire Oil";
        audioEvent("oil_fire");
        notifyHud();
    }

    public void useRepair() {
        if (state != GameState.PLAYING || supplies < 45 || gate >= 100) {
            statusText = "Repair Blocked";
            notifyHud();
            return;
        }
        supplies -= 45;
        gate = Math.min(100, gate + 28);
        effects.add(new Effect(0.16f, 0.50f, 0.45f, 0.32f, color(R.color.cst_success), 1));
        statusText = "Gate Repaired";
        audioEvent("repair");
        notifyHud();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (state != GameState.PLAYING) {
            return true;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return true;
        }
        float nx = event.getX() / w;
        float ny = event.getY() / h;
        int lane = nearestLane(ny);
        if (nx < 0.24f && Math.abs(ny - laneY[lane]) < 0.10f) {
            rallySoldiers(lane);
            return true;
        }
        Slot slot = findSlot(nx, ny);
        if (slot != null) {
            Defense existing = findDefense(slot);
            if (existing == null) {
                buildDefense(slot);
            } else {
                upgradeDefense(existing);
            }
            return true;
        }
        statusText = "Tap Slot Or Rally";
        notifyHud();
        return true;
    }

    private void update(float dt) {
        hudTimer -= dt;
        updateWave(dt);
        updateDefenses(dt);
        updateSoldiers(dt);
        updateEnemies(dt);
        updateProjectiles(dt);
        updateEffects(dt);
        if (waveActive && spawnRemaining <= 0 && enemies.isEmpty()) {
            waveActive = false;
            supplies += 25 + wave * 5;
            if (wave >= maxWave) {
                finishRun(true);
            } else {
                statusText = "Plan Next Wave";
                notifyHud();
            }
        }
        if (hudTimer <= 0f) {
            notifyHud();
            hudTimer = 0.18f;
        }
    }

    private void updateWave(float dt) {
        if (!waveActive || spawnRemaining <= 0) {
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            spawnEnemy();
            spawnRemaining--;
            spawnTimer = Math.max(0.42f, 1.05f - wave * 0.055f);
        }
    }

    private void spawnEnemy() {
        int lane = random.nextInt(laneY.length);
        int type = ENEMY_RAIDER;
        if (wave == maxWave && spawnRemaining == 1) {
            type = ENEMY_WARLORD;
        } else if (wave >= 3 && spawnRemaining % 7 == 0) {
            type = ENEMY_SIEGE;
        } else if (wave >= 5 && spawnRemaining % 5 == 0) {
            type = ENEMY_BRUTE;
        } else if (wave >= 2 && spawnRemaining % 3 == 0) {
            type = ENEMY_SCOUT;
        }
        Enemy enemy = new Enemy();
        enemy.type = type;
        enemy.lane = lane;
        enemy.x = 0.98f + random.nextFloat() * 0.04f;
        enemy.y = laneY[lane] + (random.nextFloat() - 0.5f) * 0.025f;
        enemy.targetY = laneY[lane];
        enemy.facing = -1f;
        enemy.radius = 0.034f;
        enemy.maxHp = 36f + wave * 10f;
        enemy.speed = 0.050f + wave * 0.003f;
        enemy.damage = 6 + wave;
        enemy.reward = 8 + wave;
        enemy.attackTime = 0.95f;
        enemy.state = STATE_MOVE;
        if (type == ENEMY_SCOUT) {
            enemy.maxHp *= 0.72f;
            enemy.speed *= 1.62f;
            enemy.damage += 1;
            enemy.reward += 3;
            enemy.radius = 0.028f;
        } else if (type == ENEMY_BRUTE) {
            enemy.maxHp *= 1.85f;
            enemy.speed *= 0.72f;
            enemy.damage += 5;
            enemy.reward += 9;
            enemy.radius = 0.042f;
        } else if (type == ENEMY_SIEGE) {
            enemy.maxHp *= 2.35f;
            enemy.speed *= 0.58f;
            enemy.damage += 7;
            enemy.reward += 16;
            enemy.radius = 0.050f;
            enemy.siege = true;
            enemy.attackTime = 1.65f;
        } else if (type == ENEMY_WARLORD) {
            enemy.maxHp *= 5.6f;
            enemy.speed *= 0.55f;
            enemy.damage += 14;
            enemy.reward += 60;
            enemy.radius = 0.062f;
            enemy.siege = true;
            enemy.attackTime = 1.25f;
        }
        enemy.hp = enemy.maxHp;
        enemies.add(enemy);
    }

    private void updateDefenses(float dt) {
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            defense.animTime += dt;
            defense.cooldown -= dt * beaconMultiplier(defense);
            if (defense.flash > 0f) {
                defense.flash -= dt;
            }
            if (defense.type == BUILD_BARRACKS) {
                reinforceSoldier(defense, dt);
                continue;
            }
            if (defense.type == BUILD_BEACON) {
                if (defense.cooldown <= 0f) {
                    defense.cooldown = 1.2f;
                    effects.add(new Effect(defense.slot.x, defense.slot.y, 0.25f, 0.14f + defense.level * 0.03f, color(R.color.cst_accent), 1));
                }
                continue;
            }
            Enemy target = findTarget(defense);
            if (target != null && defense.cooldown <= 0f) {
                fireDefense(defense, target);
                defense.cooldown = defenseCooldown(defense);
                defense.flash = 0.16f;
            }
        }
    }

    private void updateSoldiers(float dt) {
        for (int i = soldiers.size() - 1; i >= 0; i--) {
            Soldier soldier = soldiers.get(i);
            soldier.animTime += dt;
            if (soldier.hitFlash > 0f) {
                soldier.hitFlash -= dt;
            }
            if (soldier.hp <= 0f) {
                effects.add(new Effect(soldier.x, soldier.y, 0.35f, 0.11f, color(R.color.cst_danger), 1));
                soldiers.remove(i);
                continue;
            }
            float desiredY = laneY[soldier.targetLane];
            float dy = desiredY - soldier.y;
            if (Math.abs(dy) > 0.004f) {
                soldier.y += Math.signum(dy) * Math.min(Math.abs(dy), dt * 0.34f);
                soldier.state = STATE_MOVE;
            } else {
                soldier.y = desiredY;
                soldier.lane = soldier.targetLane;
                soldier.state = STATE_IDLE;
            }
            Enemy target = findSoldierTarget(soldier);
            if (target != null) {
                soldier.facing = target.x >= soldier.x ? 1f : -1f;
                soldier.state = STATE_ATTACK;
                soldier.attackCooldown -= dt;
                if (soldier.attackCooldown <= 0f) {
                    damageEnemy(target, 5f + soldier.level * 3f);
                    soldier.attackCooldown = 0.55f;
                    soldier.hitFlash = 0.12f;
                    audioEvent("shield_block");
                }
            } else {
                soldier.facing = 1f;
                soldier.attackCooldown = Math.max(0f, soldier.attackCooldown - dt);
            }
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.animTime += dt;
            enemy.stateTime += dt;
            if (enemy.hitFlash > 0f) {
                enemy.hitFlash -= dt;
            }
            if (enemy.burn > 0f) {
                enemy.burn -= dt;
                damageEnemy(enemy, dt * (10f + wave));
            }
            if (enemy.hp <= 0f && enemy.state != STATE_DYING) {
                enemy.state = STATE_DYING;
                enemy.stateTime = 0f;
                effects.add(new Effect(enemy.x, enemy.y, 0.45f, enemy.radius * 3.2f, color(R.color.cst_success), 1));
                audioEvent("enemy_death");
            }
            if (enemy.state == STATE_DYING) {
                if (enemy.stateTime > 0.28f) {
                    supplies += enemy.reward;
                    effects.add(new Effect(enemy.x, enemy.y, 0.28f, enemy.radius * 2.0f, color(R.color.cst_warning), 1));
                    enemies.remove(i);
                    audioEvent("supply_pickup");
                }
                continue;
            }
            Soldier blocker = findBlocker(enemy);
            if (blocker != null) {
                enemy.state = STATE_ATTACK;
                enemy.facing = blocker.x >= enemy.x ? 1f : -1f;
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    blocker.hp -= enemy.damage * 0.55f;
                    blocker.hitFlash = 0.18f;
                    enemy.attackCooldown = enemy.attackTime;
                    effects.add(new Effect(blocker.x, blocker.y, 0.22f, 0.08f, color(R.color.cst_warning), 1));
                    audioEvent("shield_block");
                }
                continue;
            }
            if (enemy.siege && enemy.x <= 0.63f) {
                enemy.state = STATE_ATTACK;
                enemy.facing = -1f;
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    fireSiege(enemy);
                    enemy.attackCooldown = enemy.attackTime;
                }
                continue;
            }
            if (enemy.x <= 0.18f) {
                enemy.state = STATE_ATTACK;
                enemy.facing = -1f;
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    damageGate(enemy.damage);
                    enemy.attackCooldown = enemy.attackTime;
                    effects.add(new Effect(0.16f, enemy.targetY, 0.28f, 0.12f, color(R.color.cst_danger), 1));
                }
                continue;
            }
            enemy.state = STATE_MOVE;
            enemy.facing = -1f;
            enemy.x -= enemy.speed * dt;
            float dy = enemy.targetY - enemy.y;
            enemy.y += dy * Math.min(1f, dt * 4f);
        }
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.life -= dt;
            if (projectile.target != null && projectile.target.state != STATE_DYING) {
                projectile.tx = projectile.target.x;
                projectile.ty = projectile.target.y;
            }
            float dx = projectile.tx - projectile.x;
            float dy = projectile.ty - projectile.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.001f) {
                projectile.angle = (float) Math.atan2(dy, dx);
                float step = projectile.speed * dt;
                projectile.x += dx / dist * step;
                projectile.y += dy / dist * step;
            }
            if (dist < 0.018f || projectile.life <= 0f) {
                if (projectile.gateHit) {
                    damageGate((int) projectile.damage);
                    effects.add(new Effect(0.16f, projectile.ty, 0.30f, 0.13f, color(R.color.cst_danger), 1));
                } else {
                    applyProjectileDamage(projectile);
                }
                projectiles.remove(i);
            }
        }
    }

    private void updateEffects(float dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect effect = effects.get(i);
            effect.life -= dt;
            if (effect.life <= 0f) {
                effects.remove(i);
            }
        }
    }

    private void reinforceSoldier(Defense defense, float dt) {
        Soldier soldier = findSoldierForDefense(defense);
        if (soldier == null) {
            defense.cooldown -= dt;
            if (defense.cooldown <= 0f) {
                Soldier created = new Soldier();
                created.owner = defense;
                created.lane = defense.slot.lane;
                created.targetLane = defense.slot.lane;
                created.x = 0.22f;
                created.y = laneY[created.lane];
                created.facing = 1f;
                created.level = defense.level;
                created.maxHp = 42f + defense.level * 24f;
                created.hp = created.maxHp;
                created.state = STATE_IDLE;
                soldiers.add(created);
                defense.cooldown = 4f;
                statusText = "Shield Ready";
                audioEvent("build_tower");
            }
            return;
        }
        soldier.level = defense.level;
        soldier.maxHp = 42f + defense.level * 24f;
        if (soldier.hp < soldier.maxHp) {
            soldier.hp = Math.min(soldier.maxHp, soldier.hp + dt * (5f + defense.level * 2f));
        }
    }

    private void fireDefense(Defense defense, Enemy target) {
        Projectile projectile = new Projectile();
        projectile.x = defense.slot.x;
        projectile.y = defense.slot.y;
        projectile.tx = target.x;
        projectile.ty = target.y;
        projectile.target = target;
        projectile.damage = defenseDamage(defense);
        projectile.speed = defense.type == BUILD_STONE ? 0.70f : 0.92f;
        projectile.splash = defense.type == BUILD_STONE ? 0.085f + defense.level * 0.010f : 0.0f;
        projectile.type = defense.type;
        projectile.life = 1.6f;
        projectiles.add(projectile);
        defense.lastAngle = (float) Math.atan2(target.y - defense.slot.y, target.x - defense.slot.x);
        if (defense.type == BUILD_ARCHER) {
            audioEvent("archer_shot");
        } else if (defense.type == BUILD_STONE) {
            audioEvent("stone_throw");
        } else {
            audioEvent("oil_fire");
        }
    }

    private void fireSiege(Enemy enemy) {
        Projectile projectile = new Projectile();
        projectile.x = enemy.x - enemy.radius * 0.5f;
        projectile.y = enemy.y;
        projectile.tx = 0.16f;
        projectile.ty = laneY[enemy.lane];
        projectile.damage = enemy.type == ENEMY_WARLORD ? 16f : 10f;
        projectile.speed = 0.46f;
        projectile.splash = 0f;
        projectile.type = BUILD_STONE;
        projectile.gateHit = true;
        projectile.life = 2.2f;
        projectiles.add(projectile);
        effects.add(new Effect(enemy.x, enemy.y, 0.22f, 0.10f, color(R.color.cst_warning), 1));
        audioEvent("stone_throw");
    }

    private void applyProjectileDamage(Projectile projectile) {
        if (projectile.splash > 0f) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.state != STATE_DYING && distance(projectile.tx, projectile.ty, enemy.x, enemy.y) < projectile.splash) {
                    damageEnemy(enemy, projectile.damage * 0.72f);
                }
            }
            effects.add(new Effect(projectile.tx, projectile.ty, 0.34f, projectile.splash, color(R.color.cst_warning), 1));
        } else if (projectile.target != null) {
            damageEnemy(projectile.target, projectile.damage);
            effects.add(new Effect(projectile.tx, projectile.ty, 0.22f, 0.055f, color(R.color.cst_accent), 1));
        }
        audioEvent("enemy_hit");
    }

    private void damageEnemy(Enemy enemy, float amount) {
        if (enemy.state == STATE_DYING) {
            return;
        }
        enemy.hp -= amount;
        enemy.hitFlash = 0.15f;
        enemy.stateTime = 0f;
        if (enemy.state != STATE_ATTACK) {
            enemy.state = STATE_HIT;
        }
    }

    private void damageGate(int amount) {
        gate -= amount;
        breaches++;
        audioEvent("gate_damage");
        if (gate <= 0) {
            gate = 0;
            finishRun(false);
        }
        notifyHud();
    }

    private Enemy findTarget(Defense defense) {
        Enemy best = null;
        float bestDist = 10f;
        float range = defenseRange(defense);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING) {
                continue;
            }
            float d = distance(defense.slot.x, defense.slot.y, enemy.x, enemy.y);
            if (d < range && d < bestDist) {
                best = enemy;
                bestDist = d;
            }
        }
        return best;
    }

    private Enemy findSoldierTarget(Soldier soldier) {
        Enemy best = null;
        float bestDist = 10f;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING || enemy.lane != soldier.lane) {
                continue;
            }
            float d = Math.abs(enemy.x - soldier.x);
            if (d < 0.12f && d < bestDist) {
                best = enemy;
                bestDist = d;
            }
        }
        return best;
    }

    private Soldier findBlocker(Enemy enemy) {
        Soldier best = null;
        float bestDist = 10f;
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            if (soldier.lane != enemy.lane || soldier.hp <= 0f) {
                continue;
            }
            float d = Math.abs(enemy.x - soldier.x);
            if (d < 0.060f && d < bestDist) {
                best = soldier;
                bestDist = d;
            }
        }
        return best;
    }

    private Soldier findSoldierForDefense(Defense defense) {
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            if (soldier.owner == defense) {
                return soldier;
            }
        }
        return null;
    }

    private float beaconMultiplier(Defense defense) {
        float multiplier = 1f;
        for (int i = 0; i < defenses.size(); i++) {
            Defense other = defenses.get(i);
            if (other.type == BUILD_BEACON && other != defense && distance(defense.slot.x, defense.slot.y, other.slot.x, other.slot.y) < 0.24f) {
                multiplier += 0.16f + other.level * 0.05f;
            }
        }
        return multiplier;
    }

    private void buildDefense(Slot slot) {
        if (waveActive && supplies < defenseCost(buildMode) + 20) {
            statusText = "Build Costs More In Wave";
            notifyHud();
            return;
        }
        int cost = defenseCost(buildMode) + (waveActive ? 20 : 0);
        if (supplies < cost) {
            statusText = "Need Supplies " + cost;
            notifyHud();
            return;
        }
        supplies -= cost;
        Defense defense = new Defense();
        defense.type = buildMode;
        defense.level = 1;
        defense.slot = slot;
        defense.cooldown = 0.2f;
        defense.lastAngle = 0f;
        defenses.add(defense);
        effects.add(new Effect(slot.x, slot.y, 0.35f, 0.15f, color(R.color.cst_accent), 1));
        statusText = defenseName(buildMode) + " Built";
        audioEvent("build_tower");
        notifyHud();
    }

    private void upgradeDefense(Defense defense) {
        if (defense.level >= 3) {
            statusText = defenseName(defense.type) + " Max";
            notifyHud();
            return;
        }
        int cost = 38 + defense.level * 28;
        if (supplies < cost) {
            statusText = "Need Supplies " + cost;
            notifyHud();
            return;
        }
        supplies -= cost;
        defense.level++;
        defense.flash = 0.22f;
        effects.add(new Effect(defense.slot.x, defense.slot.y, 0.36f, 0.16f, color(R.color.cst_warning), 1));
        statusText = defenseName(defense.type) + " Tier " + defense.level;
        audioEvent("upgrade");
        notifyHud();
    }

    private void rallySoldiers(int lane) {
        selectedRallyLane = lane;
        int count = 0;
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            soldier.targetLane = lane;
            count++;
        }
        effects.add(new Effect(0.20f, laneY[lane], 0.35f, 0.16f, color(R.color.cst_accent_2), 1));
        statusText = count == 0 ? "No Shields Ready" : "Rally " + laneName(lane);
        audioEvent("shield_block");
        notifyHud();
    }

    private Slot findSlot(float x, float y) {
        Slot best = null;
        float bestDist = 0.075f;
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            float d = distance(x, y, slot.x, slot.y);
            if (d < bestDist) {
                best = slot;
                bestDist = d;
            }
        }
        return best;
    }

    private Defense findDefense(Slot slot) {
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            if (defense.slot == slot) {
                return defense;
            }
        }
        return null;
    }

    private int nearestLane(float y) {
        int best = 0;
        float bestDist = Math.abs(y - laneY[0]);
        for (int i = 1; i < laneY.length; i++) {
            float dist = Math.abs(y - laneY[i]);
            if (dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        return best;
    }

    private void finishRun(boolean cleared) {
        if (state == GameState.GAME_OVER || state == GameState.STAGE_CLEAR) {
            return;
        }
        state = cleared ? GameState.STAGE_CLEAR : GameState.GAME_OVER;
        waveActive = false;
        statusText = cleared ? "Frontier Held" : "Gate Broken";
        notifyHud();
        if (listener != null) {
            listener.onRunEnded(cleared, wave, breaches, calculateStars(cleared), gate);
        }
    }

    private int calculateStars(boolean cleared) {
        if (!cleared) {
            return 0;
        }
        int stars = 1;
        if (gate >= 60) {
            stars++;
        }
        if (breaches <= 2) {
            stars++;
        }
        return stars;
    }

    private void notifyHud() {
        if (listener != null) {
            listener.onHudChanged(gate, supplies, wave, maxWave, breaches, statusText);
        }
    }

    private void audioEvent(String key) {
        if (soundEnabled && listener != null) {
            listener.onAudioEvent(key);
        }
    }

    private String defenseName(int type) {
        if (type == BUILD_BARRACKS) {
            return "Barracks";
        }
        if (type == BUILD_STONE) {
            return "Stone";
        }
        if (type == BUILD_OIL) {
            return "Cauldron";
        }
        if (type == BUILD_BEACON) {
            return "Beacon";
        }
        return "Archer";
    }

    private String laneName(int lane) {
        if (lane == 0) {
            return "North";
        }
        if (lane == 2) {
            return "South";
        }
        return "Center";
    }

    private int defenseCost(int type) {
        if (type == BUILD_BARRACKS) {
            return 58;
        }
        if (type == BUILD_STONE) {
            return 78;
        }
        if (type == BUILD_OIL) {
            return 68;
        }
        if (type == BUILD_BEACON) {
            return 72;
        }
        return 42;
    }

    private float defenseDamage(Defense defense) {
        float base;
        if (defense.type == BUILD_STONE) {
            base = 25f;
        } else if (defense.type == BUILD_OIL) {
            base = 19f;
        } else {
            base = 12f;
        }
        return base * (1f + (defense.level - 1) * 0.42f);
    }

    private float defenseCooldown(Defense defense) {
        if (defense.type == BUILD_STONE) {
            return 1.45f;
        }
        if (defense.type == BUILD_OIL) {
            return 1.10f;
        }
        return 0.58f;
    }

    private float defenseRange(Defense defense) {
        if (defense.type == BUILD_OIL) {
            return 0.23f + defense.level * 0.018f;
        }
        if (defense.type == BUILD_STONE) {
            return 0.46f + defense.level * 0.025f;
        }
        return 0.38f + defense.level * 0.025f;
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private int color(int resId) {
        return getResources().getColor(resId, null);
    }

    private void loadSprites() {
        if (spritesLoaded) {
            return;
        }
        spritesLoaded = true;
        loadSprite("archer", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile249.png");
        loadSprite("stone", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile245.png");
        loadSprite("oil", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile247.png");
        loadSprite("beacon", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile250.png");
        loadSprite("platform", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile181.png");
        loadSprite("projectile", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile251.png");
        loadSprite("siege", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile268.png");
        loadSprite("road", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile037.png");
        unitSheet = loadBitmap("game_art/kenney_roguelike_rpg_pack/assets/Spritesheet/roguelikeSheet_transparent.png");
    }

    private void loadSprite(String key, String path) {
        Bitmap bitmap = loadBitmap(path);
        if (bitmap != null) {
            sprites.put(key, bitmap);
        }
    }

    private Bitmap loadBitmap(String path) {
        AssetManager assets = getContext().getAssets();
        try (InputStream input = assets.open(path)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBoard(canvas);
        drawWall(canvas);
        drawSlots(canvas);
        drawDefenses(canvas);
        drawSoldiers(canvas);
        drawEnemies(canvas);
        drawProjectiles(canvas);
        drawEffects(canvas);
        drawRallyMarkers(canvas);
        if (state == GameState.PLAYING && !waveActive) {
            drawPlanningText(canvas);
        }
    }

    private void drawBoard(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        canvas.drawColor(color(R.color.cst_bg_main));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_bg_alt));
        rect.set(w * 0.02f, h * 0.12f, w * 0.98f, h * 0.88f);
        canvas.drawRoundRect(rect, h * 0.035f, h * 0.035f, paint);
        for (int i = 0; i < laneY.length; i++) {
            float y = h * laneY[i];
            paint.setColor(color(R.color.cst_game_road));
            rect.set(w * 0.18f, y - h * 0.050f, w * 0.96f, y + h * 0.050f);
            canvas.drawRoundRect(rect, h * 0.030f, h * 0.030f, paint);
            paint.setColor(Color.argb(70, 255, 209, 102));
            paint.setStrokeWidth(h * 0.004f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(w * 0.21f, y, w * 0.94f, y, paint);
            paint.setStyle(Paint.Style.FILL);
            drawSprite(canvas, "road", w * 0.91f, y, h * 0.10f, h * 0.10f, 0f, false);
        }
        paint.setColor(color(R.color.cst_game_dust));
        for (int i = 0; i < 18; i++) {
            float x = w * (0.25f + (i * 0.041f) % 0.65f);
            float y = h * (0.18f + ((i * 7) % 63) / 100f);
            canvas.drawCircle(x, y, h * 0.006f, paint);
        }
    }

    private void drawWall(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_stone));
        rect.set(w * 0.07f, h * 0.16f, w * 0.19f, h * 0.84f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(w * 0.10f, h * 0.37f, w * 0.18f, h * 0.63f);
        canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        paint.setColor(color(R.color.cst_game_bronze));
        rect.set(w * 0.12f, h * 0.43f, w * 0.18f, h * 0.63f);
        canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.006f);
        paint.setColor(gate > 35 ? color(R.color.cst_success) : color(R.color.cst_danger));
        rect.set(w * 0.085f, h * 0.18f, w * 0.185f, h * 0.82f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 8; i++) {
            paint.setColor(i % 2 == 0 ? color(R.color.cst_panel_stroke) : color(R.color.cst_game_stone));
            rect.set(w * 0.08f, h * (0.19f + i * 0.075f), w * 0.18f, h * (0.22f + i * 0.075f));
            canvas.drawRect(rect, paint);
        }
    }

    private void drawSlots(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            float x = slot.x * w;
            float y = slot.y * h;
            boolean occupied = findDefense(slot) != null;
            drawSprite(canvas, "platform", x, y, h * 0.11f, h * 0.11f, 0f, false);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.005f);
            paint.setColor(occupied ? color(R.color.cst_accent) : color(R.color.cst_warning));
            rect.set(x - h * 0.042f, y - h * 0.042f, x + h * 0.042f, y + h * 0.042f);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        }
    }

    private void drawDefenses(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            float x = defense.slot.x * w;
            float y = defense.slot.y * h;
            float pulse = 1f + (float) Math.sin(defense.animTime * 5f) * (defense.type == BUILD_BEACON ? 0.08f : 0.025f);
            String key = defenseSprite(defense.type);
            if (defense.type == BUILD_BARRACKS) {
                drawBarracks(canvas, x, y, h, defense);
            } else {
                drawSprite(canvas, key, x, y - h * 0.006f, h * 0.105f * pulse, h * 0.105f * pulse, defense.lastAngle, false);
            }
            if (defense.flash > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.007f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawCircle(x, y, h * (0.055f + defense.flash * 0.08f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.026f);
            paint.setColor(color(R.color.cst_text_on_primary));
            canvas.drawText(String.valueOf(defense.level), x, y + h * 0.057f, paint);
        }
    }

    private void drawBarracks(Canvas canvas, float x, float y, int h, Defense defense) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(x - h * 0.040f, y - h * 0.032f, x + h * 0.040f, y + h * 0.034f);
        canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
        paint.setColor(color(R.color.cst_game_bronze));
        rect.set(x - h * 0.030f, y - h * 0.044f, x + h * 0.030f, y - h * 0.018f);
        canvas.drawRect(rect, paint);
        drawSheetTile(canvas, 52, 16, x, y - h * 0.010f, h * 0.050f, 1f, defense.animTime, STATE_IDLE);
    }

    private String defenseSprite(int type) {
        if (type == BUILD_STONE) {
            return "stone";
        }
        if (type == BUILD_OIL) {
            return "oil";
        }
        if (type == BUILD_BEACON) {
            return "beacon";
        }
        return "archer";
    }

    private void drawSoldiers(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            float x = soldier.x * w;
            float y = soldier.y * h;
            float bob = soldier.state == STATE_MOVE ? (float) Math.sin(soldier.animTime * 14f) * h * 0.006f : 0f;
            drawSheetTile(canvas, 52, 16, x, y + bob, h * 0.075f, soldier.facing, soldier.animTime, soldier.state);
            drawFacingMark(canvas, x, y + bob, h * 0.075f, soldier.facing, color(R.color.cst_accent_2), soldier.state);
            drawHpBar(canvas, x, y - h * 0.055f, h * 0.055f, soldier.hp, soldier.maxHp);
            if (soldier.hitFlash > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.005f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawCircle(x, y, h * 0.045f, paint);
            }
        }
    }

    private void drawEnemies(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            float x = enemy.x * w;
            float y = enemy.y * h;
            float bob = enemy.state == STATE_MOVE ? (float) Math.sin(enemy.animTime * enemy.speed * 165f) * h * 0.010f : 0f;
            float shake = enemy.hitFlash > 0f ? (float) Math.sin(enemy.animTime * 50f) * h * 0.006f : 0f;
            float size = h * enemy.radius * 2.4f;
            if (enemy.type == ENEMY_SIEGE) {
                drawSprite(canvas, "siege", x + shake, y + bob, size * 1.35f, size * 1.35f, enemy.facing < 0f ? 3.14f : 0f, false);
                drawFacingMark(canvas, x + shake, y + bob, size * 0.80f, enemy.facing, color(R.color.cst_danger), enemy.state);
            } else {
                int col = enemy.type == ENEMY_SCOUT ? 52 : 51;
                int row = enemy.type == ENEMY_BRUTE ? 16 : 17;
                if (enemy.type == ENEMY_WARLORD) {
                    col = 52;
                    row = 16;
                }
                drawSheetTile(canvas, col, row, x + shake, y + bob, size, enemy.facing, enemy.animTime, enemy.state);
                int markColor = enemy.type == ENEMY_WARLORD ? color(R.color.cst_danger) : color(R.color.cst_warning);
                drawFacingMark(canvas, x + shake, y + bob, size, enemy.facing, markColor, enemy.state);
                if (enemy.type == ENEMY_WARLORD) {
                    drawWarlordCrest(canvas, x + shake, y + bob, size, enemy.facing);
                }
            }
            if (enemy.burn > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_game_fire));
                canvas.drawCircle(x, y, size * 0.38f, paint);
            }
            drawHpBar(canvas, x, y - size * 0.56f, size * 0.62f, enemy.hp, enemy.maxHp);
        }
    }

    private void drawWarlordCrest(Canvas canvas, float x, float y, float size, float facing) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_fire));
        float fx = facing >= 0f ? 1f : -1f;
        float top = y - size * 0.54f;
        rect.set(x + fx * size * 0.10f - size * 0.08f, top, x + fx * size * 0.10f + size * 0.08f, top + size * 0.20f);
        canvas.drawOval(rect, paint);
    }

    private void drawProjectiles(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile projectile = projectiles.get(i);
            float x = projectile.x * w;
            float y = projectile.y * h;
            if (projectile.type == BUILD_STONE) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_stone));
                canvas.drawCircle(x, y, h * 0.014f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawLine(x, y, x - (float) Math.cos(projectile.angle) * h * 0.035f, y - (float) Math.sin(projectile.angle) * h * 0.035f, paint);
            } else {
                drawSprite(canvas, "projectile", x, y, h * 0.045f, h * 0.045f, projectile.angle, false);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(projectile.type == BUILD_OIL ? color(R.color.cst_game_fire) : color(R.color.cst_accent));
                canvas.drawLine(x, y, x - (float) Math.cos(projectile.angle) * h * 0.030f, y - (float) Math.sin(projectile.angle) * h * 0.030f, paint);
            }
        }
    }

    private void drawEffects(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            float t = effect.life / effect.maxLife;
            float radius = effect.radius * h * (1f - t + 0.35f);
            if (effect.type == 2) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (120 * t), 255, 138, 42));
                rect.set(effect.x * w - radius, effect.y * h - radius * 0.55f, effect.x * w + radius, effect.y * h + radius * 0.55f);
                canvas.drawOval(rect, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.008f * t);
            paint.setColor(effect.color);
            canvas.drawCircle(effect.x * w, effect.y * h, radius, paint);
        }
    }

    private void drawRallyMarkers(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < laneY.length; i++) {
            float x = w * 0.205f;
            float y = h * laneY[i];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == selectedRallyLane ? color(R.color.cst_accent_2) : color(R.color.cst_panel_stroke));
            rect.set(x - h * 0.030f, y - h * 0.030f, x + h * 0.030f, y + h * 0.030f);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
            paint.setColor(color(R.color.cst_text_on_secondary));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.020f);
            canvas.drawText(laneName(i).substring(0, 1).toUpperCase(Locale.US), x, y + h * 0.007f, paint);
        }
    }

    private void drawPlanningText(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.036f);
        paint.setColor(color(R.color.cst_text_primary));
        canvas.drawText("Build on platforms, rally shields, then start the next wave.", w * 0.56f, h * 0.16f, paint);
    }

    private boolean drawSprite(Canvas canvas, String key, float cx, float cy, float maxWidth, float maxHeight, float angle, boolean flip) {
        Bitmap bitmap = sprites.get(key);
        if (bitmap == null) {
            return false;
        }
        float scale = Math.min(maxWidth / bitmap.getWidth(), maxHeight / bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        canvas.save();
        canvas.translate(cx, cy);
        if (angle != 0f) {
            canvas.rotate((float) Math.toDegrees(angle));
        }
        if (flip) {
            canvas.scale(-1f, 1f);
        }
        dstRect.set(-width * 0.5f, -height * 0.5f, width * 0.5f, height * 0.5f);
        canvas.drawBitmap(bitmap, null, dstRect, paint);
        canvas.restore();
        return true;
    }

    private boolean drawSheetTile(Canvas canvas, int col, int row, float cx, float cy, float size, float facing, float animTime, int unitState) {
        if (unitSheet == null) {
            return false;
        }
        int tile = 16;
        int margin = 1;
        int sx = col * (tile + margin);
        int sy = row * (tile + margin);
        srcRect.set(sx, sy, sx + tile, sy + tile);
        float pulse = 1f;
        if (unitState == STATE_ATTACK) {
            pulse = 1f + (float) Math.sin(animTime * 18f) * 0.08f;
        } else if (unitState == STATE_DYING) {
            pulse = 1f - Math.min(0.45f, animTime * 0.6f);
        }
        canvas.save();
        canvas.translate(cx, cy);
        if (facing < 0f) {
            canvas.scale(-1f, 1f);
        }
        dstRect.set(-size * 0.5f * pulse, -size * 0.5f, size * 0.5f * pulse, size * 0.5f);
        canvas.drawBitmap(unitSheet, srcRect, dstRect, paint);
        canvas.restore();
        return true;
    }

    private void drawFacingMark(Canvas canvas, float x, float y, float size, float facing, int markColor, int unitState) {
        float fx = facing >= 0f ? 1f : -1f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(markColor);
        float frontX = x + fx * size * 0.28f;
        rect.set(frontX - size * 0.07f, y - size * 0.12f, frontX + size * 0.07f, y + size * 0.02f);
        canvas.drawOval(rect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.055f);
        paint.setColor(unitState == STATE_ATTACK ? color(R.color.cst_warning) : color(R.color.cst_text_primary));
        float reach = unitState == STATE_ATTACK ? size * 0.50f : size * 0.36f;
        canvas.drawLine(x + fx * size * 0.12f, y + size * 0.05f, x + fx * reach, y - size * 0.05f, paint);
    }

    private void drawHpBar(Canvas canvas, float x, float y, float width, float hp, float maxHp) {
        float height = Math.max(4f, width * 0.10f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_danger));
        rect.set(x - width * 0.5f, y, x + width * 0.5f, y + height);
        canvas.drawRect(rect, paint);
        paint.setColor(color(R.color.cst_success));
        float ratio = Math.max(0f, Math.min(1f, hp / maxHp));
        rect.set(x - width * 0.5f, y, x - width * 0.5f + width * ratio, y + height);
        canvas.drawRect(rect, paint);
    }

    private static final class Slot {
        final float x;
        final float y;
        final int lane;

        Slot(float x, float y, int lane) {
            this.x = x;
            this.y = y;
            this.lane = lane;
        }
    }

    private static final class Defense {
        Slot slot;
        int type;
        int level;
        float cooldown;
        float animTime;
        float flash;
        float lastAngle;
    }

    private static final class Soldier {
        Defense owner;
        int lane;
        int targetLane;
        int level;
        int state;
        float x;
        float y;
        float hp;
        float maxHp;
        float facing;
        float animTime;
        float attackCooldown;
        float hitFlash;
    }

    private static final class Enemy {
        int type;
        int lane;
        int damage;
        int reward;
        int state;
        float x;
        float y;
        float targetY;
        float hp;
        float maxHp;
        float speed;
        float radius;
        float facing;
        float animTime;
        float stateTime;
        float attackCooldown;
        float attackTime;
        float hitFlash;
        float burn;
        boolean siege;
    }

    private static final class Projectile {
        Enemy target;
        int type;
        float x;
        float y;
        float tx;
        float ty;
        float speed;
        float damage;
        float splash;
        float life;
        float angle;
        boolean gateHit;
    }

    private static final class Effect {
        final float x;
        final float y;
        final float maxLife;
        final float radius;
        final int color;
        final int type;
        float life;

        Effect(float x, float y, float life, float radius, int color, int type) {
            this.x = x;
            this.y = y;
            this.life = life;
            this.maxLife = life;
            this.radius = radius;
            this.color = color;
            this.type = type;
        }
    }
}
