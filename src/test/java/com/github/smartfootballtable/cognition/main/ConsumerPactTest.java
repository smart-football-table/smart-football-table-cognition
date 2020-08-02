package com.github.smartfootballtable.cognition.main;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static com.github.smartfootballtable.cognition.MessageMother.TOPIC_BALL_POSITION_REL;
import static com.github.smartfootballtable.cognition.MessageMother.absolutePosition;
import static com.github.smartfootballtable.cognition.Topic.GAME_START;
import static com.github.smartfootballtable.cognition.Topic.isNotTopic;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactFolder;
import au.com.dius.pact.core.model.messaging.MessagePact;

@ExtendWith(PactConsumerTestExt.class)
@PactFolder("pacts")
class ConsumerPactTest {

	Table table = new Table(200, 100, CENTIMETER);
	List<Message> consumed = new ArrayList<>();
	SFTCognition cognition = new SFTCognition(table, consumed::add);

	@Test
	@PactTestFor(providerName = "detection", pactMethod = "relativeBallPositionPact", providerType = ASYNCH)
	void verifyRelativePositionIsPublished(MessagePact pact) {
		process(messagesOf(pact).map(this::toRelPosition));
		assertThat(filter(isNotTopic(GAME_START)), is(asList(absolutePosition("24.60", "45.60"))));
	}

//	String date = "2020-07-21T18:42:24.123456";
//	System.out.println(DateTime.parse(date).getMillis());

	@Pact(consumer = "cognition")
	MessagePact relativeBallPositionPact(MessagePactBuilder builder) {
		return builder //
				.given("the ball moved on the table") //
				.expectsToReceive("the relative position gets published") //
				.withContent(body(csv("123456789012345678", "0.123", "0.456"),
						csv(positiveLongValue(), positiveFloatingPoint(), positiveFloatingPoint()))) //
				.toPact();
	}

	private String csv(String... values) {
		return stream(values).collect(joining(","));
	}

	private void process(Stream<RelativePosition> positions) {
		positions.forEach(cognition::process);
	}

	private Stream<Message> messagesOf(MessagePact pact) {
		return pact.getMessages().stream().map(this::toMessage);
	}

	private PactDslJsonBody body(String payload, String matcher) {
		return new PactDslJsonBody() //
				.stringValue("topic", TOPIC_BALL_POSITION_REL) //
				.stringMatcher("payload", matcher, payload);
	}

	private static String positiveLongValue() {
		return "[0-9]+";
	}

	private static String positiveFloatingPoint() {
		return positiveLongValue() + "(?:\\.[0-9]+)?";
	}

	private List<Message> filter(Predicate<Message> predicate) {
		return consumed().filter(predicate).collect(toList());
	}

	private Stream<Message> consumed() {
		return consumed.stream();
	}

	private Message toMessage(au.com.dius.pact.core.model.messaging.Message message) {
		JSONObject jsonObject = new JSONObject(message.contentsAsString());
		return message(jsonObject.getString("topic"), jsonObject.getString("payload"));
	}

	private RelativePosition toRelPosition(Message message) {
		return Optional.of(message) //
				.filter(cognition.messages()::isRelativePosition) //
				.map(Message::getPayload) //
				.map(cognition.messages()::parsePosition) //
				.orElseThrow(() -> new IllegalStateException(message + " not a relative position"));
	}

}
