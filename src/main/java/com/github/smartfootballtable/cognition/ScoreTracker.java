package com.github.smartfootballtable.cognition;

public interface ScoreTracker {

	public static interface Listener {
		void scoreChanged(int teamid, int oldScore, int newScore);

		void won(int teamid);

		void draw(int[] teamids);
	}

	int teamScored(int teamid);

	int revertGoal(int teamid);

}