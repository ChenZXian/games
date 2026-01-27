package com.android.boot.fx;

public class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float life;
    public float maxLife;
    public float size;
    public int color;
    public boolean active;

    public void spawn(float startX, float startY, float velX, float velY, float particleLife, float particleSize, int particleColor) {
        x = startX;
        y = startY;
        vx = velX;
        vy = velY;
        life = particleLife;
        maxLife = particleLife;
        size = particleSize;
        color = particleColor;
        active = true;
    }
}
