package com.android.boot.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameDefs;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void render(Canvas canvas, GameEngine engine) {
        canvas.drawColor(Color.rgb(220, 240, 255));
        paint.setColor(Color.rgb(180, 220, 255));
        canvas.drawRect(0f, 390f, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(Color.rgb(140, 100, 80));
        for (GameEngine.Platform p : engine.getPlatforms()) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, paint);
            paint.setColor(Color.rgb(160, 120, 100));
            canvas.drawRect(p.x + 2f, p.y + 2f, p.x + p.width - 2f, p.y + p.height - 2f, paint);
            paint.setColor(Color.rgb(140, 100, 80));
        }
        drawSnowman(canvas, engine.getPlayerX(), engine.getPlayerY());
        for (GameEngine.Enemy enemy : engine.getEnemies()) {
            if (!enemy.alive) {
                continue;
            }
            if (enemy.snowball) {
                paint.setColor(Color.rgb(244, 252, 255));
                canvas.drawCircle(enemy.x + 14f, enemy.y - 12f, 16f, paint);
                paint.setColor(Color.rgb(220, 240, 250));
                canvas.drawCircle(enemy.x + 14f, enemy.y - 12f, 14f, paint);
            } else {
                drawEnemy(canvas, enemy);
            }
        }
        paint.setColor(Color.rgb(245, 250, 255));
        for (GameEngine.Snowball snowball : engine.getSnowballs()) {
            if (snowball.active) {
                canvas.drawCircle(snowball.x, snowball.y - 12f, 14f, paint);
            }
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        for (GameEngine.AttackWave wave : engine.getAttackWaves()) {
            if (wave.active) {
                float alpha = 1f - (wave.radius / wave.maxRadius);
                paint.setColor(Color.argb((int) (alpha * 200), 255, 200, 100));
                canvas.drawCircle(wave.x, wave.y, wave.radius, paint);
                paint.setColor(Color.argb((int) (alpha * 150), 255, 255, 200));
                canvas.drawCircle(wave.x, wave.y, wave.radius * 0.7f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        for (GameEngine.SprayEffect effect : engine.getSprayEffects()) {
            if (effect.active) {
                float t = effect.progress / effect.lifetime;
                float alpha = 1f - t;
                float currentX = effect.startX + (effect.endX - effect.startX) * t;
                float currentY = effect.startY + (effect.endY - effect.startY) * t;
                paint.setColor(Color.argb((int) (alpha * 220), 255, 100, 100));
                canvas.drawCircle(currentX, currentY, 8f + t * 4f, paint);
                paint.setColor(Color.argb((int) (alpha * 180), 255, 150, 150));
                canvas.drawCircle(currentX, currentY, 6f + t * 3f, paint);
                paint.setColor(Color.argb((int) (alpha * 150), 255, 200, 200));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                canvas.drawLine(effect.startX, effect.startY, currentX, currentY, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        if (engine.getCombo() > 1) {
            paint.setColor(Color.rgb(70, 80, 180));
            paint.setTextSize(44f);
            canvas.drawText("Combo x" + engine.getCombo(), 380f, 80f, paint);
        }
    }

    private void drawSnowman(Canvas canvas, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 255, 255));
        float headY = y - 50f;
        float bodyY = y - 20f;
        canvas.drawCircle(x + 16f, headY, 14f, paint);
        canvas.drawCircle(x + 16f, bodyY, 18f, paint);
        paint.setColor(Color.rgb(0, 0, 0));
        canvas.drawCircle(x + 12f, headY - 2f, 2.5f, paint);
        canvas.drawCircle(x + 20f, headY - 2f, 2.5f, paint);
        paint.setColor(Color.rgb(255, 150, 150));
        Path nosePath = new Path();
        float noseY = headY + 2f;
        nosePath.moveTo(x + 16f, noseY);
        nosePath.lineTo(x + 20f, noseY + 4f);
        nosePath.lineTo(x + 16f, noseY + 4f);
        nosePath.close();
        canvas.drawPath(nosePath, paint);
        paint.setColor(Color.rgb(100, 150, 200));
        canvas.drawRect(x + 8f, bodyY - 8f, x + 24f, bodyY - 4f, paint);
        paint.setColor(Color.rgb(0, 0, 0));
        canvas.drawRect(x + 6f, bodyY + 8f, x + 10f, bodyY + 14f, paint);
        canvas.drawRect(x + 22f, bodyY + 8f, x + 26f, bodyY + 14f, paint);
    }

    private void drawEnemy(Canvas canvas, GameEngine.Enemy enemy) {
        int snow = Math.min(enemy.snowStage, 5);
        float bodyY = enemy.y - 17f;
        float headY = enemy.y - 40f;
        if (enemy.type == GameDefs.ENEMY_WALKER) {
            paint.setColor(Color.rgb(150, 100, 80));
            canvas.drawRect(enemy.x + 4f, bodyY, enemy.x + 24f, enemy.y, paint);
            paint.setColor(Color.rgb(200, 150, 120));
            canvas.drawCircle(enemy.x + 14f, headY, 8f, paint);
            paint.setColor(Color.rgb(0, 0, 0));
            canvas.drawCircle(enemy.x + 11f, headY - 1f, 1.5f, paint);
            canvas.drawCircle(enemy.x + 17f, headY - 1f, 1.5f, paint);
        } else if (enemy.type == GameDefs.ENEMY_HOPPER) {
            paint.setColor(Color.rgb(100, 150, 100));
            canvas.drawRect(enemy.x + 4f, bodyY, enemy.x + 24f, enemy.y, paint);
            paint.setColor(Color.rgb(150, 200, 150));
            canvas.drawCircle(enemy.x + 14f, headY, 8f, paint);
            paint.setColor(Color.rgb(0, 0, 0));
            canvas.drawCircle(enemy.x + 11f, headY - 1f, 1.5f, paint);
            canvas.drawCircle(enemy.x + 17f, headY - 1f, 1.5f, paint);
        } else if (enemy.type == GameDefs.ENEMY_FLYER) {
            paint.setColor(Color.rgb(200, 100, 150));
            canvas.drawRect(enemy.x + 4f, bodyY, enemy.x + 24f, enemy.y, paint);
            paint.setColor(Color.rgb(250, 150, 200));
            canvas.drawCircle(enemy.x + 14f, headY, 8f, paint);
            paint.setColor(Color.rgb(0, 0, 0));
            canvas.drawCircle(enemy.x + 11f, headY - 1f, 1.5f, paint);
            canvas.drawCircle(enemy.x + 17f, headY - 1f, 1.5f, paint);
        } else if (enemy.type == GameDefs.ENEMY_SHIELD) {
            paint.setColor(Color.rgb(100, 100, 200));
            canvas.drawRect(enemy.x + 4f, bodyY, enemy.x + 24f, enemy.y, paint);
            paint.setColor(Color.rgb(150, 150, 250));
            canvas.drawCircle(enemy.x + 14f, headY, 8f, paint);
            paint.setColor(Color.rgb(0, 0, 0));
            canvas.drawCircle(enemy.x + 11f, headY - 1f, 1.5f, paint);
            canvas.drawCircle(enemy.x + 17f, headY - 1f, 1.5f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.rgb(200, 200, 255));
            canvas.drawRect(enemy.x + 2f, bodyY - 4f, enemy.x + 26f, enemy.y + 2f, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            paint.setColor(Color.rgb(180, 80, 80));
            canvas.drawRect(enemy.x + 4f, bodyY, enemy.x + 24f, enemy.y, paint);
            paint.setColor(Color.rgb(230, 120, 120));
            canvas.drawCircle(enemy.x + 14f, headY, 10f, paint);
            paint.setColor(Color.rgb(0, 0, 0));
            canvas.drawCircle(enemy.x + 11f, headY - 1f, 2f, paint);
            canvas.drawCircle(enemy.x + 17f, headY - 1f, 2f, paint);
        }
        if (snow > 0) {
            int shade = 200 + snow * 10;
            paint.setColor(Color.rgb(shade, shade, shade + 20));
            float snowY = headY - 4f - snow * 2f;
            canvas.drawCircle(enemy.x + 14f, snowY, 4f + snow * 1.5f, paint);
        }
    }
}
