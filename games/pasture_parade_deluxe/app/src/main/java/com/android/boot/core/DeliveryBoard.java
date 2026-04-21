package com.android.boot.core;

import com.android.boot.entity.AnimalSpecies;
import com.android.boot.entity.DeliveryOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeliveryBoard {
    private final Random random = new Random();
    public final List<DeliveryOrder> active = new ArrayList<>();

    public void seed(int ranchLevel) {
        while (active.size() < 3) {
            active.add(generate(ranchLevel));
        }
    }

    public DeliveryOrder generate(int level) {
        AnimalSpecies[] list = AnimalSpecies.values();
        int max = Math.min(list.length - 1, Math.max(3, level + 1));
        AnimalSpecies species = list[random.nextInt(max + 1)];
        int qty = 1 + random.nextInt(2 + level / 3);
        boolean urgent = random.nextFloat() < 0.25f;
        float urgency = urgent ? 1.4f : 1f;
        float streak = 1f + random.nextFloat() * 0.6f;
        int coins = Math.round((species.baseValue * qty) * urgency * streak);
        int xp = Math.max(4, qty * 3 + species.ordinal());
        return new DeliveryOrder(species, qty, coins, xp, streak, urgency, urgent);
    }

    public void replace(int index, int level) {
        active.set(index, generate(level));
    }
}
