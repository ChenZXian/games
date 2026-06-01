package com.android.boot.ui.panel;

import android.content.Context;
import android.widget.Toast;

public class CodexPanelController {
    private final Context context;
    public CodexPanelController(Context context) { this.context = context; }
    public void show() { Toast.makeText(context, "Codex wall opened", Toast.LENGTH_SHORT).show(); }
}
