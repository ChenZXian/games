package com.android.boot.core;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.entity.Archer;
import com.android.boot.entity.BaseCore;
import com.android.boot.entity.Behemoth;
import com.android.boot.entity.BlackKnight;
import com.android.boot.entity.ChaosMage;
import com.android.boot.entity.Cleric;
import com.android.boot.entity.CultPriest;
import com.android.boot.entity.DarkArcher;
import com.android.boot.entity.Footman;
import com.android.boot.entity.Knight;
import com.android.boot.entity.Marauder;
import com.android.boot.entity.OathChampion;
import com.android.boot.entity.Projectile;
import com.android.boot.entity.Team;
import com.android.boot.entity.Titan;
import com.android.boot.entity.Unit;
import com.android.boot.entity.WarMage;
import com.android.boot.fx.FloatingText;
import com.android.boot.fx.Particle;
import com.android.boot.fx.ParticleSystem;
import com.android.boot.fx.ScreenShake;

import java.util.ArrayList;
import java.util.List;

public class BattleManager {
    public interface BattleEvents {
        void onHudChanged();
        void onBattleEnded(boolean victory, MatchStats stats);
    }

    private final Context context;
    private final ArrayList<Unit> allies = new ArrayList<>();
    private final ArrayList<Unit> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ParticleSystem particleSystem = new ParticleSystem(160, 48);
    private final ScreenShake screenShake = new ScreenShake();
    private final MatchStats matchStats = new MatchStats();
    private final OathPowerMeter oathPowerMeter = new OathPowerMeter(100f);
    private final ArrayList<ChapterData> chapters = new ArrayList<>();
    private final BaseCore allyCastle = new BaseCore(Team.ALLY, 70f, 120f, 1200);
    private final BaseCore enemyStronghold = new BaseCore(Team.ENEMY, 0f, 140f, 1400);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private BattleEvents battleEvents;
    private ChapterData currentChapter;
    private GameState gameState = GameState.MENU;
    private BattlePhase battlePhase = BattlePhase.OPENING;
    private RoyalOath activeOath = RoyalOath.EMBER;
    private OathChampion oathChampion;
    private int width;
    private int height;
    private float energy;
    private float time;
    private float enemySpawnTimer;
    private float heroSkillCooldown;
    private float meteorCooldown;
    private float chainCooldown;
    private float holyCooldown;
    private float chapterPressure;
    private String statusText = "Form the line";
    private boolean battleResultSent;
    private float hudRefreshTimer;
    private int colorAllyBody;
    private int colorAllyAccent;
    private int colorEnemyBody;
    private int colorEnemyAccent;
    private int colorDanger;
    private int colorSuccess;
    private int colorWarning;
    private int colorAccent;
    private int colorAccentTwo;
    private int colorText;
    private int colorBackground;

    public BattleManager(Context context) {
        this.context = context;
        loadColors();
        buildChapters();
        for (int i = 0; i < 32; i++) {
            projectiles.add(new Projectile());
        }
    }

    private void loadColors() {
        Resources resources = context.getResources();
        colorAllyBody = ContextCompat.getColor(context, R.color.cst_panel_header_bg);
        colorAllyAccent = ContextCompat.getColor(context, R.color.cst_accent);
        colorEnemyBody = ContextCompat.getColor(context, R.color.cst_danger);
        colorEnemyAccent = ContextCompat.getColor(context, R.color.cst_warning);
        colorDanger = ContextCompat.getColor(context, R.color.cst_danger);
        colorSuccess = ContextCompat.getColor(context, R.color.cst_success);
        colorWarning = ContextCompat.getColor(context, R.color.cst_warning);
        colorAccent = ContextCompat.getColor(context, R.color.cst_accent);
        colorAccentTwo = ContextCompat.getColor(context, R.color.cst_accent_2);
        colorText = ContextCompat.getColor(context, R.color.cst_text_primary);
        colorBackground = ContextCompat.getColor(context, R.color.cst_bg_main);
        paint.setStrokeWidth(resources.getDisplayMetrics().density * 2f);
    }

