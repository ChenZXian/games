package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

public class BgmPlayer {
	private MediaPlayer player;
	private boolean muted = false;
	private static final float DEFAULT_VOLUME = 0.3f;

	public void start(Context context) {
		if (player != null) return;
		try {
			AssetFileDescriptor afd = context.getAssets().openFd("audio/bgm.mp3");
			player = new MediaPlayer();
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			player.setLooping(true);
			player.setVolume(muted ? 0f : DEFAULT_VOLUME, muted ? 0f : DEFAULT_VOLUME);
			player.prepare();
			player.start();
			afd.close();
		} catch (IOException e) {
			if (player != null) {
				try { player.release(); } catch (Throwable ignored) {}
				player = null;
			}
		}
	}

	public void pause() {
		if (player != null && player.isPlaying()) {
			try { player.pause(); } catch (Throwable ignored) {}
		}
	}

	public void resume() {
		if (player != null && !player.isPlaying() && !muted) {
			try { player.start(); } catch (Throwable ignored) {}
		}
	}

	public void stop() {
		if (player == null) return;
		try {
			if (player.isPlaying()) player.stop();
			player.release();
		} catch (Throwable ignored) {}
		player = null;
	}

	public void setMuted(boolean m) {
		muted = m;
		if (player != null) {
			float vol = muted ? 0f : DEFAULT_VOLUME;
			player.setVolume(vol, vol);
			if (muted && player.isPlaying()) pause();
			else if (!muted && !player.isPlaying()) resume();
		}
	}

	public boolean isMuted() {
		return muted;
	}
}

