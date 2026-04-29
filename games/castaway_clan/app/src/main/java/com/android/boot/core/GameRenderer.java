package com.android.boot.core;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.android.boot.entity.CampStructure;
import com.android.boot.entity.ResourceNode;
import com.android.boot.entity.Survivor;
import com.android.boot.entity.Wildlife;
import com.android.boot.fx.FloatText;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private boolean loaded;

    public void ensureAssets(AssetManager assets) {
        if (loaded) {
            return;
        }
        loaded = true;
        load(assets, "leader_idle", "game_art/kenney_top_down_shooter/assets/PNG/Man Blue/manBlue_stand.png");
        load(assets, "leader_move", "game_art/kenney_top_down_shooter/assets/PNG/Man Blue/manBlue_hold.png");
        load(assets, "survivor_idle", "game_art/kenney_top_down_shooter/assets/PNG/Man Brown/manBrown_stand.png");
        load(assets, "survivor_move", "game_art/kenney_top_down_shooter/assets/PNG/Man Brown/manBrown_hold.png");
        load(assets, "boar", "game_art/kenney_animal_pack/assets/PNG/Round (outline)/pig.png");
        load(assets, "monkey", "game_art/kenney_animal_pack/assets/PNG/Round (outline)/monkey.png");
        load(assets, "snake", "game_art/kenney_animal_pack/assets/PNG/Round (outline)/snake.png");
        load(assets, "crab", "game_art/kenney_animal_pack/assets/PNG/Round (outline)/slice08_086.png");
        load(assets, "tile_beach", "game_art/kenney_tiny_town/assets/Tiles/tile_0016.png");
        load(assets, "tile_grass", "game_art/kenney_tiny_town/assets/Tiles/tile_0007.png");
        load(assets, "tile_water", "game_art/kenney_tiny_town/assets/Tiles/tile_0077.png");
        load(assets, "wreck", "game_art/kenney_pirate_pack/assets/PNG/Default size/Ship parts/hullSmall (1).png");
        load(assets, "fire", "game_art/kenney_pirate_pack/assets/PNG/Default size/Effects/fire1.png");
        load(assets, "beacon", "game_art/kenney_pirate_pack/assets/PNG/Default size/Ship parts/pole.png");
        load(assets, "flag", "game_art/kenney_pirate_pack/assets/PNG/Default size/Ship parts/flag (2).png");
    }

    private void load(AssetManager assets, String key, String path) {
        try (InputStream stream = assets.open(path)) {
            bitmaps.put(key, BitmapFactory.decodeStream(stream));
        } catch (IOException ignored) {
        }
    }

    public void render(Canvas canvas, CastawayWorld world, int width, int height, float stickX, float stickY, boolean actionDown) {
        drawBackground(canvas, world, width, height);
        drawCamp(canvas, world);
        drawNodes(canvas, world);
        drawWildlife(canvas, world);
        drawSurvivors(canvas, world);
        drawFloatTexts(canvas, world);
        drawModeHints(canvas, world, width, height);
        drawControls(canvas, width, height, stickX, stickY, actionDown);
        drawStatePanel(canvas, world, width, height);
    }

    private void drawBackground(Canvas canvas, CastawayWorld world, int width, int height) {
        canvas.drawColor(0xFFF5F8EE);
        float startX = -(world.camX % 96f);
        float startY = -(world.camY % 96f);
        for (float x = startX; x < width + 96f; x += 96f) {
            for (float y = startY; y < height + 96f; y += 96f) {
                float worldX = x + world.camX;
                float worldY = y + world.camY;
                Bitmap tile = pickTile(worldX, worldY);
                if (tile != null) {
                    canvas.drawBitmap(tile, null, new RectF(x, y, x + 96f, y + 96f), paint);
                } else {
                    paint.setColor((worldY > 1180f || worldX < 420f) ? 0xFFE3D2A6 : 0xFF98C47A);
                    canvas.drawRect(x, y, x + 96f, y + 96f, paint);
                }
            }
        }
        paint.setColor(0x5530B4D8);
        canvas.drawRect(-world.camX, 1180f - world.camY, width, height, paint);
        Bitmap wreck = bitmaps.get("wreck");
        if (wreck != null) {
            canvas.drawBitmap(wreck, null, new RectF(1600f - world.camX, 1000f - world.camY, 1830f - world.camX, 1180f - world.camY), paint);
        }
    }

    private Bitmap pickTile(float worldX, float worldY) {
        if (worldY > 1180f) {
            return bitmaps.get("tile_beach");
        }
        if (worldX < 580f && worldY < 760f) {
            return bitmaps.get("tile_water");
        }
        return bitmaps.get("tile_grass");
    }

    private void drawCamp(Canvas canvas, CastawayWorld world) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x22FFF4D2);
        canvas.drawCircle(world.campCenterX - world.camX, world.campCenterY - world.camY, 220f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFB88C56);
        paint.setStrokeWidth(6f);
        canvas.drawCircle(world.campCenterX - world.camX, world.campCenterY - world.camY, 220f, paint);
        Bitmap fire = bitmaps.get("fire");
        if (fire != null) {
            canvas.drawBitmap(fire, null, new RectF(world.campCenterX - 38f - world.camX, world.campCenterY - 42f - world.camY, world.campCenterX + 38f - world.camX, world.campCenterY + 50f - world.camY), paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFA13E);
            canvas.drawCircle(world.campCenterX - world.camX, world.campCenterY - world.camY, 28f, paint);
        }
        for (int i = 0; i < 6; i++) {
            CampStructure structure = findStructure(world, i);
            float slotX = world.campCenterX - world.camX - 160f + (i % 3) * 120f + (i >= 3 ? 60f : 0f);
            float slotY = world.campCenterY - world.camY - 110f + (i >= 3 ? 160f : 0f);
            rect.set(slotX - 42f, slotY - 42f, slotX + 42f, slotY + 42f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(structure == null ? 0x33FFFFFF : 0x55FFDFAE);
            canvas.drawRoundRect(rect, 22f, 22f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(structure == null ? 0x88B88C56 : 0xFFD17E3E);
            paint.setStrokeWidth(3f);
            canvas.drawRoundRect(rect, 22f, 22f, paint);
            if (structure != null) {
                drawStructure(canvas, structure, slotX, slotY);
            }
        }
    }

    private CampStructure findStructure(CastawayWorld world, int index) {
        float[] slotX = new float[]{560f, 660f, 760f, 860f, 610f, 810f};
        float[] slotY = new float[]{820f, 760f, 820f, 760f, 920f, 920f};
        for (CampStructure structure : world.structures) {
            if (Math.abs(structure.x - slotX[index]) < 1f && Math.abs(structure.y - slotY[index]) < 1f) {
                return structure;
            }
        }
        return null;
    }

    private void drawStructure(Canvas canvas, CampStructure structure, float cx, float cy) {
        Bitmap beacon = bitmaps.get("beacon");
        Bitmap flag = bitmaps.get("flag");
        paint.setStyle(Paint.Style.FILL);
        switch (structure.type) {
            case SHELTER:
                paint.setColor(0xFFE6BC7D);
                canvas.drawRoundRect(new RectF(cx - 26f, cy - 18f, cx + 26f, cy + 24f), 12f, 12f, paint);
                break;
            case FENCE:
                paint.setColor(0xFFC48E52);
                canvas.drawRect(cx - 30f, cy - 8f, cx + 30f, cy + 12f, paint);
                break;
            case COLLECTOR:
                paint.setColor(0xFF65B7D8);
                canvas.drawCircle(cx, cy, 22f, paint);
                break;
            case INFIRMARY:
                paint.setColor(0xFFF4EED8);
                canvas.drawRoundRect(new RectF(cx - 24f, cy - 20f, cx + 24f, cy + 20f), 10f, 10f, paint);
                paint.setColor(0xFFE35A64);
                canvas.drawRect(cx - 5f, cy - 16f, cx + 5f, cy + 16f, paint);
                canvas.drawRect(cx - 16f, cy - 5f, cx + 16f, cy + 5f, paint);
                break;
            case WORKSHOP:
                paint.setColor(0xFF8D7158);
                canvas.drawRoundRect(new RectF(cx - 30f, cy - 18f, cx + 30f, cy + 20f), 10f, 10f, paint);
                break;
            case BEACON:
                if (beacon != null) {
                    canvas.drawBitmap(beacon, null, new RectF(cx - 18f, cy - 50f, cx + 18f, cy + 26f), paint);
                }
                if (flag != null) {
                    canvas.drawBitmap(flag, null, new RectF(cx - 10f, cy - 56f, cx + 34f, cy - 6f), paint);
                }
                paint.setColor(0xFFFFA13E);
                canvas.drawCircle(cx, cy - 44f, 12f, paint);
                break;
        }
    }

    private void drawNodes(Canvas canvas, CastawayWorld world) {
        for (ResourceNode node : world.nodes) {
            if (!node.active) {
                continue;
            }
            float cx = node.x - world.camX;
            float cy = node.y - world.camY;
            paint.setStyle(Paint.Style.FILL);
            switch (node.type) {
                case WOOD:
                    paint.setColor(0xFFBD7D4B);
                    canvas.drawRoundRect(new RectF(cx - 24f, cy - 18f, cx + 24f, cy + 18f), 10f, 10f, paint);
                    break;
                case FOOD:
                    paint.setColor(0xFFF6C04A);
                    canvas.drawCircle(cx, cy, 22f, paint);
                    break;
                case WATER:
                    paint.setColor(0xFF38C9F2);
                    canvas.drawCircle(cx, cy, 24f, paint);
                    break;
                case HERB:
                    paint.setColor(0xFF4FC15B);
                    canvas.drawCircle(cx, cy, 18f, paint);
                    break;
                case SCRAP:
                    paint.setColor(0xFFB1A48F);
                    canvas.drawRect(cx - 20f, cy - 14f, cx + 20f, cy + 14f, paint);
                    break;
                case SURVIVOR:
                    paint.setColor(0xFF7B61FF);
                    canvas.drawCircle(cx, cy, 24f, paint);
                    paint.setColor(Color.WHITE);
                    canvas.drawCircle(cx, cy - 8f, 8f, paint);
                    canvas.drawRect(cx - 10f, cy + 2f, cx + 10f, cy + 20f, paint);
                    break;
            }
        }
    }

    private void drawWildlife(Canvas canvas, CastawayWorld world) {
        for (Wildlife animal : world.wildlife) {
            if (!animal.active) {
                continue;
            }
            float cx = animal.x - world.camX;
            float cy = animal.y - world.camY;
            Bitmap bitmap = null;
            switch (animal.type) {
                case BOAR:
                    bitmap = bitmaps.get("boar");
                    break;
                case MONKEY:
                    bitmap = bitmaps.get("monkey");
                    break;
                case SNAKE:
                    bitmap = bitmaps.get("snake");
                    break;
                case CRAB:
                    bitmap = bitmaps.get("crab");
                    break;
            }
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, new RectF(cx - 30f, cy - 30f, cx + 30f, cy + 30f), paint);
            } else {
                paint.setColor(0xFFD06A54);
                canvas.drawCircle(cx, cy, 24f, paint);
            }
        }
    }

    private void drawSurvivors(Canvas canvas, CastawayWorld world) {
        for (int i = 0; i < world.survivors.size(); i++) {
            Survivor survivor = world.survivors.get(i);
            float cx = survivor.x - world.camX;
            float cy = survivor.y - world.camY;
            Bitmap bitmap = i == 0 ? bitmaps.get("leader_idle") : bitmaps.get("survivor_idle");
            Bitmap move = i == 0 ? bitmaps.get("leader_move") : bitmaps.get("survivor_move");
            float activity = (float) Math.abs(Math.sin(survivor.animTime * 5f));
            Bitmap frame = activity > 0.5f ? move : bitmap;
            if (frame != null) {
                canvas.drawBitmap(frame, null, new RectF(cx - 26f, cy - 26f, cx + 26f, cy + 26f), paint);
            } else {
                paint.setColor(i == 0 ? 0xFF2F9EFF : 0xFF9B7CFF);
                canvas.drawCircle(cx, cy, 18f, paint);
            }
            paint.setColor(i == 0 ? 0xFF15304A : roleColor(survivor.role));
            canvas.drawRect(cx - 16f, cy + 20f, cx + 16f, cy + 28f, paint);
        }
    }

    private int roleColor(SurvivorRole role) {
        switch (role) {
            case GATHERER:
                return 0xFF6C9E44;
            case FISHER:
                return 0xFF2AA0C8;
            case BUILDER:
                return 0xFFB97845;
            case GUARD:
                return 0xFFE35A64;
            case HEALER:
                return 0xFF7B61FF;
            default:
                return 0xFF5A6C82;
        }
    }

    private void drawFloatTexts(Canvas canvas, CastawayWorld world) {
        paint.setTextSize(28f);
        for (FloatText item : world.floatTexts) {
            if (item.life > 0f) {
                paint.setColor(item.color);
                canvas.drawText(item.text, item.x - world.camX, item.y - world.camY, paint);
            }
        }
    }

    private void drawModeHints(Canvas canvas, CastawayWorld world, int width, int height) {
        paint.setColor(0xB2FFFFFF);
        rect.set(24f, height - 300f, width - 24f, height - 242f);
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        paint.setColor(0xFF244260);
        paint.setTextSize(28f);
        canvas.drawText(world.debugRegionLabel(world.playerX, world.playerY), 44f, height - 264f, paint);
        if (world.mapMode) {
            paint.setColor(0xFF7B61FF);
            canvas.drawText("Map focus active", width - 260f, height - 264f, paint);
        }
    }

    private void drawControls(Canvas canvas, int width, int height, float stickX, float stickY, boolean actionDown) {
        float baseX = 150f;
        float baseY = height - 140f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55FFFFFF);
        canvas.drawCircle(baseX, baseY, 92f, paint);
        paint.setColor(0xAA2ED6FF);
        canvas.drawCircle(baseX + stickX * 44f, baseY + stickY * 44f, 38f, paint);
        float actionX = width - 132f;
        float actionY = height - 150f;
        paint.setColor(actionDown ? 0xFFFFB24A : 0xAA7B61FF);
        canvas.drawCircle(actionX, actionY, 62f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(26f);
        canvas.drawText("Act", actionX - 22f, actionY + 8f, paint);
    }

    private void drawStatePanel(Canvas canvas, CastawayWorld world, int width, int height) {
        if (world.state == CastawayState.PLAYING) {
            return;
        }
        paint.setColor(0xCCFFF6E4);
        rect.set(width * 0.12f, height * 0.22f, width * 0.88f, height * 0.54f);
        canvas.drawRoundRect(rect, 28f, 28f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFB98A58);
        paint.setStrokeWidth(5f);
        canvas.drawRoundRect(rect, 28f, 28f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF17304A);
        paint.setTextSize(44f);
        if (world.state == CastawayState.MENU) {
            canvas.drawText("Castaway Clan", rect.left + 42f, rect.top + 82f, paint);
            paint.setTextSize(30f);
            canvas.drawText("Lead a stranded camp from first shelter to rescue beacon", rect.left + 42f, rect.top + 132f, paint);
        } else if (world.state == CastawayState.PAUSED) {
            canvas.drawText("Camp Paused", rect.left + 42f, rect.top + 82f, paint);
            paint.setTextSize(30f);
            canvas.drawText("Resume when the next shift is ready", rect.left + 42f, rect.top + 132f, paint);
        } else if (world.state == CastawayState.GAME_OVER) {
            canvas.drawText("Camp Lost", rect.left + 42f, rect.top + 82f, paint);
            paint.setTextSize(30f);
            canvas.drawText("Morale or supplies collapsed before rescue", rect.left + 42f, rect.top + 132f, paint);
        } else if (world.state == CastawayState.VICTORY) {
            canvas.drawText("Rescue Secured", rect.left + 42f, rect.top + 82f, paint);
            paint.setTextSize(30f);
            canvas.drawText("The beacon held through the final storm", rect.left + 42f, rect.top + 132f, paint);
        }
    }
}
