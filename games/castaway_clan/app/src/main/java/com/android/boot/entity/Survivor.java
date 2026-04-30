package com.android.boot.entity;

import com.android.boot.core.SurvivorRole;

public class Survivor {
    public final String name;
    public SurvivorRole role;
    public float x;
    public float y;
    public float targetX;
    public float targetY;
    public float animTime;
    public boolean selected;

    public Survivor(String name, SurvivorRole role, float x, float y) {
        this.name = name;
        this.role = role;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
    }
}
