package com.github.smartfootballtable.cognition.data;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPS;

import java.util.concurrent.TimeUnit;

import com.github.smartfootballtable.cognition.data.unit.SpeedUnit;

public class Velocity {

	private final SpeedUnit source = MPS;
	private final double metersPerSecond;

	public Velocity(Distance distance, long duration, TimeUnit timeUnit) {
		this.metersPerSecond = mps(distance, duration, timeUnit);
	}

	private double mps(Distance distance, long duration, TimeUnit timeUnit) {
		return 10 * distance.value(CENTIMETER) / timeUnit.toMillis(duration);
	}

	public double value(SpeedUnit target) {
		return source.convertTo(target, metersPerSecond);
	}

}