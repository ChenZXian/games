package com.android.boot.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.android.boot.engine.GameSession;
import com.android.boot.engine.GameState;
import com.android.boot.engine.GroundLoot;
import com.android.boot.model.Enums.SlotType;
import com.android.boot.model.BossEnemy;
import com.android.boot.model.Enemy;
import com.android.boot.model.ItemDefinition;
import com.android.boot.model.Player;
import com.android.boot.fx.DamageText;
import com.android.boot.fx.HitEffect;

public class GameRenderer {
    private final Paint paint = new Paint();
    private final Paint controlFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint controlStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiStroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void draw(Canvas canvas, GameSession s) {
        canvas.drawColor(Color.rgb(8, 16, 34));
        paint.setColor(Color.rgb(20, 40, 70));
        canvas.drawRect(0, 460, canvas.getWidth(), canvas.getHeight(), paint);
        if (s.state == GameState.MENU) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(56f);
            canvas.drawText("Relic Blade Trials", 80, 130, paint);
            paint.setTextSize(30f);
            canvas.drawText("Tap center to start", 80, 190, paint);
            canvas.drawText("Inventory and equipment in menu and result", 80, 235, paint);
            return;
        }
        drawPlayer(canvas, s.player);
        for (Enemy enemy : s.enemies) {
            drawEnemy(canvas, enemy);
        }
        if (s.boss != null) {
            drawBoss(canvas, s.boss);
        }
        for (GroundLoot g : s.groundLoot) {
            drawLoot(canvas, g);
        }
        for (HitEffect fx : s.hitEffects) {
            float alpha = Math.max(0f, Math.min(1f, fx.ttl / 0.18f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb((int) (220f * alpha), 255, 235, 120));
            canvas.drawCircle(fx.x, fx.y, 18f + (1f - alpha) * 20f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        for (DamageText t : s.damageTexts) {
            paint.setColor(Color.rgb(255, 210, 110));
            paint.setTextSize(24f);
            canvas.drawText("-" + t.value, t.x - 14f, t.y, paint);
        }
        if (s.attackFxTimer > 0f) {
            float k = Math.max(0f, Math.min(1f, s.attackFxTimer / 0.18f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10f);
            paint.setColor(Color.argb((int) (235f * k), 160, 240, 255));
            float ex = s.attackFxX + (1f - k) * s.attackFxFacing * 28f;
            float ey = s.attackFxY;
            canvas.drawArc(ex - 70f, ey - 52f, ex + 70f, ey + 52f, s.attackFxFacing > 0 ? -35f : 145f, 120f, false, paint);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb((int) (200f * k), 255, 230, 120));
            canvas.drawCircle(ex, ey, 20f + (1f - k) * 26f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (s.dragonFxTimer > 0f) {
            float t = Math.max(0f, Math.min(1f, s.dragonFxTimer / 0.95f));
            float headX = s.dragonFxX + (1f - t) * s.dragonFxFacing * 420f;
            float headY = s.dragonFxY - (1f - t) * 40f;
            for (int i = 0; i < 5; i++) {
                float trail = i / 4f;
                float alpha = (1f - trail) * t;
                float tx = headX - s.dragonFxFacing * (50f + i * 68f);
                float ry = headY + (float) Math.sin((1f - t) * 8f + i * 0.9f) * (8f + i * 3f);
                paint.setColor(Color.argb((int) (190f * alpha), 255, 90 + i * 24, 25));
                canvas.drawCircle(tx, ry, 34f - i * 4f, paint);
                paint.setColor(Color.argb((int) (170f * alpha), 255, 205, 80));
                canvas.drawCircle(tx + s.dragonFxFacing * 10f, ry, 16f - i * 2f, paint);
            }
            paint.setColor(Color.argb((int) (230f * t), 255, 72, 36));
            canvas.drawCircle(headX, headY, 46f, paint);
            paint.setColor(Color.argb((int) (220f * t), 255, 220, 110));
            canvas.drawCircle(headX + s.dragonFxFacing * 8f, headY - 2f, 22f, paint);
            paint.setColor(Color.argb((int) (180f * t), 255, 255, 255));
            canvas.drawCircle(headX + s.dragonFxFacing * 18f, headY - 4f, 9f, paint);
        }
        if (s.portalActive) {
            drawPortal(canvas, s);
        }
        drawHud(canvas, s);
        if (s.inspectItem != null && s.inspectTimer > 0f) {
            drawInspect(canvas, s);
        }
        drawBottomButtons(canvas, s);
        if (!s.modalOpen) {
            if (s.showStatusPanel) drawStatusPanel(canvas, s);
            if (s.showEquipmentPanel) drawEquipmentPanel(canvas, s);
            // Inventory bar removed: use modal backpack instead.
        } else {
            drawModal(canvas, s);
        }
        if (s.state == GameState.PAUSED) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(54f);
            canvas.drawText("Paused", canvas.getWidth() * 0.45f, canvas.getHeight() * 0.45f, paint);
        }
        if (s.state == GameState.GAME_OVER) {
            paint.setColor(Color.RED);
            paint.setTextSize(52f);
            canvas.drawText("Game Over", canvas.getWidth() * 0.41f, canvas.getHeight() * 0.45f, paint);
        }
        if (s.state == GameState.STAGE_RESULT) {
            paint.setColor(Color.GREEN);
            paint.setTextSize(44f);
            canvas.drawText("Stage Clear", 80, 120, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30f);
            canvas.drawText("Boss Defeated: " + s.resultBoss, 80, 170, paint);
            if (s.resultLoot != null) {
                canvas.drawText("Loot: " + s.resultLoot.name + " " + s.resultLoot.rarity.name(), 80, 210, paint);
            }
            canvas.drawText("Tap left for replay, right for stage select", 80, 250, paint);
        }
        if (s.state == GameState.PLAYING || s.state == GameState.PAUSED) {
            drawControls(canvas, s);
        }
    }

    private void drawHud(Canvas canvas, GameSession s) {
        uiFill.setColor(Color.argb(150, 10, 18, 34));
        canvas.drawRoundRect(18f, 14f, 370f, 50f, 12f, 12f, uiFill);
        uiStroke.setStyle(Paint.Style.STROKE);
        uiStroke.setStrokeWidth(2f);
        uiStroke.setColor(Color.argb(160, 120, 200, 255));
        canvas.drawRoundRect(18f, 14f, 370f, 50f, 12f, 12f, uiStroke);
        uiStroke.setStyle(Paint.Style.FILL);
        float hpW = 340f * (s.player.hp / Math.max(1f, s.player.maxHp));
        uiFill.setColor(Color.argb(210, 50, 230, 160));
        canvas.drawRoundRect(22f, 18f, 22f + hpW, 46f, 10f, 10f, uiFill);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24f);
        canvas.drawText("Stage " + s.stageIndex, 380, 40, paint);
        canvas.drawText("Coins " + s.coins, 520, 40, paint);
        canvas.drawText("Wave " + s.waveCount, 660, 40, paint);
        canvas.drawText("Charge " + (int) s.relicCharge + "%", 860, 40, paint);
        int cd = (int) Math.ceil(Math.max(0f, s.dragonSkillCooldown));
        canvas.drawText(cd <= 0 ? "Dragon Ready" : "Dragon CD " + cd + "s", 1040, 40, paint);
        if (s.boss != null) {
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(760, 20, 1180, 44, paint);
            paint.setColor(Color.MAGENTA);
            canvas.drawRect(760, 20, 760 + (s.boss.hp / (140f + s.stageIndex * 55f)) * 420f, 44, paint);
        }
    }

    private void drawStatusPanel(Canvas canvas, GameSession s) {
        float x = 20f;
        float y = 60f;
        float w = 380f;
        float h = 198f;
        uiFill.setColor(Color.argb(210, 12, 18, 34));
        canvas.drawRoundRect(x, y, x + w, y + h, 18f, 18f, uiFill);
        uiStroke.setStyle(Paint.Style.STROKE);
        uiStroke.setStrokeWidth(3f);
        uiStroke.setColor(Color.argb(190, 120, 200, 255));
        canvas.drawRoundRect(x, y, x + w, y + h, 18f, 18f, uiStroke);
        uiStroke.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22f);
        canvas.drawText("Status", x + 14f, y + 30f, paint);

        // HP bar
        float hpPct = s.player.hp / Math.max(1f, s.player.maxHp);
        uiFill.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRoundRect(x + 14f, y + 40f, x + w - 14f, y + 62f, 10f, 10f, uiFill);
        uiFill.setColor(Color.argb(220, 50, 230, 160));
        canvas.drawRoundRect(x + 14f, y + 40f, x + 14f + (w - 28f) * hpPct, y + 62f, 10f, 10f, uiFill);
        paint.setTextSize(18f);
        canvas.drawText("HP " + (int) s.player.hp + "/" + (int) s.player.maxHp, x + 18f, y + 58f, paint);

        paint.setTextSize(18f);
        float ty = y + 88f;
        canvas.drawText("ATK  " + (int) s.player.attack, x + 14f, ty, paint);
        canvas.drawText("DEF  " + (int) s.player.defense, x + 200f, ty, paint);
        canvas.drawText("CRIT " + (int) (s.player.critChance * 100f) + "%", x + 14f, ty + 24f, paint);
        canvas.drawText("CDMG " + String.format("%.0f%%", s.player.critDamage * 100f), x + 200f, ty + 24f, paint);
        canvas.drawText("MSPD " + (int) s.player.moveSpeed, x + 14f, ty + 48f, paint);
        canvas.drawText("POWER " + calcPower(s), x + 200f, ty + 48f, paint);

        uiFill.setColor(Color.argb(210, 38, 120, 200));
        canvas.drawRoundRect(x + 14f, y + h - 42f, x + 182f, y + h - 12f, 12f, 12f, uiFill);
        canvas.drawRoundRect(x + 198f, y + h - 42f, x + w - 14f, y + h - 12f, 12f, 12f, uiFill);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16f);
        canvas.drawText("AUTO WEAPON", x + 32f, y + h - 22f, paint);
        canvas.drawText("AUTO ARMOR", x + 220f, y + h - 22f, paint);
    }

