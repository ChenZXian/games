package com.android.boot.core;

import com.android.boot.entity.Position;

public class InputManager {
    private Position selected;

    public Position handleTap(int row, int col, GameStateMachine stateMachine) {
        if (!stateMachine.acceptsInput()) {
            return null;
        }
        Position tapped = new Position(row, col);
        if (selected == null) {
            selected = tapped;
            return null;
        }
        if (selected.equals(tapped)) {
            selected = null;
            return null;
        }
        if (selected.isAdjacent(tapped)) {
            Position origin = selected;
            selected = null;
            return origin;
        }
        selected = tapped;
        return null;
    }

    public Position getSelected() {
        return selected;
    }

    public void clear() {
        selected = null;
    }
}
