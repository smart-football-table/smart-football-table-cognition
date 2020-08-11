package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.MessageMother.*;
import static com.github.smartfootballtable.cognition.MessageMother.TOPIC_BALL_POSITION_REL;

import java.util.function.Predicate;

import com.github.smartfootballtable.cognition.data.Message;

public enum Topic {

	BALL_POSITION_ABS(topicIs(TOPIC_BALL_POSITION_ABS)), //
	BALL_POSITION_REL(topicIs(TOPIC_BALL_POSITION_REL)), //
	BALL_DISTANCE_CM(topicIs("ball/distance/cm")), //
	BALL_OVERALL_DISTANCE_CM(topicIs("ball/distance/overall/cm")), //
	BALL_VELOCITY_KMH(topicIs("ball/velocity/kmh")), //
	BALL_VELOCITY_MS(topicIs("ball/velocity/ms")), //
	GAME_START(topicIs("game/start")), //
	GAME_FOUL(topicIs("game/foul")), //
	GAME_IDLE(topicIs("game/idle")), //
	TEAM_SCORE(topicStartsWith(MessageMother.TEAM_SCORE)), //
	TEAM_SCORE_LEFT(topicIs(MessageMother.TEAM_SCORE + Topic.TEAM_ID_LEFT)), //
	TEAM_SCORE_RIGHT(topicIs(MessageMother.TEAM_SCORE + Topic.TEAM_ID_RIGHT)), //
	TEAM_SCORED(topicIs("team/scored")); //

	public static final String TEAM_ID_LEFT = "0";
	public static final String TEAM_ID_RIGHT = "1";

	private final Predicate<Message> predicate;

	private static Predicate<Message> topicIs(String topic) {
		return m -> m.getTopic().equals(topic);
	}

	private static Predicate<Message> topicStartsWith(String topic) {
		return m -> m.getTopic().startsWith(topic);
	}

	private Topic(Predicate<Message> predicate) {
		this.predicate = predicate;
	}

	public static Predicate<Message> isTopic(Topic topic) {
		return topic.predicate;
	}

	public static Predicate<Message> isNotTopic(Topic topic) {
		return isTopic(topic).negate();
	}

}