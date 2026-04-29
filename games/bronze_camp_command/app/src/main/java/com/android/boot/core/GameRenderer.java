package com.android.boot.core;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Map<String, Bitmap> art = new HashMap<>();

    public GameRenderer() {
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
    }

    public void ensureAssets(AssetManager assets) {
        if (!art.isEmpty()) {
            return;
        }
        load(assets, "terrain_grass", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile024.png");
        load(assets, "terrain_stone", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile130.png");
        load(assets, "building", "game_art/kenney_tower_defense_top_down/assets/PNG/Default size/towerDefense_tile249.png");
        load(assets, "unit", "game_art/kenney_top_down_shooter/assets/PNG/Survivor 1/survivor1_stand.png");
        load(assets, "enemy", "game_art/kenney_top_down_shooter/assets/PNG/Zombie 1/zoimbie1_stand.png");
    }

    public void render(Canvas canvas, BronzeWorld world, int width, int height) {
        if (width <= 0 || height <= 0) {
            width = canvas.getWidth();
            height = canvas.getHeight();
        }
        float sx = width / 1280f;
        float sy = height / 720f;
        canvas.drawColor(Color.rgb(18, 28, 24));
        drawMap(canvas, sx, sy);
        drawResources(canvas, world, sx, sy);
        drawObelisks(canvas, world, sx, sy);
        drawBuildings(canvas, world, sx, sy);
        drawUnits(canvas, world, sx, sy);
        drawEffects(canvas, world, sx, sy);
        drawFogAndZones(canvas, world, sx, sy);
        if (world.dragActive) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(190, 126, 235, 205));
            canvas.drawRect(world.dragLeft, world.dragTop, world.dragRight, world.dragBottom, paint);
        }
    }

    private void drawMap(Canvas canvas, float sx, float sy) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(49, 68, 48));
        canvas.drawRect(0f, 0f, 1280f * sx, 720f * sy, paint);
        paint.setColor(Color.rgb(74, 82, 55));
        rect.set(66f * sx, 145f * sy, 520f * sx, 640f * sy);
        canvas.drawRoundRect(rect, 28f * sx, 28f * sy, paint);
        paint.setColor(Color.rgb(61, 58, 46));
        rect.set(760f * sx, 112f * sy, 1200f * sx, 610f * sy);
        canvas.drawRoundRect(rect, 32f * sx, 32f * sy, paint);
        paint.setColor(Color.rgb(92, 78, 49));
        for (int i = 0; i < 4; i++) {
            float y = (210f + i * 105f) * sy;
            rect.set(420f * sx, y - 13f * sy, 840f * sx, y + 13f * sy);
            canvas.drawRoundRect(rect, 18f * sx, 18f * sy, paint);
        }
        paint.setColor(Color.rgb(44, 52, 45));
        for (int i = 0; i < 7; i++) {
            float x = (500f + i * 35f) * sx;
            rect.set(x, 80f * sy, x + 28f * sx, 160f * sy);
            canvas.drawRoundRect(rect, 14f * sx, 14f * sy, paint);
        }
    }

    private void drawResources(Canvas canvas, BronzeWorld world, float sx, float sy) {
        for (BronzeWorld.ResourceNode node : world.resources) {
            int color;
            if (node.type == BronzeWorld.ResourceType.FOOD) {
                color = Color.rgb(219, 176, 76);
            } else if (node.type == BronzeWorld.ResourceType.WOOD) {
                color = Color.rgb(79, 111, 65);
            } else {
                color = Color.rgb(142, 142, 130);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(node.x * sx, node.y * sy, 28f * sx, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f * sx);
            paint.setColor(Color.argb(180, 232, 255, 248));
            canvas.drawCircle(node.x * sx, node.y * sy, 31f * sx, paint);
        }
    }

    private void drawObelisks(Canvas canvas, BronzeWorld world, float sx, float sy) {
        for (BronzeWorld.Obelisk obelisk : world.obelisks) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(108, 99, 81));
            rect.set((obelisk.x - 22f) * sx, (obelisk.y - 50f) * sy, (obelisk.x + 22f) * sx, (obelisk.y + 42f) * sy);
            canvas.drawRoundRect(rect, 10f * sx, 10f * sy, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f * sx);
            paint.setColor(obelisk.control >= 0f ? Color.rgb(79, 227, 193) : Color.rgb(255, 90, 95));
            canvas.drawCircle(obelisk.x * sx, obelisk.y * sy, (48f + Math.abs(obelisk.control) * 18f) * sx, paint);
        }
    }

    private void drawBuildings(Canvas canvas, BronzeWorld world, float sx, float sy) {
        for (BronzeWorld.Building building : world.buildings) {
            float w = building.type.w * sx;
            float h = building.type.h * sy;
            rect.set(building.x * sx - w * 0.5f, building.y * sy - h * 0.5f, building.x * sx + w * 0.5f, building.y * sy + h * 0.5f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(building.player ? Color.rgb(109, 94, 53) : Color.rgb(111, 51, 48));
            Bitmap bitmap = art.get("building");
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, rect, paint);
                paint.setColor(building.player ? Color.argb(88, 79, 227, 193) : Color.argb(95, 255, 90, 95));
                canvas.drawRoundRect(rect, 9f * sx, 9f * sy, paint);
            } else {
                canvas.drawRoundRect(rect, 10f * sx, 10f * sy, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(building.selected ? 5f * sx : 2f * sx);
            paint.setColor(building.player ? Color.rgb(79, 227, 193) : Color.rgb(255, 209, 102));
            canvas.drawRoundRect(rect, 10f * sx, 10f * sy, paint);
            drawHealth(canvas, building.x, building.y - building.type.h * 0.62f, building.hp, building.type.hp, sx, sy);
            drawLabel(canvas, building.type.shortLabel, building.x, building.y + building.type.h * 0.62f, sx, sy);
        }
    }

    private void drawUnits(Canvas canvas, BronzeWorld world, float sx, float sy) {
        for (BronzeWorld.Unit unit : world.units) {
            if (!unit.alive) {
                continue;
            }
            float bob = ((int) unit.walkPhase % 2 == 0) ? -2f : 2f;
            rect.set((unit.x - 17f) * sx, (unit.y - 24f + bob) * sy, (unit.x + 17f) * sx, (unit.y + 21f + bob) * sy);
            Bitmap bitmap = art.get(unit.player ? "unit" : "enemy");
            if (bitmap != null) {
                canvas.save();
                if (unit.facing < 0) {
                    canvas.scale(-1f, 1f, unit.x * sx, unit.y * sy);
                }
                canvas.drawBitmap(bitmap, null, rect, paint);
                canvas.restore();
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(unit.player ? Color.rgb(79, 227, 193) : Color.rgb(255, 90, 95));
                canvas.drawOval(rect, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit.selected ? 4f * sx : 2f * sx);
            paint.setColor(unit.selected ? Color.rgb(255, 209, 102) : Color.argb(120, 232, 255, 248));
            canvas.drawCircle(unit.x * sx, (unit.y + 20f) * sy, 20f * sx, paint);
            if (unit.type.range > 60f) {
                paint.setColor(unit.player ? Color.argb(90, 79, 227, 193) : Color.argb(90, 255, 90, 95));
                canvas.drawCircle(unit.x * sx, unit.y * sy, 30f * sx, paint);
            }
            drawHealth(canvas, unit.x, unit.y - 34f, unit.hp, unit.type.hp, sx, sy);
        }
    }

    private void drawEffects(Canvas canvas, BronzeWorld world, float sx, float sy) {
        textPaint.setTextSize(14f * sx);
        textPaint.setColor(Color.rgb(255, 209, 102));
        for (BronzeWorld.Effect effect : world.effects) {
            textPaint.setAlpha(Math.max(20, Math.min(255, (int) (effect.life * 255f))));
            canvas.drawText(effect.text, effect.x * sx, effect.y * sy, textPaint);
        }
        textPaint.setAlpha(255);
    }

    private void drawFogAndZones(Canvas canvas, BronzeWorld world, float sx, float sy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * sx);
        paint.setColor(Color.argb(80, 79, 227, 193));
        canvas.drawCircle(150f * sx, 350f * sy, 310f * sx, paint);
        if (world.matchTime < 45f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(72, 5, 8, 8));
            rect.set(820f * sx, 75f * sy, 1228f * sx, 640f * sy);
            canvas.drawRoundRect(rect, 28f * sx, 28f * sy, paint);
        }
    }

    private void drawHealth(Canvas canvas, float wx, float wy, int hp, int maxHp, float sx, float sy) {
        float pct = Math.max(0f, Math.min(1f, hp / (float) maxHp));
        rect.set((wx - 24f) * sx, (wy - 4f) * sy, (wx + 24f) * sx, (wy + 2f) * sy);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(39, 65, 58));
        canvas.drawRect(rect, paint);
        rect.right = rect.left + 48f * sx * pct;
        paint.setColor(pct < 0.35f ? Color.rgb(255, 90, 95) : Color.rgb(79, 227, 138));
        canvas.drawRect(rect, paint);
    }

    private void drawLabel(Canvas canvas, String text, float wx, float wy, float sx, float sy) {
        textPaint.setTextSize(13f * sx);
        textPaint.setColor(Color.rgb(232, 255, 248));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, wx * sx, wy * sy, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void load(AssetManager assets, String key, String path) {
        try {
            InputStream input = assets.open(path);
            art.put(key, BitmapFactory.decodeStream(input));
            input.close();
        } catch (Exception ignored) {
        }
    }
}
