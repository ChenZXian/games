package com.android.boot.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.android.boot.MainActivity;
import com.android.boot.engine.GameLoopThread;
import com.android.boot.engine.GameSession;
import com.android.boot.engine.GameState;
import com.android.boot.input.InputController;
import com.android.boot.model.Enums.SlotType;
import com.android.boot.render.GameRenderer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final GameSession session;
    private final InputController input;
    private final GameRenderer renderer;
    private GameLoopThread thread;

    private float pressX;
    private float pressY;
    private long pressTime;
    private int pressInvIndex = -1;
    private int pressLootIndex = -1;
    private boolean pressMoved;
    private boolean modalDragging;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        session = new GameSession();
        input = new InputController();
        renderer = new GameRenderer();
    }

    public GameView(Context context, int stageIndex) {
        this(context);
        session.startAtStage(stageIndex);
    }

    public void tick(float dt) {
        session.update(dt, input);
        if (session.consumeReturnToMainMenuRequest()) {
            Context context = getContext();
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }

    public void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            renderer.draw(canvas, session);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (session.state == GameState.MENU && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            session.state = GameState.STAGE_SELECT;
            return true;
        }
        if (session.state == GameState.STAGE_SELECT && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            session.state = GameState.PLAYING;
            session.player.hp = session.player.maxHp;
            return true;
        }
        if (session.state == GameState.STAGE_RESULT && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (event.getX() < getWidth() / 2f) {
                session.state = GameState.PLAYING;
                session.player.hp = session.player.maxHp;
            } else {
                session.stageIndex = Math.min(6, session.stageIndex + 1);
                session.state = GameState.STAGE_SELECT;
            }
            return true;
        }
        if ((session.state == GameState.PLAYING || session.state == GameState.PAUSED) && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            float w = getWidth();
            float btnY = getHeight() - 70f;
            float cx = w * 0.5f;
            if (y >= btnY && y <= btnY + 44f) {
                if (x >= cx - 120f && x <= cx - 10f) {
                    session.showStatusPanel = !session.showStatusPanel;
                    return true;
                }
                if (x >= cx + 10f && x <= cx + 120f) {
                    session.toggleModal(true, false);
                    return true;
                }
            }
            if (session.modalOpen && x >= w * 0.92f && y <= w * 0.1f) {
                session.toggleModal(session.modalBag, session.modalEquip);
                return true;
            }
            if (x >= 32f && x <= 188f && y >= 178f && y <= 210f) {
                session.equipBestForSlot(SlotType.WEAPON);
                return true;
            }
            if (x >= 202f && x <= 358f && y >= 178f && y <= 210f) {
                session.equipBestForSlot(SlotType.ARMOR);
                return true;
            }
            if (!session.modalOpen && session.showEquipmentPanel) {
                if (x >= 32f && x <= 192f && y >= 270f && y <= 308f) {
                    session.unequipSlot(SlotType.WEAPON);
                    return true;
                }
                if (x >= 208f && x <= 368f && y >= 270f && y <= 308f) {
                    session.unequipSlot(SlotType.SHOES);
                    return true;
                }
                if (x >= 32f && x <= 192f && y >= 316f && y <= 354f) {
                    session.unequipSlot(SlotType.ARMOR);
                    return true;
                }
                if (x >= 208f && x <= 368f && y >= 316f && y <= 354f) {
                    session.unequipSlot(SlotType.HELMET);
                    return true;
                }
                if (x >= 32f && x <= 192f && y >= 362f && y <= 400f) {
                    session.unequipSlot(SlotType.BRACERS);
                    return true;
                }
                if (x >= 208f && x <= 368f && y >= 362f && y <= 400f) {
                    session.unequipSlot(SlotType.BELT);
                    return true;
                }
            }
        }

        if (session.modalOpen) {
            handleModalTouch(event);
            return true;
        }

        if (!session.modalOpen) {
            handleWorldLongPress(event);
        }
        input.onTouch(event, getWidth(), getHeight());
        return true;
    }

    private void handleModalTouch(MotionEvent event) {
        float w = getWidth();
        float h = getHeight();
        float px = event.getX();
        float py = event.getY();
        int pageSize = 8;
        float panelL = w * 0.08f;
        float panelT = h * 0.10f;
        float panelR = w * 0.92f;
        float panelB = h * 0.86f;
        float mid = (panelL + panelR) * 0.5f;
        float invL = mid + 14f;
        float invR = panelR - 14f;
        float invT = panelT + 70f;
        float invB = panelB - 70f;
        float avatarX = (panelL + mid) * 0.5f;
        float slotY = panelT + 130f;
        float slotGapY = 54f;
        float slotLeftX = avatarX - 220f;
        float slotRightX = avatarX + 60f;

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (px >= invL && px <= invL + 90f && py >= panelB - 54f && py <= panelB - 14f) {
                session.modalPrevPage();
                return;
            }
            if (px >= invR - 90f && px <= invR && py >= panelB - 54f && py <= panelB - 14f) {
                session.modalNextPage(pageSize);
                return;
            }
            int idx = hitInventoryIndex(px, py, invL, invT, invR, invB, pageSize);
            if (idx >= 0) {
                pressInvIndex = idx;
                pressLootIndex = -1;
                pressX = px;
                pressY = py;
                pressTime = event.getEventTime();
                pressMoved = false;
                modalDragging = false;
                return;
            }
            int slot = hitEquipSlot(px, py, slotLeftX, slotRightX, slotY, slotGapY);
            if (slot >= 0) {
                unequipBySlotIndex(slot);
                return;
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (pressInvIndex >= 0) {
                float dx = px - pressX;
                float dy = py - pressY;
                if (!pressMoved && (dx * dx + dy * dy) > 14f * 14f) {
                    pressMoved = true;
                    modalDragging = true;
                    session.modalDragIndex = pressInvIndex;
                    session.modalDragX = px;
                    session.modalDragY = py;
                }
                if (modalDragging) {
                    session.modalDragX = px;
                    session.modalDragY = py;
                }
            }
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            long held = event.getEventTime() - pressTime;
            if (!modalDragging && pressInvIndex >= 0 && held >= 480) {
                if (pressInvIndex < session.inventory.items().size()) {
                    session.showInspect(session.inventory.items().get(pressInvIndex), px, py);
                }
            }
            if (modalDragging && session.modalDragIndex >= 0) {
                int slot = hitEquipSlot(px, py, slotLeftX, slotRightX, slotY, slotGapY);
                if (slot >= 0) {
                    session.equipInventoryIndex(session.modalDragIndex);
                }
                session.modalDragIndex = -1;
            }
            pressInvIndex = -1;
            modalDragging = false;
        }
    }

    private int hitInventoryIndex(float x, float y, float l, float t, float r, float b, int pageSize) {
        float rowH = 54f;
        int maxPage = session.modalMaxPage(pageSize);
        int page = Math.max(0, Math.min(maxPage, session.modalPage));
        for (int i = 0; i < pageSize; i++) {
            float top = t + i * rowH;
            float bot = top + rowH - 6f;
            if (x >= l && x <= r && y >= top && y <= bot) {
                int idx = page * pageSize + i;
                return idx < session.inventory.items().size() ? idx : -1;
            }
        }
        return -1;
    }

    private int hitEquipSlot(float x, float y, float leftX, float rightX, float baseY, float gapY) {
        float boxW = 160f;
        float boxH = 42f;
        for (int r = 0; r < 3; r++) {
            float ty = baseY + r * gapY;
            if (x >= leftX && x <= leftX + boxW && y >= ty && y <= ty + boxH) return r * 2;
            if (x >= rightX && x <= rightX + boxW && y >= ty && y <= ty + boxH) return r * 2 + 1;
        }
        return -1;
    }

    private void handleWorldLongPress(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pressX = x;
            pressY = y;
            pressTime = event.getEventTime();
            pressMoved = false;
            pressInvIndex = -1;
            pressLootIndex = findLootIndexAt(x, y);
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dx = x - pressX;
            float dy = y - pressY;
            if (!pressMoved && (dx * dx + dy * dy) > 18f * 18f) pressMoved = true;
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (!pressMoved && pressLootIndex >= 0) {
                long held = event.getEventTime() - pressTime;
                if (held >= 520 && pressLootIndex < session.groundLoot.size()) {
                    session.showInspect(session.groundLoot.get(pressLootIndex).item, x, y);
                }
            }
            pressLootIndex = -1;
        }
    }

    private int findLootIndexAt(float x, float y) {
        for (int i = 0; i < session.groundLoot.size(); i++) {
            float dx = x - session.groundLoot.get(i).x;
            float dy = y - session.groundLoot.get(i).y;
            if (dx * dx + dy * dy < 34f * 34f) return i;
        }
        return -1;
    }

    private void unequipBySlotIndex(int idx) {
        if (idx == 0) session.unequipSlot(SlotType.WEAPON);
        else if (idx == 1) session.unequipSlot(SlotType.SHOES);
        else if (idx == 2) session.unequipSlot(SlotType.ARMOR);
        else if (idx == 3) session.unequipSlot(SlotType.HELMET);
        else if (idx == 4) session.unequipSlot(SlotType.BRACERS);
        else if (idx == 5) session.unequipSlot(SlotType.BELT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new GameLoopThread(this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    public void onPauseView() {
        stopLoop();
    }

    public void onResumeView() {
        if (thread == null || !thread.isAlive()) {
            thread = new GameLoopThread(this);
            thread.setRunning(true);
            thread.start();
        }
    }

    private void stopLoop() {
        if (thread != null) {
            thread.setRunning(false);
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
