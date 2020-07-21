package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.detector.GoalDetector;

public class ReadPythonOutputFromFileMain {

	public static void main(String[] args) throws IOException {
		Consumer<Message> sysout = System.out::println;
		// Consumer<Message> publisher = t -> {
		// if (t.getTopic().contains("score")) {
		// sysout.accept(t);
		// }
		// };

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File("python_output_opencv.txt"))))) {
			SFTCognition cognition = new SFTCognition(new Table(120, 68, CENTIMETER), sysout)
					.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
			reader.lines().map(fromPythonFormat()).forEach(cognition::process);
		}
	}

	private static Function<String, RelativePosition> fromPythonFormat() {
		return new Function<String, RelativePosition>() {
			@Override
			public RelativePosition apply(String line) {
				String[] values = line.split("\\|");
				return values.length == 3 ? create(toLong(values[0]) * 10, toDouble(values[1]), toDouble(values[2]))
						: null;
			}

			private Double toDouble(String val) {
				try {
					return Double.valueOf(val);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			private Long toLong(String val) {
				try {
					return Long.valueOf(val);
				} catch (NumberFormatException e) {
					return null;
				}
			}

		};
	}
}
