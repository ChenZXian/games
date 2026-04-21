package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.RectF;

public abstract class GameEntity {
    public final RectF bounds = new RectF();
    public boolean active = true;

    public abstract void update(float dt);

    public abstract void render(Canvas canvas);
}
