package com.android.boot.audio;

import android.content.Context;

public class SoundManager {
    private final BgmSynth bgm;

    public SoundManager(Context context) {
        bgm = new BgmSynth();
    }

    public void playFlip() {
    }

    public void playCollect() {
    }

    public void playHit() {
    }

    public void playGameOver() {
    }

    public void startBgm() {
        bgm.start();
    }

    public void stopBgm() {
        bgm.stop();
    }

    public void release() {
        bgm.stop();
    }
}
