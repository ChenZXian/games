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
    public static final int BUILD_ARCHER = 0;
    public static final int BUILD_BARRACKS = 1;
    public static final int BUILD_CANNON = 2;
    public static final int BUILD_MAGE = 3;
    public static final int BUILD_SHRINE = 4;

    private static final int COMMAND_NONE = 0;
    private static final int COMMAND_HERO = 1;
    private static final int COMMAND_REINFORCE = 2;
    private static final int COMMAND_SPELL = 3;

    private static final int ENEMY_RAIDER = 0;
    private static final int ENEMY_RUNNER = 1;
    private static final int ENEMY_SHIELD = 2;
    private static final int ENEMY_WOLF = 3;
    private static final int ENEMY_SHAMAN = 4;
    private static final int ENEMY_OGRE = 5;
    private static final int ENEMY_BAT = 6;
    private static final int ENEMY_RAM = 7;
    private static final int ENEMY_WARLORD = 8;

    private static final int STATE_IDLE = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_ATTACK = 2;
    private static final int STATE_HIT = 3;
    private static final int STATE_DYING = 4;

    public interface GameListener {
        void onHudChanged(int lives, int gold, int wave, int maxWave, int escaped, String status);
        void onRunEnded(boolean cleared, int wave, int escaped, int stars, int lives);
        void onAudioEvent(String key);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF rect = new RectF();
    private final RectF dstRect = new RectF();
    private final Path drawPath = new Path();
    private final Random random = new Random(43);
    private final HashMap<String, Bitmap> sprites = new HashMap<>();
    private final ArrayList<Tower> towers = new ArrayList<>();
    private final ArrayList<Unit> units = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final Node[] nodes = new Node[]{
            new Node(0.13f, 0.18f, "Forest"),
            new Node(0.27f, 0.25f, "Pines"),
            new Node(0.42f, 0.41f, "Market"),
            new Node(0.96f, 0.34f, "East"),
            new Node(0.75f, 0.41f, "Bridge"),
            new Node(0.54f, 0.34f, "Hill Top"),
            new Node(0.65f, 0.22f, "Crest"),
            new Node(0.74f, 0.55f, "Rejoin"),
            new Node(0.53f, 0.52f, "Hill Low"),
            new Node(0.58f, 0.70f, "River"),
            new Node(0.31f, 0.72f, "Gate Road"),
            new Node(0.12f, 0.62f, "Bastion")
    };
    private final int[][] routes = new int[][]{
            new int[]{0, 1, 2, 5, 6, 7, 10, 11},
            new int[]{3, 4, 2, 5, 6, 7, 10, 11},
            new int[]{3, 4, 2, 8, 9, 10, 11},
            new int[]{0, 1, 2, 8, 9, 10, 11},
            new int[]{3, 4, 7, 11}
    };
    private final BuildPad[] pads = new BuildPad[]{
            new BuildPad(0.28f, 0.16f, 1),
            new BuildPad(0.37f, 0.31f, 2),
            new BuildPad(0.51f, 0.25f, 5),
            new BuildPad(0.67f, 0.31f, 6),
            new BuildPad(0.82f, 0.50f, 4),
            new BuildPad(0.56f, 0.63f, 9),
            new BuildPad(0.36f, 0.67f, 10),
            new BuildPad(0.22f, 0.55f, 11),
            new BuildPad(0.45f, 0.58f, 8)
    };
    private GameListener listener;
    private GameState state = GameState.MENU;
    private Bitmap unitSheet;
    private boolean running;
    private boolean soundEnabled = true;
    private boolean spritesLoaded;
    private long lastTick;
    private int lives = 20;
    private int gold = 210;
    private int wave;
    private int maxWave = 10;
    private int escaped;
    private int buildMode = BUILD_ARCHER;
    private int commandMode = COMMAND_NONE;
    private int spawnRemaining;
    private float spawnTimer;
    private float hudTimer;
    private float spellCooldown;
    private float reinforceCooldown;
    private float heroRespawn;
    private boolean waveActive;
    private String statusText = "Plan Defense";
    private final Unit hero = new Unit();

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
        resetHero();
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
        towers.clear();
        units.clear();
        enemies.clear();
        projectiles.clear();
        effects.clear();
        waveActive = false;
        spawnRemaining = 0;
        buildMode = BUILD_ARCHER;
        commandMode = COMMAND_NONE;
        statusText = "Plan Defense";
        resetHero();
        notifyHud();
        invalidate();
    }

    public void startGame() {
        lives = 20;
        gold = 210;
        wave = 0;
        escaped = 0;
        spawnRemaining = 0;
        spawnTimer = 0f;
        hudTimer = 0f;
        spellCooldown = 0f;
        reinforceCooldown = 0f;
        heroRespawn = 0f;
        buildMode = BUILD_ARCHER;
        commandMode = COMMAND_NONE;
        waveActive = false;
        statusText = "Plan Defense";
        towers.clear();
        units.clear();
        enemies.clear();
        projectiles.clear();
        effects.clear();
        resetHero();
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
        commandMode = COMMAND_NONE;
        buildMode = mode;
        statusText = "Build " + towerName(mode);
        notifyHud();
    }

    public void beginHeroCommand() {
        if (state != GameState.PLAYING) {
            return;
        }
        commandMode = COMMAND_HERO;
        statusText = "Tap Road Node For Hero";
        notifyHud();
    }

    public void beginReinforceCommand() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (reinforceCooldown > 0f || gold < 35) {
            statusText = "Guard Not Ready";
            notifyHud();
            return;
        }
        commandMode = COMMAND_REINFORCE;
        statusText = "Tap Road Node For Guard";
        notifyHud();
    }

    public void beginSpellCommand() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (spellCooldown > 0f || gold < 55) {
            statusText = "Meteor Not Ready";
            notifyHud();
            return;
        }
        commandMode = COMMAND_SPELL;
        statusText = "Tap Cluster For Meteor";
        notifyHud();
    }

    public void startNextWave() {
        if (state != GameState.PLAYING || waveActive || wave >= maxWave) {
            return;
        }
        wave++;
        spawnRemaining = 8 + wave * 3;
        spawnTimer = 0f;
        waveActive = true;
        gold += 12 + wave * 2;
        commandMode = COMMAND_NONE;
        statusText = nextPreview();
        audioEvent("wave_start");
        if (wave >= maxWave - 1 || wave % 4 == 0) {
            audioEvent("warning_horn");
        }
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
        if (commandMode == COMMAND_HERO) {
            moveHeroTo(nx, ny);
            return true;
        }
        if (commandMode == COMMAND_REINFORCE) {
            summonGuard(nx, ny);
            return true;
        }
        if (commandMode == COMMAND_SPELL) {
            castMeteor(nx, ny);
            return true;
        }
        BuildPad pad = findPad(nx, ny);
        if (pad != null) {
            Tower tower = findTower(pad);
            if (tower == null) {
                buildTower(pad);
            } else {
                upgradeTower(tower);
            }
            return true;
        }
        Node node = nearestNode(nx, ny);
        if (distance(nx, ny, node.x, node.y) < 0.08f) {
            statusText = "Node " + node.name;
            notifyHud();
        } else {
            statusText = "Tap Build Pad Or Skill";
            notifyHud();
        }
        return true;
    }

    private void update(float dt) {
        hudTimer -= dt;
        if (spellCooldown > 0f) {
            spellCooldown -= dt;
        }
        if (reinforceCooldown > 0f) {
            reinforceCooldown -= dt;
        }
        updateWave(dt);
        updateTowers(dt);
        updateUnits(dt);
        updateHero(dt);
        updateEnemies(dt);
        updateProjectiles(dt);
        updateEffects(dt);
        if (waveActive && spawnRemaining <= 0 && enemies.isEmpty()) {
            waveActive = false;
            gold += 30 + wave * 5;
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
            spawnTimer = Math.max(0.38f, 1.03f - wave * 0.055f);
        }
    }

    private void spawnEnemy() {
        int type = chooseEnemyType();
        int routeIndex = chooseRoute(type);
        Enemy enemy = new Enemy();
        enemy.type = type;
        enemy.route = routes[routeIndex];
        enemy.segment = 0;
        Node start = nodes[enemy.route[0]];
        enemy.x = start.x;
        enemy.y = start.y;
        enemy.state = STATE_MOVE;
        enemy.radius = 0.029f;
        enemy.maxHp = 42f + wave * 10f;
        enemy.speed = 0.062f + wave * 0.003f;
        enemy.damage = 1;
        enemy.reward = 9 + wave;
        enemy.attackTime = 0.82f;
        if (type == ENEMY_RUNNER) {
            enemy.maxHp *= 0.68f;
            enemy.speed *= 1.62f;
            enemy.reward += 3;
            enemy.radius = 0.025f;
        } else if (type == ENEMY_SHIELD) {
            enemy.maxHp *= 1.45f;
            enemy.speed *= 0.86f;
            enemy.armor = 0.35f;
            enemy.reward += 5;
        } else if (type == ENEMY_WOLF) {
            enemy.maxHp *= 1.05f;
            enemy.speed *= 1.32f;
            enemy.damage = 2;
            enemy.reward += 6;
            enemy.radius = 0.031f;
        } else if (type == ENEMY_SHAMAN) {
            enemy.maxHp *= 1.22f;
            enemy.speed *= 0.88f;
            enemy.healer = true;
            enemy.reward += 10;
        } else if (type == ENEMY_OGRE) {
            enemy.maxHp *= 2.45f;
            enemy.speed *= 0.62f;
            enemy.damage = 3;
            enemy.reward += 15;
            enemy.radius = 0.045f;
        } else if (type == ENEMY_BAT) {
            enemy.maxHp *= 0.78f;
            enemy.speed *= 1.38f;
            enemy.flying = true;
            enemy.reward += 7;
            enemy.radius = 0.026f;
        } else if (type == ENEMY_RAM) {
            enemy.maxHp *= 3.2f;
            enemy.speed *= 0.50f;
            enemy.damage = 5;
            enemy.siege = true;
            enemy.reward += 26;
            enemy.radius = 0.054f;
            enemy.attackTime = 1.18f;
        } else if (type == ENEMY_WARLORD) {
            enemy.maxHp *= 6.4f;
            enemy.speed *= 0.48f;
            enemy.damage = 7;
            enemy.siege = true;
            enemy.reward += 80;
            enemy.radius = 0.064f;
            enemy.attackTime = 0.95f;
        }
        enemy.hp = enemy.maxHp;
        enemies.add(enemy);
    }

    private int chooseEnemyType() {
        if (wave == maxWave && spawnRemaining == 1) {
            return ENEMY_WARLORD;
        }
        if (wave >= 8 && spawnRemaining % 7 == 0) {
            return ENEMY_RAM;
        }
        if (wave >= 7 && spawnRemaining % 6 == 0) {
            return ENEMY_BAT;
        }
        if (wave >= 6 && spawnRemaining % 5 == 0) {
            return ENEMY_SHAMAN;
        }
        if (wave >= 5 && spawnRemaining % 8 == 0) {
            return ENEMY_OGRE;
        }
        if (wave >= 4 && spawnRemaining % 4 == 0) {
            return ENEMY_SHIELD;
        }
        if (wave >= 3 && spawnRemaining % 5 == 0) {
            return ENEMY_WOLF;
        }
        if (wave >= 2 && spawnRemaining % 3 == 0) {
            return ENEMY_RUNNER;
        }
        return ENEMY_RAIDER;
    }

    private int chooseRoute(int type) {
        if (type == ENEMY_BAT) {
            return 4;
        }
        int selector = Math.abs(wave + spawnRemaining + random.nextInt(3)) % 4;
        return selector;
    }

    private void updateTowers(float dt) {
        for (int i = 0; i < towers.size(); i++) {
            Tower tower = towers.get(i);
            tower.animTime += dt;
            tower.cooldown -= dt * shrineMultiplier(tower);
            if (tower.flash > 0f) {
                tower.flash -= dt;
            }
            if (tower.type == BUILD_BARRACKS) {
                reinforceBarracks(tower, dt);
                continue;
            }
            if (tower.type == BUILD_SHRINE) {
                if (tower.cooldown <= 0f) {
                    tower.cooldown = 1.15f;
                    effects.add(new Effect(tower.pad.x, tower.pad.y, 0.42f, 0.20f + tower.level * 0.025f, color(R.color.cst_accent), 1));
                }
                continue;
            }
            Enemy target = findTowerTarget(tower);
            if (target != null && tower.cooldown <= 0f) {
                fireTower(tower, target);
                tower.cooldown = towerCooldown(tower);
                tower.flash = 0.16f;
            }
        }
    }

    private void reinforceBarracks(Tower tower, float dt) {
        int count = 0;
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).owner == tower) {
                count++;
            }
        }
        int max = Math.min(3, tower.level + 1);
        if (count >= max) {
            return;
        }
        tower.cooldown -= dt;
        if (tower.cooldown <= 0f) {
            Unit unit = createUnit(tower.pad.rallyNode, tower.level, false);
            unit.owner = tower;
            unit.x = tower.pad.x;
            unit.y = tower.pad.y;
            unit.targetX = nodes[tower.pad.rallyNode].x;
            unit.targetY = nodes[tower.pad.rallyNode].y;
            units.add(unit);
            tower.cooldown = 3.8f;
            audioEvent("reinforcement_summon");
        }
    }

    private void updateUnits(float dt) {
        for (int i = units.size() - 1; i >= 0; i--) {
            Unit unit = units.get(i);
            unit.animTime += dt;
            if (unit.hitFlash > 0f) {
                unit.hitFlash -= dt;
            }
            if (unit.temporary) {
                unit.duration -= dt;
                if (unit.duration <= 0f) {
                    effects.add(new Effect(unit.x, unit.y, 0.32f, 0.11f, color(R.color.cst_accent), 1));
                    units.remove(i);
                    continue;
                }
            }
            if (unit.hp <= 0f) {
                effects.add(new Effect(unit.x, unit.y, 0.35f, 0.12f, color(R.color.cst_danger), 1));
                units.remove(i);
                continue;
            }
            Enemy target = findUnitTarget(unit);
            if (target != null) {
                unit.state = STATE_ATTACK;
                unit.facingAngle = angleTo(unit.x, unit.y, target.x, target.y);
                unit.attackCooldown -= dt;
                if (unit.attackCooldown <= 0f) {
                    damageEnemy(target, 7f + unit.level * 4f, false);
                    unit.attackCooldown = 0.58f;
                    unit.hitFlash = 0.12f;
                    audioEvent("soldier_block");
                }
            } else {
                moveUnit(unit, dt);
            }
        }
    }

    private void updateHero(float dt) {
        if (hero.hp <= 0f) {
            heroRespawn -= dt;
            if (heroRespawn <= 0f) {
                resetHero();
                effects.add(new Effect(hero.x, hero.y, 0.50f, 0.18f, color(R.color.cst_success), 1));
            }
            return;
        }
        hero.animTime += dt;
        if (hero.hitFlash > 0f) {
            hero.hitFlash -= dt;
        }
        Enemy target = findHeroTarget();
        if (target != null) {
            hero.state = STATE_ATTACK;
            hero.facingAngle = angleTo(hero.x, hero.y, target.x, target.y);
            hero.attackCooldown -= dt;
            if (hero.attackCooldown <= 0f) {
                damageEnemy(target, 18f + wave * 0.8f, false);
                hero.attackCooldown = 0.50f;
                hero.hitFlash = 0.14f;
                audioEvent("hero_attack");
            }
        } else {
            moveUnit(hero, dt);
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
            if (enemy.slow > 0f) {
                enemy.slow -= dt;
            }
            if (enemy.hp <= 0f && enemy.state != STATE_DYING) {
                enemy.state = STATE_DYING;
                enemy.stateTime = 0f;
                effects.add(new Effect(enemy.x, enemy.y, 0.38f, enemy.radius * 2.8f, color(R.color.cst_success), 1));
                audioEvent("enemy_death");
            }
            if (enemy.state == STATE_DYING) {
                if (enemy.stateTime > 0.30f) {
                    gold += enemy.reward;
                    effects.add(new Effect(enemy.x, enemy.y, 0.25f, enemy.radius * 2.0f, color(R.color.cst_game_gold), 1));
                    enemies.remove(i);
                    audioEvent("gold_pickup");
                }
                continue;
            }
            if (enemy.healer) {
                enemy.healPulse -= dt;
                if (enemy.healPulse <= 0f) {
                    healNearby(enemy);
                    enemy.healPulse = 1.3f;
                }
            }
            Unit blocker = findBlocker(enemy);
            if (blocker != null) {
                enemy.state = STATE_ATTACK;
                enemy.facingAngle = angleTo(enemy.x, enemy.y, blocker.x, blocker.y);
                enemy.attackCooldown -= dt;
                if (enemy.attackCooldown <= 0f) {
                    blocker.hp -= enemyAttack(enemy);
                    blocker.hitFlash = 0.18f;
                    enemy.attackCooldown = enemy.attackTime;
                    effects.add(new Effect(blocker.x, blocker.y, 0.20f, 0.08f, color(R.color.cst_warning), 1));
                    audioEvent("enemy_hit");
                    if (blocker == hero && hero.hp <= 0f) {
                        heroRespawn = 7.0f;
                    }
                }
                continue;
            }
            moveEnemy(enemy, dt);
            if (enemy.segment >= enemy.route.length - 1) {
                lives -= enemy.damage;
                escaped++;
                effects.add(new Effect(enemy.x, enemy.y, 0.42f, 0.16f, color(R.color.cst_danger), 2));
                enemies.remove(i);
                if (lives <= 0) {
                    lives = 0;
                    finishRun(false);
                    return;
                }
            }
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
                applyProjectile(projectile);
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

    private void moveUnit(Unit unit, float dt) {
        float dx = unit.targetX - unit.x;
        float dy = unit.targetY - unit.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.006f) {
            float step = unit.speed * dt;
            unit.x += dx / dist * Math.min(dist, step);
            unit.y += dy / dist * Math.min(dist, step);
            unit.facingAngle = (float) Math.atan2(dy, dx);
            unit.state = STATE_MOVE;
        } else {
            unit.x = unit.targetX;
            unit.y = unit.targetY;
            unit.state = STATE_IDLE;
        }
    }

    private void moveEnemy(Enemy enemy, float dt) {
        if (enemy.segment >= enemy.route.length - 1) {
            return;
        }
        Node target = nodes[enemy.route[enemy.segment + 1]];
        float dx = target.x - enemy.x;
        float dy = target.y - enemy.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.006f) {
            enemy.segment++;
            if (enemy.segment < enemy.route.length - 1) {
                Node next = nodes[enemy.route[enemy.segment + 1]];
                enemy.facingAngle = angleTo(enemy.x, enemy.y, next.x, next.y);
            }
            return;
        }
        float speed = enemy.speed;
        if (enemy.slow > 0f) {
            speed *= 0.55f;
        }
        float step = Math.min(dist, speed * dt);
        enemy.x += dx / dist * step;
        enemy.y += dy / dist * step;
        enemy.facingAngle = (float) Math.atan2(dy, dx);
        enemy.state = STATE_MOVE;
    }

    private void fireTower(Tower tower, Enemy target) {
        Projectile projectile = new Projectile();
        projectile.target = target;
        projectile.type = tower.type;
        projectile.x = tower.pad.x;
        projectile.y = tower.pad.y;
        projectile.tx = target.x;
        projectile.ty = target.y;
        projectile.damage = towerDamage(tower);
        projectile.speed = tower.type == BUILD_CANNON ? 0.54f : 0.78f;
        projectile.splash = tower.type == BUILD_CANNON ? 0.095f + tower.level * 0.018f : 0f;
        projectile.life = 1.2f;
        projectile.angle = angleTo(projectile.x, projectile.y, projectile.tx, projectile.ty);
        tower.lastAngle = projectile.angle;
        projectiles.add(projectile);
        if (tower.type == BUILD_CANNON) {
            audioEvent("cannon_shot");
        } else if (tower.type == BUILD_MAGE) {
            audioEvent("magic_bolt");
        } else {
            audioEvent("archer_shot");
        }
    }

    private void applyProjectile(Projectile projectile) {
        if (projectile.splash > 0f) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.state != STATE_DYING && distance(projectile.tx, projectile.ty, enemy.x, enemy.y) < projectile.splash) {
                    damageEnemy(enemy, projectile.damage * 0.82f, false);
                }
            }
            effects.add(new Effect(projectile.tx, projectile.ty, 0.32f, projectile.splash, color(R.color.cst_game_fire), 2));
        } else if (projectile.target != null && projectile.target.state != STATE_DYING) {
            boolean magic = projectile.type == BUILD_MAGE;
            damageEnemy(projectile.target, projectile.damage, magic);
            if (magic) {
                projectile.target.slow = Math.max(projectile.target.slow, 0.75f);
                effects.add(new Effect(projectile.tx, projectile.ty, 0.22f, 0.065f, color(R.color.cst_game_magic), 1));
            }
        }
    }

    private void damageEnemy(Enemy enemy, float damage, boolean magic) {
        float applied = magic ? damage : damage * (1f - enemy.armor);
        enemy.hp -= applied;
        enemy.hitFlash = 0.14f;
        audioEvent("enemy_hit");
    }

    private void healNearby(Enemy healer) {
        boolean healed = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy != healer && enemy.state != STATE_DYING && distance(healer.x, healer.y, enemy.x, enemy.y) < 0.14f) {
                enemy.hp = Math.min(enemy.maxHp, enemy.hp + 10f + wave * 1.5f);
                healed = true;
            }
        }
        if (healed) {
            effects.add(new Effect(healer.x, healer.y, 0.34f, 0.13f, color(R.color.cst_success), 1));
        }
    }

    private Enemy findTowerTarget(Tower tower) {
        Enemy best = null;
        float bestProgress = -1f;
        float range = towerRange(tower);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING) {
                continue;
            }
            if (tower.type == BUILD_CANNON && enemy.flying) {
                continue;
            }
            float d = distance(tower.pad.x, tower.pad.y, enemy.x, enemy.y);
            if (d <= range) {
                float progress = enemy.segment + (1f - d);
                if (progress > bestProgress) {
                    best = enemy;
                    bestProgress = progress;
                }
            }
        }
        return best;
    }

    private Enemy findUnitTarget(Unit unit) {
        Enemy best = null;
        float bestDist = 9f;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING || enemy.flying) {
                continue;
            }
            float d = distance(unit.x, unit.y, enemy.x, enemy.y);
            if (d < 0.070f && d < bestDist) {
                best = enemy;
                bestDist = d;
            }
        }
        return best;
    }

    private Enemy findHeroTarget() {
        Enemy best = null;
        float bestDist = 9f;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state == STATE_DYING) {
                continue;
            }
            float d = distance(hero.x, hero.y, enemy.x, enemy.y);
            if (d < 0.095f && d < bestDist) {
                best = enemy;
                bestDist = d;
            }
        }
        return best;
    }

    private Unit findBlocker(Enemy enemy) {
        if (enemy.flying) {
            return null;
        }
        if (hero.hp > 0f && distance(enemy.x, enemy.y, hero.x, hero.y) < enemy.radius + hero.radius) {
            return hero;
        }
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.hp > 0f && distance(enemy.x, enemy.y, unit.x, unit.y) < enemy.radius + unit.radius) {
                return unit;
            }
        }
        return null;
    }

    private void buildTower(BuildPad pad) {
        int cost = towerCost(buildMode);
        if (gold < cost) {
            statusText = "Need Gold " + cost;
            notifyHud();
            return;
        }
        gold -= cost;
        Tower tower = new Tower();
        tower.pad = pad;
        tower.type = buildMode;
        tower.level = 1;
        tower.cooldown = 0.20f;
        towers.add(tower);
        statusText = towerName(buildMode) + " Built";
        effects.add(new Effect(pad.x, pad.y, 0.35f, 0.16f, color(R.color.cst_accent), 1));
        audioEvent("build_tower");
        notifyHud();
    }

    private void upgradeTower(Tower tower) {
        if (tower.level >= 3) {
            statusText = towerName(tower.type) + " Max";
            notifyHud();
            return;
        }
        int cost = 45 + tower.level * 35;
        if (gold < cost) {
            statusText = "Need Gold " + cost;
            notifyHud();
            return;
        }
        gold -= cost;
        tower.level++;
        tower.flash = 0.25f;
        statusText = towerName(tower.type) + " Lv" + tower.level;
        effects.add(new Effect(tower.pad.x, tower.pad.y, 0.40f, 0.17f, color(R.color.cst_warning), 1));
        audioEvent("upgrade");
        notifyHud();
    }

    private void moveHeroTo(float x, float y) {
        if (hero.hp <= 0f) {
            statusText = "Hero Recovering";
            commandMode = COMMAND_NONE;
            notifyHud();
            return;
        }
        Node node = nearestNode(x, y);
        hero.targetX = node.x;
        hero.targetY = node.y;
        commandMode = COMMAND_NONE;
        statusText = "Hero To " + node.name;
        audioEvent("hero_move");
        notifyHud();
    }

    private void summonGuard(float x, float y) {
        if (reinforceCooldown > 0f || gold < 35) {
            statusText = "Guard Not Ready";
            commandMode = COMMAND_NONE;
            notifyHud();
            return;
        }
        Node node = nearestNode(x, y);
        Unit guard = createUnit(indexOfNode(node), 2, true);
        guard.duration = 12f;
        units.add(guard);
        gold -= 35;
        reinforceCooldown = 12f;
        commandMode = COMMAND_NONE;
        statusText = "Royal Guard";
        effects.add(new Effect(guard.x, guard.y, 0.40f, 0.14f, color(R.color.cst_accent_2), 1));
        audioEvent("reinforcement_summon");
        notifyHud();
    }

    private void castMeteor(float x, float y) {
        if (spellCooldown > 0f || gold < 55) {
            statusText = "Meteor Not Ready";
            commandMode = COMMAND_NONE;
            notifyHud();
            return;
        }
        boolean hit = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.state != STATE_DYING && distance(x, y, enemy.x, enemy.y) < 0.16f) {
                damageEnemy(enemy, 52f + wave * 4f, true);
                enemy.slow = Math.max(enemy.slow, 1.1f);
                hit = true;
            }
        }
        if (!hit) {
            statusText = "No Meteor Target";
            commandMode = COMMAND_NONE;
            notifyHud();
            return;
        }
        gold -= 55;
        spellCooldown = 13f;
        commandMode = COMMAND_NONE;
        statusText = "Meteor Strike";
        effects.add(new Effect(x, y, 0.56f, 0.18f, color(R.color.cst_game_fire), 3));
        audioEvent("spell_cast");
        notifyHud();
    }

    private Unit createUnit(int nodeIndex, int level, boolean temporary) {
        Node node = nodes[nodeIndex];
        Unit unit = new Unit();
        unit.x = node.x;
        unit.y = node.y;
        unit.targetX = node.x;
        unit.targetY = node.y;
        unit.level = level;
        unit.maxHp = temporary ? 82f : 64f + level * 18f;
        unit.hp = unit.maxHp;
        unit.radius = temporary ? 0.045f : 0.040f;
        unit.speed = temporary ? 0.30f : 0.25f;
        unit.state = STATE_IDLE;
        unit.temporary = temporary;
        unit.facingAngle = 0f;
        return unit;
    }

    private void resetHero() {
        hero.x = nodes[10].x;
        hero.y = nodes[10].y;
        hero.targetX = hero.x;
        hero.targetY = hero.y;
        hero.level = 2;
        hero.maxHp = 170f;
        hero.hp = hero.maxHp;
        hero.radius = 0.050f;
        hero.speed = 0.31f;
        hero.state = STATE_IDLE;
        hero.facingAngle = 0f;
        hero.temporary = false;
        hero.owner = null;
    }

    private BuildPad findPad(float x, float y) {
        for (int i = 0; i < pads.length; i++) {
            if (distance(x, y, pads[i].x, pads[i].y) < 0.060f) {
                return pads[i];
            }
        }
        return null;
    }

    private Tower findTower(BuildPad pad) {
        for (int i = 0; i < towers.size(); i++) {
            if (towers.get(i).pad == pad) {
                return towers.get(i);
            }
        }
        return null;
    }

    private Node nearestNode(float x, float y) {
        int best = 0;
        float bestDist = distance(x, y, nodes[0].x, nodes[0].y);
        for (int i = 1; i < nodes.length; i++) {
            float d = distance(x, y, nodes[i].x, nodes[i].y);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return nodes[best];
    }

    private int indexOfNode(Node node) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == node) {
                return i;
            }
        }
        return 10;
    }

    private float shrineMultiplier(Tower tower) {
        float boost = 1f;
        for (int i = 0; i < towers.size(); i++) {
            Tower other = towers.get(i);
            if (other.type == BUILD_SHRINE && other != tower && distance(tower.pad.x, tower.pad.y, other.pad.x, other.pad.y) < 0.22f) {
                boost += 0.12f * other.level;
            }
        }
        return boost;
    }

    private int towerCost(int type) {
        if (type == BUILD_BARRACKS) {
            return 75;
        }
        if (type == BUILD_CANNON) {
            return 95;
        }
        if (type == BUILD_MAGE) {
            return 90;
        }
        if (type == BUILD_SHRINE) {
            return 80;
        }
        return 60;
    }

    private String towerName(int type) {
        if (type == BUILD_BARRACKS) {
            return "Barracks";
        }
        if (type == BUILD_CANNON) {
            return "Cannon";
        }
        if (type == BUILD_MAGE) {
            return "Mage";
        }
        if (type == BUILD_SHRINE) {
            return "Shrine";
        }
        return "Archer";
    }

    private float towerRange(Tower tower) {
        if (tower.type == BUILD_CANNON) {
            return 0.20f + tower.level * 0.018f;
        }
        if (tower.type == BUILD_MAGE) {
            return 0.24f + tower.level * 0.020f;
        }
        return 0.22f + tower.level * 0.022f;
    }

    private float towerDamage(Tower tower) {
        if (tower.type == BUILD_CANNON) {
            return 31f + tower.level * 14f;
        }
        if (tower.type == BUILD_MAGE) {
            return 24f + tower.level * 10f;
        }
        return 14f + tower.level * 7f;
    }

    private float towerCooldown(Tower tower) {
        if (tower.type == BUILD_CANNON) {
            return 1.55f;
        }
        if (tower.type == BUILD_MAGE) {
            return 0.96f;
        }
        return 0.48f;
    }

    private float enemyAttack(Enemy enemy) {
        if (enemy.type == ENEMY_WARLORD) {
            return 28f;
        }
        if (enemy.type == ENEMY_RAM) {
            return 22f;
        }
        if (enemy.type == ENEMY_OGRE) {
            return 16f;
        }
        return 7f + wave * 0.7f;
    }

    private String nextPreview() {
        if (wave >= maxWave) {
            return "Warlord March";
        }
        if (wave >= 8) {
            return "Rams On Split Road";
        }
        if (wave >= 6) {
            return "Bats And Shamans";
        }
        if (wave >= 4) {
            return "Ogres And Shields";
        }
        if (wave >= 2) {
            return "Runners At Merge";
        }
        return "Raiders Incoming";
    }

    private void finishRun(boolean cleared) {
        state = cleared ? GameState.STAGE_CLEAR : GameState.GAME_OVER;
        waveActive = false;
        commandMode = COMMAND_NONE;
        statusText = cleared ? "Stage Clear" : "Bastion Breached";
        notifyHud();
        if (listener != null) {
            listener.onRunEnded(cleared, wave, escaped, calculateStars(cleared), lives);
        }
    }

    private int calculateStars(boolean cleared) {
        if (!cleared) {
            return 0;
        }
        int stars = 1;
        if (lives >= 12) {
            stars++;
        }
        if (escaped == 0) {
            stars++;
        }
        return stars;
    }

    private void notifyHud() {
        if (listener != null) {
            listener.onHudChanged(lives, gold, wave, maxWave, escaped, statusText);
        }
    }

    private void audioEvent(String key) {
        if (listener != null && soundEnabled) {
            listener.onAudioEvent(key);
        }
    }

    private float angleTo(float ax, float ay, float bx, float by) {
        return (float) Math.atan2(by - ay, bx - ax);
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
        loadSprite("cannon", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile245.png");
        loadSprite("mage", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile250.png");
        loadSprite("shrine", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile247.png");
        loadSprite("platform", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile181.png");
        loadSprite("projectile", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile251.png");
        loadSprite("ram", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile268.png");
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
        drawMap(canvas);
        drawRoutes(canvas);
        drawPads(canvas);
        drawTowers(canvas);
        drawUnits(canvas);
        drawHero(canvas);
        drawEnemies(canvas);
        drawProjectiles(canvas);
        drawEffects(canvas);
        drawNodeMarkers(canvas);
        drawCommandHint(canvas);
        if (state == GameState.PLAYING && !waveActive) {
            drawPlanningText(canvas);
        }
    }

    private void drawMap(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        canvas.drawColor(color(R.color.cst_bg_main));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_grass));
        rect.set(w * 0.02f, h * 0.12f, w * 0.98f, h * 0.88f);
        canvas.drawRoundRect(rect, h * 0.035f, h * 0.035f, paint);
        paint.setColor(color(R.color.cst_game_grass_dark));
        for (int i = 0; i < 18; i++) {
            float x = w * (0.08f + ((i * 37) % 86) / 100f);
            float y = h * (0.17f + ((i * 23) % 67) / 100f);
            canvas.drawCircle(x, y, h * (0.012f + (i % 3) * 0.004f), paint);
        }
        paint.setColor(color(R.color.cst_panel_header_bg));
        rect.set(w * 0.36f, h * 0.35f, w * 0.49f, h * 0.48f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setColor(color(R.color.cst_game_stone));
        rect.set(w * 0.08f, h * 0.52f, w * 0.18f, h * 0.75f);
        canvas.drawRoundRect(rect, h * 0.018f, h * 0.018f, paint);
        paint.setColor(color(R.color.cst_accent_2));
        rect.set(w * 0.10f, h * 0.57f, w * 0.17f, h * 0.73f);
        canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        paint.setColor(color(R.color.cst_warning));
        drawPath.reset();
        drawPath.moveTo(w * 0.14f, h * 0.50f);
        drawPath.lineTo(w * 0.11f, h * 0.42f);
        drawPath.lineTo(w * 0.17f, h * 0.42f);
        drawPath.close();
        canvas.drawPath(drawPath, paint);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(w * 0.60f, h * 0.17f, w * 0.71f, h * 0.30f);
        canvas.drawOval(rect, paint);
    }

    private void drawRoutes(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < 4; i++) {
            drawRoute(canvas, routes[i], w, h, color(R.color.cst_game_road_edge), h * 0.078f);
        }
        for (int i = 0; i < 4; i++) {
            drawRoute(canvas, routes[i], w, h, color(R.color.cst_game_road), h * 0.054f);
        }
        drawRoute(canvas, routes[4], w, h, Color.argb(90, 123, 97, 255), h * 0.022f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.022f);
        paint.setColor(color(R.color.cst_text_primary));
        canvas.drawText("Merge", nodes[2].x * w, nodes[2].y * h - h * 0.055f, paint);
        canvas.drawText("Split", nodes[5].x * w, nodes[5].y * h - h * 0.045f, paint);
        canvas.drawText("Rejoin", nodes[7].x * w, nodes[7].y * h + h * 0.060f, paint);
    }

    private void drawRoute(Canvas canvas, int[] route, int w, int h, int routeColor, float stroke) {
        drawPath.reset();
        Node first = nodes[route[0]];
        drawPath.moveTo(first.x * w, first.y * h);
        for (int i = 1; i < route.length; i++) {
            Node node = nodes[route[i]];
            drawPath.lineTo(node.x * w, node.y * h);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(routeColor);
        canvas.drawPath(drawPath, paint);
    }

    private void drawPads(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < pads.length; i++) {
            BuildPad pad = pads[i];
            float x = pad.x * w;
            float y = pad.y * h;
            boolean occupied = findTower(pad) != null;
            drawSprite(canvas, "platform", x, y, h * 0.105f, h * 0.105f, 0f, false);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.005f);
            paint.setColor(occupied ? color(R.color.cst_accent) : color(R.color.cst_warning));
            rect.set(x - h * 0.043f, y - h * 0.043f, x + h * 0.043f, y + h * 0.043f);
            canvas.drawRoundRect(rect, h * 0.014f, h * 0.014f, paint);
        }
    }

    private void drawTowers(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < towers.size(); i++) {
            Tower tower = towers.get(i);
            float x = tower.pad.x * w;
            float y = tower.pad.y * h;
            float pulse = 1f + (float) Math.sin(tower.animTime * 5f) * (tower.type == BUILD_SHRINE ? 0.08f : 0.025f);
            if (tower.type == BUILD_BARRACKS) {
                drawBarracks(canvas, x, y, h, tower);
            } else {
                drawSprite(canvas, towerSprite(tower.type), x, y - h * 0.006f, h * 0.108f * pulse, h * 0.108f * pulse, tower.lastAngle, false);
            }
            if (tower.flash > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.007f);
                paint.setColor(color(R.color.cst_warning));
                canvas.drawCircle(x, y, h * (0.055f + tower.flash * 0.08f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.025f);
            paint.setColor(color(R.color.cst_text_on_primary));
            canvas.drawText(String.valueOf(tower.level), x, y + h * 0.058f, paint);
        }
    }

    private void drawBarracks(Canvas canvas, float x, float y, int h, Tower tower) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_panel_stroke));
        rect.set(x - h * 0.042f, y - h * 0.030f, x + h * 0.042f, y + h * 0.036f);
        canvas.drawRoundRect(rect, h * 0.010f, h * 0.010f, paint);
        paint.setColor(color(R.color.cst_accent_2));
        rect.set(x - h * 0.030f, y - h * 0.050f, x + h * 0.030f, y - h * 0.018f);
        canvas.drawRect(rect, paint);
        drawSheetTile(canvas, 52, 16, x, y - h * 0.012f, h * 0.050f, tower.animTime, 0f, STATE_IDLE);
    }

    private String towerSprite(int type) {
        if (type == BUILD_CANNON) {
            return "cannon";
        }
        if (type == BUILD_MAGE) {
            return "mage";
        }
        if (type == BUILD_SHRINE) {
            return "shrine";
        }
        return "archer";
    }

    private void drawUnits(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            float x = unit.x * w;
            float y = unit.y * h;
            float bob = unit.state == STATE_MOVE ? (float) Math.sin(unit.animTime * 14f) * h * 0.006f : 0f;
            int col = unit.temporary ? 53 : 52;
            int row = unit.temporary ? 16 : 16;
            drawSheetTile(canvas, col, row, x, y + bob, h * (unit.temporary ? 0.086f : 0.074f), unit.animTime, unit.facingAngle, unit.state);
            drawFacingMark(canvas, x, y + bob, h * 0.072f, unit.facingAngle, color(R.color.cst_accent_2), unit.state);
            drawHpBar(canvas, x, y - h * 0.052f, h * 0.055f, unit.hp, unit.maxHp);
            if (unit.hitFlash > 0f) {
                drawHitFlash(canvas, x, y, h * 0.045f);
            }
        }
    }

    private void drawHero(Canvas canvas) {
        if (hero.hp <= 0f) {
            return;
        }
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        float x = hero.x * w;
        float y = hero.y * h;
        float bob = hero.state == STATE_MOVE ? (float) Math.sin(hero.animTime * 15f) * h * 0.008f : 0f;
        drawSheetTile(canvas, 52, 16, x, y + bob, h * 0.095f, hero.animTime, hero.facingAngle, hero.state);
        drawFacingMark(canvas, x, y + bob, h * 0.090f, hero.facingAngle, color(R.color.cst_warning), hero.state);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_gold));
        drawPath.reset();
        drawPath.moveTo(x - h * 0.020f, y - h * 0.060f + bob);
        drawPath.lineTo(x - h * 0.006f, y - h * 0.082f + bob);
        drawPath.lineTo(x + h * 0.008f, y - h * 0.060f + bob);
        drawPath.lineTo(x + h * 0.022f, y - h * 0.082f + bob);
        drawPath.lineTo(x + h * 0.030f, y - h * 0.055f + bob);
        drawPath.close();
        canvas.drawPath(drawPath, paint);
        drawHpBar(canvas, x, y - h * 0.074f, h * 0.070f, hero.hp, hero.maxHp);
        if (hero.hitFlash > 0f) {
            drawHitFlash(canvas, x, y, h * 0.055f);
        }
    }

    private void drawEnemies(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            float x = enemy.x * w;
            float y = enemy.y * h;
            float bob = enemy.state == STATE_MOVE ? (float) Math.sin(enemy.animTime * enemy.speed * 160f) * h * 0.009f : 0f;
            float shake = enemy.hitFlash > 0f ? (float) Math.sin(enemy.animTime * 50f) * h * 0.006f : 0f;
            float size = h * enemy.radius * 2.35f;
            if (enemy.type == ENEMY_RAM) {
                drawSprite(canvas, "ram", x + shake, y + bob, size * 1.45f, size * 1.45f, enemy.facingAngle, false);
                drawFacingMark(canvas, x + shake, y + bob, size, enemy.facingAngle, color(R.color.cst_danger), enemy.state);
            } else {
                int col = enemyCol(enemy.type);
                int row = enemyRow(enemy.type);
                drawSheetTile(canvas, col, row, x + shake, y + bob, size, enemy.animTime, enemy.facingAngle, enemy.state);
                drawFacingMark(canvas, x + shake, y + bob, size, enemy.facingAngle, enemyColor(enemy.type), enemy.state);
                if (enemy.type == ENEMY_WARLORD) {
                    drawWarlordCrest(canvas, x + shake, y + bob, size);
                }
                if (enemy.type == ENEMY_BAT) {
                    drawBatWings(canvas, x + shake, y + bob, size, enemy.animTime);
                }
            }
            if (enemy.healer) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.004f);
                paint.setColor(color(R.color.cst_success));
                canvas.drawCircle(x, y + bob, size * 0.46f, paint);
            }
            drawHpBar(canvas, x, y - size * 0.58f, size * 0.66f, enemy.hp, enemy.maxHp);
        }
    }

    private int enemyCol(int type) {
        if (type == ENEMY_RUNNER) {
            return 52;
        }
        if (type == ENEMY_SHIELD) {
            return 51;
        }
        if (type == ENEMY_WOLF) {
            return 49;
        }
        if (type == ENEMY_SHAMAN) {
            return 50;
        }
        if (type == ENEMY_OGRE) {
            return 51;
        }
        if (type == ENEMY_BAT) {
            return 48;
        }
        if (type == ENEMY_WARLORD) {
            return 52;
        }
        return 51;
    }

    private int enemyRow(int type) {
        if (type == ENEMY_OGRE || type == ENEMY_WARLORD) {
            return 16;
        }
        if (type == ENEMY_BAT || type == ENEMY_WOLF) {
            return 15;
        }
        return 17;
    }

    private int enemyColor(int type) {
        if (type == ENEMY_SHIELD || type == ENEMY_RAM) {
            return color(R.color.cst_game_stone);
        }
        if (type == ENEMY_SHAMAN) {
            return color(R.color.cst_success);
        }
        if (type == ENEMY_OGRE || type == ENEMY_WARLORD) {
            return color(R.color.cst_danger);
        }
        if (type == ENEMY_BAT) {
            return color(R.color.cst_game_magic);
        }
        return color(R.color.cst_warning);
    }

    private void drawProjectiles(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile projectile = projectiles.get(i);
            float x = projectile.x * w;
            float y = projectile.y * h;
            if (projectile.type == BUILD_CANNON) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_stone));
                canvas.drawCircle(x, y, h * 0.014f, paint);
            } else if (projectile.type == BUILD_MAGE) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_magic));
                canvas.drawCircle(x, y, h * 0.012f, paint);
            } else {
                drawSprite(canvas, "projectile", x, y, h * 0.046f, h * 0.046f, projectile.angle, false);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.004f);
            paint.setColor(projectile.type == BUILD_MAGE ? color(R.color.cst_game_magic) : color(R.color.cst_warning));
            canvas.drawLine(x, y, x - (float) Math.cos(projectile.angle) * h * 0.035f, y - (float) Math.sin(projectile.angle) * h * 0.035f, paint);
        }
    }

    private void drawEffects(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            float t = effect.life / effect.maxLife;
            float radius = effect.radius * h * (1f - t + 0.35f);
            if (effect.type == 2 || effect.type == 3) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (120 * t), 255, 122, 42));
                rect.set(effect.x * w - radius, effect.y * h - radius * 0.55f, effect.x * w + radius, effect.y * h + radius * 0.55f);
                canvas.drawOval(rect, paint);
            }
            if (effect.type == 3) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_fire));
                canvas.drawCircle(effect.x * w, effect.y * h - radius * 0.65f, radius * 0.22f, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(h * 0.008f * t);
            paint.setColor(effect.color);
            canvas.drawCircle(effect.x * w, effect.y * h, radius, paint);
        }
    }

    private void drawNodeMarkers(Canvas canvas) {
        if (commandMode == COMMAND_NONE) {
            return;
        }
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.004f);
        paint.setColor(commandMode == COMMAND_SPELL ? color(R.color.cst_game_fire) : color(R.color.cst_accent_2));
        for (int i = 0; i < nodes.length; i++) {
            canvas.drawCircle(nodes[i].x * w, nodes[i].y * h, h * 0.030f, paint);
        }
    }

    private void drawCommandHint(Canvas canvas) {
        if (commandMode == COMMAND_NONE) {
            return;
        }
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 255, 246, 218));
        rect.set(w * 0.34f, h * 0.13f, w * 0.66f, h * 0.20f);
        canvas.drawRoundRect(rect, h * 0.014f, h * 0.014f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.026f);
        paint.setColor(color(R.color.cst_text_primary));
        canvas.drawText(statusText, w * 0.50f, h * 0.175f, paint);
    }

    private void drawPlanningText(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.033f);
        paint.setColor(color(R.color.cst_text_primary));
        canvas.drawText("Build around bends, move the hero to nodes, then start the next wave.", w * 0.56f, h * 0.16f, paint);
        paint.setTextSize(h * 0.024f);
        paint.setColor(color(R.color.cst_text_secondary));
        canvas.drawText(nextPreview(), w * 0.56f, h * 0.205f, paint);
    }

    private boolean drawSprite(Canvas canvas, String key, float cx, float cy, float maxWidth, float maxHeight, float angle, boolean flip) {
        Bitmap bitmap = sprites.get(key);
        if (bitmap == null) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.cst_card_bg));
            rect.set(cx - maxWidth * 0.35f, cy - maxHeight * 0.35f, cx + maxWidth * 0.35f, cy + maxHeight * 0.35f);
            canvas.drawRoundRect(rect, maxHeight * 0.12f, maxHeight * 0.12f, paint);
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

    private boolean drawSheetTile(Canvas canvas, int col, int row, float cx, float cy, float size, float animTime, float angle, int unitState) {
        if (unitSheet == null) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.cst_text_secondary));
            rect.set(cx - size * 0.35f, cy - size * 0.45f, cx + size * 0.35f, cy + size * 0.45f);
            canvas.drawOval(rect, paint);
            return false;
        }
        int tile = 16;
        int margin = 1;
        int sx = col * (tile + margin);
        int sy = row * (tile + margin);
        srcRect.set(sx, sy, sx + tile, sy + tile);
        float stretch = 1f;
        if (unitState == STATE_ATTACK) {
            stretch = 1f + (float) Math.sin(animTime * 18f) * 0.10f;
        } else if (unitState == STATE_DYING) {
            stretch = 1f - Math.min(0.45f, animTime * 0.7f);
        }
        canvas.save();
        canvas.translate(cx, cy);
        if (Math.cos(angle) < 0f) {
            canvas.scale(-1f, 1f);
        }
        dstRect.set(-size * 0.5f * stretch, -size * 0.5f, size * 0.5f * stretch, size * 0.5f);
        canvas.drawBitmap(unitSheet, srcRect, dstRect, paint);
        canvas.restore();
        return true;
    }

    private void drawFacingMark(Canvas canvas, float x, float y, float size, float angle, int markColor, int unitState) {
        float cx = x + (float) Math.cos(angle) * size * 0.25f;
        float cy = y + (float) Math.sin(angle) * size * 0.25f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(markColor);
        canvas.drawCircle(cx, cy, size * 0.070f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.055f);
        paint.setColor(unitState == STATE_ATTACK ? color(R.color.cst_warning) : color(R.color.cst_text_primary));
        float reach = unitState == STATE_ATTACK ? size * 0.58f : size * 0.38f;
        canvas.drawLine(x, y, x + (float) Math.cos(angle) * reach, y + (float) Math.sin(angle) * reach, paint);
    }

    private void drawHitFlash(Canvas canvas, float x, float y, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.12f);
        paint.setColor(color(R.color.cst_warning));
        canvas.drawCircle(x, y, radius, paint);
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

    private void drawWarlordCrest(Canvas canvas, float x, float y, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_fire));
        rect.set(x - size * 0.10f, y - size * 0.58f, x + size * 0.10f, y - size * 0.40f);
        canvas.drawOval(rect, paint);
    }

    private void drawBatWings(Canvas canvas, float x, float y, float size, float animTime) {
        float wing = (float) Math.sin(animTime * 18f) * size * 0.18f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.060f);
        paint.setColor(color(R.color.cst_game_magic));
        canvas.drawLine(x - size * 0.18f, y, x - size * 0.45f, y - wing, paint);
        canvas.drawLine(x + size * 0.18f, y, x + size * 0.45f, y + wing, paint);
    }

    private static final class Node {
        final float x;
        final float y;
        final String name;

        Node(float x, float y, String name) {
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }

    private static final class BuildPad {
        final float x;
        final float y;
        final int rallyNode;

        BuildPad(float x, float y, int rallyNode) {
            this.x = x;
            this.y = y;
            this.rallyNode = rallyNode;
        }
    }

    private static final class Tower {
        BuildPad pad;
        int type;
        int level;
        float cooldown;
        float animTime;
        float flash;
        float lastAngle;
    }

    private static final class Unit {
        Tower owner;
        int level;
        int state;
        float x;
        float y;
        float targetX;
        float targetY;
        float hp;
        float maxHp;
        float radius;
        float speed;
        float facingAngle;
        float animTime;
        float attackCooldown;
        float hitFlash;
        float duration;
        boolean temporary;
    }

    private static final class Enemy {
        int type;
        int[] route;
        int segment;
        int damage;
        int reward;
        int state;
        float x;
        float y;
        float hp;
        float maxHp;
        float speed;
        float radius;
        float armor;
        float facingAngle;
        float animTime;
        float stateTime;
        float attackCooldown;
        float attackTime;
        float hitFlash;
        float slow;
        float healPulse;
        boolean flying;
        boolean healer;
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
