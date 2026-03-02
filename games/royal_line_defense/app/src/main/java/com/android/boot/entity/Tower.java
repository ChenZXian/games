package com.android.boot.entity;

public class Tower {
    public enum Type { ARROW, CANNON, MAGE, BARRACKS, SUPPORT_TOTEM }
    public Type type;
    public float x;
    public float y;
    public int level;
    public int branch;
    public float cooldown;

    public Tower(Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.level = 1;
    }

    public void upgradeBase() {
        if (level < 3) {
            level++;
        }
    }

    public void chooseBranch(int value) {
        branch = value;
        if (level < 4) {
            level = 4;
        } else if (level == 4) {
            level = 5;
        }
    }

    public float range() {
        float r = 120f + level * 20f;
        if (type == Type.SUPPORT_TOTEM) {
            r += 30f;
        }
        return r;
    }

    public float damage() {
        float d = 10f + level * 7f;
        if (type == Type.CANNON) {
            d += 10f;
        }
        if (type == Type.MAGE) {
            d += 6f;
        }
        if (branch == 1) {
            d *= 1.35f;
        }
        return d;
    }

    public float fireRate() {
        float base = 1f;
        if (type == Type.ARROW) {
            base = 0.45f;
        } else if (type == Type.CANNON) {
            base = 1.1f;
        } else if (type == Type.MAGE) {
            base = 0.75f;
        } else if (type == Type.BARRACKS) {
            base = 1.4f;
        } else if (type == Type.SUPPORT_TOTEM) {
            base = 1.3f;
        }
        if (branch == 2) {
            base *= 0.75f;
        }
        return base;
    }
}
