package com.android.boot.entity;

public class ObstacleSeed {
    private final int row;
    private final int col;
    private final ObstacleType obstacleType;
    private final int health;

    public ObstacleSeed(int row, int col, ObstacleType obstacleType, int health) {
        this.row = row;
        this.col = col;
        this.obstacleType = obstacleType;
        this.health = health;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public ObstacleType getObstacleType() {
        return obstacleType;
    }

    public int getHealth() {
        return health;
    }
}
