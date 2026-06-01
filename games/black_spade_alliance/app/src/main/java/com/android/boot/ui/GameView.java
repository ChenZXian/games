package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.ToneFx;
import com.android.boot.engine.GameEngine;
import com.android.boot.engine.GameEngine.Card;
import com.android.boot.model.GameSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface UiCallbacks {
        void onSnapshot(GameSnapshot snapshot);
    }

    private final SurfaceHolder surfaceHolder;
    private final ToneFx toneFx;
    private final GameEngine engine;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Bitmap feltBadge;
    private Bitmap leadToken;
    private Bitmap allianceSeal;
    private Thread loopThread;
    private boolean running;
    private boolean surfaceReady;
    private long lastFrameNs;
    private long lastUiPushNs;
    private UiCallbacks callbacks;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        toneFx = new ToneFx();
        engine = new GameEngine(context);
        engine.setAudio(toneFx);
        loadAssets(context.getAssets());
        textPaint.setColor(Color.WHITE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);
        outlinePaint.setColor(Color.argb(255, 176, 223, 255));
        setFocusable(true);
    }

    public void setUiCallbacks(UiCallbacks callbacks) {
        this.callbacks = callbacks;
        pushSnapshot();
    }

    public GameEngine getEngine() {
        return engine;
    }

    public void onResumeView() {
        startLoop();
    }

    public void onPauseView() {
        stopLoop();
        engine.pause();
        pushSnapshot();
    }

    public void release() {
        stopLoop();
        toneFx.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            engine.onPlayerTap(event.getX(), event.getY(), getWidth(), getHeight());
            pushSnapshot();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }

    @Override
    public void run() {
        lastFrameNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastFrameNs) / 1000000000f;
            lastFrameNs = now;
            engine.update(dt);
            drawFrame();
            if (now - lastUiPushNs > 70000000L) {
                lastUiPushNs = now;
                post(this::pushSnapshot);
            }
        }
    }

    private void loadAssets(AssetManager assetManager) {
        feltBadge = loadBitmap(assetManager, "game_art/black_spade_table_art/felt_badge.png");
        leadToken = loadBitmap(assetManager, "game_art/black_spade_table_art/lead_token.png");
        allianceSeal = loadBitmap(assetManager, "game_art/black_spade_table_art/alliance_seal.png");
    }

    private Bitmap loadBitmap(AssetManager assetManager, String path) {
        try {
            InputStream inputStream = assetManager.open(path);
            try {
                return BitmapFactory.decodeStream(inputStream);
            } finally {
                inputStream.close();
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    private void drawFrame() {
        if (!surfaceReady) {
            return;
        }
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            render(canvas, getWidth(), getHeight());
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void render(Canvas canvas, int width, int height) {
        drawBackground(canvas, width, height);
        drawSeatArea(canvas, width, height);
        drawCenterPlay(canvas, width, height);
        drawPlayerHand(canvas, width, height);
    }

    private void drawBackground(Canvas canvas, int width, int height) {
        paint.setShader(new LinearGradient(0f, 0f, 0f, height, Color.rgb(6, 18, 26), Color.rgb(10, 52, 36), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(new RadialGradient(width * 0.5f, height * 0.5f, Math.min(width, height) * 0.42f, Color.argb(180, 42, 122, 76), Color.argb(0, 42, 122, 76), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);
        if (feltBadge != null) {
            rect.set(width * 0.18f, height * 0.14f, width * 0.82f, height * 0.78f);
            canvas.drawBitmap(feltBadge, null, rect, null);
        }
    }

    private void drawSeatArea(Canvas canvas, int width, int height) {
        drawSeatChip(canvas, width * 0.5f, height * 0.11f, 2);
        drawSeatChip(canvas, width * 0.12f, height * 0.45f, 1);
        drawSeatChip(canvas, width * 0.88f, height * 0.45f, 3);
        drawSeatChip(canvas, width * 0.5f, height * 0.79f, 0);
    }

    private void drawSeatChip(Canvas canvas, float cx, float cy, int seat) {
        chipPaint.setColor(teamColor(engine.getTeamForSeat(seat)));
        rect.set(cx - 78f, cy - 38f, cx + 78f, cy + 38f);
        canvas.drawRoundRect(rect, 20f, 20f, chipPaint);
        outlinePaint.setColor(seat == engine.getCurrentSeat() ? Color.argb(255, 255, 214, 92) : Color.argb(160, 186, 222, 255));
        canvas.drawRoundRect(rect, 20f, 20f, outlinePaint);
        if (engine.getLiveSeat() == seat && leadToken != null) {
            rect.set(cx - 18f, cy - 62f, cx + 18f, cy - 26f);
            canvas.drawBitmap(leadToken, null, rect, null);
        }
        if (engine.getTeamForSeat(seat) == engine.getTeamForSeat(0) && seat != 0 && allianceSeal != null) {
            rect.set(cx + 48f, cy - 54f, cx + 86f, cy - 16f);
            canvas.drawBitmap(allianceSeal, null, rect, null);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(26f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(engine.getSeatLabel(seat), cx, cy - 4f, textPaint);
        textPaint.setTextSize(20f);
        String status = engine.isSeatFinished(seat) ? "Out" : engine.getSeatCardCount(seat) + " cards";
        canvas.drawText(status, cx, cy + 22f, textPaint);
    }

    private void drawCenterPlay(Canvas canvas, int width, int height) {
        rect.set(width * 0.31f, height * 0.24f, width * 0.69f, height * 0.58f);
        paint.setColor(Color.argb(170, 10, 28, 44));
        canvas.drawRoundRect(rect, 28f, 28f, paint);
        outlinePaint.setColor(Color.argb(180, 94, 184, 222));
        canvas.drawRoundRect(rect, 28f, 28f, outlinePaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.argb(255, 240, 247, 255));
        textPaint.setTextSize(26f);
        canvas.drawText(engine.getPlayLabel(), rect.centerX(), rect.top + 34f, textPaint);
        List<Card> liveCards = engine.getLiveCards();
        if (liveCards.isEmpty()) {
            textPaint.setTextSize(22f);
            textPaint.setColor(Color.argb(255, 182, 214, 233));
            canvas.drawText("Play any legal set", rect.centerX(), rect.centerY() + 10f, textPaint);
            return;
        }
        float cardWidth = Math.min(width * 0.075f, 96f);
        float cardHeight = cardWidth * 1.42f;
        float overlap = cardWidth * 0.4f;
        float totalWidth = cardWidth + overlap * Math.max(0, liveCards.size() - 1);
        float startX = rect.centerX() - totalWidth * 0.5f;
        float y = rect.centerY() - cardHeight * 0.35f;
        for (int i = 0; i < liveCards.size(); i++) {
            drawCard(canvas, liveCards.get(i), startX + overlap * i, y, cardWidth, cardHeight, true, false);
        }
    }

    private void drawPlayerHand(Canvas canvas, int width, int height) {
        List<Card> hand = engine.getPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            engine.getPlayerCardRect(i, hand.size(), width, height, engine.isSelected(i), rect);
            drawCard(canvas, hand.get(i), rect.left, rect.top, rect.width(), rect.height(), true, engine.isSelected(i));
        }
    }

    private void drawCard(Canvas canvas, Card card, float left, float top, float width, float height, boolean faceUp, boolean selected) {
        rect.set(left, top, left + width, top + height);
        paint.setColor(faceUp ? Color.argb(255, 246, 245, 239) : Color.argb(255, 34, 56, 92));
        canvas.drawRoundRect(rect, 18f, 18f, paint);
        outlinePaint.setColor(selected ? Color.argb(255, 255, 220, 110) : Color.argb(255, 26, 45, 70));
        canvas.drawRoundRect(rect, 18f, 18f, outlinePaint);
        if (!faceUp) {
            paint.setColor(Color.argb(180, 104, 166, 255));
            rect.inset(12f, 16f);
            canvas.drawRoundRect(rect, 14f, 14f, paint);
            return;
        }
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(width * 0.19f);
        textPaint.setColor(card.suit == 1 || card.suit == 2 ? Color.rgb(188, 24, 53) : Color.rgb(12, 22, 40));
        String label = engine.getCardLabel(card);
        canvas.drawText(label, left + 12f, top + 26f, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(width * 0.3f);
        canvas.drawText(label.substring(0, label.length() - 1), left + width * 0.5f, top + height * 0.58f, textPaint);
        textPaint.setTextSize(width * 0.24f);
        canvas.drawText(label.substring(label.length() - 1), left + width * 0.5f, top + height * 0.78f, textPaint);
    }

    private int teamColor(int team) {
        if (engine.getSoloSeat() >= 0 && team == engine.getTeamForSeat(engine.getSoloSeat())) {
            return Color.argb(220, 110, 58, 24);
        }
        if (team == 1) {
            return Color.argb(220, 32, 74, 88);
        }
        return Color.argb(220, 56, 44, 84);
    }

    private void startLoop() {
        if (running || !surfaceReady) {
            return;
        }
        if (GameEngine.STATE_PAUSED.equals(engine.getSnapshot().state)) {
            engine.resume();
        }
        running = true;
        loopThread = new Thread(this, "BlackSpadeAllianceLoop");
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

    private void pushSnapshot() {
        if (callbacks != null) {
            callbacks.onSnapshot(engine.getSnapshot());
        }
    }
}