    private void drawModal(Canvas canvas, GameSession s) {
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        uiFill.setColor(Color.argb(190, 0, 0, 0));
        canvas.drawRect(0f, 0f, w, h, uiFill);
        float l = w * 0.08f;
        float t = h * 0.10f;
        float r = w * 0.92f;
        float b = h * 0.86f;
        uiFill.setColor(Color.argb(220, 12, 18, 34));
        canvas.drawRoundRect(l, t, r, b, 18f, 18f, uiFill);
        uiStroke.setStyle(Paint.Style.STROKE);
        uiStroke.setStrokeWidth(3f);
        uiStroke.setColor(Color.argb(190, 120, 200, 255));
        canvas.drawRoundRect(l, t, r, b, 18f, 18f, uiStroke);
        uiStroke.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(26f);
        canvas.drawText("Equipment", l + 18f, t + 42f, paint);
        canvas.drawText("Backpack", (l + r) * 0.5f + 18f, t + 42f, paint);

        paint.setTextSize(22f);
        canvas.drawText("X", w * 0.92f - 18f, t + 36f, paint);

        float mid = (l + r) * 0.5f;
        float leftHalfMid = (l + mid) * 0.5f;
        float avatarX = leftHalfMid;
        float avatarY = t + (b - t) * 0.58f;

        float slotY = t + 130f;
        float slotGapY = 54f;
        float slotLeftX = avatarX - 220f;
        float slotRightX = avatarX + 60f;
        drawEquipSlotModal(canvas, s, SlotType.WEAPON, "Weapon", slotLeftX, slotY + 0f * slotGapY);
        drawEquipSlotModal(canvas, s, SlotType.ARMOR, "Armor", slotLeftX, slotY + 1f * slotGapY);
        drawEquipSlotModal(canvas, s, SlotType.BRACERS, "Bracers", slotLeftX, slotY + 2f * slotGapY);
        drawEquipSlotModal(canvas, s, SlotType.SHOES, "Shoes", slotRightX, slotY + 0f * slotGapY);
        drawEquipSlotModal(canvas, s, SlotType.HELMET, "Helmet", slotRightX, slotY + 1f * slotGapY);
        drawEquipSlotModal(canvas, s, SlotType.BELT, "Belt", slotRightX, slotY + 2f * slotGapY);

        drawAvatar(canvas, avatarX, avatarY);

        float invL = mid + 14f;
        float invR = r - 14f;
        float invT = t + 70f;
        float invB = b - 70f;
        drawModalInventory(canvas, s, invL, invT, invR, invB);

        if (s.modalDragIndex >= 0 && s.modalDragIndex < s.inventory.items().size()) {
            ItemDefinition item = s.inventory.items().get(s.modalDragIndex);
            uiFill.setColor(rarityColor(item));
            uiFill.setAlpha(220);
            canvas.drawRoundRect(s.modalDragX - 90f, s.modalDragY - 22f, s.modalDragX + 90f, s.modalDragY + 22f, 12f, 12f, uiFill);
            paint.setColor(Color.BLACK);
            paint.setTextSize(18f);
            String label = item.name.length() > 16 ? item.name.substring(0, 16) : item.name;
            canvas.drawText(label, s.modalDragX - 80f, s.modalDragY + 6f, paint);
        }
    }

