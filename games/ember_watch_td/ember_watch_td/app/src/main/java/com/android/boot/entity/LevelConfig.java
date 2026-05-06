package com.android.boot.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelConfig {
    public String id;
    public String name;
    public int startGold;
    public int startLife;
    public final List<PointF2> path = new ArrayList<>();
    public final List<TowerSlot> slots = new ArrayList<>();
    public final List<Wave> waves = new ArrayList<>();
    public final Map<String, EnemyTemplate> enemies = new HashMap<>();
}
