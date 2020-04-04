package com.github.smartfootballtable.cognition.detector;

import com.github.smartfootballtable.cognition.data.Distance;
import com.github.smartfootballtable.cognition.data.Movement;
import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;

public class MovementDetector implements Detector {

	public static MovementDetector onMovement(DistanceUnit distanceUnit, Listener listener) {
		return new MovementDetector(distanceUnit, listener);
	}

	@Override
	public MovementDetector newInstance() {
		return new MovementDetector(overallDistance.unit(), listener);
	}

	private final MovementDetector.Listener listener;

	public static interface Listener {
		void movement(Movement movement, Distance overallDistance);
	}

	private MovementDetector(DistanceUnit distanceUnit, MovementDetector.Listener listener) {
		this.listener = listener;
		this.overallDistance = new Distance(0, distanceUnit);
	}

	private AbsolutePosition prevPos;
	private Distance overallDistance;

	@Override
	public void detect(AbsolutePosition pos) {
		RelativePosition relPos = pos.getRelativePosition();
		if (relPos.isNull()) {
			prevPos = null;
		} else {
			if (prevPos != null) {
				Movement movement = new Movement(prevPos, pos, overallDistance.unit());
				listener.movement(movement, (overallDistance = overallDistance.add(movement.distance())));
			}
			prevPos = pos;
		}
	}

}