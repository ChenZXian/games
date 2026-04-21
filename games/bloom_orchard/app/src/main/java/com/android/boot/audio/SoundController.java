package com.android.boot.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundController {
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 30);
    private boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void click() {
        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_ACK, 40);
    }
}
