package com.android.boot.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.android.boot.engine.GameSession;
import com.android.boot.engine.GameState;
import com.android.boot.model.Enemy;

public class GameRenderer {
    private final Paint paint = new Paint();

    public void draw(Canvas canvas, GameSession s) {
        canvas.drawColor(Color.rgb(8, 16, 34));
        paint.setColor(Color.rgb(20, 40, 70));
        canvas.drawRect(0, 460, canvas.getWidth(), canvas.getHeight(), paint);
        if (s.state == GameState.MENU) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(56f);
            canvas.drawText("Relic Blade Trials", 80, 130, paint);
            paint.setTextSize(30f);
            canvas.drawText("Tap center to start", 80, 190, paint);
            canvas.drawText("Inventory and equipment in menu and result", 80, 235, paint);
            return;
        }
        paint.setColor(Color.CYAN);
        canvas.drawRect(s.player.x - 24, s.player.y - 70, s.player.x + 24, s.player.y, paint);
        paint.setColor(Color.RED);
        for (Enemy enemy : s.enemies) {
            canvas.drawRect(enemy.x - 22, enemy.y - 56, enemy.x + 22, enemy.y, paint);
        }
        if (s.boss != null) {
            paint.setColor(Color.MAGENTA);
            canvas.drawRect(s.boss.x - 44, s.boss.y - 88, s.boss.x + 44, s.boss.y, paint);
        }
        drawHud(canvas, s);
        if (s.state == GameState.PAUSED) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(54f);
            canvas.drawText("Paused", canvas.getWidth() * 0.45f, canvas.getHeight() * 0.45f, paint);
        }
        if (s.state == GameState.GAME_OVER) {
            paint.setColor(Color.RED);
            paint.setTextSize(52f);
            canvas.drawText("Game Over", canvas.getWidth() * 0.41f, canvas.getHeight() * 0.45f, paint);
        }
        if (s.state == GameState.STAGE_RESULT) {
            paint.setColor(Color.GREEN);
            paint.setTextSize(44f);
            canvas.drawText("Stage Clear", 80, 120, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30f);
            canvas.drawText("Boss Defeated: " + s.resultBoss, 80, 170, paint);
            if (s.resultLoot != null) {
                canvas.drawText("Loot: " + s.resultLoot.name + " " + s.resultLoot.rarity.name(), 80, 210, paint);
            }
            canvas.drawText("Tap left for replay, right for stage select", 80, 250, paint);
        }
    }

    private void drawHud(Canvas canvas, GameSession s) {
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(20, 20, 360, 44, paint);
        paint.setColor(Color.GREEN);
        canvas.drawRect(20, 20, 20 + (s.player.hp / s.player.maxHp) * 340f, 44, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24f);
        canvas.drawText("Stage " + s.stageIndex, 380, 40, paint);
        canvas.drawText("Coins " + s.coins, 520, 40, paint);
        if (s.boss != null) {
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(760, 20, 1180, 44, paint);
            paint.setColor(Color.MAGENTA);
            canvas.drawRect(760, 20, 760 + (s.boss.hp / (140f + s.stageIndex * 55f)) * 420f, 44, paint);
        }
    }
}
