package com.zeravorn.team;

public enum TeamId {
	BLUE,
	RED;

	public TeamId opponent() {
		return this == BLUE ? RED : BLUE;
	}
}
