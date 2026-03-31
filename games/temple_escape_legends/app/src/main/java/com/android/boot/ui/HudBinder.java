package com.android.boot.ui;

import android.view.View;
import android.widget.TextView;
import com.android.boot.R;
import com.android.boot.model.RunSession;
import com.android.boot.model.Runner;

public class HudBinder {
    private final View root;

    public HudBinder(View root) {
        this.root = root;
    }

    public void bind(RunSession session, Runner runner) {
        set(R.id.hudScore, String.valueOf((int) session.score));
        set(R.id.hudCoins, String.valueOf(session.coinsRun));
        set(R.id.hudLife, String.valueOf(runner.life));
        set(R.id.hudPressure, (int) (session.bossPressure * 100) + "%");
        set(R.id.hudSpeed, "T" + session.speedTier);
        set(R.id.hudDistance, (int) session.distance + "m");
        set(R.id.hudShield, session.shield ? "On" : "Off");
        set(R.id.hudMagnet, session.magnetTimer > 0f ? "On" : "Off");
        set(R.id.hudSlow, session.slowTimer > 0f ? "On" : "Off");
        set(R.id.hudRevive, session.reviveToken ? "Yes" : "No");
    }

    private void set(int id, String value) {
        TextView tv = root.findViewById(id);
        tv.setText(value);
    }
}
