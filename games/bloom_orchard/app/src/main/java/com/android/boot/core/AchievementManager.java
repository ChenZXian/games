package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {
    private final SharedPreferences pref;
    public final List<String> names = new ArrayList<>();

    public AchievementManager(Context context) {
        pref = context.getSharedPreferences("ach", Context.MODE_PRIVATE);
        String[] all = {"First Seed","Ten Green Plots","Hundred Harvests","Combo Apprentice","Combo Master","Bloom Collector","Rare Blossom","Golden Farmer","Beauty Specialist","Orchard Tycoon","Perfect Watering I","Perfect Watering II","Fertilizer Expert","Rain Chaser","Sun Blessing","Full Catalog Bronze","Full Catalog Silver","Full Catalog Gold","First Order","Premium Supplier","Festival Merchant","Storage Keeper","Big Barn","Open Horizon","Land Baron","Seven Days Warmup","Week Crown","Rainbow Witness","Golden Harvest Day","Petal Dancer","Perfect Orchard","Master Curator"};
        for (String it : all) {
            names.add(it);
        }
    }

    public boolean unlock(String key) {
        if (pref.getBoolean(key, false)) return false;
        pref.edit().putBoolean(key, true).apply();
        return true;
    }
}
