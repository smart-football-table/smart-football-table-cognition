package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.github.smartfootballtable.cognition.data.Message;

public final class MessageMother {

	public static final String TOPIC_BALL_POSITION_REL = "ball/position/rel";
	public static final String TOPIC_BALL_POSITION_ABS = "ball/position/abs";
	public static final String GAME_FOUL = "game/foul";

	public static final String GAME_START = "game/start";
	public static final String TEAM_SCORE = "team/score/";
	public static final String GAME_GAMEOVER = "game/gameover";
	public static final String GAME_IDLE = "game/idle";

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

	public static String scoreOfTeam(int team) {
		return TEAM_SCORE + team;
	}

	public static String teamScored() {
		return "team/scored";
	}

	private static String joined(Object... values) {
		return stream(values).map(String::valueOf).collect(joining(","));
	}

}
