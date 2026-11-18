package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.studio.logic.stats.Stat;

import java.util.List;

/**
 * One card of the edge analysis, shown in this order:
 *
 * @param table   null without a table
 * @param bridges from the index's value to the account's
 * @param facts   values shown under the bridges
 * @param chart   null without a chart
 */
public record EdgeCard(String title, String subtitle, String help, EdgeTable table, List<EdgeBridge> bridges,
					   List<Stat> facts, EdgeChart chart) {
}
