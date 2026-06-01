package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioController {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> soundIds = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private boolean muted;
    private String currentBgm;

    public AudioController(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(muted ? 0f : 0.6f, muted ? 0f : 0.6f);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void playMenuBgm() {
        playBgm("audio/bgm_menu.wav");
    }

    public void playStageBgm(boolean bossStage) {
        playBgm(bossStage ? "audio/bgm_boss.wav" : "audio/bgm.wav");
    }

    private void playBgm(String assetPath) {
        if (assetPath.equals(currentBgm)) {
            return;
        }
        releaseBgm();
        currentBgm = assetPath;
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
            bgmPlayer = new MediaPlayer();
            bgmPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            bgmPlayer.setLooping(true);
            bgmPlayer.setVolume(muted ? 0f : 0.6f, muted ? 0f : 0.6f);
            bgmPlayer.prepare();
            bgmPlayer.start();
            descriptor.close();
        } catch (IOException ignored) {
            releaseBgm();
        }
    }

    public void playAttack() {
        playSound("audio/sfx_attack.wav", 0.75f);
    }

    public void playHeavy() {
        playSound("audio/sfx_heavy.wav", 0.85f);
    }

    public void playSkill() {
        playSound("audio/sfx_flame.wav", 0.9f);
    }

    public void playOverdrive() {
        playSound("audio/sfx_overdrive.wav", 1f);
    }

    public void playHit() {
        playSound("audio/sfx_hit.wav", 0.8f);
    }

    public void playPickup() {
        playSound("audio/sfx_pickup.wav", 0.7f);
    }

    public void playDefeat() {
        playSound("audio/sfx_defeat.wav", 0.8f);
    }

    private void playSound(String assetPath, float volume) {
        if (muted) {
            return;
        }
        Integer cached = soundIds.get(assetPath);
        int soundId = cached != null ? cached : loadSound(assetPath);
        if (soundId != 0) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        }
    }

    private int loadSound(String assetPath) {
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
            int soundId = soundPool.load(descriptor, 1);
            descriptor.close();
            soundIds.put(assetPath, soundId);
            return soundId;
        } catch (IOException ignored) {
            return 0;
        }
    }

    public void onPause() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void onResume() {
        if (bgmPlayer != null && !muted) {
            bgmPlayer.start();
        }
    }

    public void release() {
        releaseBgm();
        soundPool.release();
    }

    private void releaseBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
        }
        currentBgm = "";
    }
}
