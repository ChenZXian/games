package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.core.GameLoopThread;
import com.android.boot.core.GameSession;
import com.android.boot.entity.CropPlot;
import com.android.boot.entity.CropType;
import com.android.boot.fx.ParticleSystem;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private GameLoopThread loop;
    private GameSession session;
    private UiOverlayController overlay;
    private ParticleSystem particles;
    private int selectedTool;
    private Bitmap farmlandTile;
    private Bitmap flowerAccent;
    private Bitmap cropAccent;
    private Bitmap fenceAccent;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void bind(GameSession session, UiOverlayController overlay) {
        this.session = session;
        this.overlay = overlay;
        this.particles = new ParticleSystem();
        loadArt();
    }

    public void step(float dt) {
        if (session == null) return;
        session.update(dt);
        particles.update(dt);
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) drawWorld(canvas);
        } finally {
            if (canvas != null) getHolder().unlockCanvasAndPost(canvas);
        }
        post(() -> overlay.sync(session));
    }

    private void drawWorld(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_alt));
        int cols = 6;
        int rows = 6;
        float playLeft = dp(112f);
        float playTop = dp(146f);
        float playRight = getWidth() - dp(20f);
        float playBottom = getHeight() - dp(28f);
        float w = (playRight - playLeft) / cols;
        float h = (playBottom - playTop) / rows;
        drawBackdrop(canvas, playLeft, playTop, playRight, playBottom, h);
        for (int i = 0; i < session.plots.size(); i++) {
            CropPlot p = session.plots.get(i);
            int c = i % cols;
            int r = i / cols;
            float x = playLeft + c * w;
            float y = playTop + r * h;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.unlocked ? ContextCompat.getColor(getContext(), R.color.cst_success) : ContextCompat.getColor(getContext(), R.color.cst_text_muted));
            drawPlot(canvas, p, x, y, w, h);
            if (p.crop != null) {
                drawCrop(canvas, p, x + w * 0.5f, y + h * 0.56f, Math.min(w, h) * 0.23f);
            }
        }
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        particles.draw(canvas, paint);
        paint.setTextSize(34f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        canvas.drawText("Weather " + session.weather.state.name() + "  Best Bloom " + session.bestBeauty, playLeft, playTop - dp(26f), paint);
        for (int i = 0; i < session.texts.size(); i++) {
            if (session.texts.get(i).life > 0f) {
                paint.setAlpha((int) (255 * session.texts.get(i).life));
                canvas.drawText(session.texts.get(i).text, session.texts.get(i).x, session.texts.get(i).y, paint);
            }
        }
        paint.setAlpha(255);
    }

    private void drawBackdrop(Canvas canvas, float playLeft, float playTop, float playRight, float playBottom, float plotHeight) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFF8EC);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(0xFFE8F8F1);
        canvas.drawRoundRect(playLeft - dp(14f), playTop - dp(12f), playRight + dp(10f), playBottom + dp(12f), dp(28f), dp(28f), paint);
        paint.setColor(0xFFFDE9F1);
        canvas.drawCircle(getWidth() * 0.12f, getHeight() * 0.22f, 72f, paint);
        canvas.drawCircle(getWidth() * 0.86f, getHeight() * 0.28f, 56f, paint);
        if (fenceAccent != null) {
            drawRect.set(playLeft - dp(12f), playBottom - plotHeight * 0.2f, playLeft + dp(72f), playBottom + dp(54f));
            canvas.drawBitmap(fenceAccent, null, drawRect, paint);
            drawRect.set(playRight - dp(72f), playBottom - plotHeight * 0.2f, playRight + dp(12f), playBottom + dp(54f));
            canvas.drawBitmap(fenceAccent, null, drawRect, paint);
        }
    }

    private void drawPlot(Canvas canvas, CropPlot plot, float x, float y, float w, float h) {
        float left = x + 10f;
        float top = y + 10f;
        float right = x + w - 10f;
        float bottom = y + h - 10f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(plot.unlocked ? 0xFFFFF7E5 : 0xFFE2D9D9);
        canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, paint);
        if (farmlandTile != null && plot.unlocked) {
            drawRect.set(left + 6f, top + 10f, right - 6f, bottom - 8f);
            canvas.drawBitmap(farmlandTile, null, drawRect, paint);
        } else {
            paint.setColor(plot.unlocked ? 0xFFCFA56D : 0xFFB6A4A4);
            canvas.drawRoundRect(left + 8f, top + 14f, right - 8f, bottom - 10f, 18f, 18f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(plot.unlocked ? 0xFFD89CBC : 0xFFBCAFB0);
        canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCrop(Canvas canvas, CropPlot p, float cx, float cy, float rad) {
        CropType t = p.crop;
        int stage = (int) (p.growth * 3.99f);
        int stageIndex = Math.min(stage, 3);
        paint.setColor(t.stageColors[stageIndex]);
        if ("flower".equals(t.shapeStyle) && flowerAccent != null && stage >= 2) {
            drawRect.set(cx - rad * 2.1f, cy - rad * 2.3f, cx + rad * 2.1f, cy + rad * 1.8f);
            canvas.drawBitmap(flowerAccent, null, drawRect, paint);
        } else if (!"flower".equals(t.shapeStyle) && cropAccent != null && stage >= 2) {
            drawRect.set(cx - rad * 2.0f, cy - rad * 2.2f, cx + rad * 2.0f, cy + rad * 1.8f);
            canvas.drawBitmap(cropAccent, null, drawRect, paint);
        }
        if ("flower".equals(t.shapeStyle)) {
            for (int i = 0; i < 6; i++) {
                float a = (float) (i * Math.PI / 3f + p.sway * 0.3f);
                canvas.drawCircle(cx + (float) Math.cos(a) * rad * 0.72f, cy + (float) Math.sin(a) * rad * 0.72f, rad * (0.36f + 0.08f * stage), paint);
            }
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(cx, cy, rad * 0.35f, paint);
            paint.setColor(0xFF58B865);
            canvas.drawRect(cx - rad * 0.1f, cy, cx + rad * 0.1f, cy + rad * 1.2f, paint);
        } else {
            canvas.drawCircle(cx, cy, rad * (0.5f + 0.15f * stage), paint);
            paint.setColor(t.stageColors[1]);
            canvas.drawRect(cx - rad * 0.1f, cy - rad * 1.1f, cx + rad * 0.1f, cy, paint);
        }
        if (p.mature) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
            canvas.drawCircle(cx + rad * 0.25f, cy - rad * 0.2f, rad * 0.15f, paint);
        }
        if (p.watered > 0) {
            paint.setColor(0x6630CFFF);
            canvas.drawCircle(cx - rad * 1.1f, cy - rad * 1.2f, rad * 0.22f, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || session == null) return true;
        if (session.state != com.android.boot.core.GameState.PLAYING) return true;
        int cols = 6;
        int rows = 6;
        float playLeft = dp(112f);
        float playTop = dp(146f);
        float playRight = getWidth() - dp(20f);
        float playBottom = getHeight() - dp(28f);
        float w = (playRight - playLeft) / cols;
        float h = (playBottom - playTop) / rows;
        int col = (int) ((event.getX() - playLeft) / w);
        int row = (int) ((event.getY() - playTop) / h);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true;
        int idx = row * cols + col;
        CropPlot p = session.plots.get(idx);
        if (!p.unlocked) return true;
        if (p.isEmpty()) {
            session.plant(p, session.cropCatalog.pickForPlot(session.progression.level, p.index));
        } else if (p.mature) {
            session.harvest(p);
            particles.burst(event.getX(), event.getY());
        } else if (selectedTool == 1) {
            session.water(p);
        } else if (selectedTool == 2) {
            session.fertilizer(p, "BEAUTY");
        }
        return true;
    }

    public void setTool(int tool) {
        selectedTool = tool;
    }

    private void loadArt() {
        try {
            AssetManager assets = getContext().getAssets();
            farmlandTile = decodeAsset(assets, "game_art/kenney_isometric_miniature_farm/dirtFarmland_S.png");
            flowerAccent = decodeAsset(assets, "game_art/kenney_isometric_miniature_farm/cornYoungDouble_S.png");
            cropAccent = decodeAsset(assets, "game_art/kenney_isometric_miniature_farm/corn_S.png");
            fenceAccent = decodeAsset(assets, "game_art/kenney_isometric_miniature_farm/fenceLow_S.png");
        } catch (Exception ignored) {
            farmlandTile = null;
            flowerAccent = null;
            cropAccent = null;
            fenceAccent = null;
        }
    }

    private Bitmap decodeAsset(AssetManager assets, String path) throws Exception {
        return BitmapFactory.decodeStream(assets.open(path));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        loop = new GameLoopThread(this);
        loop.setRunning(true);
        loop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (loop != null) {
            loop.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    loop.join();
                    retry = false;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
