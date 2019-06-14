package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.KILOMETERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.METERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.MILES;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

public enum SpeedUnit {

	MPS(METERS, SECONDS), KMH(KILOMETERS, HOURS), IPM(INCHES, MINUTES), MPH(MILES, HOURS);

	private final DistanceUnit distanceUnit;
	private final TimeUnit timeUnit;

	private SpeedUnit(DistanceUnit distanceUnit, TimeUnit timeUnit) {
		this.distanceUnit = distanceUnit;
		this.timeUnit = timeUnit;
	}

	public double convertTo(SpeedUnit target, double value) {
		return value * (convertDistance(target) / convertTime(target));
	}

	private double convertDistance(SpeedUnit target) {
		return target.distanceUnit.convert(1, distanceUnit);
	}

	private double convertTime(SpeedUnit target) {
		return target.timeUnit.compareTo(timeUnit) > 0 //
				? 1D / timeUnit.convert(1, target.timeUnit) //
				: target.timeUnit.convert(1, timeUnit);
	}

}