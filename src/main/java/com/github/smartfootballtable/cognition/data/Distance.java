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
		if (other.distanceUnit == distanceUnit) {
			// this is an (not really needed optimization). Not really needed because
			// Distance#value will directly return the value without any conversion if
			// Distance is in the passed DistanceUnit.
			return new Distance(value + other.value, distanceUnit);
		}
		return new Distance(value + other.value(distanceUnit), distanceUnit);
	}

	@Generated
	@Override
	public String toString() {
		return "Distance [value=" + value + ", distanceUnit=" + distanceUnit + "]";
	}

}