package com.github.smartfootballtable.cognition.mqtt;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.github.smartfootballtable.cognition.MessageProvider;
import com.github.smartfootballtable.cognition.data.Message;

public class MqttAdapter implements Consumer<Message>, MessageProvider, Closeable {

	private static final class ConsumerMultiplexer implements Consumer<Message> {

		private final List<Consumer<Message>> consumers;

		public ConsumerMultiplexer(Consumer<Message> consumer1, Consumer<Message> consumer2) {
			consumers = new CopyOnWriteArrayList<>(Arrays.asList(consumer1, consumer2));
		}

		@Override
		public void accept(Message message) {
			for (Consumer<Message> consumer : consumers) {
				try {
					consumer.accept(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void add(Consumer<Message> consumer) {
			this.consumers.add(consumer);
		}
	}

	private static final Consumer<Message> NOOP = m -> {
	};

	private final MqttClient mqttClient;
	private Consumer<Message> consumer = NOOP;

	public MqttAdapter(String host, int port) throws IOException {
		try {
			mqttClient = new MqttClient("tcp://" + host + ":" + port, getClass().getName(), new MemoryPersistence());
			mqttClient.setTimeToWait(SECONDS.toMillis(5));
			mqttClient.setCallback(callback());
			// connect in background, so even if the broker is not running at startup we
			// will connect when it's available
			connectInBackground();
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

	private void connectInBackground() {
		newSingleThreadExecutor().execute(() -> {
			while (true) {
				try {
					mqttClient.connect(connectOptions());
				} catch (MqttException e) {
					e.printStackTrace();
				}
				if (mqttClient.isConnected()) {
					break;
				}
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	private MqttCallback callback() {
		return new MqttCallbackExtended() {

			@Override
			public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
				Message message = message(topic, new String(mqttMessage.getPayload()));
				consumer.accept(message);
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void connectionLost(Throwable cause) {
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				try {
					subscribe();
				} catch (MqttException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	private void subscribe() throws MqttException {
		mqttClient.subscribe("#");
	}

	@Override
	public void accept(Message message) {
		try {
			mqttClient.publish(message.getTopic(), convert(message));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private MqttMessage convert(Message message) {
		MqttMessage mqttMessage = new MqttMessage(message.getPayload().getBytes());
		mqttMessage.setRetained(message.isRetained());
		return mqttMessage;
	}

	@Override
	public void close() throws IOException {
		try {
			if (mqttClient.isConnected()) {
				mqttClient.disconnect();
			}
			mqttClient.close();
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

	public boolean isConnected() {
		return mqttClient.isConnected();
	}

	@Override
	public synchronized void addConsumer(Consumer<Message> consumer) {
		if (this.consumer == NOOP) {
			this.consumer = consumer;
		} else if (this.consumer instanceof ConsumerMultiplexer) {
			((ConsumerMultiplexer) this.consumer).add(consumer);
		} else {
			this.consumer = new ConsumerMultiplexer(this.consumer, consumer);
		}
	}

}