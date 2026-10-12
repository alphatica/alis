package com.alphatica.alis.studio.web.live;

/**
 * GUI → server message: {"op": "subscribe", "topic": "job/..."}.
 */
record ClientCommand(String op, String topic) {
	static final String SUBSCRIBE = "subscribe";
	static final String UNSUBSCRIBE = "unsubscribe";
}
