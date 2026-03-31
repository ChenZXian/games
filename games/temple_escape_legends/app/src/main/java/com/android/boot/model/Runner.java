package com.android.boot.model;

public class Runner {
    public int currentLane = 1;
    public int targetLane = 1;
    public float laneBlend = 1f;
    public float y;
    public float vy;
    public boolean airborne;
    public boolean sliding;
    public float slideTimer;
    public int life = 3;
    public float invulnTimer;
}
