package com.android.boot.engine;

import com.android.boot.input.InputState;
import com.android.boot.model.FloatText;
import com.android.boot.model.Obstacle;
import com.android.boot.model.Particle;
import com.android.boot.model.Pickup;
import com.android.boot.model.RunSession;
import com.android.boot.model.Runner;
import com.android.boot.model.StageDefinition;
import com.android.boot.model.StageRepository;

public class GameWorld {
    private final Runner runner = new Runner();
    private final RunSession session = new RunSession();
    private final SpawnController spawnController = new SpawnController();
    private final StageRepository stages = new StageRepository();
    private final Obstacle[] obstacles = new Obstacle[48];
    private final Pickup[] pickups = new Pickup[64];
    private final FloatText[] floatTexts = new FloatText[24];
    private final Particle[] particles = new Particle[40];
    private float speed = 20f;
    private float gravity = 45f;
    private GameState state = GameState.MENU;
    private float revivePromptTimer;

    public GameWorld() {
        for (int i = 0; i < obstacles.length; i++) obstacles[i] = new Obstacle();
        for (int i = 0; i < pickups.length; i++) pickups[i] = new Pickup();
        for (int i = 0; i < floatTexts.length; i++) floatTexts[i] = new FloatText();
        for (int i = 0; i < particles.length; i++) particles[i] = new Particle();
    }

    public void start(GameMode mode, int stage) {
        state = GameState.PLAYING;
        session.mode = mode;
        session.stage = stage;
        session.score = 0f;
        session.distance = 0f;
        session.coinsRun = 0;
        session.speedTier = 1;
        session.bossPressure = 0.1f;
        session.shield = false;
        session.slowTimer = 0f;
        session.magnetTimer = 0f;
        session.reviveToken = false;
        session.revived = false;
        runner.currentLane = 1;
        runner.targetLane = 1;
        runner.laneBlend = 1f;
        runner.y = 0f;
        runner.vy = 0f;
        runner.airborne = false;
        runner.sliding = false;
        runner.slideTimer = 0f;
        runner.life = 3;
        runner.invulnTimer = 0f;
        speed = mode == GameMode.STAGE ? stages.get(stage).baseSpeed : 21f;
        spawnController.reset();
        deactivateAll();
    }

    private void deactivateAll() {
        for (Obstacle o : obstacles) o.active = false;
        for (Pickup p : pickups) p.active = false;
        for (FloatText f : floatTexts) f.active = false;
        for (Particle p : particles) p.active = false;
    }

    public void update(float dt, InputState input) {
        if (state != GameState.PLAYING && state != GameState.REVIVE_PROMPT) return;
        if (state == GameState.REVIVE_PROMPT) {
            revivePromptTimer -= dt;
            if (revivePromptTimer <= 0f) applyRevive();
            return;
        }
        float worldFactor = session.slowTimer > 0f ? 0.55f : 1f;
        float worldDt = dt * worldFactor;
        if (session.mode == GameMode.ENDLESS) speed += worldDt * 0.16f;
        session.speedTier = 1 + (int) ((speed - 20f) / 4f);
        if (session.slowTimer > 0f) session.slowTimer -= dt;
        if (session.magnetTimer > 0f) session.magnetTimer -= dt;
        if (runner.invulnTimer > 0f) runner.invulnTimer -= dt;

        applyInput(input);
        updateRunner(worldDt);
        updateSpawns(worldDt);
        updateEntities(worldDt);
        updateBoss(worldDt);
        updateFx(worldDt);

        session.distance += speed * worldDt;
        session.score += (speed * worldDt) + session.coinsRun * 0.04f;

        if (session.mode == GameMode.STAGE) {
            StageDefinition def = stages.get(session.stage);
            if (session.distance >= def.distanceTarget) state = GameState.STAGE_CLEAR;
        }
    }

    private void applyInput(InputState input) {
        if (input.leftPressed && runner.targetLane > 0) runner.targetLane--;
        if (input.rightPressed && runner.targetLane < 2) runner.targetLane++;
        if (input.upPressed && !runner.airborne && !runner.sliding) {
            runner.airborne = true;
            runner.vy = 16.5f;
        }
        if (input.downPressed && !runner.airborne && !runner.sliding) {
            runner.sliding = true;
            runner.slideTimer = 0.65f;
        }
        input.consume();
    }

    private void updateRunner(float dt) {
        float laneSpeed = 10f;
        if (runner.currentLane != runner.targetLane) {
            runner.laneBlend -= dt * laneSpeed;
            if (runner.laneBlend <= 0f) {
                runner.currentLane = runner.targetLane;
                runner.laneBlend = 1f;
            }
        }
        if (runner.airborne) {
            runner.vy -= gravity * dt;
            runner.y += runner.vy * dt;
            if (runner.y <= 0f) {
                runner.y = 0f;
                runner.vy = 0f;
                runner.airborne = false;
            }
        }
        if (runner.sliding) {
            runner.slideTimer -= dt;
            if (runner.slideTimer <= 0f) runner.sliding = false;
        }
    }

    private void updateSpawns(float dt) {
        boolean endless = session.mode == GameMode.ENDLESS;
        if (spawnController.shouldSpawnObstacle(dt, speed, session.distance)) {
            for (Obstacle o : obstacles) {
                if (!o.active) {
                    spawnController.fillObstacle(o, session.stage, endless);
                    break;
                }
            }
        }
        if (spawnController.shouldSpawnPickup(dt)) {
            for (Pickup p : pickups) {
                if (!p.active) {
                    spawnController.fillPickup(p, session.distance);
                    break;
                }
            }
        }
    }

