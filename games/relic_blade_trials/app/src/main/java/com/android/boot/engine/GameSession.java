package com.android.boot.engine;

import com.android.boot.input.InputController;
import com.android.boot.inventory.EquipmentManager;
import com.android.boot.inventory.InventoryManager;
import com.android.boot.loot.ItemDatabase;
import com.android.boot.loot.LootManager;
import com.android.boot.model.BossEnemy;
import com.android.boot.model.Enemy;
import com.android.boot.model.ItemDefinition;
import com.android.boot.model.Player;
import com.android.boot.stage.StageManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public GameSession() {
        for (ItemDefinition item : itemDatabase.all()) {
            if (item.id.equals("wooden_sword")) {
                equipment.equip(item);
            }
        }
    }

    public void update(float dt, InputController input) {
        if (state == GameState.PLAYING) {
            stepPlaying(dt, input);
        } else if (state == GameState.PAUSED && input.pause) {
            state = GameState.PLAYING;
        }
    }

    private void stepPlaying(float dt, InputController input) {
        player.velocityX = 0f;
        if (input.left) player.velocityX = -player.moveSpeed;
        if (input.right) player.velocityX = player.moveSpeed;
        if (input.jump && player.y >= 420f) player.velocityY = -360f;
        if (input.attack) {
            player.comboIndex = (player.comboIndex % 3) + 1;
            player.comboTimer = 0.22f;
            for (Enemy e : enemies) {
                if (Math.abs(e.x - player.x) < 120f) {
                    e.hp -= player.attack * (1f + player.comboIndex * 0.2f);
                    e.x += player.x < e.x ? 18f : -18f;
                }
            }
            if (boss != null && Math.abs(boss.x - player.x) < 140f) {
                boss.hp -= player.attack * 1.4f;
            }
        }
        if (input.dash) {
            player.x += player.velocityX > 0 ? 56f : -56f;
        }
        if (input.pause) {
            state = GameState.PAUSED;
        }
        player.update(dt);
        spawnTimer += dt;
        if (spawnTimer > 2.6f && boss == null) {
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
                if (drop != null) {
                    inventory.add(drop);
                }
                coins += 7 + stageIndex;
                it.remove();
            }
        }
        if (boss == null && coins > 40 + stageIndex * 20) {
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
                inventory.add(resultLoot);
                coins += 120 + stageIndex * 35;
                boss = null;
                state = GameState.STAGE_RESULT;
            }
        }
        if (player.hp <= 0f) {
            state = GameState.GAME_OVER;
        }
    }

    private void spawnEnemyWave() {
        String[] types = new String[]{"Raider", "Hound", "Thrower", "Shield Guard", "Heavy Brute", "Dark Acolyte", "Duelist", "Wisp"};
        for (int i = 0; i < 2 + stageIndex; i++) {
            String type = types[(i + stageIndex) % types.length];
            float hp = 20f + stageIndex * 9f + i * 2f;
            float atk = 4f + stageIndex * 1.4f;
            float speed = 70f + (i % 3) * 26f;
            float range = 54f + (i % 4) * 12f;
            enemies.add(new Enemy(type, 760f + i * 90f, hp, atk, speed, range));
        }
    }
}