    private void drawAvatar(Canvas canvas, float x, float y) {
        paint.setColor(Color.rgb(80, 205, 255));
        canvas.drawCircle(x, y - 80f, 14f, paint);
        paint.setColor(Color.rgb(30, 170, 240));
        canvas.drawRoundRect(x - 14f, y - 66f, x + 14f, y - 18f, 8f, 8f, paint);
        paint.setColor(Color.rgb(15, 120, 215));
        canvas.drawRect(x - 10f, y - 18f, x - 1f, y + 18f, paint);
        canvas.drawRect(x + 1f, y - 18f, x + 10f, y + 18f, paint);
    }

    private void drawEquipSlotModal(Canvas canvas, GameSession s, SlotType slot, String title, float x, float y) {
        ItemDefinition it = s.equipment.get(slot);
        uiFill.setColor(Color.argb(170, 55, 70, 98));
        canvas.drawRoundRect(x, y, x + 160f, y + 42f, 12f, 12f, uiFill);
        uiStroke.setStyle(Paint.Style.STROKE);
        uiStroke.setStrokeWidth(2f);
        uiStroke.setColor(Color.argb(160, 120, 200, 255));
        canvas.drawRoundRect(x, y, x + 160f, y + 42f, 12f, 12f, uiStroke);
        uiStroke.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        String name = it == null ? "Empty" : it.name;
        if (name.length() > 12) name = name.substring(0, 12);
        canvas.drawText(title, x + 8f, y + 18f, paint);
        canvas.drawText(name, x + 8f, y + 38f, paint);
    }

