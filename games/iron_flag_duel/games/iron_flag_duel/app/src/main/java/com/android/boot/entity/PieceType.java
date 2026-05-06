package com.android.boot.entity;

public enum PieceType {
    FLAG(0, false, true, false, false, 1000, "军旗"),
    MINE(0, false, false, true, false, 170, "地雷"),
    BOMB(0, true, false, false, true, 300, "炸弹"),
    ENGINEER(1, true, false, false, false, 80, "工兵"),
    PLATOON(2, true, false, false, false, 100, "排长"),
    COMPANY(3, true, false, false, false, 120, "连长"),
    BATTALION(4, true, false, false, false, 145, "营长"),
    REGIMENT(5, true, false, false, false, 175, "团长"),
    BRIGADE(6, true, false, false, false, 210, "旅长"),
    DIVISION(7, true, false, false, false, 250, "师长"),
    CORPS(8, true, false, false, false, 290, "军长"),
    COMMANDER(9, true, false, false, false, 360, "司令");

    private final int rank;
    private final boolean movable;
    private final boolean flag;
    private final boolean mine;
    private final boolean bomb;
    private final int value;
    private final String label;

    PieceType(int rank, boolean movable, boolean flag, boolean mine, boolean bomb, int value, String label) {
        this.rank = rank;
        this.movable = movable;
        this.flag = flag;
        this.mine = mine;
        this.bomb = bomb;
        this.value = value;
        this.label = label;
    }

    public int getRank() { return rank; }
    public boolean isMovable() { return movable; }
    public boolean isFlag() { return flag; }
    public boolean isMine() { return mine; }
    public boolean isBomb() { return bomb; }
    public int getValue() { return value; }
    public String getLabel() { return label; }
}
