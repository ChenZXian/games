package com.android.boot.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.Board;
import com.android.boot.core.GameManager;
import com.android.boot.core.GameState;
import com.android.boot.entity.Cell;
import com.android.boot.entity.CellType;
import com.android.boot.entity.Move;
import com.android.boot.entity.Piece;
import com.android.boot.entity.Side;

import java.util.List;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface TouchListener {
        void onBoardCellTapped(int row, int col);
    }

    private final SurfaceHolder holder;
    private Thread loopThread;
    private volatile boolean running;
    private GameManager gameManager;
    private TouchListener touchListener;
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railSleeperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railSidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint campPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint campRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hqPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zoneDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zoneBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint riverTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hiddenBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint movePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float cellSize;
    private float originX;
    private float originY;

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        initPaints();
    }

    public void bind(GameManager manager, TouchListener listener) {
        this.gameManager = manager;
        this.touchListener = listener;
    }

    private void initPaints() {
        boardPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_bg_alt));
        roadPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
        roadPaint.setStrokeWidth(dp(1.6f));
        roadPaint.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(3)}, 0));
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
        linePaint.setStrokeWidth(dp(1));
        railPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent_2));
        railPaint.setStrokeWidth(dp(3.8f));
        railSleeperPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        railSleeperPaint.setStrokeWidth(dp(1.4f));
        railSidePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
        railSidePaint.setStrokeWidth(dp(1.2f));
        nodePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        nodePaint.setStyle(Paint.Style.FILL);
        campPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_header_bg));
        campRingPaint.setStyle(Paint.Style.STROKE);
        campRingPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        campRingPaint.setStrokeWidth(dp(2.2f));
        hqPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        zoneDividerPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        zoneDividerPaint.setStrokeWidth(dp(2f));
        zoneDividerPaint.setPathEffect(new DashPathEffect(new float[]{dp(8), dp(5)}, 0));
        zoneBandPaint.setColor(0x1A4FC3F7);
        zoneBandPaint.setStyle(Paint.Style.FILL);
        riverTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        riverTextPaint.setTextSize(sp(9));
        riverTextPaint.setTextAlign(Paint.Align.CENTER);
        playerPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_success));
        aiPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_danger));
        hiddenBackPaint.setColor(0xFF121212);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        textPaint.setTextSize(sp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        smallTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
        smallTextPaint.setTextSize(sp(10));
        smallTextPaint.setTextAlign(Paint.Align.CENTER);
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setStrokeWidth(dp(3));
        selectPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        movePaint.setStyle(Paint.Style.FILL);
        movePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        railMovePaint.setStyle(Paint.Style.FILL);
        railMovePaint.setColor(0xFF35C9FF);
        setFocusable(true);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        loopThread = new Thread(this);
        loopThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - last) / 1000000000f);
            last = now;
            if (gameManager != null) {
                gameManager.update(dt);
            }
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    drawFrame(canvas);
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            if (gameManager != null) {
                post(gameManager::maybeRunAi);
            }
            try {
                Thread.sleep(16L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        if (gameManager == null) {
            return;
        }
        computeBoardGeometry(canvas.getWidth(), canvas.getHeight());
        canvas.drawRoundRect(originX - dp(8), originY - dp(8), originX + Board.COLS * cellSize + dp(8), originY + Board.ROWS * cellSize + dp(8), dp(16), dp(16), boardPaint);
        drawZoneDecoration(canvas);
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                drawCell(canvas, gameManager.getBoard().get(r, c));
            }
        }
        drawRoutes(canvas);
        drawRouteNodes(canvas);
        drawPieces(canvas);
        drawHighlights(canvas, gameManager.getHighlightedMoves(), gameManager.getSelected());
    }

    private void computeBoardGeometry(int width, int height) {
        float usableW = width - dp(24);
        float usableH = height - dp(180);
        cellSize = Math.min(usableW / Board.COLS, usableH / Board.ROWS);
        originX = (width - Board.COLS * cellSize) * 0.5f;
        originY = dp(70);
    }

    private void drawRoutes(Canvas canvas) {
        Board board = gameManager.getBoard();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.get(r, c);
                if (cell == null) continue;
                float cx = originX + (c + 0.5f) * cellSize;
                float cy = originY + (r + 0.5f) * cellSize;
                drawRouteTo(canvas, board, cell, r + 1, c, cx, cy);
                drawRouteTo(canvas, board, cell, r, c + 1, cx, cy);
            }
        }
    }

    private void drawRouteTo(Canvas canvas, Board board, Cell from, int toRow, int toCol, float fromCx, float fromCy) {
        Cell to = board.get(toRow, toCol);
        if (to == null) {
            return;
        }
        float toCx = originX + (to.col + 0.5f) * cellSize;
        float toCy = originY + (to.row + 0.5f) * cellSize;
        boolean railLink = board.areRailConnected(from.row, from.col, to.row, to.col);
        if (railLink) {
            drawDoubleRail(canvas, fromCx, fromCy, toCx, toCy);
            drawRailSleepers(canvas, fromCx, fromCy, toCx, toCy);
        } else {
            if (from.type == CellType.RAIL && to.type == CellType.RAIL) {
                return;
            }
            canvas.drawLine(fromCx, fromCy, toCx, toCy, roadPaint);
        }
    }

    private void drawDoubleRail(Canvas canvas, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.001f) {
            return;
        }
        float nx = -dy / len;
        float ny = dx / len;
        float offset = dp(1.8f);
        canvas.drawLine(x1 + nx * offset, y1 + ny * offset, x2 + nx * offset, y2 + ny * offset, railSidePaint);
        canvas.drawLine(x1 - nx * offset, y1 - ny * offset, x2 - nx * offset, y2 - ny * offset, railSidePaint);
        canvas.drawLine(x1, y1, x2, y2, railPaint);
    }

    private void drawRailSleepers(Canvas canvas, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < dp(8)) {
            return;
        }
        float nx = -dy / len;
        float ny = dx / len;
        float step = dp(10);
        for (float d = step; d < len; d += step) {
            float px = x1 + dx * (d / len);
            float py = y1 + dy * (d / len);
            float half = dp(2.4f);
            canvas.drawLine(px - nx * half, py - ny * half, px + nx * half, py + ny * half, railSleeperPaint);
        }
    }

    private void drawRouteNodes(Canvas canvas) {
        Board board = gameManager.getBoard();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.get(r, c);
                if (cell == null) continue;
                float cx = originX + (c + 0.5f) * cellSize;
                float cy = originY + (r + 0.5f) * cellSize;
                if (cell.type == CellType.RAIL) {
                    drawRailNode(canvas, board, r, c, cx, cy);
                } else if (cell.type == CellType.ROAD) {
                    canvas.drawCircle(cx, cy, dp(1.2f), nodePaint);
                }
            }
        }
    }

    private void drawRailNode(Canvas canvas, Board board, int row, int col, float cx, float cy) {
        int links = 0;
        if (isRailNeighbor(board, row, col, row - 1, col)) links++;
        if (isRailNeighbor(board, row, col, row + 1, col)) links++;
        if (isRailNeighbor(board, row, col, row, col - 1)) links++;
        if (isRailNeighbor(board, row, col, row, col + 1)) links++;
        if (links >= 4) {
            // Cross junction.
            float r = dp(3f);
            canvas.drawCircle(cx, cy, r, nodePaint);
            canvas.drawLine(cx - r * 1.4f, cy, cx + r * 1.4f, cy, railSidePaint);
            canvas.drawLine(cx, cy - r * 1.4f, cx, cy + r * 1.4f, railSidePaint);
        } else if (links == 3) {
            // T junction.
            float r = dp(2.5f);
            canvas.drawCircle(cx, cy, r, nodePaint);
            canvas.drawLine(cx - r, cy, cx + r, cy, railSidePaint);
            canvas.drawLine(cx, cy - r, cx, cy + r, railSidePaint);
        } else if (links == 2) {
            boolean vertical = isRailNeighbor(board, row, col, row - 1, col)
                    && isRailNeighbor(board, row, col, row + 1, col);
            boolean horizontal = isRailNeighbor(board, row, col, row, col - 1)
                    && isRailNeighbor(board, row, col, row, col + 1);
            if (vertical || horizontal) {
                // Straight node.
                canvas.drawCircle(cx, cy, dp(1.9f), nodePaint);
            } else {
                // Corner node.
                canvas.drawCircle(cx, cy, dp(2.3f), nodePaint);
                canvas.drawCircle(cx, cy, dp(1.1f), railSidePaint);
            }
        } else {
            // Rail endpoint.
            canvas.drawCircle(cx, cy, dp(2.1f), nodePaint);
        }
    }

    private boolean isRailNeighbor(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        return board.areRailConnected(fromRow, fromCol, toRow, toCol);
    }

    private void drawZoneDecoration(Canvas canvas) {
        float boardLeft = originX;
        float boardRight = originX + Board.COLS * cellSize;
        float midY = originY + Board.ROWS * cellSize * 0.5f;
        float riverHalf = cellSize * 0.24f;
        canvas.drawRect(boardLeft, midY - riverHalf, boardRight, midY + riverHalf, zoneBandPaint);
        canvas.drawLine(boardLeft, midY - riverHalf, boardRight, midY - riverHalf, zoneDividerPaint);
        canvas.drawLine(boardLeft, midY + riverHalf, boardRight, midY + riverHalf, zoneDividerPaint);
        canvas.drawText("RIVER LINE", (boardLeft + boardRight) * 0.5f, midY + dp(3), riverTextPaint);
    }

    private void drawCell(Canvas canvas, Cell cell) {
        float left = originX + cell.col * cellSize;
        float top = originY + cell.row * cellSize;
        rect.set(left, top, left + cellSize, top + cellSize);
        Paint fill = boardPaint;
        if (cell.type == CellType.RAIL) {
            fill = null;
        }
        if (cell.type == CellType.CAMP) {
            fill = campPaint;
        }
        if (cell.type == CellType.HEADQUARTERS) {
            fill = hqPaint;
        }
        if (cell.type == CellType.CAMP) {
            canvas.drawCircle(left + cellSize * 0.5f, top + cellSize * 0.5f, cellSize * 0.42f, fill);
            canvas.drawCircle(left + cellSize * 0.5f, top + cellSize * 0.5f, cellSize * 0.42f, linePaint);
            canvas.drawCircle(left + cellSize * 0.5f, top + cellSize * 0.5f, cellSize * 0.32f, campRingPaint);
            canvas.drawText("CAMP", left + cellSize * 0.5f, top + cellSize * 0.9f, smallTextPaint);
        } else {
            if (fill != null) {
                canvas.drawRoundRect(rect, dp(8), dp(8), fill);
            } else {
                Paint clear = new Paint();
                clear.setColor(Color.TRANSPARENT);
                canvas.drawRoundRect(rect, dp(8), dp(8), clear);
            }
            canvas.drawRoundRect(rect, dp(8), dp(8), linePaint);
        }
    }

    private void drawPieces(Canvas canvas) {
        Board board = gameManager.getBoard();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.get(r, c);
                if (cell == null || cell.piece == null || !cell.piece.isAlive()) {
                    continue;
                }
                float left = originX + cell.col * cellSize;
                float top = originY + cell.row * cellSize;
                drawPiece(canvas, cell, cell.piece, left, top);
            }
        }
    }

    private void drawPiece(Canvas canvas, Cell cell, Piece piece, float left, float top) {
        float margin = dp(6);
        rect.set(left + margin, top + margin, left + cellSize - margin, top + cellSize - margin);
        boolean hidden = !piece.isRevealedToPlayer();
        Paint piecePaint = hidden ? hiddenBackPaint : (piece.getSide() == Side.PLAYER ? playerPaint : aiPaint);
        canvas.drawRoundRect(rect, dp(10), dp(10), piecePaint);
        String text;
        if (hidden) {
            text = "●";
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        } else {
            text = piece.getType().getLabel().substring(0, Math.min(3, piece.getType().getLabel().length()));
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        }
        canvas.drawText(text, left + cellSize * 0.5f, top + cellSize * 0.58f, textPaint);
        if (cell.type == CellType.HEADQUARTERS) {
            canvas.drawText("BASE", left + cellSize * 0.5f, top + cellSize * 0.9f, smallTextPaint);
        }
    }

    private void drawHighlights(Canvas canvas, List<Move> moves, Cell selected) {
        if (selected != null) {
            float left = originX + selected.col * cellSize;
            float top = originY + selected.row * cellSize;
            rect.set(left + dp(2), top + dp(2), left + cellSize - dp(2), top + cellSize - dp(2));
            canvas.drawRoundRect(rect, dp(10), dp(10), selectPaint);
        }
        for (Move move : moves) {
            float cx = originX + (move.toCol + 0.5f) * cellSize;
            float cy = originY + (move.toRow + 0.5f) * cellSize;
            Paint hintPaint = isRailHint(move, selected) ? railMovePaint : movePaint;
            canvas.drawCircle(cx, cy, dp(8), hintPaint);
        }
    }

    private boolean isRailHint(Move move, Cell selected) {
        if (selected == null || gameManager == null) {
            return false;
        }
        Board board = gameManager.getBoard();
        Cell from = board.get(move.fromRow, move.fromCol);
        Cell to = board.get(move.toRow, move.toCol);
        if (from == null || to == null || from.type != CellType.RAIL || to.type != CellType.RAIL) {
            return false;
        }
        // Any rail-to-rail move uses railway mobility hints.
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && touchListener != null && gameManager != null) {
            int col = (int) ((event.getX() - originX) / cellSize);
            int row = (int) ((event.getY() - originY) / cellSize);
            if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS) {
                touchListener.onBoardCellTapped(row, col);
                return true;
            }
        }
        return true;
    }

    public void onHostPause() {
        if (gameManager != null && gameManager.getState() != GameState.MENU && gameManager.getState() != GameState.GAME_OVER) {
            gameManager.pause();
        }
    }
}
