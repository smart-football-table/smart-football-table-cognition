package detection2.data;

import static detection2.data.unit.DistanceUnit.CENTIMETER;

import java.util.concurrent.TimeUnit;

import detection2.data.unit.SpeedUnit;

public class Velocity {

	private final double metersPerSecond;

	public Velocity(Distance distance, long duration, TimeUnit timeUnit) {
		this.metersPerSecond = mps(distance, duration, timeUnit);
	}

	private double mps(Distance distance, long duration, TimeUnit timeUnit) {
		return 10 * distance.value(CENTIMETER) / timeUnit.toMillis(duration);
	}

	public double value(SpeedUnit speedUnit) {
		return speedUnit.toMps(metersPerSecond);
	}

}