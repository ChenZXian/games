package com.android.boot.entity;

import com.android.boot.core.CampStructureType;

public class CampStructure {
    public final CampStructureType type;
    public final float x;
    public final float y;
    public boolean built;

    public CampStructure(CampStructureType type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.built = true;
    }
}
