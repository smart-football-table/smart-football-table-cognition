package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.main.EnvVars.envVarsAndArgs;
import static java.util.stream.Stream.generate;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;
import com.github.smartfootballtable.cognition.detector.GoalDetector;
import com.github.smartfootballtable.cognition.mqtt.MqttConsumer;
import com.github.smartfootballtable.cognition.queue.QueueConsumer;

public class Main {

	@Option(name = "-h", help = true)
	boolean help;

	@Option(name = "-tableWidth", usage = "width of the table")
	int tableWidth = 120;
	@Option(name = "-tableHeight", usage = "height of the table")
	int tableHeight = 68;
	@Option(name = "-tableUnit", usage = "distance unit of the table")
	DistanceUnit tableUnit = CENTIMETER;

	@Option(name = "-mqttHost", usage = "hostname of the mqtt broker")
	String mqttHost = "localhost";
	@Option(name = "-mqttPort", usage = "port of the mqtt broker")
	int mqttPort = 1883;

	public static void main(String... args) throws IOException {

		// runtime configuration stuff, shouldn't be in code
//		String[] arguments = { "-v", "src/main/resources/videos/testVid_ballFromLeftToRight.avi", "-c", "20,100,100,30,255,255" };

		Main main = new Main();
		if (main.parseArgs(args)) {
			main.doMain(args);
		}
	}

	boolean parseArgs(String... args) {
		CmdLineParser parser = new CmdLineParser(this, defaults().withUsageWidth(80));
		try {
			parser.parseArgument(envVarsAndArgs(parser, args));
			if (!help) {
				return true;
			}
			printHelp(parser);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelp(parser);
		}
		return false;
	}

	void doMain(String... args) throws IOException {
		MqttConsumer mqtt = mqtt(mqttHost, mqttPort);
		SFTCognition cognition = new SFTCognition(new Table(tableWidth, tableHeight, tableUnit),
				new QueueConsumer<Message>(mqtt, 300)).receiver(mqtt)
						.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
		// TODO write test
		BlockingQueue<Message> queue = new ArrayBlockingQueue<>(10);
		mqtt.addConsumer(t -> {
			if (t.getTopic().startsWith("ball/position/rel")) {
				queue.offer(t);
			}
		});
		cognition.process(streamOf(queue));
	}

	private Stream<RelativePosition> streamOf(BlockingQueue<Message> queue) {
		return generate(() -> {
			try {
				return parse(queue.take());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private RelativePosition parse(Message message) {
		String[] coords = message.getPayload().split("\\,");
		return RelativePosition.create(System.currentTimeMillis(), Double.parseDouble(coords[0]),
				Double.parseDouble(coords[1]));
	}

	private void printHelp(CmdLineParser parser) {
		String mainClassName = getClass().getName();
		System.err.println("java " + mainClassName + " [options...] arguments...");
		parser.printUsage(System.err);
		System.err.println();
		System.err.println("  Example: java " + mainClassName + parser.printExample(ALL));
	}

	private MqttConsumer mqtt(String host, int port) throws IOException {
		return new MqttConsumer(host, port);
	}

	public static Function<String, RelativePosition> fromPythonFormat() {
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
