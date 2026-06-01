package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    public interface GameListener {
        void onHudChanged(String durability, String cargo, String heat, String target, String lane, String route, String warning);
        void onStateChanged(String state, String message, boolean finalState);
        void onAudioCue(String cue);
    }

    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_OVER = 3;
    private static final int TYPE_ORE = 0;
    private static final int TYPE_GEM = 1;
    private static final int TYPE_BARRICADE = 2;
    private static final int TYPE_SWITCH = 3;
    private static final int TYPE_BROKEN = 4;
    private static final int TYPE_COOLANT = 5;
    private static final int TYPE_BOLT = 6;
    private static final String RUNTIME_ART_MAP_PATH = "game_art/runtime_art_map.json";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tempPath = new Path();
    private final RectF tempRect = new RectF();
    private final Random random = new Random(73019L);
    private final List<Entity> entities = new ArrayList<>();
    private final Hook leftHook = new Hook(true);
    private final Hook rightHook = new Hook(false);
    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            float dt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameMs) / 1000f));
            lastFrameMs = now;
            if (state == STATE_PLAYING) {
                updateGame(dt);
            }
            invalidate();
            if (loopActive) {
                postDelayed(this, 16L);
            }
        }
    };

    private GameListener listener;
    private int state = STATE_MENU;
    private int lane = 1;
    private int durability = 100;
    private int cargo = 0;
    private int cargoTarget = 240;
    private int heat = 0;
    private float heatValue = 0f;
    private float distance = 0f;
    private float stationDistance = 2600f;
    private float speed = 225f;
    private float spawnClock = 0f;
    private float railPulse = 0f;
    private float warningCueClock = 0f;
    private float downX = 0f;
    private float downY = 0f;
    private long downTime = 0L;
    private long lastFrameMs = 0L;
    private boolean loopActive = false;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        textPaint.setColor(Color.rgb(244, 248, 238));
        textPaint.setTextSize(28f);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
    }

    public void setGameListener(GameListener gameListener) {
        listener = gameListener;
        dispatchHud();
    }

    public void showMenu() {
        state = STATE_MENU;
        dispatchState("MENU", "Tap left or right to fire side hooks. Swipe up or down to change rails.", false);
        dispatchHud();
        invalidate();
    }

    public void startGame() {
        int startWidth = Math.max(960, getWidth());
        state = STATE_PLAYING;
        lane = 1;
        durability = 100;
        cargo = 0;
        cargoTarget = 240;
        heat = 0;
        heatValue = 0f;
        distance = 0f;
        stationDistance = 2600f;
        speed = 225f;
        spawnClock = 0f;
        railPulse = 0f;
        warningCueClock = 0f;
        entities.clear();
        leftHook.reset();
        rightHook.reset();
        for (int i = 0; i < 9; i++) {
            spawnEntity(startWidth + 180f + i * 210f);
        }
        dispatchState("PLAYING", "Express rolling.", false);
        dispatchHud();
        ensureLoop();
    }

    public void pauseGame() {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED;
            suspendLoop();
            dispatchState("PAUSED", "Paused at the signal board.", false);
            invalidate();
        }
    }

    public void resumeGame() {
        if (state == STATE_PAUSED) {
            state = STATE_PLAYING;
            dispatchState("PLAYING", "Express rolling.", false);
            ensureLoop();
        }
    }

    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    public boolean isPaused() {
        return state == STATE_PAUSED;
    }

    public void suspendLoop() {
        loopActive = false;
        removeCallbacks(frameRunnable);
    }

    public void resumeLoop() {
        if (state == STATE_PLAYING) {
            ensureLoop();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (state == STATE_PLAYING) {
            ensureLoop();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        suspendLoop();
        super.onDetachedFromWindow();
    }

    private void ensureLoop() {
        if (!loopActive) {
            loopActive = true;
            lastFrameMs = System.currentTimeMillis();
            removeCallbacks(frameRunnable);
            post(frameRunnable);
        }
    }

    private void updateGame(float dt) {
        int w = Math.max(1, getWidth());
        distance += speed * dt;
        railPulse += dt * speed;
        speed = Math.min(330f, speed + dt * 1.8f);
        setHeat(heatValue - dt * 6f);
        warningCueClock = Math.max(0f, warningCueClock - dt);
        if ((heat > 84 || durability < 30) && warningCueClock <= 0f) {
            dispatchAudio("warning");
            warningCueClock = 1.6f;
        }
        spawnClock -= dt;
        if (spawnClock <= 0f) {
            spawnClock = 0.55f + random.nextFloat() * 0.38f;
            spawnEntity(w + 120f + random.nextFloat() * 180f);
        }
        leftHook.update(dt);
        rightHook.update(dt);
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            entity.x -= speed * dt;
            entity.spin += dt * 2.5f;
            if (entity.active && isImpact(entity)) {
                applyImpact(entity);
            }
            if (entity.x < -160f || entity.removed) {
                iterator.remove();
            }
        }
        if (durability <= 0) {
            finishRun("Cart disabled before the station.");
        } else if (distance >= stationDistance) {
            if (cargo >= cargoTarget) {
                finishRun("Station reached. Cargo contract cleared.");
            } else {
                finishRun("Station reached short on cargo.");
            }
        }
        dispatchHud();
    }

    private void finishRun(String message) {
        state = STATE_OVER;
        loopActive = false;
        dispatchAudio(cargo >= cargoTarget && distance >= stationDistance ? "win" : "fail");
        dispatchState("OVER", message, true);
    }

    private boolean isImpact(Entity entity) {
        if (entity.lane != lane) {
            return false;
        }
        if (entity.type != TYPE_BARRICADE && entity.type != TYPE_BROKEN) {
            return false;
        }
        float cartX = getCartX();
        return Math.abs(entity.x - cartX) < 44f;
    }

    private void applyImpact(Entity entity) {
        entity.removed = true;
        int damage = entity.type == TYPE_BARRICADE ? 18 : 24;
        durability = Math.max(0, durability - damage);
        addHeat(12f);
        dispatchAudio("hazard");
    }

    private void spawnEntity(float x) {
        Entity entity = new Entity();
        entity.x = x;
        entity.lane = random.nextInt(3);
        int roll = random.nextInt(100);
        if (roll < 31) {
            entity.type = TYPE_ORE;
            entity.value = 22;
            entity.radius = 24f;
        } else if (roll < 45) {
            entity.type = TYPE_GEM;
            entity.value = 38;
            entity.radius = 20f;
        } else if (roll < 61) {
            entity.type = TYPE_BARRICADE;
            entity.value = 0;
            entity.radius = 32f;
        } else if (roll < 73) {
            entity.type = TYPE_BROKEN;
            entity.value = 0;
            entity.radius = 30f;
        } else if (roll < 84) {
            entity.type = TYPE_SWITCH;
            entity.value = 0;
            entity.radius = 22f;
        } else if (roll < 93) {
            entity.type = TYPE_COOLANT;
            entity.value = 0;
            entity.radius = 21f;
        } else {
            entity.type = TYPE_BOLT;
            entity.value = 0;
            entity.radius = 19f;
        }
        entities.add(entity);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != STATE_PLAYING) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            downTime = System.currentTimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            long held = System.currentTimeMillis() - downTime;
            if (Math.abs(dy) > 56f && Math.abs(dy) > Math.abs(dx) * 0.8f) {
                switchLane(dy < 0f ? -1 : 1);
            } else {
                fireHook(event.getX() < getWidth() * 0.5f, held > 420L);
            }
            return true;
        }
        return true;
    }

    private void switchLane(int delta) {
        int next = Math.max(0, Math.min(2, lane + delta));
        if (next != lane) {
            lane = next;
            addHeat(2f);
            dispatchAudio("lane");
        }
    }

    private void fireHook(boolean left, boolean heavy) {
        Hook hook = left ? leftHook : rightHook;
        if (hook.active || heat >= 96) {
            return;
        }
        Entity target = findTarget(left);
        hook.active = true;
        hook.time = 0f;
        hook.heavy = heavy;
        dispatchAudio("launch");
        if (target != null) {
            hook.targetX = target.x;
            hook.targetY = railY(target.lane);
            applyHookTarget(target, heavy);
        } else {
            float range = getWidth() * (heavy ? 0.48f : 0.38f);
            hook.targetX = getCartX() + (left ? -range : range);
            hook.targetY = railY(lane);
            addHeat(heavy ? 11f : 7f);
        }
        dispatchHud();
    }

    private Entity findTarget(boolean left) {
        float cartX = getCartX();
        float maxRange = getWidth() * 0.46f;
        Entity best = null;
        float bestScore = Float.MAX_VALUE;
        for (Entity entity : entities) {
            if (!entity.active || entity.removed) {
                continue;
            }
            float side = entity.x - cartX;
            if (left && side >= -12f) {
                continue;
            }
            if (!left && side <= 12f) {
                continue;
            }
            if (Math.abs(side) > maxRange) {
                continue;
            }
            float lanePenalty = Math.abs(entity.lane - lane) * 90f;
            float priority = (entity.type == TYPE_BARRICADE || entity.type == TYPE_BROKEN) ? -95f : 0f;
            float scoreValue = Math.abs(side) + lanePenalty + priority;
            if (scoreValue < bestScore) {
                bestScore = scoreValue;
                best = entity;
            }
        }
        return best;
    }

    private void applyHookTarget(Entity entity, boolean heavy) {
        entity.active = false;
        entity.removed = true;
        int addHeat = heavy ? 16 : 10;
        if (entity.type == TYPE_ORE || entity.type == TYPE_GEM) {
            cargo += entity.value + (heavy ? 6 : 0);
            dispatchAudio("collect");
        } else if (entity.type == TYPE_BARRICADE || entity.type == TYPE_BROKEN) {
            cargo += heavy ? 8 : 4;
            dispatchAudio("hazard");
        } else if (entity.type == TYPE_SWITCH) {
            if (lane < 2 && random.nextBoolean()) {
                lane++;
            } else if (lane > 0) {
                lane--;
            }
            cargo += 5;
            dispatchAudio("lane");
        } else if (entity.type == TYPE_COOLANT) {
            setHeat(heatValue - 24f);
            addHeat = 2;
            dispatchAudio("upgrade");
        } else if (entity.type == TYPE_BOLT) {
            durability = Math.min(100, durability + 9);
            cargo += 3;
            dispatchAudio("upgrade");
        }
        addHeat(addHeat);
    }

    private void addHeat(float amount) {
        setHeat(heatValue + amount);
    }

    private void setHeat(float value) {
        heatValue = Math.max(0f, Math.min(100f, value));
        heat = Math.round(heatValue);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        drawBackground(canvas, w, h);
        drawRails(canvas, w);
        drawEntities(canvas);
        drawHooks(canvas);
        drawCart(canvas);
        drawSignalFrames(canvas, w, h);
        drawStationMeter(canvas, w, h);
        if (state == STATE_MENU || state == STATE_PAUSED || state == STATE_OVER) {
            drawVeil(canvas, w, h);
        }
    }

    private void drawBackground(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.rgb(20, 29, 30));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(33, 49, 45));
        canvas.drawRect(0f, 0f, w, h, paint);
        paint.setColor(Color.rgb(54, 71, 61));
        for (int i = 0; i < 7; i++) {
            float x = (i * 180f - (railPulse * 0.2f % 180f));
            canvas.drawOval(x, getHeight() * 0.08f, x + 220f, getHeight() * 0.72f, paint);
        }
        paint.setColor(Color.rgb(19, 24, 23));
        canvas.drawRect(0f, getHeight() * 0.76f, w, h, paint);
        paint.setColor(Color.rgb(74, 64, 54));
        for (float x = (railPulse * -0.32f) % 160f; x < w + 160f; x += 160f) {
            canvas.drawRect(x, h * 0.09f, x + 10f, h * 0.78f, paint);
            paint.setColor(Color.rgb(42, 36, 29));
            canvas.drawRect(x + 10f, h * 0.09f, x + 24f, h * 0.78f, paint);
            paint.setColor(Color.rgb(74, 64, 54));
        }
    }

    private void drawRails(Canvas canvas, int w) {
        for (int i = 0; i < 3; i++) {
            float y = railY(i);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(i == lane ? Color.rgb(255, 197, 94) : Color.rgb(132, 118, 91));
            canvas.drawLine(0f, y - 13f, w, y - 13f, paint);
            canvas.drawLine(0f, y + 13f, w, y + 13f, paint);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.rgb(79, 63, 48));
            for (float x = -(railPulse % 58f); x < w + 58f; x += 58f) {
                canvas.drawLine(x, y - 26f, x + 28f, y + 26f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSignalFrames(Canvas canvas, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        float gateX = w - 52f - (distance % 420f) * 0.18f;
        paint.setColor(Color.rgb(74, 64, 54));
        canvas.drawRect(gateX, h * 0.15f, gateX + 10f, h * 0.78f, paint);
        paint.setColor(Color.rgb(20, 25, 24));
        tempRect.set(gateX - 24f, h * 0.13f, gateX + 34f, h * 0.24f);
        canvas.drawRoundRect(tempRect, 7f, 7f, paint);
        paint.setColor(heat > 70 || durability < 35 ? Color.rgb(232, 74, 60) : Color.rgb(103, 211, 126));
        canvas.drawCircle(gateX + 5f, h * 0.185f, 13f, paint);
        textPaint.setTextSize(22f);
        textPaint.setColor(Color.rgb(243, 233, 215));
        canvas.drawText("ST", gateX - 12f, h * 0.236f, textPaint);
    }

    private void drawEntities(Canvas canvas) {
        for (Entity entity : entities) {
            if (entity.removed) {
                continue;
            }
            float x = entity.x;
            float y = railY(entity.lane);
            if (entity.type == TYPE_ORE) {
                paint.setColor(Color.rgb(170, 116, 70));
                canvas.drawCircle(x, y, entity.radius, paint);
                paint.setColor(Color.rgb(212, 168, 95));
                canvas.drawCircle(x - 7f, y - 8f, 6f, paint);
            } else if (entity.type == TYPE_GEM) {
                paint.setColor(Color.rgb(84, 219, 207));
                tempPath.reset();
                tempPath.moveTo(x, y - 28f);
                tempPath.lineTo(x + 24f, y);
                tempPath.lineTo(x, y + 28f);
                tempPath.lineTo(x - 24f, y);
                tempPath.close();
                canvas.drawPath(tempPath, paint);
                paint.setColor(Color.rgb(235, 255, 247));
                canvas.drawCircle(x - 5f, y - 7f, 4f, paint);
            } else if (entity.type == TYPE_BARRICADE) {
                paint.setColor(Color.rgb(130, 71, 47));
                tempRect.set(x - 35f, y - 28f, x + 35f, y + 28f);
                canvas.drawRoundRect(tempRect, 6f, 6f, paint);
                paint.setColor(Color.rgb(245, 188, 69));
                canvas.drawRect(x - 32f, y - 7f, x + 32f, y + 7f, paint);
            } else if (entity.type == TYPE_BROKEN) {
                paint.setColor(Color.rgb(199, 67, 65));
                paint.setStrokeWidth(8f);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(x - 30f, y - 18f, x - 6f, y + 18f, paint);
                canvas.drawLine(x + 6f, y - 18f, x + 30f, y + 18f, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (entity.type == TYPE_SWITCH) {
                paint.setColor(Color.rgb(92, 160, 238));
                tempRect.set(x - 28f, y - 20f, x + 28f, y + 20f);
                canvas.drawRoundRect(tempRect, 8f, 8f, paint);
                paint.setColor(Color.rgb(230, 244, 255));
                canvas.drawCircle(x + 8f, y, 8f, paint);
            } else if (entity.type == TYPE_COOLANT) {
                paint.setColor(Color.rgb(85, 216, 145));
                tempRect.set(x - 18f, y - 28f, x + 18f, y + 28f);
                canvas.drawRoundRect(tempRect, 14f, 14f, paint);
                paint.setColor(Color.rgb(216, 255, 231));
                canvas.drawRect(x - 9f, y - 17f, x + 9f, y + 17f, paint);
            } else {
                paint.setColor(Color.rgb(210, 214, 198));
                canvas.drawCircle(x, y, entity.radius, paint);
                paint.setColor(Color.rgb(90, 96, 88));
                canvas.drawCircle(x, y, 8f, paint);
            }
        }
    }

    private void drawHooks(Canvas canvas) {
        drawHook(canvas, leftHook);
        drawHook(canvas, rightHook);
    }

    private void drawHook(Canvas canvas, Hook hook) {
        if (!hook.active) {
            return;
        }
        float cartX = getCartX();
        float cartY = railY(lane) - 34f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(hook.heavy ? 7f : 5f);
        paint.setColor(hook.left ? Color.rgb(111, 214, 180) : Color.rgb(252, 184, 83));
        canvas.drawLine(cartX, cartY, hook.targetX, hook.targetY, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(hook.targetX, hook.targetY, hook.heavy ? 13f : 10f, paint);
    }

    private void drawCart(Canvas canvas) {
        float x = getCartX();
        float y = railY(lane);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(33, 38, 37));
        tempRect.set(x - 58f, y - 48f, x + 58f, y + 16f);
        canvas.drawRoundRect(tempRect, 12f, 12f, paint);
        paint.setColor(Color.rgb(215, 78, 63));
        tempRect.set(x - 48f, y - 40f, x + 48f, y + 4f);
        canvas.drawRoundRect(tempRect, 10f, 10f, paint);
        paint.setColor(Color.rgb(244, 158, 67));
        tempRect.set(x - 24f, y - 68f, x + 38f, y - 34f);
        canvas.drawRoundRect(tempRect, 9f, 9f, paint);
        paint.setColor(Color.rgb(32, 38, 41));
        canvas.drawCircle(x - 35f, y + 20f, 15f, paint);
        canvas.drawCircle(x + 35f, y + 20f, 15f, paint);
        paint.setColor(Color.rgb(245, 213, 107));
        canvas.drawCircle(x + 56f, y - 21f, 8f, paint);
    }

    private void drawStationMeter(Canvas canvas, int w, int h) {
        float progress = Math.max(0f, Math.min(1f, distance / stationDistance));
        float left = w * 0.08f;
        float right = w * 0.92f;
        float top = h - 44f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(14, 18, 17));
        tempRect.set(left, top, right, top + 18f);
        canvas.drawRoundRect(tempRect, 9f, 9f, paint);
        paint.setColor(Color.rgb(255, 197, 94));
        tempRect.set(left, top, left + (right - left) * progress, top + 18f);
        canvas.drawRoundRect(tempRect, 9f, 9f, paint);
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.rgb(238, 242, 220));
        canvas.drawText(Math.round(distance) + "m", left, top - 8f, textPaint);
    }

    private void drawVeil(Canvas canvas, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(96, 0, 0, 0));
        canvas.drawRect(0f, 0f, w, h, paint);
    }

    private float getCartX() {
        return getWidth() * 0.34f;
    }

    private float railY(int index) {
        float h = Math.max(1f, getHeight());
        return h * 0.26f + index * h * 0.22f;
    }

    private void dispatchHud() {
        if (listener == null) {
            return;
        }
        String warning = "CLEAR";
        if (durability < 35) {
            warning = "DAMAGE";
        } else if (heat > 78) {
            warning = "HEAT";
        } else if (speed > 290f) {
            warning = "FAST";
        }
        listener.onHudChanged(
                "DUR " + durability,
                "CARGO " + cargo,
                "HEAT " + heat,
                "TARGET " + cargo + "/" + cargoTarget,
                "LANE " + (lane + 1),
                Math.round(distance) + "/" + Math.round(stationDistance) + "m",
                warning
        );
    }

    private void dispatchState(String stateName, String message, boolean finalState) {
        if (listener != null) {
            listener.onStateChanged(stateName, message, finalState);
        }
    }

    private void dispatchAudio(String cue) {
        if (listener != null) {
            listener.onAudioCue(cue);
        }
    }

    private static class Entity {
        float x;
        int lane;
        int type;
        int value;
        float radius;
        float spin;
        boolean active = true;
        boolean removed = false;
    }

    private static class Hook {
        final boolean left;
        boolean active;
        boolean heavy;
        float targetX;
        float targetY;
        float time;

        Hook(boolean leftHook) {
            left = leftHook;
        }

        void reset() {
            active = false;
            heavy = false;
            time = 0f;
        }

        void update(float dt) {
            if (!active) {
                return;
            }
            time += dt;
            if (time > 0.24f) {
                active = false;
            }
        }
    }
}
