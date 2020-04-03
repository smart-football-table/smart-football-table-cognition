package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.ball;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.frontOfLeftGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.frontOfRightGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.kickoff;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.lowerRightCorner;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.offTable;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.pos;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder.upperLeftCorner;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.SFTCognitionTest.MessageBuilder.BallPosBuilder;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;
import com.github.smartfootballtable.cognition.detector.GoalDetector;
import com.github.smartfootballtable.cognition.main.MqttProcessor.ToRelPosition;

class SFTCognitionTest {

	public static class MessageBuilder {

		public static class BallPosBuilder {

			private double x;
			private double y;

			public BallPosBuilder(double x, double y) {
				this.x = x;
				this.y = y;
			}

			public static BallPosBuilder kickoff() {
				return pos(centerX(), centerY());
			}

			public static BallPosBuilder pos(double x, double y) {
				return new BallPosBuilder(x, y);
			}

			public static BallPosBuilder upperLeftCorner() {
				return pos(0.0, 0.0);
			}

			public static BallPosBuilder lowerRightCorner() {
				return pos(1.0, 1.0);
			}

			private static double centerX() {
				return 0.5;
			}

			private static double centerY() {
				return 0.5;
			}

			public BallPosBuilder left(double adjX) {
				x -= adjX;
				return this;
			}

			public BallPosBuilder right(double adjX) {
				x += adjX;
				return this;
			}

			public BallPosBuilder up(double adjY) {
				y -= adjY;
				return this;
			}

			public BallPosBuilder down(double adjY) {
				y += adjY;
				return this;
			}

			public static BallPosBuilder frontOfRightGoal() {
				return kickoff().right(0.3);
			}

			public static BallPosBuilder frontOfLeftGoal() {
				return kickoff().left(0.3);
			}

			public static BallPosBuilder offTable() {
				RelativePosition noBall = RelativePosition.noPosition(0L);
				return pos(noBall.getX(), noBall.getY());
			}

		}

		private long timestamp;
		private final List<Message> messages = new ArrayList<>();

		private MessageBuilder(long timestamp) {
			this.timestamp = timestamp;
		}

		public static MessageBuilder ball() {
			return ball(anyTimestamp());
		}

		public static MessageBuilder ball(long timestamp) {
			return messagesStartingAt(timestamp);
		}

		private static MessageBuilder messagesStartingAt(long timestamp) {
			return new MessageBuilder(timestamp);
		}

		private static long anyTimestamp() {
			return 1234;
		}

		private MessageBuilder then() {
			return this;
		}

		private MessageBuilder then(BallPosBuilder ballPosBuilder) {
			return at(ballPosBuilder);
		}

		private MessageBuilder at(BallPosBuilder ballPosBuilder) {
			messages.addAll(makeMessages(ballPosBuilder.x, ballPosBuilder.y));
			return autoinc(+1);
		}

		private MessageBuilder thenAfterMillis(long duration) {
			return thenAfter(duration, MILLISECONDS);
		}

		private MessageBuilder thenAfter(long duration, TimeUnit timeUnit) {
			timestamp += timeUnit.toMillis(duration);
			return autoinc(-1);
		}

		private MessageBuilder autoinc(int i) {
			timestamp += i;
			return this;
		}

		private MessageBuilder invalidData() {
			messages.addAll(makeMessages("A", "B"));
			return this;
		}

		private List<Message> makeMessages(Object x, Object y) {
			return asList( //
					message("ball/position/" + timestamp + "/rel/x", x), //
					message("ball/position/" + timestamp + "/rel/y", y) //
			);
		}

		private MessageBuilder prepareForLeftGoal() {
			return prepateForGoal().at(frontOfLeftGoal());
		}

		private MessageBuilder prepareForRightGoal() {
			return prepateForGoal().at(frontOfRightGoal());
		}

		private MessageBuilder prepateForGoal() {
			return at(kickoff()).thenAfter(100, MILLISECONDS);
		}

		private MessageBuilder score() {
			return offTableFor(2, SECONDS);
		}

		private MessageBuilder offTableFor(int duration, TimeUnit timeUnit) {
			return at(offTable()).thenAfter(duration, timeUnit).then(offTable());
		}

		public MessageBuilder thenCall(Consumer<Consumer<RelativePosition>> setter, Consumer<RelativePosition> c) {
			setter.accept(new Consumer<RelativePosition>() {
				long timestampNow = timestamp;
				private boolean processed;
				private boolean called;

				@Override
				public void accept(RelativePosition pos) {
					if (processed && !called) {
						c.accept(pos);
						called = true;
					}
					if (pos.getTimestamp() == timestampNow) {
						processed = true;
					}
				}
			});
			return this;
		}

