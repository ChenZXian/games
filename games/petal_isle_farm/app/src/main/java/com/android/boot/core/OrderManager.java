package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.boot.entity.OrderData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrderManager {
    private final SharedPreferences pref;
    private final Random random = new Random();
    public final List<OrderData> activeOrders = new ArrayList<>();

    public OrderManager(Context context) {
        pref = context.getSharedPreferences("orders", Context.MODE_PRIVATE);
        refresh();
    }

    public void refresh() {
        activeOrders.clear();
        String[] tiers = {"common", "premium", "festival"};
        for (int i = 0; i < 3; i++) {
            OrderData order = new OrderData();
            order.tier = tiers[i];
            order.cropId = i == 0 ? "carrot" : i == 1 ? "rose" : "golden_wheat";
            order.count = 2 + i;
            order.coins = 60 + i * 70;
            order.xp = 20 + i * 25;
            order.beautyTokens = 1 + i;
            activeOrders.add(order);
        }
        pref.edit().putLong("lastRefresh", System.currentTimeMillis() + random.nextInt(1000)).apply();
    }
}
