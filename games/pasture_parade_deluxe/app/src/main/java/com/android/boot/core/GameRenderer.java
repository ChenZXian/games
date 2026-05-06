package com.android.boot.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.android.boot.entity.Animal;
import com.android.boot.entity.AnimalSpecies;
import com.android.boot.entity.Pen;
import com.android.boot.fx.CoinPopup;
import com.android.boot.fx.FloatText;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();

    public void render(Canvas c, RanchWorld world, int w, int h) {
        c.drawColor(0xFFEFF6FF);
        for (int i = 0; i < world.pens.size(); i++) {
            drawPen(c, world.pens.get(i), world);
        }
        drawCoinPopups(c, world);
        drawHud(c, world, w, h);
        drawTexts(c, world);
        drawStatePanels(c, world, w, h);
    }

    private void drawPen(Canvas c, Pen p, RanchWorld world) {
        float px = p.x - world.scrollX;
        rect.set(px, p.y, px + p.w, p.y + p.h);

        // shadow card
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x14000000);
        c.drawRoundRect(px + 3f, p.y + 5f, px + p.w + 3f, p.y + p.h + 5f, 26f, 26f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.unlocked ? 0xFFFFFFFF : 0xFFE3ECFA);
        c.drawRoundRect(rect, 26f, 26f, paint);

        // header tint
        inner.set(rect.left + 6f, rect.top + 6f, rect.right - 6f, rect.top + 54f);
        paint.setColor(p.unlocked ? (0xFF000000 | (p.animal.species.color & 0x00FFFFFF)) : 0xFFB9CBE6);
        paint.setAlpha(p.unlocked ? 28 : 22);
        c.drawRoundRect(inner, 18f, 18f, paint);
        paint.setAlpha(255);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(p == world.selected ? 6f : 4f);
        paint.setColor(p == world.selected ? 0xFF2ED6FF : 0xFF9EC8FF);
        c.drawRoundRect(rect, 26f, 26f, paint);
        if (p.unlocked) {
            drawAnimal(c, p.animal, px + p.w * 0.5f, p.y + p.h * 0.56f, p, world.scrollX);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF2B5388);
            paint.setTextSize(26f);
            c.drawText(p.animal.species.name(), px + 16f, p.y + 34f, paint);
            paint.setTextSize(22f);
            c.drawText("Lv " + p.level + "  Goods " + p.storedGoods + "/" + p.capacity, px + 16f, p.y + 54f, paint);

            // status bars
            float barLeft = px + 14f;
            float barRight = px + p.w - 14f;
            float barY1 = p.y + p.h - 42f;
            float barY2 = p.y + p.h - 18f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFE6EEF9);
            c.drawRoundRect(barLeft, barY1, barRight, barY1 + 10f, 6f, 6f, paint);
            c.drawRoundRect(barLeft, barY2, barRight, barY2 + 10f, 6f, 6f, paint);

            paint.setColor(0xFF2ED67A); // feed
            c.drawRoundRect(barLeft, barY1, barLeft + (barRight - barLeft) * clamp01(p.feed), barY1 + 10f, 6f, 6f, paint);
            paint.setColor(0xFF2ED6FF); // cleanliness
            c.drawRoundRect(barLeft, barY2, barLeft + (barRight - barLeft) * clamp01(p.cleanliness), barY2 + 10f, 6f, 6f, paint);
        } else {
            paint.setColor(0xFF5F7FAF);
            paint.setTextSize(26f);
            c.drawText("Locked", px + 70f, p.y + 86f, paint);
        }
    }

    private void drawAnimal(Canvas c, Animal a, float cx, float cy, Pen p, float scrollX) {
        float bob = (float) Math.sin(a.bobTime * 3f) * 3f;
        float s = (24f + a.species.ordinal() * 0.9f) * 1.35f;

        // body outline
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF163055);
        c.drawOval(cx - s - 3f, cy - s * 0.7f - 3f + bob, cx + s + 3f, cy + s * 0.7f + 3f + bob, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(a.species.color);
        c.drawOval(cx - s, cy - s * 0.7f + bob, cx + s, cy + s * 0.7f + bob, paint);

        // highlight
        paint.setColor(0x33FFFFFF);
        c.drawOval(cx - s * 0.65f, cy - s * 0.6f + bob, cx + s * 0.2f, cy - s * 0.05f + bob, paint);

        paint.setColor(0xFF222222);
        c.drawCircle(cx + s * 0.5f, cy - s * 0.2f + bob, s * 0.48f, paint);
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

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
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

    private void drawCoinPopups(Canvas c, RanchWorld world) {
        for (CoinPopup cp : world.coinPopups) {
            if (cp.time <= 0f) continue;

            float t = clamp01(cp.time / 0.8f);
            float alpha = 1f;
            if (t < 0.25f) alpha = t / 0.25f;
            else if (t > 0.75f) alpha = (1f - t) / 0.25f;

            float bubbleW = 54f;
            float bubbleH = 36f;
            float x = cp.x - bubbleW * 0.5f;
            float y = cp.y - bubbleH;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            paint.setAlpha((int) (220 * alpha));
            c.drawRoundRect(x, y, x + bubbleW, y + bubbleH, 16f, 16f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(0xFF9EC8FF);
            paint.setAlpha((int) (255 * alpha));
            c.drawRoundRect(x, y, x + bubbleW, y + bubbleH, 16f, 16f, paint);

            // coin icon
            float cx = x + bubbleW * 0.5f;
            float cy = y + bubbleH * 0.5f + 1f;
            float r = 10f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFD34D);
            paint.setAlpha((int) (255 * alpha));
            c.drawCircle(cx, cy, r, paint);
            paint.setColor(0xFFFFB22B);
            c.drawCircle(cx, cy, r - 2f, paint);
            paint.setColor(0x33FFFFFF);
            c.drawCircle(cx - 3f, cy - 3f, r * 0.45f, paint);
            paint.setAlpha(255);
        }
    }

    private void drawStatePanels(Canvas c, RanchWorld world, int w, int h) {
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
