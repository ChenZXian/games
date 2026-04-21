package com.android.boot.core;

import android.content.Context;
import android.graphics.PointF;

import com.android.boot.R;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    private final List<LevelDefinition> levels = new ArrayList<>();

    public LevelManager(Context context) {
        build(context);
    }

    private void build(Context c) {
        LevelDefinition l1 = new LevelDefinition(1, c.getString(R.string.level_1), 100, 160, new PointF(12, 140), new PointF(88, 20), 80, false);
        l1.badgePositions.add(new PointF(55, 90));
        l1.hazards.add(new LevelDefinition.HazardDef("laser", 40, 120, 20, 4, 2.4f, 0));
        l1.hazards.add(new LevelDefinition.HazardDef("patrol", 20, 70, 8, 8, 28, 1.6f));
        levels.add(l1);

        LevelDefinition l2 = new LevelDefinition(2, c.getString(R.string.level_2), 100, 160, new PointF(10, 145), new PointF(90, 15), 75, false);
        l2.badgePositions.add(new PointF(48, 120));
        l2.badgePositions.add(new PointF(72, 65));
        l2.hazards.add(new LevelDefinition.HazardDef("electric", 25, 110, 50, 10, 1.6f, 0.8f));
        l2.hazards.add(new LevelDefinition.HazardDef("electric", 35, 75, 40, 10, 1.4f, 0.6f));
        levels.add(l2);

        LevelDefinition l3 = new LevelDefinition(3, c.getString(R.string.level_3), 100, 160, new PointF(10, 145), new PointF(92, 18), 72, false);
        l3.badgePositions.add(new PointF(22, 95));
        l3.badgePositions.add(new PointF(55, 75));
        l3.badgePositions.add(new PointF(80, 55));
        l3.checkpoints.add(new PointF(50, 100));
        l3.hazards.add(new LevelDefinition.HazardDef("gear", 30, 110, 12, 12, 65, 0.9f));
        l3.hazards.add(new LevelDefinition.HazardDef("gear", 62, 85, 12, 12, 62, 1.1f));
        levels.add(l3);

        LevelDefinition l4 = new LevelDefinition(4, c.getString(R.string.level_4), 100, 160, new PointF(10, 145), new PointF(88, 12), 68, false);
        l4.badgePositions.add(new PointF(32, 88));
        l4.badgePositions.add(new PointF(72, 70));
        l4.checkpoints.add(new PointF(53, 96));
        l4.hazards.add(new LevelDefinition.HazardDef("collapse", 36, 108, 28, 7, 2f, 0.9f));
        l4.hazards.add(new LevelDefinition.HazardDef("laser", 57, 58, 26, 4, 1.8f, 0.2f));
        levels.add(l4);

        LevelDefinition l5 = new LevelDefinition(5, c.getString(R.string.level_5), 100, 160, new PointF(12, 145), new PointF(90, 12), 65, true);
        l5.badgePositions.add(new PointF(22, 65));
        l5.badgePositions.add(new PointF(50, 52));
        l5.badgePositions.add(new PointF(76, 86));
        l5.checkpoints.add(new PointF(48, 118));
        l5.keyPosition = new PointF(80, 42);
        l5.hazards.add(new LevelDefinition.HazardDef("patrol", 36, 104, 8, 8, 30, 2f));
        l5.hazards.add(new LevelDefinition.HazardDef("patrol", 52, 76, 8, 8, 24, 1.5f));
        levels.add(l5);

        LevelDefinition l6 = new LevelDefinition(6, c.getString(R.string.level_6), 100, 160, new PointF(8, 145), new PointF(94, 8), 60, true);
        l6.badgePositions.add(new PointF(16, 124));
        l6.badgePositions.add(new PointF(50, 84));
        l6.badgePositions.add(new PointF(84, 46));
        l6.checkpoints.add(new PointF(50, 110));
        l6.keyPosition = new PointF(18, 40);
        l6.hazards.add(new LevelDefinition.HazardDef("laser", 25, 130, 50, 4, 2f, 0));
        l6.hazards.add(new LevelDefinition.HazardDef("electric", 30, 95, 40, 10, 1.2f, 0.8f));
        l6.hazards.add(new LevelDefinition.HazardDef("gear", 60, 70, 12, 12, 56, 1.3f));
        l6.hazards.add(new LevelDefinition.HazardDef("patrol", 44, 38, 8, 8, 35, 2.2f));
        l6.hazards.add(new LevelDefinition.HazardDef("collapse", 64, 24, 20, 8, 1.8f, 0.7f));
        l6.hazards.add(new LevelDefinition.HazardDef("shield", 70, 125, 14, 14, 0, 0));
        levels.add(l6);
    }

    public LevelDefinition get(int index) {
        return levels.get(Math.max(0, Math.min(index, levels.size() - 1)));
    }

    public int size() {
        return levels.size();
    }
}
