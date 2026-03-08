package com.android.boot.engine;

import com.android.boot.input.TouchState;
import com.android.boot.model.GameDefs;

public class GameEngine {
    public final TouchState touchState = new TouchState();
    public int state = GameDefs.MENU;
    public int levelIndex;
    public int unlocked = 1;
    public int score;
    public int stars;
    public int birdsLeft;
    public String toast = "";
    public float toastTime;
    public boolean muted;
    public boolean showHelp;
    public final float[] trajectoryX = new float[20];
    public final float[] trajectoryY = new float[20];
    public int trajectoryCount;
    public final Block[] blocks = new Block[30];
    public int blockCount;
    public final Mechanic[] mechanics = new Mechanic[10];
    public int mechanicCount;
    public final Bird bird = new Bird();
    public float shake;
    public float flash;
    public float explosion;
    public float popupY;
    public float popupTime;
    public int currentUnit = GameDefs.RAM_BIRD;

    public GameEngine() {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new Block();
        }
        for (int i = 0; i < mechanics.length; i++) {
            mechanics[i] = new Mechanic();
        }
        resetLevel(0);
        state = GameDefs.MENU;
    }

    public void startCampaign() {
        levelIndex = 0;
        resetLevel(levelIndex);
        state = GameDefs.PLAYING;
    }

    public void restart() {
        resetLevel(levelIndex);
        state = GameDefs.PLAYING;
    }

    public void menu() {
        state = GameDefs.MENU;
    }

    public void resume() {
        if (state == GameDefs.PAUSED) {
            state = GameDefs.PLAYING;
        }
    }

    public void pause() {
        if (state == GameDefs.PLAYING) {
            state = GameDefs.PAUSED;
        }
    }

    public void update(float dt, int width, int height) {
        if (dt > 0.033f) {
            dt = 0.033f;
        }
        if (state != GameDefs.PLAYING) {
            touchState.resetFrame();
            return;
        }
        float slingX = width * 0.18f;
        float slingY = height * 0.72f;
        if (!bird.active) {
            bird.x = slingX;
            bird.y = slingY;
        }
        if (touchState.touching && !bird.active) {
            buildTrajectory(slingX, slingY, width, height);
        } else {
            trajectoryCount = 0;
        }
        if (touchState.released && !bird.active) {
            launchBird(slingX, slingY);
        }
        if (touchState.tapped && bird.active && !bird.skillUsed) {
            activateSkill();
        }
        stepBird(dt, width, height);
        stepEffects(dt);
        checkWinLose();
        touchState.resetFrame();
    }

    private void stepEffects(float dt) {
        toastTime -= dt;
        flash = Math.max(0f, flash - dt * 4f);
        explosion = Math.max(0f, explosion - dt * 2f);
        shake = Math.max(0f, shake - dt * 6f);
        popupTime = Math.max(0f, popupTime - dt);
        popupY -= dt * 60f;
    }

    private void buildTrajectory(float sx, float sy, int width, int height) {
        float dx = touchState.nowX - sx;
        float dy = touchState.nowY - sy;
        float pull = (float) Math.sqrt(dx * dx + dy * dy);
        float maxPull = width * 0.18f;
        if (pull > maxPull) {
            dx = dx / pull * maxPull;
            dy = dy / pull * maxPull;
        }
        float vx = -dx * 3.6f;
        float vy = -dy * 3.6f;
        trajectoryCount = trajectoryX.length;
        float px = sx;
        float py = sy;
        for (int i = 0; i < trajectoryCount; i++) {
            trajectoryX[i] = px;
            trajectoryY[i] = py;
            px += vx * 0.06f;
            py += vy * 0.06f;
            vy += height * 0.0009f;
        }
    }

    private void launchBird(float sx, float sy) {
        float dx = touchState.nowX - sx;
        float dy = touchState.nowY - sy;
        float pull = (float) Math.sqrt(dx * dx + dy * dy);
        float maxPull = 240f;
        if (pull > maxPull) {
            dx = dx / pull * maxPull;
            dy = dy / pull * maxPull;
        }
        bird.x = sx;
        bird.y = sy;
        bird.vx = -dx * 3.8f;
        bird.vy = -dy * 3.8f;
        bird.active = true;
        bird.skillUsed = false;
        bird.kind = currentUnit;
        currentUnit = (currentUnit + 1) % 5;
        birdsLeft--;
    }

    private void activateSkill() {
        bird.skillUsed = true;
        if (bird.kind == GameDefs.RAM_BIRD) {
            bird.vx *= 1.5f;
        } else if (bird.kind == GameDefs.SPLIT_BIRD) {
            explosion = 0.5f;
            damageRadius(bird.x, bird.y, 90f, 20f);
        } else if (bird.kind == GameDefs.BOMB_BIRD) {
            explosion = 1f;
            flash = 1f;
            damageRadius(bird.x, bird.y, 140f, 60f);
        } else if (bird.kind == GameDefs.DRILL_BIRD) {
            bird.pierce = 5;
            bird.vx *= 1.2f;
            bird.vy *= 1.2f;
        } else {
            bird.shield = 1.2f;
        }
    }

    private void stepBird(float dt, int width, int height) {
        if (!bird.active) {
            return;
        }
        bird.vy += height * 1.45f * dt;
        for (int i = 0; i < mechanicCount; i++) {
            Mechanic m = mechanics[i];
            if (m.kind == 1 && inside(bird.x, bird.y, m.x, m.y, m.w, m.h)) {
                bird.vx += m.power * dt;
            } else if (m.kind == 2 && inside(bird.x, bird.y, m.x, m.y, m.w, m.h)) {
                bird.vy = -Math.abs(bird.vy) * 0.9f;
            }
        }
        bird.x += bird.vx * dt;
        bird.y += bird.vy * dt;
        for (int i = 0; i < blockCount; i++) {
            Block b = blocks[i];
            if (b.hp <= 0f) {
                continue;
            }
            if (inside(bird.x, bird.y, b.x, b.y, b.w, b.h)) {
                float impact = Math.abs(bird.vx) * 0.02f + Math.abs(bird.vy) * 0.01f + 10f;
                if (b.material == GameDefs.MAT_METAL && !b.weakCore) {
                    impact *= 0.4f;
                }
                if (bird.kind == GameDefs.RAM_BIRD) {
                    impact *= 1.25f;
                }
                if (bird.kind == GameDefs.DRILL_BIRD && bird.pierce > 0) {
                    bird.pierce--;
                    impact *= 1.4f;
                } else {
                    bird.vx *= -0.38f;
                    bird.vy *= -0.32f;
                }
                b.hp -= impact;
                flash = 0.7f;
                shake = 0.6f;
                if (b.barrel && b.hp <= 0f) {
                    explosion = 1f;
                    damageRadius(b.x + b.w * 0.5f, b.y + b.h * 0.5f, 160f, 70f);
                }
                if (b.shielded) {
                    b.hp += 8f;
                }
                if (b.hp <= 0f) {
                    score += 120;
                    popupTime = 0.7f;
                    popupY = b.y;
                }
            }
        }
        if (bird.shield > 0f) {
            bird.shield -= dt;
        }
        if (bird.x < -80 || bird.x > width + 80 || bird.y > height + 120) {
            bird.active = false;
        }
    }

    private void damageRadius(float x, float y, float radius, float dmg) {
        for (int i = 0; i < blockCount; i++) {
            Block b = blocks[i];
            if (b.hp <= 0f) {
                continue;
            }
            float cx = b.x + b.w * 0.5f;
            float cy = b.y + b.h * 0.5f;
            float dx = cx - x;
            float dy = cy - y;
            float d2 = dx * dx + dy * dy;
            if (d2 < radius * radius) {
                b.hp -= dmg;
                if (b.hp <= 0f) {
                    score += 100;
                }
            }
        }
    }

    private void checkWinLose() {
        int aliveTargets = 0;
        for (int i = 0; i < blockCount; i++) {
            if (blocks[i].hp > 0f && blocks[i].target) {
                aliveTargets++;
            }
        }
        if (aliveTargets == 0) {
            stars = birdsLeft >= 4 ? 3 : birdsLeft >= 2 ? 2 : 1;
            toast = "Level clear";
            toastTime = 1.5f;
            unlocked = Math.max(unlocked, levelIndex + 2);
            state = GameDefs.GAME_OVER;
            return;
        }
        if (!bird.active && birdsLeft <= 0) {
            stars = 0;
            toast = "Out of birds";
            toastTime = 1.5f;
            state = GameDefs.GAME_OVER;
        }
    }

    public void nextLevel() {
        if (stars > 0 && levelIndex < GameDefs.LEVEL_NAMES.length - 1) {
            levelIndex++;
            resetLevel(levelIndex);
            state = GameDefs.PLAYING;
        } else {
            state = GameDefs.MENU;
        }
    }

    private void resetLevel(int idx) {
        birdsLeft = 7;
        score = 0;
        stars = 0;
        bird.active = false;
        currentUnit = 0;
        if (idx == 0) {
            fillWoodenOutpost();
        } else if (idx == 1) {
            fillGlassCorridor();
        } else if (idx == 2) {
            fillStoneBastion();
        } else if (idx == 3) {
            fillCrosswindRidge();
        } else {
            fillShieldCitadel();
        }
    }

    private void fillWoodenOutpost() {
        blockCount = 0;
        mechanicCount = 0;
        addBlock(770, 360, 48, 120, GameDefs.MAT_WOOD, true, false, false, false);
        addBlock(820, 400, 80, 80, GameDefs.MAT_WOOD, true, false, false, false);
        addBlock(900, 430, 50, 50, GameDefs.MAT_GLASS, false, false, false, false);
        addBlock(860, 350, 38, 38, GameDefs.MAT_METAL, false, true, false, false);
    }

    private void fillGlassCorridor() {
        blockCount = 0;
        mechanicCount = 0;
        for (int i = 0; i < 5; i++) {
            addBlock(680 + i * 45, 350 + i * 20, 40, 90, GameDefs.MAT_GLASS, i == 4, false, false, false);
        }
        addBlock(920, 460, 44, 44, GameDefs.MAT_WOOD, false, false, true, false);
        addBlock(980, 390, 48, 100, GameDefs.MAT_STONE, true, false, false, false);
        addMechanic(2, 820, 520, 180, 26, 0f);
    }

    private void fillStoneBastion() {
        blockCount = 0;
        mechanicCount = 0;
        addBlock(760, 300, 70, 200, GameDefs.MAT_STONE, false, false, false, false);
        addBlock(835, 300, 70, 200, GameDefs.MAT_STONE, false, false, false, false);
        addBlock(910, 300, 70, 200, GameDefs.MAT_STONE, true, false, false, false);
        addBlock(845, 450, 44, 44, GameDefs.MAT_WOOD, false, false, true, false);
        addBlock(850, 340, 50, 50, GameDefs.MAT_METAL, false, true, false, false);
    }

    private void fillCrosswindRidge() {
        blockCount = 0;
        mechanicCount = 0;
        addBlock(780, 390, 60, 120, GameDefs.MAT_WOOD, false, false, false, false);
        addBlock(850, 360, 60, 150, GameDefs.MAT_GLASS, true, false, false, false);
        addBlock(920, 340, 60, 170, GameDefs.MAT_STONE, true, false, false, false);
        addBlock(990, 430, 46, 46, GameDefs.MAT_WOOD, false, false, true, false);
        addMechanic(1, 620, 230, 500, 260, 160f);
    }

    private void fillShieldCitadel() {
        blockCount = 0;
        mechanicCount = 0;
        addBlock(760, 280, 65, 230, GameDefs.MAT_STONE, false, false, false, false);
        addBlock(830, 280, 65, 230, GameDefs.MAT_METAL, false, false, false, true);
        addBlock(900, 280, 65, 230, GameDefs.MAT_STONE, false, false, false, false);
        addBlock(970, 320, 65, 190, GameDefs.MAT_GLASS, true, false, false, false);
        addBlock(850, 350, 45, 45, GameDefs.MAT_METAL, true, true, false, false);
        addBlock(1010, 450, 46, 46, GameDefs.MAT_WOOD, false, false, true, false);
        addMechanic(3, 840, 250, 130, 270, 0f);
    }

    private void addBlock(float x, float y, float w, float h, int mat, boolean target, boolean weakCore, boolean barrel, boolean shielded) {
        Block b = blocks[blockCount++];
        b.x = x;
        b.y = y;
        b.w = w;
        b.h = h;
        b.material = mat;
        b.target = target;
        b.weakCore = weakCore;
        b.barrel = barrel;
        b.shielded = shielded;
        b.hp = mat == GameDefs.MAT_WOOD ? 45f : mat == GameDefs.MAT_GLASS ? 28f : mat == GameDefs.MAT_STONE ? 80f : 130f;
    }

    private void addMechanic(int kind, float x, float y, float w, float h, float power) {
        Mechanic m = mechanics[mechanicCount++];
        m.kind = kind;
        m.x = x;
        m.y = y;
        m.w = w;
        m.h = h;
        m.power = power;
    }

    private boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static class Bird {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public boolean active;
        public boolean skillUsed;
        public int kind;
        public int pierce;
        public float shield;
    }

    public static class Block {
        public float x;
        public float y;
        public float w;
        public float h;
        public float hp;
        public int material;
        public boolean target;
        public boolean weakCore;
        public boolean barrel;
        public boolean shielded;
    }

    public static class Mechanic {
        public int kind;
        public float x;
        public float y;
        public float w;
        public float h;
        public float power;
    }
}
