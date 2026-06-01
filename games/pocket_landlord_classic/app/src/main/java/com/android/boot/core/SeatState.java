package com.android.boot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeatState {
    public final int seatIndex;
    public final List<Card> hand = new ArrayList<>();
    public boolean landlord;
    public String roleLabel = "Farmer";

    public SeatState(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public void sortHand() {
        Collections.sort(hand);
    }
}