    private void drawModalInventory(Canvas canvas, GameSession s, float l, float t, float r, float b) {
        int pageSize = 8;
        int maxPage = s.modalMaxPage(pageSize);
        int page = Math.max(0, Math.min(maxPage, s.modalPage));
        float rowH = 54f;
        for (int i = 0; i < pageSize; i++) {
            int idx = page * pageSize + i;
            float top = t + i * rowH;
            float bot = top + rowH - 6f;
            ItemDefinition item = idx < s.inventory.items().size() ? s.inventory.items().get(idx) : null;
            uiFill.setColor(item == null ? Color.argb(120, 60, 80, 110) : rarityColor(item));
            uiFill.setAlpha(item == null ? 120 : 200);
            canvas.drawRoundRect(l, top, r, bot, 12f, 12f, uiFill);
            paint.setColor(item == null ? Color.WHITE : Color.BLACK);
            paint.setTextSize(18f);
            String label = item == null ? "Empty" : item.name;
            if (label.length() > 22) label = label.substring(0, 22);
            canvas.drawText((idx + 1) + "  " + label, l + 10f, top + 34f, paint);
        }
        uiFill.setColor(Color.argb(170, 55, 70, 98));
        canvas.drawRoundRect(l, b + 16f, l + 90f, b + 56f, 12f, 12f, uiFill);
        canvas.drawRoundRect(r - 90f, b + 16f, r, b + 56f, 12f, 12f, uiFill);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        canvas.drawText("PREV", l + 18f, b + 42f, paint);
        canvas.drawText("NEXT", r - 74f, b + 42f, paint);
        canvas.drawText((page + 1) + "/" + (maxPage + 1), (l + r) * 0.5f - 18f, b + 42f, paint);
    }
    private void drawInventoryBar(Canvas canvas, GameSession s) {
        float y = canvas.getHeight() - 88f;
        paint.setColor(Color.argb(160, 14, 22, 38));
        canvas.drawRoundRect(16f, y, canvas.getWidth() - 16f, canvas.getHeight() - 16f, 14f, 14f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22f);
        canvas.drawText("Inventory", 28f, y + 28f, paint);

        int pageSize = 6;
        int maxPage = s.maxInventoryPage(pageSize);
        int page = Math.max(0, Math.min(maxPage, s.inventoryPage));
        paint.setTextSize(18f);
        canvas.drawText((page + 1) + "/" + (maxPage + 1), 112f, y + 28f, paint);

        paint.setColor(Color.argb(170, 55, 70, 98));
        canvas.drawRoundRect(16f + 112f, y + 10f, 16f + 180f, y + 62f, 10f, 10f, paint);
        canvas.drawRoundRect(canvas.getWidth() - 16f - 180f, y + 10f, canvas.getWidth() - 16f - 112f, y + 62f, 10f, 10f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        canvas.drawText("PREV", 16f + 128f, y + 40f, paint);
        canvas.drawText("NEXT", canvas.getWidth() - 16f - 168f, y + 40f, paint);

        int shown = 6;
        for (int i = 0; i < shown; i++) {
            float sx = 150f + i * 165f;
            int idx = page * pageSize + i;
            ItemDefinition item = idx < s.inventory.items().size() ? s.inventory.items().get(idx) : null;
            paint.setColor(item == null ? Color.rgb(85, 98, 116) : rarityColor(item));
            canvas.drawRoundRect(sx, y + 10f, sx + 152f, y + 62f, 10f, 10f, paint);
            paint.setColor(item == null ? Color.WHITE : Color.BLACK);
            paint.setTextSize(18f);
            String label = item == null ? (idx + 1) + ":EMPTY" : ((idx + 1) + ":" + (item.name.length() > 14 ? item.name.substring(0, 14) : item.name));
            canvas.drawText(label, sx + 6f, y + 40f, paint);
        }
    }

    private void drawBottomButtons(Canvas canvas, GameSession s) {
        float y = canvas.getHeight() - 70f;
        float cx = canvas.getWidth() * 0.5f;
        paint.setTextSize(18f);
        paint.setColor(s.showStatusPanel || s.showEquipmentPanel ? Color.rgb(55, 150, 235) : Color.rgb(60, 80, 110));
        canvas.drawRoundRect(cx - 120f, y, cx - 10f, y + 44f, 12f, 12f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("STATUS", cx - 102f, y + 28f, paint);
        paint.setColor(s.showInventoryPanel ? Color.rgb(55, 150, 235) : Color.rgb(60, 80, 110));
        canvas.drawRoundRect(cx + 10f, y, cx + 120f, y + 44f, 12f, 12f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("BAG", cx + 44f, y + 28f, paint);
    }

    private void drawEquipmentPanel(Canvas canvas, GameSession s) {
        float x = 20f;
        float y = 228f;
        float w = 360f;
        float h = 216f;
        paint.setColor(Color.argb(160, 18, 28, 44));
        canvas.drawRoundRect(x, y, x + w, y + h, 16f, 16f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22f);
        canvas.drawText("Equipment (tap slot to unequip)", x + 10f, y + 28f, paint);
        drawEquipSlot(canvas, s, SlotType.WEAPON, "Weapon", x + 12f, y + 42f);
        drawEquipSlot(canvas, s, SlotType.SHOES, "Shoes", x + 188f, y + 42f);
        drawEquipSlot(canvas, s, SlotType.ARMOR, "Armor", x + 12f, y + 88f);
        drawEquipSlot(canvas, s, SlotType.HELMET, "Helmet", x + 188f, y + 88f);
        drawEquipSlot(canvas, s, SlotType.BRACERS, "Bracers", x + 12f, y + 134f);
        drawEquipSlot(canvas, s, SlotType.BELT, "Belt", x + 188f, y + 134f);
    }

    private void drawEquipSlot(Canvas canvas, GameSession s, SlotType slot, String title, float x, float y) {
        paint.setColor(Color.argb(170, 55, 70, 98));
        canvas.drawRoundRect(x, y, x + 160f, y + 38f, 10f, 10f, paint);
        ItemDefinition item = s.equipment.get(slot);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        String name = item == null ? "None" : item.name;
        if (name.length() > 12) name = name.substring(0, 12);
        canvas.drawText(title + ": " + name, x + 6f, y + 25f, paint);
    }

    private int rarityColor(ItemDefinition item) {
        switch (item.rarity) {
            case GOLD: return Color.rgb(255, 217, 72);
            case PURPLE: return Color.rgb(194, 118, 255);
            case GREEN: return Color.rgb(108, 227, 138);
            default: return commonTint(item);
        }
    }

    private int commonTint(ItemDefinition item) {
        int base = Color.rgb(228, 228, 228);
        int tint;
        switch (item.slot) {
            case WEAPON: tint = Color.rgb(190, 220, 255); break;
            case ARMOR: tint = Color.rgb(225, 235, 245); break;
            case HELMET: tint = Color.rgb(240, 240, 240); break;
            case SHOES: tint = Color.rgb(230, 235, 255); break;
            case BRACERS: tint = Color.rgb(235, 240, 230); break;
            case BELT: tint = Color.rgb(245, 235, 225); break;
            default: tint = base; break;
        }
        return blend(base, tint, 0.55f);
    }

    private int blend(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return Color.rgb(r, g, bl);
    }

    private void drawInspect(Canvas canvas, GameSession s) {
        ItemDefinition it = s.inspectItem;
        if (it == null) return;
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        float x = Math.max(20f, Math.min(w - 420f, s.inspectX - 200f));
        float y = Math.max(70f, Math.min(h - 220f, s.inspectY - 200f));
        uiFill.setColor(Color.argb(230, 12, 18, 34));
        canvas.drawRoundRect(x, y, x + 400f, y + 190f, 16f, 16f, uiFill);
        uiStroke.setStyle(Paint.Style.STROKE);
        uiStroke.setStrokeWidth(3f);
        uiStroke.setColor(Color.argb(190, 120, 200, 255));
        canvas.drawRoundRect(x, y, x + 400f, y + 190f, 16f, 16f, uiStroke);
        uiStroke.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22f);
        canvas.drawText(it.name, x + 14f, y + 30f, paint);
        paint.setTextSize(18f);
        canvas.drawText(it.slot.name() + "  " + it.rarity.name() + "  T" + it.stageTier, x + 14f, y + 54f, paint);

        int c = rarityColor(it);
        uiFill.setColor(Color.argb(220, Color.red(c), Color.green(c), Color.blue(c)));
        canvas.drawRoundRect(x + 14f, y + 66f, x + 386f, y + 76f, 6f, 6f, uiFill);

        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        float ty = y + 100f;
        canvas.drawText("Power  +" + it.power, x + 14f, ty, paint);
        canvas.drawText("HP     +" + it.hp, x + 14f, ty + 22f, paint);
        canvas.drawText("ATK    +" + it.attack, x + 200f, ty, paint);
        canvas.drawText("DEF    +" + it.defense, x + 200f, ty + 22f, paint);
        canvas.drawText("MSPD   +" + String.format("%.2f", it.moveSpeed), x + 14f, ty + 44f, paint);
        canvas.drawText("CRIT   +" + String.format("%.1f%%", it.critChance * 100f), x + 200f, ty + 44f, paint);
        canvas.drawText("CDMG   +" + String.format("%.0f%%", it.critDamage * 100f), x + 14f, ty + 66f, paint);
        if (it.description != null && !it.description.isEmpty()) {
            String d = it.description.length() > 44 ? it.description.substring(0, 44) : it.description;
            canvas.drawText(d, x + 14f, ty + 92f, paint);
        }
    }

    private int calcPower(GameSession s) {
        int power = 0;
        for (ItemDefinition item : s.equipment.all().values()) {
            if (item != null) power += item.power;
        }
        return power;
    }

    private void drawPortal(Canvas canvas, GameSession s) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.argb(220, 160, 245, 255));
        canvas.drawCircle(s.portalX, s.portalY - 36f, 38f, paint);
        paint.setColor(Color.argb(170, 95, 190, 255));
        canvas.drawCircle(s.portalX, s.portalY - 36f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20f);
        canvas.drawText("ENTER", s.portalX - 30f, s.portalY + 18f, paint);
    }

