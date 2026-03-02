package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.core.LevelManager;
import com.android.boot.core.WaveScript;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Hero;
import com.android.boot.entity.Particle;
import com.android.boot.entity.Projectile;
import com.android.boot.entity.Tower;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    public interface Listener {
        void onHud(int lives, int gold, int wave, int waveTotal, int mana);
        void onGameOver(boolean victory);
    }

    private Thread thread;
    private boolean running;
    private boolean paused;
    private final Paint paint = new Paint();
    private final Paint text = new Paint();
    private final LevelManager levelManager = new LevelManager();
    private LevelManager.LevelData level;
    private final List<Tower> towers = new ArrayList<>();
    private final Enemy[] enemies = new Enemy[200];
    private final Projectile[] projectiles = new Projectile[300];
    private final Particle[] particles = new Particle[300];
    private final Hero hero = new Hero();
    private Listener listener;
    private int levelIndex;
    private int lives;
    private int gold;
    private int mana;
    private int wave;
    private int entryIndex;
    private int spawnedInEntry;
    private float spawnTimer;
    private boolean victory;
    private float freezeTimer;
    private float meteorCd;
    private float freezeCd;
    private float reinforceCd;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        text.setColor(Color.BLACK);
        text.setTextSize(30f);
        for (int i = 0; i < enemies.length; i++) enemies[i] = new Enemy();
        for (int i = 0; i < projectiles.length; i++) projectiles[i] = new Projectile();
        for (int i = 0; i < particles.length; i++) particles[i] = new Particle();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void startLevel(int idx) {
        levelIndex = idx;
        level = levelManager.get(idx);
        towers.clear();
        towers.add(new Tower(Tower.Type.ARROW, 220, 220));
        towers.add(new Tower(Tower.Type.CANNON, 360, 280));
        towers.add(new Tower(Tower.Type.MAGE, 490, 210));
        towers.add(new Tower(Tower.Type.BARRACKS, 610, 290));
        towers.add(new Tower(Tower.Type.SUPPORT_TOTEM, 120, 250));
        towers.get(0).upgradeBase();
        towers.get(1).upgradeBase(); towers.get(1).upgradeBase(); towers.get(1).chooseBranch(1);
        towers.get(2).chooseBranch(2);
        for (Enemy e : enemies) e.active = false;
        for (Projectile p : projectiles) p.active = false;
        for (Particle p : particles) p.active = false;
        hero.setPosition(140, 360);
        lives = 20;
        gold = 250;
        mana = 100;
        wave = 0;
        entryIndex = 0;
        spawnedInEntry = 0;
        spawnTimer = 0f;
        meteorCd = 0f;
        freezeCd = 0f;
        reinforceCd = 0f;
        freezeTimer = 0f;
        victory = false;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void castMeteor() {
        if (mana >= 35 && meteorCd <= 0f) {
            mana -= 35;
            meteorCd = 10f;
            for (Enemy e : enemies) {
                if (e.active) {
                    e.hp -= 55f;
                    spawnParticle(e.x, e.y, 0f, -30f, 0.5f, 8f);
                }
            }
        }
    }

    public void castFreeze() {
        if (mana >= 25 && freezeCd <= 0f) {
            mana -= 25;
            freezeCd = 14f;
            freezeTimer = 2.2f;
        }
    }

    public void castReinforce() {
        if (mana >= 20 && reinforceCd <= 0f) {
            mana -= 20;
            reinforceCd = 11f;
            for (int i = 0; i < 8; i++) {
                Projectile p = obtainProjectile();
                if (p != null) {
                    p.active = true;
                    p.x = hero.x;
                    p.y = hero.y;
                    p.vx = 140f + i * 15f;
                    p.vy = -40f + i * 12f;
                    p.damage = 16f;
                    p.ttl = 1.6f;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hero.moveTo(event.getX(), event.getY());
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            if (!getHolder().getSurface().isValid()) continue;
            long now = System.nanoTime();
            float dt = (now - last) * 0.000000001f;
            last = now;
            if (dt > 0.033f) dt = 0.033f;
            if (!paused) update(dt);
            float alpha = Math.min(1f, dt / 0.016f);
            Canvas c = getHolder().lockCanvas();
            render(c, alpha);
            getHolder().unlockCanvasAndPost(c);
        }
    }

    private void update(float dt) {
        mana = Math.min(100, mana + (int) (8f * dt));
        meteorCd -= dt;
        freezeCd -= dt;
        reinforceCd -= dt;
        if (freezeTimer > 0f) freezeTimer -= dt;
        spawnEnemies(dt);
        hero.update(dt);
        updateEnemies(dt);
        updateTowers(dt);
        updateProjectiles(dt);
        updateParticles(dt);
        checkWinLose();
        if (listener != null) listener.onHud(lives, gold, wave + 1, level.waves.size(), mana);
    }

    private void spawnEnemies(float dt) {
        if (wave >= level.waves.size()) return;
        WaveScript ws = level.waves.get(wave);
        if (entryIndex >= ws.entries.size()) {
            if (countEnemies() == 0) {
                wave++;
                entryIndex = 0;
                spawnedInEntry = 0;
            }
            return;
        }
        WaveScript.Entry entry = ws.entries.get(entryIndex);
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            Enemy e = obtainEnemy();
            if (e != null) {
                e.spawn(entry.type, level.path, levelIndex * 18f + wave * 12f);
                spawnedInEntry++;
            }
            spawnTimer = entry.interval;
        }
        if (spawnedInEntry >= entry.count) {
            entryIndex++;
            spawnedInEntry = 0;
            spawnTimer = 0.6f;
        }
    }

    private void updateEnemies(float dt) {
        for (Enemy e : enemies) {
            if (!e.active) continue;
            e.prevX = e.x;
            e.prevY = e.y;
            if (freezeTimer <= 0f) {
                moveEnemy(e, dt);
            }
            if (e.type == Enemy.Type.HEALER) {
                e.healTimer -= dt;
                if (e.healTimer <= 0f) {
                    e.healTimer = 1.5f;
                    for (Enemy other : enemies) {
                        if (other.active) {
                            float dx = other.x - e.x;
                            float dy = other.y - e.y;
                            if (dx * dx + dy * dy < 10000f) {
                                other.hp = Math.min(other.maxHp, other.hp + 8f);
                            }
                        }
                    }
                }
            }
            if (e.type == Enemy.Type.BOSS && e.hp < e.maxHp * 0.6f && e.phase == 0) {
                e.phase = 1;
                e.speed *= 1.3f;
            }
            if (e.hp <= 0f) {
                e.active = false;
                gold += 8;
                spawnParticle(e.x, e.y, 0f, -22f, 0.4f, 9f);
            }
        }
    }

    private void moveEnemy(Enemy e, float dt) {
        if (e.pathIndex >= level.path.size() - 1) {
            e.active = false;
            lives--;
            return;
        }
        float[] next = level.path.get(e.pathIndex + 1);
        float dx = next[0] - e.x;
        float dy = next[1] - e.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = e.speed * dt;
        if (step >= dist) {
            e.x = next[0];
            e.y = next[1];
            e.pathIndex++;
        } else {
            e.x += dx / dist * step;
            e.y += dy / dist * step;
        }
    }

    private void updateTowers(float dt) {
        for (Tower t : towers) {
            t.cooldown -= dt;
            if (t.cooldown > 0f) continue;
            Enemy target = nearestEnemy(t.x, t.y, t.range());
            if (target != null) {
                Projectile p = obtainProjectile();
                if (p != null) {
                    float dx = target.x - t.x;
                    float dy = target.y - t.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    p.active = true;
                    p.x = t.x;
                    p.y = t.y;
                    p.vx = dx / d * 250f;
                    p.vy = dy / d * 250f;
                    p.damage = t.damage();
                    p.ttl = 1.3f;
                }
                t.cooldown = t.fireRate();
            }
        }
    }

    private void updateProjectiles(float dt) {
        for (Projectile p : projectiles) {
            if (!p.active) continue;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.ttl -= dt;
            Enemy hit = nearestEnemy(p.x, p.y, 18f);
            if (hit != null) {
                hit.hp -= p.damage;
                p.active = false;
                spawnParticle(p.x, p.y, 0f, 0f, 0.2f, 4f);
            }
            if (p.ttl <= 0f) p.active = false;
        }
    }

    private void updateParticles(float dt) {
        for (Particle p : particles) {
            if (!p.active) continue;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.ttl -= dt;
            if (p.ttl <= 0f) p.active = false;
        }
    }

    private void checkWinLose() {
        if (lives <= 0) {
            lives = 0;
            if (listener != null) listener.onGameOver(false);
            paused = true;
            return;
        }
        if (wave >= level.waves.size() && countEnemies() == 0 && !victory) {
            victory = true;
            if (listener != null) listener.onGameOver(true);
            paused = true;
        }
    }

    private int countEnemies() {
        int c = 0;
        for (Enemy e : enemies) if (e.active) c++;
        return c;
    }

    private Enemy nearestEnemy(float x, float y, float range) {
        Enemy best = null;
        float bestD = range * range;
        for (Enemy e : enemies) {
            if (!e.active) continue;
            float dx = e.x - x;
            float dy = e.y - y;
            float d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private Enemy obtainEnemy() {
        for (Enemy e : enemies) if (!e.active) return e;
        return null;
    }

    private Projectile obtainProjectile() {
        for (Projectile p : projectiles) if (!p.active) return p;
        return null;
    }

    private void spawnParticle(float x, float y, float vx, float vy, float ttl, float size) {
        for (Particle p : particles) {
            if (!p.active) {
                p.active = true;
                p.x = x;
                p.y = y;
                p.vx = vx;
                p.vy = vy;
                p.ttl = ttl;
                p.size = size;
                return;
            }
        }
    }

    private void render(Canvas c, float alpha) {
        c.drawColor(Color.rgb(202, 235, 255));
        paint.setColor(Color.rgb(186, 225, 140));
        for (int i = 0; i < level.path.size() - 1; i++) {
            float[] a = level.path.get(i);
            float[] b = level.path.get(i + 1);
            paint.setStrokeWidth(36f);
            c.drawLine(a[0], a[1], b[0], b[1], paint);
        }
        for (Tower t : towers) {
            if (t.type == Tower.Type.ARROW) paint.setColor(Color.rgb(70, 120, 220));
            if (t.type == Tower.Type.CANNON) paint.setColor(Color.rgb(130, 130, 130));
            if (t.type == Tower.Type.MAGE) paint.setColor(Color.rgb(130, 70, 220));
            if (t.type == Tower.Type.BARRACKS) paint.setColor(Color.rgb(160, 110, 70));
            if (t.type == Tower.Type.SUPPORT_TOTEM) paint.setColor(Color.rgb(70, 170, 110));
            c.drawCircle(t.x, t.y, 16f + t.level, paint);
        }
        for (Enemy e : enemies) {
            if (!e.active) continue;
            float rx = e.prevX + (e.x - e.prevX) * alpha;
            float ry = e.prevY + (e.y - e.prevY) * alpha;
            if (e.type == Enemy.Type.RUNNER) paint.setColor(Color.rgb(255, 190, 90));
            else if (e.type == Enemy.Type.ARMOR) paint.setColor(Color.rgb(80, 80, 80));
            else if (e.type == Enemy.Type.FLYER) paint.setColor(Color.rgb(140, 210, 255));
            else if (e.type == Enemy.Type.HEALER) paint.setColor(Color.rgb(120, 230, 150));
            else if (e.type == Enemy.Type.BOSS) paint.setColor(Color.rgb(220, 70, 70));
            else paint.setColor(Color.rgb(220, 130, 100));
            c.drawCircle(rx, ry, e.type == Enemy.Type.BOSS ? 20f : 12f, paint);
        }
        paint.setColor(Color.rgb(30, 40, 70));
        c.drawCircle(hero.x, hero.y, 11f, paint);
        paint.setColor(Color.rgb(255, 255, 120));
        for (Projectile p : projectiles) if (p.active) c.drawCircle(p.x, p.y, 4f, paint);
        paint.setColor(Color.rgb(255, 255, 255));
        for (Particle p : particles) if (p.active) c.drawCircle(p.x, p.y, p.size, paint);
        c.drawText(level.name, 20f, getHeight() - 20f, text);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
