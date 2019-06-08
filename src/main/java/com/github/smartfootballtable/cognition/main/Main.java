package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.main.EnvVars.envVarsAndArgs;
import static com.github.smartfootballtable.cognition.main.MqttProcessor.processMqtt;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
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

	private SFTCognition cognition;

	public static void main(String... args) throws IOException {
		Main main = new Main();
		if (main.parseArgs(args)) {
			main.doMain();
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

	void doMain() throws IOException {
		MqttConsumer mqttConsumer = mqttConsumer();
		cognition = cognition(mqttConsumer);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook()));
		processMqtt(cognition, mqttConsumer);
	}

	protected SFTCognition cognition(MqttConsumer mqttConsumer) {
		return new SFTCognition(new Table(tableWidth, tableHeight, tableUnit),
				new QueueConsumer<Message>(mqttConsumer, 300)).receiver(mqttConsumer)
						.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
	}

	protected void shutdownHook() {
		cognition.messages().clearRetained();
	}

	protected MqttConsumer mqttConsumer() throws IOException {
		return new MqttConsumer(mqttHost, mqttPort);
	}

	private void printHelp(CmdLineParser parser) {
		String mainClassName = getClass().getName();
		System.err.println("java " + mainClassName + " [options...] arguments...");
		parser.printUsage(System.err);
		System.err.println();
		System.err.println("\tExample: java " + mainClassName + parser.printExample(ALL));
	}

}
