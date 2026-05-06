package com.android.boot.entity;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClearSummary {
    private final Set<Position> cleared = new HashSet<>();
    private final Map<TileColor, Integer> colorCounts = new EnumMap<>(TileColor.class);
    private final Map<ObstacleType, Integer> obstacleCounts = new EnumMap<>(ObstacleType.class);
    private int totalTiles;
    private int droppedTargets;

    public void addTile(Position position, TileColor color) {
        if (cleared.add(position)) {
            totalTiles++;
            colorCounts.put(color, colorCounts.getOrDefault(color, 0) + 1);
        }
    }

    public void addObstacle(ObstacleType obstacleType) {
        obstacleCounts.put(obstacleType, obstacleCounts.getOrDefault(obstacleType, 0) + 1);
    }

    public int getTotalTiles() {
        return totalTiles;
    }

    public int getColorCount(TileColor color) {
        return colorCounts.getOrDefault(color, 0);
    }

    public int getObstacleCount(ObstacleType obstacleType) {
        return obstacleCounts.getOrDefault(obstacleType, 0);
    }

    public void addDroppedTargets(int delta) {
        droppedTargets += delta;
    }

    public int getDroppedTargets() {
        return droppedTargets;
    }
}
