package com.android.boot.core;

import com.android.boot.entity.VisualEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EffectManager {
    private final List<VisualEffect> effects = new ArrayList<>();

    public void clear() {
        effects.clear();
    }

    public void add(VisualEffect effect) {
        effects.add(effect);
    }

    public void update(float delta) {
        Iterator<VisualEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            VisualEffect effect = iterator.next();
            effect.life -= delta;
            if (effect.life <= 0f) {
                iterator.remove();
            }
        }
    }

    public List<VisualEffect> getEffects() {
        return effects;
    }
}
