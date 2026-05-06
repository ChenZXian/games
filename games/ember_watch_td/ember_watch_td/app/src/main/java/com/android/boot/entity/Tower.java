package com.android.boot.entity;

public class Tower {
    public TowerType type;
    public BranchType branch = BranchType.NONE;
    public int level = 1;
    public int branchLevel;
    public float x;
    public float y;
    public float range;
    public float damage;
    public float attackInterval;
    public float attackTimer;
    public float splashRadius;
    public int buildCost;
    public int totalSpent;
    public int sellValue;
    public int unitCount = 1;
    public float soldierHp;
    public float soldierDamage;
    public float soldierRespawn;
    public float soldierMoveSpeed;
    public float soldierArmor;
    public float specialValue;
    public TargetPriority priority = TargetPriority.FIRST;

    public Tower(TowerType type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public boolean isBarracks() {
        return type == TowerType.BARRACKS;
    }
}
