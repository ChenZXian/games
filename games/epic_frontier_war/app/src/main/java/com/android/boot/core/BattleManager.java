package com.android.boot.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.TonePlayer;
import com.android.boot.entity.Archer;
import com.android.boot.entity.BaseCore;
import com.android.boot.entity.Knight;
import com.android.boot.entity.Militia;
import com.android.boot.entity.Priest;
import com.android.boot.entity.Projectile;
import com.android.boot.entity.Team;
import com.android.boot.entity.Titan;
import com.android.boot.entity.Unit;
import com.android.boot.entity.WarChampion;
import com.android.boot.fx.ParticleSystem;
import com.android.boot.fx.ScreenShake;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BattleManager {
    public interface HudCallback {
        void onHud(HudSnapshot snapshot);
    }

    public interface StateCallback {
        void onState(GameState state, MatchStats stats, boolean victory);
    }

    public static class HudSnapshot {
        public int playerHp;
        public int playerMaxHp;
        public int enemyHp;
        public int enemyMaxHp;
        public int energy;
        public int maxEnergy;
        public int favorPercent;
        public String favorLabel;
        public String heroCooldownLabel;
        public String phaseLabel;
        public String statusText;
        public String fireCooldown;
        public String chainCooldown;
        public String blessingCooldown;
    }

    private static class SpellCast {
        int type;
        float timer;
        float x;
        boolean ally;
    }

    public static final int SUMMON_MILITIA = 1;
    public static final int SUMMON_ARCHER = 2;
    public static final int SUMMON_KNIGHT = 3;
    public static final int SUMMON_PRIEST = 4;
    public static final int SUMMON_TITAN = 5;
    public static final int SPELL_FIRE_RAIN = 1;
    public static final int SPELL_CHAIN_BOLT = 2;
    public static final int SPELL_BLESSING_WAVE = 3;

    private final Context context;
    private final TonePlayer tonePlayer;
    private final Random random = new Random(17L);
    private final List<Unit> allyUnits = new ArrayList<>();
    private final List<Unit> enemyUnits = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<SpellCast> spellCasts = new ArrayList<>();
    private final MatchStats matchStats = new MatchStats();
    private final ParticleSystem particleSystem = new ParticleSystem(280, 90);
    private final ScreenShake screenShake = new ScreenShake();
    private final HudSnapshot hudSnapshot = new HudSnapshot();
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint projectilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final WarChampion champion = new WarChampion();
    private HudCallback hudCallback;
    private StateCallback stateCallback;
    private GameState gameState = GameState.MENU;
    private BattlePhase phase = BattlePhase.VANGUARD;
    private RuneFavor activeFavor = RuneFavor.FLAME;
    private BaseCore playerBase;
    private BaseCore enemyBase;
    private float groundY;
    private float worldWidth;
    private float worldHeight;
    private float energy;
    private float favorMeter;
    private float fireRainCooldown;
    private float chainBoltCooldown;
    private float blessingWaveCooldown;
    private float enemySpawnTimer;
    private float phaseTime;
    private float hudTimer;
    private String statusText = "Ready for battle";
    private boolean victory;

    public BattleManager(Context context, TonePlayer tonePlayer) {
        this.context = context;
        this.tonePlayer = tonePlayer;
        bgPaint.setColor(ContextCompat.getColor(context, R.color.cst_field_sky));
        groundPaint.setColor(ContextCompat.getColor(context, R.color.cst_field_ground));
        panelPaint.setColor(ContextCompat.getColor(context, R.color.cst_team_ally));
        accentPaint.setColor(ContextCompat.getColor(context, R.color.cst_accent_2));
        enemyAccentPaint.setColor(ContextCompat.getColor(context, R.color.cst_team_enemy));
        flashPaint.setColor(Color.WHITE);
        auraPaint.setColor(ContextCompat.getColor(context, R.color.cst_fx_vital));
        projectilePaint.setColor(ContextCompat.getColor(context, R.color.cst_fx_storm));
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text_primary));
        textPaint.setTextSize(28f);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    public void setHudCallback(HudCallback hudCallback) {
        this.hudCallback = hudCallback;
    }

    public void setStateCallback(StateCallback stateCallback) {
        this.stateCallback = stateCallback;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void ensureWorld(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        worldWidth = width;
        worldHeight = height;
        groundY = height * 0.84f;
        if (playerBase == null) {
            playerBase = new BaseCore(Team.ALLY, width * 0.03f, width * 0.14f, height * 0.36f, 2600);
            enemyBase = new BaseCore(Team.ENEMY, width * 0.83f, width * 0.14f, height * 0.38f, 2800);
            champion.resetChampion(width * 0.17f, groundY);
        }
    }

    public void startNewMatch(int width, int height) {
        ensureWorld(width, height);
        allyUnits.clear();
        enemyUnits.clear();
        projectiles.clear();
        spellCasts.clear();
        particleSystem.clear();
        playerBase.reset();
        enemyBase.reset();
        champion.resetChampion(width * 0.17f, groundY);
        energy = 70f;
        favorMeter = 20f;
        fireRainCooldown = 0f;
        chainBoltCooldown = 0f;
        blessingWaveCooldown = 0f;
        enemySpawnTimer = 1.2f;
        phaseTime = 0f;
        hudTimer = 0f;
        phase = BattlePhase.VANGUARD;
        activeFavor = RuneFavor.FLAME;
        statusText = "Battle joined";
        victory = false;
        matchStats.reset();
        gameState = GameState.PLAYING;
        pushState(false);
        pushHud(true);
    }

    public void returnToMenu() {
        gameState = GameState.MENU;
        pushState(false);
    }

    public void pause() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED;
            pushState(false);
        }
    }

    public void resume() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
            pushState(false);
        }
    }

    public void onExternalPause() {
        if (gameState == GameState.PLAYING) {
            pause();
        }
    }

    public void queueSummon(int type) {
        if (gameState != GameState.PLAYING) {
            return;
        }
        int cost = getCost(type);
        if (energy < cost) {
            statusText = "Need more energy";
            return;
        }
        energy -= cost;
        Unit unit = createSummon(type, Team.ALLY, activeFavor);
        if (unit != null) {
            unit.resetPosition(worldWidth * 0.16f + allyUnits.size() * 6f, groundY);
            allyUnits.add(unit);
            matchStats.addUnitSummoned();
            favorMeter = Math.min(100f, favorMeter + 5f);
            statusText = unit.getLabel() + " joined under " + activeFavor.getLabel();
            tonePlayer.playSummon();
        }
    }

    private int getCost(int type) {
        if (type == SUMMON_MILITIA) {
            return 18;
        }
        if (type == SUMMON_ARCHER) {
            return 24;
        }
        if (type == SUMMON_KNIGHT) {
            return 42;
        }
        if (type == SUMMON_PRIEST) {
            return 38;
        }
        return 70;
    }

    private Unit createSummon(int type, Team team, RuneFavor favor) {
        if (type == SUMMON_MILITIA) {
            return new Militia(team, favor);
        }
        if (type == SUMMON_ARCHER) {
            return new Archer(team, favor);
        }
        if (type == SUMMON_KNIGHT) {
            return new Knight(team, favor);
        }
        if (type == SUMMON_PRIEST) {
            return new Priest(team, favor);
        }
        if (type == SUMMON_TITAN) {
            return new Titan(team, favor);
        }
        return null;
    }

    public void selectFavor(RuneFavor favor) {
        if (gameState != GameState.PLAYING || activeFavor == favor || favorMeter < 15f) {
            return;
        }
        activeFavor = favor;
        favorMeter = Math.max(0f, favorMeter - 15f);
        statusText = favor.getLabel() + " surges across the warfront";
        int color = getFavorColor(favor);
        for (int i = 0; i < 12; i++) {
            particleSystem.emit(worldWidth * 0.2f + i * 14f, groundY - 120f, randomRange(-22f, 22f), randomRange(-50f, -12f), 0.85f, 10f + i * 0.4f, color, i % 3 == 0);
        }
    }

    public void castSpell(int spell) {
        if (gameState != GameState.PLAYING) {
            return;
        }
        if (spell == SPELL_FIRE_RAIN && fireRainCooldown > 0f) {
            return;
        }
        if (spell == SPELL_CHAIN_BOLT && chainBoltCooldown > 0f) {
            return;
        }
        if (spell == SPELL_BLESSING_WAVE && blessingWaveCooldown > 0f) {
            return;
        }
        SpellCast spellCast = new SpellCast();
        spellCast.type = spell;
        spellCast.timer = spell == SPELL_BLESSING_WAVE ? 0.35f : 0.7f;
        spellCast.ally = true;
        spellCast.x = spell == SPELL_BLESSING_WAVE ? worldWidth * 0.34f : worldWidth * 0.6f;
        spellCasts.add(spellCast);
        matchStats.addSpellCast();
        favorMeter = Math.min(100f, favorMeter + 10f);
        tonePlayer.playSpell();
        if (spell == SPELL_FIRE_RAIN) {
            fireRainCooldown = 9f;
            statusText = "Fire Rain begins to fall";
        } else if (spell == SPELL_CHAIN_BOLT) {
            chainBoltCooldown = 10f;
            statusText = "Chain Bolt arcs forward";
        } else {
            blessingWaveCooldown = 12f;
            statusText = "Blessing Wave gathers";
        }
    }

    public void triggerHeroAbility() {
        if (gameState != GameState.PLAYING || !champion.canUseAbility()) {
            return;
        }
        champion.triggerAbility(activeFavor);
        tonePlayer.playSpell();
        if (activeFavor == RuneFavor.FLAME) {
            statusText = "War Champion cleaves in flame";
            slamEnemies(champion.getX() + 90f, 170f, 78f, true);
        } else if (activeFavor == RuneFavor.STORM) {
            statusText = "War Champion chains storm strikes";
            chainStrike(champion.getX() + 60f, 4);
        } else {
            statusText = "War Champion rallies the line";
            healAllies(16f, 260f);
        }
    }

    public void update(float dt, int width, int height) {
        ensureWorld(width, height);
        if (gameState != GameState.PLAYING) {
            pushHud(false);
            return;
        }
        playerBase.update(dt);
        enemyBase.update(dt);
        phaseTime += dt;
        hudTimer += dt;
        energy = Math.min(100f, energy + dt * 9f);
        favorMeter = Math.min(100f, favorMeter + dt * 3.8f);
        fireRainCooldown = Math.max(0f, fireRainCooldown - dt);
        chainBoltCooldown = Math.max(0f, chainBoltCooldown - dt);
        blessingWaveCooldown = Math.max(0f, blessingWaveCooldown - dt);
        champion.updateTime(dt);
        champion.tickAttack(dt);
        champion.tickAbility(dt, activeFavor);
        champion.updateRetreat(dt, worldWidth * 0.13f);
        updatePhase();
        spawnEnemies(dt);
        updateUnits(dt, allyUnits, enemyUnits, enemyBase, true);
        updateUnits(dt, enemyUnits, allyUnits, playerBase, false);
        updateChampion(dt);
        updateProjectiles(dt);
        updateSpells(dt);
        applyPriestSustain(dt, allyUnits);
        applyPriestSustain(dt, enemyUnits);
        particleSystem.update(dt);
        screenShake.update(dt);
        cleanup();
        if (playerBase.isDestroyed() || enemyBase.isDestroyed()) {
            endMatch(enemyBase.isDestroyed());
        }
        if (hudTimer > 0.08f) {
            hudTimer = 0f;
            pushHud(false);
        }
    }

    private void updatePhase() {
        if (phaseTime > 85f || enemyBase.getHp() < enemyBase.getMaxHp() * 0.25f) {
            phase = BattlePhase.FORTRESS_SIEGE;
        } else if (phaseTime > 62f || enemyBase.getHp() < enemyBase.getMaxHp() * 0.45f) {
            phase = BattlePhase.SANCTIFIED_PUSH;
        } else if (phaseTime > 42f || enemyBase.getHp() < enemyBase.getMaxHp() * 0.62f) {
            phase = BattlePhase.KNIGHT_CRASH;
        } else if (phaseTime > 24f || enemyBase.getHp() < enemyBase.getMaxHp() * 0.8f) {
            phase = BattlePhase.ARROW_STORM;
        } else {
            phase = BattlePhase.VANGUARD;
        }
    }

    private void spawnEnemies(float dt) {
        enemySpawnTimer -= dt;
        if (enemySpawnTimer > 0f) {
            return;
        }
        int wave = 1;
        if (phase == BattlePhase.VANGUARD) {
            spawnEnemy(SUMMON_MILITIA, RuneFavor.FLAME);
            if (random.nextBoolean()) {
                spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            }
            enemySpawnTimer = 2.8f;
        } else if (phase == BattlePhase.ARROW_STORM) {
            spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            spawnEnemy(SUMMON_MILITIA, RuneFavor.FLAME);
            if (random.nextBoolean()) {
                spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            }
            enemySpawnTimer = 2.3f;
        } else if (phase == BattlePhase.KNIGHT_CRASH) {
            spawnEnemy(SUMMON_KNIGHT, RuneFavor.FLAME);
            spawnEnemy(SUMMON_MILITIA, RuneFavor.FLAME);
            spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            enemySpawnTimer = 2.1f;
        } else if (phase == BattlePhase.SANCTIFIED_PUSH) {
            spawnEnemy(SUMMON_PRIEST, RuneFavor.VITAL);
            spawnEnemy(SUMMON_KNIGHT, RuneFavor.FLAME);
            spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            wave = random.nextBoolean() ? SUMMON_MILITIA : SUMMON_KNIGHT;
            spawnEnemy(wave, RuneFavor.FLAME);
            enemySpawnTimer = 1.9f;
        } else {
            spawnEnemy(SUMMON_TITAN, RuneFavor.FLAME);
            spawnEnemy(SUMMON_KNIGHT, RuneFavor.FLAME);
            spawnEnemy(SUMMON_PRIEST, RuneFavor.VITAL);
            spawnEnemy(SUMMON_ARCHER, RuneFavor.STORM);
            enemySpawnTimer = 2.4f;
        }
    }

    private void spawnEnemy(int type, RuneFavor favor) {
        Unit unit = createSummon(type, Team.ENEMY, favor);
        if (unit == null) {
            return;
        }
        unit.resetPosition(worldWidth * 0.84f - enemyUnits.size() * 6f, groundY);
        enemyUnits.add(unit);
    }

    private void updateUnits(float dt, List<Unit> units, List<Unit> opponents, BaseCore opposingBase, boolean allies) {
        for (Unit unit : units) {
            unit.updateTime(dt);
            unit.tickAttack(dt);
            unit.updateDeath(dt);
            if (unit.isDying()) {
                continue;
            }
            Unit target = findTarget(unit, opponents);
            if (target != null) {
                float distance = Math.abs(target.getX() - unit.getX());
                float stopRange = unit.isRanged() ? unit.getPreferredRange() : unit.getAttackRange();
                if (distance > stopRange && (!unit.isSupport() || !alliesFrontTooFar(unit))) {
                    unit.move((allies ? 1f : -1f) * unit.getMoveSpeed() * dt);
                    if (unit.isTitan()) {
                        emitDust(unit.getX(), groundY - 6f, 2);
                    }
                } else if (unit.canAttack()) {
                    unit.startAttack(target);
                }
                if (unit.readyForImpact()) {
                    unit.markImpactDone();
                    resolveUnitImpact(unit, target);
                }
            } else {
                float baseDistance = Math.abs(opposingBase.getBodyX(unit.getTeam()) - unit.getX());
                if (baseDistance > unit.getAttackRange() + 8f) {
                    unit.move((allies ? 1f : -1f) * unit.getMoveSpeed() * dt);
                } else if (unit.canAttack()) {
                    unit.startAttack(null);
                }
                if (unit.readyForImpact()) {
                    unit.markImpactDone();
                    int amount = Math.round(unit.getDamage() * (unit.isTitan() ? 1.35f : 1f));
                    opposingBase.damage(amount);
                    if (allies) {
                        matchStats.addFortressDamage(amount);
                    }
                    statusText = allies ? "Fortress struck" : "Castle under siege";
                    tonePlayer.playImpact();
                    screenShake.trigger(unit.isTitan() ? 0.7f : 0.32f);
                    spawnSlash(opposingBase.getCenterX(), groundY - 120f, unit.isTitan() ? 8 : 4, unit.getTeam() == Team.ALLY ? getFavorColor(unit.getFavorAtSpawn()) : ContextCompat.getColor(context, R.color.cst_team_enemy));
                }
            }
            clampUnit(unit);
        }
    }

    private boolean alliesFrontTooFar(Unit unit) {
        float frontline = champion.getX();
        for (Unit ally : allyUnits) {
            if (!ally.isDying()) {
                frontline = Math.max(frontline, ally.getX());
            }
        }
        return frontline - unit.getX() > 100f;
    }

    private void updateChampion(float dt) {
        if (champion.isRetreating()) {
            return;
        }
        Unit target = findTarget(champion, enemyUnits);
        if (target != null) {
            float distance = Math.abs(target.getX() - champion.getX());
            if (distance > champion.getAttackRange()) {
                champion.move(champion.getMoveSpeed() * dt);
            } else if (champion.canAttack()) {
                champion.startAttack(target);
            }
            if (champion.readyForImpact()) {
                champion.markImpactDone();
                float damage = champion.getDamage();
                target.takeDamage(damage);
                if (activeFavor == RuneFavor.FLAME) {
                    slamEnemies(target.getX(), 80f, 14f, false);
                } else if (activeFavor == RuneFavor.VITAL) {
                    healAllies(6f, 120f);
                }
                if (target.isDying()) {
                    matchStats.addEnemyDefeated();
                }
                spawnSlash(target.getX(), target.getY() - target.getHeight() * 0.5f, 6, getFavorColor(activeFavor));
                tonePlayer.playImpact();
            }
        } else if (champion.getX() < worldWidth * 0.45f) {
            champion.move(champion.getMoveSpeed() * dt * 0.7f);
        }
        champion.tickAttack(dt);
    }

    private void resolveUnitImpact(Unit unit, Unit target) {
        if (target == null) {
            return;
        }
        if (unit.isRanged()) {
            float dir = unit.getTeam() == Team.ALLY ? 1f : -1f;
            boolean lightning = unit.getFavorAtSpawn() == RuneFavor.STORM;
            projectiles.add(new Projectile(unit.getTeam(), unit.getX() + dir * unit.getWidth() * 0.22f, unit.getY() - unit.getHeight() * 0.62f, dir * unit.getProjectileSpeed(), unit.getDamage(), unit.isSupport() ? 8f : 6f, lightning));
            emitDust(unit.getX(), groundY - unit.getHeight() * 0.2f, 1);
        } else {
            float damage = unit.getDamage();
            target.takeDamage(damage);
            favorMeter = Math.min(100f, favorMeter + 3f);
            if (unit.getFavorAtSpawn() == RuneFavor.FLAME) {
                spawnSlash(target.getX(), target.getY() - target.getHeight() * 0.5f, 5, ContextCompat.getColor(context, R.color.cst_fx_fire));
            } else {
                spawnSlash(target.getX(), target.getY() - target.getHeight() * 0.5f, 4, unit.getTeam() == Team.ALLY ? ContextCompat.getColor(context, R.color.cst_team_ally) : ContextCompat.getColor(context, R.color.cst_team_enemy));
            }
            if (unit.isTitan()) {
                screenShake.trigger(0.6f);
            }
            if (target.isDying() && unit.getTeam() == Team.ALLY) {
                matchStats.addEnemyDefeated();
            }
            if (target.isChampion()) {
                statusText = "War Champion pressured";
            }
            tonePlayer.playImpact();
            particleSystem.text(target.getX(), target.getY() - target.getHeight(), String.valueOf(Math.round(damage)), ContextCompat.getColor(context, R.color.cst_fx_text_damage));
        }
    }

    private Unit findTarget(Unit seeker, List<Unit> opponents) {
        Unit nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Unit opponent : opponents) {
            if (opponent.isDying()) {
                continue;
            }
            float delta = opponent.getX() - seeker.getX();
            if (seeker.getTeam() == Team.ALLY && delta < -12f) {
                continue;
            }
            if (seeker.getTeam() == Team.ENEMY && delta > 12f) {
                continue;
            }
            float distance = Math.abs(delta);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = opponent;
            }
        }
        if (seeker.getTeam() == Team.ENEMY && !champion.isRetreating()) {
            float delta = champion.getX() - seeker.getX();
            if (delta < 0f) {
                float distance = Math.abs(delta);
                if (distance < nearestDistance) {
                    return champion;
                }
            }
        }
        return nearest;
    }

    private void updateProjectiles(float dt) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(dt);
            if (projectile.x < 0f || projectile.x > worldWidth) {
                iterator.remove();
                continue;
            }
            List<Unit> targets = projectile.team == Team.ALLY ? enemyUnits : allyUnits;
            boolean hit = false;
            for (Unit target : targets) {
                if (target.isDying()) {
                    continue;
                }
                if (Math.abs(target.getX() - projectile.x) < projectile.radius + target.getWidth() * 0.3f) {
                    if (projectile.team == Team.ALLY && target.isChampion()) {
                        continue;
                    }
                    if (projectile.team == Team.ENEMY && !champion.isRetreating() && Math.abs(champion.getX() - projectile.x) < projectile.radius + champion.getWidth() * 0.3f) {
                        champion.takeDamage(projectile.damage);
                    } else {
                        target.takeDamage(projectile.damage);
                    }
                    int color = projectile.lightning ? ContextCompat.getColor(context, R.color.cst_fx_storm) : ContextCompat.getColor(context, R.color.cst_fx_fire);
                    spawnSlash(projectile.x, target.getY() - target.getHeight() * 0.6f, projectile.lightning ? 6 : 4, color);
                    particleSystem.text(target.getX(), target.getY() - target.getHeight(), String.valueOf(Math.round(projectile.damage)), ContextCompat.getColor(context, R.color.cst_fx_text_damage));
                    if (target.isDying() && projectile.team == Team.ALLY) {
                        matchStats.addEnemyDefeated();
                    }
                    hit = true;
                    break;
                }
            }
            if (!hit && projectile.team == Team.ENEMY && !champion.isRetreating() && Math.abs(champion.getX() - projectile.x) < projectile.radius + champion.getWidth() * 0.3f) {
                champion.takeDamage(projectile.damage);
                spawnSlash(projectile.x, champion.getY() - champion.getHeight() * 0.6f, 4, ContextCompat.getColor(context, R.color.cst_fx_storm));
                hit = true;
            }
            if (hit) {
                iterator.remove();
            }
        }
    }

    private void updateSpells(float dt) {
        Iterator<SpellCast> iterator = spellCasts.iterator();
        while (iterator.hasNext()) {
            SpellCast spellCast = iterator.next();
            spellCast.timer -= dt;
            if (spellCast.timer > 0f) {
                continue;
            }
            if (spellCast.type == SPELL_FIRE_RAIN) {
                resolveFireRain();
            } else if (spellCast.type == SPELL_CHAIN_BOLT) {
                resolveChainBolt();
            } else {
                resolveBlessingWave();
            }
            iterator.remove();
        }
    }

    private void resolveFireRain() {
        float center = worldWidth * 0.62f;
        float radius = activeFavor == RuneFavor.FLAME ? 180f : 140f;
        int color = ContextCompat.getColor(context, R.color.cst_fx_fire);
        for (int i = 0; i < 18; i++) {
            float x = center + randomRange(-radius, radius);
            float y = groundY - randomRange(70f, 160f);
            particleSystem.emit(x, y, randomRange(-10f, 10f), randomRange(16f, 52f), 0.85f, randomRange(6f, 14f), color, false);
        }
        for (Unit unit : enemyUnits) {
            if (!unit.isDying() && Math.abs(unit.getX() - center) < radius) {
                unit.takeDamage(activeFavor == RuneFavor.FLAME ? 48f : 38f);
                particleSystem.text(unit.getX(), unit.getY() - unit.getHeight(), String.valueOf(activeFavor == RuneFavor.FLAME ? 48 : 38), ContextCompat.getColor(context, R.color.cst_fx_text_damage));
                if (unit.isDying()) {
                    matchStats.addEnemyDefeated();
                }
            }
        }
        screenShake.trigger(0.55f);
    }

    private void resolveChainBolt() {
        int jumps = activeFavor == RuneFavor.STORM ? 6 : 4;
        chainStrike(worldWidth * 0.58f, jumps);
    }

    private void resolveBlessingWave() {
        float amount = activeFavor == RuneFavor.VITAL ? 36f : 24f;
        healAllies(amount, 320f);
        for (int i = 0; i < 14; i++) {
            particleSystem.emit(worldWidth * 0.32f, groundY - 90f, randomRange(-58f, 58f), randomRange(-22f, 22f), 0.95f, 18f + i, ContextCompat.getColor(context, R.color.cst_fx_vital), true);
        }
    }

    private void chainStrike(float startX, int jumps) {
        float focus = startX;
        int color = ContextCompat.getColor(context, R.color.cst_fx_storm);
        for (int i = 0; i < jumps; i++) {
            Unit target = null;
            float bestDistance = Float.MAX_VALUE;
            for (Unit enemy : enemyUnits) {
                if (enemy.isDying()) {
                    continue;
                }
                float distance = Math.abs(enemy.getX() - focus);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    target = enemy;
                }
            }
            if (target == null) {
                break;
            }
            target.takeDamage(28f + i * 3f);
            particleSystem.emit(target.getX(), target.getY() - target.getHeight() * 0.7f, 0f, 0f, 0.4f, 24f + i * 3f, color, true);
            particleSystem.text(target.getX(), target.getY() - target.getHeight(), String.valueOf(28 + i * 3), ContextCompat.getColor(context, R.color.cst_fx_text_damage));
            if (target.isDying()) {
                matchStats.addEnemyDefeated();
            }
            focus = target.getX() + 60f;
        }
        screenShake.trigger(0.32f);
    }

    private void slamEnemies(float centerX, float radius, float damage, boolean heavy) {
        int color = ContextCompat.getColor(context, R.color.cst_fx_fire);
        for (Unit enemy : enemyUnits) {
            if (!enemy.isDying() && Math.abs(enemy.getX() - centerX) < radius) {
                enemy.takeDamage(damage);
                particleSystem.emit(enemy.getX(), enemy.getY() - enemy.getHeight() * 0.5f, randomRange(-10f, 10f), randomRange(-12f, 12f), 0.6f, 16f, color, false);
                if (enemy.isDying()) {
                    matchStats.addEnemyDefeated();
                }
            }
        }
        if (heavy) {
            screenShake.trigger(0.68f);
        }
    }

    private void healAllies(float amount, float radius) {
        int color = ContextCompat.getColor(context, R.color.cst_fx_text_heal);
        if (!champion.isRetreating()) {
            champion.heal(amount * 0.55f);
            particleSystem.text(champion.getX(), champion.getY() - champion.getHeight(), "+" + Math.round(amount * 0.55f), color);
        }
        for (Unit ally : allyUnits) {
            if (!ally.isDying() && Math.abs(ally.getX() - worldWidth * 0.32f) < radius) {
                ally.heal(amount);
                particleSystem.text(ally.getX(), ally.getY() - ally.getHeight(), "+" + Math.round(amount), color);
            }
        }
    }

    private void applyPriestSustain(float dt, List<Unit> units) {
        for (Unit unit : units) {
            if (!unit.isSupport() || unit.isDying()) {
                continue;
            }
            float pulse = unit.getSustainPulse();
            if (pulse <= 0f) {
                continue;
            }
            float rate = unit.getSustainRate();
            float cycle = Math.max(0.35f, rate);
            float window = (phaseTime + unit.getX() * 0.001f) % cycle;
            if (window < dt) {
                for (Unit friend : units) {
                    if (!friend.isDying() && Math.abs(friend.getX() - unit.getX()) < 140f) {
                        friend.heal(pulse);
                        particleSystem.text(friend.getX(), friend.getY() - friend.getHeight(), "+" + Math.round(pulse), ContextCompat.getColor(context, R.color.cst_fx_text_heal));
                    }
                }
            }
        }
    }

    private void cleanup() {
        removeFaded(allyUnits);
        removeFaded(enemyUnits);
    }

    private void removeFaded(List<Unit> units) {
        Iterator<Unit> iterator = units.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isFaded()) {
                iterator.remove();
            }
        }
    }

    private void endMatch(boolean playerVictory) {
        victory = playerVictory;
        gameState = GameState.GAME_OVER;
        pushState(playerVictory);
    }

    private void pushState(boolean playerVictory) {
        if (stateCallback != null) {
            stateCallback.onState(gameState, matchStats, playerVictory);
        }
    }

    private void pushHud(boolean immediate) {
        if (hudCallback == null || playerBase == null || enemyBase == null) {
            return;
        }
        hudSnapshot.playerHp = playerBase.getHp();
        hudSnapshot.playerMaxHp = playerBase.getMaxHp();
        hudSnapshot.enemyHp = enemyBase.getHp();
        hudSnapshot.enemyMaxHp = enemyBase.getMaxHp();
        hudSnapshot.energy = Math.round(energy);
        hudSnapshot.maxEnergy = 100;
        hudSnapshot.favorPercent = Math.round(favorMeter);
        hudSnapshot.favorLabel = activeFavor.getLabel();
        hudSnapshot.heroCooldownLabel = champion.canUseAbility() ? "Ready" : String.format(Locale.US, "%.1fs", champion.getAbilityCooldown());
        hudSnapshot.phaseLabel = phase.getLabel();
        hudSnapshot.statusText = statusText;
        hudSnapshot.fireCooldown = fireRainCooldown <= 0f ? "Ready" : String.format(Locale.US, "%.1f", fireRainCooldown);
        hudSnapshot.chainCooldown = chainBoltCooldown <= 0f ? "Ready" : String.format(Locale.US, "%.1f", chainBoltCooldown);
        hudSnapshot.blessingCooldown = blessingWaveCooldown <= 0f ? "Ready" : String.format(Locale.US, "%.1f", blessingWaveCooldown);
        hudCallback.onHud(hudSnapshot);
    }

    public void render(Canvas canvas, int width, int height) {
        ensureWorld(width, height);
        canvas.drawRect(0f, 0f, width, height, bgPaint);
        canvas.drawRect(0f, groundY - 20f, width, height, groundPaint);
        drawBackdrop(canvas);
        canvas.save();
        canvas.translate(screenShake.getOffsetX(), screenShake.getOffsetY());
        Paint allyBody = new Paint(panelPaint);
        Paint allyAccent = new Paint(accentPaint);
        Paint enemyBody = new Paint(enemyAccentPaint);
        Paint enemyTrim = new Paint(accentPaint);
        enemyTrim.setColor(ContextCompat.getColor(context, R.color.cst_team_enemy_alt));
        playerBase.draw(canvas, groundY, allyBody, allyAccent, enemyTrim);
        enemyBase.draw(canvas, groundY, enemyBody, enemyTrim, allyAccent);
        drawUnits(canvas, allyUnits, true);
        if (!champion.isRetreating()) {
            drawChampion(canvas);
        }
        drawUnits(canvas, enemyUnits, false);
        drawProjectiles(canvas);
        particleSystem.draw(canvas, particlePaint, textPaint);
        canvas.restore();
        if (gameState == GameState.MENU) {
            textPaint.setAlpha(255);
            canvas.drawText("Epic Frontier War", width * 0.36f, height * 0.22f, textPaint);
        }
    }

    private void drawBackdrop(Canvas canvas) {
        Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloud.setColor(ContextCompat.getColor(context, R.color.cst_bg_alt));
        canvas.drawOval(worldWidth * 0.15f, worldHeight * 0.1f, worldWidth * 0.32f, worldHeight * 0.22f, cloud);
        canvas.drawOval(worldWidth * 0.58f, worldHeight * 0.08f, worldWidth * 0.8f, worldHeight * 0.2f, cloud);
        Paint hill = new Paint(Paint.ANTI_ALIAS_FLAG);
        hill.setColor(ContextCompat.getColor(context, R.color.cst_panel_bg));
        canvas.drawOval(worldWidth * 0.02f, groundY - 110f, worldWidth * 0.38f, groundY + 70f, hill);
        canvas.drawOval(worldWidth * 0.56f, groundY - 130f, worldWidth * 0.96f, groundY + 90f, hill);
    }

    private void drawUnits(Canvas canvas, List<Unit> units, boolean ally) {
        for (Unit unit : units) {
            Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
            base.setColor(ally ? ContextCompat.getColor(context, R.color.cst_team_ally) : ContextCompat.getColor(context, R.color.cst_team_enemy));
            accent.setColor(unit.getFavorAtSpawn() == RuneFavor.FLAME ? ContextCompat.getColor(context, R.color.cst_fx_fire) : unit.getFavorAtSpawn() == RuneFavor.STORM ? ContextCompat.getColor(context, R.color.cst_fx_storm) : ContextCompat.getColor(context, R.color.cst_fx_vital));
            auraPaint.setColor(accent.getColor());
            unit.draw(canvas, base, accent, flashPaint, auraPaint);
        }
    }

    private void drawChampion(Canvas canvas) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setColor(ContextCompat.getColor(context, R.color.cst_text_primary));
        accent.setColor(getFavorColor(activeFavor));
        auraPaint.setColor(accent.getColor());
        champion.draw(canvas, base, accent, flashPaint, auraPaint);
    }

    private void drawProjectiles(Canvas canvas) {
        for (Projectile projectile : projectiles) {
            projectilePaint.setColor(projectile.lightning ? ContextCompat.getColor(context, R.color.cst_fx_storm) : ContextCompat.getColor(context, R.color.cst_warning));
            projectile.draw(canvas, projectilePaint);
        }
    }

    private int getFavorColor(RuneFavor favor) {
        if (favor == RuneFavor.FLAME) {
            return ContextCompat.getColor(context, R.color.cst_fx_fire);
        }
        if (favor == RuneFavor.STORM) {
            return ContextCompat.getColor(context, R.color.cst_fx_storm);
        }
        return ContextCompat.getColor(context, R.color.cst_fx_vital);
    }

    private void spawnSlash(float x, float y, int count, int color) {
        for (int i = 0; i < count; i++) {
            particleSystem.emit(x, y, randomRange(-45f, 45f), randomRange(-42f, 8f), 0.55f, randomRange(4f, 10f), color, false);
        }
    }

    private void emitDust(float x, float y, int count) {
        int color = ContextCompat.getColor(context, R.color.cst_field_dust);
        for (int i = 0; i < count; i++) {
            particleSystem.emit(x, y, randomRange(-18f, 18f), randomRange(-12f, -4f), 0.5f, randomRange(5f, 9f), color, false);
        }
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private void clampUnit(Unit unit) {
        float min = playerBase.getBodyX(Team.ALLY) + 16f;
        float max = enemyBase.getBodyX(Team.ENEMY) - 16f;
        if (unit.getX() < min) {
            unit.resetPosition(min, unit.getY());
        } else if (unit.getX() > max) {
            unit.resetPosition(max, unit.getY());
        }
    }
}
