package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.KILOMETERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.METERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.MILES;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import lombok.Generated;

public final class SpeedUnit {

	public static final SpeedUnit MPS = new SpeedUnit(METERS, SECONDS, "mps");
	public static final SpeedUnit KMH = new SpeedUnit(KILOMETERS, HOURS, "kmh");
	public static final SpeedUnit IPM = new SpeedUnit(INCHES, MINUTES, "ipm");
	public static final SpeedUnit MPH = new SpeedUnit(MILES, HOURS, "mph");

	private final DistanceUnit distanceUnit;
	private final TimeUnit timeUnit;
	private final String symbol;

	public SpeedUnit(DistanceUnit distanceUnit, TimeUnit timeUnit, String symbol) {
		this.distanceUnit = distanceUnit;
		this.timeUnit = timeUnit;
		this.symbol = symbol;
	}

	public String symbol() {
		return symbol;
	}

	public double convertTo(SpeedUnit source, double value) {
		return value * (convertTime(source) / convertDistance(source));
	}

	private double convertDistance(SpeedUnit source) {
		return distanceUnit.convert(1, source.distanceUnit);
	}

	private double convertTime(SpeedUnit source) {
		return source.timeUnit.compareTo(timeUnit) < 0 //
				? 1.0 / source.timeUnit.convert(1, timeUnit) //
				: timeUnit.convert(1, source.timeUnit);
	}

	@Generated
	@Override
	public String toString() {
		return "SpeedUnit [distanceUnit=" + distanceUnit + ", timeUnit=" + timeUnit + ", symbol=" + symbol + "]";
	}

}