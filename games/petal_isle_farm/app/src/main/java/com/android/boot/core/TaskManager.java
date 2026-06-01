package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.boot.entity.DailyTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskManager {
    private final SharedPreferences pref;
    public final List<DailyTask> tasks = new ArrayList<>();

    public TaskManager(Context context) {
        pref = context.getSharedPreferences("tasks", Context.MODE_PRIVATE);
        ensureDaily();
    }

    private void ensureDaily() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String last = pref.getString("date", "");
        if (!today.equals(last)) {
            tasks.clear();
            add("plant", "Plant 8 crops", 8, 100, 40);
            add("harvest", "Harvest 5 flowers", 5, 120, 45);
            add("orders", "Complete 2 orders", 2, 140, 60);
            add("combo", "Reach combo 6", 6, 90, 35);
            pref.edit().putString("date", today).apply();
        }
    }

    private void add(String id, String title, int target, int coins, int xp) {
        DailyTask t = new DailyTask();
        t.id = id;
        t.title = title;
        t.target = target;
        t.rewardCoins = coins;
        t.rewardXp = xp;
        tasks.add(t);
    }
}
