package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

import com.android.boot.core.GameSession;

public class OrderPanelController {
    private final Context context;
    public OrderPanelController(Context context) { this.context = context; }
    public void show(GameSession s) { Toast.makeText(context, "Orders " + s.orders.activeOrders.size(), Toast.LENGTH_SHORT).show(); }
}
