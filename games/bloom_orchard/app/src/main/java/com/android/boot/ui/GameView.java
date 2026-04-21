package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    private GameLoopThread loop;
    private GameSession session;
    private UiOverlayController overlay;
    private ParticleSystem particles;
    private int selectedTool;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void bind(GameSession session, UiOverlayController overlay) {
        this.session = session;
        this.overlay = overlay;
        this.particles = new ParticleSystem();
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
        float w = getWidth() / (float) cols;
        float h = (getHeight() - 220f) / rows;
        for (int i = 0; i < session.plots.size(); i++) {
            CropPlot p = session.plots.get(i);
            int c = i % cols;
            int r = i / cols;
            float x = c * w;
            float y = 120f + r * h;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.unlocked ? ContextCompat.getColor(getContext(), R.color.cst_success) : ContextCompat.getColor(getContext(), R.color.cst_text_muted));
            canvas.drawRoundRect(x + 6, y + 6, x + w - 6, y + h - 6, 16, 16, paint);
            if (p.crop != null) {
                drawCrop(canvas, p, x + w * 0.5f, y + h * 0.5f, Math.min(w, h) * 0.24f);
            }
        }
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
        particles.draw(canvas, paint);
        paint.setTextSize(34f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        canvas.drawText("Weather " + session.weather.state.name() + " Beauty " + session.bestBeauty, 20, 90, paint);
        for (int i = 0; i < session.texts.size(); i++) {
            if (session.texts.get(i).life > 0f) {
                paint.setAlpha((int) (255 * session.texts.get(i).life));
                canvas.drawText(session.texts.get(i).text, session.texts.get(i).x, session.texts.get(i).y, paint);
            }
        }
        paint.setAlpha(255);
    }

    private void drawCrop(Canvas canvas, CropPlot p, float cx, float cy, float rad) {
        CropType t = p.crop;
        int stage = (int) (p.growth * 3.99f);
        paint.setColor(t.stageColors[Math.min(stage, 3)]);
        if ("flower".equals(t.shapeStyle)) {
            for (int i = 0; i < 6; i++) {
                float a = (float) (i * Math.PI / 3f + p.sway * 0.3f);
                canvas.drawCircle(cx + (float) Math.cos(a) * rad * 0.7f, cy + (float) Math.sin(a) * rad * 0.7f, rad * (0.35f + 0.08f * stage), paint);
            }
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(cx, cy, rad * 0.35f, paint);
        } else {
            canvas.drawCircle(cx, cy, rad * (0.5f + 0.15f * stage), paint);
            paint.setColor(t.stageColors[1]);
            canvas.drawRect(cx - rad * 0.1f, cy - rad * 1.1f, cx + rad * 0.1f, cy, paint);
        }
        if (p.mature) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
            canvas.drawCircle(cx + rad * 0.25f, cy - rad * 0.2f, rad * 0.15f, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || session == null) return true;
        if (session.state != com.android.boot.core.GameState.PLAYING) {
            session.start();
            return true;
        }
        int cols = 6;
        int rows = 6;
        float w = getWidth() / (float) cols;
        float h = (getHeight() - 220f) / rows;
        int col = (int) (event.getX() / w);
        int row = (int) ((event.getY() - 120f) / h);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true;
        int idx = row * cols + col;
        CropPlot p = session.plots.get(idx);
        if (!p.unlocked) return true;
        if (p.isEmpty()) {
            session.plant(p, session.cropCatalog.firstUnlocked(session.progression.level));
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
