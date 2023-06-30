package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.MessageMother.GAME_FOUL;
import static com.github.smartfootballtable.cognition.MessageMother.GAME_GAMEOVER;
import static com.github.smartfootballtable.cognition.MessageMother.GAME_IDLE;
import static com.github.smartfootballtable.cognition.MessageMother.GAME_START;
import static com.github.smartfootballtable.cognition.MessageMother.TOPIC_BALL_POSITION_ABS;
import static com.github.smartfootballtable.cognition.MessageMother.relativePosition;
import static com.github.smartfootballtable.cognition.MessageMother.scoreOfTeam;
import static com.github.smartfootballtable.cognition.MessageMother.teamScored;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.ball;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.frontOfLeftGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.frontOfRightGoal;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.kickoff;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.lowerRightCorner;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.offTable;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.pos;
import static com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder.upperLeftCorner;
import static com.github.smartfootballtable.cognition.Topic.TEAM_SCORE;
import static com.github.smartfootballtable.cognition.Topic.isTopic;
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
import static java.util.stream.Stream.iterate;
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
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.BallPosBuilder;
import com.github.smartfootballtable.cognition.SFTCognitionTest.PositionMessageBuilder.TimestampedMessage;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;
import com.github.smartfootballtable.cognition.detector.GoalDetector;

class SFTCognitionTest {

	public static class PositionMessageBuilder {

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

		private PositionMessageBuilder(long timestamp) {
			this.timestamp = timestamp;
		}

		public static PositionMessageBuilder ball() {
			return ball(anyTimestamp());
		}

		public static PositionMessageBuilder ball(long timestamp) {
			return messagesStartingAt(timestamp);
		}

		private static PositionMessageBuilder messagesStartingAt(long timestamp) {
			return new PositionMessageBuilder(timestamp);
		}

		private static long anyTimestamp() {
			return 1234;
		}

		private PositionMessageBuilder then() {
			return this;
		}

		private PositionMessageBuilder then(BallPosBuilder ballPosBuilder) {
			return at(ballPosBuilder);
		}

		private PositionMessageBuilder at(BallPosBuilder ballPosBuilder) {
			messages.add(makeMessage(ballPosBuilder.x, ballPosBuilder.y));
			return this;
		}

		private PositionMessageBuilder thenAfterMillis(long duration) {
			return thenAfter(duration, MILLISECONDS);
		}

		private PositionMessageBuilder thenAfter(long duration, TimeUnit timeUnit) {
			timestamp += timeUnit.toMillis(duration);
			return this;
		}

		private PositionMessageBuilder invalidData() {
			messages.add(makeMessage("A", "B"));
			return this;
		}

		private TimestampedMessage makeMessage(Object x, Object y) {
			return new TimestampedMessage(timestamp, relativePosition(timestamp, x, y));
		}

		private PositionMessageBuilder prepareForLeftGoal() {
			return prepareForGoal().at(frontOfLeftGoal());
		}

		private PositionMessageBuilder prepareForRightGoal() {
			return prepareForGoal().at(frontOfRightGoal());
		}

		private PositionMessageBuilder prepareForGoal() {
			return at(kickoff()).thenAfter(100, MILLISECONDS);
		}

		private PositionMessageBuilder score() {
			return offTableFor(2, SECONDS);
		}

		private PositionMessageBuilder offTableFor(int duration, TimeUnit timeUnit) {
			return at(offTable()).thenAfter(duration, timeUnit).then(offTable());
		}

		private PositionMessageBuilder thenCall(Consumer<Consumer<RelativePosition>> setter,
				Consumer<RelativePosition> c) {
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

		private PositionMessageBuilder times(int times, UnaryOperator<PositionMessageBuilder> op) {
			return iterate(this, op::apply).limit(times + 1).reduce((__, b) -> b).orElse(this);
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

	@Test
	void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		whenProcessed(ball().at(kickoff()));
		thenTheAbsolutePositionOnTheTableIsPublished("50.00", "40.00");
	}

	@Test
	void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		whenProcessed(ball().at(pos(0.9, 0.1)));
		thenTheAbsolutePositionOnTheTableIsPublished("90.00", "8.00");
	}

