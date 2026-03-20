package com.android.boot.entity;

import com.android.boot.core.RuneFavor;

public class Knight extends Unit {
    public Knight(Team team, RuneFavor favor) {
        super(team, favor, "Knight");
        maxHp = 148f;
        hp = maxHp;
        speed = 110f;
        range = 78f;
        preferredRange = 70f;
        damage = 30f;
        attackCooldown = 1.6f;
        attackDuration = 0.78f;
        impactMoment = 0.44f;
        size = 78f;
        bodyHeight = 98f;
    }
}
