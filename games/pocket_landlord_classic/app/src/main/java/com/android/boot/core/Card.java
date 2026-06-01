package com.android.boot.core;

import java.util.Objects;

public class Card implements Comparable<Card> {
    public static final int SUIT_CLUB = 0;
    public static final int SUIT_DIAMOND = 1;
    public static final int SUIT_HEART = 2;
    public static final int SUIT_SPADE = 3;
    public static final int SUIT_JOKER = 4;
    public static final int RANK_SMALL_JOKER = 16;
    public static final int RANK_BIG_JOKER = 17;

    public final int id;
    public final int rank;
    public final int suit;

    public Card(int id, int rank, int suit) {
        this.id = id;
        this.rank = rank;
        this.suit = suit;
    }

    public boolean isRed() {
        return suit == SUIT_DIAMOND || suit == SUIT_HEART || rank == RANK_BIG_JOKER;
    }

    public String rankLabel() {
        if (rank <= 10) {
            return String.valueOf(rank);
        }
        switch (rank) {
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 14:
                return "A";
            case 15:
                return "2";
            case RANK_SMALL_JOKER:
                return "SJ";
            case RANK_BIG_JOKER:
                return "BJ";
            default:
                return "?";
        }
    }

    public String suitLabel() {
        switch (suit) {
            case SUIT_CLUB:
                return "C";
            case SUIT_DIAMOND:
                return "D";
            case SUIT_HEART:
                return "H";
            case SUIT_SPADE:
                return "S";
            default:
                return "";
        }
    }

    @Override
    public int compareTo(Card other) {
        if (rank != other.rank) {
            return rank - other.rank;
        }
        return suit - other.suit;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Card)) {
            return false;
        }
        return id == ((Card) obj).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
