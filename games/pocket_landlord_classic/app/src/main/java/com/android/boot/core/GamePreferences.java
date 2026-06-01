package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class GamePreferences {
    private static final String PREFS = "pocket_landlord_classic";
    private final SharedPreferences prefs;

    public GamePreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getUnlockedStage() {
        return prefs.getInt("unlocked_stage", 0);
    }

    public void setUnlockedStage(int stageIndex) {
        prefs.edit().putInt("unlocked_stage", stageIndex).apply();
    }

    public int getStars() {
        return prefs.getInt("stars", 0);
    }

    public void addStars(int delta) {
        prefs.edit().putInt("stars", getStars() + Math.max(0, delta)).apply();
    }

    public int getCoins() {
        return prefs.getInt("coins", 0);
    }

    public void addCoins(int delta) {
        prefs.edit().putInt("coins", getCoins() + Math.max(0, delta)).apply();
    }

    public boolean isMuted() {
        return prefs.getBoolean("muted", false);
    }

    public void setMuted(boolean muted) {
        prefs.edit().putBoolean("muted", muted).apply();
    }
}
