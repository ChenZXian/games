package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.SoundManager;
import com.android.boot.core.GameState;
import com.android.boot.core.Level;
import com.android.boot.core.LevelGenerator;
import com.android.boot.core.ProgressStore;

import java.util.ArrayDeque;
import java.util.Deque;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int UNDO_LIMIT = 20;
    private static final long LONG_PRESS_MS = 420;

    private Thread thread;
    private boolean running;
    private boolean paused;
    private float deltaTime;
    private long lastFrameTime;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();

    private int viewWidth;
    private int viewHeight;

    private GameState state = GameState.MENU;
    private final ProgressStore progressStore;
    private final SoundManager soundManager;

    private Level level;
    private int levelIndex;
    private int[] grid;
    private int selectedColor;
    private Tool selectedTool = Tool.BRUSH;
    private boolean mirrorHorizontal = true;
    private int movesUsed;
    private int paintUsed;
    private long levelStartTime;
    private int scorePercent;
    private int starsEarned;
    private boolean perfect;

    private float gridScale = 1f;
    private float previewScale = 1f;

    private int lineStartX = -1;
    private int lineStartY = -1;

    private int lastPaintX = -1;
    private int lastPaintY = -1;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();

    private long touchDownTime;
    private boolean dragging;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        progressStore = new ProgressStore(context);
        soundManager = new SoundManager();
        soundManager.setMuted(progressStore.isMuted());
        setFocusable(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(36f);
        gridPaint.setStyle(Paint.Style.STROKE);
        tilePaint.setStyle(Paint.Style.FILL);
        ensurePaletteRects();
    }

    public void onPause() {
        paused = true;
    }

    public void onResume() {
        paused = false;
        lastFrameTime = SystemClock.uptimeMillis();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
        loadLevel(0);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        viewWidth = width;
        viewHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        soundManager.release();
        boolean retry = true;
        while (retry) {
            try {
                if (thread != null) {
                    thread.join();
                }
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        lastFrameTime = SystemClock.uptimeMillis();
        while (running) {
            long now = SystemClock.uptimeMillis();
            long elapsed = now - lastFrameTime;
            lastFrameTime = now;
            deltaTime = Math.min(elapsed / 1000f, 0.033f);
            if (!paused) {
                update(deltaTime);
            }
            drawFrame();
        }
    }

    private void update(float dt) {
        if (state == GameState.PLAYING && level != null && level.timeLimitSec > 0) {
            int elapsed = (int) ((SystemClock.uptimeMillis() - levelStartTime) / 1000L);
            if (elapsed > level.timeLimitSec) {
                state = GameState.FAIL;
            }
        }
    }

    private void drawFrame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        boolean dark = progressStore.isDarkMode();
        canvas.drawColor(dark ? 0xFF1E1F24 : 0xFFF4F5F7);
        if (state == GameState.MENU) {
            drawMenu(canvas, dark);
        } else if (state == GameState.LEVEL_SELECT) {
            drawLevelSelect(canvas, dark);
        } else if (state == GameState.SETTINGS) {
            drawSettings(canvas, dark);
        } else {
            drawGame(canvas, dark);
            if (state == GameState.PAUSED) {
                drawPauseOverlay(canvas, dark);
            } else if (state == GameState.WIN) {
                drawWinOverlay(canvas, dark);
            } else if (state == GameState.FAIL) {
                drawFailOverlay(canvas, dark);
            }
        }
        getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawMenu(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        float panelWidth = viewWidth * 0.7f;
        float panelHeight = viewHeight * 0.5f;
        float left = (viewWidth - panelWidth) / 2f;
        float top = (viewHeight - panelHeight) / 2f;
        tempRect.set(left, top, left + panelWidth, top + panelHeight);
        canvas.drawRoundRect(tempRect, 32f, 32f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(54f);
        canvas.drawText("Mosaic Studio", viewWidth / 2f, top + 90f, textPaint);
        drawMenuButton(canvas, dark, "Start", top + 150f, "start");
        drawMenuButton(canvas, dark, "Level Select", top + 230f, "levels");
        drawMenuButton(canvas, dark, "Settings", top + 310f, "settings");
    }

    private void drawMenuButton(Canvas canvas, boolean dark, String label, float y, String tag) {
        float width = viewWidth * 0.45f;
        float left = (viewWidth - width) / 2f;
        tempRect.set(left, y, left + width, y + 64f);
        paint.setColor(0xFF4B8CF5);
        canvas.drawRoundRect(tempRect, 24f, 24f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(30f);
        canvas.drawText(label, viewWidth / 2f, y + 44f, textPaint);
        if ("start".equals(tag)) {
            menuStartRect.set(tempRect);
        } else if ("levels".equals(tag)) {
            menuLevelRect.set(tempRect);
        } else if ("settings".equals(tag)) {
            menuSettingsRect.set(tempRect);
        }
    }

    private void drawLevelSelect(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        tempRect.set(viewWidth * 0.08f, viewHeight * 0.1f, viewWidth * 0.92f, viewHeight * 0.9f);
        canvas.drawRoundRect(tempRect, 32f, 32f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(44f);
        canvas.drawText("Select Level", viewWidth / 2f, viewHeight * 0.18f, textPaint);
        int columns = 4;
        int rows = 6;
        float gridLeft = viewWidth * 0.12f;
        float gridTop = viewHeight * 0.24f;
        float cellW = (viewWidth * 0.76f) / columns;
        float cellH = (viewHeight * 0.56f) / rows;
        int unlocked = progressStore.getUnlocked();
        int count = LevelGenerator.getLevelCount();
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (idx >= count) {
                    continue;
                }
                float left = gridLeft + c * cellW + 8f;
                float top = gridTop + r * cellH + 8f;
                tempRect.set(left, top, left + cellW - 16f, top + cellH - 16f);
                boolean isUnlocked = idx <= unlocked;
                paint.setColor(isUnlocked ? 0xFF4B8CF5 : 0xFFB0B5C0);
                canvas.drawRoundRect(tempRect, 20f, 20f, paint);
                textPaint.setColor(0xFFFFFFFF);
                textPaint.setTextSize(28f);
                canvas.drawText(String.valueOf(idx + 1), tempRect.centerX(), tempRect.centerY() + 8f, textPaint);
                levelCells[idx].set(tempRect);
                idx++;
            }
        }
        drawBackButton(canvas, dark);
    }

    private void drawSettings(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        tempRect.set(viewWidth * 0.15f, viewHeight * 0.2f, viewWidth * 0.85f, viewHeight * 0.8f);
        canvas.drawRoundRect(tempRect, 32f, 32f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(42f);
        canvas.drawText("Settings", viewWidth / 2f, viewHeight * 0.3f, textPaint);
        drawToggle(canvas, dark, "Dark Mode", viewHeight * 0.4f, progressStore.isDarkMode(), "dark");
        drawToggle(canvas, dark, "Mute", viewHeight * 0.5f, progressStore.isMuted(), "mute");
        drawBackButton(canvas, dark);
    }

    private void drawToggle(Canvas canvas, boolean dark, String label, float y, boolean value, String tag) {
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(30f);
        canvas.drawText(label, viewWidth / 2f, y, textPaint);
        float width = 160f;
        float left = viewWidth / 2f - width / 2f;
        tempRect.set(left, y + 16f, left + width, y + 64f);
        paint.setColor(value ? 0xFF39C18C : 0xFFB0B5C0);
        canvas.drawRoundRect(tempRect, 24f, 24f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(26f);
        canvas.drawText(value ? "On" : "Off", tempRect.centerX(), tempRect.centerY() + 8f, textPaint);
        if ("dark".equals(tag)) {
            settingsDarkRect.set(tempRect);
        } else if ("mute".equals(tag)) {
            settingsMuteRect.set(tempRect);
        }
    }

    private void drawBackButton(Canvas canvas, boolean dark) {
        float width = 160f;
        float left = viewWidth / 2f - width / 2f;
        float top = viewHeight * 0.74f;
        tempRect.set(left, top, left + width, top + 58f);
        paint.setColor(dark ? 0xFF4B8CF5 : 0xFF4B8CF5);
        canvas.drawRoundRect(tempRect, 24f, 24f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(26f);
        canvas.drawText("Back", tempRect.centerX(), tempRect.centerY() + 8f, textPaint);
        backRect.set(tempRect);
    }

    private void drawGame(Canvas canvas, boolean dark) {
        if (level == null) {
            return;
        }
        float sidePadding = viewWidth * 0.04f;
        float topPadding = viewHeight * 0.05f;
        float previewSize = viewWidth * 0.22f * previewScale;
        float previewLeft = sidePadding;
        float previewTop = topPadding;
        drawPanel(canvas, dark, previewLeft, previewTop, previewSize, previewSize);
        drawTargetPreview(canvas, previewLeft, previewTop, previewSize);

        float toolWidth = viewWidth * 0.18f;
        float toolLeft = viewWidth - toolWidth - sidePadding;
        float toolTop = topPadding;
        float toolHeight = viewHeight * 0.62f;
        drawPanel(canvas, dark, toolLeft, toolTop, toolWidth, toolHeight);
        drawTools(canvas, toolLeft, toolTop, toolWidth, toolHeight);

        float paletteHeight = viewHeight * 0.16f;
        float paletteLeft = sidePadding;
        float paletteTop = viewHeight - paletteHeight - sidePadding;
        float paletteWidth = viewWidth - sidePadding * 2f - toolWidth - sidePadding;
        drawPanel(canvas, dark, paletteLeft, paletteTop, paletteWidth, paletteHeight);
        drawPalette(canvas, paletteLeft, paletteTop, paletteWidth, paletteHeight);

        float gridLeft = previewLeft + previewSize + sidePadding;
        float gridTop = topPadding;
        float gridWidth = paletteWidth;
        float gridHeight = paletteTop - topPadding - sidePadding;
        drawPanel(canvas, dark, gridLeft, gridTop, gridWidth, gridHeight);
        drawGrid(canvas, dark, gridLeft, gridTop, gridWidth, gridHeight);
        drawHud(canvas, dark, gridLeft, gridTop, gridWidth);
        drawZoomButtons(canvas, dark, previewLeft, previewTop + previewSize + 12f, previewSize);
    }

    private void drawPanel(Canvas canvas, boolean dark, float left, float top, float width, float height) {
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        tempRect.set(left, top, left + width, top + height);
        canvas.drawRoundRect(tempRect, 26f, 26f, paint);
    }

    private void drawTargetPreview(Canvas canvas, float left, float top, float size) {
        int gridSize = level.size;
        float cell = size / gridSize;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int value = level.target[y * gridSize + x];
                if (value >= 0) {
                    tilePaint.setColor(level.palette[value]);
                    canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, tilePaint);
                }
            }
        }
    }

    private void drawTools(Canvas canvas, float left, float top, float width, float height) {
        float buttonHeight = height / 6f;
        drawToolButton(canvas, left + 12f, top + 20f, width - 24f, buttonHeight - 24f, "Brush", Tool.BRUSH);
        drawToolButton(canvas, left + 12f, top + buttonHeight + 20f, width - 24f, buttonHeight - 24f, "Line", Tool.LINE);
        drawToolButton(canvas, left + 12f, top + buttonHeight * 2f + 20f, width - 24f, buttonHeight - 24f, "Fill", Tool.FILL);
        drawToolButton(canvas, left + 12f, top + buttonHeight * 3f + 20f, width - 24f, buttonHeight - 24f, mirrorHorizontal ? "Mirror H" : "Mirror V", Tool.MIRROR);
        drawToolButton(canvas, left + 12f, top + buttonHeight * 4f + 20f, width - 24f, buttonHeight - 24f, "Undo", Tool.UNDO);
        drawToolButton(canvas, left + 12f, top + buttonHeight * 5f + 20f, width - 24f, buttonHeight - 24f, "Pause", Tool.PAUSE);
    }

    private void drawToolButton(Canvas canvas, float left, float top, float width, float height, String label, Tool tool) {
        tempRect.set(left, top, left + width, top + height);
        boolean selected = tool == selectedTool;
        paint.setColor(selected ? 0xFF4B8CF5 : 0xFFB0B5C0);
        canvas.drawRoundRect(tempRect, 18f, 18f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(22f);
        canvas.drawText(label, tempRect.centerX(), tempRect.centerY() + 8f, textPaint);
        if (tool == Tool.BRUSH) {
            toolBrushRect.set(tempRect);
        } else if (tool == Tool.LINE) {
            toolLineRect.set(tempRect);
        } else if (tool == Tool.FILL) {
            toolFillRect.set(tempRect);
        } else if (tool == Tool.MIRROR) {
            toolMirrorRect.set(tempRect);
        } else if (tool == Tool.UNDO) {
            toolUndoRect.set(tempRect);
        } else if (tool == Tool.PAUSE) {
            toolPauseRect.set(tempRect);
        }
    }

    private void drawPalette(Canvas canvas, float left, float top, float width, float height) {
        int count = level.palette.length;
        float cell = width / count;
        for (int i = 0; i < count; i++) {
            float cx = left + i * cell;
            tempRect.set(cx + 8f, top + 16f, cx + cell - 8f, top + height - 16f);
            paint.setColor(level.palette[i]);
            canvas.drawRoundRect(tempRect, 16f, 16f, paint);
            if (i == selectedColor) {
                gridPaint.setColor(0xFF101216);
                gridPaint.setStrokeWidth(4f);
                canvas.drawRoundRect(tempRect, 16f, 16f, gridPaint);
            }
            paletteRects[i].set(tempRect);
        }
    }

    private void drawGrid(Canvas canvas, boolean dark, float left, float top, float width, float height) {
        int gridSize = level.size;
        float cell = Math.min(width, height) / gridSize * gridScale;
        float gridWidth = cell * gridSize;
        float gridHeight = cell * gridSize;
        float offsetX = left + (width - gridWidth) / 2f;
        float offsetY = top + (height - gridHeight) / 2f;
        gridOriginX = offsetX;
        gridOriginY = offsetY;
        gridCellSize = cell;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int value = grid[y * gridSize + x];
                if (value >= 0) {
                    tilePaint.setColor(level.palette[value]);
                    canvas.drawRect(offsetX + x * cell, offsetY + y * cell, offsetX + (x + 1) * cell, offsetY + (y + 1) * cell, tilePaint);
                }
            }
        }
        gridPaint.setColor(dark ? 0xFF3B3E46 : 0xFFCED3DB);
        gridPaint.setStrokeWidth(2f);
        for (int i = 0; i <= gridSize; i++) {
            float x = offsetX + i * cell;
            float y = offsetY + i * cell;
            canvas.drawLine(offsetX, offsetY + i * cell, offsetX + gridWidth, offsetY + i * cell, gridPaint);
            canvas.drawLine(offsetX + i * cell, offsetY, offsetX + i * cell, offsetY + gridHeight, gridPaint);
        }
    }

    private void drawHud(Canvas canvas, boolean dark, float left, float top, float width) {
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(24f);
        canvas.drawText("Level " + (levelIndex + 1), left + width / 2f, top - 12f, textPaint);
        canvas.drawText("Score " + scorePercent + "%", left + width / 2f, top + 18f, textPaint);
    }

    private void drawZoomButtons(Canvas canvas, boolean dark, float left, float top, float size) {
        float buttonSize = 54f;
        float gap = 12f;
        tempRect.set(left, top, left + buttonSize, top + buttonSize);
        paint.setColor(0xFF4B8CF5);
        canvas.drawRoundRect(tempRect, 16f, 16f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(28f);
        canvas.drawText("+", tempRect.centerX(), tempRect.centerY() + 10f, textPaint);
        zoomInRect.set(tempRect);
        tempRect.set(left + buttonSize + gap, top, left + buttonSize * 2f + gap, top + buttonSize);
        canvas.drawRoundRect(tempRect, 16f, 16f, paint);
        canvas.drawText("-", tempRect.centerX(), tempRect.centerY() + 10f, textPaint);
        zoomOutRect.set(tempRect);
        tempRect.set(left, top + buttonSize + gap, left + buttonSize * 2f + gap, top + buttonSize * 2f + gap);
        paint.setColor(dark ? 0xFFB0B5C0 : 0xFFB0B5C0);
        canvas.drawRoundRect(tempRect, 16f, 16f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(20f);
        canvas.drawText("Canvas", tempRect.centerX(), tempRect.centerY() + 6f, textPaint);
        zoomCanvasRect.set(tempRect);
    }

    private void drawPauseOverlay(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xCC1E1F24 : 0xCCF4F5F7);
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        float width = viewWidth * 0.6f;
        float left = (viewWidth - width) / 2f;
        float top = viewHeight * 0.3f;
        tempRect.set(left, top, left + width, top + 200f);
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        canvas.drawRoundRect(tempRect, 30f, 30f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(36f);
        canvas.drawText("Paused", viewWidth / 2f, top + 50f, textPaint);
        drawOverlayButton(canvas, "Resume", top + 80f, pauseResumeRect);
        drawOverlayButton(canvas, "Restart", top + 140f, pauseRestartRect);
        drawOverlayButton(canvas, "Menu", top + 200f, pauseMenuRect);
    }

    private void drawWinOverlay(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xCC1E1F24 : 0xCCF4F5F7);
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        float width = viewWidth * 0.65f;
        float left = (viewWidth - width) / 2f;
        float top = viewHeight * 0.28f;
        tempRect.set(left, top, left + width, top + 240f);
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        canvas.drawRoundRect(tempRect, 30f, 30f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(36f);
        canvas.drawText("Complete", viewWidth / 2f, top + 50f, textPaint);
        textPaint.setTextSize(26f);
        canvas.drawText("Score " + scorePercent + "%", viewWidth / 2f, top + 90f, textPaint);
        canvas.drawText("Stars " + starsEarned, viewWidth / 2f, top + 122f, textPaint);
        if (perfect) {
            canvas.drawText("Perfect", viewWidth / 2f, top + 154f, textPaint);
        }
        drawOverlayButton(canvas, "Next", top + 170f, winNextRect);
        drawOverlayButton(canvas, "Restart", top + 230f, winRestartRect);
        drawOverlayButton(canvas, "Menu", top + 290f, winMenuRect);
    }

    private void drawFailOverlay(Canvas canvas, boolean dark) {
        paint.setColor(dark ? 0xCC1E1F24 : 0xCCF4F5F7);
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        float width = viewWidth * 0.6f;
        float left = (viewWidth - width) / 2f;
        float top = viewHeight * 0.32f;
        tempRect.set(left, top, left + width, top + 200f);
        paint.setColor(dark ? 0xFF2A2B31 : 0xFFFFFFFF);
        canvas.drawRoundRect(tempRect, 30f, 30f, paint);
        textPaint.setColor(dark ? 0xFFF5F6F8 : 0xFF101216);
        textPaint.setTextSize(36f);
        canvas.drawText("Try Again", viewWidth / 2f, top + 50f, textPaint);
        drawOverlayButton(canvas, "Retry", top + 80f, failRetryRect);
        drawOverlayButton(canvas, "Menu", top + 140f, failMenuRect);
    }

    private void drawOverlayButton(Canvas canvas, String label, float top, RectF rect) {
        float width = viewWidth * 0.4f;
        float left = (viewWidth - width) / 2f;
        tempRect.set(left, top, left + width, top + 50f);
        paint.setColor(0xFF4B8CF5);
        canvas.drawRoundRect(tempRect, 20f, 20f, paint);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(22f);
        canvas.drawText(label, tempRect.centerX(), tempRect.centerY() + 8f, textPaint);
        rect.set(tempRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (state == GameState.MENU) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (menuStartRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.PLAYING;
                    loadLevel(0);
                } else if (menuLevelRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.LEVEL_SELECT;
                } else if (menuSettingsRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.SETTINGS;
                }
            }
            return true;
        }
        if (state == GameState.LEVEL_SELECT) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                for (int i = 0; i < LevelGenerator.getLevelCount(); i++) {
                    if (levelCells[i].contains(x, y) && i <= progressStore.getUnlocked()) {
                        soundManager.playClick();
                        state = GameState.PLAYING;
                        loadLevel(i);
                        return true;
                    }
                }
                if (backRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.SETTINGS) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (settingsDarkRect.contains(x, y)) {
                    progressStore.setDarkMode(!progressStore.isDarkMode());
                    soundManager.playClick();
                } else if (settingsMuteRect.contains(x, y)) {
                    boolean newMuted = !progressStore.isMuted();
                    progressStore.setMuted(newMuted);
                    soundManager.setMuted(newMuted);
                    soundManager.playClick();
                } else if (backRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.PAUSED) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (pauseResumeRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.PLAYING;
                } else if (pauseRestartRect.contains(x, y)) {
                    soundManager.playClick();
                    loadLevel(levelIndex);
                    state = GameState.PLAYING;
                } else if (pauseMenuRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.WIN) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (winNextRect.contains(x, y)) {
                    soundManager.playClick();
                    int next = Math.min(LevelGenerator.getLevelCount() - 1, levelIndex + 1);
                    loadLevel(next);
                    state = GameState.PLAYING;
                } else if (winRestartRect.contains(x, y)) {
                    soundManager.playClick();
                    loadLevel(levelIndex);
                    state = GameState.PLAYING;
                } else if (winMenuRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.FAIL) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (failRetryRect.contains(x, y)) {
                    soundManager.playClick();
                    loadLevel(levelIndex);
                    state = GameState.PLAYING;
                } else if (failMenuRect.contains(x, y)) {
                    soundManager.playClick();
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state != GameState.PLAYING) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownTime = SystemClock.uptimeMillis();
            dragging = true;
            lastPaintX = -1;
            lastPaintY = -1;
            if (toolPauseRect.contains(x, y)) {
                soundManager.playClick();
                state = GameState.PAUSED;
                return true;
            }
            if (toolUndoRect.contains(x, y)) {
                soundManager.playClick();
                undo();
                return true;
            }
            if (toolBrushRect.contains(x, y)) {
                selectedTool = Tool.BRUSH;
                return true;
            }
            if (toolLineRect.contains(x, y)) {
                selectedTool = Tool.LINE;
                return true;
            }
            if (toolFillRect.contains(x, y)) {
                selectedTool = Tool.FILL;
                return true;
            }
            if (toolMirrorRect.contains(x, y)) {
                selectedTool = Tool.MIRROR;
                mirrorHorizontal = !mirrorHorizontal;
                return true;
            }
            if (zoomInRect.contains(x, y)) {
                previewScale = Math.min(1.6f, previewScale + 0.1f);
                return true;
            }
            if (zoomOutRect.contains(x, y)) {
                previewScale = Math.max(0.7f, previewScale - 0.1f);
                return true;
            }
            if (zoomCanvasRect.contains(x, y)) {
                gridScale += 0.1f;
                if (gridScale > 1.6f) {
                    gridScale = 0.8f;
                }
                return true;
            }
            for (int i = 0; i < paletteRects.length; i++) {
                if (paletteRects[i].contains(x, y)) {
                    selectedColor = i;
                    soundManager.playClick();
                    return true;
                }
            }
            int cellX = screenToGridX(x);
            int cellY = screenToGridY(y);
            if (cellX >= 0 && cellY >= 0) {
                if (selectedTool == Tool.LINE) {
                    lineStartX = cellX;
                    lineStartY = cellY;
                } else if (selectedTool == Tool.FILL) {
                    applyFill(cellX, cellY, resolvePaintColor());
                } else {
                    applyPaint(cellX, cellY, resolvePaintColor());
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!dragging) {
                return true;
            }
            int cellX = screenToGridX(x);
            int cellY = screenToGridY(y);
            if (cellX >= 0 && cellY >= 0) {
                if (selectedTool == Tool.BRUSH || selectedTool == Tool.MIRROR) {
                    applyPaint(cellX, cellY, resolvePaintColor());
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            if (selectedTool == Tool.LINE && lineStartX >= 0) {
                int cellX = screenToGridX(x);
                int cellY = screenToGridY(y);
                if (cellX >= 0 && cellY >= 0) {
                    applyLine(lineStartX, lineStartY, cellX, cellY, resolvePaintColor());
                }
                lineStartX = -1;
                lineStartY = -1;
            }
        }
        return true;
    }

    private int resolvePaintColor() {
        boolean longPress = SystemClock.uptimeMillis() - touchDownTime > LONG_PRESS_MS;
        return longPress ? -1 : selectedColor;
    }

    private void applyPaint(int x, int y, int colorIndex) {
        if (x == lastPaintX && y == lastPaintY && colorIndex == lastPaintColor) {
            return;
        }
        lastPaintX = x;
        lastPaintY = y;
        lastPaintColor = colorIndex;
        UndoAction action = new UndoAction();
        recordCellChange(action, x, y, colorIndex);
        if (selectedTool == Tool.MIRROR) {
            int mirrorX = mirrorHorizontal ? x : level.size - 1 - x;
            int mirrorY = mirrorHorizontal ? level.size - 1 - y : y;
            if (mirrorHorizontal) {
                mirrorY = level.size - 1 - y;
                mirrorX = x;
            } else {
                mirrorX = level.size - 1 - x;
                mirrorY = y;
            }
            if (mirrorX != x || mirrorY != y) {
                recordCellChange(action, mirrorX, mirrorY, colorIndex);
            }
        }
        if (action.count > 0) {
            pushUndo(action);
            movesUsed++;
            if (colorIndex >= 0) {
                paintUsed++;
            }
            updateScore();
            soundManager.playClick();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void applyLine(int x0, int y0, int x1, int y1, int colorIndex) {
        UndoAction action = new UndoAction();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            recordCellChange(action, x, y, colorIndex);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        if (action.count > 0) {
            pushUndo(action);
            movesUsed++;
            if (colorIndex >= 0) {
                paintUsed++;
            }
            updateScore();
            soundManager.playClick();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void applyFill(int x, int y, int colorIndex) {
        int size = level.size;
        int targetColor = grid[y * size + x];
        if (targetColor == colorIndex) {
            return;
        }
        int max = size * size * 4;
        int[] queueX = new int[max];
        int[] queueY = new int[max];
        int head = 0;
        int tail = 0;
        queueX[tail] = x;
        queueY[tail] = y;
        tail++;
        UndoAction action = new UndoAction();
        while (head < tail) {
            int cx = queueX[head];
            int cy = queueY[head];
            head++;
            if (cx < 0 || cy < 0 || cx >= size || cy >= size) {
                continue;
            }
            int idx = cy * size + cx;
            if (grid[idx] != targetColor) {
                continue;
            }
            recordCellChange(action, cx, cy, colorIndex);
            if (tail + 4 >= max) {
                break;
            }
            queueX[tail] = cx + 1;
            queueY[tail] = cy;
            tail++;
            queueX[tail] = cx - 1;
            queueY[tail] = cy;
            tail++;
            queueX[tail] = cx;
            queueY[tail] = cy + 1;
            tail++;
            queueX[tail] = cx;
            queueY[tail] = cy - 1;
            tail++;
        }
        if (action.count > 0) {
            pushUndo(action);
            movesUsed++;
            if (colorIndex >= 0) {
                paintUsed++;
            }
            updateScore();
            soundManager.playClick();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void recordCellChange(UndoAction action, int x, int y, int colorIndex) {
        int size = level.size;
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return;
        }
        int idx = y * size + x;
        int prev = grid[idx];
        if (prev == colorIndex) {
            return;
        }
        action.ensureCapacity(action.count + 1);
        action.indices[action.count] = idx;
        action.previous[action.count] = prev;
        action.next[action.count] = colorIndex;
        action.count++;
        grid[idx] = colorIndex;
    }

    private void pushUndo(UndoAction action) {
        undoStack.push(action);
        while (undoStack.size() > UNDO_LIMIT) {
            undoStack.removeLast();
        }
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoAction action = undoStack.pop();
        for (int i = 0; i < action.count; i++) {
            grid[action.indices[i]] = action.previous[i];
        }
        movesUsed = Math.max(0, movesUsed - 1);
        updateScore();
    }

    private void updateScore() {
        int size = level.size;
        int total = size * size;
        int match = 0;
        for (int i = 0; i < total; i++) {
            if (grid[i] == level.target[i]) {
                match++;
            }
        }
        scorePercent = Math.round(match * 100f / total);
        perfect = scorePercent == 100;
        int movesBonus = Math.max(0, level.maxMoves - movesUsed);
        int bonus = Math.min(10, movesBonus);
        int adjusted = Math.min(100, scorePercent + bonus);
        if (adjusted >= 98) {
            starsEarned = 3;
        } else if (adjusted >= 92) {
            starsEarned = 2;
        } else if (adjusted >= 80) {
            starsEarned = 1;
        } else {
            starsEarned = 0;
        }
        if (scorePercent == 100) {
            handleWin();
        } else if (level.maxMoves > 0 && movesUsed > level.maxMoves) {
            state = GameState.FAIL;
        }
    }

    private void handleWin() {
        if (state == GameState.WIN) {
            return;
        }
        state = GameState.WIN;
        progressStore.setBest(levelIndex, starsEarned, scorePercent);
        if (starsEarned > 0) {
            progressStore.setUnlocked(levelIndex + 1);
        }
        soundManager.playWin();
    }

    private int screenToGridX(float x) {
        if (gridCellSize <= 0f) {
            return -1;
        }
        float localX = x - gridOriginX;
        if (localX < 0 || localX > gridCellSize * level.size) {
            return -1;
        }
        int cellX = (int) (localX / gridCellSize);
        return clampCell(cellX);
    }

    private int screenToGridY(float y) {
        if (gridCellSize <= 0f) {
            return -1;
        }
        float localY = y - gridOriginY;
        if (localY < 0 || localY > gridCellSize * level.size) {
            return -1;
        }
        int cellY = (int) (localY / gridCellSize);
        return clampCell(cellY);
    }

    private int clampCell(int cell) {
        if (cell < 0) {
            return -1;
        }
        if (cell >= level.size) {
            return -1;
        }
        return cell;
    }

    private void loadLevel(int index) {
        levelIndex = index;
        level = LevelGenerator.getLevel(index);
        grid = new int[level.size * level.size];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = -1;
        }
        selectedColor = 0;
        selectedTool = Tool.BRUSH;
        mirrorHorizontal = true;
        movesUsed = 0;
        paintUsed = 0;
        levelStartTime = SystemClock.uptimeMillis();
        lineStartX = -1;
        lineStartY = -1;
        undoStack.clear();
        updateScore();
        initRects();
    }

    private void initRects() {
        menuStartRect.setEmpty();
        menuLevelRect.setEmpty();
        menuSettingsRect.setEmpty();
        backRect.setEmpty();
        settingsDarkRect.setEmpty();
        settingsMuteRect.setEmpty();
        toolBrushRect.setEmpty();
        toolLineRect.setEmpty();
        toolFillRect.setEmpty();
        toolMirrorRect.setEmpty();
        toolUndoRect.setEmpty();
        toolPauseRect.setEmpty();
        zoomInRect.setEmpty();
        zoomOutRect.setEmpty();
        zoomCanvasRect.setEmpty();
        pauseResumeRect.setEmpty();
        pauseRestartRect.setEmpty();
        pauseMenuRect.setEmpty();
        winNextRect.setEmpty();
        winRestartRect.setEmpty();
        winMenuRect.setEmpty();
        failRetryRect.setEmpty();
        failMenuRect.setEmpty();
        for (int i = 0; i < paletteRects.length; i++) {
            paletteRects[i].setEmpty();
        }
        for (int i = 0; i < levelCells.length; i++) {
            levelCells[i].setEmpty();
        }
    }

    private float gridOriginX;
    private float gridOriginY;
    private float gridCellSize;
    private int lastPaintColor = -2;

    private final RectF menuStartRect = new RectF();
    private final RectF menuLevelRect = new RectF();
    private final RectF menuSettingsRect = new RectF();
    private final RectF backRect = new RectF();
    private final RectF settingsDarkRect = new RectF();
    private final RectF settingsMuteRect = new RectF();

    private final RectF toolBrushRect = new RectF();
    private final RectF toolLineRect = new RectF();
    private final RectF toolFillRect = new RectF();
    private final RectF toolMirrorRect = new RectF();
    private final RectF toolUndoRect = new RectF();
    private final RectF toolPauseRect = new RectF();

    private final RectF zoomInRect = new RectF();
    private final RectF zoomOutRect = new RectF();
    private final RectF zoomCanvasRect = new RectF();

    private final RectF pauseResumeRect = new RectF();
    private final RectF pauseRestartRect = new RectF();
    private final RectF pauseMenuRect = new RectF();
    private final RectF winNextRect = new RectF();
    private final RectF winRestartRect = new RectF();
    private final RectF winMenuRect = new RectF();
    private final RectF failRetryRect = new RectF();
    private final RectF failMenuRect = new RectF();

    private final RectF[] paletteRects = new RectF[8];
    private final RectF[] levelCells = new RectF[LevelGenerator.getLevelCount()];

    private enum Tool {
        BRUSH,
        LINE,
        FILL,
        MIRROR,
        UNDO,
        PAUSE
    }

    private static class UndoAction {
        int[] indices = new int[4];
        int[] previous = new int[4];
        int[] next = new int[4];
        int count;

        void ensureCapacity(int needed) {
            if (needed <= indices.length) {
                return;
            }
            int newSize = Math.max(needed, indices.length * 2);
            int[] newIndices = new int[newSize];
            int[] newPrevious = new int[newSize];
            int[] newNext = new int[newSize];
            System.arraycopy(indices, 0, newIndices, 0, count);
            System.arraycopy(previous, 0, newPrevious, 0, count);
            System.arraycopy(next, 0, newNext, 0, count);
            indices = newIndices;
            previous = newPrevious;
            next = newNext;
        }
    }

    private void ensurePaletteRects() {
        for (int i = 0; i < paletteRects.length; i++) {
            if (paletteRects[i] == null) {
                paletteRects[i] = new RectF();
            }
        }
        for (int i = 0; i < levelCells.length; i++) {
            if (levelCells[i] == null) {
                levelCells[i] = new RectF();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        ensurePaletteRects();
        initRects();
    }

    private void ensureInit() {
        if (paletteRects[0] == null) {
            ensurePaletteRects();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureInit();
    }
}
