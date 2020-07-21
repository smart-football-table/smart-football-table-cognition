package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.main.EnvVars.envVarsAndArgs;
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
import com.github.smartfootballtable.cognition.mqtt.MqttAdapter;
import com.github.smartfootballtable.cognition.queue.ConsumerQueueDecorator;

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

	private MqttAdapter mqttAdapter;

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
		mqttAdapter = newMqttAdapter();
		cognition = newCognition(mqttAdapter);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook()));
		QueueWorker.consumeViaQueue(cognition, mqttAdapter);
	}

	private SFTCognition newCognition(MqttAdapter mqttAdapter) {
		return new SFTCognition(new Table(tableWidth, tableHeight, tableUnit),
				new ConsumerQueueDecorator<Message>(mqttAdapter, 300)).receiver(mqttAdapter)
						.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
	}

	private MqttAdapter newMqttAdapter() throws IOException {
		return new MqttAdapter(mqttHost, mqttPort);
	}

	public SFTCognition cognition() {
		return cognition;
	}

	public MqttAdapter mqttAdapter() {
		return mqttAdapter;
	}

	protected void shutdownHook() {
		cognition.messages().clearRetained();
	}

	private void printHelp(CmdLineParser parser) {
		String mainClassName = getClass().getName();
		System.err.println("java " + mainClassName + " [options...] arguments...");
		parser.printUsage(System.err);
		System.err.println();
		System.err.println("\tExample: java " + mainClassName + parser.printExample(ALL));
	}

}
