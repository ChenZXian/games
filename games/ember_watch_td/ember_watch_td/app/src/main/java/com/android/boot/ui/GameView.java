package com.android.boot.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.core.content.ContextCompat;
import com.android.boot.MainActivity;
import com.android.boot.R;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.entity.BranchType;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.DamageText;
import com.android.boot.entity.PointF2;
import com.android.boot.entity.Projectile;
import com.android.boot.entity.Skill;
import com.android.boot.entity.Soldier;
import com.android.boot.entity.Tower;
import com.android.boot.entity.TowerSlot;
import com.android.boot.entity.UiActionButton;
import java.util.Locale;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final float BASE_WIDTH = 1280f;
    private static final float BASE_HEIGHT = 720f;
    private final SurfaceHolder holder;
    private final GameSession session;
    private Thread loopThread;
    private boolean running;
    private long lastFrameNanos;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint towerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();
    private int cBgMain;
    private int cBgAlt;
    private int cPanel;
    private int cStroke;
    private int cPrimary;
    private int cSecondary;
    private int cMuted;
    private int cAccent;
    private int cAccent2;
    private int cDanger;
    private int cSuccess;
    private int cWarning;
    private int cTextOnPrimary;

    public GameView(Context context) {
        this(context, 1);
    }

    public GameView(Context context, int levelIndex) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        session = new GameSession(levelIndex);
        session.startGame();
        loadPalette();
        setupPaints(context);
        setFocusable(true);
    }

    private void loadPalette() {
        cBgMain = ContextCompat.getColor(getContext(), R.color.cst_bg_main);
        cBgAlt = ContextCompat.getColor(getContext(), R.color.cst_bg_alt);
        cPanel = ContextCompat.getColor(getContext(), R.color.cst_panel_bg);
        cStroke = ContextCompat.getColor(getContext(), R.color.cst_panel_stroke);
        cPrimary = ContextCompat.getColor(getContext(), R.color.cst_text_primary);
        cSecondary = ContextCompat.getColor(getContext(), R.color.cst_text_secondary);
        cMuted = ContextCompat.getColor(getContext(), R.color.cst_text_muted);
        cAccent = ContextCompat.getColor(getContext(), R.color.cst_accent);
        cAccent2 = ContextCompat.getColor(getContext(), R.color.cst_accent_2);
        cDanger = ContextCompat.getColor(getContext(), R.color.cst_danger);
        cSuccess = ContextCompat.getColor(getContext(), R.color.cst_success);
        cWarning = ContextCompat.getColor(getContext(), R.color.cst_warning);
        cTextOnPrimary = ContextCompat.getColor(getContext(), R.color.cst_text_on_primary);
    }

    private void setupPaints(Context context) {
        textPaint.setColor(cPrimary);
        textPaint.setTextSize(sp(22f));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        smallTextPaint.setColor(cSecondary);
        smallTextPaint.setTextSize(sp(13f));

        hudValuePaint.setColor(cPrimary);
        hudValuePaint.setTextSize(sp(20f));
        hudValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        hudLabelPaint.setColor(cSecondary);
        hudLabelPaint.setTextSize(sp(11f));
        hudLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeWidth(dp(44f));
        pathPaint.setColor(adjustAlpha(cWarning, 0.55f));

        hpBackPaint.setColor(adjustAlpha(cMuted, 0.5f));
        hpFillPaint.setColor(cSuccess);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.min(255, Math.max(0, Math.round(Color.alpha(color) * factor)));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        running = true;
        loopThread = new Thread(this, "GameLoop");
        lastFrameNanos = System.nanoTime();
        loopThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void onHostPause() {
        if (session.state == GameState.PLAYING) {
            session.togglePause();
        }
    }

    public void onHostResume() {
    }

    @Override
    public void run() {
        while (running) {
            try {
                long now = System.nanoTime();
                float dt = (now - lastFrameNanos) / 1000000000f;
                lastFrameNanos = now;
                session.update(dt);
                if (!holder.getSurface().isValid()) {
                    continue;
                }
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawFrame(canvas);
                    holder.unlockCanvasAndPost(canvas);
                }
            } catch (Exception ignored) {
                // Keep the render loop alive to avoid random host crashes.
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        float scaleX = canvas.getWidth() / BASE_WIDTH;
        float scaleY = canvas.getHeight() / BASE_HEIGHT;
        canvas.save();
        canvas.scale(scaleX, scaleY);
        float w = BASE_WIDTH;
        float h = BASE_HEIGHT;
        canvas.drawColor(cBgMain);
        drawBackground(canvas, w, h);
        drawPath(canvas);
        drawSlots(canvas);
        drawTowers(canvas);
        drawProjectiles(canvas);
        drawEnemies(canvas);
        drawDamageTexts(canvas);
        drawSoldiers(canvas);
        drawHero(canvas);
        drawHeroSlashFx(canvas);
        drawHud(canvas, w, h);
        if (session.state == GameState.MENU) {
            drawMenu(canvas, w, h);
        } else if (session.state == GameState.PAUSED) {
            drawPause(canvas, w, h);
        } else if (session.state == GameState.GAME_OVER) {
            drawGameOver(canvas, w, h);
        }
        drawSkillBar(canvas, w, h);
        drawSelectionPanel(canvas, w, h);
        drawBanner(canvas, w, h);
        if (session.waitingForSkillTarget) {
            drawTargeting(canvas, w, h);
        }
        canvas.restore();
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cBgAlt);
        canvas.drawRect(0f, h * 0.64f, w, h, paint);
        paint.setColor(adjustAlpha(cAccent, 0.12f));
        canvas.drawCircle(180f, 180f, 120f, paint);
        paint.setColor(adjustAlpha(cAccent2, 0.10f));
        canvas.drawCircle(w - 220f, 160f, 150f, paint);
        paint.setColor(adjustAlpha(cSuccess, 0.12f));
        canvas.drawCircle(w * 0.5f, h - 90f, 200f, paint);
        paint.setColor(adjustAlpha(cPrimary, 0.08f));
        canvas.drawRect(w - 80f, 0f, w, h, paint);
    }

    private void drawPath(Canvas canvas) {
        if (session.level.path.size() < 2) {
            return;
        }
        for (int i = 0; i < session.level.path.size() - 1; i++) {
            PointF2 a = session.level.path.get(i);
            PointF2 b = session.level.path.get(i + 1);
            canvas.drawLine(a.x, a.y, b.x, b.y, pathPaint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cDanger);
        PointF2 start = session.level.path.get(0);
        canvas.drawCircle(start.x, start.y, 20f, paint);
        paint.setColor(cAccent2);
        PointF2 end = session.level.path.get(session.level.path.size() - 1);
        canvas.drawCircle(end.x, end.y, 24f, paint);
    }

    private void drawSlots(Canvas canvas) {
        for (TowerSlot slot : session.level.slots) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(slot.tower == null ? adjustAlpha(cStroke, 0.45f) : adjustAlpha(cAccent2, 0.28f));
            canvas.drawCircle(slot.x, slot.y, 28f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(slot == session.selectedSlot ? cAccent2 : cStroke);
            canvas.drawCircle(slot.x, slot.y, 34f, paint);
        }
    }

    private void drawTowers(Canvas canvas) {
        for (TowerSlot slot : session.level.slots) {
            if (slot.tower == null) {
                continue;
            }
            Tower tower = slot.tower;
            drawTowerBase(canvas, tower);
            if (tower.type == com.android.boot.entity.TowerType.ARROW) {
                drawArrowTower(canvas, tower);
            } else if (tower.type == com.android.boot.entity.TowerType.ARTILLERY) {
                drawArtilleryTower(canvas, tower);
            } else if (tower.type == com.android.boot.entity.TowerType.BARRACKS) {
                drawBarracksTower(canvas, tower);
            } else {
                drawMageTower(canvas, tower);
            }
            if (tower == session.selectedTower) {
                towerPaint.setColor(adjustAlpha(cAccent2, 0.2f));
                towerPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(tower.x, tower.y, tower.range, towerPaint);
            }
            drawTowerBadge(canvas, tower);
        }
    }

    private void drawTowerBase(Canvas canvas, Tower tower) {
        float r = tower.type == com.android.boot.entity.TowerType.BARRACKS ? 25f : 22f;
        towerPaint.setStyle(Paint.Style.FILL);
        towerPaint.setColor(adjustAlpha(cPanel, 0.95f));
        canvas.drawCircle(tower.x, tower.y + 4f, r + 8f, towerPaint);
        towerPaint.setColor(adjustAlpha(getTowerColor(tower), 0.35f));
        canvas.drawCircle(tower.x, tower.y + 1f, r + 2f, towerPaint);
        towerPaint.setStyle(Paint.Style.STROKE);
        towerPaint.setStrokeWidth(2.5f);
        towerPaint.setColor(cStroke);
        canvas.drawCircle(tower.x, tower.y + 1f, r + 2f, towerPaint);
    }

    private void drawArrowTower(Canvas canvas, Tower tower) {
        float x = tower.x;
        float y = tower.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPrimary);
        canvas.drawRect(x - 8f, y - 16f, x + 8f, y + 14f, paint);
        paint.setColor(getTowerColor(tower));
        canvas.drawRect(x - 12f, y - 24f, x + 12f, y - 10f, paint);
        paint.setColor(cWarning);
        canvas.drawRect(x - 2f, y - 28f, x + 2f, y - 8f, paint);
        canvas.drawRect(x - 9f, y - 22f, x + 9f, y - 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRect(x - 12f, y - 24f, x + 12f, y - 10f, paint);
    }

    private void drawArtilleryTower(Canvas canvas, Tower tower) {
        float x = tower.x;
        float y = tower.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cWarning);
        canvas.drawRoundRect(x - 14f, y - 14f, x + 14f, y + 12f, 6f, 6f, paint);
        paint.setColor(getTowerColor(tower));
        canvas.drawCircle(x, y - 16f, 11f, paint);
        paint.setColor(cPrimary);
        canvas.drawRoundRect(x - 2f, y - 28f, x + 26f, y - 20f, 4f, 4f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(x - 14f, y - 14f, x + 14f, y + 12f, 6f, 6f, paint);
    }

    private void drawBarracksTower(Canvas canvas, Tower tower) {
        float x = tower.x;
        float y = tower.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getTowerColor(tower));
        canvas.drawRect(x - 16f, y - 10f, x + 16f, y + 14f, paint);
        paint.setColor(cPrimary);
        canvas.drawRect(x - 20f, y - 20f, x + 20f, y - 10f, paint);
        paint.setColor(cDanger);
        canvas.drawRect(x - 3f, y - 34f, x + 3f, y - 20f, paint);
        canvas.drawRect(x + 3f, y - 34f, x + 14f, y - 30f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRect(x - 16f, y - 10f, x + 16f, y + 14f, paint);
    }

    private void drawMageTower(Canvas canvas, Tower tower) {
        float x = tower.x;
        float y = tower.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getTowerColor(tower));
        canvas.drawRoundRect(x - 10f, y - 20f, x + 10f, y + 14f, 5f, 5f, paint);
        paint.setColor(cAccent2);
        canvas.drawCircle(x, y - 24f, 9f, paint);
        paint.setColor(adjustAlpha(cAccent, 0.9f));
        canvas.drawCircle(x, y - 24f, 4f, paint);
        paint.setColor(cPrimary);
        canvas.drawRect(x - 14f, y - 6f, x + 14f, y - 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(x - 10f, y - 20f, x + 10f, y + 14f, 5f, 5f, paint);
    }

    private int getTowerColor(Tower tower) {
        if (tower.type == com.android.boot.entity.TowerType.ARROW) {
            return tower.branch == BranchType.SHARPSHOT ? cWarning : tower.branch == BranchType.RANGER ? cSuccess : cAccent;
        }
        if (tower.type == com.android.boot.entity.TowerType.ARTILLERY) {
            return tower.branch == BranchType.SIEGE ? cDanger : tower.branch == BranchType.STORM ? cAccent2 : cWarning;
        }
        if (tower.type == com.android.boot.entity.TowerType.BARRACKS) {
            return tower.branch == BranchType.KNIGHT ? cPrimary : tower.branch == BranchType.BERSERKER ? cDanger : cSecondary;
        }
        return tower.branch == BranchType.ARCANE ? cAccent2 : tower.branch == BranchType.HEX ? cSuccess : cPrimary;
    }

    private void drawTowerBadge(Canvas canvas, Tower tower) {
        String badge = tower.branch == BranchType.NONE ? "L" + tower.level : "B" + tower.branchLevel;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawCircle(tower.x + 18f, tower.y - 18f, 12f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawCircle(tower.x + 18f, tower.y - 18f, 12f, paint);
        smallTextPaint.setColor(cPrimary);
        canvas.drawText(badge, tower.x + 10f, tower.y - 14f, smallTextPaint);
        smallTextPaint.setColor(cSecondary);
    }

    private void drawProjectiles(Canvas canvas) {
        for (Projectile projectile : session.projectiles) {
            paint.setStyle(Paint.Style.FILL);
            if (projectile.damageType == com.android.boot.entity.DamageType.MAGIC) {
                paint.setColor(cAccent2);
            } else if (projectile.damageType == com.android.boot.entity.DamageType.EXPLOSIVE) {
                paint.setColor(cWarning);
            } else {
                paint.setColor(cAccent);
            }
            canvas.drawCircle(projectile.x, projectile.y, projectile.radius, paint);
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : session.enemies) {
            enemyPaint.setStyle(Paint.Style.FILL);
            if (enemy.template.boss) {
                enemyPaint.setColor(cDanger);
            } else if (enemy.template.flying) {
                enemyPaint.setColor(cAccent2);
            } else if (enemy.template.armor >= 50f) {
                enemyPaint.setColor(cPrimary);
            } else if (enemy.template.resist >= 50f) {
                enemyPaint.setColor(cAccent);
            } else {
                enemyPaint.setColor(cWarning);
            }
            float radius = enemy.template.boss ? 24f : enemy.template.flying ? 13f : 16f;
            if (enemy.template.boss) {
                drawBossEnemy(canvas, enemy, radius);
            } else if (enemy.template.flying) {
                drawFlyingEnemy(canvas, enemy, radius);
            } else if (enemy.template.armor >= 50f) {
                drawArmoredEnemy(canvas, enemy, radius);
            } else if (enemy.template.resist >= 50f) {
                drawMysticEnemy(canvas, enemy, radius);
            } else {
                drawRunnerEnemy(canvas, enemy, radius);
            }
            tempRect.set(enemy.x - 20f, enemy.y - radius - 14f, enemy.x + 20f, enemy.y - radius - 8f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(hpBackPaint.getColor());
            canvas.drawRoundRect(tempRect, 4f, 4f, paint);
            tempRect.right = tempRect.left + 40f * Math.max(0f, enemy.hp / enemy.template.maxHp);
            paint.setColor(hpFillPaint.getColor());
            canvas.drawRoundRect(tempRect, 4f, 4f, paint);
        }
    }

    private void drawDamageTexts(Canvas canvas) {
        for (DamageText text : session.damageTexts) {
            float alpha = Math.max(0f, Math.min(1f, text.ttl / 0.6f));
            smallTextPaint.setColor(adjustAlpha(cWarning, alpha));
            smallTextPaint.setTextSize(sp(14f));
            canvas.drawText("-" + text.value, text.x - 12f, text.y, smallTextPaint);
        }
        smallTextPaint.setColor(cSecondary);
        smallTextPaint.setTextSize(sp(13f));
    }

    private void drawRunnerEnemy(Canvas canvas, Enemy enemy, float r) {
        float x = enemy.x;
        float y = enemy.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getEnemyBaseColor(enemy));
        canvas.drawRoundRect(x - r, y - r + 2f, x + r, y + r, 6f, 6f, paint);
        paint.setColor(adjustAlpha(cWarning, 0.95f));
        canvas.drawRect(x - 5f, y - r + 6f, x + 5f, y + r - 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(x - r, y - r + 2f, x + r, y + r, 6f, 6f, paint);
    }

    private void drawFlyingEnemy(Canvas canvas, Enemy enemy, float r) {
        float x = enemy.x;
        float y = enemy.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getEnemyBaseColor(enemy));
        canvas.drawCircle(x, y, r - 2f, paint);
        paint.setColor(adjustAlpha(cPrimary, 0.9f));
        canvas.drawOval(x - r - 7f, y - 6f, x - 2f, y + 6f, paint);
        canvas.drawOval(x + 2f, y - 6f, x + r + 7f, y + 6f, paint);
        paint.setColor(adjustAlpha(cAccent2, 0.9f));
        canvas.drawCircle(x, y, 4f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawCircle(x, y, r - 2f, paint);
    }

    private void drawArmoredEnemy(Canvas canvas, Enemy enemy, float r) {
        float x = enemy.x;
        float y = enemy.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getEnemyBaseColor(enemy));
        canvas.drawRect(x - r, y - r + 1f, x + r, y + r + 1f, paint);
        paint.setColor(adjustAlpha(cPrimary, 0.95f));
        canvas.drawRect(x - r + 3f, y - r + 4f, x + r - 3f, y - r + 9f, paint);
        canvas.drawRect(x - 4f, y - r + 1f, x + 4f, y + r - 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRect(x - r, y - r + 1f, x + r, y + r + 1f, paint);
    }

    private void drawMysticEnemy(Canvas canvas, Enemy enemy, float r) {
        float x = enemy.x;
        float y = enemy.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getEnemyBaseColor(enemy));
        canvas.drawCircle(x, y, r - 1f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(adjustAlpha(cAccent2, 0.95f));
        canvas.drawCircle(x, y, r + 5f, paint);
        paint.setColor(adjustAlpha(cAccent, 0.95f));
        canvas.drawCircle(x, y, r - 8f, paint);
        paint.setColor(cStroke);
        canvas.drawCircle(x, y, r - 1f, paint);
    }

    private void drawBossEnemy(Canvas canvas, Enemy enemy, float r) {
        float x = enemy.x;
        float y = enemy.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getEnemyBaseColor(enemy));
        canvas.drawRoundRect(x - r - 6f, y - r + 2f, x + r + 6f, y + r + 4f, 8f, 8f, paint);
        paint.setColor(adjustAlpha(cWarning, 0.95f));
        canvas.drawRect(x - 8f, y - r - 8f, x + 8f, y + r - 4f, paint);
        paint.setColor(adjustAlpha(cDanger, 0.95f));
        canvas.drawRect(x - r - 2f, y - r - 6f, x - r + 4f, y - 6f, paint);
        canvas.drawRect(x + r - 4f, y - r - 6f, x + r + 2f, y - 6f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(x - r - 6f, y - r + 2f, x + r + 6f, y + r + 4f, 8f, 8f, paint);
    }

    private int getEnemyBaseColor(Enemy enemy) {
        if (enemy.template.boss) return cDanger;
        if (enemy.template.flying) return cAccent2;
        if (enemy.template.armor >= 50f) return cPrimary;
        if (enemy.template.resist >= 50f) return cAccent;
        return cWarning;
    }

    private void drawSoldiers(Canvas canvas) {
        for (Soldier soldier : session.soldiers) {
            if (soldier.dead) {
                continue;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getTowerColor(soldier.owner));
            canvas.drawRect(soldier.x - 10f, soldier.y - 10f, soldier.x + 10f, soldier.y + 10f, paint);
            tempRect.set(soldier.x - 12f, soldier.y - 20f, soldier.x + 12f, soldier.y - 16f);
            paint.setColor(hpBackPaint.getColor());
            canvas.drawRoundRect(tempRect, 3f, 3f, paint);
            tempRect.right = tempRect.left + 24f * Math.max(0f, soldier.hp / soldier.maxHp);
            paint.setColor(hpFillPaint.getColor());
            canvas.drawRoundRect(tempRect, 3f, 3f, paint);
        }
    }

    private void drawHero(Canvas canvas) {
        if (session.hero.dead) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cSuccess);
        canvas.drawCircle(session.hero.x, session.hero.y, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(session.hero.selected ? cAccent2 : cPrimary);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(session.hero.x, session.hero.y, 21f, paint);
        tempRect.set(session.hero.x - 24f, session.hero.y - 34f, session.hero.x + 24f, session.hero.y - 28f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(hpBackPaint.getColor());
        canvas.drawRoundRect(tempRect, 4f, 4f, paint);
        tempRect.right = tempRect.left + 48f * Math.max(0f, session.hero.hp / session.hero.maxHp);
        paint.setColor(hpFillPaint.getColor());
        canvas.drawRoundRect(tempRect, 4f, 4f, paint);
    }

    private void drawHeroSlashFx(Canvas canvas) {
        if (session.heroSlashTimer <= 0f) {
            return;
        }
        float t = Math.max(0f, Math.min(1f, session.heroSlashTimer / 0.16f));
        float cx = session.heroSlashX + session.heroSlashDirX * (12f + (1f - t) * 14f);
        float cy = session.heroSlashY + session.heroSlashDirY * (12f + (1f - t) * 14f);
        float angle = (float) Math.toDegrees(Math.atan2(session.heroSlashDirY, session.heroSlashDirX));

        fxPaint.setStyle(Paint.Style.STROKE);
        fxPaint.setStrokeCap(Paint.Cap.ROUND);
        fxPaint.setStrokeWidth(10f);
        fxPaint.setColor(adjustAlpha(cAccent, 0.85f * t));
        tempRect.set(cx - 34f, cy - 34f, cx + 34f, cy + 34f);
        canvas.drawArc(tempRect, angle - 70f, 150f, false, fxPaint);

        fxPaint.setStrokeWidth(4f);
        fxPaint.setColor(adjustAlpha(cPrimary, 0.95f * t));
        tempRect.set(cx - 26f, cy - 26f, cx + 26f, cy + 26f);
        canvas.drawArc(tempRect, angle - 62f, 125f, false, fxPaint);

        fxPaint.setStyle(Paint.Style.FILL);
        fxPaint.setColor(adjustAlpha(cWarning, 0.8f * t));
        canvas.drawCircle(cx + session.heroSlashDirX * 10f, cy + session.heroSlashDirY * 10f, 5f + (1f - t) * 2f, fxPaint);
    }

    private void drawHud(Canvas canvas, float w, float h) {
        drawHudCard(canvas, 18f, 12f, 92f, 52f, "Gold", String.valueOf(session.gold));
        drawHudCard(canvas, 120f, 12f, 92f, 52f, "Life", String.valueOf(session.life));
        drawHudCard(canvas, 222f, 12f, 132f, 52f, "Wave", Math.max(0, session.currentWaveIndex + (session.state == GameState.MENU ? 0 : 1)) + "/" + session.totalWaves());
        RectF earlyRect = session.getEarlyCallRect(w);
        drawButton(canvas, earlyRect, "Call", session.earlyCallAvailable ? cAccent : cMuted, true);
        RectF pauseRect = session.getPauseRect(w);
        drawButton(canvas, pauseRect, session.state == GameState.PAUSED ? "Play" : "Pause", cAccent2, true);
        if (session.currentWaveIndex >= 0 && session.state == GameState.PLAYING) {
            String remaining = "Remain " + session.remainingEnemiesInWave();
            canvas.drawText(remaining, w - 300f, 82f, smallTextPaint);
        }
    }

    private void drawHudCard(Canvas canvas, float left, float top, float width, float height, String label, String value) {
        tempRect.set(left, top, left + width, top + height);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawRoundRect(tempRect, 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(cStroke);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(tempRect, 18f, 18f, paint);
        canvas.drawText(value, left + 12f, top + 26f, hudValuePaint);
        canvas.drawText(label, left + 12f, top + 44f, hudLabelPaint);
    }

    private void drawMenu(Canvas canvas, float w, float h) {
        float pw = 520f;
        float ph = 260f;
        float left = (w - pw) * 0.5f;
        float top = (h - ph) * 0.5f;
        tempRect.set(left, top, left + pw, top + ph);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawRoundRect(tempRect, 26f, 26f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(tempRect, 26f, 26f, paint);
        canvas.drawText("Ember Watch", left + 24f, top + 44f, textPaint);
        smallTextPaint.setColor(cPrimary);
        canvas.drawText("A route defense campaign with four tower lines and branching upgrades.", left + 24f, top + 82f, smallTextPaint);
        smallTextPaint.setColor(cSecondary);
        canvas.drawText(getResources().getString(R.string.tip_controls), left + 24f, top + 116f, smallTextPaint);
        RectF startRect = new RectF(left + 24f, top + ph - 72f, left + 200f, top + ph - 22f);
        RectF helpRect = new RectF(left + 214f, top + ph - 72f, left + 448f, top + ph - 22f);
        drawButton(canvas, startRect, "Start Battle", cAccent, true);
        drawButton(canvas, helpRect, session.showHowToPlay ? "Hide Guide" : "Show Guide", cAccent2, true);
    }

    private void drawPause(Canvas canvas, float w, float h) {
        drawOverlayPanel(canvas, w, h, "Paused", "Tap resume to continue the defense.");
        RectF a = new RectF(w * 0.5f - 160f, h * 0.5f + 10f, w * 0.5f - 10f, h * 0.5f + 60f);
        RectF b = new RectF(w * 0.5f + 10f, h * 0.5f + 10f, w * 0.5f + 160f, h * 0.5f + 60f);
        drawButton(canvas, a, "Resume", cAccent, true);
        drawButton(canvas, b, "Menu", cAccent2, true);
    }

    private void drawGameOver(Canvas canvas, float w, float h) {
        drawOverlayPanel(canvas, w, h, session.victory ? "Victory" : "Defeat", session.victory ? "The ember gate still burns." : "The gate has fallen.");
        RectF a = new RectF(w * 0.5f - 160f, h * 0.5f + 10f, w * 0.5f - 10f, h * 0.5f + 60f);
        RectF b = new RectF(w * 0.5f + 10f, h * 0.5f + 10f, w * 0.5f + 160f, h * 0.5f + 60f);
        drawButton(canvas, a, "Restart", cAccent, true);
        drawButton(canvas, b, "Menu", cAccent2, true);
    }

    private void drawOverlayPanel(Canvas canvas, float w, float h, String title, String subtitle) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(cPrimary, 0.25f));
        canvas.drawRect(0f, 0f, w, h, paint);
        tempRect.set(w * 0.5f - 220f, h * 0.5f - 120f, w * 0.5f + 220f, h * 0.5f + 100f);
        paint.setColor(cPanel);
        canvas.drawRoundRect(tempRect, 24f, 24f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(cStroke);
        paint.setStrokeWidth(3f);
        canvas.drawRoundRect(tempRect, 24f, 24f, paint);
        canvas.drawText(title, tempRect.left + 24f, tempRect.top + 46f, textPaint);
        canvas.drawText(subtitle, tempRect.left + 24f, tempRect.top + 82f, smallTextPaint);
    }

    private void drawSkillBar(Canvas canvas, float w, float h) {
        for (int i = 0; i < session.skills.size(); i++) {
            Skill skill = session.skills.get(i);
            RectF rect = session.getSkillRect(i, w, h);
            int color = skill.ready() ? cAccent : cMuted;
            drawButton(canvas, rect, skill.name, color, true);
            if (!skill.ready()) {
                String cd = String.format(Locale.US, "%.1f", Math.max(0f, skill.timer));
                canvas.drawText(cd, rect.left + 22f, rect.centerY() + 8f, smallTextPaint);
            }
        }
        RectF heroRect = new RectF(190f, h - 84f, 320f, h - 12f);
        drawButton(canvas, heroRect, session.hero.selected ? "Hero Move" : "Hero", session.hero.dead ? cMuted : cSuccess, true);
    }

    private void drawSelectionPanel(Canvas canvas, float w, float h) {
        if (session.selectedSlot == null) {
            return;
        }
        RectF rect = session.getPanelRect(w, h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(cStroke);
        paint.setStrokeWidth(3f);
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        String title = session.selectedTower == null ? "Build Tower" : "Tower Control";
        canvas.drawText(title, rect.left + 18f, rect.top + 34f, textPaint);
        float y = rect.top + 54f;
        for (UiActionButton button : session.panelButtons) {
            button.rect.set(rect.left + 14f, y, rect.right - 14f, y + 42f);
            drawButton(canvas, button.rect, button.text, button.id.equals("sell") ? cDanger : cAccent, button.enabled);
            y += 48f;
        }
    }

    private void drawBanner(Canvas canvas, float w, float h) {
        if (session.bannerTimer <= 0f) {
            return;
        }
        float bw = 260f;
        tempRect.set(w * 0.5f - bw * 0.5f, 76f, w * 0.5f + bw * 0.5f, 116f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(cPanel, 0.92f));
        canvas.drawRoundRect(tempRect, 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(cStroke);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(tempRect, 18f, 18f, paint);
        float tw = textPaint.measureText(session.banner);
        canvas.drawText(session.banner, tempRect.centerX() - tw * 0.5f, 103f, textPaint);
    }

    private void drawTargeting(Canvas canvas, float w, float h) {
        if (session.selectedSkill == null) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(cAccent2, 0.08f));
        canvas.drawRect(0f, 0f, w, h, paint);
        canvas.drawText("Tap a point to cast " + session.selectedSkill.name, 24f, h - 98f, textPaint);
    }

    private void drawButton(Canvas canvas, RectF rect, String text, int fillColor, boolean enabled) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? fillColor : cMuted);
        canvas.drawRoundRect(rect, 16f, 16f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(rect, 16f, 16f, paint);
        textPaint.setColor(cTextOnPrimary);
        float tw = textPaint.measureText(text);
        canvas.drawText(text, rect.centerX() - tw * 0.5f, rect.centerY() + 8f, textPaint);
        textPaint.setColor(cPrimary);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        float scaleX = Math.max(0.0001f, getWidth() / BASE_WIDTH);
        float scaleY = Math.max(0.0001f, getHeight() / BASE_HEIGHT);
        float x = event.getX() / scaleX;
        float y = event.getY() / scaleY;
        float w = BASE_WIDTH;
        float h = BASE_HEIGHT;

        if (session.state == GameState.MENU) {
            handleMenuTouch(x, y, w, h);
            return true;
        }
        if (session.state == GameState.PAUSED) {
            handlePauseTouch(x, y, w, h);
            return true;
        }
        if (session.state == GameState.GAME_OVER) {
            handleGameOverTouch(x, y, w, h);
            return true;
        }
        RectF pauseRect = session.getPauseRect(w);
        if (pauseRect.contains(x, y)) {
            session.togglePause();
            return true;
        }
        RectF earlyRect = session.getEarlyCallRect(w);
        if (earlyRect.contains(x, y)) {
            session.earlyCall();
            return true;
        }
        RectF heroRect = new RectF(190f, h - 84f, 320f, h - 12f);
        if (heroRect.contains(x, y)) {
            session.hero.selected = !session.hero.selected;
            return true;
        }
        if (session.selectSkillAt(x, y, w, h)) {
            return true;
        }
        if (session.waitingForSkillTarget) {
            session.triggerSkill(x, y);
            return true;
        }
        if (session.hero.selected) {
            session.moveHero(x, y);
            session.hero.selected = false;
            return true;
        }
        if (session.selectedSlot != null) {
            for (UiActionButton button : session.panelButtons) {
                if (button.hit(x, y)) {
                    session.executePanelAction(button.id);
                    return true;
                }
            }
        }
        TowerSlot slot = session.findSlot(x, y);
        if (slot != null) {
            session.selectSlot(slot);
            return true;
        }
        session.clearSelection();
        return true;
    }

    private void handleMenuTouch(float x, float y, float w, float h) {
        float pw = 520f;
        float ph = 260f;
        float left = (w - pw) * 0.5f;
        float top = (h - ph) * 0.5f;
        RectF startRect = new RectF(left + 24f, top + ph - 72f, left + 200f, top + ph - 22f);
        RectF helpRect = new RectF(left + 214f, top + ph - 72f, left + 448f, top + ph - 22f);
        if (startRect.contains(x, y)) {
            session.startGame();
        } else if (helpRect.contains(x, y)) {
            session.showHowToPlay = !session.showHowToPlay;
        }
    }

    private void handlePauseTouch(float x, float y, float w, float h) {
        RectF a = new RectF(w * 0.5f - 160f, h * 0.5f + 10f, w * 0.5f - 10f, h * 0.5f + 60f);
        RectF b = new RectF(w * 0.5f + 10f, h * 0.5f + 10f, w * 0.5f + 160f, h * 0.5f + 60f);
        if (a.contains(x, y)) {
            session.togglePause();
        } else if (b.contains(x, y)) {
            returnToMainMenu();
        }
    }

    private void handleGameOverTouch(float x, float y, float w, float h) {
        RectF a = new RectF(w * 0.5f - 160f, h * 0.5f + 10f, w * 0.5f - 10f, h * 0.5f + 60f);
        RectF b = new RectF(w * 0.5f + 10f, h * 0.5f + 10f, w * 0.5f + 160f, h * 0.5f + 60f);
        if (a.contains(x, y)) {
            session.startGame();
        } else if (b.contains(x, y)) {
            returnToMainMenu();
        }
    }

    private void returnToMainMenu() {
        Context context = getContext();
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}
