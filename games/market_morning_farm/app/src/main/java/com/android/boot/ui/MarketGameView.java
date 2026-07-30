package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MarketGameView extends View {
    public static final int UPGRADE_PLOT = 0;
    public static final int UPGRADE_CRATE = 1;
    public static final int UPGRADE_SHELF = 2;
    public static final int UPGRADE_WATER = 3;
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_RESULT = 3;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - lastTime) / 1000000000f);
            lastTime = now;
            if (state == STATE_PLAYING) {
                update(dt);
            }
            invalidate();
            handler.postDelayed(this, 16);
        }
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(7);
    private final CropDef[] crops = {
            new CropDef("Carrot", 4.5f, 4, 9, Color.rgb(245, 131, 32)),
            new CropDef("Tomato", 6.0f, 6, 13, Color.rgb(229, 61, 61)),
            new CropDef("Corn", 7.0f, 7, 16, Color.rgb(250, 205, 70)),
            new CropDef("Berry", 5.5f, 8, 18, Color.rgb(194, 58, 132)),
            new CropDef("Pumpkin", 9.0f, 10, 24, Color.rgb(235, 137, 38)),
            new CropDef("Herb", 3.8f, 5, 11, Color.rgb(63, 177, 92))
    };
    private final CustomerDef[] customerDefs = {
            new CustomerDef("Bargain", 0, 1, 8.0f, 0),
            new CustomerDef("Chef", 1, 2, 7.0f, 1),
            new CustomerDef("Family", 2, 4, 9.0f, 1),
            new CustomerDef("Collector", 3, 5, 6.5f, 2),
            new CustomerDef("Commuter", 0, 3, 5.8f, 1)
    };
    private final Plot[] plots = new Plot[12];
    private final int[] storage = new int[6];
    private final StallSlot[] shelves = new StallSlot[3];
    private final List<Customer> customers = new ArrayList<>();
    private final List<Feedback> feedbacks = new ArrayList<>();
    private GameListener listener;
    private int state = STATE_MENU;
    private int selectedSeed = 0;
    private int draggedCrop = -1;
    private int day = 1;
    private int revenue;
    private int targetRevenue = 120;
    private int reputation = 5;
    private int bankCoins = 30;
    private int[] upgradeLevels = new int[4];
    private int trendCrop;
    private int specialCrop;
    private boolean specialServed;
    private float dayTime = 180f;
    private float spawnTimer;
    private float lastHudPush;
    private long lastTime;
    private boolean resultVisible;

    public MarketGameView(Context context) {
        super(context);
        init();
    }

    public MarketGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(Color.rgb(23, 35, 59));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24f);
        for (int i = 0; i < plots.length; i++) {
            plots[i] = new Plot();
        }
        for (int i = 0; i < shelves.length; i++) {
            shelves[i] = new StallSlot();
        }
        lastTime = System.nanoTime();
        handler.post(tick);
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    public boolean isResultVisible() {
        return resultVisible;
    }

    public int getBankCoins() {
        return bankCoins;
    }

    public int getUpgradeLevel(int type) {
        return upgradeLevels[type];
    }

    public void shutdown() {
        handler.removeCallbacks(tick);
    }

    public void selectSeed(int crop) {
        selectedSeed = crop;
        addFeedback(crops[crop].name + " seeds", getWidth() * 0.5f, getHeight() - 72f, Color.rgb(46, 166, 255));
    }

    public void startDay() {
        state = STATE_PLAYING;
        resultVisible = false;
        revenue = 0;
        reputation = 5;
        dayTime = Math.max(120f, 180f - day * 4f);
        targetRevenue = 110 + day * 25;
        trendCrop = day % crops.length;
        specialCrop = (day + 2) % crops.length;
        specialServed = false;
        spawnTimer = 2f;
        customers.clear();
        feedbacks.clear();
        for (Plot plot : plots) {
            plot.crop = -1;
            plot.growth = 0f;
            plot.water = 0f;
            plot.wither = 0f;
        }
        for (StallSlot shelf : shelves) {
            shelf.crop = -1;
            shelf.count = 0;
            shelf.priceTier = 1;
        }
        for (int i = 0; i < storage.length; i++) {
            storage[i] = 1 + upgradeLevels[UPGRADE_CRATE] / 2;
        }
        pushHud();
        lastTime = System.nanoTime();
    }

    public void stopToMenu() {
        state = STATE_MENU;
        resultVisible = false;
        invalidate();
    }

    public void pauseGame() {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED;
        }
    }

    public void resumeGame() {
        if (state == STATE_PAUSED) {
            state = STATE_PLAYING;
            lastTime = System.nanoTime();
        }
    }

    public void buyUpgrade(int type) {
        int cost = 25 + upgradeLevels[type] * 20;
        if (bankCoins >= cost) {
            bankCoins -= cost;
            upgradeLevels[type]++;
            addFeedback("Upgrade bought", getWidth() * 0.5f, getHeight() * 0.5f, Color.rgb(46, 214, 122));
        } else {
            addFeedback("Need " + cost + " coins", getWidth() * 0.5f, getHeight() * 0.5f, Color.rgb(255, 77, 77));
        }
    }

    private void update(float dt) {
        dayTime -= dt;
        spawnTimer -= dt;
        for (Plot plot : plots) {
            if (plot.crop >= 0) {
                CropDef def = crops[plot.crop];
                float speed = 1f + upgradeLevels[UPGRADE_PLOT] * 0.12f + plot.water;
                plot.growth += dt * speed;
                plot.water = Math.max(0f, plot.water - dt * 0.18f);
                if (plot.growth >= def.growTime) {
                    plot.wither += dt;
                }
                if (plot.wither > 14f) {
                    plot.crop = -1;
                    plot.growth = 0f;
                    plot.wither = 0f;
                    addFeedback("Withered", getWidth() * 0.45f, getHeight() * 0.35f, Color.rgb(255, 77, 77));
                }
            }
        }
        if (spawnTimer <= 0f) {
            spawnCustomer();
            spawnTimer = Math.max(3.0f, 8.0f - day * 0.2f - customers.size() * 0.4f);
        }
        updateCustomers(dt);
        updateFeedback(dt);
        if (dayTime <= 0f || reputation <= 0) {
            finishDay(revenue >= targetRevenue && reputation > 0);
        }
        lastHudPush += dt;
        if (lastHudPush > 0.2f) {
            pushHud();
            lastHudPush = 0f;
        }
    }

    private void updateCustomers(float dt) {
        for (int i = customers.size() - 1; i >= 0; i--) {
            Customer c = customers.get(i);
            c.x += (c.targetX - c.x) * dt * 2.2f;
            c.patience -= dt;
            if (tryServe(c)) {
                customers.remove(i);
            } else if (c.patience <= 0f) {
                reputation--;
                addFeedback("Complaint", c.x, c.y - 30f, Color.rgb(255, 77, 77));
                customers.remove(i);
            }
        }
    }

    private boolean tryServe(Customer c) {
        for (StallSlot shelf : shelves) {
            if (shelf.crop == c.wantCrop && shelf.count > 0 && shelf.priceTier <= c.def.toleranceTier) {
                CropDef crop = crops[shelf.crop];
                int bonus = upgradeLevels[UPGRADE_SHELF] * 2 + shelf.priceTier * 3;
                int earned = crop.basePrice + bonus;
                if (shelf.crop == trendCrop) {
                    earned += 4 + day;
                }
                revenue += earned;
                bankCoins += earned;
                if (shelf.priceTier == 0 || shelf.crop == c.bonusCrop) {
                    reputation = Math.min(9, reputation + 1);
                }
                if (!specialServed && shelf.crop == specialCrop) {
                    specialServed = true;
                    revenue += 18;
                    bankCoins += 18;
                    reputation = Math.min(9, reputation + 2);
                    addFeedback("Special +18", c.x, c.y - 62f, Color.rgb(255, 176, 32));
                }
                shelf.count--;
                if (shelf.count <= 0) {
                    shelf.crop = -1;
                    shelf.priceTier = 1;
                }
                addFeedback("+" + earned, c.x, c.y - 36f, Color.rgb(46, 214, 122));
                return true;
            }
        }
        return false;
    }

    private void spawnCustomer() {
        CustomerDef def = customerDefs[(day + customers.size() + random.nextInt(customerDefs.length)) % customerDefs.length];
        Customer c = new Customer();
        c.def = def;
        if (random.nextInt(100) < 38) {
            c.wantCrop = trendCrop;
        } else if (!specialServed && random.nextInt(100) < 24) {
            c.wantCrop = specialCrop;
        } else {
            c.wantCrop = (def.favoriteCrop + day + random.nextInt(2)) % crops.length;
        }
        c.bonusCrop = def.bonusCrop;
        c.patience = def.patience + upgradeLevels[UPGRADE_SHELF] * 0.5f;
        c.x = -40f;
        c.y = getHeight() * 0.70f + customers.size() * 24f;
        c.targetX = getWidth() * (0.63f + customers.size() * 0.06f);
        customers.add(c);
    }

    private void finishDay(boolean won) {
        state = STATE_RESULT;
        resultVisible = true;
        if (won) {
            day++;
            bankCoins += 20 + reputation * 2;
        }
        String special = specialServed ? "Special served" : "Special missed";
        String summary = String.format(Locale.US, "Coins %d/%d  Rep %d  Bank %d  %s", revenue, targetRevenue, reputation, bankCoins, special);
        if (listener != null) {
            listener.onDayFinished(won, summary);
        }
        pushHud();
    }

    private void pushHud() {
        if (listener != null) {
            listener.onHudChanged("Day " + day + "  " + Math.max(0, (int) dayTime) + "s", "Coins " + revenue + "/" + targetRevenue, "Rep " + reputation);
        }
    }

    private void updateFeedback(float dt) {
        for (int i = feedbacks.size() - 1; i >= 0; i--) {
            Feedback f = feedbacks.get(i);
            f.life -= dt;
            f.y -= dt * 28f;
            if (f.life <= 0f) {
                feedbacks.remove(i);
            }
        }
    }

    private void addFeedback(String text, float x, float y, int color) {
        Feedback f = new Feedback();
        f.text = text;
        f.x = x;
        f.y = y;
        f.color = color;
        f.life = 1.1f;
        feedbacks.add(f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawForecast(canvas);
        drawGarden(canvas);
        drawStall(canvas);
        drawCustomers(canvas);
        drawFeedback(canvas);
        drawSelectedSeed(canvas);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.rgb(235, 246, 255));
        paint.setColor(Color.rgb(255, 235, 154));
        canvas.drawRect(0, 50, getWidth(), 92, paint);
        paint.setColor(Color.rgb(255, 178, 76));
        for (int i = 0; i < 12; i++) {
            float left = i * getWidth() / 12f;
            canvas.drawRect(left, 50, left + getWidth() / 24f, 92, paint);
        }
        paint.setColor(Color.rgb(214, 238, 205));
        canvas.drawRect(0, 92, getWidth(), getHeight() * 0.66f, paint);
        paint.setColor(Color.rgb(223, 187, 132));
        canvas.drawRect(0, getHeight() * 0.66f, getWidth(), getHeight(), paint);
    }

    private void drawGarden(Canvas canvas) {
        float left = getWidth() * 0.08f;
        float top = getHeight() * 0.16f;
        float cellW = getWidth() * 0.13f;
        float cellH = getHeight() * 0.13f;
        for (int i = 0; i < plots.length; i++) {
            int col = i % 4;
            int row = i / 4;
            float x = left + col * cellW * 1.15f;
            float y = top + row * cellH * 1.12f;
            RectF r = new RectF(x, y, x + cellW, y + cellH);
            plots[i].bounds.set(r);
            paint.setColor(Color.rgb(154, 102, 60));
            canvas.drawRoundRect(r, 12f, 12f, paint);
            paint.setColor(Color.rgb(111, 78, 45));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            canvas.drawRoundRect(r, 12f, 12f, paint);
            paint.setStyle(Paint.Style.FILL);
            if (plots[i].crop >= 0) {
                drawCrop(canvas, plots[i], r);
            }
        }
    }

    private void drawForecast(Canvas canvas) {
        float left = getWidth() * 0.62f;
        float top = getHeight() * 0.15f;
        RectF sign = new RectF(left, top, getWidth() * 0.94f, top + getHeight() * 0.18f);
        paint.setColor(Color.rgb(61, 95, 79));
        canvas.drawRoundRect(sign, 14f, 14f, paint);
        paint.setColor(Color.rgb(255, 248, 219));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawRoundRect(sign, 14f, 14f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(20f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Forecast", sign.centerX(), sign.top + 28f, textPaint);
        textPaint.setTextSize(18f);
        canvas.drawText("Trend " + crops[trendCrop].name, sign.centerX(), sign.top + 58f, textPaint);
        textPaint.setTextSize(16f);
        canvas.drawText("Special " + crops[specialCrop].name, sign.centerX(), sign.top + 84f, textPaint);
    }

    private void drawCrop(Canvas canvas, Plot plot, RectF r) {
        CropDef def = crops[plot.crop];
        float progress = Math.min(1f, plot.growth / def.growTime);
        paint.setColor(Color.rgb(65, 163, 83));
        float cx = r.centerX();
        float base = r.bottom - 16f;
        canvas.drawOval(new RectF(cx - 12f, base - 22f, cx, base - 6f), paint);
        canvas.drawOval(new RectF(cx, base - 26f, cx + 14f, base - 6f), paint);
        paint.setColor(def.color);
        float size = 10f + progress * 22f;
        if (progress >= 0.98f) {
            canvas.drawCircle(cx, r.centerY() + 4f, size, paint);
            textPaint.setTextSize(18f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Ready", cx, r.top + 24f, textPaint);
        } else {
            canvas.drawCircle(cx, r.centerY() + 10f, size * 0.6f, paint);
        }
        if (plot.water > 0.2f) {
            paint.setColor(Color.rgb(46, 214, 255));
            canvas.drawCircle(r.right - 16f, r.top + 16f, 8f, paint);
        }
    }

    private void drawStall(Canvas canvas) {
        float top = getHeight() * 0.66f;
        paint.setColor(Color.rgb(176, 111, 57));
        canvas.drawRoundRect(new RectF(getWidth() * 0.05f, top, getWidth() * 0.95f, getHeight() - 62f), 16f, 16f, paint);
        textPaint.setTextSize(20f);
        textPaint.setColor(Color.rgb(23, 35, 59));
        canvas.drawText("Crate", getWidth() * 0.14f, top + 28f, textPaint);
        for (int i = 0; i < crops.length; i++) {
            float x = getWidth() * 0.06f + i * getWidth() * 0.07f;
            float y = top + 42f;
            RectF r = new RectF(x, y, x + getWidth() * 0.055f, y + 52f);
            paint.setColor(crops[i].color);
            canvas.drawRoundRect(r, 10f, 10f, paint);
            textPaint.setTextSize(16f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(storage[i]), r.centerX(), r.centerY() + 6f, textPaint);
        }
        for (int i = 0; i < shelves.length; i++) {
            float x = getWidth() * (0.52f + i * 0.14f);
            RectF r = new RectF(x, top + 36f, x + getWidth() * 0.115f, top + 104f);
            shelves[i].bounds.set(r);
            paint.setColor(Color.rgb(255, 248, 219));
            canvas.drawRoundRect(r, 12f, 12f, paint);
            paint.setColor(Color.rgb(91, 63, 39));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            canvas.drawRoundRect(r, 12f, 12f, paint);
            paint.setStyle(Paint.Style.FILL);
            if (shelves[i].crop >= 0) {
                paint.setColor(crops[shelves[i].crop].color);
                canvas.drawCircle(r.centerX(), r.centerY() - 8f, 18f, paint);
                textPaint.setColor(Color.rgb(23, 35, 59));
                textPaint.setTextSize(16f);
                canvas.drawText("x" + shelves[i].count + " $" + priceLabel(shelves[i].priceTier), r.centerX(), r.bottom - 10f, textPaint);
            } else {
                textPaint.setColor(Color.rgb(93, 123, 176));
                textPaint.setTextSize(15f);
                canvas.drawText("Stock", r.centerX(), r.centerY() + 5f, textPaint);
            }
        }
    }

    private String priceLabel(int tier) {
        if (tier == 0) {
            return "Low";
        }
        if (tier == 2) {
            return "High";
        }
        return "Fair";
    }

    private void drawCustomers(Canvas canvas) {
        for (Customer c : customers) {
            int base = c.def.toleranceTier == 0 ? Color.rgb(70, 150, 220) : c.def.toleranceTier == 1 ? Color.rgb(125, 95, 210) : Color.rgb(230, 120, 60);
            paint.setColor(base);
            canvas.drawCircle(c.x, c.y, 22f, paint);
            paint.setColor(Color.rgb(255, 230, 190));
            canvas.drawCircle(c.x, c.y - 18f, 13f, paint);
            paint.setColor(Color.WHITE);
            RectF bubble = new RectF(c.x - 46f, c.y - 70f, c.x + 46f, c.y - 38f);
            canvas.drawRoundRect(bubble, 14f, 14f, paint);
            textPaint.setColor(crops[c.wantCrop].color);
            textPaint.setTextSize(15f);
            canvas.drawText(crops[c.wantCrop].name, bubble.centerX(), bubble.centerY() + 5f, textPaint);
            paint.setColor(Color.rgb(255, 77, 77));
            canvas.drawRect(c.x - 28f, c.y + 28f, c.x + 28f, c.y + 34f, paint);
            paint.setColor(Color.rgb(46, 214, 122));
            canvas.drawRect(c.x - 28f, c.y + 28f, c.x - 28f + 56f * Math.max(0f, c.patience / c.def.patience), c.y + 34f, paint);
        }
    }

    private void drawFeedback(Canvas canvas) {
        textPaint.setTextSize(22f);
        for (Feedback f : feedbacks) {
            textPaint.setColor(f.color);
            canvas.drawText(f.text, f.x, f.y, textPaint);
        }
    }

    private void drawSelectedSeed(Canvas canvas) {
        if (state == STATE_PLAYING) {
            paint.setColor(crops[selectedSeed].color);
            canvas.drawCircle(getWidth() - 42f, getHeight() - 92f, 20f, paint);
            textPaint.setColor(Color.rgb(23, 35, 59));
            textPaint.setTextSize(16f);
            canvas.drawText(crops[selectedSeed].name, getWidth() - 90f, getHeight() - 88f, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != STATE_PLAYING) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            draggedCrop = storageHit(x, y);
            if (draggedCrop >= 0) {
                return true;
            }
            handleTap(x, y);
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (draggedCrop >= 0) {
                stockShelfAt(x, y, draggedCrop);
                draggedCrop = -1;
            }
            return true;
        }
        return true;
    }

    private void handleTap(float x, float y) {
        for (Plot plot : plots) {
            if (plot.bounds.contains(x, y)) {
                tapPlot(plot);
                return;
            }
        }
        for (StallSlot shelf : shelves) {
            if (shelf.bounds.contains(x, y)) {
                tapShelf(shelf);
                return;
            }
        }
    }

    private void tapPlot(Plot plot) {
        if (plot.crop < 0) {
            CropDef def = crops[selectedSeed];
            if (bankCoins >= def.seedCost) {
                bankCoins -= def.seedCost;
                plot.crop = selectedSeed;
                plot.growth = 0f;
                plot.water = 0.5f + upgradeLevels[UPGRADE_WATER] * 0.1f;
                plot.wither = 0f;
                addFeedback("Planted", plot.bounds.centerX(), plot.bounds.top - 8f, Color.rgb(46, 166, 255));
            } else {
                addFeedback("Need coins", plot.bounds.centerX(), plot.bounds.top - 8f, Color.rgb(255, 77, 77));
            }
            return;
        }
        CropDef def = crops[plot.crop];
        if (plot.growth >= def.growTime) {
            int crateLimit = 6 + upgradeLevels[UPGRADE_CRATE] * 3;
            int yield = plot.wither > 7f ? 1 : 2;
            if (storage[plot.crop] + yield <= crateLimit) {
                storage[plot.crop] += yield;
                addFeedback("Harvest +" + yield, plot.bounds.centerX(), plot.bounds.top - 8f, Color.rgb(46, 214, 122));
                plot.crop = -1;
                plot.growth = 0f;
                plot.wither = 0f;
            } else {
                addFeedback("Crate full", plot.bounds.centerX(), plot.bounds.top - 8f, Color.rgb(255, 176, 32));
            }
        } else {
            plot.water = Math.min(1.0f, plot.water + 0.45f + upgradeLevels[UPGRADE_WATER] * 0.08f);
            addFeedback("Watered", plot.bounds.centerX(), plot.bounds.top - 8f, Color.rgb(46, 214, 255));
        }
    }

    private void tapShelf(StallSlot shelf) {
        if (shelf.crop >= 0) {
            shelf.priceTier = (shelf.priceTier + 1) % 3;
            addFeedback(priceLabel(shelf.priceTier), shelf.bounds.centerX(), shelf.bounds.top - 8f, Color.rgb(123, 97, 255));
        } else if (storage[selectedSeed] > 0) {
            shelf.crop = selectedSeed;
            shelf.count = 1;
            shelf.priceTier = 1;
            storage[selectedSeed]--;
            addFeedback("Stocked", shelf.bounds.centerX(), shelf.bounds.top - 8f, Color.rgb(46, 214, 122));
        } else {
            addFeedback("No " + crops[selectedSeed].name, shelf.bounds.centerX(), shelf.bounds.top - 8f, Color.rgb(255, 176, 32));
        }
    }

    private int storageHit(float x, float y) {
        float top = getHeight() * 0.66f;
        for (int i = 0; i < crops.length; i++) {
            float left = getWidth() * 0.06f + i * getWidth() * 0.07f;
            RectF r = new RectF(left, top + 42f, left + getWidth() * 0.055f, top + 94f);
            if (r.contains(x, y) && storage[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    private void stockShelfAt(float x, float y, int crop) {
        for (StallSlot shelf : shelves) {
            if (shelf.bounds.contains(x, y)) {
                if (shelf.crop < 0 || shelf.crop == crop) {
                    shelf.crop = crop;
                    shelf.count++;
                    storage[crop]--;
                    addFeedback("Stocked", shelf.bounds.centerX(), shelf.bounds.top - 8f, Color.rgb(46, 214, 122));
                } else {
                    addFeedback("Shelf busy", shelf.bounds.centerX(), shelf.bounds.top - 8f, Color.rgb(255, 176, 32));
                }
                return;
            }
        }
    }

    private static final class CropDef {
        final String name;
        final float growTime;
        final int seedCost;
        final int basePrice;
        final int color;

        CropDef(String name, float growTime, int seedCost, int basePrice, int color) {
            this.name = name;
            this.growTime = growTime;
            this.seedCost = seedCost;
            this.basePrice = basePrice;
            this.color = color;
        }
    }

    private static final class CustomerDef {
        final String name;
        final int favoriteCrop;
        final int bonusCrop;
        final float patience;
        final int toleranceTier;

        CustomerDef(String name, int favoriteCrop, int bonusCrop, float patience, int toleranceTier) {
            this.name = name;
            this.favoriteCrop = favoriteCrop;
            this.bonusCrop = bonusCrop;
            this.patience = patience;
            this.toleranceTier = toleranceTier;
        }
    }

    private static final class Plot {
        final RectF bounds = new RectF();
        int crop = -1;
        float growth;
        float water;
        float wither;
    }

    private static final class StallSlot {
        final RectF bounds = new RectF();
        int crop = -1;
        int count;
        int priceTier = 1;
    }

    private static final class Customer {
        CustomerDef def;
        int wantCrop;
        int bonusCrop;
        float x;
        float y;
        float targetX;
        float patience;
    }

    private static final class Feedback {
        String text;
        float x;
        float y;
        float life;
        int color;
    }

    public interface GameListener {
        void onHudChanged(String dayTime, String revenue, String reputation);

        void onDayFinished(boolean won, String summary);
    }
}
