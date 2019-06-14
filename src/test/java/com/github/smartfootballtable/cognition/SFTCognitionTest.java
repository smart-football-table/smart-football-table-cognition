package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.ball;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.frontOfLeftGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.frontOfRightGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.kickoff;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.lowerRightCorner;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.offTable;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.pos;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder.upperLeftCorner;
import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.INCHES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.BallPosBuilder;
import com.github.smartfootballtable.cognition.SFTCognitionTest.StdInBuilder.TimestampedMessage;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;
import com.github.smartfootballtable.cognition.detector.GoalDetector;

class SFTCognitionTest {

	public static class StdInBuilder {

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

		class TimestampedMessage {

			private final long timestamp;
			private final Message message;

			public TimestampedMessage(long timestamp, Message message) {
				this.timestamp = timestamp;
				this.message = message;
			}

		}

		private long timestamp;
		private final List<TimestampedMessage> messages = new ArrayList<>();

		private StdInBuilder(long timestamp) {
			this.timestamp = timestamp;
		}

		public static StdInBuilder ball() {
			return ball(anyTimestamp());
		}

		public static StdInBuilder ball(long timestamp) {
			return messagesStartingAt(timestamp);
		}

		private static StdInBuilder messagesStartingAt(long timestamp) {
			return new StdInBuilder(timestamp);
		}

		private static long anyTimestamp() {
			return 1234;
		}

		private StdInBuilder then() {
			return this;
		}

		private StdInBuilder then(BallPosBuilder ballPosBuilder) {
			return at(ballPosBuilder);
		}

		private StdInBuilder at(BallPosBuilder ballPosBuilder) {
			messages.add(makeMessage(ballPosBuilder.x, ballPosBuilder.y));
			return this;
		}

		private StdInBuilder thenAfterMillis(long duration) {
			return thenAfter(duration, MILLISECONDS);
		}

		private StdInBuilder thenAfter(long duration, TimeUnit timeUnit) {
			timestamp += timeUnit.toMillis(duration);
			return this;
		}

		private StdInBuilder invalidData() {
			messages.add(makeMessage("A", "B"));
			return this;
		}

		private TimestampedMessage makeMessage(Object x, Object y) {
			return new TimestampedMessage(timestamp, message("ball/position/rel", x + "," + y));
		}

		private StdInBuilder prepareForLeftGoal() {
			return prepateForGoal().at(frontOfLeftGoal());
		}

		private StdInBuilder prepareForRightGoal() {
			return prepateForGoal().at(frontOfRightGoal());
		}

		private StdInBuilder prepateForGoal() {
			return at(kickoff()).thenAfter(100, MILLISECONDS);
		}

		private StdInBuilder score() {
			return offTableFor(2, SECONDS);
		}

		private StdInBuilder offTableFor(int duration, TimeUnit timeUnit) {
			return at(offTable()).thenAfter(duration, timeUnit).then(offTable());
		}

		public StdInBuilder thenCall(Consumer<Consumer<RelativePosition>> setter, Consumer<RelativePosition> c) {
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

		private List<TimestampedMessage> build() {
			return new ArrayList<>(messages);
		}

	}

	private final List<Message> collectedMessages = new ArrayList<>();
	private final Consumer<Message> messageCollector = collectedMessages::add;
	private final GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();
	private Consumer<RelativePosition> inProgressConsumer = p -> {
	};

	private SFTCognition sut;
	private final List<TimestampedMessage> inputMessages = new ArrayList<>();

	@Test
	void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(kickoff()));
		whenInputWasProcessed();
		thenTheAbsolutePositionOnTheTableIsPublished(100 / 2, 80 / 2);
	}

	@Test
	void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(pos(0.0, 1.0)));
		whenInputWasProcessed();
		thenTheAbsolutePositionOnTheTableIsPublished(0, 80);
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
	void whenTwoPositionsAreRead_VelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		givenInputToProcessIs(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("ball/distance/cm"), is("128.06248474865697"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/mps"), is("1.2806248474865698"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/kmh"), is("4.610249450951652"));
	}

	@Test
	void whenTwoPositionsAreRead_VelocityGetsPublished_tableInInch() throws IOException {
		givenATableOfSize(100, 80, INCHES);
		givenInputToProcessIs(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("ball/distance/inch"), is("128.06248474865697"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/ipm"), is("7683.749084919418"));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/mph"), is("7.2762775425373265"));
	}

	@Test
	void overallDistance() throws IOException {
		makeDiamondMoveOnTableIn(CENTIMETER);
		thenPayloadsWithTopicAre("ball/distance/overall/cm", "8.0", "18.0", "26.0", "36.0");
	}

	@Test
	void overallDistanceIsSentInInchWhenTableIsInch() throws IOException {
		makeDiamondMoveOnTableIn(INCHES);
		thenPayloadsWithTopicAre("ball/distance/overall/inch", "8.0", "18.0", "26.0", "36.0");
	}

	private void makeDiamondMoveOnTableIn(DistanceUnit distanceUnit) throws IOException {
		givenATableOfSize(100, 80, distanceUnit);
		BallPosBuilder base = kickoff();
		givenInputToProcessIs(
				ball().at(base.left(0.1)).at(base.up(0.1)).at(base.right(0.1)).at(base.down(0.1)).at(base.left(0.1)));
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
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then() //
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(kickoff()).then() //
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
	}

	@Test
	void alsoRevertsIfBallIsDetectedSomewhereElseAfterGoalAndThenInTheCorner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score().then(pos(0.0, 0.5)).then(anyCorner()));
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/score/0", "1", "0");
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
		// when resetting the game the game/start message is sent immediately as well
		// when the ball is then detected at the middle line
		thenPayloadsWithTopicAre("game/start", times("", 2));
		thenPayloadsWithTopicAre("team/score/1", "1", "2");
		thenPayloadsWithTopicAre("team/score/0", "0", "0");
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

	private void givenInputToProcessIs(StdInBuilder builder) {
		givenInputToProcessIs(builder.build());
	}

	private void givenInputToProcessIs(List<TimestampedMessage> messages) {
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
		sut.process(inputMessages.stream().map(this::toPosition).peek(inProgressConsumer));
		inputMessages.clear();
	}

	private RelativePosition toPosition(TimestampedMessage timestampedMessage) {
		RelativePosition delegate = sut.messages().parsePosition(timestampedMessage.message.getPayload());
		return delegate == null ? null : create(timestampedMessage.timestamp, delegate.getX(), delegate.getY());
	}

	private void resetGameAndClearMessages() {
		this.collectedMessages.clear();
		sut.resetGame();
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertOneMessageWithPayload(messagesWithTopic("ball/position/abs"), is(makePayload(x, y)));
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
		return collectedMessages.stream().filter(predicate);
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

	private String makePayload(double x, double y) {
		return x + "," + y;
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return collectedMessages.stream().filter(topic(topic));
	}

	private Predicate<Message> topic(String topic) {
		return m -> m.getTopic().equals(topic);
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
