package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.android.boot.R;
import com.android.boot.audio.TonePlayer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GameView extends View {
    public interface HudListener {
        void onHudChanged(String timer, String score, String supply, String selected, String hint);
        void onMatchEnded(boolean win, String summary);
    }

    private static final int OWNER_NEUTRAL = 0;
    private static final int OWNER_PLAYER = 1;
    private static final int OWNER_ENEMY = 2;
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;
    private static final int STATE_GAME_OVER = 3;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Node> nodes = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Stream> streams = new ArrayList<>();
    private HudListener hudListener;
    private TonePlayer tonePlayer;
    private int state = STATE_MENU;
    private int selected = -1;
    private int dragTarget = -1;
    private float dragX;
    private float dragY;
    private boolean dragging;
    private long lastFrame;
    private float matchTime;
    private float incomeTick;
    private float enemyTick;
    private float hudTick;
    private int supply;
    private int playerScore;
    private int enemyScore;
    private float surgeCooldown;
    private int cBg;
    private int cPanel;
    private int cText;
    private int cMuted;
    private int cNeutral;
    private int cPlayer;
    private int cEnemy;
    private int cContested;
    private int cRoute;
    private Bitmap mapBackdrop;
    private Bitmap playerMarkerA;
    private Bitmap playerMarkerB;
    private Bitmap enemyMarkerA;
    private Bitmap enemyMarkerB;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cBg = color(R.color.cst_bg_alt);
        cPanel = color(R.color.cst_panel_stroke);
        cText = color(R.color.cst_text_primary);
        cMuted = color(R.color.cst_text_muted);
        cNeutral = color(R.color.cst_node_neutral);
        cPlayer = color(R.color.cst_node_player);
        cEnemy = color(R.color.cst_node_enemy);
        cContested = color(R.color.cst_node_contested);
        cRoute = color(R.color.cst_route_line);
        textPaint.setColor(cText);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mapBackdrop = loadBitmap("game_art/ring_prefecture_tokens/assets/map_backdrop.png");
        playerMarkerA = loadBitmap("game_art/ring_prefecture_tokens/assets/player_round_a.png");
        playerMarkerB = loadBitmap("game_art/ring_prefecture_tokens/assets/player_round_b.png");
        enemyMarkerA = loadBitmap("game_art/ring_prefecture_tokens/assets/enemy_round_a.png");
        enemyMarkerB = loadBitmap("game_art/ring_prefecture_tokens/assets/enemy_round_b.png");
        resetWorld();
    }

    private Bitmap loadBitmap(String path) {
        try (InputStream input = getContext().getAssets().open(path)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException ignored) {
            return null;
        }
    }

    private int color(int id) {
        return ContextCompat.getColor(getContext(), id);
    }

    public void setHudListener(HudListener hudListener) {
        this.hudListener = hudListener;
        pushHud();
    }

    public void setTonePlayer(TonePlayer tonePlayer) {
        this.tonePlayer = tonePlayer;
    }

    public void startMatch() {
        resetWorld();
        state = STATE_PLAYING;
        lastFrame = 0L;
        invalidate();
        pushHud();
    }

    public void pauseMatch() {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED;
        }
    }

    public void resumeMatch() {
        if (state == STATE_PAUSED) {
            state = STATE_PLAYING;
            lastFrame = 0L;
            invalidate();
        }
    }

    public void showMenuState() {
        state = STATE_MENU;
        invalidate();
    }

    public void upgradeSelected() {
        if (state != STATE_PLAYING || selected < 0 || supply < 18) {
            return;
        }
        Node node = nodes.get(selected);
        if (node.owner != OWNER_PLAYER || node.level >= 3) {
            return;
        }
        supply -= 18;
        node.level++;
        node.garrison += 8;
        if (tonePlayer != null) {
            tonePlayer.select();
        }
        pushHud();
        invalidate();
    }

    public void activateSurge() {
        if (state != STATE_PLAYING || supply < 22 || surgeCooldown > 0f) {
            return;
        }
        supply -= 22;
        surgeCooldown = 18f;
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.owner == OWNER_PLAYER && node.garrison > 12) {
                int target = bestPlayerTarget(i);
                if (target >= 0) {
                    sendStream(i, target, OWNER_PLAYER, 14, true);
                    node.garrison -= 7;
                }
            }
        }
        if (tonePlayer != null) {
            tonePlayer.surge();
        }
        pushHud();
        invalidate();
    }

    public void activateRepair() {
        if (state != STATE_PLAYING || selected < 0 || supply < 14) {
            return;
        }
        Node node = nodes.get(selected);
        if (node.owner == OWNER_PLAYER) {
            supply -= 14;
            node.garrison += 18;
            node.flash = 1f;
            pushHud();
            invalidate();
        }
    }

    private void resetWorld() {
        nodes.clear();
        routes.clear();
        streams.clear();
        nodes.add(new Node("Base", 0.08f, 0.50f, OWNER_PLAYER, 42, 0));
        nodes.add(new Node("Depot", 0.23f, 0.30f, OWNER_NEUTRAL, 22, 1));
        nodes.add(new Node("Mine", 0.25f, 0.70f, OWNER_NEUTRAL, 24, 2));
        nodes.add(new Node("Relay", 0.45f, 0.50f, OWNER_NEUTRAL, 28, 3));
        nodes.add(new Node("Bridge", 0.60f, 0.26f, OWNER_NEUTRAL, 22, 4));
        nodes.add(new Node("Repair", 0.62f, 0.73f, OWNER_NEUTRAL, 22, 5));
        nodes.add(new Node("Beacon", 0.76f, 0.48f, OWNER_NEUTRAL, 26, 6));
        nodes.add(new Node("Fort", 0.93f, 0.50f, OWNER_ENEMY, 42, 7));
        addRoute(0, 1);
        addRoute(0, 2);
        addRoute(1, 3);
        addRoute(2, 3);
        addRoute(3, 4);
        addRoute(3, 5);
        addRoute(4, 6);
        addRoute(5, 6);
        addRoute(6, 7);
        selected = -1;
        matchTime = 180f;
        incomeTick = 0f;
        enemyTick = 2.2f;
        hudTick = 0f;
        supply = 30;
        playerScore = 0;
        enemyScore = 0;
        surgeCooldown = 0f;
    }

    private void addRoute(int a, int b) {
        routes.add(new Route(a, b));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        if (lastFrame == 0L) {
            lastFrame = now;
        }
        float dt = Math.min(0.05f, (now - lastFrame) / 1000000000f);
        lastFrame = now;
        if (state == STATE_PLAYING) {
            update(dt);
        }
        drawWorld(canvas);
        if (state == STATE_PLAYING) {
            postInvalidateOnAnimation();
        }
    }

    private void update(float dt) {
        matchTime -= dt;
        incomeTick += dt;
        enemyTick -= dt;
        hudTick += dt;
        surgeCooldown = Math.max(0f, surgeCooldown - dt);
        for (Node node : nodes) {
            node.flash = Math.max(0f, node.flash - dt * 2.8f);
            node.pulse += dt * 5.5f;
        }
        updateStreams(dt);
        if (incomeTick >= 2f) {
            incomeTick -= 2f;
            supply += playerIncome();
            enemyReinforce();
            updateScores();
        }
        if (enemyTick <= 0f) {
            enemyTick = 2.5f + (float) Math.random() * 1.3f;
            runEnemyTurn();
        }
        if (hudTick >= 0.2f) {
            hudTick = 0f;
            pushHud();
        }
        if (nodes.get(0).owner == OWNER_ENEMY || nodes.get(0).garrison <= 0) {
            endMatch(false);
        } else if (nodes.get(7).owner == OWNER_PLAYER || nodes.get(7).garrison <= 0) {
            endMatch(true);
        } else if (matchTime <= 0f) {
            updateScores();
            endMatch(playerScore >= enemyScore);
        }
    }

    private void updateStreams(float dt) {
        for (int i = streams.size() - 1; i >= 0; i--) {
            Stream stream = streams.get(i);
            stream.t += dt * stream.speed;
            if (stream.t >= 1f) {
                applyArrival(stream);
                streams.remove(i);
            }
        }
    }

    private int playerIncome() {
        int income = 4;
        for (Node node : nodes) {
            if (node.owner == OWNER_PLAYER) {
                income += node.income();
            }
        }
        return income;
    }

    private void enemyReinforce() {
        for (Node node : nodes) {
            if (node.owner == OWNER_ENEMY) {
                node.garrison += 2 + node.level;
            }
        }
    }

    private void runEnemyTurn() {
        int source = -1;
        int best = -1;
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.owner == OWNER_ENEMY && node.garrison > best) {
                source = i;
                best = node.garrison;
            }
        }
        if (source < 0) {
            return;
        }
        int target = bestEnemyTarget(source);
        if (target >= 0) {
            int amount = Math.min(16, Math.max(8, nodes.get(source).garrison / 3));
            nodes.get(source).garrison -= amount;
            sendStream(source, target, OWNER_ENEMY, amount, false);
        }
    }

    private int bestPlayerTarget(int source) {
        int bestTarget = -1;
        int bestValue = -1000;
        for (Route route : routes) {
            int other = route.other(source);
            if (other >= 0) {
                Node node = nodes.get(other);
                int value = node.owner == OWNER_ENEMY ? 80 : 40;
                value += node.type * 3 - node.garrison;
                if (value > bestValue) {
                    bestValue = value;
                    bestTarget = other;
                }
            }
        }
        return bestTarget;
    }

    private int bestEnemyTarget(int source) {
        int bestTarget = -1;
        int bestValue = -1000;
        for (Route route : routes) {
            int other = route.other(source);
            if (other >= 0) {
                Node node = nodes.get(other);
                int value = node.owner == OWNER_PLAYER ? 85 : 35;
                value += (7 - other) * 4 - node.garrison;
                if (value > bestValue) {
                    bestValue = value;
                    bestTarget = other;
                }
            }
        }
        return bestTarget;
    }

    private void sendStream(int from, int to, int owner, int amount, boolean surge) {
        streams.add(new Stream(from, to, owner, amount, surge));
        if (owner == OWNER_PLAYER && tonePlayer != null) {
            tonePlayer.dispatch();
        }
    }

    private void applyArrival(Stream stream) {
        Node target = nodes.get(stream.to);
        if (target.owner == stream.owner) {
            target.garrison += stream.amount;
            target.flash = 0.6f;
            return;
        }
        target.garrison -= stream.amount;
        target.contested = true;
        target.contestOwner = stream.owner;
        if (target.garrison <= 0) {
            target.owner = stream.owner;
            target.garrison = 12 + stream.amount / 2;
            target.level = Math.max(1, target.level);
            target.contested = false;
            target.flash = 1f;
            if (stream.owner == OWNER_PLAYER && tonePlayer != null) {
                tonePlayer.capture();
            }
        }
    }

    private void updateScores() {
        playerScore = 0;
        enemyScore = 0;
        for (Node node : nodes) {
            int value = 10 + node.level * 4 + node.type;
            if (node.owner == OWNER_PLAYER) {
                playerScore += value;
            } else if (node.owner == OWNER_ENEMY) {
                enemyScore += value;
            }
        }
        playerScore += Math.max(0, 42 - nodes.get(7).garrison);
        enemyScore += Math.max(0, 42 - nodes.get(0).garrison);
    }

    private void endMatch(boolean win) {
        if (state == STATE_GAME_OVER) {
            return;
        }
        state = STATE_GAME_OVER;
        updateScores();
        String summary = "Score " + playerScore + " to " + enemyScore + ". Owned points " + countOwner(OWNER_PLAYER) + ". Supply " + supply + ".";
        if (hudListener != null) {
            hudListener.onMatchEnded(win, summary);
        }
    }

    private int countOwner(int owner) {
        int count = 0;
        for (Node node : nodes) {
            if (node.owner == owner) {
                count++;
            }
        }
        return count;
    }

    private void drawWorld(Canvas canvas) {
        canvas.drawColor(cBg);
        drawGrid(canvas);
        drawRoutes(canvas);
        drawStreams(canvas);
        drawDrag(canvas);
        drawNodes(canvas);
        if (state == STATE_MENU) {
            drawBoardTitle(canvas, "Capture gray points to turn them teal");
        } else if (state == STATE_PAUSED) {
            drawBoardTitle(canvas, "Command paused");
        }
    }

    private void drawGrid(Canvas canvas) {
        if (mapBackdrop != null) {
            RectF dst = new RectF(0, 0, getWidth(), getHeight());
            paint.setAlpha(88);
            canvas.drawBitmap(mapBackdrop, null, dst, paint);
            paint.setAlpha(255);
        }
        paint.setStrokeWidth(dp(1));
        paint.setColor(adjustAlpha(cPanel, 80));
        float step = dp(42);
        for (float x = 0; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (float y = 0; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawRoutes(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(5));
        paint.setColor(cRoute);
        for (Route route : routes) {
            Node a = nodes.get(route.a);
            Node b = nodes.get(route.b);
            canvas.drawLine(px(a), py(a), px(b), py(b), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawStreams(Canvas canvas) {
        for (Stream stream : streams) {
            Node a = nodes.get(stream.from);
            Node b = nodes.get(stream.to);
            float x = lerp(px(a), px(b), stream.t);
            float y = lerp(py(a), py(b), stream.t);
            Bitmap marker = markerFor(stream.owner, stream.t);
            paint.setColor(stream.owner == OWNER_PLAYER ? cPlayer : cEnemy);
            paint.setStyle(Paint.Style.FILL);
            if (marker != null) {
                float size = stream.surge ? dp(22) : dp(18);
                RectF dst = new RectF(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
                canvas.drawBitmap(marker, null, dst, paint);
            } else {
                canvas.drawCircle(x, y, stream.surge ? dp(8) : dp(6), paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stream.surge ? dp(4) : dp(2));
            paint.setColor(adjustAlpha(stream.owner == OWNER_PLAYER ? cPlayer : cEnemy, 140));
            canvas.drawLine(lerp(px(a), px(b), Math.max(0f, stream.t - 0.12f)), lerp(py(a), py(b), Math.max(0f, stream.t - 0.12f)), x, y, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawDrag(Canvas canvas) {
        if (!dragging || selected < 0) {
            return;
        }
        Node source = nodes.get(selected);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4));
        paint.setColor(dragTarget >= 0 ? cContested : cMuted);
        canvas.drawLine(px(source), py(source), dragX, dragY, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawNodes(Canvas canvas) {
        textPaint.setTextSize(sp(12));
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            float x = px(node);
            float y = py(node);
            float radius = nodeRadius(node);
            int fill = ownerColor(node);
            if (node.contested) {
                float pulse = (float) ((Math.sin(node.pulse) + 1f) * 0.5f);
                fill = blend(fill, cContested, pulse * 0.35f);
            }
            if (node.flash > 0f) {
                fill = blend(fill, Color.WHITE, node.flash * 0.35f);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(i == selected ? dp(5) : dp(3));
            paint.setColor(i == selected ? cContested : cText);
            canvas.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setColor(node.owner == OWNER_NEUTRAL ? cText : Color.rgb(8, 16, 14));
            canvas.drawText(String.valueOf(Math.max(0, node.garrison)), x, y + dp(4), textPaint);
            textPaint.setColor(cText);
            canvas.drawText(node.name, x, y + radius + dp(16), textPaint);
            if (node.owner == OWNER_PLAYER && i != 0) {
                drawFlag(canvas, x, y - radius - dp(6), cPlayer);
            } else if (node.owner == OWNER_ENEMY && i != 7) {
                drawFlag(canvas, x, y - radius - dp(6), cEnemy);
            }
        }
    }

    private void drawFlag(Canvas canvas, float x, float y, int color) {
        paint.setColor(color);
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(x, y, x, y - dp(18), paint);
        Path path = new Path();
        path.moveTo(x, y - dp(18));
        path.lineTo(x + dp(18), y - dp(13));
        path.lineTo(x, y - dp(8));
        path.close();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }

    private Bitmap markerFor(int owner, float t) {
        boolean alt = ((int) (t * 12f)) % 2 == 0;
        if (owner == OWNER_PLAYER) {
            return alt ? playerMarkerA : playerMarkerB;
        }
        return alt ? enemyMarkerA : enemyMarkerB;
    }

    private void drawBoardTitle(Canvas canvas, String text) {
        RectF rect = new RectF(dp(24), dp(24), getWidth() - dp(24), dp(72));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(cPanel, 180));
        canvas.drawRoundRect(rect, dp(8), dp(8), paint);
        textPaint.setColor(cText);
        textPaint.setTextSize(sp(16));
        canvas.drawText(text, getWidth() * 0.5f, dp(55), textPaint);
    }

    private int ownerColor(Node node) {
        if (node.owner == OWNER_PLAYER) {
            return cPlayer;
        }
        if (node.owner == OWNER_ENEMY) {
            return cEnemy;
        }
        return cNeutral;
    }

    private float nodeRadius(Node node) {
        if (node.type == 0 || node.type == 7) {
            return dp(25);
        }
        return dp(20 + node.level * 2);
    }

    private float px(Node node) {
        return node.x * getWidth();
    }

    private float py(Node node) {
        return node.y * getHeight();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private int adjustAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int blend(int from, int to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = (int) lerp(Color.red(from), Color.red(to), clamped);
        int g = (int) lerp(Color.green(from), Color.green(to), clamped);
        int b = (int) lerp(Color.blue(from), Color.blue(to), clamped);
        return Color.rgb(r, g, b);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != STATE_PLAYING) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int hit = hitNode(x, y);
            if (hit >= 0 && nodes.get(hit).owner == OWNER_PLAYER) {
                selected = hit;
                dragging = true;
                dragX = x;
                dragY = y;
                dragTarget = -1;
                if (tonePlayer != null) {
                    tonePlayer.select();
                }
                pushHud();
            }
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            dragX = x;
            dragY = y;
            dragTarget = nearestConnectedNode(selected, x, y);
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (dragging && selected >= 0 && dragTarget >= 0) {
                dispatchSelected(dragTarget);
            }
            dragging = false;
            dragTarget = -1;
            invalidate();
            return true;
        }
        return true;
    }

    private int hitNode(float x, float y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            float dx = x - px(node);
            float dy = y - py(node);
            float hitRadius = nodeRadius(node) + dp(18);
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return i;
            }
        }
        return -1;
    }

    private int nearestConnectedNode(int source, float x, float y) {
        if (source < 0) {
            return -1;
        }
        int best = -1;
        float bestDist = dp(42) * dp(42);
        for (Route route : routes) {
            int other = route.other(source);
            if (other >= 0) {
                Node node = nodes.get(other);
                float dx = x - px(node);
                float dy = y - py(node);
                float dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = other;
                }
            }
        }
        return best;
    }

    private void dispatchSelected(int target) {
        Node source = nodes.get(selected);
        if (source.owner != OWNER_PLAYER || source.garrison <= 10) {
            return;
        }
        int amount = Math.max(7, source.garrison / 2);
        source.garrison -= amount;
        sendStream(selected, target, OWNER_PLAYER, amount, false);
        pushHud();
    }

    private void pushHud() {
        if (hudListener == null) {
            return;
        }
        int seconds = Math.max(0, (int) Math.ceil(matchTime));
        String timer = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        String score = "Player " + playerScore + "  Enemy " + enemyScore;
        String supplyText = "Supply " + supply;
        String selectedName = "Select a point";
        String hint = surgeCooldown > 0f ? "Surge ready in " + Math.ceil(surgeCooldown) + "s" : "Drag from teal points to send troops";
        if (selected >= 0) {
            Node node = nodes.get(selected);
            selectedName = node.name + " Lv " + node.level + " Units " + Math.max(0, node.garrison);
            if (node.owner == OWNER_PLAYER) {
                hint = "Upgrade or drag to a linked point";
            }
        }
        hudListener.onHudChanged(timer, score, supplyText, selectedName, hint);
    }

    private static class Node {
        final String name;
        final float x;
        final float y;
        final int type;
        int owner;
        int garrison;
        int level = 1;
        boolean contested;
        int contestOwner;
        float pulse;
        float flash;

        Node(String name, float x, float y, int owner, int garrison, int type) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.garrison = garrison;
            this.type = type;
        }

        int income() {
            if (type == 0 || type == 7) {
                return 2 + level;
            }
            if (type == 2) {
                return 7 + level * 2;
            }
            if (type == 5) {
                return 3 + level;
            }
            return 4 + level;
        }
    }

    private static class Route {
        final int a;
        final int b;

        Route(int a, int b) {
            this.a = a;
            this.b = b;
        }

        int other(int id) {
            if (id == a) {
                return b;
            }
            if (id == b) {
                return a;
            }
            return -1;
        }
    }

    private static class Stream {
        final int from;
        final int to;
        final int owner;
        final int amount;
        final boolean surge;
        final float speed;
        float t;

        Stream(int from, int to, int owner, int amount, boolean surge) {
            this.from = from;
            this.to = to;
            this.owner = owner;
            this.amount = amount;
            this.surge = surge;
            this.speed = surge ? 0.72f : 0.48f;
        }
    }
}
