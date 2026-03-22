package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.RuneFavor;

public class Archer extends Unit {
    public Archer(Team team, RuneFavor favorAtSpawn) {
        super(team, "Archer", favorAtSpawn);
        width = 40f;
        height = 74f;
        maxHp = 58f;
        hp = maxHp;
        moveSpeed = 74f;
        attackRange = 190f;
        preferredRange = 160f;
        attackDamage = 13f;
        attackCooldown = 1.35f;
        attackWindup = 0.58f;
        impactWindow = 0.46f;
        projectileSpeed = favorAtSpawn == RuneFavor.STORM ? 370f : 300f;
        ranged = true;
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        RectF bow = new RectF(width * 0.12f * dir, -height * 0.86f, (width * 0.18f + armReach * 0.8f) * dir, -height * 0.28f);
        if (dir < 0f) {
            bow = new RectF((width * 0.18f + armReach * 0.8f) * dir, -height * 0.86f, width * 0.12f * dir, -height * 0.28f);
        }
        canvas.drawRoundRect(bow, width * 0.08f, width * 0.08f, accentPaint);
        Path arrow = new Path();
        arrow.moveTo(width * 0.15f * dir, -height * 0.54f);
        arrow.lineTo((width * 0.2f + armReach) * dir, -height * 0.54f);
        arrow.lineTo((width * 0.16f + armReach) * dir, -height * 0.6f);
        arrow.lineTo((width * 0.16f + armReach) * dir, -height * 0.48f);
        arrow.close();
        canvas.drawPath(arrow, fillPaint);
    }
}
