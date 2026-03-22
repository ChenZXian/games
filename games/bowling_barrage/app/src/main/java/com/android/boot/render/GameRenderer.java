package com.android.boot.render;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.engine.GameEngine;
import com.android.boot.model.BallType;
import com.android.boot.model.EnemyType;

public final class GameRenderer {
    private static final float BOARD_LEFT_RATIO = 0.08f;
    private static final float BOARD_TOP_RATIO = 0.24f;
    private static final float BOARD_RIGHT_RATIO = 0.92f;
    private static final float BOARD_BOTTOM_RATIO = 0.92f;
    private static final float CONVEYOR_TOP_RATIO = 0.08f;
    private static final float CONVEYOR_BOTTOM_RATIO = 0.19f;
    private final Paint bgPaint = new Paint();
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentTwoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dangerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint successPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint captionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boardRect = new RectF();
    private final RectF rect = new RectF();
    private final RectF cellRect = new RectF();
    private final int bgColor;
    private final int altColor;
    private final int panelColor;
    private final int strokeColor;
    private final int accentColor;
    private final int accentTwoColor;
    private final int dangerColor;
    private final int successColor;
    private final int warningColor;
    private final int textColor;
    private final int mutedTextColor;
    private final int shadowColor;
    private final float radiusS;
    private final float radiusM;
    private final float radiusL;
    private final float radiusXl;
    private final float strokeS;
    private final float strokeM;
    private final float pad2;
    private final float pad4;
    private final float pad6;
    private final float pad8;
    private final float pad12;
    private final float pad16;
    private final float textSmall;
    private final float textMedium;
    private final float textLarge;
    private final float textHuge;

