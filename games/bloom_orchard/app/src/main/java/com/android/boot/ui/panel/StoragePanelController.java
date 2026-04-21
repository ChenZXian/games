package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

import com.android.boot.core.GameSession;

public class StoragePanelController {
    private final Context context;
    public StoragePanelController(Context context) { this.context = context; }
    public void show(GameSession s) { Toast.makeText(context, "Storage " + s.storage.used + "/" + s.progression.storageCapacity, Toast.LENGTH_SHORT).show(); }
}
