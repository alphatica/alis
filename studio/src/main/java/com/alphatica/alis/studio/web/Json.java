package com.alphatica.alis.studio.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.json.JavalinJackson;

/**
 * One JSON mapper for REST responses and WebSocket messages.
 */
public final class Json {
	private static final ObjectMapper MAPPER = JavalinJackson.defaultMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	public static final JavalinJackson JAVALIN = new JavalinJackson(MAPPER, false);

	private Json() {
	}

	public static String write(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to write JSON", e);
		}
	}

	public static <T> T read(String json, Class<T> type) throws JsonProcessingException {
		return MAPPER.readValue(json, type);
	}
}
