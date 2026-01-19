package com.android.boot.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
    private final ToneGenerator toneGenerator;
    private boolean muted;
    private final SharedPreferences preferences;

    public SoundManager(Context context) {
        preferences = context.getSharedPreferences("sky_relic_runner_eclipse", Context.MODE_PRIVATE);
        muted = preferences.getBoolean("muted", false);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
    }

    public boolean isMuted() {
        return muted;
    }

    public void toggleMute() {
        muted = !muted;
        preferences.edit().putBoolean("muted", muted).apply();
    }

    public void playJump() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 120);
    }

    public void playDash() {
        playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 140);
    }

    public void playCollect() {
        playTone(ToneGenerator.TONE_PROP_ACK, 100);
    }

    public void playHit() {
        playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200);
    }

    public void playGameOver() {
        playTone(ToneGenerator.TONE_SUP_ERROR, 260);
    }

    private void playTone(int tone, int durationMs) {
        if (muted) {
            return;
        }
        toneGenerator.startTone(tone, durationMs);
    }
}
