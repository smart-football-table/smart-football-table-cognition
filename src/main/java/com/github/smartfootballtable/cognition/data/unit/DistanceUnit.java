package com.github.smartfootballtable.cognition.data.unit;

public enum DistanceUnit {

	CENTIMETER {
		@Override
		public String symbol() {
			return "cm";
		}

		@Override
		public boolean isMetric() {
			return true;
		}

		@Override
		public double toCentimeters(double value) {
			return value;
		}

		@Override
		public double toMeters(double value) {
			return value / 100;
		}

		@Override
		public double toKilometers(double value) {
			return toMeters(value) / 1_000;
		}

		@Override
		public double toMiles(double value) {
			return toKilometers(value) / 1.609344;
		}

		@Override
		public double toInches(double value) {
			return value / 2.54;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toCentimeters(value);
		}
	},
	METERS {
		@Override
		public String symbol() {
			return "m";
		}

		@Override
		public boolean isMetric() {
			return true;
		}

		@Override
		public double toCentimeters(double value) {
			return value * 100;
		}

		@Override
		public double toMeters(double value) {
			return value;
		}

		@Override
		public double toKilometers(double value) {
			return value / 1_000;
		}

		@Override
		public double toMiles(double value) {
			return toKilometers(value) / 1.609344;
		}

		@Override
		public double toInches(double value) {
			return toCentimeters(value) / 2.54;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toMeters(value);
		}
	},
	KILOMETERS {
		@Override
		public String symbol() {
			return "km";
		}

		@Override
		public boolean isMetric() {
			return true;
		}

		@Override
		public double toCentimeters(double value) {
			return toMeters(value) * 100;
		}

		@Override
		public double toMeters(double value) {
			return value * 1_000;
		}

		@Override
		public double toKilometers(double value) {
			return value;
		}

		@Override
		public double toMiles(double value) {
			return value / 1.609344;
		}

		@Override
		public double toInches(double value) {
			return toCentimeters(value) / 2.54;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toKilometers(value);
		}
	},
	INCHES {
		@Override
		public String symbol() {
			return "inch";
		}

		@Override
		public boolean isMetric() {
			return false;
		}

		@Override
		public double toCentimeters(double value) {
			return value * 2.54;
		}

		@Override
		public double toMeters(double value) {
			return toCentimeters(value) / 100;
		}

		@Override
		public double toKilometers(double value) {
			return toMeters(value) / 1_000;
		}

		@Override
		public double toInches(double value) {
			return value;
		}

		@Override
		public double toMiles(double value) {
			return value / 63360;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toInches(value);
		}
	},
	MILES {
		@Override
		public String symbol() {
			return "mi";
		}

		@Override
		public boolean isMetric() {
			return false;
		}

		@Override
		public double toCentimeters(double value) {
			return toMeters(value) * 100;
		}

		@Override
		public double toMeters(double value) {
			return toKilometers(value) * 1_000;
		}

		@Override
		public double toKilometers(double value) {
			return value * 1.609344;
		}

		@Override
		public double toMiles(double value) {
			return value;
		}

		@Override
		public double toInches(double value) {
			return value * 63360;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toMiles(value);
		}
	};

	public abstract boolean isMetric();

	public abstract String symbol();

	public abstract double toCentimeters(double value);

	public abstract double toMeters(double value);

	public abstract double toKilometers(double value);

	public abstract double toMiles(double value);

	public abstract double toInches(double value);

	public abstract double convert(double value, DistanceUnit source);

}