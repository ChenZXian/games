package com.android.boot.entity;

import java.util.EnumMap;
import java.util.Map;

public class SpawnProfile {
    private final Map<TileColor, Integer> weights = new EnumMap<>(TileColor.class);

    public SpawnProfile weight(TileColor color, int value) {
        weights.put(color, value);
        return this;
    }

    public Map<TileColor, Integer> getWeights() {
        return weights;
    }
}
