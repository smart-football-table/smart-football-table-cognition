package com.github.smartfootballtable.cognition.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.smartfootballtable.cognition.MessageProvider;
import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

public final class QueueWorker {

	private QueueWorker() {
		super();
	}

	public static void consumeViaQueue(SFTCognition cognition, MessageProvider messageProvider) {
		BlockingQueue<RelativePosition> queue = new ArrayBlockingQueue<>(10);
		Messages messages = cognition.messages();
		messageProvider.addConsumer(consumer(queue, messages));
		cognition.process(supplier(queue));
	}

	private static Consumer<Message> consumer(BlockingQueue<RelativePosition> queue, Messages messages) {
		return m -> {
			if (messages.isRelativePosition(m)) {
				queue.offer(messages.parsePosition(m.getPayload()));
			}
		};
	}

	private static Supplier<RelativePosition> supplier(BlockingQueue<RelativePosition> queue) {
		return () -> {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		};
	}

}
