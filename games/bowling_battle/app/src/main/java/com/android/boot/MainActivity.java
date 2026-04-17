package com.android.boot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.ui.GameView;

public class MainActivity extends AppCompatActivity {
	private GameView gameView;
	private boolean muted = false;
	private LinearLayout startOverlay;
	private BgmPlayer bgmPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gameView = findViewById(R.id.game_view);
		bgmPlayer = new BgmPlayer();
		bgmPlayer.start(this);

		Button btnLeft = findViewById(R.id.btn_left);
		Button btnRight = findViewById(R.id.btn_right);
		Button btnRoll = findViewById(R.id.btn_roll);
		Button btnReset = findViewById(R.id.btn_reset);
		Button btnStartGame = findViewById(R.id.btn_start_game);
		ImageButton btnHelp = findViewById(R.id.btn_help);
		ImageButton btnMute = findViewById(R.id.btn_mute);
		startOverlay = findViewById(R.id.start_overlay);

		btnLeft.setOnTouchListener((v, e) -> {
			boolean hold = e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL;
			gameView.holdLeft(hold);
			return false;
		});
		btnRight.setOnTouchListener((v, e) -> {
			boolean hold = e.getAction() != MotionEvent.ACTION_UP && e.getAction() != MotionEvent.ACTION_CANCEL;
			gameView.holdRight(hold);
			return false;
		});
		btnRoll.setOnClickListener(v -> gameView.pressRoll());
		btnReset.setOnClickListener(v -> gameView.pressReset());
		btnStartGame.setOnClickListener(v -> {
			startOverlay.setVisibility(View.GONE);
			gameView.pressRoll();
		});

		btnHelp.setOnClickListener(v -> showHowToPlay());
		btnMute.setOnClickListener(v -> {
			muted = !muted;
			btnMute.setImageResource(muted ? R.drawable.ic_sound_off : R.drawable.ic_sound_on);
			if (bgmPlayer != null) {
				bgmPlayer.setMuted(muted);
			}
		});
	}

	private void showHowToPlay() {
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.dialog_title_help))
			.setMessage(getString(R.string.how_to_play))
			.setPositiveButton(getString(R.string.dialog_ok), null)
			.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (gameView != null) gameView.start();
		if (bgmPlayer != null) bgmPlayer.resume();
	}

	@Override
	protected void onPause() {
		if (gameView != null) gameView.stop();
		if (bgmPlayer != null) bgmPlayer.pause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (bgmPlayer != null) bgmPlayer.stop();
		super.onDestroy();
	}
}
