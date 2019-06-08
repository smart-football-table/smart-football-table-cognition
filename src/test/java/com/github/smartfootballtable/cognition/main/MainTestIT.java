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
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.mqtt.MqttConsumer;

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

class MainTestIT {

	private static Duration timeout = ofSeconds(30);

	private static final String LOCALHOST = "localhost";

	private Server broker;
	private int brokerPort;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new CopyOnWriteArrayList<>();

	private volatile MqttConsumer mqttConsumer;
	private volatile Messages messages;
	private volatile Main main;
	private volatile Future<?> mainProcess;

	@BeforeEach
	void setup() throws Exception {
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2", messagesReceived);
		mainProcess = runAsync(() -> {
			try {
				main = newMain();
				main.doMain();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		waitUntil((Supplier<MqttConsumer>) () -> mqttConsumer, s -> s.get() != null);
		waitUntil(mqttConsumer, MqttConsumer::isConnected);
	}

	private Main newMain() {
		Main main = new Main() {

			@Override
			protected MqttConsumer mqttConsumer() throws IOException {
				mqttConsumer = super.mqttConsumer();
				return mqttConsumer;
			}

			@Override
			protected SFTCognition cognition(MqttConsumer mqttConsumer) {
				SFTCognition cognition = super.cognition(mqttConsumer);
				messages = cognition.messages();
				return cognition;
			}
		};
		main.mqttHost = LOCALHOST;
		main.mqttPort = brokerPort;
		main.tableWidth = 120;
		main.tableHeight = 68;
		main.tableUnit = CENTIMETER;
		return main;
	}

	@AfterEach
	void tearDown() throws Exception {
		haltMain();
		secondClient.disconnect();
		broker.stopServer();
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

	private MqttClient newMqttClient(String host, int port, String id, List<Message> received)
			throws MqttException, MqttSecurityException {
		MqttClient client = new MqttClient("tcp://" + host + ":" + port, id, new MemoryPersistence());
		client.setTimeToWait(SECONDS.toMillis(1));
		client.connect(connectOptions());
		client.setCallback(new MqttCallbackExtended() {

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				received.add(message(topic, new String(message.getPayload())));
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
			publish("ball/position/rel", "0.123,0.456");
			MILLISECONDS.sleep(250);
			assertThat(messagesWithTopic("ball/position/abs").collect(toList()),
					is(asList(message("ball/position/abs", "14.0,31.0"))));
		});
	}

	@Test
	void onResetTheNewGameIsStartedImmediatelyAndWithoutTableInteraction()
			throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		assertTimeoutPreemptively(timeout, () -> {
			publish(positions(anyAmount()));
			assertReceivesGameStartWhenSendingReset();
		});
	}

	@Test
	void doesReconnectAndResubscribe()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			publish(positions(anyAmount()));
			restartBroker();
			waitUntil(secondClient, IMqttClient::isConnected);
			waitUntil(mqttConsumer, MqttConsumer::isConnected);
			assertReceivesGameStartWhenSendingReset();
		});
	}

	@Test
	void scoreMessagesAreRetained() throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			messages.teamScore(0, 2);
			messages.teamScore(1, 3);
			MILLISECONDS.sleep(250);

			List<Message> receivedRetained = new ArrayList<>();
			MqttClient thirdClient = newMqttClient(LOCALHOST, brokerPort, "third-client", receivedRetained);
			MILLISECONDS.sleep(250);

			assertThat(messagesWithTopic(receivedRetained, "team/score/0").map(Message::getPayload).collect(toList()),
					is(asList("2")));
			assertThat(messagesWithTopic(receivedRetained, "team/score/1").map(Message::getPayload).collect(toList()),
					is(asList("3")));
			thirdClient.disconnect();
		});
	}

	@Test
	void doesRemoveRetainedMessages()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			messages.teamScore(0, 2);
			messages.teamScore(1, 3);
			MILLISECONDS.sleep(250);
			haltMain();
			main.shutdownHook();
			assertThat(messagesWithTopic("team/score/0").count(), is(not(0L)));
			assertThat(messagesWithTopic("team/score/1").count(), is(not(0L)));

			List<Message> receivedRetained = new ArrayList<>();
			MqttClient thirdClient = newMqttClient(LOCALHOST, brokerPort, "third-client", receivedRetained);
			MILLISECONDS.sleep(250);
			assertThat(receivedRetained, is(empty()));
			thirdClient.disconnect();
		});
	}

	private void haltMain() {
		mainProcess.cancel(true);
	}

	private void assertReceivesGameStartWhenSendingReset()
			throws InterruptedException, MqttPersistenceException, MqttException {
		messagesReceived.clear();
		sendReset();
		MILLISECONDS.sleep(250);
		publish(provider(anyAmount(), () -> noPosition(currentTimeMillis())));
		MILLISECONDS.sleep(250);
		assertThat(messagesWithTopic("game/start").count(), is(1L));
	}

	private int anyAmount() {
		return 5;
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return messagesWithTopic(messagesReceived, topic);
	}

	private Stream<Message> messagesWithTopic(List<Message> messages, String topic) {
		return messages.stream().filter(m -> m.getTopic().equals(topic));
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

	private void publish(Stream<RelativePosition> positions) {
		positions.forEach(this::publish);
	}

	private void publish(RelativePosition position) {
		try {
			publish("ball/position/rel", position.getX() + "," + position.getY());
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
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
