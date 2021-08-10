package com.github.smartfootballtable.cognition.queue;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ConsumerQueueDecoratorTest {

	@Test
	void singleElementInQueueOfSize1() throws InterruptedException {
		List<String> strings = new ArrayList<>();
		consumeFromSut(addTo(strings), 1).accept("test");
		TimeUnit.MILLISECONDS.sleep(50);
		assertThat(strings, is(asList("test")));
	}

	@Test
	void whenBlockingWillAcceptAsManyElementsAsTheQueueHasSize() throws InterruptedException {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = consumeFromSut(sleepVeryLong().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);
		TimeUnit.MILLISECONDS.sleep(50);
		assertThat(strings, is(emptyList()));
	}

	@Test
	void whenBlockingAndTheQueueIsFullNoMoreElementsAreAccepted() throws InterruptedException {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = consumeFromSut(sleepVeryLong().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);

		Thread backgroundAdder = new Thread(() -> queued.accept("adding-last-element"));
		backgroundAdder.start();
		TimeUnit.SECONDS.sleep(5);
		assertThat(backgroundAdder.getState(), is(Thread.State.WAITING));
		backgroundAdder.interrupt();
	}

	private void fillQueue(Consumer<String> queued, int queueSize) {
		IntStream.rangeClosed(0, queueSize).mapToObj(i -> "test" + i).forEach(queued::accept);
	}

	private Consumer<String> sleepVeryLong() {
		return t -> {
			try {
				TimeUnit.DAYS.sleep(Long.MAX_VALUE);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
	}

	private Consumer<String> addTo(List<String> strings) {
		return strings::add;
	}

	private <T> Consumer<T> consumeFromSut(Consumer<T> consumer, int queueSize) {
		return new ConsumerQueueDecorator<T>(consumer, queueSize);
	}

}
