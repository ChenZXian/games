package com.android.boot.fx;

public final class UiAnim {
    private UiAnim() {
    }

    public static float approach(float value, float target, float delta, float speed) {
        if (value < target) {
            value += speed * delta;
            if (value > target) {
                value = target;
            }
        } else if (value > target) {
            value -= speed * delta;
            if (value < target) {
                value = target;
            }
        }
        return value;
    }
}
