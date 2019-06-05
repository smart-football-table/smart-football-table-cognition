package com.github.smartfootballtable.cognition;

import java.util.function.Consumer;

import com.github.smartfootballtable.cognition.data.Message;

public interface MessageProvider {
	void addConsumer(Consumer<Message> consumer);
}