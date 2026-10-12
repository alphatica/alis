package com.alphatica.alis.studio.web.live;

/**
 * Server → GUI message: {"topic": "app", "event": "data-status", "data": ...}.
 */
public record LiveMessage(String topic, String event, Object data) {
}
