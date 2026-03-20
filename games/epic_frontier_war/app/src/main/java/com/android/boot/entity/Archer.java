package com.android.boot.entity;

import com.android.boot.core.RuneFavor;

public class Archer extends Unit {
    public Archer(Team team, RuneFavor favor) {
        super(team, favor, "Archer");
        maxHp = 58f;
        hp = maxHp;
        speed = 84f;
        range = 360f;
        preferredRange = 300f;
        damage = 18f;
        attackCooldown = 1.45f;
        attackDuration = 0.72f;
        impactMoment = 0.4f;
        size = 60f;
        bodyHeight = 82f;
        ranged = true;
    }
}