    private void updateEntities(float dt) {
        for (Obstacle o : obstacles) {
            if (!o.active) continue;
            o.z -= speed * dt;
            if (o.type == Obstacle.ROLLING) o.lane = Math.max(0, Math.min(2, o.lane + (o.xDrift > 0 ? 1 : -1)));
            if (o.z < 2f) {
                if (o.lane == runner.currentLane) resolveObstacleHit(o.type);
                o.active = false;
            }
        }
        for (Pickup p : pickups) {
            if (!p.active) continue;
            p.z -= speed * dt;
            if (session.magnetTimer > 0f && p.type == Pickup.COIN && p.z < 26f && Math.abs(p.lane - runner.currentLane) <= 1) p.lane = runner.currentLane;
            if (p.z < 2f) {
                if (p.lane == runner.currentLane) collectPickup(p.type);
                p.active = false;
            }
        }
    }

    private void resolveObstacleHit(int type) {
        if (runner.invulnTimer > 0f) return;
        boolean avoid = true;
        if (type == Obstacle.LOW || type == Obstacle.GAP || type == Obstacle.JUMP_CHAIN) avoid = runner.airborne;
        if (type == Obstacle.OVERHEAD || type == Obstacle.SLIDE_CHAIN) avoid = runner.sliding;
        if (!avoid) {
            if (session.shield) {
                session.shield = false;
                spawnFloat("Shield");
            } else {
                runner.life--;
                session.bossPressure += 0.17f;
                spawnFloat("Hit");
                runner.invulnTimer = 1.2f;
                emitBurst();
                if (runner.life <= 0 || session.bossPressure >= 1f) {
                    if (session.reviveToken && !session.revived) {
                        state = GameState.REVIVE_PROMPT;
                        revivePromptTimer = 2.5f;
                    } else {
                        state = GameState.GAME_OVER;
                    }
                }
            }
        }
    }

    private void collectPickup(int type) {
        if (type == Pickup.COIN) {
            session.coinsRun += 1;
            session.score += 10;
            spawnFloat("+1");
        } else if (type == Pickup.SHIELD) {
            session.shield = true;
            spawnFloat("Shield");
        } else if (type == Pickup.SLOW) {
            session.slowTimer = 4.5f;
            spawnFloat("Slow");
        } else if (type == Pickup.MAGNET) {
            session.magnetTimer = 6f;
            spawnFloat("Magnet");
        } else if (type == Pickup.REVIVE) {
            session.reviveToken = true;
            spawnFloat("Revive");
        }
    }

    private void updateBoss(float dt) {
        float recovery = 0.015f * dt;
        if (session.mode == GameMode.ENDLESS && session.distance > 120f) session.bossPressure += 0.008f * dt;
        if (session.mode == GameMode.STAGE) {
            StageDefinition def = stages.get(session.stage);
            if (session.distance > def.bossSurgeA) session.bossPressure += 0.02f * dt;
            if (def.bossSurgeB > 0f && session.distance > def.bossSurgeB) session.bossPressure += 0.025f * dt;
        }
        session.bossPressure = Math.max(0f, Math.min(1f, session.bossPressure - recovery));
        if (session.bossPressure >= 1f) {
            if (session.reviveToken && !session.revived) {
                state = GameState.REVIVE_PROMPT;
                revivePromptTimer = 2.5f;
            } else {
                state = GameState.GAME_OVER;
            }
        }
    }

    private void updateFx(float dt) {
        for (FloatText f : floatTexts) {
            if (!f.active) continue;
            f.y -= 18f * dt;
            f.ttl -= dt;
            if (f.ttl <= 0f) f.active = false;
        }
        for (Particle p : particles) {
            if (!p.active) continue;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.ttl -= dt;
            if (p.ttl <= 0f) p.active = false;
        }
    }

    private void emitBurst() {
        for (int i = 0; i < particles.length; i++) {
            Particle p = particles[i];
            if (!p.active) {
                p.active = true;
                p.x = 0;
                p.y = 0;
                p.vx = (i % 5 - 2) * 45f;
                p.vy = -20f - (i % 4) * 15f;
                p.ttl = 0.5f;
            }
        }
    }

    private void spawnFloat(String text) {
        for (FloatText f : floatTexts) {
            if (!f.active) {
                f.active = true;
                f.ttl = 0.7f;
                f.x = 0f;
                f.y = 0f;
                f.text = text;
                return;
            }
        }
    }

    public void applyRevive() {
        if (!session.reviveToken || session.revived) return;
        session.revived = true;
        session.reviveToken = false;
        runner.life = 2;
        runner.invulnTimer = 2f;
        session.bossPressure = Math.max(0.3f, session.bossPressure - 0.35f);
        state = GameState.PLAYING;
        for (Obstacle o : obstacles) {
            if (o.active && o.z < 8f) o.active = false;
        }
    }

    public Runner getRunner() { return runner; }
    public RunSession getSession() { return session; }
    public Obstacle[] getObstacles() { return obstacles; }
    public Pickup[] getPickups() { return pickups; }
    public FloatText[] getFloatTexts() { return floatTexts; }
    public Particle[] getParticles() { return particles; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
}
