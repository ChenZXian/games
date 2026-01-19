package com.android.boot.entity;

public class Player {
    public float x;
    public float y;
    public float width;
    public float height;
    public float vy;
    public boolean onGround;
    public boolean chargingJump;
    public float jumpCharge;
    public float dashTime;
    public float dashCooldown;
    public float shieldTime;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onGround = true;
    }

    public void startJumpCharge() {
        if (onGround) {
            chargingJump = true;
        }
    }

    public void releaseJumpCharge(float jumpStrength) {
        if (onGround && chargingJump) {
            float scale = 0.4f + 0.6f * jumpCharge;
            vy = -jumpStrength * scale;
            onGround = false;
        }
        chargingJump = false;
        jumpCharge = 0f;
    }

    public void update(float dt, float gravity) {
        if (chargingJump) {
            jumpCharge = Math.min(1f, jumpCharge + dt * 2.5f);
        }
        if (!onGround) {
            vy += gravity * dt;
            y += vy * dt;
        }
        if (dashTime > 0f) {
            dashTime -= dt;
        }
        if (dashCooldown > 0f) {
            dashCooldown -= dt;
        }
        if (shieldTime > 0f) {
            shieldTime -= dt;
        }
    }

    public boolean isDashing() {
        return dashTime > 0f;
    }
}
