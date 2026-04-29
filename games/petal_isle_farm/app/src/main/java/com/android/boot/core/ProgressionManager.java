package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class ProgressionManager {
    private final SharedPreferences pref;
    public int coins;
    public int level;
    public int xp;
    public int expansionTier;
    public int storageCapacity;

    public ProgressionManager(Context context) {
        pref = context.getSharedPreferences("progress", Context.MODE_PRIVATE);
        coins = pref.getInt("coins", 120);
        level = pref.getInt("level", 1);
        xp = pref.getInt("xp", 0);
        expansionTier = pref.getInt("expansion", 0);
        storageCapacity = pref.getInt("storageCap", 40);
    }

    public void addReward(int addCoins, int addXp) {
        coins += addCoins;
        xp += addXp;
        while (xp >= level * 120) {
            xp -= level * 120;
            level++;
        }
    }

    public boolean spend(int cost) {
        if (coins < cost) return false;
        coins -= cost;
        return true;
    }

    public void save() {
        pref.edit().putInt("coins", coins).putInt("level", level).putInt("xp", xp).putInt("expansion", expansionTier).putInt("storageCap", storageCapacity).apply();
    }
}
