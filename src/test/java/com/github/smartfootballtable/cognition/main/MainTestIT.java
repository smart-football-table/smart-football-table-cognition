package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.MessageMother.TOPIC_BALL_POSITION_ABS;
import static com.github.smartfootballtable.cognition.MessageMother.relativePosition;
import static com.github.smartfootballtable.cognition.MessageMother.scoreOfTeam;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.noPosition;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.main.MainTestIT.Broker.brokerOnRandomPort;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.mqtt.MqttAdapter;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

class MainTestIT {

	private static class MainRunner<T> implements Closeable {

		private final T main;
		private final CompletableFuture<Void> completableFuture;

		public MainRunner(T main, Consumer<T> callable) {
			this.main = main;
			completableFuture = runAsync(() -> {
				try {
					callable.accept(main);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		@Override
		public void close() throws IOException {
			completableFuture.cancel(true);
		}

	}

	public static class Broker implements Closeable {

		private final int brokerPort;
		private final IConfig config;
		private final Server server;

		public static Broker brokerOnRandomPort() throws IOException {
			return new Broker(randomPort());
		}

		public Broker(int brokerPort) throws IOException {
			this.brokerPort = brokerPort;
			this.config = config(LOCALHOST, brokerPort);
			this.server = newMqttServer(config);
		}

		private static int randomPort() {
			try (ServerSocket socket = new ServerSocket(0)) {
				return socket.getLocalPort();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private static Server newMqttServer(IConfig config) throws IOException {
			Server server = new Server();
			server.startServer(config);
			return server;
		}

		private static IConfig config(String host, int port) {
			Properties properties = new Properties();
			properties.setProperty(HOST_PROPERTY_NAME, host);
			properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
			return new MemoryConfig(properties);
		}

		@Override
		public void close() throws IOException {
			server.stopServer();
		}

		public void restart() throws IOException {
			server.stopServer();
			server.startServer(config);
		}

	}

	private static Duration timeout = ofMinutes(2);
	private static final String LOCALHOST = "localhost";

	private static class MqttClientForTest implements Closeable {

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
				client.setCallback(callback(received, client));
				client.connect(connectOptions());
				return client;
			} catch (MqttException e) {
				throw new IOException(e);
			}
		}

		private MqttCallbackExtended callback(List<Message> received, MqttClient client) {
			return new MqttCallbackExtended() {

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
			};
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

		private void publish(Message message) throws MqttException, MqttPersistenceException {
			publish(message.getTopic(), message.getPayload());
		}

		private void publish(String topic, String payload) throws MqttException, MqttPersistenceException {
			publish(topic, new MqttMessage(payload.getBytes()));
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

	}

	@BeforeAll
	static void setup() throws Exception {
		setDefaultTimeout(timeout.getSeconds(), SECONDS);
	}

	@Test
	void doesPublishAbsWhenReceivingRel() throws IOException {
		try (Broker broker = brokerOnRandomPort();
				MqttClientForTest client2 = newClient(broker.brokerPort, "client2");
				MainRunner<Main> mainRunner = mainRunner(broker.brokerPort)) {
			assertTimeoutPreemptively(timeout, () -> {
				client2.publish(relativePosition());
				await().until(() -> payloads(client2.getReceived(), TOPIC_BALL_POSITION_ABS),
						is(asList("14.76,31.01")));
			});
		}
	}

	@Test
	void onResetTheNewGameIsStartedImmediatelyAlthoughThereIsNoFurtherTableInteractionAfterReset()
			throws IOException, MqttPersistenceException, MqttException {
		try (Broker broker = brokerOnRandomPort();
				MqttClientForTest client2 = newClient(broker.brokerPort, "client2");
				MainRunner<Main> mainRunner = mainRunner(broker.brokerPort)) {
			assertTimeoutPreemptively(timeout, () -> {
				client2.publish(positions(anyAmount()));
				assertReceivesGameStartWhenSendingReset(client2);
			});
		}
	}

	@Test
	void doesReconnectAndResubscribe() throws IOException, MqttPersistenceException, MqttException {
		try (Broker broker = brokerOnRandomPort();
				MqttClientForTest client2 = newClient(broker.brokerPort, "client2");
				MainRunner<Main> mainRunner = mainRunner(broker.brokerPort)) {
			assertTimeoutPreemptively(timeout, () -> {
				client2.publish(positions(anyAmount()));
				muteSystemErr(() -> {
					broker.restart();
					await().until(client2::isConnected);
					await().until(mainRunner.main.mqttAdapter()::isConnected);
				});
				assertReceivesGameStartWhenSendingReset(client2);
			});
		}
	}

	@Test
	void scoreMessagesAreRetained() throws IOException, MqttPersistenceException, MqttException {
		try (Broker broker = brokerOnRandomPort();
				MqttClientForTest client2 = newClient(broker.brokerPort, "client2");
				MainRunner<Main> mainRunner = mainRunner(broker.brokerPort)) {
			assertTimeoutPreemptively(timeout, () -> {
				publishScoresAndShutdown(client2, mainRunner);
				try (MqttClientForTest client3 = newClient(broker.brokerPort, "client3")) {
					await().until(() -> payloads(client3.getReceived(), scoreOfTeam(0)), is(asList("2")));
					await().until(() -> payloads(client3.getReceived(), scoreOfTeam(1)), is(asList("3")));
				}
			});
		}
	}

	@Test
	void doesRemoveRetainedMessagesWhenShuttingDown() throws IOException, MqttPersistenceException, MqttException {
		try (Broker broker = brokerOnRandomPort();
				MqttClientForTest client2 = newClient(broker.brokerPort, "client2");
				MainRunner<Main> mainRunner = mainRunner(broker.brokerPort)) {
			assertTimeoutPreemptively(timeout, () -> {
				publishScoresAndShutdown(client2, mainRunner);
				mainRunner.main.shutdownHook();
				await().until(() -> {
					try (MqttClientForTest thirdClient = newClient(broker.brokerPort, "client3")) {
						return thirdClient.getReceived();
					}
				}, is(empty()));
			});
		}
	}

	private static MqttClientForTest newClient(int brokerPort, String clientId) throws IOException {
		return new MqttClientForTest(LOCALHOST, brokerPort, clientId);
	}

	private static MainRunner<Main> mainRunner(int brokerPort) {
		MainRunner<Main> mainRunner = new MainRunner<>(newMain(brokerPort), MainTestIT::doMain);
		await().until(
				() -> Optional.ofNullable(mainRunner.main.mqttAdapter()).filter(MqttAdapter::isConnected).isPresent());
		return mainRunner;
	}

	private static Main newMain(int brokerPort) {
		Main main = new Main();
		main.mqttHost = LOCALHOST;
		main.mqttPort = brokerPort;
		main.tableWidth = 120;
		main.tableHeight = 68;
		main.tableUnit = CENTIMETER;
		return main;
	}

	private static void doMain(Main main) {
		try {
			main.doMain();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void publishScoresAndShutdown(MqttClientForTest client, MainRunner<Main> mainRunner) throws IOException {
		SFTCognition cognition = await().until(() -> mainRunner.main.cognition(), notNullValue());
		cognition.messages().scoreChanged(0, 1, 2);
		mainRunner.main.cognition().messages().scoreChanged(1, 2, 3);
		await().until(() -> messagesWithTopicOf(client, scoreOfTeam(0)).count(), is(1L));
		await().until(() -> messagesWithTopicOf(client, scoreOfTeam(1)).count(), is(1L));
		mainRunner.close();
	}

	private List<String> payloads(List<Message> receivedRetained, String topic) {
		return messagesWithTopic(receivedRetained, topic).map(Message::getPayload).collect(toList());
	}

	private void assertReceivesGameStartWhenSendingReset(MqttClientForTest client)
			throws MqttPersistenceException, MqttException {
		client.getReceived().clear();
		sendReset(client);
		client.publish(provider(anyAmount(), () -> noPosition(currentTimeMillis())));
		await().until(() -> messagesWithTopicOf(client, "game/start").count(), is(1L));
	}

	private static int anyAmount() {
		return 5;
	}

	private static Stream<Message> messagesWithTopicOf(MqttClientForTest mqttClient, String topic) {
		return messagesWithTopic(mqttClient.getReceived(), topic);
	}

	private static Stream<Message> messagesWithTopic(List<Message> messages, String topic) {
		return messages.stream().filter(m -> m.isTopic(topic));
	}

	private void sendReset(MqttClientForTest client) throws MqttException, MqttPersistenceException {
		client.publish("game/reset", "");
	}

	private static Stream<RelativePosition> positions(int count) {
		return provider(count, () -> create(currentTimeMillis(), 0.2, 0.3));
	}

	private static Stream<RelativePosition> provider(int count, Supplier<RelativePosition> supplier) {
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
