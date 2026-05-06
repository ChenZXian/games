package com.android.boot.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import com.android.boot.R;

public class AudioManager {
    private final ToneGenerator tone = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 60);
    private final MediaPlayer bgmPlayer;
    public boolean muted;

    public AudioManager(Context context) {
        bgmPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.bgm);
        if (bgmPlayer != null) {
            bgmPlayer.setLooping(true);
            bgmPlayer.setVolume(0.35f, 0.35f);
        }
    }

    public void click() {
        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
    }

    public void success() {
        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_ACK, 80);
    }

    public void error() {
        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_NACK, 80);
    }

    public void startBgm() {
        if (bgmPlayer == null || muted) return;
        if (!bgmPlayer.isPlaying()) bgmPlayer.start();
    }

    public void stopBgm() {
        if (bgmPlayer == null) return;
        if (bgmPlayer.isPlaying()) bgmPlayer.pause();
    }

    public void release() {
        stopBgm();
        if (bgmPlayer != null) bgmPlayer.release();
    }
}
