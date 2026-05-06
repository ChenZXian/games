package com.android.boot.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public final class BgmPlayer {
    private static final String PREFS_NAME = "audio_settings";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static MediaPlayer player;
    private static int currentResId;
    private static boolean musicEnabled = true;
    private static boolean loaded;

    private BgmPlayer() {
    }

    public static void playLoop(Context context, int resId, float volume) {
        if (context == null || resId == 0) return;
        Context appContext = context.getApplicationContext();
        ensureLoaded(appContext);
        if (!musicEnabled) {
            pause();
            return;
        }
        if (player == null || currentResId != resId) {
            release();
            player = MediaPlayer.create(appContext, resId);
            if (player == null) return;
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

    public static void resume() {
        if (player != null && !player.isPlaying()) {
            player.start();
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

    public static boolean isMusicEnabled(Context context) {
        if (context != null) {
            ensureLoaded(context.getApplicationContext());
        }
        return musicEnabled;
    }

    public static void setMusicEnabled(Context context, boolean enabled) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        ensureLoaded(appContext);
        musicEnabled = enabled;
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();
        if (!enabled) {
            pause();
        }
    }

    private static void ensureLoaded(Context appContext) {
        if (loaded) return;
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, true);
        loaded = true;
    }
}
