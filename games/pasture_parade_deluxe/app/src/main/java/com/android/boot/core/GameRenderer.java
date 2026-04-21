package com.android.boot.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.android.boot.entity.Animal;
import com.android.boot.entity.AnimalSpecies;
import com.android.boot.entity.Pen;
import com.android.boot.fx.FloatText;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public void render(Canvas c, RanchWorld world, int w, int h) {
        c.drawColor(0xFFEAF4FF);
        for (int i = 0; i < world.pens.size(); i++) {
            drawPen(c, world.pens.get(i), world);
        }
        drawHud(c, world, w, h);
        drawTexts(c, world);
        drawStatePanels(c, world, w, h);
    }

    private void drawPen(Canvas c, Pen p, RanchWorld world) {
        float px = p.x - world.scrollX;
        rect.set(px, p.y, px + p.w, p.y + p.h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.unlocked ? 0xFFFFFFFF : 0xFFD0DEF2);
        c.drawRoundRect(rect, 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(p == world.selected ? 0xFF2ED6FF : 0xFF9EC8FF);
        c.drawRoundRect(rect, 20f, 20f, paint);
        if (p.unlocked) {
            drawAnimal(c, p.animal, px + p.w * 0.5f, p.y + p.h * 0.56f, p, world.scrollX);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF2B5388);
            paint.setTextSize(24f);
            c.drawText("L" + p.level + " G" + p.storedGoods + "/" + p.capacity, px + 14f, p.y + 34f, paint);
        } else {
            paint.setColor(0xFF5F7FAF);
            paint.setTextSize(24f);
            c.drawText("Locked", px + 50f, p.y + 72f, paint);
        }
    }

    private void drawAnimal(Canvas c, Animal a, float cx, float cy, Pen p, float scrollX) {
        float bob = (float) Math.sin(a.bobTime * 3f) * 3f;
        float s = 20f + a.species.ordinal() * 0.7f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(a.species.color);
        c.drawOval(cx - s, cy - s * 0.7f + bob, cx + s, cy + s * 0.7f + bob, paint);
        paint.setColor(0xFF222222);
        c.drawCircle(cx + s * 0.5f, cy - s * 0.2f + bob, s * 0.45f, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(cx + s * 0.65f, cy - s * 0.25f + bob, s * 0.11f, paint);
        if (a.mood == Animal.Mood.READY) {
            paint.setColor(0xFFFFB22B);
            c.drawCircle(cx, cy - s * 1.2f + bob, s * 0.25f, paint);
        }
        if (a.mood == Animal.Mood.HAPPY) {
            paint.setColor(0xFFFF6AA6);
            c.drawCircle(cx - 10f, cy - s * 1.2f + bob, 5f, paint);
            c.drawCircle(cx + 10f, cy - s * 1.2f + bob, 5f, paint);
        }
        if (a.species == AnimalSpecies.PEACOCK) {
            paint.setColor(0xFF2ED67A);
            c.drawCircle(cx - s * 0.8f, cy - s * 0.2f + bob, s * 0.35f, paint);
        }
        if (a.species == AnimalSpecies.MINI_DONKEY) {
            paint.setColor(0xFF735F4C);
            c.drawRect(cx - s * 0.1f, cy - s * 1.0f + bob, cx + s * 0.2f, cy - s * 0.4f + bob, paint);
        }
        if (p.isNeglected()) {
            paint.setColor(0x66FF5353);
            c.drawRect(p.x - scrollX + 2f, p.y + 2f, p.x - scrollX + p.w - 2f, p.y + p.h - 2f, paint);
        }
    }

    private void drawHud(Canvas c, RanchWorld world, int w, int h) {
        paint.setColor(0xFF163055);
        paint.setTextSize(34f);
        c.drawText("Coins " + world.economy.coins + " Lv " + world.economy.ranchLevel + " Combo x" + (int) world.economy.combo, 20f, h - 170f, paint);
        paint.setColor(0xFF9EC8FF);
        c.drawRect(20f, h - 145f, w - 20f, h - 118f, paint);
        paint.setColor(0xFF2ED67A);
        c.drawRect(20f, h - 145f, 20f + (w - 40f) * (1f - world.neglect), h - 118f, paint);
        paint.setColor(0xFF163055);
        c.drawText("Neglect", 22f, h - 150f, paint);
    }

    private void drawTexts(Canvas c, RanchWorld world) {
        for (FloatText ft : world.floatTexts) {
            if (ft.time > 0f) {
                paint.setColor(ft.color);
                paint.setTextSize(28f);
                c.drawText(ft.text, ft.x, ft.y, paint);
            }
        }
    }

    private void drawStatePanels(Canvas c, RanchWorld world, int w, int h) {
        if (world.state == RanchState.MENU) {
            panel(c, w, h, "Pasture Parade Deluxe", "Tap Start to open ranch");
        }
        if (world.state == RanchState.PAUSED) {
            panel(c, w, h, "Paused", "Resume to continue routes");
        }
        if (world.state == RanchState.GAME_OVER) {
            panel(c, w, h, "Game Over", "Coins " + world.sessionCoins + " Deliveries " + world.deliveriesDone + " Combo " + world.economy.highestCombo);
        }
    }

    private void panel(Canvas c, int w, int h, String title, String line) {
        rect.set(w * 0.12f, h * 0.24f, w * 0.88f, h * 0.58f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xEEFFFFFF);
        c.drawRoundRect(rect, 24f, 24f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(0xFF9EC8FF);
        c.drawRoundRect(rect, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF163055);
        paint.setTextSize(44f);
        c.drawText(title, rect.left + 32f, rect.top + 68f, paint);
        paint.setTextSize(30f);
        c.drawText(line, rect.left + 32f, rect.top + 120f, paint);
    }
}
