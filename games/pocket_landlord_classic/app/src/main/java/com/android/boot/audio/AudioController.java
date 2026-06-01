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
    private final Map<String, Integer> loadedSounds = new HashMap<>();
    private MediaPlayer music;
    private boolean muted;

    public AudioController(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_GAME).build();
        soundPool = new SoundPool.Builder().setAudioAttributes(attrs).setMaxStreams(6).build();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (music != null) {
            if (muted) {
                music.pause();
            } else if (!music.isPlaying()) {
                music.start();
            }
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void playMusic(String assetPath, boolean loop) {
        if (muted) {
            return;
        }
        stopMusic();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            music = new MediaPlayer();
            music.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            music.setLooping(loop);
            music.prepare();
            music.start();
            afd.close();
        } catch (IOException ignored) {
        }
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.release();
            music = null;
        }
    }

    public void playSfx(String key, String assetPath) {
        if (muted) {
            return;
        }
        Integer soundId = loadedSounds.get(key);
        if (soundId == null) {
            try {
                AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
                soundId = soundPool.load(afd, 1);
                loadedSounds.put(key, soundId);
                afd.close();
            } catch (IOException ignored) {
                return;
            }
        }
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    public void release() {
        stopMusic();
        soundPool.release();
    }
}
