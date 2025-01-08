package com.alphatica.alis.tools.java;

import com.alphatica.alis.data.market.MarketName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface MarketAttributes {

	static String[] getAttributeNames(Map<MarketName, MarketAttributes> customInfo) {
		Set<String> names = new HashSet<>();
		for (Map.Entry<MarketName, MarketAttributes> entry : customInfo.entrySet()) {
			Map<String, String> row = entry.getValue().toAttributes();
			names.addAll(row.keySet());
		}
		String[] array = names.toArray(new String[0]);
		Arrays.sort(array);
		String[] full = new String[array.length + 1];
		full[0] = "Market";
		System.arraycopy(array, 0, full, 1, array.length);
		return full;
	}

	Map<String, String> toAttributes();

}
