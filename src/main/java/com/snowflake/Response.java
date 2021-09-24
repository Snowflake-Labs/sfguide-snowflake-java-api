package com.snowflake;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

	private final ArrayList<Object[]> result;
	private final long time_ms;

	public Response(ArrayList<Object[]> result, long time_ms) {
		this.result = result;
		this.time_ms = time_ms;
	}

	public ArrayList<Object[]> getResult() {
		return this.result;
	}

	@JsonProperty("time_ms")
	public long getTimeMs() {
		return this.time_ms;
	}
}
