package com.android.boot.stage;

import java.util.ArrayList;
import java.util.List;

public class StageManager {
    private final List<StageDefinition> stages = new ArrayList<>();

    public StageManager() {
        stages.add(new StageDefinition(1, "ruined_gate", "Ruined Gate", "Gate Warden", 18));
        stages.add(new StageDefinition(2, "thorn_marsh", "Thorn Marsh", "Mirefang Alpha", 30));
        stages.add(new StageDefinition(3, "ash_foundry", "Ash Foundry", "Forge Tyrant", 45));
        stages.add(new StageDefinition(4, "dusk_barracks", "Dusk Barracks", "Crimson Duelist", 65));
        stages.add(new StageDefinition(5, "storm_watch", "Storm Watch", "Tempest Core", 88));
        stages.add(new StageDefinition(6, "relic_abyss", "Relic Abyss", "Abyss Sovereign", 115));
    }

    public List<StageDefinition> getStages() {
        return stages;
    }

    public StageDefinition byIndex(int index) {
        return stages.get(index - 1);
    }
}
