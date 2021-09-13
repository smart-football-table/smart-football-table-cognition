package com.github.smartfootballtable.cognition.data;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.METERS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;

class DistanceTest {

	@Test
	void canAddWhenUnitsAreIdentical() {
		Distance d1 = new Distance(1.23, METERS);
		Distance d2 = new Distance(4.56, METERS);
		assertAddIs(d1, d2, 1.23 + 4.56, METERS);
	}

	@Test
	void canAddWhenUnitsAreDifferent() {
		Distance d1 = new Distance(1.23, METERS);
		Distance d2 = new Distance(4.56, CENTIMETER);
		assertAddIs(d1, d2, 1.23 + (4.56 / 100), METERS);
	}

	@Test
	void canAddWhenUnitsAreDifferentEvenWhenSystemsDiffer() {
		Distance d1 = new Distance(1, INCHES);
		Distance d2 = new Distance(1, METERS);
		assertAddIs(d1, d2, 1.00 + 39.37007874015748, INCHES);
	}

	void assertAddIs(Distance d1, Distance d2, double value, DistanceUnit distanceUnit) {
		Distance result = d1.add(d2);
		assertAll( //
				() -> assertThat(result.unit(), is(distanceUnit)), //
				() -> assertThat(result.value(distanceUnit), is(value)) //
		);
	}

}
