package com.android.boot.core;

import android.view.MotionEvent;

public class InputController {
    public float moveX;
    public float moveY;
    public boolean dashPressed;
    public boolean pausePressed;

    private int joystickPointer = -1;
    private int actionPointer = -1;
    private float joystickCenterX;
    private float joystickCenterY;
    private float joystickRadius;

    public void setJoystick(float cx, float cy, float radius) {
        joystickCenterX = cx;
        joystickCenterY = cy;
        joystickRadius = radius;
    }

    public void resetFrameFlags() {
        dashPressed = false;
        pausePressed = false;
    }

    public boolean onTouch(MotionEvent event, float actionX, float actionY, float actionR, float pauseX, float pauseY, float pauseR) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index);
            float y = event.getY(index);
            if (dist(x, y, joystickCenterX, joystickCenterY) <= joystickRadius * 1.4f && joystickPointer == -1) {
                joystickPointer = pointerId;
                updateMove(x, y);
            } else if (dist(x, y, actionX, actionY) <= actionR * 1.2f && actionPointer == -1) {
                actionPointer = pointerId;
                dashPressed = true;
            } else if (dist(x, y, pauseX, pauseY) <= pauseR * 1.2f) {
                pausePressed = true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int id = event.getPointerId(i);
                if (id == joystickPointer) {
                    updateMove(event.getX(i), event.getY(i));
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pointerId == joystickPointer) {
                joystickPointer = -1;
                moveX = 0;
                moveY = 0;
            }
            if (pointerId == actionPointer) {
                actionPointer = -1;
            }
            return true;
        }
        return false;
    }

    private void updateMove(float x, float y) {
        float dx = x - joystickCenterX;
        float dy = y - joystickCenterY;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);
        if (mag > joystickRadius) {
            dx = dx / mag * joystickRadius;
            dy = dy / mag * joystickRadius;
            mag = joystickRadius;
        }
        moveX = joystickRadius == 0 ? 0 : dx / joystickRadius;
        moveY = joystickRadius == 0 ? 0 : dy / joystickRadius;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
