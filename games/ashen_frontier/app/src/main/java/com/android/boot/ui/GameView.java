package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.SoundController;
import com.android.boot.core.GameLoop;
import com.android.boot.core.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, GameLoop.Callback {
    public interface Listener {
        void onUiModelChanged();
    }

    public static final class HudSnapshot {
        public final String healthText;
        public final String infectionText;
        public final String fuelText;
        public final String lootText;
        public final String survivorText;
        public final String regionText;
        public final String objectiveText;
        public final String weaponText;
        public final String ammoText;
        public final String utilityText;
        public final String statusText;
        public final String pauseText;

        public HudSnapshot(
                String healthText,
                String infectionText,
                String fuelText,
                String lootText,
                String survivorText,
                String regionText,
                String objectiveText,
                String weaponText,
                String ammoText,
                String utilityText,
                String statusText,
                String pauseText) {
            this.healthText = healthText;
            this.infectionText = infectionText;
            this.fuelText = fuelText;
            this.lootText = lootText;
            this.survivorText = survivorText;
            this.regionText = regionText;
            this.objectiveText = objectiveText;
            this.weaponText = weaponText;
            this.ammoText = ammoText;
            this.utilityText = utilityText;
            this.statusText = statusText;
            this.pauseText = pauseText;
        }
    }

    public static final class SafehouseSnapshot {
        public final String summaryText;
        public final String hintText;

        public SafehouseSnapshot(String summaryText, String hintText) {
            this.summaryText = summaryText;
            this.hintText = hintText;
        }
    }

    private static final float MOVE_PAD_RADIUS = 150f;
    private static final float AIM_PAD_RADIUS = 160f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spritePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF tmpRect = new RectF();
    private final Matrix spriteMatrix = new Matrix();
    private final Random random = new Random(47L);
    private final SoundController soundController = new SoundController();
    private final RegionDefinition[] regions = buildRegions();
    private final PlayerRig player = new PlayerRig();
    private final SafehouseState safehouse = new SafehouseState();
    private final List<ZombieUnit> zombies = new ArrayList<>();
    private final List<ProjectileEntity> projectiles = new ArrayList<>();
    private final List<ResourceDrop> drops = new ArrayList<>();
    private final List<RescueTarget> rescueTargets = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<BurnZone> burnZones = new ArrayList<>();
    private final RectF relayZone = new RectF();
    private final RectF extractionZone = new RectF();
    private GameLoop loop;
    private Listener listener;
    private GameState state = GameState.MENU;
    private HudSnapshot hudSnapshot = createIdleHud();
    private SafehouseSnapshot safehouseSnapshot = createSafehouseSnapshot();
    private RegionDefinition currentRegion;
    private int currentRegionIndex = -1;
    private int activeMovePointerId = -1;
    private int activeAimPointerId = -1;
    private float moveOriginX;
    private float moveOriginY;
    private float aimOriginX;
    private float aimOriginY;
    private float moveInputX;
    private float moveInputY;
    private float aimInputX = 1f;
    private float aimInputY;
    private boolean triggerHeld;
    private boolean reloadQueued;
    private boolean healQueued;
    private boolean interactQueued;
    private boolean bikeToggleQueued;
    private boolean utilityQueued;
    private boolean switchWeaponQueued;
    private boolean relaySecured;
    private float cameraX;
    private float cameraY;
    private float worldWidth = 2600f;
    private float worldHeight = 1800f;
    private long lastUiDispatchAt;
    private String resultTitle = "Ashen Frontier";
    private String resultBody = "Secure the relay, escort survivors, and extract before the infected ring closes.";
    private Bitmap groundBitmap;
    private Bitmap altGroundBitmap;
    private Bitmap obstacleBitmap;
    private Bitmap towerBitmap;
    private Bitmap playerStandBitmap;
    private Bitmap playerGunBitmap;
    private Bitmap playerMachineBitmap;
    private Bitmap playerReloadBitmap;
    private Bitmap zombieStandBitmap;
    private Bitmap zombieAttackBitmap;
    private Bitmap survivorBitmap;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        loadArt();
        updateSnapshots();
    }

    public synchronized void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized GameState getState() {
        return state;
    }

    public synchronized HudSnapshot getHudSnapshot() {
        return hudSnapshot;
    }

    public synchronized SafehouseSnapshot getSafehouseSnapshot() {
        return safehouseSnapshot;
    }

    public synchronized String getResultTitle() {
        return resultTitle;
    }

    public synchronized String getResultBody() {
        return resultBody;
    }

    public synchronized boolean isMuted() {
        return soundController.isMuted();
    }

    public synchronized void setMuted(boolean muted) {
        soundController.setMuted(muted);
        dispatchUiNow();
    }

    public synchronized int getRegionCount() {
        return regions.length;
    }

    public synchronized String getRegionLabel(int index) {
        RegionDefinition region = regions[index];
        String suffix = safehouse.cleared[index] ? "  CLEAR" : (canEnterRegion(index) ? "  OPEN" : "  LOCK");
        return String.format(Locale.US, "%02d  %s\nT%d  %s", index + 1, region.name, region.threat, suffix);
    }

    public synchronized boolean canEnterRegion(int index) {
        return index < safehouse.unlockedRegions;
    }

    public synchronized void openWorldMap() {
        if (state != GameState.PLAYING && state != GameState.PAUSED) {
            state = GameState.WORLD_MAP;
            updateSnapshots();
            dispatchUiNow();
        }
    }

    public synchronized void openSafehouse() {
        if (state != GameState.PLAYING) {
            state = GameState.SAFEHOUSE;
            updateSnapshots();
            dispatchUiNow();
        }
    }

    public synchronized void returnToMenu() {
        state = GameState.MENU;
        currentRegion = null;
        currentRegionIndex = -1;
        triggerHeld = false;
        activeAimPointerId = -1;
        activeMovePointerId = -1;
        moveInputX = 0f;
        moveInputY = 0f;
        updateSnapshots();
        dispatchUiNow();
    }

    public synchronized void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            updateSnapshots();
            dispatchUiNow();
        }
    }

    public synchronized void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            updateSnapshots();
            dispatchUiNow();
        }
    }

    public synchronized void retryRegion() {
        if (currentRegionIndex >= 0) {
            startRegion(currentRegionIndex);
        } else {
            openWorldMap();
        }
    }

    public synchronized void triggerReload() {
        reloadQueued = true;
    }

    public synchronized void triggerHeal() {
        healQueued = true;
    }

    public synchronized void triggerInteract() {
        interactQueued = true;
    }

    public synchronized void triggerBikeToggle() {
        bikeToggleQueued = true;
    }

    public synchronized void triggerUtility() {
        utilityQueued = true;
    }

    public synchronized void triggerSwitchWeapon() {
        switchWeaponQueued = true;
    }

    public synchronized void purchaseUpgrade(String id) {
        if ("workshop".equals(id)) {
            int cost = 90 + safehouse.workshopLevel * 55;
            if (safehouse.scrap >= cost) {
                safehouse.scrap -= cost;
                safehouse.workshopLevel++;
                if (safehouse.workshopLevel >= 2) {
                    safehouse.unlockedWeapons.add(WeaponType.SHOTGUN);
                }
                if (safehouse.workshopLevel >= 3) {
                    safehouse.unlockedWeapons.add(WeaponType.SMG);
                }
                if (safehouse.workshopLevel >= 4) {
                    safehouse.unlockedWeapons.add(WeaponType.RIFLE);
                }
                if (safehouse.workshopLevel >= 5) {
                    safehouse.unlockedWeapons.add(WeaponType.CROSSBOW);
                }
            }
        } else if ("infirmary".equals(id)) {
            int cost = 70 + safehouse.infirmaryLevel * 45;
            if (safehouse.scrap >= cost) {
                safehouse.scrap -= cost;
                safehouse.infirmaryLevel++;
                safehouse.medicine += 1;
            }
        } else if ("garage".equals(id)) {
            int cost = 80 + safehouse.garageLevel * 50;
            if (safehouse.scrap >= cost) {
                safehouse.scrap -= cost;
                safehouse.garageLevel++;
                safehouse.fuel += 14;
                safehouse.unlockedRegions = Math.min(regions.length, Math.max(safehouse.unlockedRegions, 3 + safehouse.garageLevel));
            }
        }
        state = GameState.SAFEHOUSE;
        updateSnapshots();
        dispatchUiNow();
    }

    public synchronized void startRegion(int index) {
        if (!canEnterRegion(index)) {
            return;
        }
        currentRegionIndex = index;
        currentRegion = regions[index];
        state = GameState.PLAYING;
        resultTitle = currentRegion.name;
        resultBody = "Secure the relay, extract survivors, and outrun infected noise swarms.";
        resetMission();
        updateSnapshots();
        dispatchUiNow();
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

    private synchronized void startLoop() {
        if (loop != null) {
            return;
        }
        loop = new GameLoop(this);
        loop.start();
    }

    private synchronized void stopLoop() {
        if (loop == null) {
            return;
        }
        loop.shutdown();
        loop = null;
    }

    @Override
    public synchronized void step(float dt) {
        if (state == GameState.PLAYING) {
            updatePlaying(dt);
        }
        drawFrame();
        maybeDispatchUi();
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        if (state != GameState.PLAYING) {
            return true;
        }
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            handlePointerDown(pointerId, event.getX(index), event.getY(index));
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                handlePointerMove(event.getPointerId(i), event.getX(i), event.getY(i));
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            handlePointerUp(pointerId);
        }
        return true;
    }

    private void handlePointerDown(int pointerId, float x, float y) {
        if (x <= getWidth() * 0.45f && activeMovePointerId == -1) {
            activeMovePointerId = pointerId;
            moveOriginX = x;
            moveOriginY = y;
            moveInputX = 0f;
            moveInputY = 0f;
        } else if (activeAimPointerId == -1) {
            activeAimPointerId = pointerId;
            aimOriginX = x;
            aimOriginY = y;
            triggerHeld = true;
        }
    }

    private void handlePointerMove(int pointerId, float x, float y) {
        if (pointerId == activeMovePointerId) {
            float dx = x - moveOriginX;
            float dy = y - moveOriginY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > MOVE_PAD_RADIUS) {
                dx = dx / len * MOVE_PAD_RADIUS;
                dy = dy / len * MOVE_PAD_RADIUS;
            }
            moveInputX = dx / MOVE_PAD_RADIUS;
            moveInputY = dy / MOVE_PAD_RADIUS;
        } else if (pointerId == activeAimPointerId) {
            float dx = x - aimOriginX;
            float dy = y - aimOriginY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 10f) {
                triggerHeld = false;
                return;
            }
            if (len > AIM_PAD_RADIUS) {
                dx = dx / len * AIM_PAD_RADIUS;
                dy = dy / len * AIM_PAD_RADIUS;
                len = AIM_PAD_RADIUS;
            }
            aimInputX = dx / len;
            aimInputY = dy / len;
            triggerHeld = true;
        }
    }

    private void handlePointerUp(int pointerId) {
        if (pointerId == activeMovePointerId) {
            activeMovePointerId = -1;
            moveInputX = 0f;
            moveInputY = 0f;
        }
        if (pointerId == activeAimPointerId) {
            activeAimPointerId = -1;
            triggerHeld = false;
        }
    }

    private void resetMission() {
        zombies.clear();
        projectiles.clear();
        drops.clear();
        rescueTargets.clear();
        obstacles.clear();
        burnZones.clear();
        relaySecured = false;
        moveInputX = 0f;
        moveInputY = 0f;
        aimInputX = 1f;
        aimInputY = 0f;
        activeAimPointerId = -1;
        activeMovePointerId = -1;
        triggerHeld = false;
        player.resetForMission(currentRegion, safehouse);
        worldWidth = currentRegion.worldWidth;
        worldHeight = currentRegion.worldHeight;
        relayZone.set(worldWidth - 480f, worldHeight * 0.22f, worldWidth - 280f, worldHeight * 0.22f + 180f);
        extractionZone.set(120f, worldHeight - 280f, 330f, worldHeight - 90f);
        buildMissionTerrain();
        spawnMissionUnits();
        cameraX = Math.max(0f, player.x - getWidth() * 0.5f);
        cameraY = Math.max(0f, player.y - getHeight() * 0.5f);
    }

    private void buildMissionTerrain() {
        float laneY = worldHeight * 0.54f;
        for (int i = 0; i < 7; i++) {
            float width = 150f + random.nextInt(180);
            float height = 110f + random.nextInt(120);
            float x = 420f + random.nextInt((int) (worldWidth - 900f));
            float y = 170f + random.nextInt((int) (worldHeight - 420f));
            if (RectF.intersects(new RectF(x, y, x + width, y + height), relayZone)) {
                continue;
            }
            if (x < 420f && y > worldHeight - 360f) {
                continue;
            }
            obstacles.add(new Obstacle(x, y, width, height));
        }
        obstacles.add(new Obstacle(worldWidth - 650f, laneY - 210f, 210f, 160f));
        obstacles.add(new Obstacle(worldWidth - 740f, laneY + 30f, 280f, 160f));
        obstacles.add(new Obstacle(worldWidth * 0.45f, worldHeight * 0.35f, 220f, 150f));
        obstacles.add(new Obstacle(worldWidth * 0.58f, worldHeight * 0.68f, 180f, 180f));
        for (int i = 0; i < 8; i++) {
            float x = 260f + i * 260f;
            float y = laneY + (i % 2 == 0 ? -30f : 30f);
            drops.add(new ResourceDrop(x, y, LootType.SCRAP, 12 + currentRegion.threat * 2));
        }
        drops.add(new ResourceDrop(worldWidth * 0.62f, worldHeight * 0.42f, LootType.FUEL, 8));
        drops.add(new ResourceDrop(worldWidth * 0.66f, worldHeight * 0.75f, LootType.MEDICINE, 1));
        drops.add(new ResourceDrop(worldWidth - 560f, worldHeight * 0.28f, LootType.AMMO, 18));
    }

    private void spawnMissionUnits() {
        for (int i = 0; i < currentRegion.requiredRescues; i++) {
            float x = worldWidth * (0.46f + 0.12f * i);
            float y = worldHeight * (0.28f + 0.18f * i);
            rescueTargets.add(new RescueTarget(x, y));
        }
        int total = 12 + currentRegion.threat * 5;
        for (int i = 0; i < total; i++) {
            float x = 480f + random.nextFloat() * (worldWidth - 720f);
            float y = 180f + random.nextFloat() * (worldHeight - 360f);
            ZombieType type = pickZombieType(i);
            zombies.add(new ZombieUnit(type, x, y));
        }
    }

    private ZombieType pickZombieType(int index) {
        if (currentRegion.threat >= 5 && index % 12 == 0) {
            return ZombieType.HOWLER;
        }
        if (currentRegion.threat >= 4 && index % 8 == 0) {
            return ZombieType.ARMORED;
        }
        if (currentRegion.threat >= 3 && index % 7 == 0) {
            return ZombieType.BLOATER;
        }
        if (index % 5 == 0) {
            return ZombieType.RUNNER;
        }
        if (index % 6 == 0) {
            return ZombieType.CRAWLER;
        }
        return ZombieType.SHAMBLER;
    }

    private void updatePlaying(float dt) {
        handleQueuedActions();
        player.fireCooldown -= dt;
        player.reloadTimer -= dt;
        player.hitTimer -= dt;
        player.utilityCooldown -= dt;
        if (player.reloadTimer <= 0f && player.reloading) {
            player.finishReload();
        }
        if (player.bikeMode) {
            player.fuel = Math.max(0f, player.fuel - dt * (1.8f - safehouse.garageLevel * 0.15f));
            if (player.fuel <= 0f) {
                player.bikeMode = false;
            }
        }
        updatePlayerMovement(dt);
        updateProjectiles(dt);
        updateBurnZones(dt);
        updateZombies(dt);
        updateRescueTargets(dt);
        collectDrops();
        updateCamera();
        checkMissionCompletion();
        updateSnapshots();
    }

    private void handleQueuedActions() {
        if (switchWeaponQueued) {
            player.cycleWeapon(safehouse.unlockedWeapons);
            switchWeaponQueued = false;
        }
        if (bikeToggleQueued) {
            player.toggleBike();
            bikeToggleQueued = false;
        }
        if (reloadQueued) {
            player.startReload();
            reloadQueued = false;
        }
        if (healQueued) {
            if (player.medicine > 0 && player.hp < player.maxHp) {
                player.medicine--;
                player.hp = Math.min(player.maxHp, player.hp + 32 + safehouse.infirmaryLevel * 6);
                player.infection = Math.max(0f, player.infection - 12f);
            }
            healQueued = false;
        }
        if (utilityQueued) {
            triggerUtilityUse();
            utilityQueued = false;
        }
        if (interactQueued) {
            attemptInteraction();
            interactQueued = false;
        }
    }

    private void updatePlayerMovement(float dt) {
        float speed = player.bikeMode ? 340f + safehouse.garageLevel * 18f : 190f + safehouse.workshopLevel * 8f;
        player.vx = moveInputX * speed;
        player.vy = moveInputY * speed;
        player.x += player.vx * dt;
        player.y += player.vy * dt;
        clampPlayer();
        resolveObstacleCollision();
        if (aimInputX != 0f || aimInputY != 0f) {
            float len = (float) Math.sqrt(aimInputX * aimInputX + aimInputY * aimInputY);
            if (len > 0.01f) {
                player.faceX = aimInputX / len;
                player.faceY = aimInputY / len;
            }
        }
        if (triggerHeld && !player.reloading) {
            fireWeaponBurst();
        }
    }

    private void clampPlayer() {
        player.x = Math.max(70f, Math.min(worldWidth - 70f, player.x));
        player.y = Math.max(70f, Math.min(worldHeight - 70f, player.y));
    }

    private void resolveObstacleCollision() {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.rect.contains(player.x, player.y)) {
                if (player.vx > 0f) {
                    player.x = obstacle.rect.left - 10f;
                } else if (player.vx < 0f) {
                    player.x = obstacle.rect.right + 10f;
                }
                if (player.vy > 0f) {
                    player.y = obstacle.rect.top - 10f;
                } else if (player.vy < 0f) {
                    player.y = obstacle.rect.bottom + 10f;
                }
            }
        }
    }

    private void fireWeaponBurst() {
        WeaponType weapon = player.weapon;
        if (player.fireCooldown > 0f) {
            return;
        }
        if (player.currentClip() <= 0) {
            player.startReload();
            return;
        }
        player.consumeRound();
        player.fireCooldown = weapon.fireDelay;
        soundController.attack();
        int pellets = weapon.projectiles;
        for (int i = 0; i < pellets; i++) {
            float spread = (i - (pellets - 1) * 0.5f) * weapon.spread;
            float dirX = rotateX(player.faceX, player.faceY, spread);
            float dirY = rotateY(player.faceX, player.faceY, spread);
            projectiles.add(new ProjectileEntity(
                    player.x + dirX * 26f,
                    player.y + dirY * 26f,
                    dirX * weapon.speed,
                    dirY * weapon.speed,
                    weapon.damage,
                    weapon.projectileRadius,
                    weapon.life,
                    weapon == WeaponType.CROSSBOW));
        }
        player.noisePulse = Math.min(1f, player.noisePulse + weapon.noise);
    }

    private void triggerUtilityUse() {
        if (player.utilityCooldown > 0f) {
            return;
        }
        float targetX = player.x + player.faceX * 180f;
        float targetY = player.y + player.faceY * 180f;
        if (player.utilityMode == 0 && player.molotovs > 0) {
            player.molotovs--;
            burnZones.add(new BurnZone(targetX, targetY, 120f, 6f));
            player.utilityMode = 1;
            player.utilityCooldown = 1.2f;
        } else if (player.utilityMode == 1 && player.lures > 0) {
            player.lures--;
            player.lureX = targetX;
            player.lureY = targetY;
            player.lureLife = 6f;
            player.utilityMode = 0;
            player.utilityCooldown = 0.9f;
        }
    }

    private void attemptInteraction() {
        for (RescueTarget rescueTarget : rescueTargets) {
            if (!rescueTarget.rescued && distance(player.x, player.y, rescueTarget.x, rescueTarget.y) < 80f) {
                rescueTarget.rescued = true;
                player.savedThisRun++;
                return;
            }
        }
        if (!relaySecured && player.savedThisRun >= currentRegion.requiredRescues && relayZone.contains(player.x, player.y)) {
            relaySecured = true;
            drops.add(new ResourceDrop(relayZone.centerX(), relayZone.centerY(), LootType.SCRAP, 30 + currentRegion.threat * 8));
            return;
        }
        if (relaySecured && extractionZone.contains(player.x, player.y)) {
            finishMission(true);
        }
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            ProjectileEntity projectile = projectiles.get(i);
            projectile.x += projectile.vx * dt;
            projectile.y += projectile.vy * dt;
            projectile.life -= dt;
            if (projectile.life <= 0f || projectile.x < 0f || projectile.y < 0f || projectile.x > worldWidth || projectile.y > worldHeight) {
                projectiles.remove(i);
                continue;
            }
            boolean remove = false;
            for (ZombieUnit zombie : zombies) {
                if (!zombie.alive) {
                    continue;
                }
                float hitDistance = zombie.radius + projectile.radius;
                if (distance(projectile.x, projectile.y, zombie.x, zombie.y) <= hitDistance) {
                    zombie.hp -= projectile.damage;
                    zombie.hitFlash = 0.16f;
                    zombie.vx += projectile.vx * 0.05f;
                    zombie.vy += projectile.vy * 0.05f;
                    if (zombie.hp <= 0) {
                        zombie.alive = false;
                        player.scrapThisRun += 4 + zombie.type.reward;
                        if (zombie.type == ZombieType.BLOATER) {
                            burnZones.add(new BurnZone(zombie.x, zombie.y, 90f, 3f));
                        }
                    }
                    remove = true;
                    break;
                }
            }
            if (remove) {
                projectiles.remove(i);
            }
        }
    }

    private void updateBurnZones(float dt) {
        for (int i = burnZones.size() - 1; i >= 0; i--) {
            BurnZone zone = burnZones.get(i);
            zone.life -= dt;
            if (zone.life <= 0f) {
                burnZones.remove(i);
                continue;
            }
            for (ZombieUnit zombie : zombies) {
                if (!zombie.alive) {
                    continue;
                }
                if (distance(zone.x, zone.y, zombie.x, zombie.y) < zone.radius) {
                    zombie.hp -= dt * 14f;
                    zombie.hitFlash = 0.12f;
                    if (zombie.hp <= 0f) {
                        zombie.alive = false;
                        player.scrapThisRun += 3 + zombie.type.reward;
                    }
                }
            }
        }
        if (player.lureLife > 0f) {
            player.lureLife -= dt;
        }
    }

    private void updateZombies(float dt) {
        int aliveCount = 0;
        for (ZombieUnit zombie : zombies) {
            zombie.hitFlash -= dt;
            if (!zombie.alive) {
                continue;
            }
            aliveCount++;
            float targetX = player.lureLife > 0f ? player.lureX : player.x;
            float targetY = player.lureLife > 0f ? player.lureY : player.y;
            float dx = targetX - zombie.x;
            float dy = targetY - zombie.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 1f) {
                zombie.vx = dx / len * zombie.type.speed;
                zombie.vy = dy / len * zombie.type.speed;
            }
            if (player.noisePulse < 0.18f && len > zombie.type.agroRange) {
                zombie.vx *= 0.2f;
                zombie.vy *= 0.2f;
            }
            zombie.x += zombie.vx * dt;
            zombie.y += zombie.vy * dt;
            if (zombie.type == ZombieType.CRAWLER) {
                zombie.y += (float) Math.sin((zombie.phase += dt * 7f)) * 0.8f;
            }
            if (len < zombie.radius + 24f) {
                zombie.attackCooldown -= dt;
                if (zombie.attackCooldown <= 0f) {
                    player.takeDamage(zombie.type.damage, zombie.type.infection);
                    zombie.attackCooldown = zombie.type.attackDelay;
                }
            } else {
                zombie.attackCooldown -= dt;
            }
            if (zombie.type == ZombieType.HOWLER) {
                zombie.specialCooldown -= dt;
                if (len < 260f && zombie.specialCooldown <= 0f) {
                    zombie.specialCooldown = 5.5f;
                    for (int i = 0; i < 2; i++) {
                        zombies.add(new ZombieUnit(ZombieType.RUNNER, zombie.x + 40f * i, zombie.y + 28f * (i == 0 ? 1 : -1)));
                    }
                }
            }
        }
        player.noisePulse = Math.max(0f, player.noisePulse - dt * 0.18f);
        if (player.hp <= 0 || player.infection >= 100f) {
            finishMission(false);
        }
        if (aliveCount == 0 && currentRegion != null) {
            for (int i = 0; i < currentRegion.threat + 2; i++) {
                float x = worldWidth * 0.72f + random.nextFloat() * 220f;
                float y = 180f + random.nextFloat() * (worldHeight - 360f);
                zombies.add(new ZombieUnit(i % 3 == 0 ? ZombieType.RUNNER : ZombieType.SHAMBLER, x, y));
            }
        }
    }

    private void updateRescueTargets(float dt) {
        for (RescueTarget rescueTarget : rescueTargets) {
            if (!rescueTarget.rescued || rescueTarget.extracted) {
                continue;
            }
            float dx = player.x - 44f - rescueTarget.x;
            float dy = player.y + 36f - rescueTarget.y;
            rescueTarget.x += dx * Math.min(1f, dt * 3.2f);
            rescueTarget.y += dy * Math.min(1f, dt * 3.2f);
            if (relaySecured && extractionZone.contains(rescueTarget.x, rescueTarget.y)) {
                rescueTarget.extracted = true;
            }
        }
    }

    private void collectDrops() {
        for (ResourceDrop drop : drops) {
            if (drop.collected) {
                continue;
            }
            if (distance(player.x, player.y, drop.x, drop.y) < 56f) {
                drop.collected = true;
                if (drop.type == LootType.SCRAP) {
                    player.scrapThisRun += drop.value;
                } else if (drop.type == LootType.FUEL) {
                    player.fuel = Math.min(player.maxFuel, player.fuel + drop.value);
                } else if (drop.type == LootType.MEDICINE) {
                    player.medicine += drop.value;
                } else if (drop.type == LootType.AMMO) {
                    player.addReserveAmmo(drop.value);
                }
            }
        }
    }

    private void checkMissionCompletion() {
        if (relaySecured && extractionZone.contains(player.x, player.y)) {
            finishMission(true);
        }
    }

    private void finishMission(boolean success) {
        triggerHeld = false;
        activeAimPointerId = -1;
        activeMovePointerId = -1;
        if (success) {
            safehouse.scrap += player.scrapThisRun;
            safehouse.fuel += Math.round(player.fuel * 0.35f);
            safehouse.medicine += Math.max(0, player.medicine - 1);
            safehouse.survivors += player.savedThisRun;
            safehouse.cleared[currentRegionIndex] = true;
            safehouse.unlockedRegions = Math.min(regions.length, Math.max(safehouse.unlockedRegions, currentRegionIndex + 2));
            state = GameState.SAFEHOUSE;
            resultTitle = "Relay Secured";
            resultBody = String.format(
                    Locale.US,
                    "%s cleared. Scrap %d  Survivors %d  Fuel %.0f",
                    currentRegion.name,
                    player.scrapThisRun,
                    player.savedThisRun,
                    player.fuel);
        } else {
            state = GameState.GAME_OVER;
            resultTitle = player.infection >= 100f ? "Infection Critical" : "Run Lost";
            resultBody = String.format(
                    Locale.US,
                    "%s failed. Scrap %d  Survivors %d  Relay %s",
                    currentRegion != null ? currentRegion.name : "Frontier",
                    player.scrapThisRun,
                    player.savedThisRun,
                    relaySecured ? "secured" : "unsecured");
        }
        updateSnapshots();
        dispatchUiNow();
    }

    private void updateCamera() {
        cameraX = player.x - getWidth() * 0.5f;
        cameraY = player.y - getHeight() * 0.5f;
        cameraX = Math.max(0f, Math.min(worldWidth - getWidth(), cameraX));
        cameraY = Math.max(0f, Math.min(worldHeight - getHeight(), cameraY));
    }

    private void drawFrame() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            if (state == GameState.PLAYING || state == GameState.PAUSED || currentRegion != null) {
                drawMission(canvas);
            } else {
                drawAttractScreen(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawAttractScreen(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_bg_alt));
        canvas.drawRect(0f, 0f, getWidth(), getHeight() * 0.65f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
        for (int i = 0; i < 12; i++) {
            float x = getWidth() * 0.16f + (i % 4) * getWidth() * 0.18f;
            float y = getHeight() * 0.22f + (i / 4) * getHeight() * 0.18f;
            canvas.drawCircle(x, y, 26f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        canvas.drawLine(getWidth() * 0.16f, getHeight() * 0.22f, getWidth() * 0.7f, getHeight() * 0.58f, paint);
        canvas.drawLine(getWidth() * 0.34f, getHeight() * 0.22f, getWidth() * 0.52f, getHeight() * 0.58f, paint);
        paint.setStyle(Paint.Style.FILL);
        drawBitmapOrFallback(canvas, survivorBitmap, getWidth() * 0.5f, getHeight() * 0.52f, 150f, 35f, 0);
        drawBitmapOrFallback(canvas, zombieStandBitmap, getWidth() * 0.66f, getHeight() * 0.54f, 160f, -25f, ContextCompat.getColor(getContext(), R.color.cst_danger));
    }

    private void drawMission(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_bg_main));
        drawGround(canvas);
        drawRoads(canvas);
        drawObstacles(canvas);
        drawZones(canvas);
        drawDrops(canvas);
        drawRescueTargets(canvas);
        drawBurnZones(canvas);
        drawProjectiles(canvas);
        drawZombies(canvas);
        drawPlayer(canvas);
        if (state == GameState.PAUSED) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_shadow));
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        }
    }

    private void drawGround(Canvas canvas) {
        int alt = ContextCompat.getColor(getContext(), R.color.cst_bg_alt);
        if (groundBitmap == null || altGroundBitmap == null) {
            paint.setColor(alt);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
            return;
        }
        float tileSize = 160f;
        int startX = (int) (cameraX / tileSize) - 1;
        int startY = (int) (cameraY / tileSize) - 1;
        int endX = startX + (int) (getWidth() / tileSize) + 3;
        int endY = startY + (int) (getHeight() / tileSize) + 3;
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                float drawX = x * tileSize - cameraX;
                float drawY = y * tileSize - cameraY;
                Bitmap tile = ((x + y) & 1) == 0 ? groundBitmap : altGroundBitmap;
                tmpRect.set(drawX, drawY, drawX + tileSize, drawY + tileSize);
                canvas.drawBitmap(tile, null, tmpRect, spritePaint);
            }
        }
    }

    private void drawRoads(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
        RectF roadA = new RectF(-cameraX, worldHeight * 0.5f - cameraY, worldWidth - cameraX, worldHeight * 0.62f - cameraY);
        RectF roadB = new RectF(worldWidth * 0.52f - cameraX, 0f - cameraY, worldWidth * 0.63f - cameraX, worldHeight - cameraY);
        canvas.drawRect(roadA, paint);
        canvas.drawRect(roadB, paint);
    }

    private void drawObstacles(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_bg));
        for (Obstacle obstacle : obstacles) {
            RectF drawRect = new RectF(
                    obstacle.rect.left - cameraX,
                    obstacle.rect.top - cameraY,
                    obstacle.rect.right - cameraX,
                    obstacle.rect.bottom - cameraY);
            if (obstacleBitmap != null) {
                canvas.drawBitmap(obstacleBitmap, null, drawRect, spritePaint);
            } else {
                canvas.drawRoundRect(drawRect, 16f, 16f, paint);
            }
        }
    }

    private void drawZones(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
        tmpRect.set(extractionZone.left - cameraX, extractionZone.top - cameraY, extractionZone.right - cameraX, extractionZone.bottom - cameraY);
        canvas.drawRoundRect(tmpRect, 24f, 24f, paint);
        paint.setColor(relaySecured ? ContextCompat.getColor(getContext(), R.color.cst_success) : ContextCompat.getColor(getContext(), R.color.cst_warning));
        tmpRect.set(relayZone.left - cameraX, relayZone.top - cameraY, relayZone.right - cameraX, relayZone.bottom - cameraY);
        if (towerBitmap != null) {
            canvas.drawBitmap(towerBitmap, null, tmpRect, spritePaint);
        } else {
            canvas.drawRoundRect(tmpRect, 24f, 24f, paint);
        }
    }

    private void drawDrops(Canvas canvas) {
        for (ResourceDrop drop : drops) {
            if (drop.collected) {
                continue;
            }
            int color = ContextCompat.getColor(getContext(), R.color.cst_warning);
            if (drop.type == LootType.MEDICINE) {
                color = ContextCompat.getColor(getContext(), R.color.cst_success);
            } else if (drop.type == LootType.AMMO) {
                color = ContextCompat.getColor(getContext(), R.color.cst_accent);
            } else if (drop.type == LootType.FUEL) {
                color = ContextCompat.getColor(getContext(), R.color.cst_accent_2);
            }
            paint.setColor(color);
            canvas.drawCircle(drop.x - cameraX, drop.y - cameraY, 18f, paint);
        }
    }

    private void drawRescueTargets(Canvas canvas) {
        for (RescueTarget rescueTarget : rescueTargets) {
            if (rescueTarget.extracted) {
                continue;
            }
            drawBitmapOrFallback(
                    canvas,
                    survivorBitmap,
                    rescueTarget.x - cameraX,
                    rescueTarget.y - cameraY,
                    rescueTarget.rescued ? 96f : 86f,
                    0f,
                    rescueTarget.rescued ? ContextCompat.getColor(getContext(), R.color.cst_success) : 0);
        }
    }

    private void drawBurnZones(Canvas canvas) {
        for (BurnZone zone : burnZones) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_danger));
            paint.setAlpha((int) (100 + 80 * Math.min(1f, zone.life)));
            canvas.drawCircle(zone.x - cameraX, zone.y - cameraY, zone.radius, paint);
        }
        paint.setAlpha(255);
        if (player.lureLife > 0f) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_accent));
            canvas.drawCircle(player.lureX - cameraX, player.lureY - cameraY, 44f + 20f * (float) Math.sin(player.lureLife * 8f), paint);
        }
    }

    private void drawProjectiles(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        for (ProjectileEntity projectile : projectiles) {
            canvas.drawCircle(projectile.x - cameraX, projectile.y - cameraY, projectile.radius, paint);
        }
    }

    private void drawZombies(Canvas canvas) {
        for (ZombieUnit zombie : zombies) {
            if (!zombie.alive) {
                continue;
            }
            int tint = 0;
            if (zombie.type == ZombieType.RUNNER) {
                tint = ContextCompat.getColor(getContext(), R.color.cst_warning);
            } else if (zombie.type == ZombieType.ARMORED) {
                tint = ContextCompat.getColor(getContext(), R.color.cst_panel_stroke);
            } else if (zombie.type == ZombieType.BLOATER) {
                tint = ContextCompat.getColor(getContext(), R.color.cst_accent_2);
            } else if (zombie.type == ZombieType.HOWLER) {
                tint = ContextCompat.getColor(getContext(), R.color.cst_danger);
            }
            float angle = (float) Math.toDegrees(Math.atan2(zombie.vy, zombie.vx));
            drawBitmapOrFallback(canvas, zombie.hitFlash > 0f ? zombieAttackBitmap : zombieStandBitmap, zombie.x - cameraX, zombie.y - cameraY, zombie.radius * 4f, angle, tint);
        }
    }

    private void drawPlayer(Canvas canvas) {
        if (player.bikeMode) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel_stroke));
            canvas.drawOval(player.x - 58f - cameraX, player.y - 18f - cameraY, player.x + 58f - cameraX, player.y + 28f - cameraY, paint);
        }
        Bitmap frame = playerStandBitmap;
        if (player.reloading) {
            frame = playerReloadBitmap;
        } else if (player.weapon == WeaponType.SMG) {
            frame = playerMachineBitmap;
        } else if (player.weapon == WeaponType.PISTOL || player.weapon == WeaponType.RIFLE || player.weapon == WeaponType.SHOTGUN) {
            frame = playerGunBitmap;
        }
        float angle = (float) Math.toDegrees(Math.atan2(player.faceY, player.faceX));
        drawBitmapOrFallback(canvas, frame, player.x - cameraX, player.y - cameraY, player.bikeMode ? 140f : 110f, angle, 0);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_danger));
        canvas.drawRect(player.x - 42f - cameraX, player.y - 70f - cameraY, player.x + 42f - cameraX, player.y - 60f - cameraY, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_success));
        float ratio = Math.max(0f, player.hp / (float) player.maxHp);
        canvas.drawRect(player.x - 42f - cameraX, player.y - 70f - cameraY, player.x - 42f - cameraX + 84f * ratio, player.y - 60f - cameraY, paint);
    }

    private void drawBitmapOrFallback(Canvas canvas, Bitmap bitmap, float cx, float cy, float size, float angleDegrees, int tint) {
        if (bitmap == null) {
            paint.setColor(tint != 0 ? tint : ContextCompat.getColor(getContext(), R.color.cst_text_primary));
            canvas.drawCircle(cx, cy, size * 0.2f, paint);
            return;
        }
        spriteMatrix.reset();
        float scale = size / Math.max(bitmap.getWidth(), bitmap.getHeight());
        spriteMatrix.postTranslate(-bitmap.getWidth() * 0.5f, -bitmap.getHeight() * 0.5f);
        spriteMatrix.postScale(scale, scale);
        spriteMatrix.postRotate(angleDegrees);
        spriteMatrix.postTranslate(cx, cy);
        if (tint != 0) {
            spritePaint.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.MULTIPLY));
        } else {
            spritePaint.setColorFilter(null);
        }
        canvas.drawBitmap(bitmap, spriteMatrix, spritePaint);
        spritePaint.setColorFilter(null);
    }

    private void loadArt() {
        groundBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Tiles/tile_01.png");
        altGroundBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Tiles/tile_02.png");
        obstacleBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Tiles/tile_21.png");
        towerBitmap = loadBitmap("game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile040.png");
        playerStandBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_stand.png");
        playerGunBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_gun.png");
        playerMachineBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_machine.png");
        playerReloadBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_reload.png");
        zombieStandBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_stand.png");
        zombieAttackBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_machine.png");
        survivorBitmap = loadBitmap("game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_hold.png");
    }

    private Bitmap loadBitmap(String assetPath) {
        AssetManager assetManager = getContext().getAssets();
        try (InputStream inputStream = assetManager.open(assetPath)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            return BitmapFactory.decodeStream(inputStream, null, options);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void updateSnapshots() {
        String regionName = currentRegion == null ? "Ash Frontier" : currentRegion.name;
        String objective = currentRegion == null
                ? "Prepare the safehouse and choose a frontier route."
                : String.format(Locale.US, "Rescue %d  Relay %s  Extract", currentRegion.requiredRescues, relaySecured ? "ready" : "pending");
        String status = player.bikeMode ? "Bike active" : (player.lureLife > 0f ? "Noise lure active" : "On foot");
        WeaponType currentWeapon = player.weapon;
        hudSnapshot = new HudSnapshot(
                String.format(Locale.US, "HP %d", player.hp),
                String.format(Locale.US, "INF %.0f", player.infection),
                String.format(Locale.US, "FUEL %.0f", player.fuel),
                String.format(Locale.US, "SCRAP %d", player.scrapThisRun),
                String.format(Locale.US, "SAVE %d", player.savedThisRun),
                regionName,
                objective,
                "WPN " + currentWeapon.label,
                String.format(Locale.US, "%d / %d", player.currentClip(), player.currentReserve()),
                player.utilityMode == 0
                        ? String.format(Locale.US, "Molotov %d", player.molotovs)
                        : String.format(Locale.US, "Lure %d", player.lures),
                status,
                String.format(Locale.US, "%s  Scrap %d  Fuel %.0f", regionName, player.scrapThisRun, player.fuel));
        safehouseSnapshot = createSafehouseSnapshot();
    }

    private SafehouseSnapshot createSafehouseSnapshot() {
        String summary = String.format(
                Locale.US,
                "Scrap %d  Fuel %d  Med %d  Survivors %d  Workshop %d  Infirmary %d  Garage %d",
                safehouse.scrap,
                safehouse.fuel,
                safehouse.medicine,
                safehouse.survivors,
                safehouse.workshopLevel,
                safehouse.infirmaryLevel,
                safehouse.garageLevel);
        String hint = "Workshop unlocks weapons. Infirmary raises recovery. Garage boosts bike fuel and region reach.";
        return new SafehouseSnapshot(summary, hint);
    }

    private void maybeDispatchUi() {
        long now = SystemClock.uptimeMillis();
        if (now - lastUiDispatchAt > 120L) {
            lastUiDispatchAt = now;
            dispatchUiNow();
        }
    }

    private void dispatchUiNow() {
        if (listener != null) {
            post(listener::onUiModelChanged);
        }
    }

    private static float rotateX(float x, float y, float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) (x * Math.cos(radians) - y * Math.sin(radians));
    }

    private static float rotateY(float x, float y, float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) (x * Math.sin(radians) + y * Math.cos(radians));
    }

    private static float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private HudSnapshot createIdleHud() {
        return new HudSnapshot(
                "HP 0",
                "INF 0",
                "FUEL 0",
                "SCRAP 0",
                "SAVE 0",
                "Ash Frontier",
                "Choose a region and prepare your loadout.",
                "WPN Pistol",
                "0 / 0",
                "Molotov 0",
                "Idle",
                "Scout the wasteland, secure a relay, and extract.");
    }

    private static RegionDefinition[] buildRegions() {
        return new RegionDefinition[] {
                new RegionDefinition("Cinder Gate", 1, 2600f, 1780f, 1),
                new RegionDefinition("Relay Flats", 2, 2820f, 1820f, 1),
                new RegionDefinition("Dry Canal", 2, 2960f, 1880f, 1),
                new RegionDefinition("Ash Transit", 3, 3160f, 1920f, 1),
                new RegionDefinition("Refinery Scar", 3, 3320f, 1980f, 2),
                new RegionDefinition("Burned Estates", 4, 3440f, 2040f, 2),
                new RegionDefinition("Dead Freight", 4, 3580f, 2080f, 2),
                new RegionDefinition("Signal Hollow", 5, 3680f, 2140f, 2),
                new RegionDefinition("Murk Hospital", 5, 3820f, 2200f, 2),
                new RegionDefinition("Black Overpass", 6, 3940f, 2260f, 2),
                new RegionDefinition("Howler Basin", 6, 4080f, 2320f, 3),
                new RegionDefinition("Core Meridian", 7, 4200f, 2380f, 3)
        };
    }

    private static final class RegionDefinition {
        final String name;
        final int threat;
        final float worldWidth;
        final float worldHeight;
        final int requiredRescues;

        RegionDefinition(String name, int threat, float worldWidth, float worldHeight, int requiredRescues) {
            this.name = name;
            this.threat = threat;
            this.worldWidth = worldWidth;
            this.worldHeight = worldHeight;
            this.requiredRescues = requiredRescues;
        }
    }

    private static final class SafehouseState {
        int scrap = 120;
        int fuel = 80;
        int medicine = 3;
        int survivors = 2;
        int workshopLevel = 1;
        int infirmaryLevel = 1;
        int garageLevel = 1;
        int unlockedRegions = 3;
        final boolean[] cleared = new boolean[12];
        final EnumSet<WeaponType> unlockedWeapons = EnumSet.of(WeaponType.PISTOL, WeaponType.SHOTGUN);
    }

    private enum WeaponType {
        PISTOL("Pistol", 10, 60, 18, 0.25f, 620f, 1, 0f, 0.18f, 6f, 36f),
        SHOTGUN("Shotgun", 5, 24, 11, 0.72f, 560f, 5, 8f, 0.42f, 10f, 42f),
        SMG("SMG", 18, 90, 10, 0.09f, 700f, 1, 0f, 0.34f, 5f, 32f),
        RIFLE("Rifle", 8, 42, 28, 0.38f, 820f, 1, 0f, 0.22f, 7f, 34f),
        CROSSBOW("Crossbow", 1, 16, 36, 0.8f, 760f, 1, 0f, 0.05f, 9f, 30f);

        final String label;
        final int clipSize;
        final int reserveSize;
        final int damage;
        final float fireDelay;
        final float speed;
        final int projectiles;
        final float spread;
        final float noise;
        final float life;
        final float projectileRadius;

        WeaponType(String label, int clipSize, int reserveSize, int damage, float fireDelay, float speed, int projectiles, float spread, float noise, float life, float projectileRadius) {
            this.label = label;
            this.clipSize = clipSize;
            this.reserveSize = reserveSize;
            this.damage = damage;
            this.fireDelay = fireDelay;
            this.speed = speed;
            this.projectiles = projectiles;
            this.spread = spread;
            this.noise = noise;
            this.life = life;
            this.projectileRadius = projectileRadius;
        }
    }

    private enum ZombieType {
        SHAMBLER(76f, 34f, 8, 6f, 1.0f, 320f, 6),
        RUNNER(122f, 30f, 10, 5f, 0.8f, 400f, 8),
        CRAWLER(86f, 24f, 7, 5f, 1.1f, 250f, 5),
        BLOATER(58f, 46f, 14, 8f, 1.2f, 340f, 10),
        ARMORED(64f, 40f, 12, 7f, 1.0f, 360f, 12),
        HOWLER(90f, 34f, 11, 6f, 1.1f, 460f, 14);

        final float speed;
        final float radius;
        final int damage;
        final float attackDelay;
        final float infection;
        final float agroRange;
        final int reward;

        ZombieType(float speed, float radius, int damage, float attackDelay, float infection, float agroRange, int reward) {
            this.speed = speed;
            this.radius = radius;
            this.damage = damage;
            this.attackDelay = attackDelay;
            this.infection = infection;
            this.agroRange = agroRange;
            this.reward = reward;
        }
    }

    private static final class PlayerRig {
        float x;
        float y;
        float vx;
        float vy;
        float faceX = 1f;
        float faceY;
        int maxHp;
        int hp;
        float infection;
        float fuel;
        float maxFuel;
        float fireCooldown;
        float reloadTimer;
        float hitTimer;
        float utilityCooldown;
        boolean reloading;
        boolean bikeMode;
        int scrapThisRun;
        int savedThisRun;
        int medicine;
        int molotovs;
        int lures;
        int utilityMode;
        float lureX;
        float lureY;
        float lureLife;
        float noisePulse;
        WeaponType weapon = WeaponType.PISTOL;
        final int[] clipAmmo = new int[WeaponType.values().length];
        final int[] reserveAmmo = new int[WeaponType.values().length];

        void resetForMission(RegionDefinition region, SafehouseState safehouse) {
            x = 180f;
            y = region.worldHeight * 0.56f;
            vx = 0f;
            vy = 0f;
            maxHp = 118 + safehouse.infirmaryLevel * 18;
            hp = maxHp;
            infection = 0f;
            maxFuel = 70f + safehouse.garageLevel * 12f;
            fuel = maxFuel;
            fireCooldown = 0f;
            reloadTimer = 0f;
            hitTimer = 0f;
            utilityCooldown = 0f;
            reloading = false;
            bikeMode = false;
            scrapThisRun = 0;
            savedThisRun = 0;
            medicine = 1 + safehouse.infirmaryLevel;
            molotovs = 2;
            lures = 2;
            utilityMode = 0;
            lureLife = 0f;
            noisePulse = 0f;
            for (WeaponType type : WeaponType.values()) {
                clipAmmo[type.ordinal()] = safehouse.unlockedWeapons.contains(type) ? type.clipSize : 0;
                reserveAmmo[type.ordinal()] = safehouse.unlockedWeapons.contains(type) ? type.reserveSize : 0;
            }
            weapon = safehouse.unlockedWeapons.contains(WeaponType.SHOTGUN) ? WeaponType.SHOTGUN : WeaponType.PISTOL;
        }

        void cycleWeapon(EnumSet<WeaponType> unlockedWeapons) {
            WeaponType[] values = WeaponType.values();
            int start = weapon.ordinal();
            for (int i = 1; i <= values.length; i++) {
                WeaponType candidate = values[(start + i) % values.length];
                if (unlockedWeapons.contains(candidate)) {
                    weapon = candidate;
                    return;
                }
            }
        }

        void toggleBike() {
            bikeMode = !bikeMode && fuel > 8f;
            if (bikeMode && weapon != WeaponType.PISTOL && weapon != WeaponType.CROSSBOW) {
                weapon = WeaponType.PISTOL;
            }
        }

        void startReload() {
            if (!reloading && currentClip() < weapon.clipSize && currentReserve() > 0) {
                reloading = true;
                reloadTimer = weapon == WeaponType.SHOTGUN ? 0.9f : 0.75f;
            }
        }

        void finishReload() {
            reloading = false;
            int need = weapon.clipSize - currentClip();
            int available = currentReserve();
            int load = Math.min(need, available);
            clipAmmo[weapon.ordinal()] += load;
            reserveAmmo[weapon.ordinal()] -= load;
        }

        int currentClip() {
            return clipAmmo[weapon.ordinal()];
        }

        int currentReserve() {
            return reserveAmmo[weapon.ordinal()];
        }

        void consumeRound() {
            clipAmmo[weapon.ordinal()] = Math.max(0, clipAmmo[weapon.ordinal()] - 1);
        }

        void addReserveAmmo(int value) {
            reserveAmmo[weapon.ordinal()] = Math.min(weapon.reserveSize + 30, reserveAmmo[weapon.ordinal()] + value);
        }

        void takeDamage(int damage, float infectionGain) {
            if (hitTimer > 0f) {
                return;
            }
            hp -= damage;
            infection = Math.min(100f, infection + infectionGain);
            hitTimer = 0.42f;
        }
    }

    private static final class ZombieUnit {
        final ZombieType type;
        float x;
        float y;
        float vx;
        float vy;
        float hp;
        float radius;
        float attackCooldown;
        float hitFlash;
        float specialCooldown;
        float phase;
        boolean alive = true;

        ZombieUnit(ZombieType type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = type == ZombieType.ARMORED ? 64f : type == ZombieType.BLOATER ? 82f : type == ZombieType.HOWLER ? 54f : 32f;
            this.radius = type.radius;
        }
    }

    private static final class ProjectileEntity {
        float x;
        float y;
        float vx;
        float vy;
        int damage;
        float radius;
        float life;
        boolean quiet;

        ProjectileEntity(float x, float y, float vx, float vy, int damage, float radius, float life, boolean quiet) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.damage = damage;
            this.radius = radius;
            this.life = life;
            this.quiet = quiet;
        }
    }

    private enum LootType {
        SCRAP,
        AMMO,
        FUEL,
        MEDICINE
    }

    private static final class ResourceDrop {
        final float x;
        final float y;
        final LootType type;
        final int value;
        boolean collected;

        ResourceDrop(float x, float y, LootType type, int value) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.value = value;
        }
    }

    private static final class RescueTarget {
        float x;
        float y;
        boolean rescued;
        boolean extracted;

        RescueTarget(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Obstacle {
        final RectF rect;

        Obstacle(float left, float top, float width, float height) {
            this.rect = new RectF(left, top, left + width, top + height);
        }
    }

    private static final class BurnZone {
        final float x;
        final float y;
        final float radius;
        float life;

        BurnZone(float x, float y, float radius, float life) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.life = life;
        }
    }
}
