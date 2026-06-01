package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;

public class CampaignStage {
    public final int hallIndex;
    public final int stageIndex;
    public final String hallName;
    public final String displayName;
    public final int aiLevel;
    public final String modifier;
    public final long seed;
    public final boolean drill;

    public CampaignStage(int hallIndex, int stageIndex, String hallName, String displayName, int aiLevel, String modifier, long seed, boolean drill) {
        this.hallIndex = hallIndex;
        this.stageIndex = stageIndex;
        this.hallName = hallName;
        this.displayName = displayName;
        this.aiLevel = aiLevel;
        this.modifier = modifier;
        this.seed = seed;
        this.drill = drill;
    }

    public static List<CampaignStage> createCampaign() {
        List<CampaignStage> stages = new ArrayList<>();
        String[] halls = {"Bronze Hall", "Silver Hall", "Gold Hall", "Jade Hall", "Crown Hall"};
        String[] modifiers = {"Balanced Table", "Aggressive Calls", "Bomb Pressure", "Fast Finish", "Control Table", "Boss Table"};
        long seed = 1200L;
        for (int hall = 0; hall < halls.length; hall++) {
            for (int stage = 0; stage < 12; stage++) {
                int aiLevel = hall * 2 + stage / 3;
                String label = "Table " + (stage + 1);
                String modifier = modifiers[Math.min(modifiers.length - 1, stage / 2)];
                stages.add(new CampaignStage(hall, stage, halls[hall], label, aiLevel, modifier, seed++, false));
            }
        }
        return stages;
    }

    public static List<CampaignStage> createDrills() {
        List<CampaignStage> drills = new ArrayList<>();
        drills.add(new CampaignStage(0, 0, "Drill Room", "Drill 1", 3, "Tight Race", 8801L, true));
        drills.add(new CampaignStage(0, 1, "Drill Room", "Drill 2", 5, "Bomb Pressure", 8802L, true));
        drills.add(new CampaignStage(0, 2, "Drill Room", "Drill 3", 7, "Boss Drill", 8803L, true));
        return drills;
    }
}
