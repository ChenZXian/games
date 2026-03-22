package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.RuneFavor;

public class Titan extends Unit {
    public Titan(Team team, RuneFavor favorAtSpawn) {
        super(team, "Titan", favorAtSpawn);
        width = 76f;
        height = 132f;
        maxHp = 430f;
        hp = maxHp;
        moveSpeed = 42f;
        attackRange = 70f;
        attackDamage = favorAtSpawn == RuneFavor.FLAME ? 54f : 46f;
        attackCooldown = 1.7f;
        attackWindup = 0.85f;
        impactWindow = 0.58f;
        titan = true;
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        RectF hammer = new RectF(width * 0.08f * dir, -height * 0.86f, (width * 0.08f + armReach * 1.2f) * dir, -height * 0.74f);
        if (dir < 0f) {
            hammer = new RectF((width * 0.08f + armReach * 1.2f) * dir, -height * 0.86f, width * 0.08f * dir, -height * 0.74f);
        }
        canvas.drawRoundRect(hammer, width * 0.06f, width * 0.06f, fillPaint);
        Path head = new Path();
        head.moveTo((width * 0.12f + armReach * 1.1f) * dir, -height * 0.95f);
        head.lineTo((width * 0.38f + armReach * 1.1f) * dir, -height * 0.95f);
        head.lineTo((width * 0.42f + armReach * 1.1f) * dir, -height * 0.67f);
        head.lineTo((width * 0.08f + armReach * 1.1f) * dir, -height * 0.67f);
        head.close();
        canvas.drawPath(head, accentPaint);
    }
}
