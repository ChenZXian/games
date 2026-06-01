package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
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
import com.android.boot.core.PrismStage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    private static final String RUNTIME_ART_MAP_ASSET = "game_art/runtime_art_map.json";
    public interface Listener {
        void onHudUpdated(PrismStage stage, int score, int movesLeft, int chain, int charge, String objectiveLabel);
        void onPlayEffect(String effectKey);
        void onStageResolved(boolean cleared, String message);
    }

    private static final int KIND_COLOR = 0;
    private static final int KIND_ARMOR = 1;
    private static final int KIND_RELAY = 2;
    private static final int KIND_PULSE = 3;
    private static final int KIND_VOID = 4;
    private static final int COLOR_CYAN = 0;
    private static final int COLOR_LIME = 1;
    private static final int COLOR_MAGENTA = 2;
    private static final int COLOR_AMBER = 3;
    private static final int COLOR_BLUE = 4;
    private static final int COLOR_RED = 5;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF boardRect = new RectF();
    private final Path path = new Path();
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
    private final List<CellRef> previewCluster = new ArrayList<>();
    private Listener listener;
    private PrismStage currentStage;
    private Cell[][] board;
    private boolean running;
    private long lastFrameNs;
    private float cellSize;
    private float boardLeft;
    private float boardTop;
    private int score;
    private int movesLeft;
    private int bestChain;
    private int currentCharge;
    private int relaysCharged;
    private int armorBroken;
    private int holdRow = -1;
    private int holdCol = -1;
    private float touchDownX;
    private float touchDownY;
    private boolean paused;
    private boolean finished;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void loadStage(PrismStage stage) {
        currentStage = stage;
        score = 0;
        bestChain = 0;
        currentCharge = 0;
        relaysCharged = 0;
        armorBroken = 0;
        movesLeft = stage.moves;
        holdRow = -1;
        holdCol = -1;
        previewCluster.clear();
        popEffects.clear();
        waveEffects.clear();
        finished = false;
        paused = false;
        buildBoard(stage);
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

    public String getObjectiveDescription(PrismStage stage) {
        if (stage == null) {
            return "";
        }
        if (currentStage == stage && board != null) {
            return getObjectiveDescription();
        }
        switch (stage.objectiveType) {
            case SCORE:
                return "Reach " + stage.targetCount + " score";
            case RELAY:
                return "Charge " + stage.targetCount + " relay nodes";
            case ARMOR:
                return "Break " + stage.targetCount + " armor blocks";
            case CHARGE:
                return "Reach " + stage.targetCount + " reactor charge";
            default:
                return "";
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
            return;
        }
        layoutBoard();
        drawBoardFrame(canvas);
        drawCells(canvas);
        drawPreview(canvas);
        drawEffects(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentStage == null || paused || finished) {
            return true;
        }
        layoutBoard();
        int row = rowFor(event.getY());
        int col = colFor(event.getX());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                if (isInside(row, col)) {
                    holdRow = row;
                    holdCol = col;
                    updatePreview(row, col);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - touchDownX) + Math.abs(event.getY() - touchDownY) > cellSize * 0.35f) {
                    previewCluster.clear();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (holdRow >= 0 && holdCol >= 0 && row == holdRow && col == holdCol) {
                    activateCell(row, col);
                }
                holdRow = -1;
                holdCol = -1;
                previewCluster.clear();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                holdRow = -1;
                holdCol = -1;
                previewCluster.clear();
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void activateCell(int row, int col) {
        if (!isInside(row, col)) {
            return;
        }
        Cell cell = board[row][col];
        if (cell == null || cell.kind == KIND_VOID) {
            return;
        }
        if (cell.kind == KIND_PULSE) {
            triggerPulse(row, col);
            return;
        }
        List<CellRef> cluster = findCluster(row, col);
        if (cluster.size() < 4) {
            listener.onPlayEffect("warning");
            spawnWave(row, col, 0.18f, cell.color);
            return;
        }
        resolveCluster(cluster, row, col);
    }

    private void resolveCluster(List<CellRef> cluster, int anchorRow, int anchorCol) {
        movesLeft = Math.max(0, movesLeft - 1);
        int chain = 1;
        int clusterScore = cluster.size() * 55;
        if (cluster.size() >= 7) {
            clusterScore += 400;
        }
        score += clusterScore;
        currentCharge = Math.min(100, currentCharge + cluster.size() * 4);
        if (currentCharge > bestChain * 10) {
            bestChain = Math.min(9, currentCharge / 10);
        }
        boolean createPulse = cluster.size() >= 8;
        Cell anchor = board[anchorRow][anchorCol];
        for (CellRef ref : cluster) {
            if (ref.row == anchorRow && ref.col == anchorCol && createPulse) {
                continue;
            }
            board[ref.row][ref.col] = null;
            spawnPop(ref.row, ref.col, anchor.color);
            damageNeighbors(ref.row, ref.col);
        }
        if (createPulse) {
            anchor.kind = KIND_PULSE;
            anchor.color = anchor.color;
            anchor.armor = 0;
            spawnWave(anchorRow, anchorCol, 0.34f, anchor.color);
        } else {
            board[anchorRow][anchorCol] = null;
            spawnPop(anchorRow, anchorCol, anchor.color);
            damageNeighbors(anchorRow, anchorCol);
        }
        listener.onPlayEffect("collect");
        collapseBoard();
        refillBoard();
        chain += resolveAutoChain();
        bestChain = Math.max(bestChain, chain);
        currentCharge = Math.min(100, currentCharge + Math.max(0, chain - 1) * 12);
        notifyHud();
        checkResolution(chain);
    }

    private void triggerPulse(int row, int col) {
        movesLeft = Math.max(0, movesLeft - 1);
        Cell pulse = board[row][col];
        int pulseColor = pulse.color;
        board[row][col] = null;
        List<CellRef> cleared = new ArrayList<>();
        for (int r = 0; r < currentStage.rows; r++) {
            if (board[r][col] != null && board[r][col].kind != KIND_VOID) {
                cleared.add(new CellRef(r, col));
            }
        }
        for (int c = 0; c < currentStage.cols; c++) {
            if (board[row][c] != null && board[row][c].kind != KIND_VOID) {
                cleared.add(new CellRef(row, c));
            }
        }
        for (CellRef ref : cleared) {
            if (board[ref.row][ref.col] == null) {
                continue;
            }
            board[ref.row][ref.col] = null;
            spawnPop(ref.row, ref.col, pulseColor);
            damageNeighbors(ref.row, ref.col);
        }
        score += 900 + cleared.size() * 40;
        currentCharge = Math.min(100, currentCharge + 16);
        bestChain = Math.max(bestChain, 2);
        listener.onPlayEffect("collect");
        spawnWave(row, col, 0.45f, pulseColor);
        collapseBoard();
        refillBoard();
        int chain = 2 + resolveAutoChain();
        notifyHud();
        checkResolution(chain);
    }

    private int resolveAutoChain() {
        int chains = 0;
        while (true) {
            List<CellRef> largest = findLargestAutoCluster();
            if (largest.size() < 9) {
                break;
            }
            chains++;
            int anchorIndex = largest.size() / 2;
            CellRef anchorRef = largest.get(anchorIndex);
            int color = board[anchorRef.row][anchorRef.col].color;
            for (CellRef ref : largest) {
                board[ref.row][ref.col] = null;
                spawnPop(ref.row, ref.col, color);
                damageNeighbors(ref.row, ref.col);
            }
            score += largest.size() * 70 + 250 * chains;
            currentCharge = Math.min(100, currentCharge + largest.size() * 3);
            collapseBoard();
            refillBoard();
            listener.onPlayEffect("collect");
            spawnWave(anchorRef.row, anchorRef.col, 0.26f, color);
        }
        return chains;
    }

    private List<CellRef> findLargestAutoCluster() {
        boolean[][] visited = new boolean[currentStage.rows][currentStage.cols];
        List<CellRef> best = new ArrayList<>();
        for (int row = 0; row < currentStage.rows; row++) {
            for (int col = 0; col < currentStage.cols; col++) {
                Cell cell = board[row][col];
                if (cell == null || cell.kind != KIND_COLOR || visited[row][col]) {
                    continue;
                }
                List<CellRef> cluster = floodCluster(row, col, visited);
                if (cluster.size() > best.size()) {
                    best = cluster;
                }
            }
        }
        return best;
    }

    private void damageNeighbors(int row, int col) {
        for (int[] dir : DIRS) {
            int nr = row + dir[0];
            int nc = col + dir[1];
            if (!isInside(nr, nc) || board[nr][nc] == null) {
                continue;
            }
            Cell neighbor = board[nr][nc];
            if (neighbor.kind == KIND_ARMOR) {
                neighbor.armor--;
                if (neighbor.armor <= 0) {
                    board[nr][nc] = null;
                    armorBroken++;
                    score += 120;
                    spawnPop(nr, nc, COLOR_AMBER);
                }
            } else if (neighbor.kind == KIND_RELAY && !neighbor.charged) {
                neighbor.charged = true;
                relaysCharged++;
                score += 180;
                spawnWave(nr, nc, 0.28f, COLOR_CYAN);
            }
        }
    }

    private void collapseBoard() {
        for (int col = 0; col < currentStage.cols; col++) {
            int writeRow = currentStage.rows - 1;
            for (int row = currentStage.rows - 1; row >= 0; row--) {
                Cell cell = board[row][col];
                if (cell == null || cell.kind == KIND_VOID) {
                    continue;
                }
                if (board[writeRow][col] != cell) {
                    board[writeRow][col] = cell;
                    board[row][col] = null;
                }
                writeRow--;
            }
            while (writeRow >= 0) {
                if (board[writeRow][col] != null && board[writeRow][col].kind == KIND_VOID) {
                    break;
                }
                board[writeRow][col] = null;
                writeRow--;
            }
        }
        int writeCol = 0;
        for (int col = 0; col < currentStage.cols; col++) {
            if (!columnHasPlayableCells(col)) {
                continue;
            }
            if (writeCol != col) {
                for (int row = 0; row < currentStage.rows; row++) {
                    Cell dest = board[row][writeCol];
                    if (dest != null && dest.kind == KIND_VOID) {
                        continue;
                    }
                    board[row][writeCol] = board[row][col];
                    if (board[row][col] != null && board[row][col].kind != KIND_VOID) {
                        board[row][col] = null;
                    }
                }
            }
            writeCol++;
        }
    }

    private void refillBoard() {
        for (int col = 0; col < currentStage.cols; col++) {
            for (int row = 0; row < currentStage.rows; row++) {
                if (board[row][col] == null) {
                    board[row][col] = newColorCell(randomColor());
                }
            }
        }
        ensurePlayableBoard();
    }

    private boolean columnHasPlayableCells(int col) {
        for (int row = 0; row < currentStage.rows; row++) {
            Cell cell = board[row][col];
            if (cell != null && cell.kind != KIND_VOID) {
                return true;
            }
        }
        return false;
    }

    private void checkResolution(int chain) {
        if (chain > bestChain) {
            bestChain = chain;
        }
        if (isObjectiveMet()) {
            finished = true;
            listener.onStageResolved(true, buildWinMessage(chain));
        } else if (movesLeft <= 0) {
            finished = true;
            listener.onStageResolved(false, buildFailMessage());
        }
    }

    private String buildWinMessage(int chain) {
        switch (currentStage.objectiveType) {
            case SCORE:
                return "Sector stabilized with a " + chain + " chain finish.";
            case RELAY:
                return "All relay nodes are energized.";
            case ARMOR:
                return "Armor array fully shattered.";
            case CHARGE:
                return "Reactor charge reached safe output.";
            default:
                return "Sector clear.";
        }
    }

    private String buildFailMessage() {
        switch (currentStage.objectiveType) {
            case SCORE:
                return "Score target not reached before output loss.";
            case RELAY:
                return "Some relay nodes remain dark.";
            case ARMOR:
                return "Armor blocks still protect the core.";
            case CHARGE:
                return "Charge threshold not reached in time.";
            default:
                return "Objective incomplete.";
        }
    }

    private boolean isObjectiveMet() {
        switch (currentStage.objectiveType) {
            case SCORE:
                return score >= currentStage.targetCount;
            case RELAY:
                return relaysCharged >= currentStage.targetCount;
            case ARMOR:
                return armorBroken >= currentStage.targetCount;
            case CHARGE:
                return currentCharge >= currentStage.targetCount;
            default:
                return false;
        }
    }

    private void updatePreview(int row, int col) {
        previewCluster.clear();
        if (!isInside(row, col) || board[row][col] == null || board[row][col].kind == KIND_VOID) {
            invalidate();
            return;
        }
        if (board[row][col].kind == KIND_PULSE) {
            previewCluster.add(new CellRef(row, col));
        } else {
            previewCluster.addAll(findCluster(row, col));
        }
        invalidate();
    }

    private List<CellRef> findCluster(int row, int col) {
        boolean[][] visited = new boolean[currentStage.rows][currentStage.cols];
        return floodCluster(row, col, visited);
    }

    private List<CellRef> floodCluster(int row, int col, boolean[][] visited) {
        List<CellRef> cluster = new ArrayList<>();
        Cell start = board[row][col];
        if (start == null || start.kind != KIND_COLOR) {
            return cluster;
        }
        ArrayDeque<CellRef> queue = new ArrayDeque<>();
        queue.add(new CellRef(row, col));
        visited[row][col] = true;
        while (!queue.isEmpty()) {
            CellRef ref = queue.removeFirst();
            cluster.add(ref);
            for (int[] dir : DIRS) {
                int nr = ref.row + dir[0];
                int nc = ref.col + dir[1];
                if (!isInside(nr, nc) || visited[nr][nc]) {
                    continue;
                }
                Cell next = board[nr][nc];
                if (next != null && next.kind == KIND_COLOR && next.color == start.color) {
                    visited[nr][nc] = true;
                    queue.addLast(new CellRef(nr, nc));
                }
            }
        }
        return cluster;
    }

    private void buildBoard(PrismStage stage) {
        random.setSeed(stage.seed);
        board = new Cell[stage.rows][stage.cols];
        for (int row = 0; row < stage.rows; row++) {
            for (int col = 0; col < stage.cols; col++) {
                if (stage.splitBoard && isSplitVoid(stage, row, col)) {
                    board[row][col] = newVoidCell();
                } else {
                    board[row][col] = newColorCell(randomColor());
                }
            }
        }
        placeRelays(stage);
        placeArmor(stage);
        seedGuaranteedMoves(4);
        ensurePlayableBoard();
    }

    private void placeRelays(PrismStage stage) {
        int placed = 0;
        int tries = 0;
        while (placed < stage.relayCount && tries < 4000) {
            tries++;
            int row = random.nextInt(stage.rows);
            int col = random.nextInt(stage.cols);
            if (board[row][col] == null || board[row][col].kind == KIND_VOID) {
                continue;
            }
            if (board[row][col].kind == KIND_COLOR) {
                board[row][col] = newRelayCell();
                placed++;
            }
        }
    }

    private void placeArmor(PrismStage stage) {
        int placed = 0;
        int tries = 0;
        while (placed < stage.armorCount && tries < 6000) {
            tries++;
            int row = random.nextInt(stage.rows);
            int col = random.nextInt(stage.cols);
            if (board[row][col] == null || board[row][col].kind != KIND_COLOR) {
                continue;
            }
            int color = board[row][col].color;
            board[row][col] = newArmorCell(color, stage.heavyArmor ? 2 : 1);
            placed++;
        }
    }

    private void ensurePlayableBoard() {
        for (int attempt = 0; attempt < 8; attempt++) {
            if (hasMove()) {
                return;
            }
            if (plantGuaranteedCluster()) {
                return;
            }
            reshufflePlayableColors();
        }
        plantGuaranteedCluster();
    }

    private void reshufflePlayableColors() {
        for (int row = 0; row < currentStage.rows; row++) {
            for (int col = 0; col < currentStage.cols; col++) {
                if (board[row][col] != null && board[row][col].kind == KIND_COLOR) {
                    board[row][col].color = randomColor();
                }
            }
        }
    }

    private void seedGuaranteedMoves(int targetClusters) {
        int planted = 0;
        while (planted < targetClusters && plantGuaranteedCluster()) {
            planted++;
        }
    }

    private boolean plantGuaranteedCluster() {
        int color = randomColor();
        if (seedSquareCluster(color)) {
            return true;
        }
        if (seedHorizontalCluster(color, 4)) {
            return true;
        }
        return seedVerticalCluster(color, 4);
    }

    private boolean seedSquareCluster(int color) {
        int rowStart = currentStage.rows <= 1 ? 0 : random.nextInt(currentStage.rows - 1);
        int colStart = currentStage.cols <= 1 ? 0 : random.nextInt(currentStage.cols - 1);
        for (int rowOffset = 0; rowOffset < currentStage.rows - 1; rowOffset++) {
            int row = rowStart + rowOffset;
            if (row >= currentStage.rows - 1) {
                row -= currentStage.rows - 1;
            }
            for (int colOffset = 0; colOffset < currentStage.cols - 1; colOffset++) {
                int col = colStart + colOffset;
                if (col >= currentStage.cols - 1) {
                    col -= currentStage.cols - 1;
                }
                if (!canSeedBlock(row, col)) {
                    continue;
                }
                board[row][col].color = color;
                board[row + 1][col].color = color;
                board[row][col + 1].color = color;
                board[row + 1][col + 1].color = color;
                return true;
            }
        }
        return false;
    }

    private boolean seedHorizontalCluster(int color, int length) {
        if (length <= 0 || currentStage.cols < length) {
            return false;
        }
        int rowStart = random.nextInt(currentStage.rows);
        int colStart = random.nextInt(currentStage.cols - length + 1);
        for (int rowOffset = 0; rowOffset < currentStage.rows; rowOffset++) {
            int row = rowStart + rowOffset;
            if (row >= currentStage.rows) {
                row -= currentStage.rows;
            }
            for (int colOffset = 0; colOffset <= currentStage.cols - length; colOffset++) {
                int col = colStart + colOffset;
                if (col > currentStage.cols - length) {
                    col -= currentStage.cols - length + 1;
                }
                if (!canSeedHorizontal(row, col, length)) {
                    continue;
                }
                for (int i = 0; i < length; i++) {
                    board[row][col + i].color = color;
                }
                return true;
            }
        }
        return false;
    }

    private boolean seedVerticalCluster(int color, int length) {
        if (length <= 0 || currentStage.rows < length) {
            return false;
        }
        int rowStart = random.nextInt(currentStage.rows - length + 1);
        int colStart = random.nextInt(currentStage.cols);
        for (int rowOffset = 0; rowOffset <= currentStage.rows - length; rowOffset++) {
            int row = rowStart + rowOffset;
            if (row > currentStage.rows - length) {
                row -= currentStage.rows - length + 1;
            }
            for (int colOffset = 0; colOffset < currentStage.cols; colOffset++) {
                int col = colStart + colOffset;
                if (col >= currentStage.cols) {
                    col -= currentStage.cols;
                }
                if (!canSeedVertical(row, col, length)) {
                    continue;
                }
                for (int i = 0; i < length; i++) {
                    board[row + i][col].color = color;
                }
                return true;
            }
        }
        return false;
    }

    private boolean canSeedBlock(int row, int col) {
        return isSeedableColorCell(row, col)
                && isSeedableColorCell(row + 1, col)
                && isSeedableColorCell(row, col + 1)
                && isSeedableColorCell(row + 1, col + 1);
    }

    private boolean canSeedHorizontal(int row, int col, int length) {
        for (int i = 0; i < length; i++) {
            if (!isSeedableColorCell(row, col + i)) {
                return false;
            }
        }
        return true;
    }

    private boolean canSeedVertical(int row, int col, int length) {
        for (int i = 0; i < length; i++) {
            if (!isSeedableColorCell(row + i, col)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSeedableColorCell(int row, int col) {
        return isInside(row, col) && board[row][col] != null && board[row][col].kind == KIND_COLOR;
    }

    private boolean hasMove() {
        for (int row = 0; row < currentStage.rows; row++) {
            for (int col = 0; col < currentStage.cols; col++) {
                Cell cell = board[row][col];
                if (cell != null && cell.kind == KIND_COLOR) {
                    if (findCluster(row, col).size() >= 4) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSplitVoid(PrismStage stage, int row, int col) {
        if (stage.cols < 11) {
            return false;
        }
        int center = stage.cols / 2;
        if (stage.code.equals("4-1")) {
            return col == center && row > 2 && row < stage.rows - 3;
        }
        if (stage.code.equals("5-1")) {
            return (col == center || col == center - 1) && row > 3 && row < stage.rows - 4;
        }
        if (stage.code.equals("6-1")) {
            return col == center && row > 1 && row < stage.rows - 2 && row != stage.rows / 2;
        }
        return false;
    }

    private void update(float dt) {
        for (int i = popEffects.size() - 1; i >= 0; i--) {
            PopEffect effect = popEffects.get(i);
            effect.life -= dt;
            effect.radius += dt * cellSize * 2.2f;
            if (effect.life <= 0f) {
                popEffects.remove(i);
            }
        }
        for (int i = waveEffects.size() - 1; i >= 0; i--) {
            WaveEffect effect = waveEffects.get(i);
            effect.life -= dt;
            effect.radius += dt * cellSize * 1.8f;
            if (effect.life <= 0f) {
                waveEffects.remove(i);
            }
        }
    }

    private void drawBoardFrame(Canvas canvas) {
        rect.set(boardLeft - cellSize * 0.25f, boardTop - cellSize * 0.25f, boardLeft + currentStage.cols * cellSize + cellSize * 0.25f, boardTop + currentStage.rows * cellSize + cellSize * 0.25f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_frame));
        canvas.drawRoundRect(rect, cellSize * 0.32f, cellSize * 0.32f, paint);
        rect.set(boardLeft, boardTop, boardLeft + currentStage.cols * cellSize, boardTop + currentStage.rows * cellSize);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_bg));
        canvas.drawRoundRect(rect, cellSize * 0.18f, cellSize * 0.18f, paint);
    }

    private void drawCells(Canvas canvas) {
        for (int row = 0; row < currentStage.rows; row++) {
            for (int col = 0; col < currentStage.cols; col++) {
                Cell cell = board[row][col];
                float left = boardLeft + col * cellSize;
                float top = boardTop + row * cellSize;
                rect.set(left + cellSize * 0.04f, top + cellSize * 0.04f, left + cellSize * 0.96f, top + cellSize * 0.96f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_board_slot));
                canvas.drawRoundRect(rect, cellSize * 0.12f, cellSize * 0.12f, paint);
                if (cell == null) {
                    continue;
                }
                if (cell.kind == KIND_VOID) {
                    paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_void));
                    canvas.drawRoundRect(rect, cellSize * 0.12f, cellSize * 0.12f, paint);
                    continue;
                }
                drawCellBody(canvas, cell, rect);
            }
        }
    }

    private void drawCellBody(Canvas canvas, Cell cell, RectF slot) {
        int baseColor = resolveColor(cell);
        paint.setStyle(Paint.Style.FILL);
        LinearGradient gradient = new LinearGradient(slot.left, slot.top, slot.right, slot.bottom, lighten(baseColor, 0.25f), darken(baseColor, 0.15f), Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRoundRect(slot, cellSize * 0.14f, cellSize * 0.14f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.06f);
        paint.setColor(lighten(baseColor, 0.55f));
        canvas.drawRoundRect(slot, cellSize * 0.14f, cellSize * 0.14f, paint);
        paint.setStyle(Paint.Style.FILL);
        RadialGradient glow = new RadialGradient(slot.centerX(), slot.centerY(), cellSize * 0.55f, lighten(baseColor, 0.8f), baseColor, Shader.TileMode.CLAMP);
        paint.setShader(glow);
        rect.set(slot.left + cellSize * 0.18f, slot.top + cellSize * 0.16f, slot.right - cellSize * 0.18f, slot.bottom - cellSize * 0.18f);
        canvas.drawOval(rect, paint);
        paint.setShader(null);
        if (cell.kind == KIND_RELAY) {
            drawRelay(canvas, slot, cell.charged);
        } else if (cell.kind == KIND_ARMOR) {
            drawArmor(canvas, slot, cell.armor);
        } else if (cell.kind == KIND_PULSE) {
            drawPulse(canvas, slot);
        } else {
            drawSymbol(canvas, slot, cell.color);
        }
    }

    private void drawRelay(Canvas canvas, RectF slot, boolean charged) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.07f);
        paint.setColor(charged ? ContextCompat.getColor(getContext(), R.color.cst_accent_2) : ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        canvas.drawCircle(slot.centerX(), slot.centerY(), cellSize * 0.22f, paint);
        paint.setStrokeWidth(cellSize * 0.05f);
        canvas.drawLine(slot.centerX(), slot.top + cellSize * 0.18f, slot.centerX(), slot.bottom - cellSize * 0.18f, paint);
        canvas.drawLine(slot.left + cellSize * 0.18f, slot.centerY(), slot.right - cellSize * 0.18f, slot.centerY(), paint);
    }

    private void drawArmor(Canvas canvas, RectF slot, int armor) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.08f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        canvas.drawRoundRect(slot, cellSize * 0.14f, cellSize * 0.14f, paint);
        paint.setStrokeWidth(cellSize * 0.05f);
        if (armor >= 1) {
            canvas.drawLine(slot.left + cellSize * 0.2f, slot.bottom - cellSize * 0.22f, slot.right - cellSize * 0.22f, slot.top + cellSize * 0.24f, paint);
        }
        if (armor >= 2) {
            canvas.drawLine(slot.left + cellSize * 0.18f, slot.centerY(), slot.right - cellSize * 0.18f, slot.centerY() + cellSize * 0.08f, paint);
        }
    }

    private void drawPulse(Canvas canvas, RectF slot) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.06f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        rect.set(slot.left + cellSize * 0.18f, slot.top + cellSize * 0.18f, slot.right - cellSize * 0.18f, slot.bottom - cellSize * 0.18f);
        canvas.drawOval(rect, paint);
        canvas.drawLine(slot.left + cellSize * 0.16f, slot.centerY(), slot.right - cellSize * 0.16f, slot.centerY(), paint);
        canvas.drawLine(slot.centerX(), slot.top + cellSize * 0.16f, slot.centerX(), slot.bottom - cellSize * 0.16f, paint);
    }

    private void drawSymbol(Canvas canvas, RectF slot, int color) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.055f);
        float cx = slot.centerX();
        float cy = slot.centerY();
        float r = cellSize * 0.16f;
        switch (color) {
            case COLOR_CYAN:
                canvas.drawCircle(cx, cy, r, paint);
                break;
            case COLOR_LIME:
                path.reset();
                path.moveTo(cx, cy - r * 1.2f);
                path.lineTo(cx - r, cy + r);
                path.lineTo(cx + r, cy + r);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case COLOR_MAGENTA:
                rect.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawRect(rect, paint);
                break;
            case COLOR_AMBER:
                path.reset();
                path.moveTo(cx, cy - r * 1.25f);
                path.lineTo(cx - r * 1.05f, cy);
                path.lineTo(cx, cy + r * 1.25f);
                path.lineTo(cx + r * 1.05f, cy);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case COLOR_BLUE:
                canvas.drawLine(cx - r, cy, cx + r, cy, paint);
                canvas.drawLine(cx, cy - r, cx, cy + r, paint);
                break;
            case COLOR_RED:
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint);
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint);
                break;
            default:
                break;
        }
    }

    private void drawPreview(Canvas canvas) {
        if (previewCluster.isEmpty()) {
            return;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.07f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        for (CellRef ref : previewCluster) {
            float left = boardLeft + ref.col * cellSize;
            float top = boardTop + ref.row * cellSize;
            rect.set(left + cellSize * 0.08f, top + cellSize * 0.08f, left + cellSize * 0.92f, top + cellSize * 0.92f);
            canvas.drawRoundRect(rect, cellSize * 0.14f, cellSize * 0.14f, paint);
        }
    }

    private void drawEffects(Canvas canvas) {
        for (PopEffect effect : popEffects) {
            int baseColor = resolveColor(effect.color);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(lighten(baseColor, 0.4f), Math.max(0, (int) (180f * effect.life / effect.maxLife))));
            canvas.drawCircle(effect.x, effect.y, effect.radius, paint);
        }
        for (WaveEffect effect : waveEffects) {
            int baseColor = resolveColor(effect.color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.08f);
            paint.setColor(withAlpha(lighten(baseColor, 0.5f), Math.max(0, (int) (220f * effect.life / effect.maxLife))));
            canvas.drawCircle(effect.x, effect.y, effect.radius, paint);
        }
    }

    private void layoutBoard() {
        if (currentStage == null) {
            return;
        }
        float availableW = getWidth() - getPaddingLeft() - getPaddingRight();
        float availableH = getHeight() - getPaddingTop() - getPaddingBottom();
        cellSize = Math.min(availableW / currentStage.cols, availableH / currentStage.rows);
        boardLeft = (getWidth() - currentStage.cols * cellSize) * 0.5f;
        boardTop = (getHeight() - currentStage.rows * cellSize) * 0.5f;
        boardRect.set(boardLeft, boardTop, boardLeft + currentStage.cols * cellSize, boardTop + currentStage.rows * cellSize);
    }

    private int rowFor(float y) {
        return (int) ((y - boardTop) / cellSize);
    }

    private int colFor(float x) {
        return (int) ((x - boardLeft) / cellSize);
    }

    private boolean isInside(int row, int col) {
        return currentStage != null && row >= 0 && col >= 0 && row < currentStage.rows && col < currentStage.cols;
    }

    private void spawnPop(int row, int col, int color) {
        float x = boardLeft + (col + 0.5f) * cellSize;
        float y = boardTop + (row + 0.5f) * cellSize;
        popEffects.add(new PopEffect(x, y, cellSize * 0.15f, 0.24f, color));
    }

    private void spawnWave(int row, int col, float life, int color) {
        float x = boardLeft + (col + 0.5f) * cellSize;
        float y = boardTop + (row + 0.5f) * cellSize;
        waveEffects.add(new WaveEffect(x, y, cellSize * 0.18f, life, color));
    }

    private void notifyHud() {
        if (listener == null || currentStage == null) {
            return;
        }
        listener.onHudUpdated(currentStage, score, movesLeft, bestChain, currentCharge, getObjectiveDescription());
    }

    private String getObjectiveDescription() {
        switch (currentStage.objectiveType) {
            case SCORE:
                return score + " / " + currentStage.targetCount + " score";
            case RELAY:
                return relaysCharged + " / " + currentStage.targetCount + " relays charged";
            case ARMOR:
                return armorBroken + " / " + currentStage.targetCount + " armor broken";
            case CHARGE:
                return currentCharge + " / " + currentStage.targetCount + " reactor charge";
            default:
                return "";
        }
    }

    private Cell newColorCell(int color) {
        return new Cell(KIND_COLOR, color, 0, false);
    }

    private Cell newArmorCell(int color, int armor) {
        return new Cell(KIND_ARMOR, color, armor, false);
    }

    private Cell newRelayCell() {
        return new Cell(KIND_RELAY, COLOR_CYAN, 0, false);
    }

    private Cell newVoidCell() {
        return new Cell(KIND_VOID, COLOR_BLUE, 0, false);
    }

    private int randomColor() {
        return random.nextInt(getActiveColorCount());
    }

    private int getActiveColorCount() {
        if (currentStage == null) {
            return 6;
        }
        if (currentStage.sectorIndex <= 2) {
            return 4;
        }
        if (currentStage.sectorIndex <= 4) {
            return 5;
        }
        return 6;
    }

    private int resolveColor(Cell cell) {
        return resolveColor(cell.color);
    }

    private int resolveColor(int color) {
        switch (color) {
            case COLOR_CYAN:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_cyan);
            case COLOR_LIME:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_lime);
            case COLOR_MAGENTA:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_magenta);
            case COLOR_AMBER:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_amber);
            case COLOR_BLUE:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_blue);
            case COLOR_RED:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_red);
            default:
                return ContextCompat.getColor(getContext(), R.color.cst_prism_cyan);
        }
    }

    private int lighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = r + (int) ((255 - r) * factor);
        g = g + (int) ((255 - g) * factor);
        b = b + (int) ((255 - b) * factor);
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = (int) (r * (1f - factor));
        g = (int) (g * (1f - factor));
        b = (int) (b * (1f - factor));
        return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static final int[][] DIRS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private static class Cell {
        int kind;
        int color;
        int armor;
        boolean charged;

        Cell(int kind, int color, int armor, boolean charged) {
            this.kind = kind;
            this.color = color;
            this.armor = armor;
            this.charged = charged;
        }
    }

    private static class CellRef {
        final int row;
        final int col;

        CellRef(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private static class PopEffect {
        final float x;
        final float y;
        final float maxLife;
        final int color;
        float radius;
        float life;

        PopEffect(float x, float y, float radius, float life, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.life = life;
            this.maxLife = life;
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

        WaveEffect(float x, float y, float radius, float life, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }
}
