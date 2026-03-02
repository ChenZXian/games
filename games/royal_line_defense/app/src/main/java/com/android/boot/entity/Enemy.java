package com.android.boot.entity;

import java.util.List;

public class Enemy {
    public enum Type { GRUNT, RUNNER, ARMOR, FLYER, HEALER, BOSS }
    public boolean active;
    public float x;
    public float y;
    public float prevX;
    public float prevY;
    public float hp;
    public float maxHp;
    public float speed;
    public int pathIndex;
    public Type type;
    public float healTimer;
    public int phase;

    public void spawn(Type t, List<float[]> path, float bonus) {
        type = t;
        active = true;
        pathIndex = 0;
        x = path.get(0)[0];
        y = path.get(0)[1];
        prevX = x;
        prevY = y;
        healTimer = 0f;
        phase = 0;
        if (t == Type.GRUNT) {
            maxHp = 60f + bonus;
            speed = 60f;
        } else if (t == Type.RUNNER) {
            maxHp = 45f + bonus;
            speed = 95f;
        } else if (t == Type.ARMOR) {
            maxHp = 140f + bonus * 2f;
            speed = 46f;
        } else if (t == Type.FLYER) {
            maxHp = 55f + bonus;
            speed = 110f;
        } else if (t == Type.HEALER) {
            maxHp = 90f + bonus;
            speed = 58f;
        } else {
            maxHp = 540f + bonus * 5f;
            speed = 48f;
        }
        hp = maxHp;
    }
}
