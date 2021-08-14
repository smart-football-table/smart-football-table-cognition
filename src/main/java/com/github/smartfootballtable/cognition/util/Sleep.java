package com.github.smartfootballtable.cognition.util;

import java.util.concurrent.TimeUnit;

public final class Sleep {

	private Sleep() {
		super();
	}

	public static void sleep(long timeout, TimeUnit timeUnit) {
		try {
			timeUnit.sleep(timeout);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
