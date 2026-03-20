package com.android.boot.entity;

import com.android.boot.core.RuneFavor;

public class Priest extends Unit {
    public Priest(Team team, RuneFavor favor) {
        super(team, favor, "Priest");
        maxHp = 76f;
        hp = maxHp;
        speed = 74f;
        range = 220f;
        preferredRange = 180f;
        damage = 8f;
        attackCooldown = 2.2f;
        attackDuration = 0.95f;
        impactMoment = 0.38f;
        size = 62f;
        bodyHeight = 88f;
        support = true;
        healing = true;
    }
}
