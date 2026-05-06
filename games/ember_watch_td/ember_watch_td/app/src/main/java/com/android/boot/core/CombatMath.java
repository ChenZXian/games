package com.android.boot.core;

import com.android.boot.entity.DamageType;
import com.android.boot.entity.Enemy;

public class CombatMath {
    public static float apply(Enemy enemy, float rawDamage, DamageType damageType) {
        float reduced = rawDamage;
        if (damageType == DamageType.PHYSICAL || damageType == DamageType.EXPLOSIVE || damageType == DamageType.MELEE) {
            reduced *= 100f / (100f + Math.max(0f, enemy.template.armor));
        }
        if (damageType == DamageType.MAGIC) {
            reduced *= 100f / (100f + Math.max(0f, enemy.template.resist));
        }
        return Math.max(1f, reduced);
    }
}
