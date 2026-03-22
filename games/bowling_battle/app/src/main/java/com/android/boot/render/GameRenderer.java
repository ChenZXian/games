package com.android.boot.render;

import android.content.Context;
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
import com.android.boot.model.GameSnapshot;

public final class GameRenderer {
    private final Paint bgPaint = new Paint();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accent2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dangerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint successPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF boardRect = new RectF();
    private final int colorPanel;
    private final int colorStroke;
    private final int colorAccent;
    private final int colorAccent2;
    private final int colorDanger;
    private final int colorSuccess;
    private final int colorWarning;
    private final int colorText;
    private final int colorMuted;
    private final int colorAlt;

    public GameRenderer(Context context) {
        colorPanel = ContextCompat.getColor(context, R.color.cst_panel_bg);
        colorStroke = ContextCompat.getColor(context, R.color.cst_panel_stroke);
        colorAccent = ContextCompat.getColor(context, R.color.cst_accent);
        colorAccent2 = ContextCompat.getColor(context, R.color.cst_accent_2);
        colorDanger = ContextCompat.getColor(context, R.color.cst_danger);
        colorSuccess = ContextCompat.getColor(context, R.color.cst_success);
        colorWarning = ContextCompat.getColor(context, R.color.cst_warning);
        colorText = ContextCompat.getColor(context, R.color.cst_text_primary);
        colorMuted = ContextCompat.getColor(context, R.color.cst_text_muted);
        colorAlt = ContextCompat.getColor(context, R.color.cst_bg_alt);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.cst_bg_main));
        gridPaint.setColor(colorStroke);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3f);
        tilePaint.setStyle(Paint.Style.FILL);
        enemyStroke.setStyle(Paint.Style.STROKE);
        enemyStroke.setStrokeWidth(3f);
        enemyStroke.setColor(colorText);
        textPaint.setColor(colorText);
        textPaint.setTextSize(34f);
        mutedTextPaint.setColor(colorMuted);
        mutedTextPaint.setTextSize(26f);
        accentPaint.setColor(colorAccent);
        accent2Paint.setColor(colorAccent2);
        dangerPaint.setColor(colorDanger);
        successPaint.setColor(colorSuccess);
        warningPaint.setColor(colorWarning);
        glowPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(Color.argb(130, Color.red(colorAlt), Color.green(colorAlt), Color.blue(colorAlt)));
    }

    public void render(Canvas canvas, GameEngine engine, int width, int height) {
        canvas.drawRect(0f, 0f, width, height, bgPaint);
        drawBackdrop(canvas, width, height);
        computeBoard(width, height);
        drawBoard(canvas, engine);
        drawConveyor(canvas, engine, width, height);
        drawHud(canvas, engine.getSnapshot(), width, height);
        drawEffects(canvas, engine);
    }

    private void drawBackdrop(Canvas canvas, int width, int height) {
        bandPaint.setShader(new LinearGradient(0f, 0f, width, height, colorAlt, colorPanel, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height * 0.14f, width, height, bandPaint);
    }

    private void computeBoard(int width, int height) {
        boardRect.set(width * 0.08f, height * 0.25f, width * 0.92f, height * 0.9f);
    }

    private void drawBoard(Canvas canvas, GameEngine engine) {
        tilePaint.setColor(colorPanel);
        canvas.drawRoundRect(boardRect, 28f, 28f, tilePaint);
        canvas.drawRoundRect(boardRect, 28f, 28f, gridPaint);
        float cellW = boardRect.width() / GameEngine.COLS;
        float cellH = boardRect.height() / GameEngine.ROWS;
        for (int r = 0; r < GameEngine.ROWS; r++) {
            for (int c = 0; c < GameEngine.COLS; c++) {
                rect.set(boardRect.left + c * cellW + 6f, boardRect.top + r * cellH + 6f, boardRect.left + (c + 1) * cellW - 6f, boardRect.top + (r + 1) * cellH - 6f);
                int tileBase = ((r + c) & 1) == 0 ? colorAlt : colorPanel;
                tilePaint.setColor(Color.argb(((r + c) & 1) == 0 ? 120 : 150, Color.red(tileBase), Color.green(tileBase), Color.blue(tileBase)));
                canvas.drawRoundRect(rect, 18f, 18f, tilePaint);
            }
        }
        for (int i = 1; i < GameEngine.COLS; i++) {
            float x = boardRect.left + i * cellW;
            canvas.drawLine(x, boardRect.top, x, boardRect.bottom, gridPaint);
        }
        for (int i = 1; i < GameEngine.ROWS; i++) {
            float y = boardRect.top + i * cellH;
            canvas.drawLine(boardRect.left, y, boardRect.right, y, gridPaint);
        }
        tilePaint.setColor(colorDanger);
        rect.set(boardRect.left - 10f, boardRect.top, boardRect.left + 12f, boardRect.bottom);
        canvas.drawRoundRect(rect, 10f, 10f, tilePaint);
        for (GameEngine.Enemy enemy : engine.getEnemies()) {
            float cx = boardRect.left + enemy.x * cellW;
            float cy = boardRect.top + enemy.row * cellH + cellH * 0.5f;
            drawEnemy(canvas, enemy, cx, cy, cellW, cellH);
        }
        for (GameEngine.Ball ball : engine.getBalls()) {
            float cx = boardRect.left + ball.x * cellW;
            float cy = boardRect.top + ball.y * cellH;
            drawBall(canvas, ball, cx, cy, Math.min(cellW, cellH) * 0.25f);
        }
    }

    private void drawEnemy(Canvas canvas, GameEngine.Enemy enemy, float cx, float cy, float cellW, float cellH) {
        float pulse = enemy.flash * 55f;
        if (enemy.maxHp == 1) {
            enemyPaint.setColor(colorSuccess);
        } else if (enemy.maxHp == 2) {
            enemyPaint.setColor(colorWarning);
        } else {
            enemyPaint.setColor(colorDanger);
        }
        if (pulse > 0f) {
            enemyPaint.setColor(Color.argb(255, Math.min(255, Color.red(enemyPaint.getColor()) + (int) pulse), Math.min(255, Color.green(enemyPaint.getColor()) + (int) pulse), Math.min(255, Color.blue(enemyPaint.getColor()) + (int) pulse)));
        }
        rect.set(cx - cellW * 0.24f, cy - cellH * 0.28f, cx + cellW * 0.24f, cy + cellH * 0.28f);
        canvas.drawRoundRect(rect, 16f, 16f, enemyPaint);
        canvas.drawRoundRect(rect, 16f, 16f, enemyStroke);
        textPaint.setTextSize(24f);
        canvas.drawText(String.valueOf(enemy.hp), cx - 6f, cy + 8f, textPaint);
    }

    private void drawBall(Canvas canvas, GameEngine.Ball ball, float cx, float cy, float radius) {
        int fill = colorAccent;
        if (ball.type == BallType.BOMB) {
            fill = colorDanger;
        } else if (ball.type == BallType.GIANT) {
            fill = colorWarning;
        }
        glowPaint.setColor(Color.argb((int) (80 + 40 * Math.sin(ball.glow)), Color.red(fill), Color.green(fill), Color.blue(fill)));
        canvas.drawCircle(cx, cy, radius * 1.65f, glowPaint);
        accentPaint.setColor(fill);
        float rx = radius * ball.scale * (ball.type == BallType.GIANT ? 1.6f : 1f);
        float ry = radius * (ball.type == BallType.BOMB ? 0.95f : 1.05f) / Math.max(0.8f, ball.scale);
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry);
        canvas.drawOval(rect, accentPaint);
        enemyStroke.setColor(colorPanel);
        canvas.drawOval(rect, enemyStroke);
        enemyStroke.setColor(colorText);
    }

    private void drawConveyor(Canvas canvas, GameEngine engine, int width, int height) {
        float left = width * 0.08f;
        float top = height * 0.08f;
        float right = width * 0.92f;
        float bottom = height * 0.19f;
        rect.set(left, top, right, bottom);
        tilePaint.setColor(colorPanel);
        canvas.drawRoundRect(rect, 30f, 30f, tilePaint);
        canvas.drawRoundRect(rect, 30f, 30f, gridPaint);
        float bandOffset = (engine.getConveyorRoll() * 36f) % 48f;
        for (float x = left - 48f + bandOffset; x < right + 48f; x += 48f) {
            tilePaint.setColor(Color.argb(100, Color.red(colorStroke), Color.green(colorStroke), Color.blue(colorStroke)));
            canvas.drawRoundRect(x, top + 16f, x + 24f, bottom - 16f, 10f, 10f, tilePaint);
        }
        drawBallCard(canvas, engine.getCurrentBall(), left + 80f, (top + bottom) * 0.5f, 34f, engine.isConveyorReady(), engine.getConveyorProgress());
        textPaint.setTextSize(28f);
        canvas.drawText(engine.isConveyorReady() ? "Tap a row" : "Loading", left + 138f, top + 54f, textPaint);
        mutedTextPaint.setTextSize(22f);
        canvas.drawText("Next", right - 240f, top + 48f, mutedTextPaint);
        BallType[] next = engine.getNextBalls();
        drawBallCard(canvas, next[0], right - 180f, (top + bottom) * 0.5f, 24f, false, 1f);
        drawBallCard(canvas, next[1], right - 120f, (top + bottom) * 0.5f, 24f, false, 1f);
        drawBallCard(canvas, next[2], right - 60f, (top + bottom) * 0.5f, 24f, false, 1f);
    }

    private void drawBallCard(Canvas canvas, BallType type, float cx, float cy, float radius, boolean active, float progress) {
        rect.set(cx - radius * 1.3f, cy - radius * 1.1f, cx + radius * 1.3f, cy + radius * 1.1f);
        tilePaint.setColor(Color.argb(140, Color.red(colorAlt), Color.green(colorAlt), Color.blue(colorAlt)));
        canvas.drawRoundRect(rect, 18f, 18f, tilePaint);
        canvas.drawRoundRect(rect, 18f, 18f, gridPaint);
        int fill = colorAccent;
        if (type == BallType.BOMB) {
            fill = colorDanger;
        } else if (type == BallType.GIANT) {
            fill = colorWarning;
        }
        if (active) {
            float glow = (float) (0.55f + 0.45f * Math.sin(progress * Math.PI));
            glowPaint.setColor(Color.argb((int) (70 + glow * 80), Color.red(fill), Color.green(fill), Color.blue(fill)));
            canvas.drawCircle(cx, cy, radius * 1.55f, glowPaint);
        }
        accentPaint.setColor(fill);
        canvas.drawCircle(cx, cy, radius, accentPaint);
        enemyStroke.setColor(colorPanel);
        canvas.drawCircle(cx, cy, radius, enemyStroke);
        enemyStroke.setColor(colorText);
    }

    private void drawHud(Canvas canvas, GameSnapshot snapshot, int width, int height) {
        float baseY = height - 52f;
        textPaint.setTextSize(28f);
        canvas.drawText("Score " + snapshot.score, width * 0.08f, baseY, textPaint);
        canvas.drawText("Combo " + snapshot.combo, width * 0.31f, baseY, textPaint);
        canvas.drawText("Wave " + snapshot.waveIndex, width * 0.52f, baseY, textPaint);
        canvas.drawText("Life " + snapshot.remainingLife, width * 0.74f, baseY, textPaint);
    }

    private void drawEffects(Canvas canvas, GameEngine engine) {
        float cellW = boardRect.width() / GameEngine.COLS;
        float cellH = boardRect.height() / GameEngine.ROWS;
        for (GameEngine.Fx fx : engine.getFxList()) {
            float t = fx.time / Math.max(0.0001f, fx.duration);
            if (fx.type == GameEngine.Fx.TYPE_EXPLOSION) {
                int alpha = (int) ((1f - t) * 160f);
                dangerPaint.setColor(Color.argb(alpha, Color.red(colorDanger), Color.green(colorDanger), Color.blue(colorDanger)));
                rect.set(boardRect.left + (fx.colLike - 1.5f) * cellW, boardRect.top + (fx.rowLike - 1.5f) * cellH, boardRect.left + (fx.colLike + 1.5f) * cellW, boardRect.top + (fx.rowLike + 1.5f) * cellH);
                canvas.drawRoundRect(rect, 24f, 24f, dangerPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_POPUP) {
                textPaint.setTextSize(24f + 6f * (1f - t));
                float x = boardRect.left + fx.colLike * cellW;
                float y = boardRect.top + fx.rowLike * cellH - t * 40f;
                canvas.drawText(fx.value > 0 ? "+" + fx.value : typeText(fx.value), x, y, textPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_GIANT) {
                warningPaint.setColor(Color.argb((int) ((1f - t) * 130f), Color.red(colorWarning), Color.green(colorWarning), Color.blue(colorWarning)));
                rect.set(boardRect.left, boardRect.top + (fx.rowLike - 0.25f) * cellH, boardRect.right, boardRect.top + (fx.rowLike + 0.25f) * cellH);
                canvas.drawRoundRect(rect, 20f, 20f, warningPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_HIT) {
                successPaint.setColor(Color.argb((int) ((1f - t) * 180f), Color.red(colorAccent), Color.green(colorAccent), Color.blue(colorAccent)));
                canvas.drawCircle(boardRect.left + fx.colLike * cellW, boardRect.top + fx.rowLike * cellH, cellH * (0.12f + 0.25f * t), successPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_ARROW) {
                textPaint.setTextSize(30f);
                float x = boardRect.left + fx.colLike * cellW;
                float y = boardRect.top + fx.rowLike * cellH - 18f;
                canvas.drawText(fx.value > 0 ? "V" : "^", x, y, textPaint);
            } else if (fx.type == GameEngine.Fx.TYPE_WARNING) {
                dangerPaint.setColor(Color.argb((int) ((1f - t) * 150f), Color.red(colorDanger), Color.green(colorDanger), Color.blue(colorDanger)));
                rect.set(boardRect.left - 18f, boardRect.top + (fx.rowLike - 0.2f) * cellH, boardRect.left + 32f, boardRect.top + (fx.rowLike + 0.2f) * cellH);
                canvas.drawRoundRect(rect, 12f, 12f, dangerPaint);
            }
        }
        if (!GameEngine.STATE_PLAYING.equals(engine.getSnapshot().state)) {
            canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), overlayPaint);
        }
    }

    private String typeText(int value) {
        if (value == 1) {
            return "Normal";
        }
        if (value == 2) {
            return "Bomb";
        }
        return "Giant";
    }
}
