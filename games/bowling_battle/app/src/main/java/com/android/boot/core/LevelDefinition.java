package com.android.boot.core;

import com.android.boot.entity.EnemyType;

public class LevelDefinition {
    public final String name;
    public final int startLife;
    public final float conveyorCooldown;
    public final Wave[] waves;
    public final int targetScore;

    public LevelDefinition(String name, int startLife, float conveyorCooldown, int targetScore, Wave[] waves) {
        this.name = name;
        this.startLife = startLife;
        this.conveyorCooldown = conveyorCooldown;
        this.targetScore = targetScore;
        this.waves = waves;
    }

    public static class Wave {
        public final float spawnInterval;
        public final Spawn[] spawns;

        public Wave(float spawnInterval, Spawn[] spawns) {
            this.spawnInterval = spawnInterval;
            this.spawns = spawns;
        }
    }

    public static class Spawn {
        public final int row;
        public final EnemyType type;
        public final int delaySteps;

        public Spawn(int row, EnemyType type, int delaySteps) {
            this.row = row;
            this.type = type;
            this.delaySteps = delaySteps;
        }
    }
}
