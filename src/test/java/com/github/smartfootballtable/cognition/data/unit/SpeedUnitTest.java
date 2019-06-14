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
		assertThat(source.convertTo(MS, 1), closeTo(0.277778, 0.001));
		assertThat(source.convertTo(IPS, 1), closeTo(10.93613, 0.001));
		assertThat(source.convertTo(MPH, 1), closeTo(0.621371, 0.001));
	}

	@Test
	void canConvertMs() {
		SpeedUnit source = MS;
		assertThat(source.convertTo(KMH, 1), is(3.6));
		assertThat(source.convertTo(MS, 1), is(1.0));
		assertThat(source.convertTo(IPS, 1), closeTo(39.37007, 0.001));
		assertThat(source.convertTo(MPH, 1), closeTo(2.23694, 0.001));
	}

	@Test
	void canConvertIps() {
		SpeedUnit source = IPS;
		assertThat(source.convertTo(KMH, 1), closeTo(0.09144, 0.0001));
		assertThat(source.convertTo(MS, 1), closeTo(0.0253998, 0.001));
		assertThat(source.convertTo(IPS, 1), is(1.0));
		assertThat(source.convertTo(MPH, 1), closeTo(0.0574194, 0.001));
	}

	@Test
	void canConvertMph() {
		SpeedUnit source = MPH;
		assertThat(source.convertTo(KMH, 1), closeTo(1.6093, 0.001));
		assertThat(source.convertTo(MS, 1), closeTo(0.4470, 0.001));
		assertThat(source.convertTo(IPS, 1), is(17.6));
		assertThat(source.convertTo(MPH, 1), is(1.0));
	}

}
