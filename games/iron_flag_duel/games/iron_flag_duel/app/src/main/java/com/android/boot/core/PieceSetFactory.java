package com.android.boot.core;

import com.android.boot.entity.Piece;
import com.android.boot.entity.PieceType;
import com.android.boot.entity.Side;

import java.util.ArrayList;
import java.util.List;

public final class PieceSetFactory {
    private PieceSetFactory() {
    }

    public static List<Piece> createArmy(Side side) {
        List<Piece> pieces = new ArrayList<>();
        int id = side == Side.PLAYER ? 1000 : 2000;
        add(pieces, side, id, PieceType.FLAG, 1);
        add(pieces, side, id + 10, PieceType.MINE, 3);
        add(pieces, side, id + 20, PieceType.BOMB, 2);
        add(pieces, side, id + 30, PieceType.ENGINEER, 3);
        add(pieces, side, id + 40, PieceType.PLATOON, 3);
        add(pieces, side, id + 50, PieceType.COMPANY, 3);
        add(pieces, side, id + 60, PieceType.BATTALION, 2);
        add(pieces, side, id + 70, PieceType.REGIMENT, 2);
        add(pieces, side, id + 80, PieceType.BRIGADE, 2);
        add(pieces, side, id + 90, PieceType.DIVISION, 2);
        add(pieces, side, id + 100, PieceType.CORPS, 1);
        add(pieces, side, id + 110, PieceType.COMMANDER, 1);
        return pieces;
    }

    private static void add(List<Piece> pieces, Side side, int baseId, PieceType type, int count) {
        for (int i = 0; i < count; i++) {
            pieces.add(new Piece(baseId + i, side, type));
        }
    }
}
