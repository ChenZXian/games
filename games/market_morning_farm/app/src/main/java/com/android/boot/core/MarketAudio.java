package com.android.boot.core;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;

import java.io.IOException;

public final class MarketAudio {
    private final Context context;
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);
    private MediaPlayer bgmPlayer;
    private String currentBgm = "";
    private boolean enabled = true;

    public MarketAudio(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            resumeBgm();
        } else {
            pauseBgm();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void playMenuBgm() {
        playBgm("audio/bgm_menu.wav", true);
    }

    public void playGameBgm() {
        playBgm("audio/bgm.wav", true);
    }

    public void playWinBgm() {
        playBgm("audio/bgm_win.wav", false);
    }

    public void playFailBgm() {
        playBgm("audio/bgm_fail.wav", false);
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void resumeBgm() {
        if (enabled && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public void playSale() {
        play(ToneGenerator.TONE_PROP_ACK, 80);
    }

    public void playError() {
        play(ToneGenerator.TONE_PROP_NACK, 90);
    }

    public void playSelect() {
        play(ToneGenerator.TONE_PROP_BEEP, 50);
    }

    private void playBgm(String assetPath, boolean loop) {
        if (!enabled) {
            return;
        }
        if (assetPath.equals(currentBgm) && bgmPlayer != null) {
            bgmPlayer.setLooping(loop);
            resumeBgm();
            return;
        }
        stopBgm();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            player.setAudioAttributes(attributes);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(loop);
            player.setVolume(0.55f, 0.55f);
            player.prepare();
            player.start();
            bgmPlayer = player;
            currentBgm = assetPath;
        } catch (IOException ignored) {
            currentBgm = "";
        }
    }

    private void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
        }
        currentBgm = "";
    }

    private void play(int tone, int durationMs) {
        if (enabled) {
            toneGenerator.startTone(tone, durationMs);
        }
    }

    public void release() {
        stopBgm();
        toneGenerator.release();
    }
}
