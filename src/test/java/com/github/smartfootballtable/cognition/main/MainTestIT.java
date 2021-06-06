package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.MessageMother.TOPIC_BALL_POSITION_ABS;
import static com.github.smartfootballtable.cognition.MessageMother.relativePosition;
import static com.github.smartfootballtable.cognition.MessageMother.scoreOfTeam;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.noPosition;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.stefanbirkner.systemlambda.SystemLambda.muteSystemErr;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.mqtt.MqttAdapter;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

class MainTestIT {

	private static Duration timeout = ofMinutes(2);
	private static final String LOCALHOST = "localhost";

	static class MqttClientForTest implements Closeable {

		private final IMqttClient client;
		private final List<Message> received = new CopyOnWriteArrayList<>();

		public List<Message> getReceived() {
			return received;
		}

		public MqttClientForTest(String brokerHost, int brokerPort, String name) throws IOException {
			this.client = createClient(brokerHost, brokerPort, name, received);
		}

		private IMqttClient createClient(String brokerHost, int brokerPort, String name, List<Message> received)
				throws IOException {
			try {
				MqttClient client = new MqttClient("tcp://" + brokerHost + ":" + brokerPort, name,
						new MemoryPersistence());
				client.setTimeToWait(timeout.toMillis());
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
			} catch (MqttException e) {
				throw new IOException(e);
			}
		}

		private static MqttConnectOptions connectOptions() {
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setAutomaticReconnect(true);
			return mqttConnectOptions;
		}

		private static void subscribe(MqttClient client) throws MqttException {
			client.subscribe("#");
		}

		public void close() throws IOException {
			try {
				client.unsubscribe("#");
				client.disconnect();
				client.close();
			} catch (MqttException e) {
				throw new IOException(e);
			}
		}

		public boolean isConnected() {
			return client.isConnected();
		}

		public void publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException {
			client.publish(topic, message);
		}

	}

	private Server broker;
	private int brokerPort;
	private MqttClientForTest secondClient;

	private volatile Main main;
	private volatile Future<?> mainProcess;

	@BeforeEach
	void setup() throws Exception {
		setDefaultTimeout(timeout.getSeconds(), SECONDS);
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = new MqttClientForTest(LOCALHOST, brokerPort, "client2");
		main = newMain();
		mainProcess = runAsync(() -> {
			try {
				main.doMain();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		await().until(() -> {
			MqttAdapter adapter = main.mqttAdapter();
			return adapter != null && adapter.isConnected();
		});
	}

	private Main newMain() {
		Main main = new Main();
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
		if (secondClient != null) {
			secondClient.close();
		}
		if (broker != null) {
			broker.stopServer();
		}
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

	@Test
	void doesPublishAbsWhenReceivingRel() {
		assertTimeoutPreemptively(timeout, () -> {
			publish(relativePosition());
			await().until(() -> payloads(secondClient.getReceived(), TOPIC_BALL_POSITION_ABS),
					is(asList("14.76,31.01")));
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
			muteSystemErr(() -> {
				restartBroker();
				await().until(secondClient::isConnected);
				await().until(main.mqttAdapter()::isConnected);
			});
			assertReceivesGameStartWhenSendingReset();
		});
	}

	@Test
	void scoreMessagesAreRetained() throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			publishScoresAndShutdown();
			try (MqttClientForTest thirdClient = new MqttClientForTest(LOCALHOST, brokerPort, "third-client")) {
				List<Message> receivedRetained = thirdClient.getReceived();
				await().until(() -> payloads(receivedRetained, scoreOfTeam(0)), is(asList("2")));
				await().until(() -> payloads(receivedRetained, scoreOfTeam(1)), is(asList("3")));
			}
		});
	}

	@Test
	void doesRemoveRetainedMessages()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			publishScoresAndShutdown();
			main.shutdownHook();
			await().until(() -> {
				try (MqttClientForTest thirdClient = new MqttClientForTest(LOCALHOST, brokerPort, "third-client")) {
					return thirdClient.getReceived();
				}
			}, is(empty()));
		});
	}

	private void publishScoresAndShutdown() {
		main.cognition().messages().scoreChanged(0, 1, 2);
		main.cognition().messages().scoreChanged(1, 2, 3);
		await().until(() -> messagesWithTopicOf(secondClient, scoreOfTeam(0)).count(), is(1L));
		await().until(() -> messagesWithTopicOf(secondClient, scoreOfTeam(1)).count(), is(1L));
		haltMain();
	}

	private void haltMain() {
		mainProcess.cancel(true);
	}

	private List<String> payloads(List<Message> receivedRetained, String topic) {
		return messagesWithTopic(receivedRetained, topic).map(Message::getPayload).collect(toList());
	}

	private void assertReceivesGameStartWhenSendingReset()
			throws InterruptedException, MqttPersistenceException, MqttException {
		secondClient.getReceived().clear();
		sendReset();
		publish(provider(anyAmount(), () -> noPosition(currentTimeMillis())));
		await().until(() -> messagesWithTopicOf(secondClient, "game/start").count(), is(1L));
	}

	private int anyAmount() {
		return 5;
	}

	private Stream<Message> messagesWithTopicOf(MqttClientForTest mqttClient, String topic) {
		return messagesWithTopic(mqttClient.getReceived(), topic);
	}

	private Stream<Message> messagesWithTopic(List<Message> messages, String topic) {
		return messages.stream().filter(m -> m.isTopic(topic));
	}

	private void restartBroker() throws IOException, InterruptedException {
		broker.stopServer();
		broker = newMqttServer(LOCALHOST, brokerPort);
	}

	private void sendReset() throws MqttException, MqttPersistenceException {
		publish("game/reset", "");
	}

	private void publish(Stream<RelativePosition> positions) {
		positions.forEach(this::publish);
	}

	private void publish(RelativePosition position) {
		try {
			publish(relativePosition(position.getTimestamp(), position.getX(), position.getY()));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private void publish(Message message) throws MqttException, MqttPersistenceException {
		publish(message.getTopic(), message.getPayload());
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

	private static void sleep() {
		try {
			MILLISECONDS.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
