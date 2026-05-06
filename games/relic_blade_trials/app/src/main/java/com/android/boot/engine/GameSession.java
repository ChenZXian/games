package com.android.boot.engine;

import com.android.boot.input.InputController;
import com.android.boot.inventory.EquipmentManager;
import com.android.boot.inventory.InventoryManager;
import com.android.boot.loot.ItemDatabase;
import com.android.boot.loot.LootManager;
import com.android.boot.model.BossEnemy;
import com.android.boot.model.Enums.SlotType;
import com.android.boot.model.Enemy;
import com.android.boot.model.ItemDefinition;
import com.android.boot.model.Player;
import com.android.boot.fx.DamageText;
import com.android.boot.fx.HitEffect;
import com.android.boot.stage.StageManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameSession {
    public GameState state = GameState.MENU;
    public final Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<>();
    public BossEnemy boss;
    public int coins;
    public int stageIndex = 1;
    public String resultBoss = "";
    public ItemDefinition resultLoot;
    public final InventoryManager inventory = new InventoryManager();
    public final EquipmentManager equipment = new EquipmentManager();
    public final StageManager stageManager = new StageManager();
    public final ItemDatabase itemDatabase = new ItemDatabase();
    public final LootManager lootManager = new LootManager(itemDatabase);
    private float spawnTimer;
    private final Random random = new Random();
    public int waveCount;
    public int killCount;
    public float relicCharge;
    public final List<HitEffect> hitEffects = new ArrayList<>();
    public final List<DamageText> damageTexts = new ArrayList<>();
    public final List<GroundLoot> groundLoot = new ArrayList<>();
    public boolean portalActive;
    public float portalX = 1060f;
    public float portalY = 420f;
    // Panels start hidden; open via bottom buttons / modal.
    public boolean showInventoryPanel = false;
    public boolean showStatusPanel = false;
    public boolean showEquipmentPanel = false;
    public int inventoryPage;
    public boolean modalOpen;
    public boolean modalBag;
    public boolean modalEquip;
    public int modalPage;
    public int modalDragIndex = -1;
    public float modalDragX;
    public float modalDragY;
    public boolean noMoreSpawnsUntilStage1;
    public ItemDefinition inspectItem;
    public float inspectX;
    public float inspectY;
    public float inspectTimer;
    public float attackFxTimer;
    public float attackFxX;
    public float attackFxY;
    public float attackFxFacing = 1f;
    public float dragonFxTimer;
    public float dragonFxX;
    public float dragonFxY;
    public float dragonFxFacing = 1f;
    public float dragonSkillCooldown;
    private boolean returnToMainMenuRequested;
    private int stageKills;

    public GameSession() {
        for (ItemDefinition item : itemDatabase.all()) {
            if (item.id.equals("wooden_sword")) {
                equipment.equip(item);
            }
        }
        applyEquipmentStats();
    }

    public void update(float dt, InputController input) {
        if (inspectTimer > 0f) {
            inspectTimer -= dt;
            if (inspectTimer <= 0f) {
                inspectTimer = 0f;
                inspectItem = null;
            }
        }
        if (modalOpen) {
            state = GameState.PAUSED;
        }
        if (input.consumePauseTap()) {
            if (state == GameState.PLAYING) state = GameState.PAUSED;
            else if (state == GameState.PAUSED) state = GameState.PLAYING;
        }
        if (state == GameState.PLAYING) {
            stepPlaying(dt, input);
        }
    }

    private void stepPlaying(float dt, InputController input) {
        player.velocityX = 0f;
        if (input.left) player.velocityX = -player.moveSpeed;
        if (input.right) player.velocityX = player.moveSpeed;
        if (input.jump && player.y >= 420f) player.velocityY = -360f;
        if (dragonSkillCooldown > 0f) dragonSkillCooldown -= dt;
        if (input.consumeSkillTap() && dragonSkillCooldown <= 0f) {
            castDragonSkill();
        }
        if (input.attack && player.attackCooldown <= 0f) {
            player.attackCooldown = 0.22f;
            player.comboIndex = (player.comboIndex % 3) + 1;
            player.comboTimer = 0.22f;
            attackFxTimer = 0.18f;
            attackFxFacing = player.facing;
            attackFxX = player.x + player.facing * 42f;
            attackFxY = player.y - 48f;
            for (Enemy e : enemies) {
                if (Math.abs(e.x - player.x) < 120f) {
                    float damage = player.attack * (1f + player.comboIndex * 0.2f);
                    if (random.nextFloat() < player.critChance) damage *= player.critDamage;
                    e.hp -= damage;
                    e.x += player.x < e.x ? 18f : -18f;
                    relicCharge = Math.min(100f, relicCharge + 6f);
                    spawnHitEffect(e.x, e.y - 48f, (int) damage);
                }
            }
            if (boss != null && Math.abs(boss.x - player.x) < 140f) {
                float damage = player.attack * 1.4f;
                if (random.nextFloat() < player.critChance) damage *= player.critDamage;
                boss.hp -= damage;
                relicCharge = Math.min(100f, relicCharge + 8f);
                spawnHitEffect(boss.x, boss.y - 70f, (int) damage);
            }
        }
        if (input.dash && player.dashCooldown <= 0f) {
            player.dashCooldown = 0.45f;
            player.x += player.facing > 0 ? 56f : -56f;
            if (relicCharge >= 100f) {
                for (Enemy e : enemies) {
                    if (Math.abs(e.x - player.x) < 220f) {
                        e.hp -= player.attack * 2.6f;
                    }
                }
                if (boss != null && Math.abs(boss.x - player.x) < 260f) {
                    boss.hp -= player.attack * 3.2f;
                }
                relicCharge = 0f;
            }
        }
        if (input.pickup) {
            pickupNearby();
        }
        player.update(dt);
        spawnTimer += dt;
        if (spawnTimer > 2.6f && boss == null && !portalActive && !noMoreSpawnsUntilStage1) {
            spawnTimer = 0f;
            spawnEnemyWave();
        }
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.x += (player.x < e.x ? -1f : 1f) * e.speed * dt;
            if (Math.abs(e.x - player.x) < e.range) {
                e.attackCd -= dt;
                if (e.attackCd <= 0f) {
                    e.attackCd = 1.1f;
                    if (player.invulnTimer <= 0f) {
                        player.hp -= Math.max(1f, e.attack - player.defense * 0.3f);
                        player.invulnTimer = 0.45f;
                    }
                }
            }
            if (e.hp <= 0f) {
                ItemDefinition drop = lootManager.rollNormalDrop(stageIndex);
                if (drop != null) spawnGroundLoot(e.x, e.y, drop, 12f);
                killCount++;
                stageKills++;
                coins += 7 + stageIndex;
                relicCharge = Math.min(100f, relicCharge + 10f);
                if (killCount % 8 == 0) {
                    player.hp = Math.min(player.maxHp, player.hp + 12f);
                }
                it.remove();
            }
        }
        if (boss == null && !portalActive && stageKills > 8 + stageIndex * 4) {
            boss = new BossEnemy(stageManager.byIndex(stageIndex).boss, 1050f, 140f + stageIndex * 55f, 10f + stageIndex * 2f, 90f + stageIndex * 8f, 115f);
        }
        if (boss != null) {
            boss.x += (player.x < boss.x ? -1f : 1f) * boss.speed * dt;
            if (boss.hp < 65f) {
                boss.speed += 3f;
            }
            if (Math.abs(boss.x - player.x) < boss.range && player.invulnTimer <= 0f) {
                player.hp -= Math.max(2f, boss.attack - player.defense * 0.25f);
                player.invulnTimer = 0.5f;
            }
            if (boss.hp <= 0f) {
                resultBoss = boss.bossName;
                resultLoot = lootManager.rollBossGuaranteed(stageIndex);
                spawnGroundLoot(boss.x, boss.y, resultLoot, 22f);
                coins += 120 + stageIndex * 35;
                boss = null;
                enemies.clear();
                if (stageIndex >= 6) {
                    state = GameState.VICTORY;
                    returnToMainMenuRequested = true;
                } else {
                    enterNextStage();
                }
            }
        }
        if (player.hp <= 0f) {
            state = GameState.GAME_OVER;
        }
        updateFx(dt);
        if (attackFxTimer > 0f) attackFxTimer -= dt;
    }

    private void castDragonSkill() {
        dragonSkillCooldown = 8.5f;
        dragonFxTimer = 0.95f;
        dragonFxFacing = player.facing;
        dragonFxX = player.x + player.facing * 86f;
        dragonFxY = player.y - 72f;
        float centerX = player.x + player.facing * 220f;
        float span = 280f;
        float damage = player.attack * 6.5f + stageIndex * 11f;
        for (Enemy e : enemies) {
            if (Math.abs(e.x - centerX) <= span) {
                e.hp -= damage;
                e.x += player.facing * 26f;
                spawnHitEffect(e.x, e.y - 56f, (int) damage);
            }
        }
        if (boss != null && Math.abs(boss.x - centerX) <= span + 40f) {
            float bossDamage = damage * 1.25f;
            boss.hp -= bossDamage;
            boss.x += player.facing * 20f;
            spawnHitEffect(boss.x, boss.y - 78f, (int) bossDamage);
        }
        relicCharge = Math.max(0f, relicCharge - 35f);
    }

    private void spawnEnemyWave() {
        waveCount++;
        String[] types = new String[]{"Raider", "Hound", "Thrower", "Shield Guard", "Heavy Brute", "Dark Acolyte", "Duelist", "Wisp"};
        for (int i = 0; i < 2 + stageIndex; i++) {
            String type = types[(i + stageIndex) % types.length];
            float hp = 20f + stageIndex * 9f + i * 2f;
            float atk = 4f + stageIndex * 1.4f;
            float speed = 70f + (i % 3) * 26f;
            float range = 54f + (i % 4) * 12f;
            enemies.add(new Enemy(type, 760f + i * 90f, hp, atk, speed, range));
        }
        if (waveCount % 3 == 0) {
            enemies.add(new Enemy("Elite", 960f, 90f + stageIndex * 16f, 10f + stageIndex * 2f, 92f, 100f));
        }
    }

    private void enterNextStage() {
        portalActive = false;
        stageIndex = Math.min(6, stageIndex + 1);
        stageKills = 0;
        waveCount = 0;
        noMoreSpawnsUntilStage1 = false;
        spawnTimer = 0f;
        enemies.clear();
        boss = null;
        player.x = 120f;
        player.y = 420f;
        player.velocityX = 0f;
        player.velocityY = 0f;
        player.hp = Math.min(player.maxHp, player.hp + 20f);
    }

    private void updateFx(float dt) {
        for (int i = hitEffects.size() - 1; i >= 0; i--) {
            HitEffect fx = hitEffects.get(i);
            fx.ttl -= dt;
            if (fx.ttl <= 0f) hitEffects.remove(i);
        }
        for (int i = damageTexts.size() - 1; i >= 0; i--) {
            DamageText t = damageTexts.get(i);
            t.ttl -= dt;
            t.y -= dt * 30f;
            if (t.ttl <= 0f) damageTexts.remove(i);
        }
        for (int i = groundLoot.size() - 1; i >= 0; i--) {
            GroundLoot g = groundLoot.get(i);
            g.ttl -= dt;
            if (g.ttl <= 0f) groundLoot.remove(i);
        }
        if (dragonFxTimer > 0f) dragonFxTimer -= dt;
    }

    private void spawnHitEffect(float x, float y, int value) {
        HitEffect fx = new HitEffect();
        fx.x = x;
        fx.y = y;
        fx.ttl = 0.18f;
        hitEffects.add(fx);
        DamageText text = new DamageText();
        text.x = x;
        text.y = y;
        text.value = value;
        text.ttl = 0.55f;
        damageTexts.add(text);
    }

    public void equipBestForSlot(SlotType slot) {
        ItemDefinition best = null;
        int bestIdx = -1;
        for (int i = 0; i < inventory.items().size(); i++) {
            ItemDefinition item = inventory.items().get(i);
            if (item.slot != slot) continue;
            if (best == null || item.power > best.power) {
                best = item;
                bestIdx = i;
            }
        }
        if (best != null) {
            equipFromInventoryIndex(bestIdx);
            applyEquipmentStats();
        }
    }

    public void equipInventoryIndex(int index) {
        equipFromInventoryIndex(index);
        applyEquipmentStats();
    }

    private void equipFromInventoryIndex(int index) {
        if (index < 0 || index >= inventory.items().size()) return;
        ItemDefinition picked = inventory.items().remove(index);
        if (picked == null) return;

        ItemDefinition previous = equipment.get(picked.slot);
        equipment.equip(picked);
        if (previous != null) {
            putBackOrDrop(previous);
        }
    }

    public void nextInventoryPage(int pageSize) {
        int maxPage = maxInventoryPage(pageSize);
        inventoryPage = Math.min(maxPage, inventoryPage + 1);
    }

    public void prevInventoryPage() {
        inventoryPage = Math.max(0, inventoryPage - 1);
    }

    public int maxInventoryPage(int pageSize) {
        int size = inventory.items().size();
        if (size <= 0) return 0;
        return Math.max(0, (size - 1) / Math.max(1, pageSize));
    }

    public void unequipSlot(SlotType slot) {
        ItemDefinition it = equipment.get(slot);
        if (it == null) return;
        if (!inventory.add(it)) {
            // If inventory is full, keep it equipped rather than losing it.
            return;
        }
        equipment.clear(slot);
        applyEquipmentStats();
    }

    public void applyEquipmentStats() {
        float hp = 120f;
        float attack = 12f;
        float defense = 4f;
        float move = 260f;
        float critChance = 0.05f;
        float critDamage = 1.5f;
        for (ItemDefinition item : equipment.all().values()) {
            if (item == null) continue;
            hp += item.hp;
            attack += item.attack;
            defense += item.defense;
            move += item.moveSpeed * 100f;
            critChance += item.critChance;
            critDamage += item.critDamage;
        }
        player.maxHp = hp;
        player.hp = Math.min(player.hp, player.maxHp);
        player.attack = attack;
        player.defense = defense;
        player.moveSpeed = move;
        player.critChance = Math.min(0.85f, critChance);
        player.critDamage = Math.min(4.0f, critDamage);
    }

    private void spawnGroundLoot(float x, float y, ItemDefinition item, float ttl) {
        GroundLoot g = new GroundLoot();
        g.x = x;
        g.y = y;
        g.item = item;
        g.ttl = ttl;
        groundLoot.add(g);
    }

    private void putBackOrDrop(ItemDefinition item) {
        if (item == null) return;
        if (!inventory.add(item)) {
            spawnGroundLoot(player.x + random.nextInt(28) - 14, player.y, item, 18f);
        }
    }

    private void pickupNearby() {
        for (int i = groundLoot.size() - 1; i >= 0; i--) {
            GroundLoot g = groundLoot.get(i);
            if (Math.abs(g.x - player.x) < 80f) {
                if (inventory.add(g.item)) {
                    groundLoot.remove(i);
                }
            }
        }
    }

    public void toggleModal(boolean bag, boolean equip) {
        modalOpen = !(modalOpen && modalBag == bag && modalEquip == equip);
        modalBag = bag;
        modalEquip = equip;
        if (!modalOpen) {
            modalDragIndex = -1;
            modalPage = 0;
            state = GameState.PLAYING;
        } else {
            state = GameState.PAUSED;
        }
    }

    public int modalMaxPage(int pageSize) {
        int size = inventory.items().size();
        if (size <= 0) return 0;
        return Math.max(0, (size - 1) / Math.max(1, pageSize));
    }

    public void modalNextPage(int pageSize) {
        int max = modalMaxPage(pageSize);
        modalPage = Math.min(max, modalPage + 1);
    }

    public void modalPrevPage() {
        modalPage = Math.max(0, modalPage - 1);
    }

    public void resetToStage1() {
        stageIndex = 1;
        stageKills = 0;
        waveCount = 0;
        enemies.clear();
        boss = null;
        portalActive = false;
        noMoreSpawnsUntilStage1 = false;
        returnToMainMenuRequested = false;
        player.x = 120f;
        player.y = 420f;
        player.velocityX = 0f;
        player.velocityY = 0f;
        player.hp = player.maxHp;
    }

    public void startAtStage(int selectedStage) {
        int safeStage = Math.max(1, Math.min(6, selectedStage));
        stageIndex = safeStage;
        stageKills = 0;
        waveCount = 0;
        killCount = 0;
        enemies.clear();
        boss = null;
        portalActive = false;
        noMoreSpawnsUntilStage1 = false;
        resultBoss = "";
        resultLoot = null;
        relicCharge = 0f;
        spawnTimer = 0f;
        dragonSkillCooldown = 0f;
        dragonFxTimer = 0f;
        returnToMainMenuRequested = false;
        player.x = 120f;
        player.y = 420f;
        player.velocityX = 0f;
        player.velocityY = 0f;
        player.hp = player.maxHp;
        state = GameState.PLAYING;
    }

    public void showInspect(ItemDefinition item, float x, float y) {
        if (item == null) return;
        inspectItem = item;
        inspectX = x;
        inspectY = y;
        inspectTimer = 2.6f;
    }

    public boolean consumeReturnToMainMenuRequest() {
        boolean requested = returnToMainMenuRequested;
        returnToMainMenuRequested = false;
        return requested;
    }
}
