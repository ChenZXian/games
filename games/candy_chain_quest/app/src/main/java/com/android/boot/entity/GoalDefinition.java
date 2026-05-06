package com.android.boot.entity;

public class GoalDefinition {
    private final GoalType goalType;
    private final TileColor tileColor;
    private final ObstacleType obstacleType;
    private final int target;
    private int progress;

    public GoalDefinition(GoalType goalType, TileColor tileColor, ObstacleType obstacleType, int target) {
        this.goalType = goalType;
        this.tileColor = tileColor;
        this.obstacleType = obstacleType;
        this.target = target;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public TileColor getTileColor() {
        return tileColor;
    }

    public ObstacleType getObstacleType() {
        return obstacleType;
    }

    public int getTarget() {
        return target;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(target, value));
    }

    public void addProgress(int delta) {
        progress = Math.min(target, progress + delta);
    }

    public boolean isComplete() {
        return progress >= target;
    }
}
