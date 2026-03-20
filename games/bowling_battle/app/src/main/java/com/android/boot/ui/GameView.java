package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.GameEngine;
import com.android.boot.core.GameState;
import com.android.boot.entity.BallProjectile;
import com.android.boot.entity.BallType;
import com.android.boot.entity.EnemyCell;
import com.android.boot.entity.EnemyType;
import com.android.boot.entity.ExplosionPulse;
import com.android.boot.entity.FloatingText;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    public interface InputListener {
        void onRowTapped(int row);
    }

    private final Paint bgPaint = new Paint();
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileAltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF boardRect = new RectF();
    private final RectF conveyorRect = new RectF();
    private final RectF previewRect = new RectF();
    private final GameEngine engine = new GameEngine();
    private float boardLeft;
    private float boardTop;
    private float boardWidth;
    private float boardHeight;
    private float cellWidth;
    private float cellHeight;
    private float downScale = 1f;
    private InputListener inputListener;
    private LoopThread loopThread;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.cst_bg));
        panelPaint.setColor(ContextCompat.getColor(context, R.color.cst_panel));
        lanePaint.setColor(ContextCompat.getColor(context, R.color.cst_board_lane));
        tilePaint.setColor(ContextCompat.getColor(context, R.color.cst_board_tile));
        tileAltPaint.setColor(ContextCompat.getColor(context, R.color.cst_board_tile_alt));
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);
        outlinePaint.setColor(ContextCompat.getColor(context, R.color.cst_outline));
        textPaint.setColor(ContextCompat.getColor(context, R.color.cst_text_primary));
        textPaint.setTextSize(34f);
        textPaint.setFakeBoldText(true);
        smallTextPaint.setColor(ContextCompat.getColor(context, R.color.cst_text_secondary));
        smallTextPaint.setTextSize(24f);
        glowPaint.setColor(ContextCompat.getColor(context, R.color.cst_glow));
        hitPaint.setColor(Color.WHITE);
        overlayPaint.setColor(ContextCompat.getColor(context, R.color.cst_shadow));
    }

    public GameEngine getEngine() {
        return engine;
    }

    public void setInputListener(InputListener listener) {
        inputListener = listener;
    }

    public void startLoop() {
        if (loopThread == null) {
            loopThread = new LoopThread(getHolder());
            loopThread.running = true;
            loopThread.start();
        }
    }

    public void stopLoop() {
        if (loopThread != null) {
            loopThread.running = false;
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            loopThread = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downScale = 0.94f;
            if (engine.getGameState() == GameState.PLAYING) {
                int row = rowFromY(event.getY());
                if (row >= 0 && inputListener != null) {
                    inputListener.onRowTapped(row);
                }
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            downScale = 1f;
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int rowFromY(float y) {
        if (y < boardTop || y > boardTop + boardHeight) {
            return -1;
        }
        return Math.min(GameEngine.ROWS - 1, Math.max(0, (int) ((y - boardTop) / cellHeight)));
    }

    private void render(Canvas canvas) {
        if (canvas == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        canvas.drawRect(0f, 0f, width, height, bgPaint);
        boardLeft = width * 0.07f;
        boardTop = height * 0.24f;
        boardWidth = width * 0.86f;
        boardHeight = height * 0.62f;
        cellWidth = boardWidth / GameEngine.COLS;
        cellHeight = boardHeight / GameEngine.ROWS;
        boardRect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
        engine.setBoardRect(boardRect.left, boardRect.top, boardRect.right, boardRect.bottom);
        conveyorRect.set(boardLeft, height * 0.11f, boardLeft + boardWidth, height * 0.18f);
        canvas.drawRoundRect(conveyorRect, 24f, 24f, panelPaint);
        drawConveyor(canvas);
        drawBoard(canvas);
        drawEnemies(canvas);
        drawBall(canvas);
        drawEffects(canvas);
        drawLabels(canvas);
    }

    private void drawConveyor(Canvas canvas) {
        float stripeWidth = cellWidth * 0.8f;
        for (int i = -1; i < 12; i++) {
            float left = conveyorRect.left + i * stripeWidth - engine.getConveyorOffset();
            rect.set(left, conveyorRect.top, left + stripeWidth * 0.6f, conveyorRect.bottom);
            canvas.drawRoundRect(rect, 16f, 16f, (i & 1) == 0 ? tilePaint : tileAltPaint);
        }
        float centerX = conveyorRect.left + cellWidth * 0.8f;
        float centerY = conveyorRect.centerY();
        float pulse = 1f + 0.08f * (float) Math.sin(engine.getGlowPulse());
        drawBallToken(canvas, BallType.values()[engine.getCurrentBallType()], centerX, centerY, 28f * pulse, true);
        smallTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Current", conveyorRect.left + 12f, conveyorRect.top - 10f, smallTextPaint);
        canvas.drawText("Next", conveyorRect.right - cellWidth * 2.3f, conveyorRect.top - 10f, smallTextPaint);
        for (int i = 0; i < 2; i++) {
            previewRect.set(conveyorRect.right - cellWidth * (2.2f - i * 0.95f), conveyorRect.top + 8f, conveyorRect.right - cellWidth * (1.45f - i * 0.95f), conveyorRect.bottom - 8f);
            canvas.drawRoundRect(previewRect, 18f, 18f, panelPaint);
            drawBallToken(canvas, BallType.values()[engine.getPreviewBallType(i)], previewRect.centerX(), previewRect.centerY(), 18f, false);
        }
        float readyWidth = boardWidth * engine.getConveyorRatio();
        rect.set(conveyorRect.left, conveyorRect.bottom + 10f, conveyorRect.left + readyWidth, conveyorRect.bottom + 18f);
        canvas.drawRoundRect(rect, 6f, 6f, glowPaint);
    }

    private void drawBoard(Canvas canvas) {
        canvas.drawRoundRect(boardRect, 28f, 28f, lanePaint);
        for (int row = 0; row < GameEngine.ROWS; row++) {
            for (int col = 0; col < GameEngine.COLS; col++) {
                rect.set(boardLeft + col * cellWidth + 4f, boardTop + row * cellHeight + 4f, boardLeft + (col + 1) * cellWidth - 4f, boardTop + (row + 1) * cellHeight - 4f);
                canvas.drawRoundRect(rect, 14f, 14f, ((row + col) & 1) == 0 ? tilePaint : tileAltPaint);
            }
        }
        canvas.drawRoundRect(boardRect, 28f, 28f, outlinePaint);
        for (int row = 0; row < GameEngine.ROWS; row++) {
            float y = boardTop + row * cellHeight + cellHeight * 0.5f;
            smallTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Tap", boardLeft - 56f, y + 8f, smallTextPaint);
        }
    }

    private void drawEnemies(Canvas canvas) {
        EnemyCell[][] board = engine.getBoard();
        for (int row = 0; row < GameEngine.ROWS; row++) {
            for (int col = 0; col < GameEngine.COLS; col++) {
                EnemyCell enemy = board[row][col];
                if (!enemy.active) {
                    continue;
                }
                float cx = boardLeft + enemy.x * cellWidth;
                float cy = boardTop + row * cellHeight + cellHeight * 0.5f;
                float radius = Math.min(cellWidth, cellHeight) * 0.28f;
                if (enemy.type == EnemyType.LIGHT) {
                    enemyPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_enemy_light));
                } else if (enemy.type == EnemyType.MEDIUM) {
                    enemyPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_enemy_medium));
                } else {
                    enemyPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_enemy_heavy));
                }
                if (enemy.hitFlash > 0f) {
                    enemyPaint.setAlpha(255);
                    canvas.drawCircle(cx, cy, radius + 8f * enemy.hitFlash, hitPaint);
                }
                canvas.drawCircle(cx, cy, radius, enemyPaint);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(22f);
                canvas.drawText(String.valueOf(enemy.hp), cx, cy + 8f, textPaint);
            }
        }
    }

    private void drawBall(Canvas canvas) {
        BallProjectile ball = engine.getBall();
        if (!ball.active) {
            return;
        }
        float cx = boardLeft + ball.x * cellWidth;
        float cy = boardTop + ball.y * cellHeight;
        float radius = Math.min(cellWidth, cellHeight) * 0.24f;
        float squash = 1f + ball.squash;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(squash, 1f / squash);
        drawBallToken(canvas, ball.type, 0f, 0f, radius, true);
        canvas.restore();
        if (ball.type == BallType.NORMAL && engine.getBounceArrowTimer() > 0f) {
            smallTextPaint.setTextAlign(Paint.Align.CENTER);
            String arrow = ball.verticalDir >= 0 ? "v" : "^";
            canvas.drawText(arrow, cx, cy - radius - 18f, smallTextPaint);
        }
    }

    private void drawBallToken(Canvas canvas, BallType type, float cx, float cy, float radius, boolean glow) {
        int color;
        if (type == BallType.BOMB) {
            color = ContextCompat.getColor(getContext(), R.color.cst_ball_bomb);
        } else if (type == BallType.GIANT) {
            color = ContextCompat.getColor(getContext(), R.color.cst_ball_giant);
        } else {
            color = ContextCompat.getColor(getContext(), R.color.cst_ball_normal);
        }
        enemyPaint.setColor(color);
        if (glow) {
            glowPaint.setAlpha(110);
            canvas.drawCircle(cx, cy, radius + 12f, glowPaint);
        }
        canvas.drawCircle(cx, cy, radius, enemyPaint);
        if (type == BallType.BOMB) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(radius);
            canvas.drawText("B", cx, cy + radius * 0.35f, textPaint);
        } else if (type == BallType.GIANT) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(radius);
            canvas.drawText("G", cx, cy + radius * 0.35f, textPaint);
        }
    }

    private void drawEffects(Canvas canvas) {
        ExplosionPulse explosion = engine.getExplosionPulse();
        if (explosion.active) {
            for (int row = Math.max(0, explosion.centerRow - 1); row <= Math.min(GameEngine.ROWS - 1, explosion.centerRow + 1); row++) {
                for (int col = Math.max(0, explosion.centerCol - 1); col <= Math.min(GameEngine.COLS - 1, explosion.centerCol + 1); col++) {
                    float alpha = explosion.life / 0.4f;
                    overlayPaint.setAlpha((int) (120 * alpha));
                    rect.set(boardLeft + col * cellWidth + 2f, boardTop + row * cellHeight + 2f, boardLeft + (col + 1) * cellWidth - 2f, boardTop + (row + 1) * cellHeight - 2f);
                    canvas.drawRoundRect(rect, 14f, 14f, overlayPaint);
                }
            }
        }
        FloatingText floatingText = engine.getComboText();
        if (floatingText.active) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(28f);
            canvas.drawText(floatingText.text, floatingText.x, floatingText.y, textPaint);
        }
        if (engine.getStarAnim() > 0f) {
            float size = 1f + engine.getStarAnim() * 0.35f;
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(48f * size);
            canvas.drawText("***", boardRect.centerX(), boardRect.top - 28f, textPaint);
        }
        if (engine.getGiantFlash() > 0f) {
            overlayPaint.setAlpha((int) (120 * engine.getGiantFlash() / 0.12f));
            canvas.drawRect(boardRect.left, boardTop + engine.getBall().row * cellHeight, boardRect.right, boardTop + (engine.getBall().row + 1) * cellHeight, overlayPaint);
        }
    }

    private void drawLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(34f);
        canvas.drawText("5x9 Conveyor Defense", boardLeft, boardTop - 46f, textPaint);
        smallTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(engine.canDrop() ? "Drop ready" : "Conveyor loading", boardLeft, boardTop - 16f, smallTextPaint);
        if (engine.getGameState() != GameState.PLAYING) {
            overlayPaint.setAlpha(80);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), overlayPaint);
        }
    }

    private final class LoopThread extends Thread {
        private final SurfaceHolder holder;
        private long lastTime;
        private boolean running;

        private LoopThread(SurfaceHolder holder) {
            this.holder = holder;
        }

        @Override
        public void run() {
            lastTime = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                float delta = (now - lastTime) / 1000000000f;
                lastTime = now;
                if (delta > 0.05f) {
                    delta = 0.05f;
                }
                engine.update(delta);
                Canvas canvas = holder.lockCanvas();
                try {
                    render(canvas);
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}
