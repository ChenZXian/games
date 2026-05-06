package com.android.boot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.android.boot.audio.BgmPlayer;

public class MainActivity extends Activity {
    private Button musicToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button startButton = findViewById(R.id.btn_start_game);
        Button howToPlayButton = findViewById(R.id.btn_how_to_play);
        musicToggleButton = findViewById(R.id.btn_music_toggle);

        startButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LevelSelectActivity.class))
        );
        howToPlayButton.setOnClickListener(v -> showHowToPlayDialog());
        musicToggleButton.setOnClickListener(v -> {
            boolean enabled = BgmPlayer.isMusicEnabled(MainActivity.this);
            BgmPlayer.setMusicEnabled(MainActivity.this, !enabled);
            updateMusicToggleText();
            if (!enabled) {
                BgmPlayer.playLoop(MainActivity.this, R.raw.bgm, 0.42f);
            }
        });
        updateMusicToggleText();
    }

    private void showHowToPlayDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_play_title)
                .setMessage(R.string.how_to_play_body)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BgmPlayer.playLoop(this, R.raw.bgm, 0.42f);
        updateMusicToggleText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BgmPlayer.pause();
    }

    private void updateMusicToggleText() {
        if (musicToggleButton == null) return;
        boolean enabled = BgmPlayer.isMusicEnabled(this);
        musicToggleButton.setText(enabled ? R.string.music_on : R.string.music_off);
    }
}
