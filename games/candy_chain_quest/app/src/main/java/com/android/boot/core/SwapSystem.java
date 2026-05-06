package com.android.boot.core;

import com.android.boot.entity.Position;
import com.android.boot.entity.SwapData;

public class SwapSystem {
    private SwapData pendingSwap;

    public boolean beginSwap(BoardManager boardManager, Position first, Position second) {
        if (boardManager.trySwap(first, second)) {
            pendingSwap = new SwapData(first, second);
            return true;
        }
        return false;
    }

    public SwapData getPendingSwap() {
        return pendingSwap;
    }

    public void clearPendingSwap() {
        pendingSwap = null;
    }
}
