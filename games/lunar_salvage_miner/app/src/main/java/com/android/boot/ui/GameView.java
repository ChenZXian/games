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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    public interface GameListener {
        void onHudChanged(String oxygen, String battery, String parts, String goal);
        void onStateChanged(String state, String message, boolean finalState);
        void onAudioCue(String cue);
    }

    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_GAME_OVER = 3;
    private static final int HOOK_READY = 0;
    private static final int HOOK_OUT = 1;
    private static final int HOOK_RETURN = 2;
    private static final int TYPE_SCRAP = 0;
    private static final int TYPE_OXYGEN = 1;
    private static final int TYPE_BATTERY = 2;
    private static final int TYPE_MODULE = 3;
    private static final int TYPE_METEOR = 4;
    private static final int TYPE_ROCK = 5;
    private static final int TYPE_CORE = 6;
    private static final int TYPE_SNAG = 7;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(270527L);
    private final Debris[] debris = new Debris[30];
    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - lastFrameTime) / 1000000000f);
            lastFrameTime = now;
            update(dt);
            invalidate();
            if (loopActive) {
                postOnAnimation(this);
            }
        }
    };

    private GameListener listener;
    private int state = STATE_MENU;
    private int hookState = HOOK_READY;
    private boolean loopActive;
    private long lastFrameTime;
    private float cartX;
    private float cartMove;
    private float hookX;
    private float hookY;
    private float hookDirX;
    private float hookDirY;
    private float hookLength;
    private float hookAngle;
    private float oxygen;
    private float battery;
    private float hull;
    private float scanTime;
    private float boostTime;
    private float flashTime;
    private float railPulse;
    private float elapsed;
    private int parts;
    private int credits;
    private int modules;
    private int region;
    private int caughtIndex = -1;
    private boolean runtimeArtMapReady;
    private String lastMessage = "Recover parts, oxygen, and power before the moon goes dark.";
    private String lastDelivery = "";

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(true);
        textPaint.setColor(Color.rgb(232, 255, 248));
        textPaint.setTextSize(32f);
        textPaint.setFakeBoldText(true);
        for (int i = 0; i < debris.length; i++) {
            debris[i] = new Debris();
        }
        runtimeArtMapReady = loadRuntimeArtMap();
        resetRun();
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
        notifyHud();
    }

    public void showMenu() {
        state = STATE_MENU;
        notifyState("MENU", lastMessage, false);
    }

    public void startGame() {
        resetRun();
        state = STATE_PLAYING;
        notifyState("PLAYING", "", false);
    }

    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    public boolean isPaused() {
        return state == STATE_PAUSED;
    }

    public void pauseGame() {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED;
        }
    }

    public void resumeGame() {
        if (state == STATE_PAUSED) {
            state = STATE_PLAYING;
        }
    }

    public void suspendLoop() {
        loopActive = false;
    }

    public void resumeLoop() {
        if (!loopActive) {
            loopActive = true;
            lastFrameTime = System.nanoTime();
            postOnAnimation(frameRunnable);
        }
    }

    public void setCartMove(int direction) {
        cartMove = direction;
    }

    public void scan() {
        if (state == STATE_PLAYING && battery >= 7f) {
            battery -= 7f;
            scanTime = 3.2f;
            lastMessage = "Scanner sweep active.";
            notifyAudio("scan");
            notifyHud();
        }
    }

    public void boost() {
        if (state == STATE_PLAYING && battery >= 9f) {
            battery -= 9f;
            boostTime = 2.5f;
            lastMessage = "Magnet boost armed.";
            notifyAudio("boost");
            notifyHud();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        suspendLoop();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && state == STATE_PLAYING) {
            if (hookState == HOOK_READY && battery >= 4f) {
                launchHook();
            }
            return true;
        }
        return true;
    }

    private void resetRun() {
        oxygen = 100f;
        battery = 100f;
        hull = 100f;
        parts = 0;
        credits = 0;
        modules = 0;
        region = 1;
        elapsed = 0f;
        scanTime = 0f;
        boostTime = 0f;
        flashTime = 0f;
        railPulse = 0f;
        hookState = HOOK_READY;
        caughtIndex = -1;
        cartX = 0.5f;
        lastMessage = "Tap the field to fire the electromagnetic hook.";
        spawnDebris();
        notifyHud();
    }

    private void spawnDebris() {
        for (int i = 0; i < debris.length; i++) {
            Debris d = debris[i];
            d.active = true;
            d.x = 0.08f + random.nextFloat() * 0.82f;
            d.y = 0.22f + random.nextFloat() * 0.68f;
            d.radius = 16f + random.nextFloat() * 20f;
            d.weight = 0.7f + random.nextFloat() * 2.2f;
            d.phase = random.nextFloat() * 6.28f;
            d.metal = true;
            int roll = i % 10;
            if (roll == 0) {
                d.type = TYPE_OXYGEN;
                d.value = 24;
                d.metal = false;
            } else if (roll == 1) {
                d.type = TYPE_BATTERY;
                d.value = 26;
                d.metal = true;
            } else if (roll == 2 || roll == 3) {
                d.type = TYPE_MODULE;
                d.value = 1;
                d.weight += 0.9f;
            } else if (roll == 4) {
                d.type = TYPE_METEOR;
                d.value = 14;
                d.metal = false;
                d.vx = (random.nextBoolean() ? 1f : -1f) * (0.04f + random.nextFloat() * 0.05f);
            } else if (roll == 5) {
                d.type = TYPE_ROCK;
                d.value = 0;
                d.metal = false;
                d.weight += 0.5f;
            } else if (roll == 6) {
                d.type = TYPE_CORE;
                d.value = 3;
                d.weight += 1.2f;
            } else if (roll == 7) {
                d.type = TYPE_SNAG;
                d.value = 0;
                d.metal = true;
            } else {
                d.type = TYPE_SCRAP;
                d.value = 8 + random.nextInt(14);
            }
        }
    }

    private void launchHook() {
        float w = Math.max(1f, getWidth());
        float h = Math.max(1f, getHeight());
        float pivotY = h * 0.13f;
        float radians = (float) Math.toRadians(hookAngle);
        hookX = cartX * w;
        hookY = pivotY;
        hookDirX = (float) Math.sin(radians);
        hookDirY = (float) Math.cos(radians);
        hookLength = 0f;
        hookState = HOOK_OUT;
        caughtIndex = -1;
        battery -= boostTime > 0f ? 3f : 5f;
        notifyAudio("launch");
        notifyHud();
    }

    private void update(float dt) {
        if (state != STATE_PLAYING) {
            return;
        }
        elapsed += dt;
        railPulse += dt * Math.max(0.2f, Math.abs(cartMove) + 0.35f);
        flashTime = Math.max(0f, flashTime - dt);
        oxygen -= dt * (1.1f + region * 0.08f);
        scanTime = Math.max(0f, scanTime - dt);
        boostTime = Math.max(0f, boostTime - dt);
        cartX += cartMove * dt * 0.38f;
        cartX = clamp(cartX, 0.08f, 0.92f);
        hookAngle = (float) Math.sin(elapsed * 1.7f) * 72f;
        updateMeteor(dt);
        updateHook(dt);
        if (modules >= 6) {
            region++;
            modules = 0;
            oxygen = Math.min(100f, oxygen + 22f);
            battery = Math.min(100f, battery + 28f);
            hull = Math.min(100f, hull + 18f);
            spawnDebris();
            if (region > 6) {
                finishGame(true);
            }
        }
        if (oxygen <= 0f || battery <= 0f || hull <= 0f) {
            finishGame(false);
        }
        notifyHud();
    }

    private void updateMeteor(float dt) {
        for (Debris d : debris) {
            if (d.active && d.type == TYPE_METEOR) {
                d.phase += dt * 2.4f;
                d.x += d.vx * dt;
                if (d.x < 0.04f || d.x > 0.94f) {
                    d.vx = -d.vx;
                }
            }
        }
    }

    private void updateHook(float dt) {
        if (hookState == HOOK_READY) {
            return;
        }
        float w = Math.max(1f, getWidth());
        float h = Math.max(1f, getHeight());
        float pivotX = cartX * w;
        float pivotY = h * 0.13f;
        float speed = boostTime > 0f ? 640f : 500f;
        if (hookState == HOOK_OUT) {
            hookX += hookDirX * speed * dt;
            hookY += hookDirY * speed * dt;
            hookLength += speed * dt;
            int hit = findHit(hookX, hookY);
            if (hit >= 0) {
                Debris d = debris[hit];
                if (d.type == TYPE_METEOR) {
                    hull -= 16f;
                    d.active = false;
                    hookState = HOOK_RETURN;
                    lastMessage = "Meteor impact damaged the cart.";
                    notifyAudio("warning");
                } else if (!d.metal && d.type != TYPE_OXYGEN) {
                    battery -= 4f;
                    hookState = HOOK_RETURN;
                    lastMessage = "The hook bounced from moon rock.";
                    notifyAudio("warning");
                } else {
                    caughtIndex = hit;
                    hookState = HOOK_RETURN;
                    lastMessage = "Magnet lock secured.";
                }
            } else if (hookLength > h * 0.9f || hookX < 0f || hookX > w || hookY > h) {
                hookState = HOOK_RETURN;
            }
        } else if (hookState == HOOK_RETURN) {
            float dx = pivotX - hookX;
            float dy = pivotY - hookY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float weight = caughtIndex >= 0 ? debris[caughtIndex].weight : 1f;
            float rewind = speed * 0.95f / weight;
            if (dist <= rewind * dt || dist < 8f) {
                hookX = pivotX;
                hookY = pivotY;
                if (caughtIndex >= 0) {
                    deliver(debris[caughtIndex]);
                    debris[caughtIndex].active = false;
                }
                caughtIndex = -1;
                hookState = HOOK_READY;
            } else {
                hookX += dx / dist * rewind * dt;
                hookY += dy / dist * rewind * dt;
                if (caughtIndex >= 0) {
                    debris[caughtIndex].x = hookX / w;
                    debris[caughtIndex].y = hookY / h;
                }
            }
        }
    }

    private int findHit(float x, float y) {
        float w = Math.max(1f, getWidth());
        float h = Math.max(1f, getHeight());
        for (int i = 0; i < debris.length; i++) {
            Debris d = debris[i];
            if (!d.active) {
                continue;
            }
            float dx = d.x * w - x;
            float dy = d.y * h - y;
            float limit = d.radius + 13f + (boostTime > 0f && d.metal ? 18f : 0f);
            if (dx * dx + dy * dy <= limit * limit) {
                return i;
            }
        }
        return -1;
    }

    private void deliver(Debris d) {
        if (d.type == TYPE_OXYGEN) {
            oxygen = Math.min(100f, oxygen + d.value);
            lastMessage = "Oxygen recovered.";
            lastDelivery = "O2";
            flashTime = 0.55f;
            notifyAudio("collect");
        } else if (d.type == TYPE_BATTERY) {
            battery = Math.min(100f, battery + d.value);
            lastMessage = "Battery cell recovered.";
            lastDelivery = "PWR";
            flashTime = 0.55f;
            notifyAudio("collect");
        } else if (d.type == TYPE_MODULE) {
            modules += d.value;
            parts += 2;
            credits += 8;
            lastMessage = "Module part installed.";
            lastDelivery = "PART";
            flashTime = 0.65f;
            notifyAudio("repair");
        } else if (d.type == TYPE_CORE) {
            modules += d.value;
            parts += 5;
            credits += 20;
            lastMessage = "Rare module core secured.";
            lastDelivery = "CORE";
            flashTime = 0.75f;
            notifyAudio("repair");
        } else if (d.type == TYPE_SNAG) {
            battery -= 8f;
            parts += 1;
            lastMessage = "Cable snag cleared.";
            lastDelivery = "SNAG";
            flashTime = 0.45f;
            notifyAudio("warning");
        } else {
            parts += 1;
            credits += d.value;
            lastMessage = "Scrap delivered.";
            lastDelivery = "SCRAP";
            flashTime = 0.45f;
            notifyAudio("collect");
        }
    }

    private void finishGame(boolean won) {
        state = STATE_GAME_OVER;
        String message;
        if (won) {
            message = "Base launch dock restored. Credits " + credits + " Parts " + parts + ".";
            notifyAudio("win");
        } else {
            message = "Mission failed. Credits " + credits + " Parts " + parts + ".";
            notifyAudio("fail");
        }
        notifyState("GAME_OVER", message, true);
    }

    private void notifyHud() {
        if (listener != null) {
            listener.onHudChanged(
                    "O2 " + Math.max(0, Math.round(oxygen)),
                    "PWR " + Math.max(0, Math.round(battery)),
                    "PARTS " + parts,
                    String.format(Locale.US, "REGION %d REPAIR %d/6", Math.min(region, 6), modules));
        }
    }

    private void notifyState(String stateName, String message, boolean finalState) {
        if (listener != null) {
            listener.onStateChanged(stateName, message, finalState);
        }
    }

    private void notifyAudio(String cue) {
        if (listener != null) {
            listener.onAudioCue(cue);
        }
    }

    private boolean loadRuntimeArtMap() {
        try {
            InputStream stream = getContext().getAssets().open("game_art/runtime_art_map.json");
            stream.close();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        drawBackground(canvas, w, h);
        drawRegionPanel(canvas, w, h);
        drawDebris(canvas, w, h);
        drawRailAndCart(canvas, w, h);
        drawHook(canvas, w, h);
        drawStatus(canvas, w, h);
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(8, 12, 22));
        canvas.drawRect(0f, 0f, w, h, paint);
        paint.setColor(Color.rgb(14, 25, 42));
        canvas.drawRect(0f, 0f, w, h * 0.32f, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(50, 79, 227, 193));
        for (int i = 0; i < 13; i++) {
            float x = w * (0.06f + i * 0.075f);
            canvas.drawLine(x, h * 0.18f, x + 36f, h * 0.82f, paint);
        }
        paint.setColor(Color.rgb(26, 40, 48));
        for (int i = 0; i < 18; i++) {
            float x = (i * 73 % 100) * w / 100f;
            float y = (i * 47 % 80) * h / 100f;
            canvas.drawCircle(x, y, 1.8f + (i % 3), paint);
        }
        paint.setColor(Color.rgb(66, 84, 88));
        canvas.drawOval(new RectF(w * 0.63f, h * 0.09f, w * 0.86f, h * 0.25f), paint);
        paint.setColor(Color.rgb(26, 38, 44));
        canvas.drawOval(new RectF(w * 0.67f, h * 0.12f, w * 0.74f, h * 0.17f), paint);
        canvas.drawOval(new RectF(w * 0.76f, h * 0.17f, w * 0.82f, h * 0.21f), paint);
        paint.setColor(Color.rgb(39, 53, 57));
        RectF ground = new RectF(-40f, h * 0.78f, w + 40f, h + 80f);
        canvas.drawOval(ground, paint);
        paint.setColor(Color.rgb(19, 31, 35));
        for (int i = 0; i < 7; i++) {
            float cx = w * (0.08f + i * 0.15f);
            canvas.drawOval(new RectF(cx - 58f, h * 0.82f, cx + 70f, h * 0.98f), paint);
        }
        drawWreckLandmarks(canvas, w, h);
        if (scanTime > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(130, 79, 227, 193));
            canvas.drawCircle(cartX * w, h * 0.2f, h * (0.3f + scanTime * 0.02f), paint);
            paint.setStrokeWidth(2f);
            canvas.drawCircle(cartX * w, h * 0.2f, h * (0.18f + scanTime * 0.03f), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawWreckLandmarks(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(73, 91, 96));
        canvas.drawRoundRect(new RectF(w * 0.06f, h * 0.68f, w * 0.22f, h * 0.72f), 6f, 6f, paint);
        paint.setColor(Color.rgb(255, 209, 102));
        canvas.drawRect(w * 0.08f, h * 0.65f, w * 0.20f, h * 0.675f, paint);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.rgb(115, 167, 255));
        canvas.drawLine(w * 0.76f, h * 0.64f, w * 0.88f, h * 0.50f, paint);
        canvas.drawLine(w * 0.80f, h * 0.63f, w * 0.92f, h * 0.56f, paint);
        paint.setColor(Color.rgb(143, 163, 168));
        canvas.drawCircle(w * 0.74f, h * 0.65f, 18f, paint);
    }

    private void drawRegionPanel(Canvas canvas, float w, float h) {
        float left = w * 0.845f;
        float top = h * 0.18f;
        float right = w - 16f;
        float bottom = h * 0.68f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(150, 17, 27, 40));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12f, 12f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(49, 77, 88));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12f, 12f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(18f);
        textPaint.setColor(Color.rgb(232, 255, 248));
        canvas.drawText("BASE", left + 16f, top + 30f, textPaint);
        for (int i = 0; i < 6; i++) {
            float y = top + 55f + i * 34f;
            paint.setColor(i < modules ? Color.rgb(79, 227, 193) : Color.rgb(38, 55, 69));
            canvas.drawRoundRect(new RectF(left + 16f, y, right - 16f, y + 18f), 4f, 4f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(Color.rgb(115, 167, 255));
            canvas.drawRoundRect(new RectF(left + 16f, y, right - 16f, y + 18f), 4f, 4f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawRailAndCart(Canvas canvas, float w, float h) {
        float y = h * 0.13f;
        paint.setStrokeWidth(8f);
        paint.setColor(Color.rgb(143, 163, 168));
        canvas.drawLine(w * 0.06f, y, w * 0.94f, y, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(79, 227, 193));
        for (int i = 0; i < 12; i++) {
            float tick = w * (0.08f + i * 0.075f);
            canvas.drawLine(tick, y - 8f, tick, y + 8f, paint);
        }
        float x = cartX * w;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(20, 32, 44));
        canvas.drawRoundRect(new RectF(x - 56f, y - 33f, x + 56f, y + 21f), 12f, 12f, paint);
        paint.setColor(Color.rgb(46, 62, 70));
        canvas.drawRoundRect(new RectF(x - 47f, y - 27f, x + 46f, y + 14f), 9f, 9f, paint);
        paint.setColor(Color.rgb(79, 227, 193));
        canvas.drawRoundRect(new RectF(x - 32f, y - 18f, x + 16f, y + 2f), 6f, 6f, paint);
        paint.setColor(Color.rgb(232, 255, 248));
        canvas.drawRect(x - 25f, y - 13f, x - 4f, y - 8f, paint);
        paint.setColor(Color.rgb(255, 90, 95));
        canvas.drawRect(x + 24f, y - 15f, x + 38f, y - 5f, paint);
        paint.setColor(Color.rgb(255, 209, 102));
        float wheelA = (float) Math.sin(railPulse) * 2f;
        canvas.drawCircle(x + 36f, y + 18f, 10f + wheelA, paint);
        canvas.drawCircle(x - 36f, y + 18f, 10f - wheelA, paint);
        paint.setColor(Color.rgb(8, 16, 31));
        canvas.drawCircle(x + 36f, y + 18f, 4f, paint);
        canvas.drawCircle(x - 36f, y + 18f, 4f, paint);
    }

    private void drawHook(Canvas canvas, float w, float h) {
        float pivotX = cartX * w;
        float pivotY = h * 0.13f;
        float tipX;
        float tipY;
        if (hookState == HOOK_READY) {
            float radians = (float) Math.toRadians(hookAngle);
            tipX = pivotX + (float) Math.sin(radians) * 78f;
            tipY = pivotY + (float) Math.cos(radians) * 78f;
        } else {
            tipX = hookX;
            tipY = hookY;
        }
        paint.setStrokeWidth(4f);
        paint.setColor(Color.rgb(191, 235, 221));
        canvas.drawLine(pivotX, pivotY, tipX, tipY, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(boostTime > 0f ? 8f : 5f);
        paint.setColor(boostTime > 0f ? Color.rgb(255, 209, 102) : Color.rgb(79, 227, 193));
        canvas.drawCircle(tipX, tipY, boostTime > 0f ? 19f : 14f, paint);
        if (boostTime > 0f) {
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(160, 79, 227, 193));
            canvas.drawCircle(tipX, tipY, 30f + (float) Math.sin(elapsed * 12f) * 4f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(191, 235, 221));
        canvas.drawCircle(tipX, tipY, 6f, paint);
        paint.setStrokeWidth(4f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(191, 235, 221));
        canvas.drawArc(new RectF(tipX - 18f, tipY - 10f, tipX + 4f, tipY + 18f), 30f, 130f, false, paint);
        canvas.drawArc(new RectF(tipX - 4f, tipY - 10f, tipX + 18f, tipY + 18f), 20f, 130f, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawDebris(Canvas canvas, float w, float h) {
        for (Debris d : debris) {
            if (!d.active) {
                continue;
            }
            float x = d.x * w;
            float y = d.y * h;
            if (scanTime > 0f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setColor(d.metal ? Color.rgb(79, 227, 193) : Color.rgb(255, 209, 102));
                canvas.drawCircle(x, y, d.radius + 7f, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            if (d.type == TYPE_OXYGEN) {
                paint.setColor(Color.rgb(115, 167, 255));
                canvas.drawRoundRect(new RectF(x - 14f, y - 24f, x + 14f, y + 24f), 8f, 8f, paint);
                paint.setColor(Color.rgb(255, 90, 95));
                canvas.drawRect(x - 6f, y - 31f, x + 6f, y - 22f, paint);
                paint.setColor(Color.rgb(232, 255, 248));
                canvas.drawRect(x - 10f, y - 4f, x + 10f, y + 5f, paint);
                textPaint.setTextSize(12f);
                textPaint.setColor(Color.rgb(8, 16, 31));
                canvas.drawText("O2", x - 7f, y + 3f, textPaint);
            } else if (d.type == TYPE_BATTERY) {
                paint.setColor(Color.rgb(255, 209, 102));
                canvas.drawRoundRect(new RectF(x - 24f, y - 14f, x + 24f, y + 14f), 5f, 5f, paint);
                paint.setColor(Color.rgb(79, 227, 193));
                canvas.drawRect(x - 14f, y - 8f, x + 14f, y + 8f, paint);
                paint.setColor(Color.rgb(232, 255, 248));
                canvas.drawRect(x + 24f, y - 5f, x + 30f, y + 5f, paint);
            } else if (d.type == TYPE_MODULE || d.type == TYPE_CORE) {
                paint.setColor(d.type == TYPE_CORE ? Color.rgb(255, 209, 102) : Color.rgb(143, 163, 168));
                Path p = new Path();
                p.moveTo(x, y - d.radius);
                p.lineTo(x + d.radius, y);
                p.lineTo(x, y + d.radius);
                p.lineTo(x - d.radius, y);
                p.close();
                canvas.drawPath(p, paint);
                paint.setColor(Color.rgb(79, 227, 193));
                canvas.drawCircle(x, y, d.radius * 0.32f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(Color.rgb(232, 255, 248));
                canvas.drawCircle(x, y, d.radius * 0.55f, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (d.type == TYPE_METEOR) {
                paint.setColor(Color.rgb(120, 80, 76));
                canvas.drawOval(new RectF(x - d.radius, y - d.radius * 0.7f, x + d.radius, y + d.radius * 0.7f), paint);
                paint.setColor(Color.rgb(255, 90, 95));
                canvas.drawCircle(x - d.radius * 0.25f, y - d.radius * 0.1f, d.radius * 0.28f, paint);
            } else if (d.type == TYPE_ROCK) {
                paint.setColor(Color.rgb(94, 103, 104));
                canvas.drawCircle(x, y, d.radius, paint);
                paint.setColor(Color.rgb(60, 70, 72));
                canvas.drawCircle(x - d.radius * 0.25f, y - d.radius * 0.15f, d.radius * 0.28f, paint);
            } else if (d.type == TYPE_SNAG) {
                paint.setColor(Color.rgb(210, 220, 218));
                canvas.drawRect(x - d.radius, y - 7f, x + d.radius, y + 7f, paint);
                paint.setColor(Color.rgb(255, 90, 95));
                canvas.drawCircle(x + d.radius * 0.6f, y, 6f, paint);
            } else {
                paint.setColor(Color.rgb(180, 193, 196));
                canvas.drawRoundRect(new RectF(x - d.radius, y - d.radius * 0.65f, x + d.radius, y + d.radius * 0.65f), 5f, 5f, paint);
                paint.setColor(Color.rgb(79, 227, 193));
                canvas.drawRect(x - d.radius * 0.6f, y - 4f, x + d.radius * 0.6f, y + 4f, paint);
                paint.setColor(Color.rgb(255, 209, 102));
                canvas.drawLine(x - d.radius * 0.7f, y - d.radius * 0.35f, x + d.radius * 0.7f, y + d.radius * 0.35f, paint);
            }
        }
    }

    private void drawStatus(Canvas canvas, float w, float h) {
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.rgb(191, 235, 221));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(150, 13, 23, 35));
        canvas.drawRoundRect(new RectF(16f, h - 54f, w * 0.60f, h - 14f), 10f, 10f, paint);
        canvas.drawText(lastMessage, 30f, h - 27f, textPaint);
        paint.setStyle(Paint.Style.FILL);
        drawSegmentedMeter(canvas, 24f, 22f, 160f, 14f, hull / 100f, Color.rgb(79, 227, 193), "HULL");
        drawSegmentedMeter(canvas, w - 190f, 22f, 160f, 14f, oxygen / 100f, Color.rgb(115, 167, 255), "O2");
        drawSegmentedMeter(canvas, w - 190f, 48f, 160f, 14f, battery / 100f, Color.rgb(255, 209, 102), "PWR");
        if (flashTime > 0f && lastDelivery.length() > 0) {
            textPaint.setTextSize(36f);
            textPaint.setColor(Color.argb((int) (220f * Math.min(1f, flashTime * 2f)), 255, 209, 102));
            canvas.drawText(lastDelivery, w * 0.44f, h * 0.22f - flashTime * 18f, textPaint);
        }
    }

    private void drawSegmentedMeter(Canvas canvas, float x, float y, float width, float height, float value, int color, String label) {
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.rgb(191, 235, 221));
        canvas.drawText(label, x, y - 4f, textPaint);
        float segW = (width - 18f) / 8f;
        for (int i = 0; i < 8; i++) {
            float left = x + i * (segW + 2f);
            paint.setColor(i < Math.round(clamp(value, 0f, 1f) * 8f) ? color : Color.rgb(39, 55, 69));
            canvas.drawRoundRect(new RectF(left, y, left + segW, y + height), 3f, 3f, paint);
        }
    }

    private void drawMeter(Canvas canvas, float x, float y, float width, float height, float value, int color) {
        paint.setColor(Color.rgb(39, 65, 58));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 4f, 4f, paint);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(x, y, x + width * clamp(value, 0f, 1f), y + height), 4f, 4f, paint);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Debris {
        boolean active;
        boolean metal;
        int type;
        int value;
        float x;
        float y;
        float vx;
        float radius;
        float weight;
        float phase;
    }
}
