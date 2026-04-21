package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class StorageManager {
    private final SharedPreferences pref;
    public final Map<String, Integer> items = new HashMap<>();
    public int used;

    public StorageManager(Context context) {
        pref = context.getSharedPreferences("storage", Context.MODE_PRIVATE);
        used = pref.getInt("used", 0);
    }

    public boolean add(String cropId, int amount, int cap) {
        if (used + amount > cap) return false;
        items.put(cropId, items.containsKey(cropId) ? items.get(cropId) + amount : amount);
        used += amount;
        return true;
    }

    public boolean consume(String cropId, int amount) {
        int have = items.containsKey(cropId) ? items.get(cropId) : 0;
        if (have < amount) return false;
        items.put(cropId, have - amount);
        used -= amount;
        return true;
    }

    public void save() {
        SharedPreferences.Editor e = pref.edit();
        e.putInt("used", used);
        for (Map.Entry<String, Integer> it : items.entrySet()) {
            e.putInt("i_" + it.getKey(), it.getValue());
        }
        e.apply();
    }
}
