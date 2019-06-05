package com.github.smartfootballtable.cognition.detector;

import com.github.smartfootballtable.cognition.data.position.AbsolutePosition;

public interface Detector {
	void detect(AbsolutePosition pos);

	Detector newInstance();
}