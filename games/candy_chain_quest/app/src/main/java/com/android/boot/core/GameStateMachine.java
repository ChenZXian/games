package com.android.boot.core;

public class GameStateMachine {
    private GameState state = GameState.MENU;
    private BoardPhase boardPhase = BoardPhase.IDLE;

    public GameState getState() {
        return state;
    }

    public BoardPhase getBoardPhase() {
        return boardPhase;
    }

    public void setState(GameState state) {
        this.state = state;
        if (state != GameState.PLAYING) {
            boardPhase = BoardPhase.IDLE;
        }
    }

    public void setBoardPhase(BoardPhase boardPhase) {
        this.boardPhase = boardPhase;
    }

    public boolean isPlaying() {
        return state == GameState.PLAYING;
    }

    public boolean acceptsInput() {
        return state == GameState.PLAYING && boardPhase == BoardPhase.IDLE;
    }
}
