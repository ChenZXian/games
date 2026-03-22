package com.android.boot.entity;

public class BaseCore {
    private final Team team;
    private final float x;
    private final float width;
    private int maxHp;
    private int hp;
    private float shake;

    public BaseCore(Team team, float x, float width, int hp) {
        this.team = team;
        this.x = x;
        this.width = width;
        this.maxHp = hp;
        this.hp = hp;
    }

    public void reset(int value) {
        maxHp = value;
        hp = value;
        shake = 0f;
    }

    public void damage(int value) {
        hp = Math.max(0, hp - value);
        shake = 0.2f;
    }

    public void update(float dt) {
        shake = Math.max(0f, shake - dt);
    }

    public Team getTeam() {
        return team;
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public float getShake() {
        return shake;
    }
}
