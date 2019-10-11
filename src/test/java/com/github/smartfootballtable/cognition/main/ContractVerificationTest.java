package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;

@Provider("cognition")
@PactFolder("pacts")
public class ContractVerificationTest {

	@TestTarget
	public final AmpqTestTarget target = new AmpqTestTarget(asList("com.github.smartfootballtable.cognition"));

	private final List<Message> sendMessages = new ArrayList<>();
	private final SFTCognition cognition = new SFTCognition(anyTable(), sendMessages::add);

	@BeforeEach
	void before(PactVerificationContext context) {
		context.setTarget(target);
	}

	@TestTemplate
	@ExtendWith(PactVerificationInvocationContextProvider.class)
	void verifyContracts(PactVerificationContext context) {
		context.verifyInteraction();
	}

	@State("a goal was shot")
	public void aGoalWasShot() {
		int oldScore = anyScore();
		cognition.messages().scoreChanged(anyTeam(), oldScore, oldScore + 1);
	}

	@State("a team fouled")
	public void aTeamFouled() {
		cognition.messages().foul();
	}

	@State("the table is idle")
	public void theTableIsIdle() {
		cognition.messages().idle(true);
	}

	@State("a team has won the game")
	public void oneTeamHasWonTheGame() {
		cognition.messages().gameWon(anyTeam());
	}

	@State("a game ends draw")
	public void aGameEndsDraw() {
		cognition.messages().gameDraw(0, 1, anyTeam(), 2, 3, 4, 5, 6);
	}

	@PactVerifyProvider("the scoring team gets published")
	public String theScoringTeamGetsPublished() {
		return payloadsWithTopic("team/scored");
	}

	@PactVerifyProvider("the team's new score")
	public String theNewScoreGetsPublished() {
		return payloads(m -> m.getTopic().matches("team\\/score\\/\\d+"));
	}

	@PactVerifyProvider("the foul message")
	public String theFoulMessage() {
		return payloadsWithTopic("game/foul");
	}

	@PactVerifyProvider("the gameover message")
	public String theGameoverMessageWin() {
		return payloadsWithTopic("game/gameover");
	}

	@PactVerifyProvider("the idle message")
	public String theIdleMessage() {
		return payloadsWithTopic("game/idle");
	}

	private String payloadsWithTopic(String topic) {
		return payloads(m -> topic.equals(m.getTopic()));
	}

	private String payloads(Predicate<Message> predicate) {
		return sendMessages.stream().filter(predicate).map(this::toJson).findFirst().orElse("");
	}

	private Table anyTable() {
		return new Table(120, 68, CENTIMETER);
	}

	private int anyTeam() {
		return MAX_VALUE;
	}

	private int anyScore() {
		return 1;
	}

	private String toJson(Message message) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("topic", message.getTopic());
		jsonObject.put("payload", message.getPayload());
		return jsonObject.toString();
	}

}