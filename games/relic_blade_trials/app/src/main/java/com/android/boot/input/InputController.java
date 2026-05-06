package com.android.boot.input;

import android.view.MotionEvent;

public class InputController {
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean attack;
    public boolean dash;
    public boolean pickup;
    public boolean pause;
    public boolean skill;
    private boolean pauseTapped;
    private boolean skillTapped;

    public void onTouch(MotionEvent event, int w, int h) {
        int action = event.getActionMasked();
        pauseTapped = false;
        skillTapped = false;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            left = false;
            right = false;
            jump = false;
            attack = false;
            dash = false;
            pickup = false;
            pause = false;
            skill = false;
            return;
        }
        left = false;
        right = false;
        jump = false;
        attack = false;
        dash = false;
        pickup = false;
        pause = false;
        skill = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (x < w * 0.12f && y > h * 0.82f) left = true;
            if (x > w * 0.12f && x < w * 0.24f && y > h * 0.82f) right = true;
            if (x > w * 0.72f && x < w * 0.84f && y > h * 0.82f) pickup = true;
            if (x > w * 0.84f && y > h * 0.82f) attack = true;
            if (x > w * 0.84f && y > h * 0.64f && y < h * 0.82f) jump = true;
            if (x > w * 0.72f && x < w * 0.84f && y > h * 0.64f && y < h * 0.82f) skill = true;
            if (x > w * 0.9f && y < h * 0.18f) pause = true;
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (x > w * 0.9f && y < h * 0.18f) {
                    pauseTapped = true;
                }
                if (x > w * 0.72f && x < w * 0.84f && y > h * 0.64f && y < h * 0.82f) {
                    skillTapped = true;
                }
            }
        }
    }

    public boolean consumePauseTap() {
        boolean tapped = pauseTapped;
        pauseTapped = false;
        return tapped;
    }

    public boolean consumeSkillTap() {
        boolean tapped = skillTapped;
        skillTapped = false;
        return tapped;
    }
}