    private void buildChapters() {
        chapters.add(new ChapterData(1, "Chapter 1: Border Clash", "Border Clash", "Hold the frontier road", "Marauder swarms with light archer cover", "Open with Footmen and Archers, then swap to Ember when melee collisions start", 1400, 4.2f, 1.0f, 1));
        chapters.add(new ChapterData(2, "Chapter 2: Ash Valley", "Ash Valley", "Smoke over the lava trench", "Black Knights and fire cult pressure the line", "Use Ember for impact pushes, then Sanctum to recover after the knight rush", 1600, 3.8f, 1.08f, 2));
        chapters.add(new ChapterData(3, "Chapter 3: Storm Pass", "Storm Pass", "Thunder along the ridge", "Dark Archers and Chaos Mages layer ranged volleys", "Rotate Storm often to speed arrows and chain bursts through clustered casters", 1760, 3.45f, 1.15f, 3));
        chapters.add(new ChapterData(4, "Chapter 4: Sacred Ruins", "Sacred Ruins", "Shattered oath stones hum", "Cult Priests shield Behemoth escorts and revive pressure", "Sanctum keeps the front alive, then switch into Ember for siege windows", 1950, 3.2f, 1.24f, 4));
        chapters.add(new ChapterData(5, "Chapter 5: Final Siege", "Final Siege", "The grand fortress breaks the sky", "Every enemy role appears with siege heavy tempo", "Cycle all three doctrines and reserve Titan plus hero skill for the fortress collapse", 2200, 2.9f, 1.34f, 5));
    }

    public List<ChapterData> getChapters() {
        return chapters;
    }

    public void setBattleEvents(BattleEvents battleEvents) {
        this.battleEvents = battleEvents;
    }

    public void setViewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void startChapter(int chapterIndex) {
        currentChapter = chapters.get(Math.max(0, Math.min(chapterIndex - 1, chapters.size() - 1)));
        allies.clear();
        enemies.clear();
        for (Projectile projectile : projectiles) {
            projectile.active = false;
        }
        energy = 35f;
        time = 0f;
        enemySpawnTimer = 1.8f;
        heroSkillCooldown = 0f;
        meteorCooldown = 0f;
        chainCooldown = 0f;
        holyCooldown = 0f;
        chapterPressure = 0f;
        statusText = currentChapter.subtitle;
        battleState(GameState.PLAYING);
        battlePhase = BattlePhase.OPENING;
        activeOath = RoyalOath.EMBER;
        battleResultSent = false;
        hudRefreshTimer = 0f;
        matchStats.reset();
        oathPowerMeter.reset();
        allyCastle.reset(1200 + currentChapter.difficulty * 80);
        enemyStronghold.reset(currentChapter.enemyStrongholdHp);
        oathChampion = new OathChampion(180f, battleLaneY());
        oathChampion.setOath(activeOath);
        allies.add(oathChampion);
        if (battleEvents != null) {
            battleEvents.onHudChanged();
        }
    }

    private void battleState(GameState nextState) {
        gameState = nextState;
    }

