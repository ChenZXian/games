package com.android.boot.entity;

public class Cell {
    private TileColor tileColor;
    private SpecialType specialType = SpecialType.NONE;
    private ObstacleType obstacleType = ObstacleType.NONE;
    private int obstacleHealth;
    private boolean spawnBlocked;
    private boolean moveBlocked;

    public Cell copy() {
        Cell cell = new Cell();
        cell.tileColor = tileColor;
        cell.specialType = specialType;
        cell.obstacleType = obstacleType;
        cell.obstacleHealth = obstacleHealth;
        cell.spawnBlocked = spawnBlocked;
        cell.moveBlocked = moveBlocked;
        return cell;
    }

    public boolean hasTile() {
        return tileColor != null;
    }

    public TileColor getTileColor() {
        return tileColor;
    }

    public void setTileColor(TileColor tileColor) {
        this.tileColor = tileColor;
    }

    public SpecialType getSpecialType() {
        return specialType;
    }

    public void setSpecialType(SpecialType specialType) {
        this.specialType = specialType;
    }

    public ObstacleType getObstacleType() {
        return obstacleType;
    }

    public void setObstacle(ObstacleType obstacleType, int health, boolean moveBlocked, boolean spawnBlocked) {
        this.obstacleType = obstacleType;
        this.obstacleHealth = health;
        this.moveBlocked = moveBlocked;
        this.spawnBlocked = spawnBlocked;
    }

    public int getObstacleHealth() {
        return obstacleHealth;
    }

    public void damageObstacle() {
        if (obstacleType == ObstacleType.NONE || obstacleType == ObstacleType.STONE) {
            return;
        }
        obstacleHealth--;
        if (obstacleHealth <= 0) {
            obstacleType = ObstacleType.NONE;
            obstacleHealth = 0;
            moveBlocked = false;
            spawnBlocked = false;
        }
    }

    public boolean isMoveBlocked() {
        return moveBlocked;
    }

    public boolean isSpawnBlocked() {
        return spawnBlocked;
    }

    public boolean canHostTile() {
        return obstacleType != ObstacleType.CRATE && obstacleType != ObstacleType.STONE;
    }

    public void clearTile() {
        tileColor = null;
        specialType = SpecialType.NONE;
    }

    public void setTile(TileColor color, SpecialType type) {
        this.tileColor = color;
        this.specialType = type;
    }
}
