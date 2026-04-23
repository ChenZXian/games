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
    private final HashMap<String, Integer> sounds = new HashMap<>();
    private MediaPlayer music;
    private boolean enabled = true;
    private String currentTrack = "";

    public GameAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build();
        loadSfx("ui_click");
        loadSfx("build_tower");
        loadSfx("upgrade");
        loadSfx("archer_shot");
        loadSfx("cannon_shot");
        loadSfx("magic_bolt");
        loadSfx("soldier_block");
        loadSfx("hero_move");
        loadSfx("hero_attack");
        loadSfx("reinforcement_summon");
        loadSfx("enemy_hit");
        loadSfx("enemy_death");
        loadSfx("gold_pickup");
        loadSfx("wave_start");
        loadSfx("warning_horn");
        loadSfx("spell_cast");
        loadSfx("win");
        loadSfx("fail");
    }

    private void loadSfx(String key) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/sfx_" + key + ".wav");
            int id = soundPool.load(afd, 1);
            sounds.put(key, id);
            afd.close();
        } catch (IOException ignored) {
        }
    }

    public void playMenu() {
        playMusic("audio/bgm_menu.wav", "menu");
    }

    public void playGameplay() {
        playMusic("audio/bgm_gameplay.wav", "gameplay");
    }

    public void playClimax() {
        playMusic("audio/bgm_climax.wav", "climax");
    }

    private void playMusic(String asset, String key) {
        if (!enabled) {
            return;
        }
        if (key.equals(currentTrack) && music != null && music.isPlaying()) {
            return;
        }
        stopMusic();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(asset);
            music = new MediaPlayer();
            music.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            music.setLooping(true);
            music.setVolume(0.38f, 0.38f);
            music.prepare();
            music.start();
            currentTrack = key;
        } catch (IOException ignored) {
            currentTrack = "";
        }
    }

    public void playSfx(String key) {
        if (!enabled) {
            return;
        }
        Integer id = sounds.get(key);
        if (id != null) {
            soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopMusic();
        } else if (currentTrack.length() == 0) {
            playMenu();
        }
    }

    private void stopMusic() {
        if (music != null) {
            music.stop();
            music.release();
            music = null;
        }
        currentTrack = "";
    }

    public void release() {
        stopMusic();
        soundPool.release();
    }
}
