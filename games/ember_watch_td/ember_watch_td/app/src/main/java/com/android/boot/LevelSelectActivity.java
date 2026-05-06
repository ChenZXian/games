package com.android.boot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.android.boot.audio.BgmPlayer;

public class LevelSelectActivity extends Activity {
    public static final String EXTRA_LEVEL_INDEX = "level_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        bindLevelButton(R.id.btn_level_1, 1, R.string.level_1);
        bindLevelButton(R.id.btn_level_2, 2, R.string.level_2);
        bindLevelButton(R.id.btn_level_3, 3, R.string.level_3);

        TextView back = findViewById(R.id.btn_back_menu);
        back.setOnClickListener(v -> finish());
    }

    private void bindLevelButton(int buttonId, int levelIndex, int labelResId) {
        Button button = findViewById(buttonId);
        button.setText(getString(labelResId));
        button.setOnClickListener(v -> {
            Intent intent = new Intent(LevelSelectActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_LEVEL_INDEX, levelIndex);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BgmPlayer.playLoop(this, R.raw.bgm, 0.42f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BgmPlayer.pause();
    }
}
