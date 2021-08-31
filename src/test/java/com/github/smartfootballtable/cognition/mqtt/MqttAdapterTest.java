package com.github.smartfootballtable.cognition.mqtt;

import static com.github.smartfootballtable.cognition.MessageMother.relativePosition;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.util.Arrays.asList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.data.Message;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

class MqttAdapterTest {

	private static final String LOCALHOST = "localhost";

	private int brokerPort;
	private Server server;
	private MqttAdapter mqttAdapter;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new ArrayList<>();

	@BeforeEach
	void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		server = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "second-client-for-test");
		mqttAdapter = new MqttAdapter(LOCALHOST, brokerPort);
		await().until(secondClient::isConnected);
		await().until(mqttAdapter::isConnected);
	}

	@AfterEach
	void tearDown() throws MqttException, IOException {
		secondClient.disconnect();
		secondClient.close();
		mqttAdapter.close();
		server.stopServer();
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
		client.connect();
		client.setCallback(new MqttCallback() {

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				messagesReceived.add(message(topic, new String(message.getPayload())));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void connectionLost(Throwable cause) {
			}
		});
		client.subscribe("#");
		return client;
	}

	@Test
	void messagesSentByMqttAdapterAreReceivedBySecondClient() throws InterruptedException {
		List<Message> messages = asList(message("topic1", "payload1"), message("topic2", "payload2"));
		messages.forEach(mqttAdapter::accept);
		await().untilAsserted(() -> assertThat(messagesReceived, is(messages)));
	}

	@Test
	void stacktracesArePrintedOnStdErrAndThirdConsumerGetsCalled() throws Exception {
		String exceptionText1 = "any " + UUID.randomUUID() + "text";
		String exceptionText2 = "other " + UUID.randomUUID() + "text";
		assertThat(tapSystemErr(() -> {
			List<Message> messages = new ArrayList<>();
			mqttAdapter.addConsumer(aConsumerThatThrows(() -> new NullPointerException(exceptionText1)));
			mqttAdapter.addConsumer(aConsumerThatThrows(() -> new IllegalStateException(exceptionText2)));
			mqttAdapter.addConsumer(aConsumerThatCollectsTo(messages));
			Message relativePosition = relativePosition();
			secondClient.publish(relativePosition.getTopic(), relativePosition.getPayload().getBytes(), 0, false);
			await().untilAsserted(() -> assertThat(messages, is(Arrays.asList(relativePosition))));
		}), allOf(containsString(exceptionText1), containsString(exceptionText2)));
	}

	private Consumer<Message> aConsumerThatThrows(Supplier<RuntimeException> supplier) {
		return m -> {
			throw supplier.get();
		};
	}

	private Consumer<Message> aConsumerThatCollectsTo(List<Message> messages) {
		return messages::add;
	}

}
