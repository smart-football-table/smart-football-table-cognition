package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.KILOMETERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.METERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.MILES;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import lombok.Generated;

public final class SpeedUnit {

	public static final SpeedUnit MS = new SpeedUnit(METERS, SECONDS, "ms");
	public static final SpeedUnit KMH = new SpeedUnit(KILOMETERS, HOURS, "kmh");
	public static final SpeedUnit IPS = new SpeedUnit(INCHES, SECONDS, "ips");
	public static final SpeedUnit MPH = new SpeedUnit(MILES, HOURS, "mph");
	private static final SpeedUnit[] speedUnits = new SpeedUnit[] { MS, KMH, IPS, MPH };

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

	public static SpeedUnit get(DistanceUnit distanceUnit, TimeUnit timeUnit) {
		for (SpeedUnit unit : speedUnits) {
			if (unit.distanceUnit == distanceUnit && unit.timeUnit == timeUnit) {
				return unit;
			}
		}
		return new SpeedUnit(distanceUnit, timeUnit, "internal");
	}

	@Generated
	@Override
	public String toString() {
		return "SpeedUnit [distanceUnit=" + distanceUnit + ", timeUnit=" + timeUnit + ", symbol=" + symbol + "]";
	}

}