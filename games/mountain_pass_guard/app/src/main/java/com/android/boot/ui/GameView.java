package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    public static final int DEPLOY_RIFLE = 0;
    public static final int DEPLOY_GUN = 1;
    public static final int DEPLOY_SNIPER = 2;
    public static final int DEPLOY_ENGINEER = 3;
    public static final int DEPLOY_MORTAR = 4;
    public static final int DEPLOY_SIGNAL = 5;

    private static final int ENEMY_RAIDER = 0;
    private static final int ENEMY_SCOUT = 1;
    private static final int ENEMY_BRUTE = 2;
    private static final int ENEMY_MORTAR = 3;
    private static final int ENEMY_INFILTRATOR = 4;
    private static final int ENEMY_COMMANDER = 5;

    private static final int STATE_IDLE = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_ATTACK = 2;
    private static final int STATE_HIT = 3;
    private static final int STATE_DYING = 4;

    public interface GameListener {
        void onHudChanged(int bunker, int morale, int supplies, int wave, int maxWave, int breaches, String status);
        void onRunEnded(boolean cleared, int wave, int breaches, int stars, int bunker, int morale);
        void onAudioEvent(String key);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF rect = new RectF();
    private final RectF dstRect = new RectF();
    private final Path path = new Path();
    private final Random random = new Random(41);
    private final ArrayList<Defense> defenses = new ArrayList<>();
    private final ArrayList<Soldier> soldiers = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final HashMap<String, Bitmap> sprites = new HashMap<>();
    private final Slot[] slots = new Slot[]{
            new Slot(0.29f, 0.20f, 0, "North Ridge"),
            new Slot(0.43f, 0.16f, 0, "North Bend"),
            new Slot(0.30f, 0.38f, 1, "Tunnel Mouth"),
            new Slot(0.45f, 0.34f, 1, "Tunnel Shelf"),
            new Slot(0.31f, 0.58f, 2, "Bridge Turn"),
            new Slot(0.46f, 0.56f, 2, "Bridge Watch"),
            new Slot(0.33f, 0.77f, 3, "South Cliff"),
            new Slot(0.48f, 0.75f, 3, "South Cut")
    };
    private final float[] laneY = new float[]{0.20f, 0.38f, 0.58f, 0.77f};
    private final float[] routeStartY = new float[]{0.18f, 0.34f, 0.54f, 0.74f};
    private final float[] routeEndY = new float[]{0.24f, 0.42f, 0.61f, 0.80f};
    private GameListener listener;
    private GameState state = GameState.MENU;
    private Bitmap unitSheet;
    private boolean running;
    private boolean soundEnabled = true;
    private boolean spritesLoaded;
    private long lastTick;
    private int bunker = 100;
    private int morale = 100;
    private int supplies = 150;
    private int wave;
    private int maxWave = 8;
    private int breaches;
    private int buildMode = DEPLOY_RIFLE;
    private int spawnRemaining;
    private int selectedRallyLane = 1;
    private float spawnTimer;
    private float hudTimer;
    private boolean waveActive;
    private String statusText = "Assign hidden squads";

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
        buildMode = DEPLOY_RIFLE;
        statusText = "Assign hidden squads";
        notifyHud();
        invalidate();
    }

    public void startGame() {
        bunker = 100;
        morale = 100;
        supplies = 150;
        wave = 0;
        breaches = 0;
        spawnRemaining = 0;
        spawnTimer = 0f;
        selectedRallyLane = 1;
        buildMode = DEPLOY_RIFLE;
        waveActive = false;
        statusText = "Assign hidden squads";
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
        statusText = "Deploy " + defenseName(mode);
        notifyHud();
    }

    public void startNextWave() {
        if (state != GameState.PLAYING || waveActive || wave >= maxWave) {
            return;
        }
        wave++;
        spawnRemaining = 8 + wave * 3;
        if (wave >= 5) {
            spawnRemaining += 2;
        }
        spawnTimer = 0f;
        waveActive = true;
        supplies += 14 + wave * 2;
        morale = Math.min(100, morale + 3);
        statusText = wave == maxWave ? "Final assault incoming" : "Enemy routes active";
        audioEvent("wave_start");
        if (wave == maxWave || wave % 2 == 0) {
            audioEvent("siege_warning");
        }
        notifyHud();
    }

    public void useFlareScan() {
        if (state != GameState.PLAYING || supplies < 18) {
            statusText = "Need Supplies 18";
            notifyHud();
            return;
        }
        supplies -= 18;
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            if (!defense.revealed) {
                defense.revealed = true;
                defense.flash = 0.18f;
                if (defense.type == DEPLOY_ENGINEER) {
                    spawnOrHealEngineer(defense, true);
                }
            }
        }
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            enemy.scanned = 2.4f;
            if (enemy.type == ENEMY_INFILTRATOR) {
                enemy.hitFlash = 0.30f;
            }
        }
        effects.add(new Effect(0.56f, 0.50f, 0.35f, 0.52f, color(R.color.cst_accent), 3));
        statusText = "Flare scan exposed the ridges";
        audioEvent("build_tower");
        notifyHud();
    }

    public void useDemolition() {
        if (state != GameState.PLAYING || supplies < 32) {
            statusText = "Need Supplies 32";
            notifyHud();
            return;
        }
        boolean hit = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.lane == selectedRallyLane && enemy.x < 0.60f && enemy.state != STATE_DYING) {
                damageEnemy(enemy, 26f + wave * 2f);
                enemy.burn = 1.2f;
                hit = true;
            }
        }
        if (!hit) {
            statusText = "No route cluster in demolition range";
            notifyHud();
            return;
        }
        supplies -= 32;
        morale = Math.min(100, morale + 4);
        effects.add(new Effect(0.55f, laneY[selectedRallyLane], 0.45f, 0.26f, color(R.color.cst_game_fire), 2));
        statusText = "Rockslide triggered on " + laneName(selectedRallyLane);
        audioEvent("stone_throw");
        notifyHud();
    }

    public void useRetreatOrder() {
        if (state != GameState.PLAYING || supplies < 22) {
            statusText = "Need Supplies 22";
            notifyHud();
            return;
        }
        boolean moved = false;
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            if (soldier.revealed) {
                soldier.x = Math.max(0.26f, soldier.x - 0.05f);
                soldier.hp = Math.min(soldier.maxHp, soldier.hp + 12f);
                moved = true;
            }
        }
        if (!moved) {
            statusText = "No revealed squad to retreat";
            notifyHud();
            return;
        }
        supplies -= 22;
        morale = Math.min(100, morale + 10);
        effects.add(new Effect(0.28f, 0.50f, 0.30f, 0.22f, color(R.color.cst_success), 1));
        statusText = "Fallback order restored discipline";
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
        if (nx < 0.18f && Math.abs(ny - laneY[lane]) < 0.09f) {
            rallySoldiers(lane);
            return true;
        }
        Slot slot = findSlot(nx, ny);
        if (slot != null) {
            Defense existing = findDefense(slot);
            if (existing == null) {
                assignHiddenSquad(slot);
            } else if (!existing.revealed) {
                revealSquad(existing);
            } else {
                upgradeDefense(existing);
            }
            return true;
        }
        statusText = "Tap a route marker or ridge slot";
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
            supplies += 20 + wave * 5;
            morale = Math.min(100, morale + 8);
            if (wave >= maxWave) {
                finishRun(true);
            } else {
                statusText = "Regroup and assign the next ambush";
                notifyHud();
            }
        }
        if (morale <= 0 && state == GameState.PLAYING) {
            morale = 0;
            statusText = "Morale collapsed";
            finishRun(false);
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
            spawnTimer = Math.max(0.36f, 1.00f - wave * 0.055f);
        }
    }

    private void spawnEnemy() {
        int lane = random.nextInt(laneY.length);
        int type = ENEMY_RAIDER;
        if (wave == maxWave && spawnRemaining <= 1) {
            type = ENEMY_COMMANDER;
        } else if (wave >= 6 && spawnRemaining % 6 == 0) {
            type = ENEMY_INFILTRATOR;
        } else if (wave >= 4 && spawnRemaining % 5 == 0) {
            type = ENEMY_MORTAR;
        } else if (wave >= 3 && spawnRemaining % 4 == 0) {
            type = ENEMY_BRUTE;
        } else if (wave >= 2 && spawnRemaining % 3 == 0) {
            type = ENEMY_SCOUT;
        }
        Enemy enemy = new Enemy();
        enemy.type = type;
        enemy.lane = lane;
        enemy.x = 1.02f + random.nextFloat() * 0.04f;
        enemy.y = routeStartY[lane] + (random.nextFloat() - 0.5f) * 0.020f;
        enemy.targetY = routeEndY[lane];
        enemy.routeBend = 0.72f;
        enemy.facing = -1f;
        enemy.radius = 0.030f;
        enemy.maxHp = 34f + wave * 10f;
        enemy.speed = 0.050f + wave * 0.004f;
        enemy.damage = 7 + wave;
        enemy.reward = 7 + wave;
        enemy.attackTime = 0.90f;
        enemy.state = STATE_MOVE;
        if (type == ENEMY_SCOUT) {
            enemy.maxHp *= 0.74f;
            enemy.speed *= 1.55f;
            enemy.damage += 1;
            enemy.reward += 4;
            enemy.radius = 0.026f;
        } else if (type == ENEMY_BRUTE) {
            enemy.maxHp *= 1.90f;
            enemy.speed *= 0.72f;
            enemy.damage += 6;
            enemy.reward += 10;
            enemy.radius = 0.040f;
        } else if (type == ENEMY_MORTAR) {
            enemy.maxHp *= 1.55f;
            enemy.speed *= 0.66f;
            enemy.damage += 6;
            enemy.reward += 12;
            enemy.radius = 0.044f;
            enemy.mortar = true;
            enemy.attackTime = 1.55f;
        } else if (type == ENEMY_INFILTRATOR) {
            enemy.maxHp *= 0.95f;
            enemy.speed *= 1.35f;
            enemy.damage += 3;
            enemy.reward += 16;
            enemy.radius = 0.028f;
            enemy.infiltrator = true;
            enemy.attackTime = 0.72f;
        } else if (type == ENEMY_COMMANDER) {
            enemy.maxHp *= 5.60f;
            enemy.speed *= 0.60f;
            enemy.damage += 15;
            enemy.reward += 58;
            enemy.radius = 0.058f;
            enemy.mortar = true;
            enemy.attackTime = 1.18f;
        }
        enemy.hp = enemy.maxHp;
        enemies.add(enemy);
    }

    private void updateDefenses(float dt) {
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            defense.animTime += dt;
            defense.cooldown -= dt * signalMultiplier(defense);
            if (defense.flash > 0f) {
                defense.flash -= dt;
            }
            if (!defense.revealed) {
                if (waveActive && nearestEnemyDistance(defense.slot) < 0.20f) {
                    revealSquad(defense);
                }
                continue;
            }
            if (defense.type == DEPLOY_ENGINEER) {
                defense.cooldown -= dt;
                if (defense.cooldown <= 0f) {
                    spawnOrHealEngineer(defense, false);
                    defense.cooldown = 2.6f;
                }
                continue;
            }
            if (defense.type == DEPLOY_SIGNAL) {
                if (defense.cooldown <= 0f) {
                    defense.cooldown = 1.15f;
                    morale = Math.min(100, morale + 1);
                    effects.add(new Effect(defense.slot.x, defense.slot.y, 0.24f, 0.12f + defense.level * 0.02f, color(R.color.cst_accent), 1));
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
                effects.add(new Effect(soldier.x, soldier.y, 0.30f, 0.10f, color(R.color.cst_danger), 1));
                morale = Math.max(0, morale - 3);
                soldiers.remove(i);
                continue;
            }
            float desiredY = laneY[soldier.targetLane];
            float desiredX = soldier.isEngineer ? 0.25f : 0.27f;
            if (soldier.targetLane == selectedRallyLane && !soldier.isEngineer) {
                desiredX = 0.34f;
            }
            float dx = desiredX - soldier.x;
            float dy = desiredY - soldier.y;
            if (Math.abs(dx) > 0.003f) {
                soldier.x += Math.signum(dx) * Math.min(Math.abs(dx), dt * 0.18f);
            }
            if (Math.abs(dy) > 0.004f) {
                soldier.y += Math.signum(dy) * Math.min(Math.abs(dy), dt * 0.32f);
                soldier.state = STATE_MOVE;
            } else {
                soldier.y = desiredY;
                soldier.lane = soldier.targetLane;
                if (Math.abs(dx) <= 0.01f) {
                    soldier.state = STATE_IDLE;
                }
            }
            if (soldier.isEngineer) {
                if (bunker < 100 && soldier.attackCooldown <= 0f) {
                    bunker = Math.min(100, bunker + soldier.level);
                    morale = Math.min(100, morale + 1);
                    soldier.attackCooldown = 1.8f;
                    effects.add(new Effect(0.11f, laneY[soldier.lane], 0.18f, 0.07f, color(R.color.cst_success), 1));
                } else {
                    soldier.attackCooldown -= dt;
                }
                soldier.facing = 1f;
                continue;
            }
            Enemy target = findSoldierTarget(soldier);
            if (target != null) {
                soldier.facing = target.x >= soldier.x ? 1f : -1f;
                soldier.state = STATE_ATTACK;
                soldier.attackCooldown -= dt;
                if (soldier.attackCooldown <= 0f) {
                    damageEnemy(target, 7f + soldier.level * 3f);
                    soldier.attackCooldown = 0.52f;
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
            if (enemy.scanned > 0f) {
                enemy.scanned -= dt;
            }
            if (enemy.burn > 0f) {
                enemy.burn -= dt;
                damageEnemy(enemy, dt * (8f + wave));
            }
            if (enemy.hp <= 0f && enemy.state != STATE_DYING) {
                enemy.state = STATE_DYING;
                enemy.stateTime = 0f;
                effects.add(new Effect(enemy.x, enemy.y, 0.42f, enemy.radius * 3.0f, color(R.color.cst_success), 1));
                audioEvent("enemy_death");
            }
            if (enemy.state == STATE_DYING) {
                if (enemy.stateTime > 0.25f) {
                    supplies += enemy.reward;
                    effects.add(new Effect(enemy.x, enemy.y, 0.24f, enemy.radius * 1.8f, color(R.color.cst_warning), 1));
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
                    blocker.hitFlash = 0.16f;
                    enemy.attackCooldown = enemy.attackTime;
                    effects.add(new Effect(blocker.x, blocker.y, 0.20f, 0.08f, color(R.color.cst_warning), 1));
                    audioEvent("shield_block");
                }
                continue;
            }
            if (enemy.mortar && enemy.x <= 0.64f) {
                enemy.state = STATE_ATTACK;
                enemy.facing = -1f;
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    fireMortar(enemy);
                    enemy.attackCooldown = enemy.attackTime;
                }
                continue;
            }
            if (enemy.infiltrator && enemy.x <= 0.17f) {
                morale = Math.max(0, morale - 18);
                bunker = Math.max(0, bunker - 12);
                statusText = "Infiltrator reached the bunker";
                effects.add(new Effect(0.12f, laneY[enemy.lane], 0.34f, 0.16f, color(R.color.cst_danger), 1));
                enemies.remove(i);
                breaches++;
                if (bunker <= 0 || morale <= 0) {
                    finishRun(false);
                }
                continue;
            }
            if (enemy.x <= 0.13f) {
                enemy.state = STATE_ATTACK;
                enemy.facing = -1f;
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    damageBunker(enemy.damage);
                    enemy.attackCooldown = enemy.attackTime;
                    effects.add(new Effect(0.11f, laneY[enemy.lane], 0.26f, 0.10f, color(R.color.cst_danger), 1));
                }
                continue;
            }
            enemy.state = STATE_MOVE;
            enemy.facing = -1f;
            enemy.x -= enemy.speed * dt;
            float t = Math.max(0f, Math.min(1f, (1.02f - enemy.x) / (1.02f - enemy.routeBend)));
            enemy.targetY = routeStartY[enemy.lane] + (routeEndY[enemy.lane] - routeStartY[enemy.lane]) * t;
            float dy = enemy.targetY - enemy.y;
            enemy.y += dy * Math.min(1f, dt * 3.5f);
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
                if (projectile.bunkerHit) {
                    damageBunker((int) projectile.damage);
                    effects.add(new Effect(0.11f, projectile.ty, 0.28f, 0.11f, color(R.color.cst_danger), 1));
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

    private void spawnOrHealEngineer(Defense defense, boolean instant) {
        Soldier soldier = findSoldierForDefense(defense);
        if (soldier == null) {
            Soldier created = new Soldier();
            created.owner = defense;
            created.lane = defense.slot.lane;
            created.targetLane = defense.slot.lane;
            created.x = 0.24f;
            created.y = laneY[created.lane];
            created.facing = 1f;
            created.level = defense.level;
            created.maxHp = 36f + defense.level * 18f;
            created.hp = created.maxHp;
            created.state = STATE_IDLE;
            created.revealed = true;
            created.isEngineer = true;
            soldiers.add(created);
            statusText = "Engineer team moved to the bunker line";
            if (instant) {
                effects.add(new Effect(created.x, created.y, 0.22f, 0.08f, color(R.color.cst_success), 1));
            }
            return;
        }
        soldier.level = defense.level;
        soldier.maxHp = 36f + defense.level * 18f;
        soldier.hp = Math.min(soldier.maxHp, soldier.hp + (instant ? 12f : 4f));
    }

    private void fireDefense(Defense defense, Enemy target) {
        Projectile projectile = new Projectile();
        projectile.x = defense.slot.x;
        projectile.y = defense.slot.y;
        projectile.tx = target.x;
        projectile.ty = target.y;
        projectile.target = target;
        projectile.damage = defenseDamage(defense);
        projectile.speed = defense.type == DEPLOY_MORTAR ? 0.62f : (defense.type == DEPLOY_SNIPER ? 1.12f : 0.88f);
        projectile.splash = defense.type == DEPLOY_MORTAR ? 0.090f + defense.level * 0.010f : 0.0f;
        projectile.type = defense.type;
        projectile.life = 1.6f;
        projectiles.add(projectile);
        defense.lastAngle = (float) Math.atan2(target.y - defense.slot.y, target.x - defense.slot.x);
        if (defense.type == DEPLOY_MORTAR) {
            audioEvent("stone_throw");
        } else {
            audioEvent("archer_shot");
        }
    }

    private void fireMortar(Enemy enemy) {
        Projectile projectile = new Projectile();
        projectile.x = enemy.x - enemy.radius * 0.5f;
        projectile.y = enemy.y;
        projectile.tx = 0.11f;
        projectile.ty = laneY[enemy.lane];
        projectile.damage = enemy.type == ENEMY_COMMANDER ? 14f : 9f;
        projectile.speed = 0.46f;
        projectile.splash = 0f;
        projectile.type = DEPLOY_MORTAR;
        projectile.bunkerHit = true;
        projectile.life = 2.2f;
        projectiles.add(projectile);
        effects.add(new Effect(enemy.x, enemy.y, 0.20f, 0.09f, color(R.color.cst_warning), 1));
        audioEvent("stone_throw");
    }

    private void applyProjectileDamage(Projectile projectile) {
        if (projectile.splash > 0f) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.state != STATE_DYING && distance(projectile.tx, projectile.ty, enemy.x, enemy.y) < projectile.splash) {
                    damageEnemy(enemy, projectile.damage * 0.74f);
                }
            }
            effects.add(new Effect(projectile.tx, projectile.ty, 0.30f, projectile.splash, color(R.color.cst_warning), 1));
        } else if (projectile.target != null) {
            damageEnemy(projectile.target, projectile.damage);
            effects.add(new Effect(projectile.tx, projectile.ty, 0.18f, 0.055f, color(R.color.cst_accent), 1));
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

    private void damageBunker(int amount) {
        bunker -= amount;
        morale = Math.max(0, morale - Math.max(4, amount / 2));
        breaches++;
        audioEvent("gate_damage");
        if (bunker <= 0) {
            bunker = 0;
            finishRun(false);
        }
        notifyHud();
    }

    private Enemy findTarget(Defense defense) {
        Enemy best = null;
        float bestScore = 100f;
        float range = defenseRange(defense);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING) {
                continue;
            }
            float d = distance(defense.slot.x, defense.slot.y, enemy.x, enemy.y);
            if (d < range) {
                float score = d;
                if (enemy.infiltrator) {
                    score -= 0.12f;
                }
                if (enemy.lane == selectedRallyLane) {
                    score -= 0.04f;
                }
                if (score < bestScore) {
                    best = enemy;
                    bestScore = score;
                }
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
            if (d < 0.14f && d < bestDist) {
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
            if (soldier.lane != enemy.lane || soldier.hp <= 0f || soldier.isEngineer) {
                continue;
            }
            float d = Math.abs(enemy.x - soldier.x);
            if (d < 0.070f && d < bestDist) {
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

    private float signalMultiplier(Defense defense) {
        float multiplier = 1f;
        for (int i = 0; i < defenses.size(); i++) {
            Defense other = defenses.get(i);
            if (other.revealed && other.type == DEPLOY_SIGNAL && other != defense && distance(defense.slot.x, defense.slot.y, other.slot.x, other.slot.y) < 0.26f) {
                multiplier += 0.14f + other.level * 0.05f;
            }
        }
        return multiplier;
    }

    private void assignHiddenSquad(Slot slot) {
        int cost = defenseCost(buildMode) + (waveActive ? 8 : 0);
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
        defense.cooldown = 0.4f;
        defense.lastAngle = 0f;
        defense.revealed = false;
        defenses.add(defense);
        effects.add(new Effect(slot.x, slot.y, 0.22f, 0.10f, color(R.color.cst_accent_2), 1));
        statusText = defenseName(buildMode) + " hidden in " + slot.label;
        audioEvent("build_tower");
        notifyHud();
    }

    private void revealSquad(Defense defense) {
        defense.revealed = true;
        defense.flash = 0.22f;
        if (defense.type == DEPLOY_ENGINEER) {
            spawnOrHealEngineer(defense, true);
        }
        if (defense.type == DEPLOY_RIFLE || defense.type == DEPLOY_GUN) {
            spawnGuardIfNeeded(defense);
        }
        effects.add(new Effect(defense.slot.x, defense.slot.y, 0.28f, 0.14f, color(R.color.cst_warning), 1));
        statusText = defenseName(defense.type) + " revealed on " + laneName(defense.slot.lane);
        audioEvent("upgrade");
        notifyHud();
    }

    private void spawnGuardIfNeeded(Defense defense) {
        Soldier soldier = findSoldierForDefense(defense);
        if (soldier != null) {
            soldier.revealed = true;
            soldier.level = defense.level;
            soldier.maxHp = 42f + defense.level * 20f;
            soldier.hp = Math.min(soldier.maxHp, soldier.hp + 10f);
            return;
        }
        Soldier created = new Soldier();
        created.owner = defense;
        created.lane = defense.slot.lane;
        created.targetLane = defense.slot.lane;
        created.x = defense.type == DEPLOY_GUN ? 0.31f : 0.28f;
        created.y = laneY[created.lane];
        created.facing = 1f;
        created.level = defense.level;
        created.maxHp = defense.type == DEPLOY_GUN ? 54f + defense.level * 24f : 42f + defense.level * 20f;
        created.hp = created.maxHp;
        created.state = STATE_IDLE;
        created.revealed = true;
        created.isEngineer = false;
        soldiers.add(created);
    }

    private void upgradeDefense(Defense defense) {
        if (defense.level >= 3) {
            statusText = defenseName(defense.type) + " maxed";
            notifyHud();
            return;
        }
        int cost = 30 + defense.level * 24;
        if (supplies < cost) {
            statusText = "Need Supplies " + cost;
            notifyHud();
            return;
        }
        supplies -= cost;
        defense.level++;
        defense.flash = 0.22f;
        effects.add(new Effect(defense.slot.x, defense.slot.y, 0.26f, 0.14f, color(R.color.cst_warning), 1));
        if (defense.type == DEPLOY_ENGINEER) {
            spawnOrHealEngineer(defense, true);
        }
        if (defense.type == DEPLOY_RIFLE || defense.type == DEPLOY_GUN) {
            spawnGuardIfNeeded(defense);
        }
        statusText = defenseName(defense.type) + " tier " + defense.level;
        audioEvent("upgrade");
        notifyHud();
    }

    private void rallySoldiers(int lane) {
        selectedRallyLane = lane;
        int count = 0;
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            if (!soldier.isEngineer) {
                soldier.targetLane = lane;
                count++;
            }
        }
        effects.add(new Effect(0.15f, laneY[lane], 0.28f, 0.13f, color(R.color.cst_accent_2), 1));
        statusText = count == 0 ? "No revealed guard to redirect" : "Reserve focus moved to " + laneName(lane);
        audioEvent("shield_block");
        notifyHud();
    }

    private float nearestEnemyDistance(Slot slot) {
        float best = 99f;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING) {
                continue;
            }
            float d = distance(slot.x, slot.y, enemy.x, enemy.y);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    private Slot findSlot(float x, float y) {
        Slot best = null;
        float bestDist = 0.078f;
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
        statusText = cleared ? "The pass held" : "The bunker was overrun";
        notifyHud();
        if (listener != null) {
            listener.onRunEnded(cleared, wave, breaches, calculateStars(cleared), bunker, morale);
        }
    }

    private int calculateStars(boolean cleared) {
        if (!cleared) {
            return 0;
        }
        int stars = 1;
        if (bunker >= 60) {
            stars++;
        }
        if (morale >= 70 && breaches <= 4) {
            stars++;
        }
        return stars;
    }

    private void notifyHud() {
        if (listener != null) {
            listener.onHudChanged(bunker, morale, supplies, wave, maxWave, breaches, statusText);
        }
    }

    private void audioEvent(String key) {
        if (soundEnabled && listener != null) {
            listener.onAudioEvent(key);
        }
    }

    private String defenseName(int type) {
        if (type == DEPLOY_GUN) {
            return "Gun Nest";
        }
        if (type == DEPLOY_SNIPER) {
            return "Sniper Ridge";
        }
        if (type == DEPLOY_ENGINEER) {
            return "Engineers";
        }
        if (type == DEPLOY_MORTAR) {
            return "Mortar Pit";
        }
        if (type == DEPLOY_SIGNAL) {
            return "Signal Post";
        }
        return "Rifle Squad";
    }

    private String laneName(int lane) {
        if (lane == 0) {
            return "North Fork";
        }
        if (lane == 1) {
            return "Tunnel Fork";
        }
        if (lane == 2) {
            return "Bridge Fork";
        }
        return "South Fork";
    }

    private int defenseCost(int type) {
        if (type == DEPLOY_GUN) {
            return 48;
        }
        if (type == DEPLOY_SNIPER) {
            return 54;
        }
        if (type == DEPLOY_ENGINEER) {
            return 42;
        }
        if (type == DEPLOY_MORTAR) {
            return 62;
        }
        if (type == DEPLOY_SIGNAL) {
            return 46;
        }
        return 34;
    }

    private float defenseDamage(Defense defense) {
        float base;
        if (defense.type == DEPLOY_GUN) {
            base = 11f;
        } else if (defense.type == DEPLOY_SNIPER) {
            base = 22f;
        } else if (defense.type == DEPLOY_MORTAR) {
            base = 19f;
        } else {
            base = 13f;
        }
        return base * (1f + (defense.level - 1) * 0.42f);
    }

    private float defenseCooldown(Defense defense) {
        if (defense.type == DEPLOY_GUN) {
            return 0.36f;
        }
        if (defense.type == DEPLOY_SNIPER) {
            return 0.92f;
        }
        if (defense.type == DEPLOY_MORTAR) {
            return 1.34f;
        }
        return 0.58f;
    }

    private float defenseRange(Defense defense) {
        if (defense.type == DEPLOY_MORTAR) {
            return 0.48f + defense.level * 0.030f;
        }
        if (defense.type == DEPLOY_SNIPER) {
            return 0.55f + defense.level * 0.025f;
        }
        if (defense.type == DEPLOY_GUN) {
            return 0.28f + defense.level * 0.018f;
        }
        return 0.36f + defense.level * 0.022f;
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
        loadSprite("gun", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile249.png");
        loadSprite("sniper", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile245.png");
        loadSprite("mortar", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile247.png");
        loadSprite("signal", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile250.png");
        loadSprite("platform", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile181.png");
        loadSprite("projectile", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile251.png");
        loadSprite("truck", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile268.png");
        loadSprite("road", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile037.png");
        loadSprite("guard_rifle", "game_art/kenney_top_down_shooter/assets/PNG/Man Blue/manBlue_hold.png");
        loadSprite("guard_gun", "game_art/kenney_top_down_shooter/assets/PNG/Man Blue/manBlue_machine.png");
        loadSprite("guard_sniper", "game_art/kenney_top_down_shooter/assets/PNG/Man Blue/manBlue_gun.png");
        loadSprite("guard_engineer", "game_art/kenney_top_down_shooter/assets/PNG/Man Brown/manBrown_hold.png");
        loadSprite("enemy_raider", "game_art/kenney_top_down_shooter/assets/PNG/Soldier 1/soldier1_hold.png");
        loadSprite("enemy_scout", "game_art/kenney_top_down_shooter/assets/PNG/Soldier 1/soldier1_gun.png");
        loadSprite("enemy_brute", "game_art/kenney_top_down_shooter/assets/PNG/Robot 1/robot1_hold.png");
        loadSprite("enemy_infiltrator", "game_art/kenney_top_down_shooter/assets/PNG/Hitman 1/hitman1_silencer.png");
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
        drawBunker(canvas);
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
        rect.set(w * 0.02f, h * 0.05f, w * 0.98f, h * 0.95f);
        canvas.drawRoundRect(rect, h * 0.040f, h * 0.040f, paint);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(w * 0.12f, h * 0.08f, w * 0.96f, h * 0.92f);
        canvas.drawRoundRect(rect, h * 0.028f, h * 0.028f, paint);
        for (int i = 0; i < laneY.length; i++) {
            float startX = w * 0.22f;
            float bendX = w * 0.58f;
            float endX = w * 0.96f;
            float startY = h * routeStartY[i];
            float bendY = h * laneY[i];
            float endY = h * routeEndY[i];
            path.reset();
            path.moveTo(startX, startY);
            path.lineTo(bendX, bendY);
            path.lineTo(endX, endY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.074f);
            paint.setColor(color(R.color.cst_game_road));
            canvas.drawPath(path, paint);
            paint.setStrokeWidth(h * 0.010f);
            paint.setColor(Color.argb(90, 255, 209, 102));
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            drawSprite(canvas, "road", w * 0.88f, endY, h * 0.090f, h * 0.090f, 0f, false);
        }
        paint.setColor(color(R.color.cst_game_stone));
        for (int i = 0; i < 7; i++) {
            float x = w * (0.26f + i * 0.09f);
            float top = h * (0.08f + (i % 2 == 0 ? 0.04f : 0.10f));
            rect.set(x, top, x + w * 0.05f, top + h * 0.12f);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        }
        paint.setColor(color(R.color.cst_game_dust));
        for (int i = 0; i < 22; i++) {
            float x = w * (0.24f + (i * 0.032f) % 0.70f);
            float y = h * (0.10f + ((i * 9) % 74) / 100f);
            canvas.drawCircle(x, y, h * 0.005f, paint);
        }
    }

    private void drawBunker(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_stone));
        rect.set(w * 0.04f, h * 0.36f, w * 0.16f, h * 0.66f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(w * 0.07f, h * 0.41f, w * 0.15f, h * 0.61f);
        canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        paint.setColor(color(R.color.cst_game_bronze));
        rect.set(w * 0.09f, h * 0.46f, w * 0.15f, h * 0.61f);
        canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.006f);
        paint.setColor(bunker > 35 ? color(R.color.cst_success) : color(R.color.cst_danger));
        rect.set(w * 0.05f, h * 0.37f, w * 0.15f, h * 0.65f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_warning));
        path.reset();
        path.moveTo(w * 0.104f, h * 0.30f);
        path.lineTo(w * 0.120f, h * 0.30f);
        path.lineTo(w * 0.120f, h * 0.40f);
        path.lineTo(w * 0.142f, h * 0.37f);
        path.lineTo(w * 0.120f, h * 0.34f);
        path.lineTo(w * 0.120f, h * 0.25f);
        path.lineTo(w * 0.104f, h * 0.25f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawSlots(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            float x = slot.x * w;
            float y = slot.y * h;
            Defense defense = findDefense(slot);
            drawSprite(canvas, "platform", x, y, h * 0.10f, h * 0.10f, 0f, false);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.004f);
            paint.setColor(defense == null ? color(R.color.cst_warning) : (defense.revealed ? color(R.color.cst_accent) : color(R.color.cst_accent_2)));
            rect.set(x - h * 0.038f, y - h * 0.038f, x + h * 0.038f, y + h * 0.038f);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
            if (defense == null || !defense.revealed) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_panel_bg));
                canvas.drawCircle(x, y, h * 0.014f, paint);
            }
        }
    }

    private void drawDefenses(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < defenses.size(); i++) {
            Defense defense = defenses.get(i);
            float x = defense.slot.x * w;
            float y = defense.slot.y * h;
            if (!defense.revealed) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_accent_2));
                rect.set(x - h * 0.020f, y - h * 0.020f, x + h * 0.020f, y + h * 0.020f);
                canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
                paint.setColor(color(R.color.cst_text_on_secondary));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(h * 0.018f);
                canvas.drawText("?", x, y + h * 0.006f, paint);
                continue;
            }
            float pulse = 1f + (float) Math.sin(defense.animTime * 5f) * (defense.type == DEPLOY_SIGNAL ? 0.08f : 0.025f);
            String key = defenseSprite(defense.type);
            if (defense.type == DEPLOY_ENGINEER) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_bronze));
                rect.set(x - h * 0.030f, y - h * 0.026f, x + h * 0.030f, y + h * 0.028f);
                canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
                drawSheetTile(canvas, 52, 16, x, y - h * 0.008f, h * 0.050f, 1f, defense.animTime, STATE_IDLE);
            } else {
                drawSprite(canvas, key, x, y - h * 0.004f, h * 0.098f * pulse, h * 0.098f * pulse, defense.lastAngle, false);
            }
            if (defense.flash > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.006f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawCircle(x, y, h * (0.050f + defense.flash * 0.05f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.024f);
            paint.setColor(color(R.color.cst_text_on_primary));
            canvas.drawText(String.valueOf(defense.level), x, y + h * 0.054f, paint);
        }
    }

    private String defenseSprite(int type) {
        if (type == DEPLOY_SNIPER) {
            return "sniper";
        }
        if (type == DEPLOY_MORTAR) {
            return "mortar";
        }
        if (type == DEPLOY_SIGNAL) {
            return "signal";
        }
        return "gun";
    }

    private void drawSoldiers(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < soldiers.size(); i++) {
            Soldier soldier = soldiers.get(i);
            float x = soldier.x * w;
            float y = soldier.y * h;
            float bob = soldier.state == STATE_MOVE ? (float) Math.sin(soldier.animTime * 14f) * h * 0.006f : 0f;
            float size = h * 0.078f;
            if (!drawSprite(canvas, soldierSprite(soldier), x, y + bob, size, size, 0f, soldier.facing < 0f)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(soldier.isEngineer ? color(R.color.cst_success) : color(R.color.cst_accent_2));
                canvas.drawCircle(x, y + bob, h * 0.020f, paint);
            }
            int markColor = soldier.isEngineer ? color(R.color.cst_success) : color(R.color.cst_accent_2);
            drawFacingMark(canvas, x, y + bob, size, soldier.facing, markColor, soldier.state);
            drawHpBar(canvas, x, y - h * 0.052f, h * 0.050f, soldier.hp, soldier.maxHp);
            if (soldier.hitFlash > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawCircle(x, y, h * 0.042f, paint);
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
            float shake = enemy.hitFlash > 0f ? (float) Math.sin(enemy.animTime * 50f) * h * 0.005f : 0f;
            float size = h * enemy.radius * 2.4f;
            if (enemy.mortar) {
                drawSprite(canvas, "truck", x + shake, y + bob, size * 1.35f, size * 1.35f, enemy.facing < 0f ? 3.14f : 0f, false);
                drawFacingMark(canvas, x + shake, y + bob, size * 0.84f, enemy.facing, color(R.color.cst_danger), enemy.state);
            } else {
                String key = enemySprite(enemy);
                if (!drawSprite(canvas, key, x + shake, y + bob, size, size, 0f, enemy.facing < 0f)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(enemy.type == ENEMY_INFILTRATOR ? color(R.color.cst_accent) : color(R.color.cst_danger));
                    canvas.drawCircle(x + shake, y + bob, size * 0.26f, paint);
                }
                int markColor = enemy.type == ENEMY_INFILTRATOR ? color(R.color.cst_accent) : color(R.color.cst_warning);
                drawFacingMark(canvas, x + shake, y + bob, size, enemy.facing, markColor, enemy.state);
            }
            if (enemy.scanned > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_accent));
                canvas.drawCircle(x, y, size * 0.40f, paint);
            }
            if (enemy.burn > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_game_fire));
                canvas.drawCircle(x, y, size * 0.34f, paint);
            }
            drawHpBar(canvas, x, y - size * 0.55f, size * 0.62f, enemy.hp, enemy.maxHp);
        }
    }

    private void drawProjectiles(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile projectile = projectiles.get(i);
            float x = projectile.x * w;
            float y = projectile.y * h;
            if (projectile.type == DEPLOY_MORTAR) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_stone));
                canvas.drawCircle(x, y, h * 0.013f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawLine(x, y, x - (float) Math.cos(projectile.angle) * h * 0.035f, y - (float) Math.sin(projectile.angle) * h * 0.035f, paint);
            } else {
                drawSprite(canvas, "projectile", x, y, h * 0.040f, h * 0.040f, projectile.angle, false);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(projectile.type == DEPLOY_SNIPER ? color(R.color.cst_warning) : color(R.color.cst_accent));
                canvas.drawLine(x, y, x - (float) Math.cos(projectile.angle) * h * 0.028f, y - (float) Math.sin(projectile.angle) * h * 0.028f, paint);
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
            } else if (effect.type == 3) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.010f * t);
                paint.setColor(color(R.color.cst_accent));
                canvas.drawCircle(effect.x * w, effect.y * h, radius, paint);
                continue;
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.007f * t);
            paint.setColor(effect.color);
            canvas.drawCircle(effect.x * w, effect.y * h, radius, paint);
        }
    }

    private void drawRallyMarkers(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < laneY.length; i++) {
            float x = w * 0.16f;
            float y = h * laneY[i];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == selectedRallyLane ? color(R.color.cst_accent_2) : color(R.color.cst_panel_stroke));
            rect.set(x - h * 0.026f, y - h * 0.026f, x + h * 0.026f, y + h * 0.026f);
            canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
            paint.setColor(color(R.color.cst_text_on_secondary));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.018f);
            canvas.drawText(String.valueOf(i + 1), x, y + h * 0.006f, paint);
        }
    }

    private void drawPlanningText(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.032f);
        paint.setColor(color(R.color.cst_text_primary));
        canvas.drawText("Hide squads on ridge slots, reveal them on contact, then trigger the next assault.", w * 0.57f, h * 0.11f, paint);
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
        paint.setStrokeWidth(size * 0.050f);
        paint.setColor(unitState == STATE_ATTACK ? color(R.color.cst_warning) : color(R.color.cst_text_primary));
        float reach = unitState == STATE_ATTACK ? size * 0.48f : size * 0.34f;
        canvas.drawLine(x + fx * size * 0.12f, y + size * 0.05f, x + fx * reach, y - size * 0.04f, paint);
    }

    private String soldierSprite(Soldier soldier) {
        if (soldier.isEngineer) {
            return "guard_engineer";
        }
        if (soldier.owner != null) {
            if (soldier.owner.type == DEPLOY_GUN) {
                return "guard_gun";
            }
            if (soldier.owner.type == DEPLOY_SNIPER) {
                return "guard_sniper";
            }
        }
        return "guard_rifle";
    }

    private String enemySprite(Enemy enemy) {
        if (enemy.type == ENEMY_SCOUT) {
            return "enemy_scout";
        }
        if (enemy.type == ENEMY_BRUTE) {
            return "enemy_brute";
        }
        if (enemy.type == ENEMY_INFILTRATOR) {
            return "enemy_infiltrator";
        }
        return "enemy_raider";
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
        final String label;

        Slot(float x, float y, int lane, String label) {
            this.x = x;
            this.y = y;
            this.lane = lane;
            this.label = label;
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
        boolean revealed;
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
        boolean revealed;
        boolean isEngineer;
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
        float maxHp;
        float hp;
        float speed;
        float radius;
        float facing;
        float animTime;
        float stateTime;
        float attackCooldown;
        float attackTime;
        float hitFlash;
        float burn;
        float scanned;
        float routeBend;
        boolean mortar;
        boolean infiltrator;
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
        boolean bunkerHit;
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
