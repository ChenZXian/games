package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.RuneFavor;

public class Knight extends Unit {
    public Knight(Team team, RuneFavor favorAtSpawn) {
        super(team, "Knight", favorAtSpawn);
        width = 52f;
        height = 90f;
        maxHp = 145f;
        hp = maxHp;
        moveSpeed = 88f;
        attackRange = 56f;
        attackDamage = favorAtSpawn == RuneFavor.FLAME ? 27f : 21f;
        attackCooldown = 1.12f;
        attackWindup = 0.48f;
        impactWindow = 0.38f;
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        Path spear = new Path();
        spear.moveTo(width * 0.08f * dir, -height * 0.68f);
        spear.lineTo((width * 0.1f + armReach * 1.2f) * dir, -height * 0.72f);
        spear.lineTo((width * 0.26f + armReach * 1.2f) * dir, -height * 0.66f);
        spear.lineTo((width * 0.1f + armReach * 1.1f) * dir, -height * 0.6f);
        spear.close();
        canvas.drawPath(spear, accentPaint);
        RectF crest = new RectF(-width * 0.12f, -height * 1.02f, width * 0.12f, -height * 0.9f);
        canvas.drawRoundRect(crest, width * 0.04f, width * 0.04f, accentPaint);
    }
}
