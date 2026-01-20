package com.android.boot.entity;

public class Fence {
    public WireNode a;
    public WireNode b;
    public float damagePerSecond;
    public float slowFactor;

    public Fence(WireNode a, WireNode b, float damagePerSecond, float slowFactor) {
        this.a = a;
        this.b = b;
        this.damagePerSecond = damagePerSecond;
        this.slowFactor = slowFactor;
    }
}
