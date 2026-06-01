package com.android.boot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuleEngine {
    private RuleEngine() {
    }

    public static Move identify(List<Card> source) {
        if (source == null || source.isEmpty()) {
            return Move.pass();
        }
        List<Card> cards = new ArrayList<>(source);
        Collections.sort(cards);
        Map<Integer, List<Card>> groups = groupByRank(cards);
        int size = cards.size();
        if (size == 1) {
            return new Move(Move.Type.SINGLE, cards, cards.get(0).rank, 1);
        }
        if (size == 2) {
            if (cards.get(0).rank == Card.RANK_SMALL_JOKER && cards.get(1).rank == Card.RANK_BIG_JOKER) {
                return new Move(Move.Type.ROCKET, cards, Card.RANK_BIG_JOKER, 1);
            }
            if (groups.size() == 1) {
                return new Move(Move.Type.PAIR, cards, cards.get(0).rank, 1);
            }
            return null;
        }
        if (groups.size() == 1) {
            if (size == 3) {
                return new Move(Move.Type.TRIPLE, cards, cards.get(0).rank, 1);
            }
            if (size == 4) {
                return new Move(Move.Type.BOMB, cards, cards.get(0).rank, 1);
            }
        }
        if (size == 4 && groups.size() == 2 && containsCount(groups, 3)) {
            return new Move(Move.Type.TRIPLE_SINGLE, cards, highestRankWithCount(groups, 3), 1);
        }
        if (size == 5 && groups.size() == 2 && containsCount(groups, 3) && containsCount(groups, 2)) {
            return new Move(Move.Type.TRIPLE_PAIR, cards, highestRankWithCount(groups, 3), 1);
        }
        if (isStraight(groups, 1, size, 5)) {
            return new Move(Move.Type.STRAIGHT, cards, highestRank(groups), size);
        }
        if (size % 2 == 0 && isStraight(groups, 2, size / 2, 3)) {
            return new Move(Move.Type.PAIR_STRAIGHT, cards, highestRank(groups), size / 2);
        }
        List<Integer> tripleRun = getTripleRun(groups);
        if (!tripleRun.isEmpty()) {
            int runLength = tripleRun.size();
            if (size == runLength * 3) {
                return new Move(Move.Type.PLANE, cards, tripleRun.get(runLength - 1), runLength);
            }
            if (size == runLength * 4 && countRemainingSingles(groups, tripleRun) == runLength) {
                return new Move(Move.Type.PLANE_SINGLE, cards, tripleRun.get(runLength - 1), runLength);
            }
            if (size == runLength * 5 && countRemainingPairs(groups, tripleRun) == runLength) {
                return new Move(Move.Type.PLANE_PAIR, cards, tripleRun.get(runLength - 1), runLength);
            }
        }
        if (size == 6 && containsCount(groups, 4)) {
            return new Move(Move.Type.FOUR_TWO_SINGLE, cards, highestRankWithCount(groups, 4), 1);
        }
        if (size == 8 && containsCount(groups, 4)) {
            int fourRank = highestRankWithCount(groups, 4);
            int pairCount = 0;
            for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
                if (entry.getKey() == fourRank) {
                    continue;
                }
                if (entry.getValue().size() >= 2) {
                    pairCount++;
                }
            }
            if (pairCount >= 2) {
                return new Move(Move.Type.FOUR_TWO_PAIR, cards, fourRank, 1);
            }
        }
        return null;
    }

    public static boolean canBeat(Move candidate, Move previous) {
        if (candidate == null || candidate.isPass()) {
            return false;
        }
        if (previous == null || previous.isPass()) {
            return true;
        }
        if (candidate.type == Move.Type.ROCKET) {
            return true;
        }
        if (previous.type == Move.Type.ROCKET) {
            return false;
        }
        if (candidate.type == Move.Type.BOMB && previous.type != Move.Type.BOMB) {
            return true;
        }
        if (candidate.type != previous.type) {
            return false;
        }
        if (candidate.chainLength != previous.chainLength) {
            return false;
        }
        return candidate.mainRank > previous.mainRank;
    }

    public static List<Move> legalMoves(List<Card> hand, Move previous) {
        List<Move> all = generateAllMoves(hand);
        List<Move> legal = new ArrayList<>();
        if (previous == null || previous.isPass()) {
            legal.addAll(all);
        } else {
            for (Move move : all) {
                if (canBeat(move, previous)) {
                    legal.add(move);
                }
            }
        }
        Collections.sort(legal);
        return legal;
    }

    public static List<Move> generateAllMoves(List<Card> hand) {
        List<Card> cards = new ArrayList<>(hand);
        Collections.sort(cards);
        Map<Integer, List<Card>> groups = groupByRank(cards);
        List<Move> moves = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Card card : cards) {
            addMove(moves, seen, new Move(Move.Type.SINGLE, Arrays.asList(card), card.rank, 1));
        }
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            List<Card> same = entry.getValue();
            if (same.size() >= 2) {
                addMove(moves, seen, new Move(Move.Type.PAIR, same.subList(0, 2), entry.getKey(), 1));
            }
            if (same.size() >= 3) {
                List<Card> triple = same.subList(0, 3);
                addMove(moves, seen, new Move(Move.Type.TRIPLE, triple, entry.getKey(), 1));
                for (Card single : cards) {
                    if (single.rank != entry.getKey()) {
                        addMove(moves, seen, new Move(Move.Type.TRIPLE_SINGLE, join(triple, Arrays.asList(single)), entry.getKey(), 1));
                    }
                }
                for (Map.Entry<Integer, List<Card>> pairEntry : groups.entrySet()) {
                    if (pairEntry.getKey() != entry.getKey() && pairEntry.getValue().size() >= 2) {
                        addMove(moves, seen, new Move(Move.Type.TRIPLE_PAIR, join(triple, pairEntry.getValue().subList(0, 2)), entry.getKey(), 1));
                    }
                }
            }
            if (same.size() == 4) {
                addMove(moves, seen, new Move(Move.Type.BOMB, same, entry.getKey(), 1));
                List<Card> remainingSingles = remainingCards(cards, entry.getKey(), 1);
                for (int i = 0; i < remainingSingles.size(); i++) {
                    for (int j = i + 1; j < remainingSingles.size(); j++) {
                        addMove(moves, seen, new Move(Move.Type.FOUR_TWO_SINGLE, join(same, Arrays.asList(remainingSingles.get(i), remainingSingles.get(j))), entry.getKey(), 1));
                    }
                }
                List<List<Card>> pairOptions = remainingPairs(groups, entry.getKey());
                for (int i = 0; i < pairOptions.size(); i++) {
                    for (int j = i + 1; j < pairOptions.size(); j++) {
                        addMove(moves, seen, new Move(Move.Type.FOUR_TWO_PAIR, join(same, join(pairOptions.get(i), pairOptions.get(j))), entry.getKey(), 1));
                    }
                }
            }
        }
        if (groups.containsKey(Card.RANK_SMALL_JOKER) && groups.containsKey(Card.RANK_BIG_JOKER)) {
            addMove(moves, seen, new Move(Move.Type.ROCKET, Arrays.asList(groups.get(Card.RANK_SMALL_JOKER).get(0), groups.get(Card.RANK_BIG_JOKER).get(0)), Card.RANK_BIG_JOKER, 1));
        }
        addStraights(cards, groups, moves, seen);
        addPairStraights(groups, moves, seen);
        addPlanes(cards, groups, moves, seen);
        Collections.sort(moves);
        return moves;
    }

    private static void addStraights(List<Card> cards, Map<Integer, List<Card>> groups, List<Move> moves, Set<String> seen) {
        List<Integer> ranks = eligibleRanks(groups, 1);
        for (int start = 0; start < ranks.size(); start++) {
            int end = start;
            while (end + 1 < ranks.size() && ranks.get(end + 1) == ranks.get(end) + 1) {
                end++;
            }
            int run = end - start + 1;
            if (run >= 5) {
                for (int length = 5; length <= run; length++) {
                    for (int offset = start; offset + length - 1 <= end; offset++) {
                        List<Card> pick = new ArrayList<>();
                        for (int i = 0; i < length; i++) {
                            pick.add(groups.get(ranks.get(offset + i)).get(0));
                        }
                        addMove(moves, seen, new Move(Move.Type.STRAIGHT, pick, ranks.get(offset + length - 1), length));
                    }
                }
            }
            start = end;
        }
    }

    private static void addPairStraights(Map<Integer, List<Card>> groups, List<Move> moves, Set<String> seen) {
        List<Integer> ranks = eligibleRanks(groups, 2);
        for (int start = 0; start < ranks.size(); start++) {
            int end = start;
            while (end + 1 < ranks.size() && ranks.get(end + 1) == ranks.get(end) + 1) {
                end++;
            }
            int run = end - start + 1;
            if (run >= 3) {
                for (int length = 3; length <= run; length++) {
                    for (int offset = start; offset + length - 1 <= end; offset++) {
                        List<Card> pick = new ArrayList<>();
                        for (int i = 0; i < length; i++) {
                            pick.add(groups.get(ranks.get(offset + i)).get(0));
                            pick.add(groups.get(ranks.get(offset + i)).get(1));
                        }
                        addMove(moves, seen, new Move(Move.Type.PAIR_STRAIGHT, pick, ranks.get(offset + length - 1), length));
                    }
                }
            }
            start = end;
        }
    }

    private static void addPlanes(List<Card> cards, Map<Integer, List<Card>> groups, List<Move> moves, Set<String> seen) {
        List<Integer> tripleRanks = eligibleRanks(groups, 3);
        for (int start = 0; start < tripleRanks.size(); start++) {
            int end = start;
            while (end + 1 < tripleRanks.size() && tripleRanks.get(end + 1) == tripleRanks.get(end) + 1) {
                end++;
            }
            int run = end - start + 1;
            if (run >= 2) {
                for (int length = 2; length <= run; length++) {
                    for (int offset = start; offset + length - 1 <= end; offset++) {
                        List<Integer> ranks = tripleRanks.subList(offset, offset + length);
                        List<Card> tripleCards = new ArrayList<>();
                        for (Integer rank : ranks) {
                            tripleCards.addAll(groups.get(rank).subList(0, 3));
                        }
                        addMove(moves, seen, new Move(Move.Type.PLANE, tripleCards, ranks.get(ranks.size() - 1), length));
                        List<Card> singles = removeRanks(cards, ranks, 3);
                        if (singles.size() >= length) {
                            combineSinglesForPlane(moves, seen, tripleCards, singles, ranks.get(ranks.size() - 1), length);
                        }
                        List<List<Card>> pairs = remainingPairs(groups, ranks);
                        if (pairs.size() >= length) {
                            combinePairsForPlane(moves, seen, tripleCards, pairs, ranks.get(ranks.size() - 1), length);
                        }
                    }
                }
            }
            start = end;
        }
    }

    private static void combineSinglesForPlane(List<Move> moves, Set<String> seen, List<Card> base, List<Card> singles, int mainRank, int length) {
        List<Card> sortedSingles = new ArrayList<>(singles);
        Collections.sort(sortedSingles);
        chooseSinglesRecursive(moves, seen, base, sortedSingles, mainRank, length, 0, new ArrayList<Card>());
    }

    private static void chooseSinglesRecursive(List<Move> moves, Set<String> seen, List<Card> base, List<Card> singles, int mainRank, int target, int index, List<Card> chosen) {
        if (chosen.size() == target) {
            addMove(moves, seen, new Move(Move.Type.PLANE_SINGLE, join(base, chosen), mainRank, target));
            return;
        }
        for (int i = index; i < singles.size(); i++) {
            chosen.add(singles.get(i));
            chooseSinglesRecursive(moves, seen, base, singles, mainRank, target, i + 1, chosen);
            chosen.remove(chosen.size() - 1);
        }
    }

    private static void combinePairsForPlane(List<Move> moves, Set<String> seen, List<Card> base, List<List<Card>> pairs, int mainRank, int length) {
        choosePairsRecursive(moves, seen, base, pairs, mainRank, length, 0, new ArrayList<Card>());
    }

    private static void choosePairsRecursive(List<Move> moves, Set<String> seen, List<Card> base, List<List<Card>> pairs, int mainRank, int target, int index, List<Card> chosen) {
        if (chosen.size() == target * 2) {
            addMove(moves, seen, new Move(Move.Type.PLANE_PAIR, join(base, chosen), mainRank, target));
            return;
        }
        for (int i = index; i < pairs.size(); i++) {
            chosen.addAll(pairs.get(i));
            choosePairsRecursive(moves, seen, base, pairs, mainRank, target, i + 1, chosen);
            chosen.remove(chosen.size() - 1);
            chosen.remove(chosen.size() - 1);
        }
    }

    private static List<Card> remainingCards(List<Card> cards, int excludedRank, int minCount) {
        List<Card> remaining = new ArrayList<>();
        for (Card card : cards) {
            if (card.rank != excludedRank) {
                remaining.add(card);
            }
        }
        return remaining;
    }

    private static List<List<Card>> remainingPairs(Map<Integer, List<Card>> groups, int excludedRank) {
        List<List<Card>> pairs = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getKey() != excludedRank && entry.getValue().size() >= 2) {
                pairs.add(new ArrayList<>(entry.getValue().subList(0, 2)));
            }
        }
        return pairs;
    }

    private static List<List<Card>> remainingPairs(Map<Integer, List<Card>> groups, List<Integer> excludedRanks) {
        List<List<Card>> pairs = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (!excludedRanks.contains(entry.getKey()) && entry.getValue().size() >= 2) {
                pairs.add(new ArrayList<>(entry.getValue().subList(0, 2)));
            }
        }
        return pairs;
    }

    private static List<Card> removeRanks(List<Card> cards, List<Integer> removedRanks, int upToCount) {
        Map<Integer, Integer> consumed = new HashMap<>();
        List<Card> remaining = new ArrayList<>();
        for (Card card : cards) {
            if (removedRanks.contains(card.rank)) {
                int used = consumed.containsKey(card.rank) ? consumed.get(card.rank) : 0;
                if (used < upToCount) {
                    consumed.put(card.rank, used + 1);
                    continue;
                }
            }
            remaining.add(card);
        }
        return remaining;
    }

    private static List<Integer> eligibleRanks(Map<Integer, List<Card>> groups, int minSize) {
        List<Integer> ranks = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() >= minSize && entry.getKey() <= 14) {
                ranks.add(entry.getKey());
            }
        }
        Collections.sort(ranks);
        return ranks;
    }

    private static boolean containsCount(Map<Integer, List<Card>> groups, int count) {
        for (List<Card> cards : groups.values()) {
            if (cards.size() == count) {
                return true;
            }
        }
        return false;
    }

    private static int highestRankWithCount(Map<Integer, List<Card>> groups, int count) {
        int rank = 0;
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == count) {
                rank = Math.max(rank, entry.getKey());
            }
        }
        return rank;
    }

    private static int highestRank(Map<Integer, List<Card>> groups) {
        int rank = 0;
        for (Integer key : groups.keySet()) {
            rank = Math.max(rank, key);
        }
        return rank;
    }

    private static boolean isStraight(Map<Integer, List<Card>> groups, int requireCount, int chainLength, int minLength) {
        if (chainLength < minLength) {
            return false;
        }
        List<Integer> ranks = new ArrayList<>(groups.keySet());
        Collections.sort(ranks);
        if (ranks.size() != chainLength) {
            return false;
        }
        for (Integer rank : ranks) {
            if (rank > 14 || groups.get(rank).size() != requireCount) {
                return false;
            }
        }
        for (int i = 1; i < ranks.size(); i++) {
            if (ranks.get(i) != ranks.get(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> getTripleRun(Map<Integer, List<Card>> groups) {
        List<Integer> ranks = eligibleRanks(groups, 3);
        List<Integer> best = new ArrayList<>();
        int start = 0;
        while (start < ranks.size()) {
            int end = start;
            while (end + 1 < ranks.size() && ranks.get(end + 1) == ranks.get(end) + 1) {
                end++;
            }
            if (end - start + 1 >= 2) {
                List<Integer> current = ranks.subList(start, end + 1);
                if (current.size() > best.size()) {
                    best = new ArrayList<>(current);
                }
            }
            start = end + 1;
        }
        return best;
    }

    private static int countRemainingSingles(Map<Integer, List<Card>> groups, List<Integer> tripleRun) {
        int count = 0;
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            int value = entry.getValue().size();
            if (tripleRun.contains(entry.getKey())) {
                value -= 3;
            }
            count += Math.max(0, value);
        }
        return count;
    }

    private static int countRemainingPairs(Map<Integer, List<Card>> groups, List<Integer> tripleRun) {
        int count = 0;
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            int value = entry.getValue().size();
            if (tripleRun.contains(entry.getKey())) {
                value -= 3;
            }
            count += value / 2;
        }
        return count;
    }

    private static Map<Integer, List<Card>> groupByRank(List<Card> cards) {
        Map<Integer, List<Card>> groups = new LinkedHashMap<>();
        for (Card card : cards) {
            if (!groups.containsKey(card.rank)) {
                groups.put(card.rank, new ArrayList<Card>());
            }
            groups.get(card.rank).add(card);
        }
        return groups;
    }

    private static List<Card> join(List<Card> left, List<Card> right) {
        List<Card> cards = new ArrayList<>(left);
        cards.addAll(right);
        return cards;
    }

    private static void addMove(List<Move> moves, Set<String> seen, Move move) {
        if (move == null) {
            return;
        }
        StringBuilder builder = new StringBuilder(move.type.name()).append(':');
        List<Integer> ids = new ArrayList<>();
        for (Card card : move.cards) {
            ids.add(card.id);
        }
        Collections.sort(ids);
        for (Integer id : ids) {
            builder.append(id).append('-');
        }
        String key = builder.toString();
        if (seen.add(key)) {
            moves.add(move);
        }
    }
}
