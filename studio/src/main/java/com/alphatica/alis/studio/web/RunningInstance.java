package com.alphatica.alis.studio.web;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.alphatica.alis.studio.Constants.STUDIO_PRODUCT;

/**
 * Checks what already listens on the Studio port, so a second start opens the browser instead of failing.
 */
final class RunningInstance {
	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	enum Kind {
		/** Nothing listens on the port. */
		NONE,
		/** Studio (any build) listens on the port. */
		STUDIO,
		/** Another program listens on the port. */
		OTHER
	}

	private RunningInstance() {
	}

	static Kind probe(String host, int port) {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + AppApi.PATH))
				.timeout(TIMEOUT)
				.GET()
				.build();
		try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return isStudio(response) ? Kind.STUDIO : Kind.OTHER;
		} catch (ConnectException e) {
			return Kind.NONE;
		} catch (IOException e) {
			return Kind.OTHER;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Kind.OTHER;
		}
	}

	private static boolean isStudio(HttpResponse<String> response) {
		if (response.statusCode() != 200) {
			return false;
		}
		try {
			AppApi.AppStatus status = Json.read(response.body(), AppApi.AppStatus.class);
			return status.studioName() != null && status.studioName().startsWith(STUDIO_PRODUCT);
		} catch (JsonProcessingException e) {
			return false;
		}
	}
}
