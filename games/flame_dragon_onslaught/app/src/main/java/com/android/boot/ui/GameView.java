package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.android.boot.audio.AudioController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface Listener {
        void onHudUpdated(HudSnapshot hud);

        void onStageFinished(StageResult result);
    }

    public static class HudSnapshot {
        public final int healthPercent;
        public final int bossPercent;
        public final boolean showBoss;
        public final String healthText;
        public final String stageTitle;
        public final String scoreText;
        public final String comboText;
        public final String timerText;
        public final String objectiveText;

        public HudSnapshot(int healthPercent, int bossPercent, boolean showBoss, String healthText, String stageTitle, String scoreText, String comboText, String timerText, String objectiveText) {
            this.healthPercent = healthPercent;
            this.bossPercent = bossPercent;
            this.showBoss = showBoss;
            this.healthText = healthText;
            this.stageTitle = stageTitle;
            this.scoreText = scoreText;
            this.comboText = comboText;
            this.timerText = timerText;
            this.objectiveText = objectiveText;
        }
    }

    public static class StageResult {
        public final boolean victory;
        public final int stageIndex;
        public final int score;
        public final int medalsEarned;
        public final int rankScore;
        public final String summary;

        public StageResult(boolean victory, int stageIndex, int score, int medalsEarned, int rankScore, String summary) {
            this.victory = victory;
            this.stageIndex = stageIndex;
            this.score = score;
            this.medalsEarned = medalsEarned;
            this.rankScore = rankScore;
            this.summary = summary;
        }
    }

    public static class StageEntry {
        public final int chapter;
        public final int stageInChapter;
        public final String chapterName;
        public final String stageName;

        public StageEntry(int chapter, int stageInChapter, String chapterName, String stageName) {
            this.chapter = chapter;
            this.stageInChapter = stageInChapter;
            this.chapterName = chapterName;
            this.stageName = stageName;
        }

        public String shortCode() {
            return String.format(Locale.US, "%d-%d", chapter, stageInChapter);
        }
    }

    public static class UpgradeProfile {
        public final int[] levels = new int[4];
        public int medals;

        public int vitalityBonus() {
            return levels[0] * 24;
        }

        public float heatGainBonus() {
            return levels[1] * 0.16f;
        }

        public int skillPowerBonus() {
            return levels[2] * 5;
        }

        public float driveBonus() {
            return levels[3] * 0.9f;
        }

        public boolean buy(int index) {
            int level = levels[index];
            int cost = 20 + level * 15;
            if (level >= 8 || medals < cost) {
                return false;
            }
            medals -= cost;
            levels[index]++;
            return true;
        }

        public void copyFrom(UpgradeProfile other) {
            medals = other.medals;
            for (int i = 0; i < levels.length; i++) {
                levels[i] = other.levels[i];
            }
        }
    }

    private static final int MODE_MENU = 0;
    private static final int MODE_PLAYING = 1;
    private static final int MODE_PAUSED = 2;
    private static final int MODE_RESULT = 3;
    private static final float WORLD_FLOOR = 520f;
    private static final float BASE_GRAVITY = 1780f;
    private static final float HUD_PUBLISH_INTERVAL = 0.07f;
    private static final List<StageDefinition> STAGES = buildStages();
    private static final List<StageEntry> STAGE_ENTRIES = buildEntries();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect tempSrc = new Rect();
    private final RectF tempDst = new RectF();
    private final SurfaceHolder holder;
    private final Random random = new Random(17);
    private final AssetManager assetManager;
    private final AudioController audioController;
    private final Player player = new Player();
    private final UpgradeProfile activeUpgradeProfile = new UpgradeProfile();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<VisualEffect> effects = new ArrayList<>();
    private final SpriteLibrary sprites;
    private Listener listener;
    private Thread loopThread;
    private volatile boolean running;
    private boolean surfaceReady;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpQueued;
    private boolean lightQueued;
    private boolean heavyQueued;
    private boolean skillQueued;
    private boolean overdriveQueued;
    private int mode = MODE_MENU;
    private int currentStageIndex = -1;
    private StageDefinition currentStage;
    private int currentEncounterIndex;
    private boolean encounterActive;
    private float encounterLeft;
    private float encounterRight;
    private float cameraX;
    private float stageTime;
    private float hudTimer;
    private int score;
    private int combo;
    private float comboTimer;
    private int medalsCollected;
    private float hitStop;
    private long lastFrameTime;
    private boolean resultSent;
    private Bitmap backgroundBitmap;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        assetManager = context.getAssets();
        audioController = new AudioController(context);
        sprites = new SpriteLibrary(assetManager);
    }

    public static List<StageEntry> getStageEntries() {
        return STAGE_ENTRIES;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        publishHud(true);
    }

    public void applyUpgradeProfile(UpgradeProfile profile) {
        activeUpgradeProfile.copyFrom(profile);
        if (currentStage == null) {
            applyUpgradeValues();
        }
    }

    public void setMuted(boolean muted) {
        audioController.setMuted(muted);
    }

    public boolean isStageActive() {
        return mode == MODE_PLAYING || mode == MODE_PAUSED;
    }

    public void onHostResume() {
        audioController.onResume();
        startLoopIfNeeded();
    }

    public void onHostPause() {
        audioController.onPause();
        stopLoopIfNeeded();
    }

    public void startStage(int stageIndex) {
        if (stageIndex < 0 || stageIndex >= STAGES.size()) {
            return;
        }
        currentStageIndex = stageIndex;
        currentStage = STAGES.get(stageIndex);
        backgroundBitmap = sprites.backgroundForChapter(currentStage.chapter);
        resetStageState();
        audioController.playStageBgm(currentStage.isBossStage);
    }

    public void stopStage() {
        currentStage = null;
        currentStageIndex = -1;
        mode = MODE_MENU;
        enemies.clear();
        projectiles.clear();
        pickups.clear();
        hazards.clear();
        effects.clear();
        audioController.playMenuBgm();
        publishHud(true);
    }

    public void pauseStage() {
        if (mode == MODE_PLAYING) {
            mode = MODE_PAUSED;
        }
    }

    public void resumeStage() {
        if (mode == MODE_PAUSED) {
            mode = MODE_PLAYING;
        }
    }

    public void setLeftPressed(boolean pressed) {
        leftPressed = pressed;
        if (pressed) {
            player.face = -1;
        }
    }

    public void setRightPressed(boolean pressed) {
        rightPressed = pressed;
        if (pressed) {
            player.face = 1;
        }
    }

    public void pressJump() {
        jumpQueued = true;
    }

    public void pressLightAttack() {
        lightQueued = true;
    }

    public void pressHeavyAttack() {
        heavyQueued = true;
    }

    public void pressFlameSkill() {
        skillQueued = true;
    }

    public void pressOverdrive() {
        overdriveQueued = true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        startLoopIfNeeded();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
        stopLoopIfNeeded();
    }

    @Override
    public void run() {
        lastFrameTime = SystemClock.elapsedRealtime();
        while (running) {
            long now = SystemClock.elapsedRealtime();
            float dt = Math.min(0.033f, (now - lastFrameTime) / 1000f);
            lastFrameTime = now;
            if (mode == MODE_PLAYING) {
                updateStage(dt);
            }
            drawFrame();
            SystemClock.sleep(8L);
        }
    }

    private void startLoopIfNeeded() {
        if (!surfaceReady || running) {
            return;
        }
        running = true;
        loopThread = new Thread(this, "flame-dragon-loop");
        loopThread.start();
        if (mode == MODE_MENU) {
            audioController.playMenuBgm();
        }
    }

    private void stopLoopIfNeeded() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join(400L);
            } catch (InterruptedException ignored) {
            }
            loopThread = null;
        }
    }

    private void resetStageState() {
        mode = MODE_PLAYING;
        player.reset();
        applyUpgradeValues();
        enemies.clear();
        projectiles.clear();
        pickups.clear();
        hazards.clear();
        effects.clear();
        stageTime = 0f;
        hudTimer = 0f;
        score = 0;
        combo = 0;
        comboTimer = 0f;
        medalsCollected = 0;
        hitStop = 0f;
        cameraX = 0f;
        currentEncounterIndex = 0;
        encounterActive = false;
        resultSent = false;
        leftPressed = false;
        rightPressed = false;
        jumpQueued = false;
        lightQueued = false;
        heavyQueued = false;
        skillQueued = false;
        overdriveQueued = false;
        seedHazards();
        publishHud(true);
    }

    private void applyUpgradeValues() {
        player.maxHp = 320 + activeUpgradeProfile.vitalityBonus();
        player.hp = player.maxHp;
        player.heatGainMultiplier = 1f + activeUpgradeProfile.heatGainBonus();
        player.skillDamageBonus = activeUpgradeProfile.skillPowerBonus();
        player.overdriveDurationBonus = activeUpgradeProfile.driveBonus();
    }

    private void seedHazards() {
        hazards.clear();
        if (currentStage == null) {
            return;
        }
        float[] positions = currentStage.hazardXs;
        for (int i = 0; i < positions.length; i++) {
            Hazard hazard = new Hazard();
            hazard.type = currentStage.hazardType;
            hazard.x = positions[i];
            hazard.width = 120f;
            hazard.height = currentStage.hazardType == 2 ? 120f : 80f;
            hazard.cooldown = 1.2f + i * 0.4f;
            hazards.add(hazard);
        }
    }

    private void updateStage(float dt) {
        if (currentStage == null) {
            return;
        }
        if (hitStop > 0f) {
            hitStop -= dt;
            drawFrame();
            return;
        }
        stageTime += dt;
        hudTimer += dt;
        comboTimer -= dt;
        if (comboTimer <= 0f) {
            combo = 0;
        }
        updatePlayer(dt);
        updateEncounters();
        updateHazards(dt);
        updateProjectiles(dt);
        updateEnemies(dt);
        updatePickups(dt);
        updateEffects(dt);
        updateCamera();
        if (player.hp <= 0 && !resultSent) {
            finishStage(false);
        }
        if (currentEncounterIndex >= currentStage.encounters.length && enemies.isEmpty() && !resultSent) {
            finishStage(true);
        }
        if (hudTimer >= HUD_PUBLISH_INTERVAL) {
            publishHud(false);
            hudTimer = 0f;
        }
    }

    private void updatePlayer(float dt) {
        player.attackTimer = Math.max(0f, player.attackTimer - dt);
        player.skillCooldown = Math.max(0f, player.skillCooldown - dt);
        player.invul = Math.max(0f, player.invul - dt);
        player.hitFlash = Math.max(0f, player.hitFlash - dt);
        player.actionPoseTimer = Math.max(0f, player.actionPoseTimer - dt);
        player.frameTime += dt;
        if (player.overdriveTimer > 0f) {
            player.overdriveTimer -= dt;
            if (player.overdriveTimer <= 0f) {
                player.overdriveTimer = 0f;
                player.heat = Math.min(player.heat, 45f);
            }
        }
        float move = 0f;
        if (leftPressed) {
            move -= 1f;
        }
        if (rightPressed) {
            move += 1f;
        }
        float moveSpeed = player.overdriveTimer > 0f ? 310f : 260f;
        if (player.attackTimer <= 0f) {
            player.vx = move * moveSpeed;
        } else {
            player.vx *= 0.86f;
        }
        if (move != 0f) {
            player.face = move < 0f ? -1 : 1;
        }
        if (jumpQueued && player.onGround) {
            player.vy = -760f;
            player.onGround = false;
            player.actionPoseTimer = 0.22f;
        }
        jumpQueued = false;
        player.vy += BASE_GRAVITY * dt;
        player.x += player.vx * dt;
        player.y += player.vy * dt;
        if (player.x < 80f) {
            player.x = 80f;
        }
        if (player.x > currentStage.worldWidth - 80f) {
            player.x = currentStage.worldWidth - 80f;
        }
        if (encounterActive) {
            if (player.x < encounterLeft + 50f) {
                player.x = encounterLeft + 50f;
            }
            if (player.x > encounterRight - 50f) {
                player.x = encounterRight - 50f;
            }
        }
        if (player.y >= WORLD_FLOOR) {
            player.y = WORLD_FLOOR;
            player.vy = 0f;
            player.onGround = true;
        } else {
            player.onGround = false;
        }
        handlePlayerAttacks();
    }

    private void handlePlayerAttacks() {
        if (lightQueued && player.attackTimer <= 0f) {
            player.comboStep = player.comboTimer > 0f ? Math.min(3, player.comboStep + 1) : 1;
            player.comboTimer = 0.55f;
            player.attackTimer = 0.18f + player.comboStep * 0.05f;
            player.actionPoseTimer = 0.17f;
            float damage = 19f + player.comboStep * 7f + player.skillDamageBonus * 0.35f;
            hitEnemies(player.x + player.face * 88f, player.y - 46f, 132f, 74f, damage, 140f + player.comboStep * 26f, false);
            combo = Math.max(combo, player.comboStep);
            audioController.playAttack();
        }
        lightQueued = false;
        if (heavyQueued && player.attackTimer <= 0f) {
            player.attackTimer = 0.38f;
            player.actionPoseTimer = 0.24f;
            player.x += player.face * 24f;
            hitEnemies(player.x + player.face * 108f, player.y - 60f, 156f, 96f, 42f + player.skillDamageBonus, 260f, true);
            player.heat = Math.min(100f, player.heat + 8f * player.heatGainMultiplier);
            audioController.playHeavy();
        }
        heavyQueued = false;
        if (skillQueued && player.attackTimer <= 0f && player.skillCooldown <= 0f && player.heat >= 28f) {
            player.attackTimer = 0.42f;
            player.actionPoseTimer = 0.28f;
            player.skillCooldown = Math.max(2.6f, 4.4f - activeUpgradeProfile.levels[2] * 0.2f);
            player.heat -= 28f;
            Projectile projectile = new Projectile();
            projectile.ownerPlayer = true;
            projectile.type = 1;
            projectile.x = player.x + player.face * 72f;
            projectile.y = player.y - 56f;
            projectile.vx = player.face * 560f;
            projectile.damage = 48f + player.skillDamageBonus * 1.6f + (player.overdriveTimer > 0f ? 18f : 0f);
            projectile.radius = 30f;
            projectile.life = 0.8f;
            projectiles.add(projectile);
            effects.add(VisualEffect.burst(player.x + player.face * 54f, player.y - 54f, 0xFFFF7A2F, 74f));
            audioController.playSkill();
        }
        skillQueued = false;
        if (overdriveQueued && player.attackTimer <= 0f && player.overdriveTimer <= 0f && player.heat >= 100f) {
            player.overdriveTimer = 7.2f + player.overdriveDurationBonus;
            player.attackTimer = 0.36f;
            player.actionPoseTimer = 0.34f;
            player.heat = 0f;
            effects.add(VisualEffect.burst(player.x, player.y - 62f, 0xFFFFD166, 110f));
            effects.add(VisualEffect.burst(player.x, player.y - 62f, 0xFF61F1FF, 80f));
            audioController.playOverdrive();
        }
        overdriveQueued = false;
    }

    private void hitEnemies(float centerX, float centerY, float width, float height, float damage, float knockForce, boolean launch) {
        RectF attackRect = new RectF(centerX - width * 0.5f, centerY - height * 0.5f, centerX + width * 0.5f, centerY + height * 0.5f);
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            if (RectF.intersects(attackRect, enemy.bounds())) {
                enemy.hp -= damage + (player.overdriveTimer > 0f ? 12f : 0f);
                enemy.vx += player.face * knockForce;
                enemy.vy = launch ? -440f : -120f;
                enemy.hitFlash = 0.12f;
                hitStop = 0.03f;
                combo = Math.min(99, combo + 1);
                comboTimer = 1.1f;
                score += 20 * combo;
                player.heat = Math.min(100f, player.heat + 7f * player.heatGainMultiplier);
                effects.add(VisualEffect.burst(enemy.x, enemy.y - 48f, 0xFFFF9E4D, enemy.isBoss ? 70f : 44f));
                audioController.playHit();
                if (enemy.hp <= 0f) {
                    enemy.dead = true;
                    score += enemy.isBoss ? 1300 : 220;
                    spawnPickups(enemy);
                    audioController.playDefeat();
                }
            }
        }
    }

    private void updateEncounters() {
        if (currentStage == null || resultSent) {
            return;
        }
        if (encounterActive) {
            boolean anyAlive = false;
            for (Enemy enemy : enemies) {
                if (!enemy.dead) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                encounterActive = false;
                currentEncounterIndex++;
                effects.add(VisualEffect.banner(player.x + 180f, player.y - 180f, 0xFF51F2A5));
                if (currentEncounterIndex < currentStage.encounters.length) {
                    objectivePulse();
                }
            }
            return;
        }
        if (currentEncounterIndex >= currentStage.encounters.length) {
            return;
        }
        Encounter encounter = currentStage.encounters[currentEncounterIndex];
        if (player.x >= encounter.triggerX) {
            encounterActive = true;
            encounterLeft = encounter.triggerX - 260f;
            encounterRight = encounter.triggerX + 360f;
            spawnEncounter(encounter);
            objectivePulse();
        }
    }

    private void objectivePulse() {
        effects.add(VisualEffect.burst(player.x + 160f, 110f, 0xFFFFCD57, 56f));
    }

    private void spawnEncounter(Encounter encounter) {
        for (EnemySpec spec : encounter.enemySpecs) {
            Enemy enemy = new Enemy();
            enemy.type = spec.type;
            enemy.x = spec.x;
            enemy.y = spec.flying ? WORLD_FLOOR - 90f : WORLD_FLOOR;
            enemy.isFlying = spec.flying;
            enemy.isBoss = spec.type == 6;
            enemy.face = -1;
            enemy.maxHp = spec.hp;
            enemy.hp = spec.hp;
            enemy.speed = spec.speed;
            enemy.attackDamage = spec.damage;
            enemy.range = spec.range;
            enemy.cooldown = spec.cooldown;
            enemy.attackInterval = spec.interval;
            enemy.gravityScale = spec.flying ? 0.2f : 1f;
            enemies.add(enemy);
        }
    }

    private void updateHazards(float dt) {
        for (Hazard hazard : hazards) {
            hazard.time += dt;
            hazard.cooldown -= dt;
            if (hazard.cooldown <= 0f) {
                hazard.cooldown = 2.2f + random.nextFloat() * 1.6f;
                hazard.activeTime = hazard.type == 2 ? 1.15f : 0.8f;
            }
            if (hazard.activeTime > 0f) {
                hazard.activeTime -= dt;
                if (Math.abs(player.x - hazard.x) < hazard.width * 0.5f && player.y >= WORLD_FLOOR - hazard.height && player.invul <= 0f) {
                    damagePlayer(10f + currentStage.chapter * 1.5f);
                }
            }
        }
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.x += projectile.vx * dt;
            projectile.y += projectile.vy * dt;
            projectile.life -= dt;
            if (projectile.type == 3) {
                projectile.vx *= 0.98f;
            }
            if (projectile.ownerPlayer) {
                for (Enemy enemy : enemies) {
                    if (enemy.dead) {
                        continue;
                    }
                    float dx = enemy.x - projectile.x;
                    float dy = (enemy.y - 44f) - projectile.y;
                    float rr = projectile.radius + enemy.radius;
                    if (dx * dx + dy * dy <= rr * rr) {
                        enemy.hp -= projectile.damage;
                        enemy.vx += player.face * 180f;
                        enemy.hitFlash = 0.14f;
                        score += enemy.isBoss ? 90 : 45;
                        player.heat = Math.min(100f, player.heat + 4f * player.heatGainMultiplier);
                        effects.add(VisualEffect.burst(projectile.x, projectile.y, 0xFFFFAA38, 52f));
                        if (enemy.hp <= 0f) {
                            enemy.dead = true;
                            spawnPickups(enemy);
                            score += enemy.isBoss ? 1000 : 200;
                            audioController.playDefeat();
                        }
                        projectile.life = 0f;
                        break;
                    }
                }
            } else {
                float dx = player.x - projectile.x;
                float dy = (player.y - 50f) - projectile.y;
                float rr = projectile.radius + 28f;
                if (dx * dx + dy * dy <= rr * rr && player.invul <= 0f) {
                    damagePlayer(projectile.damage);
                    projectile.life = 0f;
                }
            }
            if (projectile.life <= 0f || projectile.x < -120f || projectile.x > currentStage.worldWidth + 120f) {
                projectiles.remove(i);
            }
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.dead) {
                enemy.deathTimer += dt;
                enemy.vx *= 0.92f;
                enemy.x += enemy.vx * dt;
                if (enemy.deathTimer >= 0.34f) {
                    enemies.remove(i);
                }
                continue;
            }
            enemy.cooldown -= dt;
            enemy.hitFlash = Math.max(0f, enemy.hitFlash - dt);
            enemy.frameTime += dt;
            if (!enemy.isFlying) {
                enemy.vy += BASE_GRAVITY * enemy.gravityScale * dt;
                enemy.y += enemy.vy * dt;
                if (enemy.y >= WORLD_FLOOR) {
                    enemy.y = WORLD_FLOOR;
                    enemy.vy = 0f;
                }
            } else {
                enemy.y = WORLD_FLOOR - 120f + (float) Math.sin(enemy.frameTime * 3f + enemy.x * 0.01f) * 26f;
            }
            enemy.x += enemy.vx * dt;
            enemy.vx *= enemy.isBoss ? 0.94f : 0.88f;
            float dx = player.x - enemy.x;
            enemy.face = dx < 0 ? -1 : 1;
            float absDx = Math.abs(dx);
            if (enemy.type == 6) {
                updateBoss(enemy, dt, absDx);
            } else if (enemy.type == 2 || enemy.type == 3) {
                updateCaster(enemy, dt, absDx);
            } else if (enemy.type == 4) {
                updateDrake(enemy, dt, absDx);
            } else {
                updateMeleeEnemy(enemy, dt, absDx);
            }
        }
    }

    private void updateMeleeEnemy(Enemy enemy, float dt, float absDx) {
        float desired = enemy.face * enemy.speed;
        if (absDx > enemy.range) {
            enemy.vx += desired * dt * 2.3f;
        } else if (enemy.cooldown <= 0f) {
            enemy.cooldown = enemy.attackInterval;
            enemy.actionPose = 1;
            if (absDx < enemy.range + 24f && player.invul <= 0f) {
                damagePlayer(enemy.attackDamage);
            }
            enemy.vx -= enemy.face * 140f;
        }
    }

    private void updateCaster(Enemy enemy, float dt, float absDx) {
        if (absDx > enemy.range + 60f) {
            enemy.vx += enemy.face * enemy.speed * dt * 1.8f;
        } else if (enemy.cooldown <= 0f) {
            enemy.cooldown = enemy.attackInterval;
            enemy.actionPose = 1;
            Projectile projectile = new Projectile();
            projectile.ownerPlayer = false;
            projectile.type = enemy.type == 2 ? 2 : 3;
            projectile.x = enemy.x + enemy.face * 38f;
            projectile.y = enemy.y - (enemy.type == 2 ? 70f : 90f);
            projectile.vx = enemy.face * (enemy.type == 2 ? 350f : 250f);
            projectile.vy = enemy.type == 3 ? -90f : 0f;
            projectile.damage = enemy.attackDamage;
            projectile.radius = enemy.type == 2 ? 18f : 26f;
            projectile.life = enemy.type == 2 ? 1.8f : 2.4f;
            projectiles.add(projectile);
        }
    }

    private void updateDrake(Enemy enemy, float dt, float absDx) {
        float targetHeight = WORLD_FLOOR - 150f;
        enemy.y += (targetHeight - enemy.y) * dt * 2f;
        if (absDx > enemy.range) {
            enemy.vx += enemy.face * enemy.speed * dt * 1.7f;
        } else if (enemy.cooldown <= 0f) {
            enemy.cooldown = enemy.attackInterval;
            Projectile projectile = new Projectile();
            projectile.ownerPlayer = false;
            projectile.type = 2;
            projectile.x = enemy.x + enemy.face * 34f;
            projectile.y = enemy.y - 10f;
            projectile.vx = enemy.face * 330f;
            projectile.damage = enemy.attackDamage;
            projectile.radius = 22f;
            projectile.life = 1.3f;
            projectiles.add(projectile);
        }
    }

    private void updateBoss(Enemy enemy, float dt, float absDx) {
        if (enemy.hp < enemy.maxHp * 0.55f) {
            enemy.phase = 1;
        }
        if (enemy.hp < enemy.maxHp * 0.28f) {
            enemy.phase = 2;
        }
        float bossSpeed = enemy.speed + enemy.phase * 18f;
        if (absDx > enemy.range + 20f) {
            enemy.vx += enemy.face * bossSpeed * dt * 1.7f;
        } else if (enemy.cooldown <= 0f) {
            enemy.cooldown = Math.max(0.9f, enemy.attackInterval - enemy.phase * 0.2f);
            enemy.actionPose = enemy.phase == 2 ? 2 : 1;
            if (enemy.phase == 0) {
                if (player.invul <= 0f) {
                    damagePlayer(enemy.attackDamage + 8f);
                }
                enemy.vx -= enemy.face * 260f;
            } else if (enemy.phase == 1) {
                Projectile projectile = new Projectile();
                projectile.ownerPlayer = false;
                projectile.type = 2;
                projectile.x = enemy.x + enemy.face * 70f;
                projectile.y = enemy.y - 100f;
                projectile.vx = enemy.face * 380f;
                projectile.damage = enemy.attackDamage + 10f;
                projectile.radius = 28f;
                projectile.life = 1.6f;
                projectiles.add(projectile);
                Projectile projectile2 = new Projectile();
                projectile2.ownerPlayer = false;
                projectile2.type = 2;
                projectile2.x = enemy.x + enemy.face * 70f;
                projectile2.y = enemy.y - 56f;
                projectile2.vx = enemy.face * 300f;
                projectile2.damage = enemy.attackDamage + 8f;
                projectile2.radius = 24f;
                projectile2.life = 1.8f;
                projectiles.add(projectile2);
            } else {
                if (player.invul <= 0f && absDx < enemy.range + 80f) {
                    damagePlayer(enemy.attackDamage + 14f);
                }
                enemy.vy = -480f;
                enemy.vx -= enemy.face * 340f;
                effects.add(VisualEffect.burst(enemy.x, enemy.y - 56f, 0xFFFF4F5E, 96f));
            }
        }
    }

    private void updatePickups(float dt) {
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pickup = pickups.get(i);
            pickup.life -= dt;
            pickup.y += (WORLD_FLOOR - 10f - pickup.y) * dt * 4f;
            float dx = player.x - pickup.x;
            float dy = (player.y - 34f) - pickup.y;
            if (dx * dx + dy * dy < 44f * 44f) {
                if (pickup.type == 0) {
                    medalsCollected += pickup.value;
                } else if (pickup.type == 1) {
                    player.heat = Math.min(100f, player.heat + pickup.value);
                } else {
                    player.hp = Math.min(player.maxHp, player.hp + pickup.value);
                }
                audioController.playPickup();
                pickups.remove(i);
                continue;
            }
            if (pickup.life <= 0f) {
                pickups.remove(i);
            }
        }
    }

    private void updateEffects(float dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            VisualEffect effect = effects.get(i);
            effect.life -= dt;
            if (effect.life <= 0f) {
                effects.remove(i);
            }
        }
    }

    private void updateCamera() {
        float screenWorld = Math.max(1f, getWidth());
        cameraX = player.x - screenWorld * 0.4f;
        cameraX = Math.max(0f, Math.min(cameraX, currentStage.worldWidth - screenWorld));
    }

    private void damagePlayer(float amount) {
        if (player.invul > 0f) {
            return;
        }
        player.hp -= amount;
        player.invul = 0.7f;
        player.hitFlash = 0.24f;
        combo = 0;
        comboTimer = 0f;
        effects.add(VisualEffect.burst(player.x, player.y - 54f, 0xFFFF4F5E, 60f));
    }

    private void spawnPickups(Enemy enemy) {
        Pickup medal = new Pickup();
        medal.type = 0;
        medal.value = enemy.isBoss ? 12 : 3;
        medal.x = enemy.x;
        medal.y = enemy.y - 64f;
        medal.life = 7f;
        pickups.add(medal);
        if (random.nextFloat() < 0.35f || enemy.isBoss) {
            Pickup orb = new Pickup();
            orb.type = 1;
            orb.value = enemy.isBoss ? 18 : 8;
            orb.x = enemy.x + 16f;
            orb.y = enemy.y - 82f;
            orb.life = 6.5f;
            pickups.add(orb);
        }
        if (enemy.isBoss || random.nextFloat() < 0.18f) {
            Pickup heal = new Pickup();
            heal.type = 2;
            heal.value = enemy.isBoss ? 36 : 18;
            heal.x = enemy.x - 14f;
            heal.y = enemy.y - 72f;
            heal.life = 6f;
            pickups.add(heal);
        }
    }

    private void finishStage(boolean victory) {
        if (resultSent) {
            return;
        }
        resultSent = true;
        mode = MODE_RESULT;
        int scoreRank = calculateRank(victory);
        int medalReward = medalsCollected + (victory ? 18 + currentStage.chapter * 2 : 4 + currentStage.chapter);
        String summary = victory
                ? String.format(Locale.US, "%s cleared in %s with combo peak x%d.", currentStage.stageName, formatTime(stageTime), combo)
                : String.format(Locale.US, "%s fell in %s. Return stronger and keep the heat core alive.", currentStage.stageName, formatTime(stageTime));
        if (listener != null) {
            listener.onStageFinished(new StageResult(victory, currentStageIndex, score, medalReward, scoreRank, summary));
        }
        audioController.playMenuBgm();
        publishHud(true);
    }

    private int calculateRank(boolean victory) {
        if (!victory) {
            return 0;
        }
        int rank = 0;
        if (score >= 2500 + currentStage.chapter * 400) {
            rank = 1;
        }
        if (score >= 4300 + currentStage.chapter * 500) {
            rank = 2;
        }
        if (score >= 6200 + currentStage.chapter * 650 && stageTime < 110f) {
            rank = 3;
        }
        return rank;
    }

    private void publishHud(boolean force) {
        if (listener == null || currentStage == null && !force) {
            return;
        }
        String stageTitle = currentStage == null ? "Chapter Select" : String.format(Locale.US, "Chapter %d-%d  %s", currentStage.chapter, currentStage.stageInChapter, currentStage.stageName);
        String healthText = String.format(Locale.US, "%d / %d   Heat %d%%", Math.max(0, Math.round(player.hp)), player.maxHp, Math.round(player.heat));
        String scoreText = String.format(Locale.US, "Score %d", score);
        String comboText = String.format(Locale.US, "Combo x%d", combo);
        String objective = currentStage == null ? "Choose a chapter to begin" : currentStage.objective(encounterActive, currentEncounterIndex, enemies);
        Enemy boss = currentBoss();
        int bossPercent = boss == null ? 0 : Math.max(0, Math.min(100, Math.round(boss.hp / boss.maxHp * 100f)));
        int hpPercent = player.maxHp <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(player.hp / player.maxHp * 100f)));
        if (listener != null) {
            listener.onHudUpdated(new HudSnapshot(hpPercent, bossPercent, boss != null, healthText, stageTitle, scoreText, comboText, formatTime(stageTime), objective));
        }
    }

    private Enemy currentBoss() {
        for (Enemy enemy : enemies) {
            if (!enemy.dead && enemy.isBoss) {
                return enemy;
            }
        }
        return null;
    }

    private String formatTime(float time) {
        int seconds = Math.max(0, (int) time);
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void drawFrame() {
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            drawScene(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawScene(Canvas canvas) {
        canvas.drawColor(Color.rgb(9, 11, 18));
        drawBackground(canvas);
        drawGround(canvas);
        drawHazards(canvas);
        drawPickups(canvas);
        drawProjectiles(canvas);
        drawEnemies(canvas);
        drawPlayer(canvas);
        drawEffects(canvas);
        drawArenaEdges(canvas);
        if (mode == MODE_PAUSED) {
            drawScrim(canvas, 0x88000000);
        } else if (mode == MODE_MENU) {
            drawScrim(canvas, 0x54000000);
        } else if (mode == MODE_RESULT) {
            drawScrim(canvas, 0x66000000);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            float scale = (float) canvas.getHeight() / backgroundBitmap.getHeight();
            float width = backgroundBitmap.getWidth() * scale;
            for (float x = -cameraX * 0.22f % width - width; x < canvas.getWidth() + width; x += width) {
                tempDst.set(x, 0f, x + width, canvas.getHeight());
                canvas.drawBitmap(backgroundBitmap, null, tempDst, paint);
            }
        } else {
            paint.setColor(0xFF161A2E);
            canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), paint);
        }
        paint.setColor(0x551E223B);
        for (int i = 0; i < 6; i++) {
            float baseX = (i * 280f - cameraX * 0.35f) % (canvas.getWidth() + 280f) - 160f;
            canvas.drawRect(baseX, 130f, baseX + 120f, WORLD_FLOOR - 60f, paint);
        }
        paint.setColor(0x3345D2F7);
        canvas.drawRect(0f, 0f, canvas.getWidth(), 46f, paint);
    }

    private void drawGround(Canvas canvas) {
        paint.setColor(0xFF2A1C22);
        canvas.drawRect(0f, WORLD_FLOOR, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(0xFF4D302A);
        for (int i = 0; i < 24; i++) {
            float left = i * 120f - cameraX % 120f;
            canvas.drawRect(left, WORLD_FLOOR + 10f, left + 110f, WORLD_FLOOR + 28f, paint);
        }
        paint.setColor(0xFFB94B22);
        canvas.drawRect(0f, WORLD_FLOOR - 6f, canvas.getWidth(), WORLD_FLOOR + 4f, paint);
    }

    private void drawHazards(Canvas canvas) {
        for (Hazard hazard : hazards) {
            float screenX = hazard.x - cameraX;
            if (screenX < -120f || screenX > canvas.getWidth() + 120f) {
                continue;
            }
            if (hazard.activeTime > 0f) {
                if (hazard.type == 0) {
                    paint.setColor(0xFFFF7A2F);
                    canvas.drawRect(screenX - 48f, WORLD_FLOOR - 70f, screenX + 48f, WORLD_FLOOR, paint);
                    paint.setColor(0x55FFD166);
                    canvas.drawCircle(screenX, WORLD_FLOOR - 60f, 44f, paint);
                } else if (hazard.type == 1) {
                    paint.setColor(0xFFFFA53A);
                    canvas.drawCircle(screenX, WORLD_FLOOR - 34f, 34f, paint);
                    paint.setColor(0x99FF4F5E);
                    canvas.drawCircle(screenX, WORLD_FLOOR - 34f, 60f, paint);
                } else {
                    paint.setColor(0xFFFFD166);
                    canvas.drawRect(screenX - 18f, WORLD_FLOOR - 120f, screenX + 18f, WORLD_FLOOR, paint);
                }
            } else {
                paint.setColor(0x88A04B26);
                canvas.drawRect(screenX - 42f, WORLD_FLOOR - 20f, screenX + 42f, WORLD_FLOOR, paint);
            }
        }
    }

    private void drawProjectiles(Canvas canvas) {
        for (Projectile projectile : projectiles) {
            float screenX = projectile.x - cameraX;
            Bitmap bitmap = projectile.ownerPlayer ? sprites.effectFireball : sprites.effectWarning;
            if (projectile.type == 3) {
                bitmap = sprites.effectBurst;
            }
            if (bitmap != null) {
                drawBitmapCentered(canvas, bitmap, screenX, projectile.y, projectile.radius * 2.1f, projectile.radius * 2.1f, projectile.vx < 0f);
            } else {
                paint.setColor(projectile.ownerPlayer ? 0xFFFF7A2F : 0xFFFF4F5E);
                canvas.drawCircle(screenX, projectile.y, projectile.radius, paint);
            }
        }
    }

    private void drawPickups(Canvas canvas) {
        for (Pickup pickup : pickups) {
            float screenX = pickup.x - cameraX;
            Bitmap bitmap = pickup.type == 0 ? sprites.effectMedal : pickup.type == 1 ? sprites.effectHeatOrb : sprites.effectBurst;
            if (bitmap != null) {
                drawBitmapCentered(canvas, bitmap, screenX, pickup.y, 40f, 40f, false);
            } else {
                paint.setColor(pickup.type == 0 ? 0xFFFFCD57 : pickup.type == 1 ? 0xFFFF7A2F : 0xFF51F2A5);
                canvas.drawCircle(screenX, pickup.y, 16f, paint);
            }
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : enemies) {
            float screenX = enemy.x - cameraX;
            Bitmap bitmap = sprites.frameForEnemy(enemy);
            float drawY = enemy.y - (enemy.isFlying ? 54f : 84f);
            if (bitmap != null) {
                float width = enemy.isBoss ? 220f : enemy.isFlying ? 102f : 96f;
                float height = enemy.isBoss ? 220f : enemy.isFlying ? 90f : 112f;
                if (enemy.hitFlash > 0f) {
                    paint.setColorFilter(new PorterDuffColorFilter(0xAAFFFFFF, PorterDuff.Mode.SRC_ATOP));
                }
                drawBitmapCentered(canvas, bitmap, screenX, drawY, width, height, enemy.face > 0);
                paint.setColorFilter(null);
            } else {
                paint.setColor(enemy.isBoss ? 0xFF9A0F30 : enemy.type == 4 ? 0xFF9B7CFF : 0xFF593234);
                canvas.drawRoundRect(screenX - enemy.radius, enemy.y - enemy.radius * 2f, screenX + enemy.radius, enemy.y, 12f, 12f, paint);
            }
            paint.setColor(0x55100000);
            canvas.drawOval(screenX - enemy.radius, WORLD_FLOOR + 2f, screenX + enemy.radius, WORLD_FLOOR + 12f, paint);
            if (enemy.isBoss) {
                paint.setColor(0xAA28181E);
                canvas.drawRect(screenX - 84f, enemy.y - 140f, screenX + 84f, enemy.y - 126f, paint);
                paint.setColor(0xFFFF4F5E);
                float ratio = Math.max(0f, enemy.hp / enemy.maxHp);
                canvas.drawRect(screenX - 84f, enemy.y - 140f, screenX - 84f + 168f * ratio, enemy.y - 126f, paint);
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        float screenX = player.x - cameraX;
        Bitmap bitmap = sprites.frameForPlayer(player);
        if (bitmap != null) {
            drawBitmapCentered(canvas, bitmap, screenX, player.y - 84f, player.overdriveTimer > 0f ? 120f : 108f, player.overdriveTimer > 0f ? 132f : 120f, player.face < 0);
        } else {
            paint.setColor(player.overdriveTimer > 0f ? 0xFFFFCD57 : 0xFFFF7A2F);
            canvas.drawRoundRect(screenX - 30f, player.y - 92f, screenX + 30f, player.y, 14f, 14f, paint);
        }
        if (player.overdriveTimer > 0f) {
            paint.setColor(0x55FFD166);
            canvas.drawCircle(screenX, player.y - 52f, 44f + (float) Math.sin(player.frameTime * 8f) * 6f, paint);
        }
        if (player.hitFlash > 0f) {
            paint.setColor(0x55FFFFFF);
            canvas.drawCircle(screenX, player.y - 54f, 46f, paint);
        }
        paint.setColor(0x55000000);
        canvas.drawOval(screenX - 32f, WORLD_FLOOR + 2f, screenX + 32f, WORLD_FLOOR + 12f, paint);
    }

    private void drawEffects(Canvas canvas) {
        for (VisualEffect effect : effects) {
            float alphaRatio = Math.max(0f, effect.life / effect.maxLife);
            int alpha = Math.max(0, Math.min(255, Math.round(255f * alphaRatio)));
            int color = (effect.color & 0x00FFFFFF) | (alpha << 24);
            paint.setColor(color);
            float radius = effect.size * (1f - alphaRatio * 0.2f);
            canvas.drawCircle(effect.x - cameraX, effect.y, radius, paint);
        }
    }

    private void drawArenaEdges(Canvas canvas) {
        if (!encounterActive) {
            return;
        }
        paint.setColor(0x55FF4F5E);
        float left = encounterLeft - cameraX;
        float right = encounterRight - cameraX;
        canvas.drawRect(left - 12f, WORLD_FLOOR - 180f, left + 12f, WORLD_FLOOR + 24f, paint);
        canvas.drawRect(right - 12f, WORLD_FLOOR - 180f, right + 12f, WORLD_FLOOR + 24f, paint);
    }

    private void drawScrim(Canvas canvas, int color) {
        paint.setColor(color);
        canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), paint);
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float width, float height, boolean flipX) {
        if (flipX) {
            canvas.save();
            canvas.scale(-1f, 1f, x, y + height * 0.45f);
        }
        tempSrc.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        tempDst.set(x - width * 0.5f, y - height * 0.5f, x + width * 0.5f, y + height * 0.5f);
        canvas.drawBitmap(bitmap, tempSrc, tempDst, paint);
        if (flipX) {
            canvas.restore();
        }
    }

    private static List<StageDefinition> buildStages() {
        List<StageDefinition> list = new ArrayList<>();
        String[] chapterNames = {"Forge Gate", "Volcanic Pass", "Ash Shrine", "Dragon Forge", "Ember Forest", "Sky Citadel"};
        int[] hazardTypes = {0, 1, 2, 0, 1, 2};
        for (int chapter = 1; chapter <= 6; chapter++) {
            String chapterName = chapterNames[chapter - 1];
            for (int stage = 1; stage <= 4; stage++) {
                boolean boss = stage == 4;
                StageDefinition def = new StageDefinition();
                def.chapter = chapter;
                def.stageInChapter = stage;
                def.chapterName = chapterName;
                def.stageName = boss ? chapterName + " Wyrm Lord" : chapterName + " " + stageNameForStage(stage);
                def.isBossStage = boss;
                def.worldWidth = boss ? 3000f : 3400f + chapter * 120f;
                def.hazardType = hazardTypes[(chapter + stage - 2) % hazardTypes.length];
                def.hazardXs = boss ? new float[]{1020f, 1680f, 2360f} : new float[]{860f, 1540f, 2220f};
                def.encounters = boss ? buildBossEncounters(chapter) : buildNormalEncounters(chapter, stage);
                list.add(def);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private static List<StageEntry> buildEntries() {
        List<StageEntry> list = new ArrayList<>();
        for (StageDefinition definition : STAGES) {
            list.add(new StageEntry(definition.chapter, definition.stageInChapter, definition.chapterName, definition.stageName));
        }
        return Collections.unmodifiableList(list);
    }

    private static Encounter[] buildNormalEncounters(int chapter, int stage) {
        float base = 760f;
        Encounter[] encounters = new Encounter[3];
        encounters[0] = new Encounter(base, enemyWave(
                spec(0, base + 120f, 78f + chapter * 14f, 140f + chapter * 3f, 14f + chapter * 1.3f, 92f, 0.9f, false),
                spec(stage % 2 == 0 ? 1 : 0, base + 270f, 94f + chapter * 18f, 120f + chapter * 3f, 16f + chapter * 1.4f, 96f, 1.05f, false),
                spec(2, base + 390f, 86f + chapter * 10f, 110f + chapter * 2f, 13f + chapter * 1.2f, 250f, 1.35f, false)
        ));
        encounters[1] = new Encounter(base + 760f, enemyWave(
                spec(0, base + 860f, 92f + chapter * 16f, 150f + chapter * 3f, 15f + chapter * 1.3f, 98f, 0.95f, false),
                spec(3, base + 1010f, 98f + chapter * 22f, 102f + chapter * 2f, 14f + chapter * 1.3f, 260f, 1.6f, false),
                spec(4, base + 1160f, 90f + chapter * 20f, 170f + chapter * 2f, 15f + chapter * 1.4f, 230f, 1.3f, true)
        ));
        encounters[2] = new Encounter(base + 1500f, enemyWave(
                spec(1, base + 1620f, 120f + chapter * 20f, 122f + chapter * 4f, 18f + chapter * 1.5f, 100f, 1.05f, false),
                spec(5, base + 1790f, 150f + chapter * 26f, 136f + chapter * 3f, 20f + chapter * 1.7f, 112f, 1.1f, false),
                spec(stage % 2 == 1 ? 2 : 3, base + 1980f, 102f + chapter * 18f, 112f + chapter * 2f, 15f + chapter * 1.3f, 260f, 1.35f, false)
        ));
        return encounters;
    }

    private static Encounter[] buildBossEncounters(int chapter) {
        float base = 980f;
        Encounter[] encounters = new Encounter[2];
        encounters[0] = new Encounter(base, enemyWave(
                spec(5, base + 180f, 170f + chapter * 24f, 142f + chapter * 3f, 20f + chapter * 1.5f, 108f, 1.1f, false),
                spec(2, base + 360f, 98f + chapter * 16f, 112f + chapter * 2f, 15f + chapter * 1.3f, 250f, 1.3f, false),
                spec(4, base + 530f, 106f + chapter * 20f, 176f + chapter * 2f, 16f + chapter * 1.4f, 220f, 1.1f, true)
        ));
        encounters[1] = new Encounter(base + 980f, enemyWave(
                spec(6, base + 1140f, 920f + chapter * 180f, 126f + chapter * 4f, 20f + chapter * 2.4f, 116f, 1.2f, false)
        ));
        return encounters;
    }

    private static String stageNameForStage(int stage) {
        if (stage == 1) {
            return "Scorch March";
        }
        if (stage == 2) {
            return "Cinder Locks";
        }
        return "Molten Rush";
    }

    private static EnemySpec spec(int type, float x, float hp, float speed, float damage, float range, float interval, boolean flying) {
        EnemySpec spec = new EnemySpec();
        spec.type = type;
        spec.x = x;
        spec.hp = hp;
        spec.speed = speed;
        spec.damage = damage;
        spec.range = range;
        spec.interval = interval;
        spec.cooldown = 0.4f + type * 0.12f;
        spec.flying = flying;
        return spec;
    }

    private static EnemySpec[] enemyWave(EnemySpec... specs) {
        return specs;
    }

    private static class StageDefinition {
        int chapter;
        int stageInChapter;
        String chapterName;
        String stageName;
        boolean isBossStage;
        float worldWidth;
        int hazardType;
        float[] hazardXs;
        Encounter[] encounters;

        String objective(boolean encounterActive, int encounterIndex, List<Enemy> enemies) {
            if (isBossStage) {
                if (encounterIndex == 0 && encounterActive) {
                    return "Break the honor guard and push into the sanctum";
                }
                if (encounterIndex >= 1 && currentBossAlive(enemies)) {
                    return "Read the boss windup and punish between flame volleys";
                }
                return "Hold the lane and finish the chapter";
            }
            if (encounterActive) {
                return "Clear the lock zone and collect flame shards";
            }
            if (encounterIndex >= encounters.length) {
                return "Advance and claim the medal cache";
            }
            return "Advance right and trigger the next arena";
        }

        private boolean currentBossAlive(List<Enemy> enemies) {
            for (Enemy enemy : enemies) {
                if (!enemy.dead && enemy.isBoss) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class Encounter {
        final float triggerX;
        final EnemySpec[] enemySpecs;

        Encounter(float triggerX, EnemySpec[] enemySpecs) {
            this.triggerX = triggerX;
            this.enemySpecs = enemySpecs;
        }
    }

    private static class EnemySpec {
        int type;
        float x;
        float hp;
        float speed;
        float damage;
        float range;
        float cooldown;
        float interval;
        boolean flying;
    }

    private static class Player {
        float x = 170f;
        float y = WORLD_FLOOR;
        float vx;
        float vy;
        int face = 1;
        boolean onGround = true;
        int maxHp = 320;
        float hp = 320f;
        float heat;
        float attackTimer;
        float skillCooldown;
        float invul;
        float hitFlash;
        float frameTime;
        float actionPoseTimer;
        float comboTimer;
        int comboStep;
        float overdriveTimer;
        float heatGainMultiplier = 1f;
        int skillDamageBonus;
        float overdriveDurationBonus;

        void reset() {
            x = 170f;
            y = WORLD_FLOOR;
            vx = 0f;
            vy = 0f;
            face = 1;
            onGround = true;
            hp = maxHp;
            heat = 0f;
            attackTimer = 0f;
            skillCooldown = 0f;
            invul = 0f;
            hitFlash = 0f;
            frameTime = 0f;
            actionPoseTimer = 0f;
            comboTimer = 0f;
            comboStep = 0;
            overdriveTimer = 0f;
        }
    }

    private static class Enemy {
        int type;
        float x;
        float y;
        float vx;
        float vy;
        float speed;
        float hp;
        float maxHp;
        float attackDamage;
        float range;
        float cooldown;
        float attackInterval;
        float hitFlash;
        float frameTime;
        float deathTimer;
        int face = -1;
        int actionPose;
        int phase;
        boolean isFlying;
        boolean isBoss;
        boolean dead;
        float gravityScale = 1f;
        float radius = 36f;

        RectF bounds() {
            float width = isBoss ? 130f : isFlying ? 72f : 62f;
            float height = isBoss ? 146f : isFlying ? 66f : 94f;
            return new RectF(x - width * 0.5f, y - height, x + width * 0.5f, y);
        }
    }

    private static class Projectile {
        boolean ownerPlayer;
        int type;
        float x;
        float y;
        float vx;
        float vy;
        float damage;
        float radius;
        float life;
    }

    private static class Pickup {
        int type;
        int value;
        float x;
        float y;
        float life;
    }

    private static class Hazard {
        int type;
        float x;
        float width;
        float height;
        float cooldown;
        float activeTime;
        float time;
    }

    private static class VisualEffect {
        float x;
        float y;
        int color;
        float size;
        float life;
        float maxLife;

        static VisualEffect burst(float x, float y, int color, float size) {
            VisualEffect effect = new VisualEffect();
            effect.x = x;
            effect.y = y;
            effect.color = color;
            effect.size = size;
            effect.life = 0.26f;
            effect.maxLife = effect.life;
            return effect;
        }

        static VisualEffect banner(float x, float y, int color) {
            VisualEffect effect = new VisualEffect();
            effect.x = x;
            effect.y = y;
            effect.color = color;
            effect.size = 48f;
            effect.life = 0.34f;
            effect.maxLife = effect.life;
            return effect;
        }
    }

    private static class SpriteLibrary {
        final AssetManager assets;
        final Bitmap[] chapterBackgrounds = new Bitmap[6];
        final Bitmap playerIdle;
        final Bitmap playerRunA;
        final Bitmap playerRunB;
        final Bitmap playerJump;
        final Bitmap playerLight1;
        final Bitmap playerLight2;
        final Bitmap playerLight3;
        final Bitmap playerHeavy;
        final Bitmap playerSkill;
        final Bitmap playerOverIdle;
        final Bitmap playerOverRunA;
        final Bitmap playerOverRunB;
        final Bitmap playerOverSkill;
        final Bitmap enemySoldierIdle;
        final Bitmap enemySoldierMoveA;
        final Bitmap enemySoldierMoveB;
        final Bitmap enemyShieldIdle;
        final Bitmap enemyShieldMoveA;
        final Bitmap enemyShieldMoveB;
        final Bitmap enemyArcherIdle;
        final Bitmap enemyArcherAttack;
        final Bitmap enemyCultistIdle;
        final Bitmap enemyCultistCast;
        final Bitmap enemyDrakeA;
        final Bitmap enemyDrakeB;
        final Bitmap enemyCaptainA;
        final Bitmap enemyCaptainB;
        final Bitmap enemyCaptainAttack;
        final Bitmap bossIdle;
        final Bitmap bossMove;
        final Bitmap bossCast;
        final Bitmap bossSlam;
        final Bitmap bossPhase;
        final Bitmap effectFireball;
        final Bitmap effectBurst;
        final Bitmap effectHeatOrb;
        final Bitmap effectMedal;
        final Bitmap effectWarning;

        SpriteLibrary(AssetManager assets) {
            this.assets = assets;
            for (int i = 0; i < chapterBackgrounds.length; i++) {
                chapterBackgrounds[i] = load("game_art/flame_dragon_forge_pack/assets/background_" + (i + 1) + ".png");
            }
            playerIdle = load("game_art/flame_dragon_forge_pack/assets/player_idle.png");
            playerRunA = load("game_art/flame_dragon_forge_pack/assets/player_run_a.png");
            playerRunB = load("game_art/flame_dragon_forge_pack/assets/player_run_b.png");
            playerJump = load("game_art/flame_dragon_forge_pack/assets/player_jump.png");
            playerLight1 = load("game_art/flame_dragon_forge_pack/assets/player_light_1.png");
            playerLight2 = load("game_art/flame_dragon_forge_pack/assets/player_light_2.png");
            playerLight3 = load("game_art/flame_dragon_forge_pack/assets/player_light_3.png");
            playerHeavy = load("game_art/flame_dragon_forge_pack/assets/player_heavy.png");
            playerSkill = load("game_art/flame_dragon_forge_pack/assets/player_skill.png");
            playerOverIdle = load("game_art/flame_dragon_forge_pack/assets/player_overdrive_idle.png");
            playerOverRunA = load("game_art/flame_dragon_forge_pack/assets/player_overdrive_run_a.png");
            playerOverRunB = load("game_art/flame_dragon_forge_pack/assets/player_overdrive_run_b.png");
            playerOverSkill = load("game_art/flame_dragon_forge_pack/assets/player_overdrive_skill.png");
            enemySoldierIdle = load("game_art/flame_dragon_forge_pack/assets/enemy_soldier_idle.png");
            enemySoldierMoveA = load("game_art/flame_dragon_forge_pack/assets/enemy_soldier_move_a.png");
            enemySoldierMoveB = load("game_art/flame_dragon_forge_pack/assets/enemy_soldier_move_b.png");
            enemyShieldIdle = load("game_art/flame_dragon_forge_pack/assets/enemy_shield_idle.png");
            enemyShieldMoveA = load("game_art/flame_dragon_forge_pack/assets/enemy_shield_move_a.png");
            enemyShieldMoveB = load("game_art/flame_dragon_forge_pack/assets/enemy_shield_move_b.png");
            enemyArcherIdle = load("game_art/flame_dragon_forge_pack/assets/enemy_archer_idle.png");
            enemyArcherAttack = load("game_art/flame_dragon_forge_pack/assets/enemy_archer_attack.png");
            enemyCultistIdle = load("game_art/flame_dragon_forge_pack/assets/enemy_cultist_idle.png");
            enemyCultistCast = load("game_art/flame_dragon_forge_pack/assets/enemy_cultist_cast.png");
            enemyDrakeA = load("game_art/flame_dragon_forge_pack/assets/enemy_drake_fly_a.png");
            enemyDrakeB = load("game_art/flame_dragon_forge_pack/assets/enemy_drake_fly_b.png");
            enemyCaptainA = load("game_art/flame_dragon_forge_pack/assets/enemy_captain_move_a.png");
            enemyCaptainB = load("game_art/flame_dragon_forge_pack/assets/enemy_captain_move_b.png");
            enemyCaptainAttack = load("game_art/flame_dragon_forge_pack/assets/enemy_captain_attack.png");
            bossIdle = load("game_art/flame_dragon_forge_pack/assets/boss_idle.png");
            bossMove = load("game_art/flame_dragon_forge_pack/assets/boss_move.png");
            bossCast = load("game_art/flame_dragon_forge_pack/assets/boss_cast.png");
            bossSlam = load("game_art/flame_dragon_forge_pack/assets/boss_slam.png");
            bossPhase = load("game_art/flame_dragon_forge_pack/assets/boss_phase.png");
            effectFireball = load("game_art/flame_dragon_forge_pack/assets/effect_fireball.png");
            effectBurst = load("game_art/flame_dragon_forge_pack/assets/effect_burst.png");
            effectHeatOrb = load("game_art/flame_dragon_forge_pack/assets/effect_heat_orb.png");
            effectMedal = load("game_art/flame_dragon_forge_pack/assets/effect_medal.png");
            effectWarning = load("game_art/flame_dragon_forge_pack/assets/effect_warning.png");
        }

        Bitmap backgroundForChapter(int chapter) {
            int index = Math.max(0, Math.min(chapterBackgrounds.length - 1, chapter - 1));
            return chapterBackgrounds[index];
        }

        Bitmap frameForPlayer(Player player) {
            boolean over = player.overdriveTimer > 0f;
            if (!player.onGround) {
                return playerJump != null ? playerJump : playerIdle;
            }
            if (player.actionPoseTimer > 0f) {
                if (player.skillCooldown > 2f && player.attackTimer > 0.2f) {
                    return over ? playerOverSkill : playerSkill;
                }
                if (player.attackTimer > 0.24f) {
                    return playerHeavy != null ? playerHeavy : playerLight3;
                }
                if (player.comboStep == 1) {
                    return playerLight1;
                }
                if (player.comboStep == 2) {
                    return playerLight2;
                }
                if (player.comboStep >= 3) {
                    return playerLight3;
                }
            }
            if (Math.abs(player.vx) > 20f) {
                boolean alt = ((int) (player.frameTime * 10f)) % 2 == 0;
                if (over) {
                    return alt ? playerOverRunA : playerOverRunB;
                }
                return alt ? playerRunA : playerRunB;
            }
            return over ? playerOverIdle : playerIdle;
        }

        Bitmap frameForEnemy(Enemy enemy) {
            if (enemy.isBoss) {
                if (enemy.actionPose == 2) {
                    return bossSlam != null ? bossSlam : bossCast;
                }
                if (enemy.cooldown > enemy.attackInterval - 0.25f) {
                    return enemy.phase > 0 ? bossCast : bossPhase;
                }
                return Math.abs(enemy.vx) > 24f ? bossMove : bossIdle;
            }
            boolean alt = ((int) (enemy.frameTime * 8f)) % 2 == 0;
            if (enemy.type == 0) {
                return Math.abs(enemy.vx) > 16f ? (alt ? enemySoldierMoveA : enemySoldierMoveB) : enemySoldierIdle;
            }
            if (enemy.type == 1) {
                return Math.abs(enemy.vx) > 16f ? (alt ? enemyShieldMoveA : enemyShieldMoveB) : enemyShieldIdle;
            }
            if (enemy.type == 2) {
                return enemy.cooldown > enemy.attackInterval - 0.2f ? enemyArcherAttack : enemyArcherIdle;
            }
            if (enemy.type == 3) {
                return enemy.cooldown > enemy.attackInterval - 0.2f ? enemyCultistCast : enemyCultistIdle;
            }
            if (enemy.type == 4) {
                return alt ? enemyDrakeA : enemyDrakeB;
            }
            return enemy.cooldown > enemy.attackInterval - 0.25f ? enemyCaptainAttack : (alt ? enemyCaptainA : enemyCaptainB);
        }

        private Bitmap load(String path) {
            try (InputStream inputStream = assets.open(path)) {
                return BitmapFactory.decodeStream(inputStream);
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}
