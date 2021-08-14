package com.github.smartfootballtable.cognition.queue;

import static com.github.smartfootballtable.cognition.util.Sleep.sleep;
import static java.lang.Long.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ConsumerQueueDecoratorTest {

	@Test
	void singleElementInQueueOfSize1() {
		List<String> strings = new ArrayList<>();
		consumeFromSut(addTo(strings), 1).accept("test");
		sleep(50, MILLISECONDS);
		assertThat(strings, is(asList("test")));
	}

	@Test
	void whenBlockingWillAcceptAsManyElementsAsTheQueueHasSize() {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = consumeFromSut(sleepVeryLong().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);
		sleep(50, MILLISECONDS);
		assertThat(strings, is(emptyList()));
	}

	@Test
	void whenBlockingAndTheQueueIsFullNoMoreElementsAreAccepted() {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = consumeFromSut(sleepVeryLong().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);

		Thread backgroundAdder = new Thread(() -> queued.accept("adding-last-element"));
		backgroundAdder.start();
		sleep(5, SECONDS);
		assertThat(backgroundAdder.getState(), is(Thread.State.WAITING));
		backgroundAdder.interrupt();
	}

	private void fillQueue(Consumer<String> queued, int queueSize) {
		IntStream.rangeClosed(0, queueSize).mapToObj(i -> "test" + i).forEach(queued::accept);
	}

	private Consumer<String> sleepVeryLong() {
		return t -> sleep(MAX_VALUE, DAYS);
	}

	private Consumer<String> addTo(List<String> strings) {
		return strings::add;
	}

	private <T> Consumer<T> consumeFromSut(Consumer<T> consumer, int queueSize) {
		return new ConsumerQueueDecorator<T>(consumer, queueSize);
	}

}
