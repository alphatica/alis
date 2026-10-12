package com.alphatica.alis.studio.web;

import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static com.alphatica.alis.studio.web.RunningInstance.Kind.NONE;
import static com.alphatica.alis.studio.web.RunningInstance.Kind.OTHER;
import static com.alphatica.alis.studio.web.RunningInstance.Kind.STUDIO;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RunningInstanceTest {
	private static final String HOST = "127.0.0.1";

	@Test
	void nothingListening() throws IOException {
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		assertEquals(NONE, RunningInstance.probe(HOST, port));
	}

	@Test
	void studioListening() throws IOException {
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		Javalin app = WebApp.create(Set.of(HOST + ":" + port));
		app.start(HOST, port);
		try {
			assertEquals(STUDIO, RunningInstance.probe(HOST, port));
		} finally {
			app.stop();
		}
	}

	@Test
	void otherHttpServerWithSamePath() throws IOException {
		assertEquals(OTHER, probeHttpServer(200, "{\"studioName\":\"Something else\"}"));
	}

	@Test
	void otherHttpServerWithoutJson() throws IOException {
		assertEquals(OTHER, probeHttpServer(200, "<html></html>"));
	}

	@Test
	void otherHttpServerWithStudioNameButError() throws IOException {
		assertEquals(OTHER, probeHttpServer(404, "{\"studioName\":\"Alis :: Algorithmic Investing Software\"}"));
	}

	@Test
	void notHttp() throws IOException {
		try (ServerSocket server = new ServerSocket(0)) {
			Thread.ofVirtual().start(() -> {
				try (Socket client = server.accept()) {
					client.getOutputStream().write("SSH-2.0-test\r\n".getBytes(StandardCharsets.US_ASCII));
				} catch (IOException e) {
					// the probe closed the connection
				}
			});
			assertEquals(OTHER, RunningInstance.probe(HOST, server.getLocalPort()));
		}
	}

	private static RunningInstance.Kind probeHttpServer(int status, String body) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(HOST, 0), 0);
		server.createContext(AppApi.PATH, exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		try {
			return RunningInstance.probe(HOST, server.getAddress().getPort());
		} finally {
			server.stop(0);
		}
	}
}
