package com.android.boot.inventory;

import com.android.boot.model.Enums.SlotType;
import com.android.boot.model.ItemDefinition;
import java.util.EnumMap;

public class EquipmentManager {
    private final EnumMap<SlotType, ItemDefinition> equipped = new EnumMap<>(SlotType.class);

    public ItemDefinition get(SlotType slot) {
        return equipped.get(slot);
    }

    public void equip(ItemDefinition item) {
        equipped.put(item.slot, item);
    }

    public void clear(SlotType slot) {
        equipped.remove(slot);
    }

    public EnumMap<SlotType, ItemDefinition> all() {
        return equipped;
    }
}
