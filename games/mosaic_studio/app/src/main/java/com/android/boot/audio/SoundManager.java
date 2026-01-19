package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
    private ToneGenerator tone;
    private boolean muted;

    public SoundManager() {
        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void playClick() {
        if (muted) {
            return;
        }
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
    }

    public void playWin() {
        if (muted) {
            return;
        }
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 140);
    }

    public void release() {
        tone.release();
    }
}
