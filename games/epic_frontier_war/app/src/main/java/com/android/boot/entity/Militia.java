package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.RuneFavor;

public class Militia extends Unit {
    public Militia(Team team, RuneFavor favorAtSpawn) {
        super(team, "Militia", favorAtSpawn);
        width = 42f;
        height = 76f;
        maxHp = 74f;
        hp = maxHp;
        moveSpeed = 84f;
        attackRange = 42f;
        attackDamage = favorAtSpawn == RuneFavor.FLAME ? 14f : 11f;
        attackCooldown = 0.95f;
        attackWindup = 0.42f;
        impactWindow = 0.34f;
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        RectF blade = new RectF(width * 0.08f * dir, -height * 0.7f, (width * 0.08f + armReach * 0.95f) * dir, -height * 0.62f);
        if (dir < 0f) {
            blade = new RectF((width * 0.08f + armReach * 0.95f) * dir, -height * 0.7f, width * 0.08f * dir, -height * 0.62f);
        }
        canvas.drawRoundRect(blade, width * 0.03f, width * 0.03f, accentPaint);
        Path shield = new Path();
        shield.moveTo(-width * 0.32f * dir, -height * 0.72f);
        shield.lineTo(-width * 0.56f * dir, -height * 0.58f);
        shield.lineTo(-width * 0.48f * dir, -height * 0.2f);
        shield.lineTo(-width * 0.22f * dir, -height * 0.28f);
        shield.close();
        canvas.drawPath(shield, fillPaint);
    }
}
