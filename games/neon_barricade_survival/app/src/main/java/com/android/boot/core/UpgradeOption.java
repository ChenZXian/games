package com.android.boot.core;

public class UpgradeOption {
    public String name;
    public UpgradeType type;

    public UpgradeOption(String name, UpgradeType type) {
        this.name = name;
        this.type = type;
    }
}
