package com.android.boot.entity;

import java.util.ArrayList;
import java.util.List;

public class MatchGroup {
    private final List<Position> positions = new ArrayList<>();
    private boolean horizontal;
    private boolean vertical;

    public void add(Position position) {
        positions.add(position);
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public boolean isVertical() {
        return vertical;
    }

    public int size() {
        return positions.size();
    }

    public boolean isCrossLike() {
        return horizontal && vertical;
    }
}
