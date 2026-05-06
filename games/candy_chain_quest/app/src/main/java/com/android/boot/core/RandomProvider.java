package com.android.boot.core;

import com.android.boot.entity.SpawnProfile;
import com.android.boot.entity.TileColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomProvider {
    private final Random random = new Random();

    public TileColor nextColor(SpawnProfile profile) {
        List<Map.Entry<TileColor, Integer>> entries = new ArrayList<>(profile.getWeights().entrySet());
        int total = 0;
        for (Map.Entry<TileColor, Integer> entry : entries) {
            total += Math.max(0, entry.getValue());
        }
        int roll = random.nextInt(Math.max(total, 1));
        int cursor = 0;
        for (Map.Entry<TileColor, Integer> entry : entries) {
            cursor += Math.max(0, entry.getValue());
            if (roll < cursor) {
                return entry.getKey();
            }
        }
        return TileColor.RED;
    }

    public Random getRandom() {
        return random;
    }
}