		private List<Message> build() {
			return new ArrayList<>(messages);
		}

	}

	private final List<Message> collectedMessages = new ArrayList<>();
	private final Consumer<Message> messageCollector = collectedMessages::add;
	private final GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();
	private Consumer<RelativePosition> inProgressConsumer = p -> {
	};

	private SFTCognition sut;
	private final List<Message> inputMessages = new ArrayList<>();

	@Test
	void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(kickoff()));
		whenInputWasProcessed();
		thenTheAbsolutePositionOnTheTableIsPublished("50.00", "40.00");
	}

	@Test
	void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(pos(0.9, 0.1)));
		whenInputWasProcessed();
		thenTheAbsolutePositionOnTheTableIsPublished("90.00", "8.00");
	}

	@Test
	void malformedMessageIsRead() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().invalidData());
		whenInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(offTable()));
		whenInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	void whenTwoPositionsAreRead_VelocityGetsPublished_MetricTable() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("ball/distance/cm"), is("128.06"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/ms"), is("1.28"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/kmh"), is("4.61"));
	}

	@Test
	void whenTwoPositionsAreRead_VelocityGetsPublished_ImperialTable() throws IOException {
		givenATableOfSize(100, 80, INCHES);
		givenInputToProcessIs(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("ball/distance/inch"), is("128.06"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/ips"), is("128.06"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/mph"), is("7.28"));
	}

	@Test
	void overallDistance() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		makeDiamondMoveOnTableIn();
		thenPayloadsWithTopicAre("ball/distance/overall/cm", "8.00", "18.00", "26.00", "36.00");
	}

	@Test
	void overallDistanceIsSentInInchWhenTableIsImperial() throws IOException {
		givenATableOfSize(100, 80, INCHES);
		makeDiamondMoveOnTableIn();
		thenPayloadsWithTopicAre("ball/distance/overall/inch", "8.00", "18.00", "26.00", "36.00");
	}

	private void makeDiamondMoveOnTableIn() throws IOException {
		BallPosBuilder base = kickoff();
		givenInputToProcessIs(ball() //
				.at(base.left(0.1)) //
				.at(base.up(0.1)) //
				.at(base.right(0.1)) //
				.at(base.down(0.1)) //
				.at(base.left(0.1)));
		whenInputWasProcessed();
	}

	@Test
	void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(0);
		thenPayloadsWithTopicAre("team/score/0", "1");
	}

	@Test
	void canDetectGoalOnRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForRightGoal().then().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(1);
		thenPayloadsWithTopicAre("team/score/1", "1");
	}

	@Test
	void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForRightGoal().then().at(frontOfRightGoal().left(0.01)).score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().at(frontOfLeftGoal().right(0.01)).score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/scored", times("0", 3));
		thenPayloadsWithTopicAre("team/score/0", "1", "2", "3");
	}

	@Test
	void noGoalsIfBallWasNotDetectedAtMiddleLine() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().at(frontOfLeftGoal()).then().score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	void noGoalsIfThereAreNotTwoSecondsWithoutPositions() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		long timeout = SECONDS.toMillis(2);
		givenTimeWithoutBallTilGoal(timeout, MILLISECONDS);
		long oneMsMeforeTimeout = timeout - 1;
		givenInputToProcessIs(ball()
				//
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then()
				//
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then()
				//
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(kickoff()).then()
				//
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(kickoff()) //
		);
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	void withoutWaitTimeTheGoalDirectlyCounts() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenTimeWithoutBallTilGoal(0, MILLISECONDS);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(0);
	}

	@Test
	void canRevertGoals() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score().then(anyCorner()));
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/score/0", "1", "0");
		thenPayloadsWithTopicAre("team/scored", "0");
	}

	@Test
	void alsoRevertsIfBallIsDetectedSomewhereElseAfterGoalAndThenInTheCorner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score().then(pos(0.0, 0.5)).then(anyCorner()));
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/score/0", "1", "0");
		thenPayloadsWithTopicAre("team/scored", "0");
	}

	@Test
	void afterRevertedGoalHappensOnOtherSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score().then(anyCorner()).then().prepareForRightGoal()
				.then().score());
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/score/1", "1");
	}

	@Test
	void doesSendWinner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenInputWasProcessed();
		assertThat(lastMessageWithTopic("team/score/0").getPayload(), is("6"));
		thenWinnerAre(0);
	}

	@Test
	void doesSendDrawWinners() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().then().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score() //
		);
		whenInputWasProcessed();
		assertThat(lastMessageWithTopic("team/score/0").getPayload(), is("5"));
		assertThat(lastMessageWithTopic("team/score/1").getPayload(), is("5"));
		thenWinnerAre(0, 1);
	}

	@Test
	void newGameGetsStarted() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenInputWasProcessed();
		assertThat(lastMessageWithTopic("team/score/0").getPayload(), is("6"));
		thenNoMessageWithTopicIsSent("team/score/1");

		collectedMessages.clear();

		givenInputToProcessIs(ball() //
				.prepareForRightGoal().then().score().then() //
				.prepareForRightGoal().then().score().then() //
				.prepareForRightGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
		);
		whenInputWasProcessed();
		assertThat(lastMessageWithTopic("team/score/0").getPayload(), is("1"));
		assertThat(lastMessageWithTopic("team/score/1").getPayload(), is("3"));
	}

	@Test
	void doesSendGameStartAndScoresWhenGameStarts() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()).at(kickoff()));
		whenInputWasProcessed();
		thenNoMessageIsSent(m -> m.getTopic().startsWith("team/score/"));
	}

	@Test
	void doesSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(5, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	void doesNotSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(4, SECONDS).at(frontOfLeftGoal()) //
				.thenAfter(1, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("game/foul");
	}

	@Test
	void doesSendFoulOnlyOnceUntilFoulIsOver() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow.up(0.49)) //
				.thenAfter(15, SECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	void doesRestartAfterGameEnd() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score() //
				//
				.prepareForLeftGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForRightGoal().score() //
		);
		whenInputWasProcessed();

		Predicate<Message> deprecatedTopic = m -> m.getTopic().startsWith("game/score/");
		assertThat("The deprecated topic no more is sent. Please remove the filtering predicate",
				collectedMessages.stream().filter(deprecatedTopic).anyMatch(deprecatedTopic), is(true));

		assertThat(
				collectedMessages(m -> !m.getTopic().startsWith("ball/")).filter(deprecatedTopic.negate())
						.collect(toList()), //
				is(asList( //
						message("game/start", ""), //
						message("team/scored", 0), //
						message("team/score/0", 1), //
						message("team/scored", 1), //
						message("team/score/1", 1), //
						message("team/scored", 0), //
						message("team/score/0", 2), //
						message("team/scored", 1), //
						message("team/score/1", 2), //
						message("team/scored", 0), //
						message("team/score/0", 3), //
						message("team/scored", 1), //
						message("team/score/1", 3), //
						message("team/scored", 0), //
						message("team/score/0", 4), //
						message("team/scored", 1), //
						message("team/score/1", 4), //
						message("team/scored", 0), //
						message("team/score/0", 5), //
						message("team/scored", 1), //
						message("team/score/1", 5), //
						message("game/gameover", winners(0, 1)), //
						message("game/start", ""), //
						message("team/score/0", 0), //
						message("team/score/1", 0), //
						message("team/scored", 0), //
						message("team/score/0", 1), //
						message("team/scored", 0), //
						message("team/score/0", 2), //
						message("team/scored", 1), //
						message("team/score/1", 1), //
						message("team/scored", 1), //
						message("team/score/1", 2))));
	}

	@Test
	void doesSendIdleOnWhenBallIsOffTableForOneMinuteOrMore() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true");
	}

	@Test
	void doesSendIdleOnWhenBallHasNoMovementForOneMinuteOrMore() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, MINUTES).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true");
	}

	@Test
	void doesSendIdleOffWhenBallWasOffTableAndComesBack() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true", "false");
	}

	@Test
	void doesSendIdleOffWhenBallIsMovedAgainAfterLongerPeriodOfTime() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, MINUTES).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff().down(0.01)) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true", "false");
	}

	@Test
	@Disabled("not working at the moment, has to been fixed")
	void canResetAgameInPlay() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);

		givenInputToProcessIs(ball(MINUTES.toMillis(15)) //
				.prepareForLeftGoal().score().thenAfter(5, SECONDS) //
				.prepareForLeftGoal().score().thenCall(this::setInProgressConsumer, p -> resetGameAndClearMessages()) //
				.prepareForRightGoal().score().thenAfter(5, SECONDS) //
				.prepareForRightGoal().score() //
		);

		whenInputWasProcessed();
		// when resetting the game the game/start message is sent immediately as
		// well when the ball is then detected at the middle line
		thenPayloadsWithTopicAre("game/start", times("", 2));
		thenPayloadsWithTopicAre("game/score/1", "1", "2");
		thenPayloadsWithTopicAre("team/score/1", "1", "2");
		thenPayloadsWithTopicAre("game/score/0", "0", "0");
		thenPayloadsWithTopicAre("team/score/0", "0", "0");
	}

	@Test
	@Disabled("not yet implemented")
	void addTestIfTheDistancesAreCorrect() throws Exception {
		givenInputToProcessIs(ball().at(kickoff()) //
				.then().offTableFor(1, MILLISECONDS) //
				.then().at(kickoff()));
		fail();
	}

	@Test
	void whenDurationIsZeroNoVelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(anyPos()).thenAfter(0, MILLISECONDS).at(anyPos()));
		whenInputWasProcessed();
		thenNoMessageIsSent(m -> m.getTopic().startsWith("ball/velocity/"));
	}

	public void setInProgressConsumer(Consumer<RelativePosition> inProgressConsumer) {
		this.inProgressConsumer = inProgressConsumer;
	}

	private void givenATableOfSize(double width, double height, DistanceUnit distanceUnit) {
		this.sut = new SFTCognition(new Table(width, height, distanceUnit), messageCollector);
	}

	private void givenATableOfAnySize() {
		givenATableOfSize(123, 45, CENTIMETER);
	}

	private void givenInputToProcessIs(MessageBuilder builder) {
		givenInputToProcessIs(builder.build());
	}

	private void givenInputToProcessIs(List<Message> messages) {
		inputMessages.addAll(messages);
	}

	private void givenFrontOfGoalPercentage(int frontOfGoalPercentage) {
		this.goalDetectorConfig.frontOfGoalPercentage(frontOfGoalPercentage);
	}

	private void givenTimeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
		this.goalDetectorConfig.timeWithoutBallTilGoal(duration, timeUnit);
	}

	void whenInputWasProcessed() throws IOException {
		sut = sut.withGoalConfig(goalDetectorConfig);
		ToRelPosition toRelPosition = new ToRelPosition(sut.messages());
		sut.process(inputMessages.stream().map(toRelPosition).filter(Optional::isPresent).map(Optional::get)
				.peek(inProgressConsumer));
		inputMessages.clear();
	}

	private void resetGameAndClearMessages() {
		this.collectedMessages.clear();
		sut.resetGame();
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(String x, String y) {
		assertOneMessageWithPayload(matchingMessages(topic(compile("ball/position/\\d+/abs/x"))), is(x));
		assertOneMessageWithPayload(matchingMessages(topic(compile("ball/position/\\d+/abs/y"))), is(y));
	}

	private void thenGoalForTeamIsPublished(int teamid) {
		assertOneMessageWithPayload(messagesWithTopic("team/scored"), is(String.valueOf(teamid)));
	}

	private void assertOneMessageWithPayload(Stream<Message> messagesWithTopic, Matcher<String> matcher) {
		assertThat(onlyElement(messagesWithTopic).getPayload(), matcher);
	}

	private void thenNoMessageIsSent() {
		thenNoMessageIsSent(a -> true);
	}

	private void thenNoMessageWithTopicIsSent(String topic) {
		thenNoMessageIsSent(m -> m.getTopic().equals(topic));
	}

	private void thenNoMessageIsSent(Predicate<Message> predicate) {
		assertThat(collectedMessages(predicate).collect(toList()), is(emptyList()));
	}

	private Stream<Message> collectedMessages(Predicate<Message> predicate) {
		return matchingMessages(predicate);
	}

	private Message lastMessageWithTopic(String topic) {
		List<Message> messages = messagesWithTopic(topic).collect(toList());
		return messages.isEmpty() ? null : messages.get(messages.size() - 1);
	}

	private void thenPayloadsWithTopicAre(String topic, String... payloads) {
		assertThat(payloads(messagesWithTopic(topic)), is(asList(payloads)));
	}

	private void thenWinnerAre(int... winners) {
		thenPayloadsWithTopicAre("game/gameover", winners(winners));
	}

	private String winners(int... winners) {
		return IntStream.of(winners).mapToObj(String::valueOf).collect(joining(","));
	}

	private List<String> payloads(Stream<Message> messages) {
		return messages.map(Message::getPayload).collect(toList());
	}

	private static String[] times(String value, int times) {
		return range(0, times).mapToObj(i -> value).toArray(String[]::new);
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return matchingMessages(topic(topic));
	}

	private Stream<Message> matchingMessages(Predicate<Message> predicate) {
		return collectedMessages.stream().filter(predicate);
	}

	private Predicate<Message> topic(String topic) {
		return m -> m.getTopic().equals(topic);
	}

	private Predicate<Message> topic(Pattern pattern) {
		return m -> pattern.matcher(m.getTopic()).matches();
	}

	private BallPosBuilder anyPos() {
		return kickoff();
	}

	private BallPosBuilder anyCorner() {
		return upperLeftCorner();
	}

	private static <T> T onlyElement(Stream<T> stream) {
		return stream.reduce(toOnlyElement()).orElseThrow(() -> new NoSuchElementException("empty stream"));
	}

	private static <T> BinaryOperator<T> toOnlyElement() {
		return (t1, t2) -> {
			throw new IllegalStateException("more than one element");
		};
	}

}
