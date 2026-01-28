package com.android.boot.core;

public class Weapon {
  public static final int TYPE_PISTOL = 0;
  public static final int TYPE_SHOTGUN = 1;
  public static final int TYPE_SMG = 2;
  public static final int TYPE_RIFLE = 3;
  public static final int TYPE_LAUNCHER = 4;

  public int type;
  public String name;
  public float fireRate;
  public float damage;
  public float bulletSpeed;
  public int bulletCount;
  public float spread;
  public int pierce;
  public float knockback;
  public int color;

  public Weapon(int type, String name, float fireRate, float damage, float bulletSpeed, int bulletCount, float spread, int pierce, float knockback, int color) {
    this.type = type;
    this.name = name;
    this.fireRate = fireRate;
    this.damage = damage;
    this.bulletSpeed = bulletSpeed;
    this.bulletCount = bulletCount;
    this.spread = spread;
    this.pierce = pierce;
    this.knockback = knockback;
    this.color = color;
  }

  public static Weapon createPistol() {
    return new Weapon(TYPE_PISTOL, "Pistol", 3f, 15f, 720f, 1, 0.05f, 0, 60f, 0xFFFFD700);
  }

  public static Weapon createShotgun() {
    return new Weapon(TYPE_SHOTGUN, "Shotgun", 1.2f, 8f, 680f, 5, 0.25f, 0, 80f, 0xFFFF6B35);
  }

  public static Weapon createSMG() {
    return new Weapon(TYPE_SMG, "SMG", 8f, 6f, 760f, 1, 0.08f, 0, 40f, 0xFF00F5FF);
  }

  public static Weapon createRifle() {
    return new Weapon(TYPE_RIFLE, "Rifle", 5f, 22f, 800f, 1, 0.02f, 1, 70f, 0xFF20FFB2);
  }

  public static Weapon createLauncher() {
    return new Weapon(TYPE_LAUNCHER, "Launcher", 0.8f, 45f, 520f, 1, 0f, 0, 120f, 0xFFFF3DFF);
  }
}

