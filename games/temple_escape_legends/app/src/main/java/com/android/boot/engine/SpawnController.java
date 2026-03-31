package com.android.boot.engine;

import com.android.boot.model.Obstacle;
import com.android.boot.model.Pickup;

public class SpawnController {
    private float obstacleCooldown;
    private float pickupCooldown;
    private int seed = 7;

    private int nextRand() {
        seed = seed * 1103515245 + 12345;
        return (seed >>> 1) & 0x7fffffff;
    }

    public void reset() {
        obstacleCooldown = 2f;
        pickupCooldown = 1.5f;
        seed = 7;
    }

    public boolean shouldSpawnObstacle(float dt, float speed, float distance) {
        obstacleCooldown -= dt;
        if (obstacleCooldown <= 0f) {
            float density = Math.max(0.35f, 1.5f - speed * 0.025f - distance * 0.0004f);
            obstacleCooldown = density;
            return true;
        }
        return false;
    }

    public boolean shouldSpawnPickup(float dt) {
        pickupCooldown -= dt;
        if (pickupCooldown <= 0f) {
            pickupCooldown = 2.2f + (nextRand() % 80) * 0.01f;
            return true;
        }
        return false;
    }

    public void fillObstacle(Obstacle o, int stage, boolean endless) {
        int r = nextRand() % 10;
        o.active = true;
        o.lane = nextRand() % 3;
        o.type = r;
        if (!endless && stage == 6 && (nextRand() % 6 == 0)) {
            o.type = Obstacle.ROLLING;
        }
        o.z = 120f;
        o.xDrift = (nextRand() % 3) - 1;
    }

    public void fillPickup(Pickup p, float distance) {
        int r = nextRand() % 100;
        p.active = true;
        p.lane = nextRand() % 3;
        p.type = Pickup.COIN;
        if (r > 82) p.type = Pickup.SHIELD;
        if (r > 90) p.type = Pickup.SLOW;
        if (r > 95) p.type = Pickup.MAGNET;
        if (r > 98 && distance > 200f) p.type = Pickup.REVIVE;
        p.z = 118f;
    }
}
