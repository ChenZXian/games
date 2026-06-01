package com.android.boot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class GameView extends View {
    public interface GameListener {
        void onSound(String role);

        void onDayClear();

        void onGameOver();
    }

    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private static final int CONDITION_HUNGRY = 0;
    private static final int CONDITION_DIRTY = 1;
    private static final int CONDITION_CHILLED = 2;
    private static final int CONDITION_SCRATCHED = 3;
    private static final int CONDITION_TIRED = 4;
    private static final int CONDITION_STRESSED = 5;
    private static final int STATION_TONIC = 0;
    private static final int STATION_BRUSH = 1;
    private static final int STATION_BLANKET = 2;
    private static final int STATION_MEDICINE = 3;
    private static final int STATION_RECOVERY = 4;
    private static final String GAME_ART_MAP_PATH = "game_art/runtime_art_map.json";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(11);
    private final Path shapePath = new Path();
    private final RectF board = new RectF();
    private final RectF tray = new RectF();
    private final RectF intake = new RectF();
    private final RectF rack = new RectF();
    private final RectF releaseGate = new RectF();
    private final RectF scratch = new RectF();
    private final Animal[] animals = new Animal[6];
    private final Station[] stations = new Station[5];
    private final RectF[] cardRects = new RectF[6];
    private final RectF[] stationRects = new RectF[5];
    private final int[] treatmentStock = new int[6];
    private GameState state = GameState.MENU;
    private long lastTick;
    private boolean loopRunning;
    private int selectedAnimal = -1;
    private boolean dragActive;
    private float dragX;
    private float dragY;
    private int coins;
    private int trust;
    private int targetTrust;
    private int day;
    private int stressedMisses;
    private float shiftTime;
    private float arrivalTimer;
    private float toastTimer;
    private String toast = "Start a clinic shift";
    private GameListener listener;
    private int cBgMain;
    private int cPanel;
    private int cStroke;
    private int cText;
    private int cMuted;
    private int cAccent;
    private int cAccent2;
    private int cSuccess;
    private int cWarning;
    private int cDanger;
    private int cGrass;
    private int cGrassDark;
    private int cPath;
    private int cCanvas;
    private int cWood;
    private int cKit;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void startGame() {
        coins = 35;
        trust = 0;
        targetTrust = 85;
        day = 1;
        stressedMisses = 0;
        shiftTime = 240f;
        arrivalTimer = 0f;
        selectedAnimal = -1;
        for (int i = 0; i < treatmentStock.length; i++) {
            treatmentStock[i] = 3;
        }
        for (int i = 0; i < stations.length; i++) {
            stations[i].animalIndex = -1;
            stations[i].timer = 0f;
        }
        for (int i = 0; i < animals.length; i++) {
            setupAnimal(i, i, false);
        }
        state = GameState.PLAYING;
        toast = "Drag animals to matching care stations";
        toastTimer = 3f;
        lastTick = SystemClock.uptimeMillis();
        invalidate();
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastTick = SystemClock.uptimeMillis();
        }
    }

    public void showHelp() {
        toast = "Inspect symptoms, move animals to matching stations, release happy patients.";
        toastTimer = 5f;
        invalidate();
    }

    public void resumeLoop() {
        if (!loopRunning) {
            loopRunning = true;
            lastTick = SystemClock.uptimeMillis();
            postOnAnimation(loopStep);
        }
    }

    public void pauseLoop() {
        loopRunning = false;
        removeCallbacks(loopStep);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        pauseLoop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        computeLayout();
        drawBackground(canvas);
        drawClinic(canvas);
        drawAnimals(canvas);
        drawTray(canvas);
        drawRack(canvas);
        drawHud(canvas);
        drawToast(canvas);
        if (state == GameState.GAME_OVER) {
            drawResult(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state != GameState.PLAYING) {
            return true;
        }
        computeLayout();
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            selectedAnimal = hitAnimal(x, y);
            if (selectedAnimal >= 0) {
                dragActive = true;
                dragX = x;
                dragY = y;
                toast = animals[selectedAnimal].name + " needs " + conditionName(animals[selectedAnimal].condition);
                toastTimer = 2f;
                emitSound("animal_select");
                invalidate();
                return true;
            }
            int station = hitStation(x, y);
            if (station >= 0) {
                releaseStation(station);
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (selectedAnimal >= 0 && dragActive) {
                dragX = x;
                dragY = y;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (selectedAnimal >= 0) {
                int station = hitStation(x, y);
                if (station >= 0) {
                    assignAnimal(selectedAnimal, station);
                }
                selectedAnimal = -1;
                dragActive = false;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            selectedAnimal = -1;
            dragActive = false;
            invalidate();
            return true;
        }
        return true;
    }

    private void init() {
        setFocusable(true);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
        cBgMain = color(R.color.cst_bg_main);
        cPanel = color(R.color.cst_panel_bg);
        cStroke = color(R.color.cst_panel_stroke);
        cText = color(R.color.cst_text_primary);
        cMuted = color(R.color.cst_text_muted);
        cAccent = color(R.color.cst_accent);
        cAccent2 = color(R.color.cst_accent_2);
        cSuccess = color(R.color.cst_success);
        cWarning = color(R.color.cst_warning);
        cDanger = color(R.color.cst_danger);
        cGrass = color(R.color.cst_grass);
        cGrassDark = color(R.color.cst_grass_dark);
        cPath = color(R.color.cst_path);
        cCanvas = color(R.color.cst_canvas);
        cWood = color(R.color.cst_wood);
        cKit = color(R.color.cst_kit);
        verifyRuntimeArtMap();
        for (int i = 0; i < animals.length; i++) {
            animals[i] = new Animal();
            setupAnimal(i, i, true);
        }
        for (int i = 0; i < stations.length; i++) {
            stations[i] = new Station();
            stations[i].kind = i;
            stations[i].animalIndex = -1;
        }
        for (int i = 0; i < treatmentStock.length; i++) {
            treatmentStock[i] = 3;
        }
        coins = 35;
        targetTrust = 85;
        day = 1;
        shiftTime = 240f;
    }

    private final Runnable loopStep = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(0.05f, (now - lastTick) / 1000f);
            lastTick = now;
            if (state == GameState.PLAYING) {
                update(dt);
            }
            invalidate();
            postOnAnimation(this);
        }
    };

    private int color(int resId) {
        return getResources().getColor(resId, null);
    }

    private void verifyRuntimeArtMap() {
        try {
            getContext().getAssets().open(GAME_ART_MAP_PATH).close();
        } catch (Exception ex) {
        }
    }

    private void update(float dt) {
        shiftTime -= dt;
        arrivalTimer += dt;
        if (toastTimer > 0f) {
            toastTimer -= dt;
        }
        if (arrivalTimer > 18f) {
            arrivalTimer = 0f;
            addArrival();
        }
        for (int i = 0; i < stations.length; i++) {
            Station station = stations[i];
            if (station.animalIndex >= 0) {
                station.timer -= dt;
                if (station.timer <= 0f) {
                    finishTreatment(i);
                }
            }
        }
        for (int i = 0; i < animals.length; i++) {
            Animal animal = animals[i];
            if (animal.active && !animal.inStation && !animal.recovered) {
                animal.wait += dt;
                animal.anim += dt;
                animal.stress = Math.min(100f, animal.stress + dt * 1.25f);
                if (animal.wait > 55f && animal.stress > 92f) {
                    stressedMisses++;
                    emitSound("stress_warning");
                    setupAnimal(i, day + i + random.nextInt(9), false);
                    toast = "A patient left stressed";
                    toastTimer = 2f;
                }
            }
        }
        if (trust >= targetTrust) {
            nextDay();
        } else if (shiftTime <= 0f || stressedMisses >= 4) {
            state = GameState.GAME_OVER;
            toast = "Shift ended";
            toastTimer = 4f;
            if (listener != null) {
                listener.onGameOver();
            }
        }
    }

    private void addArrival() {
        int slot = -1;
        for (int i = 0; i < animals.length; i++) {
            if (!animals[i].active) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            stressedMisses++;
            emitSound("stress_warning");
            toast = "Intake lane overflow";
            toastTimer = 2f;
            return;
        }
        setupAnimal(slot, day * 7 + random.nextInt(30), false);
        toast = "New patient arrived";
        toastTimer = 2f;
    }

    private void nextDay() {
        day++;
        coins += 24 + day * 4;
        trust = 0;
        targetTrust += 35;
        stressedMisses = 0;
        shiftTime = Math.max(175f, 245f - day * 8f);
        for (int i = 0; i < treatmentStock.length; i++) {
            treatmentStock[i] = Math.min(6, treatmentStock[i] + 2);
        }
        for (int i = 0; i < animals.length; i++) {
            setupAnimal(i, day * 10 + i, false);
        }
        for (int i = 0; i < stations.length; i++) {
            stations[i].animalIndex = -1;
            stations[i].timer = 0f;
        }
        toast = "New clinic shift";
        toastTimer = 3f;
        if (listener != null) {
            listener.onDayClear();
        }
    }

    private void setupAnimal(int index, int seed, boolean preview) {
        Animal animal = animals[index];
        String[] names = {"Cow", "Hen", "Sheep", "Goat", "Piglet", "Rabbit"};
        animal.name = names[Math.abs(seed) % names.length];
        animal.condition = Math.abs(seed + index) % 6;
        animal.active = preview || index < 4;
        animal.inStation = false;
        animal.recovered = false;
        animal.wait = 0f;
        animal.anim = (seed % 11) * 0.17f;
        animal.stress = 18f + (Math.abs(seed) % 25);
        animal.rating = 0;
    }

    private void assignAnimal(int animalIndex, int stationIndex) {
        Animal animal = animals[animalIndex];
        Station station = stations[stationIndex];
        if (!animal.active || animal.inStation || animal.recovered) {
            return;
        }
        if (station.animalIndex >= 0) {
            toast = "Station is busy";
            toastTimer = 2f;
            emitSound("bad_treatment");
            return;
        }
        int treatment = stationTreatment(station.kind);
        if (treatmentStock[treatment] <= 0) {
            toast = "Treatment card is out";
            toastTimer = 2f;
            emitSound("bad_treatment");
            return;
        }
        treatmentStock[treatment]--;
        emitSound("assign");
        station.animalIndex = animalIndex;
        station.timer = station.kind == STATION_RECOVERY ? 7f : 6f + station.kind;
        animal.inStation = true;
        boolean match = stationMatches(animal.condition, station.kind);
        animal.rating = match ? 2 : 1;
        animal.stress = Math.max(0f, animal.stress + (match ? -22f : 18f));
        toast = match ? "Good treatment match" : "Care helps but stress rises";
        toastTimer = 2f;
        emitSound(match ? "good_treatment" : "bad_treatment");
    }

    private void finishTreatment(int stationIndex) {
        Station station = stations[stationIndex];
        if (station.animalIndex < 0) {
            return;
        }
        Animal animal = animals[station.animalIndex];
        animal.inStation = false;
        if (station.kind == STATION_RECOVERY || animal.rating >= 2) {
            animal.recovered = true;
            animal.stress = Math.max(0f, animal.stress - 18f);
            toast = animal.name + " is ready to release";
        } else {
            animal.condition = CONDITION_STRESSED;
            toast = animal.name + " needs recovery";
        }
        toastTimer = 2f;
        station.animalIndex = -1;
        station.timer = 0f;
    }

    private void releaseStation(int stationIndex) {
        Station station = stations[stationIndex];
        if (station.animalIndex >= 0) {
            toast = "Treatment in progress";
            toastTimer = 2f;
            emitSound("ui_click");
            invalidate();
            return;
        }
        for (int i = 0; i < animals.length; i++) {
            Animal animal = animals[i];
            if (animal.active && animal.recovered && releaseGate.contains(animal.bounds.centerX(), animal.bounds.centerY())) {
                releaseAnimal(i);
                return;
            }
        }
        if (stationIndex == STATION_RECOVERY) {
            int index = nearestRecoveredCandidate();
            if (index >= 0) {
                releaseAnimal(index);
            }
        }
    }

    private int nearestRecoveredCandidate() {
        for (int i = 0; i < animals.length; i++) {
            if (animals[i].active && animals[i].recovered) {
                return i;
            }
        }
        return -1;
    }

    private void releaseAnimal(int index) {
        Animal animal = animals[index];
        int bonus = animal.stress < 30f ? 2 : 1;
        coins += 16 + bonus * 6;
        trust += 14 + bonus * 8;
        setupAnimal(index, day * 20 + index + random.nextInt(40), false);
        int restockIndex = random.nextInt(treatmentStock.length);
        treatmentStock[restockIndex] = Math.min(6, treatmentStock[restockIndex] + 1);
        toast = bonus > 1 ? "Excellent recovery" : "Patient released";
        toastTimer = 2f;
        emitSound("release");
        invalidate();
    }

    private void emitSound(String role) {
        if (listener != null) {
            listener.onSound(role);
        }
    }

    private int hitAnimal(float x, float y) {
        for (int i = 0; i < animals.length; i++) {
            if (animals[i].active && animals[i].bounds.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private int hitStation(float x, float y) {
        for (int i = 0; i < stationRects.length; i++) {
            if (stationRects[i] != null && stationRects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private int stationTreatment(int station) {
        if (station == STATION_TONIC) {
            return CONDITION_HUNGRY;
        }
        if (station == STATION_BRUSH) {
            return CONDITION_DIRTY;
        }
        if (station == STATION_BLANKET) {
            return CONDITION_CHILLED;
        }
        if (station == STATION_MEDICINE) {
            return CONDITION_SCRATCHED;
        }
        return CONDITION_STRESSED;
    }

    private boolean stationMatches(int condition, int station) {
        if (condition == CONDITION_HUNGRY && station == STATION_TONIC) {
            return true;
        }
        if (condition == CONDITION_DIRTY && station == STATION_BRUSH) {
            return true;
        }
        if (condition == CONDITION_CHILLED && station == STATION_BLANKET) {
            return true;
        }
        if (condition == CONDITION_SCRATCHED && station == STATION_MEDICINE) {
            return true;
        }
        return station == STATION_RECOVERY && (condition == CONDITION_TIRED || condition == CONDITION_STRESSED);
    }

    private String conditionName(int condition) {
        String[] labels = {"tonic", "brush", "blanket", "medicine", "rest", "comfort"};
        return labels[Math.max(0, Math.min(labels.length - 1, condition))];
    }

    private String stationName(int station) {
        String[] labels = {"Tonic", "Brush", "Blanket", "Medicine", "Recovery"};
        return labels[Math.max(0, Math.min(labels.length - 1, station))];
    }

    private void computeLayout() {
        float w = getWidth();
        float h = getHeight();
        tray.set(w * 0.12f, h * 0.78f, w * 0.8f, h);
        intake.set(0f, h * 0.13f, w * 0.17f, h * 0.78f);
        rack.set(w * 0.8f, h * 0.12f, w, h * 0.92f);
        board.set(w * 0.17f, h * 0.13f, w * 0.8f, h * 0.78f);
        releaseGate.set(board.right - board.width() * 0.18f, board.bottom - board.height() * 0.22f, board.right - board.width() * 0.02f, board.bottom - board.height() * 0.04f);
        float sw = board.width() * 0.18f;
        float sh = board.height() * 0.19f;
        for (int i = 0; i < stations.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float x = board.left + board.width() * 0.12f + col * (sw + board.width() * 0.07f);
            float y = board.top + board.height() * 0.24f + row * (sh + board.height() * 0.1f);
            stationRects[i] = new RectF(x, y, x + sw, y + sh);
        }
        float aw = intake.width() * 0.72f;
        float ah = board.height() * 0.11f;
        for (int i = 0; i < animals.length; i++) {
            Animal animal = animals[i];
            if (i == selectedAnimal && dragActive) {
                animal.bounds.set(dragX - aw * 0.5f, dragY - ah * 0.5f, dragX + aw * 0.5f, dragY + ah * 0.5f);
            } else if (animal.inStation) {
                int station = stationForAnimal(i);
                if (station >= 0) {
                    RectF r = stationRects[station];
                    animal.bounds.set(r.centerX() - aw * 0.45f, r.centerY() - ah * 0.42f, r.centerX() + aw * 0.45f, r.centerY() + ah * 0.42f);
                }
            } else if (animal.recovered) {
                int row = i % 2;
                animal.bounds.set(releaseGate.left + 8f, releaseGate.top + 12f + row * (ah + 8f), releaseGate.left + 8f + aw, releaseGate.top + 12f + row * (ah + 8f) + ah);
            } else {
                animal.bounds.set(intake.left + 16f, intake.top + 18f + i * (ah + 12f), intake.left + 16f + aw, intake.top + 18f + i * (ah + 12f) + ah);
            }
        }
    }

    private int stationForAnimal(int animalIndex) {
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].animalIndex == animalIndex) {
                return i;
            }
        }
        return -1;
    }

    private void drawBackground(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), Color.rgb(247, 253, 247), cBgMain, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
        paint.setColor(cGrass);
        canvas.drawRoundRect(board, 24f, 24f, paint);
        paint.setColor(cGrassDark);
        for (int i = 0; i < 18; i++) {
            float x = board.left + (i * 83f) % Math.max(1f, board.width());
            float y = board.top + (i * 41f) % Math.max(1f, board.height());
            canvas.drawOval(x, y, x + 26f, y + 9f, paint);
        }
        paint.setColor(cPath);
        RectF path = new RectF(intake.right - 18f, board.top + 22f, releaseGate.right, board.bottom - 30f);
        canvas.drawRoundRect(path, 28f, 28f, paint);
        paint.setColor(Color.argb(120, 255, 255, 255));
        for (int i = 0; i < 12; i++) {
            float x = board.left + 38f + i * board.width() / 12f;
            canvas.drawCircle(x, board.top + board.height() * 0.82f + (i % 2) * 8f, 5f, paint);
        }
        drawFence(canvas, board.left + 12f, board.bottom - 78f, board.width() * 0.35f);
    }

    private void drawClinic(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cCanvas);
        RectF awning = new RectF(board.left + 20f, board.top + 14f, board.right - 20f, board.top + 72f);
        canvas.drawRoundRect(awning, 18f, 18f, paint);
        paint.setColor(cAccent);
        for (int i = 0; i < 8; i++) {
            float x = awning.left + i * awning.width() / 8f;
            canvas.drawRect(x, awning.top, x + awning.width() / 16f, awning.bottom, paint);
        }
        drawText(canvas, "Pasture Clinic", awning.centerX(), awning.centerY() + 8f, 22f, cText, Paint.Align.CENTER);
        for (int i = 0; i < stations.length; i++) {
            drawStation(canvas, i, stationRects[i]);
        }
        paint.setColor(cSuccess);
        canvas.drawRoundRect(releaseGate, 22f, 22f, paint);
        paint.setColor(Color.argb(60, 255, 255, 255));
        canvas.drawCircle(releaseGate.centerX(), releaseGate.centerY() + 12f, releaseGate.width() * 0.34f, paint);
        drawText(canvas, "Release", releaseGate.centerX(), releaseGate.top + 28f, 16f, Color.WHITE, Paint.Align.CENTER);
        drawText(canvas, "Heart Gate", releaseGate.centerX(), releaseGate.bottom - 18f, 12f, Color.WHITE, Paint.Align.CENTER);
    }

    private void drawStation(Canvas canvas, int index, RectF r) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawRoundRect(r, 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(r, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(index == STATION_MEDICINE ? cAccent : index == STATION_RECOVERY ? cSuccess : cAccent2);
        canvas.drawRoundRect(r.left + 12f, r.top + 10f, r.right - 12f, r.top + 38f, 12f, 12f, paint);
        drawText(canvas, stationName(index), r.centerX(), r.top + 31f, 14f, Color.WHITE, Paint.Align.CENTER);
        drawStationIcon(canvas, index, r.centerX(), r.centerY() - 4f, Math.min(r.width(), r.height()) / 105f);
        Station station = stations[index];
        if (station.animalIndex >= 0) {
            drawProgressRing(canvas, r.right - 28f, r.bottom - 28f, 15f, station.timer / (station.kind == STATION_RECOVERY ? 7f : 6f + station.kind));
            drawText(canvas, String.valueOf(Math.max(0, (int) station.timer)) + "s", r.centerX(), r.bottom - 14f, 16f, cText, Paint.Align.CENTER);
        } else {
            drawText(canvas, "Open", r.centerX(), r.bottom - 14f, 15f, cMuted, Paint.Align.CENTER);
        }
    }

    private void drawAnimals(Canvas canvas) {
        for (int i = 0; i < animals.length; i++) {
            Animal animal = animals[i];
            if (!animal.active) {
                continue;
            }
            RectF r = animal.bounds;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 253, 244));
            canvas.drawRoundRect(r, 16f, 16f, paint);
            if (animal.recovered) {
                paint.setColor(Color.argb(90, 67, 182, 109));
                canvas.drawRoundRect(r.left + 4f, r.top + 4f, r.right - 4f, r.bottom - 4f, 14f, 14f, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(i == selectedAnimal ? 5f : 3f);
            paint.setColor(i == selectedAnimal ? cAccent : cStroke);
            canvas.drawRoundRect(r, 16f, 16f, paint);
            paint.setStyle(Paint.Style.FILL);
            drawAnimalShape(canvas, animal, r);
            drawText(canvas, animal.name, r.left + 12f, r.top + 22f, 14f, cText, Paint.Align.LEFT);
            drawText(canvas, animal.recovered ? "Ready" : conditionName(animal.condition), r.left + 12f, r.bottom - 8f, 12f, animal.recovered ? cSuccess : cMuted, Paint.Align.LEFT);
            drawConditionBadge(canvas, animal, r);
        }
    }

    private void drawAnimalShape(Canvas canvas, Animal animal, RectF r) {
        float cx = r.centerX() + r.width() * 0.1f;
        float cy = r.centerY();
        float s = Math.min(r.width(), r.height()) / 82f;
        float bob = (float) Math.sin(animal.anim * 3.2f) * 1.5f * s;
        cy += bob;
        if ("Hen".equals(animal.name)) {
            drawHen(canvas, cx, cy, s);
        } else if ("Rabbit".equals(animal.name)) {
            drawRabbit(canvas, cx, cy, s);
        } else if ("Piglet".equals(animal.name)) {
            drawPiglet(canvas, cx, cy, s);
        } else if ("Sheep".equals(animal.name)) {
            drawSheep(canvas, cx, cy, s);
        } else if ("Goat".equals(animal.name)) {
            drawGoat(canvas, cx, cy, s);
        } else {
            drawCow(canvas, cx, cy, s);
        }
    }

    private void drawCow(Canvas canvas, float cx, float cy, float s) {
        drawLegs(canvas, cx - 10f * s, cy + 10f * s, s, Color.rgb(238, 238, 230));
        paint.setColor(Color.rgb(250, 250, 242));
        canvas.drawOval(cx - 36f * s, cy - 18f * s, cx + 34f * s, cy + 19f * s, paint);
        paint.setColor(Color.rgb(36, 36, 34));
        canvas.drawOval(cx - 22f * s, cy - 12f * s, cx - 4f * s, cy + 8f * s, paint);
        canvas.drawOval(cx + 5f * s, cy - 16f * s, cx + 25f * s, cy + 6f * s, paint);
        paint.setColor(Color.rgb(250, 250, 242));
        canvas.drawOval(cx + 25f * s, cy - 18f * s, cx + 55f * s, cy + 8f * s, paint);
        paint.setColor(Color.rgb(246, 184, 170));
        canvas.drawOval(cx + 36f * s, cy - 4f * s, cx + 60f * s, cy + 13f * s, paint);
        paint.setColor(Color.rgb(80, 62, 45));
        canvas.drawLine(cx + 32f * s, cy - 18f * s, cx + 22f * s, cy - 33f * s, paint);
        canvas.drawLine(cx + 43f * s, cy - 16f * s, cx + 52f * s, cy - 31f * s, paint);
        drawEye(canvas, cx + 42f * s, cy - 9f * s, s);
        drawTail(canvas, cx - 37f * s, cy - 6f * s, s, Color.rgb(70, 55, 42));
    }

    private void drawHen(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(Color.rgb(244, 237, 208));
        canvas.drawOval(cx - 23f * s, cy - 15f * s, cx + 21f * s, cy + 20f * s, paint);
        paint.setColor(Color.rgb(236, 228, 198));
        canvas.drawOval(cx - 31f * s, cy - 7f * s, cx - 2f * s, cy + 17f * s, paint);
        paint.setColor(Color.rgb(238, 55, 66));
        canvas.drawCircle(cx + 13f * s, cy - 22f * s, 7f * s, paint);
        paint.setColor(Color.rgb(244, 237, 208));
        canvas.drawCircle(cx + 19f * s, cy - 9f * s, 13f * s, paint);
        paint.setColor(cWarning);
        shapePath.reset();
        shapePath.moveTo(cx + 29f * s, cy - 8f * s);
        shapePath.lineTo(cx + 45f * s, cy - 3f * s);
        shapePath.lineTo(cx + 29f * s, cy + 2f * s);
        shapePath.close();
        canvas.drawPath(shapePath, paint);
        drawEye(canvas, cx + 23f * s, cy - 11f * s, s);
        paint.setColor(Color.rgb(195, 120, 34));
        canvas.drawLine(cx - 4f * s, cy + 18f * s, cx - 8f * s, cy + 33f * s, paint);
        canvas.drawLine(cx + 8f * s, cy + 18f * s, cx + 4f * s, cy + 33f * s, paint);
    }

    private void drawRabbit(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(Color.rgb(224, 219, 204));
        canvas.drawOval(cx - 30f * s, cy - 14f * s, cx + 24f * s, cy + 21f * s, paint);
        canvas.drawOval(cx + 7f * s, cy - 51f * s, cx + 19f * s, cy - 10f * s, paint);
        canvas.drawOval(cx + 22f * s, cy - 49f * s, cx + 34f * s, cy - 8f * s, paint);
        paint.setColor(Color.rgb(238, 186, 196));
        canvas.drawOval(cx + 11f * s, cy - 45f * s, cx + 15f * s, cy - 16f * s, paint);
        canvas.drawOval(cx + 26f * s, cy - 43f * s, cx + 30f * s, cy - 15f * s, paint);
        paint.setColor(Color.rgb(224, 219, 204));
        canvas.drawCircle(cx + 25f * s, cy - 7f * s, 14f * s, paint);
        drawEye(canvas, cx + 31f * s, cy - 10f * s, s);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - 28f * s, cy + 2f * s, 9f * s, paint);
    }

    private void drawPiglet(Canvas canvas, float cx, float cy, float s) {
        drawLegs(canvas, cx - 6f * s, cy + 12f * s, s, Color.rgb(232, 134, 151));
        paint.setColor(Color.rgb(242, 157, 171));
        canvas.drawOval(cx - 33f * s, cy - 17f * s, cx + 31f * s, cy + 18f * s, paint);
        canvas.drawOval(cx + 21f * s, cy - 15f * s, cx + 49f * s, cy + 10f * s, paint);
        paint.setColor(Color.rgb(220, 100, 130));
        canvas.drawOval(cx + 38f * s, cy - 5f * s, cx + 57f * s, cy + 8f * s, paint);
        paint.setColor(Color.rgb(242, 157, 171));
        shapePath.reset();
        shapePath.moveTo(cx + 24f * s, cy - 14f * s);
        shapePath.lineTo(cx + 18f * s, cy - 29f * s);
        shapePath.lineTo(cx + 34f * s, cy - 18f * s);
        shapePath.close();
        canvas.drawPath(shapePath, paint);
        drawEye(canvas, cx + 36f * s, cy - 10f * s, s);
        paint.setColor(Color.rgb(160, 78, 96));
        canvas.drawCircle(cx + 45f * s, cy + 1f * s, 2f * s, paint);
        canvas.drawCircle(cx + 51f * s, cy + 1f * s, 2f * s, paint);
    }

    private void drawSheep(Canvas canvas, float cx, float cy, float s) {
        drawLegs(canvas, cx - 8f * s, cy + 13f * s, s, Color.rgb(55, 48, 42));
        paint.setColor(Color.rgb(247, 247, 235));
        for (int j = 0; j < 6; j++) {
            canvas.drawCircle(cx - 27f * s + j * 12f * s, cy - 1f * s + (j % 2) * 5f * s, 14f * s, paint);
        }
        paint.setColor(Color.rgb(72, 62, 54));
        canvas.drawOval(cx + 25f * s, cy - 13f * s, cx + 49f * s, cy + 11f * s, paint);
        paint.setColor(Color.rgb(247, 247, 235));
        canvas.drawCircle(cx + 25f * s, cy - 18f * s, 9f * s, paint);
        drawEye(canvas, cx + 39f * s, cy - 8f * s, s);
    }

    private void drawGoat(Canvas canvas, float cx, float cy, float s) {
        drawLegs(canvas, cx - 8f * s, cy + 13f * s, s, Color.rgb(166, 154, 126));
        paint.setColor(Color.rgb(218, 210, 186));
        canvas.drawOval(cx - 35f * s, cy - 14f * s, cx + 31f * s, cy + 17f * s, paint);
        canvas.drawOval(cx + 23f * s, cy - 25f * s, cx + 49f * s, cy + 6f * s, paint);
        paint.setColor(Color.rgb(90, 76, 55));
        paint.setStrokeWidth(3f * s);
        canvas.drawLine(cx + 30f * s, cy - 24f * s, cx + 21f * s, cy - 42f * s, paint);
        canvas.drawLine(cx + 40f * s, cy - 23f * s, cx + 48f * s, cy - 40f * s, paint);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.rgb(198, 188, 160));
        canvas.drawOval(cx + 18f * s, cy - 19f * s, cx + 28f * s, cy - 3f * s, paint);
        drawEye(canvas, cx + 39f * s, cy - 13f * s, s);
        paint.setColor(Color.rgb(90, 76, 55));
        canvas.drawLine(cx + 47f * s, cy + 2f * s, cx + 55f * s, cy + 13f * s, paint);
    }

    private void drawLegs(Canvas canvas, float cx, float cy, float s, int color) {
        paint.setColor(color);
        canvas.drawRoundRect(cx - 22f * s, cy, cx - 15f * s, cy + 20f * s, 3f * s, 3f * s, paint);
        canvas.drawRoundRect(cx - 4f * s, cy + 2f * s, cx + 3f * s, cy + 22f * s, 3f * s, 3f * s, paint);
        canvas.drawRoundRect(cx + 16f * s, cy, cx + 23f * s, cy + 20f * s, 3f * s, 3f * s, paint);
    }

    private void drawTail(Canvas canvas, float x, float y, float s, int color) {
        paint.setColor(color);
        paint.setStrokeWidth(3f * s);
        canvas.drawLine(x, y, x - 15f * s, y - 11f * s, paint);
        paint.setStrokeWidth(1f);
    }

    private void drawEye(Canvas canvas, float x, float y, float s) {
        paint.setColor(Color.rgb(30, 35, 32));
        canvas.drawCircle(x, y, 2.8f * s, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x + 0.8f * s, y - 0.8f * s, 0.9f * s, paint);
    }

    private void drawConditionBadge(Canvas canvas, Animal animal, RectF r) {
        int color = animal.stress > 70f ? cDanger : animal.recovered ? cSuccess : cWarning;
        paint.setColor(color);
        canvas.drawCircle(r.right - 19f, r.top + 20f, 12f, paint);
        drawText(canvas, conditionIcon(animal.condition), r.right - 19f, r.top + 25f, 12f, Color.WHITE, Paint.Align.CENTER);
    }

    private String conditionIcon(int condition) {
        String[] labels = {"F", "B", "W", "M", "R", "C"};
        return labels[Math.max(0, Math.min(labels.length - 1, condition))];
    }

    private void drawTray(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cCanvas);
        canvas.drawRoundRect(tray, 24f, 24f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(cWood);
        canvas.drawRoundRect(tray, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);
        drawText(canvas, "Treatment Cards", tray.left + 20f, tray.top + 28f, 18f, cText, Paint.Align.LEFT);
        float cardW = tray.width() / 6.4f;
        for (int i = 0; i < 6; i++) {
            float x = tray.left + 18f + i * (cardW + 8f);
            float y = tray.top + 42f;
            cardRects[i] = new RectF(x, y, x + cardW, tray.bottom - 12f);
            paint.setColor(i == CONDITION_SCRATCHED ? cAccent : i == CONDITION_STRESSED ? cSuccess : cAccent2);
            canvas.drawRoundRect(cardRects[i], 16f, 16f, paint);
            drawCardIcon(canvas, i, cardRects[i].centerX(), y + 31f, Math.min(cardW, cardRects[i].height()) / 82f);
            drawText(canvas, conditionName(i), cardRects[i].centerX(), y + 56f, 11f, Color.WHITE, Paint.Align.CENTER);
            drawText(canvas, "x" + treatmentStock[i], cardRects[i].centerX(), cardRects[i].bottom - 14f, 15f, Color.WHITE, Paint.Align.CENTER);
        }
    }

    private void drawRack(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        canvas.drawRect(rack, paint);
        drawText(canvas, "Stations", rack.centerX(), rack.top + 36f, 20f, cText, Paint.Align.CENTER);
        drawText(canvas, "Tap a station to check progress", rack.centerX(), rack.top + 62f, 11f, cMuted, Paint.Align.CENTER);
        for (int i = 0; i < stations.length; i++) {
            Station station = stations[i];
            float y = rack.top + 88f + i * 42f;
            paint.setColor(station.animalIndex >= 0 ? cWarning : cSuccess);
            canvas.drawCircle(rack.left + 24f, y, 10f, paint);
            drawText(canvas, stationName(i), rack.left + 42f, y + 5f, 14f, cText, Paint.Align.LEFT);
        }
    }

    private void drawStationIcon(Canvas canvas, int index, float cx, float cy, float s) {
        paint.setColor(Color.argb(235, 255, 255, 255));
        canvas.drawCircle(cx, cy, 22f * s, paint);
        paint.setColor(index == STATION_MEDICINE ? cAccent : index == STATION_RECOVERY ? cSuccess : cAccent2);
        if (index == STATION_TONIC) {
            canvas.drawRoundRect(cx - 8f * s, cy - 12f * s, cx + 8f * s, cy + 12f * s, 4f * s, 4f * s, paint);
            paint.setColor(Color.WHITE);
            canvas.drawRect(cx - 5f * s, cy - 1f * s, cx + 5f * s, cy + 2f * s, paint);
        } else if (index == STATION_BRUSH) {
            canvas.drawRoundRect(cx - 15f * s, cy - 4f * s, cx + 8f * s, cy + 5f * s, 5f * s, 5f * s, paint);
            paint.setStrokeWidth(2f * s);
            for (int i = 0; i < 5; i++) {
                canvas.drawLine(cx - 8f * s + i * 4f * s, cy + 5f * s, cx - 8f * s + i * 4f * s, cy + 13f * s, paint);
            }
            paint.setStrokeWidth(1f);
        } else if (index == STATION_BLANKET) {
            canvas.drawRoundRect(cx - 16f * s, cy - 10f * s, cx + 16f * s, cy + 10f * s, 5f * s, 5f * s, paint);
            paint.setColor(Color.WHITE);
            canvas.drawLine(cx - 8f * s, cy - 10f * s, cx - 8f * s, cy + 10f * s, paint);
        } else if (index == STATION_MEDICINE) {
            canvas.drawRect(cx - 4f * s, cy - 15f * s, cx + 4f * s, cy + 15f * s, paint);
            canvas.drawRect(cx - 15f * s, cy - 4f * s, cx + 15f * s, cy + 4f * s, paint);
        } else {
            shapePath.reset();
            shapePath.moveTo(cx, cy + 15f * s);
            shapePath.cubicTo(cx - 24f * s, cy, cx - 8f * s, cy - 20f * s, cx, cy - 6f * s);
            shapePath.cubicTo(cx + 8f * s, cy - 20f * s, cx + 24f * s, cy, cx, cy + 15f * s);
            canvas.drawPath(shapePath, paint);
        }
    }

    private void drawCardIcon(Canvas canvas, int index, float cx, float cy, float s) {
        paint.setColor(Color.argb(245, 255, 255, 255));
        if (index == CONDITION_HUNGRY) {
            canvas.drawOval(cx - 12f * s, cy - 9f * s, cx + 12f * s, cy + 9f * s, paint);
        } else if (index == CONDITION_DIRTY) {
            for (int i = 0; i < 4; i++) {
                canvas.drawCircle(cx - 10f * s + i * 7f * s, cy - 3f * s + (i % 2) * 7f * s, 4f * s, paint);
            }
        } else if (index == CONDITION_CHILLED) {
            canvas.drawRoundRect(cx - 14f * s, cy - 10f * s, cx + 14f * s, cy + 10f * s, 5f * s, 5f * s, paint);
        } else if (index == CONDITION_SCRATCHED) {
            canvas.drawRect(cx - 3f * s, cy - 14f * s, cx + 3f * s, cy + 14f * s, paint);
            canvas.drawRect(cx - 14f * s, cy - 3f * s, cx + 14f * s, cy + 3f * s, paint);
        } else if (index == CONDITION_TIRED) {
            canvas.drawCircle(cx - 6f * s, cy, 8f * s, paint);
            canvas.drawCircle(cx + 8f * s, cy, 8f * s, paint);
        } else {
            shapePath.reset();
            shapePath.moveTo(cx, cy + 13f * s);
            shapePath.cubicTo(cx - 20f * s, cy, cx - 7f * s, cy - 18f * s, cx, cy - 5f * s);
            shapePath.cubicTo(cx + 7f * s, cy - 18f * s, cx + 20f * s, cy, cx, cy + 13f * s);
            canvas.drawPath(shapePath, paint);
        }
    }

    private void drawProgressRing(Canvas canvas, float cx, float cy, float radius, float fill) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(cStroke);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(cAccent);
        scratch.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(scratch, -90f, Math.max(0.05f, Math.min(1f, fill)) * 360f, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFence(Canvas canvas, float left, float top, float width) {
        paint.setColor(cWood);
        paint.setStrokeWidth(5f);
        canvas.drawLine(left, top + 14f, left + width, top + 14f, paint);
        canvas.drawLine(left, top + 33f, left + width, top + 33f, paint);
        for (int i = 0; i < 8; i++) {
            float x = left + i * width / 7f;
            canvas.drawLine(x, top, x, top + 48f, paint);
        }
        paint.setStrokeWidth(1f);
    }

    private void drawHud(Canvas canvas) {
        float h = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cPanel);
        RectF time = new RectF(18f, 12f, 130f, 52f);
        canvas.drawRoundRect(time, 18f, 18f, paint);
        drawText(canvas, "Time " + Math.max(0, (int) shiftTime), time.centerX(), time.centerY() + 6f, 15f, cText, Paint.Align.CENTER);
        RectF trustMeter = new RectF(getWidth() * 0.34f, 12f, getWidth() * 0.66f, 52f);
        paint.setColor(cStroke);
        canvas.drawRoundRect(trustMeter, 20f, 20f, paint);
        float fill = Math.min(1f, targetTrust <= 0 ? 0f : trust / (float) targetTrust);
        paint.setColor(cSuccess);
        canvas.drawRoundRect(trustMeter.left, trustMeter.top, trustMeter.left + trustMeter.width() * fill, trustMeter.bottom, 20f, 20f, paint);
        drawText(canvas, "Trust " + trust + "/" + targetTrust, trustMeter.centerX(), trustMeter.centerY() + 6f, 16f, Color.WHITE, Paint.Align.CENTER);
        RectF coinsBox = new RectF(getWidth() - 252f, 12f, getWidth() - 88f, 52f);
        paint.setColor(cPanel);
        canvas.drawRoundRect(coinsBox, 18f, 18f, paint);
        drawText(canvas, "Coins " + coins, coinsBox.centerX(), coinsBox.centerY() + 6f, 15f, cText, Paint.Align.CENTER);
        RectF stressBox = new RectF(getWidth() - 430f, 12f, getWidth() - 268f, 52f);
        paint.setColor(cPanel);
        canvas.drawRoundRect(stressBox, 18f, 18f, paint);
        drawText(canvas, "Miss " + stressedMisses + "/4", stressBox.centerX(), stressBox.centerY() + 6f, 15f, cText, Paint.Align.CENTER);
        drawText(canvas, "Day " + day, getWidth() * 0.18f, h - 12f, 15f, cMuted, Paint.Align.LEFT);
    }

    private void drawToast(Canvas canvas) {
        if (toastTimer <= 0f || toast == null) {
            return;
        }
        float w = Math.min(getWidth() * 0.58f, toast.length() * 10f + 52f);
        RectF r = new RectF(board.centerX() - w * 0.5f, board.bottom - 52f, board.centerX() + w * 0.5f, board.bottom - 12f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225, 33, 55, 44));
        canvas.drawRoundRect(r, 18f, 18f, paint);
        drawText(canvas, toast, r.centerX(), r.centerY() + 6f, 15f, Color.WHITE, Paint.Align.CENTER);
    }

    private void drawResult(Canvas canvas) {
        RectF r = new RectF(board.left + board.width() * 0.22f, board.top + board.height() * 0.24f, board.right - board.width() * 0.22f, board.bottom - board.height() * 0.24f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(240, 255, 253, 245));
        canvas.drawRoundRect(r, 28f, 28f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(cWood);
        canvas.drawRoundRect(r, 28f, 28f, paint);
        drawText(canvas, "Shift Result", r.centerX(), r.top + 58f, 28f, cText, Paint.Align.CENTER);
        drawText(canvas, "Restart to improve patient care", r.centerX(), r.top + 96f, 18f, cMuted, Paint.Align.CENTER);
    }

    private void drawText(Canvas canvas, String value, float x, float y, float size, int color, Paint.Align align) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        canvas.drawText(value, x, y, textPaint);
    }

    private static class Animal {
        String name;
        int condition;
        boolean active;
        boolean inStation;
        boolean recovered;
        int rating;
        float stress;
        float wait;
        float anim;
        final RectF bounds = new RectF();
    }

    private static class Station {
        int kind;
        int animalIndex;
        float timer;
    }
}
