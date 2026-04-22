package com.android.boot.inventory;

import com.android.boot.model.ItemDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InventoryManager {
    private final List<ItemDefinition> items = new ArrayList<>();
    private final int capacity = 120;

    public boolean add(ItemDefinition item) {
        if (items.size() >= capacity) {
            return false;
        }
        items.add(item);
        return true;
    }

    public List<ItemDefinition> items() {
        return items;
    }

    public void sortByPower() {
        items.sort(Comparator.comparingInt(i -> -i.power));
    }

    public void sortBySlot() {
        items.sort(Comparator.comparing(i -> i.slot.name()));
    }

    public void sortByRarity() {
        items.sort(Comparator.comparing(i -> i.rarity.name()));
        Collections.reverse(items);
    }
}
