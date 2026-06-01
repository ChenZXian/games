package com.android.boot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Random;

public class RanchGameView extends View {
    public interface GameListener {
        void onGameOver();

        default void onSound(String role) {
        }

        default void onDayClear() {
        }
    }

    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private static final int QUALITY_STANDARD = 0;
    private static final int QUALITY_FRESH = 1;
    private static final int QUALITY_PREMIUM = 2;
    private static final int QUALITY_PERFECT = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(7);
    private final Recipe[] recipes = new Recipe[] {
            new Recipe("Sweet Grain", "milk", 0, 8),
            new Recipe("Meadow Roll", "egg", 1, 7),
            new Recipe("Apple Crunch", "wool", 2, 9),
            new Recipe("Carrot Nibble", "fur", 5, 6),
            new Recipe("Herb Comfort", "goat milk", 3, 10)
    };
    private final Animal[] animals = new Animal[] {
            new Animal("Cow", "milk", 0),
            new Animal("Hen", "egg", 1),
            new Animal("Sheep", "wool", 2),
            new Animal("Goat", "goat milk", 4),
            new Animal("Piglet", "truffle", 0),
            new Animal("Rabbit", "fur", 3)
    };
    private final Order[] orders = new Order[3];
    private final int[][] inventory = new int[6][4];
    private final int[] recipeStock = new int[recipes.length];
    private final RectF[] recipeRects = new RectF[recipes.length];
    private final RectF[] orderRects = new RectF[orders.length];
    private final RectF playRect = new RectF();
    private final RectF leftPanel = new RectF();
    private final RectF rightPanel = new RectF();
    private final RectF topHud = new RectF();
    private final RectF coolerRect = new RectF();
    private Bitmap pigBadge;
    private Bitmap rabbitBadge;
    private GameState state = GameState.MENU;
    private GameListener listener;
    private long lastTick;
    private boolean loopRunning;
    private int draggingRecipe = -1;
    private float dragX;
    private float dragY;
    private int coins;
    private int reputation;
    private int targetReputation;
    private int day;
    private float dayTime;
    private float stockTimer;
    private float toastTimer;
    private String toast = "Drag recipes to animals";
    private int cBgMain;
    private int cPanel;
    private int cPanelStroke;
    private int cText;
    private int cMuted;
    private int cAccent;
    private int cAccent2;
    private int cSuccess;
    private int cWarning;
    private int cDanger;
    private int cGrass;
    private int cGrassDark;
    private int cPath;
    private int cStraw;
    private int cBarn;
    private int cWood;
    private int cWater;

    public RanchGameView(Context context) {
        super(context);
        init();
    }

