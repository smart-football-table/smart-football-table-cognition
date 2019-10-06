package com.github.smartfootballtable.cognition.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.smartfootballtable.cognition.MessageProvider;
import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

public final class MqttProcessor {

	private MqttProcessor() {
		super();
	}

	public static void processMqtt(SFTCognition cognition, MessageProvider messageProvider) {
		BlockingQueue<RelativePosition> queue = new ArrayBlockingQueue<>(3);
		Messages messages = cognition.messages();
		messageProvider.addConsumer(m -> {
			if (messages.isRelativePosition(m)) {
				queue.offer(messages.parsePosition(m.getPayload()));
			}
		});
		cognition.process(() -> {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
