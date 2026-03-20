package com.android.boot.entity;

import com.android.boot.core.RuneFavor;

public class Titan extends Unit {
    public Titan(Team team, RuneFavor favor) {
        super(team, favor, "Titan");
        maxHp = 360f;
        hp = maxHp;
        speed = 52f;
        range = 96f;
        preferredRange = 90f;
        damage = 54f;
        attackCooldown = 2.25f;
        attackDuration = 1.1f;
        impactMoment = 0.4f;
        size = 108f;
        bodyHeight = 136f;
        titan = true;
    }
}
