package com.github.smartfootballtable.cognition.data;

import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;
import com.github.smartfootballtable.cognition.data.position.RelativePosition;
import com.github.smartfootballtable.cognition.data.unit.DistanceUnit;

import lombok.Generated;

public class Table {

	private final double width;
	private final double height;
	private final DistanceUnit distanceUnit;

	public Table(double width, double height, DistanceUnit distanceUnit) {
		this.width = width;
		this.height = height;
		this.distanceUnit = distanceUnit;
	}

	public AbsolutePosition toAbsolute(RelativePosition pos) {
		return new AbsolutePosition(pos, convertX(pos.getX()), convertY(pos.getY()));
	}

	private double convertY(double y) {
		return height * y;
	}

	private double convertX(double x) {
		return width * x;
	}

	public Distance getHeight() {
		return new Distance(height, distanceUnit);
	}

	public Distance getWidth() {
		return new Distance(width, distanceUnit);
	}

	public DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	@Generated
	@Override
	public String toString() {
		return "Table [width=" + width + ", height=" + height + ", distanceUnit=" + distanceUnit + "]";
	}

}
