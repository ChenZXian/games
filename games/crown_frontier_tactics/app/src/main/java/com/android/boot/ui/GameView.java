package com.android.boot.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.GameAudio;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    public interface Callbacks {
        void onHudChanged(HudSnapshot snapshot);
        void onBattleEnded(boolean victory, BattleReport report);
    }

    public static final class HudSnapshot {
        public final String regionText;
        public final String turnText;
        public final String goldText;
        public final String incomeText;
        public final String difficultyText;
        public final String hintText;
        public final String objectiveText;
        public final String selectedName;
        public final String selectedStats;
        public final String selectedTerrain;
        public final String contextLabel;
        public final boolean contextEnabled;
        public final boolean orderEnabled;
        public final boolean recruitEnabled;

        public HudSnapshot(String regionText, String turnText, String goldText, String incomeText, String difficultyText,
                           String hintText, String objectiveText, String selectedName, String selectedStats,
                           String selectedTerrain, String contextLabel, boolean contextEnabled, boolean orderEnabled,
                           boolean recruitEnabled) {
            this.regionText = regionText;
            this.turnText = turnText;
            this.goldText = goldText;
            this.incomeText = incomeText;
            this.difficultyText = difficultyText;
            this.hintText = hintText;
            this.objectiveText = objectiveText;
            this.selectedName = selectedName;
            this.selectedStats = selectedStats;
            this.selectedTerrain = selectedTerrain;
            this.contextLabel = contextLabel;
            this.contextEnabled = contextEnabled;
            this.orderEnabled = orderEnabled;
            this.recruitEnabled = recruitEnabled;
        }
    }

    public static final class BattleReport {
        public final String summaryText;

        public BattleReport(String summaryText) {
            this.summaryText = summaryText;
        }
    }

    public static final class ChapterDefinition {
        public final int index;
        public final String title;
        public final String region;
        public final String objectiveLabel;
        public final String briefing;
        public final int width;
        public final int height;
        public final ObjectiveType objectiveType;
        public final int surviveTurns;
        public final int patternId;

        public ChapterDefinition(int index, String title, String region, String objectiveLabel, String briefing,
                                 int width, int height, ObjectiveType objectiveType, int surviveTurns, int patternId) {
            this.index = index;
            this.title = title;
            this.region = region;
            this.objectiveLabel = objectiveLabel;
            this.briefing = briefing;
            this.width = width;
            this.height = height;
            this.objectiveType = objectiveType;
            this.surviveTurns = surviveTurns;
            this.patternId = patternId;
        }
    }

    public enum UnitKind {
        MARSHAL_COMMANDER,
        MILITIA,
        SWORDSMAN,
        SPEARMAN,
        ARCHER,
        RANGER,
        KNIGHT,
        LANCER,
        HEALER,
        BATTLE_MAGE,
        SCOUT_RIDER,
        WYVERN_RIDER,
        BALLISTA_CREW,
        RAIDER_SWORDSMAN,
        PIKE_GUARD,
        CROSSBOWMAN,
        WOLF_RIDER,
        HEAVY_KNIGHT,
        WAR_PRIEST,
        ROGUE_MAGE,
        ELITE_COMMANDER,
        HEIR
    }

    public enum ObjectiveType {
        DEFEAT_COMMANDER,
        CAPTURE_CAPITAL,
        SURVIVE_TURNS,
        ESCORT_HEIR
    }

    private enum TerrainType {
        PLAINS,
        ROAD,
        FOREST,
        HILL,
        MOUNTAIN,
        RIVER,
        BRIDGE,
        MARSH,
        RUINS,
        SHRINE,
        VILLAGE,
        FORT,
        KEEP,
        CAPITAL,
        EXIT
    }

    private static final class Tile {
        TerrainType terrain = TerrainType.PLAINS;
        int owner;
        String label = "";
        int income = 0;
        boolean recruitSite;
        boolean healSite;
        boolean capturable;
        boolean exitTile;
        float pulse;
    }

    private static final class Unit {
        UnitKind kind;
        String name;
        boolean ally;
        boolean commander;
        boolean healer;
        boolean cavalry;
        boolean flying;
        boolean ranged;
        boolean siege;
        boolean heir;
        boolean canCapture;
        int x;
        int y;
        float renderX;
        float renderY;
        float fromX;
        float fromY;
        float moveTime;
        float attackTime;
        float hitTime;
        float captureTime;
        int faceX = 1;
        int faceY = 0;
        int hp;
        int maxHp;
        int attack;
        int defense;
        int move;
        int rangeMin;
        int rangeMax;
        int cost;
        int auraTurns;
        boolean moved;
        boolean acted;
        boolean defeated;
        int kills;
    }

    private static final class ActionPlan {
        int moveX;
        int moveY;
        Unit target;
        float score;
        boolean capture;
        boolean heal;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final RectF boardRect = new RectF();
    private final Path shapePath = new Path();
    private final Random random = new Random(44);
    private final ArrayList<ChapterDefinition> chapters = new ArrayList<>();
    private final ArrayList<Unit> units = new ArrayList<>();
    private final HashMap<String, Bitmap> bitmaps = new HashMap<>();
    private final HashMap<TerrainType, String> terrainAssetKeys = new HashMap<>();
    private final int[][] cardinal = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!attached) {
                return;
            }
            long now = System.nanoTime();
            float dt = (now - lastFrameNs) / 1000000000f;
            if (dt < 0f) {
                dt = 0f;
            }
            if (dt > 0.033f) {
                dt = 0.033f;
            }
            lastFrameNs = now;
            update(dt);
            invalidate();
            handler.postDelayed(this, 16L);
        }
    };

    private Callbacks callbacks;
    private GameAudio audio;
    private ChapterDefinition currentChapter;
    private Tile[][] tiles;
    private Unit selectedUnit;
    private Unit alliedCommander;
    private Unit enemyCommander;
    private Unit heirUnit;
    private boolean[][] reachable;
    private boolean[][] attackable;
    private boolean battleActive;
    private boolean paused;
    private boolean attached;
    private boolean aiTurn;
    private boolean battleEnded;
    private boolean dragging;
    private float touchDownX;
    private float touchDownY;
    private float dragAnchorCamX;
    private float dragAnchorCamY;
    private float cameraX;
    private float cameraY;
    private float cellSize = 96f;
    private float aiDelay;
    private long lastFrameNs;
    private int viewWidth;
    private int viewHeight;
    private int turnNumber = 1;
    private int gold;
    private int enemyGold;
    private int difficultyIndex = 1;
    private int doctrineEconomy;
    private int doctrineTactics;
    private int doctrineCommand;
    private int orderCharges;
    private int orderCap = 1;
    private int alliedLosses;
    private int enemyLosses;
    private int villagesCaptured;
    private int battleHighlights;
    private int touchSlop;
    private String hintText = "Open the atlas to begin the march.";
    private String objectiveText = "";

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        seedTerrainAssetKeys();
        buildChapters();
        loadSharedBitmaps();
        reachable = new boolean[1][1];
        attackable = new boolean[1][1];
        sendHud();
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
        sendHud();
    }

    public void setAudio(GameAudio audio) {
        this.audio = audio;
    }

    public List<ChapterDefinition> getChapters() {
        return Collections.unmodifiableList(chapters);
    }

    public ChapterDefinition getChapter(int index) {
        int safe = Math.max(1, Math.min(index, chapters.size()));
        return chapters.get(safe - 1);
    }

    public void startChapter(int chapterIndex, int difficultyIndex, int doctrineEconomy, int doctrineTactics, int doctrineCommand) {
        this.currentChapter = getChapter(chapterIndex);
        this.difficultyIndex = difficultyIndex;
        this.doctrineEconomy = doctrineEconomy;
        this.doctrineTactics = doctrineTactics;
        this.doctrineCommand = doctrineCommand;
        this.orderCap = Math.min(3, 1 + doctrineCommand / 2);
        this.orderCharges = orderCap;
        this.turnNumber = 1;
        this.gold = 220 + currentChapter.index * 14 + doctrineEconomy * 18;
        this.enemyGold = 190 + currentChapter.index * 12 + difficultyIndex * 28;
        this.alliedLosses = 0;
        this.enemyLosses = 0;
        this.villagesCaptured = 0;
        this.battleHighlights = 0;
        this.aiTurn = false;
        this.paused = false;
        this.battleEnded = false;
        this.battleActive = true;
        this.hintText = "Secure income, then break the enemy line.";
        buildBattlefield(currentChapter);
        centerCameraOn(alliedCommander);
        sendHud();
    }

    public void endTurn() {
        if (!battleActive || aiTurn || paused || battleEnded) {
            return;
        }
        clearSelection();
        beginEnemyTurn();
    }

    public void performContextAction() {
        if (!battleActive || aiTurn || paused || battleEnded || selectedUnit == null || !selectedUnit.ally || selectedUnit.acted) {
            return;
        }
        Tile tile = getTile(selectedUnit.x, selectedUnit.y);
        Unit healTarget = findHealTarget(selectedUnit);
        if (tile != null && tile.capturable && tile.owner != 1 && selectedUnit.canCapture) {
            captureTile(selectedUnit, tile);
            selectedUnit.acted = true;
            selectedUnit.moved = true;
            clearReachable();
            sendHud();
            checkVictoryState();
            return;
        }
        if (healTarget != null && selectedUnit.healer) {
            performHeal(selectedUnit, healTarget);
            selectedUnit.acted = true;
            selectedUnit.moved = true;
            clearReachable();
            sendHud();
            return;
        }
        selectedUnit.acted = true;
        selectedUnit.moved = true;
        clearReachable();
        hintText = selectedUnit.name + " holds position.";
        sendHud();
    }

    public void recruit(UnitKind kind) {
        if (!battleActive || aiTurn || paused || battleEnded) {
            return;
        }
        Unit recruiter = selectedUnit;
        if (recruiter == null || !recruiter.ally || recruiter.acted) {
            hintText = "Select a keep, fort, or capital first.";
            sendHud();
            return;
        }
        Tile tile = getTile(recruiter.x, recruiter.y);
        if (tile == null || !tile.recruitSite || tile.owner != 1) {
            hintText = "Recruitment requires a captured keep, fort, or capital.";
            sendHud();
            return;
        }
        int cost = createUnit(kind, true, 0, 0).cost;
        if (gold < cost) {
            hintText = "Not enough gold for " + unitLabel(kind) + ".";
            sendHud();
            return;
        }
        int[] spawn = findRecruitSpawn(recruiter.x, recruiter.y);
        if (spawn == null) {
            hintText = "No adjacent deployment tile is free.";
            sendHud();
            return;
        }
        Unit unit = createUnit(kind, true, spawn[0], spawn[1]);
        unit.fromX = recruiter.x;
        unit.fromY = recruiter.y;
        unit.renderX = recruiter.x;
        unit.renderY = recruiter.y;
        unit.moveTime = 0.18f;
        unit.moved = true;
        unit.acted = true;
        units.add(unit);
        gold -= cost;
        recruiter.acted = true;
        recruiter.moved = true;
        selectedUnit = unit;
        hintText = unit.name + " marches from the reserve.";
        if (audio != null) {
            audio.playMove();
        }
        sendHud();
    }

    public void useCommanderOrder() {
        if (!battleActive || aiTurn || paused || battleEnded || orderCharges <= 0 || alliedCommander == null || alliedCommander.defeated) {
            return;
        }
        orderCharges--;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (!unit.ally || unit.defeated) {
                continue;
            }
            int dist = Math.abs(unit.x - alliedCommander.x) + Math.abs(unit.y - alliedCommander.y);
            if (dist <= 2) {
                unit.auraTurns = Math.max(unit.auraTurns, 2 + doctrineCommand / 2);
                unit.hp = Math.min(unit.maxHp, unit.hp + 2 + doctrineCommand);
                unit.captureTime = 0.35f;
            }
        }
        hintText = "Marshal order surges through the line.";
        if (audio != null) {
            audio.playHeal();
        }
        sendHud();
    }

    public void pauseGame() {
        paused = true;
    }

    public void resumeGame() {
        if (battleActive && !battleEnded) {
            paused = false;
        }
    }

    public void onPauseView() {
        paused = true;
    }

    public void onResumeView() {
        if (battleActive && !battleEnded && !aiTurn) {
            paused = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        lastFrameNs = System.nanoTime();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        recomputeBoardBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(color(R.color.cst_bg_main));
        drawBackdrop(canvas);
        if (!battleActive || tiles == null) {
            drawMenuBackdrop(canvas);
            return;
        }
        drawTiles(canvas);
        drawStructures(canvas);
        drawHighlights(canvas);
        drawUnits(canvas);
        drawBoardFrame(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!battleActive || paused || battleEnded) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownX = x;
            touchDownY = y;
            dragAnchorCamX = cameraX;
            dragAnchorCamY = cameraY;
            dragging = false;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = x - touchDownX;
            float dy = y - touchDownY;
            if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                dragging = true;
            }
            if (dragging) {
                cameraX = dragAnchorCamX - dx / cellSize;
                cameraY = dragAnchorCamY - dy / cellSize;
                clampCamera();
                invalidate();
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!dragging && !aiTurn) {
                handleTap(x, y);
            }
            dragging = false;
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void update(float dt) {
        if (tiles == null) {
            return;
        }
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                if (tiles[x][y].pulse > 0f) {
                    tiles[x][y].pulse = Math.max(0f, tiles[x][y].pulse - dt);
                }
            }
        }
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.moveTime > 0f) {
                unit.moveTime = Math.max(0f, unit.moveTime - dt);
                float t = 1f - (unit.moveTime / 0.18f);
                if (t > 1f) {
                    t = 1f;
                }
                unit.renderX = lerp(unit.fromX, unit.x, t);
                unit.renderY = lerp(unit.fromY, unit.y, t);
            } else {
                unit.renderX = unit.x;
                unit.renderY = unit.y;
            }
            if (unit.attackTime > 0f) {
                unit.attackTime = Math.max(0f, unit.attackTime - dt);
            }
            if (unit.hitTime > 0f) {
                unit.hitTime = Math.max(0f, unit.hitTime - dt);
            }
            if (unit.captureTime > 0f) {
                unit.captureTime = Math.max(0f, unit.captureTime - dt);
            }
        }
        if (battleActive && aiTurn && !paused && !battleEnded) {
            aiDelay -= dt;
            if (aiDelay <= 0f) {
                aiDelay = 0.28f;
                executeEnemyStep();
            }
        }
    }

    private void handleTap(float sx, float sy) {
        int tx = screenToTileX(sx);
        int ty = screenToTileY(sy);
        if (!isInsideBoard(tx, ty)) {
            return;
        }
        Unit tappedUnit = getUnitAt(tx, ty);
        if (tappedUnit != null && tappedUnit.ally && !tappedUnit.defeated && !tappedUnit.acted) {
            selectUnit(tappedUnit);
            return;
        }
        if (selectedUnit == null || selectedUnit.defeated || !selectedUnit.ally || selectedUnit.acted) {
            updateSelectionInfo(tx, ty);
            return;
        }
        if (tappedUnit != null && !tappedUnit.ally && canAttack(selectedUnit, tappedUnit, selectedUnit.x, selectedUnit.y)) {
            performAttack(selectedUnit, tappedUnit);
            clearReachable();
            sendHud();
            checkVictoryState();
            return;
        }
        if (!selectedUnit.moved && isReachable(tx, ty) && getUnitAt(tx, ty) == null) {
            moveUnit(selectedUnit, tx, ty);
            clearReachable();
            computeAttackTargets(selectedUnit);
            updateSelectionInfo(tx, ty);
            sendHud();
            return;
        }
        updateSelectionInfo(tx, ty);
    }

    private void buildChapters() {
        chapters.clear();
        String[] regions = new String[]{
                "Lowland March",
                "Red Ford",
                "Pinewatch",
                "Iron Pass",
                "Ashen Border",
                "Crown Approach"
        };
        String[][] titles = new String[][]{
                {"March At Dawn", "Broken Toll", "Field Of Standards", "Herald's Crossing", "The First Pennant"},
                {"Red Water Rise", "Bridge Oath", "Watchfire Bend", "Ferry Under Siege", "Ford Of Kings"},
                {"Pinewatch Muster", "Timberline Raid", "Abbey Smoke", "Ranger's Hollow", "Thorn Banner"},
                {"Iron Gate Alarm", "Viaduct Duel", "Stone Stair Break", "Bastion Of Nails", "Pass Marshal"},
                {"Ash Ridge Sweep", "Shrine Embers", "Marsh Ashes", "Cinder Convoy", "Border Witch"},
                {"Royal Route", "Gate Of Seals", "Twin Standards", "Pretender's Road", "Crown Frontier"}
        };
        for (int regionIndex = 0; regionIndex < regions.length; regionIndex++) {
            for (int local = 0; local < 5; local++) {
                int index = regionIndex * 5 + local + 1;
                ObjectiveType objectiveType;
                int surviveTurns = 0;
                if (local == 1) {
                    objectiveType = ObjectiveType.CAPTURE_CAPITAL;
                } else if (local == 3) {
                    objectiveType = ObjectiveType.ESCORT_HEIR;
                } else if (local == 2) {
                    objectiveType = ObjectiveType.SURVIVE_TURNS;
                    surviveTurns = 7 + regionIndex;
                } else {
                    objectiveType = ObjectiveType.DEFEAT_COMMANDER;
                }
                int width = 15 + regionIndex * 2 + (local % 2 == 0 ? 1 : 3);
                int height = 11 + regionIndex + (local == 4 ? 2 : 0);
                String objectiveLabel = buildObjectiveLabel(objectiveType, surviveTurns);
                String briefing = buildBriefing(regions[regionIndex], titles[regionIndex][local], objectiveType, surviveTurns, regionIndex, local);
                chapters.add(new ChapterDefinition(index, titles[regionIndex][local], regions[regionIndex], objectiveLabel, briefing, width, height, objectiveType, surviveTurns, local + regionIndex * 2));
            }
        }
    }

    private String buildObjectiveLabel(ObjectiveType objectiveType, int surviveTurns) {
        if (objectiveType == ObjectiveType.CAPTURE_CAPITAL) {
            return "Capture the enemy capital.";
        }
        if (objectiveType == ObjectiveType.SURVIVE_TURNS) {
            return "Hold the line for " + surviveTurns + " turns.";
        }
        if (objectiveType == ObjectiveType.ESCORT_HEIR) {
            return "Escort the heir to the frontier road.";
        }
        return "Defeat the enemy commander.";
    }

    private String buildBriefing(String region, String title, ObjectiveType objectiveType, int surviveTurns, int regionIndex, int local) {
        String enemyFocus;
        if (regionIndex < 2) {
            enemyFocus = "raiders and fast riders";
        } else if (regionIndex < 4) {
            enemyFocus = "armored lancers and crossbow lines";
        } else {
            enemyFocus = "elite commanders and battle mages";
        }
        String mission;
        if (objectiveType == ObjectiveType.CAPTURE_CAPITAL) {
            mission = "Break through the outer ring and seize the enemy capital before their treasury snowballs.";
        } else if (objectiveType == ObjectiveType.SURVIVE_TURNS) {
            mission = "Anchor the villages, brace the choke points, and survive " + surviveTurns + " turns of pressure.";
        } else if (objectiveType == ObjectiveType.ESCORT_HEIR) {
            mission = "Protect the heir and clear a road to the marked escape tiles on the far edge.";
        } else {
            mission = "Push across the region, secure income, and cut down the rival field commander.";
        }
        return title + " opens the " + region + " arc. Expect " + enemyFocus + ", split approach lanes, and capturable structures that decide the tempo. " + mission;
    }

    private void buildBattlefield(ChapterDefinition chapter) {
        tiles = new Tile[chapter.width][chapter.height];
        reachable = new boolean[chapter.width][chapter.height];
        attackable = new boolean[chapter.width][chapter.height];
        units.clear();
        alliedCommander = null;
        enemyCommander = null;
        heirUnit = null;
        selectedUnit = null;
        for (int x = 0; x < chapter.width; x++) {
            for (int y = 0; y < chapter.height; y++) {
                tiles[x][y] = new Tile();
            }
        }
        paintBaseTerrain(chapter);
        carveRoadNet(chapter);
        placeStructures(chapter);
        populateUnits(chapter);
        objectiveText = chapter.objectiveLabel;
        recomputeBoardBounds();
        sendHud();
    }

    private void paintBaseTerrain(ChapterDefinition chapter) {
        for (int x = 0; x < chapter.width; x++) {
            for (int y = 0; y < chapter.height; y++) {
                Tile tile = tiles[x][y];
                if (chapter.region.equals("Red Ford") && Math.abs(y - chapter.height / 2) <= 1 && x > 2 && x < chapter.width - 3) {
                    tile.terrain = TerrainType.RIVER;
                } else if (chapter.region.equals("Pinewatch") && random.nextFloat() < 0.28f) {
                    tile.terrain = TerrainType.FOREST;
                } else if (chapter.region.equals("Iron Pass") && (x < 2 || x > chapter.width - 3 || random.nextFloat() < 0.18f)) {
                    tile.terrain = random.nextFloat() < 0.55f ? TerrainType.HILL : TerrainType.MOUNTAIN;
                } else if (chapter.region.equals("Ashen Border") && random.nextFloat() < 0.2f) {
                    tile.terrain = random.nextFloat() < 0.6f ? TerrainType.MARSH : TerrainType.RUINS;
                } else if (chapter.region.equals("Crown Approach") && random.nextFloat() < 0.12f) {
                    tile.terrain = TerrainType.HILL;
                } else {
                    tile.terrain = TerrainType.PLAINS;
                }
            }
        }
        if (chapter.objectiveType == ObjectiveType.ESCORT_HEIR) {
            for (int y = 1; y < chapter.height - 1; y++) {
                tiles[chapter.width - 1][y].terrain = TerrainType.EXIT;
                tiles[chapter.width - 1][y].exitTile = true;
                tiles[chapter.width - 1][y].label = "Frontier Road";
            }
        }
    }

    private void carveRoadNet(ChapterDefinition chapter) {
        int midY = chapter.height / 2;
        for (int x = 0; x < chapter.width; x++) {
            setTerrain(x, midY, TerrainType.ROAD);
            if (chapter.height > 12 && x % 4 == 0 && midY - 2 >= 0) {
                setTerrain(x, midY - 1, TerrainType.ROAD);
            }
        }
        if (chapter.region.equals("Red Ford")) {
            int bridgeX = chapter.width / 2;
            setTerrain(bridgeX, midY, TerrainType.BRIDGE);
            if (midY - 1 >= 0) {
                setTerrain(bridgeX, midY - 1, TerrainType.BRIDGE);
            }
        }
        if (chapter.region.equals("Iron Pass")) {
            for (int y = 1; y < chapter.height - 1; y++) {
                setTerrain(chapter.width / 2, y, TerrainType.ROAD);
            }
        }
        if (chapter.patternId % 3 == 0) {
            for (int y = 1; y < chapter.height - 1; y++) {
                setTerrain(chapter.width / 3, y, TerrainType.ROAD);
            }
        }
    }

    private void placeStructures(ChapterDefinition chapter) {
        int allyKeepY = chapter.height / 2;
        int enemyKeepY = chapter.height / 2;
        setStructure(1, allyKeepY, TerrainType.KEEP, 1, "Marshal Keep", 4, true, true);
        setStructure(chapter.width - 2, enemyKeepY, chapter.objectiveType == ObjectiveType.CAPTURE_CAPITAL ? TerrainType.CAPITAL : TerrainType.KEEP, -1,
                chapter.objectiveType == ObjectiveType.CAPTURE_CAPITAL ? "Pretender Capital" : "Enemy Keep", 4, true, true);
        setStructure(chapter.width / 2, Math.max(1, allyKeepY - 2), TerrainType.VILLAGE, 0, "Border Village", 2, false, true);
        setStructure(chapter.width / 2 + 2, Math.min(chapter.height - 2, allyKeepY + 2), TerrainType.VILLAGE, 0, "Trade Hamlet", 2, false, true);
        setStructure(Math.max(2, chapter.width / 2 - 3), Math.max(1, allyKeepY + 3), TerrainType.FORT, 0, "Watch Fort", 3, true, true);
        setStructure(Math.min(chapter.width - 3, chapter.width / 2 + 4), Math.max(1, allyKeepY - 3), TerrainType.FORT, -1, "Enemy Fort", 3, true, true);
        if (chapter.region.equals("Pinewatch")) {
            setStructure(chapter.width / 2, 1, TerrainType.SHRINE, 0, "Abbey Shrine", 1, false, true);
        }
        if (chapter.region.equals("Iron Pass")) {
            setStructure(chapter.width / 2, chapter.height - 2, TerrainType.FORT, -1, "Stone Gate", 2, true, false);
        }
        if (chapter.objectiveType == ObjectiveType.SURVIVE_TURNS) {
            setStructure(2, Math.max(1, allyKeepY - 3), TerrainType.CAPITAL, 1, "Frontier Hold", 5, true, true);
        }
        if (chapter.objectiveType == ObjectiveType.ESCORT_HEIR) {
            setStructure(chapter.width - 3, 1, TerrainType.VILLAGE, -1, "Roadblock Village", 2, false, true);
        }
    }

    private void populateUnits(ChapterDefinition chapter) {
        int midY = chapter.height / 2;
        alliedCommander = createUnit(UnitKind.MARSHAL_COMMANDER, true, 1, midY);
        alliedCommander.commander = true;
        alliedCommander.name = "Marshal Rowan";
        units.add(alliedCommander);
        units.add(createUnit(UnitKind.MILITIA, true, 2, Math.max(1, midY - 1)));
        units.add(createUnit(UnitKind.SPEARMAN, true, 2, Math.min(chapter.height - 2, midY + 1)));
        units.add(createUnit(UnitKind.ARCHER, true, 1, Math.max(1, midY - 3)));
        if (chapter.index >= 4) {
            units.add(createUnit(UnitKind.KNIGHT, true, 3, midY));
        }
        if (chapter.index >= 7) {
            units.add(createUnit(UnitKind.HEALER, true, 2, Math.min(chapter.height - 2, midY + 3)));
        }
        if (chapter.index >= 11) {
            units.add(createUnit(UnitKind.BATTLE_MAGE, true, 3, Math.max(1, midY - 3)));
        }
        if (chapter.index >= 15) {
            units.add(createUnit(UnitKind.SCOUT_RIDER, true, 4, Math.max(1, midY - 2)));
        }
        if (chapter.index >= 20) {
            units.add(createUnit(UnitKind.LANCER, true, 4, Math.min(chapter.height - 2, midY + 2)));
        }
        if (chapter.index >= 24) {
            units.add(createUnit(UnitKind.BALLISTA_CREW, true, 1, Math.min(chapter.height - 2, midY + 4)));
        }
        if (chapter.index >= 27) {
            units.add(createUnit(UnitKind.WYVERN_RIDER, true, 4, Math.max(1, midY - 4)));
        }

        enemyCommander = createUnit(chapter.index >= 23 ? UnitKind.ELITE_COMMANDER : UnitKind.RAIDER_SWORDSMAN, false, chapter.width - 2, midY);
        enemyCommander.commander = true;
        enemyCommander.name = chapter.index >= 25 ? "Pretender Lord" : "Rival Captain";
        enemyCommander.kind = chapter.index >= 23 ? UnitKind.ELITE_COMMANDER : enemyCommander.kind;
        units.add(enemyCommander);
        units.add(createUnit(UnitKind.RAIDER_SWORDSMAN, false, chapter.width - 3, Math.max(1, midY - 1)));
        units.add(createUnit(UnitKind.PIKE_GUARD, false, chapter.width - 3, Math.min(chapter.height - 2, midY + 1)));
        units.add(createUnit(UnitKind.CROSSBOWMAN, false, chapter.width - 4, Math.max(1, midY - 3)));
        if (chapter.index >= 5) {
            units.add(createUnit(UnitKind.WOLF_RIDER, false, chapter.width - 4, Math.min(chapter.height - 2, midY + 3)));
        }
        if (chapter.index >= 9) {
            units.add(createUnit(UnitKind.HEAVY_KNIGHT, false, chapter.width - 5, midY));
        }
        if (chapter.index >= 13) {
            units.add(createUnit(UnitKind.WAR_PRIEST, false, chapter.width - 4, Math.max(1, midY - 4)));
        }
        if (chapter.index >= 18) {
            units.add(createUnit(UnitKind.ROGUE_MAGE, false, chapter.width - 5, Math.min(chapter.height - 2, midY + 4)));
        }
        if (chapter.index >= 26) {
            units.add(createUnit(UnitKind.ELITE_COMMANDER, false, chapter.width - 6, Math.max(1, midY - 2)));
        }
        if (chapter.objectiveType == ObjectiveType.ESCORT_HEIR) {
            heirUnit = createUnit(UnitKind.HEIR, true, 1, Math.max(1, midY + 2));
            heirUnit.name = "Prince Alden";
            units.add(heirUnit);
        }
    }

    private Unit createUnit(UnitKind kind, boolean ally, int x, int y) {
        Unit unit = new Unit();
        unit.kind = kind;
        unit.ally = ally;
        unit.x = x;
        unit.y = y;
        unit.renderX = x;
        unit.renderY = y;
        unit.fromX = x;
        unit.fromY = y;
        unit.canCapture = true;
        unit.name = unitLabel(kind);
        applyStats(unit);
        return unit;
    }

    private void applyStats(Unit unit) {
        unit.rangeMin = 1;
        unit.rangeMax = 1;
        unit.move = 4;
        unit.maxHp = 12;
        unit.attack = 5;
        unit.defense = 2;
        unit.cost = 80;
        if (unit.kind == UnitKind.MARSHAL_COMMANDER) {
            unit.maxHp = 18;
            unit.attack = 7;
            unit.defense = 4;
            unit.move = 5;
            unit.commander = true;
            unit.cost = 0;
        } else if (unit.kind == UnitKind.MILITIA) {
            unit.maxHp = 11;
            unit.attack = 4;
            unit.defense = 2;
            unit.move = 4;
            unit.cost = 60;
        } else if (unit.kind == UnitKind.SWORDSMAN || unit.kind == UnitKind.RAIDER_SWORDSMAN) {
            unit.maxHp = 12;
            unit.attack = 5;
            unit.defense = 3;
            unit.move = 4;
            unit.cost = 80;
        } else if (unit.kind == UnitKind.SPEARMAN || unit.kind == UnitKind.PIKE_GUARD) {
            unit.maxHp = 12;
            unit.attack = 5;
            unit.defense = 4;
            unit.move = 4;
            unit.cost = 90;
        } else if (unit.kind == UnitKind.ARCHER || unit.kind == UnitKind.CROSSBOWMAN) {
            unit.maxHp = 10;
            unit.attack = 5;
            unit.defense = 1;
            unit.move = 4;
            unit.rangeMin = 2;
            unit.rangeMax = 2;
            unit.ranged = true;
            unit.cost = 90;
        } else if (unit.kind == UnitKind.RANGER) {
            unit.maxHp = 11;
            unit.attack = 6;
            unit.defense = 2;
            unit.move = 5;
            unit.rangeMin = 2;
            unit.rangeMax = 2;
            unit.ranged = true;
            unit.cost = 100;
        } else if (unit.kind == UnitKind.KNIGHT || unit.kind == UnitKind.HEAVY_KNIGHT) {
            unit.maxHp = 15;
            unit.attack = 7;
            unit.defense = 5;
            unit.move = 6;
            unit.cavalry = true;
            unit.cost = 130;
        } else if (unit.kind == UnitKind.LANCER || unit.kind == UnitKind.WOLF_RIDER) {
            unit.maxHp = 13;
            unit.attack = 7;
            unit.defense = 3;
            unit.move = 7;
            unit.cavalry = true;
            unit.cost = 120;
        } else if (unit.kind == UnitKind.HEALER || unit.kind == UnitKind.WAR_PRIEST) {
            unit.maxHp = 10;
            unit.attack = 3;
            unit.defense = 2;
            unit.move = 4;
            unit.healer = true;
            unit.cost = 110;
        } else if (unit.kind == UnitKind.BATTLE_MAGE || unit.kind == UnitKind.ROGUE_MAGE) {
            unit.maxHp = 10;
            unit.attack = 7;
            unit.defense = 1;
            unit.move = 4;
            unit.rangeMin = 2;
            unit.rangeMax = 2;
            unit.ranged = true;
            unit.cost = 125;
        } else if (unit.kind == UnitKind.SCOUT_RIDER) {
            unit.maxHp = 11;
            unit.attack = 5;
            unit.defense = 2;
            unit.move = 8;
            unit.cavalry = true;
            unit.cost = 105;
        } else if (unit.kind == UnitKind.WYVERN_RIDER) {
            unit.maxHp = 12;
            unit.attack = 6;
            unit.defense = 3;
            unit.move = 7;
            unit.flying = true;
            unit.cost = 150;
        } else if (unit.kind == UnitKind.BALLISTA_CREW) {
            unit.maxHp = 12;
            unit.attack = 8;
            unit.defense = 2;
            unit.move = 3;
            unit.rangeMin = 2;
            unit.rangeMax = 3;
            unit.ranged = true;
            unit.siege = true;
            unit.canCapture = false;
            unit.cost = 150;
        } else if (unit.kind == UnitKind.ELITE_COMMANDER) {
            unit.maxHp = 20;
            unit.attack = 8;
            unit.defense = 5;
            unit.move = 5;
            unit.commander = true;
            unit.cost = 0;
        } else if (unit.kind == UnitKind.HEIR) {
            unit.maxHp = 9;
            unit.attack = 2;
            unit.defense = 1;
            unit.move = 5;
            unit.heir = true;
            unit.canCapture = false;
            unit.cost = 0;
        }
        unit.hp = unit.maxHp;
        if (!unit.ally && difficultyIndex == 0) {
            unit.hp = Math.max(8, unit.maxHp - 1);
        } else if (!unit.ally && difficultyIndex == 2) {
            unit.hp = unit.maxHp + (unit.commander ? 2 : 1);
            unit.maxHp = unit.hp;
            unit.attack += 1;
        }
        if (unit.ally && doctrineTactics > 0) {
            unit.attack += doctrineTactics / 2;
        }
    }

    private void beginEnemyTurn() {
        aiTurn = true;
        paused = false;
        selectedUnit = null;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (!unit.ally && !unit.defeated) {
                unit.moved = false;
                unit.acted = false;
            }
            if (unit.ally && !unit.defeated) {
                if (unit.auraTurns > 0) {
                    unit.auraTurns--;
                }
            }
        }
        enemyGold += incomeFor(false) + difficultyIndex * 8;
        maybeEnemyReinforce();
        hintText = "Enemy turn in motion.";
        aiDelay = 0.15f;
        sendHud();
    }

    private void finishEnemyTurn() {
        aiTurn = false;
        turnNumber++;
        orderCharges = Math.min(orderCap, orderCharges + 1);
        gold += incomeFor(true) + doctrineEconomy * 5;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.ally && !unit.defeated) {
                unit.moved = false;
                unit.acted = false;
                Tile tile = getTile(unit.x, unit.y);
                if (tile != null && tile.owner == 1 && tile.healSite) {
                    unit.hp = Math.min(unit.maxHp, unit.hp + 2);
                }
                if (unit.auraTurns > 0) {
                    unit.auraTurns--;
                }
            }
        }
        hintText = "Your command returns to the field.";
        if (currentChapter != null && currentChapter.objectiveType == ObjectiveType.SURVIVE_TURNS && turnNumber > currentChapter.surviveTurns) {
            finishBattle(true, "The frontier line held long enough for relief to arrive.");
            return;
        }
        sendHud();
    }

    private void executeEnemyStep() {
        Unit unit = nextEnemyUnit();
        if (unit == null) {
            finishEnemyTurn();
            return;
        }
        ActionPlan plan = chooseEnemyPlan(unit);
        if (plan == null) {
            unit.acted = true;
            unit.moved = true;
            return;
        }
        if (plan.moveX != unit.x || plan.moveY != unit.y) {
            moveUnit(unit, plan.moveX, plan.moveY);
        }
        if (plan.target != null) {
            if (plan.heal) {
                performHeal(unit, plan.target);
            } else {
                performAttack(unit, plan.target);
                checkVictoryState();
            }
        } else if (plan.capture) {
            Tile tile = getTile(unit.x, unit.y);
            if (tile != null) {
                captureTile(unit, tile);
                checkVictoryState();
            }
        } else {
            unit.acted = true;
            unit.moved = true;
        }
        sendHud();
    }

    private ActionPlan chooseEnemyPlan(Unit unit) {
        ActionPlan best = null;
        ArrayList<int[]> reachableCells = computeReachableCells(unit);
        for (int i = 0; i < reachableCells.size(); i++) {
            int[] cell = reachableCells.get(i);
            for (int j = 0; j < units.size(); j++) {
                Unit target = units.get(j);
                if (target.ally == unit.ally || target.defeated) {
                    continue;
                }
                if (canAttack(unit, target, cell[0], cell[1])) {
                    float score = attackScore(unit, target, cell[0], cell[1]);
                    if (best == null || score > best.score) {
                        best = new ActionPlan();
                        best.moveX = cell[0];
                        best.moveY = cell[1];
                        best.target = target;
                        best.score = score;
                    }
                }
            }
        }
        if (unit.healer) {
            for (int i = 0; i < reachableCells.size(); i++) {
                int[] cell = reachableCells.get(i);
                Unit target = bestHealFromCell(unit, cell[0], cell[1]);
                if (target != null) {
                    float score = 30f + (target.maxHp - target.hp) * 3f;
                    if (best == null || score > best.score) {
                        best = new ActionPlan();
                        best.moveX = cell[0];
                        best.moveY = cell[1];
                        best.target = target;
                        best.heal = true;
                        best.score = score;
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }
        for (int i = 0; i < reachableCells.size(); i++) {
            int[] cell = reachableCells.get(i);
            Tile tile = getTile(cell[0], cell[1]);
            if (tile != null && tile.capturable && tile.owner != -1 && unit.canCapture) {
                float score = captureScore(tile);
                if (difficultyIndex == 2) {
                    score += 10f;
                }
                if (best == null || score > best.score) {
                    best = new ActionPlan();
                    best.moveX = cell[0];
                    best.moveY = cell[1];
                    best.capture = true;
                    best.score = score;
                }
            }
        }
        if (best != null) {
            return best;
        }
        int[] targetTile = chooseAdvanceTarget(unit);
        if (targetTile == null) {
            return null;
        }
        int[] move = chooseAdvanceCell(unit, reachableCells, targetTile[0], targetTile[1]);
        if (move == null) {
            return null;
        }
        best = new ActionPlan();
        best.moveX = move[0];
        best.moveY = move[1];
        best.score = 1f;
        return best;
    }

    private Unit nextEnemyUnit() {
        Unit best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.ally || unit.defeated || unit.acted) {
                continue;
            }
            float score = unit.commander ? 40f : 0f;
            score += unit.move;
            score += random.nextFloat();
            if (score > bestScore) {
                bestScore = score;
                best = unit;
            }
        }
        return best;
    }

    private void selectUnit(Unit unit) {
        selectedUnit = unit;
        computeReachable(unit);
        computeAttackTargets(unit);
        updateSelectionInfo(unit.x, unit.y);
        centerCameraOn(unit);
        sendHud();
    }

    private void clearSelection() {
        selectedUnit = null;
        clearReachable();
        sendHud();
    }

    private void clearReachable() {
        if (reachable == null || attackable == null) {
            return;
        }
        for (int x = 0; x < reachable.length; x++) {
            for (int y = 0; y < reachable[x].length; y++) {
                reachable[x][y] = false;
                attackable[x][y] = false;
            }
        }
    }

    private void computeReachable(Unit unit) {
        clearReachable();
        if (tiles == null || unit == null) {
            return;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int[][] bestLeft = new int[tiles.length][tiles[0].length];
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                bestLeft[x][y] = -999;
            }
        }
        queue.add(new int[]{unit.x, unit.y, unit.move});
        bestLeft[unit.x][unit.y] = unit.move;
        reachable[unit.x][unit.y] = true;
        while (!queue.isEmpty()) {
            int[] entry = queue.removeFirst();
            int cx = entry[0];
            int cy = entry[1];
            int left = entry[2];
            for (int i = 0; i < cardinal.length; i++) {
                int nx = cx + cardinal[i][0];
                int ny = cy + cardinal[i][1];
                if (!isInsideBoard(nx, ny)) {
                    continue;
                }
                if (getUnitAt(nx, ny) != null && !(nx == unit.x && ny == unit.y)) {
                    continue;
                }
                int cost = movementCost(unit, tiles[nx][ny].terrain);
                int nextLeft = left - cost;
                if (cost >= 99 || nextLeft < 0) {
                    continue;
                }
                if (nextLeft > bestLeft[nx][ny]) {
                    bestLeft[nx][ny] = nextLeft;
                    reachable[nx][ny] = true;
                    queue.addLast(new int[]{nx, ny, nextLeft});
                }
            }
        }
    }

    private ArrayList<int[]> computeReachableCells(Unit unit) {
        computeReachable(unit);
        ArrayList<int[]> cells = new ArrayList<>();
        for (int x = 0; x < reachable.length; x++) {
            for (int y = 0; y < reachable[x].length; y++) {
                if (reachable[x][y]) {
                    if ((x == unit.x && y == unit.y) || getUnitAt(x, y) == null) {
                        cells.add(new int[]{x, y});
                    }
                }
            }
        }
        return cells;
    }

    private void computeAttackTargets(Unit unit) {
        if (attackable == null || unit == null) {
            return;
        }
        for (int x = 0; x < attackable.length; x++) {
            for (int y = 0; y < attackable[x].length; y++) {
                attackable[x][y] = false;
            }
        }
        for (int i = 0; i < units.size(); i++) {
            Unit target = units.get(i);
            if (target.ally == unit.ally || target.defeated) {
                continue;
            }
            if (canAttack(unit, target, unit.x, unit.y)) {
                attackable[target.x][target.y] = true;
            }
        }
    }

    private void performAttack(Unit attacker, Unit defender) {
        attacker.acted = true;
        attacker.moved = true;
        attacker.attackTime = 0.22f;
        attacker.faceX = clamp(defender.x - attacker.x);
        attacker.faceY = clamp(defender.y - attacker.y);
        int damage = forecastDamage(attacker, defender, attacker.x, attacker.y);
        applyDamage(defender, damage);
        if (audio != null) {
            audio.playAttack();
        }
        hintText = attacker.name + " strikes " + defender.name + " for " + damage + ".";
        if (!defender.defeated && canCounter(defender, attacker)) {
            int counter = Math.max(1, forecastDamage(defender, attacker, defender.x, defender.y) - 1);
            applyDamage(attacker, counter);
            hintText = hintText + " Counter " + counter + ".";
        }
    }

    private void performHeal(Unit healer, Unit target) {
        healer.acted = true;
        healer.moved = true;
        healer.attackTime = 0.18f;
        target.captureTime = 0.3f;
        int amount = 4 + doctrineCommand;
        target.hp = Math.min(target.maxHp, target.hp + amount);
        hintText = healer.name + " restores " + amount + " to " + target.name + ".";
        if (audio != null) {
            audio.playHeal();
        }
    }

    private void applyDamage(Unit unit, int damage) {
        unit.hp -= damage;
        unit.hitTime = 0.22f;
        if (unit.hp <= 0) {
            unit.hp = 0;
            unit.defeated = true;
            if (unit.ally) {
                alliedLosses++;
            } else {
                enemyLosses++;
            }
            if (selectedUnit == unit) {
                selectedUnit = null;
            }
        }
    }

    private void captureTile(Unit unit, Tile tile) {
        tile.owner = unit.ally ? 1 : -1;
        tile.pulse = 0.55f;
        unit.captureTime = 0.45f;
        if (tile.income > 0 && unit.ally) {
            villagesCaptured++;
        }
        hintText = unit.name + " claims " + tile.label + ".";
        if (audio != null) {
            audio.playCapture();
        }
    }

    private void moveUnit(Unit unit, int tx, int ty) {
        unit.fromX = unit.x;
        unit.fromY = unit.y;
        unit.x = tx;
        unit.y = ty;
        unit.renderX = unit.fromX;
        unit.renderY = unit.fromY;
        unit.moveTime = 0.18f;
        unit.faceX = clamp(Math.round(tx - unit.fromX));
        unit.faceY = clamp(Math.round(ty - unit.fromY));
        unit.moved = true;
        if (audio != null) {
            audio.playMove();
        }
    }

    private void maybeEnemyReinforce() {
        if (currentChapter == null || difficultyIndex == 0) {
            return;
        }
        if (turnNumber % 4 != 0) {
            return;
        }
        int spawnX = Math.max(1, currentChapter.width - 2);
        int spawnY = Math.max(1, currentChapter.height / 2 + (turnNumber % 3) - 1);
        if (getUnitAt(spawnX, spawnY) != null) {
            return;
        }
        Unit reinforcement = createUnit(difficultyIndex == 2 ? UnitKind.HEAVY_KNIGHT : UnitKind.RAIDER_SWORDSMAN, false, spawnX, spawnY);
        reinforcement.name = difficultyIndex == 2 ? "Reserve Knight" : "Late Raider";
        units.add(reinforcement);
        hintText = "Enemy reserves enter from the rear.";
        if (audio != null) {
            audio.playWarning();
        }
    }

    private int movementCost(Unit unit, TerrainType terrain) {
        if (unit.flying) {
            return terrain == TerrainType.MOUNTAIN ? 2 : 1;
        }
        if (terrain == TerrainType.ROAD || terrain == TerrainType.BRIDGE || terrain == TerrainType.KEEP || terrain == TerrainType.CAPITAL || terrain == TerrainType.FORT || terrain == TerrainType.VILLAGE) {
            return 1;
        }
        if (terrain == TerrainType.PLAINS || terrain == TerrainType.SHRINE || terrain == TerrainType.EXIT) {
            return 1;
        }
        if (terrain == TerrainType.FOREST) {
            return unit.kind == UnitKind.RANGER ? 1 : 2;
        }
        if (terrain == TerrainType.HILL || terrain == TerrainType.RUINS) {
            return unit.cavalry ? 3 : 2;
        }
        if (terrain == TerrainType.MARSH) {
            return unit.cavalry ? 4 : 2;
        }
        if (terrain == TerrainType.RIVER) {
            return 99;
        }
        if (terrain == TerrainType.MOUNTAIN) {
            return unit.kind == UnitKind.RANGER ? 3 : 99;
        }
        return 1;
    }

    private int terrainDefense(TerrainType terrain) {
        if (terrain == TerrainType.FOREST) {
            return 2;
        }
        if (terrain == TerrainType.HILL || terrain == TerrainType.FORT || terrain == TerrainType.KEEP || terrain == TerrainType.CAPITAL) {
            return 3;
        }
        if (terrain == TerrainType.RUINS || terrain == TerrainType.SHRINE) {
            return 1;
        }
        return 0;
    }

    private boolean canAttack(Unit attacker, Unit defender, int ax, int ay) {
        int dist = Math.abs(defender.x - ax) + Math.abs(defender.y - ay);
        return dist >= attacker.rangeMin && dist <= attacker.rangeMax;
    }

    private boolean canCounter(Unit defender, Unit attacker) {
        return canAttack(defender, attacker, defender.x, defender.y);
    }

    private int forecastDamage(Unit attacker, Unit defender, int fromX, int fromY) {
        Tile attackerTile = getTile(fromX, fromY);
        Tile defenderTile = getTile(defender.x, defender.y);
        int attackBonus = attacker.auraTurns > 0 ? 2 : 0;
        if (attacker.commander) {
            attackBonus += 1;
        }
        int base = attacker.attack + attackBonus - defender.defense - (defenderTile == null ? 0 : terrainDefense(defenderTile.terrain));
        if (attacker.kind == UnitKind.SPEARMAN || attacker.kind == UnitKind.PIKE_GUARD) {
            if (defender.cavalry) {
                base += 2;
            }
        }
        if (attacker.kind == UnitKind.KNIGHT || attacker.kind == UnitKind.HEAVY_KNIGHT || attacker.kind == UnitKind.LANCER || attacker.kind == UnitKind.WOLF_RIDER) {
            if (defender.kind == UnitKind.ARCHER || defender.kind == UnitKind.CROSSBOWMAN || defender.kind == UnitKind.BATTLE_MAGE || defender.kind == UnitKind.ROGUE_MAGE) {
                base += 1;
            }
        }
        if (attacker.kind == UnitKind.ARCHER || attacker.kind == UnitKind.CROSSBOWMAN || attacker.kind == UnitKind.BALLISTA_CREW) {
            if (attackerTile != null && attackerTile.terrain == TerrainType.HILL) {
                base += 1;
            }
        }
        return Math.max(1, base);
    }

    private float attackScore(Unit attacker, Unit target, int moveX, int moveY) {
        int damage = forecastDamage(attacker, target, moveX, moveY);
        float score = damage * 4f;
        if (damage >= target.hp) {
            score += 30f;
        }
        if (target.commander) {
            score += 36f;
        }
        if (attacker.healer) {
            score -= 15f;
        }
        if (difficultyIndex == 2 && target == alliedCommander) {
            score += 12f;
        }
        if (difficultyIndex == 0 && attacker.commander) {
            score -= 10f;
        }
        return score;
    }

    private float captureScore(Tile tile) {
        if (tile.terrain == TerrainType.CAPITAL) {
            return 60f;
        }
        if (tile.terrain == TerrainType.KEEP || tile.terrain == TerrainType.FORT) {
            return 30f;
        }
        if (tile.terrain == TerrainType.VILLAGE) {
            return 20f;
        }
        return 8f;
    }

    private int[] chooseAdvanceTarget(Unit unit) {
        if (currentChapter == null) {
            return null;
        }
        if (currentChapter.objectiveType == ObjectiveType.CAPTURE_CAPITAL) {
            return findStructureTile(unit.ally ? -1 : 1, TerrainType.CAPITAL, TerrainType.KEEP);
        }
        if (currentChapter.objectiveType == ObjectiveType.ESCORT_HEIR && unit.ally) {
            return new int[]{currentChapter.width - 1, currentChapter.height / 2};
        }
        Unit preferred = unit.ally ? enemyCommander : alliedCommander;
        if (preferred != null && !preferred.defeated) {
            return new int[]{preferred.x, preferred.y};
        }
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                Tile tile = tiles[x][y];
                if (tile.capturable && tile.owner != (unit.ally ? 1 : -1)) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private int[] chooseAdvanceCell(Unit unit, ArrayList<int[]> cells, int targetX, int targetY) {
        int[] best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < cells.size(); i++) {
            int[] cell = cells.get(i);
            Tile tile = getTile(cell[0], cell[1]);
            float score = -distance(cell[0], cell[1], targetX, targetY) * 3f;
            score += terrainDefense(tile.terrain) * (difficultyIndex == 2 ? 2f : 1f);
            if (tile.capturable && tile.owner != (unit.ally ? 1 : -1)) {
                score += captureScore(tile);
            }
            if (tile.terrain == TerrainType.HILL && (unit.ranged || unit.healer)) {
                score += 4f;
            }
            if (best == null || score > bestScore) {
                best = cell;
                bestScore = score;
            }
        }
        return best;
    }

    private Unit bestHealFromCell(Unit healer, int fromX, int fromY) {
        Unit best = null;
        int bestNeed = 0;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.ally != healer.ally || unit.defeated || unit == healer) {
                continue;
            }
            int dist = Math.abs(unit.x - fromX) + Math.abs(unit.y - fromY);
            if (dist == 1) {
                int need = unit.maxHp - unit.hp;
                if (need > bestNeed) {
                    bestNeed = need;
                    best = unit;
                }
            }
        }
        return best;
    }

    private Unit findHealTarget(Unit healer) {
        return bestHealFromCell(healer, healer.x, healer.y);
    }

    private int[] findRecruitSpawn(int x, int y) {
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        int targetX = currentChapter == null ? x : currentChapter.width - 1;
        for (int i = 0; i < cardinal.length; i++) {
            int nx = x + cardinal[i][0];
            int ny = y + cardinal[i][1];
            if (!isInsideBoard(nx, ny) || getUnitAt(nx, ny) != null) {
                continue;
            }
            Tile tile = getTile(nx, ny);
            if (tile == null || tile.terrain == TerrainType.RIVER || tile.terrain == TerrainType.MOUNTAIN) {
                continue;
            }
            int dist = Math.abs(targetX - nx);
            if (dist < bestDist) {
                bestDist = dist;
                best = new int[]{nx, ny};
            }
        }
        return best;
    }

    private int incomeFor(boolean ally) {
        int owner = ally ? 1 : -1;
        int total = 0;
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                Tile tile = tiles[x][y];
                if (tile.owner == owner) {
                    total += tile.income;
                }
            }
        }
        return total;
    }

    private void updateSelectionInfo(int tileX, int tileY) {
        Tile tile = getTile(tileX, tileY);
        if (tile == null) {
            return;
        }
        if (selectedUnit == null) {
            hintText = tile.label.isEmpty() ? terrainLabel(tile.terrain) : tile.label;
            sendHud();
            return;
        }
        if (selectedUnit.healer && findHealTarget(selectedUnit) != null) {
            hintText = "Heal a nearby ally or wait for the enemy turn.";
        } else if (tile.capturable && tile.owner != 1 && selectedUnit.canCapture) {
            hintText = "Capture " + tile.label + " to change the income line.";
        } else if (!selectedUnit.acted && canAttackAny(selectedUnit)) {
            hintText = "Tap a marked enemy to commit the attack.";
        } else if (!selectedUnit.moved) {
            hintText = "Move to a highlighted tile or hold this line.";
        } else {
            hintText = selectedUnit.name + " can wait or attack from the new ground.";
        }
        sendHud();
    }

    private boolean canAttackAny(Unit unit) {
        for (int i = 0; i < units.size(); i++) {
            Unit target = units.get(i);
            if (target.ally == unit.ally || target.defeated) {
                continue;
            }
            if (canAttack(unit, target, unit.x, unit.y)) {
                return true;
            }
        }
        return false;
    }

    private void checkVictoryState() {
        if (battleEnded || currentChapter == null) {
            return;
        }
        if (alliedCommander == null || alliedCommander.defeated) {
            finishBattle(false, "The marshal fell and the frontier standard collapsed.");
            return;
        }
        if (currentChapter.objectiveType == ObjectiveType.ESCORT_HEIR) {
            if (heirUnit == null || heirUnit.defeated) {
                finishBattle(false, "The heir was cut down before reaching the frontier road.");
                return;
            }
            Tile heirTile = getTile(heirUnit.x, heirUnit.y);
            if (heirTile != null && heirTile.exitTile) {
                finishBattle(true, "The heir escaped through the frontier road under your guard.");
                return;
            }
        }
        if (currentChapter.objectiveType == ObjectiveType.CAPTURE_CAPITAL) {
            int[] capital = findStructureTile(-1, TerrainType.CAPITAL, TerrainType.KEEP);
            if (capital != null) {
                Tile tile = getTile(capital[0], capital[1]);
                if (tile != null && tile.owner == 1) {
                    finishBattle(true, "The capital gates fell and the frontier campaign surged forward.");
                    return;
                }
            }
        }
        if (enemyCommander != null && enemyCommander.defeated && currentChapter.objectiveType != ObjectiveType.SURVIVE_TURNS) {
            finishBattle(true, "The rival commander is gone and the field is yours.");
        }
    }

    private void finishBattle(boolean victory, String resultLine) {
        battleEnded = true;
        paused = true;
        aiTurn = false;
        String summary = "Chapter " + currentChapter.index + " - " + currentChapter.title + "\n"
                + resultLine + "\n\n"
                + "Turns: " + turnNumber + "\n"
                + "Gold Left: " + gold + "\n"
                + "Income: " + incomeFor(true) + "\n"
                + "Villages Claimed: " + villagesCaptured + "\n"
                + "Allied Losses: " + alliedLosses + "\n"
                + "Enemy Losses: " + enemyLosses;
        if (callbacks != null) {
            callbacks.onBattleEnded(victory, new BattleReport(summary));
        }
    }

    private void sendHud() {
        if (callbacks == null) {
            return;
        }
        String region = currentChapter == null ? "Campaign Atlas" : currentChapter.region + "  " + currentChapter.title;
        String turn = battleActive ? (aiTurn ? "Enemy Phase " + turnNumber : "Player Phase " + turnNumber) : "No Battle";
        String goldText = battleActive ? String.valueOf(gold) : "0";
        String incomeText = battleActive ? "+" + incomeFor(true) : "+0";
        String difficulty = difficultyLabel();
        String selectedName = selectedUnit == null ? "No Unit Selected" : selectedUnit.name;
        String selectedStats = selectedUnit == null ? "Tap a unit or inspect a tile." : buildUnitStats(selectedUnit);
        String selectedTerrain = selectedUnit == null ? buildTerrainSummary(-1, -1) : buildTerrainSummary(selectedUnit.x, selectedUnit.y);
        callbacks.onHudChanged(new HudSnapshot(
                region,
                turn,
                goldText,
                incomeText,
                difficulty,
                hintText,
                objectiveText,
                selectedName,
                selectedStats,
                selectedTerrain,
                contextLabel(),
                contextEnabled(),
                battleActive && !aiTurn && !battleEnded && orderCharges > 0,
                recruitEnabled()
        ));
    }

    private String contextLabel() {
        if (selectedUnit == null || selectedUnit.defeated || selectedUnit.acted || !selectedUnit.ally) {
            return "Wait";
        }
        Tile tile = getTile(selectedUnit.x, selectedUnit.y);
        if (tile != null && tile.capturable && tile.owner != 1 && selectedUnit.canCapture) {
            return "Capture";
        }
        if (selectedUnit.healer && findHealTarget(selectedUnit) != null) {
            return "Heal";
        }
        return "Wait";
    }

    private boolean contextEnabled() {
        return selectedUnit != null && !selectedUnit.defeated && selectedUnit.ally && !selectedUnit.acted;
    }

    private boolean recruitEnabled() {
        if (selectedUnit == null || selectedUnit.defeated || !selectedUnit.ally || selectedUnit.acted) {
            return false;
        }
        Tile tile = getTile(selectedUnit.x, selectedUnit.y);
        return tile != null && tile.recruitSite && tile.owner == 1 && findRecruitSpawn(selectedUnit.x, selectedUnit.y) != null;
    }

    private String difficultyLabel() {
        if (difficultyIndex == 0) {
            return "Easy AI";
        }
        if (difficultyIndex == 2) {
            return "Hard AI";
        }
        return "Normal AI";
    }

    private String buildUnitStats(Unit unit) {
        return unit.hp + "/" + unit.maxHp + " HP   ATK " + unit.attack + "   DEF " + unit.defense + "   MOV " + unit.move
                + "\nRange " + unit.rangeMin + "-" + unit.rangeMax + "   Cost " + unit.cost
                + "\n" + (unit.auraTurns > 0 ? "Inspired  " : "") + roleLabel(unit);
    }

    private String buildTerrainSummary(int x, int y) {
        if (!isInsideBoard(x, y)) {
            return "Terrain data appears here.";
        }
        Tile tile = getTile(x, y);
        if (tile == null) {
            return "Terrain data appears here.";
        }
        String ownerText = tile.owner == 1 ? "Allied" : (tile.owner == -1 ? "Enemy" : "Neutral");
        String label = tile.label.isEmpty() ? terrainLabel(tile.terrain) : tile.label;
        return label + "  " + ownerText + "\nDEF +" + terrainDefense(tile.terrain) + "  Move " + terrainMoveSummary(tile.terrain) + "  Income " + tile.income;
    }

    private String terrainMoveSummary(TerrainType terrain) {
        if (terrain == TerrainType.RIVER) {
            return "Blocked";
        }
        if (terrain == TerrainType.MOUNTAIN) {
            return "Harsh";
        }
        if (terrain == TerrainType.FOREST || terrain == TerrainType.HILL || terrain == TerrainType.MARSH || terrain == TerrainType.RUINS) {
            return "Slow";
        }
        return "Fast";
    }

    private String terrainLabel(TerrainType terrain) {
        if (terrain == TerrainType.PLAINS) {
            return "Plains";
        }
        if (terrain == TerrainType.ROAD) {
            return "Road";
        }
        if (terrain == TerrainType.FOREST) {
            return "Forest";
        }
        if (terrain == TerrainType.HILL) {
            return "Hill";
        }
        if (terrain == TerrainType.MOUNTAIN) {
            return "Mountain";
        }
        if (terrain == TerrainType.RIVER) {
            return "River";
        }
        if (terrain == TerrainType.BRIDGE) {
            return "Bridge";
        }
        if (terrain == TerrainType.MARSH) {
            return "Marsh";
        }
        if (terrain == TerrainType.RUINS) {
            return "Ruins";
        }
        if (terrain == TerrainType.SHRINE) {
            return "Shrine";
        }
        if (terrain == TerrainType.VILLAGE) {
            return "Village";
        }
        if (terrain == TerrainType.FORT) {
            return "Fort";
        }
        if (terrain == TerrainType.KEEP) {
            return "Keep";
        }
        if (terrain == TerrainType.CAPITAL) {
            return "Capital";
        }
        return "Exit";
    }

    private String roleLabel(Unit unit) {
        if (unit.commander) {
            return "Commander";
        }
        if (unit.healer) {
            return "Support";
        }
        if (unit.ranged && unit.siege) {
            return "Siege";
        }
        if (unit.ranged) {
            return "Ranged";
        }
        if (unit.cavalry) {
            return "Cavalry";
        }
        if (unit.flying) {
            return "Flying";
        }
        if (unit.heir) {
            return "Escort";
        }
        return "Front Line";
    }

    private void drawBackdrop(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.cst_bg_alt));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setColor(adjustAlpha(color(R.color.cst_accent_2), 0.18f));
        for (int i = 0; i < 7; i++) {
            float left = (i * 163f) % Math.max(1f, getWidth());
            canvas.drawRect(left, 0f, left + 1f, getHeight(), paint);
        }
        paint.setColor(adjustAlpha(color(R.color.cst_text_muted), 0.08f));
        for (int i = 0; i < 5; i++) {
            float top = (i * 121f) % Math.max(1f, getHeight());
            canvas.drawRect(0f, top, getWidth(), top + 1f, paint);
        }
    }

    private void drawMenuBackdrop(Canvas canvas) {
        paint.setColor(adjustAlpha(color(R.color.cst_accent), 0.16f));
        canvas.drawCircle(getWidth() * 0.25f, getHeight() * 0.35f, Math.min(getWidth(), getHeight()) * 0.12f, paint);
        paint.setColor(adjustAlpha(color(R.color.cst_accent_2), 0.14f));
        canvas.drawCircle(getWidth() * 0.72f, getHeight() * 0.54f, Math.min(getWidth(), getHeight()) * 0.16f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(color(R.color.cst_text_primary));
        paint.setTextSize(dp(18));
        canvas.drawText("Frontier War Table", getWidth() * 0.5f, getHeight() * 0.56f, paint);
        paint.setTextSize(dp(11));
        paint.setColor(color(R.color.cst_text_secondary));
        canvas.drawText("Open the atlas and begin the chapter march.", getWidth() * 0.5f, getHeight() * 0.61f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTiles(Canvas canvas) {
        int maxX = tiles.length;
        int maxY = tiles[0].length;
        int startX = Math.max(0, (int) Math.floor(cameraX));
        int startY = Math.max(0, (int) Math.floor(cameraY));
        int endX = Math.min(maxX, (int) Math.ceil(cameraX + boardRect.width() / cellSize) + 1);
        int endY = Math.min(maxY, (int) Math.ceil(cameraY + boardRect.height() / cellSize) + 1);
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                float left = boardRect.left + (x - cameraX) * cellSize;
                float top = boardRect.top + (y - cameraY) * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                Tile tile = tiles[x][y];
                Bitmap terrainBitmap = bitmaps.get(terrainAssetKey(tile.terrain));
                if (terrainBitmap != null) {
                    dstRect.set(left, top, right, bottom);
                    canvas.drawBitmap(terrainBitmap, null, dstRect, null);
                } else {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(terrainColor(tile.terrain));
                    canvas.drawRect(left, top, right, bottom, paint);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(adjustAlpha(color(R.color.cst_shadow), 0.25f));
                canvas.drawRect(left, top, right, bottom, paint);
                if (tile.pulse > 0f) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(adjustAlpha(tile.owner == 1 ? color(R.color.cst_success) : color(R.color.cst_danger), tile.pulse));
                    canvas.drawRect(left + dp(3), top + dp(3), right - dp(3), bottom - dp(3), paint);
                }
            }
        }
    }

    private void drawStructures(Canvas canvas) {
        int maxX = tiles.length;
        int maxY = tiles[0].length;
        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                Tile tile = tiles[x][y];
                if (!tile.capturable && tile.terrain != TerrainType.SHRINE) {
                    continue;
                }
                float left = boardRect.left + (x - cameraX) * cellSize;
                float top = boardRect.top + (y - cameraY) * cellSize;
                float cx = left + cellSize * 0.5f;
                float cy = top + cellSize * 0.5f;
                if (rightOutsideBoard(left, top)) {
                    continue;
                }
                drawStructureToken(canvas, tile, cx, cy, cellSize * 0.78f);
            }
        }
    }

    private void drawHighlights(Canvas canvas) {
        if (selectedUnit == null || selectedUnit.defeated || !selectedUnit.ally || paused || aiTurn) {
            return;
        }
        for (int x = 0; x < reachable.length; x++) {
            for (int y = 0; y < reachable[x].length; y++) {
                if (!reachable[x][y] || (x == selectedUnit.x && y == selectedUnit.y)) {
                    continue;
                }
                float left = boardRect.left + (x - cameraX) * cellSize;
                float top = boardRect.top + (y - cameraY) * cellSize;
                if (rightOutsideBoard(left, top)) {
                    continue;
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(adjustAlpha(color(R.color.cst_accent_2), 0.24f));
                canvas.drawRect(left + dp(3), top + dp(3), left + cellSize - dp(3), top + cellSize - dp(3), paint);
            }
        }
        for (int x = 0; x < attackable.length; x++) {
            for (int y = 0; y < attackable[x].length; y++) {
                if (!attackable[x][y]) {
                    continue;
                }
                float left = boardRect.left + (x - cameraX) * cellSize;
                float top = boardRect.top + (y - cameraY) * cellSize;
                if (rightOutsideBoard(left, top)) {
                    continue;
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(adjustAlpha(color(R.color.cst_danger), 0.8f));
                canvas.drawRect(left + dp(5), top + dp(5), left + cellSize - dp(5), top + cellSize - dp(5), paint);
            }
        }
        float left = boardRect.left + (selectedUnit.x - cameraX) * cellSize;
        float top = boardRect.top + (selectedUnit.y - cameraY) * cellSize;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4));
        paint.setColor(color(R.color.cst_accent));
        canvas.drawRect(left + dp(2), top + dp(2), left + cellSize - dp(2), top + cellSize - dp(2), paint);
    }

    private void drawUnits(Canvas canvas) {
        ArrayList<Unit> drawList = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (!unit.defeated) {
                drawList.add(unit);
            }
        }
        Collections.sort(drawList, (a, b) -> Float.compare(a.renderY, b.renderY));
        for (int i = 0; i < drawList.size(); i++) {
            Unit unit = drawList.get(i);
            float cx = boardRect.left + (unit.renderX - cameraX + 0.5f) * cellSize;
            float cy = boardRect.top + (unit.renderY - cameraY + 0.5f) * cellSize;
            if (cx < boardRect.left - cellSize || cx > boardRect.right + cellSize || cy < boardRect.top - cellSize || cy > boardRect.bottom + cellSize) {
                continue;
            }
            drawUnitToken(canvas, unit, cx, cy, cellSize * 0.72f);
        }
    }

    private void drawBoardFrame(Canvas canvas) {
        Bitmap roguelikeSheet = bitmaps.get("game_art/kenney_roguelike_rpg_pack/assets/Spritesheet/roguelikeSheet_transparent.png");
        if (roguelikeSheet != null) {
            srcRect.set(270, 410, 760, 560);
            dstRect.set(boardRect.left + dp(10), boardRect.top + dp(10), boardRect.right - dp(10), boardRect.top + dp(38));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setAlpha(72);
            canvas.drawBitmap(roguelikeSheet, srcRect, dstRect, paint);
            paint.setAlpha(255);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(adjustAlpha(color(R.color.cst_panel_stroke), 0.85f));
        canvas.drawRect(boardRect, paint);
        paint.setStrokeWidth(dp(1));
        paint.setColor(adjustAlpha(color(R.color.cst_text_muted), 0.35f));
        canvas.drawRect(boardRect.left - dp(4), boardRect.top - dp(4), boardRect.right + dp(4), boardRect.bottom + dp(4), paint);
    }

    private void drawStructureToken(Canvas canvas, Tile tile, float cx, float cy, float size) {
        float half = size * 0.5f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tile.owner == 1 ? adjustAlpha(color(R.color.cst_success), 0.24f) : tile.owner == -1 ? adjustAlpha(color(R.color.cst_danger), 0.22f) : adjustAlpha(color(R.color.cst_warning), 0.22f));
        canvas.drawCircle(cx, cy, half * 0.78f, paint);
        if (tile.terrain == TerrainType.VILLAGE) {
            shapePath.reset();
            shapePath.moveTo(cx - half * 0.52f, cy + half * 0.22f);
            shapePath.lineTo(cx, cy - half * 0.44f);
            shapePath.lineTo(cx + half * 0.52f, cy + half * 0.22f);
            shapePath.close();
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawPath(shapePath, paint);
        } else if (tile.terrain == TerrainType.FORT || tile.terrain == TerrainType.KEEP || tile.terrain == TerrainType.CAPITAL) {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawRect(cx - half * 0.44f, cy - half * 0.24f, cx + half * 0.44f, cy + half * 0.34f, paint);
            paint.setColor(color(R.color.cst_bg_main));
            canvas.drawRect(cx - half * 0.14f, cy - half * 0.02f, cx + half * 0.14f, cy + half * 0.34f, paint);
        } else if (tile.terrain == TerrainType.SHRINE) {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawCircle(cx, cy, half * 0.32f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(color(R.color.cst_accent));
            canvas.drawCircle(cx, cy, half * 0.52f, paint);
        }
    }

    private void drawUnitToken(Canvas canvas, Unit unit, float cx, float cy, float size) {
        float bob = unit.moveTime > 0f ? (float) Math.sin((1f - unit.moveTime / 0.18f) * Math.PI) * size * 0.06f : 0f;
        float pulse = unit.attackTime > 0f ? 1f + unit.attackTime * 0.6f : 1f;
        float radius = size * 0.36f * pulse;
        int baseColor = unit.ally ? color(R.color.cst_accent_2) : color(R.color.cst_danger);
        if (unit.commander) {
            baseColor = unit.ally ? color(R.color.cst_accent) : color(R.color.cst_danger);
        } else if (unit.healer) {
            baseColor = unit.ally ? color(R.color.cst_success) : adjustAlpha(color(R.color.cst_warning), 0.95f);
        }
        if (unit.hitTime > 0f) {
            baseColor = Color.WHITE;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(Color.BLACK, 0.25f));
        canvas.drawCircle(cx, cy + size * 0.22f, radius * 0.94f, paint);
        paint.setColor(baseColor);
        canvas.drawCircle(cx, cy + bob, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(unit.auraTurns > 0 ? color(R.color.cst_warning) : color(R.color.cst_text_primary));
        canvas.drawCircle(cx, cy + bob, radius, paint);
        float fx = unit.faceX == 0 ? 1f : unit.faceX;
        float fy = unit.faceY;
        shapePath.reset();
        shapePath.moveTo(cx + fx * radius * 0.92f, cy + bob + fy * radius * 0.18f);
        shapePath.lineTo(cx + fx * radius * 0.24f - fy * radius * 0.22f, cy + bob - fy * radius * 0.10f - fx * radius * 0.18f);
        shapePath.lineTo(cx + fx * radius * 0.22f + fy * radius * 0.22f, cy + bob - fy * radius * 0.08f + fx * radius * 0.18f);
        shapePath.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(unit.ally ? color(R.color.cst_text_primary) : color(R.color.cst_bg_main));
        canvas.drawPath(shapePath, paint);
        paint.setStrokeWidth(dp(3));
        if (unit.kind == UnitKind.ARCHER || unit.kind == UnitKind.CROSSBOWMAN || unit.kind == UnitKind.BALLISTA_CREW || unit.kind == UnitKind.BATTLE_MAGE || unit.kind == UnitKind.ROGUE_MAGE) {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawLine(cx - radius * 0.36f, cy + bob, cx + radius * 0.36f, cy + bob, paint);
            canvas.drawLine(cx, cy + bob - radius * 0.34f, cx, cy + bob + radius * 0.34f, paint);
        } else if (unit.kind == UnitKind.KNIGHT || unit.kind == UnitKind.HEAVY_KNIGHT || unit.kind == UnitKind.LANCER || unit.kind == UnitKind.WOLF_RIDER) {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawLine(cx - radius * 0.3f, cy + bob + radius * 0.24f, cx + radius * 0.3f, cy + bob - radius * 0.24f, paint);
            canvas.drawLine(cx - radius * 0.22f, cy + bob - radius * 0.24f, cx + radius * 0.22f, cy + bob + radius * 0.24f, paint);
        } else if (unit.healer) {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawLine(cx - radius * 0.25f, cy + bob, cx + radius * 0.25f, cy + bob, paint);
            canvas.drawLine(cx, cy + bob - radius * 0.25f, cx, cy + bob + radius * 0.25f, paint);
        } else if (unit.commander) {
            shapePath.reset();
            shapePath.moveTo(cx, cy + bob - radius * 0.38f);
            shapePath.lineTo(cx + radius * 0.28f, cy + bob - radius * 0.02f);
            shapePath.lineTo(cx + radius * 0.12f, cy + bob + radius * 0.26f);
            shapePath.lineTo(cx - radius * 0.12f, cy + bob + radius * 0.26f);
            shapePath.lineTo(cx - radius * 0.28f, cy + bob - radius * 0.02f);
            shapePath.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawPath(shapePath, paint);
        } else {
            paint.setColor(color(R.color.cst_text_primary));
            canvas.drawCircle(cx, cy + bob, radius * 0.16f, paint);
        }
        if (unit.attackTime > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(adjustAlpha(color(R.color.cst_warning), 0.85f));
            canvas.drawArc(cx - radius * 1.25f, cy + bob - radius * 1.25f, cx + radius * 1.25f, cy + bob + radius * 1.25f, -38f, 96f, false, paint);
        }
        if (unit.captureTime > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(adjustAlpha(unit.ally ? color(R.color.cst_success) : color(R.color.cst_danger), unit.captureTime));
            canvas.drawCircle(cx, cy + bob, radius * (1.2f + (0.45f - unit.captureTime)), paint);
        }
        drawHealthBar(canvas, unit, cx, cy + bob - size * 0.46f, radius * 1.4f);
    }

    private void drawHealthBar(Canvas canvas, Unit unit, float cx, float cy, float width) {
        float half = width * 0.5f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(adjustAlpha(Color.BLACK, 0.45f));
        canvas.drawRect(cx - half, cy, cx + half, cy + dp(5), paint);
        float ratio = unit.maxHp <= 0 ? 0f : unit.hp / (float) unit.maxHp;
        paint.setColor(ratio > 0.55f ? color(R.color.cst_success) : ratio > 0.25f ? color(R.color.cst_warning) : color(R.color.cst_danger));
        canvas.drawRect(cx - half, cy, cx - half + width * ratio, cy + dp(5), paint);
    }

    private void recomputeBoardBounds() {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        float left = getResources().getDimension(R.dimen.cst_side_panel_w) + getResources().getDimension(R.dimen.cst_board_margin) * 2f;
        float right = getResources().getDimension(R.dimen.cst_right_panel_w) + getResources().getDimension(R.dimen.cst_board_margin) * 2f;
        float top = dp(16);
        float bottom = getResources().getDimension(R.dimen.cst_bottom_bar_h) + getResources().getDimension(R.dimen.cst_board_margin) * 2f;
        boardRect.set(left, top, Math.max(left + dp(160), viewWidth - right), Math.max(top + dp(180), viewHeight - bottom));
        if (tiles != null) {
            float visibleCols = Math.min(10.5f, tiles.length);
            float visibleRows = Math.min(8.4f, tiles[0].length);
            cellSize = Math.min(boardRect.width() / visibleCols, boardRect.height() / visibleRows);
            if (cellSize < dp(28)) {
                cellSize = dp(28);
            }
            clampCamera();
        }
    }

    private void centerCameraOn(Unit unit) {
        if (unit == null || tiles == null) {
            return;
        }
        float visibleCols = boardRect.width() / cellSize;
        float visibleRows = boardRect.height() / cellSize;
        cameraX = unit.x - visibleCols * 0.5f;
        cameraY = unit.y - visibleRows * 0.5f;
        clampCamera();
    }

    private void clampCamera() {
        if (tiles == null) {
            cameraX = 0f;
            cameraY = 0f;
            return;
        }
        float visibleCols = boardRect.width() / cellSize;
        float visibleRows = boardRect.height() / cellSize;
        float maxX = Math.max(0f, tiles.length - visibleCols);
        float maxY = Math.max(0f, tiles[0].length - visibleRows);
        if (cameraX < 0f) {
            cameraX = 0f;
        }
        if (cameraY < 0f) {
            cameraY = 0f;
        }
        if (cameraX > maxX) {
            cameraX = maxX;
        }
        if (cameraY > maxY) {
            cameraY = maxY;
        }
    }

    private void seedTerrainAssetKeys() {
        terrainAssetKeys.put(TerrainType.PLAINS, "game_art/kenney_tiny_town/assets/Tiles/tile_0000.png");
        terrainAssetKeys.put(TerrainType.ROAD, "game_art/kenney_tiny_town/assets/Tiles/tile_0012.png");
        terrainAssetKeys.put(TerrainType.FOREST, "game_art/kenney_tiny_town/assets/Tiles/tile_0086.png");
        terrainAssetKeys.put(TerrainType.HILL, "game_art/kenney_tiny_town/assets/Tiles/tile_0046.png");
        terrainAssetKeys.put(TerrainType.MOUNTAIN, "game_art/kenney_tiny_town/assets/Tiles/tile_0038.png");
        terrainAssetKeys.put(TerrainType.RIVER, "game_art/kenney_tiny_town/assets/Tiles/tile_0104.png");
        terrainAssetKeys.put(TerrainType.BRIDGE, "game_art/kenney_tiny_town/assets/Tiles/tile_0090.png");
        terrainAssetKeys.put(TerrainType.MARSH, "game_art/kenney_tiny_town/assets/Tiles/tile_0054.png");
        terrainAssetKeys.put(TerrainType.RUINS, "game_art/kenney_tiny_town/assets/Tiles/tile_0027.png");
        terrainAssetKeys.put(TerrainType.SHRINE, "game_art/kenney_tiny_town/assets/Tiles/tile_0067.png");
        terrainAssetKeys.put(TerrainType.VILLAGE, "game_art/kenney_tiny_town/assets/Tiles/tile_0072.png");
        terrainAssetKeys.put(TerrainType.FORT, "game_art/kenney_tiny_town/assets/Tiles/tile_0117.png");
        terrainAssetKeys.put(TerrainType.KEEP, "game_art/kenney_tiny_town/assets/Tiles/tile_0120.png");
        terrainAssetKeys.put(TerrainType.CAPITAL, "game_art/kenney_tiny_town/assets/Tiles/tile_0121.png");
        terrainAssetKeys.put(TerrainType.EXIT, "game_art/kenney_tiny_town/assets/Tiles/tile_0015.png");
    }

    private void loadSharedBitmaps() {
        String[] keys = new String[]{
                "game_art/kenney_tiny_town/assets/Tiles/tile_0000.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0012.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0086.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0046.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0038.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0104.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0090.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0054.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0027.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0067.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0072.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0117.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0120.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0121.png",
                "game_art/kenney_tiny_town/assets/Tiles/tile_0015.png",
                "game_art/kenney_roguelike_rpg_pack/assets/Spritesheet/roguelikeSheet_transparent.png"
        };
        for (int i = 0; i < keys.length; i++) {
            Bitmap bitmap = loadBitmap(keys[i]);
            if (bitmap != null) {
                bitmaps.put(keys[i], bitmap);
            }
        }
    }

    private Bitmap loadBitmap(String assetPath) {
        AssetManager manager = getContext().getAssets();
        InputStream stream = null;
        try {
            stream = manager.open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) {
                return null;
            }
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        } catch (IOException ignored) {
            return null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String terrainAssetKey(TerrainType terrain) {
        return terrainAssetKeys.get(terrain);
    }

    private Unit getUnitAt(int x, int y) {
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (!unit.defeated && unit.x == x && unit.y == y) {
                return unit;
            }
        }
        return null;
    }

    private Tile getTile(int x, int y) {
        if (!isInsideBoard(x, y)) {
            return null;
        }
        return tiles[x][y];
    }

    private boolean isReachable(int x, int y) {
        return reachable != null && isInsideBoard(x, y) && reachable[x][y];
    }

    private boolean isInsideBoard(int x, int y) {
        return tiles != null && x >= 0 && y >= 0 && x < tiles.length && y < tiles[0].length;
    }

    private int[] findStructureTile(int owner, TerrainType primary, TerrainType fallback) {
        int[] fallbackPos = null;
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                Tile tile = tiles[x][y];
                if (tile.owner == owner && tile.terrain == primary) {
                    return new int[]{x, y};
                }
                if (tile.owner == owner && tile.terrain == fallback && fallbackPos == null) {
                    fallbackPos = new int[]{x, y};
                }
            }
        }
        return fallbackPos;
    }

    private void setTerrain(int x, int y, TerrainType terrain) {
        if (!isInsideBoard(x, y)) {
            return;
        }
        Tile tile = tiles[x][y];
        tile.terrain = terrain;
        if (terrain == TerrainType.BRIDGE || terrain == TerrainType.ROAD) {
            tile.income = 0;
        }
    }

    private void setStructure(int x, int y, TerrainType terrain, int owner, String label, int income, boolean recruitSite, boolean healSite) {
        if (!isInsideBoard(x, y)) {
            return;
        }
        Tile tile = tiles[x][y];
        tile.terrain = terrain;
        tile.owner = owner;
        tile.label = label;
        tile.income = income;
        tile.recruitSite = recruitSite;
        tile.healSite = healSite;
        tile.capturable = true;
    }

    private int screenToTileX(float sx) {
        return (int) Math.floor((sx - boardRect.left) / cellSize + cameraX);
    }

    private int screenToTileY(float sy) {
        return (int) Math.floor((sy - boardRect.top) / cellSize + cameraY);
    }

    private boolean rightOutsideBoard(float left, float top) {
        return left + cellSize < boardRect.left || left > boardRect.right || top + cellSize < boardRect.top || top > boardRect.bottom;
    }

    private int terrainColor(TerrainType terrain) {
        if (terrain == TerrainType.PLAINS) {
            return adjustAlpha(color(R.color.cst_success), 0.72f);
        }
        if (terrain == TerrainType.ROAD) {
            return color(R.color.cst_game_road);
        }
        if (terrain == TerrainType.FOREST) {
            return Color.rgb(77, 123, 79);
        }
        if (terrain == TerrainType.HILL) {
            return Color.rgb(154, 132, 93);
        }
        if (terrain == TerrainType.MOUNTAIN) {
            return color(R.color.cst_game_stone);
        }
        if (terrain == TerrainType.RIVER) {
            return Color.rgb(89, 134, 191);
        }
        if (terrain == TerrainType.BRIDGE) {
            return Color.rgb(139, 98, 63);
        }
        if (terrain == TerrainType.MARSH) {
            return Color.rgb(97, 118, 88);
        }
        if (terrain == TerrainType.RUINS) {
            return Color.rgb(138, 133, 121);
        }
        if (terrain == TerrainType.SHRINE) {
            return Color.rgb(182, 166, 137);
        }
        if (terrain == TerrainType.VILLAGE) {
            return Color.rgb(205, 176, 129);
        }
        if (terrain == TerrainType.FORT || terrain == TerrainType.KEEP || terrain == TerrainType.CAPITAL) {
            return Color.rgb(134, 141, 155);
        }
        return Color.rgb(217, 188, 128);
    }

    private String unitLabel(UnitKind kind) {
        if (kind == UnitKind.MARSHAL_COMMANDER) {
            return "Marshal Commander";
        }
        if (kind == UnitKind.MILITIA) {
            return "Militia";
        }
        if (kind == UnitKind.SWORDSMAN) {
            return "Swordsman";
        }
        if (kind == UnitKind.SPEARMAN) {
            return "Spearman";
        }
        if (kind == UnitKind.ARCHER) {
            return "Archer";
        }
        if (kind == UnitKind.RANGER) {
            return "Ranger";
        }
        if (kind == UnitKind.KNIGHT) {
            return "Knight";
        }
        if (kind == UnitKind.LANCER) {
            return "Lancer";
        }
        if (kind == UnitKind.HEALER) {
            return "Healer";
        }
        if (kind == UnitKind.BATTLE_MAGE) {
            return "Battle Mage";
        }
        if (kind == UnitKind.SCOUT_RIDER) {
            return "Scout Rider";
        }
        if (kind == UnitKind.WYVERN_RIDER) {
            return "Wyvern Rider";
        }
        if (kind == UnitKind.BALLISTA_CREW) {
            return "Ballista Crew";
        }
        if (kind == UnitKind.RAIDER_SWORDSMAN) {
            return "Raider Swordsman";
        }
        if (kind == UnitKind.PIKE_GUARD) {
            return "Pike Guard";
        }
        if (kind == UnitKind.CROSSBOWMAN) {
            return "Crossbowman";
        }
        if (kind == UnitKind.WOLF_RIDER) {
            return "Wolf Rider";
        }
        if (kind == UnitKind.HEAVY_KNIGHT) {
            return "Heavy Knight";
        }
        if (kind == UnitKind.WAR_PRIEST) {
            return "War Priest";
        }
        if (kind == UnitKind.ROGUE_MAGE) {
            return "Rogue Mage";
        }
        if (kind == UnitKind.ELITE_COMMANDER) {
            return "Elite Commander";
        }
        return "Heir";
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    private int adjustAlpha(int color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(255f * alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int distance(int x0, int y0, int x1, int y1) {
        return Math.abs(x0 - x1) + Math.abs(y0 - y1);
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private int clamp(int delta) {
        if (delta > 0) {
            return 1;
        }
        if (delta < 0) {
            return -1;
        }
        return 0;
    }

    private float dp(float value) {
        return getResources().getDisplayMetrics().density * value;
    }
}
