package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.github.smartfootballtable.cognition.data.Message;

public final class MessageMother {

	public static final String TOPIC_BALL_POSITION_REL = "ball/position/rel";
	public static final String TOPIC_BALL_POSITION_ABS = "ball/position/abs";

	private MessageMother() {
		super();
	}

	public static Message relativePosition() {
		return relativePosition(123456789012345678L, 0.123, 0.456);
	}

	public static Message relativePosition(Object... values) {
		return message(TOPIC_BALL_POSITION_REL, joined(values));
	}

	public static Message absolutePosition(Object... values) {
		return message(TOPIC_BALL_POSITION_ABS, joined(values));
	}

	private static String joined(Object... values) {
		return stream(values).map(String::valueOf).collect(joining(","));
	}

}
