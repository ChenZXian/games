package com.android.boot.ui;

import android.view.View;

public class OverlayController {
    public void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
