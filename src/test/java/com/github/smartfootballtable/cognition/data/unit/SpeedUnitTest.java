package com.github.smartfootballtable.cognition.data.unit;

import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.KMH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPS;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class SpeedUnitTest {

	@Test
	void canConvertKmh() {
		assertThat(KMH.convertTo(KMH, 1), is(1.0));
		assertThat(KMH.convertTo(MPS, 1), closeTo(0.277778, 0.001));
		assertThat(KMH.convertTo(MPH, 1), closeTo(0.621371, 0.001));
	}

	@Test
	void canConvertMps() {
		assertThat(MPS.convertTo(MPS, 1), is(1.0));
		assertThat(MPS.convertTo(KMH, 1), is(3.6));
	}

}
