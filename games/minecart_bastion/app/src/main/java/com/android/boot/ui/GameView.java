package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.boot.R;
import com.android.boot.core.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    public static final int BUILD_GATLING = 0;
    public static final int BUILD_FROST = 1;
    public static final int BUILD_TESLA = 2;
    public static final int BUILD_DRILL = 3;

    public interface GameListener {
        void onHudChanged(int reactor, int ore, int wave, int maxWave, int leaks, String route, String selected);
        void onRunEnded(boolean cleared, int wave, int leaks, int stars);
        void onAudioEvent(String key);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random(19);
    private final ArrayList<Cart> carts = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Shot> shots = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final HashMap<String, Bitmap> sprites = new HashMap<>();
    private final float[] laneY = new float[]{0.27f, 0.50f, 0.73f};
    private GameListener listener;
    private GameState state = GameState.MENU;
    private boolean running;
    private boolean soundEnabled = true;
    private long lastTick;
    private int reactor = 100;
    private int ore = 160;
    private int wave;
    private int maxWave = 8;
    private int leaks;
    private int routeMode = 1;
    private int buildMode = BUILD_GATLING;
    private int spawnRemaining;
    private float spawnTimer;
    private boolean waveActive;
    private boolean spritesLoaded;
    private String selectedText = "Build Gatling";

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.nanoTime();
            float dt = (now - lastTick) / 1000000000f;
            lastTick = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            if (state == GameState.PLAYING) {
                update(dt);
            }
            invalidate();
            handler.postDelayed(this, 16);
        }
    };

    public GameView(Context context) {
        super(context);
        setup();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        setFocusable(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        loadSprites();
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
        notifyHud();
    }

    public void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastTick = System.nanoTime();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    public void stopLoop() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    public void pauseFromLifecycle() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
        stopLoop();
    }

    public void resetForMenu() {
        state = GameState.MENU;
        carts.clear();
        enemies.clear();
        shots.clear();
        effects.clear();
        waveActive = false;
        spawnRemaining = 0;
        selectedText = "Build Gatling";
        notifyHud();
        invalidate();
    }

    public void startGame() {
        reactor = 100;
        ore = 160;
        wave = 0;
        leaks = 0;
        routeMode = 1;
        buildMode = BUILD_GATLING;
        waveActive = false;
        spawnRemaining = 0;
        spawnTimer = 0f;
        selectedText = "Build Gatling";
        carts.clear();
        enemies.clear();
        shots.clear();
        effects.clear();
        state = GameState.PLAYING;
        startLoop();
        notifyHud();
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            notifyHud();
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastTick = System.nanoTime();
            startLoop();
            notifyHud();
        }
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void setBuildMode(int mode) {
        buildMode = mode;
        selectedText = "Build " + cartName(mode);
        notifyHud();
    }

    public void startNextWave() {
        if (state != GameState.PLAYING || waveActive || wave >= maxWave) {
            return;
        }
        wave++;
        spawnRemaining = 7 + wave * 3;
        spawnTimer = 0f;
        waveActive = true;
        ore += 10;
        selectedText = "Wave Running";
        notifyHud();
    }

    public void useRockfall() {
        if (state != GameState.PLAYING || ore < 35 || enemies.isEmpty()) {
            selectedText = "Need Rockfall Target";
            notifyHud();
            return;
        }
        ore -= 35;
        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).stun = Math.max(enemies.get(i).stun, 1.6f);
        }
        effects.add(new Effect(0.55f, 0.50f, 0.65f, 0.55f, color(R.color.cst_warning)));
        selectedText = "Rockfall";
        notifyHud();
    }

    public void useRepair() {
        if (state != GameState.PLAYING || ore < 45 || reactor >= 100) {
            selectedText = "Repair Blocked";
            notifyHud();
            return;
        }
        ore -= 45;
        reactor = Math.min(100, reactor + 25);
        effects.add(new Effect(0.12f, 0.50f, 0.5f, 0.45f, color(R.color.cst_success)));
        selectedText = "Repair";
        notifyHud();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (state != GameState.PLAYING) {
            return true;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return true;
        }
        float nx = event.getX() / w;
        float ny = event.getY() / h;
        if (touchesJunction(nx, ny)) {
            routeMode = (routeMode + 1) % laneY.length;
            selectedText = "Route " + routeName();
            effects.add(new Effect(0.32f, laneY[routeMode], 0.35f, 0.35f, color(R.color.cst_accent)));
            audioEvent("junction");
            notifyHud();
            return true;
        }
        Cart touched = findCart(nx, ny);
        if (touched != null) {
            upgradeCart(touched);
            return true;
        }
        int lane = nearestLane(ny);
        if (nx < 0.35f && Math.abs(ny - laneY[lane]) < 0.11f) {
            buildCart(lane);
            return true;
        }
        return true;
    }

    private void update(float dt) {
        updateWave(dt);
        updateCarts(dt);
        updateEnemies(dt);
        updateShots(dt);
        updateEffects(dt);
        if (waveActive && spawnRemaining <= 0 && enemies.isEmpty()) {
            waveActive = false;
            ore += 24 + wave * 4;
            if (wave >= maxWave) {
                finishRun(true);
            } else {
                selectedText = "Plan Next Wave";
                notifyHud();
            }
        }
    }

    private void updateWave(float dt) {
        if (!waveActive || spawnRemaining <= 0) {
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            spawnEnemy();
            spawnRemaining--;
            spawnTimer = Math.max(0.45f, 1.05f - wave * 0.05f);
        }
    }

    private void spawnEnemy() {
        int lane = random.nextInt(laneY.length);
        int type = 0;
        if ((wave == maxWave || wave % 3 == 0) && spawnRemaining == 1) {
            type = 3;
        } else if (wave >= 5 && spawnRemaining % 5 == 0) {
            type = 2;
        } else if (wave >= 3 && spawnRemaining % 3 == 0) {
            type = 1;
        }
        Enemy enemy = new Enemy();
        enemy.x = 0.98f;
        enemy.y = laneY[lane];
        enemy.lane = lane;
        enemy.type = type;
        enemy.maxHp = 36f + wave * 8f;
        enemy.speed = 0.055f + wave * 0.004f;
        enemy.damage = 8;
        enemy.reward = 8 + wave;
        enemy.radius = 0.028f;
        if (type == 1) {
            enemy.maxHp *= 0.85f;
            enemy.speed *= 1.55f;
            enemy.reward += 3;
        } else if (type == 2) {
            enemy.maxHp *= 1.75f;
            enemy.speed *= 0.72f;
            enemy.damage = 12;
            enemy.reward += 8;
            enemy.radius = 0.035f;
        } else if (type == 3) {
            enemy.maxHp *= 4.2f;
            enemy.speed *= 0.58f;
            enemy.damage = 25;
            enemy.reward += 35;
            enemy.radius = 0.052f;
        }
        enemy.hp = enemy.maxHp;
        enemies.add(enemy);
    }

    private void updateCarts(float dt) {
        for (int i = 0; i < carts.size(); i++) {
            Cart cart = carts.get(i);
            cart.x += cart.dir * cart.speed * dt;
            if (cart.x > 0.84f) {
                cart.x = 0.84f;
                cart.dir = -1f;
            }
            if (cart.x < 0.20f) {
                cart.x = 0.20f;
                cart.dir = 1f;
                cart.targetLane = routeMode;
            }
            float targetY = laneY[cart.targetLane];
            cart.y += (targetY - cart.y) * Math.min(1f, dt * 3.2f);
            cart.cooldown -= dt;
            if (cart.cooldown <= 0f) {
                Enemy target = findTarget(cart);
                if (target != null) {
                    fireCart(cart, target);
                    cart.cooldown = cartCooldown(cart);
                }
            }
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.stun > 0f) {
                enemy.stun -= dt;
            } else {
                float speed = enemy.speed;
                if (enemy.slow > 0f) {
                    enemy.slow -= dt;
                    speed *= 0.55f;
                }
                enemy.x -= speed * dt;
            }
            if (enemy.hp <= 0f) {
                ore += enemy.reward;
                effects.add(new Effect(enemy.x, enemy.y, 0.35f, enemy.radius * 2.8f, color(R.color.cst_success)));
                enemies.remove(i);
            } else if (enemy.x <= 0.12f) {
                reactor -= enemy.damage;
                leaks++;
                effects.add(new Effect(0.12f, 0.50f, 0.45f, 0.35f, color(R.color.cst_danger)));
                enemies.remove(i);
                if (reactor <= 0) {
                    reactor = 0;
                    finishRun(false);
                    return;
                }
            }
        }
    }

    private void updateShots(float dt) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            shot.life -= dt;
            if (shot.life <= 0f) {
                shots.remove(i);
            }
        }
    }

    private void updateEffects(float dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect effect = effects.get(i);
            effect.life -= dt;
            if (effect.life <= 0f) {
                effects.remove(i);
            }
        }
    }

    private void fireCart(Cart cart, Enemy target) {
        float damage = cartDamage(cart);
        target.hp -= damage;
        if (cart.type == BUILD_FROST) {
            target.slow = Math.max(target.slow, 1.3f + cart.level * 0.25f);
        } else if (cart.type == BUILD_TESLA) {
            int chained = 0;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy other = enemies.get(i);
                if (other != target && distance(target.x, target.y, other.x, other.y) < 0.18f && chained < 2) {
                    other.hp -= damage * 0.48f;
                    chained++;
                }
            }
        } else if (cart.type == BUILD_DRILL) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy other = enemies.get(i);
                if (other != target && distance(target.x, target.y, other.x, other.y) < 0.11f) {
                    other.hp -= damage * 0.55f;
                }
            }
            effects.add(new Effect(target.x, target.y, 0.25f, 0.16f, color(R.color.cst_warning)));
        }
        shots.add(new Shot(cart.x, cart.y, target.x, target.y, 0.12f, cartColor(cart.type)));
        audioEvent("cart_shot");
        if (soundEnabled) {
            cart.flash = 0.15f;
        }
    }

    private Enemy findTarget(Cart cart) {
        Enemy best = null;
        float bestDist = 10f;
        float range = cartRange(cart);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            float d = distance(cart.x, cart.y, enemy.x, enemy.y);
            if (d < range && d < bestDist) {
                best = enemy;
                bestDist = d;
            }
        }
        return best;
    }

    private void buildCart(int lane) {
        if (carts.size() >= 8) {
            selectedText = "Cart Limit";
            notifyHud();
            return;
        }
        int cost = cartCost(buildMode);
        if (ore < cost) {
            selectedText = "Need Ore " + cost;
            notifyHud();
            return;
        }
        ore -= cost;
        Cart cart = new Cart();
        cart.type = buildMode;
        cart.level = 1;
        cart.x = 0.21f + carts.size() * 0.018f;
        cart.y = laneY[lane];
        cart.targetLane = lane;
        cart.dir = 1f;
        cart.speed = 0.15f + buildMode * 0.012f;
        cart.cooldown = 0.25f;
        carts.add(cart);
        selectedText = cartName(buildMode) + " Ready";
        effects.add(new Effect(cart.x, cart.y, 0.35f, 0.20f, cartColor(buildMode)));
        audioEvent("upgrade");
        notifyHud();
    }

    private void upgradeCart(Cart cart) {
        if (cart.level >= 3) {
            selectedText = cartName(cart.type) + " Max";
            notifyHud();
            return;
        }
        int cost = 35 + cart.level * 25;
        if (ore < cost) {
            selectedText = "Need Ore " + cost;
            notifyHud();
            return;
        }
        ore -= cost;
        cart.level++;
        selectedText = cartName(cart.type) + " Lv" + cart.level;
        effects.add(new Effect(cart.x, cart.y, 0.35f, 0.18f, color(R.color.cst_accent)));
        audioEvent("upgrade");
        notifyHud();
    }

    private Cart findCart(float x, float y) {
        for (int i = carts.size() - 1; i >= 0; i--) {
            Cart cart = carts.get(i);
            if (distance(x, y, cart.x, cart.y) < 0.055f) {
                return cart;
            }
        }
        return null;
    }

    private boolean touchesJunction(float x, float y) {
        return x > 0.27f && x < 0.39f && y > 0.18f && y < 0.82f;
    }

    private int nearestLane(float y) {
        int best = 0;
        float bestDist = Math.abs(y - laneY[0]);
        for (int i = 1; i < laneY.length; i++) {
            float dist = Math.abs(y - laneY[i]);
            if (dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        return best;
    }

    private void finishRun(boolean cleared) {
        state = cleared ? GameState.STAGE_CLEAR : GameState.GAME_OVER;
        waveActive = false;
        selectedText = cleared ? "Stage Clear" : "Mine Lost";
        notifyHud();
        if (listener != null) {
            listener.onRunEnded(cleared, wave, leaks, calculateStars(cleared));
        }
    }

    private int calculateStars(boolean cleared) {
        if (!cleared) {
            return 0;
        }
        int stars = 1;
        if (reactor >= 65) {
            stars++;
        }
        if (leaks == 0) {
            stars++;
        }
        return stars;
    }

    private void notifyHud() {
        if (listener != null) {
            listener.onHudChanged(reactor, ore, wave, maxWave, leaks, "Route " + routeName(), selectedText);
        }
    }

    private void audioEvent(String key) {
        if (listener != null) {
            listener.onAudioEvent(key);
        }
    }

    private String routeName() {
        if (routeMode == 0) {
            return "Top";
        }
        if (routeMode == 2) {
            return "Low";
        }
        return "Mid";
    }

    private String cartName(int type) {
        if (type == BUILD_FROST) {
            return "Frost";
        }
        if (type == BUILD_TESLA) {
            return "Tesla";
        }
        if (type == BUILD_DRILL) {
            return "Drill";
        }
        return "Gatling";
    }

    private int cartCost(int type) {
        if (type == BUILD_FROST) {
            return 60;
        }
        if (type == BUILD_TESLA) {
            return 80;
        }
        if (type == BUILD_DRILL) {
            return 100;
        }
        return 45;
    }

    private float cartDamage(Cart cart) {
        float base;
        if (cart.type == BUILD_FROST) {
            base = 12f;
        } else if (cart.type == BUILD_TESLA) {
            base = 15f;
        } else if (cart.type == BUILD_DRILL) {
            base = 28f;
        } else {
            base = 10f;
        }
        return base * (1f + (cart.level - 1) * 0.42f);
    }

    private float cartCooldown(Cart cart) {
        if (cart.type == BUILD_FROST) {
            return 0.86f;
        }
        if (cart.type == BUILD_TESLA) {
            return 1.05f;
        }
        if (cart.type == BUILD_DRILL) {
            return 1.55f;
        }
        return 0.42f;
    }

    private float cartRange(Cart cart) {
        return 0.18f + cart.level * 0.025f;
    }

    private int cartColor(int type) {
        if (type == BUILD_FROST) {
            return 0xff7fc8ff;
        }
        if (type == BUILD_TESLA) {
            return 0xff73ffcf;
        }
        if (type == BUILD_DRILL) {
            return color(R.color.cst_warning);
        }
        return color(R.color.cst_accent_2);
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private int color(int resId) {
        return getResources().getColor(resId, null);
    }

    private void loadSprites() {
        if (spritesLoaded) {
            return;
        }
        spritesLoaded = true;
        loadSprite("cart_gatling", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile249.png");
        loadSprite("cart_frost", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile250.png");
        loadSprite("cart_tesla", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile245.png");
        loadSprite("cart_drill", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile246.png");
        loadSprite("enemy_basic", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_stand.png");
        loadSprite("enemy_fast", "game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_stand.png");
        loadSprite("enemy_armored", "game_art/kenney_top_down_shooter/assets/PNG/Robot 1/robot1_stand.png");
        loadSprite("enemy_boss", "game_art/kenney_top_down_shooter/assets/PNG/Robot 1/robot1_machine.png");
        loadSprite("reactor", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile247.png");
        loadSprite("cave", "game_art/kenney_top_down_shooter/assets/PNG/Tiles/tile_100.png");
    }

    private void loadSprite(String key, String path) {
        AssetManager assets = getContext().getAssets();
        try (InputStream input = assets.open(path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap != null) {
                sprites.put(key, bitmap);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean drawSprite(Canvas canvas, String key, float cx, float cy, float maxWidth, float maxHeight) {
        Bitmap bitmap = sprites.get(key);
        if (bitmap == null) {
            return false;
        }
        float scale = Math.min(maxWidth / bitmap.getWidth(), maxHeight / bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        rect.set(cx - width * 0.5f, cy - height * 0.5f, cx + width * 0.5f, cy + height * 0.5f);
        canvas.drawBitmap(bitmap, null, rect, paint);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawMine(canvas);
        drawRails(canvas);
        drawReactor(canvas);
        drawEnemies(canvas);
        drawCarts(canvas);
        drawShots(canvas);
        drawEffects(canvas);
        drawDepotHints(canvas);
        if (state == GameState.PLAYING && !waveActive) {
            drawPlanningText(canvas);
        }
    }

    private void drawMine(Canvas canvas) {
        canvas.drawColor(color(R.color.cst_bg_main));
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_bg_alt));
        rect.set(w * 0.02f, h * 0.12f, w * 0.98f, h * 0.88f);
        canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, paint);
        paint.setColor(color(R.color.cst_card_bg));
        for (int i = 0; i < laneY.length; i++) {
            float y = h * laneY[i];
            rect.set(w * 0.78f, y - h * 0.06f, w * 1.03f, y + h * 0.06f);
            canvas.drawRoundRect(rect, h * 0.03f, h * 0.03f, paint);
            drawSprite(canvas, "cave", w * 0.91f, y, h * 0.13f, h * 0.13f);
        }
    }

    private void drawRails(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.018f);
        paint.setColor(color(R.color.cst_panel_stroke));
        for (int i = 0; i < laneY.length; i++) {
            canvas.drawLine(w * 0.18f, h * laneY[i], w * 0.86f, h * laneY[i], paint);
        }
        canvas.drawLine(w * 0.31f, h * laneY[0], w * 0.31f, h * laneY[2], paint);
        canvas.drawLine(w * 0.72f, h * laneY[0], w * 0.72f, h * laneY[2], paint);
        paint.setStrokeWidth(h * 0.006f);
        paint.setColor(color(R.color.cst_accent));
        canvas.drawLine(w * 0.19f, h * laneY[routeMode], w * 0.85f, h * laneY[routeMode], paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_accent_2));
        rect.set(w * 0.27f, h * 0.43f, w * 0.35f, h * 0.57f);
        canvas.drawRoundRect(rect, h * 0.015f, h * 0.015f, paint);
        paint.setColor(color(R.color.cst_text_primary));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.035f);
        canvas.drawText(routeName().toUpperCase(Locale.US), w * 0.31f, h * 0.515f, paint);
    }

    private void drawReactor(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_panel_bg));
        rect.set(w * 0.05f, h * 0.38f, w * 0.16f, h * 0.62f);
        canvas.drawRoundRect(rect, h * 0.025f, h * 0.025f, paint);
        if (!drawSprite(canvas, "reactor", w * 0.105f, h * 0.50f, h * 0.16f, h * 0.16f)) {
            paint.setColor(reactor > 35 ? color(R.color.cst_success) : color(R.color.cst_danger));
            rect.set(w * 0.08f, h * 0.43f, w * 0.13f, h * 0.57f);
            canvas.drawOval(rect, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.006f);
        paint.setColor(color(R.color.cst_accent));
        rect.set(w * 0.07f, h * 0.41f, w * 0.14f, h * 0.59f);
        canvas.drawRoundRect(rect, h * 0.02f, h * 0.02f, paint);
    }

    private void drawEnemies(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            paint.setStyle(Paint.Style.FILL);
            if (enemy.type == 3) {
                paint.setColor(color(R.color.cst_danger));
            } else if (enemy.type == 2) {
                paint.setColor(color(R.color.cst_warning));
            } else if (enemy.type == 1) {
                paint.setColor(color(R.color.cst_success));
            } else {
                paint.setColor(color(R.color.cst_text_secondary));
            }
            float r = enemy.radius * h;
            float x = enemy.x * w;
            float y = enemy.y * h;
            if (!drawSprite(canvas, enemySprite(enemy.type), x, y, r * 3.2f, r * 3.2f)) {
                rect.set(x - r, y - r, x + r, y + r);
                canvas.drawOval(rect, paint);
            }
            paint.setColor(color(R.color.cst_danger));
            rect.set(x - r, y - r - h * 0.014f, x + r, y - r - h * 0.008f);
            canvas.drawRect(rect, paint);
            paint.setColor(color(R.color.cst_success));
            float hpWidth = (enemy.hp / enemy.maxHp) * r * 2f;
            rect.set(x - r, y - r - h * 0.014f, x - r + hpWidth, y - r - h * 0.008f);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawCarts(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int i = 0; i < carts.size(); i++) {
            Cart cart = carts.get(i);
            float x = cart.x * w;
            float y = cart.y * h;
            float bw = h * 0.07f;
            float bh = h * 0.042f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(cartColor(cart.type));
            rect.set(x - bw, y - bh, x + bw, y + bh);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
            if (!drawSprite(canvas, cartSprite(cart.type), x, y - bh * 0.25f, bw * 2.0f, bh * 3.6f)) {
                paint.setColor(color(R.color.cst_bg_main));
                canvas.drawCircle(x - bw * 0.45f, y + bh, h * 0.014f, paint);
                canvas.drawCircle(x + bw * 0.45f, y + bh, h * 0.014f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(h * 0.008f);
                paint.setColor(color(R.color.cst_text_primary));
                canvas.drawLine(x, y - bh, x + cart.dir * bw * 0.75f, y - bh * 1.6f, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.03f);
            paint.setColor(color(R.color.cst_text_on_primary));
            canvas.drawText(String.valueOf(cart.level), x, y + h * 0.01f, paint);
        }
    }

    private String cartSprite(int type) {
        if (type == BUILD_FROST) {
            return "cart_frost";
        }
        if (type == BUILD_TESLA) {
            return "cart_tesla";
        }
        if (type == BUILD_DRILL) {
            return "cart_drill";
        }
        return "cart_gatling";
    }

    private String enemySprite(int type) {
        if (type == 3) {
            return "enemy_boss";
        }
        if (type == 2) {
            return "enemy_armored";
        }
        if (type == 1) {
            return "enemy_fast";
        }
        return "enemy_basic";
    }

    private void drawShots(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.006f);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            paint.setColor(shot.color);
            canvas.drawLine(shot.x1 * w, shot.y1 * h, shot.x2 * w, shot.y2 * h, paint);
        }
    }

    private void drawEffects(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            float t = effect.life / effect.maxLife;
            paint.setColor(effect.color);
            paint.setStrokeWidth(h * 0.008f * t);
            canvas.drawCircle(effect.x * w, effect.y * h, effect.radius * h * (1f - t + 0.35f), paint);
        }
    }

    private void drawDepotHints(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.004f);
        paint.setColor(color(R.color.cst_warning));
        for (int i = 0; i < laneY.length; i++) {
            float y = laneY[i] * h;
            rect.set(w * 0.17f, y - h * 0.045f, w * 0.25f, y + h * 0.045f);
            canvas.drawRoundRect(rect, h * 0.012f, h * 0.012f, paint);
        }
    }

    private void drawPlanningText(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_text_primary));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(h * 0.04f);
        canvas.drawText("Tap a left depot to build. Tap junction to switch route.", w * 0.52f, h * 0.18f, paint);
    }

    private static final class Cart {
        int type;
        int level;
        int targetLane;
        float x;
        float y;
        float dir;
        float speed;
        float cooldown;
        float flash;
    }

    private static final class Enemy {
        int type;
        int lane;
        int damage;
        int reward;
        float x;
        float y;
        float hp;
        float maxHp;
        float speed;
        float slow;
        float stun;
        float radius;
    }

    private static final class Shot {
        final float x1;
        final float y1;
        final float x2;
        final float y2;
        float life;
        final int color;

        Shot(float x1, float y1, float x2, float y2, float life, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.life = life;
            this.color = color;
        }
    }

    private static final class Effect {
        final float x;
        final float y;
        final float maxLife;
        final float radius;
        final int color;
        float life;

        Effect(float x, float y, float life, float radius, int color) {
            this.x = x;
            this.y = y;
            this.life = life;
            this.maxLife = life;
            this.radius = radius;
            this.color = color;
        }
    }
}
