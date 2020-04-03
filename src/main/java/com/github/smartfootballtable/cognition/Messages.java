package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.Message.retainedMessage;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.IPS;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.KMH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MS;
import static java.lang.Double.isFinite;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.US;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.github.smartfootballtable.cognition.data.Distance;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Movement;
import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;
import com.github.smartfootballtable.cognition.data.unit.SpeedUnit;

public class Messages {

	private final class VelocityPublisher {

		private final SpeedUnit[] units;

		private VelocityPublisher(SpeedUnit... units) {
			this.units = units.clone();
		}

		public void publishMovement(Movement movement) {
			for (SpeedUnit unit : units) {
				double velocity = movement.velocity(unit);
				if (isFinite(velocity)) {
					Messages.this.publish(message("ball/velocity/" + unit.symbol(), formatDouble(velocity)));
				}
			}
		}

	}

	private static final String GAME_START = "game/start";
	private static final String GAME_GAMEOVER = "game/gameover";
	private static final String GAME_IDLE = "game/idle";
	private static final String GAME_RESET = "game/reset";

	private static final Pattern BALL_POSITION_REL_X = compile("ball/position/(\\d+)/rel/x");
	private static final Pattern BALL_POSITION_REL_Y = compile("ball/position/(\\d+)/rel/y");
	private static final String BALL_POSITION_ABS_X = "ball/position/%d/abs/x";
	private static final String BALL_POSITION_ABS_Y = "ball/position/%d/abs/y";

	private final Consumer<Message> consumer;
	private final DistanceUnit distanceUnit;
	private final VelocityPublisher velocityPublisher;

	private final Set<Integer> teamsEverScored = new HashSet<>();
	private final Set<String> retainedTopics = new HashSet<>();

	private final NumberFormat formatter = createDoubleFormatter();

	private static NumberFormat createDoubleFormatter() {
		NumberFormat formatter = NumberFormat.getInstance(US);
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setGroupingUsed(false);
		formatter.setRoundingMode(HALF_UP);
		return formatter;
	}

	public Messages(Consumer<Message> consumer, DistanceUnit distanceUnit) {
		this.consumer = consumer;
		this.distanceUnit = distanceUnit;
		this.velocityPublisher = distanceUnit.isMetric() //
				? new VelocityPublisher(MS, KMH) //
				: new VelocityPublisher(IPS, MPH);
	}

	public void gameStart() {
		publish(message(GAME_START, ""));
		resetScores();
	}

	private void resetScores() {
		teamsEverScored.forEach(teamId -> publishTeamScore(teamId, 0));
	}

	public void pos(AbsolutePosition pos) {
		publish(message(String.format(BALL_POSITION_ABS_X, pos.getTimestamp()), formatDouble(pos.getX())));
		publish(message(String.format(BALL_POSITION_ABS_Y, pos.getTimestamp()), formatDouble(pos.getY())));
	}

	public void movement(Movement movement, Distance overallDistance) {
		publish(message("ball/distance/" + distanceUnit.symbol(), formatDouble(movement.distance(distanceUnit))));
		velocityPublisher.publishMovement(movement);
		publish(message("ball/distance/overall/" + distanceUnit.symbol(),
				formatDouble(overallDistance.value(distanceUnit))));
	}

	private String formatDouble(double number) {
		return formatter.format(number);
	}

	private void teamScored(int teamid) {
		publish(message("team/scored", teamid));
	}

	private void publishTeamScore(int teamid, int score) {
		gameScore(teamid, score);
		publish(retainedMessage("team/score/" + teamid, score));
	}

	/**
	 * Will be removed in future versions
	 * 
	 * @deprecated replaces by team/score/$id
	 */
	@Deprecated
	private void gameScore(int teamid, int score) {
		publish(message("game/score/" + teamid, score));
	}

	public void foul() {
		publish(message("game/foul", ""));
	}

	public void gameWon(int teamid) {
		publish(message(GAME_GAMEOVER, teamid));
	}

	public void gameDraw(int... teamids) {
		publish(message(GAME_GAMEOVER, join(",", teamids)));
	}

	public void idle(boolean isIdle) {
		publish(message(GAME_IDLE, Boolean.toString(isIdle)));
	}

	private static String join(String separator, int... teamids) {
		return IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(separator));
	}

	private void publish(Message message) {
		consumer.accept(message);
		boolean retained = message.isRetained();
		if (retained) {
			retainedTopics.add(message.getTopic());
		}
	}

	public void clearRetained() {
		retainedTopics.forEach(t -> publish(retainedMessage(t, "")));
	}

	public boolean isReset(Message message) {
		return message.getTopic().equals(GAME_RESET);
	}

	public boolean isRelativePositionX(Message message) {
		return matcher(message, BALL_POSITION_REL_X).matches();
	}

	public boolean isRelativePositionY(Message message) {
		return matcher(message, BALL_POSITION_REL_Y).matches();
	}

	private Matcher matcher(Message message, Pattern pattern) {
		return pattern.matcher(message.getTopic());
	}

	public Optional<RelativePosition> parsePosition(Message xPosition, Message yPosition) {
		if (xPosition == null || yPosition == null) {
			return Optional.empty();
		}
		Matcher matcherX = matcher(xPosition, BALL_POSITION_REL_X);
		Matcher matcherY = matcher(yPosition, BALL_POSITION_REL_Y);
		if (matcherX.find() && matcherY.find()) {
			Long timestampX = toLong(matcherX.group(1));
			Long timestampY = toLong(matcherY.group(1));
			Double x = toDouble(xPosition.getPayload());
			Double y = toDouble(yPosition.getPayload());
			if (x != null && y != null && timestampX != null && timestampY != null
					&& Objects.equals(timestampX, timestampY)) {
				return Optional.of(RelativePosition.create(timestampX, x, y));
			}
		}
		System.out.println("--- " + xPosition + " " + yPosition);
		return Optional.empty();
	}

	public void scoreChanged(int teamid, int oldScore, int newScore) {
		if (newScore > oldScore) {
			teamScored(teamid);
		}
		publishTeamScore(teamid, newScore);
		teamsEverScored.add(teamid);
	}

	private static Double toDouble(String val) {
		try {
			return Double.valueOf(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long toLong(String val) {
		try {
			return Long.valueOf(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}