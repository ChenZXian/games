package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.GameLoopThread;
import com.android.boot.core.GameSession;
import com.android.boot.entity.Convoy;
import com.android.boot.entity.FloatingText;
import com.android.boot.entity.HarborNode;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mapArea = new RectF();
    private final RectF cardRect = new RectF();
    private final float[] positions = new float[2];
    private GameLoopThread loop;
    private GameSession session;
    private UiOverlayController overlay;
    private LinearGradient waterGradient;
    private boolean surfaceReady;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        smallTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void bind(GameSession session, UiOverlayController overlay) {
        this.session = session;
        this.overlay = overlay;
    }

    public void step(float dt) {
        if (session == null) {
            return;
        }
        session.update(dt);
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                drawWorld(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
        if (overlay != null) {
            post(() -> overlay.sync(session));
        }
    }

    public void onHostResume() {
        if (surfaceReady) {
            startLoop();
        }
    }

    public void onHostPause() {
        stopLoop();
    }

    public void setTool(int tool) {
        if (session != null) {
            session.setTool(tool);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || session == null) {
            return true;
        }
        if (event.getY() < mapArea.top || event.getY() > mapArea.bottom) {
            return true;
        }
        int tapped = findTappedHarbor(event.getX(), event.getY());
        session.handleHarborTap(tapped);
        if (overlay != null) {
            overlay.sync(session);
        }
        return true;
    }

    private int findTappedHarbor(float x, float y) {
        for (HarborNode harbor : session.harbors) {
            computeHarborPosition(harbor, positions);
            float radius = harbor.flagship ? dp(26f) : dp(22f);
            float dx = x - positions[0];
            float dy = y - positions[1];
            if (dx * dx + dy * dy <= radius * radius) {
                return harbor.id;
            }
        }
        return -1;
    }

    private void drawWorld(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        mapArea.set(dp(18f), dp(92f), width - dp(18f), height - dp(158f));
        if (waterGradient == null) {
            waterGradient = new LinearGradient(
                    0f,
                    mapArea.top,
                    0f,
                    mapArea.bottom,
                    ContextCompat.getColor(getContext(), R.color.cst_game_water_light),
                    ContextCompat.getColor(getContext(), R.color.cst_game_water_dark),
                    Shader.TileMode.CLAMP
            );
        }
        paint.setShader(null);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(waterGradient);
        canvas.drawRoundRect(mapArea, dp(28f), dp(28f), paint);
        paint.setShader(null);

        drawBaseLinks(canvas);
        drawRoutes(canvas);
        drawConvoys(canvas);
        drawHarbors(canvas, density);
        drawFloatingTexts(canvas, density);
        drawSelectionCard(canvas, density);
    }

    private void drawBaseLinks(Canvas canvas) {
        linePaint.setStrokeWidth(dp(3f));
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_divider));
        linePaint.setAlpha(150);
        for (HarborNode harbor : session.harbors) {
            computeHarborPosition(harbor, positions);
            float startX = positions[0];
            float startY = positions[1];
            for (int linkedId : harbor.links) {
                if (linkedId <= harbor.id) {
                    continue;
                }
                HarborNode linked = session.harbors.get(linkedId);
                float[] linkedPos = new float[2];
                computeHarborPosition(linked, linkedPos);
                canvas.drawLine(startX, startY, linkedPos[0], linkedPos[1], linePaint);
            }
        }
    }

    private void drawRoutes(Canvas canvas) {
        for (HarborNode harbor : session.harbors) {
            if (harbor.routeTargetId < 0 || harbor.routeTargetId >= session.harbors.size()) {
                continue;
            }
            HarborNode target = session.harbors.get(harbor.routeTargetId);
            computeHarborPosition(harbor, positions);
            float startX = positions[0];
            float startY = positions[1];
            float[] targetPos = new float[2];
            computeHarborPosition(target, targetPos);
            linePaint.setStrokeWidth(dp(5f));
            linePaint.setColor(ContextCompat.getColor(getContext(),
                    harbor.owner == HarborNode.OWNER_PLAYER ? R.color.cst_game_route_player : R.color.cst_game_route_enemy));
            linePaint.setAlpha(215);
            canvas.drawLine(startX, startY, targetPos[0], targetPos[1], linePaint);
            drawRouteArrow(canvas, startX, startY, targetPos[0], targetPos[1], harbor.owner == HarborNode.OWNER_PLAYER);
        }
        if (session.pendingRouteSourceId >= 0 && session.pendingRouteSourceId < session.harbors.size()) {
            HarborNode harbor = session.harbors.get(session.pendingRouteSourceId);
            computeHarborPosition(harbor, positions);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(dp(3f));
            glowPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
            glowPaint.setAlpha(220);
            canvas.drawCircle(positions[0], positions[1], dp(30f), glowPaint);
        }
    }

    private void drawRouteArrow(Canvas canvas, float startX, float startY, float endX, float endY, boolean playerRoute) {
        float dx = endX - startX;
        float dy = endY - startY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 1f) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        float arrowX = endX - ux * dp(26f);
        float arrowY = endY - uy * dp(26f);
        float wing = dp(7f);
        linePaint.setStrokeWidth(dp(4f));
        linePaint.setColor(ContextCompat.getColor(getContext(), playerRoute ? R.color.cst_text_on_primary : R.color.cst_text_primary));
        canvas.drawLine(arrowX, arrowY, arrowX - ux * dp(14f) + uy * wing, arrowY - uy * dp(14f) - ux * wing, linePaint);
        canvas.drawLine(arrowX, arrowY, arrowX - ux * dp(14f) - uy * wing, arrowY - uy * dp(14f) + ux * wing, linePaint);
    }

    private void drawConvoys(Canvas canvas) {
        for (Convoy convoy : session.convoys) {
            HarborNode source = session.harbors.get(convoy.sourceId);
            HarborNode target = session.harbors.get(convoy.targetId);
            computeHarborPosition(source, positions);
            float sx = positions[0];
            float sy = positions[1];
            float[] targetPos = new float[2];
            computeHarborPosition(target, targetPos);
            float cx = sx + (targetPos[0] - sx) * convoy.progress;
            float cy = sy + (targetPos[1] - sy) * convoy.progress;
            float radius = dp(9f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ContextCompat.getColor(getContext(),
                    convoy.owner == HarborNode.OWNER_PLAYER ? R.color.cst_game_convoy_player : R.color.cst_game_convoy_enemy));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
            canvas.drawRect(cx - radius * 0.55f, cy - radius * 0.45f, cx + radius * 0.55f, cy + radius * 0.45f, paint);
            linePaint.setStrokeWidth(dp(2f));
            linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            canvas.drawLine(cx - radius * 0.55f, cy - radius * 0.1f, cx + radius * 0.55f, cy - radius * 0.1f, linePaint);
        }
    }

    private void drawHarbors(Canvas canvas, float density) {
        for (HarborNode harbor : session.harbors) {
            computeHarborPosition(harbor, positions);
            float cx = positions[0];
            float cy = positions[1];
            float radius = harbor.flagship ? dp(24f) : dp(20f);
            int fillColor;
            if (harbor.owner == HarborNode.OWNER_PLAYER) {
                fillColor = ContextCompat.getColor(getContext(), R.color.cst_game_player);
            } else if (harbor.owner == HarborNode.OWNER_ENEMY) {
                fillColor = ContextCompat.getColor(getContext(), R.color.cst_game_enemy);
            } else {
                fillColor = ContextCompat.getColor(getContext(), R.color.cst_game_neutral);
            }

            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setColor(fillColor);
            glowPaint.setAlpha(65);
            canvas.drawCircle(cx, cy, radius + dp(8f), glowPaint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fillColor);
            canvas.drawCircle(cx, cy, radius, paint);

            linePaint.setStrokeWidth(dp(2f));
            linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            linePaint.setAlpha(235);
            canvas.drawCircle(cx, cy, radius, linePaint);

            if (harbor.flagship) {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
                textPaint.setTextSize(15f * density);
                canvas.drawText("*", cx, cy + dp(5f), textPaint);
            } else {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_on_primary));
                textPaint.setTextSize(12f * density);
                canvas.drawText(String.valueOf((int) Math.max(0f, harbor.stock)), cx, cy + dp(4f), textPaint);
            }

            float barWidth = radius * 2.3f;
            float stockRatio = harbor.stock / harbor.maxStock();
            float defenseRatio = harbor.defense / harbor.effectiveDefenseCap();

            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_track));
            canvas.drawRoundRect(cx - barWidth * 0.5f, cy + radius + dp(8f), cx + barWidth * 0.5f, cy + radius + dp(14f), dp(3f), dp(3f), paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_stock));
            canvas.drawRoundRect(cx - barWidth * 0.5f, cy + radius + dp(8f), cx - barWidth * 0.5f + barWidth * stockRatio, cy + radius + dp(14f), dp(3f), dp(3f), paint);

            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_track));
            canvas.drawRoundRect(cx - barWidth * 0.5f, cy + radius + dp(18f), cx + barWidth * 0.5f, cy + radius + dp(24f), dp(3f), dp(3f), paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_defense));
            canvas.drawRoundRect(cx - barWidth * 0.5f, cy + radius + dp(18f), cx - barWidth * 0.5f + barWidth * defenseRatio, cy + radius + dp(24f), dp(3f), dp(3f), paint);

            smallTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            smallTextPaint.setTextSize(11f * density);
            canvas.drawText(harbor.name, cx, cy - radius - dp(8f), smallTextPaint);

            if (session.selectedHarborId == harbor.id) {
                linePaint.setStrokeWidth(dp(3f));
                linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_game_selection));
                linePaint.setAlpha(230);
                canvas.drawCircle(cx, cy, radius + dp(6f), linePaint);
            }
        }
    }

    private void drawFloatingTexts(Canvas canvas, float density) {
        textPaint.setTextSize(12f * density);
        for (FloatingText text : session.texts) {
            if (text.life <= 0f) {
                continue;
            }
            float alpha = Math.max(0f, Math.min(1f, text.life));
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            textPaint.setAlpha((int) (255 * alpha));
            float x = mapArea.left + text.x * mapArea.width();
            float y = mapArea.top + text.y * mapArea.height();
            canvas.drawText(text.text, x, y, textPaint);
        }
        textPaint.setAlpha(255);
    }

    private void drawSelectionCard(Canvas canvas, float density) {
        if (session.selectedHarborId < 0 || session.selectedHarborId >= session.harbors.size()) {
            return;
        }
        HarborNode harbor = session.harbors.get(session.selectedHarborId);
        float centerX = getWidth() * 0.5f;
        float bottom = mapArea.bottom - dp(10f);
        cardRect.set(centerX - dp(150f), bottom - dp(92f), centerX + dp(150f), bottom);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        paint.setAlpha(240);
        canvas.drawRoundRect(cardRect, dp(18f), dp(18f), paint);
        linePaint.setStrokeWidth(dp(2f));
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
        linePaint.setAlpha(240);
        canvas.drawRoundRect(cardRect, dp(18f), dp(18f), linePaint);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        textPaint.setTextSize(18f * density);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(harbor.name, cardRect.left + dp(16f), cardRect.top + dp(28f), textPaint);

        smallTextPaint.setTextAlign(Paint.Align.LEFT);
        smallTextPaint.setTextSize(12f * density);
        smallTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        String ownerText = harbor.owner == HarborNode.OWNER_PLAYER ? "Player" : harbor.owner == HarborNode.OWNER_ENEMY ? "Enemy" : "Neutral";
        canvas.drawText("Owner " + ownerText, cardRect.left + dp(16f), cardRect.top + dp(50f), smallTextPaint);
        canvas.drawText("Stock " + (int) harbor.stock + "/" + (int) harbor.maxStock(), cardRect.left + dp(16f), cardRect.top + dp(68f), smallTextPaint);
        canvas.drawText("Defense " + (int) harbor.defense + "/" + (int) harbor.effectiveDefenseCap(), cardRect.left + dp(16f), cardRect.top + dp(86f), smallTextPaint);

        String routeText = "Route idle";
        if (harbor.routeTargetId >= 0 && harbor.routeTargetId < session.harbors.size()) {
            routeText = "Route " + session.harbors.get(harbor.routeTargetId).name;
        }
        smallTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(routeText, cardRect.right - dp(16f), cardRect.top + dp(50f), smallTextPaint);
        canvas.drawText("Upgrades D" + harbor.dockLevel + " H" + harbor.armorLevel + " C" + harbor.cannonLevel,
                cardRect.right - dp(16f), cardRect.top + dp(68f), smallTextPaint);
        String toolText = session.activeTool == GameSession.TOOL_ROUTE ? "Tap linked harbor to route"
                : session.activeTool == GameSession.TOOL_SURGE ? "Tap owned harbor to surge"
                : "Tap owned harbor to upgrade";
        canvas.drawText(toolText, cardRect.right - dp(16f), cardRect.top + dp(86f), smallTextPaint);
    }

    private void computeHarborPosition(HarborNode harbor, float[] out) {
        out[0] = mapArea.left + harbor.x * mapArea.width();
        out[1] = mapArea.top + harbor.y * mapArea.height();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void startLoop() {
        if (loop != null) {
            return;
        }
        loop = new GameLoopThread(this);
        loop.setRunning(true);
        loop.start();
    }

    private void stopLoop() {
        if (loop == null) {
            return;
        }
        loop.setRunning(false);
        boolean waiting = true;
        while (waiting) {
            try {
                loop.join();
                waiting = false;
            } catch (InterruptedException ignored) {
            }
        }
        loop = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = true;
        waterGradient = null;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }
}
