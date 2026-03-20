package com.android.boot.entity;

import com.android.boot.core.RuneFavor;

public class Militia extends Unit {
    public Militia(Team team, RuneFavor favor) {
        super(team, favor, "Militia");
        maxHp = 72f;
        hp = maxHp;
        speed = 96f;
        range = 62f;
        preferredRange = 56f;
        damage = 16f;
        attackCooldown = 1.15f;
        attackDuration = 0.58f;
        impactMoment = 0.46f;
        size = 64f;
        bodyHeight = 84f;
    }
}
