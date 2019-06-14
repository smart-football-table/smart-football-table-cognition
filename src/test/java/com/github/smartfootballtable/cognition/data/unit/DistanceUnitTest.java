package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.KILOMETERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.METERS;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.MILES;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class DistanceUnitTest {

	@Test
	void merics() {
		assertThat(CENTIMETER.isMetric(), is(true));
		assertThat(METERS.isMetric(), is(true));
		assertThat(KILOMETERS.isMetric(), is(true));
	}

	@Test
	void imperial() {
		assertThat(INCHES.isMetric(), is(false));
		assertThat(MILES.isMetric(), is(false));
	}

	@Test
	void canConvertCentimeter() {
		DistanceUnit source = CENTIMETER;
		assertThat(CENTIMETER.convert(1, source), is(1.0));
		assertThat(METERS.convert(1, source), is(0.01));
		assertThat(KILOMETERS.convert(1, source), is(0.00001));
		assertThat(INCHES.convert(1, source), is(closeTo(0.393701, 0.001)));
		assertThat(MILES.convert(1, source), is(closeTo(0.0000062137152777, 0.0000001)));
	}

	@Test
	void canConvertMeters() {
		DistanceUnit source = METERS;
		assertThat(CENTIMETER.convert(1, source), is(100.0));
		assertThat(METERS.convert(1, source), is(1.0));
		assertThat(KILOMETERS.convert(1, source), is(0.001));
		assertThat(INCHES.convert(1, source), is(closeTo(39.3701, 0.001)));
		assertThat(MILES.convert(1, source), is(closeTo(0.00062137152777, 0.00001)));
	}

	@Test
	void canConvertKilometers() {
		DistanceUnit source = KILOMETERS;
		assertThat(CENTIMETER.convert(1, source), is(100000.0));
		assertThat(METERS.convert(1, source), is(1000.0));
		assertThat(KILOMETERS.convert(1, source), is(1.0));
		assertThat(INCHES.convert(1, source), is(closeTo(39370.078, 0.01)));
		assertThat(MILES.convert(1, source), is(closeTo(0.62137152777, 0.001)));
	}

	@Test
	void canConvertInches() {
		DistanceUnit source = INCHES;
		assertThat(CENTIMETER.convert(1, source), is(closeTo(2.5400013716009097742, 0.0001)));
		assertThat(METERS.convert(1, source), is(closeTo(0.0254, 0.001)));
		assertThat(KILOMETERS.convert(1, source), is(closeTo(0.0000254, 0.0001)));
		assertThat(INCHES.convert(1, source), is(1.0));
		assertThat(MILES.convert(1, source), is(closeTo(0.0000157828, 0.00001)));
	}

	@Test
	void canConvertMiles() {
		DistanceUnit source = MILES;
		assertThat(CENTIMETER.convert(1, source), is(closeTo(160934.4, 0.1)));
		assertThat(METERS.convert(1, source), is(closeTo(1609.344, 0.01)));
		assertThat(KILOMETERS.convert(1, source), is(closeTo(1.609, 0.01)));
		assertThat(INCHES.convert(1, source), is(closeTo(63360.0, 0.1)));
		assertThat(MILES.convert(1, source), is(1.0));
	}

}
