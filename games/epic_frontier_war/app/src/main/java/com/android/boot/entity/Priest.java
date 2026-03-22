package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.android.boot.core.RuneFavor;

public class Priest extends Unit {
    public Priest(Team team, RuneFavor favorAtSpawn) {
        super(team, "Priest", favorAtSpawn);
        width = 44f;
        height = 82f;
        maxHp = 68f;
        hp = maxHp;
        moveSpeed = 64f;
        attackRange = 122f;
        preferredRange = 128f;
        attackDamage = 7f;
        attackCooldown = 1.8f;
        attackWindup = 0.74f;
        impactWindow = 0.52f;
        projectileSpeed = 250f;
        sustainPulse = favorAtSpawn == RuneFavor.VITAL ? 10f : 6f;
        sustainRate = favorAtSpawn == RuneFavor.VITAL ? 2.2f : 2.8f;
        ranged = true;
        support = true;
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        Path staff = new Path();
        staff.moveTo(width * 0.08f * dir, -height * 0.84f);
        staff.lineTo((width * 0.08f + armReach * 0.2f) * dir, -height * 0.18f);
        staff.lineTo((width * 0.15f + armReach * 0.26f) * dir, -height * 0.18f);
        staff.lineTo((width * 0.15f + armReach * 0.06f) * dir, -height * 0.84f);
        staff.close();
        canvas.drawPath(staff, fillPaint);
        canvas.drawOval((width * 0.04f + armReach * 0.02f) * dir - width * 0.1f, -height * 0.96f, (width * 0.04f + armReach * 0.02f) * dir + width * 0.1f, -height * 0.76f, accentPaint);
    }
}
