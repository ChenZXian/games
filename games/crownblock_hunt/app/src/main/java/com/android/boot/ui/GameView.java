package com.android.boot.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.boot.audio.AudioController;
import com.android.boot.core.GameInput;
import com.android.boot.core.GameState;
import com.android.boot.fx.ScreenShake;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
  public enum Control {
    LEFT,
    RIGHT,
    JUMP,
    LIGHT,
    HEAVY,
    DASH,
    SHOOT,
    SPECIAL,
    INTERACT
  }

  public interface Listener {
    void onStateChanged(GameState state);
    void onSnapshot(UiSnapshot snapshot);
  }

  public static class UiSnapshot {
    public String blockTitle = "";
    public String objective = "";
    public String control = "";
    public String health = "";
    public String stamina = "";
    public String armor = "";
    public String heat = "";
    public String cash = "";
    public String weapon = "";
    public String ammo = "";
    public String combo = "";
    public String finisher = "";
    public String partnerOne = "";
    public String partnerTwo = "";
    public String menuSummary = "";
    public String boardSelection = "";
    public String boardSummary = "";
    public String safehouseSummary = "";
    public String pauseSummary = "";
    public String resultSummary = "";
    public String gameOverSummary = "";
    public String helpSummary = "";
    public String partnerOneAction = "";
    public String partnerTwoAction = "";
    public String swapAction = "";
    public String reloadAction = "";
    public String interactAction = "";
  }

  private static final class District {
    final String name;
    final String boss;
    final int totalSectors;
    final int threat;
    boolean unlocked;
    int clearedSectors;

    District(String name, String boss, int totalSectors, int threat) {
      this.name = name;
      this.boss = boss;
      this.totalSectors = totalSectors;
      this.threat = threat;
    }

    int controlPercent() {
      return Math.round((clearedSectors * 100f) / totalSectors);
    }
  }

  private enum AssistType {
    STRIKER,
    GUNNER,
    MEDIC,
    TANK
  }

  private static final class Partner {
    final String name;
    final String role;
    final AssistType assistType;
    final float cooldownMax;
    final int accentColor;
    boolean unlocked;
    float cooldown;
    int loyalty;

    Partner(String name, String role, AssistType assistType, float cooldownMax, int accentColor, boolean unlocked) {
      this.name = name;
      this.role = role;
      this.assistType = assistType;
      this.cooldownMax = cooldownMax;
      this.accentColor = accentColor;
      this.unlocked = unlocked;
    }
  }

  private static final class Weapon {
    final String name;
    final int clipSize;
    final int damage;
    final float fireCooldown;
    final float reloadTime;
    final int pellets;
    final float spread;
    final float projectileSpeed;
    boolean unlocked;
    int clip;
    int reserve;
    int tier;

    Weapon(String name, int clipSize, int damage, float fireCooldown, float reloadTime, int pellets, float spread, float projectileSpeed, int reserve, boolean unlocked) {
      this.name = name;
      this.clipSize = clipSize;
      this.damage = damage;
      this.fireCooldown = fireCooldown;
      this.reloadTime = reloadTime;
      this.pellets = pellets;
      this.spread = spread;
      this.projectileSpeed = projectileSpeed;
      this.reserve = reserve;
      this.unlocked = unlocked;
      this.clip = clipSize;
      this.tier = 1;
    }
  }

  private static final class Enemy {
    static final int BRAWLER = 0;
    static final int RUSHER = 1;
    static final int PIPE = 2;
    static final int GUNNER = 3;
    static final int BRUTE = 4;
    static final int LIEUTENANT = 5;

    float x;
    float y;
    float vx;
    float hp;
    float maxHp;
    float speed;
    float hurtTimer;
    float attackCooldown;
    float flashTimer;
    float spawnTimer;
    boolean facingRight = false;
    boolean dead;
    boolean elite;
    int type;
  }

  private static final class Projectile {
    float x;
    float y;
    float vx;
    float vy;
    float life;
    int damage;
    boolean fromPlayer;
    int color;
    float radius;
  }

  private static final class Pickup {
    static final int CASH = 0;
    static final int AMMO = 1;
    static final int MED = 2;

    float x;
    float y;
    float bob;
    int type;
    int amount;
  }

  private static final class SpawnEvent {
    float x;
    int count;
    int type;
    boolean elite;
    boolean triggered;
  }

  private static final class HitSpark {
    float x;
    float y;
    float life;
    float size;
    int color;
    String label;
  }

  private static final class SpriteSet {
    Bitmap idle;
    Bitmap stand;
    Bitmap walk1;
    Bitmap walk2;
    Bitmap action1;
    Bitmap action2;
    Bitmap hurt;
    Bitmap jump;
  }

  private static final float FIXED_DT = 1f / 60f;

  private final GameInput input = new GameInput();
  private final AudioController audioController = new AudioController();
  private final ScreenShake screenShake = new ScreenShake();
  private final Random random = new Random(23);
  private final List<Enemy> enemies = new ArrayList<>();
  private final List<Projectile> projectiles = new ArrayList<>();
  private final List<Pickup> pickups = new ArrayList<>();
  private final List<SpawnEvent> spawnEvents = new ArrayList<>();
  private final List<HitSpark> hitSparks = new ArrayList<>();
  private final Paint skyPaint = new Paint();
  private final Paint hazePaint = new Paint();
  private final Paint roadPaint = new Paint();
  private final Paint lanePaint = new Paint();
  private final Paint buildingPaint = new Paint();
  private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint elitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint pickupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint bulletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF tempRect = new RectF();
  private Thread gameThread;
  private boolean running;
  private Listener listener;
  private GameState state = GameState.MENU;
  private UiSnapshot lastSnapshot = new UiSnapshot();
  private District[] districts;
  private Partner[] roster;
  private final Partner[] activePartners = new Partner[2];
  private Weapon[] weapons;
  private int selectedDistrict;
  private int highestUnlockedDistrict;
  private District activeDistrict;
  private int activeSectorNumber;
  private int capturedBlocks;
  private int totalScore;
  private int maxCombo;
  private int benchLevel;
  private int reviveTokens;
  private int recruitIndex;
  private int currentWeaponIndex;
  private float playerX;
  private float playerY;
  private float playerVelY;
  private boolean playerOnGround = true;
  private boolean facingRight = true;
  private float playerMaxHp = 120f;
  private float playerHp = 120f;
  private float staminaMax = 100f;
  private float stamina = 100f;
  private float armorMax = 45f;
  private float armor = 45f;
  private float heat;
  private float finisher;
  private int cash = 160;
  private int combo;
  private float comboTimer;
  private float attackTimer;
  private float attackLock;
  private float reloadTimer;
  private float gunCooldown;
  private float dashTimer;
  private float invulnerableTimer;
  private float hitstopTimer;
  private float sectorTime;
  private float resultReward;
  private float sectorRewardCash;
  private float sectorRewardScore;
  private float snapshotTimer;
  private float ambientTimer;
  private int attackStyle;
  private boolean attackResolved;
  private boolean campaignCompleted;
  private float worldWidth = 3200f;
  private float exitGateX = 3000f;
  private float cameraX;
  private boolean prevJump;
  private boolean prevLight;
  private boolean prevHeavy;
  private boolean prevDash;
  private boolean prevShoot;
  private boolean prevSpecial;
  private boolean prevInteract;
  private Bitmap bgStreet;
  private Bitmap bgCastle;
  private Bitmap tileDirt;
  private Bitmap tileBrick;
  private Bitmap propBox;
  private Bitmap propChain;
  private Bitmap propFlag;
  private Bitmap pickupCoin;
  private Bitmap pickupMed;
  private Bitmap pickupAmmo;
  private SpriteSet playerSprites;
  private SpriteSet femaleSprites;
  private SpriteSet adventurerSprites;
  private SpriteSet soldierSprites;
  private SpriteSet zombieSprites;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    setFocusable(true);
    initPaints();
    initCampaign(false);
    loadAssets();
    audioController.load(context);
    dispatchSnapshot(true);
  }

  private void initPaints() {
    skyPaint.setColor(Color.rgb(12, 18, 30));
    hazePaint.setColor(Color.argb(110, 233, 165, 82));
    roadPaint.setColor(Color.rgb(26, 31, 40));
    lanePaint.setColor(Color.argb(120, 248, 228, 188));
    buildingPaint.setColor(Color.rgb(31, 38, 52));
    panelPaint.setColor(Color.argb(220, 26, 35, 50));
    accentPaint.setColor(Color.rgb(217, 165, 82));
    textPaint.setColor(Color.rgb(245, 241, 232));
    textPaint.setTextSize(42f);
    smallTextPaint.setColor(Color.rgb(221, 206, 180));
    smallTextPaint.setTextSize(24f);
    enemyPaint.setColor(Color.rgb(198, 96, 96));
    elitePaint.setColor(Color.rgb(217, 165, 82));
    pickupPaint.setColor(Color.rgb(168, 255, 216));
    bulletPaint.setColor(Color.rgb(240, 209, 110));
    shadowPaint.setColor(Color.argb(90, 0, 0, 0));
  }

  private void initCampaign(boolean freshCampaign) {
    districts = new District[] {
        new District("Iron Market", "Mallet Voss", 4, 1),
        new District("Canal Steps", "Rook Saint", 4, 2),
        new District("Pawn Strip", "The Bolt Twins", 5, 2),
        new District("Tram Yard", "Needle Brann", 4, 3),
        new District("Needle Square", "Harbor Cain", 5, 3),
        new District("Salt Docks", "Brass Morrow", 4, 4),
        new District("Court Block", "Riot Vale", 5, 4),
        new District("Station Maze", "Warden Pike", 4, 5),
        new District("Old King Crest", "Kingbreaker Holt", 5, 5)
    };
    roster = new Partner[] {
        new Partner("Rook", "Striker", AssistType.STRIKER, 10f, Color.rgb(198, 96, 96), true),
        new Partner("Vale", "Gunner", AssistType.GUNNER, 12f, Color.rgb(217, 165, 82), true),
        new Partner("Ada", "Medic", AssistType.MEDIC, 15f, Color.rgb(125, 220, 174), true),
        new Partner("Mercer", "Tank", AssistType.TANK, 16f, Color.rgb(152, 167, 191), false),
        new Partner("Kade", "Striker", AssistType.STRIKER, 9f, Color.rgb(230, 119, 82), false),
        new Partner("Moss", "Gunner", AssistType.GUNNER, 11f, Color.rgb(179, 193, 96), false),
        new Partner("Pike", "Tank", AssistType.TANK, 18f, Color.rgb(128, 140, 168), false),
        new Partner("Nix", "Medic", AssistType.MEDIC, 14f, Color.rgb(104, 202, 199), false)
    };
    weapons = new Weapon[] {
        new Weapon("Street Pistol", 12, 14, 0.18f, 1.2f, 1, 0f, 900f, 96, true),
        new Weapon("Needle SMG", 24, 10, 0.08f, 1.45f, 1, 0.03f, 980f, 160, true),
        new Weapon("Checkpoint Shotgun", 6, 12, 0.62f, 1.6f, 5, 0.18f, 840f, 36, false),
        new Weapon("District Rifle", 10, 26, 0.28f, 1.55f, 1, 0.01f, 1160f, 60, false)
    };
    if (freshCampaign) {
      cash = 160;
      totalScore = 0;
      maxCombo = 0;
      capturedBlocks = 0;
      benchLevel = 0;
      reviveTokens = 1;
      recruitIndex = 3;
      highestUnlockedDistrict = 0;
      selectedDistrict = 0;
      currentWeaponIndex = 0;
      campaignCompleted = false;
      playerMaxHp = 120f;
      playerHp = playerMaxHp;
      staminaMax = 100f;
      stamina = staminaMax;
      armorMax = 45f;
      armor = armorMax;
      heat = 0f;
      finisher = 0f;
      combo = 0;
      comboTimer = 0f;
      for (int i = 0; i < districts.length; i++) {
        districts[i].clearedSectors = 0;
        districts[i].unlocked = i == 0;
      }
      for (int i = 0; i < roster.length; i++) {
        roster[i].cooldown = 0f;
        roster[i].loyalty = 1;
        roster[i].unlocked = i < 3;
      }
      activePartners[0] = roster[0];
      activePartners[1] = roster[1];
      for (int i = 0; i < weapons.length; i++) {
        weapons[i].tier = 1;
        weapons[i].clip = weapons[i].clipSize;
      }
      weapons[0].reserve = 96;
      weapons[1].reserve = 160;
      weapons[2].reserve = 36;
      weapons[3].reserve = 60;
      weapons[2].unlocked = false;
      weapons[3].unlocked = false;
    }
  }

  private void loadAssets() {
    bgStreet = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/bg.png");
    bgCastle = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/bg_castle.png");
    tileDirt = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Tiles/dirtCenter.png");
    tileBrick = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Tiles/brickWall.png");
    propBox = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Tiles/boxWarning.png");
    propChain = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Items/chain.png");
    propFlag = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Items/flagRedHanging.png");
    pickupCoin = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Items/coinGold.png");
    pickupMed = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Items/gemGreen.png");
    pickupAmmo = loadBitmap("game_art/kenney_platformer_art_deluxe/assets/Base pack/Items/bomb.png");
    playerSprites = loadSpriteSet("Player", "player");
    femaleSprites = loadSpriteSet("Female", "female");
    adventurerSprites = loadSpriteSet("Adventurer", "adventurer");
    soldierSprites = loadSpriteSet("Soldier", "soldier");
    zombieSprites = loadSpriteSet("Zombie", "zombie");
  }

  private SpriteSet loadSpriteSet(String folder, String prefix) {
    String root = "game_art/kenney_platformer_characters/assets/PNG/" + folder + "/Poses/";
    SpriteSet set = new SpriteSet();
    set.idle = loadBitmap(root + prefix + "_idle.png");
    set.stand = loadBitmap(root + prefix + "_stand.png");
    set.walk1 = loadBitmap(root + prefix + "_walk1.png");
    set.walk2 = loadBitmap(root + prefix + "_walk2.png");
    set.action1 = loadBitmap(root + prefix + "_action1.png");
    set.action2 = loadBitmap(root + prefix + "_action2.png");
    set.hurt = loadBitmap(root + prefix + "_hurt.png");
    set.jump = loadBitmap(root + prefix + "_jump.png");
    return set;
  }

  private Bitmap loadBitmap(String assetPath) {
    try {
      InputStream inputStream = getContext().getAssets().open(assetPath);
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
      inputStream.close();
      return bitmap;
    } catch (Exception ignored) {
      return null;
    }
  }

  public void setListener(Listener listener) {
    this.listener = listener;
    dispatchSnapshot(true);
  }

  public void setControlState(Control control, boolean pressed) {
    switch (control) {
      case LEFT:
        input.left = pressed;
        break;
      case RIGHT:
        input.right = pressed;
        break;
      case JUMP:
        input.jump = pressed;
        break;
      case LIGHT:
        input.light = pressed;
        break;
      case HEAVY:
        input.heavy = pressed;
        break;
      case DASH:
        input.dash = pressed;
        break;
      case SHOOT:
        input.shoot = pressed;
        break;
      case SPECIAL:
        input.special = pressed;
        break;
      case INTERACT:
        input.interact = pressed;
        break;
    }
  }

  public void beginNewCampaign() {
    initCampaign(true);
    activeDistrict = null;
    activeSectorNumber = 0;
    openDistrictBoard();
  }

  public void openDistrictBoard() {
    clearControlInput();
    activeDistrict = null;
    setState(GameState.DISTRICT_BOARD);
  }

  public void openSafehouse() {
    clearControlInput();
    setState(GameState.SAFEHOUSE);
  }

  public void deploySelectedDistrict() {
    District district = districts[selectedDistrict];
    if (!district.unlocked) {
      return;
    }
    if (district.clearedSectors >= district.totalSectors) {
      if (selectedDistrict < districts.length - 1 && districts[selectedDistrict + 1].unlocked) {
        selectedDistrict++;
        deploySelectedDistrict();
      }
      return;
    }
    activeDistrict = district;
    activeSectorNumber = district.clearedSectors + 1;
    prepareSector(district);
    setState(GameState.PLAYING);
  }

  private void prepareSector(District district) {
    enemies.clear();
    projectiles.clear();
    pickups.clear();
    spawnEvents.clear();
    hitSparks.clear();
    playerX = 180f;
    playerY = getHeight() > 0 ? getHeight() - 156f : 540f;
    playerVelY = 0f;
    playerOnGround = true;
    facingRight = true;
    attackTimer = 0f;
    attackLock = 0f;
    reloadTimer = 0f;
    gunCooldown = 0f;
    dashTimer = 0f;
    invulnerableTimer = 0f;
    hitstopTimer = 0f;
    combo = 0;
    comboTimer = 0f;
    sectorTime = 0f;
    resultReward = 0f;
    sectorRewardCash = 0f;
    sectorRewardScore = 0f;
    worldWidth = 2800f + district.threat * 360f + district.clearedSectors * 180f;
    exitGateX = worldWidth - 220f;
    float spacing = worldWidth / (district.threat + 4f);
    int waveCount = 4 + district.threat;
    for (int i = 0; i < waveCount; i++) {
      SpawnEvent event = new SpawnEvent();
      event.x = 620f + spacing * i + random.nextInt(180);
      event.count = 2 + district.threat / 2 + (i % 2);
      event.type = i % 5;
      spawnEvents.add(event);
    }
    SpawnEvent bossEvent = new SpawnEvent();
    bossEvent.x = exitGateX - 240f;
    bossEvent.count = 1;
    bossEvent.type = Enemy.LIEUTENANT;
    bossEvent.elite = true;
    spawnEvents.add(bossEvent);
  }

  public void pauseGame() {
    if (state == GameState.PLAYING) {
      setState(GameState.PAUSED);
    }
  }

  public void resumeGame() {
    if (state == GameState.PAUSED) {
      setState(GameState.PLAYING);
    }
  }

  public void returnToMenu() {
    clearControlInput();
    setState(GameState.MENU);
  }

  public void continueAfterResult() {
    if (activeDistrict == null) {
      openDistrictBoard();
      return;
    }
    if (campaignCompleted) {
      openDistrictBoard();
      return;
    }
    if (activeDistrict.clearedSectors >= activeDistrict.totalSectors) {
      openDistrictBoard();
      return;
    }
    prepareSector(activeDistrict);
    setState(GameState.PLAYING);
  }

  public void retrySector() {
    if (activeDistrict == null) {
      openDistrictBoard();
      return;
    }
    playerHp = playerMaxHp;
    stamina = staminaMax;
    armor = armorMax;
    finisher = 0f;
    heat = 0f;
    for (Weapon weapon : weapons) {
      weapon.clip = weapon.clipSize;
    }
    prepareSector(activeDistrict);
    setState(GameState.PLAYING);
  }

  public void cycleDistrict(int delta) {
    int target = selectedDistrict + delta;
    while (target >= 0 && target < districts.length && !districts[target].unlocked) {
      target += delta;
    }
    if (target >= 0 && target < districts.length) {
      selectedDistrict = target;
      audioController.playUiClick();
      dispatchSnapshot(true);
    }
  }

  public void triggerPartner(int slot) {
    if (slot < 0 || slot >= activePartners.length) {
      return;
    }
    if (state == GameState.PLAYING) {
      activateAssist(slot);
      return;
    }
    cyclePartnerAssignment(slot);
  }

  private void cyclePartnerAssignment(int slot) {
    Partner current = activePartners[slot];
    int start = 0;
    for (int i = 0; i < roster.length; i++) {
      if (roster[i] == current) {
        start = i;
        break;
      }
    }
    for (int offset = 1; offset < roster.length; offset++) {
      Partner candidate = roster[(start + offset) % roster.length];
      if (candidate.unlocked && candidate != activePartners[1 - slot]) {
        activePartners[slot] = candidate;
        audioController.playUiClick();
        dispatchSnapshot(true);
        return;
      }
    }
  }

  private void activateAssist(int slot) {
    Partner partner = activePartners[slot];
    if (partner == null || partner.cooldown > 0f) {
      return;
    }
    partner.cooldown = partner.cooldownMax;
    partner.loyalty = Math.min(5, partner.loyalty + 1);
    audioController.playUiClick();
    if (partner.assistType == AssistType.STRIKER) {
      int hits = 0;
      for (Enemy enemy : enemies) {
        if (!enemy.dead && Math.abs(enemy.x - playerX) < 260f) {
          damageEnemy(enemy, 26 + partner.loyalty * 2, 200f * directionSign(), true, "TAG");
          hits++;
          if (hits >= 3) {
            break;
          }
        }
      }
    } else if (partner.assistType == AssistType.GUNNER) {
      for (int i = 0; i < 6; i++) {
        Projectile projectile = new Projectile();
        projectile.x = playerX + 40f;
        projectile.y = playerY - 96f - i * 4f;
        projectile.vx = 1100f + i * 20f;
        projectile.vy = (random.nextFloat() - 0.5f) * 40f;
        projectile.life = 0.7f;
        projectile.damage = 11 + partner.loyalty;
        projectile.fromPlayer = true;
        projectile.color = partner.accentColor;
        projectile.radius = 8f;
        projectiles.add(projectile);
      }
    } else if (partner.assistType == AssistType.MEDIC) {
      playerHp = Math.min(playerMaxHp, playerHp + 26f + partner.loyalty * 2f);
      heat = Math.max(0f, heat - 24f);
      armor = Math.min(armorMax, armor + 5f);
      addSpark(playerX, playerY - 140f, "PATCH", partner.accentColor, 24f);
    } else if (partner.assistType == AssistType.TANK) {
      armor = Math.min(armorMax, armor + 18f);
      for (Enemy enemy : enemies) {
        if (!enemy.dead && Math.abs(enemy.x - playerX) < 180f) {
          enemy.hurtTimer = 0.4f;
          enemy.vx = 220f * Math.signum(enemy.x - playerX);
          enemy.flashTimer = 0.15f;
        }
      }
      addSpark(playerX, playerY - 120f, "SHOVE", partner.accentColor, 26f);
    }
    dispatchSnapshot(true);
  }

  public void cycleWeapon() {
    int start = currentWeaponIndex;
    do {
      currentWeaponIndex = (currentWeaponIndex + 1) % weapons.length;
      if (weapons[currentWeaponIndex].unlocked) {
        audioController.playUiClick();
        dispatchSnapshot(true);
        return;
      }
    } while (currentWeaponIndex != start);
  }

  public void reloadWeapon() {
    Weapon weapon = currentWeapon();
    if (weapon == null || reloadTimer > 0f || weapon.reserve <= 0 || weapon.clip >= weapon.clipSize) {
      return;
    }
    reloadTimer = weapon.reloadTime;
    audioController.playUiClick();
    dispatchSnapshot(true);
  }

  public void toggleMute() {
    audioController.setMuted(!audioController.isMuted());
  }

  public void buyFieldMeds() {
    if (cash < 45) {
      return;
    }
    cash -= 45;
    playerHp = Math.min(playerMaxHp, playerHp + 42f);
    heat = Math.max(0f, heat - 14f);
    audioController.playPickup();
    dispatchSnapshot(true);
  }

  public void buyAmmoCache() {
    if (cash < 40) {
      return;
    }
    cash -= 40;
    for (Weapon weapon : weapons) {
      if (weapon.unlocked) {
        weapon.reserve += weapon.clipSize * 2;
      }
    }
    audioController.playPickup();
    dispatchSnapshot(true);
  }

  public void upgradeWeaponBench() {
    int cost = 70 + benchLevel * 30;
    if (cash < cost) {
      return;
    }
    cash -= cost;
    benchLevel++;
    if (benchLevel >= 1) {
      weapons[2].unlocked = true;
    }
    if (benchLevel >= 2) {
      weapons[3].unlocked = true;
    }
    for (Weapon weapon : weapons) {
      weapon.tier = 1 + benchLevel;
      if (weapon.unlocked) {
        weapon.reserve += weapon.clipSize;
      }
    }
    audioController.playPickup();
    dispatchSnapshot(true);
  }

  public void recruitPartner() {
    int cost = 95 + Math.max(0, recruitIndex - 3) * 25;
    if (cash < cost) {
      return;
    }
    if (recruitIndex >= roster.length) {
      cash -= cost;
      reviveTokens++;
      audioController.playPickup();
      dispatchSnapshot(true);
      return;
    }
    cash -= cost;
    roster[recruitIndex].unlocked = true;
    activePartners[1] = roster[recruitIndex];
    recruitIndex++;
    audioController.playPickup();
    dispatchSnapshot(true);
  }

  private Weapon currentWeapon() {
    if (currentWeaponIndex < 0 || currentWeaponIndex >= weapons.length) {
      return null;
    }
    return weapons[currentWeaponIndex];
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    running = true;
    gameThread = new Thread(this, "crownblock-hunt-loop");
    gameThread.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    running = false;
    if (gameThread != null) {
      try {
        gameThread.join();
      } catch (InterruptedException ignored) {
      }
      gameThread = null;
    }
  }

  @Override
  public void run() {
    long previous = System.nanoTime();
    float accumulator = 0f;
    while (running) {
      long now = System.nanoTime();
      float frameDt = (now - previous) / 1_000_000_000f;
      previous = now;
      if (frameDt > 0.05f) {
        frameDt = 0.05f;
      }
      accumulator += frameDt;
      while (accumulator >= FIXED_DT) {
        update(FIXED_DT);
        accumulator -= FIXED_DT;
      }
      drawFrame();
    }
  }

  private void update(float dt) {
    ambientTimer += dt;
    screenShake.update(dt);
    for (Partner partner : roster) {
      if (partner.cooldown > 0f) {
        partner.cooldown = Math.max(0f, partner.cooldown - dt);
      }
    }
    snapshotTimer += dt;
    if (state == GameState.PLAYING) {
      updatePlaying(dt);
    } else if (state == GameState.RESULT || state == GameState.GAME_OVER) {
      combo = 0;
    }
    if (snapshotTimer >= 0.1f) {
      snapshotTimer = 0f;
      dispatchSnapshot(false);
    }
  }

  private void updatePlaying(float dt) {
    if (hitstopTimer > 0f) {
      hitstopTimer = Math.max(0f, hitstopTimer - dt);
      updateEffects(dt);
      return;
    }
    sectorTime += dt;
    if (attackLock > 0f) {
      attackLock = Math.max(0f, attackLock - dt);
    }
    if (reloadTimer > 0f) {
      reloadTimer = Math.max(0f, reloadTimer - dt);
      if (reloadTimer == 0f) {
        finishReload();
      }
    }
    if (gunCooldown > 0f) {
      gunCooldown = Math.max(0f, gunCooldown - dt);
    }
    if (dashTimer > 0f) {
      dashTimer = Math.max(0f, dashTimer - dt);
    }
    if (invulnerableTimer > 0f) {
      invulnerableTimer = Math.max(0f, invulnerableTimer - dt);
    }
    if (attackTimer > 0f) {
      float previousTimer = attackTimer;
      attackTimer = Math.max(0f, attackTimer - dt);
      resolveAttack(previousTimer, attackTimer);
    }
    handlePlayerMovement(dt);
    handleActionButtons();
    updateSpawns();
    updateEnemies(dt);
    updateProjectiles(dt);
    updatePickups(dt);
    updateEffects(dt);
    cameraX = clamp(playerX - getWidth() * 0.34f, 0f, Math.max(0f, worldWidth - getWidth()));
    stamina = Math.min(staminaMax, stamina + dt * 11f);
    armor = Math.min(armorMax, armor + dt * 1.2f);
    heat = Math.max(0f, heat - dt * 2.6f);
    if (comboTimer > 0f) {
      comboTimer = Math.max(0f, comboTimer - dt);
      if (comboTimer == 0f) {
        combo = 0;
      }
    }
    prevJump = input.jump;
    prevLight = input.light;
    prevHeavy = input.heavy;
    prevDash = input.dash;
    prevShoot = input.shoot;
    prevSpecial = input.special;
    prevInteract = input.interact;
    if (allEventsCleared() && enemies.isEmpty() && playerX > exitGateX - 120f) {
      completeSector();
    }
  }

  private void handlePlayerMovement(float dt) {
    float move = 0f;
    if (input.left) {
      move -= 1f;
    }
    if (input.right) {
      move += 1f;
    }
    float speed = dashTimer > 0f ? 520f : 270f;
    if (attackTimer > 0f && attackStyle == 2) {
      speed *= 0.72f;
    }
    if (move != 0f) {
      facingRight = move > 0f;
      playerX += move * speed * dt;
      stamina = Math.max(0f, stamina - dt * 2.4f);
    }
    if (dashTimer > 0f) {
      playerX += directionSign() * 220f * dt;
    }
    playerX = clamp(playerX, 90f, exitGateX + 40f);
    boolean jumpPressed = input.jump && !prevJump;
    if (jumpPressed && playerOnGround && stamina >= 12f) {
      playerVelY = -650f;
      playerOnGround = false;
      stamina -= 12f;
    }
    if (!playerOnGround) {
      playerVelY += 1450f * dt;
      playerY += playerVelY * dt;
      float groundLevel = getHeight() - 156f;
      if (playerY >= groundLevel) {
        playerY = groundLevel;
        playerVelY = 0f;
        playerOnGround = true;
      }
    } else {
      playerY = getHeight() - 156f;
    }
  }

  private void handleActionButtons() {
    boolean lightPressed = input.light && !prevLight;
    boolean heavyPressed = input.heavy && !prevHeavy;
    boolean dashPressed = input.dash && !prevDash;
    boolean shootPressed = input.shoot;
    boolean specialPressed = input.special && !prevSpecial;
    boolean interactPressed = input.interact && !prevInteract;

    if (dashPressed && dashTimer <= 0f && stamina >= 14f) {
      dashTimer = 0.18f;
      invulnerableTimer = 0.28f;
      stamina -= 14f;
      screenShake.trigger(0.1f, 4f);
      addSpark(playerX, playerY - 80f, "STEP", Color.rgb(168, 255, 216), 18f);
    }
    if (lightPressed && attackTimer <= 0f && attackLock <= 0f && reloadTimer <= 0f) {
      attackStyle = 1;
      attackTimer = 0.24f;
      attackResolved = false;
      attackLock = 0.26f;
    }
    if (heavyPressed && attackTimer <= 0f && attackLock <= 0f && reloadTimer <= 0f && stamina >= 10f) {
      attackStyle = 2;
      attackTimer = 0.38f;
      attackResolved = false;
      attackLock = 0.42f;
      stamina -= 10f;
    }
    if (specialPressed && attackTimer <= 0f && attackLock <= 0f && finisher >= 100f) {
      attackStyle = 3;
      attackTimer = 0.48f;
      attackResolved = false;
      attackLock = 0.52f;
      finisher = 0f;
      screenShake.trigger(0.22f, 10f);
      audioController.playHeavy();
    }
    if (shootPressed) {
      tryShoot();
    }
    if (interactPressed) {
      collectNearbyPickup();
    }
  }

  private void resolveAttack(float previousTimer, float currentTimer) {
    if (attackResolved) {
      return;
    }
    float triggerWindow = attackStyle == 1 ? 0.13f : attackStyle == 2 ? 0.22f : 0.28f;
    if (previousTimer >= triggerWindow && currentTimer < triggerWindow) {
      attackResolved = true;
      if (attackStyle == 1) {
        performMeleeAttack(135f, 18f + benchLevel * 2f, 120f, false, "CUT");
        audioController.playHit();
      } else if (attackStyle == 2) {
        performMeleeAttack(175f, 34f + benchLevel * 3f, 240f, true, "CRACK");
        screenShake.trigger(0.12f, 8f);
        audioController.playHeavy();
      } else {
        performMeleeAttack(230f, 52f + benchLevel * 4f, 320f, true, "STAMP");
        for (Enemy enemy : enemies) {
          if (!enemy.dead && Math.abs(enemy.x - playerX) < 260f) {
            enemy.hurtTimer = 0.45f;
            enemy.vx = 240f * Math.signum(enemy.x - playerX);
          }
        }
      }
    }
  }

  private void performMeleeAttack(float range, float damage, float knockback, boolean pierce, String label) {
    int landed = 0;
    for (Enemy enemy : enemies) {
      if (enemy.dead) {
        continue;
      }
      if (Math.abs(enemy.x - playerX) <= range && ((facingRight && enemy.x >= playerX - 10f) || (!facingRight && enemy.x <= playerX + 10f))) {
        damageEnemy(enemy, damage, knockback * directionSign(), pierce, label);
        landed++;
        if (!pierce && landed > 0) {
          break;
        }
      }
    }
    if (landed > 0) {
      comboTimer = 2.8f;
      combo += landed;
      maxCombo = Math.max(maxCombo, combo);
      finisher = Math.min(100f, finisher + landed * 11f);
    }
  }

  private void tryShoot() {
    Weapon weapon = currentWeapon();
    if (weapon == null || !weapon.unlocked || gunCooldown > 0f || reloadTimer > 0f) {
      return;
    }
    if (weapon.clip <= 0) {
      reloadWeapon();
      return;
    }
    weapon.clip--;
    gunCooldown = weapon.fireCooldown;
    float originX = playerX + directionSign() * 58f;
    float originY = playerY - 88f;
    for (int i = 0; i < weapon.pellets; i++) {
      Projectile projectile = new Projectile();
      projectile.x = originX;
      projectile.y = originY;
      projectile.vx = directionSign() * weapon.projectileSpeed;
      projectile.vy = (random.nextFloat() - 0.5f) * weapon.spread * 420f;
      projectile.life = 0.85f;
      projectile.damage = weapon.damage + weapon.tier * 2;
      projectile.fromPlayer = true;
      projectile.color = Color.rgb(240, 209, 110);
      projectile.radius = weapon.pellets > 1 ? 6f : 8f;
      projectiles.add(projectile);
    }
    heat = Math.min(100f, heat + 2.2f + weapon.pellets * 0.5f);
    finisher = Math.min(100f, finisher + 1.2f);
    screenShake.trigger(0.05f, 2.5f + weapon.pellets);
    audioController.playShoot();
  }

  private void finishReload() {
    Weapon weapon = currentWeapon();
    if (weapon == null) {
      return;
    }
    int needed = weapon.clipSize - weapon.clip;
    int loaded = Math.min(needed, weapon.reserve);
    weapon.reserve -= loaded;
    weapon.clip += loaded;
    audioController.playUiClick();
  }

  private void collectNearbyPickup() {
    Iterator<Pickup> iterator = pickups.iterator();
    while (iterator.hasNext()) {
      Pickup pickup = iterator.next();
      if (Math.abs(pickup.x - playerX) < 110f) {
        applyPickup(pickup);
        iterator.remove();
        return;
      }
    }
  }

  private void applyPickup(Pickup pickup) {
    if (pickup.type == Pickup.CASH) {
      cash += pickup.amount;
      addSpark(pickup.x, pickup.y - 34f, "+$" + pickup.amount, Color.rgb(168, 255, 216), 16f);
    } else if (pickup.type == Pickup.AMMO) {
      for (Weapon weapon : weapons) {
        if (weapon.unlocked) {
          weapon.reserve += pickup.amount;
        }
      }
      addSpark(pickup.x, pickup.y - 34f, "AMMO", Color.rgb(217, 165, 82), 16f);
    } else {
      playerHp = Math.min(playerMaxHp, playerHp + pickup.amount);
      addSpark(pickup.x, pickup.y - 34f, "MED", Color.rgb(125, 220, 174), 16f);
    }
    audioController.playPickup();
  }

  private void updateSpawns() {
    for (SpawnEvent spawnEvent : spawnEvents) {
      if (!spawnEvent.triggered && playerX > spawnEvent.x - 480f) {
        spawnEvent.triggered = true;
        spawnWave(spawnEvent);
      }
    }
  }

  private void spawnWave(SpawnEvent spawnEvent) {
    for (int i = 0; i < spawnEvent.count; i++) {
      Enemy enemy = new Enemy();
      enemy.type = spawnEvent.type == Enemy.LIEUTENANT ? Enemy.LIEUTENANT : (spawnEvent.type + i) % 5;
      enemy.elite = spawnEvent.elite || enemy.type == Enemy.LIEUTENANT;
      enemy.x = spawnEvent.x + i * 54f + random.nextInt(60);
      enemy.y = getHeight() - 156f;
      enemy.hp = enemy.type == Enemy.BRUTE ? 72f : enemy.type == Enemy.GUNNER ? 42f : enemy.type == Enemy.LIEUTENANT ? 170f : 48f;
      enemy.maxHp = enemy.hp;
      enemy.speed = enemy.type == Enemy.RUSHER ? 172f : enemy.type == Enemy.BRUTE ? 86f : enemy.type == Enemy.LIEUTENANT ? 118f : 120f;
      enemy.attackCooldown = 0.6f + random.nextFloat() * 0.4f;
      enemy.spawnTimer = i * 0.08f;
      enemies.add(enemy);
    }
  }

  private void updateEnemies(float dt) {
    Iterator<Enemy> iterator = enemies.iterator();
    while (iterator.hasNext()) {
      Enemy enemy = iterator.next();
      if (enemy.spawnTimer > 0f) {
        enemy.spawnTimer = Math.max(0f, enemy.spawnTimer - dt);
        continue;
      }
      if (enemy.dead) {
        iterator.remove();
        continue;
      }
      if (enemy.hurtTimer > 0f) {
        enemy.hurtTimer = Math.max(0f, enemy.hurtTimer - dt);
      }
      if (enemy.flashTimer > 0f) {
        enemy.flashTimer = Math.max(0f, enemy.flashTimer - dt);
      }
      if (enemy.attackCooldown > 0f) {
        enemy.attackCooldown = Math.max(0f, enemy.attackCooldown - dt);
      }
      enemy.x += enemy.vx * dt;
      enemy.vx *= 0.88f;
      float dx = playerX - enemy.x;
      enemy.facingRight = dx > 0f;
      if (enemy.hurtTimer <= 0f) {
        if (enemy.type == Enemy.GUNNER && Math.abs(dx) > 180f && Math.abs(dx) < 540f && enemy.attackCooldown == 0f) {
          Projectile projectile = new Projectile();
          projectile.x = enemy.x + Math.signum(dx) * 34f;
          projectile.y = enemy.y - 84f;
          projectile.vx = Math.signum(dx) * 720f;
          projectile.vy = 0f;
          projectile.life = 0.85f;
          projectile.damage = enemy.elite ? 16 : 12;
          projectile.fromPlayer = false;
          projectile.color = Color.rgb(198, 96, 96);
          projectile.radius = 7f;
          projectiles.add(projectile);
          enemy.attackCooldown = enemy.elite ? 0.8f : 1.1f;
        } else {
          float step = Math.signum(dx) * enemy.speed * dt;
          if (Math.abs(dx) > 64f) {
            enemy.x += step;
          } else if (enemy.attackCooldown == 0f) {
            enemy.attackCooldown = enemy.type == Enemy.LIEUTENANT ? 0.9f : 1.15f;
            hurtPlayer(enemy.type == Enemy.BRUTE ? 17f : enemy.type == Enemy.LIEUTENANT ? 22f : 11f, Math.signum(dx) * 120f);
          }
        }
      }
      enemy.x = clamp(enemy.x, 80f, worldWidth - 60f);
    }
  }

  private void updateProjectiles(float dt) {
    Iterator<Projectile> iterator = projectiles.iterator();
    while (iterator.hasNext()) {
      Projectile projectile = iterator.next();
      projectile.x += projectile.vx * dt;
      projectile.y += projectile.vy * dt;
      projectile.life -= dt;
      if (projectile.life <= 0f) {
        iterator.remove();
        continue;
      }
      if (projectile.fromPlayer) {
        boolean hit = false;
        for (Enemy enemy : enemies) {
          if (!enemy.dead && enemy.spawnTimer == 0f && Math.abs(enemy.x - projectile.x) < 42f && Math.abs((enemy.y - 88f) - projectile.y) < 48f) {
            damageEnemy(enemy, projectile.damage, 96f * Math.signum(projectile.vx), false, "SHOT");
            hit = true;
            break;
          }
        }
        if (hit) {
          iterator.remove();
        }
      } else if (Math.abs(projectile.x - playerX) < 36f && Math.abs(projectile.y - (playerY - 80f)) < 48f) {
        hurtPlayer(projectile.damage, Math.signum(projectile.vx) * 80f);
        iterator.remove();
      }
    }
  }

  private void updatePickups(float dt) {
    for (Pickup pickup : pickups) {
      pickup.bob += dt * 2.4f;
      if (Math.abs(pickup.x - playerX) < 70f) {
        applyPickup(pickup);
        pickup.amount = -1;
      }
    }
    Iterator<Pickup> iterator = pickups.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().amount < 0) {
        iterator.remove();
      }
    }
  }

  private void updateEffects(float dt) {
    Iterator<HitSpark> iterator = hitSparks.iterator();
    while (iterator.hasNext()) {
      HitSpark spark = iterator.next();
      spark.life -= dt;
      spark.y -= dt * 36f;
      if (spark.life <= 0f) {
        iterator.remove();
      }
    }
  }

  private void damageEnemy(Enemy enemy, float damage, float knockback, boolean heavy, String label) {
    enemy.hp -= damage;
    enemy.hurtTimer = heavy ? 0.28f : 0.16f;
    enemy.flashTimer = 0.12f;
    enemy.vx += knockback;
    if (enemy.hp <= 0f) {
      enemy.dead = true;
      scoreKill(enemy);
      maybeDropPickup(enemy);
      addSpark(enemy.x, enemy.y - 110f, enemy.elite ? "BREAK" : label, enemy.elite ? Color.rgb(217, 165, 82) : Color.rgb(245, 241, 232), heavy ? 30f : 22f);
    } else {
      addSpark(enemy.x, enemy.y - 104f, label, heavy ? Color.rgb(217, 165, 82) : Color.rgb(245, 241, 232), heavy ? 24f : 18f);
    }
    comboTimer = 2.8f;
    combo++;
    maxCombo = Math.max(maxCombo, combo);
    finisher = Math.min(100f, finisher + (heavy ? 8f : 4f));
    totalScore += heavy ? 22 : 12;
    hitstopTimer = heavy ? 0.06f : 0.03f;
    audioController.playHit();
  }

  private void scoreKill(Enemy enemy) {
    int rewardCash = enemy.type == Enemy.LIEUTENANT ? 48 : enemy.type == Enemy.BRUTE ? 24 : 14;
    int rewardScore = enemy.type == Enemy.LIEUTENANT ? 120 : enemy.type == Enemy.GUNNER ? 54 : 38;
    cash += rewardCash;
    totalScore += rewardScore;
    sectorRewardCash += rewardCash;
    sectorRewardScore += rewardScore;
  }

  private void maybeDropPickup(Enemy enemy) {
    float roll = random.nextFloat();
    Pickup pickup = new Pickup();
    pickup.x = enemy.x;
    pickup.y = enemy.y - 12f;
    if (roll < 0.38f) {
      pickup.type = Pickup.CASH;
      pickup.amount = 12 + random.nextInt(10);
    } else if (roll < 0.58f) {
      pickup.type = Pickup.AMMO;
      pickup.amount = 8 + random.nextInt(8);
    } else if (roll < 0.72f) {
      pickup.type = Pickup.MED;
      pickup.amount = 12 + random.nextInt(10);
    } else {
      return;
    }
    pickups.add(pickup);
  }

  private void hurtPlayer(float damage, float knockback) {
    if (invulnerableTimer > 0f) {
      return;
    }
    float absorbed = Math.min(armor, damage * 0.45f);
    armor -= absorbed;
    playerHp -= damage - absorbed * 0.35f;
    heat = Math.min(100f, heat + damage * 1.8f);
    combo = 0;
    comboTimer = 0f;
    invulnerableTimer = 0.55f;
    playerX -= knockback;
    screenShake.trigger(0.16f, 8f);
    audioController.playFail();
    addSpark(playerX, playerY - 124f, "HIT", Color.rgb(198, 96, 96), 24f);
    if (playerHp <= 0f) {
      if (reviveTokens > 0) {
        reviveTokens--;
        playerHp = playerMaxHp * 0.45f;
        stamina = staminaMax;
        armor = armorMax * 0.4f;
        heat = 22f;
        addSpark(playerX, playerY - 148f, "REVIVE", Color.rgb(125, 220, 174), 28f);
      } else {
        setState(GameState.GAME_OVER);
      }
    }
  }

  private void completeSector() {
    if (activeDistrict == null) {
      return;
    }
    activeDistrict.clearedSectors++;
    capturedBlocks++;
    playerHp = Math.min(playerMaxHp, playerHp + 18f);
    stamina = staminaMax;
    armor = Math.min(armorMax, armor + 8f);
    finisher = Math.min(100f, finisher + 18f);
    sectorRewardCash += 30 + activeDistrict.threat * 12f;
    cash += 30 + activeDistrict.threat * 12;
    if (activeDistrict.clearedSectors >= activeDistrict.totalSectors) {
      if (selectedDistrict < districts.length - 1) {
        highestUnlockedDistrict = Math.max(highestUnlockedDistrict, selectedDistrict + 1);
        districts[selectedDistrict + 1].unlocked = true;
      } else {
        campaignCompleted = true;
      }
    }
    if (campaignCompleted) {
      audioController.playWin();
    } else {
      audioController.playPickup();
    }
    setState(GameState.RESULT);
  }

  private boolean allEventsCleared() {
    for (SpawnEvent spawnEvent : spawnEvents) {
      if (!spawnEvent.triggered) {
        return false;
      }
    }
    return true;
  }

  private void addSpark(float x, float y, String label, int color, float size) {
    HitSpark spark = new HitSpark();
    spark.x = x;
    spark.y = y;
    spark.life = 0.5f;
    spark.size = size;
    spark.color = color;
    spark.label = label;
    hitSparks.add(spark);
  }

  private float directionSign() {
    return facingRight ? 1f : -1f;
  }

  private void clearControlInput() {
    input.clear();
    prevJump = false;
    prevLight = false;
    prevHeavy = false;
    prevDash = false;
    prevShoot = false;
    prevSpecial = false;
    prevInteract = false;
  }

  private void setState(GameState newState) {
    if (state == newState) {
      dispatchSnapshot(true);
      return;
    }
    state = newState;
    clearControlInput();
    dispatchSnapshot(true);
    if (listener != null) {
      listener.onStateChanged(newState);
    }
  }

  private void dispatchSnapshot(boolean immediate) {
    UiSnapshot snapshot = buildSnapshot();
    lastSnapshot = snapshot;
    if (listener != null) {
      listener.onSnapshot(snapshot);
    }
    if (immediate) {
      snapshotTimer = 0f;
    }
  }

  private UiSnapshot buildSnapshot() {
    UiSnapshot snapshot = new UiSnapshot();
    District boardDistrict = districts[selectedDistrict];
    snapshot.blockTitle = activeDistrict != null && (state == GameState.PLAYING || state == GameState.PAUSED || state == GameState.RESULT || state == GameState.GAME_OVER)
        ? activeDistrict.name + " B" + activeSectorNumber
        : boardDistrict.name;
    snapshot.objective = state == GameState.PLAYING
        ? (allEventsCleared() ? "Reach the exit banner" : "Crack patrol lines and push forward")
        : state == GameState.RESULT
        ? "Block stamped and route secured"
        : state == GameState.GAME_OVER
        ? "Crew wiped before the stamp"
        : "Select routes, prepare partners, and take blocks";
    snapshot.control = "Control " + boardDistrict.controlPercent() + "%  Districts " + totalControlledDistricts() + "/9";
    snapshot.health = Math.round(playerHp) + " / " + Math.round(playerMaxHp);
    snapshot.stamina = Math.round(stamina) + " / " + Math.round(staminaMax);
    snapshot.armor = Math.round(armor) + " / " + Math.round(armorMax) + "  Revive " + reviveTokens;
    snapshot.heat = Math.round(heat) + "%  Bench " + benchLevel;
    snapshot.cash = "$" + cash + "  Score " + totalScore;
    Weapon weapon = currentWeapon();
    if (weapon != null) {
      snapshot.weapon = weapon.name + " T" + weapon.tier;
      snapshot.ammo = weapon.clip + " / " + weapon.reserve;
    }
    snapshot.combo = combo > 0 ? "x" + combo + "  Best x" + maxCombo : "Ready";
    snapshot.finisher = Math.round(finisher) + "%";
    snapshot.partnerOne = formatPartner(activePartners[0]);
    snapshot.partnerTwo = formatPartner(activePartners[1]);
    snapshot.menuSummary = "Old-city districts are split across 9 territories and 40 total blocks. Lead a two-partner crew, push sector by sector, rotate weapons, and keep heat under control.";
    snapshot.boardSelection = boardDistrict.name + "  Threat " + boardDistrict.threat + "  Blocks " + boardDistrict.clearedSectors + "/" + boardDistrict.totalSectors;
    snapshot.boardSummary = "Boss " + boardDistrict.boss + "  Control " + boardDistrict.controlPercent() + "%  Campaign " + totalControlledBlocks() + "/40 blocks.";
    snapshot.safehouseSummary = "Cash $" + cash + "  Bench " + benchLevel + "  Recruits " + countUnlockedPartners() + "/8  Shotgun " + (weapons[2].unlocked ? "online" : "locked") + "  Rifle " + (weapons[3].unlocked ? "online" : "locked");
    snapshot.pauseSummary = activeDistrict == null ? "No route active." : activeDistrict.name + " block " + activeSectorNumber + "  Time " + formatTime(sectorTime) + "  Enemies " + enemies.size();
    snapshot.resultSummary = (activeDistrict == null ? "" : activeDistrict.name + " block " + activeSectorNumber)
        + "  Cash +" + Math.round(sectorRewardCash)
        + "  Score +" + Math.round(sectorRewardScore)
        + "  Combo x" + maxCombo
        + (campaignCompleted ? "  Districts secured." : "");
    snapshot.gameOverSummary = (activeDistrict == null ? "" : activeDistrict.name + " block " + activeSectorNumber)
        + "  Heat " + Math.round(heat)
        + "%  Cash $" + cash
        + "  Retry to keep the route alive.";
    snapshot.helpSummary = "Hold shoot for firearms. Tap light and heavy to chain melee. Dash breaks pressure, jump clears low swings, interact grabs drops, partner buttons call assists in battle and rotate crew outside battle.";
    snapshot.partnerOneAction = state == GameState.PLAYING ? "CALL A" : "ROTATE A";
    snapshot.partnerTwoAction = state == GameState.PLAYING ? "CALL B" : "ROTATE B";
    snapshot.swapAction = state == GameState.PLAYING ? "SWAP" : "LOAD";
    snapshot.reloadAction = state == GameState.PLAYING ? "RELOAD" : "CHECK";
    snapshot.interactAction = state == GameState.PLAYING ? "PICK" : "NOTE";
    return snapshot;
  }

  private String formatPartner(Partner partner) {
    if (partner == null) {
      return "Empty";
    }
    String ready = partner.cooldown > 0f ? Math.round(partner.cooldown) + "s" : "ready";
    return partner.name + "  " + partner.role + "  " + ready;
  }

  private int totalControlledDistricts() {
    int count = 0;
    for (District district : districts) {
      if (district.clearedSectors >= district.totalSectors) {
        count++;
      }
    }
    return count;
  }

  private int totalControlledBlocks() {
    int total = 0;
    for (District district : districts) {
      total += district.clearedSectors;
    }
    return total;
  }

  private int countUnlockedPartners() {
    int count = 0;
    for (Partner partner : roster) {
      if (partner.unlocked) {
        count++;
      }
    }
    return count;
  }

  private String formatTime(float seconds) {
    int total = Math.max(0, Math.round(seconds));
    int mins = total / 60;
    int secs = total % 60;
    return mins + ":" + (secs < 10 ? "0" : "") + secs;
  }

  private void drawFrame() {
    SurfaceHolder holder = getHolder();
    if (!holder.getSurface().isValid()) {
      return;
    }
    Canvas canvas = holder.lockCanvas();
    if (canvas == null) {
      return;
    }
    try {
      canvas.drawColor(Color.rgb(10, 15, 24));
      if (state == GameState.PLAYING || state == GameState.PAUSED || state == GameState.RESULT || state == GameState.GAME_OVER) {
        drawSector(canvas);
      } else if (state == GameState.SAFEHOUSE) {
        drawSafehouse(canvas);
      } else {
        drawDistrictBoard(canvas, state == GameState.MENU);
      }
    } finally {
      holder.unlockCanvasAndPost(canvas);
    }
  }

  private void drawSector(Canvas canvas) {
    drawBackdrops(canvas, activeDistrict != null && activeDistrict.threat >= 4);
    float shakeX = (random.nextFloat() - 0.5f) * screenShake.getIntensity();
    float shakeY = (random.nextFloat() - 0.5f) * screenShake.getIntensity();
    canvas.save();
    canvas.translate(-cameraX + shakeX, shakeY);
    drawWorldProps(canvas);
    for (Pickup pickup : pickups) {
      drawPickup(canvas, pickup);
    }
    for (Projectile projectile : projectiles) {
      bulletPaint.setColor(projectile.color);
      canvas.drawCircle(projectile.x, projectile.y, projectile.radius, bulletPaint);
    }
    for (Enemy enemy : enemies) {
      drawEnemy(canvas, enemy);
    }
    drawPlayer(canvas);
    drawExitGate(canvas);
    for (HitSpark spark : hitSparks) {
      smallTextPaint.setColor(spark.color);
      smallTextPaint.setTextSize(spark.size);
      canvas.drawText(spark.label, spark.x, spark.y, smallTextPaint);
    }
    canvas.restore();
  }

  private void drawBackdrops(Canvas canvas, boolean castleVariant) {
    canvas.drawRect(0f, 0f, getWidth(), getHeight(), skyPaint);
    Bitmap background = castleVariant && bgCastle != null ? bgCastle : bgStreet;
    if (background != null) {
      float scaledHeight = getHeight() * 0.62f;
      float scaledWidth = background.getWidth() * (scaledHeight / background.getHeight());
      float offset = (cameraX * 0.18f) % scaledWidth;
      for (float x = -offset - scaledWidth; x < getWidth() + scaledWidth; x += scaledWidth) {
        tempRect.set(x, 0f, x + scaledWidth, scaledHeight);
        canvas.drawBitmap(background, null, tempRect, null);
      }
    }
    for (int i = 0; i < 7; i++) {
      float width = 130f + i * 24f;
      float height = 180f + (i % 4) * 60f;
      float x = i * (getWidth() / 6.5f) - (cameraX * 0.22f % 140f);
      tempRect.set(x, getHeight() * 0.28f + (i % 2) * 18f, x + width, getHeight() * 0.28f + height);
      canvas.drawRect(tempRect, buildingPaint);
    }
    canvas.drawRect(0f, getHeight() - 130f, getWidth(), getHeight(), roadPaint);
    for (int i = 0; i < 16; i++) {
      float x = i * 110f - (cameraX % 110f);
      canvas.drawRect(x, getHeight() - 92f, x + 56f, getHeight() - 84f, lanePaint);
    }
  }

  private void drawWorldProps(Canvas canvas) {
    float ground = getHeight() - 130f;
    for (float x = 0f; x < worldWidth; x += 140f) {
      if (tileDirt != null) {
        tempRect.set(x, ground, x + 140f, getHeight());
        canvas.drawBitmap(tileDirt, null, tempRect, null);
      } else {
        tempRect.set(x, ground, x + 140f, getHeight());
        canvas.drawRect(tempRect, roadPaint);
      }
    }
    for (SpawnEvent spawnEvent : spawnEvents) {
      if (propFlag != null) {
        tempRect.set(spawnEvent.x - 28f, ground - 180f, spawnEvent.x + 32f, ground - 40f);
        canvas.drawBitmap(propFlag, null, tempRect, null);
      }
      if (propBox != null) {
        tempRect.set(spawnEvent.x - 26f, ground - 44f, spawnEvent.x + 34f, ground + 10f);
        canvas.drawBitmap(propBox, null, tempRect, null);
      }
    }
    if (propChain != null) {
      for (float x = 460f; x < worldWidth - 200f; x += 620f) {
        tempRect.set(x, ground - 180f, x + 30f, ground - 36f);
        canvas.drawBitmap(propChain, null, tempRect, null);
      }
    }
  }

  private void drawExitGate(Canvas canvas) {
    float ground = getHeight() - 130f;
    panelPaint.setColor(Color.argb(220, 40, 45, 58));
    tempRect.set(exitGateX - 42f, ground - 168f, exitGateX + 48f, ground + 6f);
    canvas.drawRect(tempRect, panelPaint);
    canvas.drawRect(exitGateX - 90f, ground - 182f, exitGateX + 104f, ground - 146f, accentPaint);
    smallTextPaint.setColor(Color.rgb(16, 24, 36));
    canvas.drawText("STAMP", exitGateX - 62f, ground - 156f, smallTextPaint);
  }

  private void drawPlayer(Canvas canvas) {
    float footY = playerY;
    float shadowW = dashTimer > 0f ? 112f : 92f;
    canvas.drawOval(playerX - shadowW / 2f, footY - 10f, playerX + shadowW / 2f, footY + 12f, shadowPaint);
    SpriteSet spriteSet = playerSprites;
    Bitmap frame = resolvePlayerFrame(spriteSet);
    if (frame != null) {
      drawBitmapFacing(canvas, frame, playerX - 62f, footY - 146f, 124f, 146f, facingRight, invulnerableTimer > 0f ? 0.72f : 1f);
    } else {
      panelPaint.setColor(invulnerableTimer > 0f ? Color.argb(180, 230, 221, 210) : Color.rgb(226, 221, 210));
      tempRect.set(playerX - 42f, footY - 126f, playerX + 42f, footY);
      canvas.drawRoundRect(tempRect, 18f, 18f, panelPaint);
    }
  }

  private Bitmap resolvePlayerFrame(SpriteSet spriteSet) {
    if (!playerOnGround && spriteSet.jump != null) {
      return spriteSet.jump;
    }
    if (attackTimer > 0f) {
      return attackStyle == 1 && spriteSet.action1 != null ? spriteSet.action1
          : spriteSet.action2 != null ? spriteSet.action2 : spriteSet.stand;
    }
    if (invulnerableTimer > 0f && spriteSet.hurt != null) {
      return spriteSet.hurt;
    }
    if (Math.abs((input.left ? -1f : 0f) + (input.right ? 1f : 0f)) > 0f) {
      return (ambientTimer % 0.34f) < 0.17f && spriteSet.walk1 != null ? spriteSet.walk1
          : spriteSet.walk2 != null ? spriteSet.walk2 : spriteSet.idle;
    }
    return spriteSet.idle != null ? spriteSet.idle : spriteSet.stand;
  }

  private void drawEnemy(Canvas canvas, Enemy enemy) {
    float footY = enemy.y;
    canvas.drawOval(enemy.x - 44f, footY - 8f, enemy.x + 44f, footY + 10f, shadowPaint);
    SpriteSet spriteSet = enemy.type == Enemy.GUNNER || enemy.type == Enemy.LIEUTENANT ? soldierSprites : zombieSprites;
    Bitmap frame = resolveEnemyFrame(spriteSet, enemy);
    if (frame != null) {
      drawBitmapFacing(canvas, frame, enemy.x - (enemy.elite ? 72f : 58f), footY - (enemy.elite ? 164f : 146f), enemy.elite ? 144f : 116f, enemy.elite ? 164f : 146f, enemy.facingRight, 1f);
    } else {
      Paint fill = enemy.elite ? elitePaint : enemyPaint;
      tempRect.set(enemy.x - 38f, footY - 118f, enemy.x + 38f, footY);
      canvas.drawRoundRect(tempRect, 18f, 18f, fill);
    }
    Paint hpPaint = enemy.elite ? elitePaint : enemyPaint;
    tempRect.set(enemy.x - 40f, footY - 134f, enemy.x + 40f, footY - 126f);
    canvas.drawRoundRect(tempRect, 4f, 4f, panelPaint);
    float ratio = enemy.hp / enemy.maxHp;
    tempRect.set(enemy.x - 40f, footY - 134f, enemy.x - 40f + 80f * ratio, footY - 126f);
    canvas.drawRoundRect(tempRect, 4f, 4f, hpPaint);
  }

  private Bitmap resolveEnemyFrame(SpriteSet spriteSet, Enemy enemy) {
    if (enemy.flashTimer > 0f && spriteSet.hurt != null) {
      return spriteSet.hurt;
    }
    if (enemy.attackCooldown > 0f && Math.abs(playerX - enemy.x) < 72f && spriteSet.action1 != null) {
      return spriteSet.action1;
    }
    if (Math.abs(enemy.vx) > 14f || Math.abs(playerX - enemy.x) > 90f) {
      return (ambientTimer % 0.34f) < 0.17f && spriteSet.walk1 != null ? spriteSet.walk1
          : spriteSet.walk2 != null ? spriteSet.walk2 : spriteSet.idle;
    }
    return spriteSet.idle != null ? spriteSet.idle : spriteSet.stand;
  }

  private void drawPickup(Canvas canvas, Pickup pickup) {
    float bob = (float) Math.sin(pickup.bob) * 6f;
    Bitmap bitmap = pickup.type == Pickup.CASH ? pickupCoin : pickup.type == Pickup.AMMO ? pickupAmmo : pickupMed;
    if (bitmap != null) {
      tempRect.set(pickup.x - 22f, pickup.y - 46f + bob, pickup.x + 22f, pickup.y - 2f + bob);
      canvas.drawBitmap(bitmap, null, tempRect, null);
    } else {
      pickupPaint.setColor(pickup.type == Pickup.CASH ? Color.rgb(168, 255, 216) : pickup.type == Pickup.AMMO ? Color.rgb(217, 165, 82) : Color.rgb(125, 220, 174));
      canvas.drawCircle(pickup.x, pickup.y - 24f + bob, 14f, pickupPaint);
    }
  }

  private void drawDistrictBoard(Canvas canvas, boolean menuVariant) {
    canvas.drawColor(Color.rgb(10, 15, 24));
    drawBackdrops(canvas, false);
    panelPaint.setColor(Color.argb(220, 20, 28, 40));
    tempRect.set(90f, 84f, getWidth() - 90f, getHeight() - 90f);
    canvas.drawRoundRect(tempRect, 22f, 22f, panelPaint);
    accentPaint.setColor(Color.rgb(217, 165, 82));
    smallTextPaint.setColor(Color.rgb(245, 241, 232));
    textPaint.setColor(Color.rgb(245, 241, 232));
    textPaint.setTextSize(44f);
    canvas.drawText(menuVariant ? "DISTRICT EVIDENCE WALL" : "OLD KING DISTRICT BOARD", 132f, 150f, textPaint);
    float startX = 176f;
    float startY = 220f;
    float gapX = (getWidth() - 352f) / 2f;
    float gapY = (getHeight() - 360f) / 2f;
    int index = 0;
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        float cx = startX + col * gapX;
        float cy = startY + row * gapY;
        District district = districts[index];
        int fillColor = district.unlocked ? Color.argb(220, 37, 46, 66) : Color.argb(180, 24, 28, 36);
        panelPaint.setColor(fillColor);
        tempRect.set(cx - 84f, cy - 42f, cx + 84f, cy + 42f);
        canvas.drawRoundRect(tempRect, 18f, 18f, panelPaint);
        if (index == selectedDistrict) {
          accentPaint.setColor(Color.rgb(217, 165, 82));
          canvas.drawRoundRect(tempRect, 18f, 18f, accentPaint);
          panelPaint.setColor(Color.argb(220, 22, 30, 42));
          tempRect.inset(6f, 6f);
          canvas.drawRoundRect(tempRect, 16f, 16f, panelPaint);
        }
        smallTextPaint.setColor(district.unlocked ? Color.rgb(245, 241, 232) : Color.rgb(128, 140, 168));
        canvas.drawText((index + 1) + ". " + district.name, cx - 68f, cy - 4f, smallTextPaint);
        canvas.drawText(district.controlPercent() + "%  " + district.clearedSectors + "/" + district.totalSectors, cx - 68f, cy + 22f, smallTextPaint);
        if (index < districts.length - 1) {
          float nextCx = col == 2 ? startX : cx + gapX;
          float nextCy = col == 2 ? cy + gapY : cy;
          accentPaint.setStrokeWidth(6f);
          canvas.drawLine(cx + 84f, cy, nextCx - 84f, nextCy, accentPaint);
        }
        index++;
      }
    }
    canvas.drawText("Campaign control " + totalControlledBlocks() + "/40  Partners " + countUnlockedPartners() + "/8", 132f, getHeight() - 136f, smallTextPaint);
    canvas.drawText("Current boss " + districts[selectedDistrict].boss, 132f, getHeight() - 102f, smallTextPaint);
  }

  private void drawSafehouse(Canvas canvas) {
    canvas.drawColor(Color.rgb(14, 20, 31));
    drawBackdrops(canvas, true);
    panelPaint.setColor(Color.argb(224, 20, 29, 42));
    tempRect.set(76f, 92f, getWidth() - 76f, getHeight() - 92f);
    canvas.drawRoundRect(tempRect, 22f, 22f, panelPaint);
    textPaint.setColor(Color.rgb(245, 241, 232));
    textPaint.setTextSize(46f);
    canvas.drawText("SAFEHOUSE LOCKER", 120f, 156f, textPaint);
    smallTextPaint.setColor(Color.rgb(221, 206, 180));
    canvas.drawText("Bench level " + benchLevel + "  Cash $" + cash + "  Revives " + reviveTokens, 120f, 198f, smallTextPaint);
    drawPortrait(canvas, femaleSprites, 178f, 278f, 112f, 132f, true);
    drawPortrait(canvas, adventurerSprites, 330f, 278f, 112f, 132f, true);
    drawPortrait(canvas, soldierSprites, 482f, 278f, 112f, 132f, true);
    drawPortrait(canvas, zombieSprites, getWidth() - 240f, 278f, 112f, 132f, false);
    panelPaint.setColor(Color.argb(210, 36, 48, 68));
    for (int i = 0; i < 4; i++) {
      float x = 150f + i * 170f;
      tempRect.set(x, getHeight() - 280f, x + 124f, getHeight() - 148f);
      canvas.drawRoundRect(tempRect, 18f, 18f, panelPaint);
    }
    smallTextPaint.setColor(Color.rgb(245, 241, 232));
    canvas.drawText("Field meds", 164f, getHeight() - 232f, smallTextPaint);
    canvas.drawText("Ammo cache", 332f, getHeight() - 232f, smallTextPaint);
    canvas.drawText("Bench unlock", 500f, getHeight() - 232f, smallTextPaint);
    canvas.drawText("Crew recruit", 668f, getHeight() - 232f, smallTextPaint);
  }

  private void drawPortrait(Canvas canvas, SpriteSet spriteSet, float x, float y, float width, float height, boolean faceRight) {
    panelPaint.setColor(Color.argb(200, 34, 44, 60));
    tempRect.set(x, y, x + width, y + height);
    canvas.drawRoundRect(tempRect, 18f, 18f, panelPaint);
    Bitmap bitmap = spriteSet != null ? (spriteSet.idle != null ? spriteSet.idle : spriteSet.stand) : null;
    if (bitmap != null) {
      drawBitmapFacing(canvas, bitmap, x + 10f, y + 10f, width - 20f, height - 16f, faceRight, 1f);
    }
  }

  private void drawBitmapFacing(Canvas canvas, Bitmap bitmap, float x, float y, float width, float height, boolean faceRight, float alpha) {
    if (bitmap == null) {
      return;
    }
    Paint drawPaint = null;
    if (alpha < 0.99f) {
      panelPaint.setAlpha(Math.round(alpha * 255f));
      drawPaint = panelPaint;
    }
    canvas.save();
    if (faceRight) {
      tempRect.set(x, y, x + width, y + height);
      canvas.drawBitmap(bitmap, null, tempRect, drawPaint);
    } else {
      canvas.translate(x + width, y);
      canvas.scale(-1f, 1f);
      tempRect.set(0f, 0f, width, height);
      canvas.drawBitmap(bitmap, null, tempRect, drawPaint);
    }
    canvas.restore();
    panelPaint.setAlpha(255);
  }

  private float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