	@Test
	void malformedMessageIsRead() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().invalidData());
		thenNoMessageIsSent();
	}

	@Test
	void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(offTable()));
		thenNoMessageIsSent();
	}

	@Test
	void whenTwoPositionsAreRead_VelocityGetsPublished_MetricTable() throws IOException {
		givenATableOfSize(100, 80, CENTIMETER);
		whenProcessed(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		assertOneMessageWithPayload(withTopic("ball/distance/cm"), is("128.06"));
		assertOneMessageWithPayload(withTopic("ball/velocity/ms"), is("1.28"));
		assertOneMessageWithPayload(withTopic("ball/velocity/kmh"), is("4.61"));
	}

	@Test
	void whenTwoPositionsAreRead_VelocityGetsPublished_ImperialTable() throws IOException {
		givenATableOfSize(100, 80, INCHES);
		whenProcessed(ball().at(upperLeftCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		assertOneMessageWithPayload(withTopic("ball/distance/inch"), is("128.06"));
		assertOneMessageWithPayload(withTopic("ball/velocity/ips"), is("128.06"));
		assertOneMessageWithPayload(withTopic("ball/velocity/mph"), is("7.28"));
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
		whenProcessed(ball() //
				.at(base.left(0.1)) //
				.at(base.up(0.1)) //
				.at(base.right(0.1)) //
				.at(base.down(0.1)) //
				.at(base.left(0.1)));
	}

	@Test
	void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForLeftGoal().then().score());
		thenGoalForTeamIsPublished(0);
		thenPayloadsWithTopicAre(scoreOfTeam(0), "1");
	}

	@Test
	void canDetectGoalOnRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForRightGoal().then().score());
		thenGoalForTeamIsPublished(1);
		thenPayloadsWithTopicAre(scoreOfTeam(1), "1");
	}

	@Test
	void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForRightGoal().then().at(frontOfRightGoal().left(0.01)).score());
		thenNoMessageWithTopicIsSent(teamScored());
	}

	@Test
	void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForLeftGoal().then().at(frontOfLeftGoal().right(0.01)).then().score());
		thenNoMessageWithTopicIsSent(teamScored());
	}

	@Test
	void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().times(3, b -> b.prepareForLeftGoal().then().score()));
		thenPayloadsWithTopicAre(teamScored(), times("0", 3));
		thenPayloadsWithTopicAre(scoreOfTeam(0), "1", "2", "3");
	}

	@Test
	void noGoalsIfBallWasNotDetectedAtMiddleLine() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().at(frontOfLeftGoal()).then().score());
		thenNoMessageWithTopicIsSent(teamScored());
	}

	@Test
	void noGoalsIfThereAreNotTwoSecondsWithoutPositions() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		long timeout = SECONDS.toMillis(2);
		givenTimeWithoutBallTilGoal(timeout, MILLISECONDS);
		long oneMsBeforeTimeout = timeout - 1;
		whenProcessed(ball() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsBeforeTimeout).then(offTable()).then() //
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsBeforeTimeout).then(offTable()).then() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsBeforeTimeout).then(kickoff()).then() //
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsBeforeTimeout).then(kickoff()) //
		);
		thenNoMessageWithTopicIsSent(teamScored());
	}

	@Test
	void withoutWaitTimeTheGoalDirectlyCounts() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenTimeWithoutBallTilGoal(0, MILLISECONDS);
		whenProcessed(ball().prepareForLeftGoal().then().score());
		thenGoalForTeamIsPublished(0);
	}

	@Test
	void canRevokeGoals() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForLeftGoal().then().score().then(anyCorner()));
		thenPayloadsWithTopicAre(scoreOfTeam(0), "1", "0");
		thenPayloadsWithTopicAre(teamScored(), "0");
	}

	@Test
	void alsoRevokesIfBallIsDetectedSomewhereElseAfterGoalAndThenInTheCorner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForLeftGoal().then().score().then(pos(0.0, 0.5)).then(anyCorner()));
		thenPayloadsWithTopicAre(scoreOfTeam(0), "1", "0");
		thenPayloadsWithTopicAre(teamScored(), "0");
	}

	@Test
	void afterRevokingAnotherGoalIsShotOnOppositeSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().prepareForLeftGoal().then().score().then(anyCorner()).then().prepareForRightGoal().then()
				.score());
		thenPayloadsWithTopicAre(scoreOfTeam(1), "1");
	}

	@Test
	void doesSendWinner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().times(6, b -> b.prepareForLeftGoal().then().score()));
		assertThat(lastMessageWithTopic(scoreOfTeam(0)).getPayload(), is("6"));
		thenWinnerAre(0);
	}

	@Test
	void doesSendDrawWinners() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().times(5,
				b -> b.prepareForLeftGoal().then().score().then().prepareForRightGoal().then().score()));
		assertThat(lastMessageWithTopic(scoreOfTeam(0)).getPayload(), is("5"));
		assertThat(lastMessageWithTopic(scoreOfTeam(1)).getPayload(), is("5"));
		thenWinnerAre(0, 1);
	}

	@Test
	void newGameGetsStarted() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball().times(6, b -> b.prepareForLeftGoal().then().score()));
		assertThat(lastMessageWithTopic(scoreOfTeam(0)).getPayload(), is("6"));
		thenNoMessageWithTopicIsSent(scoreOfTeam(1));

		collectedMessages.clear();

		whenProcessed(ball() //
				.times(3, b -> b.prepareForRightGoal().then().score()) //
				.prepareForLeftGoal().then().score() //
		);
		assertThat(lastMessageWithTopic(scoreOfTeam(0)).getPayload(), is("1"));
		assertThat(lastMessageWithTopic(scoreOfTeam(1)).getPayload(), is("3"));
	}

	@Test
	void doesSendGameStartAndScoresWhenGameStarts() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(kickoff()).at(kickoff()));
		thenNoMessageIsSent(isTopic(TEAM_SCORE));
	}

	@Test
	void doesSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		whenProcessed(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(5, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		assertOneMessageWithPayload(withTopic(GAME_FOUL), is(""));
	}

	@Test
	void doesNotSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		whenProcessed(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(4, SECONDS).at(frontOfLeftGoal()) //
				.thenAfter(1, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		thenNoMessageWithTopicIsSent(GAME_FOUL);
	}

	@Test
	void doesNotSendFoulWhenBallIsOffTable() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(anyPos()) //
				.offTableFor(15, SECONDS).offTableFor(1, SECONDS));
		thenNoMessageWithTopicIsSent(GAME_FOUL);
	}

	@Test
	void doesSendFoulOnlyOnceUntilFoulIsOver() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		whenProcessed(ball().at(middlefieldRow.up(0.49)) //
				.thenAfter(15, SECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
		);
		assertOneMessageWithPayload(withTopic(GAME_FOUL), is(""));
	}

	@Test
	void doesRestartAfterGameEnd() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		whenProcessed(ball() //
				.times(5, b -> b.prepareForLeftGoal().score().then().prepareForRightGoal().score())
				// game get's restarted
				.times(2, b -> b.prepareForLeftGoal().score()) //
				.times(2, b -> b.prepareForRightGoal().score()) //
		);

		assertThat(collectedMessages(m -> !m.getTopic().startsWith("ball/")).collect(toList()), //
				is(asList( //
						message(GAME_START, ""), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 1), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 1), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 2), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 2), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 3), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 3), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 4), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 4), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 5), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 5), //
						message(GAME_GAMEOVER, winners(0, 1)), //
						message(GAME_START, ""), //
						message(scoreOfTeam(0), 0), //
						message(scoreOfTeam(1), 0), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 1), //
						message(teamScored(), 0), //
						message(scoreOfTeam(0), 2), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 1), //
						message(teamScored(), 1), //
						message(scoreOfTeam(1), 2))));
	}

	@Test
	void doesSendIdleOnWhenBallIsOffTableForOneMinuteOrMore() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
		);
		thenPayloadsWithTopicAre(GAME_IDLE, "true");
	}

	@Test
	void doesSendIdleOnWhenBallHasNoMovementForOneMinuteOrMore() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, MINUTES).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		thenPayloadsWithTopicAre(GAME_IDLE, "true");
	}

	@Test
	void doesSendIdleOffWhenBallWasOffTableAndComesBack() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		thenPayloadsWithTopicAre(GAME_IDLE, "true", "false");
	}

	@Test
	void doesSendIdleOffWhenBallIsMovedAgainAfterLongerPeriodOfTime() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, MINUTES).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff().down(0.01)) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		thenPayloadsWithTopicAre(GAME_IDLE, "true", "false");
	}

	@Test
	void canResetAgameInPlay() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);

		whenProcessed(ball(MINUTES.toMillis(15)) //
				.prepareForLeftGoal().score().thenAfter(5, SECONDS) //
				.prepareForLeftGoal().score().thenCall(this::setInProgressConsumer, p -> resetGameAndClearMessages()) //
				.prepareForRightGoal().score().thenAfter(5, SECONDS) //
				.prepareForRightGoal().score() //
		);

		// when resetting the game the game/start message is sent immediately as
		// well when the ball is then detected at the middle line
		thenPayloadsWithTopicAre(GAME_START, times("", 2));
		thenPayloadsWithTopicAre(scoreOfTeam(1), "1", "2");
		thenPayloadsWithTopicAre(scoreOfTeam(0), "0", "0");
	}

	@Test
	void whenDurationIsZeroNoVelocityGetsPublished() throws IOException {
		givenATableOfAnySize();
		whenProcessed(ball().at(anyPos()).thenAfter(0, MILLISECONDS).at(anyPos()));
		thenNoMessageIsSent(m -> m.getTopic().startsWith("ball/velocity/"));
	}

	@Test
	void whenBallWasOffTableThereIsNoMovementBetweenPositionBeforeAndAfter() throws Exception {
		givenATableOfAnySize();
		whenProcessed(ball().at(upperLeftCorner()) //
				.then().offTableFor(1, MILLISECONDS) //
				.then().at(lowerRightCorner()));
		thenNoMessageIsSent(m -> m.getTopic().startsWith("ball/distance/"));
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

	private void whenProcessed(PositionMessageBuilder builder) throws IOException {
		builder.build().stream().map(this::toPosition).peek(inProgressConsumer)
				.forEach(sut.withGoalConfig(goalDetectorConfig)::process);
	}

	private void givenFrontOfGoalPercentage(int frontOfGoalPercentage) {
		this.goalDetectorConfig.frontOfGoalPercentage(frontOfGoalPercentage);
	}

	private void givenTimeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
		this.goalDetectorConfig.timeWithoutBallTilGoal(duration, timeUnit);
	}

	private RelativePosition toPosition(TimestampedMessage timestampedMessage) {
		RelativePosition delegate = sut.messages().parsePosition(timestampedMessage.message.getPayload());
		return delegate == null ? null : create(timestampedMessage.timestamp, delegate.getX(), delegate.getY());
	}

	private void resetGameAndClearMessages() {
		this.collectedMessages.clear();
		sut.resetGame();
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(String x, String y) {
		assertOneMessageWithPayload(withTopic(TOPIC_BALL_POSITION_ABS), is(makePayload(x, y)));
	}

	private void thenGoalForTeamIsPublished(int teamid) {
		assertOneMessageWithPayload(withTopic(teamScored()), is(String.valueOf(teamid)));
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
		return withTopic(topic).reduce((first, second) -> second).orElse(null);
	}

	private void thenPayloadsWithTopicAre(String topic, String... payloads) {
		assertThat(payloads(withTopic(topic)), is(asList(payloads)));
	}

	private void thenWinnerAre(int... winners) {
		thenPayloadsWithTopicAre(GAME_GAMEOVER, winners(winners));
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

	private String makePayload(String x, String y) {
		return x + "," + y;
	}

	private Stream<Message> withTopic(String topic) {
		return collectedMessages.stream().filter(topic(topic));
	}

	private Predicate<Message> topic(String topic) {
		return m -> m.isTopic(topic);
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
