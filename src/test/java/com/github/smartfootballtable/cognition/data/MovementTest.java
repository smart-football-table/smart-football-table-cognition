package com.github.smartfootballtable.cognition.data;

import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MS;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.data.position.Position;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;

class MovementTest {

	@Test
	void twoPositionsWithIdenticalTimestampsAreAcceptableAndDistanceAndVelocityCanBeCalculated() {
		Position p1 = create(anyTimestamp(), 0.0, 0.0);
		Position p2 = create(p1.getTimestamp(), 1.0, 1.0);
		Movement sut = sut(p1, p2);
		assertThat(sut.distance(CENTIMETER), closeTo(1.414, 0.001));
		assertThat(sut.velocity(MS), is(POSITIVE_INFINITY));
	}

	@Test
	void failsIfDifferenceOfTimestampsIsNegative() {
		Position p1 = create(anyTimestamp(), 0.0, 0.0);
		Position p2 = create(p1.getTimestamp() - 1, 1.0, 1.0);
		assertThat(assertThrows(RuntimeException.class, () -> sut(p1, p2)).getMessage(),
				allOf(containsString("timestamp"), containsString("before")));
	}

	private Movement sut(Position p1, Position p2) {
		return new Movement(p1, p2, anyDistanceUnit());
	}

	private DistanceUnit anyDistanceUnit() {
		return CENTIMETER;
	}

	private int anyTimestamp() {
		return 42;
	}

}
