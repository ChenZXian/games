package com.android.boot.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;

import com.android.boot.audio.ToneFx;
import com.android.boot.model.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class GameEngine {
    public static final String STATE_MENU = "MENU";
    public static final String STATE_PLAYING = "PLAYING";
    public static final String STATE_PAUSED = "PAUSED";
    public static final String STATE_ROUND_RESULT = "ROUND_RESULT";
    public static final String STATE_GAME_OVER = "GAME_OVER";
    private static final String PREFS = "black_spade_alliance_prefs";
    private static final String KEY_BEST_SCORE = "best_score";
    private static final int SEAT_PLAYER = 0;
    private static final int SUIT_CLUB = 0;
    private static final int SUIT_DIAMOND = 1;
    private static final int SUIT_HEART = 2;
    private static final int SUIT_SPADE = 3;
    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_PAIR = 2;
    private static final int TYPE_TRIPLE = 3;
    private static final int TYPE_FLUSH = 4;
    private static final int TYPE_THREE_TWO = 5;
    private static final int START_SCORE = 10;
    private static final int TOTAL_ROUNDS = 5;
    private static final String[] RANK_LABELS = {"4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "3"};
    private static final String[] SUIT_LABELS = {"C", "D", "H", "S"};
    private static final String[] SEAT_LABELS = {"You", "Left AI", "Top AI", "Right AI"};
    private static final Comparator<Card> CARD_COMPARATOR = new Comparator<Card>() {
        @Override
        public int compare(Card left, Card right) {
            if (left.rank != right.rank) {
                return Integer.compare(left.rank, right.rank);
            }
            return Integer.compare(left.suit, right.suit);
        }
    };
    private final SharedPreferences preferences;
    private final Random random = new Random();
    private final ArrayList<PlayerState> players = new ArrayList<>();
    private final ArrayList<Integer> selectedIndexes = new ArrayList<>();
    private final ArrayList<Integer> finishOrder = new ArrayList<>();
    private final GameSnapshot snapshot = new GameSnapshot();
    private ToneFx audio;
    private String state = STATE_MENU;
    private Play livePlay;
    private int liveSeat = -1;
    private int currentSeat = 0;
    private int leadSeat = 0;
    private int passCount = 0;
    private int roundNumber = 0;
    private int totalScore = START_SCORE;
    private int bestScore;
    private final int[] teamBySeat = new int[4];
    private int soloSeat = -1;
    private float aiDelay = 0.7f;
    private String infoLabel = "Tap Start Match";
    private String allianceLabel = "Anchor cards decide the side";
    private String resultTitle = "";
    private String resultBody = "";

    public GameEngine(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        bestScore = Math.max(START_SCORE, preferences.getInt(KEY_BEST_SCORE, START_SCORE));
        for (int seat = 0; seat < 4; seat++) {
            players.add(new PlayerState());
        }
        updateSnapshot();
    }

    public void setAudio(ToneFx audio) {
        this.audio = audio;
        snapshot.muted = audio != null && audio.isMuted();
    }

    public void update(float dt) {
        float clamped = Math.min(0.05f, Math.max(0.0f, dt));
        if (STATE_PLAYING.equals(state) && currentSeat != SEAT_PLAYER) {
            aiDelay -= clamped;
            if (aiDelay <= 0f) {
                aiDelay = 0.8f;
                performAiTurn();
            }
        }
        updateSnapshot();
    }

    public void startMatch() {
        totalScore = START_SCORE;
        roundNumber = 0;
        startRound();
    }

    public void restartMatch() {
        startMatch();
    }

    public void goToMenu() {
        state = STATE_MENU;
        livePlay = null;
        liveSeat = -1;
        selectedIndexes.clear();
        finishOrder.clear();
        roundNumber = 0;
        totalScore = START_SCORE;
        resultTitle = "";
        resultBody = "";
        infoLabel = "Tap Start Match";
        allianceLabel = "Anchor cards decide the side";
        updateSnapshot();
    }

    public void pause() {
        if (STATE_PLAYING.equals(state)) {
            state = STATE_PAUSED;
        }
        updateSnapshot();
    }

    public void resume() {
        if (STATE_PAUSED.equals(state)) {
            state = STATE_PLAYING;
            aiDelay = 0.6f;
        }
        updateSnapshot();
    }

    public void toggleMuted() {
        if (audio != null) {
            audio.setMuted(!audio.isMuted());
            snapshot.muted = audio.isMuted();
        }
    }

    public boolean isMuted() {
        return audio != null && audio.isMuted();
    }

    public GameSnapshot getSnapshot() {
        return snapshot;
    }

    public void advanceAfterResult() {
        if (STATE_ROUND_RESULT.equals(state)) {
            startRound();
        } else if (STATE_GAME_OVER.equals(state)) {
            goToMenu();
        }
    }

    public void onPlayerTap(float x, float y, float width, float height) {
        if (!STATE_PLAYING.equals(state) || currentSeat != SEAT_PLAYER) {
            return;
        }
        int index = findPlayerCardIndex(x, y, width, height);
        if (index < 0) {
            return;
        }
        if (selectedIndexes.contains(index)) {
            selectedIndexes.remove(Integer.valueOf(index));
        } else {
            selectedIndexes.add(index);
        }
        Collections.sort(selectedIndexes);
        if (audio != null) {
            audio.playSelect();
        }
        updateSnapshot();
    }

    public boolean canPlaySelection() {
        if (!STATE_PLAYING.equals(state) || currentSeat != SEAT_PLAYER) {
            return false;
        }
        Play play = buildPlayFromSelection(players.get(SEAT_PLAYER).hand, selectedIndexes);
        return play != null && canBeatLive(play);
    }

    public boolean canPass() {
        return STATE_PLAYING.equals(state) && currentSeat == SEAT_PLAYER && livePlay != null;
    }

    public void playSelectedCards() {
        if (!canPlaySelection()) {
            return;
        }
        Play play = buildPlayFromSelection(players.get(SEAT_PLAYER).hand, selectedIndexes);
        if (play != null) {
            applyPlay(SEAT_PLAYER, play);
        }
    }

    public void playerPass() {
        if (canPass()) {
            applyPass(SEAT_PLAYER);
        }
    }

    public List<Card> getPlayerHand() {
        return players.get(SEAT_PLAYER).hand;
    }

    public List<Card> getLiveCards() {
        return livePlay == null ? Collections.<Card>emptyList() : livePlay.cards;
    }

    public boolean isSelected(int index) {
        return selectedIndexes.contains(index);
    }

    public int getSeatCardCount(int seat) {
        return players.get(seat).hand.size();
    }

    public boolean isSeatFinished(int seat) {
        return players.get(seat).finished;
    }

    public int getCurrentSeat() {
        return currentSeat;
    }

    public int getLeadSeat() {
        return leadSeat;
    }

    public int getLiveSeat() {
        return liveSeat;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getTeamForSeat(int seat) {
        return teamBySeat[seat];
    }

    public int getSoloSeat() {
        return soloSeat;
    }

    public String getSeatLabel(int seat) {
        return SEAT_LABELS[seat];
    }

    public String getCardLabel(Card card) {
        return RANK_LABELS[card.rank] + SUIT_LABELS[card.suit];
    }

    public String getPlayLabel() {
        return livePlay == null ? "Open lead" : livePlay.label;
    }

    public void getPlayerCardRect(int index, int totalCards, float width, float height, boolean selected, RectF outRect) {
        float cardWidth = Math.min(width * 0.095f, 120f);
        float cardHeight = cardWidth * 1.42f;
        float overlap = Math.min(cardWidth * 0.54f, (width - 140f) / Math.max(1f, totalCards - 1f));
        float totalWidth = cardWidth + overlap * Math.max(0, totalCards - 1);
        float startX = (width - totalWidth) * 0.5f;
        float x = startX + overlap * index;
        float baseY = height - cardHeight - 28f;
        if (selected) {
            baseY -= 26f;
        }
        outRect.set(x, baseY, x + cardWidth, baseY + cardHeight);
    }

    private void startRound() {
        roundNumber++;
        livePlay = null;
        liveSeat = -1;
        selectedIndexes.clear();
        finishOrder.clear();
        passCount = 0;
        dealRound();
        resolveTeams();
        currentSeat = random.nextInt(4);
        leadSeat = currentSeat;
        state = STATE_PLAYING;
        aiDelay = 0.9f;
        infoLabel = getSeatLabel(currentSeat) + " leads";
        resultTitle = "";
        resultBody = "";
        if (audio != null) {
            audio.playDeal();
        }
        updateSnapshot();
    }

    private void dealRound() {
        ArrayList<Card> deck = new ArrayList<>();
        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 0; rank < 13; rank++) {
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck, random);
        for (int seat = 0; seat < 4; seat++) {
            PlayerState player = players.get(seat);
            player.hand.clear();
            player.finished = false;
            for (int i = 0; i < 13; i++) {
                player.hand.add(deck.get(seat * 13 + i));
            }
            Collections.sort(player.hand, CARD_COMPARATOR);
        }
    }

    private void resolveTeams() {
        int spadeThreeSeat = -1;
        int spadeASeat = -1;
        for (int seat = 0; seat < 4; seat++) {
            for (Card card : players.get(seat).hand) {
                if (card.suit == SUIT_SPADE && card.rank == 12) {
                    spadeThreeSeat = seat;
                }
                if (card.suit == SUIT_SPADE && card.rank == 10) {
                    spadeASeat = seat;
                }
            }
        }
        soloSeat = -1;
        if (spadeThreeSeat == spadeASeat) {
            soloSeat = spadeThreeSeat;
            for (int seat = 0; seat < 4; seat++) {
                teamBySeat[seat] = seat == soloSeat ? 1 : 2;
            }
            if (soloSeat == SEAT_PLAYER) {
                allianceLabel = "You hold S3 and SA";
            } else {
                allianceLabel = getSeatLabel(soloSeat) + " holds S3 and SA";
            }
        } else {
            for (int seat = 0; seat < 4; seat++) {
                teamBySeat[seat] = seat == spadeThreeSeat || seat == spadeASeat ? 1 : 2;
            }
            allianceLabel = teamBySeat[SEAT_PLAYER] == 1 ? "You pair with " + allyLabelForPlayer() : "You chase with " + allyLabelForPlayer();
        }
    }

    private String allyLabelForPlayer() {
        for (int seat = 1; seat < 4; seat++) {
            if (teamBySeat[seat] == teamBySeat[SEAT_PLAYER]) {
                return getSeatLabel(seat);
            }
        }
        return "no one";
    }

    private void performAiTurn() {
        if (!STATE_PLAYING.equals(state) || currentSeat == SEAT_PLAYER) {
            return;
        }
        int seat = currentSeat;
        ArrayList<Play> plays = findLegalPlays(players.get(seat).hand, livePlay);
        if (livePlay != null && shouldAiPass(seat, plays)) {
            applyPass(seat);
            return;
        }
        if (plays.isEmpty()) {
            applyPass(seat);
            return;
        }
        applyPlay(seat, chooseAiPlay(seat, plays));
    }

    private boolean shouldAiPass(int seat, ArrayList<Play> plays) {
        if (livePlay == null) {
            return false;
        }
        if (liveSeat >= 0 && teamBySeat[liveSeat] == teamBySeat[seat]) {
            for (Play play : plays) {
                if (play.cardIndexes.size() == players.get(seat).hand.size()) {
                    return false;
                }
            }
            return true;
        }
        return plays.isEmpty();
    }

    private Play chooseAiPlay(int seat, ArrayList<Play> plays) {
        if (livePlay == null && players.get(seat).hand.size() <= 5) {
            Play best = plays.get(0);
            for (Play play : plays) {
                if (play.cardIndexes.size() > best.cardIndexes.size()) {
                    best = play;
                } else if (play.cardIndexes.size() == best.cardIndexes.size() && play.compareTo(best) < 0) {
                    best = play;
                }
            }
            return best;
        }
        return plays.get(0);
    }

    private void applyPlay(int seat, Play play) {
        PlayerState player = players.get(seat);
        Play materialized = play.withMaterializedCards(player.hand);
        removeCards(player.hand, play.cardIndexes);
        selectedIndexes.clear();
        livePlay = materialized;
        liveSeat = seat;
        leadSeat = seat;
        passCount = 0;
        infoLabel = getSeatLabel(seat) + " played " + livePlay.label;
        if (audio != null) {
            audio.playCard();
        }
        if (player.hand.isEmpty()) {
            player.finished = true;
            if (!finishOrder.contains(seat)) {
                finishOrder.add(seat);
            }
        }
        if (activeSeatCount() == 1) {
            int remainingSeat = firstActiveSeat();
            if (remainingSeat >= 0 && !finishOrder.contains(remainingSeat)) {
                finishOrder.add(remainingSeat);
                players.get(remainingSeat).finished = true;
            }
            closeRound();
            return;
        }
        currentSeat = nextActiveSeat(seat);
        if (currentSeat != SEAT_PLAYER) {
            aiDelay = 0.7f;
        }
        updateSnapshot();
    }

    private void applyPass(int seat) {
        passCount++;
        infoLabel = getSeatLabel(seat) + " passed";
        if (audio != null) {
            audio.playPass();
        }
        if (passCount >= activeSeatCount() - 1) {
            currentSeat = liveSeat;
            livePlay = null;
            liveSeat = -1;
            passCount = 0;
            infoLabel = getSeatLabel(currentSeat) + " reopens";
            if (currentSeat != SEAT_PLAYER) {
                aiDelay = 0.6f;
            }
            updateSnapshot();
            return;
        }
        currentSeat = nextActiveSeat(seat);
        if (currentSeat != SEAT_PLAYER) {
            aiDelay = 0.7f;
        }
        updateSnapshot();
    }

    private void closeRound() {
        int delta = computePlayerScoreDelta();
        totalScore += delta;
        bestScore = Math.max(bestScore, totalScore);
        preferences.edit().putInt(KEY_BEST_SCORE, bestScore).apply();
        StringBuilder orderBuilder = new StringBuilder();
        for (int i = 0; i < finishOrder.size(); i++) {
            if (i > 0) {
                orderBuilder.append(" > ");
            }
            orderBuilder.append(getSeatLabel(finishOrder.get(i)));
        }
        resultTitle = "Round " + roundNumber + " Result";
        resultBody = "Finish order: " + orderBuilder + "\nScore change: " + (delta >= 0 ? "+" : "") + delta + "\nTotal score: " + totalScore;
        if (audio != null) {
            audio.playScore();
        }
        if (roundNumber >= TOTAL_ROUNDS) {
            state = STATE_GAME_OVER;
            resultTitle = "Five Rounds Complete";
            resultBody = resultBody + "\nBest score: " + bestScore;
            if (audio != null) {
                audio.playResult();
            }
        } else {
            state = STATE_ROUND_RESULT;
        }
        updateSnapshot();
    }

    private int computePlayerScoreDelta() {
        ArrayList<Integer> ownPositions = new ArrayList<>();
        ArrayList<Integer> opponentPositions = new ArrayList<>();
        for (int i = 0; i < finishOrder.size(); i++) {
            int seat = finishOrder.get(i);
            if (teamBySeat[seat] == teamBySeat[SEAT_PLAYER]) {
                ownPositions.add(i);
            } else {
                opponentPositions.add(i);
            }
        }
        if (teamSize(teamBySeat[SEAT_PLAYER]) == 1) {
            return !ownPositions.isEmpty() && ownPositions.get(0) == 0 ? 6 : 0;
        }
        if (ownPositions.isEmpty() || opponentPositions.isEmpty()) {
            return 0;
        }
        int ownBest = ownPositions.get(0);
        int oppBest = opponentPositions.get(0);
        if (ownBest < oppBest) {
            if (ownPositions.size() >= 2 && ownPositions.get(1) < oppBest) {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    private int teamSize(int team) {
        int count = 0;
        for (int seat = 0; seat < 4; seat++) {
            if (teamBySeat[seat] == team) {
                count++;
            }
        }
        return count;
    }

    private int activeSeatCount() {
        int count = 0;
        for (int seat = 0; seat < 4; seat++) {
            if (!players.get(seat).finished) {
                count++;
            }
        }
        return count;
    }

    private int firstActiveSeat() {
        for (int seat = 0; seat < 4; seat++) {
            if (!players.get(seat).finished) {
                return seat;
            }
        }
        return -1;
    }

    private int nextActiveSeat(int fromSeat) {
        int seat = fromSeat;
        for (int i = 0; i < 4; i++) {
            seat = (seat + 1) % 4;
            if (!players.get(seat).finished) {
                return seat;
            }
        }
        return fromSeat;
    }

    private boolean canBeatLive(Play play) {
        if (play == null) {
            return false;
        }
        if (livePlay == null) {
            return true;
        }
        return play.type == livePlay.type && play.compareTo(livePlay) > 0;
    }

    private ArrayList<Play> findLegalPlays(List<Card> hand, Play currentLivePlay) {
        ArrayList<Play> plays = new ArrayList<>();
        addSingles(hand, plays);
        addPairs(hand, plays);
        addTriples(hand, plays);
        addThreeWithTwo(hand, plays);
        addFlushes(hand, plays);
        ArrayList<Play> filtered = new ArrayList<>();
        for (Play play : plays) {
            if (currentLivePlay == null || (play.type == currentLivePlay.type && play.compareTo(currentLivePlay) > 0)) {
                filtered.add(play);
            }
        }
        Collections.sort(filtered);
        return filtered;
    }

    private void addSingles(List<Card> hand, ArrayList<Play> plays) {
        for (int i = 0; i < hand.size(); i++) {
            ArrayList<Integer> indexes = new ArrayList<>();
            indexes.add(i);
            ArrayList<Integer> key = new ArrayList<>();
            key.add(hand.get(i).rank);
            plays.add(new Play(TYPE_SINGLE, indexes, key, rankLabel(hand.get(i).rank)));
        }
    }

    private void addPairs(List<Card> hand, ArrayList<Play> plays) {
        for (int rank = 0; rank < 13; rank++) {
            ArrayList<Integer> indexes = indexesForRank(hand, rank);
            if (indexes.size() >= 2) {
                ArrayList<Integer> pair = new ArrayList<>();
                pair.add(indexes.get(0));
                pair.add(indexes.get(1));
                ArrayList<Integer> key = new ArrayList<>();
                key.add(rank);
                plays.add(new Play(TYPE_PAIR, pair, key, "Pair " + rankLabel(rank)));
            }
        }
    }

    private void addTriples(List<Card> hand, ArrayList<Play> plays) {
        for (int rank = 0; rank < 13; rank++) {
            ArrayList<Integer> indexes = indexesForRank(hand, rank);
            if (indexes.size() >= 3) {
                ArrayList<Integer> triple = new ArrayList<>();
                triple.add(indexes.get(0));
                triple.add(indexes.get(1));
                triple.add(indexes.get(2));
                ArrayList<Integer> key = new ArrayList<>();
                key.add(rank);
                plays.add(new Play(TYPE_TRIPLE, triple, key, "Triple " + rankLabel(rank)));
            }
        }
    }

    private void addThreeWithTwo(List<Card> hand, ArrayList<Play> plays) {
        for (int tripleRank = 0; tripleRank < 13; tripleRank++) {
            ArrayList<Integer> tripleIndexes = indexesForRank(hand, tripleRank);
            if (tripleIndexes.size() < 3) {
                continue;
            }
            for (int pairRank = 0; pairRank < 13; pairRank++) {
                if (pairRank == tripleRank) {
                    continue;
                }
                ArrayList<Integer> pairIndexes = indexesForRank(hand, pairRank);
                if (pairIndexes.size() < 2) {
                    continue;
                }
                ArrayList<Integer> combo = new ArrayList<>();
                combo.add(tripleIndexes.get(0));
                combo.add(tripleIndexes.get(1));
                combo.add(tripleIndexes.get(2));
                combo.add(pairIndexes.get(0));
                combo.add(pairIndexes.get(1));
                ArrayList<Integer> key = new ArrayList<>();
                key.add(tripleRank);
                key.add(pairRank);
                plays.add(new Play(TYPE_THREE_TWO, combo, key, "Three With Two"));
            }
        }
    }

    private void addFlushes(List<Card> hand, ArrayList<Play> plays) {
        for (int suit = 0; suit < 4; suit++) {
            ArrayList<Integer> suitIndexes = new ArrayList<>();
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).suit == suit) {
                    suitIndexes.add(i);
                }
            }
            if (suitIndexes.size() < 5) {
                continue;
            }
            int size = suitIndexes.size();
            for (int a = 0; a < size - 4; a++) {
                for (int b = a + 1; b < size - 3; b++) {
                    for (int c = b + 1; c < size - 2; c++) {
                        for (int d = c + 1; d < size - 1; d++) {
                            for (int e = d + 1; e < size; e++) {
                                ArrayList<Integer> combo = new ArrayList<>();
                                combo.add(suitIndexes.get(a));
                                combo.add(suitIndexes.get(b));
                                combo.add(suitIndexes.get(c));
                                combo.add(suitIndexes.get(d));
                                combo.add(suitIndexes.get(e));
                                ArrayList<Integer> key = sortedRankKey(hand, combo);
                                plays.add(new Play(TYPE_FLUSH, combo, key, "Flush " + SUIT_LABELS[suit]));
                            }
                        }
                    }
                }
            }
        }
    }

    private ArrayList<Integer> indexesForRank(List<Card> hand, int rank) {
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).rank == rank) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private ArrayList<Integer> sortedRankKey(List<Card> hand, ArrayList<Integer> indexes) {
        ArrayList<Integer> key = new ArrayList<>();
        for (int index : indexes) {
            key.add(hand.get(index).rank);
        }
        Collections.sort(key, Collections.reverseOrder());
        return key;
    }

    private Play buildPlayFromSelection(List<Card> hand, List<Integer> indexes) {
        if (indexes.isEmpty()) {
            return null;
        }
        ArrayList<Integer> sorted = new ArrayList<>(indexes);
        Collections.sort(sorted);
        ArrayList<Card> cards = new ArrayList<>();
        for (int index : sorted) {
            if (index < 0 || index >= hand.size()) {
                return null;
            }
            cards.add(hand.get(index));
        }
        if (cards.size() == 1) {
            ArrayList<Integer> key = new ArrayList<>();
            key.add(cards.get(0).rank);
            return new Play(TYPE_SINGLE, sorted, key, rankLabel(cards.get(0).rank));
        }
        if (cards.size() == 2 && cards.get(0).rank == cards.get(1).rank) {
            ArrayList<Integer> key = new ArrayList<>();
            key.add(cards.get(0).rank);
            return new Play(TYPE_PAIR, sorted, key, "Pair " + rankLabel(cards.get(0).rank));
        }
        if (cards.size() == 3 && cards.get(0).rank == cards.get(1).rank && cards.get(1).rank == cards.get(2).rank) {
            ArrayList<Integer> key = new ArrayList<>();
            key.add(cards.get(0).rank);
            return new Play(TYPE_TRIPLE, sorted, key, "Triple " + rankLabel(cards.get(0).rank));
        }
        if (cards.size() == 5) {
            if (allSameSuit(cards)) {
                ArrayList<Integer> key = new ArrayList<>();
                for (Card card : cards) {
                    key.add(card.rank);
                }
                Collections.sort(key, Collections.reverseOrder());
                return new Play(TYPE_FLUSH, sorted, key, "Flush " + SUIT_LABELS[cards.get(0).suit]);
            }
            int tripleRank = findTripleRank(cards);
            int pairRank = findPairRank(cards);
            if (tripleRank >= 0 && pairRank >= 0) {
                ArrayList<Integer> key = new ArrayList<>();
                key.add(tripleRank);
                key.add(pairRank);
                return new Play(TYPE_THREE_TWO, sorted, key, "Three With Two");
            }
        }
        return null;
    }

    private boolean allSameSuit(List<Card> cards) {
        int suit = cards.get(0).suit;
        for (Card card : cards) {
            if (card.suit != suit) {
                return false;
            }
        }
        return true;
    }

    private int findTripleRank(List<Card> cards) {
        for (int rank = 0; rank < 13; rank++) {
            int count = 0;
            for (Card card : cards) {
                if (card.rank == rank) {
                    count++;
                }
            }
            if (count == 3) {
                return rank;
            }
        }
        return -1;
    }

    private int findPairRank(List<Card> cards) {
        for (int rank = 0; rank < 13; rank++) {
            int count = 0;
            for (Card card : cards) {
                if (card.rank == rank) {
                    count++;
                }
            }
            if (count == 2) {
                return rank;
            }
        }
        return -1;
    }

    private void removeCards(ArrayList<Card> hand, List<Integer> indexes) {
        for (int i = indexes.size() - 1; i >= 0; i--) {
            int index = indexes.get(i);
            if (index >= 0 && index < hand.size()) {
                hand.remove(index);
            }
        }
    }

    private int findPlayerCardIndex(float x, float y, float width, float height) {
        List<Card> hand = players.get(SEAT_PLAYER).hand;
        if (hand.isEmpty()) {
            return -1;
        }
        RectF rect = new RectF();
        for (int i = hand.size() - 1; i >= 0; i--) {
            getPlayerCardRect(i, hand.size(), width, height, isSelected(i), rect);
            if (rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private void updateSnapshot() {
        snapshot.state = state;
        snapshot.roundNumber = roundNumber;
        snapshot.totalScore = totalScore;
        snapshot.bestScore = bestScore;
        snapshot.muted = audio != null && audio.isMuted();
        snapshot.playerTurn = STATE_PLAYING.equals(state) && currentSeat == SEAT_PLAYER;
        snapshot.playEnabled = canPlaySelection();
        snapshot.passEnabled = canPass();
        snapshot.showResultOverlay = STATE_ROUND_RESULT.equals(state) || STATE_GAME_OVER.equals(state);
        snapshot.finalResult = STATE_GAME_OVER.equals(state);
        snapshot.roundLabel = roundNumber > 0 ? "Round " + roundNumber + " / " + TOTAL_ROUNDS : "Round -";
        snapshot.scoreLabel = "Score " + totalScore + "  Best " + bestScore;
        snapshot.allianceLabel = allianceLabel;
        snapshot.leadLabel = livePlay == null ? "Lead open" : getSeatLabel(liveSeat) + " set " + livePlay.label;
        snapshot.infoLabel = infoLabel;
        snapshot.resultTitle = resultTitle;
        snapshot.resultBody = resultBody;
        snapshot.actionLabel = STATE_GAME_OVER.equals(state) ? "Back To Menu" : "Next Round";
    }

    private String rankLabel(int rank) {
        return RANK_LABELS[rank];
    }

    public static final class Card {
        public final int suit;
        public final int rank;

        public Card(int suit, int rank) {
            this.suit = suit;
            this.rank = rank;
        }
    }

    private static final class PlayerState {
        private final ArrayList<Card> hand = new ArrayList<>();
        private boolean finished;
    }

    public static final class Play implements Comparable<Play> {
        public final int type;
        public final ArrayList<Integer> cardIndexes;
        public final ArrayList<Integer> keyRanks;
        public final String label;
        public final ArrayList<Card> cards = new ArrayList<>();

        public Play(int type, ArrayList<Integer> cardIndexes, ArrayList<Integer> keyRanks, String label) {
            this.type = type;
            this.cardIndexes = new ArrayList<>(cardIndexes);
            this.keyRanks = new ArrayList<>(keyRanks);
            this.label = label;
        }

        public Play withMaterializedCards(List<Card> sourceHand) {
            Play play = new Play(type, cardIndexes, keyRanks, label);
            for (int index : cardIndexes) {
                if (index >= 0 && index < sourceHand.size()) {
                    play.cards.add(sourceHand.get(index));
                }
            }
            return play;
        }

        @Override
        public int compareTo(Play other) {
            if (type != other.type) {
                return Integer.compare(type, other.type);
            }
            int size = Math.min(keyRanks.size(), other.keyRanks.size());
            for (int i = 0; i < size; i++) {
                int compare = Integer.compare(keyRanks.get(i), other.keyRanks.get(i));
                if (compare != 0) {
                    return compare;
                }
            }
            if (keyRanks.size() != other.keyRanks.size()) {
                return Integer.compare(keyRanks.size(), other.keyRanks.size());
            }
            return Integer.compare(cardIndexes.size(), other.cardIndexes.size());
        }
    }
}
