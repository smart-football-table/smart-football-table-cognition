package com.github.smartfootballtable.cognition.main;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.main.MqttProcessor.ToRelPosition;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;

@ExtendWith(PactConsumerTestExt.class)
class ConsumerPactTest {

	@BeforeEach
	void setup() {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = "detection", pactMethod = "relativeBallPositionPact", providerType = ASYNCH)
	void verifyRelativePositionIsPublished(MessagePact pact) {
		System.out.println(pact.getMessages());
		Table table = new Table(200, 100, CENTIMETER);
		List<Message> consumed = new ArrayList<>();
		SFTCognition cognition = new SFTCognition(table, consumed::add);
		cognition.process(
				toRelPosition(cognition.messages(), pact.getMessages().stream().map(this::toMessage)).stream());
		assertThat(filter(consumed), is(asList( //
				message("ball/position/123456789/abs/x", "24.60"), //
				message("ball/position/123456789/abs/y", "45.60") //
		)));
	}

	@Pact(consumer = "cognition")
	MessagePact relativeBallPositionPact(MessagePactBuilder builder) {
		return builder //
				.given("the ball moved on the table") //
				.expectsToReceive("the relative x position gets published")
				.withContent(body("ball/position/123456789/rel/x", "0.123")) //
				.expectsToReceive("the relative y position gets published")
				.withContent(body("ball/position/123456789/rel/y", "0.456")) //
				.toPact();
	}

	private PactDslJsonBody body(String topic, String payload) {
		return new PactDslJsonBody() //
				.stringType("topic", topic) //
				.stringMatcher("payload", "\\d*\\.?\\d+", payload);
	}

	private List<Message> filter(List<Message> messages) {
		return messages.stream().filter(m -> !m.getTopic().equals("game/start")).collect(toList());
	}

	private Message toMessage(au.com.dius.pact.core.model.messaging.Message message) {
		JSONObject jsonObject = new JSONObject(message.contentsAsString());
		return message(jsonObject.getString("topic"), jsonObject.getString("payload"));
	}

	private List<RelativePosition> toRelPosition(Messages messages, Stream<Message> m) {
		ToRelPosition toRelPosition = new ToRelPosition(messages);
		return m.map(toRelPosition).filter(Optional::isPresent).map(Optional::get).collect(toList());
	}

}
