package com.github.smartfootballtable.cognition;

import static com.github.smartfootballtable.cognition.detector.GoalDetector.onGoal;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.detector.Detector;
import com.github.smartfootballtable.cognition.detector.GoalDetector;
import com.github.smartfootballtable.cognition.detector.GoalDetector.Config;

public abstract class Game {

	protected final List<Detector> detectors;
	protected final List<ScoreTracker.Listener> scoreTrackerListeners = new ArrayList<>();
	protected GoalDetector.Config goalDetectorConfig;

	private static class DefaultScoreTracker implements ScoreTracker {

		private enum ScoreChange {
			SCORED(+1), REVERTED(-1);
			private final int changeBy;

			private ScoreChange(int changeBy) {
				this.changeBy = changeBy;
			}
		}

		private static final int MAX_BALLS = 10;

		private final ScoreTracker.Listener listener;

		private DefaultScoreTracker(ScoreTracker.Listener listener) {
			this.listener = listener;
		}

		private final Map<Integer, Integer> scores = new HashMap<>();

		@Override
		public int teamScored(int teamid) {
			return changeScore(teamid, ScoreChange.SCORED);
		}

		@Override
		public int revertGoal(int teamid) {
			return changeScore(teamid, ScoreChange.REVERTED);
		}

		private int changeScore(int teamid, ScoreChange change) {
			int oldScore = scoreOf(teamid);
			Integer newScore = oldScore + change.changeBy;
			scores.put(teamid, newScore);
			listener.scoreChanged(teamid, oldScore, newScore);
			checkState(teamid, newScore);
			return newScore;
		}

		private void checkState(int teamid, Integer newScore) {
			if (isWinningGoal(newScore)) {
				listener.won(teamid);
			} else if (isDraw()) {
				listener.draw(teamids());
			}
		}

		private int scoreOf(int teamid) {
			return scores.getOrDefault(teamid, 0);
		}

		private boolean isWinningGoal(int score) {
			return score > ((double) MAX_BALLS) / 2;
		}

		private boolean isDraw() {
			return scores().sum() == MAX_BALLS;
		}

		private IntStream scores() {
			return scores.values().stream().mapToInt(Integer::intValue);
		}

		private int[] teamids() {
			return scores.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
		}

	}

	protected Game(List<Detector> detectors, Config goalDetectorConfig,
			List<ScoreTracker.Listener> scoreTrackerListeners) {
		this.detectors = detectors;
		this.goalDetectorConfig = goalDetectorConfig;
		this.scoreTrackerListeners.addAll(scoreTrackerListeners);
	}

	public abstract Game update(AbsolutePosition pos);

	public abstract Game reset();

	public static Game newGame(Detector... detectors) {
		return newGame(Arrays.asList(detectors));
	}

	public static Game newGame(List<Detector> detectors) {
		return new GameoverGame(detectors, new GoalDetector.Config(), Collections.emptyList());
	}

	private static class InGameGame extends Game {

		private static class GameOverScoreState implements ScoreTracker.Listener {

			private boolean gameover;

			@Override
			public void scoreChanged(int teamid, int oldScore, int newScore) {
				// not interested on new scores
			}

			@Override
			public void won(int teamid) {
				gameover = true;
			}

			@Override
			public void draw(int[] teamids) {
				gameover = true;
			}

		}

		private final GameOverScoreState gameOverScoreState = new GameOverScoreState();
		private final List<Detector> detectorsWithGoalDetector;

		public InGameGame(List<Detector> detectors, Config goalDetectorConfig,
				List<ScoreTracker.Listener> scoreTrackerListeners) {
			super(detectors, goalDetectorConfig, scoreTrackerListeners);
			this.detectorsWithGoalDetector = addOnGoalDetector(detectors, scoreTrackerListeners);
			this.goalDetectorConfig = goalDetectorConfig;
		}

		@Override
		public Game update(AbsolutePosition pos) {
			for (Detector detector : detectorsWithGoalDetector) {
				detector.detect(pos);
			}
			return isGameover() ? reset() : this;
		}

		@Override
		public Game reset() {
			return new GameoverGame(detectors, goalDetectorConfig, scoreTrackerListeners);
		}

		private boolean isGameover() {
			return gameOverScoreState.gameover;
		}

		private List<Detector> addOnGoalDetector(List<Detector> detectors,
				List<ScoreTracker.Listener> scoreTrackerListeners) {
			return add(detectors, onGoal(goalDetectorConfig,
					inform(new DefaultScoreTracker(multiplexed(add(scoreTrackerListeners, gameOverScoreState))))));
		}

		private static <T> List<T> add(Collection<T> ts, T t) {
			List<T> result = new ArrayList<>(ts);
			result.add(t);
			return result;
		}

		private ScoreTracker.Listener multiplexed(List<ScoreTracker.Listener> listeners) {
			return new ScoreTracker.Listener() {
				@Override
				public void scoreChanged(int teamid, int oldScore, int newScore) {
					for (ScoreTracker.Listener listener : listeners) {
						listener.scoreChanged(teamid, oldScore, newScore);
					}
				}

				@Override
				public void won(int teamid) {
					for (ScoreTracker.Listener listener : listeners) {
						listener.won(teamid);
					}
				}

				@Override
				public void draw(int[] teamids) {
					for (ScoreTracker.Listener listener : listeners) {
						listener.draw(teamids);
					}
				}
			};
		}

		private GoalDetector.Listener inform(ScoreTracker scoreTracker) {
			return new GoalDetector.Listener() {
				@Override
				public void goal(int teamid) {
					scoreTracker.teamScored(teamid);
				}

				@Override
				public void goalRevert(int teamid) {
					scoreTracker.revertGoal(teamid);
				}
			};
		}

	}

	private static class GameoverGame extends Game {

		public GameoverGame(List<Detector> detectors, Config goalDetectorConfig,
				List<ScoreTracker.Listener> scoreTrackerListeners) {
			super(detectors, goalDetectorConfig, scoreTrackerListeners);
		}

		@Override
		public Game update(AbsolutePosition pos) {
			return new InGameGame(detectors.stream().map(Detector::newInstance).collect(toList()), goalDetectorConfig,
					scoreTrackerListeners).update(pos);
		}

		@Override
		public Game reset() {
			return this;
		}

	}

	public Game withGoalConfig(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
		return this;
	}

	public Game addScoreTracker(ScoreTracker.Listener scoreTracker) {
		this.scoreTrackerListeners.add(scoreTracker);
		return this;
	}

}
