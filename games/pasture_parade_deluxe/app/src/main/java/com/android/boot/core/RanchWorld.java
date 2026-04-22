package com.android.boot.core;

import com.android.boot.entity.Animal;
import com.android.boot.entity.AnimalSpecies;
import com.android.boot.entity.DeliveryOrder;
import com.android.boot.entity.Pen;
import com.android.boot.fx.FloatText;
import com.android.boot.fx.ParticleSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RanchWorld {
    public final List<Pen> pens = new ArrayList<>();
    public final EconomySystem economy = new EconomySystem();
    public final UpgradeSystem upgrades = new UpgradeSystem();
    public final DeliveryBoard board = new DeliveryBoard();
    public final AchievementManager achievements = new AchievementManager();
    public final ParticleSystem particles = new ParticleSystem();
    public final FloatText[] floatTexts = new FloatText[24];
    public final Map<AnimalSpecies, Integer> inventory = new HashMap<>();
    public RanchState state = RanchState.MENU;
    public Pen selected;
    public float neglect;
    public float neglectMaxTime;
    public float scrollX;
    public int deliveriesDone;
    public int sessionCoins;

    public RanchWorld() {
        for (int i = 0; i < floatTexts.length; i++) floatTexts[i] = new FloatText();
        AnimalSpecies[] s = AnimalSpecies.values();
        float x = 80f;
        for (int i = 0; i < s.length; i++) {
            boolean unlocked = i < 3;
            Pen pen = new Pen(i, new Animal(s[i], x + 60f, 260f + (i % 2) * 180f), unlocked, x, 210f + (i % 2) * 180f, 180f, 140f);
            pens.add(pen);
            x += 200f;
        }
        board.seed(economy.ranchLevel);
    }

    public void startGame() {
        state = RanchState.PLAYING;
        neglect = 0f;
        neglectMaxTime = 0f;
        sessionCoins = 0;
    }

    public void update(float dt) {
        if (state != RanchState.PLAYING) return;
        economy.update(dt, upgrades.comboRetention);
        float neglectAdd = 0f;
        for (Pen pen : pens) {
            float speed = 1f + adjacencySpeedBonus(pen) + premiumComboBonus(pen);
            float value = 1f + adjacencyValueBonus(pen);
            pen.update(dt, speed, value);
            if (pen.isNeglected() && pen.unlocked) neglectAdd += 0.04f;
        }
        neglect += (neglectAdd - upgrades.neglectResistance * 0.03f) * dt;
        if (neglect < 0f) neglect = 0f;
        if (neglect > 1f) {
            neglect = 1f;
            neglectMaxTime += dt;
            if (neglectMaxTime > 6f) {
                state = RanchState.GAME_OVER;
            }
        } else {
            neglectMaxTime = 0f;
        }
        particles.update(dt);
        for (FloatText ft : floatTexts) {
            if (ft.time > 0f) {
                ft.time -= dt;
                ft.y -= 20f * dt;
            }
        }
        checkDeliveries();
    }

    private float adjacencySpeedBonus(Pen current) {
        float bonus = 0f;
        for (Pen p : pens) {
            if (p == current || !p.unlocked) continue;
            if (Math.abs(p.x - current.x) <= 230f && p.animal.species.isBird() && current.animal.species.isBird()) bonus += 0.05f;
        }
        return bonus;
    }

    private float adjacencyValueBonus(Pen current) {
        float bonus = 0f;
        for (Pen p : pens) {
            if (p == current || !p.unlocked) continue;
            if (Math.abs(p.x - current.x) <= 230f && p.animal.species.isWool() && current.animal.species.isWool()) bonus += 0.07f;
        }
        return bonus;
    }

    private float premiumComboBonus(Pen current) {
        int premium = 0;
        for (Pen p : pens) if (p.unlocked && p.animal.species.isPremium()) premium++;
        return premium >= 3 && current.animal.species.isPremium() ? 0.08f : 0f;
    }

    private void checkDeliveries() {
        for (int i = 0; i < board.active.size(); i++) {
            DeliveryOrder o = board.active.get(i);
            int amount = inventory.containsKey(o.species) ? inventory.get(o.species) : 0;
            if (amount >= o.quantity) {
                inventory.put(o.species, amount - o.quantity);
                int coinGain = (int) (o.rewardCoins * (1f + upgrades.deliveryBonus) * economy.combo);
                economy.addCoins(coinGain);
                economy.addXp(o.rewardXp);
                economy.hitCombo();
                deliveriesDone++;
                sessionCoins += coinGain;
                spawnText("+" + coinGain, 420f, 120f, 0xFF2ED67A);
                board.replace(i, economy.ranchLevel);
            }
        }
    }

    public void selectPen(float px, float py) {
        for (Pen p : pens) {
            if (p.contains(px + scrollX, py)) {
                selected = p;
                p.animal.onTap();
                return;
            }
        }
    }

    public void feedSelected() {
        if (selected == null || !selected.unlocked) return;
        selected.feed = Math.min(1f, selected.feed + 0.65f + upgrades.careEfficiency * 0.1f);
        progress("care_20", 1);
        progress("care_200", 1);
    }

    public void cleanSelected() {
        if (selected == null || !selected.unlocked) return;
        selected.cleanliness = Math.min(1f, selected.cleanliness + 0.7f + upgrades.careEfficiency * 0.1f);
        progress("clean_20", 1);
        progress("clean_200", 1);
    }

    public void collectSelected() {
        if (selected == null || !selected.unlocked || selected.storedGoods <= 0) return;
        selected.storedGoods--;
        int value = (int) (selected.animal.species.baseValue * selected.valueBonus * economy.combo);
        economy.addCoins(value);
        sessionCoins += value;
        int count = inventory.containsKey(selected.animal.species) ? inventory.get(selected.animal.species) : 0;
        inventory.put(selected.animal.species, count + 1);
        particles.burst(selected.x - scrollX + selected.w * 0.5f, selected.y + 30f);
        economy.hitCombo();
        spawnText("+" + value, selected.x - scrollX + 40f, selected.y, 0xFFFFB22B);
        progress("collect_50", 1);
        progress("collect_500", 1);
    }

    public void upgradeSelected() {
        if (selected == null || !selected.unlocked) return;
        int cost = 60 + selected.level * 40;
        if (economy.coins < cost) return;
        economy.coins -= cost;
        selected.level++;
        selected.capacity++;
        selected.speedBonus += 0.1f;
        selected.valueBonus += 0.08f;
        selected.autoClean = Math.min(0.4f, selected.autoClean + 0.05f);
        selected.feedDuration += 0.08f;
        progress("upgrade_3", 1);
        progress("upgrade_10", 1);
    }

    public void unlockByLevel() {
        for (Pen p : pens) {
            if (!p.unlocked && economy.ranchLevel >= p.animal.species.unlockLevel / 12 && economy.coins >= p.animal.species.baseValue * 4) {
                p.unlocked = true;
                economy.coins -= p.animal.species.baseValue * 4;
                spawnText("Unlocked " + p.animal.species.name(), p.x - scrollX, p.y - 30f, 0xFF7F68FF);
            }
        }
    }

    private void spawnText(String text, float x, float y, int color) {
        for (FloatText ft : floatTexts) {
            if (ft.time <= 0f) {
                ft.set(text, x, y, color);
                break;
            }
        }
    }

    private void progress(String id, int amount) {
        for (Achievement a : achievements.list) {
            if (a.id.equals(id)) {
                a.progress += amount;
                return;
            }
        }
    }
}
