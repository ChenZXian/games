package com.android.boot.entity;

import java.util.ArrayList;
import java.util.List;

public class LevelConfig {
    private final int rows;
    private final int cols;
    private final int moveLimit;
    private final int timeLimitSeconds;
    private final SpawnProfile spawnProfile;
    private final List<GoalDefinition> goals = new ArrayList<>();
    private final List<ObstacleSeed> obstacleSeeds = new ArrayList<>();
    private final String title;

    public LevelConfig(String title, int rows, int cols, int moveLimit, int timeLimitSeconds, SpawnProfile spawnProfile) {
        this.title = title;
        this.rows = rows;
        this.cols = cols;
        this.moveLimit = moveLimit;
        this.timeLimitSeconds = timeLimitSeconds;
        this.spawnProfile = spawnProfile;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMoveLimit() {
        return moveLimit;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public SpawnProfile getSpawnProfile() {
        return spawnProfile;
    }

    public List<GoalDefinition> getGoals() {
        return goals;
    }

    public List<ObstacleSeed> getObstacleSeeds() {
        return obstacleSeeds;
    }

    public LevelConfig addGoal(GoalDefinition goalDefinition) {
        goals.add(goalDefinition);
        return this;
    }

    public LevelConfig addObstacle(ObstacleSeed seed) {
        obstacleSeeds.add(seed);
        return this;
    }
}
