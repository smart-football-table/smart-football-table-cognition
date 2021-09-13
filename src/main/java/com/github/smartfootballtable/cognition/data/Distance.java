package com.github.smartfootballtable.cognition.data;

import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;

import lombok.Generated;

public class Distance {

	private final double value;
	private final DistanceUnit distanceUnit;

	public Distance(double value, DistanceUnit distanceUnit) {
		this.value = value;
		this.distanceUnit = distanceUnit;
	}

	public double value(DistanceUnit target) {
		return target.convert(value, distanceUnit);
	}

	public DistanceUnit unit() {
		return distanceUnit;
	}

	public Distance add(Distance other) {
		return new Distance(value + other.value(distanceUnit), distanceUnit);
	}

	@Generated
	@Override
	public String toString() {
		return "Distance [value=" + value + ", distanceUnit=" + distanceUnit + "]";
	}

}