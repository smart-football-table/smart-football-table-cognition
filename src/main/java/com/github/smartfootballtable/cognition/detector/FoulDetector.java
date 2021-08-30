package com.github.smartfootballtable.cognition.detector;

import static java.lang.Math.abs;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

public class FoulDetector implements Detector {

	public static interface Listener {
		void foulHappenend();
	}

	private static final long TIMEOUT = SECONDS.toMillis(15);
	private static final double MOVEMENT_GREATER_THAN = 0.05;

	private RelativePosition lastMovingPos;

	public static FoulDetector onFoul(Listener listener) {
		return new FoulDetector(listener);
	}

	@Override
	public FoulDetector newInstance() {
		return new FoulDetector(listener);
	}

	private final FoulDetector.Listener listener;

	private State state = pos -> new Moving(pos);

	private FoulDetector(FoulDetector.Listener listener) {
		this.listener = listener;
	}

	static interface State {
		State update(AbsolutePosition pos);

		default boolean isFoul() {
			return false;
		}
	}

	class Foul implements State {

		@Override
		public boolean isFoul() {
			return true;
		}

		@Override
		public State update(AbsolutePosition pos) {
			return xChanged(pos) ? new Moving(pos) : this;
		}

	}

	class NotMoving implements State {

		@Override
		public State update(AbsolutePosition pos) {
			return xChanged(pos) //
					? new Moving(pos) //
					: timeout(pos) >= TIMEOUT //
							? new Foul() //
							: this;
		}

		private long timeout(AbsolutePosition pos) {
			return pos.getTimestamp() - lastMovingPos.getTimestamp();
		}

	}

	class Moving implements State {

		public Moving(AbsolutePosition pos) {
			FoulDetector.this.lastMovingPos = pos.getRelativePosition();
		}

		@Override
		public State update(AbsolutePosition pos) {
			if (FoulDetector.this.lastMovingPos != null && !xChanged(pos)) {
				return new NotMoving();
			}
			FoulDetector.this.lastMovingPos = pos.getRelativePosition();
			return this;
		}

	}

	@Override
	public void detect(AbsolutePosition pos) {
		boolean wasFoul = state.isFoul();
		state = state.update(pos);
		if (!wasFoul && state.isFoul()) {
			listener.foulHappenend();
		}
	}

	private boolean xChanged(AbsolutePosition pos) {
		return pos.isNull() || xDiff(pos) > MOVEMENT_GREATER_THAN;
	}

	private double xDiff(AbsolutePosition pos) {
		return abs(pos.getRelativePosition().getX() - lastMovingPos.getX());
	}

}