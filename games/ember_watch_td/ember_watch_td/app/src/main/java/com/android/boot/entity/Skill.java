package com.android.boot.entity;

public class Skill {
    public String id;
    public String name;
    public float cooldown;
    public float timer;
    public boolean targetPoint;
    public int charges = 1;

    public Skill(String id, String name, float cooldown, boolean targetPoint) {
        this.id = id;
        this.name = name;
        this.cooldown = cooldown;
        this.targetPoint = targetPoint;
    }

    public boolean ready() {
        return timer <= 0f;
    }
}
