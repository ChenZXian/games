package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class BaseCore {
    public final Team team;
    public final float x;
    public final float width;
    public final float height;
    public final float maxHp;
    public float hp;
    public float shake;

    public BaseCore(Team team, float x, float width, float height, float hp) {
        this.team = team;
        this.x = x;
        this.width = width;
        this.height = height;
        this.maxHp = hp;
        this.hp = hp;
    }

    public void damage(float amount) {
        hp -= amount;
        if (hp < 0f) {
            hp = 0f;
        }
        shake = 0.35f;
    }

    public void update(float dt) {
        if (shake > 0f) {
            shake -= dt;
            if (shake < 0f) {
                shake = 0f;
            }
        }
    }

    public void render(Canvas canvas, Paint fillPaint, Paint strokePaint, float groundY, int bodyColor, int trimColor) {
        float shakeOffset = shake > 0f ? (float) Math.sin(shake * 80f) * 8f : 0f;
        float left = team == Team.ALLY ? x - width * 0.35f : x - width * 0.65f;
        float right = left + width;
        float top = groundY - height;
        RectF body = new RectF(left, top, right, groundY + 12f);
        fillPaint.setColor(bodyColor);
        canvas.drawRoundRect(body, 22f, 22f, fillPaint);
        strokePaint.setColor(trimColor);
        canvas.drawRoundRect(body, 22f, 22f, strokePaint);
        Path roof = new Path();
        roof.moveTo(left - 10f + shakeOffset, top + 28f);
        roof.lineTo((left + right) * 0.5f + shakeOffset, top - 44f);
        roof.lineTo(right + 10f + shakeOffset, top + 28f);
        roof.close();
        fillPaint.setColor(trimColor);
        canvas.drawPath(roof, fillPaint);
        RectF gate = new RectF((left + right) * 0.5f - 34f + shakeOffset, groundY - 88f, (left + right) * 0.5f + 34f + shakeOffset, groundY + 12f);
        fillPaint.setColor(bodyColor == trimColor ? trimColor : bodyColor - 0x00111111);
        canvas.drawRoundRect(gate, 18f, 18f, fillPaint);
        for (int i = 0; i < 3; i++) {
            float towerX = left + 28f + i * (width - 56f) / 2f + shakeOffset;
            RectF tower = new RectF(towerX, top - 38f, towerX + 26f, top + 82f);
            fillPaint.setColor(trimColor);
            canvas.drawRoundRect(tower, 10f, 10f, fillPaint);
        }
    }
}
