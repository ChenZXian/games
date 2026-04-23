package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.boot.entity.Convoy;
import com.android.boot.entity.FloatingText;
import com.android.boot.entity.HarborNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameSession {
    public static final int TOOL_ROUTE = 0;
    public static final int TOOL_SURGE = 1;
    public static final int TOOL_UPGRADE = 2;

    public final List<HarborNode> harbors = new ArrayList<>();
    public final List<Convoy> convoys = new ArrayList<>();
    public final List<FloatingText> texts = new ArrayList<>();
    public GameState state = GameState.MENU;
    public int activeTool = TOOL_ROUTE;
    public int commandPoints;
    public float stageTimeLeft;
    public int stageIndex;
    public int highestUnlockedStage = 1;
    public int totalStars;
    public int selectedHarborId = -1;
    public int pendingRouteSourceId = -1;
    public boolean lastStageWon;
    public int lastStageStars;
    public int convoyLosses;
    public String resultTitle = "";
    public String resultBody = "";

    private final Context context;
    private final SharedPreferences prefs;
    private final Random random = new Random();
    private final List<StageDefinition> stages = new ArrayList<>();
    private int[] bestStageStars;
    private float aiDecisionTimer;

    public GameSession(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("island_supply_command_progress", Context.MODE_PRIVATE);
        buildStages();
        bestStageStars = new int[stages.size()];
        loadProgress();
        for (int i = 0; i < 24; i++) {
            texts.add(new FloatingText());
        }
        returnToMenu();
    }

    public void loadProgress() {
        highestUnlockedStage = Math.max(1, Math.min(stages.size(), prefs.getInt("highest_unlocked_stage", 1)));
        String encodedStars = prefs.getString("best_stage_stars", "");
        if (encodedStars != null && !encodedStars.isEmpty()) {
            String[] parts = encodedStars.split(",");
            for (int i = 0; i < parts.length && i < bestStageStars.length; i++) {
                try {
                    bestStageStars[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException ignored) {
                    bestStageStars[i] = 0;
                }
            }
        }
        recalcTotalStars();
        stageIndex = Math.max(0, highestUnlockedStage - 1);
    }

    public void saveProgress() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bestStageStars.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(bestStageStars[i]);
        }
        prefs.edit()
                .putInt("highest_unlocked_stage", highestUnlockedStage)
                .putString("best_stage_stars", builder.toString())
                .apply();
    }

    public void setTool(int tool) {
        activeTool = tool;
        pendingRouteSourceId = -1;
    }

    public void startCampaign() {
        int startStage = Math.max(0, Math.min(stages.size() - 1, highestUnlockedStage - 1));
        startStage(startStage);
    }

    public void startNextStage() {
        int next = Math.min(stages.size() - 1, stageIndex + 1);
        startStage(next);
    }

    public boolean canAdvanceStage() {
        return stageIndex + 1 < stages.size() && highestUnlockedStage > stageIndex + 1;
    }

    public void startStage(int index) {
        stageIndex = Math.max(0, Math.min(stages.size() - 1, index));
        StageDefinition def = stages.get(stageIndex);
        harbors.clear();
        convoys.clear();
        selectedHarborId = -1;
        pendingRouteSourceId = -1;
        commandPoints = def.startCommandPoints;
        stageTimeLeft = def.timeLimitSec;
        convoyLosses = 0;
        lastStageWon = false;
        lastStageStars = 0;
        resultTitle = "";
        resultBody = "";
        aiDecisionTimer = 0f;
        for (StageNodeSpec spec : def.nodes) {
            HarborNode node = new HarborNode(spec.id, spec.name, spec.x, spec.y, spec.owner, spec.baseDefense, spec.production, spec.flagship);
            node.setLinks(spec.links);
            if (spec.owner == HarborNode.OWNER_PLAYER) {
                node.stock = node.maxStock() * 0.55f;
            } else if (spec.owner == HarborNode.OWNER_ENEMY) {
                node.stock = node.maxStock() * 0.4f;
            } else {
                node.stock = node.maxStock() * 0.2f;
            }
            harbors.add(node);
        }
        state = GameState.PLAYING;
    }

    public void restartStage() {
        startStage(stageIndex);
    }

    public void returnToMenu() {
        state = GameState.MENU;
        selectedHarborId = -1;
        pendingRouteSourceId = -1;
        resultTitle = "";
        resultBody = "";
    }

    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) {
            updateTexts(dt);
            return;
        }
        stageTimeLeft -= dt;
        if (stageTimeLeft <= 0f) {
            stageTimeLeft = 0f;
            failStage("Time Lost", "The fleet ran out of time before the enemy flagship broke.");
            updateTexts(dt);
            return;
        }
        updateHarbors(dt);
        updateAi(dt);
        updateConvoys(dt);
        updateTexts(dt);
        checkOutcome();
    }

    public void handleHarborTap(int harborId) {
        if (harborId < 0 || harborId >= harbors.size()) {
            selectedHarborId = -1;
            pendingRouteSourceId = -1;
            return;
        }
        HarborNode node = harbors.get(harborId);
        selectedHarborId = harborId;
        if (state != GameState.PLAYING) {
            return;
        }
        if (activeTool == TOOL_ROUTE) {
            handleRouteTap(node);
        } else if (activeTool == TOOL_SURGE) {
            useSurge(node);
        } else if (activeTool == TOOL_UPGRADE) {
            upgradeHarbor(node);
        }
    }

    public int getPlayerOwnedCount() {
        int count = 0;
        for (HarborNode harbor : harbors) {
            if (harbor.owner == HarborNode.OWNER_PLAYER) {
                count++;
            }
        }
        return count;
    }

    public int getDisplayStage() {
        return stageIndex + 1;
    }

    public int getStartStageNumber() {
        return Math.max(1, highestUnlockedStage);
    }

    private void handleRouteTap(HarborNode tapped) {
        if (tapped.owner == HarborNode.OWNER_PLAYER && pendingRouteSourceId == -1) {
            pendingRouteSourceId = tapped.id;
            spawnText(tapped.x, tapped.y, "Source");
            return;
        }
        if (pendingRouteSourceId == -1) {
            return;
        }
        HarborNode source = harbors.get(pendingRouteSourceId);
        if (source.owner != HarborNode.OWNER_PLAYER) {
            pendingRouteSourceId = -1;
            return;
        }
        if (!source.isLinkedTo(tapped.id)) {
            pendingRouteSourceId = tapped.owner == HarborNode.OWNER_PLAYER ? tapped.id : -1;
            return;
        }
        if (source.routeTargetId == tapped.id) {
            source.routeTargetId = -1;
            spawnText(tapped.x, tapped.y, "Route Off");
        } else {
            source.routeTargetId = tapped.id;
            spawnText(tapped.x, tapped.y, "Route On");
        }
        pendingRouteSourceId = -1;
    }

    private void useSurge(HarborNode node) {
        if (node.owner != HarborNode.OWNER_PLAYER) {
            return;
        }
        int cost = 35;
        if (commandPoints < cost) {
            spawnText(node.x, node.y, "Need 35");
            return;
        }
        commandPoints -= cost;
        node.stock = Math.min(node.maxStock(), node.stock + 30f);
        node.cannonBoostTimer = Math.max(node.cannonBoostTimer, 6f);
        spawnText(node.x, node.y, "Surge");
    }

    private void upgradeHarbor(HarborNode node) {
        if (node.owner != HarborNode.OWNER_PLAYER) {
            return;
        }
        if (node.dockLevel >= 2 && node.armorLevel >= 2 && node.cannonLevel >= 2) {
            spawnText(node.x, node.y, "Maxed");
            return;
        }
        int cost = 40 + node.totalUpgradeLevel() * 15;
        if (commandPoints < cost) {
            spawnText(node.x, node.y, "Need " + cost);
            return;
        }
        commandPoints -= cost;
        if (node.dockLevel <= node.armorLevel && node.dockLevel <= node.cannonLevel && node.dockLevel < 2) {
            node.dockLevel++;
            spawnText(node.x, node.y, "Dock +" + node.dockLevel);
        } else if (node.armorLevel <= node.cannonLevel && node.armorLevel < 2) {
            node.armorLevel++;
            spawnText(node.x, node.y, "Hull +" + node.armorLevel);
        } else if (node.cannonLevel < 2) {
            node.cannonLevel++;
            spawnText(node.x, node.y, "Cannon +" + node.cannonLevel);
        } else if (node.dockLevel < 2) {
            node.dockLevel++;
            spawnText(node.x, node.y, "Dock +" + node.dockLevel);
        } else {
            node.armorLevel++;
            spawnText(node.x, node.y, "Hull +" + node.armorLevel);
        }
        node.refreshDefenseAfterUpgrade();
    }

    private void updateHarbors(float dt) {
        for (HarborNode node : harbors) {
            if (node.owner != HarborNode.OWNER_NEUTRAL) {
                float ownerBoost = node.owner == HarborNode.OWNER_PLAYER ? 1f : 1.08f;
                node.stock = Math.min(node.maxStock(), node.stock + node.production * ownerBoost * dt);
            }
            if (node.cannonBoostTimer > 0f) {
                node.cannonBoostTimer = Math.max(0f, node.cannonBoostTimer - dt);
            }
            if (node.routeTargetId >= 0) {
                node.convoyCooldown -= dt;
                if (node.convoyCooldown <= 0f) {
                    spawnConvoyIfReady(node);
                }
            } else {
                node.convoyCooldown = 0f;
            }
        }
    }

    private void updateAi(float dt) {
        aiDecisionTimer += dt;
        if (aiDecisionTimer < 1.4f) {
            return;
        }
        aiDecisionTimer = 0f;
        for (HarborNode node : harbors) {
            if (node.owner != HarborNode.OWNER_ENEMY) {
                continue;
            }
            HarborNode target = chooseEnemyTarget(node);
            node.routeTargetId = target != null ? target.id : -1;
        }
    }

    private HarborNode chooseEnemyTarget(HarborNode source) {
        HarborNode best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int linkedId : source.links) {
            HarborNode target = harbors.get(linkedId);
            float score;
            if (target.owner == HarborNode.OWNER_PLAYER) {
                score = 120f - target.defense;
                if (target.flagship) {
                    score += 40f;
                }
            } else if (target.owner == HarborNode.OWNER_NEUTRAL) {
                score = 80f - target.defense * 0.6f + target.production * 4f;
            } else {
                score = 18f + Math.max(0f, target.maxStock() - target.stock);
            }
            if (score > bestScore) {
                bestScore = score;
                best = target;
            }
        }
        return best;
    }

    private void spawnConvoyIfReady(HarborNode source) {
        if (source.routeTargetId < 0 || source.routeTargetId >= harbors.size()) {
            source.routeTargetId = -1;
            return;
        }
        HarborNode target = harbors.get(source.routeTargetId);
        if (!source.isLinkedTo(target.id)) {
            source.routeTargetId = -1;
            return;
        }
        float threshold = source.owner == target.owner ? 16f : 22f;
        if (source.stock < threshold) {
            source.convoyCooldown = 0.6f;
            return;
        }
        float cargo = Math.min(source.stock * 0.45f, 18f + source.dockLevel * 6f);
        if (cargo < 8f) {
            source.convoyCooldown = 0.6f;
            return;
        }
        source.stock -= cargo;
        float distance = distance(source.x, source.y, target.x, target.y);
        float speed = 0.25f / Math.max(0.22f, distance);
        Convoy convoy = new Convoy(source.id, target.id, source.owner, cargo, source.convoyHp(), speed);
        convoys.add(convoy);
        source.convoyCooldown = source.launchCooldown();
    }

    private void updateConvoys(float dt) {
        for (int i = convoys.size() - 1; i >= 0; i--) {
            Convoy convoy = convoys.get(i);
            HarborNode source = harbors.get(convoy.sourceId);
            HarborNode target = harbors.get(convoy.targetId);
            convoy.progress += convoy.speed * dt;
            applyInterception(convoy, source, target, dt);
            if (convoy.hp <= 0f) {
                if (convoy.owner == HarborNode.OWNER_PLAYER) {
                    convoyLosses++;
                }
                spawnText(midpoint(source.x, target.x), midpoint(source.y, target.y), "Lost");
                convoys.remove(i);
                continue;
            }
            if (convoy.progress >= 1f) {
                onConvoyArrive(convoy, target);
                convoys.remove(i);
            }
        }
    }

    private void applyInterception(Convoy convoy, HarborNode source, HarborNode target, float dt) {
        float convoyX = source.x + (target.x - source.x) * convoy.progress;
        float convoyY = source.y + (target.y - source.y) * convoy.progress;
        for (HarborNode node : harbors) {
            if (node.owner == HarborNode.OWNER_NEUTRAL || node.owner == convoy.owner) {
                continue;
            }
            float range = 0.08f + node.cannonLevel * 0.012f + (node.flagship ? 0.012f : 0f);
            if (distance(node.x, node.y, convoyX, convoyY) <= range && node.stock > 4f) {
                convoy.hp -= node.interceptionDamage() * dt;
                node.stock = Math.max(0f, node.stock - dt * 1.5f);
            }
        }
    }

    private void onConvoyArrive(Convoy convoy, HarborNode target) {
        if (target.owner == convoy.owner) {
            target.stock = Math.min(target.maxStock(), target.stock + convoy.cargo);
            if (convoy.owner == HarborNode.OWNER_PLAYER) {
                commandPoints += 4;
                spawnText(target.x, target.y, "+" + 4 + " CP");
            }
            return;
        }
        float pressure = convoy.cargo * 1.15f;
        target.defense -= pressure;
        if (convoy.owner == HarborNode.OWNER_PLAYER) {
            commandPoints += 2;
        }
        if (target.defense <= 0f) {
            int oldOwner = target.owner;
            target.capture(convoy.owner);
            target.stock = Math.min(target.maxStock(), convoy.cargo * 0.45f + 8f);
            if (convoy.owner == HarborNode.OWNER_PLAYER) {
                commandPoints += target.flagship ? 55 : 22;
                spawnText(target.x, target.y, target.flagship ? "Flagship Down" : "Captured");
            } else if (oldOwner == HarborNode.OWNER_PLAYER) {
                spawnText(target.x, target.y, "Harbor Lost");
            }
        } else {
            spawnText(target.x, target.y, "-" + (int) pressure);
        }
    }

    private void checkOutcome() {
        HarborNode playerFlagship = null;
        HarborNode enemyFlagship = null;
        int playerOwned = 0;
        int enemyOwned = 0;
        for (HarborNode harbor : harbors) {
            if (harbor.owner == HarborNode.OWNER_PLAYER) {
                playerOwned++;
            } else if (harbor.owner == HarborNode.OWNER_ENEMY) {
                enemyOwned++;
            }
            if (harbor.flagship && harbor.owner == HarborNode.OWNER_PLAYER) {
                playerFlagship = harbor;
            }
            if (harbor.flagship && harbor.owner == HarborNode.OWNER_ENEMY) {
                enemyFlagship = harbor;
            }
        }
        if (playerOwned == 0 || playerFlagship == null) {
            failStage("Flagship Lost", "The player flagship harbor was taken and the convoy line collapsed.");
        } else if (enemyOwned == 0 || enemyFlagship == null) {
            winStage();
        }
    }

    private void winStage() {
        if (state == GameState.GAME_OVER) {
            return;
        }
        lastStageWon = true;
        lastStageStars = 1;
        if (stageTimeLeft >= 45f) {
            lastStageStars++;
        }
        if (convoyLosses <= 3) {
            lastStageStars++;
        }
        if (lastStageStars > bestStageStars[stageIndex]) {
            bestStageStars[stageIndex] = lastStageStars;
            recalcTotalStars();
        }
        if (stageIndex + 2 > highestUnlockedStage) {
            highestUnlockedStage = Math.min(stages.size(), stageIndex + 2);
        }
        resultTitle = "Mission Clear";
        resultBody = String.format(Locale.US,
                "Stage %d cleared\nStars %d\nTime left %ds\nConvoys lost %d\nUnlocked stage %d",
                getDisplayStage(),
                lastStageStars,
                (int) stageTimeLeft,
                convoyLosses,
                highestUnlockedStage);
        state = GameState.GAME_OVER;
        saveProgress();
    }

    private void failStage(String title, String body) {
        if (state == GameState.GAME_OVER) {
            return;
        }
        lastStageWon = false;
        lastStageStars = 0;
        resultTitle = title;
        resultBody = body + String.format(Locale.US, "\nStage %d\nConvoys lost %d", getDisplayStage(), convoyLosses);
        state = GameState.GAME_OVER;
        saveProgress();
    }

    private void updateTexts(float dt) {
        for (FloatingText text : texts) {
            if (text.life > 0f) {
                text.life -= dt;
                text.y -= dt * 0.02f;
            }
        }
    }

    private void spawnText(float x, float y, String message) {
        for (FloatingText text : texts) {
            if (text.life <= 0f) {
                text.set(x, y, message);
                return;
            }
        }
    }

    private void recalcTotalStars() {
        totalStars = 0;
        for (int star : bestStageStars) {
            totalStars += star;
        }
    }

    private float midpoint(float a, float b) {
        return (a + b) * 0.5f;
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void buildStages() {
        stages.add(new StageDefinition(
                200f,
                55,
                new StageNodeSpec[]{
                        new StageNodeSpec(0, "Harbor Dawn", 0.11f, 0.56f, HarborNode.OWNER_PLAYER, 78f, 6.2f, true, new int[]{1, 2}),
                        new StageNodeSpec(1, "North Pier", 0.29f, 0.33f, HarborNode.OWNER_NEUTRAL, 44f, 4.2f, false, new int[]{0, 3}),
                        new StageNodeSpec(2, "Shell Bay", 0.31f, 0.75f, HarborNode.OWNER_NEUTRAL, 42f, 4.0f, false, new int[]{0, 3}),
                        new StageNodeSpec(3, "Mid Relay", 0.54f, 0.55f, HarborNode.OWNER_ENEMY, 60f, 5.4f, false, new int[]{1, 2, 4, 5}),
                        new StageNodeSpec(4, "Sun Dock", 0.74f, 0.31f, HarborNode.OWNER_NEUTRAL, 48f, 4.4f, false, new int[]{3, 5}),
                        new StageNodeSpec(5, "Iron Admiral", 0.88f, 0.64f, HarborNode.OWNER_ENEMY, 82f, 6.0f, true, new int[]{3, 4})
                }
        ));
        stages.add(new StageDefinition(
                220f,
                65,
                new StageNodeSpec[]{
                        new StageNodeSpec(0, "Harbor Dawn", 0.10f, 0.52f, HarborNode.OWNER_PLAYER, 82f, 6.3f, true, new int[]{1, 2}),
                        new StageNodeSpec(1, "Blue Fork", 0.25f, 0.27f, HarborNode.OWNER_NEUTRAL, 46f, 4.3f, false, new int[]{0, 3}),
                        new StageNodeSpec(2, "Lagoon Gate", 0.25f, 0.76f, HarborNode.OWNER_NEUTRAL, 46f, 4.3f, false, new int[]{0, 4}),
                        new StageNodeSpec(3, "Crosswind", 0.47f, 0.29f, HarborNode.OWNER_ENEMY, 56f, 5.2f, false, new int[]{1, 5}),
                        new StageNodeSpec(4, "Tide Fold", 0.47f, 0.76f, HarborNode.OWNER_NEUTRAL, 54f, 4.8f, false, new int[]{2, 5}),
                        new StageNodeSpec(5, "Glass Inlet", 0.67f, 0.53f, HarborNode.OWNER_ENEMY, 68f, 5.8f, false, new int[]{3, 4, 6}),
                        new StageNodeSpec(6, "Razor Flagship", 0.88f, 0.52f, HarborNode.OWNER_ENEMY, 88f, 6.2f, true, new int[]{5})
                }
        ));
        stages.add(new StageDefinition(
                240f,
                75,
                new StageNodeSpec[]{
                        new StageNodeSpec(0, "Harbor Dawn", 0.10f, 0.56f, HarborNode.OWNER_PLAYER, 84f, 6.4f, true, new int[]{1, 2}),
                        new StageNodeSpec(1, "Coral Arc", 0.24f, 0.24f, HarborNode.OWNER_NEUTRAL, 48f, 4.5f, false, new int[]{0, 3, 4}),
                        new StageNodeSpec(2, "Manta Loop", 0.24f, 0.78f, HarborNode.OWNER_NEUTRAL, 48f, 4.5f, false, new int[]{0, 4, 5}),
                        new StageNodeSpec(3, "Beacon Crown", 0.44f, 0.18f, HarborNode.OWNER_ENEMY, 58f, 5.2f, false, new int[]{1, 6}),
                        new StageNodeSpec(4, "Spine Bastion", 0.48f, 0.50f, HarborNode.OWNER_NEUTRAL, 72f, 5.6f, false, new int[]{1, 2, 6, 7}),
                        new StageNodeSpec(5, "Kelp Drift", 0.42f, 0.82f, HarborNode.OWNER_ENEMY, 58f, 5.0f, false, new int[]{2, 7}),
                        new StageNodeSpec(6, "Red Current", 0.70f, 0.30f, HarborNode.OWNER_ENEMY, 64f, 5.8f, false, new int[]{3, 4, 8}),
                        new StageNodeSpec(7, "Storm Keys", 0.69f, 0.71f, HarborNode.OWNER_ENEMY, 64f, 5.8f, false, new int[]{4, 5, 8}),
                        new StageNodeSpec(8, "Dread Anchorage", 0.89f, 0.50f, HarborNode.OWNER_ENEMY, 92f, 6.3f, true, new int[]{6, 7})
                }
        ));
        stages.add(new StageDefinition(
                260f,
                82,
                new StageNodeSpec[]{
                        new StageNodeSpec(0, "Harbor Dawn", 0.10f, 0.50f, HarborNode.OWNER_PLAYER, 86f, 6.5f, true, new int[]{1, 2}),
                        new StageNodeSpec(1, "Top Sails", 0.24f, 0.20f, HarborNode.OWNER_NEUTRAL, 50f, 4.4f, false, new int[]{0, 3}),
                        new StageNodeSpec(2, "Deep Wake", 0.24f, 0.80f, HarborNode.OWNER_NEUTRAL, 50f, 4.4f, false, new int[]{0, 4}),
                        new StageNodeSpec(3, "Shard Port", 0.45f, 0.18f, HarborNode.OWNER_ENEMY, 60f, 5.3f, false, new int[]{1, 5, 6}),
                        new StageNodeSpec(4, "Pearl Gate", 0.45f, 0.82f, HarborNode.OWNER_ENEMY, 60f, 5.3f, false, new int[]{2, 6, 7}),
                        new StageNodeSpec(5, "Upper Relay", 0.63f, 0.30f, HarborNode.OWNER_NEUTRAL, 60f, 5.0f, false, new int[]{3, 8}),
                        new StageNodeSpec(6, "Core Sound", 0.58f, 0.50f, HarborNode.OWNER_NEUTRAL, 74f, 5.8f, false, new int[]{3, 4, 8, 9}),
                        new StageNodeSpec(7, "Lower Relay", 0.63f, 0.70f, HarborNode.OWNER_NEUTRAL, 60f, 5.0f, false, new int[]{4, 9}),
                        new StageNodeSpec(8, "Flare Spoke", 0.80f, 0.34f, HarborNode.OWNER_ENEMY, 70f, 6.0f, false, new int[]{5, 6, 10}),
                        new StageNodeSpec(9, "Anchor Spoke", 0.80f, 0.66f, HarborNode.OWNER_ENEMY, 70f, 6.0f, false, new int[]{6, 7, 10}),
                        new StageNodeSpec(10, "Black Flag Port", 0.91f, 0.50f, HarborNode.OWNER_ENEMY, 96f, 6.5f, true, new int[]{8, 9})
                }
        ));
        stages.add(new StageDefinition(
                280f,
                90,
                new StageNodeSpec[]{
                        new StageNodeSpec(0, "Harbor Dawn", 0.09f, 0.50f, HarborNode.OWNER_PLAYER, 90f, 6.6f, true, new int[]{1, 2}),
                        new StageNodeSpec(1, "North Ring", 0.20f, 0.18f, HarborNode.OWNER_NEUTRAL, 54f, 4.6f, false, new int[]{0, 3, 4}),
                        new StageNodeSpec(2, "South Ring", 0.20f, 0.82f, HarborNode.OWNER_NEUTRAL, 54f, 4.6f, false, new int[]{0, 4, 5}),
                        new StageNodeSpec(3, "Foam Apex", 0.38f, 0.14f, HarborNode.OWNER_ENEMY, 62f, 5.2f, false, new int[]{1, 6}),
                        new StageNodeSpec(4, "Atlas Reef", 0.42f, 0.50f, HarborNode.OWNER_NEUTRAL, 78f, 6.0f, false, new int[]{1, 2, 6, 7}),
                        new StageNodeSpec(5, "Wake Hollow", 0.38f, 0.86f, HarborNode.OWNER_ENEMY, 62f, 5.2f, false, new int[]{2, 7}),
                        new StageNodeSpec(6, "Signal Crest", 0.61f, 0.25f, HarborNode.OWNER_ENEMY, 70f, 6.1f, false, new int[]{3, 4, 8}),
                        new StageNodeSpec(7, "Storm Shelf", 0.61f, 0.75f, HarborNode.OWNER_ENEMY, 70f, 6.1f, false, new int[]{4, 5, 9}),
                        new StageNodeSpec(8, "Iron Choke", 0.79f, 0.34f, HarborNode.OWNER_NEUTRAL, 74f, 6.0f, false, new int[]{6, 10}),
                        new StageNodeSpec(9, "Torch Choke", 0.79f, 0.66f, HarborNode.OWNER_NEUTRAL, 74f, 6.0f, false, new int[]{7, 10}),
                        new StageNodeSpec(10, "Dread Armada", 0.92f, 0.50f, HarborNode.OWNER_ENEMY, 104f, 6.8f, true, new int[]{8, 9})
                }
        ));
    }

    private static final class StageDefinition {
        final float timeLimitSec;
        final int startCommandPoints;
        final StageNodeSpec[] nodes;

        StageDefinition(float timeLimitSec, int startCommandPoints, StageNodeSpec[] nodes) {
            this.timeLimitSec = timeLimitSec;
            this.startCommandPoints = startCommandPoints;
            this.nodes = nodes;
        }
    }

    private static final class StageNodeSpec {
        final int id;
        final String name;
        final float x;
        final float y;
        final int owner;
        final float baseDefense;
        final float production;
        final boolean flagship;
        final int[] links;

        StageNodeSpec(int id, String name, float x, float y, int owner, float baseDefense, float production, boolean flagship, int[] links) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.baseDefense = baseDefense;
            this.production = production;
            this.flagship = flagship;
            this.links = links;
        }
    }
}
