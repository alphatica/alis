package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.Constants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.studio.Constants.BACKEND_URL;

/**
 * Sends an error report to the Alphatica server in the background.
 */
final class ErrorReporter {

	private ErrorReporter() {
	}

	static void reportError(String message, Exception e) {
		Thread.ofVirtual().start(() -> {
			try (HttpClient client = HttpClient.newHttpClient()) {
				HttpRequest request = HttpRequest.newBuilder(URI.create(BACKEND_URL + "/error/report"))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(Json.write(new ErrorReport(prepareLines(message, e)))))
						.build();
				client.send(request, HttpResponse.BodyHandlers.discarding());
			} catch (IOException ex) {
				System.err.println("Error while reporting: " + ex);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
	}

	private static List<String> prepareLines(String message, Exception e) {
		List<String> lines = new ArrayList<>();
		lines.add("Version: " + Constants.STUDIO_BUILD);
		lines.add("Error message: " + message);
		describeException(lines, e);
		return lines;
	}

	private static void describeException(List<String> lines, Exception e) {
		Throwable describing = e;
		while (describing != null) {
			lines.add("Exception: " + describing + " " + describing.getMessage());
			addStackTrace(lines, describing);
			describing = describing.getCause();
		}
	}

	private static void addStackTrace(List<String> lines, Throwable e) {
		lines.add(e.getClass().getName());
		for (StackTraceElement element : e.getStackTrace()) {
			lines.add(element.getFileName() + ":" + element.getLineNumber() + " " + element.getMethodName());
		}
	}

	record ErrorReport(List<String> content) {
	}
}
