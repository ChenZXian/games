package com.android.boot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LandlordGame {
    public enum Phase {
        MENU,
        BIDDING,
        PLAYING,
        ROUND_END
    }

    public static class RoundResult {
        public final boolean playerWon;
        public final int stars;
        public final int coins;
        public final String title;
        public final String body;

        public RoundResult(boolean playerWon, int stars, int coins, String title, String body) {
            this.playerWon = playerWon;
            this.stars = stars;
            this.coins = coins;
            this.title = title;
            this.body = body;
        }
    }

    private final List<CampaignStage> campaignStages = CampaignStage.createCampaign();
    private final List<CampaignStage> drills = CampaignStage.createDrills();
    private final SeatState[] seats = {new SeatState(0), new SeatState(1), new SeatState(2)};
    private final List<Card> kitty = new ArrayList<>();
    private final Random random = new Random();
    private CampaignStage stage;
    private int stagePointer;
    private int currentSeat;
    private int startingSeat;
    private int highestBid;
    private int highestBidder;
    private int bidTurns;
    private Move lastMove = Move.pass();
    private int lastMoveSeat = -1;
    private int passCount;
    private Phase phase = Phase.MENU;
    private RoundResult pendingResult;
    private int bombCount;
    private int playerBid;

    public void startCampaignStage(int index) {
        stagePointer = Math.max(0, Math.min(index, campaignStages.size() - 1));
        stage = campaignStages.get(stagePointer);
        startRound();
    }

    public void startDrillStage(int index) {
        stagePointer = Math.max(0, Math.min(index, drills.size() - 1));
        stage = drills.get(stagePointer);
        startRound();
    }

    public CampaignStage getCurrentStage() {
        return stage;
    }

    public int getStagePointer() {
        return stagePointer;
    }

    public List<CampaignStage> getCampaignStages() {
        return campaignStages;
    }

    public List<Card> getPlayerHand() {
        return seats[0].hand;
    }

    public List<Card> getKitty() {
        return kitty;
    }

    public SeatState getSeat(int index) {
        return seats[index];
    }

    public int getCurrentSeat() {
        return currentSeat;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public Move getLastMove() {
        return lastMove;
    }

    public int getLastMoveSeat() {
        return lastMoveSeat;
    }

    public RoundResult consumePendingResult() {
        RoundResult result = pendingResult;
        pendingResult = null;
        return result;
    }

    public boolean isPlayerTurn() {
        return currentSeat == 0;
    }

    public int getLandlordSeat() {
        for (SeatState seat : seats) {
            if (seat.landlord) {
                return seat.seatIndex;
            }
        }
        return -1;
    }

    public boolean playerBid(int bid) {
        if (phase != Phase.BIDDING || currentSeat != 0) {
            return false;
        }
        applyBid(0, bid);
        return true;
    }

    public boolean playerPass() {
        if (phase != Phase.PLAYING || currentSeat != 0 || lastMoveSeat == currentSeat || lastMove.isPass()) {
            return false;
        }
        passCount++;
        if (passCount >= 2) {
            currentSeat = lastMoveSeat;
            lastMove = Move.pass();
            lastMoveSeat = -1;
            passCount = 0;
        } else {
            currentSeat = (currentSeat + 1) % 3;
        }
        return true;
    }

    public Move getHint() {
        if (phase != Phase.PLAYING || currentSeat != 0) {
            return null;
        }
        List<Move> moves = RuleEngine.legalMoves(seats[0].hand, lastMove.isPass() ? null : lastMove);
        for (Move move : moves) {
            if (!move.isBombLike()) {
                return move;
            }
        }
        return moves.isEmpty() ? null : moves.get(0);
    }

    public boolean playerPlay(List<Card> cards) {
        if (phase != Phase.PLAYING || currentSeat != 0) {
            return false;
        }
        Move move = RuleEngine.identify(cards);
        if (move == null) {
            return false;
        }
        Move previous = lastMove.isPass() ? null : lastMove;
        if (!RuleEngine.canBeat(move, previous)) {
            return false;
        }
        applyMove(0, move);
        return true;
    }

    public String takeAiTurn() {
        if (currentSeat == 0 || phase == Phase.MENU || phase == Phase.ROUND_END) {
            return "";
        }
        if (phase == Phase.BIDDING) {
            int bid = LandlordAi.chooseBid(seats[currentSeat].hand, highestBid, stage.aiLevel);
            applyBid(currentSeat, bid);
            return bid == 0 ? "Pass" : "Call " + bid;
        }
        boolean teammateLeading = lastMoveSeat != -1 && sameTeam(currentSeat, lastMoveSeat) && !lastMove.isPass();
        Move previous = lastMove.isPass() ? null : lastMove;
        Move move = LandlordAi.chooseMove(seats[currentSeat].hand, previous, teammateLeading, stage.aiLevel);
        if (move == null || move.isPass()) {
            passCount++;
            if (passCount >= 2) {
                currentSeat = lastMoveSeat;
                lastMove = Move.pass();
                lastMoveSeat = -1;
                passCount = 0;
            } else {
                currentSeat = (currentSeat + 1) % 3;
            }
            return "Pass";
        }
        applyMove(currentSeat, move);
        return move.label();
    }

    public boolean canAdvanceStage() {
        return stage != null && !stage.drill && stagePointer < campaignStages.size() - 1;
    }

    public void advanceStage() {
        if (canAdvanceStage()) {
            stagePointer++;
        }
        startCampaignStage(stagePointer);
    }

    public void restartRound() {
        startRound();
    }

    private void startRound() {
        for (SeatState seat : seats) {
            seat.hand.clear();
            seat.landlord = false;
            seat.roleLabel = "Farmer";
        }
        kitty.clear();
        pendingResult = null;
        highestBid = 0;
        highestBidder = -1;
        bidTurns = 0;
        passCount = 0;
        lastMove = Move.pass();
        lastMoveSeat = -1;
        bombCount = 0;
        playerBid = 0;
        random.setSeed(stage.seed + System.nanoTime());
        List<Card> deck = createDeck();
        Collections.shuffle(deck, random);
        for (int i = 0; i < 51; i++) {
            seats[i % 3].hand.add(deck.get(i));
        }
        kitty.add(deck.get(51));
        kitty.add(deck.get(52));
        kitty.add(deck.get(53));
        for (SeatState seat : seats) {
            seat.sortHand();
        }
        startingSeat = (int) ((stage.seed + random.nextInt(1000)) % 3);
        currentSeat = startingSeat;
        phase = Phase.BIDDING;
    }

    private void applyBid(int seatIndex, int bid) {
        if (bid > highestBid) {
            highestBid = bid;
            highestBidder = seatIndex;
            if (seatIndex == 0) {
                playerBid = bid;
            }
        }
        bidTurns++;
        if (highestBid == 3 || bidTurns >= 3) {
            if (highestBidder == -1) {
                startRound();
                return;
            }
            beginPlay();
            return;
        }
        currentSeat = (currentSeat + 1) % 3;
    }

    private void beginPlay() {
        int landlordSeat = highestBidder;
        seats[landlordSeat].landlord = true;
        seats[landlordSeat].roleLabel = "Landlord";
        for (Card card : kitty) {
            seats[landlordSeat].hand.add(card);
        }
        seats[landlordSeat].sortHand();
        currentSeat = landlordSeat;
        phase = Phase.PLAYING;
        passCount = 0;
        lastMove = Move.pass();
        lastMoveSeat = -1;
    }

    private void applyMove(int seatIndex, Move move) {
        seats[seatIndex].hand.removeAll(move.cards);
        if (move.type == Move.Type.BOMB || move.type == Move.Type.ROCKET) {
            bombCount++;
        }
        lastMove = move;
        lastMoveSeat = seatIndex;
        passCount = 0;
        if (seats[seatIndex].hand.isEmpty()) {
            finishRound(seatIndex);
            return;
        }
        currentSeat = (seatIndex + 1) % 3;
    }

    private void finishRound(int winningSeat) {
        phase = Phase.ROUND_END;
        boolean playerWon = sameTeam(0, winningSeat);
        int stars = playerWon ? 1 : 0;
        if (playerWon && seats[0].hand.size() <= 4) {
            stars++;
        }
        if (playerWon && (winningSeat == 0 || bombCount > 0 || playerBid >= 2)) {
            stars++;
        }
        int coins = playerWon ? 60 + stage.aiLevel * 12 + stars * 15 : 18 + stage.aiLevel * 5;
        String title = playerWon ? "Table Cleared" : "Table Lost";
        String body = playerWon
                ? "Stars " + stars + "  Coins " + coins + "  Bombs " + bombCount
                : "You were outpaced this round. Coins " + coins;
        pendingResult = new RoundResult(playerWon, stars, coins, title, body);
    }

    private boolean sameTeam(int leftSeat, int rightSeat) {
        boolean leftLandlord = seats[leftSeat].landlord;
        boolean rightLandlord = seats[rightSeat].landlord;
        return leftLandlord == rightLandlord;
    }

    private List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();
        int id = 1;
        for (int rank = 3; rank <= 15; rank++) {
            for (int suit = 0; suit < 4; suit++) {
                deck.add(new Card(id++, rank, suit));
            }
        }
        deck.add(new Card(id++, Card.RANK_SMALL_JOKER, Card.SUIT_JOKER));
        deck.add(new Card(id, Card.RANK_BIG_JOKER, Card.SUIT_JOKER));
        return deck;
    }
}
