package com.android.boot.system;

import com.android.boot.entity.CookingStep;

public class CookingStepSystem {
    public float progressValue(int current, CookingStep step) {
        if (step == null || step.targetTaps <= 0) return 1f;
        float value = (float) current / (float) step.targetTaps;
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    public boolean isFinalStep(int index, int size) {
        return index >= size - 1;
    }
}
