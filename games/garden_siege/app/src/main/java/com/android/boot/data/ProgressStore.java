package com.android.boot.data;

import android.content.Context;
import android.content.SharedPreferences;

public class ProgressStore {
    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        prefs = context.getSharedPreferences("garden_siege", Context.MODE_PRIVATE);
    }

    public int getUnlockedLevels() {
        return prefs.getInt("unlocked", 1);
    }

    public void unlockLevel(int level) {
        int next = Math.max(getUnlockedLevels(), Math.min(6, level));
        prefs.edit().putInt("unlocked", next).apply();
    }

    public long getBestMillis(int level) {
        return prefs.getLong("best_" + level, 0L);
    }

    public void updateBest(int level, long elapsed) {
        long old = getBestMillis(level);
        if (old == 0L || elapsed < old) {
            prefs.edit().putLong("best_" + level, elapsed).apply();
        }
    }

    public boolean isMuted() {
        return prefs.getBoolean("muted", false);
    }

    public void setMuted(boolean muted) {
        prefs.edit().putBoolean("muted", muted).apply();
    }

    public boolean isFpsEnabled() {
        return prefs.getBoolean("fps", false);
    }

    public void setFpsEnabled(boolean enabled) {
        prefs.edit().putBoolean("fps", enabled).apply();
    }
}
