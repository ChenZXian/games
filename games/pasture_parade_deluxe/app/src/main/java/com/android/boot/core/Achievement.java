package com.android.boot.core;

public class Achievement {
    public final String id;
    public final String title;
    public final String category;
    public final int target;
    public final int rewardCoins;
    public int progress;
    public boolean claimed;

    public Achievement(String id, String title, String category, int target, int rewardCoins) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.target = target;
        this.rewardCoins = rewardCoins;
    }

    public boolean complete() {
        return progress >= target;
    }
}
