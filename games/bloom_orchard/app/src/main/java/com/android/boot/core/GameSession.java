package com.android.boot.core;

import android.content.Context;

import com.android.boot.entity.CropPlot;
import com.android.boot.entity.CropType;
import com.android.boot.entity.FloatingText;
import com.android.boot.entity.OrderData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameSession {
    public final CropCatalog cropCatalog;
    public final ProgressionManager progression;
    public final StorageManager storage;
    public final OrderManager orders;
    public final CodexManager codex;
    public final TaskManager tasks;
    public final LoginRewardManager login;
    public final AchievementManager achievements;
    public final WeatherManager weather;
    public final List<CropPlot> plots = new ArrayList<>();
    public final List<FloatingText> texts = new ArrayList<>();
    private final Random random = new Random();
    public GameState state = GameState.MENU;
    public float sessionLeft = 480f;
    public int combo;
    public int bestBeauty;
    public OrderData activeOrder;
    public int orderProgress;
    public int ordersCompleted;
    public int pestPlotIndex = -1;
    private float pestTimer;
    private float comboDecayTimer;

    public GameSession(Context context) {
        cropCatalog = new CropCatalog();
        progression = new ProgressionManager(context);
        storage = new StorageManager(context);
        orders = new OrderManager(context);
        codex = new CodexManager(context);
        tasks = new TaskManager(context);
        login = new LoginRewardManager(context);
        achievements = new AchievementManager(context);
        weather = new WeatherManager();
        int total = 36;
        int unlocked = progression.expansionTier == 0 ? 12 : progression.expansionTier == 1 ? 20 : progression.expansionTier == 2 ? 30 : 36;
        for (int i = 0; i < total; i++) {
            plots.add(new CropPlot(i, i < unlocked));
        }
        for (int i = 0; i < 40; i++) {
            texts.add(new FloatingText());
            texts.get(i).life = 0f;
        }
    }

    public void start() {
        state = GameState.PLAYING;
        sessionLeft = 480f;
        combo = 0;
        orderProgress = 0;
        ordersCompleted = 0;
        pestPlotIndex = -1;
        pestTimer = 0f;
        comboDecayTimer = 0f;
        if (orders.activeOrders.isEmpty()) orders.refresh();
        activeOrder = orders.activeOrders.get(random.nextInt(orders.activeOrders.size()));
        for (CropPlot p : plots) {
            p.crop = null;
            p.growth = 0f;
            p.mature = false;
            p.watered = 0;
            p.fertilizer = "NONE";
        }
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) return;
        sessionLeft -= dt;
        if (sessionLeft <= 0f) {
            state = GameState.GAME_OVER;
            progression.save();
            storage.save();
            return;
        }
        weather.update(dt);
        updatePest(dt);
        updateComboDecay(dt);
        float weatherMul = weather.state.growthBoost;
        for (CropPlot p : plots) {
            if (!p.unlocked || p.crop == null || p.mature) continue;
            if (p.index == pestPlotIndex) continue;
            float grow = dt / p.crop.baseGrowTimeSec;
            if (p.watered > 0) grow *= 1.2f;
            if ("YIELD".equals(p.fertilizer)) grow *= 1.06f;
            p.growth += grow * weatherMul;
            p.sway += dt;
            p.sparkle += dt * 2f;
            if (p.growth >= 1f) {
                p.growth = 1f;
                p.mature = true;
            }
        }
        for (FloatingText t : texts) {
            if (t.life > 0f) {
                t.life -= dt;
                t.y -= dt * 24f;
            }
        }
    }

    public void plant(CropPlot p, CropType crop) {
        if (!p.unlocked || !p.isEmpty()) return;
        if (!progression.spend(crop.seedCost)) return;
        p.crop = crop;
        p.growth = 0f;
        p.mature = false;
        p.watered = 0;
        p.fertilizer = "NONE";
    }

    public void water(CropPlot p) {
        if (p.index == pestPlotIndex) {
            pestPlotIndex = -1;
            combo = Math.min(combo + 1, 99);
            spawnText("Pest Cleared", 60 + p.index * 6, 180 + p.index * 4);
            return;
        }
        if (p.crop == null || p.mature) return;
        p.watered++;
        p.growth = Math.min(1f, p.growth + 0.14f);
    }

    public void fertilizer(CropPlot p, String type) {
        if (p.crop == null || p.mature) return;
        p.fertilizer = type;
    }

    public void harvest(CropPlot p) {
        if (p.crop == null || !p.mature) return;
        String harvestedCropId = p.crop.id;
        float beauty = p.crop.matureBeautyBase;
        beauty += p.watered * 4f;
        if (p.crop.fertilizerAffinity.equals(p.fertilizer)) beauty += 10f;
        beauty *= weather.state.beautyBoost;
        int beautyInt = (int) beauty;
        bestBeauty = Math.max(bestBeauty, beautyInt);
        int coinReward = p.crop.sellValue + beautyInt / 8 + combo * 2;
        int xp = p.crop.xpValue + beautyInt / 14;
        progression.addReward(coinReward, xp);
        storage.add(p.crop.id, p.crop.storageYield, progression.storageCapacity);
        codex.onHarvest(p.crop.id, beautyInt, weather.state.name());
        combo++;
        comboDecayTimer = 0f;
        onHarvestForOrder(harvestedCropId, beautyInt);
        spawnText("+" + coinReward + "c B" + beautyInt, 60 + p.index * 6, 180 + p.index * 4);
        p.crop = null;
        p.growth = 0f;
        p.mature = false;
        p.watered = 0;
        p.fertilizer = "NONE";
        achievements.unlock("First Seed");
    }

    private void onHarvestForOrder(String cropId, int beautyInt) {
        if (activeOrder == null) return;
        if (!activeOrder.cropId.equals(cropId)) return;
        orderProgress++;
        if (orderProgress >= activeOrder.count) {
            progression.addReward(activeOrder.coins + beautyInt / 6, activeOrder.xp);
            ordersCompleted++;
            orderProgress = 0;
            activeOrder = orders.activeOrders.get(random.nextInt(orders.activeOrders.size()));
            spawnText("Order Complete", 44f, 140f);
        }
    }

    private void updatePest(float dt) {
        pestTimer += dt;
        if (pestPlotIndex >= 0) {
            CropPlot p = plots.get(pestPlotIndex);
            if (p.crop == null || p.mature || !p.unlocked) pestPlotIndex = -1;
            return;
        }
        if (pestTimer < 18f) return;
        pestTimer = 0f;
        List<Integer> candidates = new ArrayList<>();
        for (CropPlot p : plots) {
            if (p.unlocked && p.crop != null && !p.mature) candidates.add(p.index);
        }
        if (candidates.isEmpty()) return;
        pestPlotIndex = candidates.get(random.nextInt(candidates.size()));
    }

    private void updateComboDecay(float dt) {
        if (combo <= 0) return;
        comboDecayTimer += dt;
        if (comboDecayTimer >= 9f) {
            comboDecayTimer = 0f;
            combo = Math.max(0, combo - 1);
        }
    }

    public String getObjectiveText() {
        if (activeOrder == null) return "Objective: Grow and harvest crops";
        return "Order " + activeOrder.cropId + " " + orderProgress + "/" + activeOrder.count + " Done " + ordersCompleted;
    }

    private void spawnText(String text, float x, float y) {
        for (FloatingText t : texts) {
            if (t.life <= 0f) {
                t.set(x, y, text);
                break;
            }
        }
    }
}
