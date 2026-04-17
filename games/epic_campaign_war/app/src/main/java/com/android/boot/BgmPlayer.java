package com.android.boot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Simple BGM player shared across menu and gameplay.
 * Expects audio file at assets/audio/bgm.mp3 (or bgm.ogg/bgm.wav).
 */
public final class BgmPlayer {
    private static final float DEFAULT_VOLUME = 0.35f;

    private MediaPlayer player;
    private boolean muted;

    public void start(Context context) {
        if (player != null) {
            return;
        }

        String[] candidates = new String[]{"audio/bgm.mp3", "audio/bgm.ogg", "audio/bgm.wav"};
        for (String path : candidates) {
            try {
                AssetFileDescriptor afd = context.getAssets().openFd(path);
                MediaPlayer p = new MediaPlayer();
                p.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();

                p.setLooping(true);

                float volume = muted ? 0f : DEFAULT_VOLUME;
                p.setVolume(volume, volume);

                p.prepare();
                p.start();
                player = p;
                return;
            } catch (IOException ignored) {
            } catch (Throwable ignored) {
                // ignore and try next candidate
            }
        }
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            try {
                player.pause();
            } catch (Throwable ignored) {
            }
        }
    }

    public void resume() {
        if (player != null && !player.isPlaying() && !muted) {
            try {
                player.start();
            } catch (Throwable ignored) {
            }
        }
    }

    public void stop() {
        if (player == null) {
            return;
        }
        try {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
        } catch (Throwable ignored) {
        }
        player = null;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (player != null) {
            float volume = muted ? 0f : DEFAULT_VOLUME;
            try {
                player.setVolume(volume, volume);
                if (muted) {
                    pause();
                } else {
                    resume();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}

