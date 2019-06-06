package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.main.Main;

class MainTest {

	private static final String ENV_MQTTPORT = "MQTTPORT";
	private static final String ENV_MQTTHOST = "MQTTHOST";
	private static final String ENV_TABLEHEIGHT = "TABLEHEIGHT";
	private static final String ENV_TABLEWIDTH = "TABLEWIDTH";
	private static final String ENV_TABLEUNIT = "TABLEUNIT";

	@Test
	void printsHelpOnMinusH() throws Exception {
		assertThat(tapSystemErr(() -> Main.main("-h")), allOf(//
				containsString("-mqttHost "), //
				containsString("-mqttPort "), //
				containsString("-tableWidth "), //
				containsString("-tableHeight "), //
				containsString("-tableUnit ")));
	}

	@Test
	void canReadEnvVars() throws Exception {
		Main main = new Main();
		withEnvironmentVariable(ENV_MQTTPORT, "1") //
				.and(ENV_MQTTHOST, "someHostname") //
				.and(ENV_TABLEHEIGHT, "2") //
				.and(ENV_TABLEWIDTH, "3") //
				.and(ENV_TABLEUNIT, CENTIMETER.name()) //
				.execute(() -> assertThat(main.parseArgs(), is(true)));
		assertThat(main.mqttPort, is(1));
		assertThat(main.mqttHost, is("someHostname"));
		assertThat(main.tableHeight, is(2));
		assertThat(main.tableWidth, is(3));
		assertThat(main.tableUnit, is(CENTIMETER));
	}

}