    public RanchGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public void startGame() {
        coins = 40;
        reputation = 0;
        targetReputation = 80;
        day = 1;
        dayTime = 240f;
        stockTimer = 0f;
        draggingRecipe = -1;
        for (int i = 0; i < recipeStock.length; i++) {
            recipeStock[i] = 3;
        }
        for (int i = 0; i < inventory.length; i++) {
            for (int j = 0; j < inventory[i].length; j++) {
                inventory[i][j] = 0;
            }
        }
        for (Animal animal : animals) {
            animal.timer = 0f;
            animal.ready = false;
            animal.fed = false;
            animal.mood = 72f;
            animal.quality = QUALITY_STANDARD;
            animal.walkPhase = random.nextFloat() * 6f;
        }
        for (int i = 0; i < orders.length; i++) {
            orders[i] = makeOrder(i);
        }
        state = GameState.PLAYING;
        toast = "Drag a snack card to a favorite animal";
        toastTimer = 3f;
        lastTick = SystemClock.uptimeMillis();
        invalidate();
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastTick = SystemClock.uptimeMillis();
        }
    }

    public void resumeLoop() {
        if (!loopRunning) {
            loopRunning = true;
            lastTick = SystemClock.uptimeMillis();
            postOnAnimation(loopStep);
        }
    }

    public void pauseLoop() {
        loopRunning = false;
        removeCallbacks(loopStep);
    }

    public void showHelpToast() {
        toast = "Drag recipe cards to animals. Collect products. Stamp matching orders.";
        toastTimer = 5f;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        pauseLoop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        computeLayout();
        drawBackground(canvas);
        drawRanch(canvas);
        drawAnimals(canvas);
        drawPanels(canvas);
        drawHud(canvas);
        drawToast(canvas);
        if (draggingRecipe >= 0) {
            drawRecipeCard(canvas, recipes[draggingRecipe], draggingRecipe, dragX - playRect.width() * 0.045f, dragY - playRect.height() * 0.04f, playRect.width() * 0.09f, playRect.height() * 0.08f, true);
        }
        if (state == GameState.GAME_OVER) {
            drawEndBanner(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != GameState.PLAYING) {
            return true;
        }
        computeLayout();
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            for (int i = 0; i < recipeRects.length; i++) {
                if (recipeRects[i] != null && recipeRects[i].contains(x, y) && recipeStock[i] > 0) {
                    draggingRecipe = i;
                    dragX = x;
                    dragY = y;
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < animals.length; i++) {
                if (animals[i].bounds.contains(x, y) && animals[i].ready) {
                    collectAnimal(i);
                    return true;
                }
            }
            for (int i = 0; i < orders.length; i++) {
                if (orderRects[i] != null && orderRects[i].contains(x, y)) {
                    deliverOrder(i);
                    return true;
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (draggingRecipe >= 0) {
                dragX = x;
                dragY = y;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (draggingRecipe >= 0) {
                int recipe = draggingRecipe;
                draggingRecipe = -1;
                for (int i = 0; i < animals.length; i++) {
                    if (animals[i].bounds.contains(x, y)) {
                        feedAnimal(i, recipe);
                        invalidate();
                        return true;
                    }
                }
                toast = "Drop snacks onto an animal pen";
                toastTimer = 2f;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void init() {
        setFocusable(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
        cBgMain = color(R.color.cst_bg_main);
        cPanel = color(R.color.cst_panel_bg);
        cPanelStroke = color(R.color.cst_panel_stroke);
        cText = color(R.color.cst_text_primary);
        cMuted = color(R.color.cst_text_muted);
        cAccent = color(R.color.cst_accent);
        cAccent2 = color(R.color.cst_accent_2);
        cSuccess = color(R.color.cst_success);
        cWarning = color(R.color.cst_warning);
        cDanger = color(R.color.cst_danger);
        cGrass = color(R.color.cst_grass);
        cGrassDark = color(R.color.cst_grass_dark);
        cPath = color(R.color.cst_path);
        cStraw = color(R.color.cst_straw);
        cBarn = color(R.color.cst_barn);
        cWood = color(R.color.cst_wood);
        cWater = color(R.color.cst_water);
        pigBadge = loadGameArtBitmap("game_art/kenney_animal_pack/assets/PNG/Round (outline)/pig.png");
        rabbitBadge = loadGameArtBitmap("game_art/kenney_animal_pack/assets/PNG/Round (outline)/rabbit.png");
        resetPreviewState();
    }

    private Bitmap loadGameArtBitmap(String path) {
        try {
            InputStream stream = getContext().getAssets().open(path);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();
            return bitmap;
        } catch (IOException ex) {
            return null;
        }
    }

    private final Runnable loopStep = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(0.05f, (now - lastTick) / 1000f);
            lastTick = now;
            if (state == GameState.PLAYING) {
                update(dt);
            }
            invalidate();
            postOnAnimation(this);
        }
    };

    private int color(int resId) {
        return getResources().getColor(resId, null);
    }

    private void update(float dt) {
        dayTime -= dt;
        stockTimer += dt;
        if (toastTimer > 0f) {
            toastTimer -= dt;
        }
        if (stockTimer >= 18f) {
            stockTimer = 0f;
            int i = random.nextInt(recipeStock.length);
            recipeStock[i] = Math.min(5, recipeStock[i] + 1);
            toast = recipes[i].name + " restocked";
            toastTimer = 2f;
        }
        for (Animal animal : animals) {
            animal.walkPhase += dt * 4f;
            if (animal.fed && !animal.ready) {
                animal.timer -= dt;
                animal.mood = Math.min(100f, animal.mood + dt * 2f);
                if (animal.timer <= 0f) {
                    animal.ready = true;
                    animal.fed = false;
                    toast = animal.name + " product is ready";
                    toastTimer = 2f;
                }
            } else if (!animal.ready) {
                animal.mood = Math.max(30f, animal.mood - dt * 0.55f);
            }
        }
        if (reputation >= targetReputation) {
            if (listener != null) {
                listener.onDayClear();
            }
            nextDay();
        } else if (dayTime <= 0f) {
            state = GameState.GAME_OVER;
            toast = "Day ended before the reputation target";
            toastTimer = 4f;
            if (listener != null) {
                listener.onSound("day_fail");
            }
            if (listener != null) {
                listener.onGameOver();
            }
        }
    }

    private void nextDay() {
        day++;
        coins += 24 + day * 4;
        reputation = 0;
        targetReputation += 35;
        dayTime = Math.max(185f, 245f - day * 7f);
        for (int i = 0; i < recipeStock.length; i++) {
            recipeStock[i] = Math.min(5, recipeStock[i] + 1);
        }
        for (int i = 0; i < orders.length; i++) {
            orders[i] = makeOrder(i + day);
        }
        toast = "New day: better orders unlocked";
        toastTimer = 3f;
    }

    private void feedAnimal(int animalIndex, int recipeIndex) {
        Animal animal = animals[animalIndex];
        if (animal.ready) {
            toast = "Collect before feeding again";
            toastTimer = 2f;
            if (listener != null) {
                listener.onSound("ui_click");
            }
            return;
        }
        if (animal.fed) {
            toast = animal.name + " is still eating";
            toastTimer = 2f;
            if (listener != null) {
                listener.onSound("ui_click");
            }
            return;
        }
        if (recipeStock[recipeIndex] <= 0) {
            toast = "Recipe is out of stock";
            toastTimer = 2f;
            if (listener != null) {
                listener.onSound("ui_click");
            }
            return;
        }
        recipeStock[recipeIndex]--;
        animal.fed = true;
        animal.ready = false;
        animal.timer = recipes[recipeIndex].seconds + random.nextFloat() * 3f;
        boolean favorite = animal.favoriteRecipe == recipeIndex;
        animal.quality = favorite ? QUALITY_PREMIUM : QUALITY_FRESH;
        if (favorite && animal.mood > 84f) {
            animal.quality = QUALITY_PERFECT;
        }
        if (!favorite && animal.mood < 48f) {
            animal.quality = QUALITY_STANDARD;
        }
        animal.mood = Math.min(100f, animal.mood + (favorite ? 14f : 5f));
        toast = favorite ? animal.name + " loves that snack" : animal.name + " accepts the snack";
        toastTimer = 2f;
        if (listener != null) {
            listener.onSound("feed_drop");
            listener.onSound(favorite ? "animal_happy" : "feed_mix");
        }
    }

    private void collectAnimal(int animalIndex) {
        Animal animal = animals[animalIndex];
        int quality = animal.quality;
        inventory[animalIndex][quality]++;
        animal.ready = false;
        animal.timer = 0f;
        animal.mood = Math.min(100f, animal.mood + 4f);
        toast = qualityName(quality) + " " + animal.product + " collected";
        toastTimer = 2f;
        if (listener != null) {
            listener.onSound("product_collect");
        }
        invalidate();
    }

    private void deliverOrder(int orderIndex) {
        Order order = orders[orderIndex];
        int animalIndex = productIndex(order.product);
        int availableQuality = -1;
        for (int q = QUALITY_PERFECT; q >= order.minQuality; q--) {
            if (inventory[animalIndex][q] >= order.amount) {
                availableQuality = q;
                break;
            }
        }
        if (availableQuality < 0) {
            toast = "Need " + order.amount + " " + qualityName(order.minQuality) + " " + order.product;
            toastTimer = 3f;
            if (listener != null) {
                listener.onSound("ui_click");
            }
            invalidate();
            return;
        }
        inventory[animalIndex][availableQuality] -= order.amount;
        coins += order.reward + availableQuality * 4;
        reputation += order.reputation + availableQuality * 3;
        orders[orderIndex] = makeOrder(orderIndex + day + random.nextInt(20));
        toast = "Order stamped";
        toastTimer = 2f;
        if (listener != null) {
            listener.onSound("order_stamp");
        }
        invalidate();
    }

    private Order makeOrder(int seed) {
        int animalIndex = Math.abs(seed + random.nextInt(animals.length)) % animals.length;
        int minQuality = Math.min(QUALITY_PREMIUM, seed % 3);
        int amount = 1 + (seed % 2);
        int reward = 18 + amount * 8 + minQuality * 10;
        int rep = 16 + amount * 6 + minQuality * 7;
        return new Order(animals[animalIndex].product, minQuality, amount, reward, rep);
    }

    private void resetPreviewState() {
        coins = 40;
        reputation = 0;
        targetReputation = 80;
        day = 1;
        dayTime = 240f;
        for (int i = 0; i < recipeStock.length; i++) {
            recipeStock[i] = 3;
        }
        for (int i = 0; i < orders.length; i++) {
            orders[i] = makeOrder(i);
        }
    }

    private int productIndex(String product) {
        for (int i = 0; i < animals.length; i++) {
            if (animals[i].product.equals(product)) {
                return i;
            }
        }
        return 0;
    }

    private String qualityName(int quality) {
        if (quality == QUALITY_PERFECT) {
            return "perfect";
        }
        if (quality == QUALITY_PREMIUM) {
            return "premium";
        }
        if (quality == QUALITY_FRESH) {
            return "fresh";
        }
        return "standard";
    }

    private void computeLayout() {
        float w = getWidth();
        float h = getHeight();
        float leftW = w * 0.18f;
        float rightW = w * 0.22f;
        float topH = h * 0.12f;
        float bottomH = h * 0.1f;
        leftPanel.set(0f, 0f, leftW, h);
        rightPanel.set(w - rightW, 0f, w, h);
        topHud.set(leftW, 0f, w - rightW, topH);
        coolerRect.set(w - rightW - w * 0.22f, h - bottomH * 0.9f, w - rightW - w * 0.02f, h - h * 0.02f);
        playRect.set(leftW, topH, w - rightW, h - bottomH);
        float penW = playRect.width() * 0.28f;
        float penH = playRect.height() * 0.28f;
        for (int i = 0; i < animals.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float gapX = playRect.width() * 0.04f;
            float gapY = playRect.height() * 0.08f;
            float x = playRect.left + gapX + col * (penW + gapX);
            float y = playRect.top + gapY + row * (penH + gapY);
            animals[i].bounds.set(x, y, x + penW, y + penH);
        }
    }

    private void drawBackground(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), Color.rgb(225, 248, 237), cBgMain, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
        paint.setColor(Color.argb(95, 255, 255, 255));
        canvas.drawOval(playRect.left - 120f, playRect.top - 90f, playRect.left + 360f, playRect.top + 80f, paint);
        canvas.drawOval(playRect.right - 300f, playRect.top - 70f, playRect.right + 170f, playRect.top + 70f, paint);
        paint.setColor(cGrass);
        canvas.drawRect(playRect, paint);
        paint.setColor(Color.argb(40, 255, 255, 255));
        for (int i = 0; i < 9; i++) {
            float bandY = playRect.top + i * playRect.height() / 9f;
            canvas.drawRect(playRect.left, bandY, playRect.right, bandY + 5f, paint);
        }
        paint.setColor(cGrassDark);
        for (int i = 0; i < 36; i++) {
            float x = playRect.left + (i * 71f) % Math.max(1f, playRect.width());
            float y = playRect.top + (i * 43f) % Math.max(1f, playRect.height());
            canvas.drawOval(x, y, x + 22f, y + 8f, paint);
            if (i % 4 == 0) {
                paint.setColor(cAccent);
                canvas.drawCircle(x + 26f, y + 3f, 3f, paint);
                paint.setColor(cGrassDark);
            }
        }
        paint.setColor(cPath);
        RectF path = new RectF(playRect.left + playRect.width() * 0.37f, playRect.top, playRect.left + playRect.width() * 0.48f, playRect.bottom);
        canvas.drawRoundRect(path, 20f, 20f, paint);
        paint.setColor(Color.argb(70, 124, 88, 45));
        for (int i = 0; i < 8; i++) {
            float y = path.top + i * path.height() / 8f;
            canvas.drawRoundRect(path.left + 10f, y + 8f, path.right - 10f, y + 14f, 5f, 5f, paint);
        }
    }

    private void drawRanch(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        RectF barn = new RectF(playRect.left + playRect.width() * 0.05f, playRect.top + playRect.height() * 0.02f, playRect.left + playRect.width() * 0.24f, playRect.top + playRect.height() * 0.18f);
        paint.setColor(cBarn);
        canvas.drawRoundRect(barn, 14f, 14f, paint);
        Path roof = new Path();
        roof.moveTo(barn.left - 12f, barn.top + 10f);
        roof.lineTo(barn.centerX(), barn.top - 42f);
        roof.lineTo(barn.right + 12f, barn.top + 10f);
        roof.close();
        paint.setColor(cWood);
        canvas.drawPath(roof, paint);
        paint.setColor(Color.rgb(255, 248, 220));
        canvas.drawRoundRect(barn.left + barn.width() * 0.35f, barn.top + barn.height() * 0.42f, barn.right - barn.width() * 0.35f, barn.bottom, 6f, 6f, paint);
        paint.setColor(cWood);
        canvas.drawCircle(barn.centerX(), barn.top + barn.height() * 0.68f, 4f, paint);
        drawText(canvas, "Snack Barn", barn.centerX(), barn.bottom + 20f, 15f, cText, Paint.Align.CENTER);
        drawIngredientBeds(canvas);
        drawFeedMixer(canvas);
        drawCooler(canvas);
    }

    private void drawFeedMixer(Canvas canvas) {
        float x = playRect.left + playRect.width() * 0.34f;
        float y = playRect.top + playRect.height() * 0.12f;
        RectF base = new RectF(x, y, x + playRect.width() * 0.12f, y + playRect.height() * 0.16f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cWood);
        canvas.drawRoundRect(base, 16f, 16f, paint);
        paint.setColor(Color.rgb(255, 253, 243));
        RectF bowl = new RectF(base.left + 14f, base.top + 12f, base.right - 14f, base.top + base.height() * 0.62f);
        canvas.drawOval(bowl, paint);
        paint.setColor(cAccent);
        canvas.drawOval(bowl.left + 10f, bowl.top + 9f, bowl.right - 10f, bowl.bottom - 4f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(cPanelStroke);
        canvas.drawLine(base.right - 18f, base.top + 12f, base.right + 16f, base.top - 18f, paint);
        canvas.drawCircle(base.right + 21f, base.top - 23f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);
        drawText(canvas, "Mixer", base.centerX(), base.bottom + 18f, 14f, cText, Paint.Align.CENTER);
    }

    private void drawIngredientBeds(Canvas canvas) {
        float x = playRect.right - playRect.width() * 0.27f;
        float y = playRect.top + playRect.height() * 0.05f;
        float sw = playRect.width() * 0.2f;
        float sh = playRect.height() * 0.1f;
        for (int i = 0; i < 3; i++) {
            RectF bed = new RectF(x, y + i * sh * 1.25f, x + sw, y + i * sh * 1.25f + sh);
            paint.setColor(cWood);
            canvas.drawRoundRect(bed, 16f, 16f, paint);
            paint.setColor(i == 0 ? cAccent : i == 1 ? cSuccess : cAccent2);
            for (int j = 0; j < 5; j++) {
                float cx = bed.left + 18f + j * (bed.width() - 36f) / 4f;
                canvas.drawOval(cx - 8f, bed.centerY() - 8f, cx + 8f, bed.centerY() + 8f, paint);
            }
        }
    }

    private void drawCooler(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 252, 255));
        canvas.drawRoundRect(coolerRect, 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(cAccent2);
        canvas.drawRoundRect(coolerRect, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        float bottleX = coolerRect.left + 18f;
        float bottleY = coolerRect.top + 10f;
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(bottleX + 9f, bottleY, bottleX + 25f, bottleY + 12f, 5f, 5f, paint);
        canvas.drawRoundRect(bottleX, bottleY + 9f, bottleX + 34f, bottleY + 46f, 10f, 10f, paint);
        paint.setColor(cAccent2);
        canvas.drawRect(bottleX + 4f, bottleY + 28f, bottleX + 30f, bottleY + 42f, paint);
        drawText(canvas, "Fresh Cooler", coolerRect.left + 62f, coolerRect.top + 25f, 15f, cText, Paint.Align.LEFT);
        drawText(canvas, "Stored " + totalInventory(), coolerRect.left + 62f, coolerRect.top + 48f, 18f, cText, Paint.Align.LEFT);
    }

    private void drawAnimals(Canvas canvas) {
        for (int i = 0; i < animals.length; i++) {
            Animal animal = animals[i];
            drawPen(canvas, animal.bounds, animal);
            RectF r = animal.bounds;
            float cx = r.centerX();
            float cy = r.centerY() + (float) Math.sin(animal.walkPhase) * 3f;
            float scale = Math.min(r.width(), r.height()) / 150f;
            if (i == 0) {
                drawCow(canvas, cx, cy, scale, animal);
            } else if (i == 1) {
                drawHen(canvas, cx, cy, scale, animal);
            } else if (i == 2) {
                drawSheep(canvas, cx, cy, scale, animal);
            } else if (i == 3) {
                drawGoat(canvas, cx, cy, scale, animal);
            } else if (i == 4) {
                drawPig(canvas, cx, cy, scale, animal);
            } else {
                drawRabbit(canvas, cx, cy, scale, animal);
            }
            drawAnimalLabel(canvas, animal);
        }
    }

    private void drawPen(Canvas canvas, RectF r, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(242, 210, 121));
        canvas.drawRoundRect(r, 22f, 22f, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(r.left + 8f, r.top + 8f, r.right - 8f, r.bottom - 8f, 18f, 18f, paint);
        paint.setColor(cStraw);
        for (int i = 0; i < 7; i++) {
            float sy = r.top + 18f + i * (r.height() - 36f) / 6f;
            canvas.drawRoundRect(r.left + 16f, sy, r.right - 16f, sy + 3f, 4f, 4f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(cWood);
        canvas.drawRoundRect(r, 22f, 22f, paint);
        paint.setStrokeWidth(4f);
        for (int i = 1; i < 4; i++) {
            float fx = r.left + i * r.width() / 4f;
            canvas.drawLine(fx, r.top + 2f, fx, r.bottom - 2f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cWater);
        canvas.drawRoundRect(r.left + 14f, r.top + 12f, r.left + 58f, r.top + 30f, 8f, 8f, paint);
        if (animal.ready) {
            paint.setColor(cAccent);
            canvas.drawCircle(r.right - 24f, r.top + 24f, 14f, paint);
        } else if (animal.fed) {
            paint.setColor(cSuccess);
            canvas.drawCircle(r.right - 24f, r.top + 24f, 12f, paint);
        }
    }

    private void drawCow(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        RectF body = new RectF(cx - 52f * s, cy - 18f * s, cx + 45f * s, cy + 32f * s);
        canvas.drawOval(body, paint);
        Path tail = new Path();
        tail.moveTo(cx - 50f * s, cy - 5f * s);
        tail.lineTo(cx - 70f * s, cy - 22f * s);
        tail.lineTo(cx - 61f * s, cy - 2f * s);
        tail.close();
        canvas.drawPath(tail, paint);
        paint.setColor(Color.rgb(36, 43, 35));
        canvas.drawOval(cx - 38f * s, cy - 13f * s, cx - 13f * s, cy + 8f * s, paint);
        canvas.drawOval(cx + 8f * s, cy - 18f * s, cx + 35f * s, cy + 5f * s, paint);
        paint.setColor(Color.WHITE);
        RectF head = new RectF(cx + 32f * s, cy - 28f * s, cx + 70f * s, cy + 16f * s);
        canvas.drawOval(head, paint);
        paint.setColor(Color.rgb(245, 236, 220));
        canvas.drawOval(cx + 24f * s, cy - 27f * s, cx + 42f * s, cy - 7f * s, paint);
        canvas.drawOval(cx + 61f * s, cy - 27f * s, cx + 79f * s, cy - 7f * s, paint);
        paint.setColor(Color.rgb(245, 184, 165));
        canvas.drawOval(cx + 48f * s, cy - 5f * s, cx + 74f * s, cy + 18f * s, paint);
        paint.setColor(Color.rgb(56, 38, 30));
        canvas.drawCircle(cx + 56f * s, cy - 15f * s, 3f * s, paint);
        canvas.drawCircle(cx + 62f * s, cy + 5f * s, 2f * s, paint);
        paint.setColor(Color.rgb(224, 212, 170));
        drawHorn(canvas, cx + 47f * s, cy - 27f * s, -1f, s);
        drawHorn(canvas, cx + 62f * s, cy - 25f * s, 1f, s);
        paint.setColor(Color.rgb(70, 50, 35));
        for (int i = 0; i < 4; i++) {
            float lx = cx - 35f * s + i * 24f * s;
            canvas.drawRoundRect(lx, cy + 24f * s, lx + 7f * s, cy + 52f * s, 4f * s, 4f * s, paint);
            canvas.drawOval(lx - 1f * s, cy + 48f * s, lx + 10f * s, cy + 57f * s, paint);
        }
        paint.setColor(Color.rgb(242, 180, 170));
        canvas.drawOval(cx - 8f * s, cy + 18f * s, cx + 18f * s, cy + 38f * s, paint);
        drawMood(canvas, animal, cx, cy - 48f * s);
    }

    private void drawHorn(Canvas canvas, float x, float y, float dir, float s) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + dir * 12f * s, y - 12f * s);
        path.lineTo(x + dir * 4f * s, y + 2f * s);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawHen(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 239, 218));
        canvas.drawOval(cx - 34f * s, cy - 22f * s, cx + 26f * s, cy + 34f * s, paint);
        paint.setColor(Color.rgb(150, 92, 55));
        Path tail = new Path();
        tail.moveTo(cx - 28f * s, cy - 10f * s);
        tail.lineTo(cx - 58f * s, cy - 32f * s);
        tail.lineTo(cx - 49f * s, cy + 0f * s);
        tail.lineTo(cx - 63f * s, cy + 14f * s);
        tail.close();
        canvas.drawPath(tail, paint);
        paint.setColor(Color.rgb(235, 70, 48));
        canvas.drawOval(cx + 10f * s, cy - 43f * s, cx + 33f * s, cy - 20f * s, paint);
        paint.setColor(Color.rgb(245, 239, 218));
        canvas.drawOval(cx + 4f * s, cy - 36f * s, cx + 42f * s, cy + 4f * s, paint);
        paint.setColor(Color.rgb(236, 168, 39));
        Path beak = new Path();
        beak.moveTo(cx + 38f * s, cy - 17f * s);
        beak.lineTo(cx + 58f * s, cy - 10f * s);
        beak.lineTo(cx + 38f * s, cy - 4f * s);
        beak.close();
        canvas.drawPath(beak, paint);
        paint.setColor(Color.rgb(80, 55, 36));
        canvas.drawCircle(cx + 30f * s, cy - 20f * s, 3f * s, paint);
        paint.setColor(Color.rgb(232, 194, 90));
        canvas.drawOval(cx - 24f * s, cy - 5f * s, cx + 10f * s, cy + 24f * s, paint);
        paint.setStrokeWidth(4f * s);
        canvas.drawLine(cx - 5f * s, cy + 32f * s, cx - 10f * s, cy + 48f * s, paint);
        canvas.drawLine(cx + 12f * s, cy + 32f * s, cx + 15f * s, cy + 48f * s, paint);
        canvas.drawLine(cx - 10f * s, cy + 48f * s, cx - 20f * s, cy + 52f * s, paint);
        canvas.drawLine(cx + 15f * s, cy + 48f * s, cx + 25f * s, cy + 52f * s, paint);
        drawMood(canvas, animal, cx, cy - 52f * s);
    }

    private void drawSheep(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 245, 235));
        for (int i = 0; i < 7; i++) {
            float angle = (float) (Math.PI * 2 * i / 7);
            canvas.drawCircle(cx + (float) Math.cos(angle) * 27f * s, cy + (float) Math.sin(angle) * 16f * s, 23f * s, paint);
        }
        canvas.drawOval(cx - 42f * s, cy - 24f * s, cx + 46f * s, cy + 28f * s, paint);
        paint.setColor(Color.rgb(70, 60, 50));
        canvas.drawOval(cx + 30f * s, cy - 20f * s, cx + 62f * s, cy + 17f * s, paint);
        paint.setColor(Color.rgb(55, 48, 42));
        canvas.drawOval(cx + 24f * s, cy - 20f * s, cx + 38f * s, cy - 4f * s, paint);
        canvas.drawOval(cx + 54f * s, cy - 19f * s, cx + 68f * s, cy - 3f * s, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx + 52f * s, cy - 8f * s, 3f * s, paint);
        paint.setColor(Color.rgb(80, 60, 42));
        for (int i = 0; i < 4; i++) {
            float lx = cx - 30f * s + i * 18f * s;
            canvas.drawRoundRect(lx, cy + 24f * s, lx + 6f * s, cy + 46f * s, 3f * s, 3f * s, paint);
        }
        drawMood(canvas, animal, cx, cy - 50f * s);
    }

    private void drawGoat(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(218, 210, 186));
        canvas.drawOval(cx - 46f * s, cy - 18f * s, cx + 38f * s, cy + 28f * s, paint);
        canvas.drawOval(cx + 24f * s, cy - 38f * s, cx + 58f * s, cy + 5f * s, paint);
        paint.setColor(Color.rgb(188, 176, 148));
        canvas.drawOval(cx + 18f * s, cy - 33f * s, cx + 34f * s, cy - 12f * s, paint);
        canvas.drawOval(cx + 52f * s, cy - 32f * s, cx + 69f * s, cy - 11f * s, paint);
        paint.setColor(Color.rgb(90, 76, 55));
        drawHorn(canvas, cx + 35f * s, cy - 36f * s, -1f, s);
        drawHorn(canvas, cx + 51f * s, cy - 35f * s, 1f, s);
        paint.setColor(Color.rgb(90, 76, 55));
        Path beard = new Path();
        beard.moveTo(cx + 51f * s, cy + 2f * s);
        beard.lineTo(cx + 58f * s, cy + 23f * s);
        beard.lineTo(cx + 42f * s, cy + 11f * s);
        beard.close();
        canvas.drawPath(beard, paint);
        paint.setColor(Color.rgb(60, 50, 40));
        canvas.drawCircle(cx + 45f * s, cy - 20f * s, 3f * s, paint);
        for (int i = 0; i < 4; i++) {
            float lx = cx - 28f * s + i * 19f * s;
            canvas.drawRoundRect(lx, cy + 22f * s, lx + 7f * s, cy + 49f * s, 3f * s, 3f * s, paint);
        }
        drawMood(canvas, animal, cx, cy - 54f * s);
    }

    private void drawPig(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(242, 157, 171));
        canvas.drawOval(cx - 48f * s, cy - 20f * s, cx + 42f * s, cy + 28f * s, paint);
        canvas.drawOval(cx + 28f * s, cy - 25f * s, cx + 67f * s, cy + 13f * s, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f * s);
        paint.setColor(Color.rgb(180, 90, 110));
        RectF tailCurl = new RectF(cx - 62f * s, cy - 15f * s, cx - 42f * s, cy + 5f * s);
        canvas.drawArc(tailCurl, 80f, 290f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(242, 157, 171));
        Path ear = new Path();
        ear.moveTo(cx + 38f * s, cy - 22f * s);
        ear.lineTo(cx + 30f * s, cy - 44f * s);
        ear.lineTo(cx + 51f * s, cy - 28f * s);
        ear.close();
        canvas.drawPath(ear, paint);
        paint.setColor(Color.rgb(235, 120, 145));
        canvas.drawOval(cx + 52f * s, cy - 8f * s, cx + 76f * s, cy + 10f * s, paint);
        paint.setColor(Color.rgb(80, 40, 50));
        canvas.drawCircle(cx + 48f * s, cy - 15f * s, 3f * s, paint);
        canvas.drawCircle(cx + 60f * s, cy + 0f * s, 2f * s, paint);
        canvas.drawCircle(cx + 68f * s, cy + 1f * s, 2f * s, paint);
        paint.setColor(Color.rgb(120, 70, 65));
        for (int i = 0; i < 4; i++) {
            float lx = cx - 32f * s + i * 22f * s;
            canvas.drawRoundRect(lx, cy + 22f * s, lx + 7f * s, cy + 43f * s, 4f * s, 4f * s, paint);
        }
        if (pigBadge != null) {
            RectF badge = new RectF(cx - 66f * s, cy - 42f * s, cx - 40f * s, cy - 16f * s);
            canvas.drawBitmap(pigBadge, null, badge, null);
        }
        drawMood(canvas, animal, cx, cy - 48f * s);
    }

    private void drawRabbit(Canvas canvas, float cx, float cy, float s, Animal animal) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(225, 221, 205));
        canvas.drawOval(cx - 34f * s, cy - 18f * s, cx + 34f * s, cy + 32f * s, paint);
        canvas.drawOval(cx + 20f * s, cy - 34f * s, cx + 54f * s, cy + 5f * s, paint);
        canvas.drawOval(cx + 28f * s, cy - 75f * s, cx + 40f * s, cy - 28f * s, paint);
        canvas.drawOval(cx + 43f * s, cy - 72f * s, cx + 55f * s, cy - 25f * s, paint);
        paint.setColor(Color.rgb(240, 175, 185));
        canvas.drawOval(cx + 31f * s, cy - 66f * s, cx + 37f * s, cy - 32f * s, paint);
        canvas.drawOval(cx + 46f * s, cy - 63f * s, cx + 52f * s, cy - 32f * s, paint);
        paint.setColor(Color.rgb(70, 50, 40));
        canvas.drawCircle(cx + 43f * s, cy - 18f * s, 3f * s, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * s);
        canvas.drawLine(cx + 49f * s, cy - 10f * s, cx + 69f * s, cy - 16f * s, paint);
        canvas.drawLine(cx + 49f * s, cy - 7f * s, cx + 70f * s, cy - 6f * s, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - 32f * s, cy + 4f * s, 12f * s, paint);
        paint.setColor(Color.rgb(125, 95, 70));
        canvas.drawOval(cx - 18f * s, cy + 25f * s, cx - 4f * s, cy + 42f * s, paint);
        canvas.drawOval(cx + 12f * s, cy + 25f * s, cx + 28f * s, cy + 42f * s, paint);
        if (rabbitBadge != null) {
            RectF badge = new RectF(cx - 58f * s, cy - 48f * s, cx - 32f * s, cy - 22f * s);
            canvas.drawBitmap(rabbitBadge, null, badge, null);
        }
        drawMood(canvas, animal, cx, cy - 82f * s);
    }

    private void drawMood(Canvas canvas, Animal animal, float cx, float cy) {
        if (animal.ready) {
            paint.setColor(cAccent);
            drawStar(canvas, cx, cy, 12f);
        } else if (animal.fed) {
            paint.setColor(cSuccess);
            canvas.drawCircle(cx, cy, 8f, paint);
        } else if (animal.mood < 45f) {
            paint.setColor(cDanger);
            canvas.drawCircle(cx, cy, 7f, paint);
        }
    }

    private void drawAnimalLabel(Canvas canvas, Animal animal) {
        RectF r = animal.bounds;
        drawText(canvas, animal.name, r.left + 12f, r.bottom - 24f, 18f, cText, Paint.Align.LEFT);
        String line = animal.ready ? "Ready " + qualityName(animal.quality) : animal.fed ? String.format(Locale.US, "%.0fs", animal.timer) : "Mood " + (int) animal.mood;
        drawText(canvas, line, r.left + 12f, r.bottom - 6f, 14f, cMuted, Paint.Align.LEFT);
    }

    private void drawPanels(Canvas canvas) {
        drawLeftPanel(canvas);
        drawRightPanel(canvas);
    }

    private void drawLeftPanel(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(250, 242, 214));
        canvas.drawRect(leftPanel, paint);
        paint.setColor(cWood);
        canvas.drawRect(leftPanel.right - 10f, leftPanel.top, leftPanel.right, leftPanel.bottom, paint);
        paint.setColor(Color.rgb(224, 197, 137));
        canvas.drawRect(leftPanel.left + 8f, leftPanel.top, leftPanel.left + 22f, leftPanel.bottom, paint);
        paint.setColor(cBarn);
        canvas.drawRoundRect(leftPanel.left + 10f, leftPanel.top + 12f, leftPanel.right - 10f, leftPanel.top + 58f, 16f, 16f, paint);
        drawText(canvas, "Recipe Book", leftPanel.left + 28f, leftPanel.top + 42f, 20f, Color.WHITE, Paint.Align.LEFT);
        paint.setColor(cAccent);
        canvas.drawCircle(leftPanel.right - 31f, leftPanel.top + 35f, 8f, paint);
        float cardW = leftPanel.width() - 22f;
        float cardH = Math.max(58f, getHeight() * 0.105f);
        for (int i = 0; i < recipes.length; i++) {
            float x = leftPanel.left + 11f;
            float y = leftPanel.top + 76f + i * (cardH + 10f);
            recipeRects[i] = new RectF(x, y, x + cardW, y + cardH);
            drawRecipeCard(canvas, recipes[i], i, x, y, cardW, cardH, draggingRecipe == i);
        }
    }

    private void drawRecipeCard(Canvas canvas, Recipe recipe, int index, float x, float y, float w, float h, boolean raised) {
        RectF card = new RectF(x, y, x + w, y + h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(raised ? Color.rgb(255, 230, 130) : Color.rgb(255, 252, 238));
        canvas.drawRoundRect(card, 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(cWood);
        canvas.drawRoundRect(card, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(index % 2 == 0 ? cSuccess : cAccent2);
        canvas.drawRoundRect(x + 10f, y + h * 0.25f, x + 39f, y + h * 0.75f, 10f, 10f, paint);
        paint.setColor(cAccent);
        canvas.drawCircle(x + 24f, y + h * 0.5f, 7f, paint);
        drawText(canvas, recipe.name, x + 46f, y + h * 0.42f, 16f, cText, Paint.Align.LEFT);
        drawText(canvas, "Stock " + recipeStock[index] + "  " + recipe.productHint, x + 46f, y + h * 0.72f, 13f, cMuted, Paint.Align.LEFT);
    }

    private void drawRightPanel(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(249, 238, 202));
        canvas.drawRect(rightPanel, paint);
        paint.setColor(cWood);
        canvas.drawRoundRect(rightPanel.left + 24f, rightPanel.top + 6f, rightPanel.right - 24f, rightPanel.bottom - 10f, 18f, 18f, paint);
        paint.setColor(Color.rgb(255, 253, 243));
        canvas.drawRoundRect(rightPanel.left + 34f, rightPanel.top + 18f, rightPanel.right - 34f, rightPanel.bottom - 22f, 14f, 14f, paint);
        paint.setColor(cWood);
        canvas.drawRoundRect(rightPanel.left + rightPanel.width() * 0.35f, rightPanel.top + 8f, rightPanel.right - rightPanel.width() * 0.35f, rightPanel.top + 42f, 12f, 12f, paint);
        paint.setColor(Color.rgb(210, 210, 205));
        canvas.drawRoundRect(rightPanel.left + rightPanel.width() * 0.39f, rightPanel.top + 2f, rightPanel.right - rightPanel.width() * 0.39f, rightPanel.top + 27f, 8f, 8f, paint);
        drawText(canvas, "Delivery Board", rightPanel.centerX(), rightPanel.top + 70f, 20f, cText, Paint.Align.CENTER);
        float cardW = rightPanel.width() - 24f;
        float cardH = Math.max(76f, getHeight() * 0.145f);
        for (int i = 0; i < orders.length; i++) {
            float x = rightPanel.left + 12f;
            float y = rightPanel.top + 86f + i * (cardH + 16f);
            orderRects[i] = new RectF(x, y, x + cardW, y + cardH);
            if (orders[i] != null) {
                drawOrderCard(canvas, orders[i], x, y, cardW, cardH);
            }
        }
        drawInventorySummary(canvas);
    }

    private void drawOrderCard(Canvas canvas, Order order, float x, float y, float w, float h) {
        RectF card = new RectF(x, y, x + w, y + h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 254, 246));
        canvas.drawRoundRect(card, 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(cPanelStroke);
        canvas.drawRoundRect(card, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(45, 198, 66, 50));
        canvas.drawCircle(x + w - 34f, y + h - 26f, 24f, paint);
        drawText(canvas, "STAMP", x + w - 34f, y + h - 21f, 10f, cBarn, Paint.Align.CENTER);
        drawText(canvas, order.amount + " " + qualityName(order.minQuality), x + 16f, y + 28f, 16f, cText, Paint.Align.LEFT);
        drawText(canvas, order.product, x + 16f, y + 52f, 20f, cBarn, Paint.Align.LEFT);
        drawText(canvas, "+" + order.reward + "c +" + order.reputation + "r", x + 16f, y + h - 16f, 14f, cMuted, Paint.Align.LEFT);
        paint.setColor(cAccent);
        drawStar(canvas, x + w - 26f, y + 26f, 11f);
    }

    private void drawInventorySummary(Canvas canvas) {
        float y = rightPanel.bottom - 120f;
        drawText(canvas, "Inventory", rightPanel.left + 20f, y, 18f, cText, Paint.Align.LEFT);
        int line = 0;
        for (int i = 0; i < animals.length; i++) {
            int count = 0;
            for (int q = 0; q < 4; q++) {
                count += inventory[i][q];
            }
            if (count > 0 && line < 4) {
                drawText(canvas, animals[i].product + " x" + count, rightPanel.left + 20f, y + 24f + line * 20f, 14f, cMuted, Paint.Align.LEFT);
                line++;
            }
        }
    }

    private void drawHud(Canvas canvas) {
        float badgeH = Math.max(38f, topHud.height() * 0.58f);
        float y = topHud.top + 12f;
        drawHudBadge(canvas, topHud.left + 18f, y, 106f, badgeH, "Day", String.valueOf(day), cBarn);
        drawHudBadge(canvas, topHud.left + 136f, y, 128f, badgeH, "Coins", String.valueOf(coins), cAccent);
        RectF ribbon = new RectF(topHud.centerX() - topHud.width() * 0.18f, y, topHud.centerX() + topHud.width() * 0.18f, y + badgeH);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cSuccess);
        canvas.drawRoundRect(ribbon, 22f, 22f, paint);
        Path leftTail = new Path();
        leftTail.moveTo(ribbon.left, ribbon.centerY());
        leftTail.lineTo(ribbon.left - 24f, ribbon.top + 8f);
        leftTail.lineTo(ribbon.left - 24f, ribbon.bottom - 8f);
        leftTail.close();
        canvas.drawPath(leftTail, paint);
        Path rightTail = new Path();
        rightTail.moveTo(ribbon.right, ribbon.centerY());
        rightTail.lineTo(ribbon.right + 24f, ribbon.top + 8f);
        rightTail.lineTo(ribbon.right + 24f, ribbon.bottom - 8f);
        rightTail.close();
        canvas.drawPath(rightTail, paint);
        drawText(canvas, "Reputation " + reputation + "/" + targetReputation, ribbon.centerX(), ribbon.centerY() + 7f, 18f, Color.WHITE, Paint.Align.CENTER);
        drawHudBadge(canvas, topHud.right - 148f, y, 124f, badgeH, "Time", String.valueOf(Math.max(0, (int) dayTime)), cAccent2);
    }

    private void drawHudBadge(Canvas canvas, float x, float y, float w, float h, String label, String value, int accent) {
        RectF r = new RectF(x, y, x + w, y + h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 253, 243));
        canvas.drawRoundRect(r, 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(accent);
        canvas.drawRoundRect(r, 20f, 20f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawCircle(r.left + 18f, r.centerY(), 9f, paint);
        drawText(canvas, label, r.left + 34f, r.top + 16f, 11f, cMuted, Paint.Align.LEFT);
        drawText(canvas, value, r.left + 34f, r.bottom - 8f, 18f, cText, Paint.Align.LEFT);
    }

    private void drawToast(Canvas canvas) {
        if (toastTimer <= 0f || toast == null) {
            return;
        }
        float w = Math.min(getWidth() * 0.46f, toast.length() * 11f + 44f);
        float h = 42f;
        RectF r = new RectF(playRect.centerX() - w * 0.5f, getHeight() - h - 18f, playRect.centerX() + w * 0.5f, getHeight() - 18f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225, 32, 53, 34));
        canvas.drawRoundRect(r, 18f, 18f, paint);
        drawText(canvas, toast, r.centerX(), r.centerY() + 7f, 17f, Color.WHITE, Paint.Align.CENTER);
    }

    private void drawEndBanner(Canvas canvas) {
        RectF r = new RectF(playRect.left + playRect.width() * 0.22f, playRect.top + playRect.height() * 0.25f, playRect.right - playRect.width() * 0.22f, playRect.bottom - playRect.height() * 0.25f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(235, 255, 253, 243));
        canvas.drawRoundRect(r, 28f, 28f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(cWood);
        canvas.drawRoundRect(r, 28f, 28f, paint);
        drawText(canvas, "Day Result", r.centerX(), r.top + 55f, 28f, cText, Paint.Align.CENTER);
        drawText(canvas, "Restart to improve your snack plan", r.centerX(), r.top + 92f, 18f, cMuted, Paint.Align.CENTER);
    }

    private int totalInventory() {
        int total = 0;
        for (int i = 0; i < inventory.length; i++) {
            for (int q = 0; q < 4; q++) {
                total += inventory[i][q];
            }
        }
        return total;
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius) {
        Path path = new Path();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? radius : radius * 0.45f;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawText(Canvas canvas, String value, float x, float y, float size, int color, Paint.Align align) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        canvas.drawText(value, x, y, textPaint);
    }

    private static class Recipe {
        final String name;
        final String productHint;
        final int favoriteAnimal;
        final float seconds;

        Recipe(String name, String productHint, int favoriteAnimal, float seconds) {
            this.name = name;
            this.productHint = productHint;
            this.favoriteAnimal = favoriteAnimal;
            this.seconds = seconds;
        }
    }

    private static class Animal {
        final String name;
        final String product;
        final int favoriteRecipe;
        final RectF bounds = new RectF();
        float timer;
        float mood;
        float walkPhase;
        int quality;
        boolean ready;
        boolean fed;

        Animal(String name, String product, int favoriteRecipe) {
            this.name = name;
            this.product = product;
            this.favoriteRecipe = favoriteRecipe;
            this.mood = 72f;
        }
    }

    private static class Order {
        final String product;
        final int minQuality;
        final int amount;
        final int reward;
        final int reputation;

        Order(String product, int minQuality, int amount, int reward, int reputation) {
            this.product = product;
            this.minQuality = minQuality;
            this.amount = amount;
            this.reward = reward;
            this.reputation = reputation;
        }
    }
}
