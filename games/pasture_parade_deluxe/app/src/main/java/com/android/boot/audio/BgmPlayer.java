package com.android.boot.audio;

import android.content.Context;
import android.media.MediaPlayer;

public class BgmPlayer {
    private final Context context;
    private MediaPlayer player;
    private boolean muted;

    public BgmPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        applyVolume();
    }

    public boolean isMuted() {
        return muted;
    }

    public void playLoop(int resId, float volume) {
        if (resId == 0) return;
        if (player == null) {
            player = MediaPlayer.create(context, resId);
            if (player == null) return;
            player.setLooping(true);
        }
        player.setVolume(volume, volume);
        applyVolume();
        if (!player.isPlaying()) {
            player.start();
        }
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
    }

    private void applyVolume() {
        if (player == null) return;
        float v = muted ? 0f : 1f;
        player.setVolume(v, v);
    }
}

