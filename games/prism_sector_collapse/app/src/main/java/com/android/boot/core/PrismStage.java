package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;

public class PrismStage {
    public enum ObjectiveType {
        SCORE,
        RELAY,
        ARMOR,
        CHARGE
    }

    public final int sectorIndex;
    public final String code;
    public final int cols;
    public final int rows;
    public final int moves;
    public final ObjectiveType objectiveType;
    public final int targetCount;
    public final int armorCount;
    public final int relayCount;
    public final boolean splitBoard;
    public final boolean heavyArmor;
    public final long seed;

    public PrismStage(int sectorIndex, String code, int cols, int rows, int moves, ObjectiveType objectiveType, int targetCount, int armorCount, int relayCount, boolean splitBoard, boolean heavyArmor, long seed) {
        this.sectorIndex = sectorIndex;
        this.code = code;
        this.cols = cols;
        this.rows = rows;
        this.moves = moves;
        this.objectiveType = objectiveType;
        this.targetCount = targetCount;
        this.armorCount = armorCount;
        this.relayCount = relayCount;
        this.splitBoard = splitBoard;
        this.heavyArmor = heavyArmor;
        this.seed = seed;
    }

    public static List<PrismStage> createCampaign() {
        List<PrismStage> stages = new ArrayList<>();
        stages.add(new PrismStage(1, "1-1", 10, 14, 22, ObjectiveType.SCORE, 7200, 8, 2, false, false, 101));
        stages.add(new PrismStage(2, "2-1", 10, 14, 24, ObjectiveType.RELAY, 5, 10, 5, false, false, 202));
        stages.add(new PrismStage(3, "3-1", 11, 14, 25, ObjectiveType.ARMOR, 16, 16, 4, false, true, 303));
        stages.add(new PrismStage(4, "4-1", 11, 15, 26, ObjectiveType.RELAY, 7, 14, 7, true, true, 404));
        stages.add(new PrismStage(5, "5-1", 12, 15, 27, ObjectiveType.CHARGE, 100, 18, 6, true, true, 505));
        stages.add(new PrismStage(6, "6-1", 12, 16, 28, ObjectiveType.SCORE, 14500, 22, 8, true, true, 606));
        return stages;
    }
}
