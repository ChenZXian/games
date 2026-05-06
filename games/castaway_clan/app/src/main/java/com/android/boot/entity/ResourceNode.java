package com.android.boot.entity;

public class ResourceNode {
    public enum Type {
        WOOD,
        FOOD,
        WATER,
        HERB,
        SCRAP,
        SURVIVOR
    }

    public final Type type;
    public final float x;
    public final float y;
    public final float radius;
    public boolean active;
    public int tier;

    public ResourceNode(Type type, float x, float y, float radius, int tier) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.active = true;
        this.tier = tier;
    }
}
