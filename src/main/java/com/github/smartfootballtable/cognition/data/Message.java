package com.github.smartfootballtable.cognition.data;

import javax.annotation.Generated;

public class Message {

	public static Message message(String topic, Object payload) {
		return new Message(topic, payload, false);
	}

	public static Message retainedMessage(String topic, Object payload) {
		return new Message(topic, payload, true);
	}

	private final String topic;
	private final String payload;
	private final boolean retained;

	protected Message(String topic, Object payload, boolean retained) {
		this.topic = topic;
		this.payload = payload == null ? null : String.valueOf(payload);
		this.retained = retained;
	}

	public String getPayload() {
		return payload;
	}

	public String getTopic() {
		return topic;
	}

	public boolean isRetained() {
		return retained;
	}

	@Generated("Eclipse-IDE")
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Generated("Eclipse-IDE")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	@Generated("Eclipse-IDE")
	@Override
	public String toString() {
		return "Message [topic=" + topic + ", payload=" + payload + "]";
	}

}