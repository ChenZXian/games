package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmpRect = new RectF();
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
    private final SoundController soundController = new SoundController();
    private GameLoop loop;
    private GameState state = GameState.MENU;
    private float stageWidth = 2100f;
    private float cameraX;
    private float hitStop;
    private boolean showHowTo;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void onHostResume() {
        if (getHolder().getSurface().isValid()) {
            startLoop();
        }
    }

    public void onHostPause() {
        stopLoop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
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
        player.resetRun();
        stage.reset();
        enemies.clear();
        cameraX = 0f;
        hitStop = 0f;
        impact.life = 0f;
        slash.life = 0f;
        spawnWave(1);
        state = GameState.PLAYING;
    }

    private void spawnWave(int wave) {
        if (wave == 1) {
            enemies.add(new Enemy(660f, 420f));
            enemies.add(new Enemy(760f, 420f));
            enemies.add(new Enemy(850f, 420f));
        } else if (wave == 3) {
            enemies.add(new Enemy(1060f, 420f));
            enemies.add(new HeavyEnemy(1160f, 420f));
            enemies.add(new Enemy(1260f, 420f));
        } else if (wave == 5) {
            enemies.add(new BossEnemy(1700f, 420f));
            stage.bossSpawned = true;
        }
    }

    private void updateGame(float dt) {
        if (hitStop > 0f) {
            hitStop -= dt;
            return;
        }
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
        if (player.x > stageWidth - 100f) {
            player.x = stageWidth - 100f;
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
        if (stage.wave == 6) {
            state = GameState.STAGE_CLEAR;
            soundController.clear();
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
        float maxCam = stageWidth - getWidth();
        if (cameraX > maxCam) {
            cameraX = maxCam;
        }
        input.clearOneShot();
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
            float face = input.left ? -1f : 1f;
            if (input.left && !input.right) {
                face = -1f;
            }
            if (input.right && !input.left) {
                face = 1f;
            }
            hitbox.set(player.x, player.y - 48f, player.x + range * face, player.y + 18f, damage, knock * face, 0.08f, true);
            player.x += 22f * face;
            slash.trigger(player.x + 42f * face, player.y - 26f, range);
            soundController.attack();
            applyHitbox();
        }
        if (input.skill && player.skillCd <= 0f && player.attackTimer <= 0f) {
            float face = input.left ? -1f : 1f;
            if (input.right && !input.left) {
                face = 1f;
            }
            if (input.left && !input.right) {
                face = -1f;
            }
            player.attackTimer = 0.34f;
            player.skillCd = Math.max(3.4f, 4.6f - player.cdBonus);
            player.x += 74f * face;
            hitbox.set(player.x, player.y - 60f, player.x + 180f * face, player.y + 26f, 34 + player.atkPowerBonus, 420f * face, 0.1f, true);
            slash.trigger(player.x + 88f * face, player.y - 30f, 190f);
            applyHitbox();
        }
    }

    private void applyHitbox() {
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            if (RectF.intersects(hitbox.rect, enemy.bounds(tmpRect))) {
                enemy.takeHit(hitbox.damage, hitbox.knock);
                hitStop = 0.045f;
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
        canvas.drawColor(bg);
        paint.setColor(alt);
        canvas.drawRect(0, 0, getWidth(), getHeight() * 0.65f, paint);
        paint.setColor(Color.argb(70, 0, 0, 0));
        for (int i = 0; i < 8; i++) {
            float mx = (i * 340f - cameraX * 0.4f) % (getWidth() + 260f) - 120f;
            canvas.drawRect(mx, 190f, mx + 120f, 420f, paint);
        }
        paint.setColor(Color.argb(255, 24, 28, 42));
        canvas.drawRect(0, 460f, getWidth(), getHeight(), paint);
        float px = player.x - cameraX;
        paint.setColor(player.invul > 0 ? ContextCompat.getColor(getContext(), R.color.cst_warning) : accent);
        canvas.drawRoundRect(px - 24f, player.y - 66f, px + 24f, player.y, 8f, 8f, paint);
        for (Enemy enemy : enemies) {
            float ex = enemy.x - cameraX;
            paint.setColor(enemy instanceof BossEnemy ? ContextCompat.getColor(getContext(), R.color.cst_danger)
                    : enemy instanceof HeavyEnemy ? ContextCompat.getColor(getContext(), R.color.cst_warning)
                    : ContextCompat.getColor(getContext(), R.color.cst_accent_2));
            canvas.drawRoundRect(ex - enemy.radius, enemy.y - enemy.radius * 2f, ex + enemy.radius, enemy.y, 8f, 8f, paint);
        }
        if (slash.life > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
            canvas.drawArc(slash.x - cameraX - slash.width * 0.35f, slash.y - 30f, slash.x - cameraX + slash.width * 0.35f, slash.y + 30f, -45f, 120f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (impact.life > 0f) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_warning));
            canvas.drawCircle(impact.x - cameraX, impact.y, impact.size * (impact.life / 0.12f), paint);
        }
        drawHud(canvas, txt);
        drawControls(canvas);
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
        canvas.drawText("Wave " + Math.min(stage.wave + 1, 6), 460f, 32f, paint);
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
            canvas.drawText("Use S for burst slash and save it for boss gaps", tmpRect.left + 20f, tmpRect.top + 106f, paint);
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
        drawResultOverlay(canvas, "Stage Clear", "Choose one permanent boon for the next run");
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
        String aText = state == GameState.STAGE_CLEAR ? "Rewards" : getContext().getString(R.string.btn_restart);
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
                        state = GameState.REWARD;
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
                    resetRun();
                } else if (rewardB.contains(x, y)) {
                    player.applyReward(1);
                    resetRun();
                } else if (rewardC.contains(x, y)) {
                    player.applyReward(2);
                    resetRun();
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
