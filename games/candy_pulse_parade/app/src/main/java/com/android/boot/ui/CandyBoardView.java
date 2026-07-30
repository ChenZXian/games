package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.CandyStage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CandyBoardView extends View {
    public interface Listener {
        void onHudUpdated(CandyStage stage, int score, int movesLeft, int combo, String objectiveLabel);
        void onPlayEffect(String effectKey);
        void onStageResolved(boolean cleared, String message);
    }

    private static final int COLOR_RED = 0;
    private static final int COLOR_YELLOW = 1;
    private static final int COLOR_BLUE = 2;
    private static final int COLOR_GREEN = 3;
    private static final int COLOR_PURPLE = 4;
    private static final int SPECIAL_NONE = 0;
    private static final int SPECIAL_STRIPED_H = 1;
    private static final int SPECIAL_STRIPED_V = 2;
    private static final int SPECIAL_WRAPPED = 3;
    private static final int SPECIAL_RAINBOW = 4;
    private static final int STATE_IDLE = 0;
    private static final int STATE_SWAPPING = 1;
    private static final int STATE_MATCH = 2;
    private static final int STATE_COLLAPSE = 3;
    private static final int STATE_FINISHED = 4;
    private static final String RUNTIME_ART_MAP_ASSET = "game_art/runtime_art_map.json";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF boardRect = new RectF();
    private final Path candyPath = new Path();
    private final Random random = new Random();
    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.nanoTime();
            float dt = lastFrameNs == 0L ? 0.016f : (now - lastFrameNs) / 1000000000f;
            lastFrameNs = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            update(dt);
            invalidate();
            if (running) {
                postOnAnimation(this);
            }
        }
    };
    private final List<PopEffect> popEffects = new ArrayList<>();
    private final List<WaveEffect> waveEffects = new ArrayList<>();
    private final List<ShardEffect> shardEffects = new ArrayList<>();
    private final List<HintMarker> hintMarkers = new ArrayList<>();
    private Listener listener;
    private CandyStage currentStage;
    private Tile[][] board;
    private boolean running;
    private long lastFrameNs;
    private int boardState = STATE_IDLE;
    private int score;
    private int movesLeft;
    private int comboChain;
    private int bestCombo;
    private int jellyRemaining;
    private int frostingRemaining;
    private int orderRemaining;
    private int ingredientsDelivered;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int downRow = -1;
    private int downCol = -1;
    private float downX;
    private float downY;
    private float cellSize;
    private float boardLeft;
    private float boardTop;
    private float resolveTimer;
    private boolean paused;
    private boolean inputLocked;
    private SwapAnimation swapAnimation;
    private ComboIntent pendingComboIntent;
    private int lastSwapRowA;
    private int lastSwapColA;
    private int lastSwapRowB;
    private int lastSwapColB;

    public CandyBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void loadStage(CandyStage stage) {
        currentStage = stage;
        score = 0;
        comboChain = 0;
        bestCombo = 0;
        ingredientsDelivered = 0;
        movesLeft = stage.moves;
        selectedRow = -1;
        selectedCol = -1;
        hintMarkers.clear();
        buildBoard(stage);
        boardState = STATE_IDLE;
        paused = false;
        inputLocked = false;
        swapAnimation = null;
        pendingComboIntent = null;
        resolveTimer = 0f;
        notifyHud();
        invalidate();
    }

    public void onHostResume() {
        if (running) {
            return;
        }
        running = true;
        lastFrameNs = 0L;
        removeCallbacks(frameRunnable);
        postOnAnimation(frameRunnable);
    }

    public void onHostPause() {
        running = false;
        removeCallbacks(frameRunnable);
        lastFrameNs = 0L;
    }

    public void pauseBoard() {
        paused = true;
    }

    public void resumeBoard() {
        paused = false;
    }

    public String getObjectiveDescription(CandyStage stage) {
        if (stage == null) {
            return "";
        }
        if (currentStage == stage && board != null) {
            return getObjectiveDescription();
        }
        switch (stage.objectiveType) {
            case JELLY:
                return "Clear " + stage.targetCount + " jelly cells";
            case SCORE:
                return "Reach " + stage.targetCount + " score";
            case FROSTING:
                return "Break " + stage.targetCount + " frosting blocks";
            case ORDER:
                return "Collect " + stage.targetCount + " pink candies";
            case INGREDIENT:
                return "Drop " + stage.targetCount + " cherry candies";
            default:
                return "";
        }
    }

    public void triggerBooster() {
        if (currentStage == null || boardState != STATE_IDLE || movesLeft <= 0) {
            return;
        }
        if (selectedRow < 0 || selectedCol < 0) {
            showHint();
            if (hintMarkers.isEmpty()) {
                return;
            }
            selectedRow = hintMarkers.get(0).row;
            selectedCol = hintMarkers.get(0).col;
        }
        clearArea(selectedRow, selectedCol, 1, true);
        listener.onPlayEffect("warning");
        movesLeft = Math.max(0, movesLeft - 1);
        comboChain = 0;
        resolveTimer = 0.12f;
        boardState = STATE_COLLAPSE;
        selectedRow = -1;
        selectedCol = -1;
        notifyHud();
    }

    public void showHint() {
        hintMarkers.clear();
        if (board == null || boardState != STATE_IDLE) {
            return;
        }
        for (int row = 0; row < currentStage.height; row++) {
            for (int col = 0; col < currentStage.width; col++) {
                if (tryHintSwap(row, col, row, col + 1) || tryHintSwap(row, col, row + 1, col)) {
                    invalidate();
                    return;
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (currentStage != null) {
            onHostResume();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        onHostPause();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_board_bg));
        if (currentStage == null || board == null) {
            drawEmptyBoard(canvas);
            return;
        }
        layoutBoard();
        drawBoardBackdrop(canvas);
        drawBoardCells(canvas);
        drawEffects(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentStage == null || paused || boardState == STATE_FINISHED || boardState == STATE_MATCH || boardState == STATE_COLLAPSE) {
            return true;
        }
        if (boardRect.isEmpty()) {
            layoutBoard();
        }
        int row = cellRow(event.getY());
        int col = cellCol(event.getX());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRow = row;
                downCol = col;
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (!isInsideCell(row, col) || !isInsideCell(downRow, downCol)) {
                    return true;
                }
                int dRow = Math.abs(row - downRow);
                int dCol = Math.abs(col - downCol);
                if (dRow + dCol == 1) {
                    attemptSwap(downRow, downCol, row, col);
                } else {
                    handleTap(row, col);
                }
                return true;
            default:
                return true;
        }
    }

    private void handleTap(int row, int col) {
        if (!isMovable(row, col)) {
            return;
        }
        if (selectedRow < 0) {
            selectedRow = row;
            selectedCol = col;
            listener.onPlayEffect("click");
            invalidate();
            return;
        }
        if (selectedRow == row && selectedCol == col) {
            selectedRow = -1;
            selectedCol = -1;
            invalidate();
            return;
        }
        if (Math.abs(selectedRow - row) + Math.abs(selectedCol - col) == 1) {
            attemptSwap(selectedRow, selectedCol, row, col);
        } else {
            selectedRow = row;
            selectedCol = col;
            listener.onPlayEffect("click");
            invalidate();
        }
    }

    private void attemptSwap(int rowA, int colA, int rowB, int colB) {
        if (boardState != STATE_IDLE || !isMovable(rowA, colA) || !isMovable(rowB, colB)) {
            return;
        }
        Tile tileA = board[rowA][colA];
        Tile tileB = board[rowB][colB];
        pendingComboIntent = detectSpecialSwap(tileA, tileB, rowA, colA, rowB, colB);
        swapTiles(rowA, colA, rowB, colB);
        boolean valid = pendingComboIntent != null || !findMatches().isEmpty();
        if (!valid) {
            swapTiles(rowA, colA, rowB, colB);
        }
        swapAnimation = new SwapAnimation(rowA, colA, rowB, colB, valid);
        boardState = STATE_SWAPPING;
        resolveTimer = 0f;
        lastSwapRowA = rowA;
        lastSwapColA = colA;
        lastSwapRowB = rowB;
        lastSwapColB = colB;
        selectedRow = -1;
        selectedCol = -1;
        hintMarkers.clear();
        listener.onPlayEffect("click");
    }

    private void update(float dt) {
        if (paused || currentStage == null) {
            return;
        }
        updateEffects(dt);
        if (boardState == STATE_SWAPPING) {
            updateSwap(dt);
        } else if (boardState == STATE_MATCH || boardState == STATE_COLLAPSE) {
            resolveTimer -= dt;
            if (resolveTimer <= 0f) {
                if (boardState == STATE_MATCH) {
                    processMatchPhase();
                } else {
                    collapseBoard();
                    boardState = STATE_MATCH;
                    resolveTimer = 0.18f;
                }
            }
        }
    }

    private void updateSwap(float dt) {
        if (swapAnimation == null) {
            boardState = STATE_IDLE;
            return;
        }
        swapAnimation.progress += dt * 5.4f;
        if (swapAnimation.progress >= 1f) {
            if (!swapAnimation.valid) {
                swapAnimation = null;
                boardState = STATE_IDLE;
                pendingComboIntent = null;
                return;
            }
            swapAnimation = null;
            movesLeft = Math.max(0, movesLeft - 1);
            comboChain = 0;
            boardState = STATE_MATCH;
            resolveTimer = 0.06f;
            notifyHud();
        }
    }

    private void processMatchPhase() {
        MatchResult result = findMatches();
        if (pendingComboIntent != null) {
            applyComboIntent(pendingComboIntent, result);
            pendingComboIntent = null;
        }
        if (result.cells.isEmpty()) {
            endTurnIfNeeded();
            return;
        }
        comboChain++;
        if (comboChain > bestCombo) {
            bestCombo = comboChain;
        }
        Set<Long> spared = new HashSet<>();
        for (SpecialSpawn spawn : result.specialSpawns) {
            Tile anchor = board[spawn.row][spawn.col];
            if (anchor == null) {
                anchor = new Tile(random.nextInt(5));
                board[spawn.row][spawn.col] = anchor;
            }
            anchor.color = spawn.color;
            anchor.special = spawn.special;
            spared.add(key(spawn.row, spawn.col));
            clearJelly(spawn.row, spawn.col);
        }
        int clearedThisStep = 0;
        for (Cell cell : result.cells) {
            long key = key(cell.row, cell.col);
            Tile tile = board[cell.row][cell.col];
            if (tile != null && spared.contains(key)) {
                if (tile.special != SPECIAL_NONE) {
                    spawnWave(cell.row, cell.col, 0.28f, tile.color);
                }
                continue;
            }
            if (tile != null) {
                if (currentStage.objectiveType == CandyStage.ObjectiveType.ORDER && tile.color == currentStage.orderColor) {
                    orderRemaining = Math.max(0, orderRemaining - 1);
                }
                if (tile.ingredient) {
                    score += 240;
                }
                clearedThisStep++;
                score += tile.special == SPECIAL_NONE ? 110 : 180;
                spawnPop(cell.row, cell.col, tile.color, tile.special);
                clearJelly(cell.row, cell.col);
                board[cell.row][cell.col] = null;
            } else if (currentStage.frostingMap[cell.row][cell.col] > 0) {
                damageBlocker(cell.row, cell.col, true);
            }
        }
        damageAdjacentBlockers(result.cells);
        if (clearedThisStep > 0) {
            listener.onPlayEffect("collect");
        }
        if (comboChain >= 2) {
            spawnComboRibbon();
        }
        boardState = STATE_COLLAPSE;
        resolveTimer = 0.22f;
        notifyHud();
    }

    private void endTurnIfNeeded() {
        if (objectiveCleared()) {
            boardState = STATE_FINISHED;
            listener.onStageResolved(true, getClearMessage());
            return;
        }
        if (movesLeft <= 0) {
            boardState = STATE_FINISHED;
            listener.onStageResolved(false, getFailMessage());
            return;
        }
        boardState = STATE_IDLE;
        notifyHud();
    }

    private void collapseBoard() {
        for (int col = 0; col < currentStage.width; col++) {
            int writeRow = currentStage.height - 1;
            for (int row = currentStage.height - 1; row >= 0; row--) {
                if (currentStage.frostingMap[row][col] > 0) {
                    writeRow = row - 1;
                    continue;
                }
                Tile tile = board[row][col];
                if (tile == null) {
                    continue;
                }
                if (writeRow != row) {
                    board[writeRow][col] = tile;
                    board[row][col] = null;
                    tile.fallCells = row - writeRow;
                }
                writeRow--;
            }
            while (writeRow >= 0) {
                if (currentStage.frostingMap[writeRow][col] > 0) {
                    writeRow--;
                    continue;
                }
                Tile tile = spawnTileForColumn(col);
                tile.fallCells = writeRow + 1;
                board[writeRow][col] = tile;
                writeRow--;
            }
        }
        collectIngredientsAtExit();
    }

    private void collectIngredientsAtExit() {
        if (currentStage.objectiveType != CandyStage.ObjectiveType.INGREDIENT) {
            return;
        }
        int bottom = currentStage.height - 1;
        for (int col = 0; col < currentStage.width; col++) {
            Tile tile = board[bottom][col];
            if (tile != null && tile.ingredient) {
                ingredientsDelivered++;
                score += 320;
                spawnWave(bottom, col, 0.36f, tile.color);
                spawnPop(bottom, col, tile.color, tile.special);
                board[bottom][col] = null;
                listener.onPlayEffect("collect");
            }
        }
    }

    private void buildBoard(CandyStage stage) {
        board = new Tile[stage.height][stage.width];
        jellyRemaining = 0;
        frostingRemaining = 0;
        for (int row = 0; row < stage.height; row++) {
            for (int col = 0; col < stage.width; col++) {
                if (stage.jellyMap[row][col]) {
                    jellyRemaining++;
                }
                if (stage.frostingMap[row][col] > 0) {
                    frostingRemaining++;
                }
            }
        }
        orderRemaining = stage.objectiveType == CandyStage.ObjectiveType.ORDER ? stage.targetCount : 0;
        for (int row = 0; row < stage.height; row++) {
            for (int col = 0; col < stage.width; col++) {
                if (stage.frostingMap[row][col] > 0) {
                    continue;
                }
                Tile tile;
                do {
                    tile = new Tile(random.nextInt(5));
                } while (wouldCreateStartMatch(row, col, tile.color));
                board[row][col] = tile;
            }
        }
        if (stage.objectiveType == CandyStage.ObjectiveType.INGREDIENT) {
            for (int i = 0; i < stage.targetCount && i < stage.ingredientColumns.length; i++) {
                int col = stage.ingredientColumns[i];
                board[0][col] = new Tile(random.nextInt(5));
                board[0][col].ingredient = true;
            }
        }
    }

    private boolean wouldCreateStartMatch(int row, int col, int color) {
        if (col >= 2 && board[row][col - 1] != null && board[row][col - 2] != null) {
            if (board[row][col - 1].color == color && board[row][col - 2].color == color) {
                return true;
            }
        }
        if (row >= 2 && board[row - 1][col] != null && board[row - 2][col] != null) {
            return board[row - 1][col].color == color && board[row - 2][col].color == color;
        }
        return false;
    }

    private MatchResult findMatches() {
        MatchResult result = new MatchResult();
        if (board == null) {
            return result;
        }
        boolean[][] matched = new boolean[currentStage.height][currentStage.width];
        List<List<Cell>> groups = new ArrayList<>();
        for (int row = 0; row < currentStage.height; row++) {
            int col = 0;
            while (col < currentStage.width) {
                Tile tile = board[row][col];
                if (tile == null || tile.special == SPECIAL_RAINBOW) {
                    col++;
                    continue;
                }
                int end = col + 1;
                while (end < currentStage.width && board[row][end] != null && board[row][end].special != SPECIAL_RAINBOW && board[row][end].color == tile.color) {
                    end++;
                }
                if (end - col >= 3) {
                    List<Cell> group = new ArrayList<>();
                    for (int x = col; x < end; x++) {
                        matched[row][x] = true;
                        group.add(new Cell(row, x));
                    }
                    groups.add(group);
                }
                col = end;
            }
        }
        for (int col = 0; col < currentStage.width; col++) {
            int row = 0;
            while (row < currentStage.height) {
                Tile tile = board[row][col];
                if (tile == null || tile.special == SPECIAL_RAINBOW) {
                    row++;
                    continue;
                }
                int end = row + 1;
                while (end < currentStage.height && board[end][col] != null && board[end][col].special != SPECIAL_RAINBOW && board[end][col].color == tile.color) {
                    end++;
                }
                if (end - row >= 3) {
                    List<Cell> group = new ArrayList<>();
                    for (int y = row; y < end; y++) {
                        matched[y][col] = true;
                        group.add(new Cell(y, col));
                    }
                    groups.add(group);
                }
                row = end;
            }
        }
        for (int row = 0; row < currentStage.height; row++) {
            for (int col = 0; col < currentStage.width; col++) {
                if (matched[row][col]) {
                    result.cells.add(new Cell(row, col));
                    Tile tile = board[row][col];
                    if (tile != null && tile.special != SPECIAL_NONE) {
                        expandSpecialClear(row, col, tile.special, result.cells, tile.color);
                    }
                }
            }
        }
        List<SpecialSpawn> spawns = findSpecialSpawns(groups);
        result.specialSpawns.addAll(spawns);
        return result;
    }

    private List<SpecialSpawn> findSpecialSpawns(List<List<Cell>> groups) {
        List<SpecialSpawn> spawns = new ArrayList<>();
        boolean[][] horizontal = new boolean[currentStage.height][currentStage.width];
        boolean[][] vertical = new boolean[currentStage.height][currentStage.width];
        for (List<Cell> group : groups) {
            if (group.size() < 4) {
                continue;
            }
            boolean sameRow = true;
            boolean sameCol = true;
            int color = board[group.get(0).row][group.get(0).col].color;
            for (Cell cell : group) {
                sameRow &= cell.row == group.get(0).row;
                sameCol &= cell.col == group.get(0).col;
            }
            if (sameRow && group.size() >= 5) {
                Cell anchor = chooseAnchor(group);
                spawns.add(new SpecialSpawn(anchor.row, anchor.col, SPECIAL_RAINBOW, color));
                horizontal[anchor.row][anchor.col] = true;
            } else if (sameCol && group.size() >= 5) {
                Cell anchor = chooseAnchor(group);
                spawns.add(new SpecialSpawn(anchor.row, anchor.col, SPECIAL_RAINBOW, color));
                vertical[anchor.row][anchor.col] = true;
            } else if (sameRow) {
                Cell anchor = chooseAnchor(group);
                spawns.add(new SpecialSpawn(anchor.row, anchor.col, random.nextBoolean() ? SPECIAL_STRIPED_H : SPECIAL_STRIPED_V, color));
                horizontal[anchor.row][anchor.col] = true;
            } else if (sameCol) {
                Cell anchor = chooseAnchor(group);
                spawns.add(new SpecialSpawn(anchor.row, anchor.col, random.nextBoolean() ? SPECIAL_STRIPED_H : SPECIAL_STRIPED_V, color));
                vertical[anchor.row][anchor.col] = true;
            }
        }
        for (int row = 0; row < currentStage.height; row++) {
            for (int col = 0; col < currentStage.width; col++) {
                if (horizontal[row][col] && vertical[row][col]) {
                    spawns.add(new SpecialSpawn(row, col, SPECIAL_WRAPPED, board[row][col].color));
                }
            }
        }
        return dedupeSpawns(spawns);
    }

    private List<SpecialSpawn> dedupeSpawns(List<SpecialSpawn> spawns) {
        List<SpecialSpawn> deduped = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (SpecialSpawn spawn : spawns) {
            long key = key(spawn.row, spawn.col);
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            deduped.add(spawn);
        }
        return deduped;
    }

    private Cell chooseAnchor(List<Cell> group) {
        for (Cell cell : group) {
            if (cell.row == lastSwapRowA && cell.col == lastSwapColA) {
                return cell;
            }
            if (cell.row == lastSwapRowB && cell.col == lastSwapColB) {
                return cell;
            }
        }
        return group.get(group.size() / 2);
    }

    private void applyComboIntent(ComboIntent comboIntent, MatchResult result) {
        switch (comboIntent.type) {
            case "rainbow_color":
                for (int row = 0; row < currentStage.height; row++) {
                    for (int col = 0; col < currentStage.width; col++) {
                        Tile tile = board[row][col];
                        if (tile != null && tile.color == comboIntent.color) {
                            result.cells.add(new Cell(row, col));
                        }
                    }
                }
                break;
            case "double_stripe":
                clearRow(comboIntent.rowA, result.cells);
                clearCol(comboIntent.colB, result.cells);
                clearCol(comboIntent.colA, result.cells);
                clearRow(comboIntent.rowB, result.cells);
                break;
            case "wrapped_stripe":
                for (int offset = -1; offset <= 1; offset++) {
                    clearRow(comboIntent.rowA + offset, result.cells);
                    clearCol(comboIntent.colA + offset, result.cells);
                }
                break;
            case "double_wrapped":
                clearArea(comboIntent.rowA, comboIntent.colA, 2, false, result.cells);
                break;
            default:
                break;
        }
    }

    private ComboIntent detectSpecialSwap(Tile tileA, Tile tileB, int rowA, int colA, int rowB, int colB) {
        if (tileA == null || tileB == null) {
            return null;
        }
        if (tileA.special == SPECIAL_RAINBOW && tileB.special == SPECIAL_RAINBOW) {
            ComboIntent intent = new ComboIntent("double_wrapped");
            intent.rowA = rowA;
            intent.colA = colA;
            return intent;
        }
        if (tileA.special == SPECIAL_RAINBOW) {
            ComboIntent intent = new ComboIntent("rainbow_color");
            intent.color = tileB.color;
            return intent;
        }
        if (tileB.special == SPECIAL_RAINBOW) {
            ComboIntent intent = new ComboIntent("rainbow_color");
            intent.color = tileA.color;
            return intent;
        }
        boolean stripeA = tileA.special == SPECIAL_STRIPED_H || tileA.special == SPECIAL_STRIPED_V;
        boolean stripeB = tileB.special == SPECIAL_STRIPED_H || tileB.special == SPECIAL_STRIPED_V;
        if (stripeA && stripeB) {
            ComboIntent intent = new ComboIntent("double_stripe");
            intent.rowA = rowA;
            intent.colA = colA;
            intent.rowB = rowB;
            intent.colB = colB;
            return intent;
        }
        if ((tileA.special == SPECIAL_WRAPPED && stripeB) || (tileB.special == SPECIAL_WRAPPED && stripeA)) {
            ComboIntent intent = new ComboIntent("wrapped_stripe");
            intent.rowA = rowB;
            intent.colA = colB;
            if (tileA.special == SPECIAL_WRAPPED) {
                intent.rowA = rowA;
                intent.colA = colA;
            }
            return intent;
        }
        if (tileA.special == SPECIAL_WRAPPED && tileB.special == SPECIAL_WRAPPED) {
            ComboIntent intent = new ComboIntent("double_wrapped");
            intent.rowA = rowA;
            intent.colA = colA;
            return intent;
        }
        return null;
    }

    private void clearRow(int row, List<Cell> target) {
        if (row < 0 || row >= currentStage.height) {
            return;
        }
        for (int col = 0; col < currentStage.width; col++) {
            target.add(new Cell(row, col));
        }
    }

    private void clearCol(int col, List<Cell> target) {
        if (col < 0 || col >= currentStage.width) {
            return;
        }
        for (int row = 0; row < currentStage.height; row++) {
            target.add(new Cell(row, col));
        }
    }

    private void clearArea(int centerRow, int centerCol, int radius, boolean useState) {
        List<Cell> cells = new ArrayList<>();
        clearArea(centerRow, centerCol, radius, true, cells);
        for (Cell cell : cells) {
            if (board[cell.row][cell.col] != null) {
                clearJelly(cell.row, cell.col);
                spawnPop(cell.row, cell.col, board[cell.row][cell.col].color, board[cell.row][cell.col].special);
                board[cell.row][cell.col] = null;
                score += 120;
            } else if (currentStage.frostingMap[cell.row][cell.col] > 0) {
                damageBlocker(cell.row, cell.col, true);
            }
        }
        if (useState) {
            boardState = STATE_COLLAPSE;
        }
    }

    private void clearArea(int centerRow, int centerCol, int radius, boolean includeCenter, List<Cell> target) {
        for (int row = centerRow - radius; row <= centerRow + radius; row++) {
            for (int col = centerCol - radius; col <= centerCol + radius; col++) {
                if (!isInsideCell(row, col)) {
                    continue;
                }
                if (!includeCenter && row == centerRow && col == centerCol) {
                    continue;
                }
                target.add(new Cell(row, col));
            }
        }
    }

    private void expandSpecialClear(int row, int col, int special, List<Cell> target, int color) {
        if (special == SPECIAL_STRIPED_H) {
            clearRow(row, target);
            spawnWave(row, col, 0.28f, color);
        } else if (special == SPECIAL_STRIPED_V) {
            clearCol(col, target);
            spawnWave(row, col, 0.28f, color);
        } else if (special == SPECIAL_WRAPPED) {
            clearArea(row, col, 1, true, target);
            spawnWave(row, col, 0.34f, color);
        } else if (special == SPECIAL_RAINBOW) {
            int chosenColor = color;
            for (int scan = 0; scan < currentStage.width; scan++) {
                Tile tile = board[row][scan];
                if (tile != null && tile.special == SPECIAL_NONE) {
                    chosenColor = tile.color;
                    break;
                }
            }
            for (int r = 0; r < currentStage.height; r++) {
                for (int c = 0; c < currentStage.width; c++) {
                    Tile tile = board[r][c];
                    if (tile != null && tile.color == chosenColor) {
                        target.add(new Cell(r, c));
                    }
                }
            }
            spawnWave(row, col, 0.42f, color);
        }
    }

    private void damageAdjacentBlockers(List<Cell> cleared) {
        for (Cell cell : cleared) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int row = cell.row + dy;
                    int col = cell.col + dx;
                    if (isInsideCell(row, col) && currentStage.frostingMap[row][col] > 0) {
                        damageBlocker(row, col, false);
                    }
                }
            }
        }
    }

    private void damageBlocker(int row, int col, boolean strong) {
        if (currentStage.frostingMap[row][col] <= 0) {
            return;
        }
        currentStage.frostingMap[row][col] = Math.max(0, currentStage.frostingMap[row][col] - (strong ? 2 : 1));
        spawnWave(row, col, strong ? 0.32f : 0.18f, COLOR_YELLOW);
        if (currentStage.frostingMap[row][col] == 0) {
            frostingRemaining = Math.max(0, frostingRemaining - 1);
            score += 160;
            board[row][col] = spawnTileForColumn(col);
            board[row][col].fallCells = 1.2f;
        }
    }

    private void clearJelly(int row, int col) {
        if (currentStage.jellyMap[row][col]) {
            currentStage.jellyMap[row][col] = false;
            jellyRemaining = Math.max(0, jellyRemaining - 1);
        }
    }

    private Tile spawnTileForColumn(int col) {
        Tile tile = new Tile(random.nextInt(5));
        if (currentStage.objectiveType == CandyStage.ObjectiveType.INGREDIENT) {
            for (int sourceCol : currentStage.ingredientColumns) {
                if (sourceCol == col && ingredientsDelivered + countIngredientsOnBoard() < currentStage.targetCount && random.nextFloat() < 0.12f) {
                    tile.ingredient = true;
                    break;
                }
            }
        }
        return tile;
    }

    private int countIngredientsOnBoard() {
        int count = 0;
        for (int row = 0; row < currentStage.height; row++) {
            for (int col = 0; col < currentStage.width; col++) {
                if (board[row][col] != null && board[row][col].ingredient) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean objectiveCleared() {
        switch (currentStage.objectiveType) {
            case JELLY:
                return jellyRemaining <= 0;
            case SCORE:
                return score >= currentStage.targetCount;
            case FROSTING:
                return frostingRemaining <= 0;
            case ORDER:
                return orderRemaining <= 0;
            case INGREDIENT:
                return ingredientsDelivered >= currentStage.targetCount;
            default:
                return false;
        }
    }

    private String getObjectiveDescription() {
        switch (currentStage.objectiveType) {
            case JELLY:
                return "Clear " + jellyRemaining + " jelly cells";
            case SCORE:
                return "Reach " + currentStage.targetCount + " score";
            case FROSTING:
                return "Break " + frostingRemaining + " frosting blocks";
            case ORDER:
                return "Collect " + orderRemaining + " pink candies";
            case INGREDIENT:
                return "Drop " + (currentStage.targetCount - ingredientsDelivered) + " cherry candies";
            default:
                return "";
        }
    }

    private String getClearMessage() {
        String bonus = bestCombo >= 5 ? " Candy fireworks everywhere." : " Sweet and smooth.";
        return currentStage.stageName + " cleared with " + bestCombo + " chain combo." + bonus;
    }

    private String getFailMessage() {
        return "One more cascade was needed. Re-route a bigger combo and try again.";
    }

    private void notifyHud() {
        if (listener != null && currentStage != null) {
            listener.onHudUpdated(currentStage, score, movesLeft, bestCombo, getObjectiveDescription());
        }
    }

    private void layoutBoard() {
        float available = Math.min(getWidth(), getHeight());
        float horizontalMargin = getWidth() * 0.05f;
        float verticalMargin = getHeight() * 0.04f;
        float maxBoardWidth = getWidth() - horizontalMargin * 2f;
        float maxBoardHeight = getHeight() - verticalMargin * 2f;
        cellSize = Math.min(maxBoardWidth / currentStage.width, maxBoardHeight / currentStage.height);
        float boardWidth = cellSize * currentStage.width;
        float boardHeight = cellSize * currentStage.height;
        boardLeft = (getWidth() - boardWidth) * 0.5f;
        boardTop = (getHeight() - boardHeight) * 0.5f;
        boardRect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
    }

    private void drawEmptyBoard(Canvas canvas) {
        rect.set(getWidth() * 0.12f, getHeight() * 0.12f, getWidth() * 0.88f, getHeight() * 0.88f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        canvas.drawRoundRect(rect, 36f, 36f, paint);
    }

    private void drawBoardBackdrop(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_shadow));
        canvas.drawRoundRect(boardRect.left + 8f, boardRect.top + 12f, boardRect.right + 8f, boardRect.bottom + 14f, cellSize * 0.24f, cellSize * 0.24f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_frame));
        canvas.drawRoundRect(boardRect, cellSize * 0.24f, cellSize * 0.24f, paint);
        rect.set(boardRect.left + cellSize * 0.12f, boardRect.top + cellSize * 0.12f, boardRect.right - cellSize * 0.12f, boardRect.bottom - cellSize * 0.12f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_bg));
        canvas.drawRoundRect(rect, cellSize * 0.18f, cellSize * 0.18f, paint);
    }

    private void drawBoardCells(Canvas canvas) {
        for (int row = 0; row < currentStage.height; row++) {
            for (int col = 0; col < currentStage.width; col++) {
                float left = boardLeft + col * cellSize;
                float top = boardTop + row * cellSize;
                rect.set(left + cellSize * 0.08f, top + cellSize * 0.08f, left + cellSize * 0.92f, top + cellSize * 0.92f);
                paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_slot));
                canvas.drawRoundRect(rect, cellSize * 0.18f, cellSize * 0.18f, paint);
                if (currentStage.jellyMap[row][col]) {
                    paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_jelly_glow));
                    canvas.drawRoundRect(rect.left + 3f, rect.top + 3f, rect.right - 3f, rect.bottom - 3f, cellSize * 0.14f, cellSize * 0.14f, paint);
                }
                if (currentStage.frostingMap[row][col] > 0) {
                    drawFrosting(canvas, row, col, left, top, currentStage.frostingMap[row][col]);
                    continue;
                }
                Tile tile = board[row][col];
                if (tile == null) {
                    continue;
                }
                drawCandy(canvas, row, col, left, top, tile);
            }
        }
    }

    private void drawCandy(Canvas canvas, int row, int col, float left, float top, Tile tile) {
        float offsetX = 0f;
        float offsetY = -tile.fallCells * cellSize * 0.28f;
        if (Math.abs(tile.fallCells) > 0.01f) {
            tile.fallCells *= 0.78f;
            if (Math.abs(tile.fallCells) < 0.02f) {
                tile.fallCells = 0f;
            }
        }
        if (swapAnimation != null && swapAnimation.progress < 1f) {
            float eased = easeInOut(swapAnimation.progress);
            if (row == swapAnimation.rowA && col == swapAnimation.colA) {
                offsetX = (swapAnimation.colB - swapAnimation.colA) * cellSize * eased;
                offsetY += (swapAnimation.rowB - swapAnimation.rowA) * cellSize * eased;
            } else if (row == swapAnimation.rowB && col == swapAnimation.colB) {
                offsetX = (swapAnimation.colA - swapAnimation.colB) * cellSize * eased;
                offsetY += (swapAnimation.rowA - swapAnimation.rowB) * cellSize * eased;
            }
        }
        float centerX = left + cellSize * 0.5f + offsetX;
        float centerY = top + cellSize * 0.5f + offsetY;
        float radius = cellSize * 0.33f;
        int baseColor = resolveCandyColor(tile.color);
        RadialGradient gradient = new RadialGradient(centerX - radius * 0.24f, centerY - radius * 0.3f, radius * 1.1f,
                new int[]{Color.WHITE, baseColor},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setShader(null);
        paint.setColor(Color.argb(56, 255, 255, 255));
        canvas.drawCircle(centerX - radius * 0.28f, centerY - radius * 0.28f, radius * 0.28f, paint);
        paint.setColor(adjust(baseColor, 0.62f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.045f);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        if (tile.special == SPECIAL_STRIPED_H || tile.special == SPECIAL_STRIPED_V) {
            drawStripedOverlay(canvas, centerX, centerY, radius, tile.special == SPECIAL_STRIPED_H);
        } else if (tile.special == SPECIAL_WRAPPED) {
            drawWrappedOverlay(canvas, centerX, centerY, radius);
        } else if (tile.special == SPECIAL_RAINBOW) {
            drawRainbowOverlay(canvas, centerX, centerY, radius);
        }
        if (tile.ingredient) {
            drawIngredientOverlay(canvas, centerX, centerY, radius);
        }
        if (selectedRow == row && selectedCol == col) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.08f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(centerX, centerY, radius + cellSize * 0.06f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawStripedOverlay(Canvas canvas, float centerX, float centerY, float radius, boolean horizontal) {
        paint.setColor(Color.argb(210, 255, 255, 255));
        for (int i = -2; i <= 2; i++) {
            if (horizontal) {
                rect.set(centerX - radius * 0.9f, centerY + i * radius * 0.28f - radius * 0.08f, centerX + radius * 0.9f, centerY + i * radius * 0.28f + radius * 0.08f);
            } else {
                rect.set(centerX + i * radius * 0.28f - radius * 0.08f, centerY - radius * 0.9f, centerX + i * radius * 0.28f + radius * 0.08f, centerY + radius * 0.9f);
            }
            canvas.drawRoundRect(rect, radius * 0.12f, radius * 0.12f, paint);
        }
    }

    private void drawWrappedOverlay(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setColor(Color.argb(220, 255, 246, 186));
        rect.set(centerX - radius * 0.28f, centerY - radius * 0.92f, centerX + radius * 0.28f, centerY + radius * 0.92f);
        canvas.drawRoundRect(rect, radius * 0.18f, radius * 0.18f, paint);
        rect.set(centerX - radius * 0.92f, centerY - radius * 0.28f, centerX + radius * 0.92f, centerY + radius * 0.28f);
        canvas.drawRoundRect(rect, radius * 0.18f, radius * 0.18f, paint);
        paint.setColor(Color.argb(255, 255, 214, 76));
        canvas.drawCircle(centerX, centerY, radius * 0.22f, paint);
    }

    private void drawRainbowOverlay(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.26f);
        int[] colors = new int[]{
                ContextCompat.getColor(getContext(), R.color.cst_candy_red),
                ContextCompat.getColor(getContext(), R.color.cst_candy_yellow),
                ContextCompat.getColor(getContext(), R.color.cst_candy_green),
                ContextCompat.getColor(getContext(), R.color.cst_candy_blue),
                ContextCompat.getColor(getContext(), R.color.cst_candy_purple)
        };
        for (int i = 0; i < colors.length; i++) {
            paint.setColor(colors[i]);
            canvas.drawArc(centerX - radius * 0.8f, centerY - radius * 0.8f, centerX + radius * 0.8f, centerY + radius * 0.8f,
                    i * 72f - 90f, 66f, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_candy_white));
        canvas.drawCircle(centerX, centerY, radius * 0.28f, paint);
    }

    private void drawIngredientOverlay(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setColor(Color.argb(255, 210, 45, 66));
        canvas.drawCircle(centerX, centerY, radius * 0.36f, paint);
        paint.setColor(Color.argb(255, 86, 202, 104));
        candyPath.reset();
        candyPath.moveTo(centerX, centerY - radius * 0.58f);
        candyPath.cubicTo(centerX + radius * 0.42f, centerY - radius * 1.1f, centerX + radius * 0.64f, centerY - radius * 0.32f, centerX + radius * 0.14f, centerY - radius * 0.18f);
        candyPath.close();
        canvas.drawPath(candyPath, paint);
    }

    private void drawFrosting(Canvas canvas, int row, int col, float left, float top, int strength) {
        float centerX = left + cellSize * 0.5f;
        float centerY = top + cellSize * 0.5f;
        float radius = cellSize * 0.32f;
        int alpha = 170 + strength * 20;
        paint.setColor(Color.argb(alpha, 255, 250, 255));
        canvas.drawRoundRect(left + cellSize * 0.16f, top + cellSize * 0.16f, left + cellSize * 0.84f, top + cellSize * 0.84f, cellSize * 0.18f, cellSize * 0.18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.055f);
        paint.setColor(Color.argb(255, 214, 192, 222));
        canvas.drawRoundRect(left + cellSize * 0.16f, top + cellSize * 0.16f, left + cellSize * 0.84f, top + cellSize * 0.84f, cellSize * 0.18f, cellSize * 0.18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, 255, 214, 232));
        for (int i = 0; i < strength; i++) {
            float angle = 35f + i * 56f;
            float crackX = centerX + (float) Math.cos(Math.toRadians(angle)) * radius * 0.52f;
            float crackY = centerY + (float) Math.sin(Math.toRadians(angle)) * radius * 0.52f;
            canvas.drawCircle(crackX, crackY, cellSize * 0.05f, paint);
        }
        if (selectedRow == row && selectedCol == col) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.08f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(centerX, centerY, radius + cellSize * 0.1f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawEffects(Canvas canvas) {
        for (HintMarker marker : hintMarkers) {
            marker.life -= 0.016f;
            if (marker.life <= 0f) {
                continue;
            }
            float cx = boardLeft + (marker.col + 0.5f) * cellSize;
            float cy = boardTop + (marker.row + 0.5f) * cellSize;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.06f);
            paint.setColor(Color.argb((int) (180f * marker.life), 255, 200, 77));
            canvas.drawCircle(cx, cy, cellSize * (0.34f + (1f - marker.life) * 0.08f), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        hintMarkers.removeIf(marker -> marker.life <= 0f);
        for (WaveEffect effect : waveEffects) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.09f);
            paint.setColor(Color.argb((int) (effect.alpha * 255f), Color.red(resolveCandyColor(effect.color)), Color.green(resolveCandyColor(effect.color)), Color.blue(resolveCandyColor(effect.color))));
            canvas.drawCircle(effect.x, effect.y, effect.radius, paint);
        }
        for (PopEffect effect : popEffects) {
            paint.setColor(Color.argb((int) (effect.alpha * 255f), Color.red(resolveCandyColor(effect.color)), Color.green(resolveCandyColor(effect.color)), Color.blue(resolveCandyColor(effect.color))));
            canvas.drawCircle(effect.x, effect.y, effect.radius, paint);
        }
        for (ShardEffect effect : shardEffects) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(effect.width);
            paint.setColor(Color.argb((int) (effect.alpha * 255f), effect.r, effect.g, effect.b));
            canvas.drawLine(effect.x, effect.y, effect.x - effect.vx * 0.045f, effect.y - effect.vy * 0.045f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void updateEffects(float dt) {
        for (int i = popEffects.size() - 1; i >= 0; i--) {
            PopEffect effect = popEffects.get(i);
            effect.life -= dt;
            effect.radius += dt * cellSize * 0.52f;
            effect.alpha = Math.max(0f, effect.life / effect.maxLife);
            if (effect.life <= 0f) {
                popEffects.remove(i);
            }
        }
        for (int i = waveEffects.size() - 1; i >= 0; i--) {
            WaveEffect effect = waveEffects.get(i);
            effect.life -= dt;
            effect.radius += dt * cellSize * 2.6f;
            effect.alpha = Math.max(0f, effect.life / effect.maxLife);
            if (effect.life <= 0f) {
                waveEffects.remove(i);
            }
        }
        for (int i = shardEffects.size() - 1; i >= 0; i--) {
            ShardEffect effect = shardEffects.get(i);
            effect.life -= dt;
            effect.x += effect.vx * dt;
            effect.y += effect.vy * dt;
            effect.vx *= 0.95f;
            effect.vy *= 0.95f;
            effect.alpha = Math.max(0f, effect.life / effect.maxLife);
            if (effect.life <= 0f) {
                shardEffects.remove(i);
            }
        }
    }

    private void spawnPop(int row, int col, int color, int special) {
        float x = boardLeft + (col + 0.5f) * cellSize;
        float y = boardTop + (row + 0.5f) * cellSize;
        popEffects.add(new PopEffect(x, y, cellSize * 0.12f, 0.22f, color));
        int shardCount = special == SPECIAL_NONE ? 6 : 12;
        for (int i = 0; i < shardCount; i++) {
            double angle = Math.toRadians((360d / shardCount) * i + random.nextInt(16));
            float speed = cellSize * (special == SPECIAL_NONE ? 2.1f : 3.2f);
            int baseColor = resolveCandyColor(color);
            shardEffects.add(new ShardEffect(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 0.28f, baseColor));
        }
    }

    private void spawnWave(int row, int col, float life, int color) {
        float x = boardLeft + (col + 0.5f) * cellSize;
        float y = boardTop + (row + 0.5f) * cellSize;
        waveEffects.add(new WaveEffect(x, y, cellSize * 0.18f, life, color));
    }

    private void spawnComboRibbon() {
        waveEffects.add(new WaveEffect(boardRect.centerX(), boardRect.top + cellSize * 0.5f, cellSize * 0.4f, 0.34f, COLOR_YELLOW));
    }

    private boolean tryHintSwap(int rowA, int colA, int rowB, int colB) {
        if (!isInsideCell(rowA, colA) || !isInsideCell(rowB, colB) || !isMovable(rowA, colA) || !isMovable(rowB, colB)) {
            return false;
        }
        swapTiles(rowA, colA, rowB, colB);
        boolean valid = !findMatches().isEmpty() || detectSpecialSwap(board[rowA][colA], board[rowB][colB], rowA, colA, rowB, colB) != null;
        swapTiles(rowA, colA, rowB, colB);
        if (valid) {
            hintMarkers.add(new HintMarker(rowA, colA));
            hintMarkers.add(new HintMarker(rowB, colB));
        }
        return valid;
    }

    private void swapTiles(int rowA, int colA, int rowB, int colB) {
        Tile temp = board[rowA][colA];
        board[rowA][colA] = board[rowB][colB];
        board[rowB][colB] = temp;
    }

    private boolean isMovable(int row, int col) {
        return isInsideCell(row, col) && currentStage.frostingMap[row][col] == 0 && board[row][col] != null;
    }

    private boolean isInsideCell(int row, int col) {
        return currentStage != null && row >= 0 && col >= 0 && row < currentStage.height && col < currentStage.width;
    }

    private int cellRow(float y) {
        if (y < boardTop || y >= boardRect.bottom) {
            return -1;
        }
        return (int) ((y - boardTop) / cellSize);
    }

    private int cellCol(float x) {
        if (x < boardLeft || x >= boardRect.right) {
            return -1;
        }
        return (int) ((x - boardLeft) / cellSize);
    }

    private long key(int row, int col) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }

    private int resolveCandyColor(int index) {
        switch (index) {
            case COLOR_RED:
                return ContextCompat.getColor(getContext(), R.color.cst_candy_red);
            case COLOR_YELLOW:
                return ContextCompat.getColor(getContext(), R.color.cst_candy_yellow);
            case COLOR_BLUE:
                return ContextCompat.getColor(getContext(), R.color.cst_candy_blue);
            case COLOR_GREEN:
                return ContextCompat.getColor(getContext(), R.color.cst_candy_green);
            case COLOR_PURPLE:
            default:
                return ContextCompat.getColor(getContext(), R.color.cst_candy_purple);
        }
    }

    private int adjust(int color, float factor) {
        int r = Math.max(0, Math.min(255, (int) (Color.red(color) * factor)));
        int g = Math.max(0, Math.min(255, (int) (Color.green(color) * factor)));
        int b = Math.max(0, Math.min(255, (int) (Color.blue(color) * factor)));
        return Color.rgb(r, g, b);
    }

    private float easeInOut(float value) {
        return value < 0.5f ? 4f * value * value * value : 1f - (float) Math.pow(-2f * value + 2f, 3f) / 2f;
    }

    private static class Tile {
        int color;
        int special;
        boolean ingredient;
        float fallCells;

        Tile(int color) {
            this.color = color;
        }
    }

    private static class Cell {
        final int row;
        final int col;

        Cell(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private static class SwapAnimation {
        final int rowA;
        final int colA;
        final int rowB;
        final int colB;
        final boolean valid;
        float progress;

        SwapAnimation(int rowA, int colA, int rowB, int colB, boolean valid) {
            this.rowA = rowA;
            this.colA = colA;
            this.rowB = rowB;
            this.colB = colB;
            this.valid = valid;
        }
    }

    private static class PopEffect {
        final float x;
        final float y;
        final float maxLife;
        final int color;
        float radius;
        float life;
        float alpha;

        PopEffect(float x, float y, float radius, float life, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.life = life;
            this.maxLife = life;
            this.alpha = 1f;
            this.color = color;
        }
    }

    private static class WaveEffect {
        final float x;
        final float y;
        final float maxLife;
        final int color;
        float radius;
        float life;
        float alpha;

        WaveEffect(float x, float y, float radius, float life, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.life = life;
            this.maxLife = life;
            this.alpha = 1f;
            this.color = color;
        }
    }

    private static class ShardEffect {
        final float maxLife;
        final int r;
        final int g;
        final int b;
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float alpha;
        float width;

        ShardEffect(float x, float y, float vx, float vy, float life, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.alpha = 1f;
            this.r = Color.red(color);
            this.g = Color.green(color);
            this.b = Color.blue(color);
            this.width = 5f;
        }
    }

    private static class HintMarker {
        final int row;
        final int col;
        float life = 1.2f;

        HintMarker(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private static class MatchResult {
        final List<Cell> cells = new ArrayList<>();
        final List<SpecialSpawn> specialSpawns = new ArrayList<>();

        boolean isEmpty() {
            return cells.isEmpty() && specialSpawns.isEmpty();
        }
    }

    private static class SpecialSpawn {
        final int row;
        final int col;
        final int special;
        final int color;

        SpecialSpawn(int row, int col, int special, int color) {
            this.row = row;
            this.col = col;
            this.special = special;
            this.color = color;
        }
    }

    private static class ComboIntent {
        final String type;
        int color;
        int rowA;
        int colA;
        int rowB;
        int colB;

        ComboIntent(String type) {
            this.type = type;
        }
    }
}
