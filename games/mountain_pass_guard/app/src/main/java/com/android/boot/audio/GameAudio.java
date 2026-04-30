package com.android.boot.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameAudio {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> sounds = new HashMap<>();
    private MediaPlayer bgm;
    private boolean enabled = true;

    public GameAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();
        loadSound("ui_click", "audio/sfx_ui_click.wav", "audio/sfx_click.wav");
        loadSound("build_tower", "audio/sfx_build_tower.wav", "audio/sfx_build.wav");
        loadSound("upgrade", "audio/sfx_upgrade.wav");
        loadSound("archer_shot", "audio/sfx_archer_shot.wav", "audio/sfx_attack.wav");
        loadSound("stone_throw", "audio/sfx_stone_throw.wav", "audio/sfx_warning.wav");
        loadSound("shield_block", "audio/sfx_shield_block.wav", "audio/sfx_move.wav");
        loadSound("oil_fire", "audio/sfx_oil_fire.wav", "audio/sfx_attack.wav");
        loadSound("enemy_hit", "audio/sfx_enemy_hit.wav", "audio/sfx_hit.wav");
        loadSound("enemy_death", "audio/sfx_enemy_death.wav", "audio/sfx_collect.wav");
        loadSound("supply_pickup", "audio/sfx_supply_pickup.wav", "audio/sfx_collect.wav");
        loadSound("gate_damage", "audio/sfx_gate_damage.wav", "audio/sfx_warning.wav");
        loadSound("repair", "audio/sfx_repair.wav", "audio/sfx_heal.wav");
        loadSound("wave_start", "audio/sfx_wave_start.wav", "audio/sfx_warning.wav");
        loadSound("siege_warning", "audio/sfx_siege_warning.wav", "audio/sfx_warning.wav");
        loadSound("win", "audio/sfx_win.wav");
        loadSound("fail", "audio/sfx_fail.wav");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (bgm != null) {
            if (enabled && !bgm.isPlaying()) {
                bgm.start();
            } else if (!enabled && bgm.isPlaying()) {
                bgm.pause();
            }
        }
    }

    public void playMenu() {
        playBgm("audio/bgm_menu.wav", "audio/bgm.wav");
    }

    public void playGameplay() {
        playBgm("audio/bgm_gameplay.wav", "audio/bgm_play.wav", "audio/bgm.wav");
    }

    public void playClimax() {
        playBgm("audio/bgm_climax.wav", "audio/bgm_play.wav", "audio/bgm.wav");
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

    public void release() {
        stopBgm();
        soundPool.release();
    }

    private void loadSound(String key, String... paths) {
        for (String path : paths) {
            try {
                AssetFileDescriptor descriptor = context.getAssets().openFd(path);
                int id = soundPool.load(descriptor, 1);
                descriptor.close();
                sounds.put(key, id);
                return;
            } catch (IOException ignored) {
            }
        }
    }

    private void playBgm(String... paths) {
        stopBgm();
        for (String path : paths) {
            try {
                AssetFileDescriptor descriptor = context.getAssets().openFd(path);
                MediaPlayer player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();
                player.setLooping(true);
                player.prepare();
                bgm = player;
                if (enabled) {
                    bgm.start();
                }
                return;
            } catch (IOException ignored) {
            }
        }
        bgm = null;
    }

    private void stopBgm() {
        if (bgm != null) {
            bgm.stop();
            bgm.release();
            bgm = null;
        }
    }
}
