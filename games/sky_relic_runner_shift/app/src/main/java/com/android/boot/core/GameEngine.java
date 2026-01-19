package com.android.boot.core;

import android.content.SharedPreferences;

import com.android.boot.entity.Obstacle;
import com.android.boot.entity.Orb;
import com.android.boot.entity.Player;
import com.android.boot.fx.Particle;

import java.util.Random;

public class GameEngine {
    private static final int MAX_OBSTACLES = 18;
    private static final int MAX_ORBS = 18;
    private static final int MAX_PARTICLES = 80;
    private static final float GRAVITY = 2200f;
    private static final float JUMP_STRENGTH = 1200f;
    private static final float BASE_SPEED = 520f;
    private static final float SPEED_GROWTH = 22f;
    private static final float SPAWN_BASE = 1.2f;

    private final Obstacle[] obstacles = new Obstacle[MAX_OBSTACLES];
    private final Orb[] orbs = new Orb[MAX_ORBS];
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final Random random = new Random();
    private final SharedPreferences preferences;

    private Player player;
    private GameState state = GameState.MENU;
    private float width;
    private float height;
    private float groundY;
    private float speed;
    private float spawnTimer;
    private float distance;
    private int score;
    private int energy;
    private int bestScore;
    private int bestDistance;

    public GameEngine(SharedPreferences preferences) {
        this.preferences = preferences;
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle();
        }
        for (int i = 0; i < orbs.length; i++) {
            orbs[i] = new Orb();
        }
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
        bestScore = preferences.getInt("best_score", 0);
        bestDistance = preferences.getInt("best_distance", 0);
    }

    public void resize(float width, float height) {
        this.width = width;
        this.height = height;
        groundY = height * 0.74f;
        if (player == null) {
            player = new Player(width * 0.18f, groundY - height * 0.18f, height * 0.09f, height * 0.14f);
        }
    }

    public void resetRun() {
        for (Obstacle obstacle : obstacles) {
            obstacle.active = false;
        }
        for (Orb orb : orbs) {
            orb.active = false;
        }
        for (Particle particle : particles) {
            particle.active = false;
        }
        player.x = width * 0.18f;
        player.y = groundY - player.height;
        player.vy = 0f;
        player.onGround = true;
        player.chargingJump = false;
        player.jumpCharge = 0f;
        player.dashTime = 0f;
        player.dashCooldown = 0f;
        player.shieldTime = 0f;
        speed = BASE_SPEED;
        spawnTimer = 0.5f;
        distance = 0f;
        score = 0;
        energy = 60;
    }

    public void setState(GameState newState) {
        state = newState;
    }

    public GameState getState() {
        return state;
    }

    public void update(float dt) {
        if (state != GameState.PLAYING) {
            return;
        }
        speed = BASE_SPEED + distance * 0.015f + SPEED_GROWTH * (distance / 500f);
        distance += speed * dt * 0.1f;

        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            spawnPattern();
            float scale = Math.max(0.55f, 1.2f - distance * 0.002f);
            spawnTimer = SPAWN_BASE * scale + random.nextFloat() * 0.3f;
        }

        player.update(dt, GRAVITY);
        player.onGround = false;

        if (player.y + player.height >= groundY) {
            player.y = groundY - player.height;
            player.vy = 0f;
            player.onGround = true;
        }

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.active) {
                continue;
            }
            obstacle.x -= speed * dt;
            if (obstacle.moveRange > 0f) {
                obstacle.y = obstacle.baseY + (float) Math.sin(distance * 0.05f + obstacle.x * 0.01f) * obstacle.moveRange;
            }
            if (obstacle.type == Obstacle.Type.PLATFORM && obstacle.collapsing) {
                obstacle.collapseTimer -= dt;
                if (obstacle.collapseTimer <= 0f) {
                    obstacle.active = false;
                }
            }
            if (obstacle.x + obstacle.width < -100f) {
                obstacle.active = false;
            }
        }

        for (Orb orb : orbs) {
            if (!orb.active) {
                continue;
            }
            orb.x -= speed * dt;
            if (orb.x + orb.radius < -50f) {
                orb.active = false;
            }
        }

        for (Particle particle : particles) {
            particle.update(dt);
        }

        handleCollisions();
    }

    private void spawnPattern() {
        float spawnX = width + 60f;
        float platformWidth = width * (0.18f + random.nextFloat() * 0.18f);
        float platformY = groundY;
        boolean upper = random.nextFloat() > 0.6f;
        if (upper) {
            platformY = groundY - height * (0.18f + random.nextFloat() * 0.15f);
        }
        Obstacle platform = findObstacle();
        if (platform != null) {
            platform.reset(spawnX, platformY, platformWidth, height * 0.05f, Obstacle.Type.PLATFORM);
            if (random.nextFloat() > 0.7f) {
                platform.collapsing = true;
                platform.collapseTimer = 0.7f + random.nextFloat() * 0.4f;
            }
        }

        if (random.nextFloat() > 0.35f) {
            Obstacle spike = findObstacle();
            if (spike != null) {
                spike.reset(spawnX + platformWidth * 0.5f, groundY - height * 0.08f, height * 0.06f, height * 0.08f, Obstacle.Type.SPIKE);
            }
        }

        if (random.nextFloat() > 0.55f) {
            Obstacle block = findObstacle();
            if (block != null) {
                float blockY = groundY - height * (0.22f + random.nextFloat() * 0.2f);
                block.reset(spawnX + platformWidth * 0.7f, blockY, height * 0.08f, height * 0.12f, Obstacle.Type.BLOCK);
                block.moveRange = height * 0.08f;
            }
        }

        spawnOrbs(spawnX + platformWidth * 0.5f, platformY - height * 0.08f, upper);
    }

    private void spawnOrbs(float x, float y, boolean upper) {
        float orbY = upper ? y - height * 0.08f : y - height * 0.2f;
        Orb shard = findOrb();
        if (shard != null) {
            shard.reset(x, orbY, height * 0.03f, Orb.Type.SHARD);
        }
        if (random.nextFloat() > 0.6f) {
            Orb core = findOrb();
            if (core != null) {
                core.reset(x + height * 0.12f, orbY - height * 0.06f, height * 0.035f, Orb.Type.CORE);
            }
        }
        if (random.nextFloat() > 0.85f) {
            Orb rune = findOrb();
            if (rune != null) {
                rune.reset(x + height * 0.2f, orbY - height * 0.1f, height * 0.038f, Orb.Type.RUNE);
            }
        }
    }

    private Obstacle findObstacle() {
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.active) {
                return obstacle;
            }
        }
        return null;
    }

    private Orb findOrb() {
        for (Orb orb : orbs) {
            if (!orb.active) {
                return orb;
            }
        }
        return null;
    }

    private Particle findParticle() {
        for (Particle particle : particles) {
            if (!particle.active) {
                return particle;
            }
        }
        return null;
    }

    private void handleCollisions() {
        float px = player.x;
        float py = player.y;
        float pw = player.width;
        float ph = player.height;

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.active) {
                continue;
            }
            boolean overlaps = px < obstacle.x + obstacle.width && px + pw > obstacle.x
                    && py < obstacle.y + obstacle.height && py + ph > obstacle.y;
            if (!overlaps) {
                continue;
            }
            if (obstacle.type == Obstacle.Type.PLATFORM) {
                if (player.vy >= 0f && py + ph - obstacle.y < ph * 0.6f) {
                    player.y = obstacle.y - ph;
                    player.vy = 0f;
                    player.onGround = true;
                    if (obstacle.collapsing) {
                        obstacle.collapseTimer -= 0.02f;
                    }
                }
            } else if (obstacle.isHazard()) {
                if (!player.isDashing() && player.shieldTime <= 0f) {
                    state = GameState.GAME_OVER;
                }
            }
        }

        for (Orb orb : orbs) {
            if (!orb.active) {
                continue;
            }
            float dx = (px + pw * 0.5f) - orb.x;
            float dy = (py + ph * 0.5f) - orb.y;
            float dist2 = dx * dx + dy * dy;
            float limit = (orb.radius + pw * 0.45f);
            if (dist2 <= limit * limit) {
                orb.active = false;
                if (orb.type == Orb.Type.SHARD) {
                    score += 10;
                } else if (orb.type == Orb.Type.CORE) {
                    energy = Math.min(100, energy + 20);
                } else {
                    player.shieldTime = 4f;
                }
                emitParticles(orb.x, orb.y, 6);
            }
        }

        if (state == GameState.GAME_OVER) {
            bestScore = Math.max(bestScore, score);
            bestDistance = Math.max(bestDistance, (int) distance);
            preferences.edit().putInt("best_score", bestScore).putInt("best_distance", bestDistance).apply();
        }
    }

    private void emitParticles(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Particle particle = findParticle();
            if (particle == null) {
                return;
            }
            float angle = (float) (random.nextFloat() * Math.PI * 2f);
            float speedValue = 120f + random.nextFloat() * 160f;
            particle.reset(x, y, (float) Math.cos(angle) * speedValue, (float) Math.sin(angle) * speedValue, 0.6f, 6f + random.nextFloat() * 6f);
        }
    }

    public void jumpPress() {
        if (state == GameState.PLAYING) {
            player.startJumpCharge();
        }
    }

    public void jumpRelease() {
        if (state == GameState.PLAYING) {
            player.releaseJumpCharge(JUMP_STRENGTH);
        }
    }

    public boolean dash() {
        if (state != GameState.PLAYING) {
            return false;
        }
        if (player.dashCooldown <= 0f && energy >= 25) {
            player.dashTime = 0.25f;
            player.dashCooldown = 1.1f;
            energy -= 25;
            return true;
        }
        return false;
    }

    public Player getPlayer() {
        return player;
    }

    public Obstacle[] getObstacles() {
        return obstacles;
    }

    public Orb[] getOrbs() {
        return orbs;
    }

    public Particle[] getParticles() {
        return particles;
    }

    public int getScore() {
        return score;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getBestDistance() {
        return bestDistance;
    }

    public int getEnergy() {
        return energy;
    }

    public float getDistance() {
        return distance;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean hasShield() {
        return player.shieldTime > 0f;
    }
}
