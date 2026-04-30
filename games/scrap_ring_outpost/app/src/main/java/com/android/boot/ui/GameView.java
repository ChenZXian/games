package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
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
    public static final int BUILD_BARRICADE = 0;
    public static final int BUILD_GUN_NEST = 1;
    public static final int BUILD_SPIKE_TRAP = 2;
    public static final int BUILD_FLAME_PIPE = 3;
    public static final int BUILD_ARC_LAMP = 4;
    public static final int BUILD_REPAIR_STATION = 5;

    public static final int TRAIT_SALVAGE = 0;
    public static final int TRAIT_FLARE = 1;
    public static final int TRAIT_WELDER = 2;

    public interface GameListener {
        void onHudChanged(HudSnapshot hud);
        void onRunEnded(boolean cleared, int nights, int breaches, int stars);
        void onAudioEvent(String key);
    }

    public static final class HudSnapshot {
        public final int scrap;
        public final int fuel;
        public final int parts;
        public final int night;
        public final int maxNight;
        public final String objective;
        public final String sectorName;
        public final int integrity;
        public final boolean focusReady;
        public final int commandCore;
        public final int selectedSector;

        public HudSnapshot(int scrap, int fuel, int parts, int night, int maxNight, String objective, String sectorName, int integrity, boolean focusReady, int commandCore, int selectedSector) {
            this.scrap = scrap;
            this.fuel = fuel;
            this.parts = parts;
            this.night = night;
            this.maxNight = maxNight;
            this.objective = objective;
            this.sectorName = sectorName;
            this.integrity = integrity;
            this.focusReady = focusReady;
            this.commandCore = commandCore;
            this.selectedSector = selectedSector;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF tempRect = new RectF();
    private final Path path = new Path();
    private final Random random = new Random(41);
    private final ArrayList<Zombie> zombies = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final HashMap<String, Bitmap> sprites = new HashMap<>();
    private final Sector[] sectors = new Sector[4];
    private GameListener listener;
    private GameState state = GameState.MENU;
    private boolean running;
    private boolean soundEnabled = true;
    private boolean prepPhase = true;
    private long lastTick;
    private int scrap;
    private int fuel;
    private int parts;
    private int currentNight;
    private final int maxNight = 4;
    private int commandCore;
    private int breaches;
    private int selectedSector;
    private int buildMode = BUILD_BARRICADE;
    private int trait = TRAIT_SALVAGE;
    private int pendingSpawns;
    private float spawnTimer;
    private float focusCooldown;
    private float prepTimer;
    private String objectiveText = "Prep the ring";
    private float centerX;
    private float centerY;
    private float outerRadius;
    private float innerRadius;

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

    public GameView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        setFocusable(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        sectors[0] = new Sector("North Gate", -90f, 0);
        sectors[1] = new Sector("East Gate", 0f, 1);
        sectors[2] = new Sector("South Gate", 90f, 2);
        sectors[3] = new Sector("West Gate", 180f, 3);
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
        prepPhase = true;
        zombies.clear();
        effects.clear();
        pendingSpawns = 0;
        focusCooldown = 0f;
        objectiveText = "Prep the ring";
        notifyHud();
        invalidate();
    }

    public void startGame() {
        resetRunValues();
        state = GameState.PLAYING;
        startLoop();
        notifyHud();
    }

    private void resetRunValues() {
        scrap = trait == TRAIT_SALVAGE ? 180 : 150;
        fuel = trait == TRAIT_FLARE ? 85 : 70;
        parts = trait == TRAIT_WELDER ? 80 : 65;
        currentNight = 0;
        commandCore = 100;
        breaches = 0;
        selectedSector = 0;
        buildMode = BUILD_BARRICADE;
        pendingSpawns = 0;
        spawnTimer = 0f;
        focusCooldown = 0f;
        prepTimer = 0f;
        prepPhase = true;
        objectiveText = "Fortify before nightfall";
        zombies.clear();
        effects.clear();
        for (Sector sector : sectors) {
            sector.reset();
        }
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

    public void setBuildMode(int buildMode) {
        this.buildMode = buildMode;
        notifyHud();
    }

    public void setTrait(int trait) {
        this.trait = trait;
    }

    public void selectSector(int sectorIndex) {
        if (sectorIndex >= 0 && sectorIndex < sectors.length) {
            selectedSector = sectorIndex;
            notifyHud();
            invalidate();
        }
    }

    public void beginNight() {
        if (state != GameState.PLAYING || !prepPhase) {
            return;
        }
        currentNight++;
        prepPhase = false;
        pendingSpawns = 8 + currentNight * 5;
        spawnTimer = 0.35f;
        objectiveText = "Hold until dawn";
        emitAudio("warning");
        notifyHud();
    }

    public void useFocusBurst() {
        if (state != GameState.PLAYING || focusCooldown > 0f || fuel < 18) {
            return;
        }
        fuel -= 18;
        focusCooldown = trait == TRAIT_FLARE ? 10f : 14f;
        sectors[selectedSector].focusBoost = 6f;
        effects.add(new Effect(sectors[selectedSector].angleDeg, 0.85f, color(R.color.cst_accent)));
        emitAudio("warning");
        notifyHud();
    }

    public void useFieldPatch() {
        if (state != GameState.PLAYING || parts < 18) {
            return;
        }
        parts -= 18;
        commandCore = Math.min(100, commandCore + (trait == TRAIT_WELDER ? 14 : 10));
        for (Sector sector : sectors) {
            sector.integrity = Math.min(100, sector.integrity + (trait == TRAIT_WELDER ? 12 : 8));
        }
        effects.add(new Effect(999f, 0.45f, color(R.color.cst_success)));
        emitAudio("repair");
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
        if (event.getAction() != MotionEvent.ACTION_DOWN || state != GameState.PLAYING) {
            return true;
        }
        updateBoardMetrics();
        float x = event.getX();
        float y = event.getY();
        int sectorIndex = angleToSector(x, y);
        if (sectorIndex >= 0) {
            selectedSector = sectorIndex;
            Slot slot = findSlotHit(sectors[sectorIndex], x, y);
            if (slot != null && prepPhase) {
                buildOrUpgrade(slot, sectors[sectorIndex]);
            }
            notifyHud();
            invalidate();
        }
        return true;
    }

    private void buildOrUpgrade(Slot slot, Sector sector) {
        if (slot.type == -1) {
            int scrapCost = buildCostScrap(buildMode);
            int fuelCost = buildCostFuel(buildMode);
            int partCost = buildCostParts(buildMode);
            if (scrap < scrapCost || fuel < fuelCost || parts < partCost) {
                objectiveText = "Need more salvage";
                notifyHud();
                return;
            }
            scrap -= scrapCost;
            fuel -= fuelCost;
            parts -= partCost;
            slot.type = buildMode;
            slot.level = 1;
            emitAudio("build");
            effects.add(new Effect(sector.angleDeg, 0.55f, moduleColor(buildMode)));
        } else if (slot.level < 3) {
            int level = slot.level;
            int scrapCost = 16 + level * 12;
            int fuelCost = (slot.type == BUILD_FLAME_PIPE || slot.type == BUILD_ARC_LAMP) ? 6 + level * 3 : 0;
            int partCost = 6 + level * 4;
            if (scrap < scrapCost || fuel < fuelCost || parts < partCost) {
                objectiveText = "Need more salvage";
                notifyHud();
                return;
            }
            scrap -= scrapCost;
            fuel -= fuelCost;
            parts -= partCost;
            slot.level++;
            emitAudio("upgrade");
            effects.add(new Effect(sector.angleDeg, 0.55f, color(R.color.cst_warning)));
        }
        notifyHud();
    }

    private void update(float dt) {
        if (focusCooldown > 0f) {
            focusCooldown -= dt;
        }
        if (prepPhase) {
            prepTimer += dt;
            if (prepTimer >= 1.2f) {
                prepTimer = 0f;
                if (trait == TRAIT_SALVAGE) {
                    scrap += 4;
                } else if (trait == TRAIT_FLARE) {
                    fuel += 2;
                } else {
                    parts += 2;
                }
            }
            for (Sector sector : sectors) {
                if (sector.focusBoost > 0f) {
                    sector.focusBoost -= dt;
                }
                updateSectorRecovery(sector, dt * 0.6f);
            }
            return;
        }
        updateSpawns(dt);
        updateModules(dt);
        updateZombies(dt);
        updateEffects(dt);
        if (pendingSpawns <= 0 && zombies.isEmpty()) {
            if (currentNight >= maxNight) {
                finishRun(true);
            } else {
                prepPhase = true;
                objectiveText = "Rebuild before the next siren";
                rewardAfterNight();
                notifyHud();
            }
        }
    }

    private void updateSpawns(float dt) {
        spawnTimer -= dt;
        if (spawnTimer <= 0f && pendingSpawns > 0) {
            spawnZombie();
            pendingSpawns--;
            spawnTimer = Math.max(0.24f, 0.92f - currentNight * 0.08f);
        }
    }

    private void spawnZombie() {
        Sector sector = pickSpawnSector();
        Zombie zombie = new Zombie();
        zombie.sectorIndex = sector.index;
        zombie.progress = 0f;
        zombie.speed = 0.085f + currentNight * 0.008f;
        zombie.maxHp = 18f + currentNight * 8f;
        zombie.damage = 7 + currentNight;
        zombie.reward = 6 + currentNight;
        zombie.type = 0;
        if (pendingSpawns % 5 == 0) {
            zombie.type = 1;
            zombie.maxHp *= 0.75f;
            zombie.speed *= 1.45f;
            zombie.reward += 2;
        }
        if (pendingSpawns % 7 == 0) {
            zombie.type = 2;
            zombie.maxHp *= 1.7f;
            zombie.speed *= 0.72f;
            zombie.damage += 4;
            zombie.reward += 8;
        }
        if (currentNight >= 3 && pendingSpawns % 11 == 0) {
            zombie.type = 3;
            zombie.maxHp *= 2.6f;
            zombie.speed *= 0.82f;
            zombie.damage += 6;
            zombie.reward += 14;
        }
        zombie.hp = zombie.maxHp;
        zombies.add(zombie);
    }

    private Sector pickSpawnSector() {
        Sector best = sectors[random.nextInt(sectors.length)];
        for (Sector sector : sectors) {
            if (sector.integrity < best.integrity) {
                best = sector;
            }
        }
        if (random.nextFloat() < 0.35f) {
            best = sectors[random.nextInt(sectors.length)];
        }
        return best;
    }

    private void updateModules(float dt) {
        for (Sector sector : sectors) {
            if (sector.focusBoost > 0f) {
                sector.focusBoost -= dt;
            }
            updateSectorRecovery(sector, dt * 0.15f);
            for (Slot slot : sector.slots) {
                if (slot.type == -1) {
                    continue;
                }
                if (slot.cooldown > 0f) {
                    slot.cooldown -= dt;
                }
                if (slot.type == BUILD_REPAIR_STATION) {
                    sector.integrity = Math.min(100, sector.integrity + dt * (2.2f + slot.level * 1.2f));
                    continue;
                }
                Zombie target = findTarget(sector.index);
                if (target != null && slot.cooldown <= 0f) {
                    fireModule(sector, slot, target);
                }
            }
        }
    }

    private void updateSectorRecovery(Sector sector, float dt) {
        if (trait == TRAIT_WELDER) {
            sector.integrity = Math.min(100, sector.integrity + dt * 1.3f);
        } else {
            sector.integrity = Math.min(100, sector.integrity + dt * 0.5f);
        }
    }

    private Zombie findTarget(int sectorIndex) {
        Zombie best = null;
        float bestProgress = -1f;
        for (Zombie zombie : zombies) {
            if (zombie.sectorIndex != sectorIndex) {
                continue;
            }
            if (zombie.progress > bestProgress) {
                best = zombie;
                bestProgress = zombie.progress;
            }
        }
        return best;
    }

    private void fireModule(Sector sector, Slot slot, Zombie target) {
        float damage = moduleDamage(slot.type, slot.level);
        if (sector.index == selectedSector) {
            damage *= 1.12f;
        }
        if (sector.focusBoost > 0f) {
            damage *= 1.28f;
        }
        target.hp -= damage;
        if (slot.type == BUILD_SPIKE_TRAP) {
            target.slow = Math.max(target.slow, 1.4f);
        } else if (slot.type == BUILD_FLAME_PIPE) {
            target.burn = Math.max(target.burn, 2.2f);
        } else if (slot.type == BUILD_ARC_LAMP) {
            target.stun = Math.max(target.stun, 0.9f);
        }
        slot.cooldown = moduleCooldown(slot.type, slot.level, sector.focusBoost > 0f);
        emitAudio("attack");
        effects.add(new Effect(sector.angleDeg, 0.74f - target.progress * 0.36f, moduleColor(slot.type)));
    }

    private void updateZombies(float dt) {
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            if (zombie.stun > 0f) {
                zombie.stun -= dt;
            } else {
                float speed = zombie.speed;
                if (zombie.slow > 0f) {
                    zombie.slow -= dt;
                    speed *= 0.58f;
                }
                zombie.progress += speed * dt;
            }
            if (zombie.burn > 0f) {
                zombie.burn -= dt;
                zombie.hp -= dt * 7f;
            }
            if (zombie.hp <= 0f) {
                scrap += zombie.reward;
                if (zombie.type >= 2) {
                    parts += 2;
                    fuel += 1;
                }
                emitAudio("collect");
                effects.add(new Effect(sectors[zombie.sectorIndex].angleDeg, 0.72f - zombie.progress * 0.32f, color(R.color.cst_success)));
                zombies.remove(i);
                continue;
            }
            if (zombie.progress >= 1f) {
                Sector sector = sectors[zombie.sectorIndex];
                sector.integrity -= zombie.damage;
                breaches++;
                effects.add(new Effect(sector.angleDeg, 0.38f, color(R.color.cst_danger)));
                emitAudio("warning");
                if (sector.integrity <= 0) {
                    commandCore -= Math.max(8, zombie.damage + 4);
                    sector.integrity = 28;
                }
                zombies.remove(i);
                if (commandCore <= 0) {
                    commandCore = 0;
                    finishRun(false);
                    return;
                }
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

    private void rewardAfterNight() {
        scrap += 28 + currentNight * 8;
        fuel += 10 + currentNight * 4;
        parts += 8 + currentNight * 4;
        if (trait == TRAIT_SALVAGE) {
            scrap += 18;
        } else if (trait == TRAIT_FLARE) {
            fuel += 10;
        } else {
            parts += 10;
        }
    }

    private void finishRun(boolean cleared) {
        state = cleared ? GameState.STAGE_CLEAR : GameState.GAME_OVER;
        objectiveText = cleared ? "Outpost held" : "The ring collapsed";
        notifyHud();
        if (listener != null) {
            listener.onRunEnded(cleared, currentNight, breaches, calculateStars(cleared));
        }
    }

    private int calculateStars(boolean cleared) {
        if (!cleared) {
            return 0;
        }
        int stars = 1;
        if (commandCore >= 70) {
            stars++;
        }
        if (breaches <= 8) {
            stars++;
        }
        return stars;
    }

    private void emitAudio(String key) {
        if (listener != null && soundEnabled) {
            listener.onAudioEvent(key);
        }
    }

    private void notifyHud() {
        if (listener == null) {
            return;
        }
        Sector sector = sectors[selectedSector];
        String phase = prepPhase ? "Prep the ring" : "Night assault in progress";
        HudSnapshot snapshot = new HudSnapshot(
                scrap,
                fuel,
                parts,
                currentNight,
                maxNight,
                objectiveText.length() > 0 ? objectiveText : phase,
                sector.name,
                (int) sector.integrity,
                focusCooldown <= 0f,
                commandCore,
                selectedSector
        );
        listener.onHudChanged(snapshot);
    }

    private int buildCostScrap(int type) {
        if (type == BUILD_BARRICADE) {
            return 18;
        }
        if (type == BUILD_SPIKE_TRAP) {
            return 24;
        }
        if (type == BUILD_REPAIR_STATION) {
            return 28;
        }
        return 32;
    }

    private int buildCostFuel(int type) {
        if (type == BUILD_FLAME_PIPE) {
            return 12;
        }
        if (type == BUILD_ARC_LAMP) {
            return 9;
        }
        return 0;
    }

    private int buildCostParts(int type) {
        if (type == BUILD_BARRICADE) {
            return 2;
        }
        if (type == BUILD_SPIKE_TRAP) {
            return 4;
        }
        return 8;
    }

    private float moduleDamage(int type, int level) {
        if (type == BUILD_GUN_NEST) {
            return 8f + level * 4f;
        }
        if (type == BUILD_SPIKE_TRAP) {
            return 10f + level * 5f;
        }
        if (type == BUILD_FLAME_PIPE) {
            return 6f + level * 3f;
        }
        if (type == BUILD_ARC_LAMP) {
            return 5f + level * 2.5f;
        }
        if (type == BUILD_BARRICADE) {
            return 3f + level * 2f;
        }
        return 0f;
    }

    private float moduleCooldown(int type, int level, boolean focused) {
        float base;
        if (type == BUILD_GUN_NEST) {
            base = 0.52f;
        } else if (type == BUILD_SPIKE_TRAP) {
            base = 1.2f;
        } else if (type == BUILD_FLAME_PIPE) {
            base = 0.88f;
        } else if (type == BUILD_ARC_LAMP) {
            base = 1.4f;
        } else {
            base = 1.05f;
        }
        base -= (level - 1) * 0.08f;
        if (focused) {
            base *= 0.74f;
        }
        return Math.max(0.18f, base);
    }

    private int moduleColor(int type) {
        if (type == BUILD_GUN_NEST) {
            return color(R.color.cst_accent_2);
        }
        if (type == BUILD_SPIKE_TRAP) {
            return color(R.color.cst_warning);
        }
        if (type == BUILD_FLAME_PIPE) {
            return color(R.color.cst_danger);
        }
        if (type == BUILD_ARC_LAMP) {
            return color(R.color.cst_success);
        }
        if (type == BUILD_REPAIR_STATION) {
            return color(R.color.cst_success);
        }
        return color(R.color.cst_panel_stroke);
    }

    private int angleToSector(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < innerRadius * 0.65f || dist > outerRadius * 1.18f) {
            return -1;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < -45f && angle >= -135f) {
            return 0;
        }
        if (angle >= -45f && angle < 45f) {
            return 1;
        }
        if (angle >= 45f && angle < 135f) {
            return 2;
        }
        return 3;
    }

    private Slot findSlotHit(Sector sector, float x, float y) {
        for (Slot slot : sector.slots) {
            float[] point = slotPosition(sector, slot.index);
            float radius = outerRadius * 0.12f;
            float dx = x - point[0];
            float dy = y - point[1];
            if (dx * dx + dy * dy <= radius * radius) {
                return slot;
            }
        }
        return null;
    }

    private void updateBoardMetrics() {
        centerX = getWidth() * 0.47f;
        centerY = getHeight() * 0.49f;
        outerRadius = Math.min(getWidth(), getHeight()) * 0.28f;
        innerRadius = outerRadius * 0.53f;
    }

    private float[] slotPosition(Sector sector, int slotIndex) {
        float baseAngle = sector.angleDeg;
        float offset = (slotIndex - 1.5f) * 18f;
        float angle = (float) Math.toRadians(baseAngle + offset);
        float radius = outerRadius * 0.86f;
        return new float[]{
                centerX + (float) Math.cos(angle) * radius,
                centerY + (float) Math.sin(angle) * radius
        };
    }

    private float[] zombiePosition(Zombie zombie) {
        Sector sector = sectors[zombie.sectorIndex];
        float angle = (float) Math.toRadians(sector.angleDeg);
        float radius = outerRadius + outerRadius * 0.18f - zombie.progress * outerRadius * 0.54f;
        if (radius < innerRadius * 0.96f) {
            radius = innerRadius * 0.96f;
        }
        return new float[]{
                centerX + (float) Math.cos(angle) * radius,
                centerY + (float) Math.sin(angle) * radius
        };
    }

    private void loadSprites() {
        loadSprite("gate", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile179.png");
        loadSprite("core", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile180.png");
        loadSprite("barricade", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile134.png");
        loadSprite("gun_nest", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile249.png");
        loadSprite("spike", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile136.png");
        loadSprite("flame", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile250.png");
        loadSprite("arc", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile245.png");
        loadSprite("repair", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile247.png");
        loadSprite("enemy_basic_a", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_stand.png");
        loadSprite("enemy_basic_b", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_hold.png");
        loadSprite("enemy_fast_a", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 2/zoimbie2_stand.png");
        loadSprite("enemy_fast_b", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 2/zoimbie2_hold.png");
        loadSprite("enemy_brute_a", "game_art/kenney_top_down_shooter/assets/PNG/Robot 1/robot1_stand.png");
        loadSprite("enemy_brute_b", "game_art/kenney_top_down_shooter/assets/PNG/Robot 1/robot1_machine.png");
        loadSprite("enemy_spitter_a", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_machine.png");
        loadSprite("enemy_spitter_b", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_hold.png");
        loadSprite("yard", "game_art/kenney_top_down_shooter/assets/PNG/Tiles/tile_96.png");
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

    private boolean drawSprite(Canvas canvas, String key, float cx, float cy, float maxWidth, float maxHeight, float rotation) {
        Bitmap bitmap = sprites.get(key);
        if (bitmap == null) {
            return false;
        }
        float scale = Math.min(maxWidth / bitmap.getWidth(), maxHeight / bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        tempRect.set(cx - width * 0.5f, cy - height * 0.5f, cx + width * 0.5f, cy + height * 0.5f);
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        canvas.drawBitmap(bitmap, null, tempRect, paint);
        canvas.restore();
        return true;
    }

    private int color(int resId) {
        return getResources().getColor(resId, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateBoardMetrics();
        drawBoard(canvas);
        drawRing(canvas);
        drawModules(canvas);
        drawZombies(canvas);
        drawEffects(canvas);
        if (prepPhase && state == GameState.PLAYING) {
            drawPrepHint(canvas);
        }
    }

    private void drawBoard(Canvas canvas) {
        canvas.drawColor(color(R.color.cst_bg_main));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_bg_alt));
        rect.set(centerX - outerRadius * 1.55f, centerY - outerRadius * 1.15f, centerX + outerRadius * 1.55f, centerY + outerRadius * 1.15f);
        canvas.drawRoundRect(rect, outerRadius * 0.16f, outerRadius * 0.16f, paint);
        drawSprite(canvas, "yard", centerX, centerY, outerRadius * 2.45f, outerRadius * 2.45f, 0f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_game_oil));
        canvas.drawCircle(centerX, centerY, outerRadius, paint);
        paint.setColor(color(R.color.cst_game_ash));
        canvas.drawCircle(centerX, centerY, innerRadius, paint);
    }

    private void drawRing(Canvas canvas) {
        for (Sector sector : sectors) {
            float start = sector.angleDeg - 36f;
            float sweep = 72f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(outerRadius * 0.22f);
            paint.setColor(sector.index == selectedSector ? color(R.color.cst_accent) : color(R.color.cst_panel_stroke));
            rect.set(centerX - outerRadius * 0.88f, centerY - outerRadius * 0.88f, centerX + outerRadius * 0.88f, centerY + outerRadius * 0.88f);
            canvas.drawArc(rect, start, sweep, false, paint);
            float gateAngle = (float) Math.toRadians(sector.angleDeg);
            float gateX = centerX + (float) Math.cos(gateAngle) * innerRadius;
            float gateY = centerY + (float) Math.sin(gateAngle) * innerRadius;
            if (!drawSprite(canvas, "gate", gateX, gateY, outerRadius * 0.33f, outerRadius * 0.33f, sector.angleDeg + 90f)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_game_scrap));
                canvas.drawCircle(gateX, gateY, outerRadius * 0.11f, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.cst_danger));
            rect.set(gateX - outerRadius * 0.12f, gateY + outerRadius * 0.13f, gateX + outerRadius * 0.12f, gateY + outerRadius * 0.17f);
            canvas.drawRoundRect(rect, 6f, 6f, paint);
            paint.setColor(color(R.color.cst_success));
            rect.set(gateX - outerRadius * 0.12f, gateY + outerRadius * 0.13f, gateX - outerRadius * 0.12f + outerRadius * 0.24f * (sector.integrity / 100f), gateY + outerRadius * 0.17f);
            canvas.drawRoundRect(rect, 6f, 6f, paint);
        }
        if (!drawSprite(canvas, "core", centerX, centerY, innerRadius * 1.1f, innerRadius * 1.1f, 0f)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.cst_warning));
            canvas.drawCircle(centerX, centerY, innerRadius * 0.34f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(outerRadius * 0.028f);
        paint.setColor(color(R.color.cst_warning));
        canvas.drawCircle(centerX, centerY, innerRadius * 0.42f + commandCore * 0.0022f, paint);
    }

    private void drawModules(Canvas canvas) {
        for (Sector sector : sectors) {
            for (Slot slot : sector.slots) {
                float[] point = slotPosition(sector, slot.index);
                if (slot.type == -1) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(outerRadius * 0.025f);
                    paint.setColor(sector.index == selectedSector ? color(R.color.cst_warning) : color(R.color.cst_panel_stroke));
                    canvas.drawCircle(point[0], point[1], outerRadius * 0.09f, paint);
                    continue;
                }
                if (!drawSprite(canvas, moduleSprite(slot.type), point[0], point[1], outerRadius * 0.22f, outerRadius * 0.22f, sector.angleDeg + 90f)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(moduleColor(slot.type));
                    canvas.drawCircle(point[0], point[1], outerRadius * 0.09f, paint);
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_text_primary));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(outerRadius * 0.09f);
                canvas.drawText(String.valueOf(slot.level), point[0], point[1] + outerRadius * 0.03f, paint);
            }
        }
    }

    private void drawZombies(Canvas canvas) {
        float time = (System.nanoTime() / 100000000L) % 2;
        for (Zombie zombie : zombies) {
            float[] point = zombiePosition(zombie);
            String spriteKey = zombieSprite(zombie.type, time == 0);
            if (!drawSprite(canvas, spriteKey, point[0], point[1], outerRadius * 0.23f, outerRadius * 0.23f, sectors[zombie.sectorIndex].angleDeg + 90f)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color(R.color.cst_danger));
                canvas.drawCircle(point[0], point[1], outerRadius * 0.08f, paint);
            }
            paint.setColor(color(R.color.cst_danger));
            rect.set(point[0] - outerRadius * 0.10f, point[1] - outerRadius * 0.14f, point[0] + outerRadius * 0.10f, point[1] - outerRadius * 0.10f);
            canvas.drawRect(rect, paint);
            paint.setColor(color(R.color.cst_success));
            rect.set(point[0] - outerRadius * 0.10f, point[1] - outerRadius * 0.14f, point[0] - outerRadius * 0.10f + outerRadius * 0.20f * (zombie.hp / zombie.maxHp), point[1] - outerRadius * 0.10f);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawEffects(Canvas canvas) {
        for (Effect effect : effects) {
            float t = effect.life / effect.maxLife;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(outerRadius * 0.02f * t);
            paint.setColor(effect.color);
            if (effect.angleDeg == 999f) {
                canvas.drawCircle(centerX, centerY, innerRadius * (0.8f + (1f - t) * 0.4f), paint);
            } else {
                float angle = (float) Math.toRadians(effect.angleDeg);
                float radius = outerRadius * effect.radiusFactor;
                float x = centerX + (float) Math.cos(angle) * radius;
                float y = centerY + (float) Math.sin(angle) * radius;
                canvas.drawCircle(x, y, outerRadius * (0.06f + (1f - t) * 0.08f), paint);
            }
        }
    }

    private void drawPrepHint(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_text_primary));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(outerRadius * 0.12f);
        String text = String.format(Locale.US, "Prep Phase  Tap slots in %s", sectors[selectedSector].name);
        canvas.drawText(text, centerX, centerY - outerRadius * 1.03f, paint);
    }

    private String moduleSprite(int type) {
        if (type == BUILD_GUN_NEST) {
            return "gun_nest";
        }
        if (type == BUILD_SPIKE_TRAP) {
            return "spike";
        }
        if (type == BUILD_FLAME_PIPE) {
            return "flame";
        }
        if (type == BUILD_ARC_LAMP) {
            return "arc";
        }
        if (type == BUILD_REPAIR_STATION) {
            return "repair";
        }
        return "barricade";
    }

    private String zombieSprite(int type, boolean frameA) {
        if (type == 1) {
            return frameA ? "enemy_fast_a" : "enemy_fast_b";
        }
        if (type == 2) {
            return frameA ? "enemy_brute_a" : "enemy_brute_b";
        }
        if (type == 3) {
            return frameA ? "enemy_spitter_a" : "enemy_spitter_b";
        }
        return frameA ? "enemy_basic_a" : "enemy_basic_b";
    }

    private static final class Sector {
        final String name;
        final float angleDeg;
        final Slot[] slots = new Slot[4];
        int index;
        float integrity = 100f;
        float focusBoost;

        Sector(String name, float angleDeg, int index) {
            this.name = name;
            this.angleDeg = angleDeg;
            this.index = index;
            for (int i = 0; i < slots.length; i++) {
                slots[i] = new Slot(i);
            }
        }

        void reset() {
            integrity = 100f;
            focusBoost = 0f;
            for (Slot slot : slots) {
                slot.type = -1;
                slot.level = 0;
                slot.cooldown = 0f;
            }
        }
    }

    private static final class Slot {
        final int index;
        int type = -1;
        int level;
        float cooldown;

        Slot(int index) {
            this.index = index;
        }
    }

    private static final class Zombie {
        int sectorIndex;
        int type;
        int damage;
        int reward;
        float progress;
        float hp;
        float maxHp;
        float speed;
        float slow;
        float stun;
        float burn;
    }

    private static final class Effect {
        final float angleDeg;
        final float radiusFactor;
        final int color;
        float life = 0.32f;
        final float maxLife = 0.32f;

        Effect(float angleDeg, float radiusFactor, int color) {
            this.angleDeg = angleDeg;
            this.radiusFactor = radiusFactor;
            this.color = color;
        }
    }
}
