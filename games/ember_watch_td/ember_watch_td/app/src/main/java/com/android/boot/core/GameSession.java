package com.android.boot.core;

import android.graphics.RectF;
import android.media.ToneGenerator;
import android.media.AudioManager;
import com.android.boot.data.GameDatabase;
import com.android.boot.entity.BranchType;
import com.android.boot.entity.DamageType;
import com.android.boot.entity.DamageText;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.EnemyTemplate;
import com.android.boot.entity.Hero;
import com.android.boot.entity.LevelConfig;
import com.android.boot.entity.PointF2;
import com.android.boot.entity.Projectile;
import com.android.boot.entity.Skill;
import com.android.boot.entity.Soldier;
import com.android.boot.entity.TargetPriority;
import com.android.boot.entity.Tower;
import com.android.boot.entity.TowerSlot;
import com.android.boot.entity.TowerType;
import com.android.boot.entity.UiActionButton;
import com.android.boot.entity.Wave;
import com.android.boot.entity.WaveSpawn;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GameSession {
    private static class PendingSplit {
        String enemyId;
        float x;
        float y;
        int waypointIndex;
    }

    public final LevelConfig level;
    public final ArrayList<Enemy> enemies = new ArrayList<>();
    public final ArrayList<Projectile> projectiles = new ArrayList<>();
    public final ArrayList<DamageText> damageTexts = new ArrayList<>();
    public final ArrayList<Soldier> soldiers = new ArrayList<>();
    public final ArrayList<UiActionButton> panelButtons = new ArrayList<>();
    public final ArrayList<Skill> skills = new ArrayList<>();
    public final Hero hero = new Hero();
    public GameState state = GameState.MENU;
    public int gold;
    public int life;
    public int currentWaveIndex = -1;
    public float nextWaveTimer;
    public float spawnTimer;
    public int currentSpawnGroup;
    public int currentSpawnCount;
    public boolean waitingForSkillTarget;
    public Skill selectedSkill;
    public TowerSlot selectedSlot;
    public Tower selectedTower;
    public boolean showHowToPlay = true;
    public boolean earlyCallAvailable = true;
    public int earlyCallReward = 25;
    public String banner = "";
    public float bannerTimer;
    public boolean victory;
    public boolean muted;
    public float heroSlashTimer;
    public float heroSlashX;
    public float heroSlashY;
    public float heroSlashDirX = 1f;
    public float heroSlashDirY = 0f;
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);

    public GameSession() {
        this(1);
    }

    public GameSession(int levelIndex) {
        level = GameDatabase.createLevel(levelIndex);
        resetRun();
    }

    public void resetRun() {
        enemies.clear();
        projectiles.clear();
        damageTexts.clear();
        soldiers.clear();
        panelButtons.clear();
        skills.clear();
        gold = level.startGold;
        life = level.startLife;
        currentWaveIndex = -1;
        nextWaveTimer = 1f;
        spawnTimer = 0f;
        currentSpawnGroup = 0;
        currentSpawnCount = 0;
        waitingForSkillTarget = false;
        selectedSkill = null;
        selectedSlot = null;
        selectedTower = null;
        earlyCallAvailable = true;
        earlyCallReward = 25;
        banner = "Hold the pass";
        bannerTimer = 2f;
        victory = false;
        state = GameState.MENU;
        for (TowerSlot slot : level.slots) {
            slot.tower = null;
        }
        hero.x = 150f;
        hero.y = 520f;
        hero.targetX = hero.x;
        hero.targetY = hero.y;
        hero.maxHp = 500f;
        hero.hp = hero.maxHp;
        hero.damage = 34f;
        hero.armor = 18f;
        hero.speed = 120f;
        hero.range = 78f;
        hero.attackTimer = 0f;
        hero.attackInterval = 0.65f;
        hero.respawnTime = 0f;
        hero.dead = false;
        hero.selected = false;
        heroSlashTimer = 0f;
        skills.add(new Skill("reinforce", "Reinforce", 18f, true));
        skills.add(new Skill("meteor", "Meteor", 24f, true));
    }

    public void startGame() {
        resetRun();
        state = GameState.PLAYING;
        startNextWave();
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    private void startNextWave() {
        currentWaveIndex++;
        currentSpawnGroup = 0;
        currentSpawnCount = 0;
        spawnTimer = 0f;
        if (currentWaveIndex < level.waves.size()) {
            Wave wave = level.waves.get(currentWaveIndex);
            banner = wave.bossWave ? "Boss incoming" : "Wave " + (currentWaveIndex + 1) + " " + wave.name;
            bannerTimer = 2.6f;
            nextWaveTimer = wave.prepareTime;
            earlyCallAvailable = true;
        }
    }

    public void earlyCall() {
        if (state != GameState.PLAYING) {
            return;
        }
        if (!earlyCallAvailable) {
            return;
        }
        if (currentWaveIndex >= level.waves.size()) {
            return;
        }
        nextWaveTimer = 0.3f;
        gold += earlyCallReward;
        earlyCallAvailable = false;
        playTone(ToneGenerator.TONE_PROP_ACK);
    }

    public void update(float dt) {
        if (dt > 0.033f) {
            dt = 0.033f;
        }
        if (state != GameState.PLAYING) {
            return;
        }
        if (heroSlashTimer > 0f) {
            heroSlashTimer -= dt;
        }
        if (bannerTimer > 0f) {
            bannerTimer -= dt;
        }
        for (int i = damageTexts.size() - 1; i >= 0; i--) {
            DamageText text = damageTexts.get(i);
            text.ttl -= dt;
            text.y -= dt * 28f;
            if (text.ttl <= 0f) {
                damageTexts.remove(i);
            }
        }
        for (Skill skill : skills) {
            if (skill.timer > 0f) {
                skill.timer -= dt;
            }
        }
        updateWave(dt);
        updateEnemies(dt);
        updateTowers(dt);
        updateProjectiles(dt);
        updateSoldiers(dt);
        updateHero(dt);
        cleanupDead();
        checkEndState();
    }

    private void updateWave(float dt) {
        if (currentWaveIndex >= level.waves.size()) {
            return;
        }
        if (nextWaveTimer > 0f) {
            nextWaveTimer -= dt;
            return;
        }
        Wave wave = level.waves.get(currentWaveIndex);
        if (currentSpawnGroup >= wave.spawns.size()) {
            if (enemies.isEmpty()) {
                startNextWave();
            }
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            WaveSpawn spawn = wave.spawns.get(currentSpawnGroup);
            spawnEnemy(spawn.enemyId);
            currentSpawnCount++;
            spawnTimer = spawn.interval;
            if (currentSpawnCount >= spawn.count) {
                currentSpawnGroup++;
                currentSpawnCount = 0;
                spawnTimer = 0.6f;
            }
        }
    }

    private void spawnEnemy(String enemyId) {
        EnemyTemplate template = level.enemies.get(enemyId);
        PointF2 start = level.path.get(0);
        enemies.add(new Enemy(template, start.x, start.y));
    }

    private void updateEnemies(float dt) {
        int pathLast = level.path.size() - 1;
        for (Enemy enemy : enemies) {
            if (enemy.dead || enemy.reachedEnd) {
                continue;
            }
            if (enemy.burnTime > 0f) {
                enemy.burnTime -= dt;
                enemy.hp -= enemy.burnDps * dt;
            }
            if (enemy.slowTime > 0f) {
                enemy.slowTime -= dt;
                if (enemy.slowTime <= 0f) {
                    enemy.slowFactor = 1f;
                }
            }
            if (enemy.hp <= 0f) {
                enemy.dead = true;
                continue;
            }
            if (enemy.blocker != null && !enemy.blocker.dead) {
                float dx = enemy.blocker.x - enemy.x;
                float dy = enemy.blocker.y - enemy.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 24f) {
                    enemy.blocker = null;
                    enemy.blocked = false;
                    enemy.blockedTime = 0f;
                } else {
                    enemy.blockedTime += dt;
                    // Failsafe: prevent permanent lane lock when blockers desync.
                    if (enemy.blockedTime > 2.4f) {
                        enemy.blocker = null;
                        enemy.blocked = false;
                        enemy.blockedTime = 0f;
                    } else {
                        continue;
                    }
                }
            } else {
                enemy.blocker = null;
                enemy.blocked = false;
                enemy.blockedTime = 0f;
            }
            if (enemy.waypointIndex >= pathLast) {
                enemy.reachedEnd = true;
                life--;
                playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
                continue;
            }
            PointF2 target = level.path.get(enemy.waypointIndex + 1);
            float dx = target.x - enemy.x;
            float dy = target.y - enemy.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float step = enemy.template.speed * enemy.slowFactor * dt;
            if (dist <= step) {
                enemy.x = target.x;
                enemy.y = target.y;
                enemy.waypointIndex++;
                enemy.progress = enemy.waypointIndex + 0.001f;
            } else if (dist > 0f) {
                enemy.x += dx / dist * step;
                enemy.y += dy / dist * step;
                enemy.progress = enemy.waypointIndex + 1f - (dist / 1000f);
            }
        }
    }

    private void updateTowers(float dt) {
        for (TowerSlot slot : level.slots) {
            Tower tower = slot.tower;
            if (tower == null) {
                continue;
            }
            tower.attackTimer -= dt;
            if (tower.isBarracks()) {
                continue;
            }
            if (tower.attackTimer > 0f) {
                continue;
            }
            Enemy target = pickEnemy(tower);
            if (target == null) {
                continue;
            }
            if (tower.type == TowerType.ARTILLERY) {
                Projectile projectile = new Projectile(tower.x, tower.y, 260f, 10f, tower.damage, DamageType.EXPLOSIVE, target);
                projectile.splash = true;
                projectile.splashRadius = tower.splashRadius;
                if (tower.branch == BranchType.STORM) {
                    projectile.slowFactor = 0.55f;
                    projectile.slowDuration = 1.6f;
                    projectile.chainRadius = 110f;
                    projectile.chainCount = 2;
                }
                if (tower.branch == BranchType.SIEGE) {
                    projectile.damage *= 1.25f;
                    projectile.splashRadius += 18f;
                }
                projectiles.add(projectile);
                tower.attackTimer = tower.attackInterval;
                playTone(ToneGenerator.TONE_PROP_BEEP);
            } else {
                DamageType damageType = tower.type == TowerType.MAGE ? DamageType.MAGIC : DamageType.PHYSICAL;
                Projectile projectile = new Projectile(tower.x, tower.y, tower.type == TowerType.MAGE ? 340f : 420f, 8f, tower.damage, damageType, target);
                if (tower.branch == BranchType.RANGER) {
                    projectile.chainRadius = 0f;
                }
                if (tower.branch == BranchType.HEX) {
                    projectile.slowFactor = 0.6f;
                    projectile.slowDuration = 2.0f;
                }
                projectiles.add(projectile);
                tower.attackTimer = tower.attackInterval;
            }
        }
    }

    private Enemy pickEnemy(Tower tower) {
        Enemy best = null;
        float bestValue = -999999f;
        for (Enemy enemy : enemies) {
            if (enemy.dead || enemy.reachedEnd) {
                continue;
            }
            if (enemy.template.flying && tower.type == TowerType.BARRACKS) {
                continue;
            }
            if (enemy.template.flying && tower.type == TowerType.ARTILLERY && tower.branch != BranchType.STORM) {
                continue;
            }
            float dx = enemy.x - tower.x;
            float dy = enemy.y - tower.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > tower.range) {
                continue;
            }
            float value;
            if (tower.priority == TargetPriority.NEAREST) {
                value = -dist;
            } else if (tower.priority == TargetPriority.LOWEST_HP) {
                value = -enemy.hp;
            } else if (tower.priority == TargetPriority.HIGHEST_THREAT) {
                value = enemy.template.threat * 1000f - dist;
            } else {
                value = enemy.progress * 1000f - dist;
            }
            if (best == null || value > bestValue) {
                best = enemy;
                bestValue = value;
            }
        }
        return best;
    }

    private void updateProjectiles(float dt) {
        for (Projectile projectile : projectiles) {
            if (!projectile.active) {
                continue;
            }
            if (projectile.target == null || projectile.target.dead || projectile.target.reachedEnd) {
                projectile.active = false;
                continue;
            }
            float dx = projectile.target.x - projectile.x;
            float dy = projectile.target.y - projectile.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float step = projectile.speed * dt;
            if (dist <= step + projectile.radius) {
                applyProjectile(projectile);
                projectile.active = false;
            } else if (dist > 0f) {
                projectile.x += dx / dist * step;
                projectile.y += dy / dist * step;
            }
        }
    }

    private void applyProjectile(Projectile projectile) {
        if (projectile.splash) {
            for (Enemy enemy : enemies) {
                if (enemy.dead || enemy.reachedEnd) {
                    continue;
                }
                float dx = enemy.x - projectile.target.x;
                float dy = enemy.y - projectile.target.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= projectile.splashRadius) {
                    damageEnemy(enemy, projectile.damage, projectile.damageType);
                    if (projectile.slowDuration > 0f) {
                        enemy.slowFactor = Math.min(enemy.slowFactor, projectile.slowFactor);
                        enemy.slowTime = Math.max(enemy.slowTime, projectile.slowDuration);
                    }
                }
            }
        } else {
            damageEnemy(projectile.target, projectile.damage, projectile.damageType);
            if (projectile.slowDuration > 0f) {
                projectile.target.slowFactor = Math.min(projectile.target.slowFactor, projectile.slowFactor);
                projectile.target.slowTime = Math.max(projectile.target.slowTime, projectile.slowDuration);
            }
        }
        if (projectile.chainCount > 0 && projectile.chainRadius > 0f) {
            int chains = 0;
            for (Enemy enemy : enemies) {
                if (enemy == projectile.target || enemy.dead || enemy.reachedEnd) {
                    continue;
                }
                float dx = enemy.x - projectile.target.x;
                float dy = enemy.y - projectile.target.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= projectile.chainRadius) {
                    damageEnemy(enemy, projectile.damage * 0.6f, DamageType.MAGIC);
                    enemy.slowFactor = Math.min(enemy.slowFactor, 0.7f);
                    enemy.slowTime = Math.max(enemy.slowTime, 1.2f);
                    chains++;
                    if (chains >= projectile.chainCount) {
                        break;
                    }
                }
            }
        }
    }

    private void updateSoldiers(float dt) {
        for (Soldier soldier : soldiers) {
            if (soldier.temporary) {
                soldier.lifetime -= dt;
                if (soldier.lifetime <= 0f) {
                    soldier.dead = true;
                }
            }
            if (soldier.dead) {
                if (soldier.temporary) {
                    continue;
                }
                soldier.respawnTime -= dt;
                if (soldier.respawnTime <= 0f) {
                    soldier.dead = false;
                    soldier.hp = soldier.maxHp;
                    soldier.x = soldier.homeX;
                    soldier.y = soldier.homeY;
                }
                continue;
            }
            syncSoldierStats(soldier);
            if (soldier.target == null || soldier.target.dead || soldier.target.reachedEnd || soldier.target.template.flying) {
                soldier.target = pickBlockTarget(soldier);
            }
            if (soldier.target != null) {
                if (soldier.target.blocker != soldier) {
                    soldier.target.blockedTime = 0f;
                }
                soldier.target.blocked = true;
                soldier.target.blocker = soldier;
                float dx = soldier.target.x - soldier.x;
                float dy = soldier.target.y - soldier.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 24f) {
                    float step = soldier.speed * dt;
                    if (dist > 0f) {
                        soldier.x += dx / dist * step;
                        soldier.y += dy / dist * step;
                    }
                } else {
                    soldier.attackCooldown -= dt;
                    if (soldier.attackCooldown <= 0f) {
                        damageEnemy(soldier.target, soldier.damage, DamageType.MELEE);
                        soldier.attackCooldown = soldier.attackInterval;
                        soldier.hp -= Math.max(3f, soldier.target.template.threat * 1.8f);
                        if (soldier.hp <= 0f) {
                            killSoldier(soldier);
                        }
                    }
                }
            } else {
                float dx = soldier.homeX - soldier.x;
                float dy = soldier.homeY - soldier.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 2f) {
                    float step = soldier.speed * dt;
                    soldier.x += dx / dist * Math.min(step, dist);
                    soldier.y += dy / dist * Math.min(step, dist);
                }
            }
        }
    }

    private void updateHero(float dt) {
        if (hero.dead) {
            hero.respawnTime -= dt;
            if (hero.respawnTime <= 0f) {
                hero.dead = false;
                hero.hp = hero.maxHp;
                hero.x = 200f;
                hero.y = 520f;
                hero.targetX = hero.x;
                hero.targetY = hero.y;
            }
            return;
        }
        Enemy target = null;
        float dx = hero.targetX - hero.x;
        float dy = hero.targetY - hero.y;
        float distMove = (float) Math.sqrt(dx * dx + dy * dy);
        if (distMove > 4f) {
            float step = hero.speed * dt;
            hero.x += dx / distMove * Math.min(step, distMove);
            hero.y += dy / distMove * Math.min(step, distMove);
        }
        float bestDist = Float.MAX_VALUE;
        for (Enemy enemy : enemies) {
            if (enemy.dead || enemy.reachedEnd) {
                continue;
            }
            float ex = enemy.x - hero.x;
            float ey = enemy.y - hero.y;
            float d = (float) Math.sqrt(ex * ex + ey * ey);
            if (d <= 240f && d < bestDist) {
                bestDist = d;
                target = enemy;
            }
        }
        if (target != null) {
            float tx = target.x - hero.x;
            float ty = target.y - hero.y;
            float targetDist = (float) Math.sqrt(tx * tx + ty * ty);
            if (targetDist > hero.range) {
                float chaseStep = hero.speed * 1.2f * dt;
                if (targetDist > 0f) {
                    hero.x += tx / targetDist * Math.min(chaseStep, targetDist - hero.range + 4f);
                    hero.y += ty / targetDist * Math.min(chaseStep, targetDist - hero.range + 4f);
                }
            } else {
                hero.attackTimer -= dt;
                if (hero.attackTimer <= 0f) {
                    damageEnemy(target, hero.damage, DamageType.MELEE);
                    hero.attackTimer = hero.attackInterval;
                    triggerHeroSlash(target);
                    hero.hp -= Math.max(2f, target.template.threat * 0.8f);
                    if (hero.hp <= 0f) {
                        hero.dead = true;
                        hero.respawnTime = 8f;
                    }
                }
            }
        }
    }

    private void triggerHeroSlash(Enemy target) {
        heroSlashTimer = 0.16f;
        heroSlashX = hero.x;
        heroSlashY = hero.y;
        float dx = target.x - hero.x;
        float dy = target.y - hero.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.001f) {
            heroSlashDirX = dx / len;
            heroSlashDirY = dy / len;
        } else {
            heroSlashDirX = 1f;
            heroSlashDirY = 0f;
        }
    }

    private Enemy pickBlockTarget(Soldier soldier) {
        Enemy best = null;
        float bestDist = Float.MAX_VALUE;
        for (Enemy enemy : enemies) {
            if (enemy.dead || enemy.reachedEnd || enemy.template.flying || enemy.blocker != null) {
                continue;
            }
            float dx = enemy.x - soldier.homeX;
            float dy = enemy.y - soldier.homeY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= 92f && dist < bestDist) {
                best = enemy;
                bestDist = dist;
            }
        }
        return best;
    }

    private void syncSoldierStats(Soldier soldier) {
        Tower tower = soldier.owner;
        soldier.maxHp = tower.soldierHp;
        if (soldier.hp > soldier.maxHp) {
            soldier.hp = soldier.maxHp;
        }
        soldier.damage = tower.soldierDamage;
        soldier.speed = tower.soldierMoveSpeed;
        soldier.armor = tower.soldierArmor;
        soldier.respawnDelay = tower.soldierRespawn;
    }

    private void killSoldier(Soldier soldier) {
        soldier.dead = true;
        soldier.target = null;
        soldier.respawnTime = soldier.respawnDelay;
    }

    private void cleanupDead() {
        ArrayList<PendingSplit> pendingSplits = new ArrayList<>();
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.dead) {
                gold += enemy.template.reward;
                if (enemy.template.splitOnDeath && enemy.template.splitChildId != null) {
                    for (int i = 0; i < enemy.template.splitCount; i++) {
                        PendingSplit split = new PendingSplit();
                        split.enemyId = enemy.template.splitChildId;
                        split.x = enemy.x + i * 10f;
                        split.y = enemy.y + i * 8f;
                        split.waypointIndex = enemy.waypointIndex;
                        pendingSplits.add(split);
                    }
                }
                enemyIterator.remove();
                continue;
            }
            if (enemy.reachedEnd) {
                enemyIterator.remove();
            }
        }
        for (PendingSplit split : pendingSplits) {
            spawnSplit(split.enemyId, split.x, split.y, split.waypointIndex);
        }
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            if (!projectileIterator.next().active) {
                projectileIterator.remove();
            }
        }
        rebuildBarracksUnits();
    }

    private void spawnSplit(String enemyId, float x, float y, int waypointIndex) {
        EnemyTemplate template = level.enemies.get(enemyId);
        Enemy enemy = new Enemy(template, x, y);
        enemy.waypointIndex = waypointIndex;
        enemies.add(enemy);
    }

    private void rebuildBarracksUnits() {
        HashMap<Tower, Integer> counts = new HashMap<>();
        for (Soldier soldier : soldiers) {
            Integer count = counts.get(soldier.owner);
            counts.put(soldier.owner, count == null ? 1 : count + 1);
        }
        for (TowerSlot slot : level.slots) {
            Tower tower = slot.tower;
            if (tower == null || !tower.isBarracks()) {
                continue;
            }
            int count = counts.containsKey(tower) ? counts.get(tower) : 0;
            while (count < tower.unitCount) {
                float offset = count == 0 ? -18f : count == 1 ? 18f : 0f;
                Soldier soldier = new Soldier(tower, tower.x + offset, tower.y + 22f);
                syncSoldierStats(soldier);
                soldier.hp = soldier.maxHp;
                soldiers.add(soldier);
                count++;
            }
        }
        Iterator<Soldier> iterator = soldiers.iterator();
        while (iterator.hasNext()) {
            Soldier soldier = iterator.next();
            if (soldier.temporary) {
                if (soldier.dead && soldier.lifetime <= 0f) {
                    iterator.remove();
                }
                continue;
            }
            boolean valid = false;
            for (TowerSlot slot : level.slots) {
                if (slot.tower == soldier.owner) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                iterator.remove();
            }
        }
    }

    private void checkEndState() {
        if (life <= 0) {
            state = GameState.GAME_OVER;
            victory = false;
            banner = "Defeat";
            bannerTimer = 100f;
            return;
        }
        if (currentWaveIndex >= level.waves.size() && enemies.isEmpty()) {
            state = GameState.GAME_OVER;
            victory = true;
            banner = "Victory";
            bannerTimer = 100f;
        }
    }

    public Tower buildTower(TowerSlot slot, TowerType type) {
        if (slot.tower != null) {
            return null;
        }
        int cost = getBuildCost(type);
        if (gold < cost) {
            return null;
        }
        gold -= cost;
        Tower tower = createTower(type, slot.x, slot.y);
        tower.buildCost = cost;
        tower.totalSpent = cost;
        tower.sellValue = (int) (tower.totalSpent * 0.7f);
        slot.tower = tower;
        selectedTower = tower;
        selectedSlot = slot;
        rebuildBarracksUnits();
        playTone(ToneGenerator.TONE_PROP_ACK);
        return tower;
    }

    public boolean upgradeSelectedTower() {
        if (selectedTower == null) {
            return false;
        }
        int cost = getUpgradeCost(selectedTower);
        if (cost <= 0 || gold < cost) {
            return false;
        }
        gold -= cost;
        selectedTower.totalSpent += cost;
        selectedTower.sellValue = (int) (selectedTower.totalSpent * 0.7f);
        if (selectedTower.branch == BranchType.NONE) {
            selectedTower.level++;
            applyBaseStats(selectedTower);
        } else {
            selectedTower.branchLevel++;
            applyBranchStats(selectedTower);
        }
        rebuildBarracksUnits();
        playTone(ToneGenerator.TONE_PROP_BEEP2);
        return true;
    }

    public boolean branchSelectedTower(BranchType branchType) {
        if (selectedTower == null || selectedTower.level < 3 || selectedTower.branch != BranchType.NONE) {
            return false;
        }
        int cost = getBranchCost(selectedTower.type);
        if (gold < cost) {
            return false;
        }
        gold -= cost;
        selectedTower.totalSpent += cost;
        selectedTower.sellValue = (int) (selectedTower.totalSpent * 0.7f);
        selectedTower.branch = branchType;
        selectedTower.branchLevel = 1;
        applyBranchStats(selectedTower);
        rebuildBarracksUnits();
        playTone(ToneGenerator.TONE_PROP_PROMPT);
        return true;
    }

    public void sellSelectedTower() {
        if (selectedSlot == null || selectedSlot.tower == null) {
            return;
        }
        gold += selectedSlot.tower.sellValue;
        selectedSlot.tower = null;
        selectedTower = null;
        selectedSlot = null;
        rebuildBarracksUnits();
        playTone(ToneGenerator.TONE_PROP_NACK);
    }

    public int getBuildCost(TowerType type) {
        if (type == TowerType.ARROW) {
            return 70;
        }
        if (type == TowerType.ARTILLERY) {
            return 95;
        }
        if (type == TowerType.BARRACKS) {
            return 80;
        }
        return 90;
    }

    public int getUpgradeCost(Tower tower) {
        if (tower.branch == BranchType.NONE) {
            if (tower.level == 1) {
                return 55;
            }
            if (tower.level == 2) {
                return 85;
            }
            return 0;
        }
        if (tower.branchLevel == 1) {
            return 110;
        }
        if (tower.branchLevel == 2) {
            return 145;
        }
        return 0;
    }

    public int getBranchCost(TowerType type) {
        return 120;
    }

    public Tower createTower(TowerType type, float x, float y) {
        Tower tower = new Tower(type, x, y);
        applyBaseStats(tower);
        return tower;
    }

    public void applyBaseStats(Tower tower) {
        if (tower.type == TowerType.ARROW) {
            tower.range = 180f + (tower.level - 1) * 16f;
            tower.damage = 18f + (tower.level - 1) * 8f;
            tower.attackInterval = Math.max(0.28f, 0.7f - (tower.level - 1) * 0.08f);
        } else if (tower.type == TowerType.ARTILLERY) {
            tower.range = 160f + (tower.level - 1) * 10f;
            tower.damage = 36f + (tower.level - 1) * 18f;
            tower.attackInterval = Math.max(1.0f, 1.8f - (tower.level - 1) * 0.15f);
            tower.splashRadius = 52f + (tower.level - 1) * 12f;
        } else if (tower.type == TowerType.BARRACKS) {
            tower.unitCount = tower.level >= 3 ? 3 : 2;
            tower.soldierHp = 90f + (tower.level - 1) * 55f;
            tower.soldierDamage = 12f + (tower.level - 1) * 6f;
            tower.soldierRespawn = Math.max(4f, 6.5f - (tower.level - 1) * 0.7f);
            tower.soldierMoveSpeed = 72f + (tower.level - 1) * 8f;
            tower.soldierArmor = 10f + (tower.level - 1) * 8f;
        } else if (tower.type == TowerType.MAGE) {
            tower.range = 170f + (tower.level - 1) * 16f;
            tower.damage = 28f + (tower.level - 1) * 10f;
            tower.attackInterval = Math.max(0.55f, 1.1f - (tower.level - 1) * 0.1f);
        }
    }

    public void applyBranchStats(Tower tower) {
        if (tower.branch == BranchType.SHARPSHOT) {
            tower.range = 235f + (tower.branchLevel - 1) * 15f;
            tower.damage = 62f + (tower.branchLevel - 1) * 22f;
            tower.attackInterval = 1.05f - (tower.branchLevel - 1) * 0.08f;
            tower.priority = TargetPriority.HIGHEST_THREAT;
        } else if (tower.branch == BranchType.RANGER) {
            tower.range = 200f + (tower.branchLevel - 1) * 12f;
            tower.damage = 22f + (tower.branchLevel - 1) * 9f;
            tower.attackInterval = 0.28f;
            tower.priority = TargetPriority.FIRST;
        } else if (tower.branch == BranchType.SIEGE) {
            tower.range = 182f + (tower.branchLevel - 1) * 10f;
            tower.damage = 90f + (tower.branchLevel - 1) * 28f;
            tower.attackInterval = 1.75f - (tower.branchLevel - 1) * 0.12f;
            tower.splashRadius = 88f + (tower.branchLevel - 1) * 12f;
        } else if (tower.branch == BranchType.STORM) {
            tower.range = 178f + (tower.branchLevel - 1) * 10f;
            tower.damage = 58f + (tower.branchLevel - 1) * 18f;
            tower.attackInterval = 1.35f - (tower.branchLevel - 1) * 0.1f;
            tower.splashRadius = 65f + (tower.branchLevel - 1) * 8f;
        } else if (tower.branch == BranchType.KNIGHT) {
            tower.unitCount = 3;
            tower.soldierHp = 290f + (tower.branchLevel - 1) * 120f;
            tower.soldierDamage = 26f + (tower.branchLevel - 1) * 10f;
            tower.soldierRespawn = 4.8f - (tower.branchLevel - 1) * 0.3f;
            tower.soldierMoveSpeed = 88f;
            tower.soldierArmor = 40f + (tower.branchLevel - 1) * 12f;
        } else if (tower.branch == BranchType.BERSERKER) {
            tower.unitCount = 3;
            tower.soldierHp = 210f + (tower.branchLevel - 1) * 85f;
            tower.soldierDamage = 42f + (tower.branchLevel - 1) * 18f;
            tower.soldierRespawn = 3.8f - (tower.branchLevel - 1) * 0.3f;
            tower.soldierMoveSpeed = 110f + (tower.branchLevel - 1) * 10f;
            tower.soldierArmor = 18f + (tower.branchLevel - 1) * 6f;
        } else if (tower.branch == BranchType.ARCANE) {
            tower.range = 220f + (tower.branchLevel - 1) * 14f;
            tower.damage = 65f + (tower.branchLevel - 1) * 22f;
            tower.attackInterval = 0.92f - (tower.branchLevel - 1) * 0.06f;
            tower.priority = TargetPriority.HIGHEST_THREAT;
        } else if (tower.branch == BranchType.HEX) {
            tower.range = 210f + (tower.branchLevel - 1) * 12f;
            tower.damage = 34f + (tower.branchLevel - 1) * 11f;
            tower.attackInterval = 0.72f - (tower.branchLevel - 1) * 0.04f;
            tower.priority = TargetPriority.FIRST;
        }
    }

    public void selectSlot(TowerSlot slot) {
        selectedSlot = slot;
        selectedTower = slot.tower;
        buildPanelButtons();
    }

    public void clearSelection() {
        selectedSlot = null;
        selectedTower = null;
        panelButtons.clear();
        waitingForSkillTarget = false;
        selectedSkill = null;
    }

    public void buildPanelButtons() {
        panelButtons.clear();
        if (selectedSlot == null) {
            return;
        }
        if (selectedSlot.tower == null) {
            panelButtons.add(new UiActionButton("build_arrow", "Arrow 70"));
            panelButtons.add(new UiActionButton("build_artillery", "Cannon 95"));
            panelButtons.add(new UiActionButton("build_barracks", "Barracks 80"));
            panelButtons.add(new UiActionButton("build_mage", "Mage 90"));
        } else {
            Tower tower = selectedSlot.tower;
            int upgrade = getUpgradeCost(tower);
            if (upgrade > 0) {
                panelButtons.add(new UiActionButton("upgrade", "Upgrade " + upgrade));
            }
            if (tower.level >= 3 && tower.branch == BranchType.NONE) {
                for (BranchType branch : getBranches(tower.type)) {
                    panelButtons.add(new UiActionButton("branch_" + branch.name(), formatBranch(branch) + " " + getBranchCost(tower.type)));
                }
            }
            panelButtons.add(new UiActionButton("priority", "Target " + tower.priority.name()));
            panelButtons.add(new UiActionButton("sell", "Sell " + tower.sellValue));
        }
        panelButtons.add(new UiActionButton("close", "Close"));
    }

    public BranchType[] getBranches(TowerType type) {
        if (type == TowerType.ARROW) {
            return new BranchType[]{BranchType.SHARPSHOT, BranchType.RANGER};
        }
        if (type == TowerType.ARTILLERY) {
            return new BranchType[]{BranchType.SIEGE, BranchType.STORM};
        }
        if (type == TowerType.BARRACKS) {
            return new BranchType[]{BranchType.KNIGHT, BranchType.BERSERKER};
        }
        return new BranchType[]{BranchType.ARCANE, BranchType.HEX};
    }

    public String formatBranch(BranchType branchType) {
        String low = branchType.name().toLowerCase();
        return Character.toUpperCase(low.charAt(0)) + low.substring(1);
    }

    public void executePanelAction(String actionId) {
        if (actionId.equals("build_arrow")) {
            buildTower(selectedSlot, TowerType.ARROW);
        } else if (actionId.equals("build_artillery")) {
            buildTower(selectedSlot, TowerType.ARTILLERY);
        } else if (actionId.equals("build_barracks")) {
            buildTower(selectedSlot, TowerType.BARRACKS);
        } else if (actionId.equals("build_mage")) {
            buildTower(selectedSlot, TowerType.MAGE);
        } else if (actionId.equals("upgrade")) {
            upgradeSelectedTower();
        } else if (actionId.equals("sell")) {
            sellSelectedTower();
        } else if (actionId.equals("priority")) {
            cyclePriority();
        } else if (actionId.startsWith("branch_")) {
            branchSelectedTower(BranchType.valueOf(actionId.substring(7)));
        } else if (actionId.equals("close")) {
            clearSelection();
            return;
        }
        buildPanelButtons();
    }

    private void cyclePriority() {
        if (selectedTower == null) {
            return;
        }
        if (selectedTower.priority == TargetPriority.FIRST) {
            selectedTower.priority = TargetPriority.NEAREST;
        } else if (selectedTower.priority == TargetPriority.NEAREST) {
            selectedTower.priority = TargetPriority.LOWEST_HP;
        } else if (selectedTower.priority == TargetPriority.LOWEST_HP) {
            selectedTower.priority = TargetPriority.HIGHEST_THREAT;
        } else {
            selectedTower.priority = TargetPriority.FIRST;
        }
    }

    private void damageEnemy(Enemy enemy, float rawDamage, DamageType damageType) {
        float dealt = CombatMath.apply(enemy, rawDamage, damageType);
        enemy.hp -= dealt;
        spawnDamageText(enemy.x, enemy.y - 18f, Math.max(1, Math.round(dealt)));
        if (enemy.hp <= 0f) {
            enemy.dead = true;
        }
    }

    private void spawnDamageText(float x, float y, int value) {
        DamageText text = new DamageText();
        text.x = x;
        text.y = y;
        text.value = value;
        text.ttl = 0.6f;
        damageTexts.add(text);
    }

    public RectF getPanelRect(float width, float height) {
        return new RectF(width - 330f, 120f, width - 20f, 120f + 60f + panelButtons.size() * 54f);
    }

    public RectF getSkillRect(int index, float width, float height) {
        float left = 18f + index * 86f;
        return new RectF(left, height - 84f, left + 72f, height - 12f);
    }

    public RectF getPauseRect(float width) {
        return new RectF(width - 90f, 10f, width - 18f, 58f);
    }

    public RectF getEarlyCallRect(float width) {
        return new RectF(width - 180f, 10f, width - 100f, 58f);
    }

    public TowerSlot findSlot(float x, float y) {
        for (TowerSlot slot : level.slots) {
            float dx = x - slot.x;
            float dy = y - slot.y;
            if (dx * dx + dy * dy <= 34f * 34f) {
                return slot;
            }
        }
        return null;
    }

    public boolean selectSkillAt(float x, float y, float width, float height) {
        for (int i = 0; i < skills.size(); i++) {
            RectF rect = getSkillRect(i, width, height);
            if (rect.contains(x, y)) {
                Skill skill = skills.get(i);
                if (skill.ready()) {
                    waitingForSkillTarget = true;
                    selectedSkill = skill;
                    clearSelection();
                    waitingForSkillTarget = true;
                    selectedSkill = skill;
                }
                return true;
            }
        }
        return false;
    }

    public boolean triggerSkill(float x, float y) {
        if (!waitingForSkillTarget || selectedSkill == null || !selectedSkill.ready()) {
            return false;
        }
        if (selectedSkill.id.equals("reinforce")) {
            Tower tower = new Tower(TowerType.BARRACKS, x, y);
            tower.level = 3;
            tower.unitCount = 2;
            tower.soldierHp = 120f;
            tower.soldierDamage = 20f;
            tower.soldierRespawn = 1000f;
            tower.soldierMoveSpeed = 100f;
            tower.soldierArmor = 12f;
            for (int i = 0; i < 2; i++) {
                Soldier soldier = new Soldier(tower, x + (i == 0 ? -14f : 14f), y);
                syncSoldierStats(soldier);
                soldier.hp = soldier.maxHp;
                soldier.temporary = true;
                soldier.lifetime = 8f;
                soldiers.add(soldier);
            }
            banner = "Reinforce";
            bannerTimer = 1f;
        } else if (selectedSkill.id.equals("meteor")) {
            for (Enemy enemy : enemies) {
                if (enemy.dead || enemy.reachedEnd) {
                    continue;
                }
                float dx = enemy.x - x;
                float dy = enemy.y - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= 110f) {
                    damageEnemy(enemy, 220f, DamageType.MAGIC);
                    enemy.burnTime = 2.4f;
                    enemy.burnDps = 18f;
                }
            }
            banner = "Meteor";
            bannerTimer = 1f;
        }
        selectedSkill.timer = selectedSkill.cooldown;
        waitingForSkillTarget = false;
        selectedSkill = null;
        playTone(ToneGenerator.TONE_PROP_ACK);
        return true;
    }

    public void moveHero(float x, float y) {
        if (!hero.dead) {
            hero.targetX = x;
            hero.targetY = y;
        }
    }

    public void playTone(int toneCode) {
        if (!muted) {
            tone.startTone(toneCode, 90);
        }
    }

    public int totalWaves() {
        return level.waves.size();
    }

    public int remainingEnemiesInWave() {
        int alive = enemies.size();
        if (currentWaveIndex < 0 || currentWaveIndex >= level.waves.size()) {
            return alive;
        }
        Wave wave = level.waves.get(currentWaveIndex);
        int pending = 0;
        for (int i = currentSpawnGroup; i < wave.spawns.size(); i++) {
            WaveSpawn spawn = wave.spawns.get(i);
            if (i == currentSpawnGroup) {
                pending += spawn.count - currentSpawnCount;
            } else {
                pending += spawn.count;
            }
        }
        return alive + Math.max(0, pending);
    }
}
