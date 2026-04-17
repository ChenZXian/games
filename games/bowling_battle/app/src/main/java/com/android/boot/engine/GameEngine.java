package com.android.boot.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.android.boot.input.TouchState;

import java.util.ArrayList;
import java.util.List;

public class GameEngine {
	public enum State { MENU, PLAYING, PAUSED }

	private State state = State.MENU;
	private int viewW = 1, viewH = 1;

	// Lane and entities
	private final RectF laneRect = new RectF();
	private final Paint pLane = new Paint();
	private final Paint pLaneBorder = new Paint();
	private final Paint pBall = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint pPin = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint pPinStripe = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint pHud = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);

	private float aimX;           // aiming position (center)
	private float ballX, ballY;   // ball position
	private float ballVX, ballVY; // velocity
	private boolean ballRolling;
	private static final int MAX_LEVEL = 5;
	private static final int BALLS_PER_LEVEL = 5;
	private int level = 1;
	private int throwsInLevel = 0;
	private int ballsLeft = BALLS_PER_LEVEL;
	private int totalScore = 0;
	private float messageTimer = 0f;
	private String centerMessage = "";

	private static class Pin {
		float x, y;
		boolean down;
	}
	private final List<Pin> pins = new ArrayList<>();

	public GameEngine() {
		pLane.setColor(Color.parseColor("#2E384A"));
		pLaneBorder.setColor(Color.parseColor("#3D4C69"));
		pLaneBorder.setStyle(Paint.Style.STROKE);
		pLaneBorder.setStrokeWidth(6f);
		pBall.setColor(Color.parseColor("#5BC3FF"));
		pPin.setColor(Color.parseColor("#F9FAFB"));
		pPinStripe.setColor(Color.parseColor("#FF5650"));
		pHud.setColor(Color.parseColor("#1B2230"));
		pText.setColor(Color.parseColor("#E6F0FF"));
		pText.setTextSize(32f);
	}

	public void onResize(int w, int h) {
		viewW = Math.max(1, w);
		viewH = Math.max(1, h);
		float margin = viewW * 0.08f;
		laneRect.set(margin, margin, viewW - margin, viewH - margin);
		resetStage();
	}

	private void resetStage() {
		aimX = (laneRect.left + laneRect.right) * 0.5f;
		ballX = aimX;
		ballY = laneRect.bottom - 92f;
		ballVX = 0f;
		ballVY = 0f;
		ballRolling = false;
		pins.clear();
		// Arrange 10 pins (classic triangle)
		buildLevelPins(level);
	}

	private void buildLevelPins(int levelIndex) {
		float cx = (laneRect.left + laneRect.right) * 0.5f;
		float top = laneRect.top + laneRect.height() * 0.18f;
		float mid = laneRect.top + laneRect.height() * 0.30f;
		float low = laneRect.top + laneRect.height() * 0.42f;
		float dx = 42f;
		switch (levelIndex) {
			case 1:
				// classic triangle
				addPin(cx, top);
				addPin(cx - dx * 0.5f, mid);
				addPin(cx + dx * 0.5f, mid);
				addPin(cx - dx, low);
				addPin(cx, low);
				addPin(cx + dx, low);
				break;
			case 2:
				// zig-zag
				addPin(cx - dx, top);
				addPin(cx, top + 24f);
				addPin(cx + dx, top + 48f);
				addPin(cx - dx * 0.5f, low);
				addPin(cx + dx * 0.5f, low);
				addPin(cx, low + 26f);
				break;
			case 3:
				// two columns
				addPin(cx - dx * 0.9f, top);
				addPin(cx - dx * 0.9f, mid);
				addPin(cx - dx * 0.9f, low);
				addPin(cx + dx * 0.9f, top);
				addPin(cx + dx * 0.9f, mid);
				addPin(cx + dx * 0.9f, low);
				break;
			case 4:
				// diamond
				addPin(cx, top);
				addPin(cx - dx, mid);
				addPin(cx + dx, mid);
				addPin(cx, low);
				addPin(cx - dx * 0.5f, low + 26f);
				addPin(cx + dx * 0.5f, low + 26f);
				break;
			default:
				// spread hard pattern
				addPin(cx - dx * 1.2f, top);
				addPin(cx, top + 18f);
				addPin(cx + dx * 1.2f, top);
				addPin(cx - dx * 0.6f, low);
				addPin(cx + dx * 0.6f, low);
				addPin(cx, low + 30f);
				addPin(cx - dx * 1.4f, low + 34f);
				addPin(cx + dx * 1.4f, low + 34f);
				break;
		}
	}

	private void addPin(float x, float y) {
		Pin pin = new Pin();
		pin.x = x;
		pin.y = y;
		pin.down = false;
		pins.add(pin);
	}

	public void update(float dt, TouchState input) {
		if (viewW <= 1 || viewH <= 1) return;

		if (state == State.MENU) {
			// Any roll starts the game
			if (input.rollPressed) {
				state = State.PLAYING;
				level = 1;
				throwsInLevel = 0;
				ballsLeft = BALLS_PER_LEVEL;
				totalScore = 0;
				centerMessage = "";
				messageTimer = 0f;
				resetStage();
				input.rollPressed = false;
			}
		}

		if (state != State.PLAYING) return;

		// Aiming left/right when not rolling
		float aimSpeed = 220f;
		if (!ballRolling) {
			if (input.leftHeld) aimX -= aimSpeed * dt;
			if (input.rightHeld) aimX += aimSpeed * dt;
			aimX = clamp(aimX, laneRect.left + 28f, laneRect.right - 28f);
			ballX = aimX;
		}

		// Press to roll
		if (input.rollPressed && !ballRolling) {
			ballRolling = true;
			ballVX = 0f;
			ballVY = -520f; // forward (upwards)
			throwsInLevel++;
			ballsLeft = Math.max(0, ballsLeft - 1);
		}
		// Reset pins
		if (input.resetPressed) {
			throwsInLevel = 0;
			ballsLeft = BALLS_PER_LEVEL;
			resetStage();
		}

		// Integrate ball
		if (ballRolling) {
			ballX += ballVX * dt;
			ballY += ballVY * dt;
			// Friction slows down
			ballVY += 280f * dt; // slight deceleration (reduce upward speed)
			// Lane side walls
			if (ballX < laneRect.left + 20f) { ballX = laneRect.left + 20f; ballVX = -ballVX * 0.6f; }
			if (ballX > laneRect.right - 20f) { ballX = laneRect.right - 20f; ballVX = -ballVX * 0.6f; }
			// End of lane: when reaching the back board, end this roll and reset ball to ready position
			if (ballY < laneRect.top + 12f) {
				ballVY = 0f;
				ballVX = 0f;
				ballRolling = false;
				// Reset ball to the ready position at the bottom so the next ROLL works immediately
				ballX = aimX;
				ballY = laneRect.bottom - 92f;
			}
			// Hit pins
			checkCollisionsWithPins();
		}

		if (messageTimer > 0f) {
			messageTimer -= dt;
			if (messageTimer <= 0f) {
				centerMessage = "";
			}
		}

		checkLevelProgress();
	}

	private void checkCollisionsWithPins() {
		float ballR = 18f;
		for (Pin pin : pins) {
			if (pin.down) continue;
			float dx = pin.x - ballX;
			float dy = pin.y - ballY;
			float d2 = dx*dx + dy*dy;
			float hitR = ballR + 16f;
			if (d2 <= hitR*hitR) {
				pin.down = true;
				// deflect ball slightly
				float len = (float)Math.sqrt(Math.max(1e-4, d2));
				float nx = dx / len;
				ballVX += nx * 90f;
				ballVY -= 60f;
			}
		}
	}

	private void checkLevelProgress() {
		boolean allDown = true;
		for (Pin pin : pins) {
			if (!pin.down) {
				allDown = false;
				break;
			}
		}
		if (allDown) {
			int levelScore = Math.max(200, 1500 - (throwsInLevel - 1) * 180);
			totalScore += levelScore;
			if (level >= MAX_LEVEL) {
				state = State.MENU;
				centerMessage = "All 5 Levels Cleared! Score +" + levelScore;
				messageTimer = 2.0f;
				return;
			}
			level++;
			throwsInLevel = 0;
			ballsLeft = BALLS_PER_LEVEL;
			centerMessage = "Level " + (level - 1) + " Clear! Next: " + level;
			messageTimer = 1.4f;
			resetStage();
			return;
		}
		// No balls left and not cleared: restart current level
		if (!ballRolling && ballsLeft == 0) {
			centerMessage = "Out of balls! Retry Level " + level;
			messageTimer = 1.4f;
			throwsInLevel = 0;
			ballsLeft = BALLS_PER_LEVEL;
			resetStage();
		}
	}

	public void render(Canvas c) {
		// Background
		c.drawColor(Color.parseColor("#0E1116"));
		// Lane
		c.drawRect(laneRect, pLane);
		c.drawRect(laneRect, pLaneBorder);

		// Pins
		for (Pin pin : pins) {
			if (pin.down) continue;
			drawPin(c, pin.x, pin.y);
		}

		// HUD
		float hudH = 64f;
		RectF hud = new RectF(laneRect.left, laneRect.bottom - hudH, laneRect.right, laneRect.bottom);
		c.drawRect(hud, pHud);
		int pinsLeft = 0;
		for (Pin pin : pins) if (!pin.down) pinsLeft++;
		String tip = (state == State.MENU)
				? "Press START GAME"
				: "L" + level + "/5  Pins:" + pinsLeft + "  Throws:" + throwsInLevel + "  Balls:" + ballsLeft + "  Score:" + totalScore;
		c.drawText(tip, hud.left + 16f, hud.centerY() + 10f, pText);

		// Ball (draw after HUD so the ready ball is always visible)
		c.drawCircle(ballX, ballY, 18f, pBall);

		if (!centerMessage.isEmpty()) {
			pText.setTextSize(42f);
			float msgW = pText.measureText(centerMessage);
			c.drawText(centerMessage, (viewW - msgW) * 0.5f, laneRect.centerY(), pText);
			pText.setTextSize(32f);
		}
	}

	private void drawPin(Canvas c, float x, float y) {
		// Simple bottle shape
		RectF body = new RectF(x - 10f, y - 26f, x + 10f, y + 14f);
		c.drawRoundRect(body, 8f, 8f, pPin);
		// red stripes
		RectF stripe = new RectF(x - 9f, y - 10f, x + 9f, y - 4f);
		c.drawRoundRect(stripe, 4f, 4f, pPinStripe);
	}

	private static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
