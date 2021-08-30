package com.github.smartfootballtable.cognition.data.position;

import static java.lang.Math.abs;

import lombok.Generated;

public abstract class RelativePosition implements Position {

	private static class Absent extends RelativePosition {

		public Absent(long timestamp) {
			super(timestamp);
		}

		@Override
		public boolean isNull() {
			return true;
		}

		@Override
		public double getX() {
			return -1;
		}

		@Override
		public double getY() {
			return -1;
		}

		@Override
		public boolean equalsPosition(RelativePosition other) {
			return other.isNull();
		}

		@Override
		public RelativePosition normalizeX() {
			throw newUnsupportedOperationException();
		}

		@Override
		public RelativePosition normalizeY() {
			throw newUnsupportedOperationException();
		}

		@Override
		public boolean isLeftHandSide() {
			throw newUnsupportedOperationException();
		}

		private RuntimeException newUnsupportedOperationException() {
			return new UnsupportedOperationException();
		}

	}

	private static class Present extends RelativePosition {

		private static final double CENTER_X = 0.5;
		private static final double CENTER_Y = 0.5;

		private final double x;
		private final double y;

		public Present(long timestamp, double x, double y) {
			super(timestamp);
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean isNull() {
			return false;
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		public RelativePosition normalizeX() {
			return create(getTimestamp(), CENTER_X + abs(CENTER_X - x), y);
		}

		public RelativePosition normalizeY() {
			return create(getTimestamp(), x, CENTER_Y + abs(CENTER_Y - y));
		}

		public boolean isLeftHandSide() {
			return getX() < CENTER_X;
		}

	}

	private final long timestamp;

	public static RelativePosition noPosition(long timestamp) {
		return new Absent(timestamp);
	}

	public static RelativePosition create(long timestamp, double x, double y) {
		return x == -1 && y == -1 ? noPosition(timestamp) : new Present(timestamp, x, y);
	}

	private RelativePosition(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	public abstract RelativePosition normalizeX();

	public abstract RelativePosition normalizeY();

	public abstract boolean isLeftHandSide();

	public boolean equalsPosition(RelativePosition other) {
		return Double.doubleToLongBits(getX()) == Double.doubleToLongBits(other.getX()) //
				&& Double.doubleToLongBits(getY()) == Double.doubleToLongBits(other.getY());
	}

	@Generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		long temp;
		temp = Double.doubleToLongBits(getX());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(getY());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Generated
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelativePosition other = (RelativePosition) obj;
		if (timestamp != other.timestamp)
			return false;
		return equalsPosition(other);
	}

	@Generated
	@Override
	public String toString() {
		return "RelativePosition [timestamp=" + timestamp + ", x=" + getX() + ", y=" + getY() + "]";
	}

}