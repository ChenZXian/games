package com.android.boot.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.android.boot.engine.GameEngine;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void render(Canvas canvas, GameEngine engine) {
        canvas.drawColor(Color.rgb(220, 240, 255));
        paint.setColor(Color.rgb(180, 220, 255));
        canvas.drawRect(0f, 390f, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(Color.rgb(20, 60, 110));
        canvas.drawRect(engine.getPlayerX(), engine.getPlayerY() - 44f, engine.getPlayerX() + 32f, engine.getPlayerY(), paint);
        paint.setColor(Color.rgb(240, 250, 255));
        canvas.drawCircle(engine.getPlayerX() + 16f, engine.getPlayerY() - 54f, 11f, paint);
        for (GameEngine.Enemy enemy : engine.getEnemies()) {
            if (!enemy.alive) {
                continue;
            }
            if (enemy.snowball) {
                paint.setColor(Color.rgb(244, 252, 255));
                canvas.drawCircle(enemy.x + 14f, enemy.y - 12f, 16f, paint);
            } else {
                int snow = Math.min(enemy.snowStage, 5);
                int shade = 180 + snow * 14;
                paint.setColor(Color.rgb(200 - snow * 10, 80 + snow * 18, 90 + snow * 24));
                canvas.drawRect(enemy.x, enemy.y - 34f, enemy.x + 28f, enemy.y, paint);
                paint.setColor(Color.rgb(shade, shade, shade));
                canvas.drawCircle(enemy.x + 14f, enemy.y - 40f, 6f + snow, paint);
            }
        }
        paint.setColor(Color.rgb(245, 250, 255));
        for (GameEngine.Snowball snowball : engine.getSnowballs()) {
            if (snowball.active) {
                canvas.drawCircle(snowball.x, snowball.y - 12f, 14f, paint);
            }
        }
        if (engine.getCombo() > 1) {
            paint.setColor(Color.rgb(70, 80, 180));
            paint.setTextSize(44f);
            canvas.drawText("Combo x" + engine.getCombo(), 380f, 80f, paint);
        }
    }
}
