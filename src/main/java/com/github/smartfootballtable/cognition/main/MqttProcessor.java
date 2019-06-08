package com.github.smartfootballtable.cognition.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.mqtt.MqttConsumer;

public final class MqttProcessor {

	private MqttProcessor() {
		super();
	}

	public static void processMqtt(SFTCognition cognition, MqttConsumer mqttConsumer) {
		BlockingQueue<Message> queue = new ArrayBlockingQueue<>(3);
		Messages messages = cognition.messages();
		mqttConsumer.addConsumer(m -> {
			if (messages.isRelativePosition(m)) {
				queue.offer(m);
			}
		});
		cognition.process(() -> {
			try {
				return messages.parsePosition(queue.take().getPayload());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
