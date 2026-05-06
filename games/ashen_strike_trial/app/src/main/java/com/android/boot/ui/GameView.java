package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.SoundController;
import com.android.boot.core.GameLoop;
import com.android.boot.core.GameState;
import com.android.boot.core.InputState;
import com.android.boot.core.StageController;
import com.android.boot.entity.AttackHitbox;
import com.android.boot.entity.BossEnemy;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.HeavyEnemy;
import com.android.boot.entity.Player;
import com.android.boot.fx.ImpactEffect;
import com.android.boot.fx.SlashEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, GameLoop.Callback {
    private static class DamageText {
        float x;
        float y;
        int value;
        float life;
        float vy;
    }

    private static class Fireball {
        float x;
        float y;
        float vx;
        float radius;
        float life;
        int damage;
        float knock;
        boolean active;
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmpRect = new RectF();
    private final Path tmpPath = new Path();
    private final RectF buttonLeft = new RectF();
    private final RectF buttonRight = new RectF();
    private final RectF buttonJump = new RectF();
    private final RectF buttonAttack = new RectF();
    private final RectF buttonSkill = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF menuButtonA = new RectF();
    private final RectF menuButtonB = new RectF();
    private final RectF menuButtonC = new RectF();
    private final RectF rewardA = new RectF();
    private final RectF rewardB = new RectF();
    private final RectF rewardC = new RectF();
    private final InputState input = new InputState();
    private final Player player = new Player();
    private final StageController stage = new StageController();
    private final ImpactEffect impact = new ImpactEffect();
    private final SlashEffect slash = new SlashEffect();
    private final AttackHitbox hitbox = new AttackHitbox();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<DamageText> damageTexts = new ArrayList<>();
    private final Fireball fireball = new Fireball();
    private final SoundController soundController = new SoundController();
    private GameLoop loop;
    private GameState state = GameState.MENU;
    private float cameraX;
    private float hitStop;
    private float shakeTime;
    private float shakeAmp;
    private boolean showHowTo;
    private boolean pendingNextLevel;
    private float portalTime;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        soundController.init(context);
    }

    public void onHostResume() {
        soundController.onResume();
        if (getHolder().getSurface().isValid()) {
            startLoop();
        }
    }

    public void onHostPause() {
        soundController.onPause();
        stopLoop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        soundController.onResume();
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        soundController.onPause();
        stopLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        soundController.release();
        super.onDetachedFromWindow();
    }

    private void startLoop() {
        if (loop != null) {
            return;
        }
        loop = new GameLoop(this);
        loop.start();
    }

    private void stopLoop() {
        if (loop == null) {
            return;
        }
        loop.shutdown();
        loop = null;
    }

    @Override
    public void step(float dt) {
        if (state == GameState.PLAYING) {
            updateGame(dt);
        }
        drawFrame();
    }

    private void resetRun() {
        stage.reset();
        startLevel();
    }

    private void startLevel() {
        player.resetRun();
        enemies.clear();
        damageTexts.clear();
        cameraX = 0f;
        hitStop = 0f;
        shakeTime = 0f;
        shakeAmp = 0f;
        pendingNextLevel = false;
        portalTime = 0f;
        impact.life = 0f;
        slash.life = 0f;
        fireball.active = false;
        spawnWave(1);
        state = GameState.PLAYING;
    }

    private void spawnWave(int wave) {
        int lvl = stage.level;
        float baseY = 420f;
        if (wave == 1) {
            float start = stage.gateStart + 110f;
            enemies.add(new Enemy(start + 0f, baseY));
            enemies.add(new Enemy(start + 90f, baseY));
            enemies.add(new Enemy(start + 180f, baseY));
            if (lvl >= 2) {
                enemies.add(new Enemy(start + 270f, baseY));
            }
        } else if (wave == 3) {
            float start = stage.gateEnd + 160f;
            enemies.add(new Enemy(start + 0f, baseY));
            enemies.add(new HeavyEnemy(start + 110f, baseY));
            enemies.add(new Enemy(start + 230f, baseY));
            if (lvl >= 3) {
                enemies.add(new HeavyEnemy(start + 340f, baseY));
            }
        } else if (wave == 5) {
            float bossX = stage.bossStart + 250f;
            enemies.add(new BossEnemy(bossX, baseY));
            if (lvl >= 2) {
                enemies.add(new Enemy(bossX - 260f, baseY));
                enemies.add(new Enemy(bossX - 170f, baseY));
            }
            stage.bossSpawned = true;
        }
    }

    private void updateGame(float dt) {
        // Still animate floating texts during hit stop
        updateDamageTexts(dt);
        if (hitStop > 0f) {
            hitStop -= dt;
            return;
        }
        shakeTime -= dt;
        portalTime += dt;
        updateFireball(dt);
        player.update(dt, input.left, input.right, input.jump);
        if (player.x < 60f) {
            player.x = 60f;
        }
        if (stage.gateLocked) {
            if (player.x > stage.bossStart + 180f) {
                player.x = stage.bossStart + 180f;
            }
            if (stage.wave == 1 || stage.wave == 3) {
                if (player.x > stage.gateStart + 220f) {
                    player.x = stage.gateStart + 220f;
                }
            }
        }
        if (player.x > stage.stageWidth - 100f) {
            player.x = stage.stageWidth - 100f;
        }
        handleCombat();
        int alive = 0;
        for (Enemy enemy : enemies) {
            enemy.update(dt, player);
            if (!enemy.dead) {
                alive++;
            }
        }
        removeDead();
        stage.update(player.x, alive);
        if (stage.wave == 3 && alive == 0 && enemies.isEmpty()) {
            spawnWave(3);
        }
        if (stage.wave == 5 && !stage.bossSpawned) {
            spawnWave(5);
        }
        // Clear condition now requires entering the exit portal
        if (stage.exitActive) {
            float dx = player.x - stage.exitX;
            if (Math.abs(dx) < 46f && Math.abs(player.y - 420f) < 10f) {
                pendingNextLevel = true;
                state = GameState.STAGE_CLEAR;
                soundController.clear();
            }
        }
        if (player.hp <= 0) {
            state = GameState.GAME_OVER;
        }
        impact.update(dt);
        slash.update(dt);
        cameraX = player.x - getWidth() * 0.42f;
        if (cameraX < 0f) {
            cameraX = 0f;
        }
        float maxCam = stage.stageWidth - getWidth();
        if (cameraX > maxCam) {
            cameraX = maxCam;
        }
        input.clearOneShot();
    }

    private void updateDamageTexts(float dt) {
        for (int i = damageTexts.size() - 1; i >= 0; i--) {
            DamageText t = damageTexts.get(i);
            t.life -= dt;
            t.y += t.vy * dt;
            t.vy += 220f * dt; // gravity-like slow down (vy is negative initially)
            if (t.life <= 0f) {
                damageTexts.remove(i);
            }
        }
    }

    private void removeDead() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.dead) {
                it.remove();
            }
        }
    }

    private void handleCombat() {
        if (input.attack && player.attackTimer <= 0f) {
            int step = 1;
            if (player.comboTimer > 0f) {
                step = Math.min(3, player.comboStep + 1);
            }
            player.comboStep = step;
            player.comboTimer = 0.42f;
            player.attackTimer = step == 1 ? 0.20f : (step == 2 ? 0.27f : 0.35f);
            int damage = (step == 1 ? 12 : (step == 2 ? 16 : 25)) + player.atkPowerBonus;
            float range = step == 1 ? 82f : (step == 2 ? 102f : 132f);
            float knock = step == 1 ? 140f : (step == 2 ? 220f : 340f);
            float face = player.facing;
            hitbox.set(player.x, player.y - 48f, player.x + range * face, player.y + 18f, damage, knock * face, 0.08f, true);
            player.x += 22f * face;
            slash.trigger(player.x + 42f * face, player.y - 26f, range);
            soundController.attack();
            applyHitbox();
        }
        if (input.skill && player.skillCd <= 0f && player.attackTimer <= 0f) {
            float face = player.facing;
            player.attackTimer = 0.30f;
            player.skillCd = Math.max(3.8f, 5.2f - player.cdBonus);
            fireball.active = true;
            fireball.x = player.x + 72f * face;
            fireball.y = player.y - 36f;
            fireball.vx = 620f * face;
            fireball.radius = 34f;
            fireball.life = 1.2f;
            fireball.damage = 62 + player.atkPowerBonus;
            fireball.knock = 520f * face;
        }
    }

    private void updateFireball(float dt) {
        if (!fireball.active) {
            return;
        }
        fireball.life -= dt;
        if (fireball.life <= 0f) {
            fireball.active = false;
            return;
        }
        fireball.x += fireball.vx * dt;
        if (fireball.x < -200f || fireball.x > stage.stageWidth + 200f) {
            fireball.active = false;
            return;
        }
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            float dx = enemy.x - fireball.x;
            float dy = (enemy.y - enemy.radius * 0.5f) - fireball.y;
            float hitDist = enemy.radius * 0.92f + fireball.radius;
            if (dx * dx + dy * dy > hitDist * hitDist) {
                continue;
            }
            int before = enemy.hp;
            enemy.takeHit(fireball.damage, fireball.knock);
            int dealt = Math.max(0, before - enemy.hp);
            if (dealt > 0) {
                DamageText t = new DamageText();
                t.x = enemy.x;
                t.y = enemy.y - enemy.radius * 2.2f;
                t.value = dealt;
                t.life = 0.78f;
                t.vy = -190f;
                damageTexts.add(t);
            }
            hitStop = 0.055f;
            shakeTime = 0.14f;
            shakeAmp = enemy instanceof BossEnemy ? 12f : 8f;
            impact.trigger(enemy.x, enemy.y - 26f, enemy instanceof BossEnemy ? 62f : 42f);
            soundController.hit();
            fireball.active = false;
            break;
        }
    }

    private void applyHitbox() {
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            if (RectF.intersects(hitbox.rect, enemy.bounds(tmpRect))) {
                int before = enemy.hp;
                enemy.takeHit(hitbox.damage, hitbox.knock);
                int dealt = Math.max(0, before - enemy.hp);
                if (dealt > 0) {
                    DamageText t = new DamageText();
                    t.x = enemy.x;
                    t.y = enemy.y - enemy.radius * 2.15f;
                    t.value = dealt;
                    t.life = 0.75f;
                    t.vy = -180f;
                    damageTexts.add(t);
                }
                hitStop = 0.045f;
                shakeTime = 0.10f;
                shakeAmp = enemy instanceof BossEnemy ? 10f : 7f;
                impact.trigger(enemy.x, enemy.y - 26f, enemy instanceof BossEnemy ? 54f : 34f);
                soundController.hit();
            }
        }
    }

    private void drawFrame() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            drawGame(canvas);
            if (state == GameState.MENU) {
                drawMenu(canvas);
            } else if (state == GameState.PAUSED) {
                drawPause(canvas);
            } else if (state == GameState.GAME_OVER) {
                drawGameOver(canvas);
            } else if (state == GameState.STAGE_CLEAR) {
                drawClear(canvas);
            } else if (state == GameState.REWARD) {
                drawReward(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawGame(Canvas canvas) {
        int bg = ContextCompat.getColor(getContext(), R.color.cst_bg_main);
        int alt = ContextCompat.getColor(getContext(), R.color.cst_bg_alt);
        int accent = ContextCompat.getColor(getContext(), R.color.cst_accent);
        int txt = ContextCompat.getColor(getContext(), R.color.cst_text_primary);
        drawBackground(canvas, bg, alt);

        float shakeX = 0f;
        float shakeY = 0f;
        if (shakeTime > 0f) {
            float t = shakeTime * 60f;
            float s = (float) Math.sin(t * 2.2f) * 0.6f + (float) Math.sin(t * 4.7f) * 0.4f;
            float c = (float) Math.cos(t * 3.1f) * 0.6f + (float) Math.cos(t * 5.4f) * 0.4f;
            shakeX = s * shakeAmp;
            shakeY = c * shakeAmp * 0.55f;
        }

        float px = player.x - cameraX + shakeX;
        drawPlayer(canvas, px, player.y + shakeY, accent);
        for (Enemy enemy : enemies) {
            float ex = enemy.x - cameraX + shakeX;
            int base = enemy instanceof BossEnemy ? ContextCompat.getColor(getContext(), R.color.cst_danger)
                    : enemy instanceof HeavyEnemy ? ContextCompat.getColor(getContext(), R.color.cst_warning)
                    : ContextCompat.getColor(getContext(), R.color.cst_accent_2);
            drawEnemy(canvas, enemy, ex, enemy.y + shakeY, base);
        }
        if (stage.exitActive) {
            drawExitPortal(canvas, shakeX, shakeY);
        }
        if (fireball.active) {
            drawFireball(canvas, shakeX, shakeY);
        }
        drawDamageTexts(canvas, shakeX, shakeY);
        if (slash.life > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
            canvas.drawArc(slash.x - cameraX - slash.width * 0.35f + shakeX, slash.y - 30f + shakeY, slash.x - cameraX + slash.width * 0.35f + shakeX, slash.y + 30f + shakeY, -45f, 120f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (impact.life > 0f) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(impact.x - cameraX + shakeX, impact.y + shakeY, impact.size * (impact.life / 0.12f), paint);
        }
        drawHud(canvas, txt);
        drawControls(canvas);
    }

    private void drawBackground(Canvas canvas, int bg, int alt) {
        // Sky gradient bands (no shader dependency)
        canvas.drawColor(bg);
        int sky1 = Color.argb(255, 14, 18, 40);
        int sky2 = Color.argb(255, 22, 26, 54);
        int sky3 = alt;
        paint.setColor(sky1);
        canvas.drawRect(0, 0, getWidth(), getHeight() * 0.25f, paint);
        paint.setColor(sky2);
        canvas.drawRect(0, getHeight() * 0.25f, getWidth(), getHeight() * 0.55f, paint);
        paint.setColor(sky3);
        canvas.drawRect(0, getHeight() * 0.55f, getWidth(), getHeight() * 0.70f, paint);

        // Stars (deterministic)
        paint.setColor(Color.argb(160, 255, 255, 255));
        for (int i = 0; i < 42; i++) {
            float sx = (i * 197f + 33f) % getWidth();
            float sy = (i * 73f + 21f) % (getHeight() * 0.38f);
            float r = (i % 7 == 0) ? 2.2f : 1.4f;
            canvas.drawCircle(sx, sy, r, paint);
        }

        // Mountains (parallax)
        float p1 = cameraX * 0.18f;
        float p2 = cameraX * 0.32f;
        paint.setColor(Color.argb(255, 18, 22, 44));
        drawMountainLayer(canvas, p1, getHeight() * 0.62f, 260f, 140f);
        paint.setColor(Color.argb(255, 12, 16, 34));
        drawMountainLayer(canvas, p2, getHeight() * 0.68f, 220f, 120f);

        // Fore pillars silhouettes
        paint.setColor(Color.argb(70, 0, 0, 0));
        for (int i = 0; i < 9; i++) {
            float mx = (i * 320f - cameraX * 0.55f) % (getWidth() + 260f) - 120f;
            canvas.drawRect(mx, 190f, mx + 120f, 420f, paint);
        }

        // Ground
        paint.setColor(Color.argb(255, 24, 28, 42));
        canvas.drawRect(0, 460f, getWidth(), getHeight(), paint);
        paint.setColor(Color.argb(40, 255, 255, 255));
        for (int i = 0; i < 14; i++) {
            float gx = (i * 180f - cameraX * 0.75f) % (getWidth() + 220f) - 80f;
            canvas.drawRect(gx, 470f, gx + 90f, 474f, paint);
        }
    }

    private void drawMountainLayer(Canvas canvas, float parallax, float baseY, float step, float height) {
        tmpPath.reset();
        float startX = -step - (parallax % step);
        tmpPath.moveTo(startX, baseY);
        for (float x = startX; x <= getWidth() + step; x += step) {
            float peak = baseY - height - (float) (Math.sin((x + parallax) * 0.01f) * 24f);
            tmpPath.lineTo(x + step * 0.5f, peak);
            tmpPath.lineTo(x + step, baseY);
        }
        tmpPath.lineTo(getWidth() + step, baseY + 200f);
        tmpPath.lineTo(startX, baseY + 200f);
        tmpPath.close();
        canvas.drawPath(tmpPath, paint);
    }

    private void drawExitPortal(Canvas canvas, float shakeX, float shakeY) {
        float cx = stage.exitX - cameraX + shakeX;
        float cy = 444f + shakeY;
        float t = portalTime;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3.2f);
        float r1 = 34f + 8f * pulse;
        float r2 = 20f + 6f * (1f - pulse);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 0, 0, 0));
        canvas.drawOval(cx - 44f, cy + 6f, cx + 44f, cy + 18f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setColor(Color.argb(190, 90, 255, 235));
        canvas.drawCircle(cx, cy, r1, paint);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.argb(160, 255, 255, 255));
        canvas.drawCircle(cx, cy, r2, paint);
        paint.setStyle(Paint.Style.FILL);

        // Hint text
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(22f);
        paint.setColor(Color.argb(210, 230, 245, 255));
        canvas.drawText("EXIT", cx, cy - 52f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawFireball(Canvas canvas, float shakeX, float shakeY) {
        float fx = fireball.x - cameraX + shakeX;
        float fy = fireball.y + shakeY;
        float pulse = 0.5f + 0.5f * (float) Math.sin(portalTime * 16f);
        float r = fireball.radius + 5f * pulse;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawOval(fx - r * 0.85f, fy + r * 0.45f, fx + r * 0.85f, fy + r * 0.95f, paint);
        paint.setColor(Color.argb(180, 255, 120, 40));
        canvas.drawCircle(fx, fy, r, paint);
        paint.setColor(Color.argb(235, 255, 180, 70));
        canvas.drawCircle(fx, fy, r * 0.68f, paint);
        paint.setColor(Color.argb(255, 255, 245, 170));
        canvas.drawCircle(fx, fy, r * 0.36f, paint);
    }

    private void drawDamageTexts(Canvas canvas, float shakeX, float shakeY) {
        paint.setTextAlign(Paint.Align.CENTER);
        for (DamageText t : damageTexts) {
            float alpha = Math.max(0f, Math.min(1f, t.life / 0.75f));
            int a = (int) (alpha * 255);
            float sx = t.x - cameraX + shakeX;
            float sy = t.y + shakeY;
            paint.setTextSize(28f + (1f - alpha) * 10f);
            paint.setColor(Color.argb(a, 0, 0, 0));
            canvas.drawText(String.valueOf(t.value), sx + 2f, sy + 2f, paint);
            paint.setColor(Color.argb(a, 255, 245, 210));
            canvas.drawText(String.valueOf(t.value), sx, sy, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawEnemy(Canvas canvas, Enemy enemy, float ex, float ey, int baseColor) {
        boolean flash = enemy.hitFlash > 0f;
        int body = flash ? Color.WHITE : baseColor;
        int outline = Color.argb(210, 0, 0, 0);
        int steel = Color.argb(255, 210, 220, 235);

        float r = enemy.radius;
        float bodyW = r * 0.82f;
        float bodyH = r * 1.35f;
        float headR = r * (enemy instanceof BossEnemy ? 0.42f : 0.34f);
        float bob = (float) Math.sin((enemy.x * 0.02f) + (enemy.hitStun * 18f)) * 1.0f;

        // Facing: toward movement (or toward player if idle) - approximate using vx
        float face = enemy.vx < -10f ? -1f : (enemy.vx > 10f ? 1f : 1f);

        canvas.save();
        canvas.translate(ex, ey + bob);

        // HP bar
        float hpRatio = enemy.maxHp <= 0 ? 0f : Math.max(0f, Math.min(1f, enemy.hp / (float) enemy.maxHp));
        float barW = r * (enemy instanceof BossEnemy ? 2.2f : 1.7f);
        float barH = enemy instanceof BossEnemy ? 8f : 6f;
        float barY = -bodyH - 46f;
        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRoundRect(-barW * 0.5f - 2f, barY - 2f, barW * 0.5f + 2f, barY + barH + 2f, 8f, 8f, paint);
        paint.setColor(Color.argb(255, 40, 45, 70));
        canvas.drawRoundRect(-barW * 0.5f, barY, barW * 0.5f, barY + barH, 8f, 8f, paint);
        int fill = enemy instanceof BossEnemy ? Color.argb(255, 255, 90, 90) : Color.argb(255, 120, 235, 140);
        paint.setColor(fill);
        canvas.drawRoundRect(-barW * 0.5f, barY, -barW * 0.5f + barW * hpRatio, barY + barH, 8f, 8f, paint);

        // Shadow
        paint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawOval(-bodyW * 0.95f, 10f, bodyW * 0.95f, 20f, paint);

        // Legs
        paint.setStrokeWidth(enemy instanceof HeavyEnemy ? 7f : 6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(body);
        canvas.drawLine(-bodyW * 0.35f, -8f, -bodyW * 0.55f, 10f, paint);
        canvas.drawLine(bodyW * 0.35f, -8f, bodyW * 0.55f, 10f, paint);

        // Body (torso)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(body);
        tmpRect.set(-bodyW, -bodyH - 10f, bodyW, -10f);
        canvas.drawRoundRect(tmpRect, 10f, 10f, paint);

        // Head
        paint.setColor(body);
        canvas.drawCircle(0f, -bodyH - 18f, headR, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.3f);
        paint.setColor(outline);
        canvas.drawCircle(0f, -bodyH - 18f, headR, paint);
        paint.setStyle(Paint.Style.FILL);

        // Arms + weapon silhouette
        paint.setStrokeWidth(enemy instanceof BossEnemy ? 9f : (enemy instanceof HeavyEnemy ? 8f : 6f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(body);
        float armY = -bodyH + 4f;
        float armX = bodyW * 0.92f * face;
        canvas.drawLine(armX, armY, armX + 12f * face, armY + 16f, paint);

        // Weapon (simple blade/club)
        paint.setStrokeWidth(enemy instanceof BossEnemy ? 7f : 5f);
        paint.setColor(flash ? Color.WHITE : steel);
        if (enemy instanceof HeavyEnemy || enemy instanceof BossEnemy) {
            canvas.drawLine(armX + 10f * face, armY + 12f, armX + 32f * face, armY + 32f, paint);
        } else {
            canvas.drawLine(armX + 10f * face, armY + 10f, armX + 26f * face, armY + 20f, paint);
        }

        // Boss extra silhouette: shoulders + horn
        if (enemy instanceof BossEnemy) {
            paint.setColor(body);
            tmpRect.set(-bodyW * 1.25f, -bodyH - 20f, bodyW * 1.25f, -bodyH + 4f);
            canvas.drawRoundRect(tmpRect, 14f, 14f, paint);
            paint.setColor(flash ? Color.WHITE : Color.argb(255, 250, 220, 120));
            tmpPath.reset();
            tmpPath.moveTo(headR * 0.2f, -bodyH - 36f);
            tmpPath.lineTo(headR * 1.2f, -bodyH - 28f);
            tmpPath.lineTo(headR * 0.3f, -bodyH - 22f);
            tmpPath.close();
            canvas.drawPath(tmpPath, paint);
        }

        canvas.restore();
    }

    private void drawPlayer(Canvas canvas, float px, float py, int accent) {
        int outline = Color.argb(220, 0, 0, 0);
        int skin = Color.argb(255, 235, 220, 200);
        int cloak = Color.argb(255, 32, 35, 54);

        float face = player.facing;
        float speed = Math.min(1f, Math.abs(player.vx) / 250f);
        float bob = (float) Math.sin(player.runTime * 6.283f) * (2.2f * speed);
        float lean = Math.max(-1f, Math.min(1f, player.vx / 280f)) * 7.0f;
        float atk = Math.max(0f, Math.min(1f, player.attackTimer / 0.35f));
        float swing = (float) Math.sin((1f - atk) * Math.PI) * 1.0f;
        float squash = player.landSquash > 0f ? (player.landSquash / 0.10f) : 0f;

        float bodyH = 44f - 6f * squash;
        float bodyW = 22f + 5f * squash;
        float headR = 10f - 1.5f * squash;

        canvas.save();
        canvas.translate(px, py + bob);

        // Shadow
        paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(-18f, 8f, 18f, 18f, paint);

        // Legs (simple stride)
        float stride = (float) Math.sin(player.runTime * 6.283f) * 10f * speed;
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(cloak);
        canvas.drawLine(-6f, -6f, -10f - stride * 0.2f, 8f, paint);
        canvas.drawLine(6f, -6f, 10f + stride * 0.2f, 8f, paint);

        // Body
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        tmpRect.set(-bodyW, -bodyH - 10f, bodyW, -10f);
        canvas.drawRoundRect(tmpRect, 10f, 10f, paint);

        // Cloak flap (gives silhouette)
        paint.setColor(cloak);
        tmpPath.reset();
        tmpPath.moveTo(-bodyW, -bodyH - 8f);
        tmpPath.lineTo(bodyW, -bodyH - 8f);
        tmpPath.lineTo(bodyW * 0.65f, -10f);
        tmpPath.lineTo(-bodyW * 0.75f, -10f);
        tmpPath.close();
        canvas.drawPath(tmpPath, paint);

        // Head
        paint.setColor(skin);
        canvas.drawCircle(0f, -bodyH - 18f, headR, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(outline);
        canvas.drawCircle(0f, -bodyH - 18f, headR, paint);
        paint.setStyle(Paint.Style.FILL);

        // Arms: idle swing + attack swing (front arm)
        paint.setStrokeWidth(6f);
        paint.setColor(accent);
        float armY = -bodyH + 2f;
        float backArmX = -bodyW * 0.8f;
        float backArmSwing = -stride * 0.25f;
        canvas.drawLine(backArmX, armY, backArmX + backArmSwing, armY + 14f, paint);

        float frontArmX = bodyW * 0.82f * face;
        float frontArmY = armY - 2f;
        float swordLen = 26f;
        float swordAngle = (-30f + 110f * swing) * face;
        float rad = (float) (swordAngle * Math.PI / 180.0);
        float ax = (float) Math.cos(rad) * swordLen;
        float ay = (float) Math.sin(rad) * swordLen;
        canvas.drawLine(frontArmX, frontArmY, frontArmX + ax, frontArmY + ay, paint);

        // Sword trail hint (extra readability)
        if (player.attackTimer > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawArc(-42f, -bodyH - 30f, 42f, -bodyH + 54f, face > 0 ? -40f : 100f, 60f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Invul blink overlay
        if (player.invul > 0f) {
            paint.setColor(Color.argb(110, 255, 215, 64));
            canvas.drawRoundRect(-bodyW - 6f, -bodyH - 30f, bodyW + 6f, 6f, 10f, 10f, paint);
        }

        // Lean (subtle): just a small horizontal nudge
        canvas.translate(lean, 0f);
        canvas.restore();
    }

    private void drawHud(Canvas canvas, int txt) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        canvas.drawRect(0, 0, getWidth(), 60f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_track));
        canvas.drawRect(16f, 16f, 236f, 32f, paint);
        float hpRatio = Math.max(0f, player.hp / (float) (player.maxHp + player.hpBonus));
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_meter_fill));
        canvas.drawRect(16f, 16f, 16f + 220f * hpRatio, 32f, paint);
        paint.setColor(txt);
        paint.setTextSize(20f);
        canvas.drawText("HP " + player.hp, 16f, 52f, paint);
        String skillTxt = player.skillCd <= 0f ? "Skill Ready" : "Skill " + (int) (player.skillCd + 1f);
        canvas.drawText(skillTxt, 260f, 32f, paint);
        canvas.drawText("L" + stage.level + " W" + Math.min(stage.wave + 1, 6), 460f, 32f, paint);
        canvas.drawText("Boons " + player.atkPowerBonus + "/" + player.hpBonus + "/" + (int) (player.cdBonus * 10f), 620f, 32f, paint);
        pauseButton.set(getWidth() - 90f, 12f, getWidth() - 18f, 52f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(pauseButton, 8f, 8f, paint);
        paint.setColor(txt);
        canvas.drawText("II", getWidth() - 62f, 40f, paint);
    }

    private void drawControls(Canvas canvas) {
        float h = getHeight();
        buttonLeft.set(20f, h - 130f, 120f, h - 30f);
        buttonRight.set(130f, h - 130f, 230f, h - 30f);
        buttonJump.set(getWidth() - 360f, h - 130f, getWidth() - 260f, h - 30f);
        buttonAttack.set(getWidth() - 240f, h - 130f, getWidth() - 140f, h - 30f);
        buttonSkill.set(getWidth() - 120f, h - 130f, getWidth() - 20f, h - 30f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_chip_bg));
        canvas.drawRoundRect(buttonLeft, 12f, 12f, paint);
        canvas.drawRoundRect(buttonRight, 12f, 12f, paint);
        canvas.drawRoundRect(buttonJump, 12f, 12f, paint);
        canvas.drawRoundRect(buttonAttack, 12f, 12f, paint);
        canvas.drawRoundRect(buttonSkill, 12f, 12f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(24f);
        canvas.drawText("L", 58f, getHeight() - 72f, paint);
        canvas.drawText("R", 168f, getHeight() - 72f, paint);
        canvas.drawText("J", getWidth() - 325f, getHeight() - 72f, paint);
        canvas.drawText("A", getWidth() - 205f, getHeight() - 72f, paint);
        canvas.drawText("S", getWidth() - 85f, getHeight() - 72f, paint);
    }

    private void drawMenu(Canvas canvas) {
        paint.setColor(Color.argb(210, 8, 10, 18));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        tmpRect.set(getWidth() * 0.2f, getHeight() * 0.16f, getWidth() * 0.8f, getHeight() * 0.84f);
        canvas.drawRoundRect(tmpRect, 18f, 18f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(46f);
        canvas.drawText(getContext().getString(R.string.app_name), tmpRect.left + 40f, tmpRect.top + 80f, paint);
        paint.setTextSize(24f);
        canvas.drawText("Side-scroll battle run with wave locks and boss finish", tmpRect.left + 40f, tmpRect.top + 124f, paint);
        menuButtonA.set(tmpRect.left + 40f, tmpRect.top + 170f, tmpRect.right - 40f, tmpRect.top + 240f);
        menuButtonB.set(tmpRect.left + 40f, tmpRect.top + 260f, tmpRect.right - 40f, tmpRect.top + 330f);
        menuButtonC.set(tmpRect.left + 40f, tmpRect.top + 350f, tmpRect.right - 40f, tmpRect.top + 420f);
        drawActionButton(canvas, menuButtonA, getContext().getString(R.string.btn_start), true);
        drawActionButton(canvas, menuButtonB, getContext().getString(R.string.btn_how_to_play), false);
        drawActionButton(canvas, menuButtonC, getContext().getString(R.string.btn_mute) + ": " + (soundController.isMuted() ? "Off" : "On"), false);
        if (showHowTo) {
            paint.setColor(Color.argb(230, 17, 21, 41));
            tmpRect.set(tmpRect.left + 30f, tmpRect.top + 430f, tmpRect.right - 30f, tmpRect.bottom - 20f);
            canvas.drawRoundRect(tmpRect, 14f, 14f, paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
            paint.setTextSize(22f);
            canvas.drawText("Move with L R, jump with J, combo with A", tmpRect.left + 20f, tmpRect.top + 42f, paint);
            canvas.drawText("Press A in rhythm for 3-hit chain", tmpRect.left + 20f, tmpRect.top + 74f, paint);
            canvas.drawText("Use S to launch a huge fireball for high damage", tmpRect.left + 20f, tmpRect.top + 106f, paint);
        }
    }

    private void drawPause(Canvas canvas) {
        paint.setColor(Color.argb(220, 8, 10, 18));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        tmpRect.set(getWidth() * 0.33f, 150f, getWidth() * 0.67f, 430f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        canvas.drawRoundRect(tmpRect, 16f, 16f, paint);
        menuButtonA.set(tmpRect.left + 30f, tmpRect.top + 70f, tmpRect.right - 30f, tmpRect.top + 130f);
        menuButtonB.set(tmpRect.left + 30f, tmpRect.top + 145f, tmpRect.right - 30f, tmpRect.top + 205f);
        menuButtonC.set(tmpRect.left + 30f, tmpRect.top + 220f, tmpRect.right - 30f, tmpRect.top + 280f);
        drawActionButton(canvas, menuButtonA, getContext().getString(R.string.btn_resume), true);
        drawActionButton(canvas, menuButtonB, getContext().getString(R.string.btn_restart), false);
        drawActionButton(canvas, menuButtonC, getContext().getString(R.string.btn_menu), false);
    }

    private void drawGameOver(Canvas canvas) {
        drawResultOverlay(canvas, "Game Over", "Try timing combo chains and save skill for boss openings");
    }

    private void drawClear(Canvas canvas) {
        drawResultOverlay(canvas, "Stage Cleared", "Enter the next stage");
    }

    private void drawReward(Canvas canvas) {
        paint.setColor(Color.argb(235, 8, 10, 18));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        tmpRect.set(getWidth() * 0.2f, 120f, getWidth() * 0.8f, 500f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        canvas.drawRoundRect(tmpRect, 18f, 18f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(40f);
        canvas.drawText("Select One Reward", tmpRect.left + 34f, tmpRect.top + 62f, paint);
        rewardA.set(tmpRect.left + 24f, tmpRect.top + 100f, tmpRect.right - 24f, tmpRect.top + 170f);
        rewardB.set(tmpRect.left + 24f, tmpRect.top + 188f, tmpRect.right - 24f, tmpRect.top + 258f);
        rewardC.set(tmpRect.left + 24f, tmpRect.top + 276f, tmpRect.right - 24f, tmpRect.top + 346f);
        drawActionButton(canvas, rewardA, "Increase Max HP", true);
        drawActionButton(canvas, rewardB, "Increase Attack", false);
        drawActionButton(canvas, rewardC, "Reduce Skill Cooldown", false);
    }

    private void drawResultOverlay(Canvas canvas, String title, String body) {
        paint.setColor(Color.argb(230, 8, 10, 18));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        tmpRect.set(getWidth() * 0.24f, 160f, getWidth() * 0.76f, 450f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        canvas.drawRoundRect(tmpRect, 16f, 16f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(42f);
        canvas.drawText(title, tmpRect.left + 30f, tmpRect.top + 70f, paint);
        paint.setTextSize(22f);
        canvas.drawText(body, tmpRect.left + 30f, tmpRect.top + 110f, paint);
        menuButtonA.set(tmpRect.left + 30f, tmpRect.top + 150f, tmpRect.right - 30f, tmpRect.top + 210f);
        menuButtonB.set(tmpRect.left + 30f, tmpRect.top + 220f, tmpRect.right - 30f, tmpRect.top + 280f);
        String aText = state == GameState.STAGE_CLEAR ? "Next Stage" : getContext().getString(R.string.btn_restart);
        drawActionButton(canvas, menuButtonA, aText, true);
        drawActionButton(canvas, menuButtonB, getContext().getString(R.string.btn_menu), false);
    }

    private void drawActionButton(Canvas canvas, RectF rect, String text, boolean primary) {
        paint.setColor(primary ? ContextCompat.getColor(getContext(), R.color.cst_btn_primary_bg_start)
                : ContextCompat.getColor(getContext(), R.color.cst_btn_secondary_bg_start));
        canvas.drawRoundRect(rect, 12f, 12f, paint);
        paint.setColor(primary ? ContextCompat.getColor(getContext(), R.color.cst_text_on_primary)
                : ContextCompat.getColor(getContext(), R.color.cst_text_on_secondary));
        paint.setTextSize(24f);
        canvas.drawText(text, rect.left + 24f, rect.centerY() + 8f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int act = event.getActionMasked();
        if (state == GameState.MENU) {
            if (act == MotionEvent.ACTION_DOWN) {
                if (menuButtonA.contains(x, y)) {
                    resetRun();
                } else if (menuButtonB.contains(x, y)) {
                    showHowTo = !showHowTo;
                } else if (menuButtonC.contains(x, y)) {
                    soundController.setMuted(!soundController.isMuted());
                }
            }
            return true;
        }
        if (state == GameState.PAUSED) {
            if (act == MotionEvent.ACTION_DOWN) {
                if (menuButtonA.contains(x, y)) {
                    state = GameState.PLAYING;
                } else if (menuButtonB.contains(x, y)) {
                    resetRun();
                } else if (menuButtonC.contains(x, y)) {
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.GAME_OVER || state == GameState.STAGE_CLEAR) {
            if (act == MotionEvent.ACTION_DOWN) {
                if (menuButtonA.contains(x, y)) {
                    if (state == GameState.STAGE_CLEAR) {
                        stage.nextLevel();
                        startLevel();
                    } else {
                        resetRun();
                    }
                } else if (menuButtonB.contains(x, y)) {
                    state = GameState.MENU;
                }
            }
            return true;
        }
        if (state == GameState.REWARD) {
            if (act == MotionEvent.ACTION_DOWN) {
                if (rewardA.contains(x, y)) {
                    player.applyReward(0);
                    stage.nextLevel();
                    startLevel();
                } else if (rewardB.contains(x, y)) {
                    player.applyReward(1);
                    stage.nextLevel();
                    startLevel();
                } else if (rewardC.contains(x, y)) {
                    player.applyReward(2);
                    stage.nextLevel();
                    startLevel();
                }
            }
            return true;
        }
        if (state == GameState.PLAYING) {
            if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN || act == MotionEvent.ACTION_MOVE) {
                if (pauseButton.contains(x, y)) {
                    state = GameState.PAUSED;
                    return true;
                }
                input.left = buttonLeft.contains(x, y);
                input.right = buttonRight.contains(x, y);
                if (buttonJump.contains(x, y)) {
                    input.jump = true;
                }
                if (buttonAttack.contains(x, y)) {
                    input.attack = true;
                }
                if (buttonSkill.contains(x, y)) {
                    input.skill = true;
                }
            } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                input.left = false;
                input.right = false;
            }
        }
        return true;
    }
}
