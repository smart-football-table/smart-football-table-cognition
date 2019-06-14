package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.KMH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.*;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class SpeedUnitTest {

	@Test
	void canConvertKmh() {
		SpeedUnit source = KMH;
		assertThat(source.convertTo(KMH, 1), is(1.0));
		assertThat(source.convertTo(MPS, 1), closeTo(0.277778, 0.001));
		assertThat(source.convertTo(IPM, 1), closeTo(656.1679, 0.001));
		assertThat(source.convertTo(MPH, 1), closeTo(0.621371, 0.001));
	}

	@Test
	void canConvertMps() {
		SpeedUnit source = MPS;
		assertThat(source.convertTo(KMH, 1), is(3.6));
		assertThat(source.convertTo(MPS, 1), is(1.0));
		assertThat(source.convertTo(IPM, 1), closeTo(2362.2047, 0.001));
		assertThat(source.convertTo(MPH, 1), closeTo(2.23694, 0.001));
	}

	@Test
	void canConvertIpm() {
		SpeedUnit source = IPM;
		assertThat(source.convertTo(KMH, 1), closeTo(0.0015240000000000002, 0.0001));
		assertThat(source.convertTo(MPS, 1), closeTo(0.00042333, 0.001));
		assertThat(source.convertTo(IPM, 1), is(1.0));
		assertThat(source.convertTo(MPH, 1), closeTo(0.00095699, 0.001));
	}

	@Test
	void canConvertMph() {
		SpeedUnit source = MPH;
		assertThat(source.convertTo(KMH, 1), closeTo(1.6093, 0.001));
		assertThat(source.convertTo(MPS, 1), closeTo(0.4470, 0.001));
		assertThat(source.convertTo(IPM, 1), is(1056.0));
		assertThat(source.convertTo(MPH, 1), is(1.0));
	}

}
