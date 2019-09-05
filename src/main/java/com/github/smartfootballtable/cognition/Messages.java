package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.data.Message.message;
import static com.github.smartfootballtable.cognition.data.Message.retainedMessage;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.IPS;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.KMH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MPH;
import static com.github.smartfootballtable.cognition.data.unit.SpeedUnit.MS;
import static java.lang.Double.isFinite;
import static java.util.Locale.US;
import static java.util.stream.Collectors.joining;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.github.smartfootballtable.cognition.data.Distance;
import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Movement;
import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.data.position.Position;
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

	private static final String BALL_POSITION_REL = "ball/position/rel";
	private static final String BALL_POSITION_ABS = "ball/position/abs";

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
		publish(message(BALL_POSITION_ABS, posPayload(pos)));
	}

	private String posPayload(Position pos) {
		return pos.getX() + "," + pos.getY();
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

	public void teamScored(int teamid) {
		publish(message("team/scored", teamid));
	}

	public void changeScore(int teamid, int newScore) {
		publishTeamScore(teamid, newScore);
		teamsEverScored.add(teamid);
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

	public void gameDraw(int[] teamids) {
		publish(message(GAME_GAMEOVER, IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
	}

	public void idle(boolean b) {
		publish(message(GAME_IDLE, Boolean.toString(b)));
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

	public boolean isRelativePosition(Message message) {
		return message.getTopic().equals(BALL_POSITION_REL);
	}

	public RelativePosition parsePosition(String payload) {
		String[] coords = payload.split("\\,");
		if (coords.length == 2) {
			Double x = toDouble(coords[0]);
			Double y = toDouble(coords[1]);
			if (x != null && y != null) {
				return RelativePosition.create(System.currentTimeMillis(), x, y);
			}
		}
		return null;
	}

	private static Double toDouble(String val) {
		try {
			return Double.valueOf(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}