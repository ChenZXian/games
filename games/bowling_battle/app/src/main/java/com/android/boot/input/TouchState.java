package com.android.boot.input;

public class TouchState {
	public boolean leftHeld;
	public boolean rightHeld;
	public boolean rollPressed;
	public boolean resetPressed;

	public void clearInstant() {
		rollPressed = false;
		resetPressed = false;
	}
}
