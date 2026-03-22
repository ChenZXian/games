package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class CampaignProgress {
    private static final String PREFS = "epic_campaign_war_progress";
    private static final String KEY_UNLOCKED = "highest_unlocked_chapter";
    private static final String KEY_SELECTED = "selected_chapter";
    private static final String KEY_MUTED = "muted";
    private final SharedPreferences preferences;

    public CampaignProgress(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getHighestUnlockedChapter() {
        return Math.max(1, preferences.getInt(KEY_UNLOCKED, 1));
    }

    public void unlockNextChapter(int chapterIndex, int chapterCount) {
        int next = Math.min(chapterCount, chapterIndex + 1);
        int current = getHighestUnlockedChapter();
        if (next > current) {
            preferences.edit().putInt(KEY_UNLOCKED, next).apply();
        }
        setCleared(chapterIndex, true);
    }

    public boolean isCleared(int chapterIndex) {
        return preferences.getBoolean("chapter_cleared_" + chapterIndex, false);
    }

    public void setCleared(int chapterIndex, boolean cleared) {
        preferences.edit().putBoolean("chapter_cleared_" + chapterIndex, cleared).apply();
    }

    public int getSelectedChapter() {
        int value = preferences.getInt(KEY_SELECTED, 1);
        return Math.max(1, Math.min(value, getHighestUnlockedChapter()));
    }

    public void setSelectedChapter(int chapterIndex) {
        preferences.edit().putInt(KEY_SELECTED, chapterIndex).apply();
    }

    public boolean isMuted() {
        return preferences.getBoolean(KEY_MUTED, false);
    }

    public void setMuted(boolean muted) {
        preferences.edit().putBoolean(KEY_MUTED, muted).apply();
    }
}
