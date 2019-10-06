package com.github.smartfootballtable.cognition.main;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.smartfootballtable.cognition.Messages;
import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;

@ExtendWith(PactConsumerTestExt.class)
class ConsumerPactTest {

	// JAVA https://reflectoring.io/cdc-pact-messages/
	// JS https://reflectoring.io/pact-node-messaging/
	// JS
	// https://github.com/pact-foundation/pact-js/tree/feat/message-pact/#asynchronous-api-testing

	@Test
	@PactTestFor(providerName = "detection", providerType = ASYNCH)
	void verifyCreatePersonPact(MessagePact pact) {
		Table table = new Table(120, 68, CENTIMETER);
		List<Message> consumed = new ArrayList<>();
		SFTCognition cognition = new SFTCognition(table, consumed::add);
		cognition.process(
				pact.getMessages().stream().map(this::toMessage).map(m -> toRelPosition(cognition.messages(), m)));
		assertThat(filter(consumed), is(asList(message("ball/position/abs", "14.0,31.0"))));
	}

	@Pact(consumer = "cognition")
	MessagePact userCreatedMessagePact(MessagePactBuilder builder) {
		return builder.expectsToReceive("ball moves on table") //
				.withMetadata(topic("ball/position/rel")) //
				.withContent(payload("0.123,0.456")) //
				.toPact();
	}

	private DslPart payload(String payload) {
		return new PactDslRootValue() {
			@Override
			public String toString() {
				return payload;
			}
		};
	}

	private Map<String, Object> topic(String topic) {
		Map<String, Object> map = new HashMap<>();
		map.put("contentType", "text/plain");
		map.put("topic", topic);
		return map;
	}

	private List<Message> filter(List<Message> messages) {
		return messages.stream().filter(m -> !m.getTopic().equals("game/start")).collect(toList());
	}

	private Message toMessage(au.com.dius.pact.core.model.messaging.Message message) {
		return message(message.getMetaData().get("topic"), message.contentsAsString());
	}

	private RelativePosition toRelPosition(Messages messages, Message m) {
		return messages.isRelativePosition(m) ? messages.parsePosition(m.getPayload()) : null;
	}

}
