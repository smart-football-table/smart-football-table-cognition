package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.BallPositionHandler;

public class BallPositionHandlerTest {

	@Test
	public void valueFromStringShouldBeTransferedCorrectlyIntoInteger() {

		String string = "100";

		BallPositionHandler positionHandler = new BallPositionHandler();

		assertThat(positionHandler.getValueFromString(string), is(100));
	}

	@Test
	public void positionAsStringShouldBeTransferedCorrectlyIntoModel() {

		int xCoordinate = 100;
		int yCoordinate = 200;
		String timepoint = anyTimepoint();

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler
				.createBallPositionFrom(timepoint + "|" + xCoordinate + "|" + yCoordinate);

		assertThat(ballPosition.getXCoordinate(), is(100));
		assertThat(ballPosition.getYCoordinate(), is(200));

		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("154166463822"));
	}

	private String anyTimepoint() {
		return "1541664638.22";
	}

	@Test
	public void anotherPositionAsStringShouldBeTransferedCorrectlyIntoModel() {

		String string = "1235232348.00|999|111";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string);

		assertThat(ballPosition.getXCoordinate(), is(999));
		assertThat(ballPosition.getYCoordinate(), is(111));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

}
