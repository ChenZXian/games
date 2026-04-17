package com.android.boot.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class GameEngine {
	private int viewW = 1, viewH = 1;
	private final Paint pBg = new Paint();
	private final Paint pPanel = new Paint();
	private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF battlefield = new RectF();
	private float t;
	private boolean battleStarted;

	public GameEngine() {
		pBg.setColor(Color.parseColor("#0D0F14"));
		pPanel.setColor(Color.parseColor("#151C26"));
		pText.setColor(Color.parseColor("#EAF1FF"));
		pText.setTextSize(32f);
	}

	public void onResize(int w, int h) {
		viewW = Math.max(1, w);
		viewH = Math.max(1, h);
		float margin = Math.min(viewW, viewH) * 0.06f;
		battlefield.set(margin, margin, viewW - margin, viewH - margin);
	}

	public void update(float dt) {
		if (!battleStarted) {
			return;
		}
		t += dt;
	}

	public void startBattle() {
		battleStarted = true;
		t = 0f;
	}

	public void render(Canvas c) {
		c.drawColor(pBg.getColor());
		c.drawRect(battlefield, pPanel);
		String title = battleStarted ? "Battle Running" : "Press START";
		float w = pText.measureText(title);
		c.drawText(title, (viewW - w) * 0.5f, battlefield.centerY(), pText);
	}
}

