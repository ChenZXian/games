package com.android.boot.entity;

public class HarborNode {
    public static final int OWNER_NEUTRAL = 0;
    public static final int OWNER_PLAYER = 1;
    public static final int OWNER_ENEMY = 2;

    public final int id;
    public final String name;
    public final float x;
    public final float y;
    public final boolean flagship;
    public final float baseDefense;
    public final float production;
    public int owner;
    public float defense;
    public float stock;
    public int routeTargetId = -1;
    public float convoyCooldown;
    public int dockLevel;
    public int armorLevel;
    public int cannonLevel;
    public float cannonBoostTimer;
    public int[] links = new int[0];

    public HarborNode(int id, String name, float x, float y, int owner, float baseDefense, float production, boolean flagship) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.baseDefense = baseDefense;
        this.production = production;
        this.flagship = flagship;
        this.defense = effectiveDefenseCap();
    }

    public void setLinks(int[] links) {
        this.links = links != null ? links : new int[0];
    }

    public boolean isLinkedTo(int otherId) {
        for (int linkedId : links) {
            if (linkedId == otherId) {
                return true;
            }
        }
        return false;
    }

    public float maxStock() {
        return 72f + dockLevel * 18f + (flagship ? 18f : 0f);
    }

    public float launchCooldown() {
        return Math.max(1.15f, 3.0f - dockLevel * 0.45f);
    }

    public float convoyHp() {
        return 20f + armorLevel * 8f + (flagship ? 6f : 0f);
    }

    public float interceptionDamage() {
        return 5f + cannonLevel * 5f + (cannonBoostTimer > 0f ? 8f : 0f);
    }

    public int totalUpgradeLevel() {
        return dockLevel + armorLevel + cannonLevel;
    }

    public float effectiveDefenseCap() {
        return baseDefense + armorLevel * 18f + (flagship ? 18f : 0f);
    }

    public void refreshDefenseAfterUpgrade() {
        defense = Math.min(effectiveDefenseCap(), defense + 16f);
        stock = Math.min(maxStock(), stock + 6f);
    }

    public void capture(int newOwner) {
        owner = newOwner;
        routeTargetId = -1;
        dockLevel = 0;
        armorLevel = 0;
        cannonLevel = 0;
        cannonBoostTimer = 0f;
        convoyCooldown = 0f;
        defense = effectiveDefenseCap() * 0.68f;
    }
}
