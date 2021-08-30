package com.github.smartfootballtable.cognition.queue;

import static java.util.concurrent.CompletableFuture.runAsync;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class ConsumerQueueDecorator<T> implements Consumer<T> {

	private final BlockingQueue<T> blockingQueue;

	public ConsumerQueueDecorator(Consumer<T> delegate) {
		this(delegate, 100);
	}

	public ConsumerQueueDecorator(Consumer<T> delegate, int queueSize) {
		this.blockingQueue = new LinkedBlockingDeque<>(queueSize);
		runAsync(() -> {
			while (true) {
				try {
					delegate.accept(take());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	private T take() {
		try {
			return blockingQueue.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public void accept(T s) {
		try {
			blockingQueue.put(s);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}