    public void pause() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED;
        }
    }

    public void resume() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public void update(float dt) {
        if (gameState != GameState.PLAYING || currentChapter == null) {
            return;
        }
        time += dt;
        energy = Math.min(100f, energy + dt * 12f);
        oathPowerMeter.add(dt * 5f + allies.size() * 0.02f);
        heroSkillCooldown = Math.max(0f, heroSkillCooldown - dt);
        meteorCooldown = Math.max(0f, meteorCooldown - dt);
        chainCooldown = Math.max(0f, chainCooldown - dt);
        holyCooldown = Math.max(0f, holyCooldown - dt);
        chapterPressure += dt * currentChapter.phaseRate;
        updatePhase();
        enemySpawnTimer -= dt;
        if (enemySpawnTimer <= 0f) {
            spawnEnemyWave();
            enemySpawnTimer = Math.max(1.5f, currentChapter.spawnInterval - chapterPressure * 0.16f);
        }
        float allyFront = frontLine(allies, 160f);
        float enemyFront = frontLine(enemies, width - 170f);
        updateUnits(allies, enemies, allyFront, enemyFront, dt);
        updateUnits(enemies, allies, enemyFront, allyFront, dt);
        updateProjectiles(dt);
        updateBases(dt);
        cleanupUnits(allies, true);
        cleanupUnits(enemies, false);
        particleSystem.update(dt);
        screenShake.update(dt);
        hudRefreshTimer -= dt;
        if (battleEvents != null && hudRefreshTimer <= 0f) {
            hudRefreshTimer = 0.12f;
            battleEvents.onHudChanged();
        }
        checkBattleEnd();
    }

    private void updatePhase() {
        if (time > 95f || enemyStronghold.getHp() < enemyStronghold.getMaxHp() * 0.35f) {
            battlePhase = BattlePhase.FINALE;
        } else if (time > 65f || enemyStronghold.getHp() < enemyStronghold.getMaxHp() * 0.55f) {
            battlePhase = BattlePhase.SIEGE;
        } else if (time > 30f || enemyStronghold.getHp() < enemyStronghold.getMaxHp() * 0.78f) {
            battlePhase = BattlePhase.PRESSURE;
        } else {
            battlePhase = BattlePhase.OPENING;
        }
    }

    private void updateUnits(ArrayList<Unit> own, ArrayList<Unit> other, float ownFront, float otherFront, float dt) {
        for (Unit unit : own) {
            Unit target = findTarget(unit, other);
            unit.setOath(activeOath);
            unit.update(dt, target, unit.getTeam() == Team.ALLY ? ownFront : otherFront);
            if (unit.shouldImpact()) {
                if (unit.isSupport()) {
                    applySupportPulse(unit, own);
                } else if (target != null) {
                    float damage = unit.getDamageAgainst(target);
                    target.damage(damage);
                    unit.markImpact();
                    oathPowerMeter.add(4f);
                    particleSystem.spawnBurst(target.getX(), target.getY() - 40f, unit.getTeam() == Team.ALLY ? colorAccent : colorDanger, unit.isSiege() || unit.isHero() ? 12 : 6, unit.isSiege() ? 120f : 80f);
                    particleSystem.spawnText(target.getX(), target.getY() - 60f, String.valueOf(Math.round(damage)), colorWarning);
                    if (unit.isSiege() || unit.isHero()) {
                        screenShake.trigger(8f, 0.18f);
                    }
                } else {
                    damageBase(unit);
                    unit.markImpact();
                }
            }
            if (unit.shouldReleaseProjectile() && target != null) {
                Projectile projectile = nextProjectile();
                if (projectile != null) {
                    projectile.launch(unit.getX(), unit.getY() - unit.getSize() * 0.35f, unit.getProjectileSpeed(), unit.getDamageAgainst(target), unit.getTeam(), unit.getLabel().contains("Mage"));
                    unit.markProjectileReleased();
                }
            }
        }
    }

    private void applySupportPulse(Unit unit, ArrayList<Unit> alliesToHeal) {
        float total = unit.getHealPower();
        for (Unit ally : alliesToHeal) {
            if (ally.isAlive() && Math.abs(ally.getX() - unit.getX()) < 120f) {
                ally.heal(total * 0.25f);
                particleSystem.spawnText(ally.getX(), ally.getY() - 70f, "+" + Math.round(total * 0.25f), colorSuccess);
            }
        }
        particleSystem.spawnBurst(unit.getX(), unit.getY() - 55f, colorSuccess, 9, 70f);
        oathPowerMeter.add(3f);
        statusText = unit.getLabel() + " pulses sustain";
    }

    private void damageBase(Unit attacker) {
        if (attacker.getTeam() == Team.ALLY) {
            int damage = Math.round(attacker.getDamageAgainst(null) * (attacker.isSiege() ? 1.6f : 0.8f));
            enemyStronghold.damage(damage);
            matchStats.strongholdDamage += damage;
            particleSystem.spawnBurst(width - 120f, battleLaneY() - 50f, colorDanger, 12, 100f);
            statusText = attacker.getLabel() + " breaks the stronghold";
        } else {
            int damage = Math.round(attacker.getDamageAgainst(null) * (attacker.isSiege() ? 1.4f : 0.75f));
            allyCastle.damage(damage);
            particleSystem.spawnBurst(95f, battleLaneY() - 45f, colorWarning, 10, 90f);
            statusText = attacker.getLabel() + " batters the castle";
        }
        if (attacker.isSiege() || attacker.isHero()) {
            screenShake.trigger(10f, 0.22f);
        }
    }

    private void updateProjectiles(float dt) {
        for (Projectile projectile : projectiles) {
            if (!projectile.active) {
                continue;
            }
            projectile.update(dt);
            ArrayList<Unit> targets = projectile.team == Team.ALLY ? enemies : allies;
            for (Unit target : targets) {
                if (target.isAlive() && Math.abs(target.getX() - projectile.x) < target.getSize() * 0.45f) {
                    target.damage(projectile.damage);
                    particleSystem.spawnBurst(target.getX(), target.getY() - 52f, projectile.magical ? colorAccentTwo : colorAccent, projectile.magical ? 9 : 5, projectile.magical ? 100f : 70f);
                    particleSystem.spawnText(target.getX(), target.getY() - 70f, String.valueOf(Math.round(projectile.damage)), projectile.magical ? colorAccentTwo : colorWarning);
                    projectile.active = false;
                    break;
                }
            }
            if (projectile.x < 0f || projectile.x > width) {
                projectile.active = false;
            }
        }
    }

    private void updateBases(float dt) {
        allyCastle.update(dt);
        enemyStronghold.update(dt);
    }

    private void cleanupUnits(ArrayList<Unit> list, boolean alliedList) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Unit unit = list.get(i);
            if (!unit.isAlive() && unit.getHp() <= 0f && unit.getMaxHp() > 0f && unit.getSize() > 0f && unit.getLabel() != null && unit.getSize() < 1000f) {
                if (unit.canBeRemoved()) {
                    if (!alliedList && !unit.isHero()) {
                        matchStats.enemiesDefeated++;
                    }
                    list.remove(i);
                }
            }
        }
    }

    private Unit findTarget(Unit unit, ArrayList<Unit> others) {
        Unit chosen = null;
        float best = Float.MAX_VALUE;
        for (Unit other : others) {
            if (!other.isAlive()) {
                continue;
            }
            float distance = Math.abs(other.getX() - unit.getX());
            boolean inFront = unit.getTeam() == Team.ALLY ? other.getX() >= unit.getX() - 12f : other.getX() <= unit.getX() + 12f;
            if (inFront && distance < best) {
                best = distance;
                chosen = other;
            }
        }
        return chosen;
    }

    private float frontLine(ArrayList<Unit> list, float fallback) {
        float line = fallback;
        if (list.isEmpty()) {
            return fallback;
        }
        if (list.get(0).getTeam() == Team.ALLY) {
            for (Unit unit : list) {
                if (unit.isAlive()) {
                    line = Math.max(line, unit.getX());
                }
            }
        } else {
            for (Unit unit : list) {
                if (unit.isAlive()) {
                    line = Math.min(line, unit.getX());
                }
            }
        }
        return line;
    }

    private Projectile nextProjectile() {
        for (Projectile projectile : projectiles) {
            if (!projectile.active) {
                return projectile;
            }
        }
        return null;
    }

    private void spawnEnemyWave() {
        if (currentChapter == null) {
            return;
        }
        float spawnX = width - 170f;
        int chapter = currentChapter.index;
        spawnEnemy(new Marauder(spawnX, battleLaneY()));
        if (battlePhase != BattlePhase.OPENING || chapter >= 2) {
            spawnEnemy(new DarkArcher(spawnX + 18f, battleLaneY()));
        }
        if (chapter >= 2 && battlePhase != BattlePhase.OPENING) {
            spawnEnemy(new BlackKnight(spawnX + 26f, battleLaneY()));
        }
        if (chapter >= 3) {
            spawnEnemy(new ChaosMage(spawnX + 12f, battleLaneY()));
        }
        if (chapter >= 4 && (battlePhase == BattlePhase.SIEGE || battlePhase == BattlePhase.FINALE)) {
            spawnEnemy(new CultPriest(spawnX + 20f, battleLaneY()));
        }
        if (chapter >= 4 && battlePhase != BattlePhase.OPENING) {
            spawnEnemy(new Behemoth(spawnX + 36f, battleLaneY()));
        }
        if (chapter >= 5) {
            spawnEnemy(new Marauder(spawnX + 44f, battleLaneY()));
            spawnEnemy(new DarkArcher(spawnX + 52f, battleLaneY()));
        }
        statusText = currentChapter.battleTitle + " enemy surge";
    }

    private void spawnEnemy(Unit unit) {
        enemies.add(unit);
    }

    public boolean summonFootman() {
        return summon(new Footman(190f, battleLaneY()), 15);
    }

    public boolean summonArcher() {
        return summon(new Archer(182f, battleLaneY()), 20);
    }

    public boolean summonKnight() {
        return summon(new Knight(180f, battleLaneY()), 35);
    }

    public boolean summonCleric() {
        return summon(new Cleric(176f, battleLaneY()), 25);
    }

    public boolean summonWarMage() {
        return summon(new WarMage(178f, battleLaneY()), 30);
    }

    public boolean summonTitan() {
        return summon(new Titan(170f, battleLaneY()), 60);
    }

    private boolean summon(Unit unit, int cost) {
        if (gameState != GameState.PLAYING || allies.size() >= 16 || energy < cost) {
            return false;
        }
        energy -= cost;
        unit.setOath(activeOath);
        allies.add(unit);
        matchStats.unitsSummoned++;
        oathPowerMeter.add(6f);
        statusText = unit.getLabel() + " answers the " + activeOath.displayName();
        return true;
    }

    public boolean switchOath(RoyalOath oath) {
        if (activeOath == oath || gameState != GameState.PLAYING) {
            return false;
        }
        if (!oathPowerMeter.spendForSwitch()) {
            statusText = "Need Oath Power to switch";
            return false;
        }
        activeOath = oath;
        oathChampion.setOath(activeOath);
        particleSystem.spawnBurst(width * 0.5f, 120f, oath == RoyalOath.EMBER ? colorWarning : oath == RoyalOath.STORM ? colorAccent : colorSuccess, 14, 120f);
        statusText = activeOath.displayName() + " now guides the line";
        return true;
    }

    public boolean castMeteor() {
        if (meteorCooldown > 0f || gameState != GameState.PLAYING) {
            return false;
        }
        meteorCooldown = 16f;
        matchStats.spellsCast++;
        float center = width * 0.72f;
        int damage = activeOath == RoyalOath.EMBER ? 78 : 58;
        for (Unit enemy : enemies) {
            if (enemy.isAlive() && Math.abs(enemy.getX() - center) < 110f) {
                enemy.damage(damage);
            }
        }
        particleSystem.spawnBurst(center, battleLaneY() - 90f, colorWarning, 18, 140f);
        screenShake.trigger(12f, 0.24f);
        statusText = "Meteor Fall crashes down";
        oathPowerMeter.add(8f);
        return true;
    }

    public boolean castChainStorm() {
        if (chainCooldown > 0f || gameState != GameState.PLAYING) {
            return false;
        }
        chainCooldown = 14f;
        matchStats.spellsCast++;
        int jumps = activeOath == RoyalOath.STORM ? 5 : 3;
        int hits = 0;
        float damage = 34f + (activeOath == RoyalOath.STORM ? 6f : 0f);
        for (Unit enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.damage(damage);
                particleSystem.spawnBurst(enemy.getX(), enemy.getY() - 70f, colorAccent, 6, 100f);
                hits++;
                if (hits >= jumps) {
                    break;
                }
            }
        }
        statusText = "Chain Storm arcs through the enemy line";
        oathPowerMeter.add(7f);
        return true;
    }

    public boolean castHolySurge() {
        if (holyCooldown > 0f || gameState != GameState.PLAYING) {
            return false;
        }
        holyCooldown = 15f;
        matchStats.spellsCast++;
        float heal = activeOath == RoyalOath.SANCTUM ? 32f : 20f;
        for (Unit ally : allies) {
            if (ally.isAlive()) {
                ally.heal(heal);
                particleSystem.spawnText(ally.getX(), ally.getY() - 72f, "+" + Math.round(heal), colorSuccess);
            }
        }
        particleSystem.spawnBurst(width * 0.3f, battleLaneY() - 80f, colorSuccess, 16, 120f);
        statusText = "Holy Surge fortifies the war host";
        oathPowerMeter.add(7f);
        return true;
    }

    public boolean triggerHeroSkill() {
        if (heroSkillCooldown > 0f || gameState != GameState.PLAYING) {
            return false;
        }
        heroSkillCooldown = activeOath == RoyalOath.STORM ? 10f : 14f;
        if (activeOath == RoyalOath.EMBER) {
            for (Unit enemy : enemies) {
                if (enemy.isAlive() && enemy.getX() > oathChampion.getX() && enemy.getX() < oathChampion.getX() + 170f) {
                    enemy.damage(58f);
                }
            }
            particleSystem.spawnBurst(oathChampion.getX() + 90f, oathChampion.getY() - 70f, colorWarning, 16, 130f);
            statusText = "Oath Champion unleashes a flame cleave";
        } else if (activeOath == RoyalOath.STORM) {
            int hits = 0;
            for (Unit enemy : enemies) {
                if (enemy.isAlive()) {
                    enemy.damage(36f);
                    particleSystem.spawnBurst(enemy.getX(), enemy.getY() - 70f, colorAccent, 8, 90f);
                    hits++;
                    if (hits >= 4) {
                        break;
                    }
                }
            }
            statusText = "Oath Champion chains rapid strikes";
        } else {
            for (Unit ally : allies) {
                if (ally.isAlive() && Math.abs(ally.getX() - oathChampion.getX()) < 180f) {
                    ally.heal(28f);
                }
            }
            particleSystem.spawnBurst(oathChampion.getX(), oathChampion.getY() - 90f, colorSuccess, 18, 110f);
            statusText = "Oath Champion rallies the host";
        }
        screenShake.trigger(9f, 0.18f);
        oathPowerMeter.add(8f);
        return true;
    }

    private void checkBattleEnd() {
        if (battleResultSent) {
            return;
        }
        if (enemyStronghold.getHp() <= 0) {
            gameState = GameState.GAME_OVER;
            battleResultSent = true;
            if (battleEvents != null) {
                battleEvents.onBattleEnded(true, matchStats);
            }
        } else if (allyCastle.getHp() <= 0) {
            gameState = GameState.GAME_OVER;
            battleResultSent = true;
            if (battleEvents != null) {
                battleEvents.onBattleEnded(false, matchStats);
            }
        }
    }

    public void render(Canvas canvas) {
        if (canvas == null) {
            return;
        }
        float shakeX = screenShake.getOffsetX(time);
        float shakeY = screenShake.getOffsetY(time);
        canvas.drawColor(colorBackground);
        canvas.save();
        canvas.translate(shakeX, shakeY);
        drawBackdrop(canvas);
        drawBases(canvas);
        for (Unit unit : allies) {
            unit.draw(canvas, paint, colorAllyBody, colorAllyAccent, Color.argb(75, 103, 245, 255), time);
        }
        for (Unit unit : enemies) {
            unit.draw(canvas, paint, colorEnemyBody, colorEnemyAccent, Color.argb(70, 255, 95, 117), time);
        }
        drawProjectiles(canvas);
        drawParticles(canvas);
        canvas.restore();
    }

    private void drawBackdrop(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, 9, 17, 31));
        canvas.drawRect(0f, 0f, width, height, paint);
        rect.set(0f, height * 0.52f, width, height);
        paint.setColor(Color.argb(255, 24, 20, 28));
        canvas.drawRect(rect, paint);
        paint.setColor(Color.argb(255, 42, 54, 70));
        for (int i = 0; i < width; i += 120) {
            canvas.drawRect(i, height * 0.69f, i + 60f, height * 0.71f, paint);
        }
        paint.setColor(Color.argb(255, 28, 40, 61));
        path.reset();
        path.moveTo(0f, height * 0.52f);
        path.lineTo(width * 0.15f, height * 0.33f);
        path.lineTo(width * 0.34f, height * 0.49f);
        path.lineTo(width * 0.52f, height * 0.27f);
        path.lineTo(width * 0.72f, height * 0.5f);
        path.lineTo(width, height * 0.34f);
        path.lineTo(width, 0f);
        path.lineTo(0f, 0f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(100, 103, 245, 255));
        canvas.drawCircle(width * 0.75f, height * 0.2f, 42f, paint);
    }

    private void drawBases(Canvas canvas) {
        drawBase(canvas, allyCastle, 40f, colorAllyBody, colorAllyAccent);
        drawBase(canvas, enemyStronghold, width - 130f, colorEnemyBody, colorEnemyAccent);
    }

    private void drawBase(Canvas canvas, BaseCore baseCore, float baseX, int bodyColor, int accentColor) {
        float y = battleLaneY() - 15f;
        float shake = baseCore.getShake() * 8f;
        paint.setColor(bodyColor);
        rect.set(baseX + shake, y - 140f, baseX + 100f + shake, y + 10f);
        canvas.drawRoundRect(rect, 12f, 12f, paint);
        rect.set(baseX - 22f + shake, y - 50f, baseX + 122f + shake, y + 24f);
        canvas.drawRoundRect(rect, 16f, 16f, paint);
        paint.setColor(accentColor);
        rect.set(baseX + 34f + shake, y - 162f, baseX + 66f + shake, y - 18f);
        canvas.drawRoundRect(rect, 10f, 10f, paint);
        path.reset();
        path.moveTo(baseX + 66f + shake, y - 146f);
        path.lineTo(baseX + 118f + shake, y - 126f);
        path.lineTo(baseX + 66f + shake, y - 102f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawProjectiles(Canvas canvas) {
        for (Projectile projectile : projectiles) {
            if (!projectile.active) {
                continue;
            }
            paint.setColor(projectile.magical ? colorAccentTwo : colorText);
            canvas.drawOval(projectile.x - 10f, projectile.y - 6f, projectile.x + 10f, projectile.y + 6f, paint);
        }
    }

    private void drawParticles(Canvas canvas) {
        for (Particle particle : particleSystem.getParticles()) {
            if (particle.active) {
                paint.setColor(particle.color);
                paint.setAlpha((int) (255 * Math.max(0f, particle.life)));
                canvas.drawOval(particle.x - particle.size, particle.y - particle.size, particle.x + particle.size, particle.y + particle.size, paint);
            }
        }
        paint.setAlpha(255);
        paint.setTextSize(28f);
        for (FloatingText text : particleSystem.getTexts()) {
            if (text.active) {
                paint.setColor(text.color);
                paint.setAlpha((int) (255 * Math.max(0f, text.life)));
                canvas.drawText(text.text, text.x, text.y, paint);
            }
        }
        paint.setAlpha(255);
    }

    private float battleLaneY() {
        return height * 0.72f;
    }

    public ChapterData getCurrentChapter() {
        return currentChapter;
    }

    public int getPlayerHp() {
        return allyCastle.getHp();
    }

    public int getPlayerMaxHp() {
        return allyCastle.getMaxHp();
    }

    public int getEnemyHp() {
        return enemyStronghold.getHp();
    }

    public int getEnemyMaxHp() {
        return enemyStronghold.getMaxHp();
    }

    public int getEnergy() {
        return Math.round(energy);
    }

    public int getOathPercent() {
        return oathPowerMeter.getPercent();
    }

    public int getHeroSkillCooldown() {
        return Math.round(heroSkillCooldown);
    }

    public int getMeteorCooldown() {
        return Math.round(meteorCooldown);
    }

    public int getChainCooldown() {
        return Math.round(chainCooldown);
    }

    public int getHolyCooldown() {
        return Math.round(holyCooldown);
    }

    public String getStatusText() {
        return statusText;
    }

    public String getBattlePhaseLabel() {
        if (battlePhase == BattlePhase.OPENING) {
            return "Opening";
        }
        if (battlePhase == BattlePhase.PRESSURE) {
            return "Pressure";
        }
        if (battlePhase == BattlePhase.SIEGE) {
            return "Siege";
        }
        return "Finale";
    }

    public RoyalOath getActiveOath() {
        return activeOath;
    }
}
