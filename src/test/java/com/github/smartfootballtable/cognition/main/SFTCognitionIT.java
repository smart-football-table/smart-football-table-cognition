package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.noPosition;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.detector.GoalDetector;
import com.github.smartfootballtable.cognition.mqtt.MqttConsumer;

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

class SFTCognitionIT {

	private static Duration timeout = ofSeconds(30);

	private static final String LOCALHOST = "localhost";

	private Server broker;
	private int brokerPort;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new CopyOnWriteArrayList<>();

	private SFTCognition sut;

	private MqttConsumer mqttConsumer;

	@BeforeEach
	void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
		sut = new SFTCognition(new Table(120, 68, CENTIMETER), mqttConsumer) //
				.receiver(mqttConsumer) //
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));

	}

	private int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		server.startServer(new MemoryConfig(properties));
		return server;
	}

	private MqttClient newMqttClient(String host, int port, String id) throws MqttException, MqttSecurityException {
		MqttClient client = new MqttClient("tcp://" + host + ":" + port, id, new MemoryPersistence());
		client.connect(connectOptions());
		client.setCallback(new MqttCallbackExtended() {

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				messagesReceived.add(message(topic, new String(message.getPayload())));
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				try {
					subscribe(client);
				} catch (MqttException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
			}
		});
		subscribe(client);
		return client;
	}

	private void subscribe(MqttClient client) throws MqttException {
		client.subscribe("#");
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	@Test
	void doesGenerateMessages() {
		assertTimeoutPreemptively(timeout, () -> {
			BlockingQueue<Message> queue = new ArrayBlockingQueue<>(10);
			mqttConsumer.addConsumer(m -> {
				if (sut.messages().isRelativePosition(m)) {
					queue.offer(m);
				}
			});
			newFixedThreadPool(1).execute(() -> sut.process(() -> {
				try {
					return sut.messages().parsePosition(queue.take().getPayload());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}));

			publish("ball/position/rel", "0.123,0.456");
			MILLISECONDS.sleep(250);

			// TODO this is JSON payload
			assertThat(messagesReceived, is(asList( //
					message("ball/position/rel", "0.123,0.456"), //
					message("game/start", ""), //
					message("ball/position/abs", "{ \"x\":14.0, \"y\":31.0 }"), //
					message("ball/position/rel", "{ \"x\":0.123, \"y\":0.456 }") //
			)));
		});
	}

	@Test
	void onResetTheNewGameIsStartedImmediatelyAndWithoutTableInteraction()
			throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		assertTimeoutPreemptively(timeout, () -> {
			sut.process(positions(42));
			messagesReceived.clear();
			sendReset();
			sut.process(provider(3, () -> noPosition(currentTimeMillis())));
			MILLISECONDS.sleep(50);
			assertThat(messagesWithTopic("game/start").count(), is(1L));
		});
	}

	@Test
	void doesReconnectAndResubscribe()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			sut.process(positions(42));
			restartBroker();
			waitUntil(secondClient, IMqttClient::isConnected);
			waitUntil(mqttConsumer, MqttConsumer::isConnected);
			messagesReceived.clear();
			sendReset();
			sut.process(provider(3, () -> noPosition(currentTimeMillis())));
			MILLISECONDS.sleep(50);
			assertThat(messagesWithTopic("game/start").count(), is(1L));
		});
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return messagesReceived.stream().filter(m -> m.getTopic().equals(topic));
	}

	private void restartBroker() throws IOException {
		broker.stopServer();
		broker = newMqttServer(LOCALHOST, brokerPort);
	}

	private static <T> void waitUntil(T object, Predicate<T> predicate) throws InterruptedException {
		while (!predicate.test(object)) {
			MILLISECONDS.sleep(500);
		}
	}

	private void sendReset() throws MqttException, MqttPersistenceException {
		publish("game/reset", "");
	}

	private void publish(String topic, String payload) throws MqttException, MqttPersistenceException {
		secondClient.publish(topic, new MqttMessage(payload.getBytes()));
	}

	private Stream<RelativePosition> positions(int count) {
		return provider(count, () -> create(currentTimeMillis(), 0.2, 0.3));
	}

	private Stream<RelativePosition> provider(int count, Supplier<RelativePosition> supplier) {
		return range(0, count).peek(i -> sleep()).mapToObj(i -> supplier.get());
	}

	private void sleep() {
		try {
			MILLISECONDS.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
