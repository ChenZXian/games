package com.android.boot.entity;

public class Bullet {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float speed;
    public float damage;
    public float radius;
    public int pierce;
    public boolean active;

    public void fire(float startX, float startY, float dirX, float dirY, float bulletSpeed, float bulletDamage, int bulletPierce) {
        x = startX;
        y = startY;
        vx = dirX;
        vy = dirY;
        speed = bulletSpeed;
        damage = bulletDamage;
        radius = 6f;
        pierce = bulletPierce;
        active = true;
    }
}
