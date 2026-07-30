package com.android.boot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LandlordAi {
    private LandlordAi() {
    }

    public static int chooseBid(List<Card> hand, int highestBid, int aiLevel) {
        int score = evaluateHand(hand);
        int bid = 0;
        if (score > 23) {
            bid = 1;
        }
        if (score > 31 || aiLevel >= 5) {
            bid = 2;
        }
        if (score > 39 || (score > 34 && aiLevel >= 7)) {
            bid = 3;
        }
        if (bid <= highestBid) {
            return 0;
        }
        return bid;
    }

    public static Move chooseMove(List<Card> hand, Move previous, boolean defendingTeammate, int aiLevel) {
        List<Move> legal = RuleEngine.legalMoves(hand, previous);
        if (previous != null && !previous.isPass() && defendingTeammate && legal.isEmpty()) {
            return Move.pass();
        }
        if (legal.isEmpty()) {
            return Move.pass();
        }
        if (previous == null || previous.isPass()) {
            return chooseLeadMove(legal, aiLevel);
        }
        return chooseResponseMove(legal, aiLevel);
    }

    private static Move chooseLeadMove(List<Move> legal, int aiLevel) {
        List<Move> sorted = new ArrayList<>(legal);
        Collections.sort(sorted);
        for (Move move : sorted) {
            if (move.type == Move.Type.STRAIGHT || move.type == Move.Type.PAIR_STRAIGHT || move.type == Move.Type.TRIPLE_PAIR) {
                return move;
            }
        }
        for (Move move : sorted) {
            if (!move.isBombLike()) {
                return move;
            }
        }
        return sorted.get(0);
    }

    private static Move chooseResponseMove(List<Move> legal, int aiLevel) {
        List<Move> sorted = new ArrayList<>(legal);
        Collections.sort(sorted);
        for (Move move : sorted) {
            if (!move.isBombLike()) {
                return move;
            }
        }
        if (aiLevel >= 6) {
            return sorted.get(0);
        }
        return Move.pass();
    }

    private static int evaluateHand(List<Card> hand) {
        int score = 0;
        int pairs = 0;
        int triples = 0;
        int bombs = 0;
        int jokers = 0;
        List<Card> cards = new ArrayList<>(hand);
        Collections.sort(cards);
        for (Card card : cards) {
            if (card.rank >= 15) {
                score += 3;
            }
            if (card.rank == Card.RANK_SMALL_JOKER || card.rank == Card.RANK_BIG_JOKER) {
                jokers++;
                score += 5;
            }
        }
        List<Move> moves = RuleEngine.generateAllMoves(cards);
        for (Move move : moves) {
            if (move.type == Move.Type.PAIR) {
                pairs++;
            } else if (move.type == Move.Type.TRIPLE) {
                triples++;
            } else if (move.type == Move.Type.BOMB) {
                bombs++;
            }
        }
        score += pairs / 2;
        score += triples * 4;
        score += bombs * 8;
        if (jokers == 2) {
            score += 7;
        }
        return score;
    }
}
