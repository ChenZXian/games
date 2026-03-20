package com.android.boot.core;

import com.android.boot.entity.EnemyType;

public final class LevelRepository {
    private LevelRepository() {
    }

    public static LevelDefinition[] createLevels() {
        return new LevelDefinition[] {
                new LevelDefinition(
                        "First Roll",
                        5,
                        1.25f,
                        900,
                        new LevelDefinition.Wave[] {
                                new LevelDefinition.Wave(0.95f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 0),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 3)
                                }),
                                new LevelDefinition.Wave(0.9f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.LIGHT, 0),
                                        new LevelDefinition.Spawn(4, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 3),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 4)
                                })
                        }),
                new LevelDefinition(
                        "Packed Lanes",
                        5,
                        1.15f,
                        1400,
                        new LevelDefinition.Wave[] {
                                new LevelDefinition.Wave(0.8f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 0),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 2),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 3)
                                }),
                                new LevelDefinition.Wave(0.75f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.MEDIUM, 0),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 0),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(4, EnemyType.MEDIUM, 2)
                                })
                        }),
                new LevelDefinition(
                        "Heavy Line",
                        4,
                        1.05f,
                        1900,
                        new LevelDefinition.Wave[] {
                                new LevelDefinition.Wave(0.8f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(2, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 4),
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.MEDIUM, 3)
                                }),
                                new LevelDefinition.Wave(0.7f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(4, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 2),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 2)
                                })
                        }),
                new LevelDefinition(
                        "Mixed Chaos",
                        4,
                        0.98f,
                        2500,
                        new LevelDefinition.Wave[] {
                                new LevelDefinition.Wave(0.72f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.LIGHT, 0),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 0),
                                        new LevelDefinition.Spawn(4, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(1, EnemyType.HEAVY, 2),
                                        new LevelDefinition.Spawn(3, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 3)
                                }),
                                new LevelDefinition.Wave(0.68f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 0),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.HEAVY, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(4, EnemyType.LIGHT, 3),
                                        new LevelDefinition.Spawn(0, EnemyType.MEDIUM, 4)
                                }),
                                new LevelDefinition.Wave(0.64f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(4, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(3, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(0, EnemyType.HEAVY, 3)
                                })
                        }),
                new LevelDefinition(
                        "Final Conveyor",
                        4,
                        0.9f,
                        3400,
                        new LevelDefinition.Wave[] {
                                new LevelDefinition.Wave(0.62f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.MEDIUM, 0),
                                        new LevelDefinition.Spawn(2, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(4, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(1, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.LIGHT, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 3)
                                }),
                                new LevelDefinition.Wave(0.58f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(1, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(3, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(0, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(4, EnemyType.LIGHT, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 3)
                                }),
                                new LevelDefinition.Wave(0.54f, new LevelDefinition.Spawn[] {
                                        new LevelDefinition.Spawn(0, EnemyType.HEAVY, 0),
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 1),
                                        new LevelDefinition.Spawn(2, EnemyType.HEAVY, 1),
                                        new LevelDefinition.Spawn(3, EnemyType.MEDIUM, 2),
                                        new LevelDefinition.Spawn(4, EnemyType.HEAVY, 2),
                                        new LevelDefinition.Spawn(2, EnemyType.LIGHT, 3),
                                        new LevelDefinition.Spawn(1, EnemyType.MEDIUM, 4)
                                })
                        })
        };
    }
}
