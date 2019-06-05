package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import com.github.smartfootballtable.cognition.SFTDetection;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.detector.GoalDetector;
import com.github.smartfootballtable.cognition.main.Main;

public class PythonOutputProcessingMain {

	public static void main(String[] args) throws IOException {
		Consumer<Message> sysout = System.out::println;
		// Consumer<Message> publisher = t -> {
		// if (t.getTopic().contains("score")) {
		// sysout.accept(t);
		// }
		// };

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File("python_output_opencv.txt"))))) {
			new SFTDetection(new Table(120, 68, CENTIMETER), sysout)
					.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40))
					.process(reader.lines().map(Main.fromPythonFormat()));
		}
	}

}
