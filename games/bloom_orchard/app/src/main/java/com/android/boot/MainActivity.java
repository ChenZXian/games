package com.android.boot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.SoundController;
import com.android.boot.core.GameSession;
import com.android.boot.core.GameState;
import com.android.boot.ui.GameView;
import com.android.boot.ui.UiOverlayController;
import com.android.boot.ui.panel.AchievementPanelController;
import com.android.boot.ui.panel.CodexPanelController;
import com.android.boot.ui.panel.DailyTaskPanelController;
import com.android.boot.ui.panel.LoginRewardPanelController;
import com.android.boot.ui.panel.MenuPanelController;
import com.android.boot.ui.panel.OrderPanelController;
import com.android.boot.ui.panel.StoragePanelController;

public class MainActivity extends AppCompatActivity {
    private GameSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        session = new GameSession(this);
        GameView gameView = findViewById(R.id.game_view);
        TextView coins = findViewById(R.id.tv_coins);
        TextView level = findViewById(R.id.tv_level);
        TextView timer = findViewById(R.id.tv_timer);
        UiOverlayController overlay = new UiOverlayController(coins, level, timer);
        gameView.bind(session, overlay);

        SoundController sound = new SoundController();
        MenuPanelController menu = new MenuPanelController(this);
        AchievementPanelController ach = new AchievementPanelController(this);
        StoragePanelController storage = new StoragePanelController(this);
        OrderPanelController orders = new OrderPanelController(this);
        CodexPanelController codex = new CodexPanelController(this);
        DailyTaskPanelController tasks = new DailyTaskPanelController(this);
        LoginRewardPanelController login = new LoginRewardPanelController(this);

        Button btnSeed = findViewById(R.id.btn_seed);
        Button btnWater = findViewById(R.id.btn_water);
        Button btnFertilizer = findViewById(R.id.btn_fertilizer);
        ImageButton btnPause = findViewById(R.id.btn_pause);

        btnSeed.setOnClickListener(v -> {
            gameView.setTool(0);
            sound.click();
            menu.showHowToPlay();
        });
        btnWater.setOnClickListener(v -> {
            gameView.setTool(1);
            sound.click();
            tasks.show(session);
        });
        btnFertilizer.setOnClickListener(v -> {
            gameView.setTool(2);
            sound.click();
            orders.show(session);
            storage.show(session);
            ach.show(session);
            codex.show();
            login.show(session);
        });
        btnPause.setOnClickListener(v -> {
            sound.click();
            session.state = session.state == GameState.PAUSED ? GameState.PLAYING : GameState.PAUSED;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        session.progression.save();
        session.storage.save();
    }
}
