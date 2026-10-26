package com.alphatica.alis.studio.web;

import io.javalin.Javalin;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

public final class WebStart {
	private static final String HOST = "127.0.0.1";
	private static final int DEFAULT_PORT = 7070;
	private static final String NO_BROWSER_ARG = "--no-browser";
	private static final String PORT_ARG_PREFIX = "--port=";

	private WebStart() {
	}

	public static void main(String[] args) {
		int port = port(args);
		String url = "http://" + HOST + ":" + port + "/";
		switch (RunningInstance.probe(HOST, port)) {
			case NONE -> {
				Javalin app = WebApp.create(Set.of(HOST + ":" + port, "localhost:" + port), Splash::close);
				app.start(HOST, port);
				System.out.println("Alis Studio: " + url);
			}
			case STUDIO -> System.out.println("Alis Studio is already running: " + url);
			case OTHER -> {
				showError("Port " + port + " is used by another program. Close it or start Alis Studio with "
						+ PORT_ARG_PREFIX + "<another port>.");
				System.exit(1);
			}
		}
		if (List.of(args).contains(NO_BROWSER_ARG)) {
			Splash.close();
		} else {
			openBrowser(url);
			Splash.closeLater();
		}
	}

	/** The JAR is often started without a console, so the message also goes to a dialog when there is a screen. */
	private static void showError(String message) {
		System.err.println(message);
		if (!GraphicsEnvironment.isHeadless()) {
			JOptionPane.showMessageDialog(null, message, "Alis Studio", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static int port(String[] args) {
		return List.of(args).stream()
				.filter(arg -> arg.startsWith(PORT_ARG_PREFIX))
				.map(arg -> Integer.parseInt(arg.substring(PORT_ARG_PREFIX.length())))
				.findFirst()
				.orElse(DEFAULT_PORT);
	}

	private static void openBrowser(String url) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(URI.create(url));
			} else {
				new ProcessBuilder("xdg-open", url).start();
			}
		} catch (IOException | UnsupportedOperationException e) {
			System.err.println("Unable to open browser, go to " + url + " (" + e.getMessage() + ")");
		}
	}
}
