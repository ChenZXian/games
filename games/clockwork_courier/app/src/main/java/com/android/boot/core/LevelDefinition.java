package com.android.boot.core;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public class LevelDefinition {
    public static class HazardDef {
        public final String type;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public final float p1;
        public final float p2;

        public HazardDef(String type, float x, float y, float w, float h, float p1, float p2) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    public final int id;
    public final String name;
    public final float stageW;
    public final float stageH;
    public final PointF start;
    public final PointF exit;
    public final float timerLimit;
    public final boolean keyRequired;
    public final List<PointF> badgePositions = new ArrayList<>();
    public final List<PointF> checkpoints = new ArrayList<>();
    public final List<HazardDef> hazards = new ArrayList<>();
    public PointF keyPosition;

    public LevelDefinition(int id, String name, float stageW, float stageH, PointF start, PointF exit, float timerLimit, boolean keyRequired) {
        this.id = id;
        this.name = name;
        this.stageW = stageW;
        this.stageH = stageH;
        this.start = start;
        this.exit = exit;
        this.timerLimit = timerLimit;
        this.keyRequired = keyRequired;
    }
}
