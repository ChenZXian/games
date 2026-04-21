package com.android.boot.core;

import android.graphics.RectF;

public final class CollisionHelper {
    private CollisionHelper() {
    }

    public static boolean overlaps(RectF a, RectF b) {
        return RectF.intersects(a, b);
    }
}
