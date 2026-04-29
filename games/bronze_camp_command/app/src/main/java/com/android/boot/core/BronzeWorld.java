package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BronzeWorld {
    public final List<Unit> units = new ArrayList<>();
    public final List<Building> buildings = new ArrayList<>();
    public final List<ResourceNode> resources = new ArrayList<>();
    public final List<Effect> effects = new ArrayList<>();
    public final List<Obelisk> obelisks = new ArrayList<>();
    public BronzeState state = BronzeState.MENU;
    public int food;
    public int wood;
    public int stone;
    public int populationCap;
    public float matchTime;
    public float warningPulse;
    public float dragLeft;
    public float dragTop;
    public float dragRight;
    public float dragBottom;
    public boolean dragActive;
    private int buildPlanIndex;
    private int trainPlanIndex;
    private int upgradeIndex;
    private int waveIndex;
    private float enemyRaidTimer;
    private float enemyTrainTimer;
    private float gatherTimer;
    private float productionTimer;
    private float viewportWidth = 1280f;
    private float viewportHeight = 720f;
    private String hint = "Assign villagers, place buildings, scout fog, then attack the Chief Hall.";
    private String alert = "Scout";
    private String resultTitle = "Result";
    private String resultBody = "";

    public BronzeWorld() {
        resetRun();
    }

    public void resetRun() {
        units.clear();
        buildings.clear();
        resources.clear();
        effects.clear();
        obelisks.clear();
        food = 120;
        wood = 110;
        stone = 45;
        populationCap = 12;
        matchTime = 0f;
        warningPulse = 0f;
        buildPlanIndex = 0;
        trainPlanIndex = 0;
        upgradeIndex = 0;
        waveIndex = 0;
        enemyRaidTimer = 28f;
        enemyTrainTimer = 8f;
        gatherTimer = 0f;
        productionTimer = 0f;
        hint = "Tap resources with villagers selected, build a Barracks, then train a strike squad.";
        alert = "Opening";
        resultTitle = "Result";
        resultBody = "";
        dragActive = false;
        for (UpgradeType upgrade : UpgradeType.values()) {
            upgrade.bought = false;
        }
        resources.add(new ResourceNode(ResourceType.FOOD, 190f, 220f, 300));
        resources.add(new ResourceNode(ResourceType.WOOD, 170f, 455f, 320));
        resources.add(new ResourceNode(ResourceType.STONE, 420f, 560f, 260));
        resources.add(new ResourceNode(ResourceType.FOOD, 820f, 135f, 240));
        resources.add(new ResourceNode(ResourceType.WOOD, 955f, 390f, 260));
        resources.add(new ResourceNode(ResourceType.STONE, 720f, 590f, 220));
        buildings.add(new Building(BuildingType.TOWN_HUT, 150f, 350f, true));
        buildings.add(new Building(BuildingType.CHIEF_HALL, 1110f, 345f, false));
        buildings.add(new Building(BuildingType.ENEMY_BARRACKS, 1030f, 500f, false));
        obelisks.add(new Obelisk(610f, 235f));
        obelisks.add(new Obelisk(620f, 470f));
        addUnit(UnitType.VILLAGER, 225f, 320f, true);
        addUnit(UnitType.VILLAGER, 245f, 370f, true);
        addUnit(UnitType.VILLAGER, 220f, 420f, true);
        addUnit(UnitType.SCOUT, 305f, 340f, true);
        addUnit(UnitType.RAIDER, 955f, 335f, false);
        addUnit(UnitType.ENEMY_SLINGER, 1005f, 390f, false);
        selectVillagers();
    }

    public void setViewport(int width, int height) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
    }

    public void startGame() {
        state = BronzeState.PLAYING;
    }

    public void openMenu() {
        state = BronzeState.MENU;
    }

    public void togglePause() {
        if (state == BronzeState.PLAYING) {
            state = BronzeState.PAUSED;
        } else if (state == BronzeState.PAUSED) {
            state = BronzeState.PLAYING;
        }
    }

    public boolean shouldPlayWarning() {
        return state == BronzeState.PLAYING && warningPulse > 0.92f;
    }

    public void update(float dt) {
        if (state != BronzeState.PLAYING) {
            return;
        }
        matchTime += dt;
        warningPulse = Math.max(0f, warningPulse - dt * 0.55f);
        gatherTimer += dt;
        productionTimer += dt;
        enemyRaidTimer -= dt;
        enemyTrainTimer -= dt;
        if (gatherTimer >= 1f) {
            gatherTimer -= 1f;
            gatherResources();
        }
        if (productionTimer >= 2.2f) {
            productionTimer = 0f;
            trainQueuedUnits();
        }
        if (enemyTrainTimer <= 0f) {
            enemyTrainTimer = 15f;
            spawnEnemyPatrol();
        }
        if (enemyRaidTimer <= 0f) {
            enemyRaidTimer = Math.max(22f, 34f - waveIndex * 2f);
            waveIndex++;
            launchRaid();
        }
        if (matchTime > 480f && waveIndex < 99) {
            waveIndex = 99;
            launchFinalAssault();
        }
        updateUnits(dt);
        updateCapture(dt);
        updateEffects(dt);
        checkWinLoss();
    }

    public void handleTap(float sx, float sy) {
        if (state != BronzeState.PLAYING) {
            return;
        }
        float x = screenToWorldX(sx);
        float y = screenToWorldY(sy);
        Unit unit = findUnit(x, y, true);
        if (unit != null) {
            clearSelection();
            unit.selected = true;
            hint = unit.type.label + " selected.";
            return;
        }
        Building ownBuilding = findBuilding(x, y, true);
        if (ownBuilding != null) {
            clearSelection();
            ownBuilding.selected = true;
            hint = ownBuilding.type.label + " selected.";
            return;
        }
        ResourceNode node = findResource(x, y);
        if (node != null && hasSelectedVillager()) {
            assignVillagers(node.x, node.y, node.type);
            hint = "Villagers gathering " + node.type.label + ".";
            return;
        }
        Building enemy = findBuilding(x, y, false);
        Unit enemyUnit = findUnit(x, y, false);
        if (enemy != null || enemyUnit != null) {
            commandSelectedAttack(x, y);
            hint = "Attack order issued.";
            return;
        }
        if (isBuildZone(x, y) && hasSelectedBuilding(BuildingType.TOWN_HUT)) {
            placeBuilding(x, y);
            return;
        }
        commandSelectedMove(x, y);
    }

    public void updateDragSelection(float sx0, float sy0, float sx1, float sy1) {
        dragActive = true;
        dragLeft = Math.min(sx0, sx1);
        dragRight = Math.max(sx0, sx1);
        dragTop = Math.min(sy0, sy1);
        dragBottom = Math.max(sy0, sy1);
    }

    public void finishDragSelection(float sx0, float sy0, float sx1, float sy1) {
        updateDragSelection(sx0, sy0, sx1, sy1);
        clearSelection();
        for (Unit unit : units) {
            if (unit.player && unit.alive) {
                float sx = worldToScreenX(unit.x);
                float sy = worldToScreenY(unit.y);
                if (sx >= dragLeft && sx <= dragRight && sy >= dragTop && sy <= dragBottom) {
                    unit.selected = true;
                }
            }
        }
        dragActive = false;
        hint = selectedUnitCount() + " units selected.";
    }

    public void cycleBuildPlan() {
        buildPlanIndex = (buildPlanIndex + 1) % 4;
        hint = "Build plan: " + currentBuildPlan().label + ". Select Town Hut then tap camp ground.";
    }

    public boolean trainSelectedUnit() {
        trainPlanIndex = (trainPlanIndex + 1) % 5;
        UnitType type = currentTrainPlan();
        Building building = findSelectedProductionBuilding(type);
        if (building == null) {
            hint = "Need " + requiredBuilding(type).label + " selected or built.";
            return false;
        }
        if (!canSpend(type.foodCost, type.woodCost, type.stoneCost) || population() >= populationCap) {
            hint = "Need resources or population space for " + type.label + ".";
            return false;
        }
        spend(type.foodCost, type.woodCost, type.stoneCost);
        building.queueType = type;
        building.queueTime += type.trainTime;
        hint = "Training " + type.label + ".";
        return true;
    }

    public boolean buyUpgrade() {
        UpgradeType upgrade = UpgradeType.values()[upgradeIndex % UpgradeType.values().length];
        upgradeIndex++;
        if (upgrade.bought) {
            hint = upgrade.label + " already researched.";
            return false;
        }
        if (!canSpend(upgrade.foodCost, upgrade.woodCost, upgrade.stoneCost)) {
            hint = "Need resources for " + upgrade.label + ".";
            return false;
        }
        spend(upgrade.foodCost, upgrade.woodCost, upgrade.stoneCost);
        upgrade.bought = true;
        if (upgrade == UpgradeType.SIGNAL_FIRES) {
            populationCap += 4;
        }
        hint = upgrade.label + " researched.";
        return true;
    }

    public void sendScout() {
        Unit scout = findFirstPlayer(UnitType.SCOUT);
        if (scout == null) {
            hint = "No scout available.";
            return;
        }
        scout.selected = true;
        scout.targetX = 930f;
        scout.targetY = 245f;
        scout.mode = UnitMode.SCOUT;
        hint = "Scout moving to reveal the enemy approach.";
    }

    public void selectArmy() {
        clearSelection();
        int count = 0;
        for (Unit unit : units) {
            if (unit.player && unit.alive && unit.type != UnitType.VILLAGER) {
                unit.selected = true;
                count++;
            }
        }
        hint = count + " combat units selected.";
    }

    public StatusSnapshot snapshot() {
        StatusSnapshot snapshot = new StatusSnapshot();
        snapshot.state = state;
        snapshot.food = food;
        snapshot.wood = wood;
        snapshot.stone = stone;
        snapshot.population = population();
        snapshot.populationCap = populationCap;
        snapshot.timeLabel = formatTime(matchTime);
        snapshot.alert = alert;
        snapshot.objective = objectiveText();
        snapshot.hint = hint;
        snapshot.buildLabel = "Build " + currentBuildPlan().shortLabel;
        snapshot.trainLabel = "Train " + currentTrainPlan().shortLabel;
        snapshot.upgradeLabel = "Tech " + UpgradeType.values()[upgradeIndex % UpgradeType.values().length].shortLabel;
        snapshot.scoutLabel = "Scout";
        snapshot.armyLabel = "Army " + combatCount();
        snapshot.resultTitle = resultTitle;
        snapshot.resultBody = resultBody;
        return snapshot;
    }

    public float screenToWorldX(float sx) {
        return sx / viewportWidth * 1280f;
    }

    public float screenToWorldY(float sy) {
        return sy / viewportHeight * 720f;
    }

    public float worldToScreenX(float wx) {
        return wx / 1280f * viewportWidth;
    }

    public float worldToScreenY(float wy) {
        return wy / 720f * viewportHeight;
    }

    private void gatherResources() {
        for (Unit unit : units) {
            if (unit.player && unit.alive && unit.type == UnitType.VILLAGER && unit.gatherType != null) {
                int amount = UpgradeType.WORKER_TOOLS.bought ? 7 : 5;
                if (unit.gatherType == ResourceType.FOOD) {
                    food += amount;
                } else if (unit.gatherType == ResourceType.WOOD) {
                    wood += amount;
                } else {
                    stone += amount;
                }
                effects.add(new Effect(unit.x, unit.y - 22f, "+" + amount, 1.0f));
            }
        }
    }

    private void trainQueuedUnits() {
        for (Building building : buildings) {
            if (building.player && building.queueTime > 0f) {
                building.queueTime -= 2.2f;
                if (building.queueTime <= 0f && building.queueType != null) {
                    addUnit(building.queueType, building.x + 42f, building.y + 18f, true);
                    effects.add(new Effect(building.x, building.y - 30f, "Ready", 1.2f));
                    building.queueType = null;
                }
            }
        }
    }

    private void updateUnits(float dt) {
        for (Unit unit : units) {
            if (!unit.alive) {
                continue;
            }
            if (!unit.player && unit.targetEnemy == null) {
                Unit target = nearestPlayerUnit(unit.x, unit.y);
                Building landmark = nearestPlayerBuilding(unit.x, unit.y);
                if (target != null && distance(unit.x, unit.y, target.x, target.y) < 150f) {
                    unit.targetEnemy = target;
                } else if (landmark != null) {
                    unit.targetX = landmark.x;
                    unit.targetY = landmark.y;
                }
            }
            if (unit.targetEnemy != null && !unit.targetEnemy.alive) {
                unit.targetEnemy = null;
            }
            if (unit.attackBuilding != null && unit.attackBuilding.hp <= 0) {
                unit.attackBuilding = null;
            }
            updateCombat(unit, dt);
            float dx = unit.targetX - unit.x;
            float dy = unit.targetY - unit.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 4f) {
                float speed = unit.type.speed;
                unit.x += dx / dist * speed * dt;
                unit.y += dy / dist * speed * dt;
                unit.facing = dx >= 0f ? 1 : -1;
                unit.walkPhase += dt * 8f;
            }
        }
        for (Building building : buildings) {
            if (building.hp <= 0) {
                effects.add(new Effect(building.x, building.y, "Broken", 1.4f));
            }
        }
        for (int i = units.size() - 1; i >= 0; i--) {
            if (!units.get(i).alive) {
                units.remove(i);
            }
        }
        for (int i = buildings.size() - 1; i >= 0; i--) {
            Building building = buildings.get(i);
            if (building.hp <= 0 && building.type != BuildingType.TOWN_HUT && building.type != BuildingType.CHIEF_HALL) {
                buildings.remove(i);
            }
        }
    }

    private void updateCombat(Unit unit, float dt) {
        unit.attackCooldown -= dt;
        Unit targetUnit = unit.targetEnemy;
        Building targetBuilding = unit.attackBuilding;
        if (targetUnit == null) {
            targetUnit = nearestEnemyUnit(unit);
        }
        if (targetBuilding == null) {
            targetBuilding = nearestEnemyBuilding(unit);
        }
        if (targetUnit != null && distance(unit.x, unit.y, targetUnit.x, targetUnit.y) <= unit.type.range) {
            unit.targetX = unit.x;
            unit.targetY = unit.y;
            if (unit.attackCooldown <= 0f) {
                targetUnit.hp -= attackPower(unit);
                unit.attackCooldown = unit.type.cooldown;
                effects.add(new Effect(targetUnit.x, targetUnit.y - 20f, "Hit", 0.45f));
                if (targetUnit.hp <= 0) {
                    targetUnit.alive = false;
                }
            }
        } else if (targetBuilding != null && distance(unit.x, unit.y, targetBuilding.x, targetBuilding.y) <= unit.type.range + 28f) {
            unit.targetX = unit.x;
            unit.targetY = unit.y;
            if (unit.attackCooldown <= 0f) {
                int power = unit.type == UnitType.RAM_CREW ? attackPower(unit) * 4 : attackPower(unit);
                targetBuilding.hp -= power;
                unit.attackCooldown = unit.type.cooldown;
                effects.add(new Effect(targetBuilding.x, targetBuilding.y - 35f, "Crack", 0.55f));
            }
        }
    }

    private void updateCapture(float dt) {
        int held = 0;
        for (Obelisk obelisk : obelisks) {
            int playerNear = 0;
            int enemyNear = 0;
            for (Unit unit : units) {
                if (unit.alive && distance(unit.x, unit.y, obelisk.x, obelisk.y) < 64f) {
                    if (unit.player) {
                        playerNear++;
                    } else {
                        enemyNear++;
                    }
                }
            }
            if (playerNear > enemyNear) {
                obelisk.control += dt * 0.22f;
            } else if (enemyNear > playerNear) {
                obelisk.control -= dt * 0.18f;
            }
            if (obelisk.control > 1f) {
                obelisk.control = 1f;
            }
            if (obelisk.control < -1f) {
                obelisk.control = -1f;
            }
            if (obelisk.control > 0.9f) {
                held++;
            }
        }
        if (matchTime > 180f && held == 2) {
            state = BronzeState.VICTORY;
            resultTitle = "Obelisks Held";
            resultBody = "Both neutral obelisks answered your banner. Stars: " + scoreStars();
        }
    }

    private void updateEffects(float dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect effect = effects.get(i);
            effect.life -= dt;
            effect.y -= dt * 18f;
            if (effect.life <= 0f) {
                effects.remove(i);
            }
        }
    }

    private void checkWinLoss() {
        Building town = findType(BuildingType.TOWN_HUT, true);
        Building enemyHall = findType(BuildingType.CHIEF_HALL, false);
        if (enemyHall == null || enemyHall.hp <= 0) {
            state = BronzeState.VICTORY;
            resultTitle = "Chief Hall Destroyed";
            resultBody = "Your bronze banner controls the valley. Stars: " + scoreStars();
            return;
        }
        if (town == null || town.hp <= 0) {
            state = BronzeState.GAME_OVER;
            resultTitle = "Town Hut Lost";
            resultBody = "The rival chief broke your camp. Rebuild faster and scout raids earlier.";
            return;
        }
        if (villagerCount() == 0 && food < UnitType.VILLAGER.foodCost) {
            state = BronzeState.GAME_OVER;
            resultTitle = "Economy Broken";
            resultBody = "No villagers remain and the granary cannot recover.";
        }
    }

    private void launchRaid() {
        alert = waveIndex >= 3 ? "Heavy Raid" : "Raid";
        warningPulse = 1f;
        addUnit(UnitType.RAIDER, 1050f, 230f, false).targetX = 210f;
        if (waveIndex > 1) {
            addUnit(UnitType.ENEMY_SPEARMAN, 1090f, 395f, false).targetX = 220f;
        }
        if (waveIndex > 2) {
            addUnit(UnitType.ENEMY_SLINGER, 1010f, 520f, false).targetX = 260f;
        }
        hint = "Enemy raid sighted. Select army and intercept.";
    }

    private void spawnEnemyPatrol() {
        if (combatCount(false) < 9) {
            UnitType type = matchTime > 180f ? UnitType.ENEMY_GUARD : UnitType.RAIDER;
            addUnit(type, 990f + combatCount(false) * 8f, 335f + combatCount(false) * 5f, false);
        }
    }

    private void launchFinalAssault() {
        alert = "Final Assault";
        warningPulse = 1f;
        addUnit(UnitType.CHIEFTAIN, 1065f, 345f, false).targetX = 170f;
        addUnit(UnitType.ENEMY_GUARD, 1030f, 300f, false).targetX = 190f;
        addUnit(UnitType.ENEMY_SPEARMAN, 1040f, 410f, false).targetX = 200f;
        addUnit(UnitType.ENEMY_SLINGER, 1100f, 450f, false).targetX = 225f;
    }

    private void placeBuilding(float x, float y) {
        BuildingType type = currentBuildPlan();
        if (!canSpend(type.foodCost, type.woodCost, type.stoneCost)) {
            hint = "Need resources for " + type.label + ".";
            return;
        }
        if (distance(x, y, 150f, 350f) > 310f) {
            hint = "Build inside the camp planning radius.";
            return;
        }
        spend(type.foodCost, type.woodCost, type.stoneCost);
        buildings.add(new Building(type, x, y, true));
        if (type == BuildingType.TOWN_HUT) {
            populationCap += 6;
        }
        hint = type.label + " placed.";
    }

    private void commandSelectedMove(float x, float y) {
        int index = 0;
        for (Unit unit : units) {
            if (unit.selected && unit.player && unit.alive) {
                unit.targetX = x + (index % 3 - 1) * 18f;
                unit.targetY = y + (index / 3) * 18f;
                unit.targetEnemy = null;
                unit.attackBuilding = null;
                unit.mode = UnitMode.MOVE;
                index++;
            }
        }
        hint = index > 0 ? "Move order issued." : "Tap or drag to select units.";
    }

    private void commandSelectedAttack(float x, float y) {
        Unit targetUnit = findUnit(x, y, false);
        Building targetBuilding = findBuilding(x, y, false);
        for (Unit unit : units) {
            if (unit.selected && unit.player && unit.alive) {
                unit.targetX = x;
                unit.targetY = y;
                unit.targetEnemy = targetUnit;
                unit.attackBuilding = targetBuilding;
                unit.mode = UnitMode.ATTACK;
            }
        }
    }

    private void assignVillagers(float x, float y, ResourceType type) {
        int index = 0;
        for (Unit unit : units) {
            if (unit.selected && unit.player && unit.alive && unit.type == UnitType.VILLAGER) {
                unit.targetX = x + (index - 1) * 16f;
                unit.targetY = y + 30f;
                unit.gatherType = type;
                unit.mode = UnitMode.GATHER;
                index++;
            }
        }
    }

    private Unit addUnit(UnitType type, float x, float y, boolean player) {
        Unit unit = new Unit(type, x, y, player);
        units.add(unit);
        return unit;
    }

    private void selectVillagers() {
        clearSelection();
        for (Unit unit : units) {
            if (unit.player && unit.type == UnitType.VILLAGER) {
                unit.selected = true;
            }
        }
    }

    private void clearSelection() {
        for (Unit unit : units) {
            unit.selected = false;
        }
        for (Building building : buildings) {
            building.selected = false;
        }
    }

    private Unit findUnit(float x, float y, boolean player) {
        for (Unit unit : units) {
            if (unit.player == player && unit.alive && distance(x, y, unit.x, unit.y) < 28f) {
                return unit;
            }
        }
        return null;
    }

    private Building findBuilding(float x, float y, boolean player) {
        for (Building building : buildings) {
            if (building.player == player && Math.abs(x - building.x) < building.type.w * 0.55f && Math.abs(y - building.y) < building.type.h * 0.55f) {
                return building;
            }
        }
        return null;
    }

    private ResourceNode findResource(float x, float y) {
        for (ResourceNode node : resources) {
            if (node.amount > 0 && distance(x, y, node.x, node.y) < 48f) {
                return node;
            }
        }
        return null;
    }

    private Building findType(BuildingType type, boolean player) {
        for (Building building : buildings) {
            if (building.type == type && building.player == player && building.hp > 0) {
                return building;
            }
        }
        return null;
    }

    private Unit findFirstPlayer(UnitType type) {
        for (Unit unit : units) {
            if (unit.player && unit.type == type && unit.alive) {
                return unit;
            }
        }
        return null;
    }

    private Unit nearestEnemyUnit(Unit unit) {
        Unit best = null;
        float bestDist = Float.MAX_VALUE;
        for (Unit other : units) {
            if (other.player != unit.player && other.alive) {
                float d = distance(unit.x, unit.y, other.x, other.y);
                if (d < bestDist && d < unit.type.aggroRange) {
                    bestDist = d;
                    best = other;
                }
            }
        }
        return best;
    }

    private Unit nearestPlayerUnit(float x, float y) {
        Unit best = null;
        float bestDist = Float.MAX_VALUE;
        for (Unit unit : units) {
            if (unit.player && unit.alive) {
                float d = distance(x, y, unit.x, unit.y);
                if (d < bestDist) {
                    bestDist = d;
                    best = unit;
                }
            }
        }
        return best;
    }

    private Building nearestEnemyBuilding(Unit unit) {
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        for (Building building : buildings) {
            if (building.player != unit.player && building.hp > 0) {
                float d = distance(unit.x, unit.y, building.x, building.y);
                if (d < bestDist && d < unit.type.aggroRange) {
                    bestDist = d;
                    best = building;
                }
            }
        }
        return best;
    }

    private Building nearestPlayerBuilding(float x, float y) {
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        for (Building building : buildings) {
            if (building.player && building.hp > 0) {
                float d = distance(x, y, building.x, building.y);
                if (d < bestDist) {
                    bestDist = d;
                    best = building;
                }
            }
        }
        return best;
    }

    private boolean hasSelectedVillager() {
        for (Unit unit : units) {
            if (unit.selected && unit.type == UnitType.VILLAGER && unit.player && unit.alive) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSelectedBuilding(BuildingType type) {
        for (Building building : buildings) {
            if (building.selected && building.type == type && building.player) {
                return true;
            }
        }
        return false;
    }

    private Building findSelectedProductionBuilding(UnitType type) {
        BuildingType required = requiredBuilding(type);
        for (Building building : buildings) {
            if (building.player && building.selected && building.type == required) {
                return building;
            }
        }
        for (Building building : buildings) {
            if (building.player && building.type == required) {
                return building;
            }
        }
        return null;
    }

    private BuildingType requiredBuilding(UnitType type) {
        if (type == UnitType.SLINGER) {
            return BuildingType.RANGE_YARD;
        }
        if (type == UnitType.RAM_CREW) {
            return BuildingType.STONE_WORKSHOP;
        }
        if (type == UnitType.VILLAGER) {
            return BuildingType.TOWN_HUT;
        }
        return BuildingType.BARRACKS;
    }

    private BuildingType currentBuildPlan() {
        BuildingType[] types = {BuildingType.BARRACKS, BuildingType.RANGE_YARD, BuildingType.STONE_WORKSHOP, BuildingType.WATCH_POST};
        return types[buildPlanIndex % types.length];
    }

    private UnitType currentTrainPlan() {
        UnitType[] types = {UnitType.VILLAGER, UnitType.MILITIA, UnitType.SPEARMAN, UnitType.SLINGER, UnitType.RAM_CREW};
        return types[trainPlanIndex % types.length];
    }

    private boolean isBuildZone(float x, float y) {
        return x < 530f && y > 120f && y < 630f;
    }

    private boolean canSpend(int f, int w, int s) {
        return food >= f && wood >= w && stone >= s;
    }

    private void spend(int f, int w, int s) {
        food -= f;
        wood -= w;
        stone -= s;
    }

    private int attackPower(Unit unit) {
        int power = unit.type.power;
        if (unit.type == UnitType.SLINGER && UpgradeType.SLING_POUCHES.bought) {
            power += 3;
        }
        if (unit.type == UnitType.SPEARMAN && UpgradeType.SPEAR_DRILL.bought) {
            power += 3;
        }
        if (unit.type == UnitType.RAM_CREW && UpgradeType.REINFORCED_RAMS.bought) {
            power += 4;
        }
        return power;
    }

    private int population() {
        int count = 0;
        for (Unit unit : units) {
            if (unit.player && unit.alive) {
                count++;
            }
        }
        return count;
    }

    private int combatCount() {
        return combatCount(true);
    }

    private int combatCount(boolean player) {
        int count = 0;
        for (Unit unit : units) {
            if (unit.player == player && unit.alive && unit.type != UnitType.VILLAGER) {
                count++;
            }
        }
        return count;
    }

    private int villagerCount() {
        int count = 0;
        for (Unit unit : units) {
            if (unit.player && unit.alive && unit.type == UnitType.VILLAGER) {
                count++;
            }
        }
        return count;
    }

    private int selectedUnitCount() {
        int count = 0;
        for (Unit unit : units) {
            if (unit.selected && unit.alive) {
                count++;
            }
        }
        return count;
    }

    private String objectiveText() {
        Building enemyHall = findType(BuildingType.CHIEF_HALL, false);
        int hp = enemyHall == null ? 0 : Math.max(0, enemyHall.hp);
        if (matchTime < 180f) {
            return "Destroy Chief Hall " + hp + " HP or prepare to hold both Obelisks.";
        }
        return "Destroy Chief Hall " + hp + " HP or hold both Obelisks now.";
    }

    private int scoreStars() {
        int stars = 1;
        if (matchTime < 420f) {
            stars++;
        }
        Building town = findType(BuildingType.TOWN_HUT, true);
        if (town != null && town.hp > 260) {
            stars++;
        }
        return stars;
    }

    private static float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static String formatTime(float seconds) {
        int total = (int) seconds;
        return String.format(Locale.US, "%d:%02d", total / 60, total % 60);
    }

    public enum ResourceType {
        FOOD("Food"),
        WOOD("Wood"),
        STONE("Stone");

        public final String label;

        ResourceType(String label) {
            this.label = label;
        }
    }

    public enum UnitMode {
        IDLE,
        MOVE,
        GATHER,
        ATTACK,
        SCOUT
    }

    public enum UnitType {
        VILLAGER("Villager", "Vil", 45, 0, 0, 5, 60f, 30f, 82f, 1.0f, 2.6f, 65),
        SCOUT("Scout", "Sct", 35, 20, 0, 6, 95f, 28f, 150f, 0.85f, 2.8f, 70),
        MILITIA("Militia", "Mil", 55, 20, 0, 10, 68f, 34f, 92f, 0.95f, 3.0f, 92),
        SPEARMAN("Spearman", "Spr", 50, 35, 0, 12, 62f, 42f, 98f, 1.05f, 3.4f, 105),
        SLINGER("Slinger", "Sln", 45, 55, 0, 9, 58f, 145f, 170f, 1.15f, 3.8f, 76),
        SHIELD_GUARD("Shield Guard", "Shd", 75, 45, 20, 8, 52f, 35f, 90f, 1.1f, 4.2f, 155),
        RAM_CREW("Ram Crew", "Ram", 40, 70, 55, 8, 38f, 38f, 80f, 1.7f, 5.0f, 190),
        RAIDER("Raider", "Rdr", 0, 0, 0, 8, 66f, 34f, 105f, 1.05f, 0f, 70),
        ENEMY_SPEARMAN("Enemy Spear", "ESp", 0, 0, 0, 11, 60f, 42f, 98f, 1.05f, 0f, 98),
        ENEMY_SLINGER("Enemy Sling", "ESl", 0, 0, 0, 8, 56f, 135f, 165f, 1.2f, 0f, 72),
        ENEMY_GUARD("Enemy Guard", "EGr", 0, 0, 0, 10, 48f, 35f, 92f, 1.1f, 0f, 140),
        CHIEFTAIN("Chieftain", "Chf", 0, 0, 0, 18, 52f, 44f, 120f, 1.0f, 0f, 260);

        public final String label;
        public final String shortLabel;
        public final int foodCost;
        public final int woodCost;
        public final int stoneCost;
        public final int power;
        public final float speed;
        public final float range;
        public final float aggroRange;
        public final float cooldown;
        public final float trainTime;
        public final int hp;

        UnitType(String label, String shortLabel, int foodCost, int woodCost, int stoneCost, int power, float speed, float range, float aggroRange, float cooldown, float trainTime, int hp) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.foodCost = foodCost;
            this.woodCost = woodCost;
            this.stoneCost = stoneCost;
            this.power = power;
            this.speed = speed;
            this.range = range;
            this.aggroRange = aggroRange;
            this.cooldown = cooldown;
            this.trainTime = trainTime;
            this.hp = hp;
        }
    }

    public enum BuildingType {
        TOWN_HUT("Town Hut", "Town", 0, 0, 0, 95f, 78f, 420),
        BARRACKS("Barracks", "Bar", 0, 95, 20, 86f, 62f, 230),
        RANGE_YARD("Range Yard", "Rng", 0, 120, 30, 86f, 62f, 220),
        STONE_WORKSHOP("Stone Workshop", "Wrk", 0, 120, 80, 92f, 66f, 260),
        WATCH_POST("Watch Post", "Post", 0, 80, 80, 54f, 90f, 180),
        CHIEF_HALL("Chief Hall", "Hall", 0, 0, 0, 116f, 90f, 620),
        ENEMY_BARRACKS("Enemy Barracks", "EBar", 0, 0, 0, 88f, 64f, 260);

        public final String label;
        public final String shortLabel;
        public final int foodCost;
        public final int woodCost;
        public final int stoneCost;
        public final float w;
        public final float h;
        public final int hp;

        BuildingType(String label, String shortLabel, int foodCost, int woodCost, int stoneCost, float w, float h, int hp) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.foodCost = foodCost;
            this.woodCost = woodCost;
            this.stoneCost = stoneCost;
            this.w = w;
            this.h = h;
            this.hp = hp;
        }
    }

    public enum UpgradeType {
        WORKER_TOOLS("Worker Tools", "Tools", 90, 50, 0),
        HARDENED_SHIELDS("Hardened Shields", "Shield", 70, 65, 20),
        SLING_POUCHES("Sling Pouches", "Sling", 60, 85, 20),
        SPEAR_DRILL("Spear Drill", "Spear", 75, 70, 30),
        REINFORCED_RAMS("Reinforced Rams", "Rams", 70, 90, 70),
        SIGNAL_FIRES("Signal Fires", "Signal", 40, 85, 60);

        public final String label;
        public final String shortLabel;
        public final int foodCost;
        public final int woodCost;
        public final int stoneCost;
        public boolean bought;

        UpgradeType(String label, String shortLabel, int foodCost, int woodCost, int stoneCost) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.foodCost = foodCost;
            this.woodCost = woodCost;
            this.stoneCost = stoneCost;
        }
    }

    public static class Unit {
        public final UnitType type;
        public final boolean player;
        public float x;
        public float y;
        public float targetX;
        public float targetY;
        public float attackCooldown;
        public float walkPhase;
        public int hp;
        public int facing = 1;
        public boolean selected;
        public boolean alive = true;
        public ResourceType gatherType;
        public UnitMode mode = UnitMode.IDLE;
        public Unit targetEnemy;
        public Building attackBuilding;

        Unit(UnitType type, float x, float y, boolean player) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.player = player;
            this.hp = type.hp;
        }
    }

    public static class Building {
        public final BuildingType type;
        public final boolean player;
        public float x;
        public float y;
        public int hp;
        public boolean selected;
        public UnitType queueType;
        public float queueTime;

        Building(BuildingType type, float x, float y, boolean player) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.player = player;
            this.hp = type.hp;
        }
    }

    public static class ResourceNode {
        public final ResourceType type;
        public final float x;
        public final float y;
        public int amount;

        ResourceNode(ResourceType type, float x, float y, int amount) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.amount = amount;
        }
    }

    public static class Obelisk {
        public final float x;
        public final float y;
        public float control;

        Obelisk(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Effect {
        public final float x;
        public float y;
        public final String text;
        public float life;

        Effect(float x, float y, String text, float life) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.life = life;
        }
    }

    public static class StatusSnapshot {
        public BronzeState state;
        public int food;
        public int wood;
        public int stone;
        public int population;
        public int populationCap;
        public String timeLabel;
        public String alert;
        public String objective;
        public String hint;
        public String buildLabel;
        public String trainLabel;
        public String upgradeLabel;
        public String scoutLabel;
        public String armyLabel;
        public String resultTitle;
        public String resultBody;
    }
}
