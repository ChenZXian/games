package com.android.boot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private enum State { MENU, PLAYING, PAUSED, GAME_OVER }
    private enum PlantType { SUNBUD, POD_SHOOTER, THORN_BLOCK, FROST_MINT, BURST_BLOOM }
    private enum EnemyType { SHAMBLER, RUNNER, HELM_BRUISER, SHIELD_CARRIER }

    private static class Plant {
        PlantType type;
        int row;
        int col;
        float hp;
        float fireCd;
        float sunCd;
        float fuse;
        float hitFlash;
    }

    private static class Enemy {
        EnemyType type;
        int row;
        float x;
        float hp;
        float armor;
        float speed;
        float biteCd;
        float slowMul = 1f;
        float slowT;
        float flash;
    }

    private static class Projectile {
        boolean active;
        int row;
        float x;
        float y;
        float speed;
        float damage;
        boolean frost;
    }

    private static class Particle {
        boolean active;
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float r;
        int color;
    }

    private static class SunOrb {
        boolean active;
        float x;
        float y;
        float baseY;
        float life;
        boolean collect;
        float tx;
        float ty;
    }

    private final SurfaceHolder holder;
    private Thread thread;
    private boolean running;
    private State state = State.MENU;
    private MainActivity host;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<Plant> plants = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Projectile[] projectiles = new Projectile[128];
    private final Particle[] particles = new Particle[256];
    private final SunOrb[] suns = new SunOrb[64];
    private final float[] cardCd = new float[5];
    private int selectedCard = -1;
    private boolean shovel;
    private int sun = 50;
    private int hearts = 5;
    private float spawnT;
    private float skySunT;
    private float levelTime;
    private int levelIndex = 1;
    private int wave = 0;
    private float waveT;
    private boolean bigWave;
    private float shake;
    private boolean showFps;
    private boolean muted;
    private long levelStart;
    private float fps;
    private float accDt;
    private int accFrames;

    private float gridLeft;
    private float gridTop;
    private float tile;
    private float laneH;
    private float previewX = -1f;
    private float previewY = -1f;

    public GameView(Context c, AttributeSet a) {
        super(c, a);
        holder = getHolder();
        holder.addCallback(this);
        for (int i = 0; i < projectiles.length; i++) projectiles[i] = new Projectile();
        for (int i = 0; i < particles.length; i++) particles[i] = new Particle();
        for (int i = 0; i < suns.length; i++) suns[i] = new SunOrb();
    }

    public void setHost(MainActivity host) { this.host = host; }
    public void showMenuState() { state = State.MENU; }
    public void startLevel(int level, boolean fpsEnabled, boolean mute) {
        state = State.PLAYING;
        levelIndex = level;
        showFps = fpsEnabled;
        muted = mute;
        reset();
    }
    public void restartLevel() { startLevel(levelIndex, showFps, muted); }
    public void pauseGame() { if (state == State.PLAYING) state = State.PAUSED; }
    public void resumeGame() { if (state == State.PAUSED) state = State.PLAYING; }

    private void reset() {
        plants.clear(); enemies.clear();
        for (Projectile pr : projectiles) pr.active = false;
        for (Particle pa : particles) pa.active = false;
        for (SunOrb s : suns) s.active = false;
        for (int i = 0; i < cardCd.length; i++) cardCd[i] = 0f;
        selectedCard = -1;
        shovel = false;
        sun = 100;
        previewX = -1f;
        previewY = -1f;
        hearts = 5;
        spawnT = 0f;
        skySunT = 2f;
        levelTime = 0f;
        wave = 0;
        waveT = 0f;
        bigWave = false;
        levelStart = System.currentTimeMillis();
        tile = getResources().getDisplayMetrics().density * 44f;
        laneH = tile;
        gridLeft = getWidth() * 0.1f;
        gridTop = getHeight() * 0.14f;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        tile = getResources().getDisplayMetrics().density * 44f;
        laneH = tile;
        gridLeft = width * 0.1f;
        gridTop = height * 0.14f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try { thread.join(); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - last) / 1000000000f);
            last = now;
            if (state == State.PLAYING) update(dt);
            drawFrame();
            accDt += dt;
            accFrames++;
            if (accDt > 0.4f) {
                fps = accFrames / accDt;
                accDt = 0f;
                accFrames = 0;
            }
        }
    }

    private void update(float dt) {
        if (tile <= 0f) return;
        levelTime += dt;
        waveT += dt;
        updateWaves(dt);
        for (int i = 0; i < cardCd.length; i++) if (cardCd[i] > 0f) cardCd[i] -= dt;
        skySunT -= dt;
        if (skySunT <= 0f) {
            spawnSkySun();
            skySunT = 4f + random.nextFloat() * 2f;
        }
        for (SunOrb s : suns) if (s.active) {
            s.life -= dt;
            if (!s.collect) {
                s.y = s.baseY + (float)Math.sin(levelTime * 5f + s.x * 0.01f) * 4f;
                if (s.life <= 0f) s.active = false;
            } else {
                s.x += (s.tx - s.x) * 10f * dt;
                s.y += (s.ty - s.y) * 10f * dt;
                if (Math.abs(s.x - s.tx) < 6f && Math.abs(s.y - s.ty) < 6f) {
                    s.active = false;
                    sun += 25;
                }
            }
        }
        for (Plant plant : plants) {
            plant.hitFlash = Math.max(0f, plant.hitFlash - dt * 4f);
            if (plant.type == PlantType.SUNBUD) {
                plant.sunCd -= dt;
                if (plant.sunCd <= 0f) {
                    plant.sunCd = 7f;
                    spawnSunOrb(gridLeft + (plant.col + 0.5f) * tile, gridTop + (plant.row + 0.5f) * laneH - 12f);
                }
            }
            if (plant.type == PlantType.BURST_BLOOM) {
                plant.fuse -= dt;
                if (plant.fuse <= 0f) {
                    explode(plant.row, plant.col);
                    plant.hp = 0f;
                }
            }
            if (plant.type == PlantType.POD_SHOOTER || plant.type == PlantType.FROST_MINT) {
                plant.fireCd -= dt;
                if (plant.fireCd <= 0f && hasEnemyAhead(plant.row, plant.col)) {
                    plant.fireCd = plant.type == PlantType.POD_SHOOTER ? 1.4f : 1.8f;
                    spawnProjectile(plant.row, gridLeft + (plant.col + 0.5f) * tile, gridTop + (plant.row + 0.5f) * laneH, plant.type == PlantType.POD_SHOOTER ? 260f : 240f, plant.type == PlantType.POD_SHOOTER ? 20f : 24f, plant.type == PlantType.FROST_MINT);
                }
            }
        }
        plants.removeIf(pl -> pl.hp <= 0f);

        for (Enemy e : enemies) {
            e.flash = Math.max(0f, e.flash - dt * 16f);
            if (e.slowT > 0f) {
                e.slowT -= dt;
                if (e.slowT <= 0f) e.slowMul = 1f;
            }
            Plant block = plantBlocking(e.row, e.x);
            e.biteCd -= dt;
            if (block != null) {
                if (e.biteCd <= 0f) {
                    e.biteCd = 0.9f;
                    block.hp -= biteDamage(e.type);
                    block.hitFlash = 1f;
                    if (block.type == PlantType.THORN_BLOCK) {
                        e.hp -= 6f;
                        e.flash = 1f;
                    }
                }
            } else {
                e.x -= e.speed * e.slowMul * dt;
            }
            if (e.x < gridLeft - tile * 0.2f) {
                e.hp = 0f;
                hearts -= 1;
                shake = 0.25f;
                if (hearts <= 0) {
                    state = State.GAME_OVER;
                    host.onLevelDefeat();
                }
            }
        }

        for (Projectile pr : projectiles) if (pr.active) {
            pr.x += pr.speed * dt;
            if (pr.x > gridLeft + tile * 9.8f) pr.active = false;
            Enemy hit = enemyAt(pr.row, pr.x);
            if (hit != null) {
                float d = pr.damage;
                if (hit.type == EnemyType.SHIELD_CARRIER) d *= 0.45f;
                if (hit.armor > 0f) {
                    float take = Math.min(hit.armor, d);
                    hit.armor -= take;
                    d -= take;
                }
                hit.hp -= d;
                hit.flash = 1f;
                if (pr.frost) {
                    hit.slowMul = 0.65f;
                    hit.slowT = 2.2f;
                }
                spark(pr.x, pr.y, pr.frost ? 0xFF44CCFF : 0xFFBDEB55);
                pr.active = false;
            }
        }

        for (Particle pa : particles) if (pa.active) {
            pa.life -= dt;
            pa.x += pa.vx * dt;
            pa.y += pa.vy * dt;
            pa.vy += 20f * dt;
            if (pa.life <= 0f) pa.active = false;
        }
        enemies.removeIf(e -> {
            if (e.hp <= 0f) {
                deathFx(e.x, gridTop + (e.row + 0.5f) * laneH);
                return true;
            }
            return false;
        });

        if (shake > 0f) shake -= dt;
    }

    private void updateWaves(float dt) {
        int total = levelIndex == 6 ? 6 : (levelIndex >= 4 ? 5 : (levelIndex == 1 ? 3 : 4));
        float[] durs = new float[]{25f,30f,35f,36f,40f,45f};
        if (wave >= total) {
            if (enemies.isEmpty()) {
                state = State.MENU;
                host.onLevelVictory(levelIndex, System.currentTimeMillis() - levelStart);
            }
            return;
        }
        float dur = durs[Math.min(durs.length - 1, wave)];
        spawnT -= dt;
        if (spawnT <= 0f) {
            spawnEnemyForLevel();
            float interval = Math.max(2.5f, 4.5f - levelIndex * 0.2f - wave * 0.1f);
            spawnT = interval * (0.9f + random.nextFloat() * 0.3f);
        }
        if (waveT > dur - 4f && !bigWave && wave == total - 1) {
            bigWave = true;
            for (int i = 0; i < 2 + levelIndex / 2; i++) spawnEnemy(EnemyType.SHAMBLER, random.nextInt(5));
        }
        if (waveT > dur) {
            wave++;
            waveT = 0f;
        }
    }

    private void spawnEnemyForLevel() {
        float r = random.nextFloat();
        EnemyType t = EnemyType.SHAMBLER;
        if (levelIndex >= 2 && r > 0.75f) t = EnemyType.RUNNER;
        if (levelIndex >= 3 && r > 0.85f) t = EnemyType.HELM_BRUISER;
        if (levelIndex >= 4 && r > 0.88f) t = EnemyType.SHIELD_CARRIER;
        int lane = random.nextInt(5);
        spawnEnemy(t, lane);
    }

    private void spawnEnemy(EnemyType t, int lane) {
        Enemy e = new Enemy();
        e.type = t;
        e.row = lane;
        e.x = gridLeft + tile * 9.25f;
        e.biteCd = 0.9f;
        if (t == EnemyType.SHAMBLER) { e.hp = 80f; e.speed = 18f; }
        if (t == EnemyType.RUNNER) { e.hp = 50f; e.speed = 32f; laneRunnerWarning(lane); }
        if (t == EnemyType.HELM_BRUISER) { e.hp = 110f; e.armor = 60f; e.speed = 16f; }
        if (t == EnemyType.SHIELD_CARRIER) { e.hp = 95f; e.speed = 14f; }
        enemies.add(e);
    }

    private float biteDamage(EnemyType type) {
        if (type == EnemyType.RUNNER) return 10f;
        if (type == EnemyType.HELM_BRUISER) return 16f;
        return 14f;
    }

    private void spawnSkySun() {
        float x = gridLeft + random.nextFloat() * tile * 9f;
        float y = gridTop + random.nextFloat() * getHeight() * 0.25f;
        spawnSunOrb(x, y);
    }

    private void spawnSunOrb(float x, float y) {
        for (SunOrb s : suns) if (!s.active) {
            s.active = true;
            s.x = x;
            s.baseY = y;
            s.y = y;
            s.life = 8f;
            s.collect = false;
            return;
        }
    }

    private void spawnProjectile(int row, float x, float y, float speed, float damage, boolean frost) {
        for (Projectile pr : projectiles) if (!pr.active) {
            pr.active = true;
            pr.row = row;
            pr.x = x;
            pr.y = y;
            pr.speed = speed;
            pr.damage = damage;
            pr.frost = frost;
            return;
        }
    }

    private void explode(int row, int col) {
        shake = 0.3f;
        float cx = gridLeft + (col + 0.5f) * tile;
        float cy = gridTop + (row + 0.5f) * laneH;
        for (Enemy e : enemies) {
            if (Math.abs(e.row - row) <= 1 && Math.abs(e.x - cx) < tile * 1.5f) {
                e.hp -= 180f;
                e.flash = 1f;
            }
        }
        spark(cx, cy, 0xFFFFAA33);
        if (!muted) new ToneGenerator(AudioManager.STREAM_MUSIC, 50).startTone(ToneGenerator.TONE_PROP_BEEP2, 90);
    }

    private void spark(float x, float y, int c) {
        for (int i = 0; i < 7; i++) {
            for (Particle pa : particles) if (!pa.active) {
                pa.active = true;
                pa.x = x;
                pa.y = y;
                pa.vx = (random.nextFloat() - 0.5f) * 140f;
                pa.vy = (random.nextFloat() - 0.8f) * 140f;
                pa.life = 0.28f + random.nextFloat() * 0.2f;
                pa.r = 2f + random.nextFloat() * 2f;
                pa.color = c;
                break;
            }
        }
    }

    private void deathFx(float x, float y) { spark(x, y, 0x88FFFFFF); }

    private boolean hasEnemyAhead(int row, int col) {
        float x = gridLeft + (col + 0.5f) * tile;
        for (Enemy e : enemies) if (e.row == row && e.x > x) return true;
        return false;
    }

    private Plant plantBlocking(int row, float x) {
        for (Plant pl : plants) {
            if (pl.row == row) {
                float px = gridLeft + (pl.col + 0.5f) * tile;
                if (Math.abs(px - x) < tile * 0.45f) return pl;
            }
        }
        return null;
    }

    private Enemy enemyAt(int row, float x) {
        for (Enemy e : enemies) if (e.row == row && Math.abs(e.x - x) < tile * 0.3f) return e;
        return null;
    }

    private void laneRunnerWarning(int lane) {
        for (int i = 0; i < 5; i++) spark(gridLeft + tile * 8.8f, gridTop + (lane + 0.5f) * laneH, 0xFFFF5566);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (state != State.PLAYING) return true;
        if (tile <= 0f) return true;
        float x = ev.getX();
        float y = ev.getY();
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            updatePreview(x, y);
            return true;
        }
        if (ev.getAction() != MotionEvent.ACTION_DOWN) return true;
        for (SunOrb s : suns) if (s.active && Math.hypot(x - s.x, y - s.y) < tile * 0.25f) {
            s.collect = true;
            s.tx = gridLeft + 30f;
            s.ty = 30f;
            if (!muted) new ToneGenerator(AudioManager.STREAM_MUSIC, 30).startTone(ToneGenerator.TONE_PROP_ACK, 40);
            return true;
        }
        float cardTop = gridTop + laneH * 5f + 8f;
        if (y > cardTop) {
            int idx = (int)((x - gridLeft) / (tile * 1.5f));
            if (idx >= 0 && idx < 5) {
                if (selectedCard == idx) selectedCard = -1;
                else { selectedCard = idx; shovel = false; }
                return true;
            }
            if (x > gridLeft + tile * 8f) {
                shovel = !shovel;
                selectedCard = -1;
                return true;
            }
        }
        int col = (int)((x - gridLeft) / tile);
        int row = (int)((y - gridTop) / laneH);
        if (row < 0 || row >= 5 || col < 0 || col >= 9) return true;
        if (shovel) {
            if (state == State.PLAYING) {
                plants.removeIf(pl -> pl.row == row && pl.col == col);
            }
            return true;
        }
        if (selectedCard >= 0 && selectedCard < PlantType.values().length && findPlant(row, col) == null) {
            PlantType t = PlantType.values()[selectedCard];
            int cost = cost(t);
            if (sun >= cost && cardCd[selectedCard] <= 0f) {
                Plant pl = new Plant();
                pl.type = t;
                pl.row = row;
                pl.col = col;
                pl.hp = hp(t);
                pl.fireCd = 0f;
                pl.sunCd = 7f;
                pl.fuse = 1f;
                pl.hitFlash = 0f;
                plants.add(pl);
                sun -= cost;
                cardCd[selectedCard] = cd(t);
            }
        }
        return true;
    }

    private void updatePreview(float x, float y) {
        int col = (int)((x - gridLeft) / tile);
        int row = (int)((y - gridTop) / laneH);
        if (row >= 0 && row < 5 && col >= 0 && col < 9) {
            previewX = gridLeft + (col + 0.5f) * tile;
            previewY = gridTop + (row + 0.5f) * laneH;
        } else {
            previewX = -1f;
            previewY = -1f;
        }
    }

    private Plant findPlant(int r, int c) {
        for (Plant pl : plants) if (pl.row == r && pl.col == c) return pl;
        return null;
    }

    private int cost(PlantType t) {
        if (t == PlantType.SUNBUD) return 50;
        if (t == PlantType.POD_SHOOTER) return 100;
        if (t == PlantType.THORN_BLOCK) return 50;
        if (t == PlantType.FROST_MINT) return 150;
        return 125;
    }

    private float cd(PlantType t) {
        if (t == PlantType.SUNBUD) return 5f;
        if (t == PlantType.POD_SHOOTER) return 6f;
        if (t == PlantType.THORN_BLOCK) return 10f;
        if (t == PlantType.FROST_MINT) return 10f;
        return 18f;
    }

    private float hp(PlantType t) {
        if (t == PlantType.SUNBUD) return 60f;
        if (t == PlantType.POD_SHOOTER) return 80f;
        if (t == PlantType.THORN_BLOCK) return 360f;
        if (t == PlantType.FROST_MINT) return 80f;
        return 40f;
    }

    private void drawFrame() {
        Canvas c = holder.lockCanvas();
        if (c == null) return;
        c.drawColor(0xFF07070C);
        if (state == State.MENU) {
            holder.unlockCanvasAndPost(c);
            return;
        }
        if (tile <= 0f) {
            tile = getResources().getDisplayMetrics().density * 44f;
            laneH = tile;
            gridLeft = getWidth() * 0.1f;
            gridTop = getHeight() * 0.14f;
        }
        float sx = (random.nextFloat() - 0.5f) * 16f * shake;
        float sy = (random.nextFloat() - 0.5f) * 16f * shake;
        c.save();
        c.translate(sx, sy);
        drawGrid(c);
        drawEntities(c);
        drawPreview(c);
        drawHud(c);
        drawCards(c);
        c.restore();
        if (state == State.PAUSED) {
            p.setColor(0xAA000000);
            c.drawRect(0f, 0f, getWidth(), getHeight(), p);
            p.setColor(Color.WHITE);
            p.setTextSize(52f);
            c.drawText("Paused", getWidth() * 0.43f, getHeight() * 0.5f, p);
        }
        if (showFps) {
            p.setColor(0xFFFFFFFF);
            p.setTextSize(24f);
            c.drawText("FPS " + ((int)fps), 20f, getHeight() - 20f, p);
        }
        holder.unlockCanvasAndPost(c);
    }

    private void drawGrid(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        for (int r = 0; r < 5; r++) {
            for (int col = 0; col < 9; col++) {
                p.setColor(((r + col) % 2 == 0) ? 0xFF141A2A : 0xFF101425);
                float l = gridLeft + col * tile;
                float t = gridTop + r * laneH;
                c.drawRect(l + 1f, t + 1f, l + tile - 1f, t + laneH - 1f, p);
            }
        }
    }

    private void drawEntities(Canvas c) {
        for (Plant pl : plants) {
            float cx = gridLeft + (pl.col + 0.5f) * tile;
            float cy = gridTop + (pl.row + 0.5f) * laneH;
            float maxHp = hp(pl.type);
            float hpRatio = pl.hp / maxHp;
            
            if (pl.type == PlantType.SUNBUD) {
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFFD94A);
                c.drawCircle(cx, cy - tile * 0.15f, tile * 0.28f, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFFAA00);
                c.drawCircle(cx, cy - tile * 0.15f, tile * 0.18f, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFFEE66);
                for (int i = 0; i < 8; i++) {
                    float angle = (float)(i * Math.PI / 4);
                    float px = cx + (float)Math.cos(angle) * tile * 0.22f;
                    float py = cy - tile * 0.15f + (float)Math.sin(angle) * tile * 0.22f;
                    c.drawCircle(px, py, tile * 0.04f, p);
                }
            } else if (pl.type == PlantType.POD_SHOOTER) {
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF4ACF7A);
                float bodyR = tile * 0.2f;
                c.drawCircle(cx, cy, bodyR, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF6BE0A2);
                c.drawRect(cx - bodyR * 0.3f, cy - bodyR * 0.8f, cx + bodyR * 0.3f, cy - bodyR * 0.2f, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF2A8F5A);
                c.drawCircle(cx + tile * 0.18f, cy - tile * 0.1f, tile * 0.1f, p);
            } else if (pl.type == PlantType.THORN_BLOCK) {
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF888844);
                float size = tile * 0.3f;
                c.drawRect(cx - size, cy - size * 0.5f, cx + size, cy + size * 0.5f, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFAA9966);
                for (int i = 0; i < 4; i++) {
                    float angle = (float)(i * Math.PI / 2);
                    float px = cx + (float)Math.cos(angle) * size * 0.75f;
                    float py = cy + (float)Math.sin(angle) * size * 0.75f;
                    float th = tile * 0.06f;
                    c.drawRect(px - th, py - th, px + th, py + th, p);
                }
            } else if (pl.type == PlantType.FROST_MINT) {
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF66CCFF);
                float centerR = tile * 0.22f;
                c.drawCircle(cx, cy, centerR, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFAAEEFF);
                for (int i = 0; i < 6; i++) {
                    float angle = (float)(i * Math.PI / 3);
                    float px = cx + (float)Math.cos(angle) * tile * 0.18f;
                    float py = cy + (float)Math.sin(angle) * tile * 0.18f;
                    float r = tile * 0.07f;
                    c.drawCircle(px, py, r, p);
                    float innerR = r * 0.5f;
                    p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFF88DDFF);
                    c.drawCircle(px, py, innerR, p);
                    p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFAAEEFF);
                }
            } else if (pl.type == PlantType.BURST_BLOOM) {
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFFAA55);
                float centerR = tile * 0.24f;
                c.drawCircle(cx, cy, centerR, p);
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFF7744);
                for (int i = 0; i < 8; i++) {
                    float angle = (float)(i * Math.PI / 4);
                    float px = cx + (float)Math.cos(angle) * tile * 0.2f;
                    float py = cy + (float)Math.sin(angle) * tile * 0.2f;
                    float r = tile * 0.08f;
                    c.drawCircle(px, py, r, p);
                }
                p.setColor(pl.hitFlash > 0f ? 0xFFFFFFFF : 0xFFFFCC88);
                c.drawCircle(cx, cy, tile * 0.12f, p);
            }
            
            float barW = tile * 0.5f;
            float barH = tile * 0.08f;
            float barX = cx - barW * 0.5f;
            float barY = cy - tile * 0.4f;
            p.setColor(0xFF000000);
            c.drawRect(barX - 1f, barY - 1f, barX + barW + 1f, barY + barH + 1f, p);
            p.setColor(0xFF333333);
            c.drawRect(barX, barY, barX + barW, barY + barH, p);
            p.setColor(hpRatio > 0.5f ? 0xFF00FF00 : (hpRatio > 0.25f ? 0xFFFFFF00 : 0xFFFF0000));
            c.drawRect(barX, barY, barX + barW * hpRatio, barY + barH, p);
        }
        
        for (Enemy e : enemies) {
            float cy = gridTop + (e.row + 0.5f) * laneH;
            float maxHp = e.type == EnemyType.SHAMBLER ? 80f : (e.type == EnemyType.RUNNER ? 50f : (e.type == EnemyType.HELM_BRUISER ? 110f : 95f));
            float hpRatio = Math.max(0f, e.hp / maxHp);
            
            int bodyColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF6B8E4A;
            int headColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF8B6B4A;
            if (e.type == EnemyType.RUNNER) {
                bodyColor = e.flash > 0f ? 0xFFFFFFFF : 0xFFFF6655;
                headColor = e.flash > 0f ? 0xFFFFFFFF : 0xFFFF8866;
            } else if (e.type == EnemyType.HELM_BRUISER) {
                bodyColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF888899;
                headColor = e.flash > 0f ? 0xFFFFFFFF : 0xFFAAAAAA;
            } else if (e.type == EnemyType.SHIELD_CARRIER) {
                bodyColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF5577AA;
                headColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF7799CC;
            }
            
            p.setColor(bodyColor);
            float bodyW = tile * 0.36f;
            float bodyH = tile * 0.32f;
            float bodyTop = cy - bodyH * 0.25f;
            float bodyBottom = cy + bodyH * 0.75f;
            c.drawRect(e.x - bodyW * 0.5f, bodyTop, e.x + bodyW * 0.5f, bodyBottom, p);
            
            p.setColor(headColor);
            float headR = tile * 0.19f;
            float headY = cy - tile * 0.18f;
            c.drawCircle(e.x, headY, headR, p);
            
            int eyeColor = e.flash > 0f ? 0xFFFFFFFF : 0xFF000000;
            p.setColor(eyeColor);
            float eyeR = tile * 0.04f;
            c.drawCircle(e.x - tile * 0.06f, headY - tile * 0.03f, eyeR, p);
            c.drawCircle(e.x + tile * 0.06f, headY - tile * 0.03f, eyeR, p);
            
            if (e.type == EnemyType.HELM_BRUISER) {
                p.setColor(e.flash > 0f ? 0xFFFFFFFF : 0xFFCCCCCC);
                c.drawRect(e.x - tile * 0.13f, headY - tile * 0.12f, e.x + tile * 0.13f, headY + tile * 0.05f, p);
                p.setColor(e.flash > 0f ? 0xFFFFFFFF : 0xFFAAAAAA);
                c.drawRect(e.x - tile * 0.1f, headY - tile * 0.15f, e.x + tile * 0.1f, headY - tile * 0.12f, p);
            }
            
            if (e.type == EnemyType.SHIELD_CARRIER && e.armor > 0f) {
                p.setColor(0xFF88CCFF);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                c.drawRect(e.x - bodyW * 0.5f - 3f, bodyTop - 3f, e.x + bodyW * 0.5f + 3f, bodyBottom + 3f, p);
                p.setStyle(Paint.Style.FILL);
            }
            
            if (e.slowT > 0f) {
                p.setColor(0x8866CCFF);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f);
                c.drawCircle(e.x, cy, headR + tile * 0.05f, p);
                p.setStyle(Paint.Style.FILL);
            }
            
            float barW = tile * 0.5f;
            float barH = tile * 0.06f;
            float barX = e.x - barW * 0.5f;
            float barY = cy - tile * 0.45f;
            p.setColor(0xFF000000);
            c.drawRect(barX - 1f, barY - 1f, barX + barW + 1f, barY + barH + 1f, p);
            p.setColor(0xFF333333);
            c.drawRect(barX, barY, barX + barW, barY + barH, p);
            p.setColor(hpRatio > 0.5f ? 0xFFFF0000 : (hpRatio > 0.25f ? 0xFFFF6600 : 0xFFFFAA00));
            c.drawRect(barX, barY, barX + barW * hpRatio, barY + barH, p);
        }
        for (Projectile pr : projectiles) if (pr.active) {
            p.setColor(pr.frost ? 0xFF66D9FF : 0xFFC5F76D);
            c.drawCircle(pr.x, pr.y, tile * 0.08f, p);
        }
        for (SunOrb s : suns) if (s.active) {
            float sunR = tile * 0.25f;
            float pulse = 1f + (float)Math.sin(levelTime * 8f + s.x * 0.1f) * 0.15f;
            float currentR = sunR * pulse;
            
            p.setColor(0x88FFFF00);
            c.drawCircle(s.x, s.y, currentR * 1.4f, p);
            p.setColor(0xAAFFFF66);
            c.drawCircle(s.x, s.y, currentR * 1.2f, p);
            p.setColor(0xFFFFD94A);
            c.drawCircle(s.x, s.y, currentR, p);
            p.setColor(0xFFFFEE88);
            c.drawCircle(s.x, s.y, currentR * 0.6f, p);
            
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(0xFFFFAA00);
            c.drawCircle(s.x, s.y, currentR, p);
            p.setStyle(Paint.Style.FILL);
        }
        for (Particle pa : particles) if (pa.active) {
            p.setColor(pa.color);
            c.drawCircle(pa.x, pa.y, pa.r, p);
        }
    }

    private void drawPreview(Canvas c) {
        if (previewX < 0f || previewY < 0f || selectedCard < 0 || selectedCard >= PlantType.values().length) return;
        if (state != State.PLAYING) return;
        PlantType t = PlantType.values()[selectedCard];
        int cost = cost(t);
        int row = (int)((previewY - gridTop) / laneH);
        int col = (int)((previewX - gridLeft) / tile);
        boolean canPlace = sun >= cost && cardCd[selectedCard] <= 0f && findPlant(row, col) == null;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(canPlace ? 0x8800FF00 : 0x88FF0000);
        float size = tile * 0.4f;
        c.drawRect(previewX - size, previewY - size, previewX + size, previewY + size, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHud(Canvas c) {
        p.setColor(0xFFFFD94A);
        p.setTextSize(tile * 0.35f);
        p.setFakeBoldText(true);
        c.drawText("Sun: " + sun, gridLeft, gridTop - 12f, p);
        p.setColor(0xFFFF6677);
        c.drawText("Base: " + hearts, gridLeft + tile * 5.5f, gridTop - 12f, p);
        p.setColor(0xFFAAEEFF);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Level " + levelIndex, getWidth() - gridLeft, gridTop - 12f, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setFakeBoldText(false);
        p.setColor(0xFF2C314A);
        c.drawRoundRect(new RectF(gridLeft + tile * 2.3f, gridTop - 32f, gridLeft + tile * 5.8f, gridTop - 18f), 8f, 8f, p);
        p.setColor(0xFF20FFB2);
        float progress = (wave + waveT / 35f) / (levelIndex == 6 ? 6f : (levelIndex >= 4 ? 5f : (levelIndex == 1 ? 3f : 4f)));
        c.drawRoundRect(new RectF(gridLeft + tile * 2.3f, gridTop - 32f, gridLeft + tile * (2.3f + 3.5f * progress), gridTop - 18f), 8f, 8f, p);
        if (bigWave) {
            p.setColor(0xFFFF3D5A);
            p.setTextSize(tile * 0.28f);
            c.drawText("Big Wave", gridLeft + tile * 3.2f, gridTop - 40f, p);
        }
    }

    private void drawCards(Canvas c) {
        float top = gridTop + laneH * 5f + 8f;
        for (int i = 0; i < 5; i++) {
            float l = gridLeft + i * tile * 1.5f;
            float r = l + tile * 1.35f;
            float b = top + tile * 0.95f;
            int cost = cost(PlantType.values()[i]);
            boolean disabled = sun < cost || cardCd[i] > 0f;
            p.setColor(disabled ? 0xFF333344 : (selectedCard == i ? 0xFF00F2FF : 0xFF1E2440));
            c.drawRoundRect(new RectF(l, top, r, b), 12f, 12f, p);
            if (selectedCard == i) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3f);
                p.setColor(0xFF00F2FF);
                c.drawRoundRect(new RectF(l + 2f, top + 2f, r - 2f, b - 2f), 10f, 10f, p);
                p.setStyle(Paint.Style.FILL);
            }
            p.setColor(disabled ? 0xFF888888 : 0xFFFFFFFF);
            p.setTextSize(tile * 0.22f);
            String name = PlantType.values()[i].name().replace('_', ' ');
            if (name.length() > 12) name = name.substring(0, 12);
            c.drawText(name, l + 8f, top + tile * 0.35f, p);
            p.setColor(disabled ? 0xFF666666 : 0xFFFFD94A);
            p.setTextSize(tile * 0.28f);
            p.setFakeBoldText(true);
            c.drawText(String.valueOf(cost), l + 8f, top + tile * 0.7f, p);
            p.setFakeBoldText(false);
            if (cardCd[i] > 0f) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5f);
                p.setColor(0xFF7A82B8);
                float max = cd(PlantType.values()[i]);
                c.drawArc(new RectF(l + 3f, top + 3f, r - 3f, b - 3f), -90f, 360f * (cardCd[i] / max), false, p);
                p.setStyle(Paint.Style.FILL);
            }
        }
        float sl = gridLeft + tile * 8f;
        p.setColor(shovel ? 0xFFFFAA33 : 0xFF1E2440);
        c.drawRoundRect(new RectF(sl, top, sl + tile, top + tile * 0.95f), 12f, 12f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(tile * 0.28f);
        c.drawText("SH", sl + tile * 0.24f, top + tile * 0.6f, p);
    }
}
