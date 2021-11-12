package com.snowflake;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultResponse {

	private final String result;
	private final long time_ms;

	public DefaultResponse() {
		this.result = "Nothing to see here";
		this.time_ms = 0;
	}

	public String getResult() {
		return this.result;
	}

	@JsonProperty("time_ms")
	public long getTimeMs() {
		return this.time_ms;
	}
}
