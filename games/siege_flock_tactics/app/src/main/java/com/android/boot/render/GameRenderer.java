package com.android.boot.render;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.android.boot.R;
import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameDefs;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int bg;
    private final int accent;
    private final int textPrimary;
    private final int wood;
    private final int stone;
    private final int glass;
    private final int metal;

    public GameRenderer(Resources r) {
        bg = r.getColor(R.color.cst_bg_main, null);
        accent = r.getColor(R.color.cst_accent, null);
        textPrimary = r.getColor(R.color.cst_text_primary, null);
        wood = Color.parseColor("#8A5A35");
        stone = Color.parseColor("#707A8D");
        glass = Color.parseColor("#79DFFF");
        metal = Color.parseColor("#A5ACC0");
        text.setColor(textPrimary);
        text.setTextSize(r.getDimension(R.dimen.cst_text_l));
    }

    public void draw(Canvas c, GameEngine e, int w, int h) {
        c.drawColor(bg);
        if (e.state == GameDefs.MENU) {
            return;
        }
        drawMechanics(c, e);
        drawBlocks(c, e);
        drawBird(c, e);
        drawFx(c, e, w, h);
        drawHud(c, e, w);
        drawState(c, e, w, h);
    }

    private void drawMechanics(Canvas c, GameEngine e) {
        for (int i = 0; i < e.mechanicCount; i++) {
            GameEngine.Mechanic m = e.mechanics[i];
            if (m.kind == 1) {
                paint.setColor(0x332ED6FF);
            } else if (m.kind == 2) {
                paint.setColor(0x55FFD166);
            } else {
                paint.setColor(0x44FF4D6D);
            }
            c.drawRect(m.x, m.y, m.x + m.w, m.y + m.h, paint);
        }
    }

    private void drawBlocks(Canvas c, GameEngine e) {
        for (int i = 0; i < e.blockCount; i++) {
            GameEngine.Block b = e.blocks[i];
            if (b.hp <= 0f) {
                continue;
            }
            if (b.target) {
                drawPig(c, b);
            } else {
                int color = b.material == GameDefs.MAT_WOOD ? wood : b.material == GameDefs.MAT_STONE ? stone : b.material == GameDefs.MAT_GLASS ? glass : metal;
                paint.setColor(color);
                c.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paint);
            }
        }
    }

    private void drawPig(Canvas c, GameEngine.Block b) {
        float cx = b.x + b.w * 0.5f;
        float cy = b.y + b.h * 0.5f;
        float radius = Math.min(b.w, b.h) * 0.4f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFF6B6B);
        c.drawCircle(cx, cy, radius, paint);
        paint.setColor(0xFFFF8E8E);
        c.drawCircle(cx, cy, radius * 0.85f, paint);
        paint.setColor(0xFF000000);
        float eyeSize = radius * 0.15f;
        c.drawCircle(cx - radius * 0.3f, cy - radius * 0.2f, eyeSize, paint);
        c.drawCircle(cx + radius * 0.3f, cy - radius * 0.2f, eyeSize, paint);
        paint.setColor(0xFF000000);
        float noseSize = radius * 0.2f;
        c.drawCircle(cx, cy + radius * 0.15f, noseSize, paint);
        paint.setColor(0xFFFF6B6B);
        c.drawCircle(cx, cy + radius * 0.15f, noseSize * 0.6f, paint);
    }

    private void drawBird(Canvas c, GameEngine e) {
        if (e.trajectoryCount > 0) {
            paint.setColor(0x88FFFFFF);
            for (int i = 0; i < e.trajectoryCount; i++) {
                c.drawCircle(e.trajectoryX[i], e.trajectoryY[i], 4f, paint);
            }
        }
        if (e.bird.active) {
            paint.setColor(accent);
            c.drawCircle(e.bird.x, e.bird.y, 18f, paint);
            if (e.bird.shield > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6f);
                paint.setColor(0xAA6EF6FF);
                c.drawCircle(e.bird.x, e.bird.y, 32f, paint);
                paint.setColor(0x666EF6FF);
                c.drawCircle(e.bird.x, e.bird.y, 38f, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        } else {
            paint.setColor(accent);
            c.drawCircle(e.bird.x, e.bird.y, 18f, paint);
        }
    }

    private void drawFx(Canvas c, GameEngine e, int w, int h) {
        if (e.flash > 0f) {
            paint.setColor(((int) (120 * e.flash) << 24) | 0xFFFFFF);
            c.drawRect(0, 0, w, h, paint);
        }
        if (e.explosion > 0f && e.bird.active) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(0xAAFFD166);
            c.drawCircle(e.bird.x, e.bird.y, 100f * e.explosion, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (e.popupTime > 0f) {
            text.setColor(0xFF43F7A0);
            text.setTextSize(38f);
            c.drawText("+120", 760f, e.popupY, text);
            text.setColor(textPrimary);
        }
    }

    private void drawHud(Canvas c, GameEngine e, int w) {
        text.setTextSize(36f);
        c.drawText("Level: " + GameDefs.LEVEL_NAMES[e.levelIndex], 24f, 48f, text);
        paint.setColor(0xFF4FC3F7);
        text.setColor(0xFF4FC3F7);
        text.setTextSize(42f);
        c.drawText("Birds Left: " + e.birdsLeft, 24f, 100f, text);
        text.setColor(textPrimary);
        text.setTextSize(36f);
        c.drawText("Score: " + e.score, w - 260f, 48f, text);
        c.drawText("Unit: " + unitName(e.currentUnit), w - 260f, 90f, text);
        if (e.toastTime > 0f) {
            paint.setColor(0xAA121B36);
            c.drawRoundRect(w * 0.35f, 16f, w * 0.65f, 76f, 18f, 18f, paint);
            c.drawText(e.toast, w * 0.42f, 56f, text);
        }
    }

    private void drawState(Canvas c, GameEngine e, int w, int h) {
        if (e.state == GameDefs.PAUSED) {
            text.setTextSize(56f);
            c.drawText("Paused", w * 0.45f, h * 0.45f, text);
        } else if (e.state == GameDefs.GAME_OVER) {
            text.setTextSize(50f);
            c.drawText(e.stars > 0 ? "Victory" : "Defeat", w * 0.44f, h * 0.42f, text);
            c.drawText("Stars: " + e.stars + " / 3", w * 0.42f, h * 0.52f, text);
        }
    }

    private String unitName(int unit) {
        if (unit == 0) {
            return "Ram Bird";
        }
        if (unit == 1) {
            return "Split Bird";
        }
        if (unit == 2) {
            return "Bomb Bird";
        }
        if (unit == 3) {
            return "Drill Bird";
        }
        return "Shield Bird";
    }
}
