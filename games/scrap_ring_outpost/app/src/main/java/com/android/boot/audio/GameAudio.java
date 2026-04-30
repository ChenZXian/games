package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;

public class GameAudio {
    private final Context context;
    private final SoundPool soundPool;
    private final HashMap<String, Integer> sfxIds = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private boolean enabled = true;
    private String activeTrack = "";

    public GameAudio(Context context) {
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

    public void playMenu() {
        playBgm("audio/bgm_menu.wav");
    }

    public void playGameplay() {
        playBgm("audio/bgm.wav");
    }

    public void playClimax() {
        playBgm("audio/bgm_climax.wav");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (bgmPlayer != null) {
            if (enabled) {
                bgmPlayer.setVolume(0.72f, 0.72f);
            } else {
                bgmPlayer.setVolume(0f, 0f);
            }
        }
    }

    public void playSfx(String key) {
        if (!enabled) {
            return;
        }
        Integer soundId = sfxIds.get(key);
        if (soundId == null) {
            soundId = loadSfx(key);
        }
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 0.85f, 0.85f, 1, 0, 1f);
        }
    }

    private Integer loadSfx(String key) {
        String path = mapSfx(key);
        if (path.length() == 0) {
            return 0;
        }
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(path);
            int soundId = soundPool.load(descriptor, 1);
            sfxIds.put(key, soundId);
            return soundId;
        } catch (IOException ignored) {
            return 0;
        }
    }

    private void playBgm(String assetPath) {
        if (assetPath.equals(activeTrack) && bgmPlayer != null) {
            if (!bgmPlayer.isPlaying()) {
                bgmPlayer.start();
            }
            return;
        }
        releasePlayer();
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            player.setLooping(true);
            player.setVolume(enabled ? 0.72f : 0f, enabled ? 0.72f : 0f);
            player.prepare();
            player.start();
            bgmPlayer = player;
            activeTrack = assetPath;
        } catch (IOException ignored) {
            activeTrack = "";
        }
    }

    private String mapSfx(String key) {
        if ("build".equals(key)) {
            return "audio/sfx_build.wav";
        }
        if ("upgrade".equals(key)) {
            return "audio/sfx_upgrade.wav";
        }
        if ("warning".equals(key)) {
            return "audio/sfx_warning.wav";
        }
        if ("repair".equals(key)) {
            return "audio/sfx_repair.wav";
        }
        if ("collect".equals(key)) {
            return "audio/sfx_collect.wav";
        }
        if ("win".equals(key)) {
            return "audio/sfx_win.wav";
        }
        if ("fail".equals(key)) {
            return "audio/sfx_fail.wav";
        }
        if ("ui_click".equals(key)) {
            return "audio/sfx_ui_click.wav";
        }
        if ("attack".equals(key)) {
            return "audio/sfx_attack.wav";
        }
        return "";
    }

    private void releasePlayer() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
        }
    }

    public void release() {
        releasePlayer();
        soundPool.release();
    }
}
