package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveRepository {
    private static final String PREF = "clockwork_courier_save";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_MUTE = "mute";

    private final SharedPreferences sharedPreferences;

    public SaveRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public LevelProgress loadProgress() {
        LevelProgress p = new LevelProgress();
        p.unlockedLevel = sharedPreferences.getInt(KEY_UNLOCKED, 1);
        for (int i = 0; i < p.bestStars.length; i++) {
            p.bestStars[i] = sharedPreferences.getInt("stars_" + (i + 1), 0);
        }
        return p;
    }

    public void saveProgress(LevelProgress p) {
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putInt(KEY_UNLOCKED, p.unlockedLevel);
        for (int i = 0; i < p.bestStars.length; i++) {
            e.putInt("stars_" + (i + 1), p.bestStars[i]);
        }
        e.apply();
    }

    public boolean isMuted() {
        return sharedPreferences.getBoolean(KEY_MUTE, false);
    }

    public void saveMuted(boolean muted) {
        sharedPreferences.edit().putBoolean(KEY_MUTE, muted).apply();
    }
}
