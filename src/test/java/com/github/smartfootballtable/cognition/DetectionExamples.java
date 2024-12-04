package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.DetectionExamples.GameSituationBuilder.gameSituation;
import static com.github.smartfootballtable.cognition.DetectionExamples.OffsetPos.offTable;
import static com.github.smartfootballtable.cognition.DetectionExamples.OffsetPos.onTable;
import static com.github.smartfootballtable.cognition.Topic.BALL_DISTANCE_CM;
import static com.github.smartfootballtable.cognition.Topic.BALL_OVERALL_DISTANCE_CM;
import static com.github.smartfootballtable.cognition.Topic.BALL_POSITION_ABS;
import static com.github.smartfootballtable.cognition.Topic.BALL_POSITION_REL;
import static com.github.smartfootballtable.cognition.Topic.BALL_VELOCITY_KMH;
import static com.github.smartfootballtable.cognition.Topic.BALL_VELOCITY_MS;
import static com.github.smartfootballtable.cognition.Topic.GAME_FOUL;
import static com.github.smartfootballtable.cognition.Topic.GAME_IDLE;
import static com.github.smartfootballtable.cognition.Topic.TEAM_ID_LEFT;
import static com.github.smartfootballtable.cognition.Topic.TEAM_ID_RIGHT;
import static com.github.smartfootballtable.cognition.Topic.TEAM_SCORED;
import static com.github.smartfootballtable.cognition.Topic.TEAM_SCORE_LEFT;
import static com.github.smartfootballtable.cognition.Topic.TEAM_SCORE_RIGHT;
import static com.github.smartfootballtable.cognition.Topic.isTopic;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.create;
import static com.github.smartfootballtable.cognition.data.position.RelativePosition.noPosition;
import static com.github.smartfootballtable.cognition.data.unit.DistanceUnit.CENTIMETER;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.frequency;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.longs;
import static net.jqwik.api.Combinators.combine;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;

import net.jqwik.api.AfterFailureMode;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.ShrinkingMode;
import net.jqwik.api.Tag;
import net.jqwik.api.Tuple;
import net.jqwik.api.arbitraries.DoubleArbitrary;
import net.jqwik.api.arbitraries.LongArbitrary;
import net.jqwik.api.arbitraries.SizableArbitrary;
import net.jqwik.api.statistics.Statistics;

@Tag("pbt")
class DetectionExamples {

	static abstract class OffsetPos {

		private static class OnTable extends OffsetPos {

			private double x;
			private double y;

			public OnTable(long offset, double x, double y) {
				super(offset);
				this.x = x;
				this.y = y;
			}

			@Override
			public RelativePosition toPos(long timestamp) {
				return create(timestamp, x, y);
			}

			@Override
			public String toString() {
				return "OffsetPos [offset=" + getOffset() + ", x=" + x + ", y=" + y + "]";
			}

		}

		private static class OffTable extends OffsetPos {

			public OffTable(long offset) {
				super(offset);
			}

			@Override
			public RelativePosition toPos(long timestamp) {
				return noPosition(timestamp);
			}

			@Override
			public String toString() {
				return "OffsetPos [offset=" + getOffset();
			}

		}

		public static OffsetPos offTable(long offset) {
			return new OffTable(offset);
		}

		public static OffsetPos onTable(long offset, double x, double y) {
			return new OnTable(offset, x, y);
		}

		private final long offset;

		public OffsetPos(long offset) {
			this.offset = offset;
		}

		public long getOffset() {
			return offset;
		}

		public abstract RelativePosition toPos(long timestamp);

	}

	private static final String METRIC_TABLE = "metricTable";

