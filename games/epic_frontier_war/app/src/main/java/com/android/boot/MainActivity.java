package com.android.boot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
	private GameView gameView;
	private boolean muted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gameView = findViewById(R.id.game_view);
		View startOverlay = findViewById(R.id.start_overlay);
		Button btnStart = findViewById(R.id.btn_start);
		ImageButton btnHelp = findViewById(R.id.btn_help);
		ImageButton btnMute = findViewById(R.id.btn_mute);

		btnStart.setOnClickListener(v -> {
			startOverlay.setVisibility(View.GONE);
			gameView.startBattle();
		});
		btnHelp.setOnClickListener(v -> showHelp());
		btnMute.setOnClickListener(v -> {
			muted = !muted;
			btnMute.setImageResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
		});
	}

	private void showHelp() {
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.btn_help))
			.setMessage(getString(R.string.how_to_play))
			.setPositiveButton(getString(R.string.btn_ok), null)
			.show();
	}
}
