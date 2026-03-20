package com.android.boot.core;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

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
import com.android.boot.fx.FloatingText;
import com.android.boot.fx.ParticleSystem;
import com.android.boot.fx.ScreenShake;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class BattleManager {
    public static final int SUMMON_MILITIA = 1;
    public static final int SUMMON_ARCHER = 2;
    public static final int SUMMON_KNIGHT = 3;
    public static final int SUMMON_PRIEST = 4;
    public static final int SUMMON_TITAN = 5;
    public static final int ACTION_FIRE_RAIN = 21;
    public static final int ACTION_CHAIN_BOLT = 22;
    public static final int ACTION_BLESSING_WAVE = 23;
    public static final int ACTION_HERO_ABILITY = 24;
    private static final float FIELD_WIDTH = 2400f;
    private static final float GROUND_Y = 860f;
    private static final float PLAYER_SPAWN_X = 250f;
    private static final float ENEMY_SPAWN_X = 2140f;
    private final Context context;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Unit> allies = new ArrayList<>();
    private final List<Unit> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<SpellEvent> spellEvents = new ArrayList<>();
    private final FloatingText[] floatingTexts = new FloatingText[48];
    private final ParticleSystem particles = new ParticleSystem(192);
    private final ScreenShake screenShake = new ScreenShake();
    private final MatchStats stats = new MatchStats();
    private TonePlayer tonePlayer;
    private GameState state = GameState.MENU;
    private BattlePhase phase = BattlePhase.PHASE_ONE;
    private BaseCore playerBase;
    private BaseCore enemyBase;
    private WarChampion warChampion;
    private RuneFavor activeFavor = RuneFavor.FLAME_FAVOR;
    private float energy;
    private float maxEnergy = 140f;
    private float favorMeter;
    private float matchTime;
    private float enemySpawnTimer;
    private float fireCooldown;
    private float chainCooldown;
    private float blessingCooldown;
    private float heroCooldown;
    private boolean victory;
    private String statusCue = "Prepare the vanguard";
    private float statusCueTimer;
    private int allyPrimary;
    private int allyTrim;
    private int allyGlow;
    private int enemyPrimary;
    private int enemyTrim;
    private int enemyGlow;
    private int battlefieldDark;
    private int battlefieldMid;
    private int battlefieldAccent;
    private int flameColor;
    private int stormColor;
    private int vitalColor;
    private int textColor;

    public BattleManager(Context context) {
        this.context = context;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(36f);
        fillPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < floatingTexts.length; i++) {
            floatingTexts[i] = new FloatingText();
        }
        loadColors();
        setupBases();
    }

    public void bindTonePlayer(TonePlayer tonePlayer) {
        this.tonePlayer = tonePlayer;
    }

    private void loadColors() {
        Resources res = context.getResources();
        allyPrimary = ContextCompat.getColor(context, R.color.cst_team_ally_primary);
        allyTrim = ContextCompat.getColor(context, R.color.cst_team_ally_trim);
        allyGlow = ContextCompat.getColor(context, R.color.cst_team_ally_glow);
        enemyPrimary = ContextCompat.getColor(context, R.color.cst_team_enemy_primary);
        enemyTrim = ContextCompat.getColor(context, R.color.cst_team_enemy_trim);
        enemyGlow = ContextCompat.getColor(context, R.color.cst_team_enemy_glow);
        battlefieldDark = ContextCompat.getColor(context, R.color.cst_bg);
        battlefieldMid = ContextCompat.getColor(context, R.color.cst_bg_alt);
        battlefieldAccent = ContextCompat.getColor(context, R.color.cst_panel_alt);
        flameColor = ContextCompat.getColor(context, R.color.cst_flame);
        stormColor = ContextCompat.getColor(context, R.color.cst_storm);
        vitalColor = ContextCompat.getColor(context, R.color.cst_vital);
        textColor = ContextCompat.getColor(context, R.color.cst_text_primary);
    }

    private void setupBases() {
        playerBase = new BaseCore(Team.ALLY, 130f, 240f, 240f, 1900f);
        enemyBase = new BaseCore(Team.ENEMY, FIELD_WIDTH - 130f, 240f, 252f, 2100f);
    }

    public void startNewMatch() {
        allies.clear();
        enemies.clear();
        projectiles.clear();
        spellEvents.clear();
        stats.reset();
        setupBases();
        warChampion = new WarChampion(Team.ALLY, activeFavor);
        placeUnit(warChampion, PLAYER_SPAWN_X + 90f, GROUND_Y + 8f, true);
        allies.add(warChampion);
        energy = 70f;
        favorMeter = 32f;
        matchTime = 0f;
        enemySpawnTimer = 2f;
        fireCooldown = 0f;
        chainCooldown = 0f;
        blessingCooldown = 0f;
        heroCooldown = 0f;
        victory = false;
        phase = BattlePhase.PHASE_ONE;
        statusCue = "Flame opens the assault";
        statusCueTimer = 2.4f;
        state = GameState.PLAYING;
    }

    public void pauseBattle() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resumeBattle() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public void returnToMenu() {
        state = GameState.MENU;
        statusCue = "Menu";
        statusCueTimer = 0f;
    }

    public GameState getState() {
        return state;
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) {
            return;
        }
        matchTime += dt;
        energy = Math.min(maxEnergy, energy + dt * 14f);
        favorMeter = Math.min(100f, favorMeter + dt * 4.4f);
        fireCooldown = Math.max(0f, fireCooldown - dt);
        chainCooldown = Math.max(0f, chainCooldown - dt);
        blessingCooldown = Math.max(0f, blessingCooldown - dt);
        heroCooldown = Math.max(0f, heroCooldown - dt);
        if (statusCueTimer > 0f) {
            statusCueTimer -= dt;
            if (statusCueTimer <= 0f) {
                statusCue = activeFavor.label;
            }
        }
        updatePhase();
        enemyBase.update(dt);
        playerBase.update(dt);
        enemySpawnTimer -= dt;
        if (enemySpawnTimer <= 0f) {
            spawnEnemyWave();
        }
        for (Unit unit : allies) {
            applyVitalFavor(unit, dt);
            unit.update(this, dt);
        }
        for (Unit unit : enemies) {
            unit.update(this, dt);
        }
        updateProjectiles(dt);
        updateSpellEvents(dt);
        cleanupUnits(allies, true);
        cleanupUnits(enemies, false);
        particles.update(dt);
        for (FloatingText text : floatingTexts) {
            text.update(dt);
        }
        screenShake.update(dt);
        if (enemyBase.hp <= 0f || playerBase.hp <= 0f) {
            finishMatch(enemyBase.hp <= 0f);
        }
    }

    private void updatePhase() {
        BattlePhase next = phase;
        if (matchTime > 88f || enemyBase.hp < enemyBase.maxHp * 0.26f) {
            next = BattlePhase.PHASE_FIVE;
        } else if (matchTime > 66f || enemyBase.hp < enemyBase.maxHp * 0.45f) {
            next = BattlePhase.PHASE_FOUR;
        } else if (matchTime > 46f || enemyBase.hp < enemyBase.maxHp * 0.62f) {
            next = BattlePhase.PHASE_THREE;
        } else if (matchTime > 24f || enemyBase.hp < enemyBase.maxHp * 0.82f) {
            next = BattlePhase.PHASE_TWO;
        }
        if (next != phase) {
            phase = next;
            statusCue = phase.label;
            statusCueTimer = 2f;
            particles.burst(FIELD_WIDTH * 0.5f, GROUND_Y - 120f, currentFavorColor(), 10, 120f, true);
        }
    }

    private void spawnEnemyWave() {
        switch (phase) {
            case PHASE_ONE:
                spawnEnemy(new Militia(Team.ENEMY, RuneFavor.FLAME_FAVOR));
                if (matchTime > 12f) {
                    spawnEnemy(new Archer(Team.ENEMY, RuneFavor.STORM_FAVOR));
                }
                enemySpawnTimer = 3.2f;
                break;
            case PHASE_TWO:
                spawnEnemy(new Militia(Team.ENEMY, RuneFavor.FLAME_FAVOR));
                spawnEnemy(new Archer(Team.ENEMY, RuneFavor.STORM_FAVOR));
                enemySpawnTimer = 2.7f;
                break;
            case PHASE_THREE:
                spawnEnemy(new Knight(Team.ENEMY, RuneFavor.FLAME_FAVOR));
                spawnEnemy(new Archer(Team.ENEMY, RuneFavor.STORM_FAVOR));
                enemySpawnTimer = 2.5f;
                break;
            case PHASE_FOUR:
                spawnEnemy(new Knight(Team.ENEMY, RuneFavor.STORM_FAVOR));
                spawnEnemy(new Priest(Team.ENEMY, RuneFavor.VITAL_FAVOR));
                spawnEnemy(new Militia(Team.ENEMY, RuneFavor.FLAME_FAVOR));
                enemySpawnTimer = 2.1f;
                break;
            case PHASE_FIVE:
                spawnEnemy(new Titan(Team.ENEMY, RuneFavor.FLAME_FAVOR));
                spawnEnemy(new Knight(Team.ENEMY, RuneFavor.STORM_FAVOR));
                spawnEnemy(new Priest(Team.ENEMY, RuneFavor.VITAL_FAVOR));
                enemySpawnTimer = 3.8f;
                break;
        }
    }

    private void spawnEnemy(Unit unit) {
        placeUnit(unit, ENEMY_SPAWN_X - enemies.size() * 12f, GROUND_Y + ((enemies.size() % 3) - 1) * 10f, false);
        enemies.add(unit);
    }

    private void finishMatch(boolean playerWon) {
        victory = playerWon;
        state = GameState.GAME_OVER;
        statusCue = playerWon ? "Fortress shattered" : "Castle fallen";
        statusCueTimer = 99f;
    }

    private void updateProjectiles(float dt) {
        for (Projectile projectile : projectiles) {
            projectile.update(dt);
            if (!projectile.active) {
                continue;
            }
            if (projectile.heal) {
                List<Unit> list = projectile.team == Team.ALLY ? allies : enemies;
                for (Unit unit : list) {
                    if (!unit.dead && Math.abs(unit.x - projectile.x) < 40f && Math.abs(unit.y - projectile.y) < 60f) {
                        unit.heal(projectile.damage);
                        projectile.active = false;
                        showText("+" + ((int) projectile.damage), unit.x, unit.y - unit.bodyHeight, vitalColor);
                        particles.burst(unit.x, unit.y - unit.bodyHeight * 0.7f, vitalColor, 6, 70f, false);
                        break;
                    }
                }
            } else {
                List<Unit> list = projectile.team == Team.ALLY ? enemies : allies;
                for (Unit unit : list) {
                    if (!unit.dead && Math.abs(unit.x - projectile.x) < 42f && Math.abs(unit.y - projectile.y) < 70f) {
                        dealDamage(unit, projectile.damage, projectile.color, false);
                        projectile.active = false;
                        break;
                    }
                }
            }
        }
    }

    private void updateSpellEvents(float dt) {
        Iterator<SpellEvent> iterator = spellEvents.iterator();
        while (iterator.hasNext()) {
            SpellEvent event = iterator.next();
            event.timer -= dt;
            if (event.timer <= 0f) {
                if (event.kind == ACTION_FIRE_RAIN) {
                    impactFireRain(event);
                } else if (event.kind == ACTION_CHAIN_BOLT) {
                    impactChainBolt(event);
                } else if (event.kind == ACTION_BLESSING_WAVE) {
                    impactBlessingWave(event);
                } else if (event.kind == ACTION_HERO_ABILITY) {
                    impactHeroAbility(event);
                }
                iterator.remove();
            }
        }
    }

    private void cleanupUnits(List<Unit> list, boolean allyList) {
        Iterator<Unit> iterator = list.iterator();
        while (iterator.hasNext()) {
            Unit unit = iterator.next();
            if (unit.isGone() && !unit.hero) {
                if (!allyList) {
                    stats.enemiesDefeated++;
                    favorMeter = Math.min(100f, favorMeter + 4f);
                }
                iterator.remove();
            }
        }
    }

    private void applyVitalFavor(Unit unit, float dt) {
        if (unit.team == Team.ALLY && activeFavor == RuneFavor.VITAL_FAVOR && !unit.dead) {
            unit.heal(dt * (unit.support ? 5f : 3.2f));
        }
    }

    public boolean performAction(int action) {
        if (state != GameState.PLAYING) {
            return false;
        }
        if (action >= SUMMON_MILITIA && action <= SUMMON_TITAN) {
            return summonUnit(action);
        }
        if (action == ACTION_FIRE_RAIN && fireCooldown <= 0f) {
            fireCooldown = 12f;
            stats.spellsCast++;
            spellEvents.add(new SpellEvent(ACTION_FIRE_RAIN, 0.65f, enemyFrontX(), 0f));
            spellFlash("Fire Rain incoming");
            return true;
        }
        if (action == ACTION_CHAIN_BOLT && chainCooldown <= 0f) {
            chainCooldown = 10f;
            stats.spellsCast++;
            spellEvents.add(new SpellEvent(ACTION_CHAIN_BOLT, 0.35f, warChampion.x + 60f, 0f));
            spellFlash("Chain Bolt primed");
            return true;
        }
        if (action == ACTION_BLESSING_WAVE && blessingCooldown <= 0f) {
            blessingCooldown = 14f;
            stats.spellsCast++;
            spellEvents.add(new SpellEvent(ACTION_BLESSING_WAVE, 0.45f, alliedFrontX(), 0f));
            spellFlash("Blessing Wave rising");
            return true;
        }
        if (action == ACTION_HERO_ABILITY && heroCooldown <= 0f && warChampion != null) {
            heroCooldown = activeFavor == RuneFavor.STORM_FAVOR ? 7.5f : 9.5f;
            spellEvents.add(new SpellEvent(ACTION_HERO_ABILITY, 0.28f, warChampion.x, warChampion.y));
            statusCue = warChampion.name + " surges";
            statusCueTimer = 1.4f;
            if (tonePlayer != null) {
                tonePlayer.playSpell();
            }
            particles.burst(warChampion.x, warChampion.y - warChampion.bodyHeight * 0.6f, currentFavorColor(), 8, 110f, true);
            return true;
        }
        return false;
    }

    private boolean summonUnit(int action) {
        int cost = summonCost(action);
        if (energy < cost) {
            statusCue = "Need more energy";
            statusCueTimer = 1f;
            return false;
        }
        energy -= cost;
        Unit unit;
        if (action == SUMMON_ARCHER) {
            unit = new Archer(Team.ALLY, activeFavor);
        } else if (action == SUMMON_KNIGHT) {
            unit = new Knight(Team.ALLY, activeFavor);
        } else if (action == SUMMON_PRIEST) {
            unit = new Priest(Team.ALLY, activeFavor);
        } else if (action == SUMMON_TITAN) {
            unit = new Titan(Team.ALLY, activeFavor);
        } else {
            unit = new Militia(Team.ALLY, activeFavor);
        }
        placeUnit(unit, PLAYER_SPAWN_X + allies.size() * 10f, GROUND_Y + ((allies.size() % 3) - 1) * 10f, true);
        allies.add(unit);
        stats.unitsSummoned++;
        favorMeter = Math.min(100f, favorMeter + 3f);
        statusCue = unit.name + " joins under " + activeFavor.label;
        statusCueTimer = 1.2f;
        particles.burst(unit.x, unit.y - unit.bodyHeight * 0.4f, currentFavorColor(), 6, 90f, false);
        return true;
    }

    private int summonCost(int action) {
        if (action == SUMMON_ARCHER) {
            return 24;
        }
        if (action == SUMMON_KNIGHT) {
            return 42;
        }
        if (action == SUMMON_PRIEST) {
            return 38;
        }
        if (action == SUMMON_TITAN) {
            return 84;
        }
        return 16;
    }

    public void selectFavor(RuneFavor favor) {
        activeFavor = favor;
        statusCue = favor.label + " active";
        statusCueTimer = 1.2f;
        particles.burst(alliedFrontX(), GROUND_Y - 160f, currentFavorColor(), 10, 120f, true);
    }

    public HudSnapshot createSnapshot() {
        HudSnapshot snapshot = new HudSnapshot();
        snapshot.playerHpText = (int) playerBase.hp + " / " + (int) playerBase.maxHp;
        snapshot.enemyHpText = (int) enemyBase.hp + " / " + (int) enemyBase.maxHp;
        snapshot.energyText = (int) energy + " / " + (int) maxEnergy;
        snapshot.activeFavorText = activeFavor.label;
        snapshot.favorMeterText = (int) favorMeter + "%";
        snapshot.heroCooldownText = coolText(heroCooldown);
        snapshot.fireCooldownText = coolText(fireCooldown);
        snapshot.chainCooldownText = coolText(chainCooldown);
        snapshot.blessingCooldownText = coolText(blessingCooldown);
        snapshot.phaseText = phase.label;
        snapshot.statusText = statusCue;
        snapshot.playerHpPercent = (int) (playerBase.hp / playerBase.maxHp * 100f);
        snapshot.enemyHpPercent = (int) (enemyBase.hp / enemyBase.maxHp * 100f);
        snapshot.energyPercent = (int) (energy / maxEnergy * 100f);
        snapshot.state = state;
        snapshot.victory = victory;
        snapshot.stats = stats;
        return snapshot;
    }

    private String coolText(float value) {
        return value <= 0f ? "Ready" : String.format(Locale.US, "%.1fs", value);
    }

    public void render(Canvas canvas, int width, int height) {
        canvas.drawColor(battlefieldDark);
        fillPaint.setShader(new LinearGradient(0f, 0f, 0f, height, battlefieldMid, battlefieldDark, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, fillPaint);
        fillPaint.setShader(null);
        float scale = width / FIELD_WIDTH;
        float sceneHeight = GROUND_Y + 180f;
        float scaleY = height / sceneHeight;
        float sx = screenShake.offsetX();
        float sy = screenShake.offsetY();
        canvas.save();
        canvas.translate(sx, sy);
        canvas.scale(scale, scaleY);
        drawBackdrop(canvas);
        playerBase.render(canvas, fillPaint, strokePaint, GROUND_Y, allyPrimary, allyTrim);
        enemyBase.render(canvas, fillPaint, strokePaint, GROUND_Y, enemyPrimary, enemyTrim);
        for (Unit unit : allies) {
            unit.render(canvas, fillPaint, strokePaint);
        }
        for (Unit unit : enemies) {
            unit.render(canvas, fillPaint, strokePaint);
        }
        for (Projectile projectile : projectiles) {
            projectile.render(canvas, fillPaint);
        }
        particles.render(canvas, fillPaint);
        drawFloatingTexts(canvas);
        canvas.restore();
    }

    private void drawBackdrop(Canvas canvas) {
        fillPaint.setColor(battlefieldAccent);
        RectF hillA = new RectF(120f, 520f, 1100f, 920f);
        RectF hillB = new RectF(980f, 480f, 2120f, 920f);
        canvas.drawOval(hillA, fillPaint);
        canvas.drawOval(hillB, fillPaint);
        fillPaint.setColor(Color.argb(255, 35, 63, 74));
        RectF ground = new RectF(0f, GROUND_Y - 34f, FIELD_WIDTH, GROUND_Y + 180f);
        canvas.drawRect(ground, fillPaint);
        fillPaint.setColor(Color.argb(255, 53, 85, 49));
        for (int i = 0; i < 18; i++) {
            float x = 40f + i * 136f;
            RectF patch = new RectF(x, GROUND_Y - 26f + (i % 3) * 6f, x + 68f, GROUND_Y + 10f + (i % 3) * 6f);
            canvas.drawOval(patch, fillPaint);
        }
    }

    private void drawFloatingTexts(Canvas canvas) {
        textPaint.setColor(textColor);
        textPaint.setTextSize(34f);
        for (FloatingText floatingText : floatingTexts) {
            floatingText.render(canvas, textPaint);
        }
    }

    public Unit findTarget(Unit source) {
        List<Unit> list = source.team == Team.ALLY ? enemies : allies;
        Unit best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Unit unit : list) {
            if (unit.dead) {
                continue;
            }
            float dx = unit.x - source.x;
            if (source.team == Team.ALLY && dx < -50f) {
                continue;
            }
            if (source.team == Team.ENEMY && dx > 50f) {
                continue;
            }
            float distance = Math.abs(dx);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = unit;
            }
        }
        return best;
    }

    public Unit findWoundedAlly(Unit source) {
        List<Unit> list = source.team == Team.ALLY ? allies : enemies;
        Unit best = null;
        float bestScore = 0f;
        for (Unit unit : list) {
            if (unit.dead || unit == source) {
                continue;
            }
            float missing = unit.maxHp - unit.hp;
            if (missing > bestScore) {
                bestScore = missing;
                best = unit;
            }
        }
        return best;
    }

    public BaseCore getEnemyBase(Team team) {
        return team == Team.ALLY ? enemyBase : playerBase;
    }

    public void spawnProjectile(Unit source, Unit target) {
        Projectile projectile = obtainProjectile();
        float travel = source.team == Team.ALLY ? 330f : -330f;
        if (source.favorAtSpawn == RuneFavor.STORM_FAVOR) {
            travel *= 1.25f;
        }
        projectile.launch(source.team, source.x, source.y - source.bodyHeight * 0.52f, travel, -44f, source.damage, 2.1f, false, source.trimColor);
        particles.burst(source.x + (source.team == Team.ALLY ? 18f : -18f), source.y - source.bodyHeight * 0.52f, stormColor, 3, 60f, false);
    }

    public void releaseSupportPulse(Unit source, Unit target) {
        Projectile projectile = obtainProjectile();
        float travel = source.team == Team.ALLY ? 220f : -220f;
        float amount = source.damage + (source.favorAtSpawn == RuneFavor.VITAL_FAVOR ? 18f : 10f);
        projectile.launch(source.team, source.x, source.y - source.bodyHeight * 0.7f, travel, -20f, amount, 2f, true, vitalColor);
        particles.burst(source.x, source.y - source.bodyHeight * 0.72f, vitalColor, 4, 54f, false);
    }

    public void applyMeleeImpact(Unit source, Unit target) {
        float amount = source.damage;
        if (source.favorAtSpawn == RuneFavor.FLAME_FAVOR && !source.ranged && !source.support) {
            amount *= 1.18f;
            particles.burst(target.x, target.y - target.bodyHeight * 0.45f, flameColor, 8, 100f, false);
        }
        if (source.hero) {
            particles.burst(target.x, target.y - target.bodyHeight * 0.4f, currentFavorColor(), 7, 110f, false);
            screenShake.trigger(0.18f, 8f);
        }
        if (source.titan) {
            amount *= 1.25f;
            screenShake.trigger(0.22f, 10f);
        }
        dealDamage(target, amount, source.trimColor, source.titan || source.hero);
        favorMeter = Math.min(100f, favorMeter + 1.5f);
        if (tonePlayer != null) {
            tonePlayer.playImpact();
        }
    }

    public void applyBaseImpact(Unit source, BaseCore base) {
        float amount = source.damage * (source.titan ? 1.6f : 0.8f);
        if (source.hero) {
            amount *= 1.2f;
        }
        base.damage(amount);
        stats.fortressDamage += source.team == Team.ALLY ? (int) amount : 0;
        screenShake.trigger(0.2f, source.titan || source.hero ? 12f : 6f);
        particles.burst(base.x, GROUND_Y - base.height * 0.5f, source.trimColor, 8, 120f, false);
        showText(String.valueOf((int) amount), base.x, GROUND_Y - base.height, source.trimColor);
        if (tonePlayer != null) {
            tonePlayer.playImpact();
        }
    }

    private void dealDamage(Unit target, float amount, int color, boolean heavy) {
        target.damage(amount);
        particles.burst(target.x, target.y - target.bodyHeight * 0.4f, color, heavy ? 10 : 6, heavy ? 130f : 90f, false);
        showText(String.valueOf((int) amount), target.x, target.y - target.bodyHeight, color);
        if (heavy) {
            screenShake.trigger(0.12f, 7f);
        }
    }

    private void impactFireRain(SpellEvent event) {
        float radius = activeFavor == RuneFavor.FLAME_FAVOR ? 220f : 170f;
        for (Unit unit : enemies) {
            if (!unit.dead && Math.abs(unit.x - event.x) < radius) {
                dealDamage(unit, 48f + (activeFavor == RuneFavor.FLAME_FAVOR ? 18f : 0f), flameColor, true);
            }
        }
        particles.burst(event.x, GROUND_Y - 160f, flameColor, 18, 150f, false);
        particles.burst(event.x, GROUND_Y - 150f, flameColor, 3, 80f, true);
        screenShake.trigger(0.22f, 9f);
    }

    private void impactChainBolt(SpellEvent event) {
        int chains = activeFavor == RuneFavor.STORM_FAVOR ? 5 : 3;
        int hits = 0;
        for (Unit unit : enemies) {
            if (!unit.dead && hits < chains) {
                dealDamage(unit, 34f + hits * 5f, stormColor, false);
                particles.burst(unit.x, unit.y - unit.bodyHeight * 0.6f, stormColor, 7, 90f, false);
                hits++;
            }
        }
        particles.burst(event.x, warChampion.y - warChampion.bodyHeight * 0.66f, stormColor, 5, 100f, true);
    }

    private void impactBlessingWave(SpellEvent event) {
        float amount = activeFavor == RuneFavor.VITAL_FAVOR ? 42f : 28f;
        for (Unit unit : allies) {
            if (!unit.dead && Math.abs(unit.x - event.x) < 260f) {
                unit.heal(amount);
                unit.sustainTimer = activeFavor == RuneFavor.VITAL_FAVOR ? 5f : 3f;
                showText("+" + ((int) amount), unit.x, unit.y - unit.bodyHeight, vitalColor);
            }
        }
        particles.burst(event.x, GROUND_Y - 130f, vitalColor, 5, 80f, true);
        particles.burst(event.x, GROUND_Y - 130f, vitalColor, 10, 95f, false);
    }

    private void impactHeroAbility(SpellEvent event) {
        if (warChampion == null) {
            return;
        }
        if (activeFavor == RuneFavor.FLAME_FAVOR) {
            for (Unit unit : enemies) {
                if (!unit.dead && Math.abs(unit.x - warChampion.x) < 170f) {
                    dealDamage(unit, 66f, flameColor, true);
                }
            }
            particles.burst(warChampion.x + 120f, warChampion.y - warChampion.bodyHeight * 0.55f, flameColor, 15, 140f, false);
            screenShake.trigger(0.26f, 12f);
        } else if (activeFavor == RuneFavor.STORM_FAVOR) {
            int hits = 0;
            for (Unit unit : enemies) {
                if (!unit.dead && hits < 4) {
                    dealDamage(unit, 38f, stormColor, false);
                    particles.burst(unit.x, unit.y - unit.bodyHeight * 0.6f, stormColor, 8, 100f, false);
                    hits++;
                }
            }
        } else {
            for (Unit unit : allies) {
                if (!unit.dead && Math.abs(unit.x - warChampion.x) < 220f) {
                    unit.heal(42f);
                    unit.sustainTimer = 4.4f;
                    showText("+42", unit.x, unit.y - unit.bodyHeight, vitalColor);
                }
            }
            particles.burst(warChampion.x, warChampion.y - warChampion.bodyHeight * 0.6f, vitalColor, 7, 100f, true);
        }
    }

    private void spellFlash(String text) {
        statusCue = text;
        statusCueTimer = 1.2f;
        particles.burst(enemyFrontX(), GROUND_Y - 180f, currentFavorColor(), 8, 120f, true);
        if (tonePlayer != null) {
            tonePlayer.playSpell();
        }
    }

    private void placeUnit(Unit unit, float x, float y, boolean ally) {
        unit.x = x;
        unit.y = y;
        if (ally) {
            unit.primaryColor = allyPrimary;
            unit.trimColor = colorForFavor(unit.favorAtSpawn, allyTrim);
            unit.glowColor = allyGlow;
        } else {
            unit.primaryColor = enemyPrimary;
            unit.trimColor = colorForFavor(unit.favorAtSpawn, enemyTrim);
            unit.glowColor = enemyGlow;
        }
    }

    private int colorForFavor(RuneFavor favor, int fallback) {
        if (favor == RuneFavor.FLAME_FAVOR) {
            return flameColor;
        }
        if (favor == RuneFavor.STORM_FAVOR) {
            return stormColor;
        }
        if (favor == RuneFavor.VITAL_FAVOR) {
            return vitalColor;
        }
        return fallback;
    }

    private Projectile obtainProjectile() {
        for (Projectile projectile : projectiles) {
            if (!projectile.active) {
                return projectile;
            }
        }
        Projectile projectile = new Projectile();
        projectiles.add(projectile);
        return projectile;
    }

    private void showText(String text, float x, float y, int color) {
        for (FloatingText floatingText : floatingTexts) {
            if (!floatingText.active) {
                floatingText.show(text, x, y, color);
                return;
            }
        }
        floatingTexts[0].show(text, x, y, color);
    }

    private float enemyFrontX() {
        float x = enemyBase.x - 220f;
        for (Unit unit : enemies) {
            if (!unit.dead) {
                x = Math.min(x, unit.x);
            }
        }
        return x;
    }

    private float alliedFrontX() {
        float x = playerBase.x + 220f;
        for (Unit unit : allies) {
            if (!unit.dead) {
                x = Math.max(x, unit.x);
            }
        }
        return x;
    }

    private int currentFavorColor() {
        return colorForFavor(activeFavor, allyTrim);
    }

    public static class HudSnapshot {
        public String playerHpText;
        public String enemyHpText;
        public String energyText;
        public String activeFavorText;
        public String favorMeterText;
        public String heroCooldownText;
        public String fireCooldownText;
        public String chainCooldownText;
        public String blessingCooldownText;
        public String phaseText;
        public String statusText;
        public int playerHpPercent;
        public int enemyHpPercent;
        public int energyPercent;
        public GameState state;
        public boolean victory;
        public MatchStats stats;
    }

    private static class SpellEvent {
        public final int kind;
        public float timer;
        public final float x;
        public final float y;

        SpellEvent(int kind, float timer, float x, float y) {
            this.kind = kind;
            this.timer = timer;
            this.x = x;
            this.y = y;
        }
    }
}