    private void drawControls(Canvas canvas, GameSession s) {
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        controlFill.setStyle(Paint.Style.FILL);
        controlFill.setColor(Color.argb(140, 22, 36, 60));
        controlStroke.setStyle(Paint.Style.STROKE);
        controlStroke.setStrokeWidth(3f);
        controlStroke.setColor(Color.argb(220, 140, 190, 255));

        float leftT = h * 0.82f;
        float leftB = h - 18f;
        float leftL = 18f;
        float leftR = w * 0.12f - 10f;
        canvas.drawRoundRect(leftL, leftT, leftR, leftB, 18f, 18f, controlFill);
        canvas.drawRoundRect(leftL, leftT, leftR, leftB, 18f, 18f, controlStroke);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20f);
        canvas.drawText("LEFT", leftL + 10f, leftT + 32f, paint);

        float rightL = w * 0.12f + 10f;
        float rightR = w * 0.24f - 10f;
        canvas.drawRoundRect(rightL, leftT, rightR, leftB, 18f, 18f, controlFill);
        canvas.drawRoundRect(rightL, leftT, rightR, leftB, 18f, 18f, controlStroke);
        canvas.drawText("RIGHT", rightL + 8f, leftT + 32f, paint);

        float pickupL = w * 0.72f + 10f;
        float pickupR = w * 0.84f - 10f;
        controlFill.setColor(Color.argb(150, 95, 190, 255));
        canvas.drawRoundRect(pickupL, leftT, pickupR, leftB, 18f, 18f, controlFill);
        canvas.drawRoundRect(pickupL, leftT, pickupR, leftB, 18f, 18f, controlStroke);
        canvas.drawText("PICK", pickupL + 10f, leftT + 32f, paint);

