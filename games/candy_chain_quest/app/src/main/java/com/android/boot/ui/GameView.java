package com.android.boot.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.BoardPhase;
import com.android.boot.core.BoardManager;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.entity.Cell;
import com.android.boot.entity.ObstacleType;
import com.android.boot.entity.Position;
import com.android.boot.entity.SpecialType;
import com.android.boot.entity.TileColor;
import com.android.boot.entity.VisualEffect;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final GameSession gameSession;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private GameLoopThread loopThread;
    private float boardLeft;
    private float boardTop;
    private float cellSize;
    private float tileInset;
    private float boardCorner;
    private float boardWidth;
    private float boardHeight;
    private int colorRed;
    private int colorBlue;
    private int colorGreen;
    private int colorYellow;
    private int colorPurple;
    private int colorOrange;
    private int colorBoard;
    private int colorBoardStroke;
    private int colorText;
    private int colorAccent;
    private int colorAccent2;
    private int colorIce;
    private int colorCrate;
    private int colorChain;
    private int colorStone;
    private int colorGridHint;

    public GameView(Context context, GameSession gameSession) {
        super(context);
        this.gameSession = gameSession;
        getHolder().addCallback(this);
        setFocusable(true);
        Resources resources = getResources();
        colorRed = ContextCompat.getColor(context, R.color.cst_tile_red);
        colorBlue = ContextCompat.getColor(context, R.color.cst_tile_blue);
        colorGreen = ContextCompat.getColor(context, R.color.cst_tile_green);
        colorYellow = ContextCompat.getColor(context, R.color.cst_tile_yellow);
        colorPurple = ContextCompat.getColor(context, R.color.cst_tile_purple);
        colorOrange = ContextCompat.getColor(context, R.color.cst_tile_orange);
        colorBoard = ContextCompat.getColor(context, R.color.cst_board_bg);
        colorBoardStroke = ContextCompat.getColor(context, R.color.cst_board_stroke);
        colorText = ContextCompat.getColor(context, R.color.cst_text_primary);
        colorAccent = ContextCompat.getColor(context, R.color.cst_accent);
        colorAccent2 = ContextCompat.getColor(context, R.color.cst_accent_2);
        colorIce = ContextCompat.getColor(context, R.color.cst_obstacle_ice);
        colorCrate = ContextCompat.getColor(context, R.color.cst_obstacle_crate);
        colorChain = ContextCompat.getColor(context, R.color.cst_obstacle_chain);
        colorStone = ContextCompat.getColor(context, R.color.cst_obstacle_stone);
        colorGridHint = ContextCompat.getColor(context, R.color.cst_grid_hint);
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.cst_bg_alt));
        boardPaint.setColor(colorBoard);
        boardStrokePaint.setStyle(Paint.Style.STROKE);
        boardStrokePaint.setStrokeWidth(resources.getDisplayMetrics().density * 2f);
        boardStrokePaint.setColor(colorBoardStroke);
        textPaint.setColor(colorText);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(resources.getDisplayMetrics().scaledDensity * 16f);
        hintPaint.setColor(colorAccent);
        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setStrokeWidth(resources.getDisplayMetrics().density * 3f);
        effectPaint.setTextAlign(Paint.Align.CENTER);
        effectPaint.setTextSize(resources.getDisplayMetrics().scaledDensity * 18f);
        effectPaint.setStyle(Paint.Style.FILL);
        tileInnerPaint.setColor(0x44FFFFFF);
    }

    public void onResume() {
        if (loopThread == null) {
            loopThread = new GameLoopThread(getHolder(), this);
            loopThread.start();
        }
    }

    public void onPause() {
        if (loopThread != null) {
            loopThread.requestStopLoop();
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
            }
            loopThread = null;
        }
    }

    public void step(float delta) {
        gameSession.update(delta);
    }

    public void render(Canvas canvas) {
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), backgroundPaint);
        drawBoard(canvas);
        drawOverlayCue(canvas);
        drawEffects(canvas);
    }

    private void drawBoard(Canvas canvas) {
        BoardManager boardManager = gameSession.getBoardManager();
        if (boardManager.getRows() == 0 || boardManager.getCols() == 0) {
            return;
        }
        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float availableHeight = getHeight() - getPaddingTop() - getPaddingBottom() - dp(160f);
        boardWidth = Math.min(availableWidth - dp(24f), availableHeight);
        boardHeight = boardWidth;
        cellSize = boardWidth / boardManager.getCols();
        tileInset = cellSize * 0.12f;
        boardLeft = (getWidth() - boardWidth) * 0.5f;
        boardTop = Math.max(dp(88f), (getHeight() - boardHeight) * 0.48f);
        boardCorner = dp(18f);
        rect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
        canvas.drawRoundRect(rect, boardCorner, boardCorner, boardPaint);
        canvas.drawRoundRect(rect, boardCorner, boardCorner, boardStrokePaint);
        Position selected = gameSession.getInputManager().getSelected();
        for (int row = 0; row < boardManager.getRows(); row++) {
            for (int col = 0; col < boardManager.getCols(); col++) {
                float left = boardLeft + col * cellSize;
                float top = boardTop + row * cellSize;
                drawCell(canvas, boardManager.getCell(row, col), row, col, left, top, selected);
            }
        }
    }

    private void drawCell(Canvas canvas, Cell cell, int row, int col, float left, float top, Position selected) {
        rect.set(left + tileInset, top + tileInset, left + cellSize - tileInset, top + cellSize - tileInset);
        if (cell.hasTile()) {
            tilePaint.setColor(resolveTileColor(cell.getTileColor()));
            canvas.drawRoundRect(rect, cellSize * 0.24f, cellSize * 0.24f, tilePaint);
            RectF inner = new RectF(rect.left + cellSize * 0.1f, rect.top + cellSize * 0.08f, rect.right - cellSize * 0.08f, rect.top + cellSize * 0.28f);
            canvas.drawRoundRect(inner, cellSize * 0.14f, cellSize * 0.14f, tileInnerPaint);
            drawSpecial(canvas, rect, cell.getSpecialType());
        }
        drawObstacle(canvas, rect, cell);
        if (selected != null && selected.row == row && selected.col == col) {
            canvas.drawRoundRect(rect, cellSize * 0.24f, cellSize * 0.24f, hintPaint);
        }
    }

    private void drawSpecial(Canvas canvas, RectF rect, SpecialType specialType) {
        if (specialType == SpecialType.NONE) {
            return;
        }
        tilePaint.setColor(colorText);
        float cx = rect.centerX();
        float cy = rect.centerY();
        if (specialType == SpecialType.LINE_HORIZONTAL) {
            canvas.drawRect(rect.left + cellSize * 0.12f, cy - cellSize * 0.06f, rect.right - cellSize * 0.12f, cy + cellSize * 0.06f, tilePaint);
        } else if (specialType == SpecialType.LINE_VERTICAL) {
            canvas.drawRect(cx - cellSize * 0.06f, rect.top + cellSize * 0.12f, cx + cellSize * 0.06f, rect.bottom - cellSize * 0.12f, tilePaint);
        } else if (specialType == SpecialType.BOMB) {
            canvas.drawCircle(cx, cy, cellSize * 0.14f, tilePaint);
        } else if (specialType == SpecialType.COLOR) {
            tilePaint.setStyle(Paint.Style.STROKE);
            tilePaint.setStrokeWidth(cellSize * 0.08f);
            canvas.drawCircle(cx, cy, cellSize * 0.18f, tilePaint);
            tilePaint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawObstacle(Canvas canvas, RectF rect, Cell cell) {
        if (cell.getObstacleType() == ObstacleType.NONE) {
            return;
        }
        if (cell.getObstacleType() == ObstacleType.ICE) {
            tilePaint.setColor(colorIce);
            tilePaint.setAlpha(120);
            canvas.drawRoundRect(rect, cellSize * 0.24f, cellSize * 0.24f, tilePaint);
            tilePaint.setAlpha(255);
        } else if (cell.getObstacleType() == ObstacleType.CRATE) {
            tilePaint.setColor(colorCrate);
            canvas.drawRoundRect(rect, cellSize * 0.18f, cellSize * 0.18f, tilePaint);
            tilePaint.setColor(colorText);
            canvas.drawRect(rect.left + cellSize * 0.14f, rect.centerY() - cellSize * 0.04f, rect.right - cellSize * 0.14f, rect.centerY() + cellSize * 0.04f, tilePaint);
            canvas.drawRect(rect.centerX() - cellSize * 0.04f, rect.top + cellSize * 0.14f, rect.centerX() + cellSize * 0.04f, rect.bottom - cellSize * 0.14f, tilePaint);
        } else if (cell.getObstacleType() == ObstacleType.CHAIN) {
            tilePaint.setColor(colorChain);
            tilePaint.setStyle(Paint.Style.STROKE);
            tilePaint.setStrokeWidth(cellSize * 0.08f);
            canvas.drawRoundRect(rect, cellSize * 0.24f, cellSize * 0.24f, tilePaint);
            tilePaint.setStyle(Paint.Style.FILL);
        } else if (cell.getObstacleType() == ObstacleType.STONE) {
            tilePaint.setColor(colorStone);
            canvas.drawRoundRect(rect, cellSize * 0.16f, cellSize * 0.16f, tilePaint);
        }
    }

    private void drawOverlayCue(Canvas canvas) {
        if (gameSession.getStateMachine().getState() == GameState.PLAYING && gameSession.getStateMachine().getBoardPhase() != BoardPhase.IDLE) {
            effectPaint.setColor(colorAccent2);
            canvas.drawText(gameSession.getStateMachine().getBoardPhase().name(), getWidth() * 0.5f, boardTop - dp(18f), effectPaint);
        }
    }

    private void drawEffects(Canvas canvas) {
        for (VisualEffect effect : gameSession.getEffectManager().getEffects()) {
            effectPaint.setColor(effect.color);
            effectPaint.setAlpha((int) (Math.max(0f, Math.min(1f, effect.life / 0.35f)) * 255));
            float cx = getWidth() * effect.x;
            float cy = getHeight() * effect.y;
            canvas.drawCircle(cx, cy, Math.min(getWidth(), getHeight()) * 0.18f * effect.radius, effectPaint);
            canvas.drawText(effect.label, cx, cy + dp(6f), effectPaint);
        }
        effectPaint.setAlpha(255);
    }

    private int resolveTileColor(TileColor tileColor) {
        switch (tileColor) {
            case RED:
                return colorRed;
            case BLUE:
                return colorBlue;
            case GREEN:
                return colorGreen;
            case YELLOW:
                return colorYellow;
            case PURPLE:
                return colorPurple;
            case ORANGE:
            default:
                return colorOrange;
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        BoardManager boardManager = gameSession.getBoardManager();
        if (boardManager.getRows() == 0 || boardManager.getCols() == 0) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (x < boardLeft || y < boardTop || x > boardLeft + boardWidth || y > boardTop + boardHeight) {
            return true;
        }
        int col = Math.min(boardManager.getCols() - 1, (int) ((x - boardLeft) / cellSize));
        int row = Math.min(boardManager.getRows() - 1, (int) ((y - boardTop) / cellSize));
        gameSession.onTileTapped(row, col);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        onResume();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        onPause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
}
