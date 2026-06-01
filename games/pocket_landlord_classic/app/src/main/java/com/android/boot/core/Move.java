package com.android.boot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Move implements Comparable<Move> {
    public enum Type {
        PASS,
        SINGLE,
        PAIR,
        TRIPLE,
        TRIPLE_SINGLE,
        TRIPLE_PAIR,
        STRAIGHT,
        PAIR_STRAIGHT,
        PLANE,
        PLANE_SINGLE,
        PLANE_PAIR,
        FOUR_TWO_SINGLE,
        FOUR_TWO_PAIR,
        BOMB,
        ROCKET
    }

    public final Type type;
    public final List<Card> cards;
    public final int mainRank;
    public final int chainLength;

    public Move(Type type, List<Card> cards, int mainRank, int chainLength) {
        this.type = type;
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);
        this.cards = Collections.unmodifiableList(sorted);
        this.mainRank = mainRank;
        this.chainLength = chainLength;
    }

    public static Move pass() {
        return new Move(Type.PASS, Collections.<Card>emptyList(), 0, 0);
    }

    public boolean isPass() {
        return type == Type.PASS;
    }

    public boolean isBombLike() {
        return type == Type.BOMB || type == Type.ROCKET;
    }

    public String label() {
        switch (type) {
            case SINGLE:
                return "Single";
            case PAIR:
                return "Pair";
            case TRIPLE:
                return "Triple";
            case TRIPLE_SINGLE:
                return "Triple With Single";
            case TRIPLE_PAIR:
                return "Triple With Pair";
            case STRAIGHT:
                return "Straight";
            case PAIR_STRAIGHT:
                return "Pair Straight";
            case PLANE:
                return "Plane";
            case PLANE_SINGLE:
                return "Plane With Singles";
            case PLANE_PAIR:
                return "Plane With Pairs";
            case FOUR_TWO_SINGLE:
                return "Four With Two";
            case FOUR_TWO_PAIR:
                return "Four With Two Pairs";
            case BOMB:
                return "Bomb";
            case ROCKET:
                return "Rocket";
            default:
                return "Pass";
        }
    }

    @Override
    public int compareTo(Move other) {
        if (type != other.type) {
            return type.ordinal() - other.type.ordinal();
        }
        if (chainLength != other.chainLength) {
            return chainLength - other.chainLength;
        }
        if (mainRank != other.mainRank) {
            return mainRank - other.mainRank;
        }
        return cards.size() - other.cards.size();
    }
}
