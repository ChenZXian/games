package com.android.boot.engine;

import com.android.boot.model.GameDefs;

public class GameEngine {
    private static final int MAX_BLOCKS = 48;
    private static final int MAX_TRAJECTORY = 24;

    private final Block[] blocks = new Block[MAX_BLOCKS];
    private final float[] trajectoryX = new float[MAX_TRAJECTORY];
    private final float[] trajectoryY = new float[MAX_TRAJECTORY];
    private final Bird bird = new Bird();
    private final Stage[] stages = new Stage[]{
            new Stage("Cliff Nest 1", GameDefs.OBJECTIVE_TEXT[0], GameDefs.WEATHER_CLEAR, 6),
            new Stage("Storm Bridge 2", GameDefs.OBJECTIVE_TEXT[1], GameDefs.WEATHER_CROSSWIND, 6),
            new Stage("Iron Cloud 3", GameDefs.OBJECTIVE_TEXT[2], GameDefs.WEATHER_STORM, 7),
            new Stage("Crystal Tempest 4", GameDefs.OBJECTIVE_TEXT[3], GameDefs.WEATHER_UPDRAFT, 7),
            new Stage("Skyfall Keep 5", GameDefs.OBJECTIVE_TEXT[4], GameDefs.WEATHER_FREEZE, 8)
    };

    private int state = GameDefs.MENU;
    private int levelIndex;
    private int birdsLeft;
    private int score;
    private int stars;
    private int trajectoryCount;
    private int blockCount;
    private int launchedBirds;
    private int width = 1920;
    private int height = 1080;
    private boolean aiming;
    private boolean resultWin;
    private boolean muted;
    private float slingX;
    private float slingY;
    private float aimX;
    private float aimY;
    private float crosswindForce;
    private float effectPulse;
    private float flash;
    private float cameraShake;
    private String toast = "";
    private float toastTime;