        float attackL = w * 0.84f + 10f;
        float attackR = w - 18f;
        controlFill.setColor(Color.argb(160, 205, 62, 62));
        canvas.drawRoundRect(attackL, leftT, attackR, leftB, 18f, 18f, controlFill);
        canvas.drawRoundRect(attackL, leftT, attackR, leftB, 18f, 18f, controlStroke);
        canvas.drawText("ATK", attackL + 18f, leftT + 32f, paint);

        float pauseR = w * 0.05f;
        float pauseCx = w * 0.94f;
        float pauseCy = h * 0.08f;
        controlFill.setColor(Color.argb(150, 80, 80, 95));
        canvas.drawCircle(pauseCx, pauseCy, pauseR, controlFill);
        canvas.drawCircle(pauseCx, pauseCy, pauseR, controlStroke);
        canvas.drawText("II", pauseCx - 10f, pauseCy + 10f, paint);

        float skillL = w * 0.72f + 10f;
        float skillR = w * 0.84f - 10f;
        float skillT = h * 0.64f + 4f;
        float skillB = h * 0.82f - 4f;
        boolean ready = s.dragonSkillCooldown <= 0f;
        controlFill.setColor(ready ? Color.argb(165, 255, 106, 42) : Color.argb(120, 90, 90, 90));
        canvas.drawRoundRect(skillL, skillT, skillR, skillB, 18f, 18f, controlFill);
        canvas.drawRoundRect(skillL, skillT, skillR, skillB, 18f, 18f, controlStroke);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        canvas.drawText("DRAGON", skillL + 4f, skillT + 34f, paint);
    }

    private void drawLoot(Canvas canvas, GroundLoot g) {
        ItemDefinition item = g.item;
        if (item == null) return;
        int glow = rarityColor(item);
        paint.setColor(Color.argb(90, Color.red(glow), Color.green(glow), Color.blue(glow)));
        canvas.drawCircle(g.x, g.y - 12f, 18f, paint);
        paint.setColor(glow);
        float x = g.x;
        float y = g.y - 22f;
        switch (item.slot) {
            case WEAPON:
                canvas.drawRect(x - 2f, y - 26f, x + 2f, y + 18f, paint);
                canvas.drawRect(x - 10f, y + 6f, x + 10f, y + 10f, paint);
                break;
            case SHOES:
                canvas.drawRoundRect(x - 14f, y, x + 14f, y + 10f, 4f, 4f, paint);
                canvas.drawRect(x - 10f, y - 12f, x + 10f, y, paint);
                break;
            case ARMOR:
                canvas.drawRoundRect(x - 14f, y - 18f, x + 14f, y + 14f, 6f, 6f, paint);
                break;
            case HELMET:
                canvas.drawArc(x - 14f, y - 18f, x + 14f, y + 8f, 180f, 180f, true, paint);
                break;
            case BRACERS:
                canvas.drawRoundRect(x - 14f, y - 8f, x - 2f, y + 10f, 4f, 4f, paint);
                canvas.drawRoundRect(x + 2f, y - 8f, x + 14f, y + 10f, 4f, 4f, paint);
                break;
            case BELT:
                canvas.drawRoundRect(x - 16f, y - 2f, x + 16f, y + 6f, 4f, 4f, paint);
                paint.setColor(Color.BLACK);
                canvas.drawRect(x - 2f, y - 2f, x + 2f, y + 6f, paint);
                break;
        }
    }

    private void drawPlayer(Canvas canvas, Player p) {
        paint.setColor(Color.rgb(75, 205, 255));
        canvas.drawCircle(p.x, p.y - 86f, 13f, paint);
        paint.setColor(Color.rgb(30, 170, 240));
        canvas.drawRect(p.x - 12f, p.y - 72f, p.x + 12f, p.y - 30f, paint);
        paint.setColor(Color.rgb(190, 230, 255));
        canvas.drawRect(p.x - 24f, p.y - 66f, p.x - 12f, p.y - 40f, paint);
        canvas.drawRect(p.x + 12f, p.y - 66f, p.x + 24f, p.y - 40f, paint);
        paint.setColor(Color.rgb(15, 120, 215));
        canvas.drawRect(p.x - 10f, p.y - 30f, p.x - 1f, p.y, paint);
        canvas.drawRect(p.x + 1f, p.y - 30f, p.x + 10f, p.y, paint);
        paint.setColor(Color.WHITE);
        float swordX = p.x + p.facing * 28f;
        canvas.drawRect(swordX - 4f, p.y - 64f, swordX + 4f, p.y - 18f, paint);
    }

    private void drawEnemy(Canvas canvas, Enemy enemy) {
        int body = "Elite".equals(enemy.type) ? Color.rgb(255, 132, 38) : Color.rgb(230, 75, 75);
        paint.setColor(Color.rgb(255, 214, 172));
        canvas.drawCircle(enemy.x, enemy.y - 70f, "Elite".equals(enemy.type) ? 12f : 10f, paint);
        paint.setColor(body);
        canvas.drawRect(enemy.x - 11f, enemy.y - 58f, enemy.x + 11f, enemy.y - 22f, paint);
        paint.setColor(Color.rgb(160, 35, 35));
        canvas.drawRect(enemy.x - 9f, enemy.y - 22f, enemy.x - 1f, enemy.y, paint);
        canvas.drawRect(enemy.x + 1f, enemy.y - 22f, enemy.x + 9f, enemy.y, paint);
    }

    private void drawBoss(Canvas canvas, BossEnemy boss) {
        paint.setColor(Color.rgb(210, 120, 255));
        canvas.drawCircle(boss.x, boss.y - 102f, 19f, paint);
        paint.setColor(Color.rgb(150, 56, 212));
        canvas.drawRect(boss.x - 24f, boss.y - 84f, boss.x + 24f, boss.y - 26f, paint);
        paint.setColor(Color.rgb(103, 31, 158));
        canvas.drawRect(boss.x - 20f, boss.y - 26f, boss.x - 4f, boss.y, paint);
        canvas.drawRect(boss.x + 4f, boss.y - 26f, boss.x + 20f, boss.y, paint);
        paint.setColor(Color.rgb(255, 240, 110));
        canvas.drawRect(boss.x - 30f, boss.y - 74f, boss.x - 24f, boss.y - 16f, paint);
    }
}
