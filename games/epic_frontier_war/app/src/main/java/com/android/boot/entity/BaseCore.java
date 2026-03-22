package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class BaseCore {
    private final Team team;
    private final float x;
    private final float width;
    private final float height;
    private final int maxHp;
    private int hp;
    private float hitShake;

    public BaseCore(Team team, float x, float width, float height, int maxHp) {
        this.team = team;
        this.x = x;
        this.width = width;
        this.height = height;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public void reset() {
        hp = maxHp;
        hitShake = 0f;
    }

    public void update(float dt) {
        hitShake = Math.max(0f, hitShake - dt * 4f);
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
        hitShake = Math.min(1f, hitShake + 0.55f);
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public float getCenterX() {
        return x + width * 0.5f;
    }

    public float getBodyX(Team viewer) {
        return viewer == Team.ALLY ? x + width : x;
    }

    public void draw(Canvas canvas, float groundY, Paint bodyPaint, Paint accentPaint, Paint trimPaint) {
        canvas.save();
        float offset = team == Team.ALLY ? hitShake * -9f : hitShake * 9f;
        canvas.translate(offset, 0f);
        float top = groundY - height;
        RectF keep = new RectF(x, top + height * 0.28f, x + width, groundY);
        canvas.drawRoundRect(keep, width * 0.08f, width * 0.08f, bodyPaint);
        Path roof = new Path();
        roof.moveTo(x + width * 0.08f, top + height * 0.28f);
        roof.lineTo(x + width * 0.5f, top);
        roof.lineTo(x + width * 0.92f, top + height * 0.28f);
        roof.close();
        canvas.drawPath(roof, accentPaint);
        float towerW = width * 0.18f;
        float leftTowerX = team == Team.ALLY ? x + width * 0.08f : x + width * 0.74f;
        RectF tower = new RectF(leftTowerX, top + height * 0.08f, leftTowerX + towerW, groundY);
        canvas.drawRoundRect(tower, towerW * 0.2f, towerW * 0.2f, trimPaint);
        RectF gate = new RectF(x + width * 0.34f, groundY - height * 0.32f, x + width * 0.66f, groundY);
        canvas.drawRoundRect(gate, width * 0.05f, width * 0.05f, accentPaint);
        Path banner = new Path();
        float poleX = team == Team.ALLY ? x + width * 0.76f : x + width * 0.22f;
        banner.moveTo(poleX, top + height * 0.1f);
        banner.lineTo(poleX, top + height * 0.38f);
        banner.lineTo(poleX + (team == Team.ALLY ? width * 0.16f : -width * 0.16f), top + height * 0.26f + hitShake * 10f);
        banner.close();
        canvas.drawPath(banner, accentPaint);
        canvas.restore();
    }
}
