package com.android.boot.core;

import com.android.boot.entity.MatchGroup;

import java.util.List;

public class MatchDetector {
    public List<MatchGroup> detect(BoardManager boardManager) {
        return boardManager.findMatches();
    }
}
