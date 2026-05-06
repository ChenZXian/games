package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.R;
import com.android.boot.audio.AudioManager;
import com.android.boot.core.GameManager;
import com.android.boot.core.GameState;
import com.android.boot.entity.CookingStep;
import com.android.boot.entity.Recipe;
import com.android.boot.fx.EffectManager;
import java.util.ArrayList;

public class GameView extends SurfaceView implements Runnable {
    private final SurfaceHolder holder;
    private final Paint paint = new Paint(1);
    private final GameManager game = new GameManager();
    private final EffectManager effects = new EffectManager();
    private final AudioManager audio;
    private final ArrayList<ButtonRect> buttons = new ArrayList<>();
    private Thread thread;
    private boolean running;
    private long lastTime;
    private int cBg;
    private int cAlt;
    private int cPanel;
    private int cStroke;
    private int cText;
    private int cMuted;
    private int cAccent;
    private int cAccent2;
    private int cSuccess;
    private int cWarn;
    private int cDanger;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        setFocusable(true);
        audio = new AudioManager(context);
        cBg = context.getColor(R.color.cst_bg_main);
        cAlt = context.getColor(R.color.cst_bg_alt);
        cPanel = context.getColor(R.color.cst_panel_bg);
        cStroke = context.getColor(R.color.cst_panel_stroke);
        cText = context.getColor(R.color.cst_text_primary);
        cMuted = context.getColor(R.color.cst_text_muted);
        cAccent = context.getColor(R.color.cst_accent);
        cAccent2 = context.getColor(R.color.cst_accent_2);
        cSuccess = context.getColor(R.color.cst_success);
        cWarn = context.getColor(R.color.cst_warning);
        cDanger = context.getColor(R.color.cst_danger);
    }

    public void run() {
        lastTime = System.nanoTime();
        while (running) {
            if (!holder.getSurface().isValid()) continue;
            long now = System.nanoTime();
            float delta = (now - lastTime) / 1000000000f;
            lastTime = now;
            if (delta > 0.05f) delta = 0.05f;
            game.update(delta);
            effects.update(delta);
            Canvas canvas = holder.lockCanvas();
            drawScreen(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawScreen(Canvas canvas) {
        buttons.clear();
        canvas.drawColor(cBg);
        if (game.state == GameState.MENU) drawMenu(canvas);
        if (game.state == GameState.PLAYING) drawKitchen(canvas);
        if (game.state == GameState.PAUSED) {
            drawKitchen(canvas);
            drawPause(canvas);
        }
        if (game.state == GameState.GAME_OVER) drawResult(canvas);
        effects.draw(canvas, paint, cSuccess);
    }

    private void drawMenu(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        drawRound(canvas, 60, 40, w - 60, h - 40, cPanel, cStroke, 28f);
        text(canvas, "Kitchen Craft Story", 100, 110, 44, cText, true);
        text(canvas, "Pick an order, prep ingredients, cook with the right tool, plate, decorate, and earn stars.", 100, 160, 22, cMuted, false);
        int y = 220;
        for (int i = 0; i < game.recipeManager.all().size(); i++) {
            Recipe recipe = game.recipeManager.get(i);
            drawRound(canvas, 100, y, w - 100, y + 86, cAlt, cStroke, 22f);
            text(canvas, recipe.name, 130, y + 34, 26, cText, true);
            text(canvas, recipe.style + " order  Reward " + recipe.reward, 130, y + 66, 19, cMuted, false);
            addButton("recipe:" + i, 100, y, w - 100, y + 86);
            y += 104;
        }
        drawButton(canvas, "start", "Start First Order", w - 360, h - 110, w - 100, h - 56, true);
    }

    private void drawKitchen(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        CookingStep step = game.step();
        drawRound(canvas, 24, 20, w - 24, 96, cPanel, cStroke, 24f);
        text(canvas, game.recipe.name, 48, 58, 28, cText, true);
        text(canvas, "Customer " + game.orderManager.customerName, 350, 58, 20, cMuted, false);
        drawMeter(canvas, w - 330, 44, w - 60, 68, game.orderManager.patience / 90f, cSuccess);
        text(canvas, "Patience", w - 330, 35, 16, cMuted, false);
        drawRound(canvas, 24, 112, 260, h - 24, cPanel, cStroke, 24f);
        text(canvas, "Ingredients", 48, 154, 25, cText, true);
        int iy = 190;
        for (String id : game.recipe.ingredients) {
            drawFood(canvas, 62, iy + 16, id);
            text(canvas, id, 106, iy + 26, 18, cText, false);
            iy += 55;
        }
        drawRound(canvas, 280, 112, w - 280, h - 120, cPanel, cStroke, 24f);
        text(canvas, step == null ? "Submit" : "Step " + (game.stepIndex + 1) + "  " + step.action + " with " + step.tool, 310, 154, 26, cText, true);
        drawKitchenScene(canvas, 300, 180, w - 310, h - 145);
        if (step != null) {
            float progress = step.targetTaps == 0 ? 1f : (float) game.stepProgress / (float) step.targetTaps;
            drawMeter(canvas, 330, h - 182, w - 340, h - 158, progress, cAccent);
            drawMeter(canvas, 330, h - 138, w - 340, h - 114, game.heat, cWarn);
            text(canvas, "Progress", 330, h - 190, 16, cMuted, false);
            text(canvas, "Heat", 330, h - 146, 16, cMuted, false);
        }
        drawTools(canvas, 280, h - 100, w - 280, h - 24);
        drawButton(canvas, "pause", "Pause", w - 160, 30, w - 48, 80, false);
    }

    private void drawKitchenScene(Canvas canvas, int l, int t, int r, int b) {
        drawRound(canvas, l, t, r, b, cAlt, cStroke, 24f);
        paint.setColor(cAccent);
        canvas.drawOval(new RectF(l + 60, b - 155, l + 260, b - 35), paint);
        paint.setColor(cWarn);
        canvas.drawOval(new RectF(l + 95, b - 125, l + 225, b - 55), paint);
        paint.setColor(cAccent2);
        canvas.drawRoundRect(new RectF(r - 250, t + 50, r - 90, t + 170), 24f, 24f, paint);
        paint.setColor(cSuccess);
        canvas.drawCircle(r - 170, t + 110, 32f, paint);
        text(canvas, "Prep", l + 105, b - 80, 24, cText, true);
        text(canvas, "Cook", r - 205, t + 118, 22, cText, true);
        text(canvas, game.recipe.finishLook, l + 60, t + 45, 19, cMuted, false);
    }

    private void drawTools(Canvas canvas, int l, int t, int r, int b) {
        String[] names = {"sink", "knife", "bowl", "pan", "pot", "oven", "bottle", "plate"};
        int count = names.length;
        int gap = 10;
        int bw = (r - l - gap * (count - 1)) / count;
        for (int i = 0; i < count; i++) {
            int x = l + i * (bw + gap);
            drawButton(canvas, "tool:" + names[i], names[i], x, t, x + bw, b, false);
        }
    }

    private void drawResult(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        drawRound(canvas, 90, 60, w - 90, h - 60, cPanel, cStroke, 30f);
        text(canvas, "Order Complete", 130, 130, 42, cText, true);
        text(canvas, game.recipe.name, 130, 180, 26, cMuted, false);
        text(canvas, "Score " + game.finalScore, 130, 245, 32, cText, true);
        text(canvas, "Stars " + game.stars + "   Coins " + game.coins, 130, 295, 28, cText, true);
        text(canvas, "Unlocked " + game.unlockText, 130, 345, 24, cAccent2, true);
        drawDish(canvas, w - 350, 155, 210);
        drawButton(canvas, "restart", "Cook Again", 130, h - 130, 360, h - 72, true);
        drawButton(canvas, "menu", "Menu", 390, h - 130, 600, h - 72, false);
    }

    private void drawPause(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        drawRound(canvas, w / 2 - 180, h / 2 - 110, w / 2 + 180, h / 2 + 110, cPanel, cStroke, 26f);
        text(canvas, "Paused", w / 2 - 70, h / 2 - 35, 36, cText, true);
        drawButton(canvas, "pause", "Resume", w / 2 - 125, h / 2 + 20, w / 2 + 125, h / 2 + 76, true);
    }

    private void drawDish(Canvas canvas, int cx, int cy, int size) {
        paint.setColor(cStroke);
        canvas.drawOval(new RectF(cx - size, cy, cx + size, cy + size / 2), paint);
        paint.setColor(cPanel);
        canvas.drawOval(new RectF(cx - size + 16, cy + 12, cx + size - 16, cy + size / 2 - 8), paint);
        paint.setColor(cWarn);
        canvas.drawOval(new RectF(cx - 95, cy + 20, cx + 80, cy + 85), paint);
        paint.setColor(cSuccess);
        canvas.drawCircle(cx + 44, cy + 38, 18, paint);
        paint.setColor(cDanger);
        canvas.drawCircle(cx - 34, cy + 46, 16, paint);
    }

    private void drawFood(Canvas canvas, int cx, int cy, String id) {
        int color = cWarn;
        if (id.equals("tomato")) color = cDanger;
        if (id.equals("basil")) color = cSuccess;
        if (id.equals("shrimp")) color = cAccent2;
        if (id.equals("rice") || id.equals("flour") || id.equals("cream")) color = cPanel;
        paint.setColor(color);
        canvas.drawCircle(cx, cy, 20, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(cStroke);
        canvas.drawCircle(cx, cy, 20, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawButton(Canvas canvas, String id, String label, int l, int t, int r, int b, boolean primary) {
        drawRound(canvas, l, t, r, b, primary ? cAccent : cAlt, primary ? cAccent2 : cStroke, 22f);
        text(canvas, label, l + 18, t + (b - t) / 2 + 8, 20, primary ? cPanel : cText, true);
        addButton(id, l, t, r, b);
    }

    private void drawMeter(Canvas canvas, int l, int t, int r, int b, float value, int fill) {
        if (value < 0f) value = 0f;
        if (value > 1f) value = 1f;
        drawRound(canvas, l, t, r, b, cStroke, cStroke, 12f);
        drawRound(canvas, l, t, l + (int)((r - l) * value), b, fill, fill, 12f);
    }

    private void drawRound(Canvas canvas, int l, int t, int r, int b, int fill, int stroke, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(new RectF(l, t, r, b), radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(stroke);
        canvas.drawRoundRect(new RectF(l, t, r, b), radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setFakeBoldText(bold);
        canvas.drawText(value, x, y, paint);
        paint.setFakeBoldText(false);
    }

    private void addButton(String id, int l, int t, int r, int b) {
        buttons.add(new ButtonRect(id, l, t, r, b));
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        float x = event.getX();
        float y = event.getY();
        for (ButtonRect button : buttons) {
            if (button.hit(x, y)) {
                handle(button.id, x, y);
                return true;
            }
        }
        return true;
    }

    private void handle(String id, float x, float y) {
        if (id.startsWith("recipe:")) {
            int index = Integer.parseInt(id.substring(7));
            game.selectRecipe(index);
            audio.click();
            return;
        }
        if (id.equals("start")) {
            game.selectRecipe(0);
            audio.click();
            return;
        }
        if (id.equals("pause")) {
            game.pauseToggle();
            audio.click();
            return;
        }
        if (id.equals("restart")) {
            game.restart();
            audio.click();
            return;
        }
        if (id.equals("menu")) {
            game.menu();
            audio.click();
            return;
        }
        if (id.startsWith("tool:")) {
            boolean ok = game.useTool(id.substring(5));
            if (ok) {
                audio.success();
                effects.pop(x, y);
            } else {
                audio.error();
            }
        }
    }

    public void resume() {
        if (running) return;
        audio.startBgm();
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        audio.stopBgm();
    }

    public void releaseAudio() {
        audio.release();
    }

    private static class ButtonRect {
        final String id;
        final RectF rect;

        ButtonRect(String id, int l, int t, int r, int b) {
            this.id = id;
            rect = new RectF(l, t, r, b);
        }

        boolean hit(float x, float y) {
            return rect.contains(x, y);
        }
    }
}
