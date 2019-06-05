package com.github.smartfootballtable.cognition.parser;

import com.github.smartfootballtable.cognition.data.position.RelativePosition;

@FunctionalInterface
public interface LineParser {
	RelativePosition parse(String line);
}