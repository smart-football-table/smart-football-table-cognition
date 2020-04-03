package com.github.smartfootballtable.cognition.main;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

import com.github.smartfootballtable.cognition.MessageProvider;
import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

public final class MqttProcessor {

	public static final class ToRelPosition implements Function<Message, Optional<RelativePosition>> {

		private final Messages messages;
		private Message lastX, lastY;

		public ToRelPosition(Messages messages) {
			this.messages = messages;
		}

		@Override
		public Optional<RelativePosition> apply(Message message) {
			if (messages.isRelativePositionX(message)) {
				lastX = message;
			} else if (messages.isRelativePositionY(message)) {
				lastY = message;
			}
			return messages.parsePosition(lastX, lastY);
		}
	}

	private MqttProcessor() {
		super();
	}

	public static void processMqtt(SFTCognition cognition, MessageProvider messageProvider) {
		BlockingQueue<RelativePosition> queue = new ArrayBlockingQueue<>(3);
		Messages messages = cognition.messages();
		ToRelPosition toRelPosition = new ToRelPosition(messages);
		messageProvider.addConsumer(m -> toRelPosition.apply(m).ifPresent(queue::offer));
		cognition.process(() -> {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
