package com.android.boot.entity;

public class Piece {
    private final int id;
    private final Side side;
    private final PieceType type;
    private boolean revealedToPlayer;
    private boolean revealedToAi;
    private boolean alive = true;

    public Piece(int id, Side side, PieceType type) {
        this.id = id;
        this.side = side;
        this.type = type;
        this.revealedToPlayer = false;
        this.revealedToAi = false;
    }

    public int getId() { return id; }
    public Side getSide() { return side; }
    public PieceType getType() { return type; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isRevealedToPlayer() { return revealedToPlayer; }
    public boolean isRevealedToAi() { return revealedToAi; }
    public void revealTo(Side viewer) {
        if (viewer == Side.PLAYER) {
            revealedToPlayer = true;
        } else {
            revealedToAi = true;
        }
    }
    public boolean isKnownBy(Side viewer) {
        return viewer == Side.PLAYER ? revealedToPlayer : revealedToAi;
    }
}
