package com.alphatica.alis.studio.web;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves the SolidJS GUI built by {@code build-gui.sh} from the classpath ({@code /web}).
 * File names under {@code /assets} contain a content hash, so the browser may cache them forever;
 * {@code index.html} is served for every other page path and is never cached.
 */
final class Frontend {
	private static final String ROOT = "/web";
	private static final String NOT_BUILT = "<!doctype html><title>Alis Studio</title>"
			+ "<p>The GUI is not included in this build. Run <code>./build-gui.sh</code>.</p>";

	private Frontend() {
	}

	static void configure(JavalinConfig config) {
		String indexHtml = readIndex();
		config.staticFiles.add(staticFiles -> {
			staticFiles.hostedPath = "/assets";
			staticFiles.directory = ROOT + "/assets";
			staticFiles.location = Location.CLASSPATH;
			staticFiles.headers = Map.of(Header.CACHE_CONTROL, "max-age=31536000, immutable");
		});
		config.spaRoot.addHandler("/", ctx -> {
			if (ctx.path().startsWith("/api/")) {
				ctx.status(HttpStatus.NOT_FOUND).json(new ErrorMessage("Not found", ctx.path(), null));
				return;
			}
			ctx.status(HttpStatus.OK);
			ctx.header(Header.CACHE_CONTROL, "no-cache");
			ctx.header("Content-Security-Policy", contentSecurityPolicy(ctx.host()));
			ctx.html(indexHtml);
		});
	}

	/**
	 * The page may load scripts and styles and connect only to this server, so even a compromised library in the
	 * bundle cannot send data elsewhere. The WebSocket address is listed explicitly: not every browser treats
	 * 'self' as covering ws://.
	 */
	private static String contentSecurityPolicy(String host) {
		return "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
				+ "connect-src 'self' ws://" + host + "; object-src 'none'; base-uri 'none'; "
				+ "form-action 'self'; frame-ancestors 'none'";
	}

	private static String readIndex() {
		try (InputStream in = Frontend.class.getResourceAsStream(ROOT + "/index.html")) {
			if (in == null) {
				System.err.println("GUI not found on the classpath. Run ./build-gui.sh");
				return NOT_BUILT;
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