    public GameEngine() {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new Block();
        }
        resetStage(0);
        state = GameDefs.MENU;
    }

    public synchronized void setViewport(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        if (state != GameDefs.MENU) {
            resetStage(levelIndex);
        }
    }

    public synchronized void startCampaign() {
        levelIndex = 0;
        resetStage(levelIndex);
        state = GameDefs.PLAYING;
    }

    public synchronized void backToMenu() {
        state = GameDefs.MENU;
        toast = "";
        toastTime = 0f;
    }

    public synchronized void pause() {
        if (state == GameDefs.PLAYING) {
            state = GameDefs.PAUSED;
        }
    }

    public synchronized void resume() {
        if (state == GameDefs.PAUSED) {
            state = GameDefs.PLAYING;
        }
    }

    public synchronized void restartStage() {
        resetStage(levelIndex);
        state = GameDefs.PLAYING;
    }

    public synchronized void advanceAfterResult() {
        if (resultWin && levelIndex < stages.length - 1) {
            levelIndex++;
            resetStage(levelIndex);
            state = GameDefs.PLAYING;
        } else if (resultWin) {
            backToMenu();
        } else {
            restartStage();
        }
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void beginTouch(float x, float y) {
        if (state != GameDefs.PLAYING) {
            return;
        }
        if (bird.active) {
            activateSkill();
            return;
        }
        aiming = true;
        aimX = x;
        aimY = y;
        buildTrajectory();
    }

    public synchronized void moveTouch(float x, float y) {
        if (!aiming || state != GameDefs.PLAYING || bird.active) {
            return;
        }
        aimX = x;
        aimY = y;
        buildTrajectory();
    }

    public synchronized void endTouch(float x, float y) {
        if (!aiming || state != GameDefs.PLAYING || bird.active) {
            aiming = false;
            trajectoryCount = 0;
            return;
        }
        aimX = x;
        aimY = y;
        launchBird();
        aiming = false;
        trajectoryCount = 0;
    }

    public synchronized void update(float dt) {
        if (dt > 0.033f) {
            dt = 0.033f;
        }
        if (state != GameDefs.PLAYING) {
            stepEffects(dt);
            return;
        }
        if (!bird.active) {
            bird.x = slingX;
            bird.y = slingY;
        }
        updateBird(dt);
        updateBlocks(dt);
        updateSupports();
        checkStageResult();
        stepEffects(dt);
    }

    public synchronized HudSnapshot getSnapshot() {
        Stage stage = stages[levelIndex];
        String resultTitle = resultWin ? "Fortress Broken" : "Squad Lost";
        String resultBody = resultWin
                ? "Stars " + stars + "  Score " + score + "  " + (levelIndex < stages.length - 1 ? "Next fortress unlocked" : "Campaign clear")
                : "Score " + score + "  Retry this fortress";
        return new HudSnapshot(
                state,
                GameDefs.CHAPTER_NAMES[Math.min(levelIndex, GameDefs.CHAPTER_NAMES.length - 1)],
                stage.name,
                stage.objective,
                GameDefs.WEATHER_TEXT[stage.weather],
                birdsLeft,
                score,
                stars,
                bird.active,
                resultWin,
                levelIndex,
                stages.length,
                GameDefs.BIRD_NAMES[launchedBirds % GameDefs.BIRD_NAMES.length],
                GameDefs.BIRD_NAMES[(launchedBirds + 1) % GameDefs.BIRD_NAMES.length],
                toast,
                resultTitle,
                resultBody
        );
    }

    public synchronized RenderState copyRenderState() {
        RenderState render = new RenderState();
        render.width = width;
        render.height = height;
        render.state = state;
        render.levelIndex = levelIndex;
        render.slingX = slingX;
        render.slingY = slingY;
        render.birdActive = bird.active;
        render.birdX = bird.x;
        render.birdY = bird.y;
        render.birdRadius = bird.radius;
        render.birdKind = bird.kind;
        render.aiming = aiming;
        render.trajectoryCount = trajectoryCount;
        render.birdsLeft = birdsLeft;
        render.weather = stages[levelIndex].weather;
        render.effectPulse = effectPulse;
        render.flash = flash;
        render.cameraShake = cameraShake;
        render.toast = toast;
        render.toastTime = toastTime;
        render.currentBirdName = GameDefs.BIRD_NAMES[launchedBirds % GameDefs.BIRD_NAMES.length];
        for (int i = 0; i < trajectoryCount; i++) {
            render.trajectoryX[i] = trajectoryX[i];
            render.trajectoryY[i] = trajectoryY[i];
        }
        render.blockCount = blockCount;
        for (int i = 0; i < blockCount; i++) {
            render.blocks[i].copyFrom(blocks[i]);
        }
        return render;
    }

    private void stepEffects(float dt) {
        if (toastTime > 0f) {
            toastTime -= dt;
            if (toastTime <= 0f) {
                toast = "";
            }
        }
        effectPulse = Math.max(0f, effectPulse - dt * 1.5f);
        flash = Math.max(0f, flash - dt * 3f);
        cameraShake = Math.max(0f, cameraShake - dt * 5f);
    }

    private void updateBird(float dt) {
        if (!bird.active) {
            return;
        }
        Stage stage = stages[levelIndex];
        bird.vy += height * 1.48f * dt;
        if (stage.weather == GameDefs.WEATHER_CROSSWIND) {
            bird.vx += crosswindForce * dt;
        } else if (stage.weather == GameDefs.WEATHER_UPDRAFT && bird.x > width * 0.45f && bird.x < width * 0.72f) {
            bird.vy -= height * 1.1f * dt;
        } else if (stage.weather == GameDefs.WEATHER_STORM && ((int) (System.nanoTime() / 300000000L) % 4 == 0)) {
            bird.vx += crosswindForce * 0.4f * dt;
            flash = 0.35f;
        } else if (stage.weather == GameDefs.WEATHER_FREEZE && bird.skillUsed) {
            bird.vy *= 0.985f;
        }
        bird.x += bird.vx * dt;
        bird.y += bird.vy * dt;
        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            if (block.alive && intersectsBird(block)) {
                resolveBirdHit(block);
            }
        }
        if (bird.y > height + bird.radius * 3f || bird.x > width + bird.radius * 3f || bird.x < -bird.radius * 3f) {
            bird.active = false;
        }
    }

    private void updateBlocks(float dt) {
        float ground = height * 0.9f;
        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            if (!block.alive || !block.dynamic) {
                continue;
            }
            block.vy += height * 1.2f * dt;
            block.x += block.vx * dt;
            block.y += block.vy * dt;
            if (block.y + block.h > ground) {
                block.y = ground - block.h;
                block.vy *= -0.18f;
                block.vx *= 0.82f;
            }
            for (int j = 0; j < blockCount; j++) {
                if (i == j) {
                    continue;
                }
                Block other = blocks[j];
                if (!other.alive) {
                    continue;
                }
                if (rectsOverlap(block, other) && block.y < other.y && block.vy > 0f) {
                    block.y = other.y - block.h;
                    block.vy *= -0.12f;
                    other.vx += block.vx * 0.2f;
                }
            }
            if (Math.abs(block.vx) < 6f) {
                block.vx = 0f;
            }
            if (Math.abs(block.vy) < 10f) {
                block.vy = 0f;
            }
        }
    }

    private void updateSupports() {
        float ground = height * 0.9f;
        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            if (!block.alive || block.dynamic || block.anchor) {
                continue;
            }
            boolean supported = block.y + block.h >= ground - 6f;
            for (int j = 0; j < blockCount && !supported; j++) {
                if (i == j) {
                    continue;
                }
                Block other = blocks[j];
                if (!other.alive) {
                    continue;
                }
                float overlap = Math.min(block.x + block.w, other.x + other.w) - Math.max(block.x, other.x);
                if (overlap > block.w * 0.28f && Math.abs((block.y + block.h) - other.y) < 18f) {
                    supported = true;
                }
            }
            if (!supported) {
                block.dynamic = true;
                block.vx += crosswindForce * 0.12f;
            }
        }
    }

    private void checkStageResult() {
        int targetsAlive = 0;
        for (int i = 0; i < blockCount; i++) {
            if (blocks[i].alive && blocks[i].targetType >= 0) {
                targetsAlive++;
            }
        }
        if (targetsAlive == 0) {
            resultWin = true;
            stars = birdsLeft >= 4 ? 3 : birdsLeft >= 2 ? 2 : 1;
            toast = "Chain collapse secured";
            toastTime = 1.7f;
            state = GameDefs.GAME_OVER;
            return;
        }
        if (!bird.active && birdsLeft <= 0) {
            resultWin = false;
            stars = 0;
            toast = "No birds remaining";
            toastTime = 1.7f;
            state = GameDefs.GAME_OVER;
        }
    }

    private void buildTrajectory() {
        float dx = aimX - slingX;
        float dy = aimY - slingY;
        float maxPull = width * 0.15f;
        float pull = (float) Math.sqrt(dx * dx + dy * dy);
        if (pull > maxPull) {
            dx = dx / pull * maxPull;
            dy = dy / pull * maxPull;
        }
        float vx = -dx * 4.3f;
        float vy = -dy * 4.3f;
        float px = slingX;
        float py = slingY;
        trajectoryCount = MAX_TRAJECTORY;
        for (int i = 0; i < trajectoryCount; i++) {
            trajectoryX[i] = px;
            trajectoryY[i] = py;
            px += vx * 0.055f;
            py += vy * 0.055f;
            vy += height * 0.085f;
        }
    }

    private void launchBird() {
        float dx = aimX - slingX;
        float dy = aimY - slingY;
        float maxPull = width * 0.15f;
        float pull = (float) Math.sqrt(dx * dx + dy * dy);
        if (pull < width * 0.015f) {
            return;
        }
        if (pull > maxPull) {
            dx = dx / pull * maxPull;
            dy = dy / pull * maxPull;
        }
        bird.kind = launchedBirds % GameDefs.BIRD_NAMES.length;
        bird.x = slingX;
        bird.y = slingY;
        bird.vx = -dx * 4.6f;
        bird.vy = -dy * 4.6f;
        bird.radius = width * 0.021f;
        bird.active = true;
        bird.skillUsed = false;
        bird.shieldTime = 0f;
        bird.pierce = bird.kind == GameDefs.BIRD_RAM ? 2 : 0;
        birdsLeft--;
        launchedBirds++;
        cameraShake = 0.18f;
    }

    private void activateSkill() {
        if (!bird.active || bird.skillUsed) {
            return;
        }
        bird.skillUsed = true;
        effectPulse = 1f;
        if (bird.kind == GameDefs.BIRD_GUST) {
            bird.vx *= 1.3f;
            bird.vy -= height * 0.18f;
        } else if (bird.kind == GameDefs.BIRD_EMBER) {
            damageRadius(bird.x, bird.y, width * 0.085f, 26f, true);
        } else if (bird.kind == GameDefs.BIRD_BOLT) {
            chainStrike(bird.x, bird.y);
        } else if (bird.kind == GameDefs.BIRD_FROST) {
            damageRadius(bird.x, bird.y, width * 0.1f, 18f, false);
            bird.shieldTime = 0.8f;
        } else if (bird.kind == GameDefs.BIRD_RAM) {
            bird.vx *= 1.55f;
            bird.pierce = 4;
        }
    }

    private void resolveBirdHit(Block block) {
        float impact = Math.abs(bird.vx) * 0.012f + Math.abs(bird.vy) * 0.008f + 8f;
        if (block.material == GameDefs.MAT_METAL) {
            impact *= bird.kind == GameDefs.BIRD_BOLT ? 1.4f : 0.68f;
        } else if (block.material == GameDefs.MAT_GLASS) {
            impact *= 1.38f;
        } else if (block.material == GameDefs.MAT_STONE) {
            impact *= bird.kind == GameDefs.BIRD_RAM ? 1.3f : 0.82f;
        }
        if (bird.kind == GameDefs.BIRD_EMBER) {
            impact *= 1.15f;
        }
        damageBlock(block, impact, true);
        if (bird.pierce > 0) {
            bird.pierce--;
            bird.vx *= 0.92f;
        } else {
            bird.vx *= -0.28f;
            bird.vy *= -0.2f;
        }
        if (bird.shieldTime > 0f) {
            bird.vx *= 1.05f;
            bird.vy *= 0.92f;
        }
        if (block.targetType == GameDefs.TARGET_BARREL) {
            damageRadius(block.x + block.w * 0.5f, block.y + block.h * 0.5f, width * 0.09f, 36f, true);
        }
        flash = 0.55f;
        cameraShake = 0.38f;
    }

    private void damageRadius(float x, float y, float radius, float damage, boolean ignite) {
        float radiusSq = radius * radius;
        for (int i = 0; i < blockCount; i++) {
            Block block = blocks[i];
            if (!block.alive) {
                continue;
            }
            float cx = block.x + block.w * 0.5f;
            float cy = block.y + block.h * 0.5f;
            float dx = cx - x;
            float dy = cy - y;
            float distSq = dx * dx + dy * dy;
            if (distSq <= radiusSq) {
                float power = 1f - (distSq / radiusSq);
                damageBlock(block, damage * power, ignite);
                block.vx += dx * 0.08f;
                block.vy -= Math.abs(dy) * 0.04f + 30f * power;
                block.dynamic = true;
            }
        }
    }

    private void chainStrike(float x, float y) {
        int hits = 0;
        for (int i = 0; i < blockCount && hits < 3; i++) {
            Block block = blocks[i];
            if (!block.alive) {
                continue;
            }
            if (block.material == GameDefs.MAT_METAL || block.targetType == GameDefs.TARGET_CORE || block.targetType == GameDefs.TARGET_RELAY) {
                float cx = block.x + block.w * 0.5f;
                float cy = block.y + block.h * 0.5f;
                float dx = cx - x;
                float dy = cy - y;
                if (dx * dx + dy * dy < width * width * 0.12f) {
                    damageBlock(block, 28f - hits * 5f, false);
                    block.dynamic = true;
                    hits++;
                }
            }
        }
    }

    private void damageBlock(Block block, float damage, boolean ignite) {
        block.hp -= damage;
        if (ignite && block.material == GameDefs.MAT_WOOD) {
            block.hp -= 4f;
        }
        if (block.hp <= block.maxHp * 0.5f) {
            block.dynamic = true;
        }
        if (block.hp <= 0f) {
            block.alive = false;
            score += block.targetType >= 0 ? 180 : 80;
            effectPulse = 1f;
        }
    }

    private boolean intersectsBird(Block block) {
        float closestX = clamp(bird.x, block.x, block.x + block.w);
        float closestY = clamp(bird.y, block.y, block.y + block.h);
        float dx = bird.x - closestX;
        float dy = bird.y - closestY;
        return dx * dx + dy * dy <= bird.radius * bird.radius;
    }

    private boolean rectsOverlap(Block a, Block b) {
        return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resetStage(int index) {
        Stage stage = stages[index];
        slingX = width * 0.18f;
        slingY = height * 0.72f;
        birdsLeft = stage.birdCount;
        score = 0;
        stars = 0;
        launchedBirds = 0;
        aiming = false;
        trajectoryCount = 0;
        resultWin = false;
        bird.active = false;
        bird.skillUsed = false;
        toast = stage.objective;
        toastTime = 1.4f;
        crosswindForce = width * 0.07f * ((index % 2 == 0) ? 1f : -1f);
        effectPulse = 0f;
        flash = 0f;
        cameraShake = 0f;
        clearBlocks();
        if (index == 0) {
            buildCliffNest();
        } else if (index == 1) {
            buildStormBridge();
        } else if (index == 2) {
            buildIronCloud();
        } else if (index == 3) {
            buildCrystalTempest();
        } else {
            buildSkyfallKeep();
        }
    }

    private void clearBlocks() {
        blockCount = 0;
        for (Block block : blocks) {
            block.reset();
        }
    }

    private void buildCliffNest() {
        addBlock(0.73f, 0.62f, 0.04f, 0.2f, GameDefs.MAT_WOOD, -1, true);
        addBlock(0.78f, 0.62f, 0.04f, 0.2f, GameDefs.MAT_WOOD, -1, true);
        addBlock(0.735f, 0.55f, 0.085f, 0.05f, GameDefs.MAT_STONE, -1, false);
        addBlock(0.755f, 0.46f, 0.045f, 0.08f, GameDefs.MAT_GLASS, GameDefs.TARGET_CORE, false);
        addBlock(0.825f, 0.7f, 0.03f, 0.05f, GameDefs.MAT_WOOD, GameDefs.TARGET_BARREL, false);
    }

    private void buildStormBridge() {
        addBlock(0.66f, 0.68f, 0.05f, 0.12f, GameDefs.MAT_WOOD, -1, true);
        addBlock(0.73f, 0.6f, 0.04f, 0.2f, GameDefs.MAT_GLASS, GameDefs.TARGET_SENTRY, false);
        addBlock(0.79f, 0.52f, 0.04f, 0.28f, GameDefs.MAT_WOOD, -1, false);
        addBlock(0.84f, 0.58f, 0.04f, 0.22f, GameDefs.MAT_STONE, GameDefs.TARGET_SENTRY, false);
        addBlock(0.72f, 0.48f, 0.17f, 0.04f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.88f, 0.71f, 0.03f, 0.05f, GameDefs.MAT_WOOD, GameDefs.TARGET_BARREL, false);
    }

    private void buildIronCloud() {
        addBlock(0.7f, 0.62f, 0.045f, 0.18f, GameDefs.MAT_STONE, -1, true);
        addBlock(0.75f, 0.58f, 0.045f, 0.22f, GameDefs.MAT_METAL, GameDefs.TARGET_RELAY, false);
        addBlock(0.805f, 0.54f, 0.045f, 0.26f, GameDefs.MAT_STONE, -1, false);
        addBlock(0.86f, 0.5f, 0.045f, 0.3f, GameDefs.MAT_GLASS, GameDefs.TARGET_RELAY, false);
        addBlock(0.73f, 0.46f, 0.19f, 0.04f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.79f, 0.39f, 0.05f, 0.08f, GameDefs.MAT_GLASS, GameDefs.TARGET_CORE, false);
    }

    private void buildCrystalTempest() {
        addBlock(0.69f, 0.64f, 0.05f, 0.16f, GameDefs.MAT_WOOD, -1, true);
        addBlock(0.75f, 0.57f, 0.05f, 0.23f, GameDefs.MAT_GLASS, -1, false);
        addBlock(0.81f, 0.54f, 0.05f, 0.26f, GameDefs.MAT_GLASS, GameDefs.TARGET_CORE, false);
        addBlock(0.87f, 0.61f, 0.05f, 0.19f, GameDefs.MAT_STONE, -1, false);
        addBlock(0.72f, 0.49f, 0.2f, 0.04f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.84f, 0.42f, 0.04f, 0.08f, GameDefs.MAT_GLASS, GameDefs.TARGET_RELAY, false);
        addBlock(0.9f, 0.72f, 0.03f, 0.05f, GameDefs.MAT_WOOD, GameDefs.TARGET_BARREL, false);
    }

    private void buildSkyfallKeep() {
        addBlock(0.68f, 0.62f, 0.05f, 0.18f, GameDefs.MAT_STONE, -1, true);
        addBlock(0.74f, 0.54f, 0.05f, 0.26f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.8f, 0.48f, 0.05f, 0.32f, GameDefs.MAT_STONE, -1, false);
        addBlock(0.86f, 0.44f, 0.05f, 0.36f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.92f, 0.56f, 0.04f, 0.24f, GameDefs.MAT_GLASS, GameDefs.TARGET_CORE, false);
        addBlock(0.76f, 0.4f, 0.16f, 0.04f, GameDefs.MAT_METAL, -1, false);
        addBlock(0.72f, 0.33f, 0.24f, 0.04f, GameDefs.MAT_STONE, -1, false);
        addBlock(0.81f, 0.26f, 0.05f, 0.08f, GameDefs.MAT_GLASS, GameDefs.TARGET_RELAY, false);
        addBlock(0.89f, 0.26f, 0.05f, 0.08f, GameDefs.MAT_GLASS, GameDefs.TARGET_SENTRY, false);
    }

    private void addBlock(float x, float y, float w, float h, int material, int targetType, boolean anchor) {
        Block block = blocks[blockCount++];
        block.x = x * width;
        block.y = y * height;
        block.w = w * width;
        block.h = h * height;
        block.material = material;
        block.targetType = targetType;
        block.anchor = anchor;
        block.dynamic = false;
        block.alive = true;
        block.maxHp = material == GameDefs.MAT_WOOD ? 42f : material == GameDefs.MAT_GLASS ? 28f : material == GameDefs.MAT_STONE ? 70f : 96f;
        if (targetType == GameDefs.TARGET_CORE) {
            block.maxHp += 18f;
        } else if (targetType == GameDefs.TARGET_RELAY) {
            block.maxHp -= 4f;
        } else if (targetType == GameDefs.TARGET_BARREL) {
            block.maxHp = 22f;
        }
        block.hp = block.maxHp;
    }

    private static class Stage {
        final String name;
        final String objective;
        final int weather;
        final int birdCount;

        Stage(String name, String objective, int weather, int birdCount) {
            this.name = name;
            this.objective = objective;
            this.weather = weather;
            this.birdCount = birdCount;
        }
    }

    private static class Bird {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        boolean active;
        boolean skillUsed;
        int kind;
        int pierce;
        float shieldTime;
    }

    public static class Block {
        public float x;
        public float y;
        public float w;
        public float h;
        public float vx;
        public float vy;
        public float hp;
        public float maxHp;
        public int material;
        public int targetType;
        public boolean anchor;
        public boolean dynamic;
        public boolean alive;

        void reset() {
            x = 0f;
            y = 0f;
            w = 0f;
            h = 0f;
            vx = 0f;
            vy = 0f;
            hp = 0f;
            maxHp = 0f;
            material = 0;
            targetType = -1;
            anchor = false;
            dynamic = false;
            alive = false;
        }

        void copyFrom(Block other) {
            x = other.x;
            y = other.y;
            w = other.w;
            h = other.h;
            vx = other.vx;
            vy = other.vy;
            hp = other.hp;
            maxHp = other.maxHp;
            material = other.material;
            targetType = other.targetType;
            anchor = other.anchor;
            dynamic = other.dynamic;
            alive = other.alive;
        }
    }

    public static class RenderState {
        public final Block[] blocks = new Block[MAX_BLOCKS];
        public final float[] trajectoryX = new float[MAX_TRAJECTORY];
        public final float[] trajectoryY = new float[MAX_TRAJECTORY];
        public int width;
        public int height;
        public int state;
        public int levelIndex;
        public int blockCount;
        public int trajectoryCount;
        public int weather;
        public int birdsLeft;
        public float slingX;
        public float slingY;
        public float birdX;
        public float birdY;
        public float birdRadius;
        public int birdKind;
        public boolean birdActive;
        public boolean aiming;
        public float effectPulse;
        public float flash;
        public float cameraShake;
        public String toast = "";
        public float toastTime;
        public String currentBirdName = "";

        RenderState() {
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = new Block();
            }
        }
    }

    public static class HudSnapshot {
        public final int state;
        public final String chapterName;
        public final String stageName;
        public final String objective;
        public final String weatherName;
        public final int birdsLeft;
        public final int score;
        public final int stars;
        public final boolean birdActive;
        public final boolean resultWin;
        public final int levelIndex;
        public final int levelCount;
        public final String currentBird;
        public final String nextBird;
        public final String toast;
        public final String resultTitle;
        public final String resultBody;

        HudSnapshot(int state, String chapterName, String stageName, String objective, String weatherName, int birdsLeft, int score, int stars, boolean birdActive, boolean resultWin, int levelIndex, int levelCount, String currentBird, String nextBird, String toast, String resultTitle, String resultBody) {
            this.state = state;
            this.chapterName = chapterName;
            this.stageName = stageName;
            this.objective = objective;
            this.weatherName = weatherName;
            this.birdsLeft = birdsLeft;
            this.score = score;
            this.stars = stars;
            this.birdActive = birdActive;
            this.resultWin = resultWin;
            this.levelIndex = levelIndex;
            this.levelCount = levelCount;
            this.currentBird = currentBird;
            this.nextBird = nextBird;
            this.toast = toast;
            this.resultTitle = resultTitle;
            this.resultBody = resultBody;
        }
    }
}
