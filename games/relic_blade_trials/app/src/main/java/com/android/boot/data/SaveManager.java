package com.android.boot.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveManager {
    private static final String PREF = "relic_blade_trials_save";
    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public int getCoins() {
        return prefs.getInt("coins", 0);
    }

    public void setCoins(int coins) {
        prefs.edit().putInt("coins", coins).apply();
    }

    public int getUnlockedStage() {
        return prefs.getInt("unlocked_stage", 1);
    }

    public void setUnlockedStage(int stage) {
        prefs.edit().putInt("unlocked_stage", stage).apply();
    }

    public String getInventory() {
        return prefs.getString("inventory", "");
    }

    public void setInventory(String raw) {
        prefs.edit().putString("inventory", raw).apply();
    }

    public String getEquipped() {
        return prefs.getString("equipped", "WEAPON:wooden_sword");
    }

    public void setEquipped(String raw) {
        prefs.edit().putString("equipped", raw).apply();
    }
}
