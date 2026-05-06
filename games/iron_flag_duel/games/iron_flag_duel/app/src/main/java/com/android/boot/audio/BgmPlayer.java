package com.android.boot.audio;

import android.content.Context;
import android.media.MediaPlayer;

public final class BgmPlayer {
    private static MediaPlayer player;
    private static int currentResId;

    private BgmPlayer() {
    }

    public static void playLoop(Context context, int resId, float volume) {
        if (context == null || resId == 0) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (player == null || currentResId != resId) {
            release();
            player = MediaPlayer.create(appContext, resId);
            if (player == null) {
                return;
            }
            player.setLooping(true);
            currentResId = resId;
        }
        player.setVolume(volume, volume);
        if (!player.isPlaying()) {
            player.start();
        }
    }

    public static void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public static void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
            currentResId = 0;
        }
    }
}
