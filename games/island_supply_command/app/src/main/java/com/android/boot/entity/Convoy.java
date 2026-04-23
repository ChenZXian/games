package com.android.boot.entity;

public class Convoy {
    public final int sourceId;
    public final int targetId;
    public final int owner;
    public float progress;
    public final float cargo;
    public float hp;
    public final float speed;

    public Convoy(int sourceId, int targetId, int owner, float cargo, float hp, float speed) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.owner = owner;
        this.cargo = cargo;
        this.hp = hp;
        this.speed = speed;
    }
}
