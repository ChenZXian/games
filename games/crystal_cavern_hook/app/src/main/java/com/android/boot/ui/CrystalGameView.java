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

import com.android.boot.core.CrystalAudio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CrystalGameView extends View {
    public static final int TOOL_DYNAMITE = 0;
    public static final int TOOL_MAGNET = 1;
    public static final int TOOL_BOOST = 2;
    public static final int TOOL_LENS = 3;
    public static final int TOOL_SHIELD = 4;
    private static final int MODE_MENU = 0;
    private static final int MODE_PLAYING = 1;
    private static final int MODE_PAUSED = 2;
    private static final int MODE_GAME_OVER = 3;
    private static final int HOOK_SWING = 0;
    private static final int HOOK_OUT = 1;
    private static final int HOOK_BACK = 2;
    private static final int TYPE_GOLD = 0;
    private static final int TYPE_EMERALD = 1;
    private static final int TYPE_AMETHYST = 2;
    private static final int TYPE_RELIC = 3;
    private static final int TYPE_BOMB = 4;
    private static final int TYPE_SHARD = 5;
    private static final int TYPE_BAT = 6;
    private static final float SWING_LIMIT = 82f;
    private static final float SWING_ARM_LENGTH = 86f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Item> items = new ArrayList<>();
    private final Random random = new Random(42);
    private GameListener listener;
    private CrystalAudio audio;
    private int mode = MODE_MENU;
    private int hookState = HOOK_SWING;
    private int stage = 1;
    private int score;
    private int grabbedIndex = -1;
    private int orderA;
    private int orderB;
    private int orderNeedA;
    private int orderNeedB;
    private int orderHaveA;
    private int orderHaveB;
    private int orderScore;
    private final int[] tools = new int[5];
    private float timer;
    private float angle;
    private float swingDir = 1f;
    private float hookX;
    private float hookY;
    private float hookVx;
    private float hookVy;
    private float anchorX;
    private float anchorY;
    private float magnetTime;
    private float boostTime;
    private float lensTime;
    private float shieldTime;
    private float flashTime;
    private long lastTime;
    private boolean prismBounced;
    private String toast = "Tap to launch";

    public CrystalGameView(Context context) {
        super(context);
        setup();
    }

    public CrystalGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    public void setAudio(CrystalAudio audio) {
        this.audio = audio;
    }

    public boolean isPlaying() {
        return mode == MODE_PLAYING;
    }

    public void startShift() {
        mode = MODE_PLAYING;
        stage = 1;
        score = 0;
        tools[TOOL_DYNAMITE] = 2;
        tools[TOOL_MAGNET] = 2;
        tools[TOOL_BOOST] = 2;
        tools[TOOL_LENS] = 2;
        tools[TOOL_SHIELD] = 1;
        setupStage();
        lastTime = System.nanoTime();
        updateHud();
        invalidate();
    }

    public void stopToMenu() {
        mode = MODE_MENU;
        toast = "Tap Start";
        invalidate();
    }

    public void pauseGame() {
        if (mode == MODE_PLAYING) {
            mode = MODE_PAUSED;
        }
    }

    public void resumeGame() {
        if (mode == MODE_PAUSED) {
            mode = MODE_PLAYING;
            lastTime = System.nanoTime();
            invalidate();
        }
    }

    public void shutdown() {
        mode = MODE_PAUSED;
    }

    public int getToolCount(int tool) {
        if (tool < 0 || tool >= tools.length) {
            return 0;
        }
        return tools[tool];
    }

    public boolean useTool(int tool) {
        if (mode != MODE_PLAYING || tool < 0 || tool >= tools.length || tools[tool] <= 0) {
            return false;
        }
        tools[tool]--;
        if (tool == TOOL_DYNAMITE) {
            removeNearestHazard();
            if (audio != null) {
                audio.playTool();
            }
        } else if (tool == TOOL_MAGNET) {
            magnetTime = 8f;
            toast = "Magnet pulse active";
            if (audio != null) {
                audio.playTool();
            }
        } else if (tool == TOOL_BOOST) {
            boostTime = 6f;
            toast = "Cable boost active";
            if (audio != null) {
                audio.playTool();
            }
        } else if (tool == TOOL_LENS) {
            lensTime = 9f;
            toast = "Prism lens active";
            if (audio != null) {
                audio.playTool();
            }
        } else if (tool == TOOL_SHIELD) {
            shieldTime = 10f;
            toast = "Shield glove active";
            if (audio != null) {
                audio.playTool();
            }
        }
        flashTime = 0.45f;
        updateHud();
        invalidate();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mode == MODE_PLAYING && hookState == HOOK_SWING) {
            launchHook();
            return true;
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        anchorX = w * 0.5f;
        anchorY = h * 0.08f;
        hookX = anchorX;
        hookY = anchorY + 36f;
        if (items.isEmpty() && w > 0 && h > 0) {
            setupStage();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = lastTime == 0L ? 0f : Math.min(0.033f, (now - lastTime) / 1000000000f);
        lastTime = now;
        if (mode == MODE_PLAYING) {
            update(dt);
        }
        drawScene(canvas);
        if (mode == MODE_MENU) {
            drawCenterHint(canvas, "Crystal orders wait below");
        } else if (mode == MODE_PAUSED) {
            drawCenterHint(canvas, "Paused");
        } else if (mode == MODE_GAME_OVER) {
            drawCenterHint(canvas, "Shift ended");
        }
        if (mode == MODE_PLAYING) {
            postInvalidateOnAnimation();
        }
    }

    private void setup() {
        setFocusable(true);
        textPaint.setColor(Color.rgb(24, 48, 68));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(28f);
    }

    private void setupStage() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        items.clear();
        random.setSeed(900L + stage * 73L);
        timer = 82f + stage * 8f;
        orderA = stage % 2 == 0 ? TYPE_AMETHYST : TYPE_EMERALD;
        orderB = stage % 3 == 0 ? TYPE_RELIC : TYPE_GOLD;
        orderNeedA = 2 + stage / 2;
        orderNeedB = 3 + stage / 2;
        orderHaveA = 0;
        orderHaveB = 0;
        orderScore = 480 + stage * 180;
        magnetTime = 0f;
        boostTime = 0f;
        lensTime = 0f;
        shieldTime = 0f;
        resetHook();
        int treasureCount = 16 + stage * 3;
        for (int i = 0; i < treasureCount; i++) {
            int type;
            int roll = random.nextInt(100);
            if (roll < 28) {
                type = TYPE_GOLD;
            } else if (roll < 50) {
                type = TYPE_EMERALD;
            } else if (roll < 70) {
                type = TYPE_AMETHYST;
            } else if (roll < 80) {
                type = TYPE_RELIC;
            } else if (roll < 89) {
                type = TYPE_SHARD;
            } else if (roll < 95) {
                type = TYPE_BOMB;
            } else {
                type = TYPE_BAT;
            }
            float x = w * (0.06f + random.nextFloat() * 0.88f);
            float y = h * (0.24f + random.nextFloat() * 0.68f);
            float size = 24f + random.nextFloat() * 30f;
            items.add(new Item(type, x, y, size));
        }
        toast = "Tap to launch";
        updateHud();
    }

    private void resetHook() {
        hookState = HOOK_SWING;
        grabbedIndex = -1;
        prismBounced = false;
        anchorX = getWidth() * 0.5f;
        anchorY = getHeight() * 0.08f;
        hookX = anchorX;
        hookY = anchorY + 46f;
        hookVx = 0f;
        hookVy = 0f;
    }

    private void launchHook() {
        float speed = boostTime > 0f ? 760f : 560f;
        double radians = Math.toRadians(angle + 90f);
        hookVx = (float) Math.cos(radians) * speed;
        hookVy = (float) Math.sin(radians) * speed;
        hookState = HOOK_OUT;
        prismBounced = false;
        toast = "Claw launched";
        if (audio != null) {
            audio.playLaunch();
        }
    }

    private void update(float dt) {
        timer -= dt;
        if (timer <= 0f) {
            finishShift(false);
            return;
        }
        if (flashTime > 0f) {
            flashTime -= dt;
        }
        if (magnetTime > 0f) {
            magnetTime -= dt;
        }
        if (boostTime > 0f) {
            boostTime -= dt;
        }
        if (lensTime > 0f) {
            lensTime -= dt;
        }
        if (shieldTime > 0f) {
            shieldTime -= dt;
        }
        updateItems(dt);
        if (hookState == HOOK_SWING) {
            angle += swingDir * dt * 70f;
            if (angle > SWING_LIMIT) {
                angle = SWING_LIMIT;
                swingDir = -1f;
            } else if (angle < -SWING_LIMIT) {
                angle = -SWING_LIMIT;
                swingDir = 1f;
            }
            double radians = Math.toRadians(angle + 90f);
            hookX = anchorX + (float) Math.cos(radians) * SWING_ARM_LENGTH;
            hookY = anchorY + (float) Math.sin(radians) * SWING_ARM_LENGTH;
        } else if (hookState == HOOK_OUT) {
            hookX += hookVx * dt;
            hookY += hookVy * dt;
            applyPrismBend();
            applyMagnet(dt);
            int hit = findHitItem();
            if (hit >= 0) {
                grabbedIndex = hit;
                hookState = HOOK_BACK;
                toast = "Pulling " + typeName(items.get(hit).type);
            } else if (hookX < 12f || hookX > getWidth() - 12f || hookY > getHeight() - 12f || hookY < anchorY - 18f) {
                hookState = HOOK_BACK;
                toast = "Cable limit";
            }
        } else if (hookState == HOOK_BACK) {
            pullHookBack(dt);
        }
        updateHud();
    }

    private void updateItems(float dt) {
        for (Item item : items) {
            if (item.type == TYPE_BAT && !item.taken) {
                item.phase += dt * 2.2f;
                item.x += Math.sin(item.phase) * dt * 26f;
            }
        }
    }

    private void applyPrismBend() {
        if (prismBounced) {
            return;
        }
        float px = getWidth() * 0.68f;
        float py = getHeight() * 0.38f;
        float reach = lensTime > 0f ? 82f : 56f;
        if (distance(hookX, hookY, px, py) < reach) {
            hookVx = -hookVx * 0.58f;
            hookVy = Math.abs(hookVy) * 0.92f;
            prismBounced = true;
            toast = "Prism bend";
            flashTime = 0.35f;
            if (audio != null) {
                audio.playPrism();
            }
        }
    }

    private void applyMagnet(float dt) {
        if (magnetTime <= 0f) {
            return;
        }
        for (Item item : items) {
            if (!item.taken && isTreasure(item.type) && distance(item.x, item.y, hookX, hookY) < 150f) {
                item.x += (hookX - item.x) * dt * 1.8f;
                item.y += (hookY - item.y) * dt * 1.8f;
            }
        }
    }

    private int findHitItem() {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (!item.taken && distance(hookX, hookY, item.x, item.y) < item.size * 0.55f + 18f) {
                return i;
            }
        }
        return -1;
    }

    private void pullHookBack(float dt) {
        float dx = anchorX - hookX;
        float dy = anchorY - hookY;
        float len = Math.max(1f, distance(anchorX, anchorY, hookX, hookY));
        float weight = 1f;
        if (grabbedIndex >= 0 && grabbedIndex < items.size()) {
            Item item = items.get(grabbedIndex);
            weight = item.weight;
            item.x = hookX;
            item.y = hookY + item.size * 0.35f;
        }
        float speed = (boostTime > 0f ? 590f : 430f) / weight;
        hookX += dx / len * speed * dt;
        hookY += dy / len * speed * dt;
        if (len < 38f) {
            collectGrabbed();
            resetHook();
        }
    }

    private void collectGrabbed() {
        if (grabbedIndex < 0 || grabbedIndex >= items.size()) {
            return;
        }
        Item item = items.get(grabbedIndex);
        item.taken = true;
        if (item.type == TYPE_BOMB || item.type == TYPE_SHARD || item.type == TYPE_BAT) {
            if (shieldTime > 0f) {
                score += 35;
                toast = "Shield blocked hazard";
                if (audio != null) {
                    audio.playTool();
                }
            } else {
                score = Math.max(0, score - 120);
                timer = Math.max(0f, timer - 8f);
                toast = "Hazard penalty";
                if (audio != null) {
                    audio.playHazard();
                }
                return;
            }
        } else {
            score += item.value;
            if (item.type == orderA) {
                orderHaveA++;
            }
            if (item.type == orderB) {
                orderHaveB++;
            }
            toast = "Collected " + typeName(item.type);
            if (audio != null) {
                audio.playCollect();
            }
        }
        if (ordersComplete()) {
            if (stage >= 3) {
                finishShift(true);
            } else {
                stage++;
                tools[TOOL_DYNAMITE] += 1;
                tools[TOOL_MAGNET] += 1;
                tools[TOOL_BOOST] += 1;
                setupStage();
                toast = "Next cavern opened";
            }
        }
    }

    private void removeNearestHazard() {
        Item best = null;
        float bestDist = Float.MAX_VALUE;
        for (Item item : items) {
            if (!item.taken && !isTreasure(item.type)) {
                float d = distance(item.x, item.y, anchorX, anchorY);
                if (d < bestDist) {
                    bestDist = d;
                    best = item;
                }
            }
        }
        if (best != null) {
            best.taken = true;
            score += 45;
            toast = "Hazard cleared";
        } else {
            toast = "No hazard in range";
        }
    }

    private boolean ordersComplete() {
        return orderHaveA >= orderNeedA && orderHaveB >= orderNeedB && score >= orderScore;
    }

    private void finishShift(boolean won) {
        mode = MODE_GAME_OVER;
        String summary;
        if (won) {
            summary = "All crystal orders filled with score " + score + ".";
        } else {
            summary = "Orders missed. Score " + score + ", needed " + orderScore + ".";
        }
        if (listener != null) {
            listener.onShiftEnded(won, summary);
        }
        invalidate();
    }

    private void updateHud() {
        if (listener != null) {
            listener.onHudChanged("Score " + score, "Time " + Math.max(0, (int) timer), "Stage " + stage, buildOrderText());
        }
    }

    private String buildOrderText() {
        return String.format(Locale.US, "Orders: %s %d/%d   %s %d/%d   Score %d/%d   %s", typeName(orderA), orderHaveA, orderNeedA, typeName(orderB), orderHaveB, orderNeedB, score, orderScore, toast);
    }

    private void drawScene(Canvas canvas) {
        drawCave(canvas);
        drawPrism(canvas);
        drawOrderHighlights(canvas);
        for (Item item : items) {
            if (!item.taken) {
                drawItem(canvas, item);
            }
        }
        drawHook(canvas);
        drawToolEffects(canvas);
    }

    private void drawCave(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), Color.rgb(15, 34, 58), Color.rgb(116, 220, 232), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShader(new RadialGradient(getWidth() * 0.58f, getHeight() * 0.42f, getWidth() * 0.55f, Color.argb(145, 80, 255, 235), Color.argb(0, 80, 255, 235), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), glowPaint);
        glowPaint.setShader(null);
        paint.setColor(Color.rgb(31, 48, 73));
        canvas.drawOval(new RectF(-110f, getHeight() * 0.54f, getWidth() * 0.40f, getHeight() * 1.16f), paint);
        canvas.drawOval(new RectF(getWidth() * 0.58f, getHeight() * 0.52f, getWidth() + 120f, getHeight() * 1.14f), paint);
        paint.setColor(Color.rgb(52, 78, 102));
        for (int i = 0; i < 8; i++) {
            float x = getWidth() * (0.06f + i * 0.13f);
            float y = getHeight() * (0.78f + (i % 2) * 0.035f);
            drawRock(canvas, x, y, 68f + i * 2f);
        }
        for (int i = 0; i < 9; i++) {
            float x = getWidth() * (0.06f + i * 0.115f);
            float y = getHeight() * (0.18f + (i % 3) * 0.10f);
            int color = i % 2 == 0 ? Color.rgb(23, 190, 174) : Color.rgb(135, 105, 242);
            drawWallCrystal(canvas, x, y, 28f + (i % 4) * 5f, color);
        }
        drawLantern(canvas, getWidth() * 0.09f, getHeight() * 0.15f);
        drawLantern(canvas, getWidth() * 0.91f, getHeight() * 0.20f);
        paint.setColor(Color.rgb(139, 87, 43));
        canvas.drawRoundRect(new RectF(anchorX - 116f, 4f, anchorX + 116f, 42f), 18f, 18f, paint);
        paint.setColor(Color.rgb(194, 134, 62));
        canvas.drawRoundRect(new RectF(anchorX - 102f, 10f, anchorX + 102f, 35f), 14f, 14f, paint);
        paint.setColor(Color.rgb(255, 202, 88));
        canvas.drawCircle(anchorX, 23f, 21f, paint);
        paint.setColor(Color.rgb(64, 57, 55));
        canvas.drawCircle(anchorX, 23f, 10f, paint);
        drawDepthMarker(canvas);
    }

    private void drawPrism(Canvas canvas) {
        float px = getWidth() * 0.68f;
        float py = getHeight() * 0.38f;
        glowPaint.setShader(new RadialGradient(px, py, 120f, Color.argb(lensTime > 0f ? 150 : 85, 185, 120, 255), Color.argb(0, 185, 120, 255), Shader.TileMode.CLAMP));
        canvas.drawCircle(px, py, 120f, glowPaint);
        glowPaint.setShader(null);
        Path path = new Path();
        path.moveTo(px, py - 46f);
        path.lineTo(px + 45f, py + 28f);
        path.lineTo(px - 48f, py + 35f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lensTime > 0f ? Color.rgb(151, 108, 255) : Color.rgb(112, 222, 229));
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(110, 255, 255, 255));
        Path shine = new Path();
        shine.moveTo(px - 6f, py - 36f);
        shine.lineTo(px + 17f, py + 18f);
        shine.lineTo(px - 21f, py + 26f);
        shine.close();
        canvas.drawPath(shine, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawOrderHighlights(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        for (Item item : items) {
            if (!item.taken && (item.type == orderA || item.type == orderB)) {
                int alpha = 95 + (int) (Math.abs(Math.sin(item.phase + timer * 2f)) * 90f);
                paint.setColor(Color.argb(alpha, 255, 244, 150));
                canvas.drawCircle(item.x, item.y, item.size * 0.72f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawItem(Canvas canvas, Item item) {
        float pulse = (float) Math.sin(item.phase + timer * 1.7f) * 2.4f;
        float y = item.y + pulse;
        if (item.type == TYPE_GOLD) {
            paint.setColor(Color.rgb(247, 186, 52));
            drawRock(canvas, item.x, y, item.size);
            paint.setColor(Color.argb(130, 255, 250, 190));
            canvas.drawOval(new RectF(item.x - item.size * 0.28f, y - item.size * 0.28f, item.x + item.size * 0.08f, y - item.size * 0.05f), paint);
        } else if (item.type == TYPE_EMERALD) {
            drawCrystal(canvas, item.x, y, item.size, Color.rgb(24, 204, 178));
        } else if (item.type == TYPE_AMETHYST) {
            drawCrystal(canvas, item.x, y, item.size, Color.rgb(139, 108, 255));
        } else if (item.type == TYPE_RELIC) {
            paint.setColor(Color.rgb(163, 108, 57));
            canvas.drawRoundRect(new RectF(item.x - item.size * 0.60f, y - item.size * 0.40f, item.x + item.size * 0.60f, y + item.size * 0.40f), 9f, 9f, paint);
            paint.setColor(Color.rgb(248, 188, 56));
            canvas.drawRect(item.x - item.size * 0.08f, y - item.size * 0.40f, item.x + item.size * 0.08f, y + item.size * 0.40f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.rgb(92, 56, 38));
            canvas.drawRoundRect(new RectF(item.x - item.size * 0.60f, y - item.size * 0.40f, item.x + item.size * 0.60f, y + item.size * 0.40f), 9f, 9f, paint);
            paint.setStyle(Paint.Style.FILL);
        } else if (item.type == TYPE_BOMB) {
            paint.setColor(Color.rgb(45, 46, 57));
            canvas.drawCircle(item.x, y, item.size * 0.42f, paint);
            paint.setColor(Color.rgb(96, 98, 112));
            canvas.drawCircle(item.x - item.size * 0.12f, y - item.size * 0.12f, item.size * 0.13f, paint);
            paint.setColor(Color.rgb(255, 79, 94));
            canvas.drawCircle(item.x + item.size * 0.20f, y - item.size * 0.30f, item.size * 0.14f, paint);
            paint.setColor(Color.rgb(255, 210, 80));
            canvas.drawCircle(item.x + item.size * 0.29f, y - item.size * 0.39f, item.size * 0.07f, paint);
        } else if (item.type == TYPE_SHARD) {
            drawCrystal(canvas, item.x, y, item.size, Color.rgb(255, 91, 118));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.rgb(255, 229, 229));
            canvas.drawLine(item.x - item.size * 0.34f, y + item.size * 0.22f, item.x + item.size * 0.22f, y - item.size * 0.31f, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            float wing = (float) Math.sin(item.phase * 6f) * item.size * 0.18f;
            paint.setColor(Color.rgb(59, 62, 82));
            canvas.drawOval(new RectF(item.x - item.size * 0.50f, y - item.size * 0.25f, item.x + item.size * 0.50f, y + item.size * 0.25f), paint);
            canvas.drawOval(new RectF(item.x - item.size * 0.92f, y - item.size * 0.42f - wing, item.x - item.size * 0.16f, y + item.size * 0.08f + wing), paint);
            canvas.drawOval(new RectF(item.x + item.size * 0.16f, y - item.size * 0.42f + wing, item.x + item.size * 0.92f, y + item.size * 0.08f - wing), paint);
            paint.setColor(Color.rgb(255, 236, 120));
            canvas.drawCircle(item.x - item.size * 0.14f, y - item.size * 0.04f, item.size * 0.055f, paint);
            canvas.drawCircle(item.x + item.size * 0.14f, y - item.size * 0.04f, item.size * 0.055f, paint);
        }
    }

    private void drawCrystal(Canvas canvas, float x, float y, float size, int color) {
        Path path = new Path();
        path.moveTo(x, y - size * 0.62f);
        path.lineTo(x + size * 0.48f, y - size * 0.05f);
        path.lineTo(x + size * 0.22f, y + size * 0.55f);
        path.lineTo(x - size * 0.42f, y + size * 0.32f);
        path.lineTo(x - size * 0.50f, y - size * 0.12f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, size * 0.07f));
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawLine(x, y - size * 0.54f, x - size * 0.18f, y + size * 0.30f, paint);
        canvas.drawLine(x, y - size * 0.54f, x + size * 0.18f, y + size * 0.38f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawCircle(x - size * 0.14f, y - size * 0.15f, size * 0.12f, paint);
    }

    private void drawRock(Canvas canvas, float x, float y, float size) {
        Path path = new Path();
        path.moveTo(x - size * 0.50f, y + size * 0.10f);
        path.lineTo(x - size * 0.26f, y - size * 0.34f);
        path.lineTo(x + size * 0.24f, y - size * 0.42f);
        path.lineTo(x + size * 0.54f, y - size * 0.03f);
        path.lineTo(x + size * 0.36f, y + size * 0.36f);
        path.lineTo(x - size * 0.28f, y + size * 0.42f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawHook(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setColor(Color.rgb(80, 64, 56));
        canvas.drawLine(anchorX, anchorY, hookX, hookY, paint);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(222, 176, 98));
        canvas.drawLine(anchorX, anchorY, hookX, hookY, paint);
        paint.setStrokeWidth(7f);
        paint.setColor(Color.rgb(70, 80, 92));
        canvas.drawLine(hookX, hookY - 18f, hookX, hookY + 12f, paint);
        canvas.drawArc(new RectF(hookX - 24f, hookY - 2f, hookX + 2f, hookY + 34f), 10f, 250f, false, paint);
        canvas.drawArc(new RectF(hookX - 2f, hookY - 2f, hookX + 24f, hookY + 34f), -80f, 250f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 255, 255));
        canvas.drawCircle(hookX, hookY, 5f, paint);
        paint.setColor(Color.rgb(245, 180, 61));
        canvas.drawCircle(hookX, hookY - 18f, 7f, paint);
    }

    private void drawToolEffects(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        if (magnetTime > 0f) {
            paint.setColor(Color.argb(130, 32, 201, 128));
            paint.setStrokeWidth(4f);
            canvas.drawCircle(hookX, hookY, 150f, paint);
        }
        if (shieldTime > 0f) {
            paint.setColor(Color.argb(155, 61, 152, 255));
            paint.setStrokeWidth(5f);
            canvas.drawCircle(hookX, hookY, 32f, paint);
        }
        if (flashTime > 0f) {
            paint.setColor(Color.argb((int) (flashTime * 180f), 255, 255, 255));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCenterHint(Canvas canvas, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(215, 255, 255, 255));
        RectF box = new RectF(getWidth() * 0.24f, getHeight() * 0.40f, getWidth() * 0.76f, getHeight() * 0.58f);
        canvas.drawRoundRect(box, 24f, 24f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(112, 222, 229));
        canvas.drawRoundRect(box, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.rgb(24, 48, 68));
        textPaint.setTextSize(30f);
        canvas.drawText(text, getWidth() * 0.5f, getHeight() * 0.50f, textPaint);
    }

    private void drawWallCrystal(Canvas canvas, float x, float y, float size, int color) {
        Path path = new Path();
        path.moveTo(x, y - size);
        path.lineTo(x + size * 0.42f, y - size * 0.18f);
        path.lineTo(x + size * 0.20f, y + size * 0.76f);
        path.lineTo(x - size * 0.30f, y + size * 0.48f);
        path.lineTo(x - size * 0.36f, y - size * 0.20f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawCircle(x - size * 0.10f, y - size * 0.26f, size * 0.12f, paint);
    }

    private void drawLantern(Canvas canvas, float x, float y) {
        glowPaint.setShader(new RadialGradient(x, y + 34f, 78f, Color.argb(120, 255, 198, 88), Color.argb(0, 255, 198, 88), Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y + 34f, 78f, glowPaint);
        glowPaint.setShader(null);
        paint.setColor(Color.rgb(68, 55, 45));
        canvas.drawRoundRect(new RectF(x - 16f, y, x + 16f, y + 50f), 8f, 8f, paint);
        paint.setColor(Color.rgb(255, 195, 74));
        canvas.drawOval(new RectF(x - 11f, y + 14f, x + 11f, y + 42f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.rgb(98, 75, 55));
        canvas.drawArc(new RectF(x - 18f, y - 12f, x + 18f, y + 24f), 205f, 130f, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawDepthMarker(Canvas canvas) {
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.argb(180, 220, 250, 255));
        for (int i = 1; i <= 3; i++) {
            float y = getHeight() * (0.25f + i * 0.18f);
            paint.setColor(Color.argb(80, 220, 250, 255));
            canvas.drawRect(18f, y, getWidth() - 18f, y + 2f, paint);
            canvas.drawText("Depth " + i, 24f, y - 6f, textPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private boolean isTreasure(int type) {
        return type == TYPE_GOLD || type == TYPE_EMERALD || type == TYPE_AMETHYST || type == TYPE_RELIC;
    }

    private String typeName(int type) {
        if (type == TYPE_GOLD) {
            return "Gold";
        }
        if (type == TYPE_EMERALD) {
            return "Emerald";
        }
        if (type == TYPE_AMETHYST) {
            return "Amethyst";
        }
        if (type == TYPE_RELIC) {
            return "Relic";
        }
        if (type == TYPE_BOMB) {
            return "Bomb";
        }
        if (type == TYPE_SHARD) {
            return "Shard";
        }
        return "Bat";
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static class Item {
        final int type;
        final float size;
        final int value;
        final float weight;
        float x;
        float y;
        float phase;
        boolean taken;

        Item(int type, float x, float y, float size) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.size = size;
            if (type == TYPE_GOLD) {
                value = 120;
                weight = 1.45f;
            } else if (type == TYPE_EMERALD) {
                value = 180;
                weight = 1.15f;
            } else if (type == TYPE_AMETHYST) {
                value = 210;
                weight = 1.25f;
            } else if (type == TYPE_RELIC) {
                value = 320;
                weight = 1.85f;
            } else {
                value = 0;
                weight = 1.0f;
            }
            phase = x * 0.03f;
        }
    }

    public interface GameListener {
        void onHudChanged(String score, String timer, String stage, String orders);

        void onShiftEnded(boolean won, String summary);
    }
}
