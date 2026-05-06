package com.android.boot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.android.boot.audio.BgmPlayer;

public class LevelSelectActivity extends Activity {
    public static final String EXTRA_STAGE_INDEX = "stage_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        bindStageButton(R.id.btn_stage_1, 1, R.string.stage_1);
        bindStageButton(R.id.btn_stage_2, 2, R.string.stage_2);
        bindStageButton(R.id.btn_stage_3, 3, R.string.stage_3);
        bindStageButton(R.id.btn_stage_4, 4, R.string.stage_4);
        bindStageButton(R.id.btn_stage_5, 5, R.string.stage_5);
        bindStageButton(R.id.btn_stage_6, 6, R.string.stage_6);

        TextView back = findViewById(R.id.btn_back_menu);
        back.setOnClickListener(v -> finish());
    }

    private void bindStageButton(int buttonId, int stageIndex, int stageLabelResId) {
        Button button = findViewById(buttonId);
        button.setText(getString(stageLabelResId));
        button.setOnClickListener(v -> {
            Intent intent = new Intent(LevelSelectActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_STAGE_INDEX, stageIndex);
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
