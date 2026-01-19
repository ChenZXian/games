package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class ProgressStore {
    private static final String PREFS = "mosaic_studio_progress";
    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getBestStars(int level) {
        return prefs.getInt("stars_" + level, 0);
    }

    public int getBestScore(int level) {
        return prefs.getInt("score_" + level, 0);
    }

    public void setBest(int level, int stars, int score) {
        SharedPreferences.Editor editor = prefs.edit();
        int bestStars = getBestStars(level);
        int bestScore = getBestScore(level);
        if (stars > bestStars) {
            editor.putInt("stars_" + level, stars);
        }
        if (score > bestScore) {
            editor.putInt("score_" + level, score);
        }
        editor.apply();
    }

    public int getUnlocked() {
        return prefs.getInt("unlocked", 0);
    }

    public void setUnlocked(int level) {
        int unlocked = getUnlocked();
        if (level > unlocked) {
            prefs.edit().putInt("unlocked", level).apply();
        }
    }

    public boolean isMuted() {
        return prefs.getBoolean("muted", false);
    }

    public void setMuted(boolean muted) {
        prefs.edit().putBoolean("muted", muted).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean("dark_mode", false);
    }

    public void setDarkMode(boolean dark) {
        prefs.edit().putBoolean("dark_mode", dark).apply();
    }
}