	@Property
	void ballOnTableNeverWillRaiseTeamScoreOrTeamsScoredEvents(
			@ForAll("positionsOnTable") List<RelativePosition> positions, @ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table) //
				.filter(anyOf(TEAM_SCORE_LEFT, TEAM_SCORE_RIGHT, TEAM_SCORED)) //
				.filter(isInitialScore().negate()) //
				.collect(toList()), is(empty()));
	}

	private Predicate<Message> isInitialScore() {
		return isTopic(TEAM_SCORE_LEFT).and(initialScorePayload()) //
				.or(isTopic(TEAM_SCORE_RIGHT).and(initialScorePayload()));
	}

	private Predicate<Message> initialScorePayload() {
		return m -> m.getPayload().equals("0");
	}

	@Property
	void leftGoalProducesTeamScoredMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_LEFT)));
	}

	@Property
	void leftGoalsProducesTeamScoreMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void rightGoalProducesTeamScoredMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_RIGHT)));
	}

	@Property
	void rightGoalProducesTeamScoreMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORE_RIGHT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterALeftHandGoalTheGoalGetsReverted(
			@ForAll("leftGoalsToReverse") List<RelativePosition> positions, @ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterARightHandGoalTheGoalGetsReverted(
			@ForAll("rightGoalsToReverse") List<RelativePosition> positions, @ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(TEAM_SCORE_RIGHT)).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallDoesNotMoveForMoreThanOneMinuteTheGameGoesToIdleMode(
			@ForAll("idleWhereBallMaybeGone") List<RelativePosition> positions, @ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(GAME_IDLE).and(payloadIs("true"))).count(), is(1L));
	}

	@Property(shrinking = ShrinkingMode.OFF, afterFailure = AfterFailureMode.SAMPLE_ONLY)
	// TODO could produce falls positives: random data could contain fouls
	void noIdleWithoutFoul(@ForAll("idle") List<RelativePosition> positions, @ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		List<Message> messages = process(positions, table).collect(toList());
		Map<String, Long> counts = new HashMap<>();
		counts.put("foul", messages.stream().filter(isTopic(GAME_FOUL)).count());
		counts.put("idleOn", messages.stream().filter(isTopic(GAME_IDLE).and(payloadIs("true"))).count());
		counts.put("idleOff", messages.stream().filter(isTopic(GAME_IDLE).and(payloadIs("false"))).count());
		assertThat("Amount of messages not equal" + counts, new HashSet<>(counts.values()).size() == 1, is(true));
	}

	@Property
	void allRelPositionAreBetween0And1(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(BALL_POSITION_REL)).map(Message::getPayload).collect(toList()),
				everyItem(allOf( //
						hasNumberBetween(0, 0, 1), hasNumberBetween(1, 0, 1))));
	}

	@Property
	boolean ballPositionAbsForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		return process(positions, table).filter(isTopic(BALL_POSITION_ABS)).count() == positions.size();
	}

	@Property(shrinking = ShrinkingMode.OFF, afterFailure = AfterFailureMode.SAMPLE_ONLY)
	void allAbsPositionAreBetween0AndTableSize(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(BALL_POSITION_ABS)).map(Message::getPayload).collect(toList()),
				everyItem(allOf( //
						hasNumberBetween(0, 0, (int) table.getWidth().value(table.getDistanceUnit())),
						hasNumberBetween(1, 0, (int) table.getHeight().value(table.getDistanceUnit())))));
	}

	@Property
	boolean ballVelocityKmhForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		return process(positions, table).filter(isTopic(BALL_VELOCITY_KMH)).count() == positions.size() - 1;
	}

	@Property
	void allBallPositionVelocitiesArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(BALL_VELOCITY_KMH)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	@Property
	boolean ballVelocityMsForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		return process(positions, table).filter(isTopic(BALL_VELOCITY_MS)).count() == positions.size() - 1;
	}

	@Property
	void ballVelocityMsArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(BALL_VELOCITY_MS)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	@Property
	void ballDistanceArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(isTopic(BALL_DISTANCE_CM)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	@Property
	void forEachOverallDistanceThereIsASingleDistance(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		List<Message> processed = process(positions, table).collect(toList());
		assertThat(processed.stream().filter(isTopic(BALL_OVERALL_DISTANCE_CM)).count(),
				is(processed.stream().filter(isTopic(BALL_DISTANCE_CM)).count()));
	}

	@Property
	void overallDistanceIsSumOfSingleDistances(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll(METRIC_TABLE) Table table) {
		statistics(positions);
		List<Message> processed = process(positions, table).collect(toList());
		OfDouble singles = doublePayload(processed, isTopic(BALL_DISTANCE_CM)).iterator();
		OfDouble overalls = doublePayload(processed, isTopic(BALL_OVERALL_DISTANCE_CM)).iterator();

		double sum = 0.0;
		int count = 0;
		while (singles.hasNext() && overalls.hasNext()) {
			count++;
			assertThat(overalls.nextDouble(), is(closeTo(sum += singles.nextDouble(), 0.01 * count)));
		}
		assertThat(singles.hasNext(), is(overalls.hasNext()));
	}

	private void statistics(Collection<?> col) {
		Statistics.collect(col.size() < 50 ? "<50" : col.size() < 100 ? "<100" : ">=100");
	}

	private Matcher<Object> hasNumberBetween(int index, int min, int max) {
		return new TypeSafeMatcher<Object>() {
			@Override
			public void describeTo(Description description) {
				description.appendText(min + ">= " + "(value of index " + index + ") <=" + max);
			}

			@Override
			protected boolean matchesSafely(Object item) {
				String[] coords = String.valueOf(item).split("\\,");
				double value = Double.parseDouble(coords[index]);
				return value >= min && value <= max;
			}

		};
	}

	private Stream<Message> process(List<RelativePosition> positions, Table table) {
		List<Message> messages = new ArrayList<>();
		SFTCognition sftCognition = new SFTCognition(table, messages::add);
		positions.stream().forEach(sftCognition::process);
		return messages.stream();
	}

	private Predicate<Message> anyOf(Topic... topics) {
		return anyOfTopic(EnumSet.allOf(Topic.class).stream().filter(f -> !Arrays.asList(topics).contains(f)));
	}

	private Predicate<Message> anyOfTopic(Stream<Topic> topics) {
		return anyOf(topics.map(Topic::isTopic));
	}

	private Predicate<Message> anyOf(Stream<Predicate<Message>> predicates) {
		return predicates.reduce(Predicate::or).map(Predicate::negate).orElse(m -> true);
	}

	static Predicate<Message> payloadIs(String value) {
		return m -> m.getPayload().equals(value);
	}

	DoubleStream doublePayload(List<Message> messages, Predicate<Message> filter) {
		return doublePayload(messages.stream(), filter);
	}

	DoubleStream doublePayload(Stream<Message> messages, Predicate<Message> filter) {
		return messages.filter(filter).map(Message::getPayload).mapToDouble(Double::parseDouble);
	}

	private Matcher<Double> positive() {
		return greaterThanOrEqualTo(0.0);
	}

	@Provide(METRIC_TABLE)
	Arbitrary<Table> metricTable() {
		return combine( //
				integers().greaterOrEqual(1), //
				integers().greaterOrEqual(1)) //
						.as((width, height) -> new Table(width, height, CENTIMETER));
	}

	@Provide
	Arbitrary<List<RelativePosition>> positionsOnTable() {
		return anyTimestamp(ts -> //
		a(gameSituation(ts).addAnyWhereOnTable()));
	}

	@Provide
	private Arbitrary<List<RelativePosition>> goalSituationsLeft() {
		return anyTimestamp(ts -> //
		a(gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add() //
				.addScoreLeftSequence() //
				.addBallNotInCornerSequence() //
		));
	}

	@Provide
	private Arbitrary<List<RelativePosition>> goalSituationsRight() {
		return anyTimestamp(ts -> //
		a(gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add() //
				.addScoreRightSequence() //
				.addBallNotInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> leftGoalsToReverse() {
		return anyTimestamp(ts -> //
		a(gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add() //
				.addScoreLeftSequence() //
				.addBallInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> rightGoalsToReverse() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add() //
				.addScoreRightSequence() //
				.addBallInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> idle() {
		return anyTimestamp(ts -> a(gameSituation(ts).addIdleSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> idleWhereBallMaybeGone() {
		return anyTimestamp(ts -> a(gameSituation(ts).addIdleSequenceBallMaybeGone()));
	}

	private Arbitrary<List<RelativePosition>> a(GameSituationBuilder builder) {
		return builder.build();
	}

	private Arbitrary<List<RelativePosition>> anyTimestamp(
			Function<AtomicLong, Arbitrary<List<RelativePosition>>> mapper) {
		return longs().between(0, Long.MAX_VALUE / 2).map(AtomicLong::new).flatMap(mapper);
	}

	static class GameSituationBuilder {

		private static final double TABLE_MIN = 0.0;
		private static final double TABLE_MAX = 1.0;
		private static final double CENTER = TABLE_MAX / 2;

		private static final double MIDDLE_LINE_DRIFT = 0.05;
		private static final double FRONT_OF_GOAL_DRIFT = 0.3;
		private static final double CORNER_DRIFT = 0.10;

		private class DurationSequence extends Sequence {

			private final Arbitrary<Long> forDuration;

			public DurationSequence(Arbitrary<OffsetPos> base, Arbitrary<Long> between, TimeUnit timeUnit) {
				super(base);
				this.forDuration = between.map(timeUnit::toMillis);
			}

			@Override
			Arbitrary<List<OffsetPos>> build() {
				return forDuration.flatMap(m -> base.collect(p -> durationReached(p, m)));
			}

			private boolean durationReached(List<? extends OffsetPos> positions, long minDuration) {
				return positions.size() > 1 && duration(positions.stream().limit(positions.size() - 1)) >= minDuration;
			}

			private long duration(Stream<? extends OffsetPos> stream) {
				return stream.mapToLong(OffsetPos::getOffset).sum();
			}

		}

		private class Sequence {

			final Arbitrary<OffsetPos> base;

			public Sequence(Arbitrary<OffsetPos> base) {
				this.base = base;
			}

			public GameSituationBuilder add() {
				return addSequence(build());
			}

			Arbitrary<List<OffsetPos>> build() {
				return base.list().ofMinSize(1);
			}

			private Sequence ofDuration(Arbitrary<Long> between, TimeUnit timeUnit) {
				return new DurationSequence(base, between, timeUnit);
			}

		}

		class Sizeable {

			private Arbitrary<OffsetPos> arbitrary;
			private Integer minSize, maxSize;
			private boolean unique;

			private Sizeable(Arbitrary<OffsetPos> arbitrary) {
				this.arbitrary = arbitrary;
			}

			private Sizeable elementsMin(int minSize) {
				this.minSize = minSize;
				return this;
			}

			private Sizeable between(int minSize, int maxSize) {
				this.minSize = minSize;
				this.maxSize = maxSize;
				return this;
			}

			private GameSituationBuilder addSequence() {
				SizableArbitrary<List<OffsetPos>> list = unique ? arbitrary.list().uniqueElements() : arbitrary.list();
				list = minSize == null ? list : list.ofMinSize(minSize);
				Arbitrary<List<OffsetPos>> seq = maxSize == null ? list : list.ofMaxSize(maxSize);
				return GameSituationBuilder.this.addSequence(seq);
			}

			private Sizeable filter(Predicate<OffsetPos> predicate) {
				arbitrary = arbitrary.filter(predicate);
				return this;
			}

			private Sizeable unique() {
				this.unique = true;
				return this;
			}

		}

		private final AtomicLong timestamp;
		private final List<Arbitrary<List<OffsetPos>>> arbitraries = new ArrayList<>();
		private Arbitrary<Long> samplingFrequency = longs().between(5, 1000);

		GameSituationBuilder(AtomicLong timestamp) {
			this.timestamp = timestamp;
		}

		static GameSituationBuilder gameSituation(AtomicLong timestamp) {
			return new GameSituationBuilder(timestamp);
		}

		GameSituationBuilder withSamplingFrequency(Arbitrary<Long> samplingFrequency, TimeUnit timeUnit) {
			this.samplingFrequency = samplingFrequency.map(timeUnit::toMillis);
			return this;
		}

		Arbitrary<List<RelativePosition>> build() {
			return join(arbitraries);
		}

		Arbitrary<List<RelativePosition>> join(List<Arbitrary<List<OffsetPos>>> arbitraries) {
			return combine(arbitraries).as(p -> //
			p.stream().flatMap(Collection::stream) //
					.map(off -> off.toPos(timestamp.getAndAdd(off.getOffset()))) //
					.collect(toList()));
		}

		static boolean isCorner(OffsetPos pos) {
			RelativePosition normalized = pos.toPos(Long.MAX_VALUE).normalizeX().normalizeY();
			return normalized.getX() >= (TABLE_MAX - CORNER_DRIFT) //
					&& normalized.getY() >= (TABLE_MAX - CORNER_DRIFT);
		}

		Sizeable anywhereOnTableSizeable() {
			return asSizeable(combine(samplingFrequency, wholeTable(), wholeTable()) //
					.as((millis, x, y) //
					-> onTable(millis, x, y)));
		}

		Sizeable asSizeable(Arbitrary<OffsetPos> as) {
			return new Sizeable(as);
		}

		GameSituationBuilder addScoreLeftSequence() {
			return addSequence(prepareLeftGoal()).offTablePositions();
		}

		GameSituationBuilder addScoreRightSequence() {
			return addSequence(prepareRightGoal()).offTablePositions();
		}

		GameSituationBuilder addAnyWhereOnTable() {
//			TODO .addSequence(anyPosition().forDuration(longs().between(1, 20), SECONDS)) //
			return anywhereOnTableSizeable().between(10, 500).addSequence();
		}

		GameSituationBuilder offTablePositions() {
			return addSequence(offTableSequence().ofDuration(longs().between(2, 15), SECONDS).build());
		}

		GameSituationBuilder addBallInCornerSequence() {
			return addSequence(corner()).anywhereOnTableSizeable().addSequence();
		}

		GameSituationBuilder addBallNotInCornerSequence() {
			return anywhereOnTableSizeable().filter(p -> !isCorner(p)).between(0, 50).addSequence();
		}

		GameSituationBuilder addIdleSequence() {
			// TODO should we add at least one pos before?
			// addSequence(noMoveOrNoBallForAtLeast(longs().between(1, 1_000),
			// MILLISECONDS)) //
			return addSequence(noMoveForAtLeast(longs().between(1, 5), MINUTES).build()) //
					.anywhereOnTableSizeable().elementsMin(2).unique().addSequence();
		}

		private Sequence noMoveForAtLeast(LongArbitrary between, TimeUnit timeUnit) {
//			Arbitrary<List<RelativePosition>> arbitrary = between
//					.flatMap(min -> longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10)).map(timeUnit::toMillis)
//							.collect(l -> (l.get(l.size() - 1) - l.get(0)) >= min))
//					.map(millis -> millis.stream().map(m -> create(timestamp.addAndGet(m), 0.12345, 0.54321))
//							.collect(toList()));
			Arbitrary<OffsetPos> base = longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10)) //
					.flatMap(millis -> {
						return combine(wholeTable(), wholeTable()).as((x, y) -> {
							// TODO howto get x and y that will not change?
							return onTable(millis, 0.12345, 0.54321);
						});
					});
			return new Sequence(base).ofDuration(between, timeUnit);
		}

		private Sequence noBallForAtLeast(LongArbitrary between, TimeUnit timeUnit) {
			return new Sequence(
					longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10)).map(millis -> offTable(millis)))
							.ofDuration(between, timeUnit);
		}

		GameSituationBuilder addIdleSequenceBallMaybeGone() {
			return idleSequence(noMoveOrNoBallForAtLeast(longs().between(1, 5), MINUTES));
		}

		GameSituationBuilder idleSequence(Arbitrary<List<OffsetPos>> arbitrary) {
			// add at least two unique elements to ensure idle is over afterwards
			return anywhereOnTableSizeable().addSequence() //
					.addSequence(arbitrary) //
					.anywhereOnTableSizeable().elementsMin(2).unique().addSequence();
		}

		GameSituationBuilder addSequence(Arbitrary<List<OffsetPos>> arbitrary) {
			arbitraries.add(arbitrary);
			return this;
		}

		Sequence aKickoffSequence() {
			return new Sequence(middleLinePositions());
		}

		Arbitrary<OffsetPos> middleLinePositions() {
			return combine(samplingFrequency, middleLine(), wholeTable()) //
					.as((millis, x, y) //
					-> onTable(millis, x, y));
		}

		Sequence offTableSequence() {
			return new Sequence(samplingFrequency.map(millis -> offTable(millis)));
		}

		Arbitrary<List<OffsetPos>> prepareLeftGoal() {
			return frontOfLeftGoal().list().ofMinSize(1);
		}

		Arbitrary<List<OffsetPos>> prepareRightGoal() {
			return frontOfRightGoal().list().ofMinSize(1);
		}

		Arbitrary<OffsetPos> frontOfLeftGoal() {
			return combine(samplingFrequency, frontOfLeftGoalX(), wholeTable()) //
					.as((millis, x, y) //
					-> onTable(millis, x, y));
		}

		Arbitrary<OffsetPos> frontOfRightGoal() {
			return combine(samplingFrequency, frontOfRightGoalX(), wholeTable()) //
					.as((millis, x, y) //
					-> onTable(millis, x, y));
		}

		Arbitrary<List<OffsetPos>> corner() {
			return combine(samplingFrequency, cornerXY(), cornerXY(), bool(), bool()) //
					.as((millis, x, y, swapX, swapY) //
					-> onTable(millis, possiblySwap(x, swapX), possiblySwap(y, swapY))).list().ofMinSize(1);
		}

		Arbitrary<List<OffsetPos>> noMoveOrNoBallForAtLeast(LongArbitrary between, TimeUnit timeUnit) {
			return frequency( //
					Tuple.of(10, noBallForAtLeast(between, timeUnit).build()), //
					Tuple.of(90, noMoveForAtLeast(between, timeUnit).build()) //
			).flatMap(identity());
		}

		static double possiblySwap(double value, boolean swap) {
			return swap ? swap(value) : value;
		}

		static double swap(double value) {
			return TABLE_MAX - value;
		}

		static Arbitrary<Double> cornerXY() {
			return doubles().between(TABLE_MAX - CORNER_DRIFT, TABLE_MAX);
		}

		static DoubleArbitrary wholeTable() {
			return doubles().between(TABLE_MIN, TABLE_MAX);
		}

		static DoubleArbitrary middleLine() {
			return doubles().between(CENTER - MIDDLE_LINE_DRIFT, CENTER + MIDDLE_LINE_DRIFT);
		}

		static Arbitrary<Double> frontOfLeftGoalX() {
			return frontOfGoalX();
		}

		static Arbitrary<Double> frontOfRightGoalX() {
			return frontOfGoalX().map(GameSituationBuilder::swap);
		}

		static Arbitrary<Double> frontOfGoalX() {
			return doubles().between(0, FRONT_OF_GOAL_DRIFT);
		}

		static Arbitrary<Boolean> bool() {
			return Arbitraries.of(true, false);
		}

	}

}
