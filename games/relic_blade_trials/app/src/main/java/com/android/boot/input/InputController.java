package com.android.boot.input;

import android.view.MotionEvent;

public class InputController {
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean attack;
    public boolean dash;
    public boolean pause;

    public void onTouch(MotionEvent event, int w, int h) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            left = false;
            right = false;
            jump = false;
            attack = false;
            dash = false;
            pause = false;
            return;
        }
        left = false;
        right = false;
        jump = false;
        attack = false;
        dash = false;
        pause = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (x < w * 0.2f && y > h * 0.55f) left = true;
            if (x > w * 0.2f && x < w * 0.4f && y > h * 0.55f) right = true;
            if (x > w * 0.65f && y > h * 0.55f && y < h * 0.78f) attack = true;
            if (x > w * 0.82f && y > h * 0.55f && y < h * 0.78f) jump = true;
            if (x > w * 0.65f && y > h * 0.78f) dash = true;
            if (x > w * 0.9f && y < h * 0.18f) pause = true;
        }
    }
}
