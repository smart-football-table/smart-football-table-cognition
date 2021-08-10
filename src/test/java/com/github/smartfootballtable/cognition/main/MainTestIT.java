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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
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

import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.mqtt.MqttAdapter;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
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

	private final int brokerPort = randomPort();

	@BeforeAll
	static void setup() throws Exception {
		setDefaultTimeout(timeout.getSeconds(), SECONDS);
	}

	private static int randomPort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		server.startServer(config(host, port));
		return server;
	}

	private IConfig config(String host, int port) {
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		return new MemoryConfig(properties);
	}

	@Test
	void doesPublishAbsWhenReceivingRel() throws IOException, InterruptedException {
		withBroker(b -> withClient("client2", c -> withMain((m, p) -> assertTimeoutPreemptively(timeout, () -> {
			c.publish(relativePosition());
			await().until(() -> payloads(c.getReceived(), TOPIC_BALL_POSITION_ABS), is(asList("14.76,31.01")));
		}))));
	}

	@Test
	void onResetTheNewGameIsStartedImmediatelyAndWithoutTableInteraction()
			throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		withBroker(b -> withClient("client2", c -> withMain((m, p) -> assertTimeoutPreemptively(timeout, () -> {
			c.publish(positions(anyAmount()));
			assertReceivesGameStartWhenSendingReset(c);
		}))));
	}

	@Test
	void doesReconnectAndResubscribe()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		withBroker(b -> withClient("client2", c -> withMain((m, p) -> assertTimeoutPreemptively(timeout, () -> {
			c.publish(positions(anyAmount()));
			muteSystemErr(() -> {
				restartBroker(b);
				await().until(c::isConnected);
				await().until(m.mqttAdapter()::isConnected);
			});
			assertReceivesGameStartWhenSendingReset(c);
		}))));
	}

	@Test
	void scoreMessagesAreRetained() throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		withBroker(b -> withClient("client2", c -> withMain((m, p) -> assertTimeoutPreemptively(timeout, () -> {
			publishScoresAndShutdown(c, m, p);
			try (MqttClientForTest thirdClient = new MqttClientForTest(LOCALHOST, brokerPort, "third-client")) {
				List<Message> receivedRetained = thirdClient.getReceived();
				await().until(() -> payloads(receivedRetained, scoreOfTeam(0)), is(asList("2")));
				await().until(() -> payloads(receivedRetained, scoreOfTeam(1)), is(asList("3")));
			}
		}))));
	}

	@Test
	void doesRemoveRetainedMessages()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		withBroker(b -> withClient("client2", c -> withMain((m, p) -> assertTimeoutPreemptively(timeout, () -> {
			publishScoresAndShutdown(c, m, p);
			m.shutdownHook();
			await().until(() -> {
				try (MqttClientForTest thirdClient = new MqttClientForTest(LOCALHOST, brokerPort, "client3")) {
					return thirdClient.getReceived();
				}
			}, is(empty()));
		}))));
	}

	// TODO make static
	private void withBroker(Consumer<Server> consumer) {
		try {
			Server broker = newMqttServer(LOCALHOST, brokerPort);
			consumer.accept(broker);
			broker.stopServer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO make static
	private void withClient(String clientId, Consumer<MqttClientForTest> runnable) {
		try (MqttClientForTest client = new MqttClientForTest(LOCALHOST, brokerPort, clientId)) {
			runnable.accept(client);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO make static
	private void withMain(BiConsumer<Main, CompletableFuture<?>> consumer) {
		Main main = newMain();
		CompletableFuture<Void> mainProcess = startMain(main);
		await().until(() -> Optional.ofNullable(main.mqttAdapter()).filter(MqttAdapter::isConnected).isPresent());
		consumer.accept(main, mainProcess);
		mainProcess.cancel(true);
	}

	private CompletableFuture<Void> startMain(Main main) {
		return runAsync(() -> {
			try {
				main.doMain();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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

	private void publishScoresAndShutdown(MqttClientForTest secondClient, Main main, CompletableFuture<?> mainProcess) {
		main.cognition().messages().scoreChanged(0, 1, 2);
		main.cognition().messages().scoreChanged(1, 2, 3);
		await().until(() -> messagesWithTopicOf(secondClient, scoreOfTeam(0)).count(), is(1L));
		await().until(() -> messagesWithTopicOf(secondClient, scoreOfTeam(1)).count(), is(1L));
		mainProcess.cancel(true);
	}

	private List<String> payloads(List<Message> receivedRetained, String topic) {
		return messagesWithTopic(receivedRetained, topic).map(Message::getPayload).collect(toList());
	}

	private void assertReceivesGameStartWhenSendingReset(MqttClientForTest secondClient)
			throws InterruptedException, MqttPersistenceException, MqttException {
		secondClient.getReceived().clear();
		sendReset(secondClient);
		secondClient.publish(provider(anyAmount(), () -> noPosition(currentTimeMillis())));
		await().until(() -> messagesWithTopicOf(secondClient, "game/start").count(), is(1L));
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

	private void restartBroker(Server broker) throws IOException, InterruptedException {
		broker.stopServer();
		broker.startServer(config(LOCALHOST, brokerPort));
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
