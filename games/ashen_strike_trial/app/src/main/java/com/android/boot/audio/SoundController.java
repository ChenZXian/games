package com.android.boot.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.android.boot.R;

public class SoundController {
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
    private MediaPlayer bgmPlayer;
    private boolean muted;

    public void init(Context context) {
        if (bgmPlayer != null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        bgmPlayer = MediaPlayer.create(appContext, R.raw.bgm);
        if (bgmPlayer == null) {
            return;
        }
        bgmPlayer.setLooping(true);
        applyMuteState();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        applyMuteState();
    }

    public boolean isMuted() {
        return muted;
    }

    public void onResume() {
        if (muted || bgmPlayer == null || bgmPlayer.isPlaying()) {
            return;
        }
        bgmPlayer.start();
    }

    public void onPause() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void attack() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
        }
    }

    public void hit() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 50);
        }
    }

    public void clear() {
        if (!muted) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        }
    }

    public void release() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        toneGenerator.release();
    }

    private void applyMuteState() {
        if (bgmPlayer == null) {
            return;
        }
        if (muted) {
            if (bgmPlayer.isPlaying()) {
                bgmPlayer.pause();
            }
            return;
        }
        if (!bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }
}
