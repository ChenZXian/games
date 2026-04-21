package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

public class CodexManager {
    private final SharedPreferences pref;

    public CodexManager(Context context) {
        pref = context.getSharedPreferences("codex", Context.MODE_PRIVATE);
    }

    public void onHarvest(String cropId, int beauty, String weather) {
        int total = pref.getInt(cropId + "_count", 0) + 1;
        int best = Math.max(pref.getInt(cropId + "_best", 0), beauty);
        pref.edit().putInt(cropId + "_count", total).putInt(cropId + "_best", best).putString(cropId + "_weather", weather).apply();
    }
}
