package com.alphatica.alis.studio.web;

import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

/**
 * Endpoint returning a table in pages: {@code GET path?offset=0&limit=200}. The GUI asks for the pages that scroll
 * into view (pagedSource in the GUI's table module).
 */
public final class TableRoutes {
	static final int MAX_LIMIT = 1000;
	private static final int DEFAULT_LIMIT = 200;

	private TableRoutes() {
	}

	/**
	 * @param table computed once per state change, so every page request is cheap
	 */
	public static void paged(RoutesConfig routes, String path, StateCache<Table> table) {
		routes.get(path, ctx -> ctx.json(table.get().page(intParam(ctx, "offset", 0), Math.min(MAX_LIMIT,
				intParam(ctx, "limit", DEFAULT_LIMIT)))));
	}

	private static int intParam(Context ctx, String name, int defaultValue) {
		String value = ctx.queryParam(name);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Math.max(0, Integer.parseInt(value));
		} catch (NumberFormatException e) {
			throw new ValidationException("Invalid " + name + ": " + value);
		}
	}
}