    public GameRenderer(Context context) {
        Resources resources = context.getResources();
        bgColor = ContextCompat.getColor(context, R.color.cst_bg_main);
        altColor = ContextCompat.getColor(context, R.color.cst_bg_alt);
        panelColor = ContextCompat.getColor(context, R.color.cst_panel_bg);
        strokeColor = ContextCompat.getColor(context, R.color.cst_panel_stroke);
        accentColor = ContextCompat.getColor(context, R.color.cst_accent);
        accentTwoColor = ContextCompat.getColor(context, R.color.cst_accent_2);
        dangerColor = ContextCompat.getColor(context, R.color.cst_danger);
        successColor = ContextCompat.getColor(context, R.color.cst_success);
        warningColor = ContextCompat.getColor(context, R.color.cst_warning);
        textColor = ContextCompat.getColor(context, R.color.cst_text_primary);
        mutedTextColor = ContextCompat.getColor(context, R.color.cst_text_muted);
        shadowColor = ContextCompat.getColor(context, R.color.cst_shadow);
        radiusS = resources.getDimension(R.dimen.cst_radius_s);
        radiusM = resources.getDimension(R.dimen.cst_radius_m);
        radiusL = resources.getDimension(R.dimen.cst_radius_l);
        radiusXl = resources.getDimension(R.dimen.cst_radius_xl);
        strokeS = resources.getDimension(R.dimen.cst_stroke_s);
        strokeM = resources.getDimension(R.dimen.cst_stroke_m);
        pad2 = resources.getDimension(R.dimen.cst_pad_2);
        pad4 = resources.getDimension(R.dimen.cst_pad_4);
        pad6 = resources.getDimension(R.dimen.cst_pad_6);
        pad8 = resources.getDimension(R.dimen.cst_pad_8);
        pad12 = resources.getDimension(R.dimen.cst_pad_12);
        pad16 = resources.getDimension(R.dimen.cst_pad_16);
        textSmall = resources.getDimension(R.dimen.cst_text_s);
        textMedium = resources.getDimension(R.dimen.cst_text_m);
        textLarge = resources.getDimension(R.dimen.cst_text_l);
        textHuge = resources.getDimension(R.dimen.cst_text_xl);
        bgPaint.setColor(bgColor);
        boardPaint.setColor(panelColor);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(strokeS);
        gridPaint.setColor(strokeColor);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textLarge);
        textPaint.setFakeBoldText(true);
        captionPaint.setColor(mutedTextColor);
        captionPaint.setTextSize(textSmall);
        enemyStrokePaint.setStyle(Paint.Style.STROKE);
        enemyStrokePaint.setStrokeWidth(strokeS);
        enemyStrokePaint.setColor(textColor);
        shadowPaint.setColor(shadowColor);
        highlightPaint.setColor(accentColor);
    }

    public void render(Canvas canvas, GameEngine engine, int width, int height) {
        canvas.drawRect(0f, 0f, width, height, bgPaint);
        drawBackdrop(canvas, width, height);
        computeBoard(width, height);
        canvas.save();
        canvas.translate(engine.getShakeOffsetX(), engine.getShakeOffsetY());
        drawBoard(canvas, engine);
        drawEffects(canvas, engine);
        canvas.restore();
        drawConveyor(canvas, engine, width, height);
    }

    private void drawBackdrop(Canvas canvas, int width, int height) {
        bandPaint.setShader(new LinearGradient(0f, 0f, width, height, altColor, panelColor, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height * 0.14f, width, height, bandPaint);
        shadowPaint.setAlpha(30);
        canvas.drawCircle(width * 0.14f, height * 0.2f, width * 0.18f, shadowPaint);
        shadowPaint.setAlpha(26);
        canvas.drawCircle(width * 0.84f, height * 0.82f, width * 0.22f, shadowPaint);
    }

    private void computeBoard(int width, int height) {
        boardRect.set(width * BOARD_LEFT_RATIO, height * BOARD_TOP_RATIO, width * BOARD_RIGHT_RATIO, height * BOARD_BOTTOM_RATIO);
    }

    private void drawBoard(Canvas canvas, GameEngine engine) {
        shadowPaint.setAlpha(70);
        rect.set(boardRect.left + pad2, boardRect.top + pad4, boardRect.right + pad4, boardRect.bottom + pad6);
        canvas.drawRoundRect(rect, radiusXl, radiusXl, shadowPaint);
        canvas.drawRoundRect(boardRect, radiusXl, radiusXl, boardPaint);
        canvas.drawRoundRect(boardRect, radiusXl, radiusXl, gridPaint);
        float cellW = boardRect.width() / GameEngine.COLS;
        float cellH = boardRect.height() / GameEngine.ROWS;
        float alpha = engine.getLaneHighlightAlpha();
        if (alpha > 0f && engine.getHighlightedRow() >= 0) {
            int highlight = Color.argb((int) (60f * alpha), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            highlightPaint.setColor(highlight);
            rect.set(boardRect.left + pad4, boardRect.top + engine.getHighlightedRow() * cellH + pad4, boardRect.right - pad4, boardRect.top + (engine.getHighlightedRow() + 1) * cellH - pad4);
            canvas.drawRoundRect(rect, radiusM, radiusM, highlightPaint);
        }
        for (int row = 0; row < GameEngine.ROWS; row++) {
            for (int col = 0; col < GameEngine.COLS; col++) {
                int base = ((row + col) & 1) == 0 ? altColor : panelColor;
                int fill = Color.argb(((row + col) & 1) == 0 ? 140 : 100, Color.red(base), Color.green(base), Color.blue(base));
                tilePaint.setColor(fill);
                cellRect.set(boardRect.left + col * cellW + pad4, boardRect.top + row * cellH + pad4, boardRect.left + (col + 1) * cellW - pad4, boardRect.top + (row + 1) * cellH - pad4);
                canvas.drawRoundRect(cellRect, radiusS, radiusS, tilePaint);
            }
        }
        for (int col = 1; col < GameEngine.COLS; col++) {
            float x = boardRect.left + col * cellW;
            canvas.drawLine(x, boardRect.top, x, boardRect.bottom, gridPaint);
        }
        for (int row = 1; row < GameEngine.ROWS; row++) {
            float y = boardRect.top + row * cellH;
            canvas.drawLine(boardRect.left, y, boardRect.right, y, gridPaint);
        }
        dangerPaint.setColor(dangerColor);
        rect.set(boardRect.left - pad8, boardRect.top, boardRect.left + pad8, boardRect.bottom);
        canvas.drawRoundRect(rect, radiusS, radiusS, dangerPaint);
        captionPaint.setTextSize(textSmall);
        canvas.drawText("Left Gate", boardRect.left - pad6, boardRect.top - pad8, captionPaint);
        for (GameEngine.Enemy enemy : engine.getEnemies()) {
            drawEnemy(canvas, enemy, cellW, cellH);
        }
        for (GameEngine.Ball ball : engine.getBalls()) {
            drawBall(canvas, ball, cellW, cellH);
        }
    }

    private void drawEnemy(Canvas canvas, GameEngine.Enemy enemy, float cellW, float cellH) {
        float cx = boardRect.left + enemy.x * cellW;
        float cy = boardRect.top + (enemy.row + 0.5f) * cellH;
        int fillColor = enemy.type == EnemyType.SCOUT ? successColor : enemy.type == EnemyType.BRUISER ? warningColor : dangerColor;
        if (enemy.flash > 0f) {
            fillColor = blend(fillColor, Color.WHITE, enemy.flash * 0.6f);
        }
        tilePaint.setColor(fillColor);
        float scalePulse = 1f + enemy.hitScale * 0.12f;
        float halfW = cellW * (enemy.type == EnemyType.TANK ? 0.26f : enemy.type == EnemyType.BRUISER ? 0.23f : 0.2f) * scalePulse;
        float halfH = cellH * (enemy.type == EnemyType.TANK ? 0.32f : enemy.type == EnemyType.BRUISER ? 0.28f : 0.24f) * scalePulse;
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        canvas.drawRoundRect(rect, radiusM, radiusM, tilePaint);
        canvas.drawRoundRect(rect, radiusM, radiusM, enemyStrokePaint);
        float meterWidth = halfW * 2f;
        rect.set(cx - meterWidth * 0.5f, cy - halfH - pad8, cx + meterWidth * 0.5f, cy - halfH - pad4);
        tilePaint.setColor(Color.argb(120, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)));
        canvas.drawRoundRect(rect, radiusS, radiusS, tilePaint);
        float fillRight = rect.left + meterWidth * (enemy.hp / (float) enemy.maxHp);
        rect.set(rect.left, rect.top, fillRight, rect.bottom);
        tilePaint.setColor(fillColor);
        canvas.drawRoundRect(rect, radiusS, radiusS, tilePaint);
        textPaint.setTextSize(textSmall);
        canvas.drawText(enemy.type.label, cx - halfW, cy + halfH + pad12, textPaint);
    }

    private void drawBall(Canvas canvas, GameEngine.Ball ball, float cellW, float cellH) {
        float cx = boardRect.left + ball.x * cellW;
        float cy = boardRect.top + ball.y * cellH;
        int fillColor = ball.type == BallType.NORMAL ? accentColor : ball.type == BallType.BOMB ? dangerColor : warningColor;
        pulsePaint.setColor(Color.argb((int) (70 + 40 * Math.abs(Math.sin(ball.glow))), Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor)));
        canvas.drawCircle(cx, cy, cellH * 0.23f * ball.scale * (1f + ball.hitPulse * 0.25f), pulsePaint);
        tilePaint.setColor(fillColor);
        float radius = Math.min(cellW, cellH) * 0.14f * ball.scale;
        if (ball.type == BallType.GIANT) {
            rect.set(cx - radius * 1.4f, cy - radius, cx + radius * 1.4f, cy + radius);
            canvas.drawOval(rect, tilePaint);
            canvas.drawOval(rect, enemyStrokePaint);
        } else {
            canvas.drawCircle(cx, cy, radius, tilePaint);
            canvas.drawCircle(cx, cy, radius, enemyStrokePaint);
        }
    }

    private void drawConveyor(Canvas canvas, GameEngine engine, int width, int height) {
        float left = width * BOARD_LEFT_RATIO;
        float right = width * BOARD_RIGHT_RATIO;
        float top = height * CONVEYOR_TOP_RATIO;
        float bottom = height * CONVEYOR_BOTTOM_RATIO;
        rect.set(left, top, right, bottom);
        shadowPaint.setAlpha(50);
        canvas.drawRoundRect(left + pad2, top + pad4, right + pad2, bottom + pad6, radiusXl, radiusXl, shadowPaint);
        canvas.drawRoundRect(rect, radiusXl, radiusXl, boardPaint);
        canvas.drawRoundRect(rect, radiusXl, radiusXl, gridPaint);
        float stripe = pad16 + pad4;
        for (float x = left + (float) (Math.sin(engine.getConveyorPulse()) * pad8); x < right; x += stripe) {
            tilePaint.setColor(Color.argb(90, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)));
            canvas.drawRoundRect(x, top + pad12, x + pad8, bottom - pad12, radiusS, radiusS, tilePaint);
        }
        drawConveyorSlot(canvas, engine.getCurrentBall(), left + (right - left) * 0.12f, (top + bottom) * 0.5f, height * 0.04f, engine.isConveyorReady(), engine.isBallArmed(), engine.getConveyorCooldownProgress());
        textPaint.setTextSize(textMedium);
        canvas.drawText("Ready Ball", left + (right - left) * 0.2f, top + pad16 + textSmall, textPaint);
        captionPaint.setTextSize(textSmall);
        String hint = engine.isBallArmed() ? "Tap a lane to launch" : engine.isConveyorReady() ? "Tap conveyor to arm" : "Loading next ball";
        canvas.drawText(hint, left + (right - left) * 0.2f, bottom - pad12, captionPaint);
        canvas.drawText("Queue", right - width * 0.24f, top + pad16 + textSmall, captionPaint);
        BallType[] nextBalls = engine.getNextBalls();
        drawConveyorSlot(canvas, nextBalls[0], right - width * 0.18f, (top + bottom) * 0.5f, height * 0.029f, false, false, 1f);
        drawConveyorSlot(canvas, nextBalls[1], right - width * 0.12f, (top + bottom) * 0.5f, height * 0.026f, false, false, 1f);
        drawConveyorSlot(canvas, nextBalls[2], right - width * 0.07f, (top + bottom) * 0.5f, height * 0.023f, false, false, 1f);
    }

    private void drawConveyorSlot(Canvas canvas, BallType type, float cx, float cy, float radius, boolean ready, boolean armed, float progress) {
        rect.set(cx - radius * 1.65f, cy - radius * 1.3f, cx + radius * 1.65f, cy + radius * 1.3f);
        tilePaint.setColor(Color.argb(120, Color.red(altColor), Color.green(altColor), Color.blue(altColor)));
        canvas.drawRoundRect(rect, radiusM, radiusM, tilePaint);
        canvas.drawRoundRect(rect, radiusM, radiusM, gridPaint);
        int fillColor = type == BallType.NORMAL ? accentColor : type == BallType.BOMB ? dangerColor : warningColor;
        if (ready) {
            int alpha = (int) ((0.45f + 0.55f * Math.abs(Math.sin(progress * Math.PI * 3f))) * 110f);
            pulsePaint.setColor(Color.argb(alpha, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor)));
            canvas.drawCircle(cx, cy, radius * 1.55f, pulsePaint);
        }
        if (armed) {
            pulsePaint.setColor(Color.argb(90, Color.red(accentTwoColor), Color.green(accentTwoColor), Color.blue(accentTwoColor)));
            canvas.drawCircle(cx, cy, radius * 1.85f, pulsePaint);
        }
        tilePaint.setColor(fillColor);
        if (type == BallType.GIANT) {
            rect.set(cx - radius * 1.2f, cy - radius * 0.82f, cx + radius * 1.2f, cy + radius * 0.82f);
            canvas.drawOval(rect, tilePaint);
            canvas.drawOval(rect, enemyStrokePaint);
        } else {
            canvas.drawCircle(cx, cy, radius, tilePaint);
            canvas.drawCircle(cx, cy, radius, enemyStrokePaint);
        }
    }

    private void drawEffects(Canvas canvas, GameEngine engine) {
        float cellW = boardRect.width() / GameEngine.COLS;
        float cellH = boardRect.height() / GameEngine.ROWS;
        for (GameEngine.Fx fx : engine.getFxList()) {
            float t = fx.time / Math.max(0.0001f, fx.duration);
            if (fx.type == GameEngine.Fx.TYPE_DAMAGE_POPUP) {
                textPaint.setTextSize(textLarge - textSmall * t * 0.3f);
                textPaint.setColor(dangerColor);
                canvas.drawText("-" + fx.value, boardRect.left + fx.colLike * cellW, boardRect.top + fx.rowLike * cellH - pad16 * t, textPaint);
                textPaint.setColor(textColor);
            } else if (fx.type == GameEngine.Fx.TYPE_SCORE_POPUP) {
                textPaint.setTextSize(textMedium);
                textPaint.setColor(successColor);
                canvas.drawText("+" + fx.value, boardRect.left + fx.colLike * cellW, boardRect.top + fx.rowLike * cellH - pad16 * t, textPaint);
                textPaint.setColor(textColor);
            } else if (fx.type == GameEngine.Fx.TYPE_PARTICLE) {
                float angle = (float) (fx.value * 1.04f + t * 3.2f);
                float dist = pad12 * t * 1.5f;
                float x = boardRect.left + fx.colLike * cellW + (float) Math.cos(angle) * dist;
                float y = boardRect.top + fx.rowLike * cellH + (float) Math.sin(angle) * dist;
                accentPaint.setColor(Color.argb((int) ((1f - t) * 180f), Color.red(accentTwoColor), Color.green(accentTwoColor), Color.blue(accentTwoColor)));
                canvas.drawCircle(x, y, pad2 * (1f - t * 0.4f), accentPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_EXPLOSION) {
                dangerPaint.setColor(Color.argb((int) ((1f - t) * 150f), Color.red(dangerColor), Color.green(dangerColor), Color.blue(dangerColor)));
                rect.set(boardRect.left + (fx.colLike - 1.2f - t * 0.3f) * cellW, boardRect.top + (fx.rowLike - 1.2f - t * 0.3f) * cellH, boardRect.left + (fx.colLike + 1.2f + t * 0.3f) * cellW, boardRect.top + (fx.rowLike + 1.2f + t * 0.3f) * cellH);
                canvas.drawRoundRect(rect, radiusL, radiusL, dangerPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_WARNING) {
                dangerPaint.setColor(Color.argb((int) ((1f - t) * 140f), Color.red(dangerColor), Color.green(dangerColor), Color.blue(dangerColor)));
                rect.set(boardRect.left - pad8, boardRect.top + (fx.rowLike - 0.4f) * cellH, boardRect.left + pad16, boardRect.top + (fx.rowLike + 0.4f) * cellH);
                canvas.drawRoundRect(rect, radiusM, radiusM, dangerPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_GIANT_SWEEP) {
                warningPaint.setColor(Color.argb((int) ((1f - t) * 110f), Color.red(warningColor), Color.green(warningColor), Color.blue(warningColor)));
                rect.set(boardRect.left, boardRect.top + (fx.rowLike - 0.4f) * cellH, boardRect.right, boardRect.top + (fx.rowLike + 0.4f) * cellH);
                canvas.drawRoundRect(rect, radiusL, radiusL, warningPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_SPARK) {
                accentTwoPaint.setColor(Color.argb((int) ((1f - t) * 120f), Color.red(accentTwoColor), Color.green(accentTwoColor), Color.blue(accentTwoColor)));
                float y = boardRect.top + fx.rowLike * cellH;
                canvas.drawLine(boardRect.left + pad8, y - pad8 * t, boardRect.left + pad16 + pad8 * t, y + pad8 * t, accentTwoPaint);
            }
        }
    }

    private int blend(int startColor, int endColor, float fraction) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        int r = (int) (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * clamped);
        int g = (int) (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * clamped);
        int b = (int) (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * clamped);
        return Color.rgb(r, g, b);
    }
}
