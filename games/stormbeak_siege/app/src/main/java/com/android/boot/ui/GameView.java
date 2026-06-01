package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.ToneFx;
import com.android.boot.engine.GameEngine;
import com.android.boot.model.GameDefs;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final GameEngine engine;
    private final ToneFx toneFx;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trajectoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Map<String, Bitmap> art = new HashMap<>();
    private final String[] assetList = new String[]{
            "game_art/bird_gust.png",
            "game_art/bird_ember.png",
            "game_art/bird_bolt.png",
            "game_art/bird_frost.png",
            "game_art/bird_ram.png",
            "game_art/block_wood.png",
            "game_art/block_stone.png",
            "game_art/block_glass.png",
            "game_art/block_metal.png",
            "game_art/core.png",
            "game_art/relay.png",
            "game_art/barrel.png",
            "game_art/island.png",
            "game_art/cloud.png",
            "game_art/sling.png"
    };
    private Thread loopThread;
    private boolean running;
    private long lastTime;
    private GameEngine.RenderState renderState;
    private boolean artLoaded;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        engine = new GameEngine();
        toneFx = new ToneFx();
        textPaint.setColor(Color.WHITE);
        trajectoryPaint.setColor(Color.argb(180, 110, 246, 255));
        trajectoryPaint.setStyle(Paint.Style.FILL);
        flashPaint.setColor(Color.WHITE);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        ensureArtLoaded();
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        engine.setViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    public void onHostResume() {
        startLoop();
    }

    public void onHostPause() {
        engine.pause();
        stopLoop();
    }

    public void onHostDestroy() {
        toneFx.release();
    }

    public void startCampaign() {
        engine.startCampaign();
    }

    public void resumeGame() {
        engine.resume();
    }

    public void pauseGame() {
        engine.pause();
    }

    public void restartLevel() {
        engine.restartStage();
    }

    public void backToMenu() {
        engine.backToMenu();
    }

    public void advanceAfterResult() {
        engine.advanceAfterResult();
    }

    public void setMuted(boolean muted) {
        engine.setMuted(muted);
    }

    public boolean isMuted() {
        return engine.isMuted();
    }

    public GameEngine.HudSnapshot getSnapshot() {
        return engine.getSnapshot();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            boolean activeBefore = engine.getSnapshot().birdActive;
            engine.beginTouch(event.getX(), event.getY());
            if (activeBefore) {
                toneFx.playSkill(engine.isMuted());
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            engine.moveTouch(event.getX(), event.getY());
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            boolean hadBird = engine.getSnapshot().birdActive;
            engine.endTouch(event.getX(), event.getY());
            if (!hadBird && engine.getSnapshot().birdActive) {
                toneFx.playLaunch(engine.isMuted());
            }
        }
        return true;
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1000000000f;
            lastTime = now;
            engine.update(dt);
            renderState = engine.copyRenderState();
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    drawFrame(canvas, renderState);
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastTime = System.nanoTime();
        loopThread = new Thread(this, "stormbeak-loop");
        loopThread.start();
    }

    private void stopLoop() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            loopThread = null;
        }
    }

    private void ensureArtLoaded() {
        if (artLoaded) {
            return;
        }
        AssetManager assets = getContext().getAssets();
        for (String assetPath : assetList) {
            try (InputStream stream = assets.open(assetPath)) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) {
                    art.put(assetPath, bitmap);
                }
            } catch (IOException ignored) {
            }
        }
        artLoaded = true;
    }

    private void drawFrame(Canvas canvas, GameEngine.RenderState state) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        drawSky(canvas, width, height, state.weather);
        drawBackdrop(canvas, width, height);
        drawPlayfieldPanels(canvas, width, height);
        drawSling(canvas, state);
        drawFortress(canvas, state);
        drawTrajectory(canvas, state);
        drawBird(canvas, state);
        drawInFieldHud(canvas, state);
        if (state.flash > 0f) {
            flashPaint.setAlpha((int) (state.flash * 90f));
            canvas.drawRect(0f, 0f, width, height, flashPaint);
        }
    }

    private void drawSky(Canvas canvas, int width, int height, int weather) {
        int top = weather == GameDefs.WEATHER_STORM ? Color.parseColor("#2A3956") : Color.parseColor("#1A2748");
        int bottom = weather == GameDefs.WEATHER_FREEZE ? Color.parseColor("#7DA9D6") : Color.parseColor("#5F87C8");
        paint.setShader(new LinearGradient(0f, 0f, 0f, height, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);
    }

    private void drawBackdrop(Canvas canvas, int width, int height) {
        Bitmap cloud = art.get("game_art/cloud.png");
        Bitmap island = art.get("game_art/island.png");
        if (cloud != null) {
            drawBitmap(canvas, cloud, width * 0.16f, height * 0.14f, width * 0.18f, height * 0.11f);
            drawBitmap(canvas, cloud, width * 0.53f, height * 0.1f, width * 0.16f, height * 0.1f);
            drawBitmap(canvas, cloud, width * 0.76f, height * 0.18f, width * 0.14f, height * 0.09f);
        }
        if (island != null) {
            drawBitmap(canvas, island, width * 0.08f, height * 0.78f, width * 0.22f, height * 0.16f);
            drawBitmap(canvas, island, width * 0.62f, height * 0.78f, width * 0.34f, height * 0.14f);
        }
    }

    private void drawPlayfieldPanels(Canvas canvas, int width, int height) {
        hudPaint.setColor(Color.argb(132, 12, 24, 48));
        rect.set(width * 0.04f, height * 0.08f, width * 0.24f, height * 0.9f);
        canvas.drawRoundRect(rect, width * 0.02f, width * 0.02f, hudPaint);
        rect.set(width * 0.78f, height * 0.12f, width * 0.96f, height * 0.54f);
        canvas.drawRoundRect(rect, width * 0.018f, width * 0.018f, hudPaint);
    }

    private void drawSling(Canvas canvas, GameEngine.RenderState state) {
        Bitmap sling = art.get("game_art/sling.png");
        if (sling != null) {
            drawBitmap(canvas, sling, state.slingX - state.width * 0.05f, state.slingY - state.height * 0.18f, state.width * 0.08f, state.height * 0.22f);
        }
        paint.setColor(Color.parseColor("#6A4B2A"));
        paint.setStrokeWidth(state.width * 0.006f);
        canvas.drawLine(state.slingX - state.width * 0.02f, state.slingY - state.height * 0.1f, state.slingX, state.slingY, paint);
        canvas.drawLine(state.slingX + state.width * 0.02f, state.slingY - state.height * 0.1f, state.slingX, state.slingY, paint);
    }

    private void drawFortress(Canvas canvas, GameEngine.RenderState state) {
        for (int i = 0; i < state.blockCount; i++) {
            GameEngine.Block block = state.blocks[i];
            if (!block.alive) {
                continue;
            }
            Bitmap sprite = pickBlockSprite(block);
            if (sprite != null) {
                drawBitmap(canvas, sprite, block.x, block.y, block.w, block.h);
            } else {
                paint.setColor(colorForMaterial(block.material));
                rect.set(block.x, block.y, block.x + block.w, block.y + block.h);
                canvas.drawRoundRect(rect, state.width * 0.008f, state.width * 0.008f, paint);
            }
            if (block.targetType >= 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(state.width * 0.003f);
                paint.setColor(Color.argb(220, 255, 219, 91));
                rect.set(block.x - 4f, block.y - 4f, block.x + block.w + 4f, block.y + block.h + 4f);
                canvas.drawRoundRect(rect, state.width * 0.01f, state.width * 0.01f, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            float hpRatio = Math.max(0f, block.hp / Math.max(1f, block.maxHp));
            paint.setColor(Color.argb(180, 25, 36, 58));
            rect.set(block.x, block.y - state.height * 0.014f, block.x + block.w, block.y - state.height * 0.007f);
            canvas.drawRoundRect(rect, 6f, 6f, paint);
            paint.setColor(block.targetType >= 0 ? Color.parseColor("#FFD166") : Color.parseColor("#6EF6FF"));
            rect.set(block.x, block.y - state.height * 0.014f, block.x + block.w * hpRatio, block.y - state.height * 0.007f);
            canvas.drawRoundRect(rect, 6f, 6f, paint);
        }
    }

    private void drawTrajectory(Canvas canvas, GameEngine.RenderState state) {
        for (int i = 0; i < state.trajectoryCount; i++) {
            float radius = state.width * 0.004f * (1f - i / (float) state.trajectoryCount);
            canvas.drawCircle(state.trajectoryX[i], state.trajectoryY[i], radius, trajectoryPaint);
        }
    }

    private void drawBird(Canvas canvas, GameEngine.RenderState state) {
        float x = state.birdActive ? state.birdX : state.slingX;
        float y = state.birdActive ? state.birdY : state.slingY;
        Bitmap bird = art.get(birdAssetPath(state.birdKind));
        if (bird != null) {
            drawBitmap(canvas, bird, x - state.birdRadius * 1.25f, y - state.birdRadius * 1.25f, state.birdRadius * 2.5f, state.birdRadius * 2.5f);
        } else {
            paint.setColor(colorForBird(state.birdKind));
            canvas.drawCircle(x, y, state.birdRadius, paint);
        }
        if (!state.birdActive) {
            paint.setColor(Color.argb(120, 255, 255, 255));
            canvas.drawCircle(x, y, state.birdRadius * 1.35f, paint);
        }
    }

    private void drawInFieldHud(Canvas canvas, GameEngine.RenderState state) {
        textPaint.setTextSize(state.width * 0.022f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Current", state.width * 0.07f, state.height * 0.16f, textPaint);
        textPaint.setTextSize(state.width * 0.027f);
        canvas.drawText(state.currentBirdName, state.width * 0.07f, state.height * 0.22f, textPaint);
        textPaint.setTextSize(state.width * 0.02f);
        canvas.drawText("Birds Left " + state.birdsLeft, state.width * 0.07f, state.height * 0.3f, textPaint);
        canvas.drawText(GameDefs.WEATHER_TEXT[state.weather], state.width * 0.81f, state.height * 0.18f, textPaint);
        String detail = state.weather == GameDefs.WEATHER_CROSSWIND ? "Arc bends right"
                : state.weather == GameDefs.WEATHER_UPDRAFT ? "Mid lane lift"
                : state.weather == GameDefs.WEATHER_STORM ? "Lightning drift"
                : state.weather == GameDefs.WEATHER_FREEZE ? "Frozen armor"
                : "Stable launch";
        canvas.drawText(detail, state.width * 0.81f, state.height * 0.24f, textPaint);
        if (state.toastTime > 0f && state.toast.length() > 0) {
            textPaint.setTextSize(state.width * 0.024f);
            float textWidth = textPaint.measureText(state.toast);
            hudPaint.setColor(Color.argb(170, 17, 24, 42));
            rect.set(state.width * 0.5f - textWidth * 0.56f, state.height * 0.08f, state.width * 0.5f + textWidth * 0.56f, state.height * 0.13f);
            canvas.drawRoundRect(rect, state.width * 0.012f, state.width * 0.012f, hudPaint);
            canvas.drawText(state.toast, state.width * 0.5f - textWidth * 0.5f, state.height * 0.114f, textPaint);
        }
    }

    private Bitmap pickBlockSprite(GameEngine.Block block) {
        if (block.targetType == GameDefs.TARGET_CORE) {
            return art.get("game_art/core.png");
        }
        if (block.targetType == GameDefs.TARGET_RELAY) {
            return art.get("game_art/relay.png");
        }
        if (block.targetType == GameDefs.TARGET_BARREL) {
            return art.get("game_art/barrel.png");
        }
        if (block.material == GameDefs.MAT_WOOD) {
            return art.get("game_art/block_wood.png");
        }
        if (block.material == GameDefs.MAT_STONE) {
            return art.get("game_art/block_stone.png");
        }
        if (block.material == GameDefs.MAT_GLASS) {
            return art.get("game_art/block_glass.png");
        }
        return art.get("game_art/block_metal.png");
    }

    private String birdAssetPath(int kind) {
        if (kind == GameDefs.BIRD_EMBER) {
            return "game_art/bird_ember.png";
        }
        if (kind == GameDefs.BIRD_BOLT) {
            return "game_art/bird_bolt.png";
        }
        if (kind == GameDefs.BIRD_FROST) {
            return "game_art/bird_frost.png";
        }
        if (kind == GameDefs.BIRD_RAM) {
            return "game_art/bird_ram.png";
        }
        return "game_art/bird_gust.png";
    }

    private int colorForMaterial(int material) {
        if (material == GameDefs.MAT_WOOD) {
            return Color.parseColor("#A07146");
        }
        if (material == GameDefs.MAT_STONE) {
            return Color.parseColor("#8F9CB6");
        }
        if (material == GameDefs.MAT_GLASS) {
            return Color.parseColor("#98F2FF");
        }
        return Color.parseColor("#6A7389");
    }

    private int colorForBird(int kind) {
        if (kind == GameDefs.BIRD_EMBER) {
            return Color.parseColor("#FF8D3A");
        }
        if (kind == GameDefs.BIRD_BOLT) {
            return Color.parseColor("#F4E663");
        }
        if (kind == GameDefs.BIRD_FROST) {
            return Color.parseColor("#8DE8FF");
        }
        if (kind == GameDefs.BIRD_RAM) {
            return Color.parseColor("#E35B6B");
        }
        return Color.parseColor("#63E7FF");
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, float x, float y, float w, float h) {
        rect.set(x, y, x + w, y + h);
        canvas.drawBitmap(bitmap, null, rect, null);
    }
}
