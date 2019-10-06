package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.detector.FoulDetector.onFoul;
import static com.github.smartfootballtable.cognition.detector.GameStartDetector.onGameStart;
import static com.github.smartfootballtable.cognition.detector.IdleDetector.onIdle;
import static com.github.smartfootballtable.cognition.detector.MovementDetector.onMovement;
import static com.github.smartfootballtable.cognition.detector.PositionDetector.onPositionChange;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.smartfootballtable.cognition.data.Message;
import com.github.smartfootballtable.cognition.data.Table;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.detector.GoalDetector;

public class SFTCognition {

	private final Table table;
	private Game game;
	private Messages messages;
	private volatile boolean reset;

	public SFTCognition(Table table, Consumer<Message> consumer) {
		this.table = table;
		this.messages = new Messages(consumer, table.getDistanceUnit());
		this.game = Game.newGame( //
				onGameStart(messages::gameStart), //
				onPositionChange(messages::pos), //
				onMovement(table.getDistanceUnit(), messages::movement), //
				onFoul(messages::foul), //
				onIdle(messages::idle) //
		).addScoreTracker(scoreTracker(messages, consumer));
	}

	public Messages messages() {
		return messages;
	}

	public SFTCognition receiver(MessageProvider provider) {
		provider.addConsumer(m -> {
			if (messages.isReset(m)) {
				resetGame();
			}
		});
		return this;
	}

	private ScoreTracker.Listener scoreTracker(Messages messages, Consumer<Message> consumer) {
		return new ScoreTracker.Listener() {

			@Override
			public void scoreChanged(int teamid, int oldScore, int newScore) {
				messages.scoreChanged(teamid, oldScore, newScore);
			}

			@Override
			public void won(int teamid) {
				messages.gameWon(teamid);
			}

			@Override
			public void draw(int[] teamids) {
				messages.gameDraw(teamids);
			}

		};
	}

	public SFTCognition withGoalConfig(GoalDetector.Config goalConfig) {
		this.game = game.withGoalConfig(goalConfig);
		return this;
	}

	public void process(Stream<RelativePosition> positions) {
		positions.forEach(this::process);
	}

	public void process(Supplier<RelativePosition> positions) {
		while (true) {
			process(positions.get());
		}
	}

	private void process(RelativePosition pos) {
		if (pos == null) {
			// TOOO log invalid line
		} else {
			if (reset) {
				game = game.reset();
				messages.gameStart();
				reset = false;
			}
			game = game.update(table.toAbsolute(pos));
		}
	}

	public void resetGame() {
		this.reset = true;
	}

}
