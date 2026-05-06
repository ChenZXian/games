package com.android.boot.entity;

public class EnemyTemplate {
    public String id;
    public String name;
    public float maxHp;
    public float speed;
    public float armor;
    public float resist;
    public int reward;
    public boolean flying;
    public int threat;
    public boolean splitOnDeath;
    public String splitChildId;
    public int splitCount;
    public boolean boss;

    public EnemyTemplate(String id, String name, float maxHp, float speed, float armor, float resist, int reward, boolean flying, int threat, boolean splitOnDeath, String splitChildId, int splitCount, boolean boss) {
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.speed = speed;
        this.armor = armor;
        this.resist = resist;
        this.reward = reward;
        this.flying = flying;
        this.threat = threat;
        this.splitOnDeath = splitOnDeath;
        this.splitChildId = splitChildId;
        this.splitCount = splitCount;
        this.boss = boss;
    }
}
