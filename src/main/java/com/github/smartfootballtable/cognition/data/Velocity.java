package com.github.smartfootballtable.cognition.data;

import java.util.concurrent.TimeUnit;

import com.github.smartfootballtable.cognition.data.unit.SpeedUnit;

public class Velocity {

	private final SpeedUnit speedUnit;
	private final long duration;
	private final double distance;

	public Velocity(Distance distance, long duration, TimeUnit timeUnit) {
		this.speedUnit = SpeedUnit.get(distance.unit(), timeUnit);
		this.distance = distance.value(distance.unit());
		this.duration = duration;
	}

	public double value(SpeedUnit target) {
		return speedUnit.convertTo(target, distance) / duration;
	}

